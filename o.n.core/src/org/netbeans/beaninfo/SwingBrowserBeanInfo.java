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

package org.netbeans.beaninfo;

import java.awt.Image;
import java.beans.*;

import org.netbeans.core.ui.SwingBrowser;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;

/**
 * Swing HTML Browser support.
 *
 * Factory for built-in Swing HTML browser.
 *
 * @author Radim Kubacki
 */
public class SwingBrowserBeanInfo extends SimpleBeanInfo {

    /**
     * Gets the bean's <code>BeanDescriptor</code>s.
     *
     * @return BeanDescriptor describing the editable
     * properties of this bean.  May return null if the
     * information should be obtained by automatic analysis.
     */
    public BeanDescriptor getBeanDescriptor() {
        BeanDescriptor beanDescriptor = new BeanDescriptor  (SwingBrowser.class);
        beanDescriptor.setDisplayName (NbBundle.getMessage (SwingBrowserBeanInfo.class, "CTL_SwingBrowser"));
        beanDescriptor.setShortDescription (NbBundle.getMessage (SwingBrowserBeanInfo.class, "HINT_SwingBrowser"));

        beanDescriptor.setValue ("helpID", "org.openide.awt.SwingBrowser");  // NOI18N
        return beanDescriptor;
    }

    /**
     * Gets the bean's <code>PropertyDescriptor</code>s.
     * 
     * @return An array of PropertyDescriptors describing the editable
     * properties supported by this bean.  May return null if the
     * information should be obtained by automatic analysis.
     * <p>
     * If a property is indexed, then its entry in the result array will
     * belong to the IndexedPropertyDescriptor subclass of PropertyDescriptor.
     * A client of getPropertyDescriptors can use "instanceof" to check
     * if a given PropertyDescriptor is an IndexedPropertyDescriptor.
     */
    public PropertyDescriptor[] getPropertyDescriptors() {
        try {
            PropertyDescriptor [] props = new PropertyDescriptor [] {
                new PropertyDescriptor (SwingBrowser.PROP_DESCRIPTION, SwingBrowser.class, "getDescritpion", null) // NOI18N
            };
            props[0].setDisplayName (NbBundle.getMessage (SwingBrowserBeanInfo.class, "PROP_SwingBrowserDescription"));
            props[0].setShortDescription (NbBundle.getMessage (SwingBrowserBeanInfo.class, "HINT_SwingBrowserDescription"));
            return props;
        } catch( IntrospectionException e) {
            return null;
        }
    }

    /**
     * Gets the bean's <code>EventSetDescriptor</code>s.
     * 
     * @return  An array of EventSetDescriptors describing the kinds of 
     * events fired by this bean.  May return null if the information
     * should be obtained by automatic analysis.
     */
    public EventSetDescriptor[] getEventSetDescriptors() {
        try {
            return new EventSetDescriptor[] {
                new EventSetDescriptor (SwingBrowser.class, 
                        "propertyChangeListener", // NOI18N
                        PropertyChangeListener.class, 
                        new String[] {"propertyChange"}, // NOI18N
                        "addPropertyChangeListener", // NOI18N
                        "removePropertyChangeListener" // NOI18N
                )   // NOI18N
            };
        }
        catch( IntrospectionException e) {
            return null;
        }
    }

    /**
    * Returns the internal browser icon. 
    */
    public Image getIcon (int type) {
        return Utilities.loadImage("org/openide/resources/html/htmlView.gif"); // NOI18N
    }
}
