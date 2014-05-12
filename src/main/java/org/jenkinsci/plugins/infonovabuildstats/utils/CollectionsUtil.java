package org.jenkinsci.plugins.infonovabuildstats.utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class CollectionsUtil {

    public static <T> List<T> minus(List<T> initialList, List<T> elementsToRemove) {
        List<T> minusedList = new ArrayList<T>(initialList);
        minusedList.removeAll(elementsToRemove);
        return minusedList;
    }

    public static <T, U> void mapMergeAdd(Map<T, List<U>> map, Map<T, List<U>> mapToAdd) {
        for (Map.Entry<T, List<U>> e : mapToAdd.entrySet()) {
            if (!map.containsKey(e.getKey())) {
                map.put(e.getKey(), new ArrayList<U>());
            }
            map.get(e.getKey()).addAll(e.getValue());
        }
    }

    public static <T> Set<T> toSet(List<T> list) {
        Set<T> set = new HashSet<T>();
        for (T t : list) {
            if (!set.contains(t)) {
                set.add(t);
            }
        }
        return set;
    }

}
