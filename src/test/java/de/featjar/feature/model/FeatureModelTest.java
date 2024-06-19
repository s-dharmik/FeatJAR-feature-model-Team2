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
import de.featjar.feature.model.TestFeature;

/**
 * Tests for {@link FeatureModel}, its elements, and its mixins.
 * Updated to include MoveFeature tests.
 */
public class FeatureModelTest {
    IFeatureModel featureModel;

    @BeforeEach
    public void createFeatureModel() {
        featureModel = new FeatureModel(Identifiers.newCounterIdentifier());
    }

    private static class TestableFeatureModel extends FeatureModel {
        private boolean shouldDeleteRootFeatureFlag = true;

        public TestableFeatureModel(IIdentifier iIdentifier) {
            super(iIdentifier);
        }
        
        public void setShouldDeleteRootFeatureFlag(boolean flag) {
            this.shouldDeleteRootFeatureFlag = flag;
        }

        @Override
        protected boolean shouldDeleteRootFeature(IFeature feature) {
            return this.shouldDeleteRootFeatureFlag;
        }

        public void deleteFeatureAndPromoteChild(IIdentifier featureId, IIdentifier childId) {
            Result<IFeature> maybeFeatureToDelete = getFeature(featureId);
            if (maybeFeatureToDelete.isEmpty()) {
                return;
            }

            Result<IFeature> maybeChildToPromote = getFeature(childId);
            if (maybeChildToPromote.isEmpty()) {
                return;
            }

            IFeature featureToDelete = maybeFeatureToDelete.get();
            featureToDelete.getFeatureTree().ifPresent(featureTree -> featureTree.mutate().removeFromTree());

            IFeature childToPromote = maybeChildToPromote.get();
            mutate().addFeatureTreeRoot(childToPromote);
        }
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
    }

    @Test
    public void testDeleteRootFeatureAndPromoteChildren() {
        featureModel = new TestableFeatureModel(Identifiers.newCounterIdentifier());
        TestableFeatureModel testableFeatureModel = (TestableFeatureModel) featureModel;

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

        testableFeatureModel.deleteFeatureAndPromoteChildren(rootFeature.getIdentifier());

        List<IFeature> children = Arrays.asList(childFeature1, childFeature2, childFeature3, childFeature4);
        for (IFeature child : children) {
            boolean promoted = testableFeatureModel.deleteFeatureAndPromoteChildren(child.getIdentifier());
            System.out.println("Status of original children:");
            System.out.println(child + " is promoted to root: " + promoted);
        }

        for (IFeature child : children) {
            Assertions.assertTrue(testableFeatureModel.getRoots().contains(child), "Child should be promoted to root");
        }
    }

    @Test
    public void testKeepRootFeatureAndChildrenIntact() {
        featureModel = new TestableFeatureModel(Identifiers.newCounterIdentifier());
        TestableFeatureModel testableFeatureModel = (TestableFeatureModel) featureModel;

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

        testableFeatureModel.setShouldDeleteRootFeatureFlag(false);

        testableFeatureModel.deleteFeatureAndPromoteChildren(rootFeature.getIdentifier());

        Assertions.assertTrue(testableFeatureModel.getFeatures().contains(rootFeature));

        List<IFeatureTree> actualChildren = (List<IFeatureTree>) rootFeature.getFeatureTree().map(IFeatureTree::getChildren).orElse(Collections.emptyList());
        System.out.println("Actual Children: " + actualChildren);
        System.out.println("Expected Children: " + Arrays.asList(childFeature1.getFeatureTree().get(), childFeature2.getFeatureTree().get(), childFeature3.getFeatureTree().get(), childFeature4.getFeatureTree().get()));

        Assertions.assertTrue(actualChildren.contains(childFeature1.getFeatureTree().get()), "Child feature 1 should be present");
        Assertions.assertTrue(actualChildren.contains(childFeature2.getFeatureTree().get()), "Child feature 2 should be present");
        Assertions.assertTrue(actualChildren.contains(childFeature3.getFeatureTree().get()), "Child feature 3 should be present");
        Assertions.assertTrue(actualChildren.contains(childFeature4.getFeatureTree().get()), "Child feature 4 should be present");

        Assertions.assertEquals(4, actualChildren.size(), "Number of children should be 4");

        List<IFeatureTree> expectedChildren = Arrays.asList(childFeature1.getFeatureTree().get(), childFeature2.getFeatureTree().get(), childFeature3.getFeatureTree().get(), childFeature4.getFeatureTree().get());
        Assertions.assertTrue(actualChildren.containsAll(expectedChildren), "All expected children should be present in the feature tree of the root feature");
    }

