package org.eclipse.update.internal.ui.search;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.update.internal.ui.*;
import org.eclipse.swt.graphics.Image;

public class SearchCategoryDescriptor {
	private IConfigurationElement config;
	private ISearchCategory category;
	public SearchCategoryDescriptor(IConfigurationElement config) {
		this.config = config;
	}
	public String getId() {
		return config.getAttribute("id");
	}
	public String getName() {
		return config.getAttribute("name");
	}
	
	public ImageDescriptor getImageDescriptor() {
		String imageName = config.getAttribute("icon");
		if (imageName == null)
			return null;
		return UpdateUIImages.getImageDescriptorFromPlugin(
			config.getDeclaringExtension().getDeclaringPluginDescriptor(),
			imageName);
	}
	public Image getImage() {
		String imageName = config.getAttribute("icon");
		if (imageName == null)
			return null;
		return UpdateUIImages.getImageFromPlugin(
			config.getDeclaringExtension().getDeclaringPluginDescriptor(),
			imageName);
	}
	public String getDescription() {
		IConfigurationElement children [] = config.getChildren("description");
		if (children.length==1) {
			return children[0].getValue();
		}
		return "<form></form>";
	}
	public ISearchCategory createCategory() {
		if (category!=null) return category;
		try {
			Object obj = config.createExecutableExtension("class");
			if (obj instanceof ISearchCategory) {
				ISearchCategory category = (ISearchCategory)obj;
				category.setId(getId());
				this.category = category;
				return category;
			}
		} catch (CoreException e) {
			UpdateUI.logException(e);
		}
		return null;
	}
}