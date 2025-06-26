package edu.vuamsterdam.MinimalConcepts;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;

import java.io.File;
import java.util.stream.*;
import java.util.*;

public class MinimizableCounter {

    public static void main(String[] args) {
        String filepath = args[0];
        try {
            processOntology(filepath);
        } catch (Throwable e) {
            GhettoLogger.logHardCrashed(filepath, e.toString());
        }
    }

    public static void processOntology(String filepath) throws Exception {
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLOntology ontology = manager.loadOntologyFromOntologyDocument(new File(filepath));

        GhettoLogger.logSize(filepath, getTBoxSize(ontology), getNrMinimizable(ontology));
    }
    private static int getNrMinimizable(OWLOntology ontology) {
        Stream<OWLClassExpression> subAxiomExpressions = ontology.axioms()
                .filter(OWLSubClassOfAxiom.class::isInstance)
                .map(OWLSubClassOfAxiom.class::cast)
                .flatMap(axiom -> Stream.of(axiom.getSubClass(), axiom.getSuperClass()));

        Stream<OWLClassExpression> eqAxiomExpressions = ontology.axioms()
                .filter(OWLEquivalentClassesAxiom.class::isInstance)
                .map(OWLEquivalentClassesAxiom.class::cast)
                .filter(x -> x.classExpressions().noneMatch(OWLClassExpression::isClassExpressionLiteral))
                .flatMap(OWLNaryClassAxiom::classExpressions);

        return Stream.concat(subAxiomExpressions, eqAxiomExpressions).toList().size();
    }

    private static int getTBoxSize(OWLOntology ontology) {
        Set<AxiomType<?>> tboxAxiomTypes = Set.of(
                AxiomType.SUBCLASS_OF,
                AxiomType.EQUIVALENT_CLASSES,
                AxiomType.DISJOINT_CLASSES,
                AxiomType.DISJOINT_UNION,
                AxiomType.OBJECT_PROPERTY_DOMAIN,
                AxiomType.OBJECT_PROPERTY_RANGE,
                AxiomType.DATA_PROPERTY_DOMAIN,
                AxiomType.DATA_PROPERTY_RANGE,
                AxiomType.FUNCTIONAL_OBJECT_PROPERTY,
                AxiomType.FUNCTIONAL_DATA_PROPERTY,
                AxiomType.INVERSE_OBJECT_PROPERTIES,
                AxiomType.TRANSITIVE_OBJECT_PROPERTY,
                AxiomType.SYMMETRIC_OBJECT_PROPERTY,
                AxiomType.ASYMMETRIC_OBJECT_PROPERTY,
                AxiomType.REFLEXIVE_OBJECT_PROPERTY,
                AxiomType.IRREFLEXIVE_OBJECT_PROPERTY
        );

        return (int) ontology.getAxioms().stream()
                .filter(ax -> tboxAxiomTypes.contains(ax.getAxiomType()))
                .count();
    }
}