/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2009 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2007 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */

package org.netbeans.modules.diff.builtin.visualizer.editable;

import java.awt.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.util.*;
import java.util.List;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.PreferenceChangeEvent;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import javax.swing.*;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.AncestorEvent;
import javax.swing.plaf.TextUI;
import javax.swing.text.*;
import org.netbeans.api.editor.fold.FoldHierarchy;
import org.netbeans.api.editor.fold.FoldUtilities;
import org.netbeans.api.editor.fold.FoldHierarchyListener;
import org.netbeans.api.editor.fold.FoldHierarchyEvent;
import org.netbeans.modules.diff.DiffModuleConfig;
import org.netbeans.modules.editor.errorstripe.privatespi.MarkProvider;
import org.netbeans.modules.editor.errorstripe.privatespi.Mark;

import org.openide.util.RequestProcessor;
import org.openide.util.NbBundle;
import org.openide.ErrorManager;
import org.openide.nodes.CookieSet;
import org.openide.awt.UndoRedo;
import org.openide.filesystems.FileObject;
import org.openide.text.CloneableEditorSupport;
import org.openide.cookies.SaveCookie;
import org.openide.cookies.EditorCookie;
import org.openide.loaders.DataObject;
import org.openide.loaders.MultiDataObject;
import org.netbeans.api.diff.Difference;
import org.netbeans.api.diff.StreamSource;
import org.netbeans.api.diff.DiffView;
import org.netbeans.api.diff.DiffController;
import org.netbeans.api.editor.mimelookup.MimeLookup;
import org.netbeans.api.editor.settings.FontColorNames;
import org.netbeans.api.editor.settings.FontColorSettings;
import org.netbeans.editor.BaseTextUI;
import org.netbeans.spi.diff.DiffProvider;
import org.netbeans.spi.diff.DiffControllerImpl;
import org.netbeans.editor.EditorUI;
import org.netbeans.lib.editor.util.swing.DocumentUtilities;
import org.openide.text.NbDocument;
import org.openide.util.Lookup;

/**
 * Panel that shows differences between two files. The code here was originally distributed among DiffPanel and
 * DiffComponent classes.
 * 
 * @author Maros Sandor
 */
public class EditableDiffView extends DiffControllerImpl implements DiffView, DocumentListener, AncestorListener, PropertyChangeListener, PreferenceChangeListener {

    private static final int INITIAL_DIVIDER_SIZE = 32;
    
    private Stroke boldStroke = new BasicStroke(3);
    
    // === Default Diff Colors ===========================================================
    private Color colorMissing;
    private Color colorAdded;
    private Color colorChanged;
    private Color colorLines   = Color.DARK_GRAY;
    private Color COLOR_READONLY_BG = new Color(255,200,200);

    private final Difference [] NO_DIFFERENCES = new Difference[0];
    
    /**
     * Left (first) half of the Diff view, contains the editor pane, actions bar and line numbers bar.
     */
    private DiffContentPanel jEditorPane1;

    /**
     * Right (second) half of the Diff view, contains the editor pane, actions bar and line numbers bar.
     */
    private DiffContentPanel jEditorPane2;

    private boolean secondSourceAvailable;
    private boolean firstSourceAvailable;
    private boolean firstSourceUnsupportedTextUI;
    private boolean secondSourceUnsupportedTextUI;
    private final boolean binaryDiff;
    
    private JViewport jViewport2;

    final JLabel fileLabel1 = new JLabel();
    final JLabel fileLabel2 = new JLabel();
    final JPanel filePanel1 = new JPanel();
    final JPanel filePanel2 = new JPanel();
    final JSplitPane jSplitPane1 = new JSplitPane();

    private int diffSerial;
    private Difference[] diffs = NO_DIFFERENCES;
   
    private boolean ignoredUpdateEvents;
    
    private int horizontalScroll1ChangedValue = -1;
    private int horizontalScroll2ChangedValue = -1;
    
    private RequestProcessor.Task   refreshDiffTask;
    private DiffViewManager manager;
    
    private boolean actionsEnabled;
    private DiffSplitPaneUI spui;
    
    private Document baseDocument;
    private Document modifiedDocument;
    
    /**
     * The right pane is editable IFF editableCookie is not null.
     */ 
    private EditorCookie.Observable editableCookie;
    private Document editableDocument;
    private UndoRedo.Manager editorUndoRedo;
    private EditableDiffMarkProvider diffMarkprovider;

    private boolean lineLocationAsked;

