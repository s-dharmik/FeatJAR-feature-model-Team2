package de.featjar.feature.model.io;

import java.util.*;
import de.featjar.base.FeatJAR;
import de.featjar.base.data.Result;
import de.featjar.base.io.format.IFormat;
import de.featjar.base.io.format.ParseException;
import de.featjar.base.io.input.AInputMapper;
import de.featjar.feature.model.*;
import de.featjar.formula.structure.*;
import de.featjar.formula.structure.formula.*;
import de.featjar.formula.structure.formula.connective.*;
import de.featjar.formula.structure.formula.predicate.Literal;
import de.featjar.formula.structure.term.value.Variable;

public class DimacsFeatureModelFormat implements IFormat<IFeatureModel> {

    private List<String> originalComments = new ArrayList<>();
    private int originalVariableCount = 0;
    private int originalClauseCount = 0;

    @Override
    public Result<String> serialize(IFeatureModel featureModel) {
        StringBuilder sb = new StringBuilder();
        Map<String, Integer> variableMap = new HashMap<>();
        int variableCount = 1;

        for (IFeature feature : featureModel.getFeatures()) {
            Result<String> featureNameResult = feature.getName();
            if (featureNameResult.isPresent()) {
                String featureName = featureNameResult.get();
                if (!variableMap.containsKey(featureName)) {
                    variableMap.put(featureName, variableCount);
                    variableCount++;
                }
            }
        }

        // Add original comments
        for (String comment : originalComments) {
            sb.append(comment).append("\n");
        }

        // Respect the original variable and clause counts
        sb.append("p cnf ").append(originalVariableCount).append(" ").append(originalClauseCount).append("\n");

        for (IConstraint constraint : featureModel.getConstraints()) {
            writeClause(sb, constraint.getFormula(), variableMap);
        }

        return Result.of(sb.toString());
    }

    private void writeClause(StringBuilder sb, IFormula formula, Map<String, Integer> variableMap) {
        if (formula instanceof Or) {
            writeOrClause(sb, (Or) formula, variableMap);
        } else if (formula instanceof And) {
            for (IExpression child : ((And) formula).getChildren()) {
                if (child instanceof IFormula) {
                    writeClause(sb, (IFormula) child, variableMap);
                }
            }
        } else if (formula instanceof Not) {
            IExpression child = ((Not) formula).getChildren().get(0);
            if (child instanceof Literal) {
                Literal literal = (Literal) child;
                String literalName = ((Variable) literal.getExpression()).getName();
                Integer varIndex = variableMap.get(literalName);
                if (varIndex != null) {
                    sb.append("-").append(varIndex).append(" 0\n");
                }
            }
        } else if (formula instanceof Implies) {
            Implies implies = (Implies) formula;
            IExpression left = implies.getLeft();
            IExpression right = implies.getRight();
            Or orClause = new Or();
            if (left instanceof Literal) {
                Literal literal = (Literal) left;
                orClause.addChild(new Literal(new Variable(((Variable) literal.getExpression()).getName()), !literal.isPositive()));
            }
            if (right instanceof Literal) {
                orClause.addChild(right);
            }
            writeOrClause(sb, orClause, variableMap);
        }
    }

    private void writeOrClause(StringBuilder sb, Or orClause, Map<String, Integer> variableMap) {
        for (IExpression child : orClause.getChildren()) {
            if (child instanceof Literal) {
                Literal literal = (Literal) child;
                String literalName = ((Variable) literal.getExpression()).getName();
                Integer varIndex = variableMap.get(literalName);
                if (varIndex != null) {
                    sb.append(literal.isPositive() ? "" : "-").append(varIndex).append(" ");
                }
            }
        }
        sb.append("0\n");
    }

    @Override
    public Result<IFeatureModel> parse(AInputMapper inputMapper) {
        try {
            FeatJAR.log().debug("Parsing DIMACS content...");
            Scanner scanner = new Scanner(inputMapper.get().get());
            IFormula formula = parseDimacs(scanner);
            FeatJAR.log().debug("Parsed formula: " + formula);
            IFeatureModel featureModel = convertToFeatureModel(formula);
            return Result.of(featureModel);
        } catch (Exception e) {
            FeatJAR.log().error("Error parsing DIMACS content", e);
            return Result.empty(new ParseException(e.getMessage(), 0));
        }
    }

    private IFormula parseDimacs(Scanner scanner) {
        And formula = new And();

        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (line.startsWith("c ")) {
                originalComments.add(line);
            } else if (line.startsWith("p cnf")) {
                String[] parts = line.split(" ");
                if (parts.length == 4) {
                    originalVariableCount = Integer.parseInt(parts[2]);
                    originalClauseCount = Integer.parseInt(parts[3]);
                }
            } else if (originalClauseCount > 0) { // Only process clauses if originalClauseCount > 0
                Scanner lineScanner = new Scanner(line);
                Or clause = new Or();
                while (lineScanner.hasNextInt()) {
                    int literal = lineScanner.nextInt();
                    if (literal == 0) break;
                    boolean isPositive = literal > 0;
                    String variableName = Integer.toString(Math.abs(literal));
                    Literal lit = new Literal(new Variable(variableName), isPositive);
                    clause.addChild(lit);
                }
                lineScanner.close();
                formula.addChild(clause);
            }
        }

        return formula;
    }

    private IFeatureModel convertToFeatureModel(IFormula formula) {
        FeatureModel featureModel = new FeatureModel();
        Map<Integer, IFeature> featureMap = new HashMap<>();

        for (String comment : originalComments) {
            if (comment.startsWith("c ")) {
                String[] parts = comment.split(" ");
                if (parts.length == 3) {
                    try {
                        int varIndex = Integer.parseInt(parts[1]);
                        String featureName = parts[2];
                        IFeature feature = featureModel.addFeature(featureName);
                        featureMap.put(varIndex, feature);
                    } catch (NumberFormatException e) {
                        FeatJAR.log().error("Failed to parse feature index from comment: " + comment, e);
                    }
                }
            }
        }

        if (originalClauseCount > 0) {
            for (IExpression clause : formula.getChildren()) {
                if (clause instanceof Or) {
                    Or orClause = (Or) clause;
                    List<Literal> literals = new ArrayList<>();
                    for (IExpression expr : orClause.getChildren()) {
                        if (expr instanceof Literal) {
                            Literal literal = (Literal) expr;
                            String variableName = ((Variable) literal.getExpression()).getName();
                            boolean isPositive = literal.isPositive();
                            int varIndex = Integer.parseInt(variableName);

                            IFeature feature = featureMap.get(varIndex);
                            if (feature == null) {
                                feature = featureModel.addFeature("Feature" + variableName);
                                featureMap.put(varIndex, feature);
                            }

                            literals.add(new Literal(new Variable(feature.getName().get()), isPositive));
                        }
                    }
                    if (!literals.isEmpty()) {
                        featureModel.addConstraint(new Constraint(featureModel, new Or(literals.toArray(new Literal[0]))));
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
