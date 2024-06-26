package de.featjar.feature.model.io;


import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import de.featjar.base.data.Result;
import de.featjar.feature.model.FeatureModel;
import de.featjar.formula.structure.formula.predicate.Literal;

public class DimacsFeatureModelFormatTest {
	
	

	// This test is for checking if code can correctly convert*(Serialization) object to specific format and back again
	
    @Test
    public void testSerialization() {
        FeatureModel featureModel = new FeatureModel();
        
        featureModel.mutate().addFeature("Feature1");
        featureModel.mutate().addFeature("Feature2");
        
        featureModel.addConstraint(new Literal("Feature1"));

        DimacsFeatureModelFormat format = new DimacsFeatureModelFormat();
        Result<String> result = format.serialize(featureModel);

        assertFalse(result.isEmpty(), "Serialization should not be successful");
        String serialized = result.get().toString();
        assertNotNull(serialized, "Serialized result should not be null");
        assertTrue(serialized.contains("p cnf 2 1"), "Serialized DIMACS should contain correct header");
    }
    
}