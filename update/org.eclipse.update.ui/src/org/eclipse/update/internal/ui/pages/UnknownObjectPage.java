package org.eclipse.update.internal.ui.pages;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.update.internal.ui.forms.UnknownObjectForm;
import org.eclipse.update.internal.ui.views.DetailsView;
import org.eclipse.update.ui.forms.internal.*;


public class UnknownObjectPage extends UpdateFormPage {
	
	public UnknownObjectPage(DetailsView view, String title) {
		super(view, title);
	}
	
	public IForm createForm() {
		return new UnknownObjectForm(this);
	}
}