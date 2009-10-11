/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2009 Sun Microsystems, Inc. All rights reserved.
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
 * Portions Copyrighted 2009 Sun Microsystems, Inc.
 */

package org.netbeans.modules.cnd.modelimpl.impl.services;

import java.util.ArrayList;
import java.util.List;
import javax.swing.text.Document;
import org.netbeans.modules.cnd.api.model.CsmClass;
import org.netbeans.modules.cnd.api.model.CsmFile;
import org.netbeans.modules.cnd.api.model.CsmFunction;
import org.netbeans.modules.cnd.api.model.CsmMember;
import org.netbeans.modules.cnd.api.model.CsmNamespaceDefinition;
import org.netbeans.modules.cnd.api.model.CsmOffsetableDeclaration;
import org.netbeans.modules.cnd.api.model.CsmParameter;
import org.netbeans.modules.cnd.api.model.util.CsmKindUtilities;
import org.netbeans.modules.cnd.editor.CsmDocGeneratorProvider;
import org.netbeans.modules.cnd.modelutil.CsmUtilities;

/**
 *
 * @author Alexander Simon
 */
@org.openide.util.lookup.ServiceProvider(service=org.netbeans.modules.cnd.editor.CsmDocGeneratorProvider.class)
public class CsmDocGeneratorProviderImpl extends CsmDocGeneratorProvider {

    @Override
    public Function getFunction(Document doc, int position) {
        final CsmOffsetableDeclaration decl = getFunction(CsmUtilities.getCsmFile(doc, false), position);
        if (decl instanceof CsmFunction) {
            return new Function() {
                public String getReturnType() {
                    return ((CsmFunction)decl).getReturnType().getCanonicalText().toString();
                }
                public List<Parameter> getParametes() {
                    List<Parameter> list = new ArrayList<Parameter>();
                    for (final CsmParameter par : ((CsmFunction)decl).getParameters()){
                        list.add(new Parameter(){
                            public String getType() {
                                return par.getType().getCanonicalText().toString();
                            }
                            public String getName() {
                                return par.getName().toString();
                            }
                        });
                    }
                    return list;
                }
            };
        }
        return null;
    }

    public CsmOffsetableDeclaration getFunction(CsmFile file, int position) {
        if (file != null) {
            CsmOffsetableDeclaration best = null;
            for(CsmOffsetableDeclaration decl : file.getDeclarations()) {
                if (decl.getStartOffset() < position && position < decl.getEndOffset()) {
                    return getInternalDeclaration(decl, position);
                } else if (decl.getStartOffset() > position) {
                    if (best == null || best.getStartOffset() > decl.getStartOffset()){
                        best = decl;
                    }
                }
            }
            return best;
        }
        return null;
    }

    private CsmOffsetableDeclaration getInternalDeclaration(CsmOffsetableDeclaration parent, int position){
        if (CsmKindUtilities.isClass(parent)) {
            CsmClass cls = (CsmClass) parent;
            CsmOffsetableDeclaration best = null;
            for(CsmMember decl : cls.getMembers()){
                if (decl.getStartOffset() < position && position < decl.getEndOffset()) {
                    return getInternalDeclaration(decl, position);
                } else if (decl.getStartOffset() > position) {
                    if (best == null || best.getStartOffset() > decl.getStartOffset()){
                        best = decl;
                    }
                }
            }
            return best;
        } else if(CsmKindUtilities.isNamespaceDefinition(parent)) {
            CsmNamespaceDefinition ns = (CsmNamespaceDefinition) parent;
            CsmOffsetableDeclaration best = null;
            for(CsmOffsetableDeclaration decl : ns.getDeclarations()) {
                if (decl.getStartOffset() < position && position < decl.getEndOffset()) {
                    return getInternalDeclaration(decl, position);
                } else if (decl.getStartOffset() > position) {
                    if (best == null || best.getStartOffset() > decl.getStartOffset()){
                        best = decl;
                    }
                }
            }
            return best;
        }
        return null;
    }
}
