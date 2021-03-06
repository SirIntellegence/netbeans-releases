/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017 Oracle and/or its affiliates. All rights reserved.
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
 */
package org.netbeans.modules.php.editor.verification;

import java.util.Collections;
import java.util.List;
import org.netbeans.api.annotations.common.NullAllowed;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.api.lexer.TokenUtilities;
import org.netbeans.editor.BaseDocument;
import org.netbeans.modules.csl.api.EditList;
import org.netbeans.modules.csl.api.Hint;
import org.netbeans.modules.csl.api.HintFix;
import org.netbeans.modules.csl.api.OffsetRange;
import org.netbeans.modules.csl.spi.support.CancelSupport;
import org.netbeans.modules.php.api.PhpVersion;
import org.netbeans.modules.php.editor.CodeUtils;
import org.netbeans.modules.php.editor.lexer.LexUtilities;
import org.netbeans.modules.php.editor.lexer.PHPTokenId;
import org.netbeans.modules.php.editor.parser.PHPParseResult;
import org.netbeans.modules.php.editor.parser.astnodes.DeclareStatement;
import org.netbeans.modules.php.editor.parser.astnodes.Expression;
import org.netbeans.modules.php.editor.parser.astnodes.Identifier;
import org.netbeans.modules.php.editor.parser.astnodes.InLineHtml;
import org.netbeans.modules.php.editor.parser.astnodes.Scalar;
import org.netbeans.modules.php.editor.parser.astnodes.visitors.DefaultVisitor;
import org.openide.filesystems.FileObject;
import org.openide.util.NbBundle;

/**
 * Suggest declaring strict types when the PHP open tag is on the caret line.
 *
 * If the PHP file contains inline HTMLs or some PHP open tags, "strict_types"
 * is declared on the first line. Otherwise it is declared after the PHP open
 * tag. PHP7.0+.
 *
 * <pre>
 * e.g.
 * &lt;?php declare(strict_types=1) ?&gt;
 * &lt;html&gt;
 * ...
 * &lt;?php echo &quot;something&quot;; ?&gt;
 * &lt;/html&gt;
 *
 * &lt;?php
 *
 * declare(strict_types=1);
 * </pre>
 */
public class DeclareStrictTypesSuggestion extends SuggestionRule {

    private static final String HINT_ID = "Declare.Strict.Types.Suggestion"; // NOI18N

    @Override
    public String getId() {
        return HINT_ID;
    }

    @NbBundle.Messages("DeclareStrictTypesSuggestion.description=Add declare(strict_types=1)")
    @Override
    public String getDescription() {
        return Bundle.DeclareStrictTypesSuggestion_description();
    }

    @NbBundle.Messages("DeclareStrictTypesSuggestion.displayName=Declare Strict Types")
    @Override
    public String getDisplayName() {
        return Bundle.DeclareStrictTypesSuggestion_displayName();
    }

    // for unit tests
    protected PhpVersion getPhpVersion(@NullAllowed FileObject fileObject) {
        return fileObject == null ? PhpVersion.getDefault() : CodeUtils.getPhpVersion(fileObject);
    }

    private boolean appliesTo(FileObject fileObject) {
        return getPhpVersion(fileObject).compareTo(PhpVersion.PHP_70) >= 0;
    }

    @Override
    public void invoke(PHPRuleContext context, List<Hint> hints) {
        PHPParseResult phpParseResult = (PHPParseResult) context.parserResult;
        if (phpParseResult.getProgram() == null) {
            return;
        }
        if (CancelSupport.getDefault().isCancelled()) {
            return;
        }
        FileObject fileObject = phpParseResult.getSnapshot().getSource().getFileObject();
        if (fileObject != null && appliesTo(fileObject)) {
            final BaseDocument doc = context.doc;
            int caretOffset = getCaretOffset();
            OffsetRange lineBounds = VerificationUtils.createLineBounds(caretOffset, doc);
            if (lineBounds.containsInclusive(caretOffset)) {
                FixInfo fixInfo = createFixInfo(doc, lineBounds, caretOffset);
                if (!fixInfo.foundOpenTag()) {
                    return;
                }
                CheckVisitor checkVisitor = new CheckVisitor();
                phpParseResult.getProgram().accept(checkVisitor);
                if (CancelSupport.getDefault().isCancelled()
                        || checkVisitor.hasDeclareStrictTypes()) {
                    return;
                }
                List<HintFix> fixes = Collections.singletonList(createHintFix(doc, checkVisitor.hasInlineHtml(), fixInfo));
                addHint(hints, fileObject, lineBounds, fixes);
            }
        }
    }

