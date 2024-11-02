package com.dabomstew.pkrandom.warps;

import org.jgrapht.Graph;
import org.jgrapht.alg.shortestpath.FloydWarshallShortestPaths;
import org.jgrapht.traverse.BreadthFirstIterator;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class WarpRandomizationState {

    private static final String EXTRA_DEADEND_TAG = "extraDeadend";

    private static final String REMOVABLE_TAG = "removeable";

    private static final String NO_RETURN_TAG = "no_return";

    private static final String NEEDS_RETURN_TAG = "needs_return";

    private int attemptsLeft;

    private WarpData data;

    private WarpConfig config;

    private List<FlagCondition> flags;

    private List<List<String>> escapePaths;

    private Set<String> rootCandidates;

    private Random random;

    Graph<Warp, MapEdge> mapGraph;

    private Warp root;

    private Map<String, Warp> warps;

    private Set<MapEdge> remainingConditionalEdges = new HashSet<>();

    private Map<String, String> remainingKeyLocations;

    private Map<String, String> remainingFlagLocations;

    private Set<String> remainingMustLinkHomeWarps = new HashSet<>();

    private Set<String> markedFlagLocations = new HashSet<>();

    private List<Warp> homePaths = null;

    private Map<String, Integer> areaKeyLocationCount = new HashMap<>();

    private List<Set<Warp>> unconnectedComponents;

    private Warp oddOnOutWarp;

    private boolean moreWarpsToMap;

    private String progress;

    public WarpRandomizationState(WarpConfig config,  WarpData warpData) {

        this.data = warpData;

        this.attemptsLeft = 5;

        this.config = config;
        this.moreWarpsToMap = true;

        this.random = new Random(config.getSeed());

        if (this.config.isInGymOrder()) {
            flags = new ArrayList<>(warpData.getFlags().getCompositeFlags().values());
        } else {
            flags = new ArrayList<>(warpData.getFlags().getCompositeFlagsOutOfSeq().values());
        }

        this.escapePaths = new ArrayList<>(warpData.getEscapePaths().getPaths());

        this.rootCandidates = warpData.getKeyLocations().getRootCandidates();

        this.warps = warpData.getWarps()
                             .entrySet()
                             .stream()
                             .filter(e -> !e.getValue().getIgnore())
                             .filter(e -> e.getValue().getGroupMain() || isNullOrEmpty(e.getValue().getGrouped()))
                             .filter(e -> e.getValue().getLevel() <= config.getLevel())
                             .filter(e -> !(config.isExtraDeadendRemoval() && e.getValue().getTags().contains(EXTRA_DEADEND_TAG)))
                             .filter(e -> !e.getValue().getTags().contains(REMOVABLE_TAG))
                             .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        this.progress = "0/" + warps.size();

        if (config.isExtraDeadendRemoval()) {
            List<String> candidates = new ArrayList<>(warpData.getKeyLocations().getOddOnOutWithDeadendsRemovedWarps());
            Collections.sort(candidates);
            oddOnOutWarp = warpData.getWarps().get(candidates.get(random.nextInt(candidates.size())));
        } else {
            List<String> candidates = new ArrayList<>(warpData.getKeyLocations().getOddOnOutWarps());
            Collections.sort(candidates);
            oddOnOutWarp = warpData.getWarps().get(candidates.get(random.nextInt(candidates.size())));
        }

        this.remainingKeyLocations = warpData.getKeyLocations().getKeyLocations();
        this.remainingFlagLocations = warpData.getKeyLocations().getLocationsTrigger();
    }

    public static boolean isNullOrEmpty( final Collection< ? > c ) {
        return c == null || c.isEmpty();
    }

    public int getAttemptsLeft() {
        return attemptsLeft;
    }

    public WarpConfig getConfig() {
        return config;
    }

    public List<FlagCondition> getFlags() {
        return flags;
    }

    public List<List<String>> getEscapePaths() {
        return escapePaths;
    }

    public Random getRandom() {
        return random;
    }

    public Map<String, Warp> getWarps() {
        return warps;
    }

    public Set<MapEdge> getRemainingConditionalEdges() {
        return remainingConditionalEdges;
    }

    public void addRemainingConditionalEdge(MapEdge mapEdge) {
        remainingConditionalEdges.add(mapEdge);
    }

    public void setUnconnectedComponents(List<Set<Warp>> unconnectedComponents) {
        this.unconnectedComponents = unconnectedComponents;
    }

    public List<Set<Warp>> getUnconnectedComponents() {
        return unconnectedComponents;
    }

    public boolean isMoreWarpsToMap() {
        return moreWarpsToMap;
    }

    public String getProgress() {
        return progress;
    }

    public WarpData getData() {
        return data;
    }

    public Graph<Warp, MapEdge> getMapGraph() {
        return mapGraph;
    }

    public void setMapGraph(Graph<Warp, MapEdge> mapGraph) {
        this.mapGraph = mapGraph;
    }

    public void selectEscapePathWarps() {
        remainingMustLinkHomeWarps = new HashSet<>();
        for (List<String> innerList : escapePaths) {
            if (!innerList.isEmpty()) {
                int randomIndex = random.nextInt(innerList.size());
                remainingMustLinkHomeWarps.add(innerList.get(randomIndex));
            }
        }
    }

    public void selectRoot() {
        List<String> candidatesList = new ArrayList<>(rootCandidates);
        int randomIndex = random.nextInt(candidatesList.size());
        this.root = warps.get(candidatesList.get(randomIndex));

        unconnectedComponents.removeIf(component -> component.contains(this.root));
    }

    public Warp getOddOnOutWarp() {
        return oddOnOutWarp;
    }

    public void moveFlagLocationToKeyIfFinalForLevel() {

        final String location;

        switch (config.getLevel())
        {
            case 1: location = "L_RUSTBORO_CITY_GYM"; break;
            case 2: location = "L_DEWFORD_TOWN_GYM"; break;
            case 3: location = "L_MAUVILLE_CITY_GYM"; break;
            case 4: location = "L_LAVARIDGE_TOWN_GYM"; break;
            case 5: location = "L_PETALBURG_GYM"; break;
            case 6: location = "L_FORTREE_CITY_GYM"; break;
            case 7: location = "L_MOSSDEEP_CITY_GYM"; break;
            case 8: location = "L_SOOTOPOLIS_CITY_GYM"; break;
            case 9:
            case 0:
            case 10:
            default:
                // E4 and upwards are location locations not progression anyway
                return;
        }

        remainingKeyLocations.entrySet()
                             .stream()
                             .filter(e -> e.getValue().equals(location))
                             .findFirst().map(Map.Entry::getKey)
                             .ifPresent(key -> remainingKeyLocations.put(key, remainingFlagLocations.remove(key)));

    }

    public void setMoreWarpsToMap(boolean moreWarpsToMap) {
        this.moreWarpsToMap = moreWarpsToMap;
    }

    public List<WarpRemapping> getRemappings() {

        List<WarpRemapping> remappings = new ArrayList<>();

        List<MapEdge> warpEdges = mapGraph.edgeSet()
                                          .stream()
                                          .filter(e -> e.getType() == MapEdge.Type.WARP)
                                          .collect(Collectors.toList());

        for (MapEdge warpEdge : warpEdges)
        {
            String[] targetParts = warpEdge.getTarget().split(",");

            Warp warp = data.getWarps().get(warpEdge.getSource());

            List<String> warpGroup = new ArrayList<>();
            warpGroup.add(warp.getId());
            warpGroup.addAll(warp.getGrouped());

            Set<String> triggers = warpGroup.stream()
                                            .map(w -> data.getWarps().get(w).getTo())
                                            .collect(Collectors.toSet());

            for (String trigger : triggers)
            {
                String[] triggerParts = trigger.split(",");
                remappings.add(new WarpRemapping(Integer.parseInt(triggerParts[1]),
                                                 Integer.parseInt(triggerParts[2]),
                                                 Integer.parseInt(triggerParts[3]),
                                                 Integer.parseInt(targetParts[1]),
                                                 Integer.parseInt(targetParts[2]),
                                                 Integer.parseInt(targetParts[3])));

            }
        }

        return remappings;
    }

    public Map<String, String> getRemainingKeyLocations() {
        return remainingKeyLocations;
    }

    public Map<String, String> getRemainingFlagLocations() {
        return remainingFlagLocations;
    }

    public Set<String> getRemainingMustLinkHomeWarps() {
        return remainingMustLinkHomeWarps;
    }

    public boolean hasHomePaths() {
        return homePaths != null;
    }

    public List<Warp> getHomePaths() {
        return homePaths;
    }

    public Warp getRandomUnmappedWarpForWarp(Warp source, List<Warp> candidates) throws ImpossibleMapException {

        /*
         *  In team aquas hideout you have maps like this
         *  ----------------------
         *  | W1      W2      W3 |
         *  ----------------------
         * To access W1 from W3 you need to use W2, then return from the other side.
         * So we need to make sure W2 never links to a drop warp or locked door
         */
        boolean sourceNeedsReturn = source.getTags().contains(NEEDS_RETURN_TAG);
        boolean sourceIsNoReturn = source.getTags().contains(NO_RETURN_TAG);


        Stream<Warp> candidateStream = candidates.stream()
                                                 .filter(w -> !w.isMapped())
                                                 .filter(w -> !w.getId().equals(source.getId()));

        if (sourceNeedsReturn) {
            candidateStream = candidateStream.filter(w -> !w.getTags().contains(NO_RETURN_TAG));
        } else if (sourceIsNoReturn) {
            candidateStream = candidateStream.filter(w -> !w.getTags().contains(NEEDS_RETURN_TAG));
        }

        List<Warp> candidatesList = candidateStream.sorted(Comparator.comparing(Warp::getId)).collect(Collectors.toList());

        if (candidatesList.isEmpty()) {
            throw new ImpossibleMapException("Failed to find a matching warp for " + source.getId());
        }

        return candidatesList.get(random.nextInt(candidatesList.size()));
    }

    public void updateConnections() {

        Set<String> accessibleNodesIds = new HashSet<>();
        BreadthFirstIterator<Warp, MapEdge> iterator = new BreadthFirstIterator<>(mapGraph, root);
        while (iterator.hasNext()) {
            try {
                accessibleNodesIds.add(iterator.next().getId());
            } catch (NullPointerException e) {
                /* Semms like the jgrapht has some bugs for graphs that aren't well connected */
            }

        }

        for (String nodeId : accessibleNodesIds) {

            if (remainingKeyLocations.containsKey(nodeId)) {
                markedFlagLocations.add(remainingKeyLocations.get(nodeId));
            }
            if (remainingFlagLocations.containsKey(nodeId)) {
                markedFlagLocations.add(remainingFlagLocations.get(nodeId));
            }

            remainingKeyLocations.remove(nodeId);
            remainingFlagLocations.remove(nodeId);
        }

        for (FlagCondition flag : flags.stream().filter(f -> !f.isSet()).collect(Collectors.toList()))
        {
            if (markedFlagLocations.containsAll(flag.getCondition())) {
                flag.setFlag();
            }
        }

        Set<String> setFlags = flags.stream()
                                    .filter(FlagCondition::isSet)
                                    .map(FlagCondition::getFlag)
                                    .collect(Collectors.toSet());

        Set<MapEdge> newEdgesToAdd = new HashSet<>();
        for (MapEdge edge: remainingConditionalEdges)
        {
           if (setFlags.contains(edge.getCondition()))
           {
               newEdgesToAdd.add(edge);
               Warp v1 = data.getWarps().get(edge.getSource());
               Warp v2 = data.getWarps().get(edge.getTarget());

               if (mapGraph.vertexSet().contains(v1) && mapGraph.vertexSet().contains(v2))
               {
                   mapGraph.addEdge(v1, v2, edge);
               }

           }
        }
        remainingConditionalEdges.removeAll(newEdgesToAdd);


        long unmappedRemainingCount = mapGraph.vertexSet().stream().filter(w -> !w.isMapped()).count();
        long totalWarps = mapGraph.vertexSet().size();

        if (unmappedRemainingCount == 0) {
            setMoreWarpsToMap(false);
        }

        progress = (totalWarps - unmappedRemainingCount) + "/" + totalWarps;
    }

    public Warp getRoot() {
        return root;
    }

    public void clearMustLinkHomeWarps() {
        this.remainingMustLinkHomeWarps = new HashSet<>();
    }

    public void removeComponentWithWarp(Warp targetWarp) {

        unconnectedComponents = unconnectedComponents.stream()
                                                     .filter(u -> !u.contains(targetWarp))
                                                     .collect(Collectors.toList());

    }

    public void generateHomePaths(List<Warp> reachableNodes) {
        FloydWarshallShortestPaths<Warp, MapEdge> fw = new FloydWarshallShortestPaths<>(mapGraph);
        List<Warp> homeEscapesList = reachableNodes.stream()
                                                   .filter(node -> fw.getPathWeight(root, node) != Double.POSITIVE_INFINITY)
                                                   .collect(Collectors.toList());
        homeEscapesList.forEach(node -> remainingMustLinkHomeWarps.remove(node.getId()));

        homePaths = homeEscapesList;
    }

    /**
     * To artificially improve distribution of goals we're going to keep track of where goal are
     * placed and try and force it to choose better spread locations if they're available
     */
    public Warp getWellSpreadNode(List<Warp> reachableNodes) {

        List<Warp> preferredNodes = reachableNodes;

        List<String> lowPopulatedAreas = areaKeyLocationCount.entrySet()
                                                             .stream()
                                                             .filter(e -> e.getValue() >= 1)
                                                             .map(Map.Entry::getKey)
                                                             .collect(Collectors.toList());
        List<Warp> firstPreference = reachableNodes.stream()
                                                   .filter(w -> !lowPopulatedAreas.contains(getAreaKey(w)))
                                                   .collect(Collectors.toList());

        if (!firstPreference.isEmpty()) {
            preferredNodes = firstPreference;
        } else {
            List<String> midPopulatedAreas = areaKeyLocationCount.entrySet()
                    .stream()
                    .filter(e -> e.getValue() >= 2)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
            List<Warp> secondPreference = reachableNodes.stream()
                    .filter(w -> !midPopulatedAreas.contains(getAreaKey(w)))
                    .collect(Collectors.toList());

            if (!secondPreference.isEmpty()) {
                preferredNodes = secondPreference;
            } else {
                List<String> wellPopulatedAreas = areaKeyLocationCount.entrySet()
                        .stream()
                        .filter(e -> e.getValue() >= 3)
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toList());
                List<Warp> thirdPreference = reachableNodes.stream()
                        .filter(w -> !wellPopulatedAreas.contains(getAreaKey(w)))
                        .collect(Collectors.toList());

                if (!thirdPreference.isEmpty()) {
                    preferredNodes = thirdPreference;
                }
            }
        }

        preferredNodes.sort(Comparator.comparing(Warp::getId));
        Warp result = preferredNodes.get(random.nextInt(preferredNodes.size()));
        String areaKey = getAreaKey(result);
        Integer newCount = areaKeyLocationCount.getOrDefault(areaKey, 0) + 1;
        areaKeyLocationCount.put(areaKey, newCount);

        return result;
    }

    private String getAreaKey(Warp warp) {
        String[] warpIdParts = warp.getId().split(",");
        return warpIdParts[1] + "," + warpIdParts[2];
    }
}
