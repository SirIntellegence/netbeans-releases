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

package org.netbeans.modules.gsf.testrunner.api;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeListener;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import javax.accessibility.AccessibleContext;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import org.openide.awt.MouseUtils;
import org.openide.awt.TabbedPaneFactory;
import org.openide.util.Exceptions;
import org.openide.util.HelpCtx;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;
import org.openide.windows.IOContainer;
import org.openide.windows.IOContainer.CallBacks;

/**
 *
 * @author Marian Petras, Erno Mononen
 */
final class ResultWindow extends TopComponent {

    /** unique ID of <code>TopComponent</code> (singleton) */
    private static final String ID = "gsf-testrunner-results";              //NOI18N
    /**
     * instance/singleton of this class
     *
     * @see  #getInstance
     */
    private static WeakReference<ResultWindow> instance = null;

    private Map<String,JSplitPane> viewMap = new HashMap<String,JSplitPane>();

    private final JTabbedPane tabPane;
    private JPopupMenu pop;
    private PopupListener popL;
    private CloseListener closeL;

    /**
     * Returns a singleton of this class.
     *
     * @return  singleton of this class
     */
    public static ResultWindow getInstance() {
        final ResultWindow[] result = new ResultWindow[1];
        if (!SwingUtilities.isEventDispatchThread()) {
            try {
                SwingUtilities.invokeAndWait(new Runnable() {

                    public void run() {
                        result[0] = getResultWindow();
                    }
                });
            } catch (InterruptedException ex) {
                Exceptions.printStackTrace(ex);
            } catch (InvocationTargetException ex) {
                Exceptions.printStackTrace(ex);
            }
        } else {
            result[0] = getResultWindow();
        }
        return result[0];
    }


    private static synchronized ResultWindow getResultWindow() {
        ResultWindow result = (ResultWindow) WindowManager.getDefault().findTopComponent(ID);
        if (result == null) {
            result = getDefault();
        }
        return result;
    }

    /**
     * Singleton accessor reserved for the window system only.
     * The window system calls this method to create an instance of this
     * <code>TopComponent</code> from a <code>.settings</code> file.
     * <p>
     * <em>This method should not be called anywhere except from the window
     * system's code.</em>
     *
     * @return  singleton - instance of this class
     */
    public static synchronized ResultWindow getDefault() {
        ResultWindow window = (instance != null) ? instance.get() : null;
        if (window == null) {
            window = new ResultWindow();
            window.initActions();
            instance = new WeakReference<ResultWindow>(window);
        }
        return window;
    }

    private void initActions() {
        ActionMap actions = getActionMap();
        actions.put("jumpNext", new PrevNextFailure(true));  //NOI18N
        actions.put("jumpPrev", new PrevNextFailure(false));  //NOI18N
    }

    /** */
//    private JSplitPane view;
//    private Object lookup;

    /** Creates a new instance of ResultWindow */
    public ResultWindow() {
        super();
        setFocusable(true);
        setLayout(new BorderLayout());

        setName(ID);
        setDisplayName(NbBundle.getMessage(ResultWindow.class,
                                           "TITLE_TEST_RESULTS"));      //NOI18N
        setIcon(ImageUtilities.loadImage(
                "org/netbeans/modules/gsf/testrunner/resources/testResults.png",//NOI18N
	        true));

        AccessibleContext accessibleContext = getAccessibleContext();
        accessibleContext.setAccessibleName(
                NbBundle.getMessage(getClass(), "ACSN_TestResults"));   //NOI18N
        accessibleContext.setAccessibleDescription(
                NbBundle.getMessage(getClass(), "ACSD_TestResults"));   //NOI18N

        pop = new JPopupMenu();
        pop.add(new Close());
        pop.add(new CloseAll());
        pop.add(new CloseAllButCurrent());
        popL = new PopupListener();
        closeL = new CloseListener();

        tabPane = TabbedPaneFactory.createCloseButtonTabbedPane();
        tabPane.setMinimumSize(new Dimension(0, 0));
        tabPane.addMouseListener(popL);
        tabPane.addPropertyChangeListener(closeL);
        add(tabPane);
    }

    /**
     */
    public void addDisplayComponent(JSplitPane displayComp, Lookup l) {
        assert EventQueue.isDispatchThread();
        String key = displayComp.getToolTipText();

        JSplitPane prevComp = viewMap.put(key, displayComp);
        if (prevComp == null){
            addView(displayComp);
        }else{
            replaceView(prevComp, displayComp);
        }
        revalidate();
    }

    /**
     */
    private void addView(final JSplitPane view) {
        assert EventQueue.isDispatchThread();

        view.setMinimumSize(new Dimension(0, 0));
        tabPane.addTab(view.getToolTipText(), view);
        tabPane.setSelectedComponent(view);
        tabPane.validate();
    }

    private void replaceView(JSplitPane oldView, JSplitPane newView){
        for (int i=0; i < tabPane.getTabCount(); i++){
            if (oldView.equals(tabPane.getComponentAt(i))){
                tabPane.setComponentAt(i, newView);
                tabPane.setSelectedComponent(newView);
                tabPane.validate();
                copyFilterMask(oldView, newView);
                continue;
            }
        }
    }
    
    private void copyFilterMask(JSplitPane oldView, JSplitPane newView) {
        StatisticsPanel oldSP = (StatisticsPanel)oldView.getLeftComponent();
        StatisticsPanel newSP = (StatisticsPanel)newView.getLeftComponent();
        newSP.copyFilterMask(oldSP);
    }