    @Test
    public void testMoveChildBelowGrandchildShouldFail() {
        // Create an instance of a class that implements IFeatureTree to use its methods
        IFeatureTree.FeatureTreeNode root = new IFeatureTree.FeatureTreeNode(new TestFeature("Root"));

        // Manually create the tree structure as the createTree method is not static
        IFeatureTree.FeatureTreeNode child1 = new IFeatureTree.FeatureTreeNode(new TestFeature("Child1"));
        IFeatureTree.FeatureTreeNode gc1 = new IFeatureTree.FeatureTreeNode(new TestFeature("GC1"));
        IFeatureTree.FeatureTreeNode gc2 = new IFeatureTree.FeatureTreeNode(new TestFeature("GC2"));
        IFeatureTree.FeatureTreeNode gc3 = new IFeatureTree.FeatureTreeNode(new TestFeature("GC3"));

        IFeatureTree.FeatureTreeNode child2 = new IFeatureTree.FeatureTreeNode(new TestFeature("Child2"));
        IFeatureTree.FeatureTreeNode gc4 = new IFeatureTree.FeatureTreeNode(new TestFeature("GC4"));
        IFeatureTree.FeatureTreeNode gc5 = new IFeatureTree.FeatureTreeNode(new TestFeature("GC5"));
        IFeatureTree.FeatureTreeNode gc6 = new IFeatureTree.FeatureTreeNode(new TestFeature("GC6"));

        // Set up the tree structure
        root.addChild(child1);
        root.addChild(child2);

        child1.addChild(gc1);
        child1.addChild(gc2);
        child1.addChild(gc3);

        child2.addChild(gc4);
        child2.addChild(gc5);
        child2.addChild(gc6);

        System.out.println("Initial tree structure:");
        root.printTree(root, "");

        assertEquals("Child1", child1.getFeature().getName().orElse(""));
        assertEquals("GC1", gc1.getFeature().getName().orElse(""));
        assertTrue(child1.getGroupFeatures().contains(gc1));
        assertTrue(root.getGroupFeatures().contains(child1));

        // Attempt to move a node below one of its own descendants
        Exception exception1 = assertThrows(IllegalArgumentException.class, () -> {
            root.moveNode(root, child1, gc1);
        });
        String expectedMessage1 = "Cannot move a node below itself or one of its own descendants.";
        String actualMessage1 = exception1.getMessage();
        assertTrue(actualMessage1.contains(expectedMessage1));

        System.out.println("\nTree structure after attempting invalid move:");
        root.printTree(root, "");

        assertTrue(child1.getGroupFeatures().contains(gc1));
        assertTrue(root.getGroupFeatures().contains(child1));

        // Attempt to move a node below itself
        Exception exception2 = assertThrows(IllegalArgumentException.class, () -> {
            root.moveNode(root, child1, child1);
        });
        String expectedMessage2 = "Cannot move a node below itself or one of its own descendants.";
        String actualMessage2 = exception2.getMessage();
        assertTrue(actualMessage2.contains(expectedMessage2));

        System.out.println("\nTree structure after attempting invalid addition:");
        root.printTree(root, "");

        assertTrue(root.getGroupFeatures().contains(child1));
        assertTrue(child1.getGroupFeatures().contains(gc1));
    }


    
    @Test
    public void testMoveChild1AboveItself() {
        // Create an instance of a class that implements IFeatureTree to use its methods
        IFeatureTree.FeatureTreeNode root = new IFeatureTree.FeatureTreeNode(new TestFeature("Root"));

        // Manually create the tree structure as the createTree method is not static
        IFeatureTree.FeatureTreeNode child1 = new IFeatureTree.FeatureTreeNode(new TestFeature("Child1"));
        IFeatureTree.FeatureTreeNode gc1 = new IFeatureTree.FeatureTreeNode(new TestFeature("GC1"));
        IFeatureTree.FeatureTreeNode gc2 = new IFeatureTree.FeatureTreeNode(new TestFeature("GC2"));
        IFeatureTree.FeatureTreeNode gc3 = new IFeatureTree.FeatureTreeNode(new TestFeature("GC3"));

        IFeatureTree.FeatureTreeNode child2 = new IFeatureTree.FeatureTreeNode(new TestFeature("Child2"));
        IFeatureTree.FeatureTreeNode gc4 = new IFeatureTree.FeatureTreeNode(new TestFeature("GC4"));
        IFeatureTree.FeatureTreeNode gc5 = new IFeatureTree.FeatureTreeNode(new TestFeature("GC5"));
        IFeatureTree.FeatureTreeNode gc6 = new IFeatureTree.FeatureTreeNode(new TestFeature("GC6"));

        // Set up the tree structure
        root.addChild(child1);
        root.addChild(child2);

        child1.addChild(gc1);
        child1.addChild(gc2);
        child1.addChild(gc3);

        child2.addChild(gc4);
        child2.addChild(gc5);
        child2.addChild(gc6);

        System.out.println("Initial tree structure:");
        root.printTree(root, "");

        assertEquals("Child1", child1.getFeature().getName().orElse(""));
        assertTrue(root.getGroupFeatures().contains(child1));

        System.out.println("\nAttempting to move Child1 above itself:");
        root.moveNode(root, child1, root);

        System.out.println("\nTree structure after attempting to move Child1 above itself:");
        root.printTree(root, "");

        assertTrue(root.getGroupFeatures().contains(child1), "Root should still have Child1 as a child.");
        assertEquals("Child1", child1.getFeature().getName().orElse(""), "The name of Child1 should remain unchanged.");
    }


    
    
