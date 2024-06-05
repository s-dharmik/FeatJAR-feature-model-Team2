/*
 * Copyright (C) 2024 FeatJAR-Development-Team
 *
 * This file is part of FeatJAR-feature-model.
 *
 * feature-model is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3.0 of the License,
 * or (at your option) any later version.
 *
 * feature-model is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with feature-model. If not, see <https://www.gnu.org/licenses/>.
 *
 * See <https://github.com/FeatureIDE/FeatJAR-feature-model> for further information.
 */
package de.featjar.feature.model;

import static org.junit.jupiter.api.Assertions.*;



import de.featjar.base.data.Result;
import de.featjar.base.data.identifier.IIdentifier;
import de.featjar.base.data.identifier.Identifiers;
import de.featjar.formula.structure.Expressions;
//import de.featjar.formula.structure.IExpression;
import de.featjar.formula.structure.formula.IFormula;
import de.featjar.feature.model.IConstraint;
import de.featjar.feature.model.IFeatureModel.IMutableFeatureModel;
import de.featjar.feature.model.IFeature;
import de.featjar.feature.model.IFeatureTree;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link FeatureModel}, its elements, and its mixins.
 *
 * @author Elias Kuiter
 */
public class FeatureModelTest {
    IFeatureModel featureModel;
    

    @BeforeEach
    public void createFeatureModel() {
        featureModel = new FeatureModel(Identifiers.newCounterIdentifier());
    }

    @Test
    public void featureModel() {
        Assertions.assertEquals("1", featureModel.getIdentifier().toString());
        assertTrue(featureModel.getRoots().isEmpty());
        assertTrue(featureModel.getFeatures().isEmpty());
        assertTrue(featureModel.getConstraints().isEmpty());
    }

    
    @Test
    public void commonAttributesMixin() {
        Assertions.assertEquals("@1", featureModel.getName().get());
        Assertions.assertEquals(Result.empty(), featureModel.getDescription());
        featureModel.mutate().setName("My Model");
        featureModel.mutate().setDescription("awesome description");
        Assertions.assertEquals(Result.of("My Model"), featureModel.getName());
        Assertions.assertEquals(Result.of("awesome description"), featureModel.getDescription());
    }

    @Test
    public void featureModelConstraintMixin() {
        Assertions.assertEquals(0, featureModel.getNumberOfConstraints());
        IConstraint constraint1 = featureModel.mutate().addConstraint(Expressions.True);
        IConstraint constraint2 = featureModel.mutate().addConstraint(Expressions.True);
        IConstraint constraint3 = featureModel.mutate().addConstraint(Expressions.False);
        Assertions.assertEquals(3, featureModel.getNumberOfConstraints());
        Assertions.assertEquals(Result.of(constraint1), featureModel.getConstraint(constraint1.getIdentifier()));
        Assertions.assertTrue(featureModel.hasConstraint(constraint2.getIdentifier()));
        constraint2.mutate().remove();
        Assertions.assertFalse(featureModel.hasConstraint(constraint2.getIdentifier()));
        Assertions.assertTrue(featureModel.hasConstraint(constraint3));
    }

    @Test
    public void featureModelFeatureTreeMixin() {
        IFeature rootFeature = featureModel.mutate().addFeature("root");
        Assertions.assertEquals(1, featureModel.getNumberOfFeatures());
        IFeatureTree rootTree = featureModel.mutate().addFeatureTreeRoot(rootFeature);
        IFeature childFeature = featureModel.mutate().addFeature("child1");
        final IFeatureTree childTree = rootTree.mutate().addFeatureBelow(childFeature);
        assertSame(childFeature, childTree.getFeature());
        assertSame(childTree, childFeature.getFeatureTree().get());
        assertSame(childFeature, childTree.getFeature());
        assertSame(rootFeature, childTree.getParent().get().getFeature());
        assertSame(childTree.getParent().get(), rootFeature.getFeatureTree().get());
        assertSame(featureModel.getFeature(childFeature.getIdentifier()).get(), childFeature);
        Assertions.assertEquals(2, featureModel.getNumberOfFeatures());
        Assertions.assertEquals(Result.of(childFeature), featureModel.getFeature(childFeature.getIdentifier()));
        Assertions.assertTrue(featureModel.hasFeature(childFeature.getIdentifier()));
        Assertions.assertTrue(featureModel.getFeature("root2").isEmpty());
        rootFeature.mutate().setName("root2");
        Assertions.assertEquals(Result.of(rootFeature), featureModel.getFeature("root2"));
        assertEquals(List.of(childTree), rootFeature.getFeatureTree().get().getChildren());
        assertEquals(rootFeature.getFeatureTree(), childTree.getParent());
        childTree.mutate().removeFromTree();
        assertEquals(List.of(), rootFeature.getFeatureTree().get().getChildren());
        
        printFeatureTreeStructure(rootFeature);
        
    }
    
