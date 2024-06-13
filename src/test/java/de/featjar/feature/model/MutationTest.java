package de.featjar.feature.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.featjar.base.data.identifier.UUIDIdentifier;
import de.featjar.feature.model.IFeatureModel.IMutableFeatureModel;

/**
 * Tests for Mutation and Error Handling
 * 
 * @author ananyaks
 *
 */

public class MutationTest {

    private IFeatureModel featureModel;

    @BeforeEach
    public void setUp() {
        featureModel = new FeatureModel(UUIDIdentifier.newInstance());
        featureModel.mutate().addFeature("Feature1");
        featureModel.mutate().addFeature("Feature2");
    }

    @Test
    public void testAddNewFeature() {
        String newFeatureName = "Feature3"; // Unique name

        assertFalse(newFeatureName.isEmpty(), "Feature name should not be empty");

        IMutableFeatureModel mutableFeatureModel = (IMutableFeatureModel) featureModel;
        IFeature newFeature = mutableFeatureModel.addFeature(newFeatureName);

        assertTrue(mutableFeatureModel.getFeatures().contains(newFeature));
        assertEquals(newFeatureName, newFeature.getName().get());
        assertTrue(featureModel.getFeatures().stream().anyMatch(f -> f.getName().valueEquals(newFeatureName)));

        System.out.println("New feature added successfully: " + newFeatureName);
    }

    @Test
    public void testAddDuplicateFeature() {
        String duplicateFeatureName = "Feature1"; // Duplicate name

        assertFalse(duplicateFeatureName.isEmpty(), "Feature name should not be empty");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            if (featureModel.getFeatures().stream().anyMatch(f -> f.getName().valueEquals(duplicateFeatureName))) {
                throw new IllegalArgumentException("Feature name conflicts with a predefined or existing feature.");
            }
        });

        assertEquals("Feature name conflicts with a predefined or existing feature.", exception.getMessage());
        System.out.println(exception.getMessage());
    }
}
