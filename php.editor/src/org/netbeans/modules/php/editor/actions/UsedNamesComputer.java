/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2012 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2012 Sun Microsystems, Inc.
 */
package org.netbeans.modules.php.editor.actions;

import java.util.*;
import org.netbeans.modules.csl.api.OffsetRange;
import org.netbeans.modules.php.editor.api.AliasedName;
import org.netbeans.modules.php.editor.api.QualifiedName;
import org.netbeans.modules.php.editor.model.ModelUtils;
import org.netbeans.modules.php.editor.model.NamespaceScope;
import org.netbeans.modules.php.editor.model.UseElement;
import org.netbeans.modules.php.editor.parser.PHPParseResult;
import org.netbeans.modules.php.editor.parser.astnodes.*;
import org.netbeans.modules.php.editor.parser.astnodes.visitors.DefaultVisitor;

/**
 *
 * @author Ondrej Brejla <obrejla@netbeans.org>
 */
public class UsedNamesComputer {
    private final PHPParseResult parserResult;
    private final int caretPosition;
    private Map<String, List<UsedNamespaceName>> possibleNames;
    private static final List<String> specialNames = new LinkedList<String>();

    static {
        specialNames.add("parent"); //NOI18N
        specialNames.add("self"); //NOI18N
        specialNames.add("static"); //NOI18N
    }

    public UsedNamesComputer(final PHPParseResult parserResult, final int caretPosition) {
        this.parserResult = parserResult;
        this.caretPosition = caretPosition;
    }

    public Map<String, List<UsedNamespaceName>> computeNames() {
        NamespaceScope namespaceScope = ModelUtils.getNamespaceScope(parserResult.getModel().getFileScope(), caretPosition);
        OffsetRange offsetRange = namespaceScope.getBlockRange();
        Collection<? extends UseElement> declaredUses = namespaceScope.getDeclaredUses();
        NamespaceNameVisitor namespaceNameVisitor = new NamespaceNameVisitor(offsetRange);
        parserResult.getProgram().accept(namespaceNameVisitor);
        possibleNames = namespaceNameVisitor.getExistingNames();
        return filterNamesWithoutUses(declaredUses);
    }

    private Map<String, List<UsedNamespaceName>> filterNamesWithoutUses(final Collection<? extends UseElement> declaredUses) {
        final Map<String, List<UsedNamespaceName>> result = new HashMap<String, List<UsedNamespaceName>>();
        for (String typeName : possibleNames.keySet()) {
            if (!existsUseForTypeName(declaredUses, QualifiedName.create(typeName))) {
                result.put(typeName, possibleNames.get(typeName));
            }
        }
        return result;
    }

    private boolean existsUseForTypeName(final Collection<? extends UseElement> declaredUses, final QualifiedName typeName) {
        boolean result = false;
        String firstSegmentName = typeName.getSegments().getFirst();
        for (UseElement useElement : declaredUses) {
            AliasedName aliasName = useElement.getAliasedName();
            if (aliasName != null) {
                if (firstSegmentName.equals(aliasName.getAliasName())) {
                    result = true;
                    break;
                }
            } else {
                if (useElement.getName().endsWith(firstSegmentName)) {
                    result = true;
                    break;
                }
            }
        }
        return result;
    }

    private class NamespaceNameVisitor extends DefaultVisitor {
        private final OffsetRange offsetRange;
        private final Map<String, List<UsedNamespaceName>> existingNames = new HashMap<String, List<UsedNamespaceName>>();

        public NamespaceNameVisitor(OffsetRange offsetRange) {
            this.offsetRange = offsetRange;
        }

        @Override
        public void scan(ASTNode node) {
            if (node != null && isInNamespace(node) && !(node instanceof UseStatement)) {
                super.scan(node);
            }
        }

        @Override
        public void visit(NamespaceDeclaration node) {
            scan(node.getBody());
        }

        @Override
        public void visit(FunctionInvocation node) {
            scan(node.getParameters());
        }

        @Override
        public void visit(NamespaceName node) {
            UsedNamespaceName usedName = new UsedNamespaceName(node);
            if (!specialNames.contains(usedName.getName())) {
                processUsedName(usedName);
            }
        }

        private void processUsedName(final UsedNamespaceName usedName) {
            List<UsedNamespaceName> usedNames = existingNames.get(usedName.getName());
            if (usedNames == null) {
                usedNames = new LinkedList<UsedNamespaceName>();
                existingNames.put(usedName.getName(), usedNames);
            }
            usedNames.add(usedName);
        }

        public Map<String, List<UsedNamespaceName>> getExistingNames() {
            return Collections.unmodifiableMap(existingNames);
        }

        private boolean isInNamespace(ASTNode node) {
            return offsetRange.containsInclusive(node.getStartOffset()) && offsetRange.containsInclusive(node.getEndOffset());
        }

    }

}
