/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2010 Sun Microsystems, Inc. All rights reserved.
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
 * Portions Copyrighted 2010 Sun Microsystems, Inc.
 */

package org.netbeans.modules.j2ee.weblogic9.config;

import java.io.File;
import org.netbeans.modules.j2ee.deployment.common.api.Datasource;

/**
 *
 * @author Petr Hejl
 */
public class WLDatasource implements Datasource {

    private final String name;

    private final String url;

    private final String jndi;

    private final String user;

    private final String password;

    private final String driver;

    private final File origin;

    public WLDatasource(String name, String url, String jndi, String user,
            String password, String driver, File origin) {
        this.name = name;
        this.url = url;
        this.jndi = jndi;
        this.user = user;
        this.password = password;
        this.driver = driver;
        this.origin = origin;
    }

    public String getName() {
        return name;
    }

    @Override
    public String getDisplayName() {
        return name;
    }

    @Override
    public String getDriverClassName() {
        return driver;
    }

    @Override
    public String getJndiName() {
        return jndi;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUrl() {
        return url;
    }

    @Override
    public String getUsername() {
        return user;
    }

    public File getOrigin() {
        return origin;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final WLDatasource other = (WLDatasource) obj;
        if ((this.name == null) ? (other.name != null) : !this.name.equals(other.name)) {
            return false;
        }
        if ((this.url == null) ? (other.url != null) : !this.url.equals(other.url)) {
            return false;
        }
        if ((this.jndi == null) ? (other.jndi != null) : !this.jndi.equals(other.jndi)) {
            return false;
        }
        if ((this.driver == null) ? (other.driver != null) : !this.driver.equals(other.driver)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + (this.name != null ? this.name.hashCode() : 0);
        hash = 97 * hash + (this.url != null ? this.url.hashCode() : 0);
        hash = 97 * hash + (this.jndi != null ? this.jndi.hashCode() : 0);
        hash = 97 * hash + (this.driver != null ? this.driver.hashCode() : 0);
        return hash;
    }

}
