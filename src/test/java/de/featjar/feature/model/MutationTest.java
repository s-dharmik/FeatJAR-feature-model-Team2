package de.featjar.feature.model;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.featjar.base.data.identifier.UUIDIdentifier;
import de.featjar.feature.model.IFeatureModel.IMutableFeatureModel;

/**
 * Tests for Mutation and Error Handling 
 *
 * @author Ananya
 *
 */


/*
 * Used a Scanner to get input for the new feature name.
 * Added validation to ensure the feature name is not empty.
 * Checked for conflicts with existing features.
 * Added the new feature to IMutableFeatureModel and verified its addition.
 * Printed a success message upon successfully adding the feature.
 */

public class MutationTest {

    private IFeatureModel featureModel;
    private List<String> predefinedFeatures;

    @BeforeEach
    public void setUp() {
        featureModel = new FeatureModel(UUIDIdentifier.newInstance());
        featureModel.mutate().addFeature("Feature1");
        featureModel.mutate().addFeature("Feature2");
    }

    @Test
    public void testAddNewFeature() {
       
        Scanner scanner = new Scanner(System.in);
        String newFeatureName = getValidFeatureName(scanner);

        assertFalse(newFeatureName.isEmpty(), "Feature name should not be empty");

        assertFalse(featureModel.getFeatures().stream().anyMatch(f -> f.getName().valueEquals(newFeatureName)));

        IMutableFeatureModel mutableFeatureModel = (IMutableFeatureModel) featureModel;
        IFeature newFeature = mutableFeatureModel.addFeature(newFeatureName);

        assertTrue(mutableFeatureModel.getFeatures().contains(newFeature));

        assertEquals(newFeatureName, newFeature.getName().get());

        assertTrue(featureModel.getFeatures().stream().anyMatch(f -> f.getName().valueEquals(newFeatureName)));
        System.out.println("New feature added successfully: " + newFeatureName);
    }

    private String getValidFeatureName(Scanner scanner) {
        System.out.println("Enter the name of the new feature: ");
        String newFeatureName = scanner.nextLine();

        while (newFeatureName.isEmpty() || isFeatureNameConflict(newFeatureName)) {
            if (newFeatureName.isEmpty()) {
                System.out.println("Feature name cannot be empty. Please enter a different name: ");
            } else {
                System.out.println("Feature name conflicts with a predefined or existing feature. Please enter a different name: ");
            }
            newFeatureName = scanner.nextLine();
        }

        return newFeatureName;
    }

    private boolean isFeatureNameConflict(String featureName) {
        return featureModel.getFeatures().stream().anyMatch(f -> f.getName().valueEquals(featureName));
    }
}
