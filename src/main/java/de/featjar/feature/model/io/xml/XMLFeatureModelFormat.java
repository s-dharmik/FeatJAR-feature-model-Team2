/*
 * Copyright (C) 2024 FeatJAR-Development-Team
 *
 * This file is part of FeatJAR-feature-model.
 *
 * feature-model is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3.0 of the License,
 * or (at your option) any later version.
 *
 * feature-model is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with feature-model. If not, see <https://www.gnu.org/licenses/>.
 *
 * See <https://github.com/FeatureIDE/FeatJAR-feature-model> for further information.
 */
package de.featjar.feature.model.io.xml;

import de.featjar.base.FeatJAR;
import de.featjar.base.data.IAttribute;
import de.featjar.base.data.Maps;
import de.featjar.base.data.Problem;
import de.featjar.base.data.Result;
import de.featjar.base.data.identifier.IIdentifier;
import de.featjar.base.data.identifier.Identifiers;
import de.featjar.base.io.format.ParseException;
import de.featjar.base.io.input.AInputMapper;
import de.featjar.feature.model.Attributes;
import de.featjar.feature.model.FeatureModel;
import de.featjar.feature.model.IConstraint;
import de.featjar.feature.model.IFeature;
import de.featjar.feature.model.IFeatureModel;
import de.featjar.feature.model.IFeatureModelElement;
import de.featjar.feature.model.IFeatureTree;
import de.featjar.feature.model.io.AttributeIO;
import de.featjar.formula.io.xml.AXMLFeatureModelFormat;
import de.featjar.formula.structure.Expressions;
import de.featjar.formula.structure.IExpression;
import de.featjar.formula.structure.formula.IFormula;
import de.featjar.formula.structure.formula.connective.And;
import de.featjar.formula.structure.formula.connective.AtMost;
import de.featjar.formula.structure.formula.connective.Implies;
import de.featjar.formula.structure.formula.connective.Not;
import de.featjar.formula.structure.formula.connective.Or;
import de.featjar.formula.structure.formula.predicate.Equals;
import de.featjar.formula.structure.formula.predicate.Literal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

/**
 * Parses and writes feature models from and to FeatureIDE XML files.
 *
 * @author Sebastian Krieter
 * @author Elias Kuiter
 */
public class XMLFeatureModelFormat extends AXMLFeatureModelFormat<IFeatureModel, IFeatureTree, IConstraint> {
    public static final String NAMESPACE = XMLFeatureModelFormat.class.getCanonicalName();
    public static final String GRAPHICS_NAMESPACE = "<graphics>"; // TODO
    public static final String CALCULATIONS_NAMESPACE = "<calculations>"; // TODO
    protected static final String COMMENTS = "comments";
    protected static final String C = "c";
    protected static final String FEATURE_ORDER = "featureOrder";
    protected static final String USER_DEFINED = "userDefined";
    protected static final String KEY = "key";
    protected static final String VALUE = "value";
    protected static final String DATA_TYPE = "data-type";
    protected static final String NAMESPACE_TAG = "namespace";

    protected static final String PROPERTIES = "properties";
    protected static final String CALCULATIONS = "calculations";
    protected static final String CALCULATE_FEATURES = "Features";
    protected static final String CALCULATE_REDUNDANT = "Redundant";
    protected static final String CALCULATE_TAUTOLOGY = "Tautology";
    protected static final String CALCULATE_CONSTRAINTS = "Constraints";
    protected static final String CALCULATE_AUTO = "Auto";
    // TODO:
    // EXTENDED_FEATURE_MODEL, STRUCT, FEATURE_ORDER, CONSTRAINTS, CONSTRAINT, COLLAPSED, FEATURES,
    // CHOSEN_LAYOUT_ALGORITHM, NAME, FALSE, SHOW_COLLAPSED_CONSTRAINTS, LEGEND, LEGEND_AUTO_LAYOUT,
    // LEGEND_HIDDEN, SHOW_SHORT_NAMES, HORIZONTAL_LAYOUT, RULE, UNKNOWN, ATMOST1, ATTRIBUTE,
    // ATTRIBUTE_UNIT, ATTRIBUTE_TYPE, ATTRIBUTE_VALUE, ATTRIBUTE_RECURSIVE, ATTRIBUTE_CONFIGURABLE,

    protected IFeatureModel featureModel;
    protected LinkedHashMap<String, IIdentifier> nameToIdentifierMap;

    @Override
    public XMLFeatureModelFormat getInstance() {
        return new XMLFeatureModelFormat();
    }

    @Override
    public String getName() {
        return "FeatureIDE";
    }

