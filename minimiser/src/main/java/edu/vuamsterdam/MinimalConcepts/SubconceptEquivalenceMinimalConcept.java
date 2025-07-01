package edu.vuamsterdam.MinimalConcepts;

import org.semanticweb.HermiT.ReasonerFactory;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.*;

import java.util.*;
import java.util.stream.*;
import java.util.concurrent.atomic.AtomicInteger;

public class SubconceptEquivalenceMinimalConcept implements MinimalConcept{
    private final boolean useStarModule;
    private final OWLOntology actualOntology;
    private final OWLOntologyManager manager;
    private final OWLDataFactory factory;
    private OWLOntology ontology;
    private Set<OWLEquivalentClassesAxiom> axiomsToRemove;

    public SubconceptEquivalenceMinimalConcept(OWLOntology ontology, boolean useStarModule) {
        this.useStarModule = useStarModule;
        this.ontology = ontology;
        this.actualOntology = ontology;
        this.manager = ontology.getOWLOntologyManager();
        this.factory = manager.getOWLDataFactory();
        this.axiomsToRemove = new HashSet<>();
    }

    @Override
    public Optional<OWLClassExpression> getMinimalConcept(OWLClassExpression base) {
        if (useStarModule) {
            this.ontology = StarModuleExtractor.extractStarModule(actualOntology, base);
        }
        axiomsToRemove = new HashSet<>();

        Optional<OWLClassExpression> ret = getMinimalConceptInner(base);

        manager.removeAxioms(ontology, axiomsToRemove);
        ontology = actualOntology;
        axiomsToRemove = new HashSet<>();
        return ret;
    }

    private Optional<OWLClassExpression> getMinimalConceptInner(OWLClassExpression base) {
        Set<OWLClassExpression> subconcepts = base.accept(new ClassExpressionAllComplexVisitor()).collect(Collectors.toSet());
        Set<OWLClass> origClasses = ontology.getClassesInSignature();

        String baseIRI = "tempFormula#";
        OWLClass baseClass = factory.getOWLClass(IRI.create(baseIRI + 0));
        OWLEquivalentClassesAxiom baseAxiom = factory.getOWLEquivalentClassesAxiom(baseClass, base);
        manager.addAxiom(ontology, baseAxiom);

        AtomicInteger i = new AtomicInteger(1);
        List<Pair<OWLClass, OWLClassExpression>> newClasses = new ArrayList<>();

        for (OWLClassExpression expression : subconcepts) {
            OWLClass cls = factory.getOWLClass(IRI.create(baseIRI + i.getAndIncrement()));
            OWLEquivalentClassesAxiom ax = factory.getOWLEquivalentClassesAxiom(cls, expression);
            manager.addAxiom(ontology, ax);

            newClasses.add(new Pair<>(cls, expression));
            axiomsToRemove.add(ax);
        }

        OWLReasonerFactory reasonerFactory = new ReasonerFactory();
        OWLReasoner reasoner = reasonerFactory.createReasoner(ontology);
        reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);

        newClasses.sort(Comparator.comparingInt(p -> p.second().accept(new ClassExpressionSizeVisitor())));
        OWLClassExpression smallest = base;

        for (Pair<OWLClass, OWLClassExpression> pair : newClasses) {
            Optional<OWLClass> equivClass = reasoner.getEquivalentClasses(pair.first()).getEntities().stream().filter(origClasses::contains).findAny();
            if (equivClass.isPresent()) {
                smallest = smallest.accept(new ClassExpressionReplacerVisitor(pair.second(), equivClass.get(), factory));
            }
        }

        return smallest.equals(base) ? Optional.empty() : Optional.of(smallest);
    }
}