    public EditableDiffView(final StreamSource ss1, final StreamSource ss2) throws IOException {
        refreshDiffTask = RequestProcessor.getDefault().create(new RefreshDiffTask());
        initColors();
        String title1 = ss1.getTitle();
        if (title1 == null) title1 = NbBundle.getMessage(EditableDiffView.class, "CTL_DiffPanel_NoTitle"); // NOI18N
        String title2 = ss2.getTitle();
        if (title2 == null) title2 = NbBundle.getMessage(EditableDiffView.class, "CTL_DiffPanel_NoTitle"); // NOI18N
        String mimeType1 = ss1.getMIMEType();
        String mimeType2 = ss2.getMIMEType();
        if (mimeType1 == null) mimeType1 = mimeType2;
        if (mimeType2 == null) mimeType2 = mimeType1;
        binaryDiff = mimeType1 == null || mimeType2 == null || mimeType1.equals("application/octet-stream") || mimeType2.equals("application/octet-stream");        
        
        actionsEnabled = ss2.isEditable();
        diffMarkprovider = new EditableDiffMarkProvider();        
                        
        initComponents ();

        if (!binaryDiff) {
            jEditorPane2.getEditorPane().putClientProperty(DiffMarkProviderCreator.MARK_PROVIDER_KEY, diffMarkprovider);
        }
        jSplitPane1.setName(org.openide.util.NbBundle.getMessage(EditableDiffView.class, "DiffComponent.title", ss1.getName(), ss2.getName())); // NOI18N
        spui = new DiffSplitPaneUI(jSplitPane1);
        jSplitPane1.setUI(spui);
        jSplitPane1.setResizeWeight(0.5);
        jSplitPane1.setDividerSize(INITIAL_DIVIDER_SIZE);
        jSplitPane1.putClientProperty("PersistenceType", "Never"); // NOI18N
        jSplitPane1.getAccessibleContext().setAccessibleName(org.openide.util.NbBundle.getMessage(EditableDiffView.class, "ACS_DiffPanelA11yName"));  // NOI18N
        jSplitPane1.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(EditableDiffView.class, "ACS_DiffPanelA11yDesc"));  // NOI18N
        
        setSourceTitle(fileLabel1, title1);
        setSourceTitle(fileLabel2, title2);

