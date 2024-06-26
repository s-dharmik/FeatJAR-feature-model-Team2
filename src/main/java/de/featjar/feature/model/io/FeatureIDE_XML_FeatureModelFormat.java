//package de.featjar.feature.model.io;
//
//import java.io.StringReader;
//
//import javax.xml.parsers.DocumentBuilder;
//import javax.xml.parsers.DocumentBuilderFactory;
//
//import org.w3c.dom.Document;
//import org.w3c.dom.Element;
//import org.w3c.dom.NodeList;
//import org.xml.sax.InputSource;
//
//import de.featjar.base.data.Result;
//import de.featjar.base.io.format.IFormat;
//import de.featjar.base.io.format.ParseException;
//import de.featjar.base.io.input.AInputMapper;
//import de.featjar.feature.model.FeatureModel;
//import de.featjar.feature.model.IFeature;
//import de.featjar.feature.model.IFeatureModel;
//import de.featjar.feature.model.IFeatureTree;
//
////
/////**
//// * @Dharmik
//// * Parses and writes feature models from and to FeatureIDE XML files.
//// */
//public class FeatureIDE_XML_FeatureModelFormat implements IFormat<IFeatureModel> {
//
////	This method is used to parse the feature model from the input file.
//	@Override
//	public Result<IFeatureModel> parse(AInputMapper inputMapper) {
//		try {
//			String content = inputMapper.get().text();
//			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
//			DocumentBuilder builder = factory.newDocumentBuilder();
//			Document doc = builder.parse(new InputSource(new StringReader(content)));
//
//			// let's assume that rootElement is the root element of FeatureTree
//			Element rootElement = doc.getDocumentElement();
//
//			IFeatureModel featureModel = new FeatureModel();
//			IFeature rootFeature = createFeature(featureModel, rootElement);
//			IFeatureTree tree = featureModel.mutate().addFeatureTreeRoot(rootFeature);
//
//			createFeatureTree(featureModel, rootElement, tree);
//
//			// Parsing constraints if any needed for the feature model, but here we do not have expressionparser class, 
////			NodeList constraintsList = doc.getElementsByTagName("constraints");
//			
//
//			return Result.of(featureModel);
//		} catch (Exception e) {
//			return Result.empty(e);
//		}
//	}
//
//	private IFeature createFeature(IFeatureModel featureModel, Element featureElement) throws ParseException {
//		IFeature feature = featureModel.mutate().addFeature(getName(featureElement));
//		// Set attributes such as abstract, type, etc.
//
//		String isAbstract = featureElement.getAttribute("abstract");
//		if (isAbstract != null && !isAbstract.isEmpty()) {
//			feature.mutate().setAbstract(Boolean.parseBoolean(isAbstract));
//		}
//
//		return feature;
//	}
//
//	private void createFeatureTree(IFeatureModel featureModel, Element parentElement, IFeatureTree tree)
//			throws ParseException {
//		// This code here is parsing the child features.
//
//		NodeList childNodeList = parentElement.getElementsByTagName("feature");
//		for (int i = 0; i < childNodeList.getLength(); i++) {
//			Element childElement = (Element) childNodeList.item(i);
//			IFeature childFeature = createFeature(featureModel, childElement);
//			tree.mutate().addChild(tree);
//
//			createFeatureTree(featureModel, childElement, childFeature.getFeatureTree().orElseThrow());
//
//		}
//
//	}
//
//	private String getName(Element featureElement) {
//		return featureElement.getAttribute("name");
//	}
//
//	// Implement other necessary methods and logic for parsing and creating the
//	// feature model tree
//
//	@Override
//	public boolean supportsParse() {
//		return true;
//	}
//
//	@Override
//	public boolean supportsSerialize() {
//		return false;
//	}
//
//	@Override
//	public String getFileExtension() {
//		return "xml";
//	}
//
//	@Override
//	public String getName() {
//		return "FeatureIDE XML";
//	}
//}


























