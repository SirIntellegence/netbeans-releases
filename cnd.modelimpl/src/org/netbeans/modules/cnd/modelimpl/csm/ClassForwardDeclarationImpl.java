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

package org.netbeans.modules.cnd.modelimpl.csm;

import org.netbeans.modules.cnd.modelimpl.csm.resolver.Resolver;
import org.netbeans.modules.cnd.modelimpl.csm.resolver.ResolverFactory;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.netbeans.modules.cnd.api.model.*;
import org.netbeans.modules.cnd.antlr.collections.AST;
import java.util.ArrayList;
import java.util.Collections;
import java.util.StringTokenizer;
import org.netbeans.modules.cnd.api.model.util.CsmKindUtilities;
import org.netbeans.modules.cnd.api.model.util.UIDs;
import org.netbeans.modules.cnd.modelimpl.parser.generated.CPPTokenTypes;
import org.netbeans.modules.cnd.modelimpl.csm.core.*;
import org.netbeans.modules.cnd.modelimpl.repository.PersistentUtils;
import org.netbeans.modules.cnd.modelimpl.repository.RepositoryUtils;
import org.netbeans.modules.cnd.modelimpl.textcache.NameCache;
import org.netbeans.modules.cnd.modelimpl.textcache.QualifiedNameCache;
import org.netbeans.modules.cnd.modelimpl.uid.UIDUtilities;
import org.netbeans.modules.cnd.repository.spi.RepositoryDataInput;
import org.netbeans.modules.cnd.repository.spi.RepositoryDataOutput;
import org.openide.util.CharSequences;

/**
 *
 * @author Vladimir Kvasihn
 */
