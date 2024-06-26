package de.featjar.feature.model.io;

import java.util.HashMap;
import java.util.Map;

import de.featjar.base.data.Result;
import de.featjar.base.io.format.IFormat;
import de.featjar.base.io.format.ParseException;
import de.featjar.base.io.input.AInputMapper;
import de.featjar.feature.model.FeatureModel;
import de.featjar.feature.model.IConstraint;
import de.featjar.feature.model.IFeature;
import de.featjar.feature.model.IFeatureModel;
import de.featjar.formula.io.dimacs.FormulaDimacsParser;
import de.featjar.formula.structure.IExpression;
import de.featjar.formula.structure.formula.IFormula;
import de.featjar.formula.structure.formula.connective.Or;
import de.featjar.formula.structure.formula.predicate.Literal;
import de.featjar.formula.structure.term.value.Variable;

public class DimacsFeatureModelFormat implements IFormat<IFeatureModel> {

    @Override
    public Result<String> serialize(IFeatureModel featureModel) {
        // Convert the feature model into a DIMACS CNF representation
        StringBuilder sb = new StringBuilder();
        Map<String, Integer> variableMap = new HashMap<>();
        int variableCount = 1;
        
        // Serialize features
        for (IFeature feature : featureModel.getFeatures()) {
            if (!variableMap.containsKey(feature)) {
                variableMap.put(getFileExtension(), variableCount++);
            }
        }
        
        // Serialize constraints
        for (IConstraint constraint : featureModel.getConstraints()) {
            writeClause(sb, constraint, variableMap);
        }

        sb.insert(0, "p cnf " + (variableCount - 1) + " " + featureModel.getConstraints().size() + "\n");
        return Result.of(sb.toString());
    }

    private void writeClause(StringBuilder sb, IConstraint constraint, Map<String, Integer> variableMap) {
        if (constraint instanceof Or) {
            Or or = (Or) constraint;
            for (IExpression child : or.getChildren()) {
                if (child instanceof Variable) {
                    Variable var = (Variable) child;
                    sb.append(variableMap.get(var.getName())).append(" ");
                }
            }
            sb.append("0\n");
        }
    }

    @Override
    public Result<IFeatureModel> parse(AInputMapper inputMapper) {
        FormulaDimacsParser parser = new FormulaDimacsParser();
        try {
        	IFormula formula = parser.parse(inputMapper.get().getNonEmptyLineIterator());
            IFeatureModel featureModel = convertToFeatureModel(formula);
            return Result.of(featureModel);
        } catch (Exception e) {
            return Result.empty(new ParseException(e.getMessage(), 0));
        }
    }

    private IFeatureModel convertToFeatureModel(IFormula formula) {
        FeatureModel featureModel = new FeatureModel();
        Map<Integer, IFeature> featureMap = new HashMap<>();
        
        // Assume each clause represents a constraint or feature relationship
        for (IExpression clause : formula.getChildren()) {
            if (clause instanceof Or) {
                Or orClause = (Or) clause;
                for (IExpression expr : orClause.getChildren()) {
                    if (expr instanceof Literal) {
                        Literal literal = (Literal) expr;
                        int varIndex = Integer.parseInt(literal.getName());
                        boolean isPositive = literal.isPositive();

                        // Create or get the feature
                        IFeature feature = featureMap.computeIfAbsent(varIndex, idx -> featureModel.addFeature("Feature" + idx));
                        
                        // Add to the feature model
                        if (isPositive) {
                            featureModel.addFeature(getName());
                        } else {
                            // Handle negative literals as constraints or special cases
                            featureModel.addConstraint((IFormula) feature);
                        }
                    }
                }
            }
        }

        return featureModel;
    }

    @Override
    public boolean supportsSerialize() {
        return true;
    }

    @Override
    public boolean supportsParse() {
        return true;
    }

    @Override
    public String getFileExtension() {
        return "dimacs";
    }

    @Override
    public String getName() {
        return "DIMACS Feature Model Format";
    }
}