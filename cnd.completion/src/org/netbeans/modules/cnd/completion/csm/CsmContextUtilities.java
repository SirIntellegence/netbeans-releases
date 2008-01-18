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
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2007 Sun
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

package org.netbeans.modules.cnd.completion.csm;

import java.util.HashSet;
import java.util.Set;
import org.netbeans.modules.cnd.api.model.CsmClass;
import org.netbeans.modules.cnd.api.model.CsmDeclaration;
import org.netbeans.modules.cnd.api.model.CsmEnum;
import org.netbeans.modules.cnd.api.model.CsmFile;
import org.netbeans.modules.cnd.api.model.CsmFunction;
import org.netbeans.modules.cnd.api.model.CsmFunctionDefinition;
import org.netbeans.modules.cnd.api.model.CsmInclude;
import org.netbeans.modules.cnd.api.model.CsmMacro;
import org.netbeans.modules.cnd.api.model.CsmNamedElement;
import org.netbeans.modules.cnd.api.model.CsmNamespaceDefinition;
import org.netbeans.modules.cnd.api.model.CsmObject;
import org.netbeans.modules.cnd.api.model.CsmOffsetable;
import org.netbeans.modules.cnd.api.model.CsmParameter;
import org.netbeans.modules.cnd.api.model.CsmProject;
import org.netbeans.modules.cnd.api.model.CsmScope;
import org.netbeans.modules.cnd.api.model.CsmScopeElement;
import org.netbeans.modules.cnd.api.model.deep.CsmDeclarationStatement;
import org.netbeans.modules.cnd.api.model.deep.CsmStatement;
import org.netbeans.modules.cnd.api.model.util.CsmBaseUtilities;
import org.netbeans.modules.cnd.api.model.util.CsmKindUtilities;
import org.netbeans.modules.cnd.api.model.util.CsmSortUtilities;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.netbeans.modules.cnd.api.model.CsmNamespace;

/**
 *
 * @author vv159170
 */
public class CsmContextUtilities {
    private static final boolean DEBUG = Boolean.getBoolean("csm.utilities.trace");

    /** Creates a new instance of CsmScopeUtilities */
    private CsmContextUtilities() {
    }

    public static List/*<CsmDeclaration*/ findGlobalVariables(CsmProject prj) {
        CsmProjectContentResolver resolver = new CsmProjectContentResolver(prj);
        return resolver.getGlobalVariables("", false);
    }
    
    public static List<CsmDeclaration> findLocalDeclarations(CsmContext context, String strPrefix, boolean match, boolean caseSensitive) {
        return findLocalDeclarations(context, strPrefix, match, caseSensitive, true/*include file locals*/, false/*include function locals*/);
    }

    public static List<CsmDeclaration> findFileLocalVariables(CsmContext context) {
        return findFileLocalVariables(context, "", false, false);
    }

    public static List<CsmDeclaration> findFileLocalVariables(CsmContext context, String strPrefix, boolean match, boolean caseSensitive) {
        return findLocalDeclarations(context, strPrefix, match, caseSensitive, true/*include file locals*/, false/*exclude function locals*/);
    }
    
    public static List<CsmDeclaration> findFunctionLocalVariables(CsmContext context) {
        List<CsmDeclaration> decls = findFunctionLocalDeclarations(context, "", false, false);
        List<CsmDeclaration> out = new ArrayList<CsmDeclaration>(decls.size());
        for (CsmDeclaration elem : decls) {
            if (CsmKindUtilities.isVariable(elem)) {
                out.add(elem);
            }
        }
        return out;        
    }

    public static List<CsmDeclaration> findFunctionLocalDeclarations(CsmContext context, String strPrefix, boolean match, boolean caseSensitive) {
        List<CsmDeclaration> decls = findLocalDeclarations(context, strPrefix, match, caseSensitive, false/*do not include file locals*/, true/*include function locals*/);
        return decls;
    }

    private static final int FILE_LOCAL_MACROS = 0;
    private static final int FILE_PROJECT_LOCAL_MACROS = 1;
    private static final int FILE_LIB_LOCAL_MACROS = 2;
    private static final int PROJECT_MACROS = 3;
    private static final int LIB_MACROS = 4;
    public static List<CsmMacro> findFileLocalMacros(CsmContext context, String strPrefix, boolean match, boolean caseSensitive) {
        return findMacros(context, strPrefix, match, caseSensitive, FILE_LOCAL_MACROS);
    }