    @Override
    public boolean supportsParse() {
        return true;
    }

    @Override
    public boolean supportsSerialize() {
        return false;
    }

    @Override
    public Result<IFeatureModel> parse(AInputMapper inputMapper, Supplier<IFeatureModel> supplier) {
        featureModel = supplier.get();
        return parse(inputMapper);
    }

    @Override
    public IFeatureModel parseDocument(Document document) throws ParseException {
        if (featureModel == null) featureModel = new FeatureModel(Identifiers.newCounterIdentifier());
        nameToIdentifierMap = Maps.empty();
        final Element featureModelElement = getDocumentElement(document, FEATURE_MODEL, EXT_FEATURE_MODEL);
        parseFeatureTree(getElement(featureModelElement, STRUCT));
        Result<Element> element = getElementResult(featureModelElement, CONSTRAINTS);
        if (element.isPresent()) parseConstraints(element.get());
        element = getElementResult(featureModelElement, COMMENTS);
        if (element.isPresent()) parseComments(element.get());
        element = getElementResult(featureModelElement, FEATURE_ORDER);
        if (element.isPresent()) parseFeatureOrder(List.of(element.get()));
        element = getElementResult(featureModelElement, PROPERTIES);
        if (element.isPresent()) parseFeatureModelProperties(element.get());
        element = getElementResult(featureModelElement, CALCULATIONS);
        element.ifPresent(this::parseCalculations);
        return featureModel;
    }

    protected Result<IFeature> getFeature(String name) {
        return Result.ofNullable(nameToIdentifierMap.get(name)).flatMap(featureModel::getFeature);
    }

    @Override
    public Pattern getInputHeaderPattern() {
        return inputHeaderPattern;
    }

    @Override
    protected IFeatureTree newFeatureLabel(
            String name, IFeatureTree parentFeatureLabel, boolean mandatory, boolean _abstract, boolean hidden)
            throws ParseException {
        IFeatureTree featureTree;
        IFeature feature = featureModel.mutate().addFeature(name);
        if (parentFeatureLabel == null) {
            // TODO cope with multiple roots
            featureTree = featureModel.mutate().addFeatureTreeRoot(feature);
        } else {
            featureTree = parentFeatureLabel.mutate().addFeatureBelow(feature);
        }
        feature.mutate().setAbstract(_abstract);
        feature.mutate().setHidden(hidden);
        if (mandatory) {
            featureTree.mutate().setMandatory();
        } else {
            featureTree.mutate().setOptional();
        }
        if (nameToIdentifierMap.get(name) != null) {
            throw new ParseException("Duplicate feature name!");
        }
        nameToIdentifierMap.put(name, feature.getIdentifier());
        return featureTree;
    }

    @Override
    protected void addAndGroup(IFeatureTree featureLabel, List<IFeatureTree> childFeatureLabels) {
        featureLabel.mutate().setAnd();
    }

    @Override
    protected void addOrGroup(IFeatureTree featureLabel, List<IFeatureTree> childFeatureLabels) {
        featureLabel.mutate().setOr();
    }

    @Override
    protected void addAlternativeGroup(IFeatureTree featureLabel, List<IFeatureTree> childFeatureLabels) {
        featureLabel.mutate().setAlternative();
    }

    @Override
    protected void addFeatureMetadata(IFeatureTree featureLabel, Element e) throws ParseException {
        switch (e.getNodeName()) {
            case DESCRIPTION:
                featureLabel.getFeature().mutate().setDescription(getDescription(e));
                break;
            case GRAPHICS:
                parseProperty(featureLabel.getFeature(), e, GRAPHICS_NAMESPACE);
                break;
            case PROPERTY:
                parseProperty(featureLabel.getFeature(), e, NAMESPACE);
                break;
        }
    }

    @Override
    protected IConstraint newConstraintLabel() {
        return featureModel.mutate().addConstraint(Expressions.True);
    }

    @Override
    protected void addConstraint(IConstraint constraintLabel, IFormula formula) {
        constraintLabel.mutate().setFormula(formula);
    }

    @Override
    protected void addConstraintMetadata(IConstraint constraintLabel, Element e) throws ParseException {
        switch (e.getNodeName()) {
            case DESCRIPTION:
                constraintLabel.mutate().setDescription(getDescription(e));
                break;
            case GRAPHICS:
                parseProperty(constraintLabel, e, GRAPHICS_NAMESPACE);
                break;
            case PROPERTY:
                parseProperty(constraintLabel, e, NAMESPACE);
                break;
            case TAGS:
                constraintLabel.mutate().setTags(getTags(e));
        }
    }

