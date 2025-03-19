package edu.vuamsterdam.MinimalConcepts;

import org.semanticweb.owlapi.model.OWLClassExpression;

import java.util.Optional;

public interface MinimalConcept {
    /*
    * returns Some(OWLClassExpression) that is smaller than the original,
    * or none if None can be found.
    *
    * @return smaller OWLClassExpression or none:
    */
    Optional<OWLClassExpression> getMinimalConcept(OWLClassExpression base);
}