    public static List<CsmMacro> findFileIncludedProjectMacros(CsmContext context, String strPrefix, boolean match, boolean caseSensitive) {
        return findMacros(context, strPrefix, match, caseSensitive, FILE_PROJECT_LOCAL_MACROS);
    }

    public static List<CsmMacro> findFileIncludedLibMacros(CsmContext context, String strPrefix, boolean match, boolean caseSensitive) {
        return findMacros(context, strPrefix, match, caseSensitive, FILE_LIB_LOCAL_MACROS);
    }

    public static List<CsmMacro> findProjectMacros(CsmContext context, String strPrefix, boolean match, boolean caseSensitive) {
        return findMacros(context, strPrefix, match, caseSensitive, PROJECT_MACROS);
    }

    public static List<CsmMacro> findLibMacros(CsmContext context, String strPrefix, boolean match, boolean caseSensitive) {
        return findMacros(context, strPrefix, match, caseSensitive, LIB_MACROS);
    }


    private static List<CsmMacro> findMacros(CsmContext context, String strPrefix, 
            boolean match, boolean caseSensitive, int kind) {
        List res = new ArrayList();
        for (Iterator itContext = context.iterator(); itContext.hasNext();) {
            CsmContext.CsmContextEntry entry = (CsmContext.CsmContextEntry) itContext.next();
            CsmScope scope = entry.getScope();
            int offsetInScope = entry.getOffset();
            if (CsmKindUtilities.isFile(scope)){
                CsmFile file = (CsmFile)scope;
                switch (kind) {
                    case FILE_LOCAL_MACROS:
                        getFileLocalMacros(file, res, new HashSet(), strPrefix, match, caseSensitive);
                        break;
                    case FILE_PROJECT_LOCAL_MACROS:
                        gatherProjectIncludedMacros(file, res, false, strPrefix, match, caseSensitive);
                        break;
                    case FILE_LIB_LOCAL_MACROS:
                        gatherLibIncludedMacros(file, res, false, strPrefix, match, caseSensitive);
                        break;
                    case PROJECT_MACROS:
                        gatherProjectIncludedMacros(file, res, true, strPrefix, match, caseSensitive);
                        break;
                    case LIB_MACROS:
                        gatherLibIncludedMacros(file, res, true, strPrefix, match, caseSensitive);
                        break;
                }
            }
        }
        return res;
    }
    
    private static void getFileLocalMacros(CsmFile file, List res, Set alredyInList, String strPrefix, boolean match, boolean caseSensitive){
        for (Iterator itFile = file.getMacros().iterator(); itFile.hasNext();) {
            CsmMacro macro = (CsmMacro) itFile.next();
            //if (macro.getStartOffset() > offsetInScope) {
            //    break;
            //}
            String name = macro.getName().toString();
            if (!alredyInList.contains(name) && CsmSortUtilities.matchName(name, strPrefix, match, caseSensitive)) {
                res.add(macro);
                alredyInList.add(name);
            }
        }
    }

    private static void gatherProjectIncludedMacros(CsmFile file, List res, boolean all, String strPrefix,  boolean match, boolean caseSensitive) {
        CsmProject prj = file.getProject();
        if (!all) {
            gatherIncludeMacros(file, prj, true, new HashSet(), new HashSet(), res, strPrefix, match, caseSensitive);
        } else {
            Set alredyInList = new HashSet();
            for(Iterator i = prj.getHeaderFiles().iterator(); i.hasNext();){
                getFileLocalMacros((CsmFile)i.next(), res, alredyInList, strPrefix, match, caseSensitive);
            }
        }
    }

    private static void gatherLibIncludedMacros(CsmFile file, List res, boolean all, String strPrefix, boolean match, boolean caseSensitive) {
        CsmProject prj = file.getProject();
        if (!all) {
            gatherIncludeMacros(file, prj, false, new HashSet(), new HashSet(), res, strPrefix, match, caseSensitive);
        } else {
            Set alredyInList = new HashSet();
            for(Iterator p = prj.getLibraries().iterator(); p.hasNext();){
                CsmProject lib = (CsmProject)p.next();
                for(Iterator i = lib.getHeaderFiles().iterator(); i.hasNext();){
                    getFileLocalMacros((CsmFile)i.next(), res, alredyInList, strPrefix, match, caseSensitive);
                }
            }
            
        }
    }
    
