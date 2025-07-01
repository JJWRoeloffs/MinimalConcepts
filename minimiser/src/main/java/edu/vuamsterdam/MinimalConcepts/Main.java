package edu.vuamsterdam.MinimalConcepts;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;

import java.io.File;
import java.util.stream.*;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        String filepath = args[0];
        try {
            processOntology(filepath);
        } catch (Throwable e) {
            GhettoLogger.logHardCrashed(filepath, e.toString());
        }
    }

    public static void processOntology(String filepath) throws Exception {
        GhettoLogger.logStart(filepath);
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLOntology ontology = manager.loadOntologyFromOntologyDocument(new File(filepath));

        Stream<OWLClassExpression> subAxiomExpressions = ontology.axioms()
                .filter(OWLSubClassOfAxiom.class::isInstance)
                .map(OWLSubClassOfAxiom.class::cast)
                .flatMap(axiom -> Stream.of(axiom.getSubClass(), axiom.getSuperClass()));

        Stream<OWLClassExpression> eqAxiomExpressions = ontology.axioms()
                .filter(OWLEquivalentClassesAxiom.class::isInstance)
                .map(OWLEquivalentClassesAxiom.class::cast)
                .filter(x -> x.classExpressions().noneMatch(OWLClassExpression::isClassExpressionLiteral))
                .flatMap(OWLNaryClassAxiom::classExpressions);

        int i = 0;
        long startTime = System.currentTimeMillis();
        for (Iterator<OWLClassExpression> it = Stream.concat(subAxiomExpressions, eqAxiomExpressions).iterator(); it.hasNext(); ) {
            i++;
            OWLClassExpression expression = it.next();
            try {
                minimizeExpression(expression, ontology);
            } catch (Throwable t) {
                GhettoLogger.logCrashed(expression.toString(), expression.accept(new ClassExpressionBuggedVisitor()), expression.accept(new ClassExpressionFeasibleVisitor()));
            }
        }
        long durationMillis = System.currentTimeMillis() - startTime;
        GhettoLogger.logFinish(filepath,  durationMillis, i);
    }

    private static void minimizeExpression(OWLClassExpression expression, OWLOntology ontology) {
        long startTime = System.currentTimeMillis();
        int origSize = expression.accept(new ClassExpressionSizeVisitor());
        if (origSize == 1)
            return;

        MinimalConcept minimalConceptGenerator = new SubsumptionLearningMinimalConcept(ontology, 5,0.02, true, false, 60*60);
        Optional<OWLClassExpression> newExpression = minimalConceptGenerator.getMinimalConcept(expression);

        long durationMillis = System.currentTimeMillis() - startTime;
        if (newExpression.isPresent()) {
            OWLClassExpression newExpr = newExpression.get();
            GhettoLogger.logMinimize(durationMillis, expression.toString(), origSize, newExpr.toString(), newExpr.accept(new ClassExpressionSizeVisitor()));
        } else {
            GhettoLogger.logMinimize(durationMillis, expression.toString(), origSize);
        }
    }
}