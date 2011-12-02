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

package org.netbeans.api.project.ui;

import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.api.project.Project;
import org.netbeans.modules.project.uiapi.OpenProjectsTrampoline;
import org.netbeans.modules.project.uiapi.Utilities;
import org.netbeans.spi.project.ui.support.CommonProjectActions;

/**
 * List of projects open in the GUI.
 * <p class="nonnormative">
 * <strong>Warning:</strong> this API is intended only for a limited set of use
 * cases where obtaining a list of all open projects is really the direct goal.
 * For example, you may wish to display a chooser letting the user select a
 * file from among the top-level source folders of any open project.
 * For many cases, however, this API is not the correct approach, so use it as
 * a last resort. Consider <a href="@org-netbeans-api-java-classpath@/org/netbeans/api/java/classpath/GlobalPathRegistry.html"><code>GlobalPathRegistry</code></a>,
 * {@link org.netbeans.spi.project.ui.ProjectOpenedHook},
 * and {@link org.netbeans.spi.project.ui.support.ProjectSensitiveActions}
 * (or {@link org.netbeans.spi.project.ui.support.MainProjectSensitiveActions})
 * first. Only certain operations should actually be aware of which projects
 * are "open"; by default, all project functionality should be available whether
 * it is open or not.
 * </p>
 * @author Jesse Glick, Petr Hrebejk
 */
public final class OpenProjects {
    
    /**
     * Property representing open projects.
     * @see #getOpenProjects
     */
    public static final String PROPERTY_OPEN_PROJECTS = "openProjects"; // NOI18N
    
    /**
     * Property representing main project.
     * @see #getMainProject
     * @since org.netbeans.modules.projectuiapi/1 1.20
     */
    public static final String PROPERTY_MAIN_PROJECT = "MainProject"; // NOI18N
    
    private static OpenProjects INSTANCE = new OpenProjects();

    private static final Logger LOG = Logger.getLogger(OpenProjects.class.getName());
    
    private OpenProjectsTrampoline trampoline;
    
    private OpenProjects() {
        this.trampoline = Utilities.getOpenProjectsTrampoline();
        addPropertyChangeListener( new OpenProjectsListener() );
    }

    /**
     * Get the default singleton instance of this class.
     * @return the default instance
     */
    public static OpenProjects getDefault() {                
        return INSTANCE;
    }
    
    /**
     * Gets a list of currently open projects. 
     * <p>
     * <span class="nonnormative">Since 1.26, the standard implementation
     * handling the list of opened projects, delays their actual loading. First of
     * all the startup of all modules is finished and only then the projects are loaded
     * and opened on background. As soon as and no sooner before 
     * all opened projects are opened, the
     * return value of this method changes and appropriate property change
     * event with <q>PROPERTY_OPEN_PROJECTS</q> is delivered.
     * </span>
     * 
     * @return list of projects currently opened in the IDE's GUI; order not specified
     */
    public Project[] getOpenProjects() {
        return trampoline.getOpenProjectsAPI();
    }

    /**
     * Method to track progress of projects opening and closing. As the
     * opening of a project may take long time, and as there can be multiple
     * projects open at once, it may be necessary to be notified that the process
     * of open project list modification started or that it has
     * finished. This method provides a <q>future</q> that can do that.
     * To find out that the list of open projects is currently modified use:
     * <pre>
     * assert openProjects().isDone() == false;
     * </pre>
     * To wait for the opening/closing to be finished and then obtain the result
     * use:
     * <pre>
     * Project[] current = openProjects().get();
     * </pre>
     * This result is different that a plain call to {@link #getOpenProjects} as
     * that methods returns the current state, whatever it is. While the call through
     * the <q>future</q> awaits for current modifications to finish. As such wait
     * can take a long time one can also wait for just a limited amount of time.
     * However this waiting methods should very likely only be used from dedicated threads,
     * where the wait does not block other essencial operations (read: do not
     * use such methods from AWT or other known threads!).
     * 
     * @return future to track computation of open projects
     * @since 1.27
     */
    public Future<Project[]> openProjects() {
        return trampoline.openProjectsAPI();
    }

    /**
     * Opens given projects.
     * Acquires {@link org.netbeans.api.project.ProjectManager#mutex()} in write mode.
     * <p class="nonnormative">
     * This method is designed for uses such as a logical view's Libraries node to open a dependent
     * project. It can also be used by other project GUI components which need to open certain
     * project(s), e.g. code generation wizards.
     * This should not be used for opening a newly created project; rather,
     * {@link org.openide.WizardDescriptor.InstantiatingIterator#instantiate}
     * should return the project directory.
     * This should also not be used to provide a GUI to open subprojects;
     * {@link CommonProjectActions#openSubprojectsAction} should be used instead.
     * </p>
     * @param projects to be opened. If some of the projects are already opened
     * these projects are ignored. If the list contain duplicates, the duplicated
     * projects are opened just once.
     * @param openSubprojects if true subprojects are also opened
     * @since org.netbeans.modules.projectuiapi/0 1.2
     */
    public void open (Project[] projects, boolean openSubprojects) {
        if (Arrays.asList(projects).contains(null)) {
            throw new NullPointerException();
        }
        trampoline.openAPI (projects,openSubprojects, false);
    }

