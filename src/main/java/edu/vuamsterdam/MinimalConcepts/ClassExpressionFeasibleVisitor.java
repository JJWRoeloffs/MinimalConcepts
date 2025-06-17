package edu.vuamsterdam.MinimalConcepts;

import org.semanticweb.owlapi.model.*;

public class ClassExpressionFeasibleVisitor implements OWLClassExpressionVisitorEx<Boolean> {
    public ClassExpressionFeasibleVisitor() {
        super();
    }

    @Override
    public Boolean visit(OWLClass ce) {
        return true;
    }

    @Override
    public Boolean visit(OWLObjectIntersectionOf ce) {
        return ce.operands().anyMatch(op -> op.accept(this));
    }

    @Override
    public Boolean visit(OWLObjectUnionOf ce) {
        return ce.operands().anyMatch(op -> op.accept(this));
    }

    @Override
    public Boolean visit(OWLObjectComplementOf ce) {
        return ce.getOperand().accept(this);
    }

    @Override
    public Boolean visit(OWLObjectSomeValuesFrom ce) {
        return ce.getFiller().accept(this);
    }

    @Override
    public Boolean visit(OWLObjectAllValuesFrom ce) {
        return ce.getFiller().accept(this);
    }

    @Override
    public Boolean visit(OWLObjectMinCardinality ce) {
        return false;
    }

    @Override
    public Boolean visit(OWLObjectExactCardinality ce) {
        return false;
    }

    @Override
    public Boolean visit(OWLObjectMaxCardinality ce) {
        return false;
    }

    @Override
    public Boolean visit(OWLObjectHasValue ce) {
        return false;
    }

    @Override
    public Boolean visit(OWLObjectHasSelf ce) {
        return false;
    }

    @Override
    public Boolean visit(OWLDataSomeValuesFrom ce) {
        return false;
    }

    @Override
    public Boolean visit(OWLDataAllValuesFrom ce) {
        return false;
    }

    @Override
    public Boolean visit(OWLDataHasValue ce) {
        return false;
    }

    @Override
    public Boolean visit(OWLDataMinCardinality ce) {
        return false;
    }

    @Override
    public Boolean visit(OWLDataExactCardinality ce) {
        return false;
    }

    @Override
    public Boolean visit(OWLDataMaxCardinality ce) {
        return false;
    }
}
