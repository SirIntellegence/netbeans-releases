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

package org.netbeans.modules.web.core;

import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.Iterator;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.text.AbstractDocument;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.EditorKit;
import javax.swing.text.View;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ui.OpenProjects;
import org.netbeans.editor.EditorUI;
import org.netbeans.editor.Utilities;
import org.netbeans.editor.Registry;
import org.netbeans.editor.view.spi.EstimatedSpanView;
import org.netbeans.editor.view.spi.LockView;
import org.netbeans.modules.web.spi.webmodule.WebModuleImplementation;
import org.openide.ErrorManager;
import org.openide.util.RequestProcessor;

/**
 * "Warm-up" task for editor. Executed after IDE startup, it should
 * pre-initialize some suitable parts of the module to improve first time usage
 * experience - which might suffer from long response time due to class loading
 * and various initialization.
 * See {@link org.netbeans.core.AfterStartWarmUp} for details about how the task is run.
 *
 * @author  Tomas Pavek, Marek Fukala
 */

public class JspEditorWarmUpTask implements Runnable{
    
    /**
     * Number of lines that an artificial document
     * for view hierarchy code optimization will have.
     * <br>
     * The default threshold for hotspot method compilation
     * is 1500 invocations.
     */
    private static final int ARTIFICIAL_DOCUMENT_LINE_COUNT = 1510;

    /**
     * Number of times a long document is assigned to the editor pane
     * which causes the view hierarchy for it to be (re)built.
     */
    private static final int VIEW_HIERARCHY_CREATION_COUNT = 1;
    
    /**
     * Width of buffered image area.
     */
    private static final int IMAGE_WIDTH = 600;
    
    /**
     * Height of buffered image area.
     */
    private static final int IMAGE_HEIGHT = 400;
    
    /**
     * Number of paints to be simulated.
     */
    private static final int PAINT_COUNT = 1;
    

    private static final boolean debug
        = Boolean.getBoolean("netbeans.debug.editor.warmup"); // NOI18N
//            =true;
    
    private static final int STATUS_INIT = 0;
    private static final int STATUS_CREATE_PANE = 1;
    private static final int STATUS_CREATE_DOCUMENTS = 2;
    private static final int STATUS_SWITCH_DOCUMENTS = 3;
    private static final int STATUS_TRAVERSE_VIEWS = 4;
    private static final int STATUS_RENDER_FRAME = 5;
    
    private int status = STATUS_INIT;

    private JEditorPane pane;
    private JFrame frame;
    private Document emptyDoc;
    private Document longDoc;
    private Graphics bGraphics;
    
    private EditorKit jspKit;

    private long startTime;
    
    //signals whether the warmuptask has already been performed
    public static boolean ALREADY_RUN = false;
    
