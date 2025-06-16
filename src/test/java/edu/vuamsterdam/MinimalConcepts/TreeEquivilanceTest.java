package edu.vuamsterdam.MinimalConcepts;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;

public class TreeEquivilanceTest {
    private OWLOntologyManager manager;
    private OWLOntology ontology;

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

        TreeEquivilanceMinimalConcept minimalConceptGenerator = new TreeEquivilanceMinimalConcept(ontology);
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

        TreeEquivilanceMinimalConcept minimalConceptGenerator = new TreeEquivilanceMinimalConcept(ontology);
        assertEquals(expected, minimalConceptGenerator.getMinimalConcept(example).orElseThrow());
    }
}
