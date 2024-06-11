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
import java.util.Scanner;

/**
 * Tests for Mutation and Error Handling 
 *
 * @author Ananya
 *
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
        return predefinedFeatures.contains(featureName) || featureModel.getFeatures().stream().anyMatch(f -> f.getName().valueEquals(featureName));
    }
}
