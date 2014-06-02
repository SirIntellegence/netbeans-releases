/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2014 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2014 Sun Microsystems, Inc.
 */
package org.netbeans.modules.javascript2.requirejs.editor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.modules.csl.api.CodeCompletionContext;
import org.netbeans.modules.csl.api.CompletionProposal;
import org.netbeans.modules.csl.api.ElementHandle;
import org.netbeans.modules.csl.spi.ParserResult;
import org.netbeans.modules.javascript2.editor.api.lexer.JsTokenId;
import org.netbeans.modules.javascript2.editor.api.lexer.LexUtilities;
import org.netbeans.modules.javascript2.editor.spi.CompletionContext;
import org.netbeans.modules.javascript2.editor.spi.CompletionProvider;
import org.netbeans.modules.javascript2.requirejs.RequireJsPreferences;
import org.netbeans.modules.javascript2.requirejs.editor.index.RequireJsIndex;
import org.netbeans.modules.parsing.api.Snapshot;
import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;

/**
 *
 * @author Petr Pisl
 */
@CompletionProvider.Registration(priority = 40)
public class RequireJSCodeCompletion implements CompletionProvider {

    @Override
    public List<CompletionProposal> complete(CodeCompletionContext ccContext, CompletionContext jsCompletionContext, final String prefix) {
        int offset = ccContext.getCaretOffset();
        Snapshot snapshot = ccContext.getParserResult().getSnapshot();
        TokenSequence<? extends JsTokenId> ts = LexUtilities.getJsTokenSequence(snapshot.getTokenHierarchy(), offset);
        if (ts == null) {
            return Collections.emptyList();
        }
        ts.move(offset);
        String writtenPath = prefix;
        if (ts.moveNext() && (ts.token().id() == JsTokenId.STRING_END || ts.token().id() == JsTokenId.STRING)) {
            if (ts.token().id() == JsTokenId.STRING_END) {
                ts.movePrevious();
            }
            if (ts.token().id() == JsTokenId.STRING) {
                String text = ts.token().text().toString();
                if (!text.startsWith(prefix)) {
                    // this is needed, because from JS the prefix is split with '.'
                    writtenPath = text.substring(0, offset - ts.offset());
                }
            }

        }

        FileObject fo = ccContext.getParserResult().getSnapshot().getSource().getFileObject();
        if (fo != null && EditorUtils.isFileReference(ts, offset)) {
            List<FileObject> relativeTo = new ArrayList<FileObject>();
            Collection<String> usedFileInDefine = EditorUtils.getUsedFileInDefine(snapshot, offset);
            for (String path : usedFileInDefine) {
                if (writtenPath.isEmpty() || path.startsWith(writtenPath)) {
                    FileObject targetFO = FSCompletionUtils.findMappedFileObject(path, fo);
                    if (targetFO != null) {
                        String[] folders = path.split("/");
                        for (int i = 0; i < folders.length; i++) {
                            targetFO = targetFO.getParent();
                        }
                        if (!relativeTo.contains(targetFO)) {
                            relativeTo.add(targetFO);
                        }
                    }
                }
            }

            if (relativeTo.isEmpty()) {
                Project project = FileOwnerQuery.getOwner(fo);
                Collection<String> basePaths = new ArrayList();
                if (project != null) {
                    RequireJsIndex rIndex = null;
                    try {
                        rIndex = RequireJsIndex.get(project);
                    } catch (IOException ex) {
                        Exceptions.printStackTrace(ex);
                    }

                    if (rIndex != null) {
                        basePaths = rIndex.getBasePaths();
                    }
                }
                relativeTo.add(fo.getParent());
                if (!basePaths.isEmpty()) {
                    for (String path : basePaths) {
                        FileObject findFO = FSCompletionUtils.findFileObject(fo, path);
                        if (findFO != null) {
                            relativeTo.add(findFO);
                        }
                    }
                } 
            }

            List<CompletionProposal> result = new ArrayList();

            try {
                result = FSCompletionUtils.computeRelativeItems(relativeTo, writtenPath, ccContext.getCaretOffset(), new FSCompletionUtils.JSIncludesFilter(fo));
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }

            FileObject fromMapping = FSCompletionUtils.findMappedFileObject(writtenPath, fo);
            String prefixAfterMapping = "";
            if (fromMapping == null) {
                // try to find file withouth the last part of part
                int index = writtenPath.lastIndexOf('/');
                if (index > -1) {
                    fromMapping = FSCompletionUtils.findMappedFileObject(writtenPath.substring(0, index), fo);
                    prefixAfterMapping = writtenPath.substring(index + 1);
                }
            }
            if (fromMapping != null) {
                // try the whole prefix
                relativeTo.clear();
                relativeTo.add(fromMapping);
                try {
                    List<CompletionProposal> newItems = FSCompletionUtils.computeRelativeItems(relativeTo, prefixAfterMapping, ccContext.getCaretOffset(), new FSCompletionUtils.JSIncludesFilter(fo));
                    for (Iterator<CompletionProposal> it = newItems.iterator(); it.hasNext();) {
                        CompletionProposal proposel = it.next();
                        if (!result.contains(proposel)) {
                            result.add(proposel);
                        }
                    }
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
            Project project = FileOwnerQuery.getOwner(fo);
            Map<String, String> mappings = new HashMap();
            RequireJsIndex rIndex = null;
            try {
                rIndex = RequireJsIndex.get(project);
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
            if (rIndex != null) {
                mappings.putAll(rIndex.getPathMappings(writtenPath));
            }
            mappings.putAll(RequireJsPreferences.getMappings(project));
            for (String mapping : mappings.keySet()) {
                if (mapping.startsWith(writtenPath)) {
                    result.add(new MappingCompletionItem(mapping, FSCompletionUtils.findMappedFileObject(mapping, fo), ccContext.getCaretOffset() - writtenPath.length()));
                }
            }

            return result;
        }
        return Collections.emptyList();
    }

    @Override
    public String getHelpDocumentation(ParserResult info, ElementHandle element) {
        if (element != null && element instanceof FSCompletionItem.FSElementHandle) {
            FileObject fo = element.getFileObject();
            if (fo != null) {
                String path = fo.getPath();
                String[] parts = path.split("/");
                StringBuilder sb = new StringBuilder();
                sb.append("<pre>"); // NOI18N
                int length = 0;
                for (String part : parts) {
                    if ((length + part.length()) > 50) {
                        sb.append("\n    "); // NOI18N
                        length = 4;
                    }
                    sb.append(part).append('/');
                    length += part.length() + 1;
                }
                sb.deleteCharAt(sb.length() - 1);
                sb.append("</pre>"); // NOI18N
                return sb.toString();
            }
        }
        return null;
    }

}