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

package org.netbeans.modules.cnd.modelimpl.impl.services;

import org.netbeans.modules.cnd.antlr.TokenStream;
import org.netbeans.modules.cnd.antlr.TokenStreamException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.netbeans.modules.cnd.api.model.*;
import org.netbeans.modules.cnd.api.model.deep.CsmGotoStatement;
import org.netbeans.modules.cnd.api.model.deep.CsmLabel;
import org.netbeans.modules.cnd.api.model.services.CsmFileReferences;
import org.netbeans.modules.cnd.api.model.services.CsmFileReferences.ReferenceVisitor;
import org.netbeans.modules.cnd.api.model.util.CsmBaseUtilities;
import org.netbeans.modules.cnd.api.model.util.CsmKindUtilities;
import org.netbeans.modules.cnd.api.model.xref.CsmReference;
import org.netbeans.modules.cnd.api.model.xref.CsmReferenceKind;
import org.netbeans.modules.cnd.api.model.xref.CsmReferenceRepository;
import org.netbeans.modules.cnd.api.model.xref.CsmReferenceResolver;
import org.netbeans.modules.cnd.api.model.xref.CsmReferenceSupport;
import org.netbeans.modules.cnd.apt.support.APTPreprocHandler;
import org.netbeans.modules.cnd.apt.support.APTToken;
import org.netbeans.modules.cnd.apt.support.APTTokenStreamBuilder;
import org.netbeans.modules.cnd.apt.support.APTTokenTypes;
import org.netbeans.modules.cnd.apt.utils.APTCommentsFilter;
import org.netbeans.modules.cnd.apt.utils.APTUtils;
import org.netbeans.modules.cnd.modelimpl.csm.core.FileBuffer;
import org.netbeans.modules.cnd.modelimpl.csm.core.FileImpl;
import org.netbeans.modules.cnd.modelimpl.csm.core.ProjectBase;
import org.netbeans.modules.cnd.modelimpl.csm.core.ReferencesIndex;
import org.netbeans.modules.cnd.modelimpl.debug.DiagnosticExceptoins;
import org.netbeans.modules.cnd.modelimpl.debug.TraceFlags;
import org.netbeans.modules.cnd.modelimpl.uid.UIDCsmConverter;
import org.openide.util.CharSequences;
import org.openide.util.Exceptions;

/**
 * prototype implementation of service
 * @author Vladimir Voskresensky
 */
@org.openide.util.lookup.ServiceProvider(service=org.netbeans.modules.cnd.api.model.xref.CsmReferenceRepository.class)
public final class ReferenceRepositoryImpl extends CsmReferenceRepository {
    
    public ReferenceRepositoryImpl() {
    }
    
    @Override
    public Collection<CsmReference> getReferences(CsmObject target, CsmProject project, Set<CsmReferenceKind> kinds, Interrupter interrupter) {
        long time = System.currentTimeMillis();
        try {
            if (Boolean.getBoolean("cnd.model.index.enabled") && Boolean.getBoolean("cnd.model.global.index")) {
                return ReferencesIndex.getAllReferences(UIDCsmConverter.objectToUID(target));
            }
            boolean unboxInstantiation = true;
            CsmObject[] decDef = CsmBaseUtilities.getDefinitionDeclaration(target, unboxInstantiation);
            CsmObject decl = decDef[0];
            CsmObject def = decDef[1];

            CsmScope scope = getDeclarationScope(decl);
            CsmFile scopeFile = CsmKindUtilities.isOffsetable(scope) ? ((CsmOffsetable)scope).getContainingFile() : null;
            List<CsmReference> out;
            Collection<FileImpl> files;
            if (scopeFile instanceof FileImpl) {
                out = new ArrayList<CsmReference>(10);
                CsmOffsetable offs = (CsmOffsetable)scope;
                out.addAll(getReferences(decl, def, (FileImpl)scopeFile, kinds, unboxInstantiation, offs.getStartOffset(), offs.getEndOffset(), interrupter));
            } else {
                if (Boolean.getBoolean("cnd.model.index.enabled")) {
                    Collection<CsmUID<CsmFile>> allFiles = ReferencesIndex.getRelevantFiles(UIDCsmConverter.objectToUID(target));
                    files = new ArrayList<FileImpl>(allFiles.size());
                    for (CsmUID<CsmFile> csmUID : allFiles) {
                        files.add((FileImpl)UIDCsmConverter.UIDtoFile(csmUID));
                    }
                } else {
                    if (!(project instanceof ProjectBase)) {
                        return Collections.<CsmReference>emptyList();
                    }
                    ProjectBase basePrj = (ProjectBase) project;
                    files = basePrj.getAllFileImpls();
                }
                out = new ArrayList<CsmReference>(files.size() * 10);
                for (FileImpl file : files) {
                    if (interrupter != null && interrupter.cancelled()) {
                        break;
                    }
                    out.addAll(getReferences(decl, def, file, kinds,unboxInstantiation, 0, Integer.MAX_VALUE, interrupter));
                }
            }
            return out;
        } finally {
            System.err.println("getReferences took " + (System.currentTimeMillis() - time));
        } 
    }
    