    protected void parseComments(Element element) throws ParseException {
        for (final Element e1 : getElements(element.getChildNodes())) {
            if (e1.getNodeName().equals(C)) {
                featureModel
                        .mutate()
                        .setDescription(featureModel.getDescription().orElse("") + "\n" + e1.getTextContent());
            } else {
                addParseProblem("Unknown comment attribute: " + e1.getNodeName(), e1, Problem.Severity.WARNING);
            }
        }
    }

    protected void parseFeatureOrder(List<Element> elements) throws ParseException {
        final ArrayList<String> order = new ArrayList<>(featureModel.getNumberOfFeatures());
        boolean userDefined = false;
        for (final Element e : elements) {
            if (e.hasAttributes()) {
                final NamedNodeMap nodeMap = e.getAttributes();
                for (int i = 0; i < nodeMap.getLength(); i++) {
                    final Node node = nodeMap.item(i);
                    final String attributeName = node.getNodeName();
                    final String attributeValue = node.getNodeValue();
                    if (attributeName.equals(USER_DEFINED)) {
                        userDefined = attributeValue.equals(TRUE);
                    } else if (attributeName.equals(NAME)) {
                        if (getFeature(attributeValue).isPresent()) {
                            order.add(attributeValue);
                        } else {
                            addParseProblem(
                                    "Feature \"" + attributeValue + "\" does not exists", e, Problem.Severity.ERROR);
                        }
                    } else {
                        addParseProblem("Unknown feature order attribute: " + attributeName, e, Problem.Severity.ERROR);
                    }
                }
            }
            if (e.hasChildNodes()) {
                parseFeatureOrder(getElements(e.getChildNodes()));
            }
        }
        // TODO Parse FeatureOrder
        if (!order.isEmpty()) {
            List<IFeature> featureList = order.stream()
                    .map(nameToIdentifierMap::get)
                    .map(featureModel::getFeature)
                    .map(Result::orElseThrow)
                    .collect(Collectors.toList());
            featureModel.mutate().setAttributeValue(Attributes.get("FeatureOrder", List.class), featureList);
        }
        featureModel.mutate().setAttributeValue(Attributes.get("HasFeatureOrder", Boolean.class), userDefined);
    }

    protected String getDescription(Node e) {
        String description = e.getTextContent();
        // NOTE: THe following code is used for backwards compatibility. It replaces
        // spaces and tabs that were added to the XML for indentation, but don't
        // belong to the actual description.
        if (description != null) {
            description = description.replaceAll("(\r\n|\r|\n)\\s*", "\n").replaceAll("\\A\n|\n\\Z", "");
        }
        return description;
    }

    protected LinkedHashSet<String> getTags(final Node e) {
        final String[] tagArray = e.getTextContent().split(",");
        return new LinkedHashSet<>(Arrays.asList(tagArray));
    }

    protected void parseProperty(IFeatureModelElement featureModelElement, Element e, String fallbackNamespace)
            throws ParseException {
        if (!e.hasAttribute(KEY) || !e.hasAttribute(VALUE)) {
            addParseProblem(
                    "Missing one of the required attributes: " + KEY + " or " + VALUE, e, Problem.Severity.WARNING);
        } else {
            String typeString = e.hasAttribute(DATA_TYPE) ? e.getAttribute(DATA_TYPE) : "string";
            final String namespace = e.hasAttribute(NAMESPACE_TAG) ? e.getAttribute(NAMESPACE_TAG) : fallbackNamespace;
            final String name = e.getAttribute(KEY);
            final String valueString = e.getAttribute(VALUE);
            parseProblems.addAll(AttributeIO.parseAndSetAttributeValue(
                    featureModelElement, namespace, name, typeString, valueString));
        }
    }

    protected void parseFeatureModelProperties(Element e) throws ParseException {
        for (final Element propertyElement : getElements(e.getChildNodes())) {
            final String nodeName = propertyElement.getNodeName();
            switch (nodeName) {
                case GRAPHICS:
                    parseProperty(featureModel, propertyElement, GRAPHICS_NAMESPACE);
                    break;
                case CALCULATIONS:
                    parseProperty(featureModel, propertyElement, CALCULATIONS_NAMESPACE);
                    break;
                case PROPERTY:
                    parseProperty(featureModel, propertyElement, NAMESPACE);
                    break;
            }
        }
    }

