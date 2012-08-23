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
 * Contributor(s): Sebastian Hörl
 *
 * Portions Copyrighted 2011 Sun Microsystems, Inc.
 */
package org.netbeans.modules.php.twig.editor.completion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenId;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.modules.csl.api.CodeCompletionContext;
import org.netbeans.modules.csl.api.CodeCompletionHandler;
import org.netbeans.modules.csl.api.CodeCompletionResult;
import org.netbeans.modules.csl.api.CompletionProposal;
import org.netbeans.modules.csl.api.ElementHandle;
import org.netbeans.modules.csl.api.ParameterInfo;
import org.netbeans.modules.csl.spi.DefaultCompletionResult;
import org.netbeans.modules.csl.spi.ParserResult;
import org.netbeans.modules.php.twig.editor.completion.TwigCompletionItem.CompletionRequest;
import org.netbeans.modules.php.twig.editor.completion.TwigFunction.Parameter;
import org.netbeans.modules.php.twig.editor.lexer.TwigTokenId;
import org.netbeans.modules.php.twig.editor.lexer.TwigTopTokenId;

public class TwigCompletionHandler implements CodeCompletionHandler {

    private static final Set<String> TAGS = new HashSet<String>();
    static {
        TAGS.add("autoescape"); //NOI18N
        TAGS.add("block"); //NOI18N
        TAGS.add("do"); //NOI18N
        TAGS.add("embed"); //NOI18N
        TAGS.add("extends"); //NOI18N
        TAGS.add("filter"); //NOI18N
        TAGS.add("flush"); //NOI18N
        TAGS.add("for"); //NOI18N
        TAGS.add("from"); //NOI18N
        TAGS.add("if"); //NOI18N
        TAGS.add("import"); //NOI18N
        TAGS.add("include"); //NOI18N
        TAGS.add("macro"); //NOI18N
        TAGS.add("raw"); //NOI18N
        TAGS.add("sandbox"); //NOI18N
        TAGS.add("set"); //NOI18N
        TAGS.add("spaceless"); //NOI18N
        TAGS.add("use"); //NOI18N
    }

    private static final Set<String> FILTERS = new HashSet<String>();
    static {
        FILTERS.add("abs"); //NOI18N
        FILTERS.add("capitalize"); //NOI18N
        FILTERS.add("convert_encoding"); //NOI18N
        FILTERS.add("date"); //NOI18N
        FILTERS.add("date_modify"); //NOI18N
        FILTERS.add("default"); //NOI18N
        FILTERS.add("escape"); //NOI18N
        FILTERS.add("format"); //NOI18N
        FILTERS.add("join"); //NOI18N
        FILTERS.add("json_encode"); //NOI18N
        FILTERS.add("keys"); //NOI18N
        FILTERS.add("length"); //NOI18N
        FILTERS.add("lower"); //NOI18N
        FILTERS.add("merge"); //NOI18N
        FILTERS.add("nl2br"); //NOI18N
        FILTERS.add("number_format"); //NOI18N
        FILTERS.add("raw"); //NOI18N
        FILTERS.add("replace"); //NOI18N
        FILTERS.add("reverse"); //NOI18N
        FILTERS.add("slice"); //NOI18N
        FILTERS.add("sort"); //NOI18N
        FILTERS.add("striptags"); //NOI18N
        FILTERS.add("title"); //NOI18N
        FILTERS.add("trim"); //NOI18N
        FILTERS.add("upper"); //NOI18N
        FILTERS.add("url_encode"); //NOI18N
    }

    private static final Set<TwigFunction> FUNCTIONS = new HashSet<TwigFunction>();
    static {
        FUNCTIONS.add(new TwigFunction("attribute", Arrays.asList(new Parameter[] {new Parameter("object"), new Parameter("method"), new Parameter("arguments", Parameter.Need.OPTIONAL)}))); //NOI18N
        FUNCTIONS.add(new TwigFunction("block", Arrays.asList(new Parameter[] {new Parameter("'name'")}))); //NOI18N
        FUNCTIONS.add(new TwigFunction("constant", Arrays.asList(new Parameter[] {new Parameter("'name'")}))); //NOI18N
        FUNCTIONS.add(new TwigFunction("cycle", Arrays.asList(new Parameter[] {new Parameter("array"), new Parameter("i")}))); //NOI18N
        FUNCTIONS.add(new TwigFunction("date", Arrays.asList(new Parameter[] {new Parameter("'date'"), new Parameter("'timezone'", Parameter.Need.OPTIONAL)}))); //NOI18N
        FUNCTIONS.add(new TwigFunction("dump", Arrays.asList(new Parameter[] {new Parameter("variable", Parameter.Need.OPTIONAL)}))); //NOI18N
        FUNCTIONS.add(new TwigFunction("parent")); //NOI18N
        FUNCTIONS.add(new TwigFunction("random", Arrays.asList(new Parameter[] {new Parameter("'value'")}))); //NOI18N
        FUNCTIONS.add(new TwigFunction("range", Arrays.asList(new Parameter[] {new Parameter("start"), new Parameter("end"), new Parameter("step", Parameter.Need.OPTIONAL)}))); //NOI18N
    }

