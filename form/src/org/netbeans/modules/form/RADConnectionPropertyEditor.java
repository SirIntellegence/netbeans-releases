/*
 *                 Sun Public License Notice
 * 
 * The contents of this file are subject to the Sun Public License
 * Version 1.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://www.sun.com/
 * 
 * The Original Code is Forte for Java, Community Edition. The Initial
 * Developer of the Original Code is Sun Microsystems, Inc. Portions
 * Copyright 1997-2000 Sun Microsystems, Inc. All Rights Reserved.
 */

package com.netbeans.developer.modules.loaders.form;

import java.awt.*;
import java.beans.*;
import java.text.MessageFormat;

import org.openide.TopManager;
import org.openide.explorer.propertysheet.editors.XMLPropertyEditor;

/**
* RADConnectionPropertyEditor is a property editor for ListModel, which encapsulates a connection to existing ListModel beans on the form
*
* @author  Ian Formanek
*/
public class RADConnectionPropertyEditor extends Object implements PropertyEditor, FormAwareEditor, XMLPropertyEditor {

  protected PropertyChangeSupport support;
  private Class propertyType;
  private RADComponent rcomponent;
  private RADConnectionDesignValue emptyValue = null;
  private RADConnectionDesignValue currentValue = emptyValue;

  /** Creates a new RADConnectionPropertyEditor */
  public RADConnectionPropertyEditor (Class propertyType) {
    support = new PropertyChangeSupport (this);
    this.propertyType = propertyType;
/*    if (System.getProperty ("Ahoj") != null) {
    System.out.println ("&&& &&& &&& >>> RADConn init: "+ this); 
      Thread.dumpStack ();
    }  */
  }

  /** Called to set the RADComponent for which this property editor was created.
  * @param node the RADComponent for which this property editor was created
  */
  public void setRADComponent (RADComponent rcomp) {
/*    if (System.getProperty ("Ahoj") != null) {
    System.out.println ("&&& &&& &&& >>> RADConn setRADCOmponent: "+ this + " , rcomponent : "+rcomponent);
} */
    rcomponent = rcomp;
  }

// -----------------------------------------------------------------------------
// PropertyEditor implementation

  public Object getValue () {
    return currentValue;
  }

  public void setValue (Object value) {
    if (value instanceof RADConnectionDesignValue) {
      currentValue = (RADConnectionDesignValue)value;
    } else {
      currentValue = emptyValue;
    }
    support.firePropertyChange ("", null, null);
  }

	public void setAsText (String string) {
	}

  public String getAsText () {
    if (currentValue != null)
      return currentValue.getName ();
    else return FormEditor.getFormBundle ().getString ("CTL_CONNECTION_NOT_SET"); //"<Not Set>"; 
  }

	public String[] getTags () 
	{
		return null;
	}

  public boolean isPaintable () {
    return false;
  }

  public void paintValue (Graphics g, Rectangle rectangle) {
  }

  public boolean supportsCustomEditor () {
    return true;
  }

  public java.awt.Component getCustomEditor () {
/*    if (System.getProperty ("Ahoj") != null) {
    System.out.println ("&&& &&& &&& >>> RADConn getCustom: "+ this + ", rcomponent: "+rcomponent);
}*/
    ParametersPicker pp = new ParametersPicker (rcomponent.getFormManager (), rcomponent, propertyType);
    pp.setPropertyValue (currentValue);
    return pp;
  }

  public String getJavaInitializationString () {
    if (currentValue != null) {
      switch (currentValue.type) {
        case RADConnectionDesignValue.TYPE_VALUE: return currentValue.value;     
        case RADConnectionDesignValue.TYPE_CODE: return currentValue.userCode;
        case RADConnectionDesignValue.TYPE_PROPERTY: 
            PropertyDescriptor pd = currentValue.getProperty ();
            if (pd == null) return null; // failed to initialize => do not generate code
            else return currentValue.radComponentName + "." + pd.getReadMethod ().getName () + " ()"; // [FUTURE: Handle indexed properties]
        case RADConnectionDesignValue.TYPE_METHOD: return currentValue.radComponentName + "." + currentValue.methodName + " ()";
      }
    }
    return null;
  }