    protected void parseCalculations(Element e) {
        parseAttribute(e, CALCULATE_AUTO);
        parseAttribute(e, CALCULATE_CONSTRAINTS);
        parseAttribute(e, CALCULATE_FEATURES);
        parseAttribute(e, CALCULATE_REDUNDANT);
        parseAttribute(e, CALCULATE_TAUTOLOGY);
    }

    private void parseAttribute(final Element e, final String key) {
        if (e.hasAttribute(key)) {
            parseProblems.addAll(AttributeIO.parseAndSetAttributeValue(
                    featureModel, CALCULATIONS_NAMESPACE, key, "bool", e.getAttribute(key)));
        }
    }

    @Override
    public void writeDocument(IFeatureModel featureModel, Document doc) {
        this.featureModel = featureModel;
        nameToIdentifierMap = new LinkedHashMap<>();
        final Element root = doc.createElement(FEATURE_MODEL);
        doc.appendChild(root);

        writeProperties(doc, root);
        writeFeatures(doc, root);
        writeConstraints(doc, root);
        writeComments(doc, root);
        writeFeatureOrder(doc, root);
    }

    protected void writeProperties(Document doc, final Element root) {
        Optional<Map<IAttribute<?>, Object>> attributes = featureModel.getAttributes();
        if (attributes.isPresent()) {
            if (!attributes.get().isEmpty()) {
                final Element properties = doc.createElement(PROPERTIES);
                root.appendChild(properties);
                addProperties(doc, attributes.get(), properties);
            }
        }
    }

    protected void writeFeatures(Document doc, final Element root) {
        final Element struct = doc.createElement(STRUCT);
        root.appendChild(struct);
        writeFeatureTreeRec(doc, struct, featureModel.getRoots().get(0));
    }

    protected void writeConstraints(Document doc, final Element root) {
        if (!featureModel.getConstraints().isEmpty()) {
            final Element constraints = doc.createElement(CONSTRAINTS);
            root.appendChild(constraints);
            for (final IConstraint constraint : featureModel.getConstraints()) {
                Element rule;
                rule = doc.createElement(RULE);

                constraints.appendChild(rule);
                addDescription(doc, constraint.getDescription().orElse(null), rule);
                addProperties(doc, constraint.getAttributes().get(), rule);
                addTags(doc, constraint.getTags(), rule);
                createPropositionalConstraints(doc, rule, constraint.getFormula());
            }
        }
    }

    protected void writeComments(Document doc, final Element root) {
        if (featureModel
                .getAttributeValue(Attributes.get("Comments", String.class))
                .isPresent()) {
            final Element comments = doc.createElement(COMMENTS);
            root.appendChild(comments);
            final Element c = doc.createElement(C);
            comments.appendChild(c);
            final Text text = doc.createTextNode(featureModel
                    .getAttributeValue(Attributes.get("Comments", String.class))
                    .get());
            c.appendChild(text);
        }
    }

    protected void writeFeatureOrder(Document doc, final Element root) {
        if (featureModel
                .getAttributeValue(Attributes.get("HasFeatureOrder", Boolean.class))
                .valueEquals(true)) {
            final Element order = doc.createElement(FEATURE_ORDER);
            order.setAttribute(USER_DEFINED, "true");
            root.appendChild(order);
            List<?> featureOrderList = featureModel
                    .getAttributeValue(Attributes.get("FeatureOrder", List.class))
                    .get();

            if (featureOrderList.isEmpty()) {
                featureOrderList = featureModel
                        .getFeatureTreeStream()
                        .map(t -> t.getFeature().getName().get())
                        .collect(Collectors.toList());
            }

            for (final Object featureName : featureOrderList) {
                final Element feature = doc.createElement(FEATURE);
                feature.setAttribute(NAME, String.valueOf(featureName));
                order.appendChild(feature);
            }
        }
    }

    /**
     * Inserts the tags concerning propositional constraints into the DOM document representation
     *
     * @param doc
     * @param node Parent node for the propositional nodes
     */
    protected void createPropositionalConstraints(Document doc, Element xmlNode, IFormula node) {
        if (node == null) {
            return;
        }

        final Element op;
        if (node instanceof Literal) {
            final Literal literal = (Literal) node;
            if (!literal.isPositive()) {
                final Element opNot = doc.createElement(NOT);
                xmlNode.appendChild(opNot);
                xmlNode = opNot;
            }
            op = doc.createElement(VAR);
            op.appendChild(doc.createTextNode(literal.getName()));
            xmlNode.appendChild(op);
            return;
        } else if (node instanceof Or) {
            op = doc.createElement(DISJ);
        } else if (node instanceof Equals) {
            op = doc.createElement(EQ);
        } else if (node instanceof Implies) {
            op = doc.createElement(IMP);
        } else if (node instanceof And) {
            op = doc.createElement(CONJ);
        } else if (node instanceof Not) {
            op = doc.createElement(NOT);
        } else if (node instanceof AtMost) {
            op = doc.createElement(ATMOST1);
        } else {
            FeatJAR.log().error("Unsupported element %s", node);
            return;
        }
        xmlNode.appendChild(op);

        for (final IExpression child : node.getChildren()) {
            createPropositionalConstraints(doc, op, (IFormula) child);
        }
    }

