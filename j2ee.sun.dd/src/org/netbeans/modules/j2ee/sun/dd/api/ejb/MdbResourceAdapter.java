/*
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at http://www.netbeans.org/cddl.html
 * or http://www.netbeans.org/cddl.txt.
 *
 * When distributing Covered Code, include this CDDL Header Notice in each file
 * and include the License file at http://www.netbeans.org/cddl.txt.
 * If applicable, add the following below the CDDL Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 */
/*
 * MdbResourceAdapter.java
 *
 * Created on November 17, 2004, 5:19 PM
 */

package org.netbeans.modules.j2ee.sun.dd.api.ejb;

/**
 *
 * @author  Nitya Doraisamy
 */
public interface MdbResourceAdapter extends org.netbeans.modules.j2ee.sun.dd.api.CommonDDBean {

    public static final String RESOURCE_ADAPTER_MID = "ResourceAdapterMid";	// NOI18N
    public static final String ACTIVATION_CONFIG = "ActivationConfig";	// NOI18N

    /** Setter for resource-adapter-mid property
     * @param value property value
     */
    public void setResourceAdapterMid(java.lang.String value);
    /** Getter for resource-adapter-mid property.
     * @return property value
     */
    public java.lang.String getResourceAdapterMid();
    /** Setter for activation-config property
     * @param value property value
     */
    public void setActivationConfig(ActivationConfig value);
    /** Getter for activation-config property.
     * @return property value
     */
    public ActivationConfig getActivationConfig(); 
    
    public ActivationConfig newActivationConfig();
}
