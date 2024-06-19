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

import de.featjar.base.data.Attribute;
import de.featjar.base.data.IAttribute;
import de.featjar.base.data.Range;
import de.featjar.base.tree.structure.ARootedTree;
import de.featjar.base.tree.structure.ITree;
import de.featjar.feature.model.IFeatureTree.IMutableFeatureTree;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class FeatureTree extends ARootedTree<IFeatureTree> implements IMutableFeatureTree {

    public static final class Group {
        private Range groupRange;

        private Group(int lowerBound, int upperBound) {
            this.groupRange = Range.of(lowerBound, upperBound);
        }

        private Group(Range groupRange) {
            this.groupRange = Range.copy(groupRange);
        }

        private Group(Group otherGroup) {
            this.groupRange = Range.copy(otherGroup.groupRange);
        }

        public int getLowerBound() {
            return groupRange.getLowerBound();
        }

        public int getUpperBound() {
            return groupRange.getUpperBound();
        }

        public boolean isCardinalityGroup() {
            return !isAlternative() && !isOr() && !isAnd();
        }

        public boolean isAlternative() {
            return groupRange.is(1, 1);
        }

        public boolean isOr() {
            return groupRange.is(1, Range.OPEN);
        }

        public boolean isAnd() {
            return groupRange.is(0, Range.OPEN);
        }

        public boolean allowsZero() {
            return groupRange.getLowerBound() == 0 || groupRange.getLowerBound() == Range.OPEN;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(this);
        }

        @Override
        public boolean equals(Object obj) {
            return this == obj;
        }

        @Override
        protected Group clone() {
            return new Group(this);
        }

        @Override
        public String toString() {
            return groupRange.toString();
        }
    }

    protected final IFeature feature;

    protected int groupID;

    protected Range featureRange;
    protected List<Group> groups;

    protected LinkedHashMap<IAttribute<?>, Object> attributeValues;

    protected FeatureTree(IFeature feature) {
        this.feature = Objects.requireNonNull(feature);
        featureRange = Range.of(0, 1);
        groups = new ArrayList<>(1);
        groups.add(new Group(Range.atLeast(0)));
    }

    protected FeatureTree(FeatureTree otherFeatureTree) {
        feature = otherFeatureTree.feature;
        groupID = otherFeatureTree.groupID;
        featureRange = otherFeatureTree.featureRange.clone();
        otherFeatureTree.groups.stream().map(Group::clone).forEach(groups::add);
        attributeValues = otherFeatureTree.cloneAttributes();
    }

    @Override
    public IFeature getFeature() {
        return feature;
    }

    @Override
    public Group getGroup() {
        return parent == null ? new Group(Range.of(0, 1)) : parent.getGroups().get(groupID);
    }

    @Override
    public int getGroupID() {
        return groupID;
    }

    @Override
    public List<IFeatureTree> getGroupFeatures() {
        return parent.getChildren().stream()
                .filter(t -> t.getGroupID() == groupID)
                .collect(Collectors.toList());
    }

    @Override
    public List<Group> getGroups() {
        return Collections.unmodifiableList(groups);
    }

    public void setGroupCount(int count) {
        groups = new ArrayList<>(count);
    }

    @Override
    public String toString() {
        return feature.getName().orElse("");
    }

    @Override
    public Optional<Map<IAttribute<?>, Object>> getAttributes() {
        return attributeValues == null ? Optional.empty() : Optional.of(Collections.unmodifiableMap(attributeValues));
    }

    @Override
    public List<IFeatureTree> getRoots() {
        return List.of(this);
    }

    @Override
    public int getFeatureRangeLowerBound() {
        return featureRange.getLowerBound();
    }

    @Override
    public int getFeatureRangeUpperBound() {
        return featureRange.getUpperBound();
    }

    @Override
    public boolean isMandatory() {
        return featureRange.is(1, 1);
    }

    @Override
    public boolean isOptional() {
        return featureRange.is(0, 1);
    }

    @Override
    public ITree<IFeatureTree> cloneNode() {
        return new FeatureTree(this);
    }

    @Override
    public boolean equalsNode(IFeatureTree other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        FeatureTree otherFeatureTree = (FeatureTree) other;
        return groupID == otherFeatureTree.groupID
                && Objects.equals(feature, otherFeatureTree.feature)
                && Objects.equals(groups, otherFeatureTree.groups);
    }

    @Override
    public int hashCodeNode() {
        return Objects.hash(feature, groupID, groups);
    }

    @Override
    public void addGroup(int lowerBound, int upperBound) {
        groups.add(new Group(lowerBound, upperBound));
    }

    @Override
    public void addGroup(Range groupRange) {
        groups.add(new Group(groupRange));
    }

    public void setGroups(List<Group> groups) {
        this.groups.clear();
        this.groups.addAll(groups);
    }

    public void setGroupID(int groupID) {
        if (parent == null) throw new IllegalArgumentException("Cannot set groupID for root feature!");
        if (groupID < 0) throw new IllegalArgumentException(String.format("groupID must be positive (%d)", groupID));
        if (groupID >= parent.getGroups().size())
            throw new IllegalArgumentException(
                    String.format("groupID must be smaller than number of groups in parent feature (%d)", groupID));
        this.groupID = groupID;
    }

    @Override
    public void setGroupRange(Range groupRange) {
        getGroup().groupRange = Range.copy(groupRange);
    }

    @Override
    public void setFeatureRange(Range featureRange) {
        featureRange = Range.copy(featureRange);
    }

    @Override
    public void setMandatory() {
        if (featureRange.getUpperBound() == 0) {
            featureRange = Range.exactly(1);
        } else {
            featureRange.setLowerBound(1);
        }
    }

    @Override
    public void setOptional() {
        featureRange.setLowerBound(0);
    }

    @Override
    public <S> void setAttributeValue(Attribute<S> attribute, S value) {
        if (value == null) {
            removeAttributeValue(attribute);
            return;
        }
        checkType(attribute, value);
        validate(attribute, value);
        if (attributeValues == null) {
            attributeValues = new LinkedHashMap<>();
        }
        attributeValues.put(attribute, value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <S> S removeAttributeValue(Attribute<S> attribute) {
        if (attributeValues == null) {
            attributeValues = new LinkedHashMap<>();
        }
        return (S) attributeValues.remove(attribute);
    }

	@Override										//sarthak
	public boolean isRoot() {
		// TODO Auto-generated method stub
		return false;
	}

	
}
