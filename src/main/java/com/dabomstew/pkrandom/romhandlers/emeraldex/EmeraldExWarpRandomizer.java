package com.dabomstew.pkrandom.romhandlers.emeraldex;

import com.dabomstew.pkrandom.warps.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.jgrapht.Graph;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.traverse.BreadthFirstIterator;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class EmeraldExWarpRandomizer {

    private static final String LOW_PRIORITY_TAG =  "low_priority";


    public static List<WarpRemapping> randomizeWarps(Long seed, int level, boolean extraDeadendRemoval, boolean inGymOrder) {

        WarpData warpData = loadRandomWarpData();
        WarpConfig warpConfig = new WarpConfig(level, extraDeadendRemoval, seed, inGymOrder);
        return getRandomWarps(warpConfig, warpData);
    }

    public static WarpData loadRandomWarpData() {

        WarpData warpData = new WarpData();

        // Escape Paths
        InputStream escapePathsStream = EmeraldExWarpRandomizer.class.getResourceAsStream("/scripts/EscapePaths.json");
        String escapePathsString = new BufferedReader(new InputStreamReader(escapePathsStream, StandardCharsets.UTF_8))
                .lines().collect(Collectors.joining("\n"));
        Gson escapePathGson = new Gson();
        EscapePaths escapePaths = escapePathGson.fromJson(escapePathsString, EscapePaths.class);
        warpData.setEscapePaths(escapePaths);

        // Flags
        InputStream flagsStream = EmeraldExWarpRandomizer.class.getResourceAsStream("/scripts/Flags.json");
        String flagsString = new BufferedReader(new InputStreamReader(flagsStream, StandardCharsets.UTF_8))
                .lines().collect(Collectors.joining("\n"));
        Gson flagsGson = new Gson();
        Flags flags = flagsGson.fromJson(flagsString, Flags.class);
        warpData.setFlags(flags);

        // Key Locations
        InputStream keyLocationsStream = EmeraldExWarpRandomizer.class.getResourceAsStream("/scripts/KeyLocations.json");
        String keyLocationsString = new BufferedReader(new InputStreamReader(keyLocationsStream, StandardCharsets.UTF_8))
                .lines().collect(Collectors.joining("\n"));
        Gson keyLocationsGson = new Gson();
        KeyLocations keyLocations = keyLocationsGson.fromJson(keyLocationsString, KeyLocations.class);
        warpData.setKeyLocations(keyLocations);

        // Warps
        InputStream warpsStream = EmeraldExWarpRandomizer.class.getResourceAsStream("/scripts/Warps.json");
        String warpsString = new BufferedReader(new InputStreamReader(warpsStream, StandardCharsets.UTF_8))
                .lines().collect(Collectors.joining("\n"));
        Gson warpsGson = new Gson();
        Type mapType = new TypeToken<Map<String, Warp>>() {}.getType();
        Map<String, Warp> warpsMap = warpsGson.fromJson(warpsString, mapType);
        warpData.setWarps(warpsMap);
        return warpData;
    }

    public static List<WarpRemapping> getRandomWarps(WarpConfig config, WarpData warpData) {
        WarpRandomizationState state = createState(config, warpData);
        return generateRandomMappings(state);
    }

    private static WarpRandomizationState createState(WarpConfig warpConfig, WarpData warpData) {

        WarpRandomizationState state = new WarpRandomizationState(warpConfig, warpData);

        Graph<Warp, MapEdge> mapGraph = new DefaultDirectedGraph<>(MapEdge.class);

        // Add all the map nodes
        state.getWarps().values().forEach(mapGraph::addVertex);

        // Connect all the edges
        for (Map.Entry<String, Warp> keyWarp : state.getWarps().entrySet()) {

            Warp warp = keyWarp.getValue();
            String warpId = keyWarp.getKey();

            if (warp.getConnections() == null) {
                continue;
            }

            for (Map.Entry<String, String> connection : warp.getConnections().entrySet()) {

                MapEdge edge;

                if (connection.getValue().equals("true")) {

                    edge = MapEdge.fixedEdge(warpId, connection.getKey());

                } else {

                    edge = MapEdge.conditionalEdge(warpId, connection.getKey(), connection.getValue());
                    state.addRemainingConditionalEdge(edge);

                }

                Warp vertexToAdd = state.getWarps().get(connection.getKey());

                if (vertexToAdd != null) {
                    mapGraph.addEdge(warp, vertexToAdd, edge);
                }

            }

        }

        ConnectivityInspector<Warp, MapEdge> inspector = new ConnectivityInspector<>(mapGraph);
        List<Set<Warp>> components = inspector.connectedSets()
                .stream()
                .filter(c -> c.size() > 1)
                .collect(Collectors.toList());
        state.setUnconnectedComponents(components);

        Set<MapEdge> conditionalEdges = mapGraph.edgeSet()
                .stream()
                .filter(edge -> edge.getType() == MapEdge.Type.CONDITIONAL)
                .collect(Collectors.toSet());

        conditionalEdges.forEach(mapGraph::removeEdge);

        state.moveFlagLocationToKeyIfFinalForLevel();
        state.selectEscapePathWarps();
        state.selectRoot();

        state.setMapGraph(mapGraph);

        return state;
    }


    private static List<WarpRemapping> generateRandomMappings(WarpRandomizationState state) {

        while (state.isMoreWarpsToMap()) {

            try {
                doNextMapping(state);
                state.updateConnections();
                //System.out.println("Warps " + state.getProgress());

                if (!state.isMoreWarpsToMap()) {
                    doSanityChecks(state);
                }

            } catch (ImpossibleMapException e) {

                // re-init the state with a new seed and try again
                WarpConfig config = state.getConfig();
                System.out.printf("Randomization failed for seed %s.\n%s \n Tying a new one.%n", e.getMessage(), config.getSeed());
                config.incrementSeed();
                state = createState(config, loadRandomWarpData());
            }

        }

        List<WarpRemapping> remappings = state.getRemappings();
        // System.out.println("Remappings " + remappings.size());
        Collections.sort(remappings);
        return remappings;
    }

    private static void doSanityChecks(WarpRandomizationState state) throws ImpossibleMapException {

        Graph<Warp, MapEdge> mapGraph = state.getMapGraph();

        boolean hasDanglingWarps = mapGraph.vertexSet()
                                           .stream()
                                           .anyMatch(w -> !mapGraph.edgesOf(w).stream().map(MapEdge::getType)
                                           .collect(Collectors.toList())
                                           .contains(MapEdge.Type.WARP));
        if (hasDanglingWarps) {
            throw new ImpossibleMapException("Final graph had warps that should be mapped but weren't");
        }

        if (state.getConfig().getLevel() == 10) {

            // This should not be hardcoded
            int warpCount = mapGraph.vertexSet().size();
            if (warpCount != 516) {
                throw new ImpossibleMapException("Expected 516 edged but found " + warpCount);
            }

            // This is slightly higher than warpCount because of double warps
            List<WarpRemapping> remappings = state.getRemappings();
            int remappingSize = remappings.size();
            if (remappingSize < 536) {
                throw new ImpossibleMapException("Expected 536 edged but found " + remappingSize);
            }

            Set<String> validationList = new HashSet<>(validationTriggerList());
            validationList.removeAll(remappings.stream().map(r -> r.triggerString()).collect(Collectors.toList()));
            if (!validationList.isEmpty()) {
                throw new ImpossibleMapException("Some warp triggers we expect are missing in the final mappings");
            }

        }

        if (state.getConfig().getLevel() == 10 && state.getConfig().isInGymOrder()) {

            if (!validateInOrderLogic(state)) {
                throw new ImpossibleMapException("The validator could not find the intended way to beat this seed");
            }

        }

        if (state.getConfig().getLevel() == 10 && !state.getConfig().isInGymOrder()) {

            if (!validateOutOfOrderLogic(state)) {
                throw new ImpossibleMapException("The validator could not find the intended way to beat this seed");
            }

        }

    }

    private static void doNextMapping(WarpRandomizationState state) throws ImpossibleMapException {

        List<Warp> reachableNodes = findUnmappedReachableNodes(state);
        reachableNodes.sort(Comparator.comparing(Warp::getId));
        Collections.shuffle(reachableNodes, state.getRandom());

        Graph<Warp, MapEdge> mapGraph = state.getMapGraph();
        List<Warp> unreachableNodes = mapGraph.vertexSet().stream().filter(w -> !w.isMapped()).collect(Collectors.toList());
        unreachableNodes.removeAll(reachableNodes);
        unreachableNodes.sort(Comparator.comparing(Warp::getId));
        Collections.shuffle(unreachableNodes, state.getRandom());

        List<Warp> unreachableFlagLocations = unreachableNodes.stream()
                .filter(w -> state.getRemainingFlagLocations().containsKey(w.getId())).collect(Collectors.toList());
        unreachableFlagLocations.sort(Comparator.comparing(Warp::getId));
        Collections.shuffle(unreachableFlagLocations, state.getRandom());

        List<Warp> unreachableKeyLocations = unreachableNodes.stream()
                .filter(w -> state.getRemainingKeyLocations().containsKey(w.getId())).collect(Collectors.toList());
        unreachableKeyLocations.sort(Comparator.comparing(Warp::getId));
        Collections.shuffle(unreachableKeyLocations, state.getRandom());

        List<Warp> unreachableHubs = unreachableNodes.stream()
                .filter(w -> mapGraph.degreeOf(w) > 1)
                .collect(Collectors.toList());
        unreachableHubs.sort(Comparator.comparing(Warp::getId));
        Collections.shuffle(unreachableHubs, state.getRandom());

        // Make sure there are still warps to map, and we have not run out of mappings to add all the locations that need to be added
        if (reachableNodes.isEmpty() && unreachableNodes.isEmpty()) {
            return;
        } else if (reachableNodes.isEmpty() && !(unreachableFlagLocations.isEmpty() && unreachableKeyLocations.isEmpty())) {
            throw new ImpossibleMapException("Can't map any more nodes but important locations were missed");
        } else if (reachableNodes.isEmpty() && !unreachableNodes.stream()
                .filter(w -> !w.getTags().contains(LOW_PRIORITY_TAG))
                .collect(Collectors.toSet()).isEmpty()) {
            throw new ImpossibleMapException("Can't map any more nodes but there are still non-low priority location missed");
        }

        // Our preference is that certain warps always have some path back to the start to avoid softlocks, so we prefer not to pick them first
        if (!state.getRemainingMustLinkHomeWarps().isEmpty()) {
            List<Warp> preferredList = reachableNodes.stream()
                    .filter(w -> !state.getRemainingMustLinkHomeWarps().contains(w.getId()))
                    .collect(Collectors.toList());

            if (!preferredList.isEmpty()) {
                reachableNodes = preferredList;
            } else {
                state.clearMustLinkHomeWarps();
            }
        }

        WarpPair warpPair;

        if (!state.getUnconnectedComponents().isEmpty()) {

            //System.out.println("Mapping MAIN HUB");
            warpPair = getUnconnectedComponentMapping(state, reachableNodes);

        } else if (!state.getRemainingMustLinkHomeWarps().isEmpty() && !reachableNodes.isEmpty()) {

            //System.out.println("Mapping HOME LINK");
            warpPair = getHomeLinkMapping(state, reachableNodes);

        } else if (!unreachableFlagLocations.isEmpty()) {

            //System.out.println("Mapping FLAG LOCATION");
            Warp sourceWarp = state.getWellSpreadNode(reachableNodes);
            reachableNodes.remove(sourceWarp);
            Warp targetWarp = state.getRandomUnmappedWarpForWarp(sourceWarp, unreachableFlagLocations);
            warpPair = new WarpPair(sourceWarp, targetWarp);

        } else if (!unreachableHubs.isEmpty()) {

            //System.out.println("Mapping OTHER HUBS");
            Warp sourceWarp = reachableNodes.get(state.getRandom().nextInt(reachableNodes.size()));
            reachableNodes.remove(sourceWarp);
            List<Warp> targetCandidates = unreachableNodes.stream()
                    .filter(w -> mapGraph.degreeOf(w) > 1)
                    .collect(Collectors.toList());
            Warp targetWarp = state.getRandomUnmappedWarpForWarp(sourceWarp, targetCandidates);
            warpPair = new WarpPair(sourceWarp, targetWarp);

        } else if (!unreachableKeyLocations.isEmpty()) {

            //System.out.println("Mapping KEY LOCATIONS");
            Warp sourceWarp = state.getWellSpreadNode(reachableNodes);
            reachableNodes.remove(sourceWarp);
            Warp targetWarp = state.getRandomUnmappedWarpForWarp(sourceWarp, unreachableKeyLocations);
            warpPair = new WarpPair(sourceWarp, targetWarp);

        } else if (!unreachableNodes.isEmpty()) {

            //System.out.println("Mapping DEAD ENDS");
            Warp sourceWarp = reachableNodes.get(state.getRandom().nextInt(reachableNodes.size()));
            Warp targetWarp;
            reachableNodes.remove(sourceWarp);

            List<Warp> priorityUnreachableNodes = unreachableNodes.stream()
                    .filter(w -> !w.getTags().contains(LOW_PRIORITY_TAG))
                    .collect(Collectors.toList());
            if (priorityUnreachableNodes.isEmpty()) {
                targetWarp = state.getRandomUnmappedWarpForWarp(sourceWarp, priorityUnreachableNodes);
            } else {
                targetWarp = state.getRandomUnmappedWarpForWarp(sourceWarp, unreachableNodes);
            }
            warpPair = new WarpPair(sourceWarp, targetWarp);

        } else if (reachableNodes.size() > 1) {

            //System.out.println("Mapping LEFT OVER CONNECTIONS");
            Warp sourceWarp = reachableNodes.get(state.getRandom().nextInt(reachableNodes.size()));
            reachableNodes.remove(sourceWarp);
            Warp targetWarp = state.getRandomUnmappedWarpForWarp(sourceWarp, reachableNodes);
            warpPair = new WarpPair(sourceWarp, targetWarp);

        } else {

            // System.out.println("Mapping FINAL UNEVEN LEFTOVER CONNECTION");
            Warp sourceWarp = reachableNodes.get(state.getRandom().nextInt(reachableNodes.size()));
            reachableNodes.remove(sourceWarp);

            Warp targetWarp = state.getOddOnOutWarp();
            mapGraph.addVertex(targetWarp);
            warpPair = new WarpPair(sourceWarp, targetWarp);
        }

        if (warpPair.getSourceWarp() == null) {
            // Target should never be null,
            // If the source is null it's not really ideal, but we can do our best

            if (!reachableNodes.isEmpty()) {
                throw new ImpossibleMapException("Some reachable nodes could not be randomized");
            } else {
                System.out.println("All reachable nodes randomized");
            }

            if (!unreachableNodes.isEmpty()) {
                throw new ImpossibleMapException("Some nodes are unreachable");
//                warpPair.getTargetWarp().setMapped();
//                state.setMoreWarpsToMap(false);
//                return;
            } else {
                System.out.println("No nodes left that can't be reached");
            }
        }

        if (warpPair.getSourceWarp().isMapped() || warpPair.getTargetWarp().isMapped()) {
            throw new ImpossibleMapException("Trying to map a warp that is already mapped...");
        }

        MapEdge mapEdge = MapEdge.warpEdge(warpPair.getSourceWarp().getId(), warpPair.getTargetWarp().getId());
        mapGraph.addEdge(warpPair.getSourceWarp(), warpPair.getTargetWarp(), mapEdge);

        if (!mapGraph.containsEdge(mapEdge)) {
            throw new ImpossibleMapException("JGrapht messed up, let's try and recover...");
        }

        if (!warpPair.getSourceWarp().getId().equals(warpPair.getTargetWarp().getId())) {
            MapEdge oppositeDirectionMapEdge = MapEdge.warpEdge(warpPair.getTargetWarp().getId(), warpPair.getSourceWarp().getId());
            mapGraph.addEdge(warpPair.getTargetWarp(), warpPair.getSourceWarp(), oppositeDirectionMapEdge);

            if (!mapGraph.containsEdge(oppositeDirectionMapEdge)) {
                throw new ImpossibleMapException("JGrapht messed up, let's try and recover...");
            }
        }

        warpPair.getSourceWarp().setMapped();
        warpPair.getTargetWarp().setMapped();
    }

    private static WarpPair getHomeLinkMapping(WarpRandomizationState state, List<Warp> reachableNodes)
            throws ImpossibleMapException {

        Warp sourceWarp;
        Warp targetWarp;

        List<Warp> sourceCandidates = state.getMapGraph()
                .vertexSet()
                .stream()
                .filter(w -> !w.isMapped())
                .filter(w -> state.getRemainingMustLinkHomeWarps().contains(w.getId()))
                .sorted(Comparator.comparing(Warp::getId))
                .collect(Collectors.toList());

        if (sourceCandidates.isEmpty()) {
            sourceWarp = reachableNodes.get(state.getRandom().nextInt(reachableNodes.size()));
            state.clearMustLinkHomeWarps();
        } else {
            sourceWarp = sourceCandidates.get(state.getRandom().nextInt(sourceCandidates.size()));
        }

        reachableNodes.remove(sourceWarp);

        if (!state.hasHomePaths()) {
            state.generateHomePaths(reachableNodes);
        }

        List<Warp> preferredTargetCandidates = state.getHomePaths();

        if (preferredTargetCandidates.isEmpty()) {
            targetWarp = state.getRandomUnmappedWarpForWarp(sourceWarp, reachableNodes);
        } else {
            targetWarp = state.getRandomUnmappedWarpForWarp(sourceWarp, preferredTargetCandidates);
        }

        reachableNodes.remove(targetWarp);

        state.getRemainingMustLinkHomeWarps().remove(sourceWarp.getId());
        state.getHomePaths().remove(targetWarp);

        return new WarpPair(sourceWarp, targetWarp);
    }

    private static WarpPair getUnconnectedComponentMapping(WarpRandomizationState state, List<Warp> reachableNodes)
            throws ImpossibleMapException {

        Warp sourceWarp;
        Warp targetWarp;

        if (reachableNodes.isEmpty()) {
            throw new ImpossibleMapException("Could not map any new component because there are no free warps");
        }

        Collections.shuffle(reachableNodes, state.getRandom());
        sourceWarp = reachableNodes.get(0);
        reachableNodes.remove(sourceWarp);

        List<Warp> allUnconnectedComponentWarps = state.getUnconnectedComponents()
                .stream()
                .flatMap(Set::stream)
                .collect(Collectors.toList());

        // If we can use unconnected warps that will not lead to softlocks that would be better
        List<Warp> preferedUnconnectedComponentWarps =
                allUnconnectedComponentWarps.stream()
                        .filter(w -> !state.getRemainingMustLinkHomeWarps().contains(w.getId()))
                        .collect(Collectors.toList());

        if (preferedUnconnectedComponentWarps.isEmpty()) {
            state.clearMustLinkHomeWarps();
        } else {
            allUnconnectedComponentWarps = preferedUnconnectedComponentWarps;
        }

        targetWarp = state.getRandomUnmappedWarpForWarp(sourceWarp, allUnconnectedComponentWarps);
        state.removeComponentWithWarp(targetWarp);

        return new WarpPair(sourceWarp, targetWarp);
    }

    private static class WarpPair {
        private final Warp sourceWarp;
        private final Warp targetWarp;

        public WarpPair(Warp sourceWarp, Warp targetWarp) {
            this.sourceWarp = sourceWarp;
            this.targetWarp = targetWarp;
        }

        public Warp getSourceWarp() {
            return sourceWarp;
        }

        public Warp getTargetWarp() {
            return targetWarp;
        }

    }

    public static List<Warp> findUnmappedReachableNodes(WarpRandomizationState state) {

        List<Warp> unmappedNodes = new ArrayList<>();
        BreadthFirstIterator<Warp, MapEdge> iterator = new BreadthFirstIterator<>(state.getMapGraph(), state.getRoot());
        while (iterator.hasNext()) {
            try {
                Warp currentVertex = iterator.next();

                if (!currentVertex.isMapped()) {
                    unmappedNodes.add(currentVertex);
                }

            } catch (NullPointerException e) {
                /* Seems like the jgrapht has some bugs for graphs that aren't well-connected */
            }
        }

        return unmappedNodes;
    }

    /**
     * This is not a definitive test, the algorithm shouldn't have generated an unsolvable map, so it's just a quick sanity check.
     * Specifically this does not check goals locked behind later gyms
     */
    private static boolean validateInOrderLogic(WarpRandomizationState state) {

        Graph<Warp, MapEdge> mapGraph = state.getMapGraph();

        Warp startingWarp = state.getWarps().get("E,0,10,2");
        List<String> e4Goals = new ArrayList<>(List.of("E,16,0,0", "E,16,1,0", "E,16,2,0", "E,16,3,0", "E,16,4,0", "E,24,107,0"));

        ConnectivityInspector<Warp, MapEdge> connectivityInspector = new ConnectivityInspector<>(mapGraph);
        for (String goal : e4Goals) {
            Warp targetWarp = state.getWarps().get(goal);
            if (!connectivityInspector.pathExists(startingWarp, targetWarp)) {
                return false;
            }
        }

        Set<MapEdge> conditionalEdges = mapGraph.edgeSet()
                                                .stream()
                                                .filter(e -> e.getType() == MapEdge.Type.CONDITIONAL)
                                                .filter(e -> e.getCondition().equals("ALL_BADGES"))
                                                .filter(e -> e.getCondition().equals("HOENN_WATERFALL"))
                                                .collect(Collectors.toSet());
        mapGraph.removeAllEdges(conditionalEdges);

        connectivityInspector = new ConnectivityInspector<>(mapGraph);
        List<String> lastGymGoals = new ArrayList<>(List.of("E,14,0,0", "E,12,1,0",  "E,15,0,0"));
        for (String goal : lastGymGoals) {
            Warp targetWarp = state.getWarps().get(goal);
            if (!connectivityInspector.pathExists(startingWarp, targetWarp)) {
                return false;
            }
        }

        // Remove Surf and check that we can still access norman
        conditionalEdges = mapGraph.edgeSet()
                                   .stream()
                                   .filter(e -> e.getType() == MapEdge.Type.CONDITIONAL)
                                   .filter(e -> e.getCondition().equals("HOENN_SURF"))
                                   .collect(Collectors.toSet());
        mapGraph.removeAllEdges(conditionalEdges);
        Warp normanWarp = state.getWarps().get("E,8,1,0");
        connectivityInspector = new ConnectivityInspector<>(mapGraph);
        if (!connectivityInspector.pathExists(startingWarp, normanWarp)) {
            return false;
        }

        // Remove strength and check we can still access Flannery and Rusturf tunnel
        conditionalEdges = mapGraph.edgeSet()
                                   .stream()
                                   .filter(e -> e.getType() == MapEdge.Type.CONDITIONAL)
                                   .filter(e -> e.getCondition().equals("HOENN_STRENGTH"))
                                   .collect(Collectors.toSet());
        mapGraph.removeAllEdges(conditionalEdges);
        connectivityInspector = new ConnectivityInspector<>(mapGraph);
        List<String> preStrengthGoals = new ArrayList<>(List.of("E,4,1,0", "E,24,4,0"));
        for (String goal : preStrengthGoals) {
            Warp targetWarp = state.getWarps().get(goal);
            if (!connectivityInspector.pathExists(startingWarp, targetWarp)) {
                return false;
            }
        }

        // Remove Rocksmash and check that we can still access Wattson and rocksmash guy... and all the gyms before
        conditionalEdges = mapGraph.edgeSet()
                                   .stream()
                                   .filter(e -> e.getType() == MapEdge.Type.CONDITIONAL)
                                   .filter(e -> e.getCondition().equals("HOENN_ROCK_SMASH"))
                                   .collect(Collectors.toSet());
        mapGraph.removeAllEdges(conditionalEdges);
        connectivityInspector = new ConnectivityInspector<>(mapGraph);
        List<String> preRockSmashGoals = new ArrayList<>(List.of("E,10,2,0", "E,10,0,0", "E,11,3,0", "E,3,3,0"));
        for (String goal : preRockSmashGoals) {
            Warp targetWarp = state.getWarps().get(goal);
            if (!connectivityInspector.pathExists(startingWarp, targetWarp)) {
                return false;
            }
        }

        return true;
    }

    /**
     * This is not a definitive test, the algorithm shouldn't have generated an unsolvable map, quick sanity check.
     * Specifically this does not check goals locked behind later gyms
     */
    private static boolean  validateOutOfOrderLogic(WarpRandomizationState state) {

        Graph<Warp, MapEdge> mapGraph = state.getMapGraph();

        Warp startingWarp = state.getWarps().get("E,0,10,2");
        List<String> allGoals = new ArrayList<>(List.of("E,16,0,0",
                                                        "E,16,1,0",
                                                        "E,16,2,0",
                                                        "E,16,3,0",
                                                        "E,16,4,0",
                                                        "E,24,107,0",
                                                        "E,14,0,0",
                                                        "E,12,1,0",
                                                        "E,0,7,2",
                                                        "E,24,42,0",
                                                        "E,15,0,0",
                                                        "E,11,3,0",
                                                        "E,3,3,0",
                                                        "E,10,0,0",
                                                        "E,4,1,0",
                                                        "E,8,1,0"));

        ConnectivityInspector<Warp, MapEdge> connectivityInspector = new ConnectivityInspector<>(mapGraph);
        for (String goal : allGoals) {
            Warp targetWarp = state.getWarps().get(goal);
            if (!connectivityInspector.pathExists(startingWarp, targetWarp)) {
                return false;
            }
        }

        // None of the gyms are all badges locked
        Graph<Warp, MapEdge> badgeLockCheckGraph = copyGraph(mapGraph);
        Set<MapEdge> conditionalEdges = badgeLockCheckGraph.edgeSet()
                                                           .stream()
                                                           .filter(e -> e.getType() == MapEdge.Type.CONDITIONAL)
                                                           .filter(e -> e.getCondition().equals("ALL_BADGES"))
                                                           .collect(Collectors.toSet());
        badgeLockCheckGraph.removeAllEdges(conditionalEdges);
        List<String> badgeGoals = new ArrayList<>(List.of("E,14,0,0",
                                                          "E,12,1,0",
                                                          "E,0,7,2",
                                                          "E,24,42,0",
                                                          "E,15,0,0",
                                                          "E,11,3,0",
                                                          "E,3,3,0",
                                                          "E,10,0,0",
                                                          "E,4,1,0",
                                                          "E,8,1,0"));
        connectivityInspector = new ConnectivityInspector<>(badgeLockCheckGraph);
        for (String goal : badgeGoals) {
            Warp targetWarp = state.getWarps().get(goal);
            if (!connectivityInspector.pathExists(startingWarp, targetWarp)) {
                return false;
            }
        }

        // Juan / Norman isn't waterfall locked
        Graph<Warp, MapEdge> waterfallLockCheckGraph = copyGraph(mapGraph);
        conditionalEdges = waterfallLockCheckGraph.edgeSet()
                                                  .stream()
                                                  .filter(e -> e.getType() == MapEdge.Type.CONDITIONAL)
                                                  .filter(e -> e.getCondition().equals("HOENN_WATERFALL"))
                                                  .collect(Collectors.toSet());
        waterfallLockCheckGraph.removeAllEdges(conditionalEdges);
        List<String> waterfallGoals = new ArrayList<>(List.of("E,15,0,0", "E,8,1,0"));
        connectivityInspector = new ConnectivityInspector<>(waterfallLockCheckGraph);
        for (String goal : waterfallGoals) {
            Warp targetWarp = state.getWarps().get(goal);
            if (!connectivityInspector.pathExists(startingWarp, targetWarp)) {
                return false;
            }
        }

        // Mauville / rocksmash guy isn't rocksmash locked or strength locked or go-goggle locked
        Graph<Warp, MapEdge> rockSmashLockCheckGraph = copyGraph(mapGraph);
        conditionalEdges = rockSmashLockCheckGraph.edgeSet()
                                                  .stream()
                                                  .filter(e -> e.getType() == MapEdge.Type.CONDITIONAL)
                                                  .filter(e -> e.getCondition().equals("HOENN_STRENGTH"))
                                                  .filter(e -> e.getCondition().equals("HOENN_ROCK_SMASH"))
                                                  .filter(e -> e.getCondition().equals("GO_GOGGLES"))
                                                  .collect(Collectors.toSet());
        rockSmashLockCheckGraph.removeAllEdges(conditionalEdges);
        List<String> rockSmashGoals = new ArrayList<>(List.of("E,10,2,0", "E,10,0,0"));
        connectivityInspector = new ConnectivityInspector<>(rockSmashLockCheckGraph);
        for (String goal : rockSmashGoals) {
            Warp targetWarp = state.getWarps().get(goal);
            if (!connectivityInspector.pathExists(startingWarp, targetWarp)) {
                return false;
            }
        }

        // Flanery / rusturf tunnel isn't strength locked / go goggles
        Graph<Warp, MapEdge> strengthLockCheckGraph = copyGraph(mapGraph);
        conditionalEdges = strengthLockCheckGraph.edgeSet()
                                                 .stream()
                                                 .filter(e -> e.getType() == MapEdge.Type.CONDITIONAL)
                                                 .filter(e -> e.getCondition().equals("HOENN_STRENGTH"))
                                                 .filter(e -> e.getCondition().equals("GO_GOGGLES"))
                                                 .collect(Collectors.toSet());
        strengthLockCheckGraph.removeAllEdges(conditionalEdges);
        List<String> strengthGoals = new ArrayList<>(List.of("E,4,1,0", "E,24,4,0"));
        connectivityInspector = new ConnectivityInspector<>(strengthLockCheckGraph);
        for (String goal : strengthGoals) {
            Warp targetWarp = state.getWarps().get(goal);
            if (!connectivityInspector.pathExists(startingWarp, targetWarp)) {
                return false;
            }
        }

        // Norman isn't surf locked
        Graph<Warp, MapEdge> surfLockCheckGraph = copyGraph(mapGraph);
        conditionalEdges = surfLockCheckGraph.edgeSet()
                                             .stream()
                                             .filter(e -> e.getType() == MapEdge.Type.CONDITIONAL)
                                             .filter(e -> e.getCondition().equals("HOENN_SURF"))
                                             .collect(Collectors.toSet());
        strengthLockCheckGraph.removeAllEdges(conditionalEdges);
        List<String> surfGoals = new ArrayList<>(List.of("E,8,1,0"));
        connectivityInspector = new ConnectivityInspector<>(surfLockCheckGraph);
        for (String goal : surfGoals) {
            Warp targetWarp = state.getWarps().get(goal);
            if (!connectivityInspector.pathExists(startingWarp, targetWarp)) {
                return false;
            }
        }

        return true;
    }

    public static Graph<Warp, MapEdge> copyGraph(Graph<Warp, MapEdge> graph) {

        Graph<Warp, MapEdge> copiedGraph = new DefaultDirectedGraph<>(MapEdge.class);

        for (Warp vertex : graph.vertexSet()) {
            copiedGraph.addVertex(vertex);
        }

        for (MapEdge edge : graph.edgeSet()) {
            Warp source = graph.getEdgeSource(edge);
            Warp target = graph.getEdgeTarget(edge);
            MapEdge clonedEdge = (MapEdge) edge.clone();
            copiedGraph.addEdge(source, target, clonedEdge);
        }

        return copiedGraph;
    }


    private static Set<String> validationTriggerList() {
        return Set.of("0,0,1",
                      "0,0,2",
                      "0,0,3",
                      "0,0,5",
                      "0,1,0",
                      "0,1,1",
                      "0,1,2",
                      "0,1,4",
                      "0,1,5",
                      "0,1,7",
                      "0,1,8",
                      "0,2,0",
                      "0,2,1",
                      "0,2,2",
                      "0,2,3",
                      "0,2,4",
                      "0,3,0",
                      "0,3,1",
                      "0,3,2",
                      "0,3,3",
                      "0,3,5",
                      "0,3,6",
                      "0,3,8",
                      "0,3,10",
                      "0,4,0",
                      "0,4,2",
                      "0,4,3",
                      "0,5,0",
                      "0,5,1",
                      "0,5,2",
                      "0,5,4",
                      "0,5,6",
                      "0,5,12",
                      "0,6,1",
                      "0,6,2",
                      "0,6,4",
                      "0,6,6",
                      "0,6,8",
                      "0,7,0",
                      "0,7,1",
                      "0,7,2",
                      "0,7,3",
                      "0,7,4",
                      "0,8,0",
                      "0,8,1",
                      "0,8,2",
                      "0,8,3",
                      "0,10,2",
                      "0,11,1",
                      "0,11,2",
                      "0,12,0",
                      "0,12,1",
                      "0,12,2",
                      "0,12,3",
                      "0,12,5",
                      "0,13,0",
                      "0,13,2",
                      "0,14,1",
                      "0,14,2",
                      "0,14,4",
                      "0,15,0",
                      "0,19,0",
                      "0,19,2",
                      "0,19,3",
                      "0,19,4",
                      "0,19,5",
                      "0,19,6",
                      "0,19,7",
                      "0,20,0",
                      "0,21,0",
                      "0,23,0",
                      "0,25,0",
                      "0,25,2",
                      "0,25,3",
                      "0,25,4",
                      "0,25,5",
                      "0,26,1",
                      "0,26,3",
                      "0,27,0",
                      "0,27,1",
                      "0,27,2",
                      "0,27,3",
                      "0,27,4",
                      "0,27,5",
                      "0,29,0",
                      "0,29,1",
                      "0,30,0",
                      "0,31,0",
                      "0,31,2",
                      "0,34,0",
                      "0,35,0",
                      "0,37,0",
                      "0,46,0",
                      "2,0,0",
                      "2,1,0",
                      "2,2,0",
                      "2,3,0",
                      "2,4,0",
                      "3,0,0",
                      "3,1,0",
                      "3,2,0",
                      "3,3,0",
                      "3,4,0",
                      "3,5,0",
                      "4,0,0",
                      "4,1,0",
                      "4,3,0",
                      "4,4,0",
                      "4,5,0",
                      "4,5,3",
                      "4,6,0",
                      "5,0,0",
                      "5,1,0",
                      "5,4,0",
                      "5,5,0",
                      "5,6,0",
                      "5,7,0",
                      "6,0,0",
                      "6,3,0",
                      "6,4,0",
                      "6,5,0",
                      "6,6,0",
                      "6,7,0",
                      "6,8,0",
                      "7,0,0",
                      "7,1,0",
                      "7,2,0",
                      "7,3,0",
                      "7,4,0",
                      "7,5,0",
                      "7,6,0",
                      "8,0,0",
                      "8,1,0",
                      "8,2,0",
                      "8,3,0",
                      "8,4,0",
                      "8,5,0",
                      "8,6,0",
                      "9,0,0",
                      "9,0,2",
                      "9,1,0",
                      "9,2,0",
                      "9,5,0",
                      "9,6,0",
                      "9,7,0",
                      "9,7,1",
                      "9,8,0",
                      "9,9,0",
                      "9,10,0",
                      "9,11,0",
                      "9,12,0",
                      "9,13,0",
                      "10,0,0",
                      "10,1,0",
                      "10,2,0",
                      "10,3,0",
                      "10,4,0",
                      "10,5,0",
                      "10,6,0",
                      "10,7,0",
                      "11,0,0",
                      "11,0,1",
                      "11,0,2",
                      "11,1,0",
                      "11,1,1",
                      "11,2,0",
                      "11,3,0",
                      "11,4,0",
                      "11,5,0",
                      "11,6,0",
                      "11,7,0",
                      "11,8,0",
                      "11,9,0",
                      "11,10,0",
                      "11,11,0",
                      "11,12,0",
                      "11,13,0",
                      "11,13,2",
                      "11,14,0",
                      "11,15,0",
                      "11,16,0",
                      "12,0,0",
                      "12,1,0",
                      "12,2,0",
                      "12,3,0",
                      "12,4,0",
                      "12,5,0",
                      "12,6,0",
                      "12,7,0",
                      "12,8,0",
                      "12,9,0",
                      "13,0,0",
                      "13,1,0",
                      "13,2,0",
                      "13,2,1",
                      "13,4,0",
                      "13,4,2",
                      "13,4,3",
                      "13,5,0",
                      "13,5,1",
                      "13,6,0",
                      "13,7,0",
                      "13,9,1",
                      "13,10,0",
                      "13,11,0",
                      "13,12,0",
                      "13,13,0",
                      "13,14,0",
                      "13,15,0",
                      "13,16,0",
                      "13,16,2",
                      "13,17,0",
                      "13,17,1",
                      "13,18,0",
                      "13,18,1",
                      "13,19,0",
                      "13,19,1",
                      "13,20,0",
                      "13,21,0",
                      "14,0,0",
                      "14,1,0",
                      "14,2,0",
                      "14,3,0",
                      "14,4,0",
                      "14,5,0",
                      "14,6,0",
                      "14,7,0",
                      "14,8,1",
                      "14,9,0",
                      "14,10,0",
                      "14,11,0",
                      "15,0,0",
                      "15,2,0",
                      "15,3,0",
                      "15,4,0",
                      "15,5,0",
                      "15,6,0",
                      "15,7,0",
                      "15,8,0",
                      "15,9,0",
                      "15,10,0",
                      "15,11,0",
                      "15,12,0",
                      "15,13,0",
                      "16,5,0",
                      "16,5,1",
                      "16,6,0",
                      "16,6,1",
                      "16,7,0",
                      "16,7,1",
                      "16,8,0",
                      "16,8,1",
                      "16,9,0",
                      "16,9,1",
                      "16,10,0",
                      "16,12,0",
                      "16,13,0",
                      "16,14,0",
                      "17,0,0",
                      "17,1,0",
                      "18,0,0",
                      "18,1,0",
                      "19,0,0",
                      "19,0,1",
                      "19,1,0",
                      "19,1,1",
                      "20,0,0",
                      "20,1,0",
                      "20,2,0",
                      "21,0,0",
                      "22,0,0",
                      "23,0,2",
                      "24,0,0",
                      "24,0,1",
                      "24,0,2",
                      "24,0,3",
                      "24,0,4",
                      "24,0,5",
                      "24,1,0",
                      "24,1,1",
                      "24,1,2",
                      "24,1,3",
                      "24,2,0",
                      "24,2,1",
                      "24,2,2",
                      "24,2,4",
                      "24,2,5",
                      "24,3,0",
                      "24,4,0",
                      "24,4,1",
                      "24,6,0",
                      "24,6,1",
                      "24,6,2",
                      "24,7,0",
                      "24,7,1",
                      "24,7,2",
                      "24,7,3",
                      "24,8,0",
                      "24,8,1",
                      "24,8,2",
                      "24,8,3",
                      "24,8,4",
                      "24,8,5",
                      "24,9,0",
                      "24,9,1",
                      "24,9,2",
                      "24,9,3",
                      "24,9,4",
                      "24,10,0",
                      "24,11,0",
                      "24,11,1",
                      "24,11,2",
                      "24,11,3",
                      "24,11,4",
                      "24,11,5",
                      "24,12,0",
                      "24,12,1",
                      "24,12,2",
                      "24,12,3",
                      "24,13,0",
                      "24,13,1",
                      "24,13,2",
                      "24,13,3",
                      "24,13,4",
                      "24,14,0",
                      "24,14,1",
                      "24,15,0",
                      "24,15,1",
                      "24,15,4",
                      "24,15,5",
                      "24,16,0",
                      "24,16,1",
                      "24,16,2",
                      "24,17,0",
                      "24,17,1",
                      "24,17,2",
                      "24,18,0",
                      "24,18,1",
                      "24,18,2",
                      "24,18,3",
                      "24,19,0",
                      "24,19,1",
                      "24,19,2",
                      "24,20,0",
                      "24,21,0",
                      "24,21,1",
                      "24,22,1",
                      "24,23,0",
                      "24,23,2",
                      "24,24,0",
                      "24,24,1",
                      "24,24,2",
                      "24,24,3",
                      "24,24,5",
                      "24,24,6",
                      "24,24,7",
                      "24,24,8",
                      "24,24,9",
                      "24,24,10",
                      "24,24,11",
                      "24,24,12",
                      "24,24,13",
                      "24,24,15",
                      "24,24,16",
                      "24,24,17",
                      "24,24,18",
                      "24,24,19",
                      "24,24,20",
                      "24,24,22",
                      "24,24,24",
                      "24,24,50",
                      "24,24,51",
                      "24,24,52",
                      "24,24,53",
                      "24,25,0",
                      "24,25,1",
                      "24,25,2",
                      "24,25,3",
                      "24,25,4",
                      "24,25,5",
                      "24,25,7",
                      "24,25,8",
                      "24,27,1",
                      "24,27,50",
                      "24,28,2",
                      "24,29,0",
                      "24,29,1",
                      "24,29,3",
                      "24,30,0",
                      "24,30,1",
                      "24,31,0",
                      "24,32,0",
                      "24,32,1",
                      "24,32,2",
                      "24,33,0",
                      "24,33,1",
                      "24,34,0",
                      "24,34,1",
                      "24,35,1",
                      "24,36,0",
                      "24,37,0",
                      "24,38,0",
                      "24,38,1",
                      "24,39,1",
                      "24,43,0",
                      "24,43,1",
                      "24,43,2",
                      "24,43,3",
                      "24,43,4",
                      "24,44,0",
                      "24,44,1",
                      "24,44,2",
                      "24,44,3",
                      "24,44,4",
                      "24,44,5",
                      "24,44,6",
                      "24,45,0",
                      "24,45,1",
                      "24,45,2",
                      "24,45,3",
                      "24,46,0",
                      "24,52,0",
                      "24,53,0",
                      "24,54,0",
                      "24,54,2",
                      "24,54,3",
                      "24,54,4",
                      "24,55,1",
                      "24,55,2",
                      "24,55,4",
                      "24,55,5",
                      "24,55,7",
                      "24,55,8",
                      "24,55,9",
                      "24,55,10",
                      "24,55,11",
                      "24,56,0",
                      "24,56,2",
                      "24,56,3",
                      "24,56,4",
                      "24,57,0",
                      "24,57,1",
                      "24,57,6",
                      "24,57,7",
                      "24,58,0",
                      "24,58,1",
                      "24,58,2",
                      "24,59,0",
                      "24,59,2",
                      "24,61,0",
                      "24,62,0",
                      "24,62,2",
                      "24,63,0",
                      "24,67,0",
                      "24,67,1",
                      "24,67,2",
                      "24,68,0",
                      "24,68,1",
                      "24,68,2",
                      "24,73,0",
                      "24,77,0",
                      "24,77,1",
                      "24,78,0",
                      "24,78,1",
                      "24,79,0",
                      "24,79,2",
                      "24,80,0",
                      "24,80,1",
                      "24,81,0",
                      "24,81,1",
                      "24,81,2",
                      "24,82,0",
                      "24,82,1",
                      "24,82,2",
                      "24,84,0",
                      "24,84,1",
                      "24,85,0",
                      "24,86,0",
                      "24,86,1",
                      "24,86,2",
                      "24,86,3",
                      "24,87,0",
                      "24,87,1",
                      "24,87,2",
                      "24,88,0",
                      "24,89,0",
                      "24,89,2",
                      "24,90,0",
                      "24,91,0",
                      "24,91,1",
                      "24,92,0",
                      "24,92,1",
                      "24,93,0",
                      "24,93,1",
                      "24,94,0",
                      "24,94,1",
                      "24,95,0",
                      "24,95,1",
                      "24,96,0",
                      "24,97,0",
                      "24,102,0",
                      "24,104,1",
                      "24,107,0",
                      "26,56,0",
                      "26,56,1",
                      "26,59,0",
                      "26,60,0",
                      "26,66,1",
                      "26,67,0",
                      "26,68,0",
                      "26,68,1",
                      "26,69,0",
                      "26,69,1",
                      "26,70,1",
                      "26,71,0",
                      "26,73,1",
                      "26,74,1",
                      "26,75,0",
                      "26,76,0",
                      "26,86,0",
                      "28,0,0",
                      "29,0,0",
                      "29,2,0",
                      "29,3,2",
                      "29,11,0",
                      "29,11,2",
                      "29,12,0",
                      "29,12,2",
                      "30,0,0",
                      "31,0,0",
                      "32,0,0",
                      "32,0,2",
                      "32,1,0",
                      "32,2,0",
                      "33,0,0");
    }

}
