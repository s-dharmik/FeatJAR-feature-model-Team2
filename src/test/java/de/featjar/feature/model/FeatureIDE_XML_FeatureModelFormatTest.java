package de.featjar.feature.model;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertNotNull;
//import static org.junit.jupiter.api.Assertions.assertTrue;
//import static org.junit.jupiter.api.Assertions.fail;
//
//import java.io.StringReader;
//
//import javax.xml.parsers.DocumentBuilder;
//import javax.xml.parsers.DocumentBuilderFactory;
//
//import org.junit.jupiter.api.Test;
//import org.w3c.dom.Document;
//import org.w3c.dom.Element;
//import org.w3c.dom.NodeList;
//import org.xml.sax.InputSource;
//
//import de.featjar.base.data.Result;
//import de.featjar.feature.model.io.FeatureIDE_XML_FeatureModelFormat;
//
//public class FeatureIDE_XML_FeatureModelFormatTest {
//	
//	FeatureIDE_XML_FeatureModelFormat parser = new FeatureIDE_XML_FeatureModelFormat();
//
//    @Test
//    public void testSerialization() {
//        // Create a sample feature model
//        IFeatureModel featureModel = new FeatureModel();
//        featureModel.mutate().addFeature("Feature1");
//        featureModel.mutate().addFeature("Feature2");
//        featureModel.mutate().addConstraint(parser.parseFormula("and(Feature1, Feature2)", featureModel));
//
//        // Serialize the feature model
//        FeatureIDE_XML_FeatureModelFormat format = new FeatureIDE_XML_FeatureModelFormat();
//        Result<String> result = format.serialize(featureModel);
//
//        // Verify serialization result
//        assertTrue(result.get() != null, "Serialization result should not be null");
//        
//        String xmlString = result.get();
//        assertNotNull(xmlString);
//
//        // Check if XML structure is correct
//        try {
//            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
//            DocumentBuilder builder = factory.newDocumentBuilder();
//            Document doc = builder.parse(new InputSource(new StringReader(xmlString)));
//
//            // Check root element
//            Element rootElement = doc.getDocumentElement();
//            assertEquals("featureModel", rootElement.getNodeName());
//
//            // Check features
//            NodeList featureNodes = rootElement.getElementsByTagName("feature");
//            assertEquals(2, featureNodes.getLength());
//
//            // Check constraints
//            NodeList constraintNodes = rootElement.getElementsByTagName("constraint");
//            assertEquals(1, constraintNodes.getLength());
//
//        } catch (Exception e) {
//            fail("Exception thrown: " + e.getMessage());
//        }
//    }
//}




//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertFalse;
//
//import java.io.IOException;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//
//import org.junit.jupiter.api.Test;
//
//import de.featjar.base.data.Result;
//import de.featjar.base.io.IO;
//import de.featjar.feature.model.io.FeatureIDE_XML_FeatureModelFormat;
//
//public class FeatureIDE_XML_FeatureModelFormatTest {
//
//    @Test
//    public void testSerializeAndParse() throws IOException {
//        
//        Path inputPath = Paths.get("src/test/java/car.xml");
//        FeatureIDE_XML_FeatureModelFormat parser = new FeatureIDE_XML_FeatureModelFormat();
//        
//        Result<IFeatureModel> fm = IO.load(inputPath.toString(), parser);
//        IFeatureModel featureModel = fm.orElseThrow();
//
//        // Assert that the feature model has features
//        assertFalse(featureModel.getFeatures().isEmpty(), "Feature model should have features");
//
//        String outputString = IO.print(featureModel, parser);
//
//        String inputString = Files.readString(inputPath);
//
//        
//
//        assertEquals(inputString, outputString, "The input and output XML File should be equal");
//
//        
//    }
//}


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import de.featjar.base.data.Result;
import de.featjar.base.io.IO;
import de.featjar.base.io.format.ParseException;
import de.featjar.feature.model.io.FeatureIDE_XML_FeatureModelFormat;

public class FeatureIDE_XML_FeatureModelFormatTest {

    @Test
    public void testSerialization() throws IOException, ParseException {
        // Constructing the path relative to the project root
        Path inputFilePath = Paths.get("src/test/java/car.xml");
        FeatureIDE_XML_FeatureModelFormat format = new FeatureIDE_XML_FeatureModelFormat();
        Result<IFeatureModel> fm = IO.load(inputFilePath, format);
        IFeatureModel featureModel = fm.orElseThrow();
        
        // Assert that the feature model has features
        assertFalse(!featureModel.getFeatures().isEmpty(), "Feature model should have features");
        
        String outputString = IO.print(featureModel, format);

        String inputString = Files.readString(inputFilePath);

        // Log the input and output strings for debugging
        System.out.println("Input XML:\n" + inputString);
        System.out.println("Output XML:\n" + outputString);

        // Normalize line endings for comparison
        inputString = inputString.replace("\r\n", "\n").trim();
        outputString = outputString.replace("\r\n", "\n").trim();

        assertEquals(inputString, outputString, "The input and output XML strings should be equal");
    }
}
