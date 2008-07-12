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

package org.netbeans.modules.cnd.remote.server;

import org.netbeans.modules.cnd.api.compilers.CompilerSetManager;
import org.netbeans.modules.cnd.api.compilers.PlatformTypes;
import org.netbeans.modules.cnd.api.remote.ServerRecord;
import org.openide.util.RequestProcessor;

/**
 * The definition of a remote server and login. 
 * 
 * @author gordonp
 */
public class RemoteServerRecord implements ServerRecord, PlatformTypes  {
    
    public static final Object STATE_INITIALIZING = "STATE_INITIALIZING"; // NOI18N
    public static final Object STATE_ONLINE = "STATE_ONLINE"; // NOI18N
    public static final Object STATE_OFFLINE = "STATE_OFFLINE"; // NOI18N
    
    private String user;
    private String server;
    private String name;
    private boolean editable;
    private Object state;
    
    protected RemoteServerRecord(final String name) {
        this.name = name;
        
        if (name.equals(CompilerSetManager.LOCALHOST)) {
            editable = false;
            state = STATE_ONLINE;
        } else {
            editable = true;
            state = STATE_INITIALIZING;
            
            RequestProcessor.getDefault().post(new Runnable() {
                public void run() {
                    if (RemoteServerSetup.needsSetupOrUpdate(name)) {
                        RemoteServerSetup.setup(name);
                    }
                    state = STATE_ONLINE;
                }
            });
        }
    }
    
    public Object getState() {
        return state;
    }
    
    public void setState(Object state) {
        this.state = state;
    }
    
    public boolean isEditable() {
        return editable;
    }

    public boolean isRemote() {
        return !name.equals(CompilerSetManager.LOCALHOST);
    }

    public String getName() {
        return name;
    }

    public String getServerName() {
        return server;
    }

    public String getUserName() {
        return user;
    }
}