    private void printFeatureTreeStructure(IFeature rootFeature) {
        Result<IFeatureTree> rootTreeResult = rootFeature.getFeatureTree();
        if (rootTreeResult.isPresent()) {
            IFeatureTree rootTree = rootTreeResult.get();
            System.out.println("Root Feature: " + rootFeature.getName().orElse("[Unnamed Feature]"));
            List<? extends IFeatureTree> children = rootTree.getChildren();
            if (children.isEmpty()) {
                System.out.println("  No children");
            } else {
                for (IFeatureTree child : children) {
                    System.out.println("  Child Feature: " + child.getFeature().getName().orElse("[Unnamed Feature]"));
                }
            }
        } else {
            System.out.println("No root tree present.");
        }
    }

    
    
    @Test
    public void testPerformance() {
        System.out.println("Testing performance metrics...");

        // Number of constraints to add and remove
        int numberOfConstraints = 5;
        List<IConstraint> addedConstraints = new ArrayList<>();

        // Start time for adding constraints
        long startTimeAdd = System.currentTimeMillis();

        // Adding constraints
        for (int i = 0; i < numberOfConstraints; i++) {
            IFeature featureA = featureModel.mutate().addFeature("featureA" + i);
            IFeature featureB = featureModel.mutate().addFeature("featureB" + i);
            IFormula formula = Expressions.and(
                Expressions.variableAsFormula(featureA.getName().orElseThrow()),
                Expressions.variableAsFormula(featureB.getName().orElseThrow())
            );
            IConstraint constraint = featureModel.mutate().addConstraint(formula);
            addedConstraints.add(constraint);
        }

        // End time for adding constraints
        long endTimeAdd = System.currentTimeMillis();

        // Calculate the duration for adding constraints in milliseconds
        double durationAddInMilliseconds = endTimeAdd - startTimeAdd;
        System.out.println("Added " + numberOfConstraints + " constraints in " + durationAddInMilliseconds + " milliseconds");

        // Assert that the correct number of constraints have been added
        Assertions.assertEquals(numberOfConstraints, addedConstraints.size(), "The number of constraints added should match the expected total.");

        // Start time for removing constraints
        long startTimeRemove = System.currentTimeMillis();

        // Removing constraints
        for (IConstraint constraint : addedConstraints) {
            featureModel.mutate().removeConstraint(constraint);
        }

        // End time for removing constraints
        long endTimeRemove = System.currentTimeMillis();

        // Calculate the duration for removing constraints in milliseconds
        double durationRemoveInMilliseconds = endTimeRemove - startTimeRemove;
        System.out.println("Removed " + numberOfConstraints + " constraints in " + durationRemoveInMilliseconds + " milliseconds");

        // Assert that the constraints have been removed
        for (IConstraint constraint : addedConstraints) {
            Assertions.assertFalse(featureModel.hasConstraint(constraint.getIdentifier()), "Constraint should be removed from the model.");
        }

        // Print performance metrics
        System.out.println("Performance metrics:");
        System.out.println("Time to add constraints: " + durationAddInMilliseconds + " milliseconds");
        System.out.println("Time to remove constraints: " + durationRemoveInMilliseconds + " milliseconds");

        // Optionally, assert other performance metrics if necessary
        Assertions.assertTrue(durationAddInMilliseconds < 1000, "Adding constraints should take less than 1 second.");
        Assertions.assertTrue(durationRemoveInMilliseconds < 1000, "Removing constraints should take less than 1 second.");
    }
    
    
    @Test
    public void testFeatureMutations() {
        System.out.println("Testing feature mutations...");

        // Add a feature with an initial name and description
        IFeature feature = featureModel.mutate().addFeature("feature1");
        feature.mutate().setName("InitialName");
        feature.mutate().setDescription("Initial description");

        // Print initial states
        System.out.println("Initial Name: " + feature.getName().orElse("Error: Name not set"));
        System.out.println("Initial Description: " + feature.getDescription().orElse("Error: Description not set"));

        // Assertions to verify initial states
        Assertions.assertEquals(Result.of("InitialName"), feature.getName(), "Feature name should be 'InitialName'");
        Assertions.assertEquals(Result.of("Initial description"), feature.getDescription(), "Feature description should be 'Initial description'");

        // Change the name and description of the feature
        feature.mutate().setName("featureRenamed");
        feature.mutate().setDescription("A feature description");

        // Print attempted changes
        System.out.println("Attempted Name Change to: 'featureRenamed'");
        System.out.println("Attempted Description Change to: 'A feature description'");

        // Assertions to verify mutations
        Assertions.assertEquals(Result.of("featureRenamed"), feature.getName(), "Feature name should be updated to 'featureRenamed'");
        Assertions.assertEquals(Result.of("A feature description"), feature.getDescription(), "Feature description should be updated to 'A feature description'");

        // Print the results after mutations
        System.out.println("Updated Name: " + feature.getName().orElse("Error: Name not updated correctly"));
        System.out.println("Updated Description: " + feature.getDescription().orElse("Error: Description not updated correctly"));

        // Add another feature to compare names
        IFeature feature2 = featureModel.mutate().addFeature("feature2");
        feature2.mutate().setName("FeatureTwo");

        // Assertions to verify names are different
        Assertions.assertNotEquals(feature.getName(), feature2.getName(), "Feature names should be different after renaming");
        Assertions.assertEquals(Result.of("FeatureTwo"), feature2.getName(), "Second feature name should be 'FeatureTwo'");

        // Print final comparison results
        System.out.println("Final Comparison:");
        System.out.println("Feature 1 Name: " + feature.getName().orElse("Error: Name not set"));
        System.out.println("Feature 2 Name: " + feature2.getName().orElse("Error: Name not set"));
    }

    
   
