package edu.vuamsterdam.MinimalConcepts;

import static org.junit.Assert.*;

import java.util.*;
import java.util.stream.*;

import org.junit.Before;
import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;

public class SanityTest {
    private OWLOntologyManager manager;
    private OWLOntology ontology;

    @Before
    public void setUp() throws Exception {
        manager = OWLManager.createOWLOntologyManager();
        ontology = manager.loadOntologyFromOntologyDocument(Resources.getResource("example.owl"));
    }

    @Test
    public void testClassNames() {
        Set<String> expectedNames = new HashSet<>();
        Collections.addAll(expectedNames, "A1", "A2", "A3", "A4");
        Set<String> actualNames = ontology.classesInSignature()
                .map(cls -> cls.getIRI().getShortForm())
                .collect(Collectors.toSet());

        assertEquals(expectedNames, actualNames);
    }

    @Test
    public void testRelationNames() {
        Set<String> expectedNames = new HashSet<>();
        Collections.addAll(expectedNames, "r", "s");
        Set<String> actualNames = ontology.objectPropertiesInSignature()
                .map(cls -> cls.getIRI().getShortForm())
                .collect(Collectors.toSet());

        assertEquals(expectedNames, actualNames);
    }
}
