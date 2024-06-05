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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.logging.Logger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.featjar.base.data.Result;
import de.featjar.base.data.identifier.IIdentifier;
import de.featjar.base.data.identifier.Identifiers;
import de.featjar.base.tree.Trees;
import de.featjar.base.tree.visitor.TreePrinter;
import de.featjar.formula.structure.Expressions;
import de.featjar.formula.structure.formula.IFormula;

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
    	// Lines for checking the name and description of the feature model are empty or not
    	Assertions.assertEquals(Result.empty(), featureModel.getName());
    	Assertions.assertEquals(Result.empty(), featureModel.getDescription());
    	
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

        TreePrinter visitor = new TreePrinter();
        visitor.setToStringFunction(tree -> ((IFeatureTree) tree).getFeature().getName().get());
        
		System.out.println(Trees.traverse(rootFeature.getFeatureTree().get(), visitor).get());
        Assertions.assertEquals(Result.of(rootFeature), featureModel.getFeature("root2"));
        assertEquals(List.of(childTree), rootFeature.getFeatureTree().get().getChildren());
        assertEquals(rootFeature.getFeatureTree(), childTree.getParent());
        childTree.mutate().removeFromTree();
        assertEquals(List.of(), rootFeature.getFeatureTree().get().getChildren());
		System.out.println(Trees.traverse(rootFeature.getFeatureTree().get(), visitor).get());	
		System.out.println("--------------------------");
		printFeatureTreeStructure(rootFeature);
		System.out.println("--------------------------");

    }
    
    
    
    // These methods are added to test the feature tree structure
    
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
            Logger.getLogger("Hello");
        }
    }

    
    
    
    // This method checks performance metrics for adding all features i.e. if there are 2 features it will show time around 10ms.
    @Test
    public void testPerformance() {
        System.out.println("Testing performance metrics...");
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 10; i++) {
            featureModel.mutate().addFeature("feature" + i);
        }
        long endTime = System.currentTimeMillis();
        System.out.println("Time taken for adding all features: " + (endTime - startTime) + " ms"); 
        Assertions.assertEquals(10, featureModel.getNumberOfFeatures());
    }
    
    
    
    // The ability to Mutate feature attributes i.e here setting name and description of the feature and verifying 
    @Test
    public void testFeatureMutations() {
        System.out.println("Testing feature mutations...");
        IFeature feature = featureModel.mutate().addFeature("feature1");
        feature.mutate().setName("featureRenamed");
        feature.mutate().setDescription("A feature description");
        Assertions.assertEquals(Result.of("featureRenamed"), feature.getName());
        Assertions.assertEquals(Result.of("A feature description"), feature.getDescription());
    }

    
    // verifies the handling of complex constraints within the FeatureModel i.e creates complex constraint using Logical Expressions
    @Test
    public void testComplexConstraints() {
        System.out.println("Testing complex constraints...");
        IFormula complexExpression = Expressions.and(Expressions.True, Expressions.not(Expressions.False));
        IConstraint complexConstraint = featureModel.mutate().addConstraint(complexExpression);
        Assertions.assertTrue(featureModel.hasConstraint(complexConstraint.getIdentifier()));
        Assertions.assertEquals(Result.of(complexConstraint), featureModel.getConstraint(complexConstraint.getIdentifier()));
    }
    


    
//    // Test concurrency i.e. if two threads are trying to add features at the same time, it should be thread safe
//    @Test
//    public void testConcurrency() {
//        System.out.println("Testing concurrency...");
//        Assertions.assertTimeout(Duration.ofSeconds(10), () -> {
//            Runnable task = () -> {
//                for (int i = 0; i < 1000; i++) {
//                	synchronized (featureModel) {          // Synchronized block will help here for Thread safety
//                    featureModel.mutate().addFeature("feature" + i); // work: to add one test where we try to add 2 same features
//                	}
//                }
//            };
//            Thread thread1 = new Thread(task);
//            Thread thread2 = new Thread(task);
//            thread1.start();
//            thread2.start();
//            thread1.join();
//            thread2.join();
//        });
//        
//        // to verify that every feature has been added
//        Assertions.assertTrue(featureModel.getNumberOfFeatures() >= 1000);
//        
//    }
    
    // Test the feature activation and deactivation 
    @Test
    public void testFeatureActivation() {
    	System.out.println("Testing activation and deactivation of features...");
        IFeature feature = featureModel.mutate().addFeature("FeatureX");
        IIdentifier featureId = feature.getIdentifier();

        // Test activating the feature
        featureModel.activateFeature(featureId);
        assertTrue(featureModel.isFeatureActive(featureId), "Feature should be active after activation.");

        // Test deactivating the feature
        featureModel.deactivateFeature(featureId);
        assertFalse(featureModel.isFeatureActive(featureId), "Feature should be inactive after deactivation.");
    }

    // Ensures that adding a null feature throws an IllegalArgumentException and that attempting to get a non-existent feature returns an empty Result
    @Test
    public void testExceptionHandling() {
    	System.out.println("Testing indicate that no feature is present for non-existent feature identifiers...");
        assertThrows(IllegalArgumentException.class, () -> {
            featureModel.mutate().addFeature(null);
        }, "Expected an IllegalArgumentException to be thrown when trying to add a null feature.");

        // Assuming getFeature returns a Result
        Result<IFeature> result = featureModel.getFeature("Nonexistent");
        assertFalse(result.isPresent(), "Should indicate that no feature is present for non-existent feature identifiers");
    }
}
