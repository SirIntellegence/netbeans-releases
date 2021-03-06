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
package org.netbeans.modules.j2ee.sun.ddloaders.multiview.common;

import java.util.HashMap;
import java.util.Map;
import org.netbeans.modules.j2ee.dd.api.client.AppClient;
import org.netbeans.modules.j2ee.dd.api.common.CommonDDBean;
import org.netbeans.modules.j2ee.dd.api.common.PortComponentRef;
import org.netbeans.modules.j2ee.dd.api.common.ServiceRef;
import org.netbeans.modules.j2ee.dd.api.common.VersionNotSupportedException;
import org.netbeans.modules.j2ee.dd.api.ejb.Ejb;
import org.netbeans.modules.j2ee.dd.api.ejb.EjbJar;
import org.netbeans.modules.j2ee.dd.api.web.WebApp;
import org.netbeans.modules.j2ee.sun.ddloaders.Utils;
import org.openide.ErrorManager;


/**
 *
 * @author Peter Williams
 */
public class PortComponentRefMetadataReader extends CommonBeanReader {

    private String serviceRefName;
    private String ejbName;
    
    public PortComponentRefMetadataReader(final String serviceRefName, final String ejbName) {
        super(DDBinding.PROP_PORTCOMPONENT_REF);
        this.serviceRefName = serviceRefName;
        this.ejbName = ejbName;
    }
    
    /** For normalizing data structures within /ejb-jar graph.
     *    /ejb-jar -> -> /ejb-jar/enterprise-beans/session[ejb-name="xxx"]
     * (finds message-driven and entity as well)
     * 
     * TODO This mechanism will probably need optimization and caching to perform
     * for larger files.
     */
    @Override
    protected CommonDDBean normalizeParent(CommonDDBean parent) {
        if(ejbName != null && parent instanceof EjbJar) {
            parent = findEjbByName((EjbJar) parent, ejbName);
        }
        if(serviceRefName != null && parent != null) {
            parent = findServiceRefByName(parent, serviceRefName);
        }
        return parent;
    }
    
    /** Used by derived classes to locate a parent ejb by it's name, if one
     *  exists and we're reading /ejb-jar.
     */ 
    protected CommonDDBean findServiceRefByName(CommonDDBean parent, String serviceRefName) {
        CommonDDBean match = null;
        try {
            ServiceRef [] serviceRefs = null;
            
            if(parent instanceof WebApp) {
                serviceRefs = ((WebApp) parent).getServiceRef();
            } else if(parent instanceof Ejb) {
                serviceRefs = ((Ejb) parent).getServiceRef();
            } else if(parent instanceof AppClient) {
                serviceRefs = ((AppClient) parent).getServiceRef();
            } else {
                ErrorManager.getDefault().notify(ErrorManager.INFORMATIONAL, new IllegalStateException(
                        "Unsupported parent for service-ref field in standard descriptor: " + parent));
            }

            match = findServiceRefByName(serviceRefs, serviceRefName);
        } catch (VersionNotSupportedException ex) {
            ErrorManager.getDefault().notify(ErrorManager.INFORMATIONAL, ex);
        }
        return match;
    }
    
    protected CommonDDBean findServiceRefByName(ServiceRef [] serviceRefs, String serviceRefName) {
        CommonDDBean match = null;
        if(serviceRefs != null) {
            for(ServiceRef serviceRef: serviceRefs) {
                if(serviceRefName.equals(serviceRef.getServiceRefName())) {
                    match = serviceRef;
                    break;
                }
            }
        }
        return match;
    }    
    
    /** Maps interesting fields from resource-env-ref descriptor to a multi-level property map.
     * 
     * @return Map<String, Object> where Object is either a String value or nested map
     *  with the same structure (and thus ad infinitum)
     */
    public Map<String, Object> genProperties(CommonDDBean [] beans) {
        Map<String, Object> result = null;
        if(beans instanceof PortComponentRef []) {
            PortComponentRef [] portComponentRefs = (PortComponentRef []) beans;
            for(PortComponentRef portComponentRef: portComponentRefs) {
                String sei = portComponentRef.getServiceEndpointInterface();
                if(Utils.notEmpty(sei)) {
                    if(result == null) {
                        result = new HashMap<String, Object>();
                    }
                    Map<String, Object> portComponentRefMap = new HashMap<String, Object>();
                    result.put(sei, portComponentRefMap);
                    portComponentRefMap.put(DDBinding.PROP_SEI, sei);
                    portComponentRefMap.put(DDBinding.PROP_NAME, sei); // Also save as name for binding purposes.
                    
                    addMapString(portComponentRefMap, DDBinding.PROP_PORTCOMPONENT_LINK, 
                            portComponentRef.getPortComponentLink());
                }
            }
        }
        return result;
    }
}
