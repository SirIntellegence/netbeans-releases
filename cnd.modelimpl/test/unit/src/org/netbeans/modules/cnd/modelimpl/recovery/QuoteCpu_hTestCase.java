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
package org.netbeans.modules.cnd.modelimpl.recovery;

import org.netbeans.modules.cnd.modelimpl.recovery.base.Diff;
import org.netbeans.modules.cnd.modelimpl.recovery.base.Grama;
import org.netbeans.modules.cnd.modelimpl.recovery.base.Diffs;
import org.netbeans.modules.cnd.modelimpl.recovery.base.Gramas;
import org.netbeans.modules.cnd.modelimpl.recovery.base.Golden;
import org.netbeans.modules.cnd.modelimpl.recovery.base.RecoveryTestCaseBase;
import java.io.File;
import org.junit.Test;
import org.netbeans.junit.Manager;

/**
 *
 * @author Alexander Simon
 */
public class QuoteCpu_hTestCase extends RecoveryTestCaseBase {

    private static final String SOURCE = "cpu.h";
    public QuoteCpu_hTestCase(String testName, Grama gramma, Diff diff, Golden golden) {
        super(testName, gramma, diff, golden);
    }
    
    @Override
    protected File getTestCaseDataDir() {
        return Manager.normalizeFile(new File(getDataDir(), "common/recovery/cpu_h"));
    }

    @Grama(newGramma = false)
    @Golden
    @Test
    public void A_Golden() throws Exception {
        implTest(SOURCE);
    }

    @Grama(newGramma = true)
    @Diff(file=SOURCE)
    @Test
    public void beforeClass0() throws Exception {
        implTest(SOURCE);
    }

    @Gramas({
        @Grama(newGramma = false),
        @Grama(newGramma = true)
    })
    @Diffs({
        @Diff(file=SOURCE, line = 46, column = 1, length = 0, insert = "ID()"),
        @Diff(file=SOURCE, line = 46, column = 1, length = 0, insert = "*"),
        @Diff(file=SOURCE, line = 46, column = 1, length = 0, insert = "&"),
        @Diff(file=SOURCE, line = 46, column = 1, length = 0, insert = "{"),
        @Diff(file=SOURCE, line = 46, column = 1, length = 0, insert = "}"),
        @Diff(file=SOURCE, line = 46, column = 1, length = 0, insert = "+"),
        @Diff(file=SOURCE, line = 46, column = 1, length = 0, type = "int * a()")
    })
    @Test
    public void beforeClass1() throws Exception {
        implTest(SOURCE);
    }
    
    @Gramas({
        @Grama(newGramma = false),
        @Grama(newGramma = true)
    })
    @Diffs({
        @Diff(file=SOURCE, line = 53, column = 1, length = 0, insert = "ID"),
        @Diff(file=SOURCE, line = 53, column = 1, length = 0, insert = "ID()"),
        @Diff(file=SOURCE, line = 53, column = 1, length = 0, insert = "ID(SIGNAL)"),
        @Diff(file=SOURCE, line = 53, column = 1, length = 0, insert = "class"),
        @Diff(file=SOURCE, line = 53, column = 1, length = 0, type = "int * a()")
    })
    @Test
    public void betweenClassMembers1() throws Exception {
        implTest(SOURCE);
    }

    @Gramas({
        @Grama(newGramma = false),
        @Grama(newGramma = true)
    })
    @Diffs({
        @Diff(file=SOURCE, line = 58, column = 36, length = 0, insert = " ID "),
        @Diff(file=SOURCE, line = 58, column = 36, length = 0, insert = " ID() "),
        @Diff(file=SOURCE, line = 58, column = 36, length = 0, insert = " ID(E) "), 
        @Diff(file=SOURCE, line = 58, column = 36, length = 0, type = "  const throw A")
    })
    @Test
    public void insideMemberDeclaration1() throws Exception {
        implTest(SOURCE);
    }

    @Gramas({
        @Grama(newGramma = false),
        @Grama(newGramma = true)
    })
    @Diffs({
        @Diff(file=SOURCE, line = 58, column = 35, length = 0, insert = "ID(E)"),
        @Diff(file=SOURCE, line = 58, column = 35, length = 0, insert = "class"),
        @Diff(file=SOURCE, line = 58, column = 35, length = 0, insert = "struct"),
        @Diff(file=SOURCE, line = 58, column = 35, length = 0, type = "int * a")
    })
    @Test
    public void insideMemberParameter1() throws Exception {
        implTest(SOURCE);
    }
}
