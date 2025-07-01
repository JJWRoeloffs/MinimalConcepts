package edu.vuamsterdam.MinimalConcepts;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;

import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
public class TreeEquivalenceTest {
    private final boolean starModule;

    private OWLOntologyManager manager;
    private OWLOntology ontology;

    public TreeEquivalenceTest(boolean starModule) {
        this.starModule = starModule;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {true},
                {false},
        });
    }

    @Before
    public void setUp() throws Exception {
        manager = OWLManager.createOWLOntologyManager();
        ontology = manager.loadOntologyFromOntologyDocument(Resources.getResource("example.owl"));
    }

    @Test
    public void testExampleMinimizerSimple() {
        OWLDataFactory factory = manager.getOWLDataFactory();
        OWLClass A1 = OWLHelpers.getClassByShortName(ontology, "A1");
        OWLClass A3 = OWLHelpers.getClassByShortName(ontology, "A3");
        OWLClassExpression example = factory.getOWLObjectUnionOf(A1, A3);

        TreeEquivalenceMinimalConcept minimalConceptGenerator = new TreeEquivalenceMinimalConcept(ontology, starModule);
        assertEquals(A3, minimalConceptGenerator.getMinimalConcept(example).orElseThrow());
    }

    @Test
    public void testExampleMinimizerComplex() {
        OWLDataFactory factory = manager.getOWLDataFactory();
        OWLClass A1 = OWLHelpers.getClassByShortName(ontology, "A1");
        OWLClass A2 = OWLHelpers.getClassByShortName(ontology, "A2");
        OWLClass A3 = OWLHelpers.getClassByShortName(ontology, "A3");
        OWLClass A4 = OWLHelpers.getClassByShortName(ontology, "A4");

        OWLObjectProperty r = OWLHelpers.getPropertyByShortName(ontology, "r");
        OWLObjectProperty s = OWLHelpers.getPropertyByShortName(ontology, "s");

        OWLClassExpression example = factory.getOWLObjectIntersectionOf(A2, factory.getOWLObjectSomeValuesFrom(r,
                factory.getOWLObjectIntersectionOf(A4, factory.getOWLObjectSomeValuesFrom(s, A3))));
        OWLClassExpression expected = factory.getOWLObjectIntersectionOf(A1, A2);

        TreeEquivalenceMinimalConcept minimalConceptGenerator = new TreeEquivalenceMinimalConcept(ontology, starModule);
        assertEquals(expected, minimalConceptGenerator.getMinimalConcept(example).orElseThrow());
    }
}
