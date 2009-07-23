/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2009 Sun Microsystems, Inc. All rights reserved.
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
 * Portions Copyrighted 2009 Sun Microsystems, Inc.
 */

package org.netbeans.modules.dlight.visualizers;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import org.netbeans.modules.dlight.spi.visualizer.Visualizer;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;
import org.openide.util.ImageUtilities;
import org.netbeans.api.settings.ConvertAsProperties;
import org.netbeans.modules.dlight.spi.visualizer.VisualizerContainer;
import org.netbeans.modules.dlight.visualizers.threadmap.ThreadMapVisualizer;

/**
 * Top component which displays something.
 */
@ConvertAsProperties(
    dtd="-//org.netbeans.modules.dlight.visualizers//ThreadMap//EN",
    autostore=false
)
public final class ThreadMapTopComponent extends TopComponent implements VisualizerContainer {

    private static ThreadMapTopComponent instance;
    /** path to the icon used by the component and its open action */
    static final String ICON_PATH = "org/netbeans/modules/dlight/visualizers/resources/threadsWindow.png"; //NOI18N

    private static final String PREFERRED_ID = "ThreadMapTopComponent"; //NOI18N
    private String currentToolName;
    private JComponent viewComponent;

    public ThreadMapTopComponent() {
        initComponents();
        setName(NbBundle.getMessage(ThreadMapTopComponent.class, "CTL_ThreadMapTopComponent")); //NOI18N
        setToolTipText(NbBundle.getMessage(ThreadMapTopComponent.class, "HINT_ThreadMapTopComponent")); //NOI18N
        setIcon(ImageUtilities.loadImage(ICON_PATH, true));

    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 300, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
    /**
     * Gets default instance. Do not use directly: reserved for *.settings files only,
     * i.e. deserialization routines; otherwise you could get a non-deserialized instance.
     * To obtain the singleton instance, use {@link #findInstance}.
     */
    public static synchronized ThreadMapTopComponent getDefault() {
        if (instance == null) {
            instance = new ThreadMapTopComponent();
        }
        return instance;
    }

    /**
     * Obtain the ThreadMapTopComponent instance. Never call {@link #getDefault} directly!
     */
    public static synchronized ThreadMapTopComponent findInstance() {
        TopComponent win = WindowManager.getDefault().findTopComponent(PREFERRED_ID);
        if (win == null) {
            Logger.getLogger(ThreadMapTopComponent.class.getName()).warning(
                    "Cannot find " + PREFERRED_ID + " component. It will not be located properly in the window system."); //NOI18N
            return getDefault();
        }
        if (win instanceof ThreadMapTopComponent) {
            return (ThreadMapTopComponent) win;
        }
        Logger.getLogger(ThreadMapTopComponent.class.getName()).warning(
                "There seem to be multiple components with the '" + PREFERRED_ID + //NOI18N
                "' ID. That is a potential source of errors and unexpected behavior."); //NOI18N
        return getDefault();
    }

    @Override
    public int getPersistenceType() {
        return TopComponent.PERSISTENCE_NEVER;
    }

    @Override
    public void componentOpened() {
    }

    @Override
    public void componentClosed() {
        if (viewComponent instanceof ThreadMapVisualizer) {
            ((ThreadMapVisualizer)viewComponent).shutdown();
        }
    }

    void writeProperties(java.util.Properties p) {
        // better to version settings since initial version as advocated at
        // http://wiki.apidesign.org/wiki/PropertyFiles
        p.setProperty("version", "1.0"); //NOI18N
        // TODO store your settings
    }

    Object readProperties(java.util.Properties p) {
        ThreadMapTopComponent singleton = ThreadMapTopComponent.getDefault();
        singleton.readPropertiesImpl(p);
        return singleton;
    }

    private void readPropertiesImpl(java.util.Properties p) {
        String version = p.getProperty("version"); //NOI18N
        // TODO read your settings according to their version
    }

    @Override
    protected String preferredID() {
        return PREFERRED_ID;
    }

    public void addVisualizer(String toolName, Visualizer view) {
        setContent(toolName, view.getComponent());
        view.refresh();
    }

    public void setContent(String toolName, JComponent component) {
        if (currentToolName != null && currentToolName.equals(toolName) &&
            viewComponent == component) {
            return;//DO NOTHING
        }
        currentToolName = toolName;
        removeAll();
        setLayout(new BorderLayout());
        viewComponent = component;
        add(viewComponent, BorderLayout.CENTER);
        setName(toolName);
        setToolTipText(toolName);
        validate();
        repaint();
    }

    public void addContent(String toolName, JComponent component) {
        if (currentToolName == null || !currentToolName.equals(toolName) ||
            viewComponent != component) {
            currentToolName = toolName;
            removeAll();
            setLayout(new BorderLayout());
        }
        viewComponent = component;
        add(viewComponent);
        setName(toolName);
        setToolTipText(toolName);
        validate();
        repaint();
    }

    public void removeVisualizer(final Visualizer view) {
        if (EventQueue.isDispatchThread()){
            closePerformanceMonitor(view);
        }else{
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    closePerformanceMonitor(view);
                }
            });
        }
    }

    public void showup() {
        open();
        requestActive();
        if (viewComponent instanceof ThreadMapVisualizer) {
            ThreadMapVisualizer view = (ThreadMapVisualizer) viewComponent;
            view.startup();
        }
    }

    private void closePerformanceMonitor(Visualizer view) {
        if (viewComponent != view.getComponent()) {
            return;
        }
        if (viewComponent instanceof ThreadMapVisualizer) {
            ((ThreadMapVisualizer)view).shutdown();
        }
        remove(view.getComponent());
        setName(NbBundle.getMessage(ThreadMapTopComponent.class, "ThreadMapDetailes")); //NOI18N
        repaint();
    }
}
