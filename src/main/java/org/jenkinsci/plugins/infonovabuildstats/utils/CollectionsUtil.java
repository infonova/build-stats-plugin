package org.jenkinsci.plugins.infonovabuildstats.utils;

import java.util.*;

/**
 * Simple helper class for collection operations
 * 
 */
public class CollectionsUtil {

    public static <T> List<T> minus(List<T> initialList, List<T> elementsToRemove) {
        List<T> minusedList = new ArrayList<>(initialList);
        minusedList.removeAll(elementsToRemove);
        return minusedList;
    }

    public static <T, U> void mapMergeAdd(Map<T, List<U>> map, Map<T, List<U>> mapToAdd) {
        for (Map.Entry<T, List<U>> e : mapToAdd.entrySet()) {
            if (!map.containsKey(e.getKey())) {
                map.put(e.getKey(), new ArrayList<>());
            }
            map.get(e.getKey()).addAll(e.getValue());
        }
    }

    public static <T> Set<T> toSet(List<T> list) {
        return new HashSet<>(list);
    }

}
