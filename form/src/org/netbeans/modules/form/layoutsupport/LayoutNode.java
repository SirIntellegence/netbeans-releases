/*
 *                 Sun Public License Notice
 * 
 * The contents of this file are subject to the Sun Public License
 * Version 1.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://www.sun.com/
 * 
 * The Original Code is NetBeans. The Initial Developer of the Original
 * Code is Sun Microsystems, Inc. Portions Copyright 1997-2000 Sun
 * Microsystems, Inc. All Rights Reserved.
 */

package org.netbeans.modules.form.layoutsupport;

import java.awt.*;
import java.util.*;
import java.beans.*;
import java.security.*;

import org.openide.ErrorManager;
import org.openide.nodes.*;
import org.openide.util.actions.SystemAction;

import org.netbeans.modules.form.*;
import org.netbeans.modules.form.actions.SelectLayoutAction;

/**
 * @author Tomas Pavek
 */

public class LayoutNode extends FormNode
                        implements RADComponentCookie, FormPropertyCookie
{
    private LayoutSupportManager layoutSupport;
    
    public LayoutNode(RADVisualContainer cont) {
        super(Children.LEAF, cont.getFormModel());
        layoutSupport = cont.getLayoutSupport();
        setName(layoutSupport.getDisplayName());
        cont.setLayoutNodeReference(this);
    }

    // RADComponentCookie
    public RADComponent getRADComponent() {
        return layoutSupport.getMetaContainer();
    }

    // FormPropertyCookie
    public FormProperty getProperty(String name) {
        Node.Property prop = layoutSupport.getLayoutProperty(name);
        return prop instanceof FormProperty ? (FormProperty) prop : null;
    }

    public void fireLayoutPropertiesChange() {
        firePropertyChange(null, null, null);
    }

    public void fireLayoutPropertySetsChange() {
        firePropertySetsChange(null, null);
    }

    public Image getIcon(int iconType) {
        return layoutSupport.getIcon(iconType);
    }

    public Node.PropertySet[] getPropertySets() {
        return layoutSupport.getPropertySets();
    }

    protected SystemAction[] createActions() {
        ArrayList actions = new ArrayList(10);

        if (!layoutSupport.getMetaContainer().isReadOnly()) {
            actions.add(SystemAction.get(SelectLayoutAction.class));
            actions.add(null);
        }

        SystemAction[] superActions = super.createActions();
        for (int i=0; i < superActions.length; i++)
            actions.add(superActions[i]);

        SystemAction[] array = new SystemAction[actions.size()];
        actions.toArray(array);
        return array;
    }

    public boolean hasCustomizer() {
        return !layoutSupport.getMetaContainer().isReadOnly()
               && layoutSupport.getCustomizerClass() != null;
    }

    public Component getCustomizer() {
        Class customizerClass = layoutSupport.getCustomizerClass();
        if (customizerClass == null)
            return null;

        Component supportCustomizer = layoutSupport.getSupportCustomizer();
        if (supportCustomizer != null)
            return supportCustomizer;

        // create bean customizer for layout manager
        Object customizerObject;
        try {
            customizerObject = customizerClass.newInstance();
        }
        catch (InstantiationException e) {
            ErrorManager.getDefault().notify(ErrorManager.WARNING, e);
            return null;
        }
        catch (IllegalAccessException e) {
            ErrorManager.getDefault().notify(ErrorManager.WARNING, e);
            return null;
        }

        if (customizerObject instanceof Component 
            && customizerObject instanceof Customizer)
        {
            Customizer customizer = (Customizer) customizerObject;
            customizer.setObject(
                layoutSupport.getPrimaryContainerDelegate().getLayout());

            customizer.addPropertyChangeListener(new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent evt) {
                    Node.Property[] properties;
                    if (evt.getPropertyName() != null) {
                        Node.Property changedProperty =
                            layoutSupport.getLayoutProperty(evt.getPropertyName());
                        if (changedProperty != null)
                            properties = new Node.Property[] { changedProperty };
                        else return; // non-existing property?
                    }
                    else {
                        properties = layoutSupport.getAllProperties();
                        evt = null;
                    }

                    updatePropertiesFromCustomizer(properties, evt);
                }
            });

            return (Component) customizer;
        }

        return null;
    }

    private void updatePropertiesFromCustomizer(
                     final Node.Property[] properties,
                     final PropertyChangeEvent evt)
    {
        // just for sure we run this as privileged to avoid security problems,
        // the property change can be fired from untrusted bean customizer code
        AccessController.doPrivileged(new PrivilegedAction() {
            public Object run() {
                try {
                    PropertyChangeEvent ev = evt == null ? null :
                         new PropertyChangeEvent(
                               layoutSupport.getLayoutDelegate(),
                               evt.getPropertyName(),
                               evt.getOldValue(), evt.getNewValue());

                    for (int i=0; i < properties.length; i++) {
                        if (properties[i] instanceof FormProperty)
                            ((FormProperty)properties[i]).reinstateProperty();
                        // [there should be something for Node.Property too]

                        if (ev != null)
                            layoutSupport.containerLayoutChanged(ev);
                    }

                    if (ev == null) // anonymous property changed
                        layoutSupport.containerLayoutChanged(null);
                        // [but this probably won't do anything...]
                }
                catch (PropertyVetoException ex) {
                    // the change is not accepted, but what can we do here?
                    // java.beans.Customizer has no veto capabilities
                }
                catch (Exception ex) {
                    ErrorManager.getDefault().notify(ex);
                }

                return null;
            }
        });
    }

/*    public HelpCtx getHelpCtx() {
        Class layoutClass = layoutSupport.getLayoutClass();
        String helpID = null;
        if (layoutClass != null) {
            if (layoutClass == BorderLayout.class)
                helpID = "gui.layouts.managers.border"; // NOI18N
            else if (layoutClass == FlowLayout.class)
                helpID = "gui.layouts.managers.flow"; // NOI18N
            else if (layoutClass == GridLayout.class)
                helpID = "gui.layouts.managers.grid"; // NOI18N
            else if (layoutClass == GridBagLayout.class)
                helpID = "gui.layouts.managers.gridbag"; // NOI18N
            else if (layoutClass == CardLayout.class)
                helpID = "gui.layouts.managers.card"; // NOI18N
            else if (layoutClass == javax.swing.BoxLayout.class)
                helpID = "gui.layouts.managers.box"; // NOI18N
            else if (layoutClass == org.netbeans.lib.awtextra.AbsoluteLayout.class)
                helpID = "gui.layouts.managers.absolute"; // NOI18N
        }
        if (helpID != null)
            return new HelpCtx(helpID);
        return super.getHelpCtx();
    } */
}
