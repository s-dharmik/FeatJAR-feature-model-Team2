package de.featjar.feature.model;

import de.featjar.base.data.Attribute;
import de.featjar.base.data.IAttributable;
import de.featjar.base.data.IAttribute;
import de.featjar.base.data.Range;
import de.featjar.base.data.Result;
import de.featjar.base.tree.structure.ARootedTree;
import de.featjar.base.tree.structure.IRootedTree;
import de.featjar.base.tree.structure.ITree;
import de.featjar.feature.model.FeatureTree.Group;
import de.featjar.feature.model.mixins.IHasFeatureTree;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * An ordered {@link ARootedTree} labeled with {@link Feature features}.
 * Implements some concepts from feature-oriented domain analysis, such as mandatory/optional features and groups.
 *
 * Merged version integrating MoveFeature functionality.
 * 
 * @version 2024
 */
public interface IFeatureTree extends IRootedTree<IFeatureTree>, IAttributable, IHasFeatureTree {

    IFeature getFeature();

    List<Group> getGroups();

    Group getGroup();

    List<IFeatureTree> getGroupFeatures();

    int getFeatureRangeLowerBound();

    int getFeatureRangeUpperBound();

    default boolean isMandatory() {
        return getFeatureRangeLowerBound() > 0;
    }

    default boolean isOptional() {
        return getFeatureRangeLowerBound() == 0;
    }

    default IMutableFeatureTree mutate() {
        return (IMutableFeatureTree) this;
    }

    static interface IMutableFeatureTree extends IFeatureTree, IMutatableAttributable {

        default IFeatureTree addFeatureBelow(IFeature newFeature) {
            return addFeatureBelow(newFeature, getChildrenCount(), 0);
        }

        default IFeatureTree addFeatureBelow(IFeature newFeature, int index) {
            return addFeatureBelow(newFeature, index, 0);
        }

        default IFeatureTree addFeatureBelow(IFeature newFeature, int index, int groupID) {
            FeatureTree newTree = new FeatureTree(newFeature);
            addChild(index, newTree);
            newTree.setGroupID(groupID);
            return newTree;
        }

        default IFeatureTree addFeatureAbove(IFeature newFeature) {
            FeatureTree newTree = new FeatureTree(newFeature);
            Result<IFeatureTree> parent = getParent();
            if (parent.isPresent()) {
                parent.get().replaceChild(this, newTree);
                newTree.setGroupID(this.getGroupID());
            }
            newTree.addChild(this);
            this.setGroupID(0);
            return newTree;
        }

        default void removeFromTree() { // TODO what about the containing constraints?
            Result<IFeatureTree> parent = getParent();
            if (parent.isPresent()) {
                int childIndex = parent.get().getChildIndex(this).orElseThrow();
                parent.get().removeChild(this);
                int groupID = parent.get().getGroups().size();
                // TODO improve group handling, probably needs slicing
                for (Group group : getGroups()) {
                    parent.get().mutate().addGroup(group.getLowerBound(), group.getUpperBound());
                }
                for (IFeatureTree child : getChildren()) {
                    parent.get().mutate().addChild(childIndex++, child);
                    child.mutate().setGroupID(groupID + child.getGroupID());
                }
            }
        }

        void setFeatureRange(Range featureRange);

        void addGroup(int lowerBound, int upperBound);

        void addGroup(Range groupRange);

        void setGroups(List<Group> groups);

        void setGroupID(int groupID);

        void setMandatory();

        void setOptional();

        void setGroupRange(Range groupRange);

        default void setAnd() {
            setGroupRange(Range.open());
        }

        default void setAlternative() {
            setGroupRange(Range.exactly(1));
        }

        default void setOr() {
            setGroupRange(Range.atLeast(1));
        }
    }

    int getGroupID();

    boolean isRoot(); //sarthak

    // Merged code from MoveFeature starts here
    public static class FeatureTreeNode implements IFeatureTree {
        private IFeature feature;
        protected List<FeatureTreeNode> children;
        protected int groupID;
        protected Range featureRange;

        public FeatureTreeNode(IFeature feature) {
            this.feature = feature;
            this.children = new ArrayList<>();
            this.groupID = 0;
            this.featureRange = Range.closed(0, 1); // Default range
        }

        @Override
        public IFeature getFeature() {
            return feature;
        }

        @Override
        public List<Group> getGroups() {
            return new ArrayList<>(); // Simplification: no groups
        }

        @Override
        public Group getGroup() {
            return null; // Simplification: no single group
        }

        @Override
        public List<IFeatureTree> getGroupFeatures() {
            return new ArrayList<>(children);
        }

        @Override
        public int getFeatureRangeLowerBound() {
            return featureRange.lowerEndpoint();
        }

        @Override
        public int getFeatureRangeUpperBound() {
            return featureRange.upperEndpoint();
        }

        @Override
        public int getGroupID() {
            return groupID;
        }

        @Override
        public boolean isRoot() {
            return this.feature.getName().orElse("Unknown").equals("Root");
        }

        public void addChild(FeatureTreeNode child) {
            children.add(child);
        }

        public boolean removeChild(FeatureTreeNode child) {
            return children.remove(child);
        }

        public List<FeatureTreeNode> getChildren() {
            return children;
        }

        @Override
        public IMutableFeatureTree mutate() {
            return new MutableFeatureTreeNode(this);
        }

        @Override
        public String toString() {
            return feature.getName().orElse("Unknown");
        }

        @Override
        public Result<IFeatureTree> getParent() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void setParent(IFeatureTree newParent) {
            // TODO Auto-generated method stub
        }

        @Override
        public void setChildren(List<? extends IFeatureTree> children) {
            // TODO Auto-generated method stub
        }