  public void addPropertyChangeListener (PropertyChangeListener propertyChangeListener) {
    support.addPropertyChangeListener (propertyChangeListener);
  }

  public void removePropertyChangeListener (PropertyChangeListener propertyChangeListener) {
    support.removePropertyChangeListener (propertyChangeListener);
  }


  public static class RADConnectionDesignValue implements FormDesignValue, java.io.Serializable {
    public final static int TYPE_PROPERTY = 0;
    public final static int TYPE_METHOD = 1;
    public final static int TYPE_CODE = 2;
    public final static int TYPE_VALUE = 3;

    /** Determines the type of connection design value */
    int type;
    
    transient RADComponent radComponent = null; // used if type = TYPE_PROPERTY or TYPE_METHOD
    String radComponentName = null;             // used if type = TYPE_PROPERTY or TYPE_METHOD

    transient MethodDescriptor method = null;             // used if type = TYPE_METHOD
    String methodName = null;                   // used if type = TYPE_METHOD
    transient PropertyDescriptor property = null;         // used if type = TYPE_PROPERTY
    String propertyName = null;                 // used if type = TYPE_PROPERTY
    String userCode = null;                     // used if type = TYPE_CODE
    String value = null;                        // used if type = TYPE_VALUE
    String requiredTypeName = null;             // used if type = TYPE_VALUE
    
    transient private boolean needsInit = false; // used for deserialization init if type = TYPE_PROPERTY or TYPE_METHOD
    transient private FormManager2 formManager;  // used for deserialization init if type = TYPE_PROPERTY or TYPE_METHOD
    
    RADConnectionDesignValue (RADComponent comp, MethodDescriptor md) {
      radComponent = comp;
      radComponentName = radComponent.getName ();
      method = md;
      methodName = md.getName ();
      type = TYPE_METHOD;
    }

    RADConnectionDesignValue (RADComponent comp, PropertyDescriptor pd) {
      radComponent = comp;
      radComponentName = radComponent.getName ();
      property = pd;
      propertyName = pd.getName ();
      type = TYPE_PROPERTY;
    }

    private RADConnectionDesignValue (String compName, int type, String name, FormManager2 manager) {
      radComponentName = compName;
      formManager = manager;
      if (type == TYPE_PROPERTY) {
        needsInit = true;
        type = TYPE_PROPERTY;
        propertyName = name;
      } else if (type == TYPE_METHOD) {
        needsInit = true;
        type = TYPE_METHOD;
        methodName = name;
      } else throw new IllegalArgumentException ();
    }

    RADConnectionDesignValue (Class requiredType, String valueText) {
      this.requiredTypeName = requiredType.getName ();
      this.value = value;
      type = TYPE_VALUE;
    }
    
    RADConnectionDesignValue (String userCode) {
      this.userCode = userCode;
      type = TYPE_CODE;
    }

    String getName () {
      switch (type) {
        case TYPE_PROPERTY: return MessageFormat.format (FormEditor.getFormBundle ().getString ("FMT_PROPERTY_CONN"), new Object[] { radComponentName, propertyName }); 
        case TYPE_METHOD: return MessageFormat.format (FormEditor.getFormBundle ().getString ("FMT_METHOD_CONN"), new Object[] { radComponentName, methodName }); 
        case TYPE_VALUE: return MessageFormat.format (FormEditor.getFormBundle ().getString ("FMT_VALUE_CONN"), new Object[] { value }); 
        case TYPE_CODE: return FormEditor.getFormBundle ().getString ("CTL_CODE_CONN"); 
      }
      throw new InternalError ();
    }
    