    private static void gatherIncludeMacros(CsmFile file, CsmProject prj, boolean own, Set visitedFiles, Set alredyInList, 
            List res, String strPrefix, boolean match, boolean caseSensitive) {
        if( visitedFiles.contains(file) ) {
            return;
        }
        visitedFiles.add(file);
        for (Iterator<CsmInclude> iter = file.getIncludes().iterator(); iter.hasNext();) {
            CsmInclude inc = iter.next();
            CsmFile incFile = inc.getIncludeFile();
            if( incFile != null ) {
                if (own) {
                    if (incFile.getProject() == prj) {
                        getFileLocalMacros(incFile, res, alredyInList, strPrefix, match, caseSensitive);
                        gatherIncludeMacros(incFile, prj, own, visitedFiles, alredyInList, res, strPrefix, match, caseSensitive);
                    }
                } else {
                    if (incFile.getProject() != prj) {
                        getFileLocalMacros(incFile, res, alredyInList, strPrefix, match, caseSensitive);
                    }
                    gatherIncludeMacros(incFile, prj, own, visitedFiles, alredyInList, res, strPrefix, match, caseSensitive);
                }
            }
        }
    }

    protected static List<CsmDeclaration> findLocalDeclarations(CsmContext context, String strPrefix, boolean match, boolean caseSensitive, boolean includeFileLocal, boolean includeFunctionVars) {
        List<CsmDeclaration> res = new ArrayList<CsmDeclaration>();
        boolean incAny = includeFileLocal || includeFunctionVars;
        assert (incAny) : "at least one must be true";
        boolean incAll = includeFileLocal && includeFunctionVars;
        for (Iterator it = context.iterator(); it.hasNext() && incAny;) {
            CsmContext.CsmContextEntry entry = (CsmContext.CsmContextEntry) it.next();
            boolean include = incAll;
            if (!include) {
                // check if something changed
                if (!includeFileLocal) {
                    assert (includeFunctionVars);
                    // if it wasn't necessary to include all file local variables, but now 
                    // we jump in function => mark that from now include all
                    if (CsmKindUtilities.isFunction(entry.getScope())) {
                        incAll = include = true;
                    }
                } else if (!includeFunctionVars) {
                    assert (includeFileLocal);
                    // we have sorted context entries => if we reached function or class =>
                    // skip function and all others
                    if (CsmKindUtilities.isFunction(entry.getScope()) ||
                            CsmKindUtilities.isClassifier(entry.getScope())) {
                        incAll = incAny = include = false;
                    } else {
                        include = true;
                    }
                }
            }
            if (include) {
                res = addEntryDeclarations(entry, res, context, strPrefix, match, caseSensitive);
            }
        }
        return res;
    }

    private static List<CsmDeclaration> addEntryDeclarations(CsmContext.CsmContextEntry entry, List<CsmDeclaration> decls, CsmContext fullContext,
                                                                String strPrefix, boolean match, boolean caseSensitive) {
        List<CsmDeclaration> newList = findEntryDeclarations(entry, fullContext, strPrefix, match, caseSensitive);
        return mergeDeclarations(decls, newList);
    }
    
    private static List<CsmDeclaration> findEntryDeclarations(CsmContext.CsmContextEntry entry, CsmContext fullContext,
                                                                String strPrefix, boolean match, boolean caseSensitive) {
        assert (entry != null) : "can't work on null entries";
        CsmScope scope = entry.getScope();
        int offsetInScope = entry.getOffset();
        List resList = new ArrayList();
        for (Iterator it = scope.getScopeElements().iterator(); it.hasNext();) {
            CsmScopeElement scpElem = (CsmScopeElement) it.next();
            if (canBreak(offsetInScope, scpElem, fullContext)) {
                break;
            }
            List<CsmDeclaration> declList = extractDeclarations(scpElem, strPrefix, match, caseSensitive);
            resList.addAll(declList);
        }
        return resList;
    }