    @Override
    public Collection<CsmReference> getReferences(CsmObject target, CsmFile file, Set<CsmReferenceKind> kinds, Interrupter interrupter) {
        CsmScope scope = getDeclarationScope(target);
        CsmFile scopeFile = CsmKindUtilities.isOffsetable(scope) ? ((CsmOffsetable)scope).getContainingFile() : null;
        if (!(file instanceof FileImpl)) {
            return Collections.<CsmReference>emptyList();
        } else if (scopeFile != null && !scopeFile.equals(file)) {
            // asked file is not scope file for target object
            return Collections.<CsmReference>emptyList();
        } else {
            boolean unboxInstantiation = true;
            CsmObject[] decDef = CsmBaseUtilities.getDefinitionDeclaration(target, unboxInstantiation);
            CsmObject decl = decDef[0];
            CsmObject def = decDef[1];            
            int start = 0, end = Integer.MAX_VALUE;
            if (CsmKindUtilities.isOffsetable(scope)) {
                start = ((CsmOffsetable)scope).getStartOffset();
                end = ((CsmOffsetable)scope).getEndOffset();
            }
            return getReferences(decl, def, (FileImpl)file, kinds, unboxInstantiation, start,end, interrupter);
        }
    }
    
    public Map<CsmObject, Collection<CsmReference>> getReferences(CsmObject[] targets, CsmProject project, Set<CsmReferenceKind> kinds, Interrupter interrupter) {
        Map<CsmObject, Collection<CsmReference>> out = new HashMap<CsmObject, Collection<CsmReference>>(targets.length);
        for (CsmObject target : targets) {
            if (interrupter != null && interrupter.cancelled()) {
                break;
            }
            out.put(target, getReferences(target, project, kinds, interrupter));
        }
        return out;
    }
    
    @Override
    public Collection<CsmReference> getReferences(CsmObject[] targets, CsmFile file, Set<CsmReferenceKind> kinds, Interrupter interrupter) {
        Collection<CsmReference> refs = new LinkedHashSet<CsmReference>(1024);
        // TODO: optimize performance
        for (CsmObject target : targets) {            
            refs.addAll(getReferences(target, file, kinds, interrupter));
        }
        if (!refs.isEmpty() && targets.length > 1) {
            // if only one target, then collection is already sorted
            List<CsmReference> sortedRefs = new ArrayList<CsmReference>(refs);
            Collections.sort(sortedRefs, new Comparator<CsmReference>() {
                @Override
                public int compare(CsmReference o1, CsmReference o2) {
                    return o1.getStartOffset() - o2.getStartOffset();
                }
            });    
            refs = sortedRefs;
        }
        return refs;
    }
    
    ////////////////////////////////////////////////////////////////////////////
    // prototype of impl
    
