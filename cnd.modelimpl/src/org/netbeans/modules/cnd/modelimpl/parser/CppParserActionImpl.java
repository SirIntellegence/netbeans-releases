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
package org.netbeans.modules.cnd.modelimpl.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.netbeans.modules.cnd.antlr.Token;
import org.netbeans.modules.cnd.api.model.CsmFile;
import org.netbeans.modules.cnd.api.model.CsmObject;
import org.netbeans.modules.cnd.apt.support.APTToken;
import org.netbeans.modules.cnd.modelimpl.csm.EnumImpl;
import org.netbeans.modules.cnd.modelimpl.csm.EnumImpl.EnumBuilder;
import org.netbeans.modules.cnd.modelimpl.csm.EnumeratorImpl.EnumeratorBuilder;

/**
 * @author Nikolay Krasilnikov (nnnnnk@netbeans.org)
 */
public class CppParserActionImpl implements CppParserAction {

    Map<Integer, CsmObject> objects;
    CsmFile file;

    EnumBuilder enumBuilder;
    
    
    public CppParserActionImpl(CsmFile file, Map<Integer, CsmObject> objects) {
        this.objects = objects;
        this.file = file;
    }
    
    @Override
    public void enum_declaration(Token token) {        
        //System.out.println("enum_declaration " + ((APTToken)token).getOffset());
        
        enumBuilder = new EnumBuilder();
        enumBuilder.setFile(file);
        if(token instanceof APTToken) {
            enumBuilder.setStartOffset(((APTToken)token).getOffset());
        }
    }

    @Override
    public void end_enum_declaration(Token token) {
        //System.out.println("end_enum_declaration " + ((APTToken)token).getOffset());

        if(enumBuilder != null) {
            EnumImpl e = enumBuilder.create(true);
            if(e != null) {
                objects.put(e.getStartOffset(), e);
            }

            enumBuilder = null;
        }
    }

    @Override
    public void enum_name(Token token) {
        //System.out.println("enum_name " + ((APTToken)token).getOffset());        

        // add to index
        if(enumBuilder != null) {
            enumBuilder.setName(token.getText());
        }
    }

    @Override
    public void enum_body(Token token) {
    }
    
    @Override
    public void enumerator(Token token) {
        if(enumBuilder != null) {
            EnumeratorBuilder builder = new EnumeratorBuilder();
            builder.setName(token.getText());
            builder.setFile(file);
            if(token instanceof APTToken) {
                builder.setStartOffset(((APTToken)token).getOffset());
                builder.setEndOffset(((APTToken)token).getEndOffset());
            }
            enumBuilder.addEnumerator(builder);
        }
        
    }
    
    @Override
    public void end_enum_body(Token token) {
        if(enumBuilder != null) {
            if(token instanceof APTToken) {
                enumBuilder.setEndOffset(((APTToken)token).getEndOffset());
            }
        }        
    }

    @Override
    public void class_body(Token token) {
    }

    @Override
    public void end_class_body(Token token) {
    }

    @Override
    public void namespace_body(Token token) {
    }

    @Override
    public void end_namespace_body(Token token) {
    }

    @Override
    public void compound_statement(Token token) {
    }

    @Override
    public void end_compound_statement(Token token) {
    }

    @Override
    public void id(Token token) {
    }

}
