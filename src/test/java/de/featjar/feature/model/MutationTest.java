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
 */
public class MutationTest {

    private IFeatureModel featureModel;

    @BeforeEach
    public void setUp() {
        featureModel = new FeatureModel(UUIDIdentifier.newInstance());
        featureModel.mutate().addFeature("Feature1");
        featureModel.mutate().addFeature("Feature2");
    }

    private void addNewFeature(String featureName) {
        assertFalse(featureName.isEmpty(), "Feature name should not be empty");

        IMutableFeatureModel mutableFeatureModel = (IMutableFeatureModel) featureModel;
        IFeature newFeature = mutableFeatureModel.addFeature(featureName);

        assertTrue(mutableFeatureModel.getFeatures().contains(newFeature));
        assertEquals(featureName, newFeature.getName().get());
        assertTrue(featureModel.getFeatures().stream().anyMatch(f -> f.getName().valueEquals(featureName)));

        System.out.println("New feature added successfully: " + featureName);
    }

    @Test
    public void testAddNewFeature() {
        String newFeatureName = "Feature3"; // Unique name
        addNewFeature(newFeatureName);
    }

    @Test
    public void testAddDuplicateFeature() {
        String duplicateFeatureName = "Feature1"; // Duplicate name

        assertFalse(duplicateFeatureName.isEmpty(), "Feature name should not be empty");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            IMutableFeatureModel mutableFeatureModel = (IMutableFeatureModel) featureModel;
            mutableFeatureModel.addFeature(duplicateFeatureName);
        });

        assertEquals("Feature name conflicts with a predefined or existing feature.", exception.getMessage());
        System.out.println(exception.getMessage());

        // Execute testAddNewFeature within testAddDuplicateFeature
        System.out.println("Attempting to add a new unique feature after handling duplicate");
        testAddNewFeature();
    }
}