    @Test
    public void testSwapGrandchildren() {
        // Create an instance of a class that implements IFeatureTree to use its methods
        IFeatureTree.FeatureTreeNode root = new IFeatureTree.FeatureTreeNode(new TestFeature("Root"));

        // Assume child1 and its grandchildren are already set up and added to root
        IFeatureTree.FeatureTreeNode child1 = new IFeatureTree.FeatureTreeNode(new TestFeature("Child1"));
        IFeatureTree.FeatureTreeNode gc1 = new IFeatureTree.FeatureTreeNode(new TestFeature("GC1"));
        IFeatureTree.FeatureTreeNode gc2 = new IFeatureTree.FeatureTreeNode(new TestFeature("GC2"));
        IFeatureTree.FeatureTreeNode gc3 = new IFeatureTree.FeatureTreeNode(new TestFeature("GC3"));

        child1.addChild(gc1);
        child1.addChild(gc2);
        child1.addChild(gc3);
        root.addChild(child1);

        // Manually create the tree structure for child2 and its grandchildren
        IFeatureTree.FeatureTreeNode child2 = new IFeatureTree.FeatureTreeNode(new TestFeature("Child2"));
        IFeatureTree.FeatureTreeNode gc4 = new IFeatureTree.FeatureTreeNode(new TestFeature("GC4"));
        IFeatureTree.FeatureTreeNode gc5 = new IFeatureTree.FeatureTreeNode(new TestFeature("GC5"));
        IFeatureTree.FeatureTreeNode gc6 = new IFeatureTree.FeatureTreeNode(new TestFeature("GC6"));
        IFeatureTree.FeatureTreeNode gc7 = new IFeatureTree.FeatureTreeNode(new TestFeature("GC7"));
        IFeatureTree.FeatureTreeNode gc8 = new IFeatureTree.FeatureTreeNode(new TestFeature("GC8"));
        IFeatureTree.FeatureTreeNode gc9 = new IFeatureTree.FeatureTreeNode(new TestFeature("GC9"));
        IFeatureTree.FeatureTreeNode gc10 = new IFeatureTree.FeatureTreeNode(new TestFeature("GC10"));

        root.addChild(child2);
        child2.addChild(gc4);
        child2.addChild(gc5);
        child2.addChild(gc6);

        gc4.addChild(gc7);
        gc4.addChild(gc8);
        gc7.addChild(gc9);
        gc7.addChild(gc10);

        System.out.println("Initial tree structure:");
        root.printTree(root, "");

        // Perform the swap operation
        root.swapGrandchildren(root, gc1, gc4);

        System.out.println("\nTree structure after swapping GC1 and GC4:");
        root.printTree(root, "");

        assertEquals("GC4", root.getGroupFeatures().get(0).getGroupFeatures().get(0).getFeature().getName().orElse(""));
        assertEquals("GC1", root.getGroupFeatures().get(1).getGroupFeatures().get(0).getFeature().getName().orElse(""));
    }