    /**
     * Creates document based on feature model step by step
     *
     * @param doc document to write
     * @param node parent node
     * @param feat current feature
     */
    protected void writeFeatureTreeRec(Document doc, Element node, IFeatureTree feat) {
        if (feat == null) {
            return;
        }

        final List<? extends IFeatureTree> children = feat.getChildren();

        final Element fnod;
        if (children.isEmpty()) {
            fnod = doc.createElement(FEATURE);
            writeFeatureProperties(doc, node, feat, fnod);
        } else {
            if (feat.getGroup().isAnd()) {
                fnod = doc.createElement(AND);
            } else if (feat.getGroup().isOr()) {
                fnod = doc.createElement(OR);
            } else if (feat.getGroup().isAlternative()) {
                fnod = doc.createElement(ALT);
            } else {
                FeatJAR.log().error("Unkown group %s", feat.getGroup());
                return;
            }

            writeFeatureProperties(doc, node, feat, fnod);

            for (final IFeatureTree feature : children) {
                writeFeatureTreeRec(doc, fnod, feature);
            }
        }
    }

    protected void writeFeatureProperties(Document doc, Element node, IFeatureTree feat, final Element fnod) {
        addDescription(doc, feat.getFeature().getDescription().orElse(null), fnod);
        addProperties(doc, feat.getAttributes().get(), fnod);
        writeAttributes(node, fnod, feat);
    }

    protected void addDescription(Document doc, String description, Element fnod) {
        if ((description != null) && !description.trim().isEmpty()) {
            final Element descr = doc.createElement(DESCRIPTION);
            descr.setTextContent(description);
            fnod.appendChild(descr);
        }
    }

    protected void addProperties(Document doc, Map<IAttribute<?>, Object> attributes, Element fnod) {
        for (final Entry<IAttribute<?>, Object> property : attributes.entrySet()) {
            final Element propNode;
            String namespace = property.getKey().getNamespace();
            if (GRAPHICS_NAMESPACE.equals(namespace)) {
                propNode = doc.createElement(GRAPHICS);
            } else if (CALCULATIONS_NAMESPACE.equals(namespace)) {
                propNode = doc.createElement(CALCULATIONS);
            } else {
                propNode = doc.createElement(PROPERTY);
                propNode.setAttribute(NAMESPACE_TAG, property.getKey().getNamespace());
                propNode.setAttribute(
                        DATA_TYPE,
                        AttributeIO.getTypeString(property.getKey().getType())
                                .orElseThrow(p -> new IllegalArgumentException()));
            }
            propNode.setAttribute(KEY, property.getKey().getName());
            propNode.setAttribute(VALUE, property.getValue().toString()); // TODO
            fnod.appendChild(propNode);
        }
    }

    /**
     * Adds the tags of a constraint to the xml file
     */
    private void addTags(Document doc, Set<String> tags, Element fnod) {
        if ((tags != null) && !tags.isEmpty()) {
            final Element tag = doc.createElement(TAGS);
            String finalTags = "";
            for (final String tagString : tags) {
                if (finalTags.equals("")) {
                    finalTags += tagString;
                    continue;
                }
                finalTags += "," + tagString;
            }
            tag.setTextContent(finalTags);
            fnod.appendChild(tag);
        }
    }

    protected void writeAttributes(Element node, Element fnod, IFeatureTree feat) {
        fnod.setAttribute(NAME, feat.getFeature().getName().get());
        if (feat.getFeature().isHidden()) {
            fnod.setAttribute(HIDDEN, TRUE);
        }
        if (feat.isMandatory()) {
            if ((feat.getParent().isPresent())
                    && feat.getParent().get().getGroup().isAnd()) {
                fnod.setAttribute(MANDATORY, TRUE);
            } else if (feat.getParent().isEmpty()) {
                fnod.setAttribute(MANDATORY, TRUE);
            }
        }
        if (feat.getFeature().isAbstract()) {
            fnod.setAttribute(ABSTRACT, TRUE);
        }

        node.appendChild(fnod);
    }
}
