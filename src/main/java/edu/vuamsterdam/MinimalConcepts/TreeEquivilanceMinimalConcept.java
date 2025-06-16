package edu.vuamsterdam.MinimalConcepts;

import org.semanticweb.HermiT.ReasonerFactory;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class TreeEquivilanceMinimalConcept implements MinimalConcept{
    private final OWLOntology ontology;
    private final OWLOntologyManager manager;
    private final OWLDataFactory factory;

    public TreeEquivilanceMinimalConcept(OWLOntology ontology) {
        this.ontology = ontology;
        this.manager = ontology.getOWLOntologyManager();
        this.factory = manager.getOWLDataFactory();
    }
    @Override
    public Optional<OWLClassExpression> getMinimalConcept(OWLClassExpression base) {
        Set<OWLClass> expressionset = ontology.classesInSignature().collect(Collectors.toSet());

        System.out.println("Generating replacements");
        Set<OWLClassExpression> replacements = generateAllReplacements(base, expressionset);
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
        Node<OWLClass> equivilantClasses = reasoner.getEquivalentClasses(baseClass);

        return newClasses.stream()
                .sorted(Comparator.comparingInt(p -> p.second().accept(new ClassExpressionSizeVisitor())))
                .filter(p -> equivilantClasses.contains(p.first()))
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
