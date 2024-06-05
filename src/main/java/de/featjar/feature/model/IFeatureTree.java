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

import de.featjar.base.data.IAttributable;
import de.featjar.base.data.Range;
import de.featjar.base.data.Result;
import de.featjar.base.tree.structure.ARootedTree;
import de.featjar.base.tree.structure.IRootedTree;
import de.featjar.feature.model.FeatureTree.Group;
import de.featjar.feature.model.mixins.IHasFeatureTree;
import java.util.List;

/**
 * An ordered {@link ARootedTree} labeled with {@link Feature features}.
 * Implements some concepts from feature-oriented domain analysis, such as mandatory/optional features and groups.
 *
 * @author Elias Kuiter
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
}