    /**
     * Opens given projects.
     * Acquires {@link org.netbeans.api.project.ProjectManager#mutex()} in write mode.
     * <p class="nonnormative">
     * This method is designed for uses such as a logical view's Libraries node to open a dependent
     * project. It can also be used by other project GUI components which need to open certain
     * project(s), e.g. code generation wizards.
     * This should not be used for opening a newly created project; rather,
     * {@link org.openide.WizardDescriptor.InstantiatingIterator#instantiate}
     * should return the project directory.
     * This should also not be used to provide a GUI to open subprojects;
     * {@link CommonProjectActions#openSubprojectsAction} should be used instead.
     * </p>
     * @param projects to be opened. If some of the projects are already opened
     * these projects are ignored. If the list contain duplicates, the duplicated
     * projects are opened just once.
     * @param openSubprojects if true subprojects are also opened
     * @param showProgress show progress dialog during the open
     * @since 1.35
     */
    public void open (Project[] projects, boolean openSubprojects, boolean showProgress) {
        if (Arrays.asList(projects).contains(null)) {
            throw new NullPointerException();
        }
        trampoline.openAPI (projects,openSubprojects, showProgress);
    }

    /** Finds out if the project is opened.
     * @param p the project to verify. Can be <code>null</code> in such case
     *    the method return <code>false</code>
     * @return true if this project is among open ones, false otherwise
     * @since 1.34
     */
    public boolean isProjectOpen(Project p) {
        if (p == null) {
            return false;
        }
        for (Project real : getOpenProjects()) {
            if (p.equals(real) || real.equals(p)) {
                LOG.log(Level.FINE, "isProjectOpen => true for {0} @{1} ~ real @{2}", new Object[] {p, p.hashCode(), real.hashCode()});
                return true;
            }
            LOG.log(Level.FINER, "distinct from {0} @{1}", new Object[] {real, real.hashCode()});
        }
        LOG.log(Level.FINE, "isProjectOpen => false for {0} @{1}", new Object[] {p, p.hashCode()});
        return false;
    }

    /**
     * Closes given projects.
     * Acquires {@link org.netbeans.api.project.ProjectManager#mutex()} in the write mode.
     * @param projects to be closed. The non opened project contained in the projects array are ignored.
     * @since org.netbeans.modules.projectuiapi/0 1.2
     */
    public void close (Project[] projects) {
        trampoline.closeAPI (projects);
    }

    /**Retrieves the current main project set in the IDE.
     *
     * <div class="nonnormative">
     * <p><strong>Warning:</strong> the set of usecases that require invoking this method is
     * limited. {@link org.netbeans.spi.project.ui.support.MainProjectSensitiveActions} should
     * be used in favour of this method if possible. In particular, this method should <em>not</em>
     * be used to let the user choose if an action should be run on the main vs. the currently selected project.</p>
     * <p>As a rule of thumb, any code outside of the project system infrastructure which behaves differently
     * depending on the choice of main project should be reviewed critically. All functionality
     * of a project ought to be available regardless of the "main project" flag, which is intended only
     * as a convenient shortcut to permit certain actions (such as <b>Run</b>) to be invoked
     * from a global context on a preselected project.</p>
     * </div>
     *
     * @return the current main project or null if none
     * @since 1.11
     */
    public Project getMainProject() {
        return trampoline.getMainProject();
    }
    
    /**Sets the main project.
     *
     * <div class="nonnormative">
     * <p><strong>Warning:</strong> the set of usecases that require invoking this method is
     * very limited and should be generally avoided if possible. In particular, this method
     * should <em>not</em> be used to mark a project just created by the <b>New Project</b> wizard
     * as the main project.</p>
     * </div>
     *
     * @param project project to set as main project (must be open), or
     *                <code>null</code> to set no project as main.
     * @since 1.11
     */
    public void setMainProject(Project project) {
        trampoline.setMainProject(project);
    }
    
    /**
     * Adds a listener to changes in the set of open projects.
     * As this class is a singleton and is not subject to garbage collection,
     * it is recommended to add only weak listeners, or remove regular listeners reliably.
     * @param listener a listener to add
     * @see #PROPERTY_OPEN_PROJECTS
     */    
    public void addPropertyChangeListener( PropertyChangeListener listener ) {
        trampoline.addPropertyChangeListenerAPI( listener , this);
    }
    
    /**
     * Removes a listener.
     * @param listener a listener to remove
     */
    public void removePropertyChangeListener( PropertyChangeListener listener ) {
        trampoline.removePropertyChangeListenerAPI( listener );
    }
    
}
