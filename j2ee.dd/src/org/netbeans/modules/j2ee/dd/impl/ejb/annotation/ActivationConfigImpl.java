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

package org.netbeans.modules.j2ee.dd.impl.ejb.annotation;

import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.netbeans.modules.j2ee.dd.api.common.VersionNotSupportedException;
import org.netbeans.modules.j2ee.dd.api.ejb.ActivationConfig;
import org.netbeans.modules.j2ee.dd.api.ejb.ActivationConfigProperty;


public class ActivationConfigImpl implements ActivationConfig {

    private List<ActivationConfigProperty> properties = new ArrayList<ActivationConfigProperty>();

    @Override
    public void setActivationConfigProperty(int index, ActivationConfigProperty value) {
        properties.set(index, value);
    }

    @Override
    public ActivationConfigProperty getActivationConfigProperty(int index) {
        return properties.get(index);
    }

    @Override
    public void setActivationConfigProperty(ActivationConfigProperty[] value) {
        properties = Arrays.asList(value);
    }

    @Override
    public ActivationConfigProperty[] getActivationConfigProperty() {
        return properties.toArray(new ActivationConfigProperty[properties.size()]);
    }

    @Override
    public int addActivationConfigProperty(ActivationConfigProperty value) {
        properties.add(value);
        return properties.size() - 1;
    }

    @Override
    public int sizeActivationConfigProperty() {
        return properties.size();
    }

    public Object clone() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public int removeActivationConfigProperty(ActivationConfigProperty value) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    public ActivationConfigProperty newActivationConfigProperty() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    public void addPropertyChangeListener(PropertyChangeListener pcl) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    public void removePropertyChangeListener(PropertyChangeListener pcl) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    public void setId(String value) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    public String getId() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    public Object getValue(String propertyName) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    public void write(OutputStream os) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    public void setDescription(String locale, String description) throws VersionNotSupportedException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    public void setDescription(String description) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    public void setAllDescriptions(Map descriptions) throws VersionNotSupportedException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    public String getDescription(String locale) throws VersionNotSupportedException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    public String getDefaultDescription() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    public Map getAllDescriptions() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    public void removeDescriptionForLocale(String locale) throws VersionNotSupportedException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    public void removeDescription() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    public void removeAllDescriptions() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    
}

