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
import java.util.*;
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

  
   
//sarthak
    //kind of internal-decision making controlled mechanism whether root feature should be deleted.
    
    private static class TestableFeatureModel extends FeatureModel {
        private boolean shouldDeleteRootFeatureFlag = true;

        public TestableFeatureModel(IIdentifier iIdentifier) {
            super();   //super(identifier)
        }
        
        public void setShouldDeleteRootFeatureFlag(boolean flag) {
            this.shouldDeleteRootFeatureFlag = flag;
        }

        @Override
        protected boolean shouldDeleteRootFeature(IFeature feature) {
            return this.shouldDeleteRootFeatureFlag;
        }

        public void deleteFeatureAndPromoteChild(IIdentifier featureId, IIdentifier childId) {
            // Retrieve the feature to be deleted
            Result<IFeature> maybeFeatureToDelete = getFeature(featureId);
            if (maybeFeatureToDelete.isEmpty()) {
                // Handle case where the feature doesn't exist
                return;
            }

            // Retrieve the child feature to be promoted
            Result<IFeature> maybeChildToPromote = getFeature(childId);
            if (maybeChildToPromote.isEmpty()) {
                // Handle case where the child feature doesn't exist
                return;
            }

            // Remove the feature to be deleted
            IFeature featureToDelete = maybeFeatureToDelete.get();
            featureToDelete.getFeatureTree().ifPresent(featureTree -> featureTree.mutate().removeFromTree());

            // Promote the child feature
            IFeature childToPromote = maybeChildToPromote.get();
            mutate().addFeatureTreeRoot(childToPromote);
        }

    }   
        
     //sarthak  
    
    
    
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
    }
    
 //sarthak
    
    
    
    
    
    
    @Test
    public void testDeleteRootFeatureAndPromoteChildren() {
        // Create a TestableFeatureModel instance and cast featureModel to it
        featureModel = new TestableFeatureModel(Identifiers.newCounterIdentifier());
        TestableFeatureModel testableFeatureModel = (TestableFeatureModel) featureModel;

        // Setup the initial tree structure with a simple hierarchy
        IFeature rootFeature = testableFeatureModel.mutate().addFeature("root");
        IFeature childFeature1 = testableFeatureModel.mutate().addFeature("child1");
        IFeature childFeature2 = testableFeatureModel.mutate().addFeature("child2");
        IFeature childFeature3 = testableFeatureModel.mutate().addFeature("child3");
        IFeature childFeature4 = testableFeatureModel.mutate().addFeature("child4");

        testableFeatureModel.mutate().addFeatureTreeRoot(rootFeature)
            .mutate().addFeatureBelow(childFeature1)
            .mutate().addFeatureBelow(childFeature2)
            .mutate().addFeatureBelow(childFeature3)
            .mutate().addFeatureBelow(childFeature4);

        
        // Add grandchildren
        IFeature childFeature1a = testableFeatureModel.mutate().addFeature("a");
        IFeature childFeature1b = testableFeatureModel.mutate().addFeature("b");
        IFeature childFeature2c = testableFeatureModel.mutate().addFeature("c");
        IFeature childFeature3d = testableFeatureModel.mutate().addFeature("d");
        IFeature childFeature3e = testableFeatureModel.mutate().addFeature("e");
        IFeature childFeature4f = testableFeatureModel.mutate().addFeature("f");

        childFeature1.getFeatureTree().get().mutate().addFeatureBelow(childFeature1a).mutate().addFeatureBelow(childFeature1b);
        childFeature2.getFeatureTree().get().mutate().addFeatureBelow(childFeature2c);
        childFeature3.getFeatureTree().get().mutate().addFeatureBelow(childFeature3d).mutate().addFeatureBelow(childFeature3e);
        childFeature4.getFeatureTree().get().mutate().addFeatureBelow(childFeature4f);

        // Execute the deletion of the root feature
        testableFeatureModel.deleteFeatureAndPromoteChildren(rootFeature.getIdentifier());

        // Print the status of each child feature
        List<IFeature> children = Arrays.asList(childFeature1, childFeature2, childFeature3, childFeature4);
        for (IFeature child : children) {
            boolean promoted = testableFeatureModel.deleteFeatureAndPromoteChildren(child.getIdentifier());
            System.out.println("Status of original children:");
            System.out.println(child + " is promoted to root: " + promoted);
        }

        // Assert that all original children of the root are now root features
    //    Assertions.assertTrue(childrenPromoted, "Original children should be promoted to roots");
    }

    
    	
    // this test checks if root feature is still present and its children are intact
    
    
    @Test
    public void testKeepRootFeatureAndChildrenIntact() {
        // Create a TestableFeatureModel instance and cast featureModel to it
        featureModel = new TestableFeatureModel(Identifiers.newCounterIdentifier());
        TestableFeatureModel testableFeatureModel = (TestableFeatureModel) featureModel;

        // Setup the initial tree structure with a simple hierarchy
        IFeature rootFeature = testableFeatureModel.mutate().addFeature("root");
        IFeature childFeature1 = testableFeatureModel.mutate().addFeature("child1");
        IFeature childFeature2 = testableFeatureModel.mutate().addFeature("child2");
        IFeature childFeature3 = testableFeatureModel.mutate().addFeature("child3");
        IFeature childFeature4 = testableFeatureModel.mutate().addFeature("child4");

        IFeatureTree rootTree = testableFeatureModel.mutate().addFeatureTreeRoot(rootFeature);

        rootTree.mutate().addFeatureBelow(childFeature1);
        rootTree.mutate().addFeatureBelow(childFeature2);
        rootTree.mutate().addFeatureBelow(childFeature3);
        rootTree.mutate().addFeatureBelow(childFeature4);

        // Add grandchildren
        IFeature childFeature1a = testableFeatureModel.mutate().addFeature("a");
        IFeature childFeature1b = testableFeatureModel.mutate().addFeature("b");
        IFeature childFeature2c = testableFeatureModel.mutate().addFeature("c");
        IFeature childFeature3d = testableFeatureModel.mutate().addFeature("d");
        IFeature childFeature3e = testableFeatureModel.mutate().addFeature("e");
        IFeature childFeature4f = testableFeatureModel.mutate().addFeature("f");

        childFeature1.getFeatureTree().get().mutate().addFeatureBelow(childFeature1a).mutate().addFeatureBelow(childFeature1b);
        childFeature2.getFeatureTree().get().mutate().addFeatureBelow(childFeature2c);
        childFeature3.getFeatureTree().get().mutate().addFeatureBelow(childFeature3d).mutate().addFeatureBelow(childFeature3e);
        childFeature4.getFeatureTree().get().mutate().addFeatureBelow(childFeature4f);

        // Set the flag to prevent root feature deletion
        testableFeatureModel.setShouldDeleteRootFeatureFlag(false);

        // Execute the deletion of the root feature
        testableFeatureModel.deleteFeatureAndPromoteChildren(rootFeature.getIdentifier());

        // Assert that the root feature is still present
        Assertions.assertTrue(testableFeatureModel.getFeatures().contains(rootFeature));

        // Debug statements to log the contents of actualChildren and expectedChildren lists
        List<IFeatureTree> actualChildren = (List<IFeatureTree>) rootFeature.getFeatureTree().map(IFeatureTree::getChildren).orElse(Collections.emptyList());
        System.out.println("Actual Children: " + actualChildren);
        System.out.println("Expected Children: " + Arrays.asList(childFeature1.getFeatureTree().get(), childFeature2.getFeatureTree().get(), childFeature3.getFeatureTree().get(), childFeature4.getFeatureTree().get()));

        // Assertion for each child feature
        Assertions.assertTrue(actualChildren.contains(childFeature1.getFeatureTree().get()), "Child feature 1 should be present");
        Assertions.assertTrue(actualChildren.contains(childFeature2.getFeatureTree().get()), "Child feature 2 should be present");
        Assertions.assertTrue(actualChildren.contains(childFeature3.getFeatureTree().get()), "Child feature 3 should be present");
        Assertions.assertTrue(actualChildren.contains(childFeature4.getFeatureTree().get()), "Child feature 4 should be present");

        // Assertion for the number of children
        Assertions.assertEquals(4, actualChildren.size(), "Number of children should be 4");

        // Verify that all expected children are present in the actual list of children
        List<IFeatureTree> expectedChildren = Arrays.asList(childFeature1.getFeatureTree().get(), childFeature2.getFeatureTree().get(), childFeature3.getFeatureTree().get(), childFeature4.getFeatureTree().get());
        Assertions.assertTrue(actualChildren.containsAll(expectedChildren), "All expected children should be present in the feature tree of the root feature");
    }

    
    
    @Test
    public void testMoveChildBelowGrandchildShouldFail() {
        // Setup the initial tree structure
        MoveFeature moveFeature = new MoveFeature();
        MoveFeature.TreeNode root = moveFeature.createTree();

        // Print initial tree structure
        System.out.println("Initial tree structure:");
        moveFeature.printTree(root, "");

        // Get nodes
        MoveFeature.TreeNode child1 = root.children.get(0); // Child1
        MoveFeature.TreeNode gc1 = child1.children.get(0);  // GC1

        // Attempt to move Child1 below GC1
        System.out.println("\nAttempting to move Child1 below GC1:");
        try {
            moveFeature.moveNode(root, child1, gc1);
        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        }

        // Print tree structure after attempting the invalid move
        System.out.println("\nTree structure after attempting invalid move:");
        moveFeature.printTree(root, "");

        // Check the exception message for moving child below grandchild
        Exception exception1 = assertThrows(IllegalArgumentException.class, () -> {
            moveFeature.moveNode(root, child1, gc1);
        });
        String expectedMessage1 = "Cannot move a node below itself or one of its own descendants or vice versa.";
        String actualMessage1 = exception1.getMessage();
        assertTrue(actualMessage1.contains(expectedMessage1));

        // Attempt to add Child1 as a grandchild under itself
        System.out.println("\nAttempting to add Child1 as a grandchild under itself:");
        try {
            moveFeature.moveNode(root, child1, child1);
        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        }

        // Print tree structure after attempting the invalid addition
        System.out.println("\nTree structure after attempting invalid addition:");
        moveFeature.printTree(root, "");

        // Check the exception message for adding child as grandchild under itself
        Exception exception2 = assertThrows(IllegalArgumentException.class, () -> {
            moveFeature.moveNode(root, child1, child1);
        });
        String expectedMessage2 = "Cannot move a node below itself or one of its own descendants or vice versa.";
        String actualMessage2 = exception2.getMessage();
        assertTrue(actualMessage2.contains(expectedMessage2));
    }
    
    
    @Test
    public void testMoveChild1AboveItself() {
        // Setup the initial tree structure
        MoveFeature moveFeature = new MoveFeature();
        MoveFeature.TreeNode root = moveFeature.createTree();

        // Print initial tree structure
        System.out.println("Initial tree structure:");
        moveFeature.printTree(root, "");

        // Move Child1 above itself (no-op)
        MoveFeature.TreeNode child1 = root.children.get(0); // Child1

        System.out.println("\nAttempting to move Child1 above itself:");
        try {
            // Since the operation is conceptually a no-op, we just ensure no exception is thrown
            moveFeature.moveNode(root, child1, root);
        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        }

        // Print tree structure after attempting the move
        System.out.println("\nTree structure after attempting to move Child1 above itself:");
        moveFeature.printTree(root, "");

        // Verify the tree structure remains unchanged
        assertTrue(root.children.contains(child1), "Root should still have Child1 as a child.");
        assertEquals("Child1", child1.name, "The name of Child1 should remain unchanged.");
    }




    @Test
    public void testSwapGrandchildren() {
        // Setup the initial tree structure
        MoveFeature moveFeature = new MoveFeature();
        MoveFeature.TreeNode root = moveFeature.createTree();

        // Add additional grandchildren
        MoveFeature.TreeNode gc4 = root.children.get(1).children.get(0); // GC4
        MoveFeature.TreeNode gc7 = new MoveFeature.TreeNode("GC7");
        MoveFeature.TreeNode gc8 = new MoveFeature.TreeNode("GC8");
        MoveFeature.TreeNode gc9 = new MoveFeature.TreeNode("GC9");
        MoveFeature.TreeNode gc10 = new MoveFeature.TreeNode("GC10");

        gc4.addChild(gc7);
        gc4.addChild(gc8);
        gc7.addChild(gc9);
        gc7.addChild(gc10);

        // Print initial tree structure
        System.out.println("Initial tree structure:");
        moveFeature.printTree(root, "");

        // Get nodes to swap
        MoveFeature.TreeNode gc1 = root.children.get(0).children.get(0); // GC1
        MoveFeature.TreeNode gc2 = root.children.get(0).children.get(1); // GC2
        MoveFeature.TreeNode gc3 = root.children.get(0).children.get(2); // GC3
        MoveFeature.TreeNode gc5 = root.children.get(1).children.get(1); // GC5
        MoveFeature.TreeNode gc6 = root.children.get(1).children.get(2); // GC6

        // Swap grandchildren GC1 and GC4
        moveFeature.swapGrandchildren(root, gc1, gc4);
        System.out.println("\nTree structure after swapping GC1 and GC4:");
        moveFeature.printTree(root, "");
        assertEquals("GC4", root.children.get(0).children.get(0).name);
        assertEquals("GC1", root.children.get(1).children.get(0).name);

        // Optional: Swap grandchildren GC2 and GC5
        moveFeature.swapGrandchildren(root, gc2, gc5);
        System.out.println("\nTree structure after swapping GC2 and GC5:");
        moveFeature.printTree(root, "");
        assertEquals("GC5", root.children.get(0).children.get(1).name);
        assertEquals("GC2", root.children.get(1).children.get(1).name);

        // Optional: Swap grandchildren GC3 and GC6
        moveFeature.swapGrandchildren(root, gc3, gc6);
        System.out.println("\nTree structure after swapping GC3 and GC6:");
        moveFeature.printTree(root, "");
        assertEquals("GC6", root.children.get(0).children.get(2).name);
        assertEquals("GC3", root.children.get(1).children.get(2).name);
    }
    
    
    @Test
    public void testMoveGrandchild7ToChild2() {
        // Setup the initial tree structure
        MoveFeature moveFeature = new MoveFeature();
        MoveFeature.TreeNode root = moveFeature.createTree();

        // Add additional grandchildren
        MoveFeature.TreeNode gc4 = root.children.get(1).children.get(0); // GC4
        MoveFeature.TreeNode gc7 = new MoveFeature.TreeNode("GC7");
        MoveFeature.TreeNode gc8 = new MoveFeature.TreeNode("GC8");
        MoveFeature.TreeNode gc9 = new MoveFeature.TreeNode("GC9");
        MoveFeature.TreeNode gc10 = new MoveFeature.TreeNode("GC10");

        gc4.addChild(gc7);
        gc4.addChild(gc8);
        gc7.addChild(gc9);
        gc7.addChild(gc10);

        // Print initial tree structure
        System.out.println("Initial tree structure:");
        moveFeature.printTree(root, "");

        // Move GC7 from below GC4 to below Child2
        MoveFeature.TreeNode child2 = root.children.get(1); // Child2

        System.out.println("\nMoving GC7 from below GC4 to below Child2:");
        try {
            moveFeature.moveNode(root, gc7, child2);
        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        }

        // Print tree structure after moving GC7
        System.out.println("\nTree structure after moving GC7:");
        moveFeature.printTree(root, "");

        // Verify the move
        assertFalse(gc4.children.contains(gc7), "GC4 should no longer have GC7 as a child.");
        assertTrue(child2.children.contains(gc7), "Child2 should now have GC7 as a child.");
    }

    
    @Test
    public void testMoveGrandchild8ToChild2() {
        // Setup the initial tree structure
        MoveFeature moveFeature = new MoveFeature();
        MoveFeature.TreeNode root = moveFeature.createTree();

        // Add additional grandchildren
        MoveFeature.TreeNode gc4 = root.children.get(1).children.get(0); // GC4
        MoveFeature.TreeNode gc7 = new MoveFeature.TreeNode("GC7");
        MoveFeature.TreeNode gc8 = new MoveFeature.TreeNode("GC8");
        MoveFeature.TreeNode gc9 = new MoveFeature.TreeNode("GC9");
        MoveFeature.TreeNode gc10 = new MoveFeature.TreeNode("GC10");

        gc4.addChild(gc7);
        gc4.addChild(gc8);
        gc7.addChild(gc9);
        gc7.addChild(gc10);

        // Print initial tree structure
        System.out.println("Initial tree structure:");
        moveFeature.printTree(root, "");

        // Move GC8 from below GC4 to below Child2
        MoveFeature.TreeNode child2 = root.children.get(1); // Child2

        System.out.println("\nMoving GC8 from below GC4 to below Child2:");
        try {
            moveFeature.moveNode(root, gc8, child2);
        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        }

        // Print tree structure after moving GC8
        System.out.println("\nTree structure after moving GC8:");
        moveFeature.printTree(root, "");

        // Verify the move
        assertFalse(gc4.children.contains(gc8), "GC4 should no longer have GC8 as a child.");
        assertTrue(child2.children.contains(gc8), "Child2 should now have GC8 as a child.");
    }

    
    @Test
    public void testMoveGrandchild7ToRoot() {
        // Setup the initial tree structure
        MoveFeature moveFeature = new MoveFeature();
        MoveFeature.TreeNode root = moveFeature.createTree();

        // Add additional grandchildren
        MoveFeature.TreeNode gc4 = root.children.get(1).children.get(0); // GC4
        MoveFeature.TreeNode gc7 = new MoveFeature.TreeNode("GC7");
        MoveFeature.TreeNode gc8 = new MoveFeature.TreeNode("GC8");
        MoveFeature.TreeNode gc9 = new MoveFeature.TreeNode("GC9");
        MoveFeature.TreeNode gc10 = new MoveFeature.TreeNode("GC10");

        gc4.addChild(gc7);
        gc4.addChild(gc8);
        gc7.addChild(gc9);
        gc7.addChild(gc10);

        // Print initial tree structure
        System.out.println("Initial tree structure:");
        moveFeature.printTree(root, "");

        // Move GC7 from below GC4 to below Root
        System.out.println("\nMoving GC7 from below GC4 to below Root:");
        try {
            moveFeature.moveNode(root, gc7, root);
        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        }

        // Print tree structure after moving GC7
        System.out.println("\nTree structure after moving GC7:");
        moveFeature.printTree(root, "");

        // Verify the move
        assertFalse(gc4.children.contains(gc7), "GC4 should no longer have GC7 as a child.");
        assertTrue(root.children.contains(gc7), "Root should now have GC7 as a child.");

        // Print the name of GC7 to confirm it has not changed
        System.out.println("Name of moved node: " + gc7.name);
        assertEquals("GC7", gc7.name, "The name of GC7 should remain unchanged.");
    }

    
    @Test
    public void testMoveGrandchild8ToRoot() {
        // Setup the initial tree structure
        MoveFeature moveFeature = new MoveFeature();
        MoveFeature.TreeNode root = moveFeature.createTree();

        // Add additional grandchildren
        MoveFeature.TreeNode gc4 = root.children.get(1).children.get(0); // GC4
        MoveFeature.TreeNode gc7 = new MoveFeature.TreeNode("GC7");
        MoveFeature.TreeNode gc8 = new MoveFeature.TreeNode("GC8");
        MoveFeature.TreeNode gc9 = new MoveFeature.TreeNode("GC9");
        MoveFeature.TreeNode gc10 = new MoveFeature.TreeNode("GC10");

        gc4.addChild(gc7);
        gc4.addChild(gc8);
        gc7.addChild(gc9);
        gc7.addChild(gc10);

        // Print initial tree structure
        System.out.println("Initial tree structure:");
        moveFeature.printTree(root, "");

        // Move GC8 from below GC4 to below Root
        System.out.println("\nMoving GC8 from below GC4 to below Root:");
        try {
            moveFeature.moveNode(root, gc8, root);
        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        }

        // Print tree structure after moving GC8
        System.out.println("\nTree structure after moving GC8:");
        moveFeature.printTree(root, "");

        // Verify the move
        assertFalse(gc4.children.contains(gc8), "GC4 should no longer have GC8 as a child.");
        assertTrue(root.children.contains(gc8), "Root should now have GC8 as a child.");

        // Print the name of GC8 to confirm it has not changed
        System.out.println("Name of moved node: " + gc8.name);
        assertEquals("GC8", gc8.name, "The name of GC8 should remain unchanged.");
    }

    
    @Test
    public void testMoveGrandchild5ToRoot() {
        // Setup the initial tree structure
        MoveFeature moveFeature = new MoveFeature();
        MoveFeature.TreeNode root = moveFeature.createTree();

        // Add additional grandchildren
        MoveFeature.TreeNode gc4 = root.children.get(1).children.get(0); // GC4
        MoveFeature.TreeNode gc5 = root.children.get(1).children.get(1); // GC5
        MoveFeature.TreeNode gc7 = new MoveFeature.TreeNode("GC7");
        MoveFeature.TreeNode gc8 = new MoveFeature.TreeNode("GC8");
        MoveFeature.TreeNode gc9 = new MoveFeature.TreeNode("GC9");
        MoveFeature.TreeNode gc10 = new MoveFeature.TreeNode("GC10");

        gc4.addChild(gc7);
        gc4.addChild(gc8);
        gc7.addChild(gc9);
        gc7.addChild(gc10);

        // Print initial tree structure
        System.out.println("Initial tree structure:");
        moveFeature.printTree(root, "");

        // Move GC5 from below Child2 to below Root
        System.out.println("\nMoving GC5 from below Child2 to below Root:");
        try {
            moveFeature.moveNode(root, gc5, root);
        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        }

        // Print tree structure after moving GC5
        System.out.println("\nTree structure after moving GC5:");
        moveFeature.printTree(root, "");

        // Verify the move
        assertFalse(root.children.get(1).children.contains(gc5), "Child2 should no longer have GC5 as a child.");
        assertTrue(root.children.contains(gc5), "Root should now have GC5 as a child.");
        assertEquals("GC5", root.children.get(2).name, "GC5 should be the third child of the root.");

        // Verify that the structure of other grandchildren remains unchanged
        assertTrue(gc4.children.contains(gc7), "GC4 should still have GC7 as a child.");
        assertTrue(gc4.children.contains(gc8), "GC4 should still have GC8 as a child.");
        assertTrue(gc7.children.contains(gc9), "GC7 should still have GC9 as a child.");
        assertTrue(gc7.children.contains(gc10), "GC7 should still have GC10 as a child.");
    }



    
}
    
    
    

    



	//sarthak


