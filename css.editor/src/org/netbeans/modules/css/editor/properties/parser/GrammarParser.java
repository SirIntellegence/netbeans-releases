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
package org.netbeans.modules.css.editor.properties.parser;

import java.util.StringTokenizer;
import org.netbeans.modules.css.editor.module.CssModuleSupport;
import org.netbeans.modules.css.editor.module.spi.Property;

/**
 * Not threadsafe
 *
 * @author mfukala
 */
public class GrammarParser {
    
    public static final char ARTIFICIAL_ELEMENT_PREFIX = '@';
    
    public static boolean isArtificialElementName(CharSequence name) {
        if(name.length() == 0) {
            return false;
        }
        return name.charAt(0) == ARTIFICIAL_ELEMENT_PREFIX;
    }

    public static GroupGrammarElement parse(String expresssion) {
        return parse(expresssion, null);
    }

    public static GroupGrammarElement parse(String expression, String propertyName) {
        int group_index = 0;
        int openedParenthesis = 0;
        GroupGrammarElement root = new GroupGrammarElement(null, group_index, propertyName);
        ParserInput input = new ParserInput(expression);

        parseElements(input, root, false, group_index, openedParenthesis);

        if (openedParenthesis != 0) {
            throw new IllegalStateException("Bracket pairing doesn't match: " + openedParenthesis);
        }
        return root;
    }

    private static void parseElements(ParserInput input, GroupGrammarElement parent, boolean ignoreInherits,
            int group_index, int openedParenthesis) {
        GrammarElement last = null;
        for (;;) {
            char c = input.read();
            if (c == Character.MAX_VALUE) {
                return;
            }
            switch (c) {
                case ' ':
                case '\t':
                    //ws, ignore
                    break;
//                case '&':
//                    char next = input.read();
//                    if (next == '&') {
//                        //the group is a list
//                        parent.setType(GroupGrammarElement.Type.ALL);
//                    } else {
//                        input.backup(1);
//                    }
//                    break;
//                    
                case '[':
                    openedParenthesis++;
                    //group start
                    last = new GroupGrammarElement(parent, ++group_index);
                    parseElements(input, (GroupGrammarElement) last, false, group_index, openedParenthesis);
                    parent.addElement(last);
                    break;

                case '|':
                    char next = input.read();
                    if (next == '|') {
                        //the group is a list
                        parent.setType(GroupGrammarElement.Type.LIST);
                    } else {
                        input.backup(1);
                        parent.setType(GroupGrammarElement.Type.SET);
                        // else it means OR
                    }
                    break;

                case ']':
                    openedParenthesis--;
                    //group end
                    return;

                case '<':
                    //reference
                    StringBuilder buf = new StringBuilder();
                    for (;;) {
                        c = input.read();
                        if (c == '>') {
                            break;
                        } else {
                            buf.append(c);
                        }
                    }

                    //resolve reference
                    String referredElementName = buf.toString();

                    //first try to resolve the refered element name with the at-sign prefix so
                    //the property appearance may contain link to appearance, which in fact
                    //will be resolved as the @appearance property:
                    //
                    //appearance=<appearance> |normal
                    //@appearance=...
                    //

                    //without explicit at-sign prefix, first try to resolve
                    //with the prefix, if not found without prefix
                    Property p = CssModuleSupport.getProperties().get("@" + referredElementName);
                    if (p == null) {
                        p = CssModuleSupport.getProperties().get(referredElementName);
                    }

                    if (p == null) {
                        throw new IllegalStateException("parsing error - no referred element '" + referredElementName + "' found!"
                                + " Read input: " + input.readText()); //NOI18N
                    }

                    last = new GroupGrammarElement(parent, ++group_index, referredElementName);

//                    System.out.println("resolving element " + referredElementName + " (" + p.valuesText() + ") into group " + last.toString()); //NOI18N
                    ParserInput pinput = new ParserInput(p.getValueGrammar());

                    //ignore inherit tokens in the subtree
                    parseElements(pinput, (GroupGrammarElement) last, true, group_index, openedParenthesis);

                    parent.addElement(last);
                    break;

                case '!':
                    //unit value
                    buf = new StringBuilder();
                    for (;;) {
                        c = input.read();
                        if (c == Character.MAX_VALUE) {
                            break;
                        }
                        if (isEndOfValueChar(c)) {
                            input.backup(1);
                            break;
                        } else {
                            buf.append(c);
                        }
                    }

                    last = new ValueGrammarElement(parent);
                    ((ValueGrammarElement) last).setValue(buf.toString());
                    ((ValueGrammarElement) last).setIsUnit(true);
                    parent.addElement(last);
                    break;

                case '{':
                    //multiplicity range {min,max}
                    StringBuilder text = new StringBuilder();
                    for (;;) {
                        c = input.read();
                        if (c == '}') {
                            break;
                        } else {
                            text.append(c);
                        }
                    }
                    StringTokenizer st = new StringTokenizer(text.toString(), ","); //NOI18N
                    int min = Integer.parseInt(st.nextToken());
                    int max = Integer.parseInt(st.nextToken());

                    last.setMinimumOccurances(min);
                    last.setMaximumOccurances(max);

                    break;

                case '+':
                    //multiplicity 1-infinity
                    last.setMaximumOccurances(Integer.MAX_VALUE);
                    break;

                case '*':
                    //multiplicity 0-infinity
                    last.setMinimumOccurances(0);
                    last.setMaximumOccurances(Integer.MAX_VALUE);
                    break;

                case '?':
                    //multiplicity 0-1
                    last.setMinimumOccurances(0);
                    last.setMaximumOccurances(1);
                    break;


                default:
                    //values
                    buf = new StringBuilder();
                    for (;;) {
                        if (c == Character.MAX_VALUE) {
                            break;
                        }
                        if (isEndOfValueChar(c)) {
                            input.backup(1);
                            break;
                        } else {
                            buf.append(c);
                        }

                        c = input.read(); //also include the char from main loop

                    }
                    String image = buf.toString();

                    if (!(ignoreInherits && "inherit".equalsIgnoreCase(image))) { //NOI18N
                        last = new ValueGrammarElement(parent);
                        ((ValueGrammarElement) last).setValue(image);
                        ((ValueGrammarElement) last).setIsUnit(false);
                        parent.addElement(last);
                    }
                    break;

            }
        }

    }

    private static boolean isEndOfValueChar(char c) {
        return c == ' ' || c == '+' || c == '?' || c == '{' || c == '[' || c == ']' || c == '|';
    }

    private static class ParserInput {

        CharSequence text;
        private int pos = 0;

        private ParserInput(CharSequence text) {
            this.text = text;
        }

        public char read() {
            if (pos == text.length()) {
                return Character.MAX_VALUE;
            } else {
                return text.charAt(pos++);
            }
        }

        public void backup(int chars) {
            pos -= chars;
        }

        public CharSequence readText() {
            return text.subSequence(0, pos);
        }
    }
}
