package edu.vuamsterdam.MinimalConcepts;

import org.semanticweb.owlapi.model.*;

import java.util.stream.*;


public class ClassExpressionAllComplexVisitor implements OWLClassExpressionVisitorEx<Stream<OWLClassExpression>> {
    public ClassExpressionAllComplexVisitor() {
        super();
    }

    @Override
    public Stream<OWLClassExpression> visit(OWLClass ce) {
        return Stream.empty();
    }

    @Override
    public Stream<OWLClassExpression> visit(OWLObjectIntersectionOf ce) {
        return Stream.concat(Stream.of(ce), ce.operands().flatMap(op -> op.accept(this)));
    }

    @Override
    public Stream<OWLClassExpression> visit(OWLObjectUnionOf ce) {
        return Stream.concat(Stream.of(ce), ce.operands().flatMap(op -> op.accept(this)));
    }

    @Override
    public Stream<OWLClassExpression> visit(OWLObjectComplementOf ce) {
        return Stream.concat(Stream.of(ce), ce.getOperand().accept(this));
    }

    @Override
    public Stream<OWLClassExpression> visit(OWLObjectSomeValuesFrom ce) {
        return Stream.concat(Stream.of(ce), ce.getFiller().accept(this));
    }

    @Override
    public Stream<OWLClassExpression> visit(OWLObjectAllValuesFrom ce) {
        return Stream.concat(Stream.of(ce), ce.getFiller().accept(this) );
    }

    @Override
    public Stream<OWLClassExpression> visit(OWLObjectMinCardinality ce) {
        return Stream.concat(Stream.of(ce), ce.getFiller().accept(this) );
    }

    @Override
    public Stream<OWLClassExpression> visit(OWLObjectExactCardinality ce) {
        return Stream.concat(Stream.of(ce), ce.getFiller().accept(this) );
    }

    @Override
    public Stream<OWLClassExpression> visit(OWLObjectMaxCardinality ce) {
        return Stream.concat(Stream.of(ce), ce.getFiller().accept(this) );
    }

    @Override
    public Stream<OWLClassExpression> visit(OWLObjectHasValue ce) {
        return Stream.empty();
    }

    @Override
    public Stream<OWLClassExpression> visit(OWLObjectHasSelf ce) {
        return Stream.empty();
    }

    @Override
    public Stream<OWLClassExpression> visit(OWLDataSomeValuesFrom ce) {
        return Stream.empty();
    }

    @Override
    public Stream<OWLClassExpression> visit(OWLDataAllValuesFrom ce) {
        return Stream.empty();
    }

    @Override
    public Stream<OWLClassExpression> visit(OWLDataHasValue ce) {
        return Stream.empty();
    }

    @Override
    public Stream<OWLClassExpression> visit(OWLDataMinCardinality ce) {
        return Stream.empty();
    }

    @Override
    public Stream<OWLClassExpression> visit(OWLDataExactCardinality ce) {
        return Stream.empty();
    }

    @Override
    public Stream<OWLClassExpression> visit(OWLDataMaxCardinality ce) {
        return Stream.empty();
    }
}