    PropertyDescriptor getProperty () {
      if (needsInit) {
        if (!initialize ()) return null;
      }
      return property;
    }

    private boolean initialize () {
      radComponent = formManager.findRADComponent (radComponentName);
      if (radComponent != null) {
        if (type == TYPE_PROPERTY) { // property
          PropertyDescriptor[] componentsProps = radComponent.getBeanInfo ().getPropertyDescriptors ();
          for (int i = 0; i < componentsProps.length; i++) {
            if (componentsProps[i].getName ().equals (propertyName)) {
              property = componentsProps[i];
              return true;
            }
          } // if the property of given name cannot be found => ignore
        } else { // method
          MethodDescriptor[] componentMethods = radComponent.getBeanInfo ().getMethodDescriptors ();
          for (int i = 0; i < componentMethods.length; i++) {
            if (componentMethods[i].getName ().equals (methodName)) {
              method = componentMethods[i];
              return true;
            }
          } // if the property of given name cannot be found => ignore
        }
      } // if the component cannot be found, simply ignore it
      return false;
    }

    /** Provides a value which should be used during design-time
    * as the real property value on the bean instance.
    * E.g. the ResourceBundle String would provide the real value
    * of the String from the resource bundle, so that the design-time
    * representation reflects the real code being generated.
    * @param radComponent the radComponent in which this property is used
    * @return the real property value to be used during design-time
    */
    public Object getDesignValue (RADComponent radComponent) {
/*      if (needsInit) {
        if (!initialize ()) {
          return IGNORED_VALUE; // failed to initialize
        }
      } */
      switch (type) {
        case TYPE_PROPERTY: 
            return FormDesignValue.IGNORED_VALUE; // [PENDING: use the value during design time]
/*            try {
              Object value = property.getReadMethod ().invoke (radComponent.getBeanInstance (), new Object[0]);
              return value;
            } catch (Exception e) {
              // in case of failure do not provide the value during design time
              return FormDesignValue.IGNORED_VALUE;
            }*/ // [PENDING]
        case TYPE_METHOD: 
            return FormDesignValue.IGNORED_VALUE; // [PENDING: use the value during design time]
/*            try {
              Object value = method.getMethod ().invoke (radComponent.getBeanInstance (), new Object[0]);
              return value;
            } catch (Exception e) {
              // in case of failure do not provide the value during design time
              return FormDesignValue.IGNORED_VALUE;
            } */ // [PENDING]
        case TYPE_VALUE: 
            return FormDesignValue.IGNORED_VALUE; // [PENDING: use the value during design time]
        case TYPE_CODE: 
        default:         
            return FormDesignValue.IGNORED_VALUE; 
      }
    }
  }

//--------------------------------------------------------------------------
// XMLPropertyEditor implementation

  public static final String XML_CONNECTION = "Connection";

  public static final String ATTR_TYPE = "type";
  public static final String ATTR_COMPONENT = "component";
  public static final String ATTR_NAME = "name";
  public static final String ATTR_CODE = "code";
  public static final String ATTR_VALUE = "value";
  public static final String ATTR_REQUIRED_TYPE = "valueType";

  public static final String VALUE_VALUE = "value";
  public static final String VALUE_PROPERTY = "property";
  public static final String VALUE_METHOD = "method";
  public static final String VALUE_CODE = "code";

