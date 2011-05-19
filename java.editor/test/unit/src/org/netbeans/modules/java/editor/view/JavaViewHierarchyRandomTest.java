/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
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

package org.netbeans.modules.java.editor.view;

import java.awt.Color;
import java.awt.Font;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.EventListenerList;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Document;
import javax.swing.text.Position;
import javax.swing.text.Position.Bias;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.View;
import org.netbeans.api.editor.mimelookup.MimePath;
import org.netbeans.api.java.lexer.JavaTokenId;
import org.netbeans.api.lexer.Language;
import org.netbeans.editor.BaseDocument;
import org.netbeans.junit.Filter;
import org.netbeans.junit.NbTestCase;
import org.netbeans.lib.editor.util.random.DocumentTesting;
import org.netbeans.lib.editor.util.random.EditorPaneTesting;
import org.netbeans.lib.editor.util.random.RandomTestContainer;
import org.netbeans.modules.editor.java.JavaKit;
import org.netbeans.modules.editor.lib2.view.ViewHierarchyRandomTesting;
import org.netbeans.spi.editor.highlighting.HighlightsChangeListener;
import org.netbeans.spi.editor.highlighting.HighlightsContainer;
import org.netbeans.spi.editor.highlighting.HighlightsLayer;
import org.netbeans.spi.editor.highlighting.HighlightsLayerFactory;
import org.netbeans.spi.editor.highlighting.HighlightsSequence;
import org.netbeans.spi.editor.highlighting.ZOrder;
import org.netbeans.spi.editor.mimelookup.MimeDataProvider;
import org.openide.util.Lookup;
import org.openide.util.RequestProcessor;
import org.openide.util.Task;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;

/**
 *
 * @author Miloslav Metelka
 */
public class JavaViewHierarchyRandomTest extends NbTestCase {

    private static final int OP_COUNT = 100;

    public JavaViewHierarchyRandomTest(String testName) {
        super(testName);
        List<String> includes = new ArrayList<String>();
//        includes.add("testToolTipView");
//        includes.add("testSimpleBadListener");
//        includes.add("testRemoveNewline");
//        includes.add("testGap");
//        includes.add("testNPEInRedo", "");
//        includes.add("testRandomModsPlainText");
//        includes.add("testInsertRemoveSingleChar");
//        includes.add("testUndo750");
//        includes.add("testUndoRedoSimple");
//        includes.add("testInsertTextWithNewlines");
//        includes.add("testInsertSimpleRemoveContent");
//        includes.add("testNewlineInsertUndo");
//        includes.add("testNewlineLineOne");
//        includes.add("testSimple1");
//        includes.add("testRandomModsJavaSeed1");
//        includes.add("testLengthyEdit");
//        filterTests(includes);
    }
    
    private void filterTests(List<String> includeTestNames) {
        List<Filter.IncludeExclude> includeTests = new ArrayList<Filter.IncludeExclude>();
        for (String testName : includeTestNames) {
            includeTests.add(new Filter.IncludeExclude(testName, ""));
        }
        Filter filter = new Filter();
        filter.setIncludes(includeTests.toArray(new Filter.IncludeExclude[includeTests.size()]));
        setFilter(filter);
    }

    private static void loggingOn() {
        Level LOG_LEVEL = Level.FINE;
        // FINEST throws ISE for integrity error in EditorView
        Logger.getLogger("org.netbeans.modules.editor.lib2.view.EditorView").setLevel(Level.FINEST);
        Logger.getLogger("org.netbeans.modules.editor.lib2.view.ViewBuilder").setLevel(LOG_LEVEL);
        Logger.getLogger("org.netbeans.modules.editor.lib2.view.ViewUpdates").setLevel(LOG_LEVEL);
        // Check gap-storage correctness
        Logger.getLogger("org.netbeans.modules.editor.lib2.view.EditorBoxViewChildren").setLevel(Level.FINE);
        Logger.getLogger("org.netbeans.editor.BaseDocument.EDT").setLevel(Level.FINE);
        Logger.getLogger("org.netbeans.editor.BaseCaret.EDT").setLevel(Level.FINE);
        ViewHierarchyRandomTesting.setDirectViewRebuild(true);
    }

    @Override
    protected Level logLevel() {
//        return Level.FINEST;
//        return Level.FINE;
//        return Level.INFO;
        return null;
    }

    private RandomTestContainer createContainer() throws Exception {
        JavaKit kit = new JavaKit();
//        kit.call();
        // org.netbeans.core.windows.actions.RecentViewListAction fails to load
        RandomTestContainer container = ViewHierarchyRandomTesting.createContainer(kit); // no problem for both java and plain mime-types
        container.setName(this.getName());
        boolean logOpAndDoc = false; //true;
        container.setLogOp(logOpAndDoc);
        DocumentTesting.setLogDoc(container, logOpAndDoc);
        return container;
    }

    public void testModelToViewAtBoundaries() throws Exception {
        loggingOn();
        RandomTestContainer container = createContainer();
        final JEditorPane pane = container.getInstance(JEditorPane.class);
        final Document doc = pane.getDocument();
        doc.putProperty("mimeType", "text/plain");
        RandomTestContainer.Context context = container.context();
        ViewHierarchyRandomTesting.disableHighlighting(container);
        DocumentTesting.insert(context, 0, "ab\ncde");
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                try {
                    int startOffset = 0;
                    int endOffset = doc.getLength() + 1;
                    modelToView(startOffset);
                    modelToView(endOffset);
                    getNextVisualPositionFrom(startOffset);
                    getNextVisualPositionFrom(endOffset);
                } catch (BadLocationException e) {
                    throw new IllegalStateException(e);
                }
            }
            
