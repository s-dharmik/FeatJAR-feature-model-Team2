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
import java.util.Optional;



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

    interface IMutableFeatureTree extends IFeatureTree, IMutatableAttributable {
        default IFeatureTree addFeatureBelow(IFeature newFeature) {
            return addFeatureBelow(newFeature, getChildren().size(), 0);
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

        default void removeFromTree() {
            Result<IFeatureTree> parent = getParent();
            if (parent.isPresent()) {
                int childIndex = parent.get().getChildIndex(this).orElseThrow();
                parent.get().removeChild(this);
                int groupID = parent.get().getGroups().size();
                for (Group group : getGroups()) {
                    parent.get().mutate().addGroup(group.getLowerBound(), group.getUpperBound());
                }
                for (IFeatureTree child : getChildren()) {
                    parent.get().mutate().addChild(childIndex++, child);
                    child.mutate().setGroupID(groupID + child.getGroupID());
                }
            }
        }
        
        default void moveNode(IFeatureTree newParent) throws IllegalArgumentException {
            if (this == newParent || isDescendant(newParent, this)) {
                throw new IllegalArgumentException("Cannot move a node below itself or one of its own descendants.");
            }

            Result<IFeatureTree> parent = getParent();
            if (parent.isPresent()) {
                parent.get().removeChild(this);
                this.setParent(newParent);
                newParent.addChild(this);
            } else {
            	// TODO handle root
            }
        }
        
        default boolean isDescendant(IFeatureTree source, IFeatureTree target) {
            if (source == target) {
                return true;
            }
            for (IFeatureTree child : target.getChildren()) {
                if (isDescendant(child, source)) {
                    return true;
                }
            }
            return false;
        }

        // TODO adapt this to IFeatureTree
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

        Result<IFeatureTree> getParent();

        List<IFeatureTree> getChildren();

        void addChild(IFeatureTree child);

        void removeChild(IFeatureTree child);

        void setParent(IFeatureTree parent);

        
        
        
        
        default void moveNode(IFeatureTree newParent) {
            if (newParent == null || this == newParent || isDescendant(newParent, this)) {
                throw new IllegalArgumentException("Cannot move a node below itself or one of its own descendants.");
            }

            IFeatureTree currentParent = getParent().orElse(null);
            if (currentParent != null) {
                currentParent.mutate().removeChild(this);
            }
            newParent.mutate().addChild(this);
            this.setParent(newParent);
        }
        
        
        
        

        default void swap(IFeatureTree other) {
            if (other == null) {
                throw new IllegalArgumentException("Cannot swap with null.");
            }

            Result<IFeatureTree> parent1Result = this.getParent();
            Result<IFeatureTree> parent2Result = other.getParent();

            if (!parent1Result.isPresent() || !parent2Result.isPresent()) {
                throw new IllegalArgumentException("One or both of the nodes are not found.");
            }

            IFeatureTree parent1 = parent1Result.get();
            IFeatureTree parent2 = parent2Result.get();

            List<IFeatureTree> children1 = new ArrayList<>(parent1.getChildren());
            List<IFeatureTree> children2 = new ArrayList<>(parent2.getChildren());
            int index1 = children1.indexOf(this);
            int index2 = children2.indexOf(other);

            if (index1 == -1 || index2 == -1) {
                throw new IllegalArgumentException("One or both of the nodes are not children of their respective parents.");
            }

            parent1.removeChild(this);
            parent2.removeChild(other);

            parent1.addChild(index1, other);
            parent2.addChild(index2, this);

            this.setParent(parent2);
            other.setParent(parent1);
        }

        default boolean isDescendant(IFeatureTree source, IFeatureTree target) {
            if (source == null || target == null) {
                return false;
            }
            if (source == target) {
                return true;
            }
            for (IFeatureTree child : target.getChildren()) {
                if (isDescendant(source, child)) {
                    return true;
                }
            }
            return false;
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

<<<<<<< HEAD
    boolean isRoot();
=======
    boolean isRoot(); //sarthak

>>>>>>> 588014115fc8bc4d4d369b84f621b4cea3acfcf2
}