    @Test
    public void testMoveGrandchild7ToChild2() {
        // Create an instance of a class that implements IFeatureTree to use its methods
        IFeatureTree.FeatureTreeNode root = new IFeatureTree.FeatureTreeNode(new TestFeature("Root"));

        // Assume child1 and its grandchildren are already set up and added to root
        IFeatureTree.FeatureTreeNode child1 = new IFeatureTree.FeatureTreeNode(new TestFeature("Child1"));
        IFeatureTree.FeatureTreeNode gc1 = new IFeatureTree.FeatureTreeNode(new TestFeature("GC1"));
        IFeatureTree.FeatureTreeNode gc2 = new IFeatureTree.FeatureTreeNode(new TestFeature("GC2"));
        IFeatureTree.FeatureTreeNode gc3 = new IFeatureTree.FeatureTreeNode(new TestFeature("GC3"));
        child1.addChild(gc1);
        child1.addChild(gc2);
        child1.addChild(gc3);
        root.addChild(child1);

        // Manually create the tree structure for child2 and its grandchildren
        IFeatureTree.FeatureTreeNode child2 = new IFeatureTree.FeatureTreeNode(new TestFeature("Child2"));
        IFeatureTree.FeatureTreeNode gc4 = new IFeatureTree.FeatureTreeNode(new TestFeature("GC4"));
        IFeatureTree.FeatureTreeNode gc7 = new IFeatureTree.FeatureTreeNode(new TestFeature("GC7"));
        IFeatureTree.FeatureTreeNode gc8 = new IFeatureTree.FeatureTreeNode(new TestFeature("GC8"));
        IFeatureTree.FeatureTreeNode gc9 = new IFeatureTree.FeatureTreeNode(new TestFeature("GC9"));
        IFeatureTree.FeatureTreeNode gc10 = new IFeatureTree.FeatureTreeNode(new TestFeature("GC10"));

        root.addChild(child2);
        child2.addChild(gc4);
        gc4.addChild(gc7);
        gc4.addChild(gc8);
        gc7.addChild(gc9);
        gc7.addChild(gc10);

        System.out.println("Initial tree structure:");
        root.printTree(root, "");

        System.out.println("\nMoving GC7 from below GC4 to below Child2:");
        root.moveNode(root, gc7, child2);

        System.out.println("\nTree structure after moving GC7:");
        root.printTree(root, "");

        assertFalse(gc4.getGroupFeatures().contains(gc7), "GC4 should no longer have GC7 as a child.");
        assertTrue(child2.getGroupFeatures().contains(gc7), "Child2 should now have GC7 as a child.");
    }


    

    
    @Test
    public void testMoveGrandchild8ToChild2() {
        // Create an instance of a class that implements IFeatureTree to use its methods
        IFeatureTree.FeatureTreeNode root = new IFeatureTree.FeatureTreeNode(new TestFeature("Root"));

        // Assume child1 and its grandchildren are already set up and added to root
        IFeatureTree.FeatureTreeNode child1 = new IFeatureTree.FeatureTreeNode(new TestFeature("Child1"));
        IFeatureTree.FeatureTreeNode gc1 = new IFeatureTree.FeatureTreeNode(new TestFeature("GC1"));
        IFeatureTree.FeatureTreeNode gc2 = new IFeatureTree.FeatureTreeNode(new TestFeature("GC2"));
        IFeatureTree.FeatureTreeNode gc3 = new IFeatureTree.FeatureTreeNode(new TestFeature("GC3"));
        
        child1.addChild(gc1);
        child1.addChild(gc2);
        child1.addChild(gc3);
        root.addChild(child1);

        // Manually create the tree structure for child2 and its grandchildren
        IFeatureTree.FeatureTreeNode child2 = new IFeatureTree.FeatureTreeNode(new TestFeature("Child2"));
        IFeatureTree.FeatureTreeNode gc4 = new IFeatureTree.FeatureTreeNode(new TestFeature("GC4"));
        IFeatureTree.FeatureTreeNode gc5 = new IFeatureTree.FeatureTreeNode(new TestFeature("GC5"));
        IFeatureTree.FeatureTreeNode gc6 = new IFeatureTree.FeatureTreeNode(new TestFeature("GC6"));
        IFeatureTree.FeatureTreeNode gc7 = new IFeatureTree.FeatureTreeNode(new TestFeature("GC7"));
        IFeatureTree.FeatureTreeNode gc8 = new IFeatureTree.FeatureTreeNode(new TestFeature("GC8"));
        IFeatureTree.FeatureTreeNode gc9 = new IFeatureTree.FeatureTreeNode(new TestFeature("GC9"));
        IFeatureTree.FeatureTreeNode gc10 = new IFeatureTree.FeatureTreeNode(new TestFeature("GC10"));

        root.addChild(child2);
        child2.addChild(gc4);
        child2.addChild(gc5);
        child2.addChild(gc6);

        gc4.addChild(gc7);
        gc4.addChild(gc8);
        gc7.addChild(gc9);
        gc7.addChild(gc10);

        System.out.println("Initial tree structure:");
        root.printTree(root, "");

        System.out.println("\nMoving GC8 from below GC4 to below Child2:");
        root.moveNode(root, gc8, child2);

        System.out.println("\nTree structure after moving GC8:");
        root.printTree(root, "");

        assertFalse(gc4.getGroupFeatures().contains(gc8), "GC4 should no longer have GC8 as a child.");
        assertTrue(child2.getGroupFeatures().contains(gc8), "Child2 should now have GC8 as a child.");
    }



    
    @Test
    public void testMoveGrandchild7ToRoot() {
        // Create an instance of a class that implements IFeatureTree to use its methods
        IFeatureTree.FeatureTreeNode root = new IFeatureTree.FeatureTreeNode(new TestFeature("Root"));

        // Assume child1 and its grandchildren are already set up and added to root
        IFeatureTree.FeatureTreeNode child1 = new IFeatureTree.FeatureTreeNode(new TestFeature("Child1"));
        IFeatureTree.FeatureTreeNode gc1 = new IFeatureTree.FeatureTreeNode(new TestFeature("GC1"));
        IFeatureTree.FeatureTreeNode gc2 = new IFeatureTree.FeatureTreeNode(new TestFeature("GC2"));
        IFeatureTree.FeatureTreeNode gc3 = new IFeatureTree.FeatureTreeNode(new TestFeature("GC3"));
        
        child1.addChild(gc1);
        child1.addChild(gc2);
        child1.addChild(gc3);
        root.addChild(child1);

        // Manually create the tree structure for child2 and its grandchildren
        IFeatureTree.FeatureTreeNode child2 = new IFeatureTree.FeatureTreeNode(new TestFeature("Child2"));
        IFeatureTree.FeatureTreeNode gc4 = new IFeatureTree.FeatureTreeNode(new TestFeature("GC4"));
        IFeatureTree.FeatureTreeNode gc5 = new IFeatureTree.FeatureTreeNode(new TestFeature("GC5"));
        IFeatureTree.FeatureTreeNode gc6 = new IFeatureTree.FeatureTreeNode(new TestFeature("GC6"));
        IFeatureTree.FeatureTreeNode gc7 = new IFeatureTree.FeatureTreeNode(new TestFeature("GC7"));
        IFeatureTree.FeatureTreeNode gc8 = new IFeatureTree.FeatureTreeNode(new TestFeature("GC8"));
        IFeatureTree.FeatureTreeNode gc9 = new IFeatureTree.FeatureTreeNode(new TestFeature("GC9"));
        IFeatureTree.FeatureTreeNode gc10 = new IFeatureTree.FeatureTreeNode(new TestFeature("GC10"));

        root.addChild(child2);
        child2.addChild(gc4);
        child2.addChild(gc5);
        child2.addChild(gc6);

        gc4.addChild(gc7);
        gc4.addChild(gc8);
        gc7.addChild(gc9);
        gc7.addChild(gc10);

        System.out.println("Initial tree structure:");
        root.printTree(root, "");

        System.out.println("\nMoving GC7 from below GC4 to below Root:");
        root.moveNode(root, gc7, root);

        System.out.println("\nTree structure after moving GC7:");
        root.printTree(root, "");

        assertFalse(gc4.getGroupFeatures().contains(gc7), "GC4 should no longer have GC7 as a child.");
        assertTrue(root.getGroupFeatures().contains(gc7), "Root should now have GC7 as a child.");

        System.out.println("Name of moved node: " + gc7.getFeature().getName().orElse("Unknown"));
        assertEquals("GC7", gc7.getFeature().getName().orElse("Unknown"), "The name of GC7 should remain unchanged.");
    }



    
    @Test
    public void testMoveGrandchild8ToRoot() {
        // Create an instance of a class that implements IFeatureTree to use its methods
        IFeatureTree.FeatureTreeNode root = new IFeatureTree.FeatureTreeNode(new TestFeature("Root"));

        // Assume child1 and its grandchildren are already set up and added to root
        IFeatureTree.FeatureTreeNode child1 = new IFeatureTree.FeatureTreeNode(new TestFeature("Child1"));
        IFeatureTree.FeatureTreeNode gc1 = new IFeatureTree.FeatureTreeNode(new TestFeature("GC1"));
        IFeatureTree.FeatureTreeNode gc2 = new IFeatureTree.FeatureTreeNode(new TestFeature("GC2"));
        IFeatureTree.FeatureTreeNode gc3 = new IFeatureTree.FeatureTreeNode(new TestFeature("GC3"));
        
        child1.addChild(gc1);
        child1.addChild(gc2);
        child1.addChild(gc3);
        root.addChild(child1);

        // Manually create the tree structure for child2 and its grandchildren
        IFeatureTree.FeatureTreeNode child2 = new IFeatureTree.FeatureTreeNode(new TestFeature("Child2"));
        IFeatureTree.FeatureTreeNode gc4 = new IFeatureTree.FeatureTreeNode(new TestFeature("GC4"));
        IFeatureTree.FeatureTreeNode gc5 = new IFeatureTree.FeatureTreeNode(new TestFeature("GC5"));
        IFeatureTree.FeatureTreeNode gc6 = new IFeatureTree.FeatureTreeNode(new TestFeature("GC6"));
        IFeatureTree.FeatureTreeNode gc7 = new IFeatureTree.FeatureTreeNode(new TestFeature("GC7"));
        IFeatureTree.FeatureTreeNode gc8 = new IFeatureTree.FeatureTreeNode(new TestFeature("GC8"));
        IFeatureTree.FeatureTreeNode gc9 = new IFeatureTree.FeatureTreeNode(new TestFeature("GC9"));
        IFeatureTree.FeatureTreeNode gc10 = new IFeatureTree.FeatureTreeNode(new TestFeature("GC10"));

        root.addChild(child2);
        child2.addChild(gc4);
        child2.addChild(gc5);
        child2.addChild(gc6);

        gc4.addChild(gc7);
        gc4.addChild(gc8);
        gc7.addChild(gc9);
        gc7.addChild(gc10);

        System.out.println("Initial tree structure:");
        root.printTree(root, "");

        System.out.println("\nMoving GC8 from below GC4 to below Root:");
        root.moveNode(root, gc8, root);

        System.out.println("\nTree structure after moving GC8:");
        root.printTree(root, "");

        assertFalse(gc4.getGroupFeatures().contains(gc8), "GC4 should no longer have GC8 as a child.");
        assertTrue(root.getGroupFeatures().contains(gc8), "Root should now have GC8 as a child.");

        System.out.println("Name of moved node: " + gc8.getFeature().getName().orElse("Unknown"));
        assertEquals("GC8", gc8.getFeature().getName().orElse("Unknown"), "The name of GC8 should remain unchanged.");
    }



    
    @Test
    public void testMoveGrandchild5ToRoot() {
        // Create an instance of a class that implements IFeatureTree to use its methods
        IFeatureTree.FeatureTreeNode root = new IFeatureTree.FeatureTreeNode(new TestFeature("Root"));

        // Assume child1 and its grandchildren are already set up and added to root
        IFeatureTree.FeatureTreeNode child1 = new IFeatureTree.FeatureTreeNode(new TestFeature("Child1"));
        IFeatureTree.FeatureTreeNode gc1 = new IFeatureTree.FeatureTreeNode(new TestFeature("GC1"));
        IFeatureTree.FeatureTreeNode gc2 = new IFeatureTree.FeatureTreeNode(new TestFeature("GC2"));
        IFeatureTree.FeatureTreeNode gc3 = new IFeatureTree.FeatureTreeNode(new TestFeature("GC3"));

        child1.addChild(gc1);
        child1.addChild(gc2);
        child1.addChild(gc3);
        root.addChild(child1);

        // Manually create the tree structure for child2 and its grandchildren
        IFeatureTree.FeatureTreeNode child2 = new IFeatureTree.FeatureTreeNode(new TestFeature("Child2"));
        IFeatureTree.FeatureTreeNode gc4 = new IFeatureTree.FeatureTreeNode(new TestFeature("GC4"));
        IFeatureTree.FeatureTreeNode gc5 = new IFeatureTree.FeatureTreeNode(new TestFeature("GC5"));
        IFeatureTree.FeatureTreeNode gc6 = new IFeatureTree.FeatureTreeNode(new TestFeature("GC6"));
        IFeatureTree.FeatureTreeNode gc7 = new IFeatureTree.FeatureTreeNode(new TestFeature("GC7"));
        IFeatureTree.FeatureTreeNode gc8 = new IFeatureTree.FeatureTreeNode(new TestFeature("GC8"));
        IFeatureTree.FeatureTreeNode gc9 = new IFeatureTree.FeatureTreeNode(new TestFeature("GC9"));
        IFeatureTree.FeatureTreeNode gc10 = new IFeatureTree.FeatureTreeNode(new TestFeature("GC10"));

        root.addChild(child2);
        child2.addChild(gc4);
        child2.addChild(gc5);
        child2.addChild(gc6);

        gc4.addChild(gc7);
        gc4.addChild(gc8);
        gc7.addChild(gc9);
        gc7.addChild(gc10);

        System.out.println("Initial tree structure:");
        root.printTree(root, "");

        System.out.println("\nMoving GC5 from below Child2 to below Root:");
        root.moveNode(root, gc5, root);

        System.out.println("\nTree structure after moving GC5:");
        root.printTree(root, "");

        assertFalse(child2.getGroupFeatures().contains(gc5), "Child2 should no longer have GC5 as a child.");
        assertTrue(root.getGroupFeatures().contains(gc5), "Root should now have GC5 as a child.");
        assertEquals("GC5", root.getGroupFeatures().get(root.getGroupFeatures().size() - 1).getFeature().getName().orElse("Unknown"), "GC5 should be the last child of the root.");

        assertTrue(gc4.getGroupFeatures().contains(gc7), "GC4 should still have GC7 as a child.");
        assertTrue(gc4.getGroupFeatures().contains(gc8), "GC4 should still have GC8 as a child.");
        assertTrue(gc7.getGroupFeatures().contains(gc9), "GC7 should still have GC9 as a child.");
        assertTrue(gc7.getGroupFeatures().contains(gc10), "GC7 should still have GC10 as a child.");
    }



    

}
