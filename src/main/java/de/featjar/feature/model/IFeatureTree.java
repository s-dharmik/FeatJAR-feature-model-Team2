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

}