        @Override
        public ITree<IFeatureTree> cloneNode() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public boolean equalsNode(IFeatureTree other) {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public int hashCodeNode() {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public Optional<Map<IAttribute<?>, Object>> getAttributes() {
            // TODO Auto-generated method stub
            return Optional.empty();
        }

        @Override
        public List<IFeatureTree> getRoots() {
            // TODO Auto-generated method stub
            return null;
        }

        // MutableFeatureTreeNode class to handle mutations
        public static class MutableFeatureTreeNode extends FeatureTreeNode implements IMutableFeatureTree {

            public MutableFeatureTreeNode(FeatureTreeNode node) {
                super(node.getFeature());
                this.children = node.getChildren();
                this.groupID = node.getGroupID();
                this.featureRange = Range.closed(node.getFeatureRangeLowerBound(), node.getFeatureRangeUpperBound());
            }

            @Override
            public void setFeatureRange(Range featureRange) {
                this.featureRange = featureRange;
            }

            @Override
            public void addGroup(int lowerBound, int upperBound) {
                // Simplification: no groups
            }

            @Override
            public void addGroup(Range groupRange) {
                // Simplification: no groups
            }

            @Override
            public void setGroups(List<Group> groups) {
                // Simplification: no groups
            }

            @Override
            public void setGroupID(int groupID) {
                this.groupID = groupID;
            }

            @Override
            public void setMandatory() {
                this.featureRange = Range.atLeast(1);
            }

            @Override
            public void setOptional() {
                this.featureRange = Range.atMost(1);
            }

            @Override
            public void setGroupRange(Range groupRange) {
                this.featureRange = groupRange;
            }

            @Override
            public Result<IFeatureTree> getParent() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public void setParent(IFeatureTree newParent) {
                // TODO Auto-generated method stub
            }

            @Override
            public void setChildren(List<? extends IFeatureTree> children) {
                // TODO Auto-generated method stub
            }

            @Override
            public ITree<IFeatureTree> cloneNode() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public boolean equalsNode(IFeatureTree other) {
                // TODO Auto-generated method stub
                return false;
            }

            @Override
            public int hashCodeNode() {
                // TODO Auto-generated method stub
                return 0;
            }

            @Override
            public Optional<Map<IAttribute<?>, Object>> getAttributes() {
                // TODO Auto-generated method stub
                return Optional.empty();
            }

            @Override
            public List<IFeatureTree> getRoots() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public <S> void setAttributeValue(Attribute<S> attribute, S value) {
                // TODO Auto-generated method stub
            }

            @Override
            public <S> S removeAttributeValue(Attribute<S> attribute) {
                // TODO Auto-generated method stub
                return null;
            }
        }
    }

    // Additional methods to manage tree structure

    default FeatureTreeNode createTree() {
        IFeature rootFeature = new TestFeature("Root");
        FeatureTreeNode root = new FeatureTreeNode(rootFeature);

        IFeature child1Feature = new TestFeature("Child1");
        IFeature child2Feature = new TestFeature("Child2");

        FeatureTreeNode child1 = new FeatureTreeNode(child1Feature);
        FeatureTreeNode child2 = new FeatureTreeNode(child2Feature);

        root.addChild(child1);
        root.addChild(child2);

        child1.addChild(new FeatureTreeNode(new TestFeature("GC1")));
        child1.addChild(new FeatureTreeNode(new TestFeature("GC2")));
        child1.addChild(new FeatureTreeNode(new TestFeature("GC3")));

        child2.addChild(new FeatureTreeNode(new TestFeature("GC4")));
        child2.addChild(new FeatureTreeNode(new TestFeature("GC5")));
        child2.addChild(new FeatureTreeNode(new TestFeature("GC6")));

        return root;
    }

    default boolean isDescendant(FeatureTreeNode source, FeatureTreeNode target) {
        if (source == target) {
            return true;
        }
        for (FeatureTreeNode child : source.children) {
            if (isDescendant(child, target)) {
                return true;
            }
        }
        return false;
    }

    default void moveNode(FeatureTreeNode root, FeatureTreeNode source, FeatureTreeNode target) throws IllegalArgumentException {
        if (source == target || isDescendant(source, target)) {
            throw new IllegalArgumentException("Cannot move a node below itself or one of its own descendants.");
        }

        FeatureTreeNode parent = findParent(root, source);
        if (parent != null) {
            parent.removeChild(source);
        }

        target.addChild(source);
    }

    default void swapGrandchildren(FeatureTreeNode root, FeatureTreeNode gc1, FeatureTreeNode gc2) throws IllegalArgumentException {
        FeatureTreeNode parent1 = findParent(root, gc1);
        FeatureTreeNode parent2 = findParent(root, gc2);

        if (parent1 == null || parent2 == null) {
            throw new IllegalArgumentException("One or both of the nodes are not found.");
        }

        int index1 = parent1.children.indexOf(gc1);
        int index2 = parent2.children.indexOf(gc2);
        parent1.children.set(index1, gc2);
        parent2.children.set(index2, gc1);
    }

    default FeatureTreeNode findParent(FeatureTreeNode root, FeatureTreeNode node) {
        if (root.children.contains(node)) {
            return root;
        }
        for (FeatureTreeNode child : root.children) {
            FeatureTreeNode parent = findParent(child, node);
            if (parent != null) {
                return parent;
            }
        }
        return null;
    }

    default void printTree(FeatureTreeNode node, String prefix) {
        System.out.println(prefix + node);
        for (FeatureTreeNode child : node.children) {
            printTree(child, prefix + "    ");
        }
    }
    // Merged code from MoveFeature ends here
}
