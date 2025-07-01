package edu.vuamsterdam.MinimalConcepts;

import org.semanticweb.HermiT.ReasonerFactory;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.*;

public class TreeEquivalenceMinimalConcept implements MinimalConcept {
    private final boolean useStarModule;
    private final OWLOntology actualOntology;
    private final OWLOntologyManager manager;
    private final OWLDataFactory factory;
    private OWLOntology ontology;
    private Set<OWLEquivalentClassesAxiom> axiomsToRemove;

    public TreeEquivalenceMinimalConcept(OWLOntology ontology, boolean useStarModule) {
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
        Set<OWLClass> expressionset = ontology.classesInSignature().collect(Collectors.toSet());

        Set<OWLClassExpression> replacements = generateAllReplacements(base, expressionset);

        String baseIRI = "tempFormula#";
        OWLClass baseClass = factory.getOWLClass(IRI.create(baseIRI + 0));
        OWLEquivalentClassesAxiom baseAxiom = factory.getOWLEquivalentClassesAxiom(baseClass, base);
        manager.addAxiom(ontology, baseAxiom);

        AtomicInteger i = new AtomicInteger(1);
        List<Pair<OWLClass, OWLClassExpression>> newClasses = new ArrayList<>();

        for (OWLClassExpression expression : replacements) {
            OWLClass cls = factory.getOWLClass(IRI.create(baseIRI + i.getAndIncrement()));
            OWLEquivalentClassesAxiom ax = factory.getOWLEquivalentClassesAxiom(cls, expression);
            manager.addAxiom(ontology, ax);

            newClasses.add(new Pair<>(cls, expression));
            axiomsToRemove.add(ax);
        }

        OWLReasonerFactory reasonerFactory = new ReasonerFactory();
        OWLReasoner reasoner = reasonerFactory.createReasoner(ontology);
        reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);
        final Node<OWLClass> equivalentClasses = reasoner.getEquivalentClasses(baseClass);

        return newClasses.stream()
                .sorted(Comparator.comparingInt(p -> p.second().accept(new ClassExpressionSizeVisitor())))
                .filter(p -> equivalentClasses.contains(p.first()))
                .map(Pair::second)
                .findAny();
    }

    private Set<OWLClassExpression> generateAllReplacements(OWLClassExpression expr, Set<OWLClass> replacements) {
        Set<OWLClassExpression> result = new HashSet<>(replacements);
        replaceRecursive(expr, replacements, result);
        return result;
    }

    private void replaceRecursive(OWLClassExpression expr, Set<OWLClass> replacements, Set<OWLClassExpression> result) {
        if (expr instanceof OWLObjectIntersectionOf andExpr) {
            for (OWLClassExpression operand : andExpr.getOperands()) {
                if (!operand.isClassExpressionLiteral()) {
                    for (OWLClass replacement : replacements) {
                        Set<OWLClassExpression> newOperands = new HashSet<>();
                        for (OWLClassExpression op : andExpr.getOperands()) {
                            newOperands.add(op.equals(operand) ? replacement : op);
                        }
                        OWLClassExpression newExpr = factory.getOWLObjectIntersectionOf(newOperands);
                        result.add(newExpr);
                        replaceRecursive(newExpr, replacements, result);
                    }
                }
            }
        } else if (expr instanceof OWLObjectUnionOf orExpr) {
            for (OWLClassExpression operand : orExpr.getOperands()) {
                if (!operand.isClassExpressionLiteral()) {
                    for (OWLClass replacement : replacements) {
                        Set<OWLClassExpression> newOperands = new HashSet<>();
                        for (OWLClassExpression op : orExpr.getOperands()) {
                            newOperands.add(op.equals(operand) ? replacement : op);
                        }
                        OWLClassExpression newExpr = factory.getOWLObjectIntersectionOf(newOperands);
                        result.add(newExpr);
                        replaceRecursive(newExpr, replacements, result);
                    }
                }
            }
        } else if (expr instanceof OWLObjectSomeValuesFrom someExpr) {
            if (!someExpr.getFiller().isClassExpressionLiteral()) {
                for (OWLClass replacement : replacements) {
                    OWLClassExpression newExpr = factory.getOWLObjectSomeValuesFrom(someExpr.getProperty(), replacement);
                    result.add(newExpr);
                    replaceRecursive(newExpr, replacements, result);
                }
            }
        } else if (expr instanceof OWLObjectAllValuesFrom allExpr) {
            if (!allExpr.getFiller().isClassExpressionLiteral()) {
                for (OWLClass replacement : replacements) {
                    OWLClassExpression newExpr = factory.getOWLObjectSomeValuesFrom(allExpr.getProperty(), replacement);
                    result.add(newExpr);
                    replaceRecursive(newExpr, replacements, result);
                }
            }
        }
    }
}
