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
import de.featjar.base.data.IAttributable.IMutatableAttributable;
import de.featjar.base.data.IAttribute;
import de.featjar.base.data.Maps;
import de.featjar.base.data.Result;
import de.featjar.base.data.identifier.IIdentifier;
import de.featjar.base.data.identifier.UUIDIdentifier;
import de.featjar.base.tree.Trees;
import de.featjar.base.tree.visitor.TreePrinter;
import de.featjar.feature.model.IFeatureModel.IMutableFeatureModel;
import de.featjar.formula.structure.formula.IFormula;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors; //sarthak

public class FeatureModel implements IMutableFeatureModel, IMutatableAttributable {

    protected final IIdentifier identifier;

    protected final List<IFeatureTree> featureTreeRoots;
    protected final LinkedHashMap<IIdentifier, IFeature> features;
    protected final LinkedHashMap<IIdentifier, IConstraint> constraints;

    protected final LinkedHashMap<IAttribute<?>, Object> attributeValues;

	private boolean shouldDeleteRootFeatureFlag;

	private IFeature rootFeature;		//sarthak

    public FeatureModel() {
        this(UUIDIdentifier.newInstance());
    }

    
   
    public FeatureModel(IIdentifier identifier) {
        this.identifier = Objects.requireNonNull(identifier);
        featureTreeRoots = new ArrayList<>(1);
        features = Maps.empty();
        constraints = Maps.empty();
        attributeValues = new LinkedHashMap<>(4);
    }

    protected FeatureModel(FeatureModel otherFeatureModel) {
        identifier = otherFeatureModel.getNewIdentifier();

        featureTreeRoots = new ArrayList<>(otherFeatureModel.featureTreeRoots.size());
        otherFeatureModel.featureTreeRoots.stream().forEach(t -> featureTreeRoots.add(Trees.clone(t)));

        features = new LinkedHashMap<>((int) (otherFeatureModel.features.size() * 1.5));
        otherFeatureModel.features.entrySet().stream()
                .map(e -> e.getValue().clone(this))
                .forEach(f -> features.put(f.getIdentifier(), f));

        constraints = new LinkedHashMap<>((int) (otherFeatureModel.constraints.size() * 1.5));
        otherFeatureModel.constraints.entrySet().stream()
                .map(e -> e.getValue().clone(this))
                .forEach(c -> constraints.put(c.getIdentifier(), c));

        attributeValues = otherFeatureModel.cloneAttributes();
    }

    @Override
    public FeatureModel clone() {
        return new FeatureModel(this);
    }

    @Override
    public FeatureModel getFeatureModel() {
        return this;
    }

    @Override
    public List<IFeatureTree> getRoots() {
        return featureTreeRoots;
    }

    @Override
    public Collection<IFeature> getFeatures() {
        return Collections.unmodifiableCollection(features.values());
    }

    @Override
    public Result<IFeature> getFeature(IIdentifier identifier) {
        return Result.of(features.get(Objects.requireNonNull(identifier)));
    }

    @Override
    public Collection<IConstraint> getConstraints() {
        return Collections.unmodifiableCollection(constraints.values());
    }

    @Override
    public Result<IConstraint> getConstraint(IIdentifier identifier) {
        return Result.of(constraints.get(Objects.requireNonNull(identifier)));
    }

    @Override
    public boolean hasConstraint(IIdentifier identifier) {
        return constraints.containsKey(identifier);
    }

    @Override
    public boolean hasConstraint(IConstraint constraint) {
        return constraints.containsKey(constraint.getIdentifier());
    }

    @Override
    public int getNumberOfConstraints() {
        return constraints.size();
    }

    @Override
    public IIdentifier getIdentifier() {
        return identifier;
    }

    @Override
    public Optional<Map<IAttribute<?>, Object>> getAttributes() {
        return Optional.of(Collections.unmodifiableMap(attributeValues));
    }

