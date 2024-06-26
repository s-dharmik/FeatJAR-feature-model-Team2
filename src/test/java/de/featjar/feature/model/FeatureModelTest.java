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

import de.featjar.base.data.Result;
import de.featjar.base.data.identifier.IIdentifier;
import de.featjar.base.data.identifier.Identifiers;
import de.featjar.base.data.identifier.UUIDIdentifier;
import de.featjar.base.tree.Trees;
import de.featjar.base.tree.visitor.TreePrinter;
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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

    
    
    private IFeatureTree root;
    private IFeatureTree childNode1;
    private IFeatureTree childNode2;
    private IFeatureTree gcNode1;
    private IFeatureTree gcNode2;
    private IFeatureTree gcNode3;
    private IFeatureTree gcNode4;
    private IFeatureTree gcNode5;
    private IFeatureTree gcNode6;
    private IFeatureTree gcNode7;
    private IFeatureTree gcNode8;
    private IFeatureTree gcNode9;
    private IFeatureTree gcNode10;

    @BeforeEach
    public void setUp() {
        FeatureModel featureModel = new FeatureModel();

        // Initialize the root feature and its tree node
        IFeature rootFeature = featureModel.mutate().addFeature("Root");
        root = featureModel.mutate().addFeatureTreeRoot(rootFeature);

        // Add child1 and its grandchildren (gc1, gc2, gc3)
        IFeature child1 = featureModel.mutate().addFeature("Child1");
        childNode1 = root.mutate().addFeatureBelow(child1);

        IFeature gc1 = featureModel.mutate().addFeature("GC1");
        gcNode1 = childNode1.mutate().addFeatureBelow(gc1);

        IFeature gc2 = featureModel.mutate().addFeature("GC2");
        gcNode2 = childNode1.mutate().addFeatureBelow(gc2);

        IFeature gc3 = featureModel.mutate().addFeature("GC3");
        gcNode3 = childNode1.mutate().addFeatureBelow(gc3);

        // Add child2 and its grandchildren (gc4, gc5, gc6, gc7, gc8, gc9, gc10)
        IFeature child2 = featureModel.mutate().addFeature("Child2");
        childNode2 = root.mutate().addFeatureBelow(child2);

        IFeature gc4 = featureModel.mutate().addFeature("GC4");
        gcNode4 = childNode2.mutate().addFeatureBelow(gc4);

        IFeature gc5 = featureModel.mutate().addFeature("GC5");
        gcNode5 = childNode2.mutate().addFeatureBelow(gc5);

        IFeature gc6 = featureModel.mutate().addFeature("GC6");
        gcNode6 = childNode2.mutate().addFeatureBelow(gc6);

        IFeature gc7 = featureModel.mutate().addFeature("GC7");
        gcNode7 = gcNode4.mutate().addFeatureBelow(gc7);

        IFeature gc8 = featureModel.mutate().addFeature("GC8");
        gcNode8 = gcNode4.mutate().addFeatureBelow(gc8);

        IFeature gc9 = featureModel.mutate().addFeature("GC9");
        gcNode9 = gcNode7.mutate().addFeatureBelow(gc9);

        IFeature gc10 = featureModel.mutate().addFeature("GC10");
        gcNode10 = gcNode7.mutate().addFeatureBelow(gc10);

        // Print initial setup to confirm
        System.out.println("Tree structure after setup:");
        printTree(root, "");
    }

    @AfterEach
    public void tearDown() {
        System.out.println("Tree structure after test:");
        printTree(root, "");
    }

    private void printTree(IFeatureTree node, String indent) {
    	System.out.println(Trees.traverse(node, new  TreePrinter()));
    }

    @Test
    public void testMoveChildBelowGrandchildShouldFail() {
<<<<<<< HEAD
=======
    	IFeature rootFeature = featureModel.mutate().addFeature("root");
    	// Create an instance of a class that implements IFeatureTree to use its methods
        IFeatureTree root = featureModel.mutate().addFeatureTreeRoot(rootFeature);

        // Manually create the tree structure as the createTree method is not static
        IFeature child1 = featureModel.mutate().addFeature("Child1");
        IFeatureTree childeNode1 = root.mutate().addFeatureBelow(child1);
        
        // TODO fix this!
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

>>>>>>> 588014115fc8bc4d4d369b84f621b4cea3acfcf2
        System.out.println("Initial tree structure:");
        printTree(root, "");

        assertEquals("Child1", childNode1.getFeature().getName().orElse(""));
        assertEquals("GC1", gcNode1.getFeature().getName().orElse(""));
        assertTrue(childNode1.getGroupFeatures().contains(gcNode1));
        assertTrue(root.getGroupFeatures().contains(childNode1));

        // Attempt to move a node below one of its own descendants
        Exception exception1 = assertThrows(IllegalArgumentException.class, () -> {
            childNode1.mutate().moveNode(gcNode1);
        });
        String expectedMessage1 = "Cannot move a node below itself or one of its own descendants.";
        String actualMessage1 = exception1.getMessage();
        assertTrue(actualMessage1.contains(expectedMessage1));

        System.out.println("\nTree structure after attempting invalid move:");
        printTree(root, "");

        assertTrue(childNode1.getGroupFeatures().contains(gcNode1));
        assertTrue(root.getGroupFeatures().contains(childNode1));

        // Attempt to move a node below itself
        Exception exception2 = assertThrows(IllegalArgumentException.class, () -> {
            childNode1.mutate().moveNode(childNode1);
        });
        String expectedMessage2 = "Cannot move a node below itself or one of its own descendants.";
        String actualMessage2 = exception2.getMessage();
        assertTrue(actualMessage2.contains(expectedMessage2));

        System.out.println("\nTree structure after attempting invalid addition:");
        printTree(root, "");

        assertTrue(root.getGroupFeatures().contains(childNode1));
        assertTrue(childNode1.getGroupFeatures().contains(gcNode1));
    }

    @Test
    public void testMoveChild1AboveItself() {
        System.out.println("Initial tree structure:");
        printTree(root, "");

        assertEquals("Child1", childNode1.getFeature().getName().orElse(""));
        assertTrue(root.getGroupFeatures().contains(childNode1));

        System.out.println("\nAttempting to move Child1 above itself:");
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            childNode1.mutate().addFeatureAbove(childNode1.getFeature());
        });
        String expectedMessage = "Cannot move a node above itself.";
        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains(expectedMessage));

        System.out.println("\nTree structure after attempting to move Child1 above itself:");
        printTree(root, "");

        assertTrue(root.getGroupFeatures().contains(childNode1), "Root should still have Child1 as a child.");
        assertEquals("Child1", childNode1.getFeature().getName().orElse(""), "The name of Child1 should remain unchanged.");
    }

    @Test
    public void testSwapGrandchildren() {
        System.out.println("Initial tree structure:");
        printTree(root, "");

        // Perform the swap operation using the swap method from the IFeatureTree interface
        gcNode1.mutate().swap(gcNode4);

        System.out.println("\nTree structure after swapping GC1 and GC4:");
        printTree(root, "");

        // Verify the swap
        assertEquals("GC4", childNode1.getChildren().get(0).getFeature().getName().orElse(""));
        assertEquals("GC1", childNode2.getChildren().get(0).getFeature().getName().orElse(""));
    }

    
    @Test
    public void testMoveGrandchild7ToChild2() {
        System.out.println("Initial tree structure:");
        printTree(root, "");

        System.out.println("\nMoving GC7 from below GC4 to below Child2:");
        gcNode7.mutate().moveNode(childNode2);

        System.out.println("\nTree structure after moving GC7:");
        printTree(root, "");

        assertFalse(gcNode4.getGroupFeatures().contains(gcNode7), "GC4 should no longer have GC7 as a child.");
        assertTrue(childNode2.getGroupFeatures().contains(gcNode7), "Child2 should now have GC7 as a child.");
    }

    @Test
    public void testMoveGrandchild8ToChild2() {
        System.out.println("Initial tree structure:");
        printTree(root, "");

        System.out.println("\nMoving GC8 from below GC4 to below Child2:");
        gcNode8.mutate().moveNode(childNode2);

        System.out.println("\nTree structure after moving GC8:");
        printTree(root, "");

        assertFalse(gcNode4.getGroupFeatures().contains(gcNode8), "GC4 should no longer have GC8 as a child.");
        assertTrue(childNode2.getGroupFeatures().contains(gcNode8), "Child2 should now have GC8 as a child.");
    }

    
    @Test
    public void testMoveGrandchild7ToRoot() {
        System.out.println("Initial tree structure:");
        printTree(root, "");

        System.out.println("\nMoving GC7 from below GC4 to below Root:");
        gcNode7.mutate().moveNode(root);

        System.out.println("\nTree structure after moving GC7:");
        printTree(root, "");

        assertFalse(gcNode4.getGroupFeatures().contains(gcNode7), "GC4 should no longer have GC7 as a child.");
        assertTrue(root.getGroupFeatures().contains(gcNode7), "Root should now have GC7 as a child.");

        System.out.println("Name of moved node: " + gcNode7.getFeature().getName().orElse("Unknown"));
        assertEquals("GC7", gcNode7.getFeature().getName().orElse("Unknown"), "The name of GC7 should remain unchanged.");
    }

    @Test
    public void testMoveGrandchild8ToRoot() {
        System.out.println("Initial tree structure:");
        printTree(root, "");

        System.out.println("\nMoving GC8 from below GC4 to below Root:");
        gcNode8.mutate().moveNode(root);

        System.out.println("\nTree structure after moving GC8:");
        printTree(root, "");

        assertFalse(gcNode4.getGroupFeatures().contains(gcNode8), "GC4 should no longer have GC8 as a child.");
        assertTrue(root.getGroupFeatures().contains(gcNode8), "Root should now have GC8 as a child.");

        System.out.println("Name of moved node: " + gcNode8.getFeature().getName().orElse("Unknown"));
        assertEquals("GC8", gcNode8.getFeature().getName().orElse("Unknown"), "The name of GC8 should remain unchanged.");
    }

    @Test
    public void testMoveGrandchild5ToRoot() {
        System.out.println("Initial tree structure:");
        printTree(root, "");

        System.out.println("\nMoving GC5 from below Child2 to below Root:");
        gcNode5.mutate().moveNode(root);

        System.out.println("\nTree structure after moving GC5:");
        printTree(root, "");

        assertFalse(childNode2.getGroupFeatures().contains(gcNode5), "Child2 should no longer have GC5 as a child.");
        assertTrue(root.getGroupFeatures().contains(gcNode5), "Root should now have GC5 as a child.");
        assertEquals("GC5", gcNode5.getFeature().getName().orElse("Unknown"), "The name of GC5 should remain unchanged.");

        assertTrue(gcNode4.getGroupFeatures().contains(gcNode7), "GC4 should still have GC7 as a child.");
        assertTrue(gcNode4.getGroupFeatures().contains(gcNode8), "GC4 should still have GC8 as a child.");
        assertTrue(gcNode7.getGroupFeatures().contains(gcNode9), "GC7 should still have GC9 as a child.");
        assertTrue(gcNode7.getGroupFeatures().contains(gcNode10), "GC7 should still have GC10 as a child.");
    }

    private void printTreeForTestMoveGrandchild5ToRoot(IFeatureTree node, String indent) {
        System.out.println(indent + node.getFeature().getName().orElse(""));
        for (IFeatureTree child : node.getChildren()) {
            printTreeForTestMoveGrandchild5ToRoot(child, indent + "  ");
        }
    }


    
}


    