public class ClassForwardDeclarationImpl extends OffsetableDeclarationBase<CsmClassForwardDeclaration> 
                                         implements CsmClassForwardDeclaration, CsmTemplate {
    private final CharSequence name;
    private CharSequence[] nameParts;
    private int lastParseCount = -1;    
    private int lastFileID = -1;
    private CsmObject lastResult;

    private final TemplateDescriptor templateDescriptor;
    
    protected ClassForwardDeclarationImpl(AST ast, CsmFile file, boolean global) {
        super(file, getClassForwardStartOffset(ast), getClassForwardEndOffset(ast));
        AST qid = AstUtil.findChildOfType(ast, CPPTokenTypes.CSM_QUALIFIED_ID);
        assert qid != null;
        name = QualifiedNameCache.getManager().getString(AstRenderer.getQualifiedName(qid));
        nameParts = initNameParts(qid);
        this.templateDescriptor = TemplateDescriptor.createIfNeeded(ast, file, null, global);
    }

    public static ClassForwardDeclarationImpl create(AST ast, CsmFile file, CsmScope scope, MutableDeclarationsContainer container, boolean global) {
        ClassForwardDeclarationImpl cfdi = new ClassForwardDeclarationImpl(ast, file, global);
        if (container != null) {
            container.addDeclaration(cfdi);
        }
        cfdi.init(ast, scope, global);
        return cfdi;
    }

    public static ClassForwardDeclarationImpl create(AST ast, CsmFile file, boolean global) {
         ClassForwardDeclarationImpl cfdi = new ClassForwardDeclarationImpl(ast, file, global);
         if (!global) {
              Utils.setSelfUID(cfdi);
         }
        return cfdi;
    }

    private static int getClassForwardStartOffset(AST ast) {        
        AST firstChild = ast.getFirstChild();
        if (firstChild != null && firstChild.getType() == CPPTokenTypes.LITERAL_typedef) {
            AST secondChild = firstChild.getNextSibling();
            if (secondChild != null &&
                    (secondChild.getType() == CPPTokenTypes.LITERAL_struct ||
                    secondChild.getType() == CPPTokenTypes.LITERAL_union ||
                    secondChild.getType() == CPPTokenTypes.LITERAL_class)) {
                return getStartOffset(secondChild);
            }
        }
        return getStartOffset(ast);        
    }
    
    private static int getClassForwardEndOffset(AST ast) {
        AST firstChild = AstRenderer.getFirstChildSkipQualifiers(ast);
        if (firstChild != null) {
            if(firstChild.getType() == CPPTokenTypes.LITERAL_typedef) {
                AST qid = AstUtil.findChildOfType(ast, CPPTokenTypes.CSM_QUALIFIED_ID);
                if(qid != null) {
                    return getEndOffset(qid);
                }
            } else if(firstChild.getType() == CPPTokenTypes.LITERAL_class ||
                firstChild.getType() == CPPTokenTypes.LITERAL_struct ||
                firstChild.getType() == CPPTokenTypes.LITERAL_union) {
                AST qid = AstUtil.findChildOfType(ast, CPPTokenTypes.CSM_QUALIFIED_ID);
                if(qid != null) {
                    AST nextSibling = qid.getNextSibling();
                    if(nextSibling != null && nextSibling.getType() == CPPTokenTypes.SEMICOLON) {
                        return getEndOffset(nextSibling);
                    } else {
                        return getEndOffset(qid);
                    }
                }                
            }
        }
        return getEndOffset(ast);        
    }
    
    @Override
    public CsmScope getScope() {
        return getContainingFile();
    }

    @Override
    public CharSequence getName() {
        return name;
    }

    @Override
    public CharSequence getQualifiedName() {
        return name;
    }

//    Moved to OffsetableDeclarationBase
//    public String getUniqueName() {
//        return getQualifiedName();
//    }
    
    @Override
    public CsmDeclaration.Kind getKind() {
        return CsmDeclaration.Kind.CLASS_FORWARD_DECLARATION;
    }

    @Override
    public CsmClass getCsmClass() {
        CsmObject o = resolve();
        return (o instanceof CsmClass) ? (CsmClass) o : (CsmClass) null;
    }

    @Override
    public boolean isTemplate() {
        return templateDescriptor != null;
    }

    @Override
    public boolean isSpecialization() {
        return false;
    }

    @Override
    public boolean isExplicitSpecialization() {
        return false;
    }
    
    @Override
    public List<CsmTemplateParameter> getTemplateParameters() {
        return (templateDescriptor != null) ? templateDescriptor.getTemplateParameters() : Collections.<CsmTemplateParameter>emptyList();
    }

    @Override
    public CharSequence getDisplayName() {
        return (templateDescriptor != null) ? CharSequences.create((getName().toString() + templateDescriptor.getTemplateSuffix())) : getName(); // NOI18N
    }

    private CharSequence[] initNameParts(AST qid) {
        if( qid != null ) {
            return AstRenderer.getNameTokens(qid);
        }
        return new CharSequence[0];
    }
    
    private boolean needRecount(int newParseCount, Resolver currentResolver) {
        if (lastParseCount != newParseCount) {
            return true;
        }
        CsmFile startFile = null;
        if (currentResolver != null) {
            startFile = currentResolver.getStartFile();
        }
        if (startFile == null) {
            startFile = getContainingFile();
        }
        int fileID = UIDUtilities.getFileID(UIDs.get(startFile));
        if (lastFileID != fileID) {
            return true;
        }
        return false;
    }

    private void updateCache(int newParseCount, Resolver currentResolver) {
        lastParseCount = newParseCount;
        CsmFile startFile = null;
        if (currentResolver != null) {
            startFile = currentResolver.getStartFile();
        }
        if (startFile == null) {
            startFile = getContainingFile();
        }
        int fileID = UIDUtilities.getFileID(UIDs.get(startFile));
        lastFileID = fileID;
    }

    private CsmObject resolve() {
        int newParseCount = FileImpl.getParseCount();
        Resolver currentResolver = ResolverFactory.getCurrentResolver();
        CsmObject result = lastResult;
        if (needRecount(newParseCount, currentResolver)) {
            Resolver aResolver = ResolverFactory.createResolver(this);
            try {
                result = aResolver.resolve(nameParts, Resolver.CLASS);
            } finally {
                ResolverFactory.releaseResolver(aResolver);
            }
            if (result == null || ForwardClass.isForwardClass((CsmDeclaration)result) || CsmKindUtilities.isClassForwardDeclaration(result)) {
                Resolver aResolver2 = ResolverFactory.createResolver(this);
                try {
                    CharSequence[] nameParts2 = new CharSequence[1];
                    nameParts2[0] = nameParts[nameParts.length - 1];
                    CsmObject result2 = aResolver2.resolve(nameParts2, Resolver.CLASS);
                    if(result == null || (result2 != null && !ForwardClass.isForwardClass((CsmDeclaration)result2) && !CsmKindUtilities.isClassForwardDeclaration(result2))) {
                        result = result2;
                    }
                } finally {
                    ResolverFactory.releaseResolver(aResolver2);
                }
            }
            if(result == null || CsmKindUtilities.isTypedef(result)) {
                result = ((ProjectBase) getContainingFile().getProject()).findClassifier(name);
            }
            if (result == null) {
                result = ((ProjectBase) getContainingFile().getProject()).getDummyForUnresolved(nameParts, getContainingFile(), getStartOffset());
            }
            lastResult = result;
            updateCache(newParseCount, currentResolver);
        //} else {
        //    System.err.println("cache hit ClassForwardDeclarationImpl");
        }
        // NOTE: result shouldn't be cached. 
        // class forward could mean different things depending on other includes
        return result;
    }

    public Collection<CsmScopeElement> getScopeElements() {
        // currently class forward declaration is a scope only for its template parameters,
        // but we do not return them as scope elements
        return Collections.emptyList();
    }

    public void init(AST ast, CsmScope scope, boolean registerInProject) {
        // we now know the scope - let's modify nameParts accordingly
        if (CsmKindUtilities.isQualified(scope)) {
            CharSequence scopeQName = ((CsmQualifiedNamedElement) scope).getQualifiedName();
            if (scopeQName != null && scopeQName.length() > 0) {
                List<CharSequence> l = new ArrayList<CharSequence>();
                for (StringTokenizer stringTokenizer = new StringTokenizer(scopeQName.toString()); stringTokenizer.hasMoreTokens();) {
                    l.add(NameCache.getManager().getString(stringTokenizer.nextToken()));
                }
                l.addAll(Arrays.asList(nameParts));
                CharSequence[] newNameParts = new CharSequence[l.size()];
                l.toArray(newNameParts);
                nameParts = newNameParts;
                if (registerInProject) {
                    RepositoryUtils.put(this);
                } else {
                    Utils.setSelfUID(this);
                }
            }
        }
        // create fake class we refer to
        createForwardClassIfNeed(ast, scope, registerInProject);
    }

    /**
     * Creates a fake class this forward declaration refers to
     */
    protected CsmClass createForwardClassIfNeed(AST ast, CsmScope scope, boolean registerInProject) {
        return ForwardClass.create(name.toString(), getContainingFile(), ast, this.getStartOffset(), this.getEndOffset(), scope, registerInProject);
    }

    @Override
    public void dispose() {
        super.dispose();
        // nobody disposes the fake forward class => we should take care of this
        CsmClass cls = getCsmClass();
        if (ForwardClass.isForwardClass(cls)) {
            ((ForwardClass) cls).dispose();
        }
        CsmNamespace scope = getContainingFile().getProject().getGlobalNamespace();
        ((NamespaceImpl) scope).removeDeclaration(this);
        ((ProjectBase) getContainingFile().getProject()).unregisterDeclaration(this);
    }

    
    ////////////////////////////////////////////////////////////////////////////
    // iml of SelfPersistent

    @Override
    public void write(RepositoryDataOutput output) throws IOException {
        super.write(output);
        assert this.name != null;
        PersistentUtils.writeUTF(name, output);
        PersistentUtils.writeStrings(this.nameParts, output);
        PersistentUtils.writeTemplateDescriptor(templateDescriptor, output);
    }
    
    public ClassForwardDeclarationImpl(RepositoryDataInput input) throws IOException {
        super(input);
        this.name = PersistentUtils.readUTF(input, QualifiedNameCache.getManager());
        assert this.name != null;
        this.nameParts = PersistentUtils.readStrings(input, NameCache.getManager());
        this.templateDescriptor = PersistentUtils.readTemplateDescriptor(input);
    }
}
