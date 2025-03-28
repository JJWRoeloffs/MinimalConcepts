package edu.vuamsterdam.MinimalConcepts;

import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.*;
import org.semanticweb.HermiT.ReasonerFactory;

import java.util.*;
import java.util.stream.*;

public class AllConceptsMinimalConcept implements MinimalConcept {
    private final OWLOntology ontology;
    private final OWLDataFactory factory;
    private final OWLReasoner reasoner;

    public AllConceptsMinimalConcept(OWLOntology ontology) {
        this.ontology = ontology;
        this.factory = ontology.getOWLOntologyManager().getOWLDataFactory();

        OWLReasonerFactory reasonerFactory = new ReasonerFactory();
        this.reasoner = reasonerFactory.createReasoner(ontology);
        reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);
    }

    @Override
    public Optional<OWLClassExpression> getMinimalConcept(OWLClassExpression base) {
        return getAllConcepts(ontology, base.accept(new ClassExpressionSizeVisitor())-1).stream()
                .sorted(Comparator.comparingInt(concept -> concept.accept(new ClassExpressionSizeVisitor())))
                .filter(concept -> reasoner.isEntailed(factory.getOWLEquivalentClassesAxiom(base, concept)))
                .findFirst();
    }

    // If needed, this could be faster with a streaming interface, where new concepts aren't generated unless
    // getMinimalConcept asks for it, removing the need of generating larger concepts if a smaller one is already the minimum.
    public static Set<OWLClassExpression> getAllConcepts(OWLOntology ontology, int maxSize) {
        OWLDataFactory factory = ontology.getOWLOntologyManager().getOWLDataFactory();

        Set<OWLClassExpression> classes = ontology.classesInSignature().collect(Collectors.toSet());
        Set<OWLObjectProperty> properties = ontology.objectPropertiesInSignature().collect(Collectors.toSet());

        // Some ugly for loops that kinda work I guess.
        for (int idx = 0; idx < maxSize; idx++) {
            HashSet<OWLClassExpression> newClasses = new HashSet<>(classes);
            for (OWLObjectProperty prop : properties) {
                for (OWLClassExpression cls : classes) {
                    if (cls.accept(new ClassExpressionSizeVisitor()) < maxSize) {
                        newClasses.add(factory.getOWLObjectSomeValuesFrom(prop, cls));
                        newClasses.add(factory.getOWLObjectAllValuesFrom(prop, cls));
                    }
                }
            }
            List<OWLClassExpression> conceptList = new ArrayList<>(classes);
            for (int i = 0; i < conceptList.size(); i++) {
                for (int j = i + 1; j < conceptList.size(); j++) {
                    OWLClassExpression lhs = conceptList.get(i);
                    OWLClassExpression rhs = conceptList.get(j);
                    if (lhs.accept(new ClassExpressionSizeVisitor()) + rhs.accept(new ClassExpressionSizeVisitor()) <= maxSize) {
                        newClasses.add(factory.getOWLObjectIntersectionOf(conceptList.get(i), conceptList.get(j)));
                        newClasses.add(factory.getOWLObjectUnionOf(conceptList.get(i), conceptList.get(j)));
                    }
                }
            }
            classes = newClasses;
        }

        return classes;
    }
}