    private static final Set<String> TESTS = new HashSet<String>();
    static {
        TESTS.add("constant"); //NOI18N
        TESTS.add("defined"); //NOI18N
        TESTS.add("divisibleby"); //NOI18N
        TESTS.add("empty"); //NOI18N
        TESTS.add("even"); //NOI18N
        TESTS.add("iterable"); //NOI18N
        TESTS.add("null"); //NOI18N
        TESTS.add("odd"); //NOI18N
        TESTS.add("sameas"); //NOI18N
    }

    private static final Set<String> OPERATORS = new HashSet<String>();
    static {
        OPERATORS.add("in"); //NOI18N
        OPERATORS.add("is"); //NOI18N
        OPERATORS.add("and"); //NOI18N
        OPERATORS.add("or"); //NOI18N
        OPERATORS.add("not"); //NOI18N
        OPERATORS.add("b-and"); //NOI18N
        OPERATORS.add("b-or"); //NOI18N
        OPERATORS.add("b-xor"); //NOI18N
    }

    @Override
    public CodeCompletionResult complete(CodeCompletionContext codeCompletionContext) {
        final List<CompletionProposal> completionProposals = new ArrayList<CompletionProposal>();
        final TokenSequence<TwigTopTokenId> topTokenSequence = codeCompletionContext.getParserResult().getSnapshot().getTokenHierarchy().tokenSequence(TwigTopTokenId.language());
        if (topTokenSequence != null) {
            topTokenSequence.move(codeCompletionContext.getCaretOffset());
            if (topTokenSequence.moveNext()) {
                TokenSequence<TwigTokenId> tokenSequence = topTokenSequence.embedded(TwigTokenId.language());
                if (tokenSequence != null) {
                    tokenSequence.move(codeCompletionContext.getCaretOffset());
                    tokenSequence.moveNext();
                    if (tokenSequence.token() != null && !isDelimiter(tokenSequence.token().id())) {
                        CompletionRequest request = new CompletionRequest();
                        request.prefix = codeCompletionContext.getPrefix();
                        int caretOffset = codeCompletionContext.getCaretOffset();
                        request.anchorOffset = caretOffset - getPrefix(codeCompletionContext.getParserResult(), caretOffset, true).length();
                        completeAll(completionProposals, request);
                    }
                }
            }
        }
        return new TwigCompletionResult(completionProposals, false);
    }

    private static boolean isDelimiter(final TokenId tokenId) {
        return TwigTokenId.T_TWIG_VARIABLE.equals(tokenId) || TwigTokenId.T_TWIG_INSTRUCTION.equals(tokenId);
    }

    private boolean startsWith(String theString, String prefix) {
        return prefix.length() == 0 ? true : theString.toLowerCase().startsWith(prefix.toLowerCase());
    }

    private void completeAll(final List<CompletionProposal> completionProposals, final CompletionRequest request) {
        completeTags(completionProposals, request);
        completeFilters(completionProposals, request);
        completeFunctions(completionProposals, request);
        completeTests(completionProposals, request);
        completeOperators(completionProposals, request);
    }

    private void completeTags(final List<CompletionProposal> completionProposals, final CompletionRequest request) {
        for (String tag : TAGS) {
            if (startsWith(tag, request.prefix)) {
                completionProposals.add(new TwigCompletionItem.TagCompletionItem(tag, request));
            }
        }
    }

    private void completeFilters(final List<CompletionProposal> completionProposals, final CompletionRequest request) {
        for (String filter : FILTERS) {
            if (startsWith(filter, request.prefix)) {
                completionProposals.add(new TwigCompletionItem.FilterCompletionItem(filter, request));
            }
        }
    }

