package edu.vuamsterdam.MinimalConcepts;

import org.semanticweb.HermiT.ReasonerFactory;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.*;

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

        String baseIRI = "tempFormula#";
        OWLClass baseClass = factory.getOWLClass(IRI.create(baseIRI + 0));
        OWLEquivalentClassesAxiom baseAxiom = factory.getOWLEquivalentClassesAxiom(baseClass, base);
        manager.addAxiom(ontology, baseAxiom);

        AtomicInteger i = new AtomicInteger(1);
        List<Pair<OWLClass, OWLClassExpression>> newClasses = new ArrayList<>();
        List<OWLEquivalentClassesAxiom> newAxioms = new ArrayList<>();

        for (OWLClassExpression expression : replacements) {
            OWLClass cls = factory.getOWLClass(IRI.create(baseIRI + i.getAndIncrement()));
            OWLEquivalentClassesAxiom ax = factory.getOWLEquivalentClassesAxiom(cls, expression);
            manager.addAxiom(ontology, ax);

            newClasses.add(new Pair<>(cls, expression));
            newAxioms.add(ax);
        }

        OWLReasonerFactory reasonerFactory = new ReasonerFactory();
        OWLReasoner reasoner = reasonerFactory.createReasoner(ontology);
        System.out.println("precomputing inferences");
        reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);
        System.out.println("precomputed inferences");
        final Node<OWLClass> equivilantClasses = reasoner.getEquivalentClasses(baseClass);
        System.out.println(equivilantClasses);

        Optional<OWLClassExpression> ret =  newClasses.stream()
                .sorted(Comparator.comparingInt(p -> p.second().accept(new ClassExpressionSizeVisitor())))
                .filter(p -> equivilantClasses.contains(p.first()))
                .map(Pair::second)
                .findAny();

        manager.removeAxioms(ontology, Stream.concat(newAxioms.stream(), Stream.of(baseAxiom)));
        reasoner.flush();

        return ret;
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
