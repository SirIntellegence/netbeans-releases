/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2016 Oracle and/or its affiliates. All rights reserved.
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
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2016 Sun Microsystems, Inc.
 */
package org.netbeans.modules.jshell.editor;

import com.sun.source.util.Trees;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;
import javax.swing.text.Document;
import javax.swing.text.NavigationFilter;
import javax.swing.text.Position;
import org.netbeans.api.editor.caret.EditorCaret;
import org.netbeans.api.editor.caret.MoveCaretsOrigin;
import org.netbeans.api.editor.document.LineDocument;
import org.netbeans.api.editor.document.LineDocumentUtils;
import org.netbeans.modules.java.preprocessorbridge.spi.WrapperFactory;
import org.netbeans.modules.jshell.env.JShellEnvironment;
import org.netbeans.modules.jshell.env.ShellEvent;
import org.netbeans.modules.jshell.env.ShellListener;
import org.netbeans.modules.jshell.env.ShellRegistry;
import org.netbeans.modules.jshell.env.ShellStatus;
import org.netbeans.modules.jshell.model.ConsoleEvent;
import org.netbeans.modules.jshell.model.ConsoleListener;
import org.netbeans.modules.jshell.model.ConsoleModel;
import org.netbeans.modules.jshell.model.ConsoleSection;
import org.netbeans.modules.jshell.model.Rng;
import org.netbeans.modules.jshell.support.ShellSession;
import org.netbeans.spi.editor.caret.CascadingNavigationFilter;
import org.netbeans.spi.editor.caret.NavigationFilterBypass;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.text.CloneableEditor;
import org.openide.text.CloneableEditorSupport;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;

/**
 *
 * @author sdedic
 */
