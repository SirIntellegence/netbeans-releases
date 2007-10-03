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

package org.netbeans.modules.db.explorer;

import java.util.*;
import java.beans.PropertyEditor;
import org.netbeans.lib.ddl.impl.*;
import org.netbeans.modules.db.explorer.infos.DatabaseNodeInfo;

public class DatabaseTypePropertySupport extends DatabasePropertySupport
{
    private int[] types;
    private String[] names;

    public DatabaseTypePropertySupport(String name, Class type, String displayName, String shortDescription, DatabaseNodeInfo rep, boolean writable, boolean expert)
    {
        super(name, type, displayName, shortDescription, rep, writable);
        repository = rep;
        int i = 0;

        Specification spec = (Specification)((DatabaseNodeInfo)repository).getSpecification();
        if (spec != null && writable) {
            Map tmap = ((Specification)((DatabaseNodeInfo)repository).getSpecification()).getTypeMap();
            if (tmap == null) tmap = new HashMap(1);
            Iterator enu = tmap.keySet().iterator();
            types = new int[tmap.size()];
            names = new String[tmap.size()];
            while(enu.hasNext()) {
                String key = (String)enu.next();
                int xtype = Specification.getType(key);
                String code = (String)tmap.get(key);
                types[i] = xtype;
                names[i++] = code;
            }
        } else {
            types = new int[] {0};
            names = new String[] {name};
        }

        if (expert) setExpert(true);
    }

    public PropertyEditor getPropertyEditor ()
    {
        PropertyEditor pe = new DatabaseTypePropertyEditor(types, names);
        return pe;
    }
}
