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


package org.netbeans.modules.j2me.cdc.project.ui.mbm;


import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.beans.PropertyEditor;
import java.beans.PropertyEditorSupport;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.openide.actions.OpenAction;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileStateInvalidException;
import org.openide.loaders.*;
import org.openide.nodes.*;
import org.openide.ErrorManager;
import org.openide.filesystems.FileUtil;
import org.openide.util.actions.SystemAction;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;


/** 
 * Object that represents one file containing an image.
 * @author Petr Hamernik, Jaroslav Tulach, Ian Formanek, Michael Wever
 * @author  Marian Petras
 */
public class MBMDataObject extends MultiDataObject implements CookieSet.Factory {
    
    /** Generated serialized version UID. */
    static final long serialVersionUID = -6035788991669336965L;

    /** Base for image resource. */
    private static final String IMAGE_ICON_BASE = "/org/netbeans/modules/j2me/cdc/project/ui/mbm/imageObject.png"; // NOI18N
    
    /** Constructor.
     * @param pf primary file object for this data object
     * @param loader the data loader creating it
     * @exception DataObjectExistsException if there was already a data object for it 
     */
    public MBMDataObject(FileObject pf, MultiFileLoader loader) throws DataObjectExistsException {
        super(pf, loader);
    }


    /** Implements <code>CookieSet.Factory</code> interface. */
    public Node.Cookie createCookie(Class clazz) {
        return null;
    }
    
    /** Help context for this object.
     * @return the help context
     */
    public HelpCtx getHelpCtx() {
        return HelpCtx.DEFAULT_HELP;
    }

    
    /** Create a node to represent the image. Overrides superclass method.
     * @return node delegate */
    protected Node createNodeDelegate () {
        return new ImageNode(this);
    }
    
    
    /** Node representing <code>ImageDataObject</code>. */
    private static final class ImageNode extends DataNode {
        /** Constructs image node. */
        public ImageNode(MBMDataObject obj) {
            super(obj, Children.LEAF);
            setIconBaseWithExtension(IMAGE_ICON_BASE);
        }
        
        /** Creates property sheet. Ovrrides superclass method. */
        protected Sheet createSheet() {
            Sheet s = super.createSheet();
            Sheet.Set ss = s.get(Sheet.PROPERTIES);
            if (ss == null) {
                ss = Sheet.createPropertiesSet();
                s.put(ss);
            }
            ss.put(new ThumbnailProperty(getDataObject()));
            return s;
        }
        

        /** Property representing for thumbanil property in the sheet. */
        private static final class ThumbnailProperty extends PropertySupport.ReadOnly {
            /** (Image) data object associated with. */
            private final DataObject obj;
            
            /** Constructs property. */
            public ThumbnailProperty(DataObject obj) {
                super("thumbnail", Icon.class, // NOI18N
                    NbBundle.getMessage(MBMDataObject.class, "PROP_Thumbnail"),
                    NbBundle.getMessage(MBMDataObject.class, "HINT_Thumbnail"));
                this.obj = obj;
            }
            
            /** Gets value of property. Overrides superclass method. */
            public Object getValue() throws InvocationTargetException {
                try {
                    return new ImageIcon(obj.getPrimaryFile().getURL());
                } catch (FileStateInvalidException fsie) {
                    throw new InvocationTargetException(fsie);
                }
            }
            
            /** Gets property editor. */
            public PropertyEditor getPropertyEditor() {
                return new ThumbnailPropertyEditor();
            }
            
            
            /** Property editor for thumbnail property. */
            private final class ThumbnailPropertyEditor extends PropertyEditorSupport {
                /** Overrides superclass method.
                 * @return <code>true</code> */
                public boolean isPaintable() {
                    return true;
                }
                
                /** Patins thumbanil of the image. Overrides superclass method. */
                public void paintValue(Graphics g, Rectangle r) {
                    ImageIcon icon = null;
                    
                    try {
                        icon = (ImageIcon)ThumbnailProperty.this.getValue();
                    } catch(InvocationTargetException ioe) {
                        if(Boolean.getBoolean("netbeans.debug.exceptions")) { // NOI18N
                            ErrorManager.getDefault().notify(ioe);
                        }
                    }
                    
                    if(icon != null) {
                        int iconWidth = icon.getIconWidth();
                        int iconHeight = icon.getIconHeight();
                        

                        // Shrink image if necessary.
                        double scale = (double)iconWidth / iconHeight;
                        
                        if(iconWidth > r.width) {
                            iconWidth = r.width;
                            iconHeight = (int) (iconWidth / scale);
                        }

                        if(iconHeight > r.height) {
                            iconHeight = r.height;
                            iconWidth = (int) (iconHeight * scale);
                        }
                        
                        // Try to center it if it fits, else paint as much as possible.
                        int x;
                        if(iconWidth < r.x) {
                            x = (r.x - iconWidth) / 2;
                        } else {
                            x = 5; // XXX Indent.
                        }
                        
                        int y;
                        if(iconHeight < r.y) {
                            y = (r.y - iconHeight) / 2;
                        } else {
                            y = 0;
                        }
                        
                        Graphics g2 = g.create(r.x, r.y, r.width, r.height);
                        g.drawImage(icon.getImage(), x, y, iconWidth, iconHeight, null);
                    }
                }

                /** Overrides superclass method.
                 * @return <code>null</code> */
                public String getAsText() {
                    return null;
                }
            } // End of class ThumbnailPropertyEditor.
        } // End of class ThumbnailProperty.
    } // End of class ImageNode.

}
