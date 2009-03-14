/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved.
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
 * Portions Copyrighted 2008 Sun Microsystems, Inc.
 */

package org.netbeans.modules.bugzilla;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;
import java.util.logging.Logger;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.mylyn.internal.bugzilla.core.BugzillaClient;
import org.eclipse.mylyn.internal.bugzilla.core.BugzillaCorePlugin;
import org.eclipse.mylyn.internal.bugzilla.core.BugzillaCustomField;
import org.eclipse.mylyn.internal.bugzilla.core.BugzillaRepositoryConnector;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Tomas Stupka
 */
public class Bugzilla {

    private BugzillaRepositoryConnector brc;

    private static Bugzilla instance;

    public static Logger LOG = Logger.getLogger("org.netbeans.modules.bugzilla.Bugzilla"); // NOI18N

    private RequestProcessor rp;

    private Bugzilla() {
        BugzillaCorePlugin bcp = new BugzillaCorePlugin();
        try {
            bcp.start(null);
        } catch (Exception ex) {
            throw new RuntimeException(ex); // XXX thisiscrap
        }
    }

    public static Bugzilla getInstance() {
        if(instance == null) {
            instance = new Bugzilla();
        }
        return instance;
    }

    public BugzillaRepositoryConnector getRepositoryConnector() {
        if(brc == null) {
            brc = new BugzillaRepositoryConnector();
        }
        return brc;
    }

    /**
     * Returns a BugzillaClient for the given repository
     * @param repository
     * @return
     * @throws java.net.MalformedURLException
     * @throws org.eclipse.core.runtime.CoreException
     */
    public BugzillaClient getClient(BugzillaRepository repository) throws MalformedURLException, CoreException {
        return getRepositoryConnector().getClientManager().getClient(repository.getTaskRepository(), new NullProgressMonitor());
    }

    /**
     * Returns all products defined in the given repository
     *
     * @param repository
     * @return
     */
    public List<String> getProducts(BugzillaRepository repository) {
        return repository.getRepositoryConfiguration().getProducts();
    }

    /**
     * Returns the componets for the given product or all known components if product is null
     *
     * @param repository
     * @param product
     * @return list of components
     */
    public List<String> getComponents(BugzillaRepository repository, String product) {
        if(product == null) {
            return repository.getRepositoryConfiguration().getComponents();
        } else {
            return repository.getRepositoryConfiguration(). getComponents(product);
        }
    }

    /**
     * Returns all resolutions defined in the given repository
     *
     * @param repository
     * @return
     */
    public List<String> getResolutions(BugzillaRepository repository) {
        return repository.getRepositoryConfiguration().getResolutions();
    }

    /**
     * Returns versiones defined for the given product or all available versions if product is null
     *
     * @param repository
     * @param product
     * @return
     */
    public List<String> getVersions(BugzillaRepository repository, String product) {
        if(product == null) {
            return repository.getRepositoryConfiguration().getVersions();
        } else {
            return repository.getRepositoryConfiguration().getVersions(product);
        }
    }

    /**
     * Returns all status defined in the given repository
     * @param repository
     * @return
     */
    public List<String> getStatusValues(BugzillaRepository repository)  {
        return repository.getRepositoryConfiguration().getStatusValues();
    }

    /**
     * Returns all open statuses defined in the given repository.
     * @param repository
     * @return all open statuses defined in the given repository.
     */
    public List<String> getOpenStatusValues(BugzillaRepository repository)  {
        return repository.getRepositoryConfiguration().getOpenStatusValues();
    }

    /**
     * Returns all priorities defined in the given repository
     * @param repository
     * @return
     */
    public List<String> getPriorities(BugzillaRepository repository)  {
        return repository.getRepositoryConfiguration().getPriorities();
    }

    /**
     * Returns all keywords defined in the given repository
     * @param repository
     * @return
     */
    public List<String> getKeywords(BugzillaRepository repository) {
        return repository.getRepositoryConfiguration().getKeywords();
    }

    /**
     * Returns all platforms defined in the given repository
     * @param repository
     * @return
     */
    public List<String> getPlatforms(BugzillaRepository repository) {
        return repository.getRepositoryConfiguration().getPlatforms();
    }

    /**
     * Returns all operating systems defined in the given repository
     * @param repository
     * @return
     */
    public List<String> getOSs(BugzillaRepository repository) {
        return repository.getRepositoryConfiguration().getOSs();
    }

    /**
     * Returns all severities defined in the given repository
     * @param repository
     * @return
     */
    public List<String> getSeverities(BugzillaRepository repository) {
        return repository.getRepositoryConfiguration().getSeverities();
    }

    /**
     * Returns all custom fields defined in the given repository
     * @param repository
     * @return
     */
    public List<BugzillaCustomField> getCustomFields(BugzillaRepository repository) {
        return repository.getRepositoryConfiguration().getCustomFields();
    }

    /**
     * Returns target milestones defined for the given product or all available 
     * milestones if product is null
     *
     * @param repository
     * @param product
     * @return
     */
    public List<String> getTargetMilestones(BugzillaRepository repository, String product) {
        if(product == null) {
            return repository.getRepositoryConfiguration().getTargetMilestones();
        } else {
            return repository.getRepositoryConfiguration().getTargetMilestones(product);
        }
    }

    /**
     * Returns the request processor for common tasks in bugzilla.
     * Do not use this when accesing a remote repository.
     * 
     * @return
     */
    public RequestProcessor getRequestProcessor() {
        if(rp == null) {
            rp = new RequestProcessor("Bugzilla"); // NOI18N
        }
        return rp;
    }
}
