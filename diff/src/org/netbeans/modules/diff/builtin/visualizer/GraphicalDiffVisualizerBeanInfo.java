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

package org.netbeans.modules.diff.builtin.visualizer;

import java.beans.*;

import org.openide.util.NbBundle;
import org.openide.util.Utilities;

/**
 * BeanInfo for graphical diff visualizer.
 *
 * @author Martin Entlicher
 */
public class GraphicalDiffVisualizerBeanInfo extends SimpleBeanInfo {

    /**
     * Gets the bean's <code>BeanDescriptor</code>s.
     *
     * @return BeanDescriptor describing the editable
     * properties of this bean.  May return null if the
     * information should be obtained by automatic analysis.
     */
    public BeanDescriptor getBeanDescriptor() {
        return new BeanDescriptor(GraphicalDiffVisualizer.class);
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
        PropertyDescriptor[] desc;
        try {
            PropertyDescriptor colorAdded = new PropertyDescriptor("colorAdded", GraphicalDiffVisualizer.class);
            colorAdded.setDisplayName      (NbBundle.getMessage(GraphicalDiffVisualizerBeanInfo.class, "PROP_colorAdded"));
            colorAdded.setShortDescription (NbBundle.getMessage(GraphicalDiffVisualizerBeanInfo.class, "HINT_colorAdded"));
            PropertyDescriptor colorMissing = new PropertyDescriptor("colorMissing", GraphicalDiffVisualizer.class);
            colorMissing.setDisplayName      (NbBundle.getMessage(GraphicalDiffVisualizerBeanInfo.class, "PROP_colorMissing"));
            colorMissing.setShortDescription (NbBundle.getMessage(GraphicalDiffVisualizerBeanInfo.class, "HINT_colorMissing"));
            PropertyDescriptor colorChanged = new PropertyDescriptor("colorChanged", GraphicalDiffVisualizer.class);
            colorChanged.setDisplayName      (NbBundle.getMessage(GraphicalDiffVisualizerBeanInfo.class, "PROP_colorChanged"));
            colorChanged.setShortDescription (NbBundle.getMessage(GraphicalDiffVisualizerBeanInfo.class, "HINT_colorChanged"));
            desc = new PropertyDescriptor[] { colorAdded, colorMissing, colorChanged };
        } catch (IntrospectionException ex) {
            org.openide.ErrorManager.getDefault().notify(ex);
            desc = null;
        }
        return desc;
    }
    
    /**
     * A bean may have a "default" property that is the property that will
     * mostly commonly be initially chosen for update by human's who are
     * customizing the bean.
     * @return  Index of default property in the PropertyDescriptor array
     * 		returned by getPropertyDescriptors.
     * <P>	Returns -1 if there is no default property.
     */
    public int getDefaultPropertyIndex() {
        return 0;
    }
    
    /**
     * This method returns an image object that can be used to
     * represent the bean in toolboxes, toolbars, etc.   Icon images
     * will typically be GIFs, but may in future include other formats.
     * <p>
     * Beans aren't required to provide icons and may return null from
     * this method.
     * <p>
     * There are four possible flavors of icons (16x16 color,
     * 32x32 color, 16x16 mono, 32x32 mono).  If a bean choses to only
     * support a single icon we recommend supporting 16x16 color.
     * <p>
     * We recommend that icons have a "transparent" background
     * so they can be rendered onto an existing background.
     *
     * @param  iconKind  The kind of icon requested.  This should be
     *    one of the constant values ICON_COLOR_16x16, ICON_COLOR_32x32,
     *    ICON_MONO_16x16, or ICON_MONO_32x32.
     * @return  An image object representing the requested icon.  May
     *    return null if no suitable icon is available.
     */
    public java.awt.Image getIcon(int iconKind) {
        switch (iconKind) {
            case ICON_COLOR_16x16:
                return Utilities.loadImage("org/netbeans/modules/diff/diffSettingsIcon.gif", true); // NOI18N
        }
        return null;
    }
    
}
