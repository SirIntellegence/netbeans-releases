/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2011 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2007-2011 Sun Microsystems, Inc.
 */

package org.netbeans.modules.java.hints;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.SynchronizedTree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.TreePath;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import org.netbeans.spi.java.hints.Hint;
import org.netbeans.spi.java.hints.TriggerTreeKind;
import org.netbeans.spi.java.hints.HintContext;
import org.netbeans.spi.java.hints.ErrorDescriptionFactory;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.openide.util.NbBundle;

/**
 *
 * @author Jan Lahoda
 */
@Hint(displayName = "#DN_org.netbeans.modules.java.hints.SyncOnNonFinal", description = "#DESC_org.netbeans.modules.java.hints.SyncOnNonFinal", id="org.netbeans.modules.java.hints.SyncOnNonFinal", category="thread", suppressWarnings="SynchronizeOnNonFinalField")
public class SyncOnNonFinal {

    @TriggerTreeKind(Kind.SYNCHRONIZED)
    public static ErrorDescription run(HintContext ctx) {
        ExpressionTree expression = ((SynchronizedTree) ctx.getPath().getLeaf()).getExpression();
        
        Element e = ctx.getInfo().getTrees().getElement(new TreePath(ctx.getPath(), expression));
        
        if (e == null || e.getKind() != ElementKind.FIELD || e.getModifiers().contains(Modifier.FINAL)) {
            return null;
        }
        
        String displayName = NbBundle.getMessage(SyncOnNonFinal.class, "ERR_SynchronizationOnNonFinalField");
        
        return ErrorDescriptionFactory.forTree(ctx, expression, displayName);
    }

}
