package com.dabomstew.pkrandom.pokemon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TypeInteractions {

    private final Type attackerType;

    private Map<Type, Float> defenderMultiplier;

    private Type originalType = null;

    public TypeInteractions(Type attackerType) {
        this.attackerType = attackerType;
        defenderMultiplier = new HashMap<>();
    }

    public void setDefenderMultiplier(Type defender, float multiplier) {
        defenderMultiplier.put(defender, multiplier);
    }

    public Type getAttackerType() {
        return attackerType;
    }

    public Float getDefenderMultiplier(Type defenderType) {
        return defenderMultiplier.getOrDefault(defenderType, 1f);
    }

    public Map<Type, Float> getDefenderMultiplier() {
        return defenderMultiplier;
    }

    public void setDefenderMultiplier(Map<Type, Float> defenderMultiplier) {
        this.defenderMultiplier = defenderMultiplier;
    }

    public void setOriginalType(Type originalType) {
        this.originalType = originalType;
    }

    public Type getOriginalType() {
        return originalType == null ? Type.NONE : originalType;
    }

    @Override
    public String toString() {
        List<String> result = new ArrayList<>();
        result.add(attackerType.displayName());

        Stream<Float> alteredDefenders = defenderMultiplier.entrySet()
                                                           .stream()
//                                                           .filter(e -> e.getKey() != Type.MYSTERY)
//                                                           .filter(e -> e.getKey() != Type.NONE)
//                                                           .filter(e -> e.getKey() != Type.STELLAR)
                                                           .map(Map.Entry::getValue);

        result.addAll(alteredDefenders.map(String::valueOf).collect(Collectors.toList()));
        return result.stream().map(t -> "|" + String.format("%-" + 8 + "s", t)).collect(Collectors.joining(""));
    }
}
