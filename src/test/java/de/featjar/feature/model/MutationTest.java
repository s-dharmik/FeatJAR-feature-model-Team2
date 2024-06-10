package de.featjar.feature.model;

import static org.junit.jupiter.api.Assertions.*;

import de.featjar.base.data.Result;
import de.featjar.base.data.identifier.Identifiers;
import de.featjar.feature.model.IFeatureModel.IMutableFeatureModel;
import de.featjar.feature.model.IFeature;
import de.featjar.feature.model.IFeatureTree;
import java.util.List;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for Mutation and Error Handling 
 *
 * @author Ananya
 */


public class MutationTest {
    IFeatureModel featureModel;
    List<String> predefinedNames;
    Scanner scanner;

    @BeforeEach
    public void setup() {
        featureModel = new FeatureModel(Identifiers.newCounterIdentifier());
        scanner = new Scanner(System.in);
    }

    @Test
    public void testFeatureMutationsAndExceptionHandling_NoDuplicates() {
        predefinedNames = List.of("featureA", "featureB");
        runFeatureMutationTest();
    }

    @Test
    public void testFeatureMutationsAndExceptionHandling_SingleDuplicate() {
        predefinedNames = List.of("feature1", "feature2", "feature1", "feature3");
        runFeatureMutationTest();
    }

    @Test
    public void testFeatureMutationsAndExceptionHandling_MultipleDuplicates() {
        predefinedNames = List.of("feature1", "feature2", "feature1", "feature2", "feature3");
        runFeatureMutationTest();
    }

    @Test
    public void testFeatureMutationsAndExceptionHandling_AllDuplicates() {
        predefinedNames = List.of("feature1", "feature1", "feature1", "feature1", "feature1");
        runFeatureMutationTest();
    }

    @Test
    public void testFeatureMutationsAndExceptionHandling_NullFeatureAddition() {
        assertThrows(IllegalArgumentException.class, () -> {
            featureModel.mutate().addFeature(null);
        });
        predefinedNames = List.of("feature1", "feature2");
        runFeatureMutationTest();
    }

    private void runFeatureMutationTest() {
        // Manually remove all existing features to ensure a clean state
        List<IFeature> existingFeatures = featureModel.getFeatures().stream().collect(Collectors.toList());
        IMutableFeatureModel mutableFeatureModel = featureModel.mutate();
        for (IFeature feature : existingFeatures) {
            mutableFeatureModel.removeFeature(feature);
        }

        // Add initial features from predefined names
        List<String> addedNames = new ArrayList<>();
        for (String name : predefinedNames) {
            addFeatureWithPredefinedName(name, addedNames);
        }

        // Add another feature to compare names
        IFeature feature3 = featureModel.mutate().addFeature("feature3");
        feature3.mutate().setName("FeatureThree");

        // Assertions to verify names are different
        verifyFeatureNameDifference("feature1", feature3);

        // Additional assertions to verify feature count and names
        List<IFeature> features = featureModel.getFeatures().stream().collect(Collectors.toList());
        int expectedFeatureCount = (int) predefinedNames.stream().distinct().count() + 1; // Unique predefined names + FeatureThree
        assertEquals(expectedFeatureCount, features.size(), "The number of features in the model should match the number of unique predefined names plus 'FeatureThree'.");

        // Assert the existence of all predefined names and "FeatureThree"
        List<String> expectedFeatureNames = predefinedNames.stream().distinct().collect(Collectors.toList());
        expectedFeatureNames.add("FeatureThree");
        expectedFeatureNames.forEach(name -> {
            boolean exists = features.stream().anyMatch(f -> f.getName().orElse("").equals(name));
            assertTrue(exists, "Feature '" + name + "' should exist");
        });

        // Test for non-existent feature identifiers
        Result<IFeature> result = featureModel.getFeature("Nonexistent");
        assertFalse(result.isPresent(), "Should indicate that no feature is present for non-existent feature identifiers");
    }

    private void addFeatureWithPredefinedName(String featureName, List<String> addedNames) {
        if (isFeatureNameUnique(featureName)) {
            IFeature feature = featureModel.mutate().addFeature(featureName);
            feature.mutate().setName(featureName);
            addedNames.add(featureName);
            assertEquals(Result.of(featureName), feature.getName(), "Feature name should be " + featureName);
        } else {
            // Verify that adding a duplicate name does not change the existing feature set
            assertTrue(addedNames.contains(featureName), "Feature name '" + featureName + "' already exists and should be in the added names list.");
        }
    }

    private void verifyFeatureNameDifference(String featureName, IFeature feature) {
        Result<IFeature> featureResult = featureModel.getFeature(featureName);
        if (featureResult.isPresent()) {
            assertNotEquals(featureName, feature.getName().orElse("Error: Name not set"), "Feature names should be different");
        }
        assertEquals(Result.of("FeatureThree"), feature.getName(), "Third feature name should be 'FeatureThree'");
    }

    private boolean isFeatureNameUnique(String featureName) {
        return featureModel.getFeatures().stream()
                .noneMatch(feature -> feature.getName().orElse("").equals(featureName));
    }

    private String getUserInput() {
        System.out.print("Enter a feature name: ");
        return scanner.nextLine();
    }
}
