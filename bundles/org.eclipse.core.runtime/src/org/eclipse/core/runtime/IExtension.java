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
package org.eclipse.core.runtime;

import org.eclipse.core.runtime.IPluginDescriptor;

/**
 * An extension declared in a plug-in.
 * All information is obtained from the declaring plug-in's 
 * manifest (<code>plugin.xml</code>) file. 
 * <p>
 * This interface is not intended to be implemented by clients.
 * </p>
 */
public interface IExtension {
	/**
	 * Returns all configuration elements declared by this extension.
	 * These elements are a direct reflection of the configuration 
	 * markup supplied in the manifest (<code>plugin.xml</code>)
	 * file for the plug-in that declares this extension.
	 * Returns an empty array if this extension does not declare any
	 * configuration elements.
	 *
	 * @return the configuration elements declared by this extension 
	 */
	public IConfigurationElement[] getConfigurationElements();
	/**
	 * Returns the descriptor of the plug-in that declares this extension.
	 * <p>
	 * <b>Note</b>: This is obsolete API that will be replaced in time with
	 * the OSGI-based Eclipse Platform Runtime introduced with Eclipse 3.0.
	 * This API will be deprecated once the APIs for the new Eclipse Platform
	 * Runtime achieve their final and stable form (post-3.0). </p>
	 * 
	 * @return the plug-in that declares this extension
	 *  
	 */
	public IPluginDescriptor getDeclaringPluginDescriptor();

	/**
	 * Returns the identifier of the parent of this extension.  This value can be used
	 * in various global facilities to discover the actual parent.
	 * <p>
	 * <b>Note</b>: This is an early access API to the new OSGI-based Eclipse 3.0
	 * Platform Runtime. Because the APIs for the new runtime have not yet been fully
	 * stabilized, they should only be used by clients needing to take particular
	 * advantage of new OSGI-specific functionality, and only then with the understanding
	 * that these APIs may well change in incompatible ways until they reach
	 * their finished, stable form (post-3.0). </p>
	 * 
	 * @return the idenifier of the parent
	 * @see Plarform.getBundle(String)
	 * @since 3.0
	 */
	public String getParentIdentifier();
	//TODO Add a method returning the long ?
	/**
	 * Returns the unique identifier of the extension point
	 * that this extension gets plugged into.
	 *
	 * @return the unique identifier of the relevant extension point
	 */
	public String getExtensionPointUniqueIdentifier();
	/**
	 * Returns a displayable label for this extension.
	 * Returns the empty string if no label for this extension
	 * is specified in the plug-in manifest file.
	 * <p> Note that any translation specified in the plug-in manifest
	 * file is automatically applied.
	 * <p>
	 *
	 * @see IPluginDescriptor#getResourceString 
	 *
	 * @return a displayable string label for this extension,
	 *    possibly the empty string
	 */
	public String getLabel();
	/**
	 * Returns the simple identifier of this extension, or <code>null</code>
	 * if this extension does not have an identifier.
	 * This identifier is specified in the plug-in manifest (<code>plugin.xml</code>) 
	 * file as a non-empty string containing no period characters 
	 * (<code>'.'</code>) and must be unique within the defining plug-in.
	 *
	 * @return the simple identifier of the extension (e.g. <code>"main"</code>)
	 *  or <code>null</code>
	 */
	public String getSimpleIdentifier();
	/**
	 * Returns the unique identifier of this extension, or <code>null</code>
	 * if this extension does not have an identifier.
	 * If available, this identifier is unique within the plug-in registry, and
	 * is composed of the identifier of the plug-in that declared
	 * this extension and this extension's simple identifier.
	 *
	 * @return the unique identifier of the extension
	 *    (e.g. <code>"com.example.acme.main"</code>), or <code>null</code>
	 */
	public String getUniqueIdentifier();

}
