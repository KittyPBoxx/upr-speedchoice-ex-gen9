package com.dabomstew.pkrandom.warps;

import org.jgrapht.graph.DefaultEdge;

public class MapEdge extends DefaultEdge {

    private String id;

    private String source;

    private String target;

    private String condition;

    private Type type;

    protected MapEdge(String id, String source, String target, String condition) {
        this.id = id;
        this.source = source;
        this.target = target;
        this.condition = condition;
        this.type = Type.CONDITIONAL;
    }

    protected MapEdge(String id, String source, String target, Type type) {
        this.id = id;
        this.source = source;
        this.target = target;
        this.condition = null;
        this.type = type;
    }

    public static MapEdge fixedEdge(String source, String target) {
        return new MapEdge(source + "-"  + target, source, target, Type.FIXED);
    }

    public static MapEdge conditionalEdge(String source, String target, String condition) {
        return new MapEdge(source + "-"  + target, source, target, condition);
    }

    public static MapEdge warpEdge(String source, String target) {
        return new MapEdge(source + "-"  + target, source, target, Type.WARP);
    }


    public enum Type {

        FIXED, // player can always walk between these points
        CONDITIONAL, // player can sometimes walk between these points (e.g they might need surf)
        WARP // the two nodes are connected by warping

    }

    public String getId() {
        return id;
    }

    @Override
    public String getSource() {
        return source;
    }

    @Override
    public String getTarget() {
        return target;
    }

    public String getCondition() {
        return condition;
    }

    public Type getType() {
        return type;
    }

    @Override
    public boolean equals(Object obj) {

        if (obj == null)
            return false;

        if (!(obj instanceof MapEdge))
            return false;

        return id.equals(((MapEdge) obj).getId());
    }
}
