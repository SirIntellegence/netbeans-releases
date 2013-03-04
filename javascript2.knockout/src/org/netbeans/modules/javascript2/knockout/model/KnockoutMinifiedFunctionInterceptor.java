/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2013 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2013 Sun Microsystems, Inc.
 */
package org.netbeans.modules.javascript2.knockout.model;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;
import org.netbeans.modules.csl.api.OffsetRange;
import org.netbeans.modules.javascript2.editor.model.Identifier;
import org.netbeans.modules.javascript2.editor.spi.model.FunctionArgument;
import org.netbeans.modules.javascript2.editor.model.JsObject;
import org.netbeans.modules.javascript2.editor.model.Occurrence;
import org.netbeans.modules.javascript2.editor.spi.model.FunctionInterceptor;
import org.netbeans.modules.javascript2.editor.spi.model.ModelElementFactory;

/**
 *
 * @author Petr Hejl
 */
@FunctionInterceptor.Registration(priority = 100)
public class KnockoutMinifiedFunctionInterceptor implements FunctionInterceptor {

    private static final String GLOBAL_KO_OBJECT = "ko"; // NOI18N

    @Override
    public Pattern getNamePattern() {
        return Pattern.compile("[a-z]\\.[a-z]"); // NOI18N
    }

    @Override
    public void intercept(String functionName, JsObject globalObject, ModelElementFactory factory, Collection<FunctionArgument> args) {
        if (args.size() == 2) {
            JsObject ko = globalObject.getProperty(GLOBAL_KO_OBJECT); // NOI18N
            if (ko == null) {
                ko = factory.newObject(globalObject, GLOBAL_KO_OBJECT, OffsetRange.NONE, true);
                globalObject.addProperty(GLOBAL_KO_OBJECT, ko);
            }

            Iterator<FunctionArgument> iterator = args.iterator();
            FunctionArgument arg1 = iterator.next();
            FunctionArgument arg2 = iterator.next();

            int offset = arg1.getOffset();
            if (arg1.getKind() == FunctionArgument.Kind.STRING
                    && !((String) arg1.getValue()).startsWith("__")) { // NOI18N
                JsObject parent = ko;
                JsObject oldParent = parent;

                String[] names = ((String) arg1.getValue()).split("\\."); // NOI18N
                int i = 0;
                if (names[0].equals(GLOBAL_KO_OBJECT)) {
                    i++;
                }

                for (; i < names.length; i++) {
                    String name = names[i];
                    if (i < names.length - 1) {
                        JsObject jsObject = oldParent.getProperty(name);
                        OffsetRange offsetRange = new OffsetRange(offset, offset + name.length());
                        if (jsObject == null) {
                            jsObject = factory.newObject(parent, name, offsetRange, true);
                            parent.addProperty(name, jsObject);
                            oldParent = jsObject;
                        } else if (!jsObject.isDeclared()) {
                            JsObject newJsObject = factory.newObject(parent, name, offsetRange, true);
                            parent.addProperty(name, newJsObject);
                            for (Occurrence occurrence : jsObject.getOccurrences()) {
                                newJsObject.addOccurrence(occurrence.getOffsetRange());
                            }
                            newJsObject.addOccurrence(jsObject.getDeclarationName().getOffsetRange());
                            oldParent = jsObject;
                            jsObject = newJsObject;
                        } else {
                            jsObject.addOccurrence(offsetRange);
                        }
                        parent = jsObject;
                    } else {
                        if (arg2.getKind() == FunctionArgument.Kind.REFERENCE) {
                            JsObject value = getReference(globalObject, (List<Identifier>) arg2.getValue());
                            if (value != null) {
                                OffsetRange offsetRange = new OffsetRange(offset, offset + name.length());
                                parent.addProperty(name, factory.newReference(parent, name, offsetRange, value, true));
                            }
                        }
                    }
                    offset += name.length() + 1;
                }
            }
        } else if (args.size() == 3) {
            Iterator<FunctionArgument> iterator = args.iterator();
            FunctionArgument arg1 = iterator.next();
            FunctionArgument arg2 = iterator.next();
            FunctionArgument arg3 = iterator.next();

            int offset = arg2.getOffset();
            if (arg1.getKind() == FunctionArgument.Kind.REFERENCE
                    && arg2.getKind() == FunctionArgument.Kind.STRING
                    && arg3.getKind() == FunctionArgument.Kind.REFERENCE) {

                JsObject object = getReference(globalObject, (List<Identifier>) arg1.getValue());
                JsObject value = getReference(globalObject, (List<Identifier>) arg3.getValue());
                if (object != null && value != null) {
                    String name = (String) arg2.getValue();
                    OffsetRange offsetRange = new OffsetRange(offset, offset + name.length());
                    object.addProperty(name, factory.newReference(object, name, offsetRange, value, true));
                }
            }
        }
    }

    private JsObject getReference(JsObject object, List<Identifier> identifier) {
        // XXX performance
        if (object == null) {
            return null;
        }
        if (identifier.isEmpty()) {
            return object;
        }
        return getReference(object.getProperty(identifier.get(0).getName()), identifier.subList(1, identifier.size()));

    }
}
