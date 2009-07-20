/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2009 Sun Microsystems, Inc. All rights reserved.
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
 * Portions Copyrighted 2009 Sun Microsystems, Inc.
 */
package org.netbeans.modules.web.jsf.editor;

import java.awt.Color;
import org.netbeans.modules.html.editor.completion.HtmlCompletionItem;

/**
 *
 * @author marekfukala
 */
public class JsfCompletionItem {

    //----------- Factory methods --------------
    public static JsfTag createTag(String name, int substitutionOffset, String helpId, String uri, boolean autoimport) {
        return new JsfTag(name, substitutionOffset, helpId, uri, autoimport);
    }

    public static class JsfTag extends HtmlCompletionItem.Tag {

        private static final String BOLD_OPEN_TAG = "<b>"; //NOI18N
        private static final String BOLD_END_TAG = "</b>"; //NOI18N

        private String uri;
        private boolean autoimport; //autoimport (declare) the tag namespace if set to true

        public JsfTag(String text, int substitutionOffset, String helpId, String uri, boolean autoimport) {
            super(text, substitutionOffset, helpId, true);
            this.uri = uri;
            this.autoimport = autoimport;
        }

        @Override
        protected String getRightHtmlText() {
            return "<font color=#" + (autoimport ? hexColorCode(Color.RED.darker().darker()) : hexColorCode(Color.GRAY)) + ">" + uri + "</font>";
        }

        //use bold font
        @Override
        protected String getLeftHtmlText() {
            StringBuffer buff = new StringBuffer();
            buff.append(BOLD_OPEN_TAG);
            buff.append(super.getLeftHtmlText());
            buff.append(BOLD_END_TAG);
            return buff.toString();
        }

        @Override
        public int getSortPriority() {
            return DEFAULT_SORT_PRIORITY - 5;
        }
    }
}
