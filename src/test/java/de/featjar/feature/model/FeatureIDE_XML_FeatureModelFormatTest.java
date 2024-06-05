package de.featjar.feature.model;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import de.featjar.base.FeatJAR;
import de.featjar.base.data.Result;
import de.featjar.base.io.IO;
import de.featjar.feature.model.io.FeatureIDE_XML_FeatureModelFormat;
import de.featjar.feature.model.io.xml.XMLFeatureModelFormat;
import de.featjar.feature.model.transformer.ComputeFormula;
import de.featjar.formula.structure.formula.IFormula;

public class FeatureIDE_XML_FeatureModelFormatTest {

	@Test
	public void parse() throws IOException{
		Result<IFeatureModel> featureModel = IO.load(Paths.get("src/test/java/car.xml"), new FeatureIDE_XML_FeatureModelFormat());
		
		
		assertTrue(featureModel.isPresent(), featureModel.printProblems());
		
		Result<IFormula> formula = featureModel.toComputation().map(ComputeFormula::new).computeResult();
		
		assertTrue(formula.isPresent(), formula.printProblems());
		FeatJAR.log().info(formula.get().toString());
	}
	    
	
	
	
	
}