    /**
     */
    public void promote() {
        assert EventQueue.isDispatchThread();

        open();
        requestVisible();
        // don't activate, see #145382
        //requestActive();
    }

    /**
     * Sets the layout orientation of the contained result pane.
     *
     * @param orientation the orientation (see {@link JSplitPane#VERTICAL_SPLIT}
     * and {@link JSplitPane#HORIZONTAL_SPLIT}) to set.
     */
    public void setOrientation(int orientation) {
        for(JSplitPane view: viewMap.values()){
            if (view.getOrientation() != orientation) {
                view.setOrientation(orientation);
            }
        }
    }

    /**
     */
    @Override
    protected String preferredID() {
        return ID;
    }

    /**
     */
    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx(getClass());
    }

    /**
     */
    @Override
    public int getPersistenceType() {
        return TopComponent.PERSISTENCE_ALWAYS;
    }
/*
    @Override
    public Lookup getLookup() {
        if (lookup == null) {
            return super.getLookup();
        }
        if (lookup instanceof Reference) {
            Object l = ((Reference) lookup).get();

            if (l instanceof Lookup) {
                return (Lookup) l;
            }
        }
        return Lookup.EMPTY;
    }
*/
    /**
     * Resolves to the {@linkplain #getDefault default instance} of this class.
     *
     * This method is necessary for correct functinality of window system's
     * mechanism of persistence of top components.
     */
    private Object readResolve() throws java.io.ObjectStreamException {
        return ResultWindow.getDefault();
    }

    private boolean activated;
    private JComponent outputComp;
    private JComponent outputTab;
    private IOContainer ioContainer;

    IOContainer getIOContainer() {
        if (ioContainer == null) {
            ioContainer = IOContainer.create(new IOContainerImpl());
        }
        return ioContainer;
    }

    void setOutputComp(JComponent comp) {
        outputComp = comp;
    }

    @Override
    protected void componentActivated() {
        activated = true;
    }

    @Override
    protected void componentDeactivated() {
        activated = false;
    }

    private JSplitPane getCurrentResultView(){
        return (JSplitPane)tabPane.getSelectedComponent();
    }

    private void closeAll(boolean butCurrent) {
        Component current = tabPane.getSelectedComponent();
        Component[] c =  tabPane.getComponents();
        for (int i = 0; i< c.length; i++) {
            if (butCurrent && c[i]==current) {
                continue;
            }
            if(c[i] instanceof JSplitPane) {
                removeView((JSplitPane) c[i]);
            }
        }
    }

    @Override
    protected void componentClosed() {
        closeAll(false);
        super.componentClosed();
    }

    private void removeView(JSplitPane view) {
        // probably it's need to stop testing if in progress?
        
        if (view == null) {
            view = (JSplitPane) tabPane.getSelectedComponent();
            if (view == null) {
                return;
            }
        }
        tabPane.remove(view);
        viewMap.remove(view.getToolTipText());

        validate();
    }

    private class IOContainerImpl implements IOContainer.Provider {

        public void remove(JComponent comp) {
            outputTab = null;
            outputComp.remove(comp);
            ResultWindow.getInstance().close();
        }

        public void select(JComponent comp) {
        }

        public JComponent getSelected() {
            return outputTab;
        }

        public boolean isActivated() {
            return activated;
        }

        public void open() {
        }

        public void requestActive() {
        }

        public void requestVisible() {
        }

        public void setIcon(JComponent comp, Icon icon) {
        }

        public void setTitle(JComponent comp, String name) {
        }

        public void setToolTipText(JComponent comp, String name) {
        }

        public void add(JComponent comp, CallBacks cb) {
            outputTab = comp;
            outputComp.add(comp);
        }

        public void setToolbarActions(JComponent comp, Action[] toolbarActions) {
        }

        public boolean isCloseable(JComponent comp) {
            return false;
        }
    }

    private final class PrevNextFailure extends AbstractAction {

        private final boolean next;

        public PrevNextFailure(boolean next) {
            this.next = next;
        }

        public void actionPerformed(ActionEvent e) {
            JSplitPane view = getCurrentResultView();
            if (view == null || !(view.getLeftComponent() instanceof StatisticsPanel)) {
                return;
            }
            StatisticsPanel statisticsPanel = (StatisticsPanel) view.getLeftComponent();
            if (next) {
                statisticsPanel.selectNextFailure();
            } else {
                statisticsPanel.selectPreviousFailure();
            }
        }
    }

    private class Close extends AbstractAction {
        public Close() {
            super(NbBundle.getMessage(ResultWindow.class, "LBL_CloseWindow"));  //NOI18N
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            removeView(null);
        }
    }

    private final class CloseAll extends AbstractAction {
        public CloseAll() {
            super(NbBundle.getMessage(ResultWindow.class, "LBL_CloseAll"));  //NOI18N
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            closeAll(false);
        }
    }

    private class CloseAllButCurrent extends AbstractAction {
        public CloseAllButCurrent() {
            super(NbBundle.getMessage(ResultWindow.class, "LBL_CloseAllButCurrent"));  //NOI18N
        }
        public void actionPerformed(ActionEvent e) {
            closeAll(true);
        }
    }

    private class CloseListener implements PropertyChangeListener {
        public void propertyChange(java.beans.PropertyChangeEvent evt) {
            if (TabbedPaneFactory.PROP_CLOSE.equals(evt.getPropertyName())) {
                removeView((JSplitPane) evt.getNewValue());
            }
        }
    }

    private class PopupListener extends MouseUtils.PopupMouseAdapter {
        protected void showPopup (MouseEvent e) {
            pop.show(ResultWindow.this, e.getX(), e.getY());
        }
    }
}
