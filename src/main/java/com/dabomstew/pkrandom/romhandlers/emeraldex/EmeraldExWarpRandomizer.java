package com.dabomstew.pkrandom.romhandlers.emeraldex;

import com.dabomstew.pkrandom.gui.WarpRandoTest;
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
import java.util.function.Consumer;
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
                System.out.println("Warps " + state.getProgress());

            } catch (ImpossibleMapException e) {

                // re-init the state with a new seed and try again
                WarpConfig config = state.getConfig();
                System.out.printf("Randomization failed for seed %s. Tying a new one.%n", config.getSeed());
                config.incrementSeed();
                state = createState(config, loadRandomWarpData());
            }

        }

        List<WarpRemapping> remappings = state.getRemappings();
        Collections.sort(remappings);
        return remappings;
    }


    public static void doNextMapping(WarpRandomizationState state) throws ImpossibleMapException {

        List<Warp> reachableNodes = findUnmappedReachableNodes(state);

        List<Warp> unreachableNodes = state.getMapGraph().vertexSet().stream().filter(w -> !w.isMapped()).collect(Collectors.toList());
        unreachableNodes.removeAll(reachableNodes);

        List<Warp> unreachableFlagLocations = unreachableNodes.stream()
                .filter(w -> state.getRemainingFlagLocations().containsKey(w.getId())).collect(Collectors.toList());

        List<Warp> unreachableKeyLocations = unreachableNodes.stream()
                .filter(w -> state.getRemainingKeyLocations().containsKey(w.getId())).collect(Collectors.toList());

        List<Warp> unreachableHubs = unreachableNodes.stream()
                .filter(w -> state.getMapGraph().degreeOf(w) > 1)
                .collect(Collectors.toList());

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

        WarpPair warpPair = new WarpPair(null, null);

        if (!state.getUnconnectedComponents().isEmpty()) {

            System.out.println("Mapping MAIN HUB");
            warpPair = getUnconnectedComponentMapping(state, reachableNodes);

        } else if (!state.getRemainingMustLinkHomeWarps().isEmpty() && !reachableNodes.isEmpty()) {

            System.out.println("Mapping HOME LINK");
            warpPair = getHomeLinkMapping(state, reachableNodes);

        } else if (!unreachableFlagLocations.isEmpty()) {

            System.out.println("Mapping FLAG LOCATION");
            Warp sourceWarp = reachableNodes.get(state.getRandom().nextInt(reachableNodes.size()));
            reachableNodes.remove(sourceWarp);
            Warp targetWarp = state.getRandomUnmappedWarpForWarp(sourceWarp, unreachableFlagLocations);
            warpPair = new WarpPair(sourceWarp, targetWarp);

        } else if (!unreachableHubs.isEmpty()) {

            System.out.println("Mapping OTHER HUBS");
            Warp sourceWarp = reachableNodes.get(state.getRandom().nextInt(reachableNodes.size()));
            reachableNodes.remove(sourceWarp);
            List<Warp> targetCandidates = unreachableNodes.stream()
                    .filter(w -> state.getMapGraph().degreeOf(w) > 1)
                    .collect(Collectors.toList());
            Warp targetWarp = state.getRandomUnmappedWarpForWarp(sourceWarp, targetCandidates);
            warpPair = new WarpPair(sourceWarp, targetWarp);

        } else if (!unreachableKeyLocations.isEmpty()) {

            System.out.println("Mapping KEY LOCATIONS");
            Warp sourceWarp = reachableNodes.get(state.getRandom().nextInt(reachableNodes.size()));
            reachableNodes.remove(sourceWarp);
            Warp targetWarp = state.getRandomUnmappedWarpForWarp(sourceWarp, unreachableKeyLocations);
            warpPair = new WarpPair(sourceWarp, targetWarp);

        } else if (!unreachableNodes.isEmpty()) {

            System.out.println("Mapping DEAD ENDS");
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

            System.out.println("Mapping LEFT OVER CONNECTIONS");
            Warp sourceWarp = reachableNodes.get(state.getRandom().nextInt(reachableNodes.size()));
            reachableNodes.remove(sourceWarp);
            Warp targetWarp = state.getRandomUnmappedWarpForWarp(sourceWarp, reachableNodes);
            warpPair = new WarpPair(sourceWarp, targetWarp);

        } else {

            System.out.println("Mapping FINAL UNEVEN LEFTOVER CONNECTION");
            Warp sourceWarp = reachableNodes.get(state.getRandom().nextInt(reachableNodes.size()));
            reachableNodes.remove(sourceWarp);

            Warp targetWarp = state.getOddOnOutWarp();
            state.getMapGraph().addVertex(targetWarp);
            warpPair = new WarpPair(sourceWarp, targetWarp);
        }

        if (warpPair.getSourceWarp() == null) {
            // Target should never be null,
            // If the source is null it's not really ideal, but we can do our best

            if (!reachableNodes.isEmpty()) {
                System.out.println("Some reachable nodes could not be randomized");
            } else {
                System.out.println("All reachable nodes randomized");
            }

            if (!unreachableNodes.isEmpty()) {
                System.out.println("Some nodes are unreachable");
                warpPair.getTargetWarp().setMapped();
                state.setMoreWarpsToMap(false);
                return;
            } else {
                System.out.println("No nodes left that can't be reached");
            }
        }

        if (warpPair.getSourceWarp().isMapped() || warpPair.getTargetWarp().isMapped()) {
            throw new ImpossibleMapException("Trying to map a warp that is already mapped...");
        }

        MapEdge mapEdge = MapEdge.warpEdge(warpPair.getSourceWarp().getId(), warpPair.getTargetWarp().getId());
        state.getMapGraph().addEdge(warpPair.getSourceWarp(), warpPair.getTargetWarp(), mapEdge);

        if (!warpPair.getSourceWarp().getId().equals(warpPair.getTargetWarp().getId())) {
            MapEdge oppositeDirectionMapEdge = MapEdge.warpEdge(warpPair.getTargetWarp().getId(), warpPair.getSourceWarp().getId());
            state.getMapGraph().addEdge(warpPair.getTargetWarp(), warpPair.getSourceWarp(), oppositeDirectionMapEdge);
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
                /* Semms like the jgrapht has some bugs for graphs that aren't well connected */
            }
        }

        return unmappedNodes;
    }



}
