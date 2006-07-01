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

package org.netbeans.beaninfo.editors;

import java.awt.*;
import java.beans.*;
import java.util.*;
import javax.swing.JList;
import javax.swing.JScrollPane;
import org.openide.explorer.propertysheet.ExPropertyEditor;
import org.openide.explorer.propertysheet.PropertyEnv;
import org.openide.explorer.propertysheet.editors.XMLPropertyEditor;
import org.openide.nodes.Node;

/** A property editor for array of Strings.
 * @author  Ian Formanek
 */
public class StringArrayEditor implements XMLPropertyEditor, StringArrayCustomizable, ExPropertyEditor {

    // constants for XML persistence
    private static final String XML_STRING_ARRAY = "StringArray"; // NOI18N
    private static final String XML_STRING_ITEM = "StringItem"; // NOI18N
    private static final String ATTR_COUNT = "count"; // NOI18N
    private static final String ATTR_INDEX = "index"; // NOI18N
    private static final String ATTR_VALUE = "value"; // NOI18N

    // private fields
    private String[] strings;
    private PropertyChangeSupport support;
    private boolean editable = true;
    private String separator = ",";

    public StringArrayEditor() {
        support = new PropertyChangeSupport (this);
    }

    public Object getValue () {
        return strings;
    }

    public void setValue (Object value) {
        strings = (String[]) value;
        support.firePropertyChange("", null, null); // NOI18N
    }

    // -----------------------------------------------------------------------------
    // StringArrayCustomizable implementation

    /** Used to acquire the current value from the PropertyEditor
    * @return the current value of the property
    */
    public String[] getStringArray () {
        return (String[])getValue ();
    }

    /** Used to modify the current value in the PropertyEditor
    * @param value the new value of the property
    */
    public void setStringArray (String[] value) {
        setValue (value);
    }

    // end of StringArrayCustomizable implementation

    protected final String getStrings(boolean quoted) {
        if (strings == null) return "null"; // NOI18N

        StringBuffer buf = new StringBuffer ();
        for (int i = 0; i < strings.length; i++) {
            // XXX handles in-string escapes if quoted
            buf.append(quoted ? "\""+strings[i]+"\"" : strings[i]); // NOI18N
            if (i != strings.length - 1) {
                buf.append (separator); 
                buf.append (' '); // NOI18N
            }
        }

        return buf.toString ();
    }

    public String getAsText () {
        return getStrings(false);
    }

    public void setAsText (String text) {
        if (text.equals("null")) { // NOI18N
            setValue(null);
            return;
        }
        StringTokenizer tok = new StringTokenizer(text, separator);
        java.util.List<String> list = new LinkedList<String>();
        while (tok.hasMoreTokens()) {
            String s = tok.nextToken();
            list.add(s.trim());
        }
        String [] a = list.toArray(new String[list.size()]);
        setValue(a);
    }

    public String getJavaInitializationString () {
        if (strings == null) return "null"; // NOI18N
        // [PENDING - wrap strings ???]
        StringBuilder buf = new StringBuilder ("new String[] {"); // NOI18N
        buf.append (getStrings(true));
        buf.append ('}'); // NOI18N
        return buf.toString ();
    }

    public String[] getTags () {
        return null;
    }

    public boolean isPaintable () {
        return false;
    }

    public void paintValue (Graphics g, Rectangle rectangle) {
    }

    public boolean supportsCustomEditor () {
        //Don't show custom editor if it's just going to show
        //an empty component
        if (!editable && (strings==null || strings.length==0)) {
            return false;
        } else {
            return true;
        }
    }

    public Component getCustomEditor () {
        if (editable) {
            return new StringArrayCustomEditor(this);
        } else {
            return new JScrollPane(new JList(getStringArray()));
        }
    }

    public void addPropertyChangeListener (PropertyChangeListener propertyChangeListener) {
        support.addPropertyChangeListener (propertyChangeListener);
    }

    public void removePropertyChangeListener (PropertyChangeListener propertyChangeListener) {
        support.removePropertyChangeListener (propertyChangeListener);
    }

    // -------------------------------------------
    // XMLPropertyEditor implementation

    /** Called to store current property value into XML subtree.
     * @param doc The XML document to store the XML in - should be used for
     *            creating nodes only
     * @return the XML DOM element representing a subtree of XML from which
               the value should be loaded
     */
    public org.w3c.dom.Node storeToXML(org.w3c.dom.Document doc) {
        org.w3c.dom.Element arrayEl = doc.createElement(XML_STRING_ARRAY);
        int count = strings != null ? strings.length : 0;
        arrayEl.setAttribute(ATTR_COUNT, Integer.toString(count));

        for (int i=0; i < count; i++) {
            org.w3c.dom.Element itemEl = doc.createElement(XML_STRING_ITEM);
            itemEl.setAttribute(ATTR_INDEX, Integer.toString(i));
            itemEl.setAttribute(ATTR_VALUE, strings[i]);
            arrayEl.appendChild(itemEl);
        }

        return arrayEl;
    }

    /** Called to load property value from specified XML subtree.
     * If succesfully loaded, the value should be available via getValue().
     * An IOException should be thrown when the value cannot be restored from
     * the specified XML element
     * @param element the XML DOM element representing a subtree of XML from
     *                which the value should be loaded
     * @exception IOException thrown when the value cannot be restored from
                  the specified XML element
     */
    public void readFromXML(org.w3c.dom.Node element) throws java.io.IOException {
        if (!XML_STRING_ARRAY.equals(element.getNodeName()))
            throw new java.io.IOException();

        org.w3c.dom.NamedNodeMap attributes = element.getAttributes();
        String[] stringArray;
        org.w3c.dom.Node countNode = null;
        int count = 0;

        if ((countNode = attributes.getNamedItem(ATTR_COUNT)) != null
                && (count = Integer.parseInt(countNode.getNodeValue())) > 0) {
            stringArray = new String[count];
            org.w3c.dom.NodeList items = element.getChildNodes();
            org.w3c.dom.Element itemEl;

            for (int i = 0; i < items.getLength(); i++) {
                if (items.item(i).getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                    itemEl = (org.w3c.dom.Element)items.item(i);
                    if (itemEl.getNodeName().equals(XML_STRING_ITEM)) {
                        String indexStr = itemEl.getAttribute(ATTR_INDEX);
                        String valueStr = itemEl.getAttribute(ATTR_VALUE);
                        if (indexStr != null && valueStr != null) {
                            int index = Integer.parseInt(indexStr);
                            if (index >=0 && index < count)
                                stringArray[index] = valueStr;
                        }
                    }
                }
            }
        }
        else stringArray = new String[0];

        setValue(stringArray);
    }
    
    public void attachEnv(PropertyEnv env) {
        FeatureDescriptor d = env.getFeatureDescriptor();
        readEnv (env.getFeatureDescriptor ());
    }
    
    final void readEnv (FeatureDescriptor d) {
        if (d instanceof Node.Property) {
            editable = ((Node.Property)d).canWrite();
        } else if (d instanceof PropertyDescriptor) {
            editable = ((PropertyDescriptor)d).getWriteMethod() != null;
        } else {
            editable = true;
        }
        
        Object v = d.getValue ("item.separator"); // NOI18N
        if (v instanceof String) {
            separator = (String)v;
        }
    }
}