//package de.featjar.feature.model.io;
//import java.io.StringReader;
//import java.io.StringWriter;
//import java.util.ArrayList;
//import java.util.List;
//
//import javax.xml.parsers.DocumentBuilder;
//import javax.xml.parsers.DocumentBuilderFactory;
//import javax.xml.transform.Transformer;
//import javax.xml.transform.TransformerFactory;
//import javax.xml.transform.dom.DOMSource;
//import javax.xml.transform.stream.StreamResult;
//
//import org.w3c.dom.Document;
//import org.w3c.dom.Element;
//import org.w3c.dom.NodeList;
//import org.xml.sax.InputSource;
//
//import de.featjar.base.FeatJAR;
//import de.featjar.base.data.Result;
//import de.featjar.base.io.format.IFormat;
//import de.featjar.base.io.format.ParseException;
//import de.featjar.base.io.input.AInputMapper;
//import de.featjar.feature.model.FeatureModel;
//import de.featjar.feature.model.IConstraint;
//import de.featjar.feature.model.IFeature;
//import de.featjar.feature.model.IFeatureModel;
//import de.featjar.formula.structure.formula.IFormula;
//import de.featjar.formula.structure.formula.connective.And;
//import de.featjar.formula.structure.formula.connective.Implies;
//import de.featjar.formula.structure.formula.connective.Not;
//import de.featjar.formula.structure.formula.connective.Or;
//import de.featjar.formula.structure.formula.predicate.Literal;
//import de.featjar.formula.structure.term.value.Variable;
//
//public class FeatureIDE_XML_FeatureModelFormat implements IFormat<IFeatureModel> {
//
//    @Override
//    public Result<String> serialize(IFeatureModel featureModel) {
//        try {
//            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
//            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
//
//            // Root elements
//            Document doc = docBuilder.newDocument();
//            Element rootElement = doc.createElement("featureModel");
//            doc.appendChild(rootElement);
//
//            // Features
//            Element featuresElement = doc.createElement("features");
//            rootElement.appendChild(featuresElement);
//            for (IFeature feature : featureModel.getFeatures()) {
//                Element featureElement = doc.createElement("feature");
//                featureElement.setAttribute("name", feature.getName().get());
//                featuresElement.appendChild(featureElement);
//            }
//
//            // Constraints
//            Element constraintsElement = doc.createElement("constraints");
//            rootElement.appendChild(constraintsElement);
//            for (IConstraint constraint : featureModel.getConstraints()) {
//                Element constraintElement = doc.createElement("constraint");
//                constraintElement.setTextContent(constraint.getFormula().toString());
//                constraintsElement.appendChild(constraintElement);
//            }
//
//            // Convert DOM to string
//            StringWriter writer = new StringWriter();
//            TransformerFactory transformerFactory = TransformerFactory.newInstance();
//            Transformer transformer = transformerFactory.newTransformer();
//            DOMSource source = new DOMSource(doc);
//            StreamResult result = new StreamResult(writer);
//            transformer.transform(source, result);
//
//            return Result.of(writer.toString());
//
//        } catch (Exception e) {
//            FeatJAR.log().error("Error serializing to FeatureIDE XML format", e);
//            return Result.empty(new ParseException(e.getMessage(), 0));
//        }
//    }
//
//    @Override
//    public Result<IFeatureModel> parse(AInputMapper inputMapper) {
//        try {
//            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
//            DocumentBuilder builder = factory.newDocumentBuilder();
//            Document doc = builder.parse(new InputSource(new StringReader(inputMapper.get().text()))); // Error here
//
//            IFeatureModel featureModel = new FeatureModel();
//
//            // Parse features
//            NodeList featureNodes = doc.getElementsByTagName("feature");
//            for (int i = 0; i < featureNodes.getLength(); i++) {
//                Element featureElement = (Element) featureNodes.item(i);
//                String featureName = featureElement.getAttribute("name");
//                featureModel.mutate().addFeature(featureName);
//            }
//
//            // Parse constraints
//            NodeList constraintNodes = doc.getElementsByTagName("constraint");
//            for (int i = 0; i < constraintNodes.getLength(); i++) {
//                Element constraintElement = (Element) constraintNodes.item(i);
//                String constraintText = constraintElement.getTextContent();
//                IFormula formula = parseFormula(constraintText, featureModel);
//                featureModel.mutate().addConstraint(formula);
//            }
//
//            return Result.of(featureModel);
//        } catch (Exception e) {
//        	e.printStackTrace();
//            FeatJAR.log().error("Error parsing FeatureIDE XML content", e);
//            return Result.empty(new ParseException(e.getMessage(), 0));
//        }
//    }
//
//    public IFormula parseFormula(String formulaText, IFeatureModel featureModel) {
//        formulaText = formulaText.trim();
//        if (formulaText.startsWith("not(")) {
//            IFormula childFormula = parseFormula(formulaText.substring(4, formulaText.length() - 1), featureModel);
//            return new Not(childFormula);
//        } else if (formulaText.startsWith("and(")) {
//            String innerText = formulaText.substring(4, formulaText.length() - 1);
//            return parseComplexFormula(innerText, "and", featureModel);
//        } else if (formulaText.startsWith("or(")) {
//            String innerText = formulaText.substring(3, formulaText.length() - 1);
//            return parseComplexFormula(innerText, "or", featureModel);
//        } else if (formulaText.startsWith("implies(")) {
//            String innerText = formulaText.substring(8, formulaText.length() - 1);
//            String[] parts = splitFormula(innerText, 2, ",");
//            IFormula left = parseFormula(parts[0].trim(), featureModel);
//            IFormula right = parseFormula(parts[1].trim(), featureModel);
//            return new Implies(left, right);
//        } else {
//            boolean isPositive = true;
//            if (formulaText.startsWith("-")) {
//                isPositive = false;
//                formulaText = formulaText.substring(1);
//            }
//            Variable variable = new Variable(formulaText);
//            if (isPositive) {
//                return new Literal(true, variable);
//            } else {
//                return new Not(new Literal(true, variable));
//            }
//        }
//    }
//
//    private IFormula parseComplexFormula(String innerText, String operator, IFeatureModel featureModel) {
//        String[] parts = splitFormula(innerText, -1, ",");
//        List<IFormula> formulas = new ArrayList<>();
//        for (String part : parts) {
//            formulas.add(parseFormula(part.trim(), featureModel));
//        }
//        if (operator.equals("and")) {
//            return new And(formulas.toArray(new IFormula[0]));
//        } else if (operator.equals("or")) {
//            return new Or(formulas.toArray(new IFormula[0]));
//        }
//        return null;
//    }
//
//    private String[] splitFormula(String text, int limit, String delimiter) {
//        List<String> parts = new ArrayList<>();
//        int depth = 0;
//        int start = 0;
//        for (int i = 0; i < text.length(); i++) {
//            char ch = text.charAt(i);
//            if (ch == '(') {
//                depth++;
//            } else if (ch == ')') {
//                depth--;
//            } else if (ch == ',' && depth == 0) {
//                parts.add(text.substring(start, i));
//                start = i + 1;
//            }
//        }
//        parts.add(text.substring(start));
//        return parts.toArray(new String[0]);
//    }
//
//    @Override
//    public boolean supportsSerialize() {
//        return true;
//    }
//
//    @Override
//    public boolean supportsParse() {
//        return true;
//    }
//
//    @Override
//    public String getFileExtension() {
//        return "xml";
//    }
//
//    @Override
//    public String getName() {
//        return "FeatureIDE XML Feature Model Format";
//    }
//}


