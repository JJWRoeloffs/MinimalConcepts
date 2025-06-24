package edu.vuamsterdam.MinimalConcepts;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.*;

import org.semanticweb.HermiT.ReasonerFactory;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.*;

public class SubsumptionLearningMinimalConcept implements MinimalConcept {
    private final boolean useStarModule;
    private final boolean disjuncts;
    private final long timeoutMillis;
    private final int maxSizeOverride;
    private final OWLOntology actualOntology;
    private final OWLReasoner reasoner;
    private final OWLDataFactory factory;
    private final OWLOntologyManager manager;
    private final double beta;
    private final Map<Pair<OWLClassExpression, OWLClassExpression>, Double> accuracies;
    private final Map<Pair<Integer, OWLClassExpression>, Set<OWLClassExpression>> rhoTops;
    private OWLOntology ontology;
    private Set<OWLEquivalentClassesAxiom> axiomsToRemove;
    private AtomicInteger removeIdx;

    public SubsumptionLearningMinimalConcept(OWLOntology ontology, int maxSize, double beta, boolean disjuncts, boolean useStarModule, int timeoutSeconds) {
        this.beta = beta;
        this.disjuncts = disjuncts;
        this.useStarModule = useStarModule;
        this.timeoutMillis = timeoutSeconds * 1000L;
        this.maxSizeOverride = maxSize;
        this.ontology = ontology;
        this.actualOntology = ontology;
        this.manager = ontology.getOWLOntologyManager();
        this.factory = manager.getOWLDataFactory();


        OWLReasonerFactory reasonerFactory = new ReasonerFactory();
        this.reasoner = reasonerFactory.createReasoner(ontology);
        System.out.println("precomputing initial inferences");
        reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);
        System.out.println("precomputed initial inferences");

