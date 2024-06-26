package de.featjar.feature.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import de.featjar.feature.model.io.xml.XMLFeatureModelFormat;

public class XMLFeatureModelFormatTest {
	
	@Test
    public void testParseBasicFeatureModel() throws Exception {
        String xml = "<featureModel><struct><and name=\"Root\"><feature name=\"A\"/><feature name=\"B\"/><feature name=\"C\"/></and></struct></featureModel>";
        XMLFeatureModelFormat parser = new XMLFeatureModelFormat();
        Document doc = loadXMLFromString(xml);
        IFeatureModel featureModel = parser.parseDocument(doc);
        assertNotNull(featureModel);
        assertEquals(3, featureModel.getNumberOfFeatures()-1);
    }

    @Test
    public void testParseFeatureWithProperties() throws Exception {
        String xml = "<featureModel><struct><feature name=\"A\"/></struct><properties><property key=\"priority\" value=\"high\"/></properties></featureModel>";
        XMLFeatureModelFormat parser = new XMLFeatureModelFormat();
        Document doc = loadXMLFromString(xml);
        IFeatureModel featureModel = parser.parseDocument(doc);
        assertNotNull(featureModel);
        assertEquals(1, featureModel.getNumberOfFeatures());
    }

    private Document loadXMLFromString(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        return factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

}
