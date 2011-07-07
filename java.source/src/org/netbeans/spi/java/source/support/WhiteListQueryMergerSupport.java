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
package org.netbeans.spi.java.source.support;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.netbeans.api.java.source.ElementHandle;
import org.netbeans.spi.java.source.WhiteListQueryImplementation;
import org.netbeans.spi.java.source.WhiteListQueryImplementation.WhiteListImplementation;
import org.netbeans.spi.project.LookupMerger;
import org.openide.filesystems.FileObject;
import org.openide.util.Lookup;

/**
 *
 */
public class WhiteListQueryMergerSupport {

    /**
     * Placed in a lookup this class will merge all other WhiteListQueryImplementation
     * registered in the same lookup. All individual WhiteListQueryImplementation must
     * allow invocation and/or overriding of a method to get positive return.
     */
    public static LookupMerger<WhiteListQueryImplementation> createWhiteListQueryMerger() {
        return new WhiteListQueryMerger();
    }
    
    private static class WhiteListQueryMerger implements LookupMerger<WhiteListQueryImplementation> {

        public Class<WhiteListQueryImplementation> getMergeableClass() {
            return WhiteListQueryImplementation.class;
        }

        public WhiteListQueryImplementation merge(Lookup lookup) {
            return new WhiteListQueryImplementationMerged(lookup);
        }
    }
    
    private static class WhiteListQueryImplementationMerged implements WhiteListQueryImplementation {
        private Lookup lkp;

        public WhiteListQueryImplementationMerged(Lookup lkp) {
            this.lkp = lkp;
        }

        @Override
        public WhiteListImplementation getWhiteList(FileObject file) {
            List<WhiteListImplementation> list = new ArrayList<WhiteListImplementation>();
            Collection<? extends WhiteListQueryImplementation> wl = lkp.lookupAll(WhiteListQueryImplementation.class);
            for (WhiteListQueryImplementation impl : wl) {
                WhiteListImplementation i = impl.getWhiteList(file);
                if (i != null) {
                    list.add(i);
                }
            }
            if (list.isEmpty()) {
                return null;
            }
            return new WhiteListImplementationMerged(list);
        }
    }
    
    private static class WhiteListImplementationMerged implements WhiteListImplementation {

        private List<WhiteListImplementation> list;

        public WhiteListImplementationMerged(List<WhiteListImplementation> list) {
            this.list = list;
        }

        @Override
        public boolean canInvoke(ElementHandle<?> element) {
            for (WhiteListImplementation impl : list) {
                if (!impl.canInvoke(element)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public boolean canOverride(ElementHandle<?> element) {
            for (WhiteListImplementation impl : list) {
                if (!impl.canOverride(element)) {
                    return false;
                }
            }
            return true;
        }
    }
    
}
