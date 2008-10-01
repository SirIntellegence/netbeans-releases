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
package org.netbeans.modules.java.j2seplatform.platformdefinition;

import org.openide.util.ImageUtilities;
import org.openide.util.Utilities;
import org.openide.util.NbBundle;

import java.beans.SimpleBeanInfo;
import java.beans.PropertyDescriptor;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.awt.*;

public class J2SEPlatformImplBeanInfo extends SimpleBeanInfo {

    public J2SEPlatformImplBeanInfo () {
    }


    public PropertyDescriptor[] getPropertyDescriptors () {
        try {
            PropertyDescriptor[] descs =  new PropertyDescriptor[] {
                new PropertyDescriptor (J2SEPlatformImpl.PROP_DISPLAY_NAME, J2SEPlatformImpl.class),
                new PropertyDescriptor (J2SEPlatformImpl.PROP_ANT_NAME, J2SEPlatformImpl.class),
                new PropertyDescriptor (J2SEPlatformImpl.PROP_SOURCE_FOLDER, J2SEPlatformImpl.class),
                new PropertyDescriptor (J2SEPlatformImpl.PROP_JAVADOC_FOLDER, J2SEPlatformImpl.class),
            };
            descs[0].setDisplayName(NbBundle.getMessage(J2SEPlatformImplBeanInfo.class,"TXT_Name"));
            descs[0].setBound(true);
            descs[1].setDisplayName(NbBundle.getMessage(J2SEPlatformImplBeanInfo.class,"TXT_AntName"));
            descs[1].setWriteMethod(null);
            descs[2].setDisplayName(NbBundle.getMessage(J2SEPlatformImplBeanInfo.class,"TXT_SourcesFolder"));
            descs[2].setPropertyEditorClass(FileObjectPropertyEditor.class);
            descs[2].setBound(true);
            descs[3].setDisplayName(NbBundle.getMessage(J2SEPlatformImplBeanInfo.class,"TXT_JavaDocFolder"));
            descs[3].setPropertyEditorClass(FileObjectPropertyEditor.class);
            descs[3].setBound(true);
            return descs;
        } catch (IntrospectionException ie) {
            return new PropertyDescriptor[0];
        }
    }


    public Image getIcon(int iconKind) {
        if ((iconKind == BeanInfo.ICON_COLOR_16x16) || (iconKind == BeanInfo.ICON_MONO_16x16)) {
            return ImageUtilities.loadImage("org/netbeans/modules/java/j2seplatform/resources/platform.gif"); // NOI18N
        } else {
            return null;
        }
    }

}