    private void completeFunctions(final List<CompletionProposal> completionProposals, final CompletionRequest request) {
        for (TwigFunction function : FUNCTIONS) {
            if (startsWith(function.getName(), request.prefix)) {
                completionProposals.add(new TwigCompletionItem.FunctionCompletionItem(function, request));
            }
        }
    }

    private void completeTests(final List<CompletionProposal> completionProposals, final CompletionRequest request) {
        for (String test : TESTS) {
            if (startsWith(test, request.prefix)) {
                completionProposals.add(new TwigCompletionItem.TestCompletionItem(test, request));
            }
        }
    }

    private void completeOperators(final List<CompletionProposal> completionProposals, final CompletionRequest request) {
        for (String operator : OPERATORS) {
            if (startsWith(operator, request.prefix)) {
                completionProposals.add(new TwigCompletionItem.OperatorCompletionItem(operator, request));
            }
        }
    }

    @Override
    public String document(ParserResult pr, ElementHandle eh) {
        return "";
    }

    @Override
    public ElementHandle resolveLink(String string, ElementHandle eh) {
        return null;
    }

    @Override
    public String getPrefix(ParserResult info, int offset, boolean upToOffset) {
        return PrefixResolver.create(info, offset, upToOffset).resolve();
    }

    @Override
    public QueryType getAutoQuery(JTextComponent jtc, String string) {
        return QueryType.ALL_COMPLETION;
    }

    @Override
    public String resolveTemplateVariable(String string, ParserResult pr, int i, String string1, Map map) {
        return null;
    }

    @Override
    public Set<String> getApplicableTemplates(Document dcmnt, int i, int i1) {
        return Collections.emptySet();
    }

    @Override
    public ParameterInfo parameters(ParserResult pr, int i, CompletionProposal cp) {
        return new ParameterInfo(new ArrayList<String>(), 0, 0);
    }

    private static class TwigCompletionResult extends DefaultCompletionResult {

        public TwigCompletionResult(List<CompletionProposal> list, boolean truncated) {
            super(list, truncated);
        }

    }

    private static class PrefixResolver {
        private final ParserResult info;
        private final int offset;
        private final boolean upToOffset;
        private String result = "";

        static PrefixResolver create(ParserResult info, int offset, boolean upToOffset) {
            return new PrefixResolver(info, offset, upToOffset);
        }

        private PrefixResolver(ParserResult info, int offset, boolean upToOffset) {
            this.info = info;
            this.offset = offset;
            this.upToOffset = upToOffset;
        }

        String resolve() {
            TokenHierarchy<?> th = info.getSnapshot().getTokenHierarchy();
            if (th != null) {
                processHierarchy(th);
            }
            return result;
        }

        private void processHierarchy(TokenHierarchy<?> th) {
            TokenSequence<TwigTopTokenId> tts = th.tokenSequence(TwigTopTokenId.language());
            if (tts != null) {
                processTopSequence(tts);
            }
        }

        private void processTopSequence(TokenSequence<TwigTopTokenId> tts) {
            tts.move(offset);
            if (tts.moveNext() || tts.movePrevious() ) {
                processSequence(tts.embedded(TwigTokenId.language()));
            }
        }

        private void processSequence(TokenSequence<TwigTokenId> ts) {
            if (ts != null) {
                processValidSequence(ts);
            }
        }

        private void processValidSequence(TokenSequence<TwigTokenId> ts) {
            ts.move(offset);
            if (ts.moveNext() || ts.movePrevious()) {
                processToken(ts);
            }
        }

        private void processToken(TokenSequence<TwigTokenId> ts) {
            if (ts.offset() == offset) {
                ts.movePrevious();
            }
            Token<TwigTokenId> token = ts.token();
            if (token != null) {
                processSelectedToken(ts);
            }
        }

        private void processSelectedToken(TokenSequence<TwigTokenId> ts) {
            TwigTokenId id = ts.token().id();
            if (isValidTokenId(id)) {
                createResult(ts);
            }
        }

        private void createResult(TokenSequence<TwigTokenId> ts) {
            if (upToOffset) {
                String text = ts.token().text().toString();
                result = text.substring(0, offset - ts.offset());
            }
        }

        private static boolean isValidTokenId(TwigTokenId id) {
            return TwigTokenId.T_TWIG_FUNCTION.equals(id) || TwigTokenId.T_TWIG_NAME.equals(id);
        }

    }

}
