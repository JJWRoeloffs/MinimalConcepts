package edu.vuamsterdam.MinimalConcepts;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.io.OWLXMLOntologyFormat;

import java.io.File;
import java.util.stream.*;
import java.util.*;

import forgetting.Fame;

public class FameWrapper {
    public static void main(String[] args) {
        try {
            processOntology(args[0]);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void processOntology(String filepath) throws Exception {
        File file = new File("FAME_ontology.owl");

        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLOntology ontology = manager.loadOntologyFromOntologyDocument(new File(filepath));

        System.out.println(ontology.getAxioms().size());

        Random random = new Random(42);

        Fame fame = new Fame();
//        Set<OWLObjectProperty> relationSignature = ontology.getObjectPropertiesInSignature().stream().filter(x -> random.nextDouble() < 0.8).collect(Collectors.toSet());
        Set<OWLObjectProperty> relationSignature = new HashSet<>();
        // Set<OWLClass> classSignature = ontology.getClassesInSignature().stream().filter(x -> random.nextDouble() < 0.1).collect(Collectors.toSet());
        Set<OWLClass> classSignature = new HashSet<>((Collections.singleton(ontology.getClassesInSignature().stream().findFirst().get())));
        OWLOntology subOntology = fame.FameRC(relationSignature, classSignature, ontology);
        //subOntology.getAxioms().forEach(System.out::println);
        System.out.println(ontology.getAxioms().size());

        manager.saveOntology(subOntology, new OWLXMLOntologyFormat(), IRI.create(file.toURI()));
        System.out.println("Saved to " + file);
    }
}