    public void run() {
        switch (status) {
            case STATUS_INIT:
                //test whether a WebProject is opened
                if(!isWebProjectOpened()) return ;
        
                if (debug) {
                    startTime = System.currentTimeMillis();
                }
        
                // Init of JSPKit and JSPOptions
                jspKit = JEditorPane.createEditorKitForContentType("text/x-jsp"); //NOI18N
        
                //creating actions instances
                jspKit.getActions();
        

                // Start of a code block that tries to force hotspot to compile
                // the view hierarchy and related classes for faster performance
                if (debug) {
                    System.out.println("Kit instances initialized: " // NOI18N
                        + (System.currentTimeMillis()-startTime));
                    startTime = System.currentTimeMillis();
                }

                Iterator componentIterator = Registry.getComponentIterator();
                if (!componentIterator.hasNext()) { // no components opened yet
                    status = STATUS_CREATE_PANE;
                    SwingUtilities.invokeLater(this); // must run in AWT
                } // otherwise stop because editor pane(s) already opened (optimized)
                break;
                
            case STATUS_CREATE_PANE: // now create editor component and assign a kit to it
                assert SwingUtilities.isEventDispatchThread(); // This part must run in AWT

                pane = new JEditorPane();
                pane.setEditorKit(jspKit);

                // Obtain extended component (with editor's toolbar and scrollpane)
                EditorUI editorUI = Utilities.getEditorUI(pane);
                if (editorUI != null) {
                    // Make sure extended component necessary classes get loaded
                    editorUI.getExtComponent();
                }

                Registry.removeComponent(pane);

                status = STATUS_CREATE_DOCUMENTS;
                RequestProcessor.getDefault().post(this);
                break;
                
            case STATUS_CREATE_DOCUMENTS:

                // Have two documents - one empty and another one filled with many lines
                emptyDoc = jspKit.createDefaultDocument();
                longDoc = pane.getDocument();

                try {
                    // Fill the document with data.
                    // Number of lines is more important here than number of columns in a line
                    // Do one big insert instead of many small inserts
                    StringBuffer sb = new StringBuffer();
                    for (int i = ARTIFICIAL_DOCUMENT_LINE_COUNT; i > 0; i--) {
                        sb.append("hello"); // NOI18N
                    }
                    longDoc.insertString(0, sb.toString(), null);

                    status = STATUS_SWITCH_DOCUMENTS;
                    SwingUtilities.invokeLater(this);

                } catch (BadLocationException e) {
                    ErrorManager.getDefault().notify(e);
                }
                break;

            case STATUS_SWITCH_DOCUMENTS:
                // Switch between empty doc and long several times
                // to force view hierarchy creation
                for (int i = 0; i < VIEW_HIERARCHY_CREATION_COUNT; i++) {
                    pane.setDocument(emptyDoc);

                    // Set long doc - causes view hierarchy to be rebuilt
                    pane.setDocument(longDoc);
                }
                
                status = STATUS_TRAVERSE_VIEWS;
                RequestProcessor.getDefault().post(this);
                break;
                
            case STATUS_TRAVERSE_VIEWS:
                try {
                    // Create buffered image for painting simulation
                    BufferedImage bImage = new BufferedImage(
                        IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
                    bGraphics = bImage.getGraphics();
                    bGraphics.setClip(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);

                    // Do view-related operations
                    AbstractDocument doc = (AbstractDocument)pane.getDocument();
                    doc.readLock();
                    try {
                        final View rootView = Utilities.getDocumentView(pane);
                        LockView lockView = LockView.get(rootView);
                        lockView.lock();
                        try {
                            int viewCount = rootView.getViewCount();

                            // Force switch the line views from estimated spans to exact measurements
                            Runnable resetChildrenEstimatedSpans = new Runnable() {
                                public void run() {
                                    int cnt = rootView.getViewCount();                            
                                    for (int j = 0; j < cnt; j++) {
                                        View v = rootView.getView(j);
                                        if (v instanceof EstimatedSpanView) {
                                            ((EstimatedSpanView)v).setEstimatedSpan(false);
                                        }
                                    }
                                }
                            };
                            if (rootView instanceof org.netbeans.lib.editor.view.GapDocumentView) {
                                ((org.netbeans.lib.editor.view.GapDocumentView)rootView).
                                    renderWithUpdateLayout(resetChildrenEstimatedSpans);
                            } else { // not specialized instance => run normally
                                resetChildrenEstimatedSpans.run();
                            }

                            // Get child allocation for each line
                            for (int j = 0; j < viewCount; j++) {
                                Rectangle alloc = new Rectangle(0, 0,
                                    (int)rootView.getPreferredSpan(View.X_AXIS),
                                    (int)rootView.getPreferredSpan(View.Y_AXIS)
                                );
                                rootView.getChildAllocation(j, alloc);
                            }

                            // Test modelToView and viewToModel
                            if (false) { // Disabled because of #
                                float rootViewYSpan = rootView.getPreferredSpan(View.Y_AXIS);
                                float maybeLineSpan = rootViewYSpan / viewCount;
                                Point point = new Point();
                                point.x = 5; // likely somewhere inside the first char on the line
                                for (int j = 0; j < viewCount; j++) {
                                    pane.modelToView(rootView.getView(j).getStartOffset());

                                    point.y = (int)(j * maybeLineSpan);
                                    int pos = pane.viewToModel(point);
                                }
                            }

                            int rootViewWidth = (int)rootView.getPreferredSpan(View.X_AXIS);
                            int rootViewHeight = (int)rootView.getPreferredSpan(View.Y_AXIS);
                            Rectangle alloc = new Rectangle(0, 0, rootViewWidth, rootViewHeight);

                            // Paint into buffered image
                            for (int i = PAINT_COUNT - 1; i >= 0; i--) {
                                rootView.paint(bGraphics, alloc);
                            }

                        } finally {
                            lockView.unlock();
                        }
                    } finally {
                        doc.readUnlock();
                    }
                } catch (BadLocationException e) {
                    ErrorManager.getDefault().notify(e);
                }
                    
                status = STATUS_RENDER_FRAME;
                SwingUtilities.invokeLater(this);
                break;

            case STATUS_RENDER_FRAME:
                frame = new JFrame();
                EditorUI ui = Utilities.getEditorUI(pane);
                JComponent mainComp = null;
                if (ui != null) {
                    mainComp = ui.getExtComponent();
                }
                if (mainComp == null) {
                    mainComp = new javax.swing.JScrollPane(pane);
                }
                frame.getContentPane().add(mainComp);
                frame.pack();
                frame.paint(bGraphics);
                frame.getContentPane().removeAll();
                frame.dispose();
                pane.setEditorKit(null);

                // Candidates Annotations.getLineAnnotations()

                if (debug) {
                    System.out.println("View hierarchy initialized: " // NOI18N
                        + (System.currentTimeMillis()-startTime));
                    startTime = System.currentTimeMillis();
                }
                break;
            default:
                throw new IllegalStateException();
        }
    }
    
    private static boolean isWebProjectOpened() {
        //init jasper for all opened projects
        Project[] openedProjects = OpenProjects.getDefault().getOpenProjects();
        for (int i = 0; i < openedProjects.length; i++) {
            WebModuleImplementation wmImpl = (WebModuleImplementation)openedProjects[i].getLookup().lookup(WebModuleImplementation.class);
            if(wmImpl != null)  return true;
        }
        return false;
    }
    
}
