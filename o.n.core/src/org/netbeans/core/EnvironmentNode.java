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

package com.netbeans.developer.impl;

import java.awt.Image;
import java.awt.Toolkit;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ResourceBundle;
import java.util.Vector;

import com.netbeans.ide.*;
import com.netbeans.ide.loaders.*;
import com.netbeans.ide.options.*;
import com.netbeans.ide.actions.PropertiesAction;
import com.netbeans.ide.actions.RenameAction;
import com.netbeans.ide.util.HelpCtx;
import com.netbeans.ide.util.NotImplementedException;
import com.netbeans.ide.util.actions.*;
import com.netbeans.ide.nodes.*;
import com.netbeans.ide.util.NbBundle;
import com.netbeans.developer.impl.actions.*;

/** This object represents environment settings in the Corona system.
* This class is final only for performance purposes.
* Can be unfinaled if desired.
*
* @author Petr Hamernik, Dafe Simonek
*/
public final class EnvironmentNode extends AbstractNode {
  /** generated Serialized Version UID */
  static final long serialVersionUID = 4782447107972624693L;
  /** icon base for icons of this node */
  private static final String EN_ICON_BASE =
    "/com.netbeans.developer.impl.resources/environment";
  /** Array of actions in popup menu of this node */
  private static SystemAction[] staticActions;

  /** used during deserialization */
  private transient Node[] ret;

  private Node paletteContextNode;

  /** Constructor */
  public EnvironmentNode () {
    super (new Children.Array());
    initialize();
  }

  public boolean canRename () {
    return true;
  }

  /** Does all initialization */
  private void initialize () {
    initializeChildren();
    setDisplayName(NbBundle.getBundle(this).
                   getString("CTL_Environment_name"));
    setIconBase(EN_ICON_BASE);
    createProperties();
  }

  /** Initialize children of this node - adds 6 currently defined subnodes. */
  private void initializeChildren () {
  /*
    ret = new Node[8];
    ret[0] = new FSPoolNode(this);
    ret[1] = new MainWindowNode(this);
    ret[2] = CoronaTopManager.getDesktopPoolContextNode(this);
    ret[3] = CoronaTopManager.getShortcutNode (this);
    if (paletteContextNode != null) // deserialized
      ret[4] = paletteContextNode;
    else {
      ret[4] = new SerializableFilterNode(PaletteContext.getPaletteContext(), this);
      paletteContextNode = ret[4];
    }
    ret[5] = com.netbeans.developer.modules.debugger.JavaDebuggerNode.getDebuggerNode(this);
    ret[6] = com.netbeans.developer.impl.execution.ExecutionNode.getExecutionNode(this);
    ret[7] = com.netbeans.developer.defaults.Default.getDefaultActions(this);

    getChildren().add(ret);
    */
  }

  /** Method that prepares properties. Called from initialize.
  */
  protected void createProperties () {
    final ResourceBundle bundle = NbBundle.getBundle(this);
    // default sheet with "properties" property set
    Sheet sheet = Sheet.createDefault();
    sheet.get(Sheet.PROPERTIES).put(
      new PropertySupport.ReadWrite (
        Node.this.PROP_DISPLAY_NAME,
        String.class,
        bundle.getString("PROP_Environment_name"),
        bundle.getString("HINT_Environment_name")
      ) {
        public Object getValue() {
          return EnvironmentNode.this.getName();
        }
        public void setValue(Object val) {
          if (! (val instanceof String)) return;
          setName((String) val);
        }
      }
    );
    // and set new sheet
    setSheet(sheet);
  }

  /** renames this node */
  /*public void rename(String name) {
    String old = getDisplayName();
    setDisplayName(name);
    firePropertyChange(Node.this.PROP_DISPLAY_NAME, old, name);
  }*/

  /** Getter for set of actions that should be present in the
  * popup menu of this node. This set is used in construction of
  * menu returned from getContextMenu and specially when a menu for
  * more nodes is constructed.
  *
  * @return array of system actions that should be in popup menu
  */
  public SystemAction[] getActions () {
    if (staticActions == null)
      staticActions = new SystemAction[] {
        SystemAction.get(RenameAction.class),
        null,
        SystemAction.get(PropertiesAction.class)
      };
    return staticActions;
  }

  /** serializes the class */
  /*private void writeObject(ObjectOutputStream os)
  throws IOException {
    os.defaultWriteObject(); // outer ref
    os.writeObject(getDisplayName());
  }*/

  /** deserializes the class */
  /*private void readObject(ObjectInputStream is)
  throws IOException, ClassNotFoundException {
    is.defaultReadObject(); // outer ref
    setDisplayName((String)is.readObject());
    is.registerValidation(new java.io.ObjectInputValidation() {
      public void validateObject() {
        initialize();
        thisNodeChange();
      }
    }, 0);
  }*/
}

/*
 * Log
 *  2    Gandalf   1.1         1/6/99   Jaroslav Tulach ide.* extended to 
 *       ide.loaders.*
 *  1    Gandalf   1.0         1/5/99   Ian Formanek    
 * $
 * Beta Change History:
 *  0    Tuborg    0.12        --/--/98 Jan Formanek    Shortcuts and desktops moved here from the MainWindowNode
 */
