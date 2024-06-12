package de.featjar.feature.model;

import java.util.ArrayList;
import java.util.List;

public class MoveFeature {

    // Inner class representing a tree node
    public static class TreeNode {
        String name;
        List<TreeNode> children;

        TreeNode(String name) {
            this.name = name;
            this.children = new ArrayList<>();
        }

        void addChild(TreeNode child) {
            children.add(child);
        }

        boolean removeChild(TreeNode child) {
            return children.remove(child);
        }

        @Override
        public String toString() {
            return name;
        }
    }

    // Method to create and return the tree structure
    public TreeNode createTree() {
        TreeNode root = new TreeNode("Root");

        TreeNode child1 = new TreeNode("Child1");
        TreeNode child2 = new TreeNode("Child2");

        root.addChild(child1);
        root.addChild(child2);

        child1.addChild(new TreeNode("GC1"));
        child1.addChild(new TreeNode("GC2"));
        child1.addChild(new TreeNode("GC3"));

        child2.addChild(new TreeNode("GC4"));
        child2.addChild(new TreeNode("GC5"));
        child2.addChild(new TreeNode("GC6"));

        return root;
    }

    // Method to check if the target node is a descendant of the source node
    private boolean isDescendant(TreeNode source, TreeNode target) {
        if (source.children.contains(target)) {
            return true;
        }
        for (TreeNode child : source.children) {
            if (isDescendant(child, target)) {
                return true;
            }
        }
        return false;
    }

    // Method to move a child node to a new parent
    public void moveNode(TreeNode root, TreeNode source, TreeNode target) throws IllegalArgumentException {
        if (source == target || isDescendant(source, target)) {
            throw new IllegalArgumentException("Cannot move a node below itself or one of its own descendants.");
        }

        // Remove the source node from its current parent
        TreeNode parent = findParent(root, source);
        if (parent != null) {
            parent.removeChild(source);
        }

        // Add the source node to the new parent
        target.addChild(source);
    }

    // Method to swap two grandchildren
    public void swapGrandchildren(TreeNode root, TreeNode gc1, TreeNode gc2) throws IllegalArgumentException {
        TreeNode parent1 = findParent(root, gc1);
        TreeNode parent2 = findParent(root, gc2);

        if (parent1 == null || parent2 == null) {
            throw new IllegalArgumentException("One or both of the nodes are not found.");
        }

        // Swap the grandchildren
        int index1 = parent1.children.indexOf(gc1);
        int index2 = parent2.children.indexOf(gc2);
        parent1.children.set(index1, gc2);
        parent2.children.set(index2, gc1);
    }

    // Helper method to find the parent of a given node
    private TreeNode findParent(TreeNode root, TreeNode node) {
        if (root.children.contains(node)) {
            return root;
        }
        for (TreeNode child : root.children) {
            TreeNode parent = findParent(child, node);
            if (parent != null) {
                return parent;
            }
        }
        return null;
    }

    // Helper method to print the tree structure
    public void printTree(TreeNode node, String prefix) {
        System.out.println(prefix + node);
        for (TreeNode child : node.children) {
            printTree(child, prefix + "    ");
        }
    }

    public static void main(String[] args) {
        MoveFeature moveFeature = new MoveFeature();
        TreeNode root = moveFeature.createTree();

        // Print initial tree structure
        System.out.println("Initial tree structure:");
        moveFeature.printTree(root, "");

        // Attempt to move Child1 below GC1 (which should cause an error)
        try {
            TreeNode child1 = root.children.get(0); // Child1
            TreeNode gc1 = child1.children.get(0); // GC1
            moveFeature.moveNode(root, child1, gc1);
        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        }

        // Print tree structure after attempting the move
        System.out.println("\nTree structure after attempting invalid move:");
        moveFeature.printTree(root, "");

        // Swap grandchildren GC1 and GC4
        try {
            TreeNode gc1 = root.children.get(0).children.get(0); // GC1
            TreeNode gc4 = root.children.get(1).children.get(0); // GC4
            moveFeature.swapGrandchildren(root, gc1, gc4);
        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        }

        // Print tree structure after swapping
        System.out.println("\nTree structure after swapping GC1 and GC4:");
        moveFeature.printTree(root, "");
    }
}
