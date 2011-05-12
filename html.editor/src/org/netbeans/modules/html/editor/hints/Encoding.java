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
package org.netbeans.modules.html.editor.hints;

import java.util.regex.Pattern;

/**
 *
 * @author marekfukala
 */
public class Encoding extends PatternRule {

    private static final String[] PATTERNS_SOURCES = new String[]{
        "The internal character encoding declaration specified .*? which is not a rough superset of ASCII",
        "The encoding .*? is not",
        "Authors should not use the character encoding",
        "The character encoding .*? is not widely supported",
        "Using .*? instead of the declared encoding",
        "Unsupported character encoding name: .*?. Will continue sniffing",
        "Overriding document character encoding from ",
        //ErrorReportingTokenizer
        "The character encoding of the document was not explicit but the document contains non-ASCII",
        "No explicit character encoding declaration has been seen yet (assumed .*?) but the document contains non-ASCII",
        "This document is not mappable to XML 1.0 without data loss due to .*? which is not a legal XML 1.0 character",
        "Astral non-character",
        "Forbidden code point",
        "Document uses the Unicode Private Use Area(s), which should not be used in publicly exchanged documents. (Charmod C073)",
        
        //TreeBuilder
        "Attribute .content. would be sniffed as an internal character encoding declaration but there was no matching",
        
            
            
    }; //NOI18N
    
    private final static Pattern[] PATTERNS = buildPatterns(PATTERNS_SOURCES);

    @Override
    public Pattern[] getPatterns() {
        return PATTERNS;
    }
    
}