            private Rectangle modelToView(int offset) throws BadLocationException {
                    return pane.modelToView(offset);
            }
            
            private int getNextVisualPositionFrom(int offset) throws BadLocationException {
                Bias[] biasRet = new Bias[1];
                int retOffset = pane.getUI().getNextVisualPositionFrom(pane, offset, Bias.Forward, View.NORTH, biasRet);
                retOffset = pane.getUI().getNextVisualPositionFrom(pane, offset, Bias.Forward, View.SOUTH, biasRet);
                retOffset = pane.getUI().getNextVisualPositionFrom(pane, offset, Bias.Forward, View.WEST, biasRet);
                retOffset = pane.getUI().getNextVisualPositionFrom(pane, offset, Bias.Forward, View.EAST, biasRet);
                return retOffset;
            }
        });
        
    }

    public void testInsertRemoveSingleChar() throws Exception {
        loggingOn();
        RandomTestContainer container = createContainer();
        JEditorPane pane = container.getInstance(JEditorPane.class);
        Document doc = pane.getDocument();
        doc.putProperty("mimeType", "text/plain");
        RandomTestContainer.Context context = container.context();
        ViewHierarchyRandomTesting.disableHighlighting(container);
        DocumentTesting.insert(context, 0, "a");
        DocumentTesting.remove(context, 0, 1);
        DocumentTesting.insert(context, 0, "b");
        DocumentTesting.undo(context, 1);
    }

    public void testRemoveNewline() throws Exception {
        loggingOn();
        RandomTestContainer container = createContainer();
        JEditorPane pane = container.getInstance(JEditorPane.class);
        Document doc = pane.getDocument();
        doc.putProperty("mimeType", "text/plain");
        RandomTestContainer.Context context = container.context();
        ViewHierarchyRandomTesting.disableHighlighting(container);
        DocumentTesting.insert(context, 0, "a\nb");
        DocumentTesting.remove(context, 1, 1);
        DocumentTesting.undo(context, 1);
    }

    public void testNewlineInEmptyDoc() throws Exception {
        loggingOn();
        RandomTestContainer container = createContainer();
        JEditorPane pane = container.getInstance(JEditorPane.class);
        Document doc = pane.getDocument();
        doc.putProperty("mimeType", "text/plain");
        RandomTestContainer.Context gContext = container.context();
        DocumentTesting.insert(gContext, 0, "\n");
    }
    
    public void testSimple1() throws Exception {
        loggingOn();
        RandomTestContainer container = createContainer();
        JEditorPane pane = container.getInstance(JEditorPane.class);
        Document doc = pane.getDocument();
        doc.putProperty("mimeType", "text/plain");
        RandomTestContainer.Context gContext = container.context();
        DocumentTesting.insert(gContext, 0, "a\nb");
        DocumentTesting.insert(gContext, 1, "c");
    }
    
    public void testSimple2() throws Exception {
        loggingOn();
        RandomTestContainer container = createContainer();
        JEditorPane pane = container.getInstance(JEditorPane.class);
        Document doc = pane.getDocument();
        doc.putProperty("mimeType", "text/plain");
        RandomTestContainer.Context gContext = container.context();
        DocumentTesting.insert(gContext, 0, "a\nb\n\n");
        DocumentTesting.remove(gContext, 2, 1);
        DocumentTesting.insert(gContext, 1, "c");
    }
    
    public void testSimpleBadListener() throws Exception {
        loggingOn();
        RandomTestContainer container = createContainer();
        JEditorPane pane = container.getInstance(JEditorPane.class);
        Document doc = pane.getDocument();
        doc.putProperty("mimeType", "text/plain");
        DocumentTesting.setSameThreadInvoke(container.context(), true); // Do not post to EDT
        doc.addDocumentListener(new DocumentListener() {

            @Override
            public void insertUpdate(DocumentEvent e) {
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                throw new IndexOutOfBoundsException("Test exception from bad listener");
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
            }
        });
        RandomTestContainer.Context gContext = container.context();
        DocumentTesting.insert(gContext, 0, "a\nb");
        DocumentTesting.insert(gContext, 1, "c");
        try {
            DocumentTesting.remove(gContext, 2, 1);
            fail("Exception expected.");
        } catch (IndexOutOfBoundsException ex) {
            // Expected
        }
        DocumentTesting.insert(gContext, 1, "d\nx");
    }
    
    public void testBeyondEndDocHighlightsLayer() throws Exception {
        loggingOn();
        String mimeType = "text/plain";
//        MimeLookup.getLookup(MimePath.get(mimeType)).lookup(HighlightsLayer.class); // Init ML
//        HighlightsLayerProvider.clear();
        HighlightsLayerProvider.add("text/x-java", new HLFactory());
        HighlightsLayerProvider.add(mimeType, new HLFactory());
//        MemoryMimeDataProvider.reset(null);
//        MemoryMimeDataProvider.addInstances("text/x-java", new HLFactory());
//        MemoryMimeDataProvider.addInstances(mimeType, new HLFactory());

        RandomTestContainer container = createContainer();
        JEditorPane pane = container.getInstance(JEditorPane.class);
        Document doc = pane.getDocument();
        doc.putProperty("mimeType", mimeType);
        DocumentTesting.setSameThreadInvoke(container.context(), true); // Do not post to EDT
        RandomTestContainer.Context gContext = container.context();
        DocumentTesting.insert(gContext, 0, "a\nb");
        DocumentTesting.insert(gContext, 1, "c");
        DocumentTesting.remove(gContext, 2, 1);
        DocumentTesting.insert(gContext, 1, "d\nx");
    }
    
    public void testInsertRemoveTransaction() throws Exception {
        loggingOn();
        RandomTestContainer container = createContainer();
        JEditorPane pane = container.getInstance(JEditorPane.class);
        final Document doc = pane.getDocument();
        doc.putProperty("mimeType", "text/plain");
        final RandomTestContainer.Context context = container.context();
//        ViewHierarchyRandomTesting.disableHighlighting(container);
        DocumentTesting.setSameThreadInvoke(container.context(), true); // Do not post to EDT
        ((BaseDocument)doc).runAtomic(new Runnable() {
            @Override
            public void run() {
                try {
                    for (int i = 0; i < 100; i++) {
                        DocumentTesting.insert(context, 0, "a\nb\n\n");
                        DocumentTesting.remove(context, i * 3, 1);
                    }
                } catch (Exception ex) {
                    throw new IllegalStateException(ex);
                }
            }
        });
        DocumentTesting.setSameThreadInvoke(container.context(), false);
        DocumentTesting.undo(context, 1);
    }

    public void testToolTipView() throws Exception {
        loggingOn();
        RandomTestContainer container = createContainer();
        final JEditorPane pane = container.getInstance(JEditorPane.class);
        final Document doc = pane.getDocument();
        doc.putProperty("mimeType", "text/plain");
        RandomTestContainer.Context context = container.context();
//        ViewHierarchyRandomTesting.disableHighlighting(container);
        DocumentTesting.setSameThreadInvoke(context, true); // Do not post to EDT
        DocumentTesting.insert(context, 0, "abc\ndef\nghi\n");
        final JEditorPane[] toolTipPaneRef = new JEditorPane[1];
        final BadLocationException[] excRef = new BadLocationException[1];
        final JFrame[] toolTipFrameRef = new JFrame[1];
        Runnable tooltipRun = new Runnable() {
            @Override
            public void run() {
                JEditorPane toolTipPane = new JEditorPane();
                toolTipPaneRef[0] = toolTipPane;
                toolTipPane.setEditorKit(pane.getEditorKit());
                try {
                    Position startPos = doc.createPosition(4); // Line begining
                    Position endPos = doc.createPosition(8); // Line boundary too
                    toolTipPane.putClientProperty("document-view-start-position", startPos);
                    toolTipPane.putClientProperty("document-view-end-position", endPos);
                    toolTipPane.setDocument(doc);
                    JFrame toolTipFrame = new JFrame("ToolTip Frame");
                    toolTipFrameRef[0] = toolTipFrame;
                    toolTipFrame.getContentPane().add(new JScrollPane(toolTipPane));
                    toolTipFrame.setSize(100, 100);
                    toolTipFrame.setVisible(true);

                    doc.insertString(4, "o", null);
                    toolTipPane.setFont(new Font("Monospaced", Font.PLAIN, 22)); // Force VH rebuild
                    toolTipPane.modelToView(6);
                    doc.remove(3, 3);
                    doc.insertString(4, "ab", null);
                    
                    assert (endPos.getOffset() == 8);
                    doc.remove(7, 2);
                    toolTipPane.setFont(new Font("Monospaced", Font.PLAIN, 23)); // Force VH rebuild
                    toolTipPane.modelToView(6);

                } catch (BadLocationException ex) {
                    excRef[0] = ex;
                }

            }
        };
        SwingUtilities.invokeAndWait(tooltipRun);
        if (excRef[0] != null) {
            throw new IllegalStateException(excRef[0]);
        }

        DocumentTesting.setSameThreadInvoke(context, false);
        DocumentTesting.undo(context, 2);
        DocumentTesting.undo(context, 1);
        DocumentTesting.undo(context, 1);
        DocumentTesting.redo(context, 4);
        
        // Hide tooltip's frame
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (toolTipFrameRef[0] != null) {
                    toolTipFrameRef[0].setVisible(false);
                }
            }
        });
    }

    public void testInsertTextWithNewlines() throws Exception {
        loggingOn();
        RandomTestContainer container = createContainer();
        JEditorPane pane = container.getInstance(JEditorPane.class);
        Document doc = pane.getDocument();
        doc.putProperty("mimeType", "text/plain");
        RandomTestContainer.Context context = container.context();
        DocumentTesting.insert(context, 0, "a\n");
    }

    public void testInsertSimpleRemoveContent() throws Exception {
        loggingOn();
        RandomTestContainer container = createContainer();
        JEditorPane pane = container.getInstance(JEditorPane.class);
        Document doc = pane.getDocument();
        doc.putProperty("mimeType", "text/plain");
        RandomTestContainer.Context context = container.context();
//        ViewHierarchyRandomTesting.disableHighlighting(container);
        DocumentTesting.insert(context, 0, "\n\n\n");
        DocumentTesting.remove(context, 0, doc.getLength());
    }

    public void testGap() throws Exception {
        loggingOn();
        RandomTestContainer container = createContainer();
        JEditorPane pane = container.getInstance(JEditorPane.class);
        Document doc = pane.getDocument();
        doc.putProperty("mimeType", "text/plain");
        ViewHierarchyRandomTesting.initRandomText(container);
        ViewHierarchyRandomTesting.addRound(container).setOpCount(OP_COUNT);

        RandomTestContainer.Context context = container.context();
        // Clear document contents
        DocumentTesting.insert(context, 0, "a\tb\tc\td\te\tf\n");
        EditorPaneTesting.setCaretOffset(context, 1);
        DocumentTesting.insert(context, 1, "x");
        EditorPaneTesting.setCaretOffset(context, 5);
        EditorPaneTesting.typeChar(context, 'y');
        EditorPaneTesting.moveOrSelect(context, SwingConstants.EAST, true);
        EditorPaneTesting.typeChar(context, 'z');
        EditorPaneTesting.moveOrSelect(context, SwingConstants.WEST, true);
        EditorPaneTesting.moveOrSelect(context, SwingConstants.WEST, true);
        EditorPaneTesting.moveOrSelect(context, SwingConstants.WEST, true);
        EditorPaneTesting.typeChar(context, 'u');
//        EditorPaneTesting.moveOrSelect(context, SwingConstants.NORTH, true);
//        EditorPaneTesting.typeChar(context, 'y');
//        EditorPaneTesting.performAction(context, pane, DefaultEditorKit.deleteNextCharAction);
        DocumentTesting.undo(context, 1);
        DocumentTesting.redo(context, 1);
        DocumentTesting.undo(context, 2);
    }

    public void testViewReplaceLineBoundary() throws Exception {
        loggingOn();
        RandomTestContainer container = createContainer();
        JEditorPane pane = container.getInstance(JEditorPane.class);
        Document doc = pane.getDocument();
        doc.putProperty("mimeType", "text/plain");
        ViewHierarchyRandomTesting.initRandomText(container);
        ViewHierarchyRandomTesting.addRound(container).setOpCount(OP_COUNT);

        RandomTestContainer.Context context = container.context();
        // Clear document contents
        DocumentTesting.insert(context, 0,
               //012 345 6 789 012 
                "ab\ncd\n\nef\ngh\n");
        EditorPaneTesting.setCaretOffset(context, 0);
        DocumentTesting.remove(context, 6, 4);
        DocumentTesting.undo(context, 1);
        DocumentTesting.redo(context, 1);
        DocumentTesting.undo(context, 2);
        DocumentTesting.redo(context, 2);
    }

    public void testNewlineLineOne() throws Exception {
        loggingOn();
        RandomTestContainer container = createContainer();
        JEditorPane pane = container.getInstance(JEditorPane.class);
        Document doc = pane.getDocument();
        doc.putProperty("mimeType", "text/plain");
        ViewHierarchyRandomTesting.initRandomText(container);
        ViewHierarchyRandomTesting.addRound(container).setOpCount(OP_COUNT);
        ViewHierarchyRandomTesting.testFixedScenarios(container);

        RandomTestContainer.Context context = container.context();
        // Clear document contents
        DocumentTesting.remove(context, 0, doc.getLength());
        DocumentTesting.insert(context, 0, "\n");
        EditorPaneTesting.moveOrSelect(context, SwingConstants.NORTH, false);
        EditorPaneTesting.moveOrSelect(context, SwingConstants.SOUTH, true);
        EditorPaneTesting.performAction(context, pane, DefaultEditorKit.deleteNextCharAction);
        DocumentTesting.undo(context, 1);
    }

    public void testNewlineInsertUndo() throws Exception {
        loggingOn();
        RandomTestContainer container = createContainer();
        JEditorPane pane = container.getInstance(JEditorPane.class);
        Document doc = pane.getDocument();
        doc.putProperty("mimeType", "text/plain");
        RandomTestContainer.Context context = container.context();
        DocumentTesting.insert(context, 0, "\n");
        DocumentTesting.remove(context, 0, 1);
        DocumentTesting.undo(context, 1);
    }

    public void testNPEInRedo() throws Exception {
        loggingOn();
        RandomTestContainer container = createContainer();
        JEditorPane pane = container.getInstance(JEditorPane.class);
        Document doc = pane.getDocument();
        doc.putProperty("mimeType", "text/plain");
        ViewHierarchyRandomTesting.initRandomText(container);
        ViewHierarchyRandomTesting.addRound(container).setOpCount(OP_COUNT);
        ViewHierarchyRandomTesting.testFixedScenarios(container);

        RandomTestContainer.Context context = container.context();
        // Clear document contents
        DocumentTesting.remove(context, 0, doc.getLength());
        DocumentTesting.insert(context, 0,
            "xrlq\n\nmz \t\tabcdef\ts \n\n\nna\n\n j   c gxo\t hw krmsl \n\n\nc " +
            " ngw \tz\tkjwu\ndlunc b\nw\n\n\n knas \t\tbcdefj\t\t n \t\tabcdef\t" +
            "rnehaf\ncl     xe \n\nq\nr\t bv\n       mu i\ny\n e\n\nx\n r\tt h \n" +
            "\n\n \n\n\n\tp\t \tiv\t\nx\nu\t\tahpi\t\tdm cg\t \tcd\nef\t\tabcdef" +
            "\taouvibcd \nwvzta\njdbm  elxb \t\tadmnuilwlbcde\tf\tmx\nz\nv f\ns " +
            "\nfsrhe\ngu  a axsnpmr\t\tab \t\tabcdef\tcdef\t\t\tabo \tdwci\tcp \n" +
            "\n\ncdef\t \t\tabcrd \t\tabcdef\tefahk vif\tfcg xo\t \nf\nvl\nyzfh\n"
        );
        EditorPaneTesting.setCaretOffset(context, 273);
        DocumentTesting.insert(context, 279, "h");
        EditorPaneTesting.typeChar(context, 's');
        EditorPaneTesting.moveOrSelect(context, SwingConstants.EAST, false);
        EditorPaneTesting.moveOrSelect(context, SwingConstants.WEST, false);
        EditorPaneTesting.moveOrSelect(context, SwingConstants.WEST, true);
        EditorPaneTesting.typeChar(context, 'r');
        EditorPaneTesting.typeChar(context, 'y');
        DocumentTesting.insert(context, 275, "j");
        EditorPaneTesting.moveOrSelect(context, SwingConstants.NORTH, false);
        DocumentTesting.remove(context, 305, 1);
        EditorPaneTesting.moveOrSelect(context, SwingConstants.EAST, true);
        EditorPaneTesting.moveOrSelect(context, SwingConstants.NORTH, false);
        DocumentTesting.redo(context, 2);
        DocumentTesting.undo(context, 1);
        EditorPaneTesting.moveOrSelect(context, SwingConstants.EAST, false);
        EditorPaneTesting.moveOrSelect(context, SwingConstants.NORTH, true);
        EditorPaneTesting.setCaretOffset(context, 142);
        EditorPaneTesting.moveOrSelect(context, SwingConstants.EAST, true);
        EditorPaneTesting.moveOrSelect(context, SwingConstants.SOUTH, true);
        EditorPaneTesting.typeChar(context, 'n');
        DocumentTesting.undo(context, 2);
        DocumentTesting.redo(context, 1);
    }

    public void testUndoRedoSimple() throws Exception {
        loggingOn();
        RandomTestContainer container = createContainer();
        JEditorPane pane = container.getInstance(JEditorPane.class);
        Document doc = pane.getDocument();
        doc.putProperty("mimeType", "text/plain");
        ViewHierarchyRandomTesting.initRandomText(container);
        RandomTestContainer.Context context = container.context();
        DocumentTesting.insert(context, 0, "ab\nglanm\nq\n        \nv  nyk\n    \ndy qucjfn\tfh cdk \t\t \nj\nsm\n t\ngqa \nsjj\n\n\n");
        EditorPaneTesting.setCaretOffset(context, 38);
        EditorPaneTesting.moveCaret(context, 31);
        EditorPaneTesting.moveOrSelect(context, SwingConstants.EAST, true);
        EditorPaneTesting.moveOrSelect(context, SwingConstants.EAST, false);
        EditorPaneTesting.typeChar(context, 'j'); // #1: INSERT: off=38 len=1 "j"
        EditorPaneTesting.moveOrSelect(context, SwingConstants.NORTH, false);
        EditorPaneTesting.moveOrSelect(context, SwingConstants.NORTH, false);
        EditorPaneTesting.moveOrSelect(context, SwingConstants.WEST, false);
        EditorPaneTesting.moveOrSelect(context, SwingConstants.SOUTH, false);
        EditorPaneTesting.moveOrSelect(context, SwingConstants.NORTH, true);
        EditorPaneTesting.moveOrSelect(context, SwingConstants.WEST, true);
        EditorPaneTesting.typeChar(context, 'q'); // #2: REMOVE: off=23 len=8; INSERT: off=23 len=1 text="q"
        EditorPaneTesting.moveOrSelect(context, SwingConstants.SOUTH, true);
        EditorPaneTesting.moveOrSelect(context, SwingConstants.WEST, false);
        EditorPaneTesting.performAction(context, pane, DefaultEditorKit.insertTabAction); // #3: INSERT: off=24 len=4 "    "
        // #3.replaceEdit(#2) => false (not replaced; added)
        EditorPaneTesting.moveOrSelect(context, SwingConstants.NORTH, true);
        DocumentTesting.undo(context, 1);
        // #3.undo() (AtomicCompoundEdit)
        EditorPaneTesting.moveOrSelect(context, SwingConstants.WEST, true);
        EditorPaneTesting.moveOrSelect(context, SwingConstants.EAST, false);
        EditorPaneTesting.moveOrSelect(context, SwingConstants.NORTH, true);
        EditorPaneTesting.moveOrSelect(context, SwingConstants.WEST, true);
        EditorPaneTesting.moveOrSelect(context, SwingConstants.SOUTH, false);
        EditorPaneTesting.moveOrSelect(context, SwingConstants.SOUTH, false);
        EditorPaneTesting.performAction(context, pane, DefaultEditorKit.insertTabAction); // #4: INSERT: off=28 len=1 " "
        // 
        DocumentTesting.undo(context, 1);
    }

    public void testUndo750Simplified() throws Exception {
        loggingOn();
        RandomTestContainer container = createContainer();
        JEditorPane pane = container.getInstance(JEditorPane.class);
        Document doc = pane.getDocument();
        doc.putProperty("mimeType", "text/plain");
        ViewHierarchyRandomTesting.initRandomText(container);
        RandomTestContainer.Context context = container.context();
        DocumentTesting.insert(context, 0,
"  \naxj \n\n\nm hebkinc  krnb\t\tabce\n\nd\t\n\n \t\talja\nj \t\tabcdef\tcdef \t \n\n\n\tabcdf\t\tq tzaicl  \t\tabcdef\t  \nglanm\nq\n        \nv  nyk\n    \ndy qucjfn\tfh cdk \t\t \nj\nsm\n t\ngqa \nsjj\n\n\ncdef\t\n \t\tpabg\to\nkbcvde\tjs\ny\tfw\nr\n\n\nced"
        );
        EditorPaneTesting.setCaretOffset(context, 131);
        EditorPaneTesting.moveCaret(context, 124);
        EditorPaneTesting.moveOrSelect(context, SwingConstants.EAST, true);
        EditorPaneTesting.moveOrSelect(context, SwingConstants.EAST, false);
        EditorPaneTesting.typeChar(context, 'j');
        EditorPaneTesting.moveOrSelect(context, SwingConstants.NORTH, false);
        EditorPaneTesting.moveOrSelect(context, SwingConstants.NORTH, false);
        EditorPaneTesting.moveOrSelect(context, SwingConstants.SOUTH, true);
        EditorPaneTesting.moveOrSelect(context, SwingConstants.NORTH, false);
        EditorPaneTesting.moveOrSelect(context, SwingConstants.WEST, false);
        EditorPaneTesting.moveOrSelect(context, SwingConstants.SOUTH, false);
        EditorPaneTesting.moveOrSelect(context, SwingConstants.NORTH, true);
        EditorPaneTesting.moveOrSelect(context, SwingConstants.WEST, true);
        EditorPaneTesting.typeChar(context, 'q');
        EditorPaneTesting.moveOrSelect(context, SwingConstants.SOUTH, true);
        EditorPaneTesting.moveOrSelect(context, SwingConstants.WEST, false);
        EditorPaneTesting.performAction(context, pane, DefaultEditorKit.insertTabAction);
        EditorPaneTesting.moveOrSelect(context, SwingConstants.NORTH, true);
        DocumentTesting.undo(context, 1);
        EditorPaneTesting.moveOrSelect(context, SwingConstants.WEST, true);
        EditorPaneTesting.moveOrSelect(context, SwingConstants.EAST, false);
        EditorPaneTesting.moveOrSelect(context, SwingConstants.NORTH, true);
        EditorPaneTesting.moveOrSelect(context, SwingConstants.WEST, true);
        EditorPaneTesting.moveOrSelect(context, SwingConstants.SOUTH, false);
        EditorPaneTesting.moveOrSelect(context, SwingConstants.NORTH, false);
        EditorPaneTesting.moveOrSelect(context, SwingConstants.SOUTH, true);
        EditorPaneTesting.moveOrSelect(context, SwingConstants.SOUTH, false);
        EditorPaneTesting.performAction(context, pane, DefaultEditorKit.insertTabAction);
        DocumentTesting.undo(context, 1);
    }

    public void testUndo750() throws Exception {
        loggingOn();
        RandomTestContainer container = createContainer();
        JEditorPane pane = container.getInstance(JEditorPane.class);
        Document doc = pane.getDocument();
        doc.putProperty("mimeType", "text/plain");
        ViewHierarchyRandomTesting.initRandomText(container);
        RandomTestContainer.Context context = container.context();
        DocumentTesting.insert(context, 0,
"  \naxj \n\n\nm hebkinc  krnb\t\tabce\n\nd\t\n\n \t\talja\nj \t\tabcdef\tcdef \t \n\n\n\tabcdf\t\tq tzaicl  \t\tabcdef\t  \nglanm\nq\n        \nv  nyk\n    \ndy qucjfn\tfh cdk \t\t \nj\nsm\n t\ngqa \nsjj\n\n\ncdef\t\n \t\tpabg\to\nkbcvde\tjs\ny\tfw\nr\n\n\nced"
        );
        EditorPaneTesting.setCaretOffset(context, 131);
        EditorPaneTesting.moveCaret(context, 124);
        DocumentTesting.insert(context, 102, " \t\tabcdef\t");
        EditorPaneTesting.moveOrSelect(context, SwingConstants.EAST, true);
        EditorPaneTesting.moveOrSelect(context, SwingConstants.EAST, false);
        DocumentTesting.insert(context, 103, "k\t\n\n ");
        EditorPaneTesting.typeChar(context, 'j');
        EditorPaneTesting.moveOrSelect(context, SwingConstants.NORTH, false);
        EditorPaneTesting.moveOrSelect(context, SwingConstants.NORTH, false);
        EditorPaneTesting.moveOrSelect(context, SwingConstants.SOUTH, true);
        EditorPaneTesting.moveOrSelect(context, SwingConstants.NORTH, false);
        EditorPaneTesting.moveOrSelect(context, SwingConstants.WEST, false);
        EditorPaneTesting.moveOrSelect(context, SwingConstants.SOUTH, false);
        EditorPaneTesting.moveOrSelect(context, SwingConstants.NORTH, true);
        EditorPaneTesting.moveOrSelect(context, SwingConstants.WEST, true);
        EditorPaneTesting.typeChar(context, 'q');
        EditorPaneTesting.moveOrSelect(context, SwingConstants.SOUTH, true);
        DocumentTesting.insert(context, 64, " \t\tabcdef\t");
        DocumentTesting.remove(context, 121, 1);
        DocumentTesting.insert(context, 52, "r");
        EditorPaneTesting.moveOrSelect(context, SwingConstants.WEST, false);
        EditorPaneTesting.performAction(context, pane, DefaultEditorKit.insertTabAction);
        EditorPaneTesting.moveOrSelect(context, SwingConstants.NORTH, true);
        DocumentTesting.redo(context, 3);
        DocumentTesting.undo(context, 1);
        EditorPaneTesting.moveOrSelect(context, SwingConstants.WEST, true);
        EditorPaneTesting.moveOrSelect(context, SwingConstants.EAST, false);
        EditorPaneTesting.moveOrSelect(context, SwingConstants.NORTH, true);
        EditorPaneTesting.moveOrSelect(context, SwingConstants.WEST, true);
        EditorPaneTesting.moveOrSelect(context, SwingConstants.SOUTH, false);
        EditorPaneTesting.moveOrSelect(context, SwingConstants.NORTH, false);
        EditorPaneTesting.moveOrSelect(context, SwingConstants.SOUTH, true);
        EditorPaneTesting.moveOrSelect(context, SwingConstants.SOUTH, false);
        EditorPaneTesting.performAction(context, pane, DefaultEditorKit.insertTabAction);
        DocumentTesting.undo(context, 1);
    }

    public void testLengthyEdit() throws Exception {
        loggingOn();
        RandomTestContainer container = createContainer();
        JEditorPane pane = container.getInstance(JEditorPane.class);
        Document doc = pane.getDocument();
        doc.putProperty("mimeType", "text/plain");
        ViewHierarchyRandomTesting.initRandomText(container);
        final RandomTestContainer.Context context = container.context();
        for (int i = 0; i < 100; i++) {
            DocumentTesting.insert(context, 0, "abcdefghijklmnopqrst\n");
        }
        DocumentTesting.setSameThreadInvoke(context, true); // Otherwise runAtomic() would lock forever waiting for EDT
        final BaseDocument bdoc = (BaseDocument) doc;
        SwingUtilities.invokeAndWait(new Runnable() {
            private boolean inRunAtomic;
            @Override
            public void run() {
                if (!inRunAtomic) {
                    inRunAtomic = true;
                    bdoc.runAtomic(this);
                    return;
                }

                try {
                    for (int i = 0; i < 100; i++) {
//                        DocumentTesting.insert(context, i * 22 + 3, "a\n");
                        DocumentTesting.remove(context, i * 20 + 2, 1);
                    }
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }
        });
        DocumentTesting.setSameThreadInvoke(context, false);
        EditorPaneTesting.moveOrSelect(context, SwingConstants.NORTH, false);
//        DocumentTesting.insert(context, 50, "x\nab\n");
//        EditorPaneTesting.setCaretOffset(context, 20);
    }
    
    private static final String PROP_HL_EXCLUDES = "HighlightsLayerExcludes"; //NOI18N
    private static void excludeHighlights(JEditorPane pane) {
        // Exclude certain highlights to test intra-line view rebuilds
        pane.putClientProperty(PROP_HL_EXCLUDES, ".*CaretRowHighlighting$");
    }

    public void testRandomModsPlainText() throws Exception {
        loggingOn();
        RandomTestContainer container = createContainer();
        final JEditorPane pane = container.getInstance(JEditorPane.class);
        final Document doc = pane.getDocument();
        doc.putProperty("mimeType", "text/plain");
        ViewHierarchyRandomTesting.initRandomText(container);
        ViewHierarchyRandomTesting.addRound(container).setOpCount(OP_COUNT);
        ViewHierarchyRandomTesting.testFixedScenarios(container);
        container.run(1271950385168L); // Failed at op=750
//        container.run(1270806278503L);
//        container.run(1270806786819L);
//        container.run(1270806387223L);
//        container.run(1271372510390L);

//        RandomTestContainer.Context context = container.context();
//        DocumentTesting.undo(context, 2);
//        DocumentTesting.redo(context, 2);
        // Simulate tooltip pane
        final JEditorPane[] toolTipPaneRef = new JEditorPane[1];
        final BadLocationException[] excRef = new BadLocationException[1];
        final JFrame[] toolTipFrameRef = new JFrame[1];
        Runnable tooltipRun = new Runnable() {
            @Override
            public void run() {
                JEditorPane toolTipPane = new JEditorPane();
                toolTipPaneRef[0] = toolTipPane;
                toolTipPane.setEditorKit(pane.getEditorKit());
                try {
                    toolTipPane.putClientProperty("document-view-start-position", doc.createPosition(4));
                    toolTipPane.putClientProperty("document-view-end-position", doc.createPosition(20));
                } catch (BadLocationException ex) {
                    excRef[0] = ex;
                }
                toolTipPane.setDocument(doc);
                JFrame toolTipFrame = new JFrame("ToolTip Frame");
                toolTipFrameRef[0] = toolTipFrame;
                toolTipFrame.getContentPane().add(new JScrollPane(toolTipPane));
                toolTipFrame.setSize(100, 100);
                toolTipFrame.setVisible(true);
            }
        };
        SwingUtilities.invokeAndWait(tooltipRun);
        if (excRef[0] != null) {
            throw new IllegalStateException(excRef[0]);
        }

        container.run(0L); // Test random ops
        // Exclude caret row highlighting
        excludeHighlights(pane);
        container.run(0L); // Re-run test

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (toolTipFrameRef[0] != null) {
                    toolTipFrameRef[0].setVisible(false);
                }
            }
        });
    }

    public void testRandomModsJava() throws Exception {
        loggingOn();
        RandomTestContainer container = createContainer();
        final JEditorPane pane = container.getInstance(JEditorPane.class);
        final Document doc = pane.getDocument();
        doc.putProperty(Language.class, JavaTokenId.language());
        doc.putProperty("mimeType", "text/x-java");
        ViewHierarchyRandomTesting.initRandomText(container);
        ViewHierarchyRandomTesting.addRound(container).setOpCount(OP_COUNT);
        ViewHierarchyRandomTesting.testFixedScenarios(container);
        container.run(1271946202898L);
        
        // Simulate tooltip pane
        final JEditorPane[] toolTipPaneRef = new JEditorPane[1];
        final BadLocationException[] excRef = new BadLocationException[1];
        final JFrame[] toolTipFrameRef = new JFrame[1];
        Runnable tooltipRun = new Runnable() {
            @Override
            public void run() {
                JEditorPane toolTipPane = new JEditorPane();
                toolTipPaneRef[0] = toolTipPane;
                toolTipPane.setEditorKit(pane.getEditorKit());
                try {
                    toolTipPane.putClientProperty("document-view-start-position", doc.createPosition(4));
                    toolTipPane.putClientProperty("document-view-end-position", doc.createPosition(20));
                } catch (BadLocationException ex) {
                    excRef[0] = ex;
                }
                toolTipPane.setDocument(doc);
                JFrame toolTipFrame = new JFrame("ToolTip Frame");
                toolTipFrameRef[0] = toolTipFrame;
                toolTipFrame.getContentPane().add(new JScrollPane(toolTipPane));
                toolTipFrame.setSize(100, 100);
                toolTipFrame.setVisible(true);
            }
        };
        SwingUtilities.invokeAndWait(tooltipRun);
        if (excRef[0] != null) {
            throw new IllegalStateException(excRef[0]);
        }

        
        container.run(1290550667174L);
        container.run(0L); // Test random ops
        // Exclude caret row highlighting
        excludeHighlights(pane);
        container.run(0L); // Re-run test

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (toolTipFrameRef[0] != null) {
                    toolTipFrameRef[0].setVisible(false);
                }
            }
        });
    }

    public void testRandomModsJavaSeed1() throws Exception {
        loggingOn();
        RandomTestContainer container = createContainer();
        JEditorPane pane = container.getInstance(JEditorPane.class);
        Document doc = pane.getDocument();
        doc.putProperty(Language.class, JavaTokenId.language());
        doc.putProperty("mimeType", "text/x-java");
        ViewHierarchyRandomTesting.initRandomText(container);
        ViewHierarchyRandomTesting.addRound(container).setOpCount(OP_COUNT);
        ViewHierarchyRandomTesting.testFixedScenarios(container);
        container.run(1286796912276L);
        // Exclude caret row highlighting
        excludeHighlights(pane);
        container.run(1286796912276L);
    }
    
    public void testParralelMods() throws Exception {
        loggingOn();
        RandomTestContainer container = createContainer();
        JEditorPane pane = container.getInstance(JEditorPane.class);
        final Document doc = pane.getDocument();
        doc.putProperty("mimeType", "text/x-java");
        final RandomTestContainer.Context context = container.context();
        DocumentTesting.setSameThreadInvoke(context, true); // Do not post to EDT
        // (Done automatically) Logger.getLogger("org.netbeans.editor.BaseDocument.EDT").setLevel(Level.OFF);
//        ViewHierarchyRandomTesting.disableHighlighting(container);
        int opCount = 100;
        final int throughput = 5; // How many truly parallel invocations
        RequestProcessor rp = new RequestProcessor("Doc-Mod", throughput, false, false);
        Task task = null;
        for (int i = opCount - 1; i >= 0; i--) {
            task = rp.post(new Runnable() {
                @Override
                public void run() {
                    // make sure insert won't fail for multiple threads
                    int offset = Math.max(doc.getLength() - throughput, 0);
                    try {
                        DocumentTesting.insert(context, offset, "ab");
                        DocumentTesting.remove(context, offset, 1);
                    } catch (Exception ex) {
                        throw new IllegalStateException(ex);
                    }
                }
            });
        }
        task.waitFinished();
    }

    @org.openide.util.lookup.ServiceProvider(service = org.netbeans.spi.editor.mimelookup.MimeDataProvider.class)
    public static final class HighlightsLayerProvider implements MimeDataProvider {

        private static final Map<MimePath, Lookup> mime2Lookup = new HashMap<MimePath, Lookup>();

        static void add(String mimePath, Object... instances) {
            InstanceContent content = new InstanceContent();
            content.set(Arrays.asList(instances), null);
            Lookup lkp = new AbstractLookup(content);
            mime2Lookup.put(MimePath.get(mimePath), lkp);
        }

        @Override
        public Lookup getLookup(MimePath mimePath) {
            return mime2Lookup.get(mimePath);
        }

    }

    static final class HLFactory implements HighlightsLayerFactory {

        @Override
        public HighlightsLayer[] createLayers(Context context) {
            return new HighlightsLayer[] { HighlightsLayer.create(
                    "BeyondDocEndHighlightsLayer",
                    ZOrder.DEFAULT_RACK,
                    false,
                    new BeyondDocEndHighlightsLayer(context.getDocument()))
            };
        }
        
    }
    
    private static final class BeyondDocEndHighlightsLayer implements HighlightsContainer {
        
        private final Document doc;
        
        private final EventListenerList listenerList = new EventListenerList();

        public BeyondDocEndHighlightsLayer(Document doc) {
            this.doc = doc;
        }
        
        @Override
        public HighlightsSequence getHighlights(int startOffset, int endOffset) {
            return new HS(doc);
        }

        @Override
        public void addHighlightsChangeListener(HighlightsChangeListener listener) {
            listenerList.add(HighlightsChangeListener.class, listener);
        }

        @Override
        public void removeHighlightsChangeListener(HighlightsChangeListener listener) {
            listenerList.remove(HighlightsChangeListener.class, listener);
        }
        
    }
    
    private static final class HS implements HighlightsSequence {

        private final Document doc;
        
        private boolean done;

        public HS(Document doc) {
            this.doc = doc;
        }

        @Override
        public boolean moveNext() {
            if (!done) {
                done = true;
                return true;
            }
            return false;
        }

        @Override
        public int getStartOffset() {
            return 0;
        }

        @Override
        public int getEndOffset() {
            return doc.getLength() + 5; // Beyond end of doc intentionally
        }

        @Override
        public AttributeSet getAttributes() {
            SimpleAttributeSet attrs = new SimpleAttributeSet();
            attrs.addAttribute(StyleConstants.Foreground, Color.red);
            return attrs;
        }

    }

}