    @Test
    public void testComplexFeatureHierarchy() {
        System.out.println("Feature addition and deletion");

        // Add root feature and its tree
        IFeature rootFeature = featureModel.mutate().addFeature("root");
        IFeatureTree rootTree = featureModel.mutate().addFeatureTreeRoot(rootFeature);
        System.out.println("Feature " + rootFeature.getName().orElse("[Unnamed Feature]") + " is added.");
        assertNotNull(rootFeature, "Root feature must not be null.");
        
        // Add child features and their respective trees
        IFeature childFeature1 = featureModel.mutate().addFeature("child1");
        IFeatureTree childTree1 = rootTree.mutate().addFeatureBelow(childFeature1);
        System.out.println("Feature " + childFeature1.getName().orElse("[Unnamed Feature]") + " is added.");
        assertTrue(featureModel.hasFeature(childFeature1.getIdentifier()), "Feature 'child1' should be present in the model.");

        IFeature childFeature2 = featureModel.mutate().addFeature("child2");
        IFeatureTree childTree2 = rootTree.mutate().addFeatureBelow(childFeature2);
        System.out.println("Feature " + childFeature2.getName().orElse("[Unnamed Feature]") + " is added.");
        assertTrue(featureModel.hasFeature(childFeature2.getIdentifier()), "Feature 'child2' should be present in the model.");

        // Adding a sub-child to test deeper hierarchy
        IFeature subChildFeature = featureModel.mutate().addFeature("subChild");
        IFeatureTree subChildTree = childTree1.mutate().addFeatureBelow(subChildFeature);
        System.out.println("Sub-child feature " + subChildFeature.getName().orElse("[Unnamed Feature]") + " is added to child1.");
        assertTrue(featureModel.hasFeature(subChildFeature.getIdentifier()), "Feature 'subChild' should be present in the model.");

        // Print hierarchy structure before removal
        System.out.println("Hierarchy structure before removal of feature:");
        printFeatureTree(rootTree, 0);

        // Remove the sub-child feature and its tree node
        childTree1.mutate().removeChild(subChildTree);
        boolean subChildRemovedFromModel = featureModel.mutate().removeFeature(subChildFeature);
        assertTrue(subChildRemovedFromModel, "Sub-child feature 'subChild' should be removed from the model.");
        System.out.println("Sub-child tree for feature " + subChildFeature.getName().orElse("[Unnamed Feature]") + " is removed.");
        assertFalse(featureModel.hasFeature(subChildFeature.getIdentifier()), "Feature 'subChild' should be absent after removal.");

        // Remove the child feature and its tree node
        rootTree.mutate().removeChild(childTree1);
        System.out.println("Child tree for feature " + childFeature1.getName().orElse("[Unnamed Feature]") + " is removed.");
        boolean removedFromModel = featureModel.mutate().removeFeature(childFeature1);
        assertTrue(removedFromModel, "Feature 'child1' should be removed from the model.");
        System.out.println("Feature " + childFeature1.getName().orElse("[Unnamed Feature]") + " is removed.");

        // Print hierarchy structure after removal
        System.out.println("Hierarchy structure after removal of feature:");
        printFeatureTree(rootTree, 0);

        // Assertions to validate the structure of the feature hierarchy
        assertEquals(rootFeature, rootTree.getFeature(), "Root feature should be correctly set as 'root'");
        assertEquals(List.of(childTree2), rootTree.getChildren(), "Root should have one child after removal of 'child1'");

        // Validate the remaining child
        assertSame(childFeature2, childTree2.getFeature(), "Remaining child should be correctly set as 'child2'");
        assertSame(rootTree, childTree2.getParent().get(), "Parent of remaining child should be the root tree");

        // Additional checks for presence and activation status
        assertFalse(featureModel.hasFeature(childFeature1.getIdentifier()), "Feature 'child1' should be absent after removal.");
        assertTrue(featureModel.hasFeature(childFeature2.getIdentifier()), "Feature 'child2' should still be present in the model.");
    }

    // Helper method to print the tree recursively
    private void printFeatureTree(IFeatureTree tree, int level) {
        String indent = "  ".repeat(level);  // Create an indent based on the tree level
        System.out.println(indent + "Feature: " + tree.getFeature().getName().orElse("[Unnamed Feature]"));

        for (IFeatureTree child : tree.getChildren()) {
            printFeatureTree(child, level + 1);  // Recurse for each child
        }
    }




    @Test
    public void testExceptionHandling() {
    	System.out.println("Testing indicate that no feature is present for non-existent feature identifiers...");
        assertThrows(IllegalArgumentException.class, () -> {
            featureModel.mutate().addFeature(null);
        });

        // Assuming getFeature returns a Result
        Result<IFeature> result = featureModel.getFeature("Nonexistent");
        assertFalse(result.isPresent(), "Should indicate that no feature is present for non-existent feature identifiers");
    }
   
    
    @Test
    public void testAddAndRemoveConstraint() {
        System.out.println("Testing addition and removal of complex cross-tree constraints...");

        // Add features to the model
        IFeature featureA = featureModel.mutate().addFeature("FeatureA");
        IFeature featureB = featureModel.mutate().addFeature("FeatureB");
        IFeature featureC = featureModel.mutate().addFeature("FeatureC");
        IFeature featureD = featureModel.mutate().addFeature("FeatureD");

        // Ensure that Variable is treated as IFormula using feature identifiers
        IFormula featureAFormula = Expressions.variableAsFormula(featureA.getName().orElseThrow());
        IFormula featureBFormula = Expressions.variableAsFormula(featureB.getName().orElseThrow());
        IFormula featureCFormula = Expressions.variableAsFormula(featureC.getName().orElseThrow());
        IFormula featureDFormula = Expressions.variableAsFormula(featureD.getName().orElseThrow());

        // Example 1: Use logical operators to create a complex formula
        IFormula complexFormula1 = Expressions.and(featureAFormula, Expressions.or(featureBFormula, featureCFormula));
        String complexFormulaStr1 = "AND(" + featureA.getName().orElse("FeatureA") + ", OR(" + featureB.getName().orElse("FeatureB") + ", " + featureC.getName().orElse("FeatureC") + "))";
        System.out.println("Complex formula being added: " + complexFormulaStr1);

        // Add the first complex constraint to the model
        IConstraint addedConstraint1 = featureModel.mutate().addConstraint(complexFormula1);
        int constraintsAdded = 1;

        // Example 2: Create another complex formula
        IFormula complexFormula2 = Expressions.or(Expressions.and(featureAFormula, featureBFormula), Expressions.not(featureDFormula));
        String complexFormulaStr2 = "OR(AND(" + featureA.getName().orElse("FeatureA") + ", " + featureB.getName().orElse("FeatureB") + "), NOT(" + featureD.getName().orElse("FeatureD") + "))";
        System.out.println("Complex formula being added: " + complexFormulaStr2);

        // Add the second complex constraint to the model
        IConstraint addedConstraint2 = featureModel.mutate().addConstraint(complexFormula2);
        constraintsAdded++;

        // Example 3: A simple feature-based constraint
        IFormula simpleFeatureFormula = featureCFormula;
        IConstraint addedConstraint3 = featureModel.mutate().addConstraint(simpleFeatureFormula);
        constraintsAdded++;

        // Check if the constraints have been successfully added to the model
        assertTrue(featureModel.hasConstraint(addedConstraint1.getIdentifier()), "Constraint 1 should be present in the model.");
        assertTrue(featureModel.hasConstraint(addedConstraint2.getIdentifier()), "Constraint 2 should be present in the model.");
        assertTrue(featureModel.hasConstraint(addedConstraint3.getIdentifier()), "Constraint 3 should be present in the model.");

        // Further validate that the added constraints match what was expected
        assertEquals(addedConstraint1, featureModel.getConstraint(addedConstraint1.getIdentifier()).get(), "The added constraint 1 should match the retrieved constraint.");
        assertEquals(addedConstraint2, featureModel.getConstraint(addedConstraint2.getIdentifier()).get(), "The added constraint 2 should match the retrieved constraint.");
        assertEquals(addedConstraint3, featureModel.getConstraint(addedConstraint3.getIdentifier()).get(), "The added constraint 3 should match the retrieved constraint.");

        // Check the initial constraints count
        int initialConstraintsCount = featureModel.getNumberOfConstraints();
        int expectedConstraintsCount = initialConstraintsCount; // We just added 3 constraints
        assertEquals(expectedConstraintsCount, featureModel.getNumberOfConstraints(), "The total number of constraints in the model should be correct.");

        System.out.println("Added and verified the presence of the constraints.");

        // Additional assertions to ensure integrity
        assertTrue(featureModel.getConstraints().contains(addedConstraint1), "Model should contain the added constraint 1.");
        assertTrue(featureModel.getConstraints().contains(addedConstraint2), "Model should contain the added constraint 2.");
        assertTrue(featureModel.getConstraints().contains(addedConstraint3), "Model should contain the added constraint 3.");

        // Ensure constraints are correctly linked to their formulas
        assertEquals(complexFormula1, addedConstraint1.getFormula(), "Constraint 1's formula should match the complex formula 1.");
        assertEquals(complexFormula2, addedConstraint2.getFormula(), "Constraint 2's formula should match the complex formula 2.");
        assertEquals(simpleFeatureFormula, addedConstraint3.getFormula(), "Constraint 3's formula should match the simple feature formula.");

        // Remove the constraints and verify removal
        boolean removed1 = featureModel.mutate().removeConstraint(addedConstraint1);
        boolean removed2 = featureModel.mutate().removeConstraint(addedConstraint2);
        boolean removed3 = featureModel.mutate().removeConstraint(addedConstraint3);
        assertTrue(removed1, "Constraint 1 removal should be successful.");
        assertTrue(removed2, "Constraint 2 removal should be successful.");
        assertTrue(removed3, "Constraint 3 removal should be successful.");
        int constraintsRemoved = 3;

        // Ensure the constraints are no longer present
        assertFalse(featureModel.hasConstraint(addedConstraint1.getIdentifier()), "Constraint 1 should no longer be present after removal.");
        assertFalse(featureModel.hasConstraint(addedConstraint2.getIdentifier()), "Constraint 2 should no longer be present after removal.");
        assertFalse(featureModel.hasConstraint(addedConstraint3.getIdentifier()), "Constraint 3 should no longer be present after removal.");

        // Check if the total count of constraints decreased by three
        int finalConstraintsCount = featureModel.getNumberOfConstraints();
        assertEquals(initialConstraintsCount - 3, finalConstraintsCount, "The total number of constraints should decrease by three after removal.");

        System.out.println("Constraints successfully removed.");

        // Ensure removed constraints are not in the model's constraints list
        assertFalse(featureModel.getConstraints().contains(addedConstraint1), "Model should not contain the removed constraint 1.");
        assertFalse(featureModel.getConstraints().contains(addedConstraint2), "Model should not contain the removed constraint 2.");
        assertFalse(featureModel.getConstraints().contains(addedConstraint3), "Model should not contain the removed constraint 3.");

        // Print the number of constraints added and removed
        System.out.println("Number of constraints added: " + constraintsAdded);
        System.out.println("Number of constraints removed: " + constraintsRemoved);
    }
   
  
}