    private FixInfo createFixInfo(BaseDocument doc, OffsetRange lineBounds, int caretOffset) {
        boolean foundOpenTag = false;
        int insertOffset = 0;
        int phpOpenTagCount = 0;
        doc.readLock();
        try {
            TokenSequence<PHPTokenId> ts = LexUtilities.getPHPTokenSequence(doc, caretOffset);
            if (ts != null) {
                ts.move(lineBounds.getStart());
                // check the php tag on the caret line
                while (ts.moveNext()
                        && ts.offset() < lineBounds.getEnd()) {
                    PHPTokenId id = ts.token().id();
                    if (id == PHPTokenId.PHP_OPENTAG
                            && TokenUtilities.equals(ts.token().text(), "<?php")) { // NOI18N
                        foundOpenTag = true;
                        break;
                    }
                }

                // check all php open tags
                if (foundOpenTag) {
                    ts.move(0);
                    while (ts.moveNext()) {
                        if (ts.token().id() == PHPTokenId.PHP_OPENTAG) {
                            if (insertOffset == 0
                                    && TokenUtilities.equals(ts.token().text(), "<?php")) { // NOI18N
                                insertOffset = ts.offset() + "<?php".length(); // NOI18N
                            }
                            phpOpenTagCount++;
                        }
                    }
                }
            }
        } finally {
            doc.readUnlock();
        }
        return new FixInfo(foundOpenTag, insertOffset, phpOpenTagCount);
    }

    private HintFix createHintFix(BaseDocument doc, boolean hasInlineHtml, FixInfo fixInfo) {
        return new Fix(doc, hasInlineHtml, fixInfo);
    }

    @NbBundle.Messages("DeclareStrictTypesSuggestion.hint.description=You can declare strict types")
    private void addHint(List<Hint> hints, FileObject file, OffsetRange offsetRang, List<HintFix> fixes) {
        hints.add(new Hint(this, Bundle.DeclareStrictTypesSuggestion_hint_description(), file, offsetRang, fixes, 500));
    }

    //~ Inner classes
    private static final class CheckVisitor extends DefaultVisitor {

        private boolean hasDeclareStrictTypes;
        private boolean hasInlineHtml;

        @Override
        public void visit(InLineHtml inLineHtml) {
            if (CancelSupport.getDefault().isCancelled()) {
                return;
            }
            hasInlineHtml = true;
            super.visit(inLineHtml);
        }

        @Override
        public void visit(DeclareStatement node) {
            if (CancelSupport.getDefault().isCancelled()) {
                return;
            }
            List<Identifier> directiveNames = node.getDirectiveNames();
            List<Expression> directiveValues = node.getDirectiveValues();
            for (int i = 0; i < directiveNames.size(); i++) {
                String name = directiveNames.get(i).getName();
                if (name.equals("strict_types")) { // NOI18N
                    Expression value = directiveValues.get(i);
                    if (value instanceof Scalar) {
                        Scalar scalar = (Scalar) value;
                        if (scalar.getScalarType() == Scalar.Type.INT
                                && scalar.getStringValue().equals("1")) { // NOI18N
                            hasDeclareStrictTypes = true;
                        }
                    }
                    break;
                }
            }
        }

        public boolean hasDeclareStrictTypes() {
            return hasDeclareStrictTypes;
        }

        public boolean hasInlineHtml() {
            return hasInlineHtml;
        }

    }

    private static final class FixInfo {

        private final boolean foundOpenTag;
        private final int insertOffset;
        private final int phpOpenTagCount;

        public FixInfo(boolean foundOpenTag, int firstOpenTagOffset, int phpOpenTagCount) {
            this.foundOpenTag = foundOpenTag;
            this.insertOffset = firstOpenTagOffset;
            this.phpOpenTagCount = phpOpenTagCount;
        }

        public boolean foundOpenTag() {
            return foundOpenTag;
        }

        public int getInsertOffset() {
            return insertOffset;
        }

        public int getPhpOpenTagCount() {
            return phpOpenTagCount;
        }

    }

    private static final class Fix implements HintFix {

        private final BaseDocument document;
        private final boolean hasInlineHtml;
        private final FixInfo fixInfo;

        private Fix(BaseDocument document, boolean hasInlineHtml, FixInfo fixInfo) {
            this.document = document;
            this.hasInlineHtml = hasInlineHtml;
            this.fixInfo = fixInfo;
        }

        @Override
        @NbBundle.Messages("DeclareStrictTypesFixDescription=Add declare(strict_types=1)")
        public String getDescription() {
            return Bundle.DeclareStrictTypesFixDescription();
        }

        @Override
        public void implement() throws Exception {
            EditList editList = new EditList(document);
            int insertOffset;
            String insertText;
            if (hasInlineHtml || fixInfo.getPhpOpenTagCount() > 1) {
                insertOffset = 0;
                insertText = "<?php declare(strict_types=1) ?>\n"; // NOI18N
            } else {
                insertOffset = fixInfo.getInsertOffset();
                // don't remove the first whitespace because the parser recognizes it as an inline html
                insertText = " declare(strict_types=1);"; // NOI18N
            }
            editList.replace(insertOffset, 0, insertText, true, 0);
            editList.apply();
        }

        @Override
        public boolean isSafe() {
            return true;
        }

        @Override
        public boolean isInteractive() {
            return false;
        }

    }

}
