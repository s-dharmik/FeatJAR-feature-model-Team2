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
 * author Ananya
 */


public class MutationTest {
    IFeatureModel featureModel;
    Scanner scanner;

    @BeforeEach
    public void setup() {
        featureModel = new FeatureModel(Identifiers.newCounterIdentifier());
        scanner = new Scanner(System.in);
    }

    @Test
    public void testFeatureMutationsAndExceptionHandling() {
        List<String> userProvidedNames = getUserInputFeatures();
        try {
            runFeatureMutationTest(userProvidedNames);
        } catch (Exception e) {
            System.out.println("Exception caught: " + e.getMessage());
        }
    }

    private void runFeatureMutationTest(List<String> predefinedNames) throws Exception {
        List<IFeature> existingFeatures = featureModel.getFeatures().stream().collect(Collectors.toList());
        IMutableFeatureModel mutableFeatureModel = featureModel.mutate();
        for (IFeature feature : existingFeatures) {
            mutableFeatureModel.removeFeature(feature);
        }

        List<String> addedNames = new ArrayList<>();
        for (String name : predefinedNames) {
            addFeatureWithPredefinedName(name, addedNames);
        }

        List<IFeature> features = featureModel.getFeatures().stream().collect(Collectors.toList());
        int expectedFeatureCount = (int) predefinedNames.stream().distinct().count();
        assertEquals(expectedFeatureCount, features.size(), "The number of features in the model should match the number of unique predefined names.");

        predefinedNames.stream().distinct().forEach(name -> {
            boolean exists = features.stream().anyMatch(f -> f.getName().orElse("").equals(name));
            assertTrue(exists, "Feature '" + name + "' should exist");
        });

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
            System.out.println("Feature name '" + featureName + "' already exists. Please enter a unique feature name: ");
            String newFeatureName = scanner.nextLine();
            addFeatureWithPredefinedName(newFeatureName, addedNames);
        }
    }

    private boolean isFeatureNameUnique(String featureName) {
        return featureModel.getFeatures().stream()
                .noneMatch(feature -> feature.getName().orElse("").equals(featureName));
    }

    private List<String> getUserInputFeatures() {
        List<String> inputFeatures = new ArrayList<>();
        int numFeatures = 0;
        while (numFeatures <= 0) {
            System.out.print("Enter the number of features: ");
            String input = scanner.nextLine();
            try {
                numFeatures = Integer.parseInt(input);
                if (numFeatures <= 0) {
                    System.out.println("Please enter a positive number.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a valid number.");
            }
        }

        for (int i = 0; i < numFeatures; i++) {
            System.out.print("Enter feature name " + (i + 1) + ": ");
            String featureName = scanner.nextLine();
            while (inputFeatures.contains(featureName) || featureName.isEmpty()) {
                if (featureName.isEmpty()) {
                    System.out.print("Feature name cannot be empty. Enter a different feature name: ");
                } else {
                    System.out.print("Feature name '" + featureName + "' already exists. Enter a different feature name: ");
                }
                featureName = scanner.nextLine();
            }
            inputFeatures.add(featureName);
        }
        return inputFeatures;
    }
}