    private Collection<CsmReference> getReferences(final CsmObject targetDecl, final CsmObject targetDef, FileImpl file,
            final Set<CsmReferenceKind> kinds, final boolean unboxInstantiation, int startOffset, int endOffset, final Interrupter interrupter) {
        if (Boolean.getBoolean("cnd.model.index.enabled")) {
            return file.getReferences(targetDef == null ? Collections.singleton(targetDecl) : Arrays.asList(targetDecl, targetDef));
        }
        assert targetDecl != null;
        assert file != null;
        CharSequence name = "";
        if (CsmKindUtilities.isNamedElement(targetDecl)) {
            name = ((CsmNamedElement)targetDecl).getName();
        } else if (CsmKindUtilities.isStatement(targetDecl)) {
            if (targetDecl instanceof CsmLabel) {
                name = ((CsmLabel)targetDecl).getLabel();
            } else if (targetDecl instanceof CsmGotoStatement){
                name = ((CsmGotoStatement)targetDecl).getLabel();
            }
        }
        if (name.length() == 0) {
            if (TraceFlags.TRACE_XREF_REPOSITORY) {
                System.err.println("resolving unnamed element is not yet supported " + targetDecl);
            }
            return Collections.<CsmReference>emptyList();
        }
        name = CharSequences.create(name);
        if (TraceFlags.TRACE_XREF_REPOSITORY) {
            System.err.println("resolving " + name + " in file " + file.getAbsolutePath());
        }
        //long time = 0;
        //if (TraceFlags.TRACE_XREF_REPOSITORY) {
        //    time = System.currentTimeMillis();
        //}
        if (!fastDetect(targetDecl, targetDef, file, name)){
            return Collections.<CsmReference>emptyList();
        }
        Collection<APTToken> tokens = getTokensToResolve(file, name, startOffset, endOffset);
        if (TraceFlags.TRACE_XREF_REPOSITORY) {
            //time = System.currentTimeMillis() - time;
            System.err.println("collecting tokens");
        }
        Collection<CsmReference> refs = new ArrayList<CsmReference>(20);
        for (APTToken token : tokens) {
            if (interrupter != null && interrupter.cancelled()){
                break;
            }
            CsmReference ref = CsmReferenceResolver.getDefault().findReference(file, token.getOffset());
            if (ref != null) {
                // this is candidate to resolve
                refs.add(ref);
            }
        }
        final Collection<CsmReference> out = new ArrayList<CsmReference>(20);
        ReferenceVisitor visitor = new ReferenceVisitor() {
            @Override
            public void visit(CsmReference ref) {
                if (interrupter != null && interrupter.cancelled()){
                    return;
                }
                if (acceptReference(ref, targetDecl, targetDef, kinds, unboxInstantiation)) {
                    out.add(ref);
                }
            }
        };
        CsmFileReferences.getDefault().visit(refs, visitor);
        return out;
    }

