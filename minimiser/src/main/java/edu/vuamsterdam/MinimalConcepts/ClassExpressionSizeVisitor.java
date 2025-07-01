package edu.vuamsterdam.MinimalConcepts;

import org.semanticweb.owlapi.model.*;

public class ClassExpressionSizeVisitor implements OWLClassExpressionVisitorEx<Integer> {
    public ClassExpressionSizeVisitor() {
        super();
    }

    @Override
    public Integer visit(OWLClass ce) {
        return 1;
    }

    @Override
    public Integer visit(OWLObjectIntersectionOf ce) {
        return ce.operands().mapToInt(op -> op.accept(this)).sum();
    }

    @Override
    public Integer visit(OWLObjectUnionOf ce) {
        return ce.operands().mapToInt(op -> op.accept(this)).sum();
    }

    @Override
    public Integer visit(OWLObjectComplementOf ce) {
        // complement is not in scope of paper, but I'll define it as one larger.
        return ce.getOperand().accept(this) + 1;
    }

    @Override
    public Integer visit(OWLObjectSomeValuesFrom ce) {
        return ce.getFiller().accept(this) + 1;
    }

    @Override
    public Integer visit(OWLObjectAllValuesFrom ce) {
        return ce.getFiller().accept(this) + 1;
    }

    @Override
    public Integer visit(OWLObjectMinCardinality ce) {
        return ce.getFiller().accept(this) + 1;
    }

    @Override
    public Integer visit(OWLObjectExactCardinality ce) {
        return ce.getFiller().accept(this) + 1;
    }

    @Override
    public Integer visit(OWLObjectMaxCardinality ce) {
        return ce.getFiller().accept(this) + 1;
    }

    @Override
    public Integer visit(OWLObjectHasValue ce) {
        return 1;
    }

    @Override
    public Integer visit(OWLObjectHasSelf ce) {
        return 1;
    }

    @Override
    public Integer visit(OWLDataSomeValuesFrom ce) {
        return 1;
    }

    @Override
    public Integer visit(OWLDataAllValuesFrom ce) {
        return 1;
    }

    @Override
    public Integer visit(OWLDataHasValue ce) {
        return 1;
    }

    @Override
    public Integer visit(OWLDataMinCardinality ce) {
        return 1;
    }

    @Override
    public Integer visit(OWLDataExactCardinality ce) {
        return 1;
    }

    @Override
    public Integer visit(OWLDataMaxCardinality ce) {
        return 1;
    }
}