    public static List<CsmDeclaration> findFileLocalEnumerators(CsmContext context, String strPrefix, boolean match, boolean caseSensitive) {
        List<CsmDeclaration> res = new ArrayList<CsmDeclaration>();
        for (Iterator itContext = context.iterator(); itContext.hasNext();) {
            CsmContext.CsmContextEntry entry = (CsmContext.CsmContextEntry) itContext.next();
            CsmScope scope = entry.getScope();
            int offsetInScope = entry.getOffset();
            if (CsmKindUtilities.isFile(scope)){
                CsmFile file = (CsmFile)scope;
                for (Iterator itFile = file.getDeclarations().iterator(); itFile.hasNext();) {
                    CsmDeclaration decl = (CsmDeclaration) itFile.next();
                    if (canBreak(offsetInScope, decl, context)) {
                        break;
                    }
                    if (CsmKindUtilities.isEnum(decl)) {
                        CsmEnum en = (CsmEnum)decl;
                        if (en.getName().length()==0){
                            addEnumerators(res, en, strPrefix, match, caseSensitive);
                        }
                    } else if (CsmKindUtilities.isNamespaceDefinition(decl) && decl.getName().length()==0){
                        CsmNamespaceDefinition ns = (CsmNamespaceDefinition)decl;
                        for(Iterator i = ns.getDeclarations().iterator(); i.hasNext();){
                            CsmDeclaration nsDecl = (CsmDeclaration) i.next();
                            if (canBreak(offsetInScope, nsDecl, context)) {
                                break;
                            }
                            if (CsmKindUtilities.isEnum(nsDecl)) {
                                CsmEnum en = (CsmEnum)nsDecl;
                                if (en.getName().length()==0){
                                    addEnumerators(res, en, strPrefix, match, caseSensitive);
                                }
                            }
                        }
                    }
                }
            }
        }
        return res;
    }

    private static void addEnumerators(List resList, CsmEnum en, String strPrefix, boolean match, boolean caseSensitive){
        for(Iterator i = en.getEnumerators().iterator(); i.hasNext();){
            CsmNamedElement scpElem = (CsmNamedElement) i.next();
            if (CsmSortUtilities.matchName(scpElem.getName(), strPrefix, match, caseSensitive)) {
                resList.add(scpElem);
            }
        }
    }
    
    private static boolean canBreak(int offsetInScope, CsmScopeElement elem, CsmContext fullContext) {
        // break if element already is in context
        // or element is after offset
        if (offsetInScope == CsmContext.CsmContextEntry.WHOLE_SCOPE) {
            return isInContext(fullContext, elem);
        } else if (CsmKindUtilities.isOffsetable(elem)) {
            return ((CsmOffsetable)elem).getStartOffset() >= offsetInScope || isInContext(fullContext, elem);
        }
        return isInContext(fullContext, elem);
    }
    
    private static List/*<CsmDeclaration>*/ mergeDeclarations(List/*<CsmDeclaration>*/ decls, List/*<CsmDeclaration>*/ newList) {
        // XXX: now just add all
        if (newList != null && newList.size() > 0) {
            decls.addAll(newList);
        }
        return decls;
    }

//    public static void updateContextObject(CsmObject obj, CsmContext context) {
//        if (context != null && obj != null) {
//            context.setLastObject(obj);
//        }
//    }
    
    public static void updateContextObject(CsmObject obj, int offset, CsmContext context) {
        if (context != null && obj != null) {
            context.setLastObject(obj);
        }
    }
    
//    public static void updateContext(CsmObject obj, CsmContext context) {
//        if (context != null && CsmKindUtilities.isScope(obj)) {
//            context.add((CsmScope)obj);
//        } else if (CsmKindUtilities.isOffsetable(obj)) {
//            updateContextObject(obj, context);
//        }
//    }
    
    public static void updateContext(CsmObject obj, int offset, CsmContext context) {
        if (context != null) {
            if (CsmKindUtilities.isScope(obj)) {
                context.add((CsmScope)obj, offset);
            } else if (CsmKindUtilities.isOffsetable(obj)) {
                updateContextObject(obj, offset, context);
            }
        }
    }
    
    private static boolean isInContext(CsmContext context, CsmObject obj) {
        // XXX: in fact better to start from end
        for (Iterator it = context.iterator(); it.hasNext();) {
            CsmContext.CsmContextEntry elem = (CsmContext.CsmContextEntry) it.next();
            if (obj.equals(elem.getScope())) {
                return true;
            }
        }
        return false;
    }

