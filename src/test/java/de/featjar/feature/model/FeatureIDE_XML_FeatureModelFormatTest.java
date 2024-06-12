package de.featjar.feature.model;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import de.featjar.Common;
import de.featjar.base.FeatJAR;
import de.featjar.base.data.Result;
import de.featjar.base.io.IO;
import de.featjar.base.io.input.AInputMapper;
import de.featjar.base.io.input.StringInputMapper;
import de.featjar.feature.model.io.FeatureIDE_XML_FeatureModelFormat;
import de.featjar.feature.model.transformer.ComputeFormula;
import de.featjar.formula.structure.Expressions;
import de.featjar.formula.structure.formula.IFormula;

public class FeatureIDE_XML_FeatureModelFormatTest extends Common {
	
	private FeatureIDE_XML_FeatureModelFormat parser;
	
	@Mock
	private AInputMapper inputMapper;

    @BeforeEach
    public void setUp() {
    	MockitoAnnotations.openMocks(this);
        parser = new FeatureIDE_XML_FeatureModelFormat();
    }

	@Test
	public void parse() throws IOException{
		Result<IFeatureModel> featureModel = IO.load(Paths.get("src/test/java/car.xml"), new FeatureIDE_XML_FeatureModelFormat());
		
		
		assertTrue(featureModel.isPresent(), featureModel.printProblems());
		
		Result<IFormula> formula = featureModel.toComputation().map(ComputeFormula::new).computeResult();
		
		assertTrue(formula.isPresent(), formula.printProblems());
		FeatJAR.log().info(Expressions.print(formula.get()));
	}
	   
    
    @Test
    public void testParseValidXML() throws IOException {
        String validXML = readFileContent();

        Result<IFeatureModel> result = parser.parse(inputMapper);
        AInputMapper contentMapper = mock(AInputMapper.class);
        
        when(contentMapper.get().text()).thenReturn(validXML);
        
//        Mockito.doReturn(contentMapper).when(contentMapper);
        
        when(inputMapper.get()).thenReturn(contentMapper.get());
        
        

        assertTrue(result.isPresent(), "Valid XML should be parsed successfully");
        IFeatureModel featureModel = result.get();
        assertNotNull(featureModel, "Parsed feature model should not be null");
    }
    
    

    @Test
    public void testParseInvalidXML() {
        String invalidXML = "<invalid></xml>";
        Charset charset = Charset.forName("UTF-8");
        Result<IFeatureModel> result = parser.parse(new StringInputMapper(invalidXML, charset, ".xml"));

        assertFalse(result.isPresent(), "Invalid XML should not be parsed successfully");
        assertTrue(!result.getProblems().isEmpty(), "Error should be present for invalid XML");
    }

    @Test
    public void testSupportsParse() {
        assertTrue(parser.supportsParse(), "Parser should support parsing");
    }

    @Test
    public void testDoesNotSupportSerialize() {
        assertFalse(parser.supportsSerialize(), "Parser should not support serialization");
    }

    @Test
    public void testGetFileExtension() {
        assertEquals("xml", parser.getFileExtension(), "File extension should be 'xml'");
    }

    @Test
    public void testGetName() {
        assertEquals("FeatureIDE XML", parser.getName(), "Name should be 'FeatureIDE XML'");
    }

    private String readFileContent() throws IOException {
        Path path = Paths.get("D:/UPB/PROJECT_CONFIG_TOOLS/FeatJAR/feature-model/src/test/java/car.xml");
        return Files.readString(path);
    }
	
	
	
}