    private static final boolean checkFileAttainability = false;
    private boolean fastDetect(final CsmObject targetDecl, final CsmObject targetDef, FileImpl file, CharSequence name){
        // in prototype use just unexpanded identifier tokens in file
        if (name.length() == 0 || !hasName(file, name)){
            return false;
        }
        if (checkFileAttainability) {
            // in prototype try to check just attainability target declaration/definition from file
            // it does not work if refereced object point to external declaration
            if (CsmKindUtilities.isOffsetableDeclaration(targetDecl)){
                CsmFile targetFile = ((CsmOffsetableDeclaration)targetDecl).getContainingFile();
                boolean included = ((ProjectBase)file.getProject()).getGraphStorage().isFileIncluded(file, targetFile);
                if (!included) {
                    if (targetDef != null) {
                        if (CsmKindUtilities.isOffsetableDeclaration(targetDef)) {
                            targetFile = ((CsmOffsetableDeclaration)targetDef).getContainingFile();
                            included = ((ProjectBase)file.getProject()).getGraphStorage().isFileIncluded(file, targetFile);
                            if (!included){
                                return false;
                            }
                        }
                    } else {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    // Fast check of name.
    private boolean hasName(FileImpl file, CharSequence name){
        try {
            if (file.isValid() && name.length() > 0) {
                FileBuffer buffer = file.getBuffer();
                if (buffer == null){
                    return false;
                }
                char[] charBuffer = buffer.getCharBuffer();
                char first = name.charAt(0);
                int nameLength = name.length();
                int bufLength = charBuffer.length;
                loop:for (int i = 0; i < bufLength-nameLength; i++) {
                    if (charBuffer[i] == first) {
                        for(int j = 1; j < nameLength; j++) {
                            if (name.charAt(j) != charBuffer[i+j]) {
                                continue loop;
                            }
                        }
                        if (i > 0) {
                            char prev = charBuffer[i-1];
                            if (prev == '_' || prev == '$' || prev == '~' || Character.isLetterOrDigit(prev)) {
                                continue;
                            }
                        }
                        if (i + nameLength + 1 < bufLength) {
                            char next = charBuffer[i + nameLength];
                            if (next == '_' || next == '$' || Character.isLetterOrDigit(next)) {
                                continue;
                            }
                        }
                        return true;
                    }
                }
                return false;
            }
        } catch (FileNotFoundException ex) {
            // TODO FileBuffer should provide method isValid()
            // Do nothing, it seems temporary file.
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        return false;
    }
    
    private Collection<APTToken> getTokensToResolve(FileImpl file, CharSequence name, int startOffset, int endOffset) {
        TokenStream ts = getTokenStream(file);
        Collection<APTToken> tokens = new ArrayList<APTToken>(100);
        boolean destructor = false;
        if (name.charAt(0) == '~') { // NOI18N
            destructor = true;
            name = name.subSequence(1, name.length());
        }
        if (ts != null) {
            try {
                APTToken token = (APTToken) ts.nextToken();
                APTToken prev = null;
                while (!APTUtils.isEOF(token)) {
                    if (token.getOffset() >= startOffset) {
                        int id = token.getType();
                        if ((id == APTTokenTypes.ID || id == APTTokenTypes.ID_DEFINED) &&
                                name.equals(token.getTextID())) {
                            // this is candidate to resolve
                            if (!destructor || (prev != null && prev.getType() == APTTokenTypes.TILDE)) {
                                tokens.add(token);
                            }
                        }
                    }
                    if (token.getEndOffset() > endOffset) {
                        break;
                    }
                    prev = token;
                    token = (APTToken) ts.nextToken();
                }
            } catch (TokenStreamException ex) {
                // IZ#163088 : unexpected char
                APTUtils.LOG.log(Level.SEVERE, ex.getMessage());
            }
        }
        return tokens;
    }
    
    private TokenStream getTokenStream(FileImpl file) {
        // build token stream for file
        TokenStream ts = null;
        try {
            if (file.isValid()) {
                FileBuffer buffer = file.getBuffer();
                if (buffer != null){
                    ts = APTTokenStreamBuilder.buildTokenStream(file.getAbsolutePath(), buffer.getCharBuffer(), file.getFileLanguage());
                }
            }
        } catch (IOException ex) {
            DiagnosticExceptoins.register(ex);
            ts = null;
        }
        if (ts == null || !file.isValid()) {
            return null;
        }
        APTPreprocHandler.State ppState = null;
        Collection<APTPreprocHandler.State> preprocStates = file.getProjectImpl(false).getPreprocStates(file);
        if (!preprocStates.isEmpty()) {
            // use start file from one of states (i.e. first)
            ppState = preprocStates.iterator().next();
        }
        return file.getLanguageFilter(ppState).getFilteredStream( new APTCommentsFilter(ts));
    }

    private boolean acceptReference(CsmReference ref, CsmObject targetDecl, CsmObject targetDef, 
            Set<CsmReferenceKind> kinds, boolean unboxInstantiation) {
        assert targetDecl != null;
        boolean accept = false;
        CsmObject referencedObj = ref == null ? null : ref.getReferencedObject();
        if (unboxInstantiation && CsmKindUtilities.isTemplateInstantiation(referencedObj)) {
            referencedObj = ((CsmInstantiation)referencedObj).getTemplateDeclaration();
        }
        if (CsmReferenceSupport.sameDeclaration(targetDecl, referencedObj) || checkDefinitions(targetDef, referencedObj)) {
            accept = CsmReferenceResolver.getDefault().isKindOf(ref, kinds);
        }
        return accept;
    }   

    private boolean checkDefinitions(CsmObject targetDef, CsmObject referencedObj) {
        if (targetDef == null) {
            return false;
        }
        if (targetDef.equals(referencedObj)) {
            return true;
        }
        if (CsmKindUtilities.isFunction(referencedObj)) {
            CsmFunctionDefinition refDef = ((CsmFunction) referencedObj).getDefinition();
            return targetDef.equals(refDef);
        }
        return false;
    }

    private CsmScope getDeclarationScope(CsmObject decl) {
        assert decl != null;
        CsmObject scopeElem = decl;
        while (CsmKindUtilities.isScopeElement(scopeElem)) {
            CsmScope scope = ((CsmScopeElement)scopeElem).getScope();
            if (CsmKindUtilities.isFunction(scope)) {
                return ((CsmFunction)scope);
            } else if (CsmKindUtilities.isScopeElement(scope)) {
                scopeElem = ((CsmScopeElement)scope);
            } else {
                break;
            }
        }
        return null;        
    }
}