        this.accuracies = new HashMap<>();
        this.rhoTops = new HashMap<>();
        this.axiomsToRemove = new HashSet<>();
        this.removeIdx = new AtomicInteger(0);
    }

    private static <T extends OWLClassExpression> T smallestItem(Stream<T> entities) {
        return entities.min(Comparator.comparingInt(x -> x.accept(new ClassExpressionSizeVisitor()))).orElseThrow();
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
        long startTime = System.currentTimeMillis();
        HashSet<OWLClassExpression> retList = new HashSet<>();
        int maxSize = Math.min(base.accept(new ClassExpressionSizeVisitor()), maxSizeOverride);
        OWLClass top = factory.getOWLThing();

        String baseIRI = "tempFormula#";
        OWLClass baseClass = factory.getOWLClass(IRI.create(baseIRI + removeIdx.getAndIncrement()));
        OWLEquivalentClassesAxiom baseAxiom = factory.getOWLEquivalentClassesAxiom(baseClass, base);
        axiomsToRemove.add(baseAxiom);
        manager.addAxiom(ontology, baseAxiom);

        // Current accuracy always gives top accuracy 0. This might change, coupling! beware!
        ArrayList<SearchNode> nodes = new ArrayList<>(Collections.singleton(new SearchNode(top, 1, 0, 0)));
        HashSet<OWLClassExpression> nodeFormulas = new HashSet<>(Collections.singleton(top));

        while (System.currentTimeMillis() - startTime < timeoutMillis) {
            SearchNode candidate = nodes.stream().max(Comparator.comparingDouble(x -> x.accuracy - beta * x.n)).orElseThrow();
            System.out.println(candidate.formula + "   " + candidate.n + "     " + candidate.accuracy);
            if (candidate.accuracy == Double.NEGATIVE_INFINITY) {
                return retList.stream().min(Comparator.comparingInt(x -> x.accept(new ClassExpressionSizeVisitor())));
            // These should be filtered out earlier already, but, formally, this is the place that guarantees it.
            } else if (candidate.formula.getClassesInSignature().stream().anyMatch(x -> x.getIRI().getShortForm().contains("tempFormula#"))) {
                candidate.accuracy = Double.NEGATIVE_INFINITY;
                continue;
            } else if (candidate.size >= maxSize || candidate.n > maxSize) {
                candidate.accuracy = Double.NEGATIVE_INFINITY;
                continue;
            } else if (reasoner.isEntailed(factory.getOWLEquivalentClassesAxiom(base, candidate.formula))) {
                retList.add(candidate.formula);
                maxSize = candidate.size;
                continue;
            }

            candidate.refined.addAll(rho(top, candidate.formula, base, candidate.n + 1));

            Set<OWLClassExpression> newSuccessors = candidate.refined.stream()
                    .filter(formula -> !formula.isOWLNothing())
                    .filter(formula -> formula.accept(new ClassExpressionSizeVisitor()) <= candidate.n + 1)
                    .filter(formula -> !nodeFormulas.contains(formula))
                    .collect(Collectors.toSet());
            nodeFormulas.addAll(newSuccessors);

            List<Pair<OWLClass, OWLClassExpression>> newClasses = new ArrayList<>();

            for (OWLClassExpression successor : newSuccessors) {
                OWLClass cls = factory.getOWLClass(IRI.create(baseIRI + removeIdx.getAndIncrement()));
                OWLEquivalentClassesAxiom ax = factory.getOWLEquivalentClassesAxiom(cls, successor);
                manager.addAxiom(ontology, ax);

                newClasses.add(new Pair<>(cls, successor));
                axiomsToRemove.add(ax);
            }

            reasoner.flush();
            System.out.println("precomputing inferences");
            reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);
            System.out.println("precomputed inferences");

            Set<SearchNode> newNodes = newClasses.stream()
                    .map(pair -> new SearchNode(pair.second(), pair.second().accept(new ClassExpressionSizeVisitor()), candidate.n, accuracyCashed(baseClass, pair)))
                    .collect(Collectors.toSet());

            nodes.addAll(newNodes);
            candidate.n += 1;
        }

        return retList.stream().min(Comparator.comparingInt(x -> x.accept(new ClassExpressionSizeVisitor())));
    }

    private Set<OWLClassExpression> rho(OWLClassExpression context, OWLClassExpression target, OWLClassExpression base, int targetSize) {
        Set<OWLClassExpression> ret = rhoPrime(context, target, base, targetSize);
        Collections.addAll(ret, factory.getOWLNothing(), factory.getOWLObjectIntersectionOf(target, factory.getOWLThing()));
        return ret.stream().filter(func -> !func.accept(new ClassExpressionBuggedVisitor())).collect(Collectors.toSet());
    }

    private Set<OWLClassExpression> rhoPrime(OWLClassExpression context, OWLClassExpression target, OWLClassExpression base, int targetSize) {
        final int currentSize = target.accept(new ClassExpressionSizeVisitor());
        if (target.isOWLNothing())
            return new HashSet<>();
        if (target.isOWLThing())
            return rhoTopCashed(targetSize, base);
        if (target instanceof OWLClass)
            return reasoner.getSubClasses(target, true)
                    .nodes()
                    .map(node -> smallestItem(node.entities()))
                    .filter(owlClass -> !owlClass.getIRI().getShortForm().contains("tempFormula#"))
                    .filter(x -> !reasoner.isEntailed(factory.getOWLDisjointClassesAxiom(x, context)))
                    .collect(Collectors.toSet());
        if (target instanceof OWLObjectComplementOf)
            return reasoner.getSuperClasses(((OWLObjectComplementOf) target).getOperand(), true)
                    .nodes()
                    .map(node -> smallestItem(node.entities()))
                    .filter(owlClass -> !owlClass.getIRI().getShortForm().contains("tempFormula#"))
                    .map(factory::getOWLObjectComplementOf)
                    .filter(x -> !reasoner.isEntailed(factory.getOWLDisjointClassesAxiom(x, context)))
                    .collect(Collectors.toSet());
        if (target instanceof OWLObjectSomeValuesFrom) {
            Set<OWLClassExpression> concepts = rho(
                    smallestItem(reasoner.getObjectPropertyRanges(((OWLObjectSomeValuesFrom) target).getProperty(), true).entities()),
                    ((OWLObjectSomeValuesFrom) target).getFiller(),
                    base,
                    targetSize - 1);
            return concepts.stream()
                    .map(x -> factory.getOWLObjectSomeValuesFrom(((OWLObjectSomeValuesFrom) target).getProperty(), x))
                    .collect(Collectors.toSet());
        }
        if (target instanceof OWLObjectAllValuesFrom) {
            Set<OWLClassExpression> concepts = rho(
                    smallestItem(reasoner.getObjectPropertyRanges(((OWLObjectAllValuesFrom) target).getProperty(), true).entities()),
                    ((OWLObjectAllValuesFrom) target).getFiller(),
                    base,
                    targetSize - 1);
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
                Set<OWLClassExpression> newItems = rho(context, operands.get(idx), base, newTargetSize)
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
                Set<OWLClassExpression> newItems = rho(context, operands.get(idx), base, newTargetSize)
                        .stream()
                        .map(x -> factory.getOWLObjectUnionOf(Helpers.replaceElement(operands, idx, x)))
                        .collect(Collectors.toSet());
                ret.addAll(newItems);
            }
            return ret;
        }

        throw new IllegalStateException();
    }

    private Set<OWLClassExpression> rhoTopCashed(int targetSize, OWLClassExpression base) {
        return rhoTops.computeIfAbsent(new Pair<>(targetSize, base), pair -> rhoTop(pair.first(), pair.second()));
    }

    private Set<OWLClassExpression> rhoTop(int targetSize, OWLClassExpression base) {
        System.out.println("Started rho top");
        Set<OWLClassExpression> bases = reasoner.getSubClasses(factory.getOWLThing(), true)
                .nodes()
                .map(node -> smallestItem(node.entities()))
                .filter(owlClass -> !owlClass.getIRI().getShortForm().contains("tempFormula#"))
                .collect(Collectors.toSet());

        Set<OWLClassExpression> botClasses = reasoner.getSuperClasses(factory.getOWLNothing(), true)
                .nodes()
                .map(node -> smallestItem(node.entities()))
                .filter(owlClass -> !owlClass.getIRI().getShortForm().contains("tempFormula#"))
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

        bases.removeIf(f -> reasoner.isEntailed(factory.getOWLDisjointClassesAxiom(base, f)));

        // If we don't introduce any disjuncts here, we won't ever create them.
        if (!disjuncts) {
            System.out.println("Finished rho top");
            return bases;
        }

        Set<OWLClassExpression> ret = new HashSet<>();
        Set<Set<OWLClassExpression>> powerSet = Helpers.powerSetOfSize(bases, targetSize);

        for (Set<OWLClassExpression> subset : powerSet) {
            if (subset.size() == 1)
                ret.addAll(subset);
            else if (!subset.isEmpty())
                ret.add(factory.getOWLObjectUnionOf(subset));
        }

        System.out.println("Finished rho top");
        return ret;
    }

    private double accuracyCashed(OWLClass target, Pair<OWLClass, OWLClassExpression> found) {
        return accuracies.computeIfAbsent(new Pair<>(target, found.second()),
                k -> accuracy(target, found.first()));
    }

    private double accuracy(OWLClass target, OWLClass found) {
        // For now, I return posinf to indicate that either we have our value,
        // I might do this differently if needed for something,
        // But I otherwise do not see a reason to overcomplicate things.
        if (reasoner.getEquivalentClasses(target).contains(found))
            return Double.POSITIVE_INFINITY;

        // Here, we do not filter out the tempFormula# classes. In fact, keeping them around here
        // for extra fidelity of the search is the reason we don't remove them earlier.
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
