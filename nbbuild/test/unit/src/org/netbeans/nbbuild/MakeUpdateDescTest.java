/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2010 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2010 Sun Microsystems, Inc.
 */

package org.netbeans.nbbuild;

import java.io.ByteArrayOutputStream;
import java.util.Properties;
import java.util.jar.Attributes;
import org.netbeans.junit.NbTestCase;
import org.w3c.dom.Element;

public class MakeUpdateDescTest extends NbTestCase {

    public MakeUpdateDescTest(String n) {
        super(n);
    }

    public void testFakeOSGiInfoXml() throws Exception {
        Attributes attr = new Attributes();
        attr.putValue("Bundle-SymbolicName", "bundle;singleton:=true");
        attr.putValue("Bundle-Name", "%OpenIDE-Module-Name");
        attr.putValue("Bundle-Category", "%OpenIDE-Module-Display-Category");
        attr.putValue("Bundle-Description", "%OpenIDE-Module-Short-Description");
        // As generated by JarWithModuleAttributes:
        attr.putValue("Require-Bundle", "org.netbeans.api.progress;bundle-version=\"[101.19,200)\", " +
                "org.netbeans.modules.options.api;bundle-version=\"[1.17,200)\", " +
                "com.jcraft.jsch;bundle-version=\"[0.1.37,0.2.0)\", " +
                "com.jcraft.jzlib;resolution:=optional, " +
                "org.openide.actions;bundle-version=\"[6.15,100)\"," +
                "javax.xml.rpc;bundle-version=1.1.0, " +
                "org.apache.xerces;bundle-version=\"[2.8.0,3.0.0)\";resolution:=optional");
        Properties localization = new Properties();
        localization.setProperty("OpenIDE-Module-Name", "My Bundle");
        localization.setProperty("OpenIDE-Module-Display-Category", "hello");
        localization.setProperty("OpenIDE-Module-Short-Description", "Hello there!");
        Element e = MakeUpdateDesc.fakeOSGiInfoXml(attr, localization, null);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        XMLUtil.write(e, baos);
        assertEquals("<module codenamebase='bundle' distribution='' downloadsize='0'> <manifest " +
                "AutoUpdate-Show-In-Client='false' " +
                "OpenIDE-Module='bundle' OpenIDE-Module-Display-Category='hello' " +
                "OpenIDE-Module-Module-Dependencies='org.netbeans.api.progress/1 &gt; 1.19, " +
                "org.netbeans.modules.options.api/0-1 &gt; 1.17, " +
                "com.jcraft.jsch &gt; 0.1.37, " +
                "org.openide.actions &gt; 6.15, " +
                "javax.xml.rpc &gt; 1.1.0' " +
                "OpenIDE-Module-Name='My Bundle' OpenIDE-Module-Short-Description='Hello there!' " +
                "OpenIDE-Module-Specification-Version='0'/> " +
                "</module> ", baos.toString().replace('"', '\'').replaceAll("\\s+", " "));
    }

}
