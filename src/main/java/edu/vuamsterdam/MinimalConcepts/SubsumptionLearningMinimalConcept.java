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
    private final Map<Pair<OWLClassExpression>, Double> accuracies;
    private final Map<Integer, Set<OWLClassExpression>> rhoTops;

    public SubsumptionLearningMinimalConcept(OWLOntology ontology, double beta) {
        this.beta = beta;
        this.ontology = ontology;
        this.factory = ontology.getOWLOntologyManager().getOWLDataFactory();

        OWLReasonerFactory reasonerFactory = new ReasonerFactory();
        this.reasoner = reasonerFactory.createReasoner(ontology);
        reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);

        this.accuracies = new HashMap<>();
        this.rhoTops = new HashMap<>();
    }

    private static <T extends OWLClassExpression> T smallestItem(Stream<T> entities) {
        return entities.min(Comparator.comparingInt(x -> x.accept(new ClassExpressionSizeVisitor()))).orElseThrow();
    }

    @Override
    public Optional<OWLClassExpression> getMinimalConcept(OWLClassExpression base) {
        int maxSize = base.accept(new ClassExpressionSizeVisitor());
        OWLClass top = factory.getOWLThing();

        ArrayList<SearchNode> nodes = new ArrayList<>(Collections.singleton(new SearchNode(top, 1, 0, accuracyCashed(base, top))));

        while (true) {
            SearchNode candidate = nodes.stream().max(Comparator.comparingDouble(x -> accuracyCashed(base, x.formula) - beta * x.n)).orElseThrow();
            if (reasoner.isEntailed(factory.getOWLEquivalentClassesAxiom(base, candidate.formula)))
                return Optional.of(candidate.formula);
            if (candidate.size >= maxSize)
                return Optional.empty();

            candidate.refined.addAll(rho(top, candidate.formula, candidate.n + 1));

            Set<SearchNode> newSuccessors = candidate.refined.stream()
                    .filter(formula -> !formula.isOWLNothing())
                    .filter(formula -> formula.accept(new ClassExpressionSizeVisitor()) == candidate.n + 1)
                    .map(formula -> new SearchNode(formula, formula.accept(new ClassExpressionSizeVisitor()), candidate.n, accuracyCashed(base, formula)))
                    .collect(Collectors.toSet());

            nodes.addAll(newSuccessors);
            candidate.n += 1;
        }
    }

    private Set<OWLClassExpression> rho(OWLClassExpression context, OWLClassExpression target, int targetSize) {
        Set<OWLClassExpression> ret = rhoPrime(context, target, targetSize);
        Collections.addAll(ret, factory.getOWLNothing(), factory.getOWLObjectIntersectionOf(target, factory.getOWLThing()));
        return ret.stream().filter(func -> !func.accept(new ClassExpressionBuggedVisitor())).collect(Collectors.toSet());
    }

    private Set<OWLClassExpression> rhoPrime(OWLClassExpression context, OWLClassExpression target, int targetSize) {
        final int currentSize = target.accept(new ClassExpressionSizeVisitor());
        if (target.isOWLNothing())
            return new HashSet<>();
        if (target.isOWLThing())
            return rhoTopCashed(targetSize);
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
                    ((OWLObjectSomeValuesFrom) target).getFiller(),
                    targetSize - 1);
            return concepts.stream()
                    .map(x -> factory.getOWLObjectSomeValuesFrom(((OWLObjectSomeValuesFrom) target).getProperty(), x))
                    .collect(Collectors.toSet());
        }
        if (target instanceof OWLObjectAllValuesFrom) {
            Set<OWLClassExpression> concepts = rho(
                    smallestItem(reasoner.getObjectPropertyRanges(((OWLObjectAllValuesFrom) target).getProperty(), true).entities()),
                    ((OWLObjectAllValuesFrom) target).getFiller(),
                    targetSize -1);
            return concepts.stream()
                    .map(x -> factory.getOWLObjectAllValuesFrom(((OWLObjectAllValuesFrom) target).getProperty(), x))
                    .collect(Collectors.toSet());
        }
        if (target instanceof OWLObjectIntersectionOf) {
            ArrayList<OWLClassExpression> operands = new ArrayList<>(((OWLObjectIntersectionOf) target).getOperands());
            HashSet<OWLClassExpression> ret = new HashSet<>();

            for (int i = 0; i < operands.size(); i++) {
                final int idx = i;
                final int newTargetSize = (targetSize - currentSize) + operands.get(idx).accept(new ClassExpressionSizeVisitor());
                Set<OWLClassExpression> newItems = rho(context, operands.get(idx), newTargetSize)
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
                final int newTargetSize = (targetSize - currentSize) + operands.get(idx).accept(new ClassExpressionSizeVisitor());
                Set<OWLClassExpression> newItems = rho(context, operands.get(idx), newTargetSize)
                        .stream()
                        .map(x -> factory.getOWLObjectUnionOf(Helpers.replaceElement(operands, idx, x)))
                        .collect(Collectors.toSet());
                ret.addAll(newItems);
            }
            return ret;
        }

        throw new IllegalStateException();
    }

    private Set<OWLClassExpression> rhoTopCashed(int targetSize) {
        return rhoTops.computeIfAbsent(targetSize, this::rhoTop);
    }

    private Set<OWLClassExpression> rhoTop(int targetSize) {
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

        Set<OWLClassExpression> ret = new HashSet<>();
        Set<Set<OWLClassExpression>> powerSet = Helpers.powerSetOfSize(bases, targetSize);

        for (Set<OWLClassExpression> subset : powerSet) {
            if (subset.size() == 1)
                ret.addAll(subset);
            else if (!subset.isEmpty())
                ret.add(factory.getOWLObjectUnionOf(subset));
        }

        return ret;
    }

    private double accuracyCashed(OWLClassExpression target, OWLClassExpression found) {
        return accuracies.computeIfAbsent(new Pair<>(target, found), pair -> accuracy(pair.getFirst(), pair.getSecond()));
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

        long falsePositives = foundSuperClasses.stream()
                .filter(c -> !targetSuperClasses.contains(c))
                .count();
        if (falsePositives >= 1)
            return Double.NEGATIVE_INFINITY;

        long falseNegatives = targetSuperClasses.stream()
                .filter(c -> !foundSuperClasses.contains(c))
                .count();
        long all = Stream.concat(targetSuperClasses.stream(), foundSuperClasses.stream())
                .distinct()
                .count();

        return 1 - (double) falseNegatives / all;
    }

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
    }
}
