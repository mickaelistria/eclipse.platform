/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.internal.ui.console;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.ui.part.IPageBookViewPage;

/**
 * A logical console. A console is commonly used to display messages. For example,
 * a console may display the output streams of a system process. A console can be
 * displayed by one or more views.
 * <p>
 * Clients may implement this interface.
 * </p>
 * @since 3.0
 */
public interface IConsole {
	
	/**
	 * Creates and returns a new label provider for this console. The label
	 * provider provides a label and image for this console, which may be
	 * dynamic, via label property change events.
	 * 
	 * @return a label provider for this console
	 */
	public ILabelProvider createLabelProvider();
	
	/**
	 * Creates and returns a new page for this console. The page is displayed
	 * for this console in the console given view.
	 * 
	 * @param view the view in which the page is to be created
	 * @return a page book representation of this console
	 */
	public IPageBookViewPage createPage(IConsoleView view);

}