@TopComponent.Description(
        preferredID = "REPLTopComponent",
        //iconBase="SET/PATH/TO/ICON/HERE",
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(mode = "editor", openAtStartup = false)
@ActionID(category = "Window", id = "org.netbeans.modules.java.repl.REPLTopComponent")
@ActionReference(path = "Menu/Window" /*, position = 333 */)
@NbBundle.Messages({
    "CTL_REPLAction=Java REPL",
    "CTL_REPLTopComponent=Java REPL",
    "CTL_REPLTopComponentProject=Java REPL for {0}",
    "HINT_REPLTopComponent=This is a Java REPL window"
})
public class ConsoleEditor extends CloneableEditor {

    private ShellSession session;
    private CL cl;
    
    public ConsoleEditor(CloneableEditorSupport support) {
        super(support);
    }

    @Override
    protected void componentOpened() {
        super.componentOpened();
    }
    
    private volatile JEditorPane pane;

    @Override
    protected void componentShowing() {
        super.componentShowing();
        if (session == null) {
            initialize();
            if (session == null) {
                pane.addPropertyChangeListener("document", 
                        (e) -> initialize());
            }
        } else {
            updateHourglass();
        }
    }
    

    @Override
    protected void componentHidden() {
        removeProgressIndicator();
        super.componentHidden();
    }
    
    private void updateHourglass() {
        ShellStatus status = env.getStatus();

        if (status == ShellStatus.STARTING) {
            showProgressIndicator(prepareInitPanel(), 0);
            return;
        } else if (status == ShellStatus.EXECUTE) {
            ConsoleModel model = session.getModel();
            ConsoleSection e = model.getExecutingSection();
            if (e != null) {
                showHourglass(e.getEnd());
                return;
            }
        } 
        removeProgressIndicator();
    }
    

    @Override
    protected void componentClosed() {
        removeProgressIndicator();
        super.componentClosed();
        pane = null;
        if (cloneableEditorSupport().getOpenedPanes() == null && session != null) {
            try {
                // terminate the JShell
                session.getEnv().shutdown();
            } catch (Exception ex) {
                Exceptions.printStackTrace(ex);
            }
        }
    }
    
    private JShellEnvironment env;
    
    private volatile boolean detached;
    
    private synchronized void detachFromSession() {
        session.getModel().removeConsoleListener(cl);
        cl = null;
        detached = true;
    }
    
    private void initialize() {
        assert SwingUtilities.isEventDispatchThread();
        
        Document d = getEditorPane().getDocument();
        ShellSession s  = ShellSession.get(d);
        final JShellEnvironment env = ShellRegistry.get().getOwnerEnvironment(s.getConsoleFile());

        if (s == null || s == this.session) {
            // some default document, not interesting
            return;
        }
        if (s != this.session && this.session != null) {
            detachFromSession();
        }
        this.session = s;

        synchronized (this) {
            detached = false;
            cl = new CL();
            session.getModel().addConsoleListener(cl);
        }
        if (env != null && env != this.env) {
            this.env = env;
            env.addShellListener(cl);
        }
        
        // post in order to synchronize after initial shell startup
        session.post(() -> {
            SwingUtilities.invokeLater(this::initialResetCaret);
        });
        
        (pane = getEditorPane()).setNavigationFilter(new NavFilter());
        
        d.putProperty(WrapperFactory.class, new WrapperFactory() {
            @Override
            public Trees wrapTrees(Trees trees) {
                return new CompletionFilter(trees);
            }
            
        });
        SwingUtilities.invokeLater(this::updateHourglass);
    }
    
    private void initialResetCaret() {
        resetEditableArea(true);
    }

    private void resetEditableArea(boolean caret) {
        if (pane == null || pane.getDocument() == null) {
            // probably closed
            return;
        }
        
        Document document = pane.getDocument();
        // may block/delay, check detached inside.
        document.render(() -> {
            if (detached) {
                return;
            }
            final LineDocument ld = LineDocumentUtils.as(document, LineDocument.class);
            if (ld == null) {
                return;
            }
            ConsoleModel model = session.getModel();
            ConsoleSection input = model.getInputSection();
            if (input != null) {
                int commandStart = input.getPartBegin();
    //            pane.setCaretPosition(commandStart);
                if (commandStart > 0 && commandStart < document.getLength()) {
                    return;
                }
                if (caret) {
                    pane.setCaretPosition(commandStart);
                }
            }
        });
    }
    
    /**
     * Modifies the movement of the caret ot strongly prefer movement within the
     * editable section.
     * If the caret is currently in non-editable section of the console, the filter does nothing
     * When in editable section, and the filter would escape it (either leave input section
     * entirely, or go to a prompt part of it), the filter will catch the caret and
     * <ul>
     * <li>if the caret is already at the first or last row of the input section, pass the navigation on.
     * <li>If the caret goes up or down, magicPosition is used to compute offset at the first/last
     * line of the input section; caret will go to that place.
     * </ul>
     * <ul>
     * <li>if the caret would enter the prompt but remain on the line (i.e. left), the caret will be moved at
     * the end of the preceding line, as if movement was made at the beginning of line.
     * <li>if the caret goes to the start of prompt (begin-line), nothing happens.
     * </ul>
     */
    private class NavFilter extends CascadingNavigationFilter {

        @Override
        public void moveDot(FilterBypass fb, int dot, Position.Bias bias) {
            if (handle(fb, dot, bias, true)) {
                super.moveDot(fb, dot, bias);
            }
        }
        
        private void setMoveDot(FilterBypass fb, int dot, Position.Bias bias, boolean setOrMove) {
            if (setOrMove) {
                super.moveDot(fb, dot, bias);
            } else {
                super.setDot(fb, dot, bias);
            }
        }
        
        private boolean handle(FilterBypass fb, int dot, Position.Bias bias, boolean setOrMove) {
            // sadly actions must set up a flag in order to be 'compatible':
            JEditorPane p = pane;
            if (p == null) {
                return true;
            }
            if (p.getClientProperty("navigational.action") == null) {
                if (!(fb instanceof NavigationFilterBypass)) {
                    return true;
                }
                MoveCaretsOrigin orig = ((NavigationFilterBypass)fb).getOrigin();
                if (!MoveCaretsOrigin.DIRECT_NAVIGATION.equals(orig.getActionType())) {
                    return true;
                }
            }
            int current = fb.getCaret().getDot();
            ConsoleSection input = session.getModel().getInputSection();
            if (input == null) {
                return true;
            }
            int s = input.getStart();
            int e = input.getEnd();

            try {
                if (current >= s && current <= e) {
                    // move from within the area
                    if (dot >= s && dot <= e) {
                        return filterWithinSection(input, fb, dot, bias, setOrMove);
                    } else {
                        return filterOutOfSection(input, fb, dot, bias, setOrMove);
                    }
                }
                return true;
            } catch (BadLocationException ex) {
                Exceptions.printStackTrace(ex);
                // permit to navigate
                return true;
            }
        }
        
        /**
         * Handles movement within section. Essentialy just ensures that the caret remains after the prompts.
         * @param fb
         * @param dot
         * @param bias
         * @param setOrMove
         * @return B
         */
        private boolean filterWithinSection(ConsoleSection s, FilterBypass fb, int dot, Position.Bias bias, boolean setOrMove) {
            Caret c = fb.getCaret();
            int curPos = c.getDot();
            LineDocument ld = LineDocumentUtils.as(getEditorPane().getDocument(), LineDocument.class);
            if (ld == null) {
                return true;
            }
            int curLine =  LineDocumentUtils.getLineStart(ld, c.getDot());
            int dotLine =  LineDocumentUtils.getLineStart(ld, dot);
            // I want to avoid positioning into the prompts:
            for (Rng range : s.getPartRanges()) {
                if (range.start > dot) {
                    if (curLine == dotLine) {
                        // if the previous position was also in this range, just relax -
                        // positioned by e.g. mouse, let's do any movement necessary:
                        if (curPos == curLine) {
                            // exception, go to start
                            setMoveDot(fb, range.start, bias, setOrMove);
                            return false;
                        }
                        if (curPos < range.start) {
                            return true;
                        }
                    }
                    if (dot == dotLine) { 
                        // jump at the beginning of the line, assuming HOME
                        if (curPos == range.start) {
                            return true;
                        } else {
                            setMoveDot(fb, range.start, bias, setOrMove);
                            return false;
                        }
                    } else if (dotLine == curLine) {
                        // assuming LEFT, WORD-LEFT etc
                        setMoveDot(fb, Math.max(0, dot = dotLine - 1), bias, setOrMove);
                        return false;
                    }
                    setMoveDot(fb, range.start, bias, setOrMove);
                    return false;
                }
                if (range.end >= dot) {
                    break;
                }
            }
            return true;
        }

        private boolean filterOutOfSection(ConsoleSection s, FilterBypass fb, int dot, Position.Bias bias, boolean setOrMove) throws BadLocationException {
            Caret c = fb.getCaret();
            Point magPosition = c.getMagicCaretPosition();
            int curPos = c.getDot();
            if (magPosition == null) {
                // some other action that move-up / move-down, i.e. start of document / end document, navigation
                if (curPos == s.getPartBegin()|| curPos == s.getEnd()) {
                    return true;
                }
                int nDot;
                if (dot < s.getPartBegin()&& curPos != s.getStart()) {
                    nDot = s.getPartBegin();
                } else if (dot > s.getEnd() && curPos != s.getEnd()) {
                    nDot = s.getEnd();
                } else {
                    return true;
                }
                setMoveDot(fb, nDot, bias, setOrMove);
                return false;
            }
            
            // magicPosition is set, so the move is accross the lines (in Y axis)
            LineDocument ld = LineDocumentUtils.as(getEditorPane().getDocument(), LineDocument.class);
            if (ld == null) {
                return true;
            }
            int curLine =  LineDocumentUtils.getLineStart(ld, c.getDot());
            int ref;
            if (dot < s.getStart()) {
                // measure Y; get X from magicPosition.
                ref  = LineDocumentUtils.getLineStart(ld, s.getStart());
            } else if (dot >= s.getEnd()) {
                ref  = LineDocumentUtils.getLineStart(ld, s.getEnd());
            } else {
                return true;
            }
            if (curLine == ref) {
                return true;
            }
            Rectangle rect = getEditorPane().getUI().modelToView(getEditorPane(), ref);
            rect.x = magPosition.x;
            int pos = getEditorPane().getUI().viewToModel(getEditorPane(), rect.getLocation());
            if (pos < 0) {
                return true;
            }
            // I want to avoid positioning into the prompts:
            for (Rng range : s.getPartRanges()) {
                if (range.start > pos) {
                    pos = range.start;
                    break;
                }
                if (range.end >= pos) {
                    break;
                }
            }
            setMoveDot(fb, pos, bias, setOrMove);
            return false;
        }

        @Override
        public void setDot(FilterBypass fb, int dot, Position.Bias bias) {
            if (handle(fb, dot, bias, false)) {
                super.setDot(fb, dot, bias);
            }
        }
        
    }
    
    private void updateStatusAndActivate() {
        updateHourglass();
        ShellStatus s = env.getStatus();
        if (s == ShellStatus.READY) {
            this.requestVisible();
        }
    }
    
    private class CL implements ConsoleListener, Runnable, PropertyChangeListener, ShellListener {
        private boolean caret;
        private ShellSession saveSession;
        
        private CL() {
            this.saveSession = session;
        }

        @Override
        public void shellStatusChanged(ShellEvent ev) {
            SwingUtilities.invokeLater(ConsoleEditor.this::updateStatusAndActivate);
        }
        
        @Override
        public void shellStarted(ShellEvent ev) {
            if (session != env.getSession()) {
                SwingUtilities.invokeLater(ConsoleEditor.this::initialize);
            }
        }

        @Override
        public void shellShutdown(ShellEvent ev) {
            env.removeShellListener(this);
        }
        
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (evt.getSource() == session &&
                ShellSession.PROP_ACTIVE.equals(evt.getPropertyName())) {
                if (evt.getNewValue() != Boolean.TRUE) {
                    detachFromSession();
                }
            }
        }
        
        public void run() {
            if (detached || session != saveSession) {
                return;
            }
            resetEditableArea(caret);
        }
        
        @Override
        public void sectionCreated(ConsoleEvent e) {
            if (e.containsInput()) {
                caret = true;
                SwingUtilities.invokeLater(this);
            }
        }

        @Override
        public void sectionUpdated(ConsoleEvent e) {
            if (e.containsInput()) {
                caret = false;
                SwingUtilities.invokeLater(this);
            }
        }

        @Override
        public void executing(ConsoleEvent e) {
            SwingUtilities.invokeLater(ConsoleEditor.this::updateHourglass);
        }
        
        public void closed(ConsoleEvent e) {
            detachFromSession();
        }
    }
    
    private JPanel  executeWaitPanel;
    private JLabel  initializingPanel;
    
    private JPanel prepareWaitPanel() {
        if (executeWaitPanel == null) {
            ExecutingGlassPanel p = new ExecutingGlassPanel();
            p.addStopListener(this::stopExecution);
            executeWaitPanel = p;
        }
        return executeWaitPanel;
    }
    
    private JComponent prepareInitPanel() {
        JLabel l = initializingPanel;
        if (initializingPanel == null) {
            l = new JLabel(Bundle.MSG_Initializing(), JLabel.LEADING);
            l.setIcon(new ImageIcon(
                getClass().getClassLoader().getResource(
                    "org/netbeans/modules/jshell/resources/wait16.gif"
                )
            ));
            initializingPanel = l;
        }
        return initializingPanel;
    }
    
    private void stopExecution(ActionEvent e) {
        Action a = pane.getActionMap().get(StopExecutionAction.NAME);
        a.actionPerformed(e);
        /*
        if (session != null) {
            BaseProgressUtils.runOffEventDispatchThread(session::stopExecutingCode, 
                    Bundle.LBL_AttemptingStop(), null, false, 100, 2000);
        }
                */
    }
    
    private JComponent progressIndicator;
    
    private JLayeredPane lastLayeredPane;
    
    private void removeProgressIndicator() {
        if (lastLayeredPane == null || progressIndicator == null) {
            return;
        }
        progressIndicator.setVisible(false);
        JLayeredPane lp = lastLayeredPane;
        lp.remove(progressIndicator);
        lp.repaint();
        progressIndicator = null;
        
    }
    
    private void showProgressIndicator(JComponent indicator, int position) {
        removeProgressIndicator();
        try {
            if (pane == null) {
                return;
            }
            Rectangle r =  pane.getUI().modelToView(pane, position);
            JLayeredPane lp = JLayeredPane.getLayeredPaneAbove(pane);
            if (lp == null) {
                return;
            }
            if (r == null) {
                r = new Rectangle(); // 0:0
            }
            
            lp.add(indicator, JLayeredPane.POPUP_LAYER, 0);
            r.setSize(indicator.getPreferredSize());
            Rectangle converted = SwingUtilities.convertRectangle(
                    pane, r, lp);
            indicator.setBounds(converted);
            indicator.setVisible(true);
            this.lastLayeredPane = lp;
            this.progressIndicator = indicator;
        } catch (BadLocationException ex) {
            
        }

    }
    
    @NbBundle.Messages({
            "MSG_Evaluating=Evaluating, please wait...",
            "MSG_Initializing=Loading and initializing Java Shell. Please wait..."
    })
    private void showHourglass(int offset) {
        showProgressIndicator(prepareWaitPanel(), offset + 1); // the character AFTER the termianting newline; should be on the next line.
    }

    @Override
    public int getPersistenceType() {
        return PERSISTENCE_NEVER;
    }
}
