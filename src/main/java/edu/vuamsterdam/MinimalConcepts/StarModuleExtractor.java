package edu.vuamsterdam.MinimalConcepts;

import org.semanticweb.owlapi.model.*;
import uk.ac.manchester.cs.owlapi.modularity.*;

import java.util.*;

public class StarModuleExtractor {
    public static OWLOntology extractStarModule(OWLOntology ontology, OWLClassExpression expression) {
        OWLOntologyManager manager = ontology.getOWLOntologyManager();
        Set<OWLEntity> signature = expression.getSignature();

        SyntacticLocalityModuleExtractor extractor = new SyntacticLocalityModuleExtractor(
                manager, ontology, ModuleType.STAR
        );

        Set<OWLAxiom> moduleAxioms = extractor.extract(signature);

        // There should not be any errors, presuming there are no bugs in any of the used libraries.
        try {
            return manager.createOntology(moduleAxioms);
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
