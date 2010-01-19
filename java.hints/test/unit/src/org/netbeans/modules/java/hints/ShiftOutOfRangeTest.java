/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2010 Sun Microsystems, Inc. All rights reserved.
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
 * Portions Copyrighted 2010 Sun Microsystems, Inc.
 */

package org.netbeans.modules.java.hints;

import org.junit.Test;
import org.netbeans.modules.java.hints.jackpot.code.spi.TestBase;
import org.openide.util.NbBundle;

/**
 *
 * @author Jan Jancura
 */
public class ShiftOutOfRangeTest extends TestBase {

    public ShiftOutOfRangeTest (String name) {
        super (name, ShiftOutOfRange.class);
    }

    @Test
    public void testOk () throws Exception {
        performAnalysisTest (
            "test/Test.java",
            "package test;\n" +
            "class Test {\n" +
            "    void test () {\n" +
            "        int i = 10;\n" +
            "        i = i >> 10;\n" +
            "        i = i >>> 10;\n" +
            "        i = i << 10;\n" +

            "        i = i >> 10l;\n" +
            "        i = i >>> 10l;\n" +
            "        i = i << 10l;\n" +

            "        long l = 35;\n" +
            "        l = l >> 35;\n" +
            "        l = l >>> 35;\n" +
            "        l = l << 35;\n" +

            "        l = l >> 35l;\n" +
            "        l = l >>> 35l;\n" +
            "        l = l << 35l;\n" +

            "        l = l >> '0';\n" +
            "        l = l >>> '0';\n" +
            "        l = l << '0';\n" +

            "    }\n" +
            "}"
        );
    }

    @Test
    public void testWarning () throws Exception {
        performAnalysisTest (
            "test/Test.java",
            "package test;\n" +
            "class Test {\n" +
            "    void test () {\n" +
            "        int i = 10;\n" +
            "        i = i >> 33;\n" +
            "        i = i >>> 33;\n" +
            "        i = i << 33;\n" +
            "        i = i >> -1;\n" +
            "        i = i >>> -1;\n" +
            "        i = i << -1;\n" +

            "        i = i >> 33l;\n" +
            "        i = i >>> 33l;\n" +
            "        i = i << 33l;\n" +

            "        long l = 35;\n" +
            "        l = l >> 65;\n" +
            "        l = l >>> 65;\n" +
            "        l = l << 65;\n" +
            "        l = l >> -1;\n" +
            "        l = l >>> -1;\n" +
            "        l = l << -1;\n" +

            "        l = l >> 65l;\n" +
            "        l = l >>> 65l;\n" +
            "        l = l << 65l;\n" +
            "        l = l >> -1l;\n" +
            "        l = l >>> -1l;\n" +
            "        l = l << -1l;\n" +

            "        l = l >> 'a';\n" +
            "        l = l >>> 'a';\n" +
            "        l = l << 'a';\n" +

            "    }\n" +
            "}",
            "4:12-4:19:verifier:Shift operation outside of the reasonable range 0..31",
            "5:12-5:20:verifier:Shift operation outside of the reasonable range 0..31",
            "6:12-6:19:verifier:Shift operation outside of the reasonable range 0..31",
            "7:12-7:19:verifier:Shift operation outside of the reasonable range 0..31",
            "8:12-8:20:verifier:Shift operation outside of the reasonable range 0..31",
            "9:12-9:19:verifier:Shift operation outside of the reasonable range 0..31",

            "10:12-10:20:verifier:Shift operation outside of the reasonable range 0..31",
            "11:12-11:21:verifier:Shift operation outside of the reasonable range 0..31",
            "12:12-12:20:verifier:Shift operation outside of the reasonable range 0..31",

            "14:12-14:19:verifier:Shift operation outside of the reasonable range 0..63",
            "15:12-15:20:verifier:Shift operation outside of the reasonable range 0..63",
            "16:12-16:19:verifier:Shift operation outside of the reasonable range 0..63",
            "17:12-17:19:verifier:Shift operation outside of the reasonable range 0..63",
            "18:12-18:20:verifier:Shift operation outside of the reasonable range 0..63",
            "19:12-19:19:verifier:Shift operation outside of the reasonable range 0..63",

            "20:12-20:20:verifier:Shift operation outside of the reasonable range 0..63",
            "21:12-21:21:verifier:Shift operation outside of the reasonable range 0..63",
            "22:12-22:20:verifier:Shift operation outside of the reasonable range 0..63",
            "23:12-23:20:verifier:Shift operation outside of the reasonable range 0..63",
            "24:12-24:21:verifier:Shift operation outside of the reasonable range 0..63",
            "25:12-25:20:verifier:Shift operation outside of the reasonable range 0..63",

            "26:12-26:20:verifier:Shift operation outside of the reasonable range 0..63",
            "27:12-27:21:verifier:Shift operation outside of the reasonable range 0..63",
            "28:12-28:20:verifier:Shift operation outside of the reasonable range 0..63"

        );
    }

    static {
        NbBundle.setBranding("test");
    }
}