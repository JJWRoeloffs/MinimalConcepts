package edu.vuamsterdam.MinimalConcepts;

import org.semanticweb.owlapi.model.*;

import java.util.*;

public class OWLExpressionReplacer {
    private final OWLDataFactory factory;

    public OWLExpressionReplacer(OWLDataFactory factory) {
        this.factory = factory;
    }

    public Set<OWLClassExpression> generateAllReplacements(OWLClassExpression expr, Set<OWLClass> replacements) {
        Set<OWLClassExpression> result = new HashSet<>(replacements);
        replaceRecursive(expr, replacements, result);
        return result;
    }

    private void replaceRecursive(OWLClassExpression expr, Set<OWLClass> replacements, Set<OWLClassExpression> result) {
        if (expr instanceof OWLObjectIntersectionOf andExpr) {
            for (OWLClassExpression operand : andExpr.getOperands()) {
                if (!operand.isClassExpressionLiteral()) {
                    for (OWLClass replacement : replacements) {
                        Set<OWLClassExpression> newOperands = new HashSet<>();
                        for (OWLClassExpression op : andExpr.getOperands()) {
                            newOperands.add(op.equals(operand) ? replacement : op);
                        }
                        OWLClassExpression newExpr = factory.getOWLObjectIntersectionOf(newOperands);
                        result.add(newExpr);
                        replaceRecursive(newExpr, replacements, result);
                    }
                }
            }
        } else if (expr instanceof OWLObjectUnionOf orExpr) {
                for (OWLClassExpression operand : orExpr.getOperands()) {
                    if (!operand.isClassExpressionLiteral()) {
                        for (OWLClass replacement : replacements) {
                            Set<OWLClassExpression> newOperands = new HashSet<>();
                            for (OWLClassExpression op : orExpr.getOperands()) {
                                newOperands.add(op.equals(operand) ? replacement : op);
                            }
                            OWLClassExpression newExpr = factory.getOWLObjectIntersectionOf(newOperands);
                            result.add(newExpr);
                            replaceRecursive(newExpr, replacements, result);
                        }
                    }
                }
        } else if (expr instanceof OWLObjectSomeValuesFrom someExpr) {
            if (!someExpr.getFiller().isClassExpressionLiteral()) {
                for (OWLClass replacement : replacements) {
                    OWLClassExpression newExpr = factory.getOWLObjectSomeValuesFrom(someExpr.getProperty(), replacement);
                    result.add(newExpr);
                    replaceRecursive(newExpr, replacements, result);
                }
            }
        } else if (expr instanceof OWLObjectAllValuesFrom allExpr) {
            if (!allExpr.getFiller().isClassExpressionLiteral()) {
                for (OWLClass replacement : replacements) {
                    OWLClassExpression newExpr = factory.getOWLObjectSomeValuesFrom(allExpr.getProperty(), replacement);
                    result.add(newExpr);
                    replaceRecursive(newExpr, replacements, result);
                }
            }
        }
    }
}