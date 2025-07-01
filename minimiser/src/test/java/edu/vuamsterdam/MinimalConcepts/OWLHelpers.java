package edu.vuamsterdam.MinimalConcepts;

import org.semanticweb.owlapi.model.*;

// factory.getOWLClass(IRI.create("http://www.example.org/ontology#{name}")) might seem like the better option
// But I have, by now, spent more than a day debugging a typo in a URI that caused a test to fail, so I prefer this
// because it throws in case the specified class does not exist inside the ontology.
public class OWLHelpers {
    public static OWLClass getClassByShortName(OWLOntology ontology, String name) {
        return ontology.classesInSignature()
                .filter(cls -> cls.getIRI().getShortForm().equals(name))
                .findAny()
                .orElseThrow();
    }

    public static OWLObjectProperty getPropertyByShortName(OWLOntology ontology, String name) {
        return ontology.objectPropertiesInSignature()
                .filter(cls -> cls.getIRI().getShortForm().equals(name))
                .findAny()
                .orElseThrow();
    }
}
