package org.eclipse.update.core.model;

/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */ 

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * An object which represents a site map.
 * <p>
 * This class may be instantiated, or further subclassed.
 * </p>
 * @since 2.0
 */

public class SiteMapModel extends ModelObject {
	
	private String type;
	private URLEntryModel description;
	private List /*of FeatureReferenceModel*/ featureReferences;
	private List /*of ArchiveReferenceModel*/ archiveReferences;
	private List /*of SiteCategoryModel*/ categories;
	
	/**
	 * Creates an uninitialized model object.
	 * 
	 * @since 2.0
	 */
	public SiteMapModel() {
		super();
	}
	
	/**
	 * @since 2.0
	 */
	public String getType() {
		return type;
	}
	
	/**
	 * @since 2.0
	 */
	public URLEntryModel getDescriptionModel() {
		return description;
	}

	/**
	 * @since 2.0
	 */
	public FeatureReferenceModel[] getFeatureReferenceModels() {
		if (featureReferences == null)
			return new FeatureReferenceModel[0];
			
		return (FeatureReferenceModel[]) featureReferences.toArray(new FeatureReferenceModel[0]);
	}

	/**
	 * @since 2.0
	 */
	public ArchiveReferenceModel[] getArchiveReferenceModels() {
		if (archiveReferences == null)
			return new ArchiveReferenceModel[0];
			
		return (ArchiveReferenceModel[]) archiveReferences.toArray(new ArchiveReferenceModel[0]);
	}

	/**
	 * @since 2.0
	 */
	public SiteCategoryModel[] getCategoryModels() {
		if (categories == null)
			return new SiteCategoryModel[0];
			
		return (SiteCategoryModel[]) categories.toArray(new SiteCategoryModel[0]);
	}
	
	/**
	 * @since 2.0
	 */
	public void setType(String type) {
		assertIsWriteable();
		this.type = type;
	}
	
	/**
	 * @since 2.0
	 */
	public void setDescriptionModel(URLEntryModel description) {
		assertIsWriteable();
		this.description = description;
	}

	/**
	 * @since 2.0
	 */
	public void setFeatureReferenceModels(FeatureReferenceModel[] featureReferences) {
		assertIsWriteable();
		if (featureReferences == null)
			this.featureReferences = null;
		else
			this.featureReferences = Arrays.asList(featureReferences);
	}

	/**
	 * @since 2.0
	 */
	public void setArchiveReferenceModels(ArchiveReferenceModel[] archiveReferences) {
		assertIsWriteable();
		if (archiveReferences == null)
			this.archiveReferences = null;
		else
			this.archiveReferences = Arrays.asList(archiveReferences);
	}

	/**
	 * @since 2.0
	 */
	public void setCategoryModels(SiteCategoryModel[] categories) {
		assertIsWriteable();
		if (categories == null)
			this.categories = null;
		else
			this.categories = Arrays.asList(categories);
	}

	/**
	 * @since 2.0
	 */
	public void addFeatureReferenceModel(FeatureReferenceModel featureReference) {
		assertIsWriteable();
		if (this.featureReferences == null)
			this.featureReferences = new ArrayList();
		if (!this.featureReferences.contains(featureReference))
			this.featureReferences.add(featureReference);
	}

	/**
	 * @since 2.0
	 */
	public void addArchiveReferenceModel(ArchiveReferenceModel archiveReference) {
		assertIsWriteable();
		if (this.archiveReferences == null)
			this.archiveReferences = new ArrayList();
		if (!this.archiveReferences.contains(archiveReference))
			this.archiveReferences.add(archiveReference);
	}

	/**
	 * @since 2.0
	 */
	public void addCategoryModel(SiteCategoryModel category) {
		assertIsWriteable();
		if (this.categories == null)
			this.categories = new ArrayList();
		if (!this.categories.contains(category))
			this.categories.add(category);
	}

	/**
	 * @since 2.0
	 */
	public void removeFeatureReferenceModel(FeatureReferenceModel featureReference) {
		assertIsWriteable();
		if (this.featureReferences != null)
			this.featureReferences.remove(featureReference);
	}

	/**
	 * @since 2.0
	 */
	public void removeArchiveReferenceModel(ArchiveReferenceModel archiveReference) {
		assertIsWriteable();
		if (this.archiveReferences != null)
			this.archiveReferences.remove(archiveReference);
	}

	/**
	 * @since 2.0
	 */
	public void removeCategoryModel(SiteCategoryModel category) {
		assertIsWriteable();
		if (this.categories != null)
			this.categories.remove(category);
	}
	
	/**
	 * @since 2.0
	 */
	public void markReadOnly() {		
		markReferenceReadOnly(getDescriptionModel());
		markListReferenceReadOnly(getFeatureReferenceModels());
		markListReferenceReadOnly(getArchiveReferenceModels());
		markListReferenceReadOnly(getCategoryModels());
	}
}
