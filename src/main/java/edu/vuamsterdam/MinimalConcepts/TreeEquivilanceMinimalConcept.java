package edu.vuamsterdam.MinimalConcepts;

import org.semanticweb.HermiT.ReasonerFactory;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class TreeEquivilanceMinimalConcept implements MinimalConcept{
    private final OWLOntology ontology;
    public TreeEquivilanceMinimalConcept(OWLOntology ontology) {
        this.ontology = ontology;
    }
    @Override
    public Optional<OWLClassExpression> getMinimalConcept(OWLClassExpression base) {
        Set<OWLClass> expressionset = ontology.classesInSignature().collect(Collectors.toSet());

        OWLOntologyManager manager = ontology.getOWLOntologyManager();
        OWLDataFactory factory = manager.getOWLDataFactory();

        OWLExpressionReplacer replacer = new OWLExpressionReplacer(factory);
        System.out.println("Generating replacements");
        Set<OWLClassExpression> replacements = replacer.generateAllReplacements(base, expressionset);
        System.out.println("Generated replacements");

        OWLOntology newOntology = Helpers.copyOntology(ontology);

        String baseIRI = "tempFormula#";
        OWLClass baseClass = factory.getOWLClass(IRI.create(baseIRI + 0));
        manager.addAxiom(newOntology, factory.getOWLEquivalentClassesAxiom(baseClass, base));

        // There seriously isn't a way to enumerate streams in java still?
        AtomicInteger i = new AtomicInteger(1);
        List<Pair<OWLClass, OWLClassExpression>> newClasses = replacements.stream()
                .map(expr -> {
                    OWLClass cls = factory.getOWLClass(IRI.create(baseIRI + i.getAndIncrement()));
                    OWLEquivalentClassesAxiom ax = factory.getOWLEquivalentClassesAxiom(cls, expr);
                    manager.addAxiom(newOntology, ax);
                    return new Pair<>(cls, expr);
                })
                .toList();

        OWLReasonerFactory reasonerFactory = new ReasonerFactory();
        OWLReasoner reasoner = reasonerFactory.createReasoner(newOntology);
        System.out.println("precomputing inferences");
        reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);
        System.out.println("precomputed inferences");

        // They are already sorted by size.
        for (Pair<OWLClass, OWLClassExpression> candidate : newClasses) {
            if (reasoner.getEquivalentClasses(baseClass).contains(candidate.first())) {
                return Optional.of(candidate.second());
            }
        }
        return Optional.empty();
    }
}
