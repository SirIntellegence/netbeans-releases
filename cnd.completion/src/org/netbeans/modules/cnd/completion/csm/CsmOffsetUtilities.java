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

import java.util.Collection;
import org.netbeans.modules.cnd.api.model.CsmObject;
import org.netbeans.modules.cnd.api.model.CsmOffsetable;
import org.netbeans.modules.cnd.api.model.util.CsmKindUtilities;
import java.util.Iterator;
import org.netbeans.modules.cnd.api.model.CsmClass;
import org.netbeans.modules.cnd.api.model.CsmFunction;
import org.netbeans.modules.cnd.api.model.CsmFunctionDefinition;
import org.netbeans.modules.cnd.api.model.CsmInitializerListContainer;
import org.netbeans.modules.cnd.api.model.CsmParameter;
import org.netbeans.modules.cnd.api.model.CsmType;
import org.netbeans.modules.cnd.api.model.deep.CsmExpression;

/**
 * utilities method for working with offsets of Csm objects
 * and CsmOffsetable objects
 * @author vv159170
 */
public class CsmOffsetUtilities {

    /** Creates a new instance of CsmOffsetUtils */
    private CsmOffsetUtilities() {
    }

    ////////////////////////////////////////////////////////////////////////////
    
    public static boolean isInObject(CsmObject obj, int offset) {
        if (!CsmKindUtilities.isOffsetable(obj)) {
            return false;
        }
        CsmOffsetable offs = (CsmOffsetable)obj;
        if ((offs.getStartOffset() <= offset) &&
                (offset <= offs.getEndOffset())) {
            if (offset == offs.getEndOffset()) {
                if (CsmKindUtilities.isType(obj)) {
                    CsmType type = (CsmType)obj;
                    // we do not accept type if offset is after '*', '&' or '[]'
                    return !type.isPointer() && !type.isReference() && (type.getArrayDepth() == 0);
                } else if (CsmKindUtilities.isScope(obj)) {
                    // if we right after closed "}" it means we are out of scope object
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }
    
    public static boolean isBeforeObject(CsmObject obj, int offset) {
        if (!CsmKindUtilities.isOffsetable(obj)) {
            return false;
        }
        CsmOffsetable offs = (CsmOffsetable)obj;
        if (offset < offs.getStartOffset()) {
            return true;
        } else {
            return false;
        }
    }
    
    public static boolean isAfterObject(CsmObject obj, int offset) {
        if (!CsmKindUtilities.isOffsetable(obj)) {
            return false;
        }
        CsmOffsetable offs = (CsmOffsetable)obj;
        if (offset > offs.getEndOffset()) {
            return true;
        } else {
            return false;
        }
    }
    
    // list is ordered by offsettable elements
    public static <T extends CsmObject> T findObject(Collection<T> list, CsmContext context, int offset) {
        assert (list != null) : "expect not null list";
        return findObject(list.iterator(), context, offset);
    }

    // list is ordered by offsettable elements
    public static <T extends CsmObject> T findObject(Iterator<T> it, CsmContext context, int offset) {
        assert (it != null) : "expect not null list";
        while (it.hasNext()) {
            T obj = it.next();
            assert (obj != null) : "can't be null declaration";
            if (CsmOffsetUtilities.isInObject((CsmObject)obj, offset)) {
                // we are inside csm element
                CsmContextUtilities.updateContextObject(obj, offset, context);
                return obj;
            }
        }
        return null;
    }
    
    public static boolean isInFunctionScope(final CsmFunction fun, final int offset) {
        boolean inScope = false;
        if (fun != null) {
            inScope = true;
            // in function, but check that not in return type
            // check if offset in return value
            CsmType retType = fun.getReturnType();
            if (CsmOffsetUtilities.isInObject(retType, offset)) {
                return false;
            }
            // check if offset is before parameters
            Collection<CsmParameter> params = fun.getParameters();
            if(!params.isEmpty()) 
            {
                CsmParameter firstParam = params.iterator().next();
                if (CsmOffsetUtilities.isBeforeObject(firstParam, offset)) {
                    return false;
                }
                return true;
            }
            // check initializer list for constructors
            if (CsmKindUtilities.isConstructor(fun)) {
                Collection<CsmExpression> izers = ((CsmInitializerListContainer) fun).getInitializerList();
                if (!izers.isEmpty()) {
                    CsmExpression firstIzer = izers.iterator().next();
                    if (CsmOffsetUtilities.isBeforeObject(firstIzer, offset)) {
                        return false;
                    }
                    return true;
                }
            }
            // for function definitions check body
            if (CsmKindUtilities.isFunctionDefinition(fun)) {
                CsmFunctionDefinition funDef = (CsmFunctionDefinition) fun;
                if (CsmOffsetUtilities.isBeforeObject(funDef.getBody(), offset)) {
                    return false;
                }
            }
            if (CsmKindUtilities.isFunctionDeclaration(fun)) {
                return false;
            }
        }
        return inScope;
    }

    public static boolean isInClassScope(final CsmClass clazz, final int offset) {
        return isInObject(clazz, offset) 
                && clazz.getLeftBracketOffset() < offset
                && offset < clazz.getEndOffset();
    }

}
