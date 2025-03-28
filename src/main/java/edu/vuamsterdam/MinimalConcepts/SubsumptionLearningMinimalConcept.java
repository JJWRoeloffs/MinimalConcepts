package edu.vuamsterdam.MinimalConcepts;

import java.util.*;
import java.util.stream.*;

import org.semanticweb.HermiT.ReasonerFactory;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.*;

public class SubsumptionLearningMinimalConcept implements MinimalConcept {
    private final OWLOntology ontology;
    private final OWLReasoner reasoner;
    private final OWLDataFactory factory;
    private final double beta;

    // I need them mutable, so I cannot use records, unfortunately.
    private static class SearchNode {
            OWLClassExpression formula;
            int size;
            int n;
            double accuracy;
            Set<OWLClassExpression> refined;

            public SearchNode(OWLClassExpression formula, int size, int n, double accuracy) {
                this.formula = formula;
                this.size = size;
                this.n = n;
                this.accuracy = accuracy;
                this.refined = new HashSet<>();
            }
    };

    public SubsumptionLearningMinimalConcept(OWLOntology ontology, double beta) {
        this.beta = beta;
        this.ontology = ontology;
        this.factory = ontology.getOWLOntologyManager().getOWLDataFactory();

        OWLReasonerFactory reasonerFactory = new ReasonerFactory();
        this.reasoner = reasonerFactory.createReasoner(ontology);
        reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);
    }

    private static <T extends OWLClassExpression> T smallestItem(Stream<T> entities) {
        return entities.min(Comparator.comparingInt(x -> x.accept(new ClassExpressionSizeVisitor()))).orElseThrow();
    }

    @Override
    public Optional<OWLClassExpression> getMinimalConcept(OWLClassExpression base) {
        int maxSize = base.accept(new ClassExpressionSizeVisitor());
        OWLClass top = factory.getOWLThing();

        SearchNode searchTree = new SearchNode(top, 1, 0, accuracy(base, top));
        ArrayList<SearchNode> allNodes = new ArrayList<>(Collections.singleton(searchTree));

        while (true) {
            SearchNode candidate = allNodes.stream().max(Comparator.comparingDouble(x -> accuracy(base, x.formula) - beta * x.n)).orElseThrow();
            if (reasoner.isEntailed(factory.getOWLEquivalentClassesAxiom(base, candidate.formula)))
                return Optional.of(candidate.formula);
            if (candidate.size >= maxSize)
                return Optional.empty();
            if (candidate.refined.isEmpty())
                candidate.refined = rho(top, candidate.formula);

            Set<SearchNode> newSuccessors = candidate.refined.stream()
                    .filter(formula -> !formula.isOWLNothing())
                    .filter(formula -> formula.accept(new ClassExpressionSizeVisitor()) == candidate.n + 1)
                    .map(formula -> new SearchNode(formula, formula.accept(new ClassExpressionSizeVisitor()), candidate.n, accuracy(base, formula)))
                    .collect(Collectors.toSet());

            allNodes.addAll(newSuccessors);
            candidate.n += 1;
        }
    }

    private Set<OWLClassExpression> rho(OWLClassExpression context, OWLClassExpression target) {
        Set<OWLClassExpression> ret = rhoPrime(context, target);
        Collections.addAll(ret, factory.getOWLNothing(), factory.getOWLObjectIntersectionOf(target, factory.getOWLThing()));
        return ret.stream().filter(func -> !func.accept(new ClassExpressionBuggedVisitor())).collect(Collectors.toSet());
    }

    private Set<OWLClassExpression> rhoPrime(OWLClassExpression context, OWLClassExpression target) {
        if (target.isOWLNothing())
            return new HashSet<>();
        if (target.isOWLThing())
            return rhoTop(context);
        if (target instanceof OWLClass)
            return reasoner.getSubClasses(target, true)
                    .nodes()
                    .map(node -> smallestItem(node.entities()))
                    .filter(x -> !reasoner.isEntailed(factory.getOWLDisjointClassesAxiom(x, context)))
                    .collect(Collectors.toSet());
        if (target instanceof OWLObjectComplementOf)
            return reasoner.getSuperClasses(((OWLObjectComplementOf) target).getOperand(), true)
                    .nodes()
                    .map(node -> smallestItem(node.entities()))
                    .map(factory::getOWLObjectComplementOf)
                    .filter(x -> !reasoner.isEntailed(factory.getOWLDisjointClassesAxiom(x, context)))
                    .collect(Collectors.toSet());
        if (target instanceof OWLObjectSomeValuesFrom) {
            Set<OWLClassExpression> concepts = rho(
                    smallestItem(reasoner.getObjectPropertyRanges(((OWLObjectSomeValuesFrom) target).getProperty(), true).entities()),
                    ((OWLObjectSomeValuesFrom) target).getFiller());
            return concepts.stream()
                    .map(x -> factory.getOWLObjectSomeValuesFrom(((OWLObjectSomeValuesFrom) target).getProperty(), x))
                    .collect(Collectors.toSet());
        }
        if (target instanceof OWLObjectAllValuesFrom) {
            Set<OWLClassExpression> concepts = rho(
                    smallestItem(reasoner.getObjectPropertyRanges(((OWLObjectAllValuesFrom) target).getProperty(), true).entities()),
                    ((OWLObjectAllValuesFrom) target).getFiller());
            return concepts.stream()
                    .map(x -> factory.getOWLObjectAllValuesFrom(((OWLObjectAllValuesFrom) target).getProperty(), x))
                    .collect(Collectors.toSet());
        }
        if (target instanceof OWLObjectIntersectionOf) {
            ArrayList<OWLClassExpression> operands = new ArrayList<>(((OWLObjectIntersectionOf) target).getOperands());
            HashSet<OWLClassExpression> ret = new HashSet<>();

            for (int i = 0; i < operands.size(); i++) {
                final int idx = i;
                Set<OWLClassExpression> newItems = rho(context, operands.get(i))
                        .stream()
                        .map(x -> factory.getOWLObjectIntersectionOf(Helpers.replaceElement(operands, idx, x)))
                        .collect(Collectors.toSet());
                ret.addAll(newItems);
            }
            return ret;
        }
        if (target instanceof OWLObjectUnionOf) {
            ArrayList<OWLClassExpression> operands = new ArrayList<>(((OWLObjectUnionOf) target).getOperands());
            HashSet<OWLClassExpression> ret = new HashSet<>();

            for (int i = 0; i < operands.size(); i++) {
                final int idx = i;
                Set<OWLClassExpression> newItems = rho(context, operands.get(i))
                        .stream()
                        .map(x -> factory.getOWLObjectUnionOf(Helpers.replaceElement(operands, idx, x)))
                        .collect(Collectors.toSet());
                ret.addAll(newItems);
            }
            return ret;
        }

        throw new IllegalStateException();
    }

    private Set<OWLClassExpression> rhoTop(OWLClassExpression context) {
        Set<OWLClassExpression> bases = reasoner.getSubClasses(factory.getOWLThing(), true)
                .nodes()
                .map(node -> smallestItem(node.entities()))
                .collect(Collectors.toSet());

        Set<OWLClassExpression> botClasses = reasoner.getSuperClasses(factory.getOWLNothing(), true)
              .nodes()
              .map(node -> smallestItem(node.entities()))
              .map(factory::getOWLObjectComplementOf)
              .collect(Collectors.toSet());
        bases.addAll(botClasses);

        Set<OWLClassExpression> existTop = ontology.objectPropertiesInSignature()
                .map(prop -> factory.getOWLObjectSomeValuesFrom(prop, factory.getOWLThing()))
                .collect(Collectors.toSet());
        bases.addAll(existTop);

        Set<OWLClassExpression> forallTop = ontology.objectPropertiesInSignature()
                .map(prop -> factory.getOWLObjectAllValuesFrom(prop, factory.getOWLThing()))
                .collect(Collectors.toSet());
        bases.addAll(forallTop);

        // This will grow massively with powerSets.
        // Is this really viable for larger ontologies?
        Set<OWLClassExpression> ret = new HashSet<>();
        Set<Set<OWLClassExpression>> powerSet = Helpers.powerSet(bases);
        powerSet.remove(new HashSet<OWLClassExpression>());

        for (Set<OWLClassExpression> subset : powerSet) {
            if (subset.size() == 1)
                ret.addAll(subset);
            else if (!subset.isEmpty())
                ret.add(factory.getOWLObjectUnionOf(subset));
        }

        return ret;
    }

    private double accuracy(OWLClassExpression target, OWLClassExpression found) {
        // For now, I return posinf and neginf to indicate that either we have our value,
        // or the branch is dead to us. I might do this differently if needed for something,
        // But I otherwise do not see a reason to overcomplicate things.
        if (found.accept(new ClassExpressionSizeVisitor()) >= target.accept(new ClassExpressionSizeVisitor()))
            return Double.NEGATIVE_INFINITY;
        if (reasoner.isEntailed(factory.getOWLEquivalentClassesAxiom(target, found)))
            return Double.POSITIVE_INFINITY;

        Set<OWLClass> targetSuperClasses = reasoner.getSuperClasses(target, false).getFlattened();
        Set<OWLClass> foundSuperClasses = reasoner.getSuperClasses(found, false).getFlattened();

        if (targetSuperClasses.isEmpty())
            throw new RuntimeException();

        // Should be in superclasses, isn't
        Set<OWLClass> falseNegativesSuper = new HashSet<>(targetSuperClasses);
        falseNegativesSuper.removeAll(foundSuperClasses);

        // Shouldn't be in superclasses, is
        Set<OWLClass> falsePositivesSuper = new HashSet<>(foundSuperClasses);
        falsePositivesSuper.removeAll(targetSuperClasses);

        // All seen superclasses
        Set<OWLClass> allSuper = new HashSet<>(foundSuperClasses);
        allSuper.addAll(targetSuperClasses);

        return 1 - ((double) falseNegativesSuper.size() + (double) falsePositivesSuper.size()) / allSuper.size();
    }
}
