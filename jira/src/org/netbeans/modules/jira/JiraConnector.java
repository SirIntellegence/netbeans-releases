/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2008-2009 Sun Microsystems, Inc. All rights reserved.
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
 * Portions Copyrighted 2008-2009 Sun Microsystems, Inc.
 */

package org.netbeans.modules.jira;

import java.awt.Image;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.modules.bugtracking.spi.IssueFinder;
import org.netbeans.modules.jira.repository.JiraRepository;
import org.netbeans.modules.bugtracking.spi.Repository;
import org.netbeans.modules.bugtracking.spi.BugtrackingConnector;
import org.netbeans.modules.jira.issue.JiraIssueFinder;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.lookup.Lookups;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Tomas Stupka
 */
@org.openide.util.lookup.ServiceProviders({@ServiceProvider(service=org.netbeans.modules.bugtracking.spi.BugtrackingConnector.class),
                                           @ServiceProvider(service=org.netbeans.modules.jira.JiraConnector.class)})
public class JiraConnector extends BugtrackingConnector {

    private static final Logger LOG = Logger.getLogger("org.netbeans.modules.jira.JiraConnector");  //  NOI18N
    private JiraIssueFinder issueFinder;
    private boolean alreadyLogged = false;

    @Override
    public String getID() {
        return "org.netbeans.modules.jira";                                     //  NOI18N
    }

    @Override
    public Image getIcon() {
        return null;
    }

    @Override
    public String getDisplayName() {
        return getConnectorName();
    }

    @Override
    public String getTooltip() {
        return NbBundle.getMessage(BugtrackingConnector.class, "LBL_ConnectorTooltip"); // NOI18N
    }

    @Override
    public Repository createRepository() {
        try {
            Jira.init();
        } catch (Throwable t) {
            if(!alreadyLogged) {
                alreadyLogged = true;
                LOG.log(Level.SEVERE, null, t);
            }
            return null;
        }
        return new JiraRepository();
    }

    @Override
    public Repository[] getRepositories() {
        Jira jira = getJira();
        if(jira != null) {
            return jira.getRepositories();
        }
        return new Repository[0];
    }

    public static String getConnectorName() {
        return NbBundle.getMessage(JiraConnector.class, "LBL_ConnectorName");           // NOI18N
    }


    @Override
    public IssueFinder getIssueFinder() {
        if (issueFinder == null) {
            issueFinder = Lookup.getDefault().lookup(JiraIssueFinder.class);
        }
        return issueFinder;
    }

    @Override
    public Lookup getLookup() {
        Jira jira = getJira();
        if(jira != null) {
            return Lookups.singleton(jira.getKenaiSupport());
        }
        return Lookup.EMPTY;
    }

    @Override
    protected void fireRepositoriesChanged(Collection<Repository> oldRepositories, Collection<Repository> newRepositories) {
        super.fireRepositoriesChanged(oldRepositories, newRepositories);
    }

    private Jira getJira() {
        try {
            return Jira.getInstance();
        } catch (Throwable t) {
            if(!alreadyLogged) {
                alreadyLogged = true;
                LOG.log(Level.SEVERE, null, t);
            }
        }
        return null;
    }
}
