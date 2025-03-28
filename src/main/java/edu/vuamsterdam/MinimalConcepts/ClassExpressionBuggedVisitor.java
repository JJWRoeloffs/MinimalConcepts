package edu.vuamsterdam.MinimalConcepts;

import org.semanticweb.owlapi.model.*;

public class ClassExpressionBuggedVisitor implements OWLClassExpressionVisitorEx<Boolean> {
    public ClassExpressionBuggedVisitor () {
        super();
    }

    @Override
    public Boolean visit(OWLClass ce) {
        return false;
    }

    @Override
    public Boolean visit(OWLObjectIntersectionOf ce) {
        if (ce.getOperandsAsList().size() <= 1)
            return true;
        return ce.operands().anyMatch(op -> op.accept(this));
    }

    @Override
    public Boolean visit(OWLObjectUnionOf ce) {
        if (ce.getOperandsAsList().size() <= 1)
            return true;
        return ce.operands().anyMatch(op -> op.accept(this));
    }

    @Override
    public Boolean visit(OWLObjectComplementOf ce) {
        // complement is not in scope of paper, but I'll define it as one larger.
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
        return ce.getFiller().accept(this);
    }

    @Override
    public Boolean visit(OWLObjectExactCardinality ce) {
        return ce.getFiller().accept(this);
    }

    @Override
    public Boolean visit(OWLObjectMaxCardinality ce) {
        return ce.getFiller().accept(this);
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
