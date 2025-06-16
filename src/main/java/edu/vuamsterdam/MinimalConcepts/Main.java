package edu.vuamsterdam.MinimalConcepts;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;

import java.io.File;
import java.util.concurrent.TimeoutException;
import java.util.stream.*;
import java.util.*;

public class Main {
    public static void main(String[] args) throws Exception {
        processOntology(args[0]);
    }
    public static void processOntology(String filepath) throws Exception {
        System.out.println("Processing file: " + filepath);
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

        try {
            for (Iterator<OWLClassExpression> it = Stream.concat(subAxiomExpressions, eqAxiomExpressions).iterator(); it.hasNext(); ) {
                OWLClassExpression expression = it.next();
                try {
                    minimizeExpression(expression, ontology);
                } catch (TimeoutException e) {
                    throw e;
                } catch (Throwable t) {
                    System.out.println("Could not Minimise: " + expression);
                    if (expression.accept(new ClassExpressionFeasibleVisitor()) && !expression.accept(new ClassExpressionBuggedVisitor())) {
                        System.out.println("WARNING: The unminimisable expression did not appear to be bugged");
                    }
                }
            }
        } catch (TimeoutException e) {
            e.printStackTrace();
            System.out.println("Not minimizing " + filepath + "Took too long");
        }
        System.out.println("Done processing file: " + filepath);
    }
    private static void minimizeExpression(OWLClassExpression expression, OWLOntology ontology) throws Exception {
        System.out.println("---------------------------------------");
        System.out.println("Minimizing expression: " + expression);
        try {
            int origSize = expression.accept(new ClassExpressionSizeVisitor());
            System.out.println("Original size: " + origSize);
            if (origSize == 1)
                return;

            if (origSize < 5) {
                MinimalConcept minimalConceptGenerator = new SubsumptionLearningMinimalConcept(ontology, 0.02);
                Optional<OWLClassExpression> newExpression = minimalConceptGenerator.getMinimalConcept(expression);
                newExpression.ifPresent(expr -> System.out.println(
                        "Minimized expression to: " + expr + "\nWith size: " + expr.accept(new ClassExpressionSizeVisitor())
                ));
                System.out.println("---------------------------------------");
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }
}