    @Override
    public <S> void setAttributeValue(Attribute<S> attribute, S value) {
        if (value == null) {
            removeAttributeValue(attribute);
            return;
        }
        checkType(attribute, value);
        validate(attribute, value);
        attributeValues.put(attribute, value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <S> S removeAttributeValue(Attribute<S> attribute) {
        return (S) attributeValues.remove(attribute);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return getIdentifier().equals(((FeatureModel) o).getIdentifier());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getIdentifier());
    }

    @Override
    public String toString() {
        StringBuilder featureString = new StringBuilder();
        for (IFeatureTree root : featureTreeRoots) {
            featureString.append(Trees.traverse(root, new TreePrinter()).get());
            featureString.append('\n');
        }
        return String.format(
                "FeatureModel{features=%s, constraints=%s}", featureString.toString(), constraints.toString());
    }

    @Override
    public void setName(String name) {
        attributeValues.put(Attributes.NAME, name);
    }

    @Override
    public void setDescription(String description) {
        attributeValues.put(Attributes.DESCRIPTION, description);
    }

    @Override
    public IFeatureTree addFeatureTreeRoot(IFeature feature) {
        FeatureTree newTree = new FeatureTree(feature);
        featureTreeRoots.add(newTree);
        return newTree;
    }

    @Override
    public void addFeatureTreeRoot(IFeatureTree featureTree) {
        featureTreeRoots.add(featureTree);
    }

    @Override
    public void removeFeatureTreeRoot(IFeature feature) {
        for (Iterator<IFeatureTree> it = featureTreeRoots.listIterator(); it.hasNext(); ) {
            if (it.next().getFeature().equals(feature)) {
                it.remove();
            }
        }
    }

    @Override
    public void removeFeatureTreeRoot(IFeatureTree featureTree) {
        for (Iterator<IFeatureTree> it = featureTreeRoots.listIterator(); it.hasNext(); ) {
            if (it.next() == featureTree) {
                it.remove();
            }
        }
    }

    @Override
    public IConstraint addConstraint(IFormula formula) {
        IConstraint newConstraint = new Constraint(this, Trees.clone(formula));
        constraints.put(newConstraint.getIdentifier(), newConstraint);
        return newConstraint;
    }

    @Override
    public boolean removeConstraint(IConstraint constraint) {
        Objects.requireNonNull(constraint);
        return constraints.remove(constraint.getIdentifier()) != null;
    }

    @Override
    public IFeature addFeature(String name) {
        Objects.requireNonNull(name);
        Feature feature = new Feature(this);
        feature.setName(name);
        features.put(feature.getIdentifier(), feature);
        return feature;
    }

    @Override
    public boolean removeFeature(IFeature feature) {
        return features.remove(feature.getIdentifier()) != null;
    }

    @Override
    public int getNumberOfFeatures() {
        return features.size();
    }

    @Override
    public Result<IFeature> getFeature(String name) {
        return Result.ofOptional(features.entrySet().stream()
                .map(e -> e.getValue())
                .filter(f -> f.getName().valueEquals(name))
                .findFirst());
    }

    @Override
    public boolean hasFeature(IIdentifier identifier) {
        return features.containsKey(identifier);
    }

    @Override
    public boolean hasFeature(IFeature feature) {
        return features.containsKey(feature.getIdentifier());
    }
    
  //sarthak
    

    @Override
    public boolean deleteFeatureAndPromoteChildren(IIdentifier featureId) {
        // Retrieve the feature to be deleted; exit if not found
        Result<IFeature> maybeFeatureToDelete = getFeature(featureId);
        if (maybeFeatureToDelete.isEmpty()) {
            return false; // Handle case where the feature doesn't exist
        }

        IFeature featureToDelete = maybeFeatureToDelete.get();
        // Retrieve the feature's tree; exit if it does not exist
        Result<IFeatureTree> maybeFeatureTree = featureToDelete.getFeatureTree();
        if (maybeFeatureTree.isEmpty()) {
            return false; // Handle case where the feature has no associated tree (safety check)
        }

        IFeatureTree featureTree = maybeFeatureTree.get();

        // Determine if the feature is a root
        if (featureTree.isRoot()) {
            // Get all direct children of the feature
            List<IFeature> children = new ArrayList<>(featureTree.getChildren().stream()
                .map(IFeatureTree::getFeature)
                .collect(Collectors.toList()));
            
            // Remove the root feature from the model if needed
            if (shouldDeleteRootFeature(featureToDelete)) {
                featureTree.mutate().removeFromTree(); // Remove the root feature

                // Promote a child to root level (allow developer to choose)
                if (!children.isEmpty()) {
                    IFeature selectedChild = children.get(0); // For simplicity, select the first child
                    // Detach the selected child from its current parent tree
                    selectedChild.getFeatureTree().get().mutate().removeFromTree();
                    // Add the selected child as a new root in the model
                    mutate().addFeatureTreeRoot(selectedChild);
                    System.out.println("Promoted child " + selectedChild.getName() + " to root feature.");
                }
            }
            return true; // Return true if root feature is deleted and child is promoted
        } else {
            // If the feature is not a root, remove it from its parent tree
            featureTree.mutate().removeFromTree();
            return true; // Return true if non-root feature is deleted
        }
    }





        protected boolean shouldDeleteRootFeature(IFeature feature) {
            return true;
        }



		@Override
		public void setShouldDeleteRootFeatureFlag(boolean b) {
			// TODO Auto-generated method stub
			
		}


		

}
