package edu.vuamsterdam.MinimalConcepts;

import static org.junit.Assert.*;

import java.util.*;

import org.junit.Test;

public class HelpersTest {
    @Test
    public void testReplace() {
        ArrayList<String> test = new ArrayList<>();
        Collections.addAll(test, "A1", "A2", "A3", "A4");
        List<String> gotten = Helpers.replaceElement(test, 2, "Hello");
        assertEquals("A1", gotten.get(0));
        assertEquals("A2", gotten.get(1));
        assertEquals("Hello", gotten.get(2));
        assertEquals("A4", gotten.get(3));

        assertEquals("A3", test.get(2));
    }

    @Test
    public void testPowerSetTwo() {
        HashSet<String> test = new HashSet<>();
        Collections.addAll(test, "A1", "A2");
        HashSet<String> oneSet = new HashSet<>();
        oneSet.add("A1");
        HashSet<String> twoSet = new HashSet<>();
        twoSet.add("A2");

        Set<Set<String>> gotten = Helpers.powerSet(test);

        assertTrue(gotten.contains(test));
        assertTrue(gotten.contains(oneSet));
        assertTrue(gotten.contains(twoSet));
        assertTrue(gotten.contains(new HashSet<String>()));
        assertEquals(4, gotten.size());
    }

    @Test
    public void testPowerSetThree() {
        HashSet<String> test = new HashSet<>();
        Collections.addAll(test, "A1", "A2", "A3");

        Set<Set<String>> gotten = Helpers.powerSet(test);

        assertEquals(8, gotten.size());
    }
}
