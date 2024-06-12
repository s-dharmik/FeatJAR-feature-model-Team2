package de.featjar.feature.model;

import static org.junit.jupiter.api.Assertions.*;

import de.featjar.base.data.identifier.UUIDIdentifier;
import de.featjar.feature.model.FeatureModel;
import de.featjar.feature.model.IFeature;
import de.featjar.feature.model.IFeatureModel;
import de.featjar.feature.model.IFeatureModel.IMutableFeatureModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests for Mutation and Error Handling 
 *
 * @author Ananya
 *
 */


/**
 * Updating unit tests for automatic feature addition in the feature model
 * Modified the testAddNewFeature() method to add a new feature automatically.
 * Removed the use of Scanner for user input.
 * Implemented getUniqueFeatureName() to generate unique feature names.
 * Ensured the method checks for conflicts with predefined or existing features.
 * Used isFeatureNameConflict(String featureName) to verify if a feature name already exists.
 */

public class MutationTest {

    private IFeatureModel featureModel;
    private List<String> predefinedFeatures;

    @BeforeEach
    public void setUp() {
        featureModel = new FeatureModel(UUIDIdentifier.newInstance());
        predefinedFeatures = new ArrayList<>();
        predefinedFeatures.add("Feature1");
        predefinedFeatures.add("Feature2");
        predefinedFeatures.add("Feature3");
        predefinedFeatures.add("Feature4");
   //     predefinedFeatures.add("Feature5");
    }

    @Test
    public void testAddNewFeature() {
        String newFeatureName = getUniqueFeatureName();

        assertFalse(newFeatureName.isEmpty(), "Feature name should not be empty");
        assertFalse(featureModel.getFeatures().stream().anyMatch(f -> f.getName().valueEquals(newFeatureName)), "Feature name already exists");

        IMutableFeatureModel mutableFeatureModel = (IMutableFeatureModel) featureModel;
        IFeature newFeature = mutableFeatureModel.addFeature(newFeatureName);

        assertTrue(mutableFeatureModel.getFeatures().contains(newFeature), "New feature not added to the model");
        assertEquals(newFeatureName, newFeature.getName().get(), "Feature name mismatch after addition");
        assertTrue(featureModel.getFeatures().stream().anyMatch(f -> f.getName().valueEquals(newFeatureName)), "Feature not found in the model after addition");

        System.out.println("New feature added successfully: " + newFeatureName);
    }

    private String getUniqueFeatureName() {
        String baseName = "Feature";
        int suffix = 4;
        String newFeatureName = baseName + suffix;

        while (isFeatureNameConflict(newFeatureName)) {
            suffix++;
            newFeatureName = baseName + suffix;
        }

        return newFeatureName;
    }

    private boolean isFeatureNameConflict(String featureName) {
        return predefinedFeatures.contains(featureName) || featureModel.getFeatures().stream().anyMatch(f -> f.getName().valueEquals(featureName));
    }
}