package de.featjar.feature.model.io;

import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import de.featjar.base.data.Result;
import de.featjar.base.io.format.IFormat;
import de.featjar.base.io.format.ParseException;
import de.featjar.base.io.input.AInputMapper;
import de.featjar.feature.model.FeatureModel;
import de.featjar.feature.model.IConstraint;
import de.featjar.feature.model.IFeature;
import de.featjar.feature.model.IFeatureModel;
import de.featjar.formula.structure.formula.IFormula;
import de.featjar.formula.structure.formula.predicate.Literal;
import de.featjar.formula.structure.term.value.Variable;

public class FeatureIDE_XML_FeatureModelFormat implements IFormat<IFeatureModel> {

    @Override
    public Result<String> serialize(IFeatureModel featureModel) {
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.newDocument();

            Element rootElement = doc.createElement("FeatureModel");
            doc.appendChild(rootElement);

            for (IFeature feature : featureModel.getFeatures()) {
                Element featureElement = doc.createElement("Feature");
                featureElement.setAttribute("name", feature.getName().get());
                rootElement.appendChild(featureElement);
            }

            for (IConstraint constraint : featureModel.getConstraints()) {
                Element constraintElement = doc.createElement("Constraint");
                constraintElement.setTextContent(constraint.getFormula().toString());
                rootElement.appendChild(constraintElement);
            }

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            StringWriter writer = new StringWriter();
            StreamResult result = new StreamResult(writer);
            transformer.transform(source, result);

            return Result.of(writer.toString());
        } catch (Exception e) {
            return Result.empty(e);
        }
    }

    @Override
    public Result<IFeatureModel> parse(AInputMapper inputMapper) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(inputMapper.get().getInputStream());

            FeatureModel featureModel = new FeatureModel();
            NodeList featureNodes = doc.getElementsByTagName("Feature");
            for (int i = 0; i < featureNodes.getLength(); i++) {
                Element featureElement = (Element) featureNodes.item(i);
                String featureName = featureElement.getAttribute("name");
                featureModel.addFeature(featureName);
            }

            NodeList constraintNodes = doc.getElementsByTagName("Constraint");
            for (int i = 0; i < constraintNodes.getLength(); i++) {
                Element constraintElement = (Element) constraintNodes.item(i);
                String formulaText = constraintElement.getTextContent();
                // Assuming a simple conversion from text to formula for demonstration
                IFormula formula = new Literal(true, new Variable(formulaText));
                
                featureModel.addConstraint(formula);
            }

            return Result.of(featureModel);
        } catch (Exception e) {
            return Result.empty(new ParseException(e.getMessage(), 0));
        }
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
        return "xml";
    }

    @Override
    public String getName() {
        return "XML Feature Model Format";
    }
}
