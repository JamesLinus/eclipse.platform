package org.eclipse.update.internal.ui.pages;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.update.internal.ui.forms.DetailsForm;
import org.eclipse.update.internal.ui.views.DetailsView;
import org.eclipse.update.ui.forms.internal.*;


public class DetailsPage extends UpdateFormPage {
	
	public DetailsPage(DetailsView view, String title) {
		super(view, title);
	}
	
	public IForm createForm() {
		return new DetailsForm(this);
	}
}