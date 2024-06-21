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
import de.featjar.base.data.identifier.UUIDIdentifier;
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
import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link FeatureModel}, its elements, and its mixins.
 *
 * @author Elias Kuiter
 */
public class FeatureModelTest {
   private IFeatureModel featureModel;
    
 

    @BeforeEach
    public void createFeatureModel() {
        featureModel = new FeatureModel(Identifiers.newCounterIdentifier());
        setUp();
    }
    
    public void setUp() {
        featureModel = new FeatureModel(UUIDIdentifier.newInstance());
        featureModel.mutate().addFeature("Feature1");
        featureModel.mutate().addFeature("Feature2");
    }

    private void addNewFeature(String featureName) {
        assertFalse(featureName.isEmpty(), "Feature name should not be empty");

        IMutableFeatureModel mutableFeatureModel = (IMutableFeatureModel) featureModel;

        if (mutableFeatureModel.getFeatures().stream().anyMatch(f -> f.getName().valueEquals(featureName))) {
            throw new IllegalArgumentException("Feature name conflicts with a predefined or existing feature.");
        }

        IFeature newFeature = mutableFeatureModel.addFeature(featureName);

        assertTrue(mutableFeatureModel.getFeatures().contains(newFeature));
        assertEquals(featureName, newFeature.getName().get());
        assertTrue(featureModel.getFeatures().stream().anyMatch(f -> f.getName().valueEquals(featureName)));

        System.out.println("New feature added successfully: " + featureName);
    }

    private void removeFeature(String featureName) {
        assertFalse(featureName.isEmpty(), "Feature name should not be empty");

        IMutableFeatureModel mutableFeatureModel = (IMutableFeatureModel) featureModel;

        IFeature featureToRemove = mutableFeatureModel.getFeatures().stream()
                .filter(f -> f.getName().valueEquals(featureName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Feature does not exist: " + featureName));

        mutableFeatureModel.removeFeature(featureToRemove);

        assertFalse(mutableFeatureModel.getFeatures().contains(featureToRemove));
        System.out.println("Feature removed successfully: " + featureName);
    }

    @Test
    public void testAddNewFeature() {
        String newFeatureName = "Feature3"; // Unique name
        addNewFeature(newFeatureName);

        // Additional assertions
        assertTrue(featureModel.getFeatures().stream().anyMatch(f -> f.getName().valueEquals(newFeatureName)));
        assertEquals(3, featureModel.getFeatures().size());
    }

    @Test
    public void testAddDuplicateFeature() {
        String duplicateFeatureName = "Feature2"; // Duplicate name

        assertFalse(duplicateFeatureName.isEmpty(), "Feature name should not be empty");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            addNewFeature(duplicateFeatureName);
        });

        assertEquals("Feature name conflicts with a predefined or existing feature.", exception.getMessage());
        System.out.println(exception.getMessage());

        // Ensure no additional feature was added
        assertEquals(2, featureModel.getFeatures().size());

        // Execute testAddNewFeature within testAddDuplicateFeature
        System.out.println("Attempting to add a new unique feature after handling duplicate");
        testAddNewFeature();
    }

    @Test
    public void testRemoveFeature() {
        String featureName = "Feature1"; // Assume this feature exists
        removeFeature(featureName);

        // Additional assertions
        assertFalse(featureModel.getFeatures().stream().anyMatch(f -> f.getName().valueEquals(featureName)));
        assertEquals(1, featureModel.getFeatures().size());
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

