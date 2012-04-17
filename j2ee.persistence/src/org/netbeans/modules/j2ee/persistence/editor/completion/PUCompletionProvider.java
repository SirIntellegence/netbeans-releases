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
package org.netbeans.modules.j2ee.persistence.editor.completion;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import javax.swing.text.StyledDocument;
import org.netbeans.api.editor.completion.Completion;
import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.api.java.source.ElementHandle;
import org.netbeans.modules.j2ee.persistence.editor.CompletionContext;
import org.netbeans.modules.j2ee.persistence.editor.completion.db.DBCompletionContextResolver;
import org.netbeans.modules.j2ee.persistence.unit.PUDataLoader;
import org.netbeans.spi.editor.completion.CompletionDocumentation;
import org.netbeans.spi.editor.completion.CompletionItem;
import org.netbeans.spi.editor.completion.CompletionProvider;
import org.netbeans.spi.editor.completion.CompletionResultSet;
import org.netbeans.spi.editor.completion.CompletionTask;
import org.netbeans.spi.editor.completion.support.AsyncCompletionQuery;
import org.netbeans.spi.editor.completion.support.AsyncCompletionTask;
import org.openide.util.Exceptions;

/**
 * 
 * @author sp153251
 */
@MimeRegistration(mimeType = PUDataLoader.REQUIRED_MIME, service = CompletionProvider.class)//NOI18N
public class PUCompletionProvider implements CompletionProvider {

    @Override
    public CompletionTask createTask(int queryType, JTextComponent component) {
        if (queryType != CompletionProvider.COMPLETION_QUERY_TYPE && queryType !=CompletionProvider.COMPLETION_ALL_QUERY_TYPE) {
            return null;
        }
        return new AsyncCompletionTask(new PUCompletionQuery(queryType, component, component.getSelectionStart(), true), component);
    }

    @Override
    public int getAutoQueryTypes(JTextComponent component, String typedText) {
        return 0;//will not appear automatically
    }

    static int getRowFirstNonWhite(StyledDocument doc, int offset)
            throws BadLocationException {
        Element lineElement = doc.getParagraphElement(offset);
        int start = lineElement.getStartOffset();
        while (start + 1 < lineElement.getEndOffset()) {
            try {
                if (doc.getText(start, 1).charAt(0) != ' ') {
                    break;
                }
            } catch (BadLocationException ex) {
                throw (BadLocationException) new BadLocationException(
                        "calling getText(" + start + ", " + (start + 1)
                        + ") on doc of length: " + doc.getLength(), start).initCause(ex);
            }
            start++;
        }
        return start;
    }

    static int indexOfWhite(char[] line) {
        int i = line.length;
        while (--i > -1) {
            final char c = line[i];
            if (Character.isWhitespace(c)) {
                return i;
            }
        }
        org.netbeans.editor.ext.ExtSyntaxSupport f = new org.netbeans.editor.ext.ExtSyntaxSupport(null);
        return -1;
    }

    class PUCompletionQuery extends AsyncCompletionQuery {

        private ArrayList<CompletionContextResolver> resolvers;
        private List<JPACompletionItem> results;
        private byte hasAdditionalItems = 0; //no additional items
        private CompletionDocumentation documentation;
        private int anchorOffset;
        private int toolTipOffset;
        private JTextComponent component;
        private int queryType;
        private int caretOffset;
        private String filterPrefix;
        private ElementHandle element;
        private boolean hasTask;

        public PUCompletionQuery(int queryType, JTextComponent component, int caretOffset, boolean hasTask) {
            this.queryType = queryType;
            this.caretOffset = caretOffset;
            this.hasTask = hasTask;
            this.component = component;
            initResolvers();
        }

        private void initResolvers() {
            //XXX temporary - should be registered somehow better
            resolvers = new ArrayList<CompletionContextResolver>();
            resolvers.add(new DBCompletionContextResolver());
        }

        @Override
        protected void query(CompletionResultSet resultSet, Document doc, int caretOffset) {
            System.out.println("QUERY");
            List<JPACompletionItem> completionItems = new ArrayList<JPACompletionItem>();

            int anchorOffset = getCompletionItems(doc, caretOffset, completionItems);
            if(completionItems==null || completionItems.size()==0){
                System.out.println("EMPTY");
            }
            resultSet.addAllItems(completionItems);
            if (anchorOffset != -1) {
                resultSet.setAnchorOffset(anchorOffset);
            }

            resultSet.finish();
        }

        // This method is here for Unit testing purpose
        int getCompletionItems(Document doc, int caretOffset, List<JPACompletionItem> completionItems) {

            int anchorOffset = -1;
            CompletionContext context = new CompletionContext(doc, caretOffset);

            if (context.getCompletionType() == CompletionContext.CompletionType.NONE) {
                return anchorOffset;
            }

            switch (context.getCompletionType()) {
                case ATTRIBUTE_VALUE:
                    anchorOffset = PUCompletionManager.getDefault().completeAttributeValues(context, completionItems);
                    break;
                case ATTRIBUTE:
                    anchorOffset = PUCompletionManager.getDefault().completeAttributes(context, completionItems);
                    break;
                case TAG:
                    anchorOffset = PUCompletionManager.getDefault().completeElements(context, completionItems);
                    break;
                case VALUE:
                    anchorOffset = PUCompletionManager.getDefault().completeValues(context, completionItems);
                    break;
            }

            return anchorOffset;
        }

        @Override
        protected boolean canFilter(JTextComponent component) {
            return false;
        }
        
        @Override
        protected void filter(CompletionResultSet resultSet) {
            try {
                if ((queryType & COMPLETION_QUERY_TYPE) != 0) {
                    if (results != null) {
                        if (filterPrefix != null) {
                            resultSet.addAllItems(getFilteredData(results, filterPrefix));
                            resultSet.setHasAdditionalItems(hasAdditionalItems > 0);
                        } else {
                            Completion.get().hideDocumentation();
                            Completion.get().hideCompletion();
                        }
                    }
                } 
                resultSet.setAnchorOffset(anchorOffset);
            } catch (Exception ex) {
                Exceptions.printStackTrace(ex);
            }
            resultSet.finish();
        }
        private Collection getFilteredData(Collection<JPACompletionItem> data, String prefix) {
            if (prefix.length() == 0)
                return data;
            List ret = new ArrayList();
            for (Iterator<JPACompletionItem> it = data.iterator(); it.hasNext();) {
                CompletionItem itm = it.next();
                if (itm.getInsertPrefix().toString().startsWith(prefix))
                    ret.add(itm);
            }
            return ret;
        }
    }

}