    private static List/*<CsmDeclaration>*/ extractDeclarations(CsmScopeElement scpElem,
                                                        String strPrefix, boolean match, boolean caseSensitive) {
        List list = new ArrayList();
        if (CsmKindUtilities.isDeclaration(scpElem)) {
            if (CsmSortUtilities.matchName((( CsmNamedElement)scpElem).getName(), strPrefix, match, caseSensitive)) {
                boolean add = true;
                // special check for "var args" parameters
                if (CsmKindUtilities.isParamVariable(scpElem)) {
                    add = !((CsmParameter)scpElem).isVarArgs();
                }
                if (add) {
                    list.add(scpElem);
                }
            }
        } else if (CsmKindUtilities.isStatement(scpElem)) {
            CsmStatement.Kind kind = ((CsmStatement)scpElem).getKind();
            if (kind == CsmStatement.Kind.DECLARATION) {
                List<CsmDeclaration> decls = ((CsmDeclarationStatement)scpElem).getDeclarators();
                List<CsmNamedElement> listByName = CsmSortUtilities.filterList(decls, strPrefix, match, caseSensitive);
                list.addAll(listByName);
                for (CsmDeclaration elem : decls) {
                    if (CsmKindUtilities.isEnum(elem)) {
                        listByName = CsmSortUtilities.filterList(((CsmEnum)elem).getEnumerators(), strPrefix, match, caseSensitive);
                        list.addAll(listByName);
                    }
                }
            }
        }
        return list;
    }

    public static CsmClass getClass(CsmContext context, boolean checkFunDefition) {
        CsmClass clazz = null;
        for (Iterator it = context.iterator(); it.hasNext();) {
            CsmContext.CsmContextEntry elem = (CsmContext.CsmContextEntry) it.next();
            if (CsmKindUtilities.isClass(elem.getScope())) {
                clazz = (CsmClass)elem.getScope();
                break;
            }
        }        
        if (clazz == null && checkFunDefition) {
            // check if we in one of class's method
            CsmFunction fun = getFunction(context);
            clazz = fun == null ? null : CsmBaseUtilities.getFunctionClass(fun);
        }
        return clazz;
    }    
    
    public static CsmFunction getFunction(CsmContext context) {
        CsmFunction fun = null;
        for (Iterator it = context.iterator(); it.hasNext();) {
            CsmContext.CsmContextEntry elem = (CsmContext.CsmContextEntry) it.next();
            if (CsmKindUtilities.isFunction(elem.getScope())) {
                fun = (CsmFunction)elem.getScope();
                break;
            }
        }
        return fun;
    }    
    
    public static CsmFunctionDefinition getFunctionDefinition(CsmContext context) {
        CsmFunctionDefinition fun = null;
        for (Iterator it = context.iterator(); it.hasNext();) {
            CsmContext.CsmContextEntry elem = (CsmContext.CsmContextEntry) it.next();
            if (CsmKindUtilities.isFunctionDefinition(elem.getScope())) {
                fun = (CsmFunctionDefinition)elem.getScope();
                break;
            }
        }        
        return fun;
    }   
    
    public static CsmNamespace getNamespace(CsmContext context) {
        CsmFunction fun = getFunction(context);
        CsmNamespace ns = null;
        if (fun != null) {
            ns = getFunctionNamespace(fun);
        } else {
            CsmClass cls = CsmContextUtilities.getClass(context, false);
            ns = cls == null ? null : getClassNamespace(cls);
        }
        return ns;
    }

    private static CsmNamespace getFunctionNamespace(CsmFunction fun) {
        if (CsmKindUtilities.isFunctionDefinition(fun)) {
            CsmFunction decl = ((CsmFunctionDefinition) fun).getDeclaration();
            fun = decl != null ? decl : fun;
        }
        if (fun != null) {
            CsmScope scope = fun.getScope();
            if (CsmKindUtilities.isNamespace(scope)) {
                CsmNamespace ns = (CsmNamespace) scope;
                return ns;
            } else if (CsmKindUtilities.isClass(scope)) {
                return getClassNamespace((CsmClass) scope);
            }
        }
        return null;
    }

    private static CsmNamespace getClassNamespace(CsmClass cls) {
        CsmScope scope = cls.getScope();
        while (scope != null) {
            if (CsmKindUtilities.isNamespace(scope)) {
                return (CsmNamespace) scope;
            }
            if (CsmKindUtilities.isScopeElement(scope)) {
                scope = ((CsmScopeElement) scope).getScope();
            } else {
                break;
            }
        }
        return null;
    }
    
    public static boolean isInFunctionBody(CsmContext context, int offset) {
        CsmFunctionDefinition funDef = getFunctionDefinition(context);
        return (funDef == null) ? false : CsmOffsetUtilities.isInObject(funDef.getBody(), offset);
    }   
    
    public static boolean isInFunction(CsmContext context, int offset) {
        CsmFunction fun = getFunction(context);
        return fun != null;
    }     
    
    public static boolean isInType(CsmContext context, int offset) {
        CsmObject last = context.getLastObject();
        return CsmKindUtilities.isType(last) && CsmOffsetUtilities.isInObject(last, offset);
    }    
}