  /** Called to load property value from specified XML subtree. If succesfully loaded, 
  * the value should be available via the getValue method.
  * An IOException should be thrown when the value cannot be restored from the specified XML element
  * @param element the XML DOM element representing a subtree of XML from which the value should be loaded
  * @exception IOException thrown when the value cannot be restored from the specified XML element
  */
  public void readFromXML (org.w3c.dom.Node element) throws java.io.IOException {
    if (!XML_CONNECTION.equals (element.getNodeName ())) {
      throw new java.io.IOException ();
    }
    org.w3c.dom.NamedNodeMap attributes = element.getAttributes ();
    try {
      String typeString = attributes.getNamedItem (ATTR_TYPE).getNodeValue ();
      int type = RADConnectionDesignValue.TYPE_CODE;
      if (VALUE_VALUE.equals (typeString)) {
        String value = attributes.getNamedItem (ATTR_VALUE).getNodeValue ();
        String valueType = attributes.getNamedItem (ATTR_REQUIRED_TYPE).getNodeValue ();
        try {
          Class reqType = TopManager.getDefault ().currentClassLoader ().loadClass (valueType);
          setValue (new RADConnectionDesignValue (reqType, value));
        } catch (Exception e) { 
          // ignore failures... and use no conn instead
        }

      } else if (VALUE_PROPERTY.equals (typeString)) {
        String component = attributes.getNamedItem (ATTR_COMPONENT).getNodeValue ();
        String name = attributes.getNamedItem (ATTR_NAME).getNodeValue ();
        setValue (new RADConnectionDesignValue (component, RADConnectionDesignValue.TYPE_PROPERTY, name, rcomponent.getFormManager ()));

      } else if (VALUE_METHOD.equals (typeString)) {
        String component = attributes.getNamedItem (ATTR_COMPONENT).getNodeValue ();
        String name = attributes.getNamedItem (ATTR_NAME).getNodeValue ();
        setValue (new RADConnectionDesignValue (component, RADConnectionDesignValue.TYPE_METHOD, name, rcomponent.getFormManager ()));

      } else {
        String code = attributes.getNamedItem (ATTR_CODE).getNodeValue ();
        setValue (new RADConnectionDesignValue (code));
      }


    } catch (NullPointerException e) {
      if (System.getProperty ("netbeans.debug.exceptions") != null) {
        e.printStackTrace ();
      }
      throw new java.io.IOException ();
    }
  }
  
  /** Called to store current property value into XML subtree. The property value should be set using the
  * setValue method prior to calling this method.
  * @param doc The XML document to store the XML in - should be used for creating nodes only
  * @return the XML DOM element representing a subtree of XML from which the value should be loaded
  */
  public org.w3c.dom.Node storeToXML(org.w3c.dom.Document doc) {
    org.w3c.dom.Element el = doc.createElement (XML_CONNECTION);
    if (currentValue == null) return null;
    String typeString;
    switch (currentValue.type) {
      case RADConnectionDesignValue.TYPE_VALUE: typeString = VALUE_VALUE; break;
      case RADConnectionDesignValue.TYPE_PROPERTY: typeString = VALUE_PROPERTY; break;
      case RADConnectionDesignValue.TYPE_METHOD: typeString = VALUE_METHOD; break;
      case RADConnectionDesignValue.TYPE_CODE: 
      default:
           typeString = VALUE_CODE; break;
    }
    el.setAttribute (ATTR_TYPE, typeString);
    switch (currentValue.type) {
      case RADConnectionDesignValue.TYPE_VALUE: 
           el.setAttribute (ATTR_VALUE, currentValue.value);
           el.setAttribute (ATTR_REQUIRED_TYPE, currentValue.requiredTypeName);
           break;
      case RADConnectionDesignValue.TYPE_PROPERTY:
           el.setAttribute (ATTR_COMPONENT, currentValue.radComponentName);
           el.setAttribute (ATTR_NAME, currentValue.propertyName);
           break;
      case RADConnectionDesignValue.TYPE_METHOD: 
           el.setAttribute (ATTR_COMPONENT, currentValue.radComponentName);
           el.setAttribute (ATTR_NAME, currentValue.methodName);
           break;
      case RADConnectionDesignValue.TYPE_CODE: 
           el.setAttribute (ATTR_CODE, org.openide.util.Utilities.replaceString (currentValue.userCode, "\n", "\\n"));
           break;
    }

    return el;
  }

}

/*
 * Log
 */
