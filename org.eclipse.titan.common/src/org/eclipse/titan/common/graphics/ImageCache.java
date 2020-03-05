/******************************************************************************
 * Copyright (c) 2000-2020 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.common.graphics;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.titan.common.product.ProductConstants;
import org.eclipse.ui.plugin.AbstractUIPlugin;

/**
 * Generic class to cache images inside the Common plug-in.
 *
 * @author Kristof Szabados
 * */
public final class ImageCache {
	private static final String ICONS_SUBDIR = "icons/";

	private ImageCache() {
		// Hide constructor
	}

	/**
	 * Creates an ImageDescriptor from the image's name, whose root is the
	 * icons directory.
	 *
	 * @param name
	 *                the name of the image starting from the icons
	 *                directory
	 * @return the created ImageDescriptor
	 */
	public static ImageDescriptor getImageDescriptor(final String name) {
		return AbstractUIPlugin.imageDescriptorFromPlugin(ProductConstants.PRODUCT_ID_COMMON, ICONS_SUBDIR + name);
	}
}
