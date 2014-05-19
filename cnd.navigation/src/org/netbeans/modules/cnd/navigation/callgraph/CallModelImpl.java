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
 * Portions Copyrighted 2007 Sun Microsystems, Inc.
 */

package org.netbeans.modules.cnd.navigation.callgraph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.netbeans.modules.cnd.api.model.CsmClass;
import org.netbeans.modules.cnd.api.model.CsmClassifier;
import org.netbeans.modules.cnd.api.model.CsmCompoundClassifier;
import org.netbeans.modules.cnd.api.model.CsmDeclaration;
import org.netbeans.modules.cnd.api.model.CsmFile;
import org.netbeans.modules.cnd.api.model.CsmFriend;
import org.netbeans.modules.cnd.api.model.CsmFunction;
import org.netbeans.modules.cnd.api.model.CsmFunctionDefinition;
import org.netbeans.modules.cnd.api.model.CsmMacro;
import org.netbeans.modules.cnd.api.model.CsmMember;
import org.netbeans.modules.cnd.api.model.CsmMethod;
import org.netbeans.modules.cnd.api.model.CsmNamespaceDefinition;
import org.netbeans.modules.cnd.api.model.CsmObject;
import org.netbeans.modules.cnd.api.model.CsmOffsetable;
import org.netbeans.modules.cnd.api.model.CsmProject;
import org.netbeans.modules.cnd.api.model.CsmScope;
import org.netbeans.modules.cnd.api.model.CsmTypedef;
import org.netbeans.modules.cnd.api.model.CsmUID;
import org.netbeans.modules.cnd.api.model.services.CsmCacheManager;
import org.netbeans.modules.cnd.api.model.services.CsmFileInfoQuery;
import org.netbeans.modules.cnd.api.model.services.CsmFileReferences;
import org.netbeans.modules.cnd.api.model.services.CsmReferenceContext;
import org.netbeans.modules.cnd.api.model.services.CsmVirtualInfoQuery;
import org.netbeans.modules.cnd.api.model.util.CsmKindUtilities;
import org.netbeans.modules.cnd.api.model.util.UIDs;
import org.netbeans.modules.cnd.api.model.xref.CsmReference;
import org.netbeans.modules.cnd.api.model.xref.CsmReferenceKind;
import org.netbeans.modules.cnd.api.model.xref.CsmReferenceRepository;
import org.netbeans.modules.cnd.api.model.xref.CsmReferenceResolver;
import org.netbeans.modules.cnd.callgraph.api.Call;
import org.netbeans.modules.cnd.callgraph.api.CallModel;
import org.netbeans.modules.cnd.callgraph.api.Function;
import org.netbeans.modules.cnd.callgraph.api.ui.CallGraphPreferences;
import org.netbeans.modules.cnd.support.Interrupter;

/**
 *
 * @author Alexander Simon
 */
public class CallModelImpl implements CallModel {
    private final CsmReferenceRepository repository;
    private final CsmFileReferences references;
    private final CsmProject project;
    private String name;
    private FunctionUIN uin;
    
    public CallModelImpl(CsmProject project, CsmFunction root){
        repository = CsmReferenceRepository.getDefault();
        references = CsmFileReferences.getDefault();
        this.project = project;
        uin = new FunctionUIN(project, root);
        name = root.getName().toString();
    }

