package de.featjar.feature.model.io;

import java.util.HashMap;
import java.util.Map;

import de.featjar.base.data.Result;
import de.featjar.base.io.format.IFormat;
import de.featjar.base.io.format.ParseException;
import de.featjar.base.io.input.AInputMapper;
import de.featjar.feature.model.IFeatureModel;
import de.featjar.formula.io.dimacs.FormulaDimacsParser;
import de.featjar.formula.structure.formula.IFormula;
import de.featjar.formula.structure.formula.connective.Or;
import de.featjar.formula.structure.term.value.Variable;

public class DimacsFeatureModelFormat implements IFormat<IFeatureModel> {

    @Override
    public Result<String> serialize(IFeatureModel featureModel) {
        // Convert the feature model into a DIMACS CNF representation
        StringBuilder sb = new StringBuilder();
        Map<String, Integer> variableMap = new HashMap<>();
        int variableCount = 1;
        
        // Serialize features
        for (String feature : featureModel.getFeatures()) {
            if (!variableMap.containsKey(feature)) {
                variableMap.put(feature, variableCount++);
            }
        }
        
        // Serialize constraints
        for (IFormula constraint : featureModel.getConstraints()) {
            writeClause(sb, constraint, variableMap);
        }

        sb.insert(0, "p cnf " + (variableCount - 1) + " " + featureModel.getConstraints().size() + "\n");
        return Result.of(sb.toString());
    }

    private void writeClause(StringBuilder sb, IFormula formula, Map<String, Integer> variableMap) {
        if (formula instanceof Or) {
            Or or = (Or) formula;
            for (IFormula child : or.getChildren()) {
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
        	
            IFormula formula = parser.parse(inputMapper.get().getNonEmptyLineIterator()).orElseThrow();
            IFeatureModel featureModel = convertToFeatureModel(formula);
            return Result.of(featureModel);
        } catch (Exception e) {
            return Result.empty(new ParseException(e.getMessage(), 0));
        }
    }

    private IFeatureModel convertToFeatureModel(IFormula formula) {
        // Convert the formula into a feature model
        // This requires creating features and constraints from the parsed formula
        // Implementation depends on how the IFeatureModel and related classes are structured
        return null;
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