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

package com.netbeans.developer.impl.actions;

import java.awt.Dimension;
import java.util.Iterator;
import java.beans.BeanInfo;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.border.CompoundBorder;

import org.openide.loaders.DataObject;
import org.openide.TopManager;
import org.openide.NotifyDescriptor;
import org.openide.DialogDescriptor;
import org.openide.util.HelpCtx;
import org.openide.util.actions.CallableSystemAction;
import org.openide.util.NbBundle;
import org.openide.nodes.Node;
import org.openide.cookies.SaveCookie;

/** SystemExit action.
* @author   Ian Formanek
* @version  0.14, Feb 13, 1998
*/
public class SystemExit extends CallableSystemAction {
  /** generated Serialized Version UID */
  static final long serialVersionUID = 5198683109749927396L;

  private static JButton[] exitOptions;
  private static Object[] secondaryExitOptions;
  private static java.awt.Dialog exitDialog;

 /** Human presentable name of the action. This should be
  * presented as an item in a menu.
  * @return the name of the action
  */
  public String getName() {
    return NbBundle.getBundle(SystemExit.class).getString("Exit");
  }

  /** Help context where to find more about the action.
  * @return the help context for this action
  */
  public HelpCtx getHelpCtx() {
    return new HelpCtx (SystemExit.class);
  }

  /** Name of this action's icon.
  * @return name of the action's icon
  */
  protected String iconResource () {
    return "/com/netbeans/developer/impl/resources/actions/exit.gif";
  }

  public void performAction() {
    java.util.Set set = org.openide.loaders.DataObject.getRegistry ().getModifiedSet ();
    if (!set.isEmpty()) {
      if (exitDialog == null) {
        exitOptions = new JButton[] {
          new JButton (NbBundle.getBundle(SystemExit.class).getString("CTL_Save")),
          new JButton (NbBundle.getBundle(SystemExit.class).getString("CTL_SaveAll")),
          new JButton (NbBundle.getBundle(SystemExit.class).getString("CTL_DiscardAll")),
        };
        secondaryExitOptions = new Object[] {
          new JButton (NbBundle.getBundle(SystemExit.class).getString("CTL_Cancel")),
        };
        ExitDlgComponent exitComponent = new ExitDlgComponent ();
        DialogDescriptor exitDlgDescriptor = new DialogDescriptor (
            exitComponent,                                                   // inside component
            NbBundle.getBundle(SystemExit.class).getString("CTL_ExitTitle"), // title
            true,                                                            // modal
            exitOptions,                                                     // options
            secondaryExitOptions [0],                                        // initial value
            DialogDescriptor.RIGHT_ALIGN,                                    // option align
            new org.openide.util.HelpCtx (SystemExit.class.getName () + ".dialog"), // HelpCtx
            exitComponent                                                    // Action Listener
        );
        exitDlgDescriptor.setAdditionalOptions (secondaryExitOptions);
        exitDialog = TopManager.getDefault ().createDialog (exitDlgDescriptor);
      }
      exitDialog.show ();
    } else {
      org.openide.TopManager.getDefault().exit();
    }
  }


  /** Dialog which is shown when any file is not saved. */
  private static class ExitDlgComponent extends JPanel implements java.awt.event.ActionListener {
    JList list;
    DefaultListModel listModel;

    /** Constructs new dlg */
    public ExitDlgComponent () {
      setLayout (new java.awt.BorderLayout ());

      listModel = new DefaultListModel();
      Iterator iter = DataObject.getRegistry ().getModifiedSet ().iterator();
      while (iter.hasNext()) {
        DataObject obj = (DataObject) iter.next();
        listModel.addElement(obj);
      }

      list = new JList(listModel);
      list.setBorder(new EmptyBorder(2, 2, 2, 2));
      list.addListSelectionListener (new javax.swing.event.ListSelectionListener () {
          public void valueChanged (javax.swing.event.ListSelectionEvent evt) {
            updateSaveButton ();
          }
        }
      );
      updateSaveButton ();
      JScrollPane scroll = new JScrollPane (list);
      scroll.setBorder (new CompoundBorder (new EmptyBorder (5, 5, 5, 5), scroll.getBorder ()));
      add(scroll, java.awt.BorderLayout.CENTER);
      list.setCellRenderer(new ExitDlgListCellRenderer());
    }

    private void updateSaveButton () {
      exitOptions [0].setEnabled (list.getSelectedIndex () != -1);
    }

    /** @return preffered size */
    public Dimension getPreferredSize() {
      Dimension prev = super.getPreferredSize();
      return new Dimension(Math.max(300, prev.width), Math.max(150, prev.height));
    }

    /** This method is called when is any of buttons pressed */
    public void actionPerformed (java.awt.event.ActionEvent evt) {
      if (exitOptions[0].equals (evt.getSource ())) {
        save(false);
      } else if (exitOptions[1].equals (evt.getSource ())) {
        save(true);
      } else if (exitOptions[2].equals (evt.getSource ())) {
        theEnd();
      } else if (secondaryExitOptions[0].equals (evt.getSource ())) {
        exitDialog.setVisible (false);
      }
    }

    /** Save the files from the listbox
    * @param all true- all files, false - just selected
    */
    private void save(boolean all) {
      if (all) {
        SaveCookie sc = null;
        for (int i = listModel.size() - 1; i >= 0; i--) {
          try {
            DataObject obj = (DataObject) listModel.getElementAt(i);
            sc = (SaveCookie)obj.getCookie(SaveCookie.class);
            if (sc != null) sc.save();
            listModel.removeElement(obj);
          }
          catch (java.io.IOException e) {
            saveExc(e);
          }
        }
      }
      else {
        Object[] array = list.getSelectedValues();
        SaveCookie sc = null;
        for (int i = 0; i < array.length; i++) {
          try {
            sc = (SaveCookie)
                 (((DataObject)array[i]).getCookie(SaveCookie.class));
            if (sc != null) sc.save();
            listModel.removeElement(array[i]);
          }
          catch (java.io.IOException e) {
            saveExc(e);
          }
        }
      }
      if (listModel.isEmpty()) {
        theEnd();
      } 
    }

    /** Exit the IDE */
    private void theEnd() {
      exitDialog.setVisible (false);
      exitDialog.dispose();
      TopManager.getDefault().exit();
    }

    /** Notification about the save exception */
    private void saveExc(Exception e) {
      TopManager.getDefault().notify(
        new NotifyDescriptor.Exception(e,
                                       NbBundle.getBundle(SystemExit.class).getString("EXC_Save"))
      );
    }
  }

  /** Renderer used in list box of exit dialog */
  private static class ExitDlgListCellRenderer extends JLabel implements ListCellRenderer {
    /** generated Serialized Version UID */
    static final long serialVersionUID = 1877692790854373689L;

    protected static Border hasFocusBorder;
    protected static Border noFocusBorder;

    public ExitDlgListCellRenderer() {
      setOpaque(true);
      setBorder(noFocusBorder);
      hasFocusBorder = new LineBorder(UIManager.getColor("List.focusCellHighlight"));
      noFocusBorder = new EmptyBorder(1, 1, 1, 1);
    }

    public java.awt.Component getListCellRendererComponent(JList list,
                                                           Object value,            // value to display
                                                           int index,               // cell index
                                                           boolean isSelected,      // is the cell selected
                                                           boolean cellHasFocus)    // the list and the cell have the focus
    {
      if (!(value instanceof DataObject)) return this;

      Node node = ((DataObject)value).getNodeDelegate();

      ImageIcon icon = new ImageIcon(node.getIcon(BeanInfo.ICON_COLOR_16x16));
      super.setIcon(icon);

      setText(node.getDisplayName());
      if (isSelected){
        setBackground(UIManager.getColor("List.selectionBackground"));
        setForeground(UIManager.getColor("List.selectionForeground"));
      }
      else {
        setBackground(list.getBackground());
        setForeground(list.getForeground());
      }

      setBorder(cellHasFocus ? hasFocusBorder : noFocusBorder);

      return this;
    }
  }
}

/*
 * Log
 *  13   Gandalf   1.12        6/24/99  Jesse Glick     Gosh-honest HelpID's.
 *  12   Gandalf   1.11        6/22/99  Ian Formanek    employed DEFAULT_HELP
 *  11   Gandalf   1.10        6/8/99   Ian Formanek    ---- Package Change To 
 *       org.openide ----
 *  10   Gandalf   1.9         5/26/99  Ian Formanek    Actions cleanup
 *  9    Gandalf   1.8         3/29/99  Ian Formanek    CoronaDialog -> 
 *       DialogDescriptor
 *  8    Gandalf   1.7         3/9/99   Jaroslav Tulach ButtonBar  
 *  7    Gandalf   1.6         3/5/99   Ales Novak      
 *  6    Gandalf   1.5         1/20/99  Jaroslav Tulach 
 *  5    Gandalf   1.4         1/14/99  David Simonek   
 *  4    Gandalf   1.3         1/7/99   Ian Formanek    fixed resource names
 *  3    Gandalf   1.2         1/6/99   Ian Formanek    Reflecting change in 
 *       datasystem package
 *  2    Gandalf   1.1         1/6/99   Ian Formanek    Reflecting changes in 
 *       location of package "awt"
 *  1    Gandalf   1.0         1/5/99   Ian Formanek    
 * $
 * Beta Change History:
 *  0    Tuborg    0.11        --/--/98 Jan Formanek    extends CallableSystemAction because the actions hierarchy has changed
 *  0    Tuborg    0.12        --/--/98 Jan Jancura     Icon ...
 *  0    Tuborg    0.13        --/--/98 Jan Formanek    action name localization
 */
