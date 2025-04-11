package edu.vuamsterdam.MinimalConcepts;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;

import java.io.File;
import java.util.Optional;
import java.util.stream.*;

public class Main {
    public static void main(String[] args) throws OWLOntologyCreationException {
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLOntology ontology = manager.loadOntologyFromOntologyDocument(new File("pizza-ontology/pizza.owl"));

        Stream<OWLClassExpression> subAxiomExpressions = ontology.axioms()
                .filter(OWLSubClassOfAxiom.class::isInstance)
                .map(OWLSubClassOfAxiom.class::cast)
                .flatMap(axiom -> Stream.of(axiom.getSubClass(), axiom.getSuperClass()));

        Stream<OWLClassExpression> eqAxiomExpressions = ontology.axioms()
                .filter(OWLEquivalentClassesAxiom.class::isInstance)
                .map(OWLEquivalentClassesAxiom.class::cast)
                .flatMap(OWLNaryClassAxiom::classExpressions);

        Stream.concat(subAxiomExpressions, eqAxiomExpressions).forEach(expression -> minimizeExpression(expression, ontology));

    }
    private static void minimizeExpression(OWLClassExpression expression, OWLOntology ontology) {
        System.out.println("---------------------------------------");
        System.out.println("Minimizing expression: " + expression);
        try {
            int origSize = expression.accept(new ClassExpressionSizeVisitor());
            System.out.println("Original size: " + origSize);

            if (origSize < 5) {
                SubsumptionLearningMinimalConcept minimalConceptGenerator = new SubsumptionLearningMinimalConcept(ontology, 0.02);
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
