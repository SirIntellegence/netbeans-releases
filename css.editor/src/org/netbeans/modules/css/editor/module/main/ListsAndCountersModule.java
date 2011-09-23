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
package org.netbeans.modules.css.editor.module.main;

import java.util.Arrays;
import java.util.Collection;
import org.netbeans.modules.css.editor.module.spi.CssEditorModule;
import org.netbeans.modules.css.editor.module.spi.CssModule;
import org.netbeans.modules.css.editor.module.spi.Property;
import org.netbeans.modules.css.editor.module.spi.Utilities;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author mfukala@netbeans.org
 */
@ServiceProvider(service = CssEditorModule.class)
public class ListsAndCountersModule extends CssEditorModule implements CssModule {

    private static final String PROPERTIES_DEFINITION_PATH = "org/netbeans/modules/css/editor/module/main/properties/lists_and_counters"; //NOI18N
    private static Collection<Property> propertyDescriptors;
    private static final Collection<String> PSEUDO_ELEMENTS = Arrays.asList(new String[]{"marker"}); //NOI18N

    @Override
    public synchronized Collection<Property> getProperties() {
        if (propertyDescriptors == null) {
            propertyDescriptors = Utilities.parsePropertyDefinitionFile(PROPERTIES_DEFINITION_PATH, this);
        }
        return propertyDescriptors;
    }

    @Override
    public Collection<String> getPseudoElements() {
        return PSEUDO_ELEMENTS;
    }

    @Override
    public String getName() {
        return "lists_and_counters"; //NOI18N
    }

    @Override
    public String getDisplayName() {
        return NbBundle.getMessage(this.getClass(), Constants.CSS_MODULE_DISPLAYNAME_BUNDLE_KEY_PREFIX + getName());
    }

    @Override
    public String getSpecificationURL() {
        return "http://www.w3.org/TR/css3-lists"; //NOI18N
    }
}
