package edu.vuamsterdam.MinimalConcepts;

import org.semanticweb.owlapi.model.*;


public class ClassExpressionReplacerVisitor implements OWLClassExpressionVisitorEx<OWLClassExpression> {
    private final OWLClassExpression what;
    private final OWLClassExpression with;
    private final OWLDataFactory factory;

    public ClassExpressionReplacerVisitor(OWLClassExpression what, OWLClassExpression with, OWLDataFactory factory) {
        super();
        this.what = what;
        this.with = with;
        this.factory = factory;
    }

    @Override
    public OWLClassExpression visit(OWLClass ce) {
        return ce.equals(what) ? with : ce ;
    }

    @Override
    public OWLClassExpression visit(OWLObjectIntersectionOf ce) {
        return ce.equals(what) ? with : factory.getOWLObjectIntersectionOf(ce.operands().map(ex -> ex.accept(this)));
    }

    @Override
    public OWLClassExpression visit(OWLObjectUnionOf ce) {
        return ce.equals(what) ? with : factory.getOWLObjectUnionOf(ce.operands().map(ex -> ex.accept(this)));
    }

    @Override
    public OWLClassExpression visit(OWLObjectComplementOf ce) {
        return ce.equals(what) ? with : factory.getOWLObjectComplementOf(ce.getOperand().accept(this));
    }

    @Override
    public OWLClassExpression visit(OWLObjectSomeValuesFrom ce) {
        return ce.equals(what) ? with : factory.getOWLObjectSomeValuesFrom(ce.getProperty(), ce.getFiller().accept(this));
    }

    @Override
    public OWLClassExpression visit(OWLObjectAllValuesFrom ce) {
        return ce.equals(what) ? with : factory.getOWLObjectAllValuesFrom(ce.getProperty(), ce.getFiller().accept(this));
    }

    @Override
    public OWLClassExpression visit(OWLObjectMinCardinality ce) {
        return ce.equals(what) ? with : factory.getOWLObjectMinCardinality(ce.getCardinality(), ce.getProperty(), ce.getFiller().accept(this));
    }

    @Override
    public OWLClassExpression visit(OWLObjectExactCardinality ce) {
        return ce.equals(what) ? with : factory.getOWLObjectExactCardinality(ce.getCardinality(), ce.getProperty(), ce.getFiller().accept(this));
    }

    @Override
    public OWLClassExpression visit(OWLObjectMaxCardinality ce) {
        return ce.equals(what) ? with : factory.getOWLObjectMaxCardinality(ce.getCardinality(), ce.getProperty(), ce.getFiller().accept(this));
    }

    @Override
    public OWLClassExpression visit(OWLObjectHasValue ce) {
        return ce.equals(what) ? with : ce;
    }

    @Override
    public OWLClassExpression visit(OWLObjectHasSelf ce) {
        return ce.equals(what) ? with : ce;
    }

    @Override
    public OWLClassExpression visit(OWLDataSomeValuesFrom ce) {
        return ce.equals(what) ? with : ce;
    }

    @Override
    public OWLClassExpression visit(OWLDataAllValuesFrom ce) {
        return ce.equals(what) ? with : ce;
    }

    @Override
    public OWLClassExpression visit(OWLDataHasValue ce) {
        return ce.equals(what) ? with : ce;
    }

    @Override
    public OWLClassExpression visit(OWLDataMinCardinality ce) {
        return ce.equals(what) ? with : ce;
    }

    @Override
    public OWLClassExpression visit(OWLDataExactCardinality ce) {
        return ce.equals(what) ? with : ce;
    }

    @Override
    public OWLClassExpression visit(OWLDataMaxCardinality ce) {
        return ce.equals(what) ? with : ce;
    }
}