    @Override
    public Function getRoot() {
        CsmFunction root = uin.getFunction();
        if (root != null) {
            CsmCacheManager.enter();
            try {
                return new FunctionImpl(root);
            } finally {
                CsmCacheManager.leave();
            }
        }
        return null;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setRoot(Function newRoot) {
        if (newRoot instanceof FunctionImpl) {
            FunctionImpl impl = (FunctionImpl)newRoot;
            CsmFunction f = impl.getDefinition();
            if (f != null) {
                uin = new FunctionUIN(project, f);
                name = f.getName().toString();
            }
        }
    }

    /**
     * declaration()<-f()
     * @param declaration
     * @return
     */
    @Override
    public List<Call> getCallers(Function declaration) {
        CsmCacheManager.enter();
        try {
            FunctionImpl functionImpl = (FunctionImpl) declaration;
            CsmFunction owner = functionImpl.getDeclaration();
            Collection<CsmFunction> functions = new ArrayList<CsmFunction>();
            functions.add(owner);
            if (CallGraphPreferences.isShowOverriding()) {
                if (CsmKindUtilities.isMethodDeclaration(owner)) {
                    Collection<CsmMethod> overrides = CsmVirtualInfoQuery.getDefault().getAllBaseDeclarations((CsmMethod) owner);
                    functions.addAll(overrides);
                }
            }
            EnumSet<CsmReferenceKind> kinds = EnumSet.of(CsmReferenceKind.DIRECT_USAGE, CsmReferenceKind.UNKNOWN);
            List<Call> res = new ArrayList<Call>();
            HashMap<CsmFunction,CsmReference> set = new HashMap<CsmFunction,CsmReference>();
            HashMap<CsmMacro,CsmReference> macros = new HashMap<CsmMacro,CsmReference>();
            for(CsmFunction function : functions) {
                if (CsmKindUtilities.isFunction(function) && function.getContainingFile().isValid()) {
                    for(CsmReference r : repository.getReferences(function, project, CsmReferenceKind.ANY_REFERENCE_IN_ACTIVE_CODE, Interrupter.DUMMY)){
                        if (r == null) {
                            continue;
                        }
                        if (CsmReferenceResolver.getDefault().isKindOf(r,kinds)) {
                            CsmFunction o = getFunctionDeclaration(getEnclosingFunction(r));
                            if (o != null) {
                                if (!set.containsKey(o)) {
                                    set.put(o, r);
                                }
                            } else {
                                CsmMacro enclosingMacro = getEnclosingMacro(r);
                                if (enclosingMacro != null && !macros.containsKey(enclosingMacro)) {
                                    macros.put(enclosingMacro, r);
                                }
                            }
                        }
                    }
                }
            }
            for(Map.Entry<CsmMacro,CsmReference> entry : macros.entrySet()) {
                for(CsmReference r : repository.getReferences(entry.getKey(), project, CsmReferenceKind.ANY_REFERENCE_IN_ACTIVE_CODE, Interrupter.DUMMY)){
                    if (r == null) {
                        continue;
                    }
                    if (CsmReferenceResolver.getDefault().isKindOf(r,kinds)) {
                        CsmFunction o = getFunctionDeclaration(getEnclosingFunction(r));
                        if (o != null) {
                            if (!set.containsKey(o)) {
                                set.put(o, r);
                            }
                        }
                    }
                }
            }
            final CsmFunction functionDeclaration = getFunctionDeclaration(owner);
            if (functionDeclaration != null) {
                for(Map.Entry<CsmFunction,CsmReference> r : set.entrySet()){
                    res.add(new CallImpl(r.getKey(), r.getValue(), functionDeclaration, true));
                }
            }
            return res;
        } finally {
            CsmCacheManager.leave();
        }
    }
    
    /**
     * declaration()->f()
     * @param declaration
     * @return
     */
    @Override
    public List<Call> getCallees(Function definition) {
        CsmCacheManager.enter();
        try {
            FunctionImpl definitionImpl = (FunctionImpl) definition;
            CsmFunction owner = definitionImpl.getDefinition();
            Collection<CsmFunction> functions = new ArrayList<CsmFunction>();
            functions.add(owner);
            if (CallGraphPreferences.isShowOverriding()) {
                if (CsmKindUtilities.isMethodDeclaration(owner)) {
                    Collection<CsmMethod> overrides = CsmVirtualInfoQuery.getDefault().getOverriddenMethods((CsmMethod) owner, false);
                    functions.addAll(overrides);
                }
            }
            List<Call> res = new ArrayList<Call>();
            final HashMap<CsmFunction,CsmReference> set = new HashMap<CsmFunction,CsmReference>();
            for(CsmFunction function : functions) {
                if (CsmKindUtilities.isFunctionDefinition(function) && function.getContainingFile().isValid()) {
                    final List<CsmOffsetable> list = CsmFileInfoQuery.getDefault().getUnusedCodeBlocks((function).getContainingFile(), Interrupter.DUMMY);
                    references.accept((CsmScope)function, null, new CsmFileReferences.Visitor() {
                        @Override
                        public void visit(CsmReferenceContext context) {
                            CsmReference r = context.getReference();
                            if (r == null) {
                                return;
                            }
                            for(CsmOffsetable offset:list){
                                if (offset.getStartOffset()<=r.getStartOffset() &&
                                    offset.getEndOffset()  >=r.getEndOffset()){
                                    return;
                                }
                            }
                            try {
                                CsmObject o = r.getReferencedObject();
                                if (CsmKindUtilities.isFunction(o) && 
                                        !CsmReferenceResolver.getDefault().isKindOf(r, CsmReferenceKind.FUNCTION_DECLARATION_KINDS)) {
                                    o = getFunctionDeclaration((CsmFunction)o);
                                    if (!set.containsKey((CsmFunction)o)) {
                                        set.put((CsmFunction)o, r);
                                    }
                                }
                            } catch (AssertionError e){
                                e.printStackTrace(System.err);
                            } catch (Exception e) {
                                e.printStackTrace(System.err);
                            }
                        }

                        @Override
                        public boolean cancelled() {
                            return false;
                        }
                    }, CsmReferenceKind.ANY_REFERENCE_IN_ACTIVE_CODE);
                }
            }
            final CsmFunction functionDeclaration = getFunctionDeclaration(owner);
            if (functionDeclaration != null) {
                for(Map.Entry<CsmFunction,CsmReference> r : set.entrySet()){
                    res.add(new CallImpl( functionDeclaration, r.getValue(),r.getKey(), false));
                }
            }
            return res;
        } finally {
            CsmCacheManager.leave();
        }
    }
    
    private CsmFunction getFunctionDeclaration(CsmFunction definition){
        if (definition != null) {
            if (CsmKindUtilities.isFunctionDefinition(definition)) {
                return ((CsmFunctionDefinition)definition).getDeclaration();
            }
        }
        return definition;
    }

    private CsmMacro getEnclosingMacro(CsmReference ref){
        CsmObject o = ref.getClosestTopLevelObject();
        if (CsmKindUtilities.isMacro(o)) {
            return (CsmMacro) o;
        }
        return null;
    }

    private CsmFunction getEnclosingFunction(CsmReference ref){
        CsmObject o = ref.getClosestTopLevelObject();
        if (CsmKindUtilities.isFunction(o)) {
            return (CsmFunction) o;
        }
        return null;
    }

    private static class FunctionUIN {

        private final CsmProject project;
        private final CharSequence functionUin;
        private final CsmUID<CsmFile> fileUid;

        private FunctionUIN(CsmProject project, CsmFunction root) {
            this.project = project;
            functionUin = root.getUniqueName();
            fileUid = UIDs.get(root.getContainingFile());
        }

        private CsmFunction getFunction() {
            if (!project.isValid()) {
                return null;
            }
            CsmFunction root = (CsmFunction) project.findDeclaration(functionUin);
            if (root != null) {
                return root;
            }
            CsmFile file = fileUid.getObject();
            if (!file.isValid()) {
                return null;
            }
            for (CsmDeclaration d : file.getDeclarations()) {
                root = findFunction(d);
                if (root != null){
                    return root;
                }
            }
            return null;
        }

        private CsmFunction findFunction(CsmDeclaration element) {
            if (CsmKindUtilities.isTypedef(element) || CsmKindUtilities.isTypeAlias(element)) {
                CsmTypedef def = (CsmTypedef) element;
                if (def.isTypeUnnamed()) {
                    CsmClassifier cls = def.getType().getClassifier();
                    if (cls != null && cls.getName().length() == 0 &&
                            (cls instanceof CsmCompoundClassifier)) {
                        return findFunction((CsmCompoundClassifier) cls);
                    }
                }
                return null;
            } else if (CsmKindUtilities.isClassifier(element)) {
                String name = ((CsmClassifier) element).getName().toString();
                if (name.length() == 0 && (element instanceof CsmCompoundClassifier)) {
                    Collection<CsmTypedef> list = ((CsmCompoundClassifier) element).getEnclosingTypedefs();
                    if (list.size() > 0) {
                        return null;
                    }
                }
                if (CsmKindUtilities.isClass(element)) {
                    CsmClass cls = (CsmClass) element;
                    for (CsmMember m : cls.getMembers()) {
                        CsmFunction res = findFunction(m);
                        if (res != null) {
                            return res;
                        }
                    }
                    for (CsmFriend m : cls.getFriends()) {
                        CsmFunction res = findFunction(m);
                        if (res != null) {
                            return res;
                        }
                    }
                }
                return null;
            } else if (CsmKindUtilities.isNamespaceDefinition(element)) {
                for (CsmDeclaration d : ((CsmNamespaceDefinition) element).getDeclarations()) {
                    CsmFunction res = findFunction(d);
                    if (res != null) {
                        return res;
                    }
                }
            } else if (CsmKindUtilities.isFunction(element)) {
                if (element.getUniqueName().equals(functionUin)) {
                    return (CsmFunction) element;
                }
            }
            return null;
        }
    }
}
