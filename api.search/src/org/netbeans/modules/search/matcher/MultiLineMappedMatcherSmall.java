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
package org.netbeans.modules.search.matcher;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.CharBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.netbeans.api.queries.FileEncodingQuery;
import org.netbeans.api.search.SearchPattern;
import org.netbeans.api.search.provider.SearchListener;
import org.netbeans.modules.search.Constants;
import org.netbeans.modules.search.MatchingObject.Def;
import org.netbeans.modules.search.TextDetail;
import org.netbeans.modules.search.TextRegexpUtil;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;

/**
 * Multi-line matcher for small files that uses file-mapped memory.
 *
 * @author jhavlin
 */
public class MultiLineMappedMatcherSmall extends AbstractMatcher {

    private SearchPattern searchPattern;
    private Pattern pattern;
    private int fileMatches = 0;
    private int itemMatches = 0;

    public MultiLineMappedMatcherSmall(SearchPattern searchPattern) {
        this.searchPattern = searchPattern;
        this.pattern = TextRegexpUtil.makeTextPattern(searchPattern);
    }

    @Override
    protected Def checkMeasuredInternal(FileObject fo,
            SearchListener listener) {

        MappedByteBuffer bb = null;
        FileChannel fc = null;
        try {

            listener.fileContentMatchingStarted(fo.getPath());
            File file = FileUtil.toFile(fo);

            // Open the file and then get a channel from the stream
            FileInputStream fis = new FileInputStream(file);
            fc = fis.getChannel();

            // Get the file's size and then map it into memory
            int sz = (int) fc.size();
            bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, sz);

            //  if (asciiPattern && !matchesIgnoringEncoding(bb)) {
            //    return null;
            //}

            // Decode the file into a char buffer
            Charset charset = FileEncodingQuery.getEncoding(fo);
            CharsetDecoder decoder = prepareDecoder(charset);
            decoder.onUnmappableCharacter(CodingErrorAction.IGNORE);
            CharBuffer cb = decoder.decode(bb);

            List<TextDetail> textDetails = matchWholeFile(cb, fo);

            if (textDetails == null) {
                return null;
            } else {
                Def def = new Def(fo, decoder.charset(), textDetails);
                return def;
            }
        } catch (Exception e) {
            listener.generalError(e);
            return null;
        } finally {
            if (fc != null) {
                try {
                    fc.close();
                } catch (IOException ex) {
                    listener.generalError(ex);
                }
            }
            MatcherUtils.unmap(bb);
        }
    }

    /**
     * Perform pattern matching inside the whole file.
     *
     * @param cb Character buffer.
     * @param fo File object.
     */
    private List<TextDetail> matchWholeFile(CharSequence cb, FileObject fo)
            throws DataObjectNotFoundException {

        Matcher textMatcher = pattern.matcher(cb);
        DataObject dataObject = null;
        LineInfoHelper lineInfoHelper = new LineInfoHelper(cb);

        List<TextDetail> textDetails = null;

        while (textMatcher.find()) {
            if (textDetails == null) {
                textDetails = new LinkedList<TextDetail>();
                dataObject = DataObject.find(fo);
                fileMatches++;
            }
            itemMatches++;
            TextDetail ntd = new TextDetail(dataObject, searchPattern);
            lineInfoHelper.findAndSetPositionInfo(ntd, textMatcher.start(),
                    textMatcher.end(), textMatcher.group());
            textDetails.add(ntd);
            if (fileMatches >= Constants.COUNT_LIMIT
                    || itemMatches
                    >= Constants.DETAILS_COUNT_LIMIT) {
                break;
            }
        }
        return textDetails;
    }

    @Override
    public void terminate() {
        // no need to terminate searching in small files
    }

    /**
     * Helper for associating line and position info to TextDetail objects.
     */
    static class LineInfoHelper {

        private static final Pattern linePattern =
                Pattern.compile("(.*)(\\r\\n|\\n|\\r)");                //NOI18N
        private CharSequence charSequence;
        private Matcher lineMatcher;
        private int lastStartPos = 0;
        private int currentLineNumber = 0;
        private int currentLineStart = -1;
        private int currentLineEnd = -1;
        private String lastLine = null;

        public LineInfoHelper(CharSequence charSequence) {
            this.charSequence = charSequence;
            this.lineMatcher = linePattern.matcher(charSequence);
        }

        /**
         * Find line number and text for passed positions.
         *
         * State of line matcher is defined by previous invocations of this
         * method with the same line matcher.
         *
         * Start position must be bigger than it was in the previous invocation.
         *
         * @param textDetail Text details to set.
         * @param startPos Start position of found text, for which we are
         * looking for the correct line number
         * @param endPos End position of found text.
         */
        public void findAndSetPositionInfo(TextDetail textDetail,
                int startPos, int endPos, String text) {
            if (startPos < lastStartPos) {
                throw new IllegalStateException(
                        "Start offset lower than the previous one.");   //NOI18N
            }
            updateStateForPosition(startPos);
            setTextDetailInfo(textDetail, startPos, endPos, text);
        }

        /**
         * Update internal state for a position of a character in the file.
         */
        private void updateStateForPosition(int pos) {
            if (pos > currentLineEnd) {
                boolean found = false;
                while (lineMatcher.find()) {
                    currentLineNumber++;
                    currentLineEnd = lineMatcher.end() - 1;
                    if (lineMatcher.end() > pos) {
                        currentLineStart = lineMatcher.start();
                        lastLine = lineMatcher.group().trim();
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    if (currentLineNumber == 0) {
                        setupOnlyLine();
                    } else {
                        setupLastLine();
                    }
                }
            }
        }

        /**
         * Set properties of a TextDetail instance.
         */
        private void setTextDetailInfo(TextDetail textDetail, int startPos,
                int endPos, String text) {
            textDetail.associate(currentLineNumber,
                    startPos - currentLineStart + 1, lastLine);
            textDetail.setStartOffset(startPos);
            textDetail.setEndOffset(endPos);
            textDetail.setMarkLength(endPos - startPos);
            textDetail.setMatchedText(text);
        }

        /**
         * Set internal state if the last line in a multi-line file has been
         * reached
         */
        private void setupLastLine() {
            currentLineNumber++;
            currentLineStart = currentLineEnd + 1;
            currentLineEnd = charSequence.length();
            lastLine = charSequence.subSequence(
                    currentLineStart,
                    currentLineEnd).toString().trim();
        }

        /**
         * Set internal state if there is only single line in the file.
         */
        private void setupOnlyLine() {
            currentLineNumber = 1;
            String s = charSequence.toString();
            currentLineStart = 0;
            currentLineEnd = s.length();
            lastLine = s.trim();
        }
    }
}
