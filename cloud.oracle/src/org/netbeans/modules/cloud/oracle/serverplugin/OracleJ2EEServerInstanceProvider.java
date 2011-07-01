/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2011 Sun Microsystems, Inc.
 */
package org.netbeans.modules.cloud.oracle.serverplugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.netbeans.api.server.ServerInstance;
import org.netbeans.modules.cloud.oracle.OracleInstance;
import org.netbeans.modules.cloud.oracle.OracleInstanceManager;
import org.netbeans.modules.j2ee.deployment.devmodules.api.Deployment;
import org.netbeans.modules.j2ee.deployment.plugins.api.InstanceCreationException;
import org.netbeans.modules.j2ee.deployment.plugins.api.InstanceProperties;
import org.netbeans.spi.server.ServerInstanceFactory;
import org.netbeans.spi.server.ServerInstanceProvider;
import org.openide.util.ChangeSupport;
import org.openide.util.Exceptions;

/**
 *
 */
public final class OracleJ2EEServerInstanceProvider implements ServerInstanceProvider, ChangeListener {

    private ChangeSupport listeners;
    private List<ServerInstance> instances;
    private static OracleJ2EEServerInstanceProvider instance;
    
    private OracleJ2EEServerInstanceProvider() {
        listeners = new ChangeSupport(this);
        instances = Collections.<ServerInstance>emptyList();
        //refreshServers();
    }
    
    public static synchronized OracleJ2EEServerInstanceProvider getProvider() {
        if (instance == null) {
            instance = new OracleJ2EEServerInstanceProvider();
            OracleInstanceManager.getDefault().addChangeListener(instance);
        }
        return instance;
    }

    @Override
    public List<ServerInstance> getInstances() {
        return instances;
    }

    @Override
    public void addChangeListener(ChangeListener listener) {
        listeners.addChangeListener(listener);
    }

    @Override
    public void removeChangeListener(ChangeListener listener) {
        listeners.removeChangeListener(listener);
    }

    private void refreshServersSynchronously() {
        List<ServerInstance> servers = new ArrayList<ServerInstance>();
        for (OracleInstance ai : OracleInstanceManager.getDefault().getInstances()) {
            for (OracleJ2EEInstance inst : ai.readJ2EEServerInstances()) {
                ServerInstance si = ServerInstanceFactory.createServerInstance(new OracleJ2EEServerInstanceImplementation(inst));
                // TODO: there must be a better way to check whether server already is registered or not:
                if (Deployment.getDefault().getJ2eePlatform(inst.getId()) == null) {
                    try {
                        InstanceProperties ip = InstanceProperties.createInstancePropertiesNonPersistent(inst.getId(), 
                                ai.getTenantUserName(), ai.getTenantPassword(), inst.getDisplayName(), new HashMap<String, String>());
                        ip.setProperty(OracleDeploymentFactory.IP_TENANT_ID, inst.getTenantId());
                        ip.setProperty(OracleDeploymentFactory.IP_SERVICE_NAME, inst.getServiceName());
                        ip.setProperty(OracleDeploymentFactory.IP_URL_ENDPOINT, ai.getUrlEndpoint());
                        ip.setProperty(InstanceProperties.URL_ATTR, inst.getId());
                    } catch (InstanceCreationException ex) {
                        Exceptions.printStackTrace(ex);
                    }
                }
                servers.add(si);
            }
        }
        instances = servers;
        listeners.fireChange();
    }
    
    public final Future<Void> refreshServers() {
        return OracleInstance.runAsynchronously(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                refreshServersSynchronously();
                return null;
            }
        });
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        refreshServers();
    }
}
