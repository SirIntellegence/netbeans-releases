/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
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

package org.netbeans.modules.cnd.debugger.gdb.disassembly;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Map;

/**
 *
 * @author eu155513
 */
public class RegisterValuesProvider {
    public static final String VALUES_UPDATED = "register values updated";// NOI18N
    public static final String VALUES_CLEAR = "register values clear";// NOI18N
    
    private static RegisterValuesProvider instance = null;
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    
    public static RegisterValuesProvider getInstance() {
        if (instance == null) {
            instance = new RegisterValuesProvider();
        }
        return instance;
    }
    
    public Map<String,RegisterValue> getRegisterValues() {
        Disassembly dis = Disassembly.getCurrent();
        if (dis != null) {
            return dis.getRegisterValues();
        } else {
            return null;
        }
    }
    
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }
    
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }
    
    void fireRegisterValuesChanged() {
        pcs.firePropertyChange(VALUES_UPDATED, 0, 1);
    }
    
    void fireRegisterValuesClear() {
        pcs.firePropertyChange(VALUES_CLEAR, 0, 1);
    }
    
    public static class RegisterValue {
        private final String value;
        private final boolean modified;

        public RegisterValue(String value, boolean modified) {
            this.value = value;
            this.modified = modified;
        }

        public boolean isModified() {
            return modified;
        }

        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return getValue();
        }
    }
}