        final String f1 = mimeType1;
        final String f2 = mimeType2;
        try {
            Runnable awtTask = new Runnable() {
                public void run() {
                    Color borderColor = UIManager.getColor("scrollpane_border"); // NOI18N
                    if (borderColor == null) borderColor = UIManager.getColor("controlShadow"); // NOI18N
                    jSplitPane1.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, borderColor));
                    
                    if (binaryDiff) {
                        adjustPreferredSizes();
                        return;
                    }
                    
                    jEditorPane1.getScrollPane().setBorder(null);
                    jEditorPane2.getScrollPane().setBorder(null);
                    jEditorPane1.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, borderColor));
                    jEditorPane2.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, borderColor));
                    
                    jEditorPane1.getEditorPane().setEditorKit(CloneableEditorSupport.getEditorKit(f1));
                    repairTextUI(jEditorPane1.getEditorPane());
                    jEditorPane2.getEditorPane().setEditorKit(CloneableEditorSupport.getEditorKit(f2));
                    repairTextUI(jEditorPane2.getEditorPane());
                    
                    try {
                        setSource1(ss1);
                        setSource2(ss2);
                    } catch (IOException ioex) {
                        Logger.getLogger(EditableDiffView.class.getName()).log(Level.INFO, "Diff source unavailable", ioex);
                    }
                    
                    if (!secondSourceAvailable) {
                        filePanel2.remove(jEditorPane2);
                        NoContentPanel ncp = new NoContentPanel(NbBundle.getMessage(EditableDiffView.class,
                                secondSourceUnsupportedTextUI ? "CTL_DiffPanel_UnsupportedTextUI" : "CTL_DiffPanel_NoContent")); // NOI18N
                        ncp.setPreferredSize(new Dimension(jEditorPane1.getPreferredSize().width, ncp.getPreferredSize().height));
                        filePanel2.add(ncp);
                        actionsEnabled = false;
                    }
                    if (!firstSourceAvailable) {
                        filePanel1.remove(jEditorPane1);
                        NoContentPanel ncp = new NoContentPanel(NbBundle.getMessage(EditableDiffView.class,
                                firstSourceUnsupportedTextUI ? "CTL_DiffPanel_UnsupportedTextUI" : "CTL_DiffPanel_NoContent")); // NOI18N
                        ncp.setPreferredSize(new Dimension(jEditorPane2.getPreferredSize().width, ncp.getPreferredSize().height));
                        filePanel1.add(ncp);
                        actionsEnabled = false;
                    }
                    adjustPreferredSizes();

                    JTextComponent leftEditor = jEditorPane1.getEditorPane();
                    JTextComponent rightEditor = jEditorPane2.getEditorPane();
                    if (rightEditor.isEditable()) {
                        setBackgroundColorForNonEditable(leftEditor, rightEditor);
                    }
                    if ((rightEditor.getBackground().getRGB() & 0xFFFFFF) == 0) {
                        colorLines   = Color.WHITE;
                    }
                }
            };
            if (SwingUtilities.isEventDispatchThread()) {
                awtTask.run();
            } else {
                 SwingUtilities.invokeAndWait(awtTask);
            }
        } catch (InterruptedException e) {
            Logger.getLogger(EditableDiffView.class.getName()).log(Level.SEVERE, ".colorLines:" + colorLines + ", .jviewPort2:" + jViewport2
                    + ", editableDocument:" + editableDocument + ", editableCookie:" + editableCookie + ", editorUndoRedo:" + editorUndoRedo, e);
        } catch (InvocationTargetException e) {
            Logger.getLogger(EditableDiffView.class.getName()).log(Level.SEVERE, ".colorLines:" + colorLines + ", .jviewPort2:" + jViewport2
                    + ", editableDocument:" + editableDocument + ", editableCookie:" + editableCookie + ", editorUndoRedo:" + editorUndoRedo, e);
        }

        if (binaryDiff) {
            return;
        }
        
        jSplitPane1.addAncestorListener(this);

        manager = new DiffViewManager(this);
        manager.init();
        refreshDiff(0);
    }

    private void setBackgroundColorForNonEditable(JTextComponent leftEditor,
                                                  JTextComponent rightEditor) {
        String mimeType = DocumentUtilities.getMimeType(leftEditor);
        if (mimeType == null) {
            mimeType = "text/plain";                                    //NOI18N
        }

        Color bgColor = null;

        Lookup lookup = MimeLookup.getLookup(mimeType);
        if (lookup != null) {
            FontColorSettings fontColorSettings = lookup.lookup(FontColorSettings.class);
            if (fontColorSettings != null) {
                AttributeSet attrSet = fontColorSettings.getFontColors(
                                          FontColorNames.GUARDED_COLORING);
                if (attrSet != null) {
                    Object bgColorObj = attrSet.getAttribute(StyleConstants.Background);
                    if (bgColorObj instanceof Color) {
                        bgColor = (Color) bgColorObj;
                    }
                }
            }
        }

        if (bgColor == null) {
            /* Fallback to the old routine: */
            int editableBgColor = rightEditor.getBackground().getRGB() & 0xFFFFFF;
            if ((editableBgColor == 0xFFFFFF)
                    && System.getProperty("netbeans.experimental.diff.ReadonlyBg") == null) { //NOI18N
                bgColor = COLOR_READONLY_BG;
            }
        }

        if (bgColor != null) {
            leftEditor.setBackground(bgColor);
        }
    }

    private void adjustPreferredSizes() {
        // Make sure split pane opens with divider in the center
        Dimension pf1 = fileLabel1.getPreferredSize();
        Dimension pf2 = fileLabel2.getPreferredSize();
        if (pf1.width > pf2.width) {
            fileLabel2.setPreferredSize(new Dimension(pf1.width, pf2.height));
        } else {
            fileLabel1.setPreferredSize(new Dimension(pf2.width, pf1.height));
        }
    }

    @Override
    public void setLocation(final DiffController.DiffPane pane, final DiffController.LocationType type, final int location) {
        manager.runWithSmartScrollingDisabled(new Runnable() {
            public void run() {
                if (type == DiffController.LocationType.DifferenceIndex) {
                    setDifferenceImpl(location);
                } else {
                    EditableDiffView.this.lineLocationAsked = true;
                    if (pane == DiffController.DiffPane.Base) {
                        setBaseLineNumberImpl(location);
                    } else {
                        setModifiedLineNumberImpl(location);
                    }
                }
            }
        });
    }

    private void setModifiedLineNumberImpl(int line) {
        initGlobalSizes(); // The window might be resized in the mean time.
        try {
            EditorUI editorUI = org.netbeans.editor.Utilities.getEditorUI(jEditorPane2.getEditorPane());
            if (editorUI == null) return;
            int off2 = org.openide.text.NbDocument.findLineOffset((StyledDocument) jEditorPane2.getEditorPane().getDocument(), line);
            jEditorPane2.getEditorPane().setCaretPosition(off2);

            int lineHeight = editorUI.getLineHeight();

            int offset = jEditorPane2.getScrollPane().getViewport().getViewRect().height / 2 + 1;
            int lineOffset = lineHeight * line - offset;

            JScrollBar rightScrollBar = jEditorPane2.getScrollPane().getVerticalScrollBar();

            rightScrollBar.setValue((int) (lineOffset));
        } catch (IndexOutOfBoundsException ex) {
            Logger.getLogger(EditableDiffView.class.getName()).log(Level.INFO, null, ex);
        }
    }

    private void setBaseLineNumberImpl(int line) {
        initGlobalSizes(); // The window might be resized in the mean time.
        try {
            EditorUI editorUI = org.netbeans.editor.Utilities.getEditorUI(jEditorPane1.getEditorPane());
            if (editorUI == null) return;
            int lineHeight = editorUI.getLineHeight();
    
            int offset = jEditorPane1.getScrollPane().getViewport().getViewRect().height / 2 + 1;
            int lineOffset = lineHeight * line - offset;
    
            int off1 = org.openide.text.NbDocument.findLineOffset((StyledDocument) jEditorPane1.getEditorPane().getDocument(), line);
            jEditorPane1.getEditorPane().setCaretPosition(off1);
    
            JScrollBar leftScrollBar = jEditorPane1.getScrollPane().getVerticalScrollBar();
            leftScrollBar.setValue(lineOffset);
        } catch (IndexOutOfBoundsException ex) {
            ErrorManager.getDefault().notify(ex);
        }
    }

    private void setDifferenceImpl(int location) {
        if (location < -1 || location >= diffs.length) throw new IllegalArgumentException("Illegal difference number: " + location); // NOI18N
        if (location == -1) {
        } else {
            setDifferenceIndex(location);
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    ignoredUpdateEvents = true;
                    showCurrentDifference();
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            ignoredUpdateEvents = false;
                        }
                    });
                }
            });
        }
    }

    public JComponent getJComponent() {
        return jSplitPane1;
    }

    /**
     * @return true if Move, Replace, Insert and Move All actions should be visible and enabled, false otherwise
     */
    public boolean isActionsEnabled() {
        return actionsEnabled;
    }
   
    private void initColors() {
        colorMissing = DiffModuleConfig.getDefault().getDeletedColor();
        colorAdded = DiffModuleConfig.getDefault().getAddedColor(); 
        colorChanged = DiffModuleConfig.getDefault().getChangedColor();
    }

    private void addDocumentListeners() {
        if (baseDocument != null) baseDocument.addDocumentListener(this);
        if (modifiedDocument != null) modifiedDocument.addDocumentListener(this);
    }

    private void removeDocumentListeners() {
        if (baseDocument != null) baseDocument.removeDocumentListener(this);
        if (modifiedDocument != null) modifiedDocument.removeDocumentListener(this);
    }
    
    public void ancestorAdded(AncestorEvent event) {
        DiffModuleConfig.getDefault().getPreferences().addPreferenceChangeListener(this);
        expandFolds();
        initGlobalSizes();
        addChangeListeners();
        addDocumentListeners();
        refreshDiff(50);        

        if (editableCookie == null) return;
        refreshEditableDocument();
        editableCookie.addPropertyChangeListener(this);
    }

    private void refreshEditableDocument() {
        Document doc = null;
        try {
            doc = editableCookie.openDocument();
        } catch (IOException e) {
            Logger.getLogger(EditableDiffView.class.getName()).log(Level.INFO, "Getting new Document from EditorCookie", e); // NOI18N
            return;
        }
        editableDocument.removeDocumentListener(this);
        if (doc != editableDocument) {
            editableDocument = doc;
            jEditorPane2.getEditorPane().setDocument(editableDocument);
            refreshDiff(20);
        }
        editableDocument.addDocumentListener(this);
    }

    public void ancestorRemoved(AncestorEvent event) {
        DiffModuleConfig.getDefault().getPreferences().removePreferenceChangeListener(this);
        removeDocumentListeners();
        if (editableCookie != null) {
            saveModifiedDocument();
            editableCookie.removePropertyChangeListener(this);
        }
    }

    public void preferenceChange(PreferenceChangeEvent evt) {
        initColors();
        diffChanged();  // trigger re-calculation of hightlights in case diff stays the same
        refreshDiff(20);
    }
    
    private void saveModifiedDocument() {
        DataObject dao = (DataObject) editableDocument.getProperty(Document.StreamDescriptionProperty);
        if (dao != null) {
            SaveCookie sc = dao.getCookie(SaveCookie.class);
            if (sc != null) {
                try {
                    sc.save();
                } catch (IOException e) {
                    Logger.getLogger(EditableDiffView.class.getName()).log(Level.INFO, "Error saving Diff document", e); // NOI18N
                }
            }
        }
    }

    public void ancestorMoved(AncestorEvent event) {
    }

    public void insertUpdate(DocumentEvent e) {
        refreshDiff(50);
    }

    public void removeUpdate(DocumentEvent e) {
        refreshDiff(50);
    }

    public void changedUpdate(DocumentEvent e) {
        refreshDiff(50);
    }
    
    Color getColor(Difference ad) {
        if (ad.getType() == Difference.ADD) return colorAdded;
        if (ad.getType() == Difference.CHANGE) return colorChanged;
        return colorMissing;
    }
    
    JComponent getMyDivider() {
        return spui.divider.getDivider();
    }

    DiffContentPanel getEditorPane1() {
        return jEditorPane1;
    }

    DiffContentPanel getEditorPane2() {
        return jEditorPane2;
    }

    public DiffViewManager getManager() {
        return manager;
    }

    Difference[] getDifferences() {
        return diffs;
    }

    private void replace(final StyledDocument doc, final int start, final int length, final String text) {
        NbDocument.runAtomic(doc, new Runnable() {
            public void run() {
                try {
                    doc.remove(start, length);
                    doc.insertString(start, text, null);
                } catch (BadLocationException e) {
                    Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, e.getMessage(), e);
                }
            }
        });
    }
    
    /**
     * Rolls back a difference in the second document.
     * 
     * @param diff a difference to roll back, null to remove all differences
     */ 
    void rollback(Difference diff) {
        StyledDocument document = (StyledDocument) getEditorPane2().getEditorPane().getDocument();
        if (diff == null) {
            Document src = getEditorPane1().getEditorPane().getDocument();
            try {
                replace(document, 0, document.getLength(), src.getText(0, src.getLength()));
            } catch (BadLocationException e) {
                ErrorManager.getDefault().notify(e);
            }
            return;
        }
        try {
            if (diff.getType() == Difference.ADD) {
                int start = DiffViewManager.getRowStartFromLineOffset(document, diff.getSecondStart() - 1);
                int end = DiffViewManager.getRowStartFromLineOffset(document, diff.getSecondEnd());
                if (end == -1) {
                    end = document.getLength();
                }
                document.remove(start, end - start);
            } else if (diff.getType() == Difference.DELETE) {
                int start = DiffViewManager.getRowStartFromLineOffset(document, diff.getSecondStart());
                /**
                 * If adding as the last line, there is no line after
                 * And start is -1;
                 */
                String addedText = diff.getFirstText();
                if (start == -1) {
                    start = document.getLength();
                    addedText = switchLineEndings(addedText);
                }
                document.insertString(start, addedText, null);
            } else {
                int start = DiffViewManager.getRowStartFromLineOffset(document, diff.getSecondStart() - 1);
                int end = DiffViewManager.getRowStartFromLineOffset(document, diff.getSecondEnd());
                if (end == -1) {
                    end = document.getLength();
                }
                replace(document, start, end - start, diff.getFirstText());
            }
        } catch (BadLocationException e) {
            ErrorManager.getDefault().notify(e);
        }
    }

    /**
     * Moves empty line from the end to the beginning
     * @param addedText
     * @return
     */
    private static String switchLineEndings(String addedText) {
        StringBuilder sb = new StringBuilder(addedText);
        sb.insert(0, '\n'); // add a line to the beginning
        if (sb.charAt(sb.length() - 1) == '\n') {
            sb.deleteCharAt(sb.length() - 1); // and remove the last empty line
        }
        return sb.toString();
    }

    Stroke getBoldStroke() {
        return boldStroke;
    }

    class DiffSplitPaneUI extends BasicSplitPaneUI {

        final DiffSplitPaneDivider divider;

        public DiffSplitPaneUI(JSplitPane splitPane) {
            this.splitPane = splitPane;
            divider = new DiffSplitPaneDivider(this, EditableDiffView.this);
        }

        public BasicSplitPaneDivider createDefaultDivider() {
            return divider;
        }
    }
    
    public boolean requestFocusInWindow() {
        return jEditorPane1.requestFocusInWindow();
    }

    public JComponent getComponent() {
        return jSplitPane1;
    }

    public int getDifferenceCount() {
        return diffs.length;
    }

    public boolean canSetCurrentDifference() {
        return true;
    }

    public void setCurrentDifference(int diffNo) throws UnsupportedOperationException {
        setLocation(null, DiffController.LocationType.DifferenceIndex, diffNo);
    }

    public int getCurrentDifference() {
        return getDifferenceIndex();
    }

    private int computeCurrentDifference() {
        // jViewport == null iff initialization failed
        if (manager == null || jViewport2 == null) return 0;
        Rectangle viewRect = jViewport2.getViewRect();
        int bottom = viewRect.y + viewRect.height * 4 / 5;
        DiffViewManager.DecoratedDifference [] ddiffs = manager.getDecorations();
        for (int i = 0; i < ddiffs.length; i++) {
            int startLine = ddiffs[i].getTopRight();
            int endLine = ddiffs[i].getBottomRight();
            if (endLine > bottom || endLine == -1 && startLine > bottom) return Math.max(0, i-1);
        }
        return ddiffs.length - 1;
    }

    /**
     * Notifies the Diff View that it should update the current difference index. If the update is called in the scope
     * of setCurrentDifference() method, this method does nothing. If not, it computes current difference base on
     * current view. This is to ensure the following workflow:
     * 1) If user only pushes Next/Previous buttons in Diff, he wants to review changes one by one
     * 2) If user touches the scrollbar, 'current difference' changes accordingly 
     */
    void updateCurrentDifference() {
        assert SwingUtilities.isEventDispatchThread();
        if (ignoredUpdateEvents) {
            return;
        }
        int cd = computeCurrentDifference();
        setDifferenceIndex(cd);
    }
    
    public JToolBar getToolBar() {
        return null;
    }

    private void showCurrentDifference() {
        int index = getDifferenceIndex();
        if (index < 0 || index >= diffs.length) {
            return;
        }
        Difference diff = diffs[index];
        
        int off1, off2;
        initGlobalSizes(); // The window might be resized in the mean time.
        try {
            off1 = org.openide.text.NbDocument.findLineOffset((StyledDocument) jEditorPane1.getEditorPane().getDocument(), diff.getFirstStart() > 0 ? diff.getFirstStart() - 1 : 0);
            off2 = org.openide.text.NbDocument.findLineOffset((StyledDocument) jEditorPane2.getEditorPane().getDocument(), diff.getSecondStart() > 0 ? diff.getSecondStart() - 1 : 0);

            jEditorPane1.getEditorPane().setCaretPosition(off1);
            jEditorPane2.getEditorPane().setCaretPosition(off2);
            
            DiffViewManager.DecoratedDifference ddiff = manager.getDecorations()[getDifferenceIndex()];
            int offset;
            if (ddiff.getDiff().getType() == Difference.DELETE) {
                offset = jEditorPane2.getScrollPane().getViewport().getViewRect().height / 2 + 1;
            } else {
                offset = jEditorPane2.getScrollPane().getViewport().getViewRect().height / 5;
            }
            jEditorPane2.getScrollPane().getVerticalScrollBar().setValue(ddiff.getTopRight() - offset);
        } catch (IndexOutOfBoundsException ex) {
            Logger.getLogger(EditableDiffView.class.getName()).log(Level.INFO, null, ex);
        }

        // scroll the left pane accordingly
        manager.scroll();
    }
    
    /** This method is called from within the constructor to initialize the form.
     */
    private void initComponents() {
        fileLabel1.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        fileLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        filePanel1.setLayout(new BorderLayout());
        filePanel1.add(fileLabel1, BorderLayout.PAGE_START);

        fileLabel2.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        fileLabel2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        filePanel2.setLayout(new BorderLayout());
        filePanel2.add(fileLabel2, BorderLayout.PAGE_START);

        if (binaryDiff) {
            NoContentPanel ncp1 = new NoContentPanel(NbBundle.getMessage(EditableDiffView.class, "CTL_DiffPanel_BinaryFile"));
            fileLabel1.setLabelFor(ncp1);
            filePanel1.add(ncp1);
            NoContentPanel ncp2 = new NoContentPanel(NbBundle.getMessage(EditableDiffView.class, "CTL_DiffPanel_BinaryFile"));
            fileLabel2.setLabelFor(ncp2);
            filePanel2.add(ncp2);
        } else {
            jEditorPane1 = new DiffContentPanel(this, true);
            jEditorPane2 = new DiffContentPanel(this, false);
            jEditorPane1.getAccessibleContext().setAccessibleName(org.openide.util.NbBundle.getMessage(EditableDiffView.class, "ACS_EditorPane1A11yName"));  // NOI18N
            jEditorPane1.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(EditableDiffView.class, "ACS_EditorPane1A11yDescr"));  // NOI18N
            jEditorPane2.getAccessibleContext().setAccessibleName(org.openide.util.NbBundle.getMessage(EditableDiffView.class, "ACS_EditorPane2A11yName"));  // NOI18N
            jEditorPane2.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(EditableDiffView.class, "ACS_EditorPane2A11yDescr"));  // NOI18N
            fileLabel1.setLabelFor(jEditorPane1);
            filePanel1.add(jEditorPane1);
            fileLabel2.setLabelFor(jEditorPane2);
            filePanel2.add(jEditorPane2);
        }        
        
        jSplitPane1.setLeftComponent(filePanel1);
        jSplitPane1.setRightComponent(filePanel2);        
    }

    // Code for dispatching events from components to event handlers.
    private void expandFolds(JEditorPane pane) {
        final FoldHierarchy fh = FoldHierarchy.get(pane);
        FoldUtilities.expandAll(fh);
        fh.addFoldHierarchyListener(new FoldHierarchyListener() {
            public void foldHierarchyChanged(FoldHierarchyEvent evt) {
                FoldUtilities.expandAll(fh);
            }
        });
    }

    private void expandFolds() {
        expandFolds(jEditorPane1.getEditorPane());
        expandFolds(jEditorPane2.getEditorPane());
    }

    private void initGlobalSizes() {
        StyledDocument doc1 = (StyledDocument) jEditorPane1.getEditorPane().getDocument();
        StyledDocument doc2 = (StyledDocument) jEditorPane2.getEditorPane().getDocument();
        int numLines1 = org.openide.text.NbDocument.findLineNumber(doc1, doc1.getEndPosition().getOffset());
        int numLines2 = org.openide.text.NbDocument.findLineNumber(doc2, doc2.getEndPosition().getOffset());

        int numLines = Math.max(numLines1, numLines2);
        if (numLines < 1) numLines = 1;
        int totHeight = jEditorPane1.getSize().height;
        int value = jEditorPane2.getSize().height;
        if (value > totHeight) totHeight = value;
    }

    private void joinScrollBars() {
        final JScrollBar scrollBarH1 = jEditorPane1.getScrollPane().getHorizontalScrollBar();
        final JScrollBar scrollBarH2 = jEditorPane2.getScrollPane().getHorizontalScrollBar();

        scrollBarH1.getModel().addChangeListener(new javax.swing.event.ChangeListener()  {
            public void stateChanged(javax.swing.event.ChangeEvent e) {
                int value = scrollBarH1.getValue();
                if (value == horizontalScroll1ChangedValue) return;
                int max1 = scrollBarH1.getMaximum();
                int max2 = scrollBarH2.getMaximum();
                int ext1 = scrollBarH1.getModel().getExtent();
                int ext2 = scrollBarH2.getModel().getExtent();
                if (max1 == ext1) horizontalScroll2ChangedValue = 0;
                else horizontalScroll2ChangedValue = (value*(max2 - ext2))/(max1 - ext1);
                horizontalScroll1ChangedValue = -1;
                scrollBarH2.setValue(horizontalScroll2ChangedValue);
            }
        });
        scrollBarH2.getModel().addChangeListener(new javax.swing.event.ChangeListener()  {
            public void stateChanged(javax.swing.event.ChangeEvent e) {
                int value = scrollBarH2.getValue();
                if (value == horizontalScroll2ChangedValue) return;
                int max1 = scrollBarH1.getMaximum();
                int max2 = scrollBarH2.getMaximum();
                int ext1 = scrollBarH1.getModel().getExtent();
                int ext2 = scrollBarH2.getModel().getExtent();
                if (max2 == ext2) horizontalScroll1ChangedValue = 0;
                else horizontalScroll1ChangedValue = (value*(max1 - ext1))/(max2 - ext2);
                horizontalScroll2ChangedValue = -1;
                scrollBarH1.setValue(horizontalScroll1ChangedValue);
            }
        });
    }

    private void customizeEditor(JEditorPane editor) {
        StyledDocument doc;
        Document document = editor.getDocument();
        try {
            doc = (StyledDocument) editor.getDocument();
        } catch(ClassCastException e) {
            doc = new DefaultStyledDocument();
            try {
                doc.insertString(0, document.getText(0, document.getLength()), null);
            } catch (BadLocationException ble) {
                // leaving the document empty
            }
            editor.setDocument(doc);
        }
    }
    
    private void addChangeListeners() {
        jEditorPane1.getEditorPane().addPropertyChangeListener("font", new java.beans.PropertyChangeListener() { // NOI18N
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                javax.swing.SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        diffChanged();
                        initGlobalSizes();
                        jEditorPane1.onUISettingsChanged();
                        getComponent().revalidate();
                        getComponent().repaint();
                    }
                });
            }
        });
        jEditorPane2.getEditorPane().addPropertyChangeListener("font", new java.beans.PropertyChangeListener() { // NOI18N
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                javax.swing.SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        diffChanged();
                        initGlobalSizes();
                        jEditorPane2.onUISettingsChanged();                        
                        getComponent().revalidate();
                        getComponent().repaint();
                    }
                });
            }
        });
    }

    private synchronized void diffChanged() {
        diffSerial++;   // we need to re-compute decorations, font size changed
    }

    private void setSource1(StreamSource ss) throws IOException {
        firstSourceAvailable = false; 
        EditorKit kit = jEditorPane1.getEditorPane().getEditorKit();
        if (kit == null) throw new IOException("Missing Editor Kit"); // NOI18N

        Document sdoc = getSourceDocument(ss);
        baseDocument = sdoc;
        Document doc = sdoc != null ? sdoc : kit.createDefaultDocument();
        if (jEditorPane1.getEditorPane().getUI() instanceof BaseTextUI) {
            if (sdoc == null) {
                Reader r = ss.createReader();
                if (r != null) {
                    firstSourceAvailable = true;
                    try {
                        kit.read(r, doc, 0);
                    } catch (javax.swing.text.BadLocationException e) {
                        throw new IOException("Can not locate the beginning of the document."); // NOI18N
                    } finally {
                        r.close();
                    }
                }
            } else {
                firstSourceAvailable = true;
            }
        } else {
            firstSourceUnsupportedTextUI = true;
        }
        jEditorPane1.initActions();        
        jEditorPane1.getEditorPane().setDocument(doc);
        customizeEditor(jEditorPane1.getEditorPane());
    }
    
    private Document getSourceDocument(StreamSource ss) {
        Document sdoc = null;
        FileObject fo = ss.getLookup().lookup(FileObject.class);
        if (fo != null) {
            try {
                DataObject dao = DataObject.find(fo);
                if (dao.getPrimaryFile() == fo) {
                    EditorCookie ec = dao.getCookie(EditorCookie.class);
                    if (ec != null) {
                        sdoc = ec.openDocument();
                    }
                }
            } catch (Exception e) {
                // fallback to other means of obtaining the source
            }
        } else {
            sdoc = ss.getLookup().lookup(Document.class);
        }
        return sdoc;
    }

    private void setSource2(StreamSource ss) throws IOException {
        secondSourceAvailable = false;
        EditorKit kit = jEditorPane2.getEditorPane().getEditorKit();
        if (kit == null) throw new IOException("Missing Editor Kit"); // NOI18N
        
        Document sdoc = getSourceDocument(ss);
        modifiedDocument = sdoc;
        if (sdoc != null && ss.isEditable()) {
            DataObject dao = (DataObject) sdoc.getProperty(Document.StreamDescriptionProperty);
            if (dao != null) {
                if (dao instanceof MultiDataObject) {
                    MultiDataObject mdao = (MultiDataObject) dao;
                    for (MultiDataObject.Entry entry : mdao.secondaryEntries()) {
                        if (entry instanceof CookieSet.Factory) {
                            CookieSet.Factory factory = (CookieSet.Factory) entry;
                            EditorCookie ec = factory.createCookie(EditorCookie.class);
                            Document entryDocument = ec.getDocument();
                            if (entryDocument == sdoc && ec instanceof EditorCookie.Observable) {
                                editableCookie = (EditorCookie.Observable) ec;
                                editableDocument = sdoc;
                                editorUndoRedo = getUndoRedo(ec);
                            }
                        }
                    }
                }
                if (editableCookie == null) {
                    EditorCookie cookie = dao.getCookie(EditorCookie.class);
                    if (cookie instanceof EditorCookie.Observable) {
                        editableCookie = (EditorCookie.Observable) cookie;
                        editableDocument = sdoc;
                        editorUndoRedo = getUndoRedo(cookie);
                    }
                }
            }
        }
        Document doc = sdoc != null ? sdoc : kit.createDefaultDocument();
        if (jEditorPane2.getEditorPane().getUI() instanceof BaseTextUI) {
            if (sdoc == null) {
                Reader r = ss.createReader();
                if (r != null) {
                    secondSourceAvailable = true;
                    try {
                        kit.read(r, doc, 0);
                    } catch (javax.swing.text.BadLocationException e) {
                        throw new IOException("Can not locate the beginning of the document."); // NOI18N
                    } finally {
                        r.close();
                    }
                }
            } else {
                secondSourceAvailable = true;
            }
        } else {
            secondSourceUnsupportedTextUI = true;
        }
        jEditorPane2.initActions();
        jSplitPane1.putClientProperty(UndoRedo.class, editorUndoRedo);
        jEditorPane2.getEditorPane().setDocument(doc);
        jEditorPane2.getEditorPane().setEditable(editableCookie != null);
        if (doc instanceof NbDocument.CustomEditor) {
            Component c = ((NbDocument.CustomEditor)doc).createEditor(jEditorPane2.getEditorPane());
            if (c instanceof JComponent) {
                jEditorPane2.setCustomEditor((JComponent)c);
            }
        }
        
        customizeEditor(jEditorPane2.getEditorPane());
        jViewport2 = jEditorPane2.getScrollPane().getViewport();
        joinScrollBars();
    }
    
    private UndoRedo.Manager getUndoRedo(EditorCookie cookie) {
        // TODO: working around #96543 
        try {
            Method method = CloneableEditorSupport.class.getDeclaredMethod("getUndoRedo"); // NOI18N
            method.setAccessible(true);
            return (UndoRedo.Manager) method.invoke(cookie);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void propertyChange(final PropertyChangeEvent evt) {
        if (EditorCookie.Observable.PROP_DOCUMENT.equals(evt.getPropertyName())) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    refreshEditableDocument();
                }
            }); 
        }
    }

    public void setSourceTitle(JLabel label, String title) {
        label.setText(title);
        label.setToolTipText(title);
        // Set the minimum size in 'x' direction to a low value, so that the splitter can be moved to corner locations
        label.setMinimumSize(new Dimension(3, label.getMinimumSize().height));
    }
    
    public void setDocument1(Document doc) {
        if (doc != null) {
            jEditorPane1.getEditorPane().setDocument(doc);
        }
    }
    
    public void setDocument2(Document doc) {
        if (doc != null) {
            jEditorPane2.getEditorPane().setDocument(doc);
        }
    }

    private void refreshDiff(int delayMillis) {
        refreshDiffTask.schedule(delayMillis);
    }

    public class RefreshDiffTask implements Runnable {

        public void run() {
            synchronized (RefreshDiffTask.this) {
                final Difference[] differences = computeDiff();
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        diffs = differences;
                        if (diffs != NO_DIFFERENCES) {
                            diffChanged();
                        }
                        if (getDifferenceIndex() >= diffs.length) updateCurrentDifference();
                        support.firePropertyChange(DiffController.PROP_DIFFERENCES, null, null);
                        jEditorPane1.setCurrentDiff(diffs);
                        jEditorPane2.setCurrentDiff(diffs);
                        refreshDividerSize();
                        jSplitPane1.repaint();
                        diffMarkprovider.refresh();
                        if (diffs.length > 0 && getCurrentDifference() == -1 && !EditableDiffView.this.lineLocationAsked) {
                            setCurrentDifference(0);
                        }
                    }
                });
            }
        }

        private Difference[] computeDiff() {
            if (!secondSourceAvailable || !firstSourceAvailable) {
                return NO_DIFFERENCES;
            }

            Reader first = getReader(jEditorPane1.getEditorPane().getDocument());
            Reader second = getReader(jEditorPane2.getEditorPane().getDocument());
            if (first == null || second == null) {
                return NO_DIFFERENCES;
            }

            DiffProvider diff = DiffModuleConfig.getDefault().getDefaultDiffProvider();
            Difference[] diffs;
            try {
                diffs = diff.computeDiff(first, second);
            } catch (IOException e) {
                diffs = NO_DIFFERENCES;
            }
            return diffs;
        }
    }

    /**
     * Runs under a read lock
     * @param doc
     * @return
     */
    private Reader getReader (final Document doc) {
        final Reader[] reader = new Reader[1];
        doc.render(new Runnable() {
            public void run() {
                try {
                    reader[0] = new StringReader(doc.getText(0, doc.getLength()));
                } catch (BadLocationException ex) {
                    Logger.getLogger(EditableDiffView.class.getName()).log(Level.INFO, null, ex);
                }
            }
        });
        return reader[0];
    }
    
    private void repairTextUI (DecoratedEditorPane pane) {
        TextUI ui = pane.getUI();
        if (!(ui instanceof BaseTextUI)) {
            // use plain editor
            pane.setEditorKit(CloneableEditorSupport.getEditorKit("text/plain")); //NOI18N
        }
    }

    private void refreshDividerSize() {
        Font font = jSplitPane1.getFont();
        if (font == null) return;
        FontMetrics fm = jSplitPane1.getFontMetrics(jSplitPane1.getFont());
        String maxDiffNumber = Integer.toString(Math.max(1, diffs.length));
        int neededWidth = fm.stringWidth(maxDiffNumber + " /" + maxDiffNumber);
        jSplitPane1.setDividerSize(Math.max(neededWidth, INITIAL_DIVIDER_SIZE));
    }

    synchronized int getDiffSerial() {
        return diffSerial;
    }

    static Difference getFirstDifference(Difference [] diff, int line) {
        if (line < 0) return null;
        for (int i = 0; i < diff.length; i++) {
            Difference difference = diff[i];
            if (line < difference.getFirstStart()) return null;
            if (difference.getType() == Difference.ADD && line == difference.getFirstStart()) return difference;
            if (line <= difference.getFirstEnd()) return difference;
        }
        return null;
    }

    static Difference getSecondDifference(Difference [] diff, int line) {
        if (line < 0) return null;
        for (int i = 0; i < diff.length; i++) {
            Difference difference = diff[i];
            if (line < difference.getSecondStart()) return null;
            if (difference.getType() == Difference.DELETE && line == difference.getSecondStart()) return difference;
            if (line <= difference.getSecondEnd()) return difference;
        }
        return null;
    }
    
    Color getColorLines() {
        return colorLines;
    }

    /**
     * Integration provider for the error stripe.
     */
    private class EditableDiffMarkProvider extends MarkProvider {

        private List<Mark> marks;

        public EditableDiffMarkProvider() {
            marks = getMarksForDifferences();
        }

        public List<Mark> getMarks() {
            return marks;
        }

        void refresh() {
            List<Mark> oldMarks = marks;
            marks = getMarksForDifferences();
            firePropertyChange(PROP_MARKS, oldMarks, marks);
        }

        private List<Mark> getMarksForDifferences() {
            if (diffs == null) return Collections.emptyList();
            List<Mark> marks = new ArrayList<Mark>(diffs.length);
            for (int i = 0; i < diffs.length; i++) {
                Difference difference = diffs[i];
                marks.add(new DiffMark(difference, getColor(difference)));
            }
            return marks;
        }
    }
}
