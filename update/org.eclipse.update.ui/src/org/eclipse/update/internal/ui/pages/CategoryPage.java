package org.eclipse.update.internal.ui.pages;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.update.internal.ui.forms.CategoryForm;
import org.eclipse.update.internal.ui.views.DetailsView;
import org.eclipse.update.ui.forms.internal.*;


public class CategoryPage extends UpdateFormPage {
	
	public CategoryPage(DetailsView view, String title) {
		super(view, title);
	}
	
	public IForm createForm() {
		return new CategoryForm(this);
	}
}