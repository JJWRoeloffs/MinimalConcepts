package edu.vuamsterdam.MinimalConcepts;

import java.util.*;

import org.semanticweb.owlapi.model.*;

public class Helpers {
    // This is why I'd do it in scala.
    // I could also add vavr to the dependencies, but I could also, you know, not.
    public static <T> List<T> replaceElement(List<T> original, int i, T target) {
        List<T> ret = new ArrayList<>(original);
        ret.set(i, target);
        return ret;
    }

    // Another one that is in many standard libraries,
    // Instead, copied from StackOverflow.
    public static <T> Set<Set<T>> powerSet(Set<T> originalSet) {
        Set<Set<T>> sets = new HashSet<>();
        if (originalSet.isEmpty()) {
            sets.add(new HashSet<>());
            return sets;
        }
        List<T> list = new ArrayList<>(originalSet);
        T head = list.get(0);
        Set<T> rest = new HashSet<>(list.subList(1, list.size()));
        for (Set<T> set : powerSet(rest)) {
            Set<T> newSet = new HashSet<>();
            newSet.add(head);
            newSet.addAll(set);
            sets.add(newSet);
            sets.add(set);
        }
        return sets;
    }

    public static <T> Set<Set<T>> powerSetOfSize(Set<T> originalSet, int n) {
        Set<Set<T>> res = new HashSet<>();
        generateSubsets(new ArrayList<>(originalSet), 0, n, new HashSet<>(), res);
        return res;
    }

    private static <T> void generateSubsets(List<T> list, int idx, int n, Set<T> current, Set<Set<T>> res) {
        if (current.size() == n) {
            res.add(new HashSet<>(current));
            return;
        }
        if (idx == list.size()) {
            return;
        }
        current.add(list.get(idx));
        generateSubsets(list, idx + 1, n, current, res);

        current.remove(list.get(idx));
        generateSubsets(list, idx + 1, n, current, res);
    }
}
