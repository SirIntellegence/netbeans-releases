/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2010 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2008 Sun Microsystems, Inc.
 */

package org.netbeans.modules.db.sql.analyzer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author Andrei Badea
 */
public class QualIdent implements Comparable<QualIdent> {

    private final List<String> parts;

    public QualIdent(String... parts) {
        this.parts = Arrays.asList(parts);
    }

    public QualIdent(List<String> parts) {
        this.parts = new ArrayList<String>(parts);
    }

    public QualIdent(QualIdent prefix, String name) {
        parts = new ArrayList<String>(prefix.parts.size() + 1);
        parts.addAll(prefix.parts);
        parts.add(name);
    }

    public QualIdent(String prefix, QualIdent name) {
        parts = new ArrayList<String>(name.parts.size() + 1);
        parts.add(prefix);
        parts.addAll(name.parts);
    }

    private QualIdent(List<String> parts, int start, int end) {
        this.parts = parts.subList(start, end);
    }

    public String getFirstQualifier() {
        if (parts.size() == 0) {
            throw new IllegalArgumentException("The identifier is empty.");
        }
        return parts.get(0);
    }

    public String getSecondQualifier() {
        if (parts.size() < 2) {
            throw new IllegalArgumentException("The identifier is empty or simple.");
        }
        return parts.get(1);
    }

    public String getSimpleName() {
        if (parts.size() == 0) {
            throw new IllegalArgumentException("The identifier is empty.");
        }
        return parts.get(parts.size() - 1);
    }

    public QualIdent getPrefix() {
        if (parts.size() == 0) {
            throw new IllegalArgumentException("The identifier is empty");
        }
        return new QualIdent(parts, 0, parts.size() - 1);
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public boolean isSimple() {
        return size() == 1;
    }

    public boolean isSingleQualified() {
        return size() == 2;
    }

    public int size() {
        return parts.size();
    }

    public boolean isPrefixedBy(QualIdent prefix) {
        if (this.size() < prefix.size()) {
            return false;
        }
        for (int i = 0; i < prefix.size(); i++) {
            if (!this.parts.get(i).equals(prefix.parts.get(i))) {
                return false;
            }
        }
        return true;
    }

    public int compareTo(QualIdent that) {
        for (int i = 0; ; i++) {
            if (i < this.parts.size()) {
                if (i < that.parts.size()) {
                    int compare = this.parts.get(i).compareToIgnoreCase(that.parts.get(i));
                    if (compare != 0) {
                        return compare;
                    }
                } else {
                    return 1;
                }
            } else {
                if (i < that.parts.size()) {
                    return -1;
                } else {
                    return 0;
                }
            }
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof QualIdent)) {
            return false;
        }
        QualIdent that = (QualIdent) obj;
        return that.parts.equals(this.parts);
    }

    @Override
    public int hashCode() {
        return parts.hashCode();
    }

    @Override
    public String toString() {
        if (parts.size() == 0) {
            return "<empty>"; // NOI18N
        }
        StringBuilder result = new StringBuilder(parts.size() * 10);
        Iterator<String> i = parts.iterator();
        while (i.hasNext()) {
            result.append(i.next());
            if (i.hasNext()) {
                result.append('.');
            }
        }
        return result.toString();
    }
}
