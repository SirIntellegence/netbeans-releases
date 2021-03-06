/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
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
 * Contributor(s):
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2007 Sun
 * Microsystems, Inc. All Rights Reserved.
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
 */

package org.netbeans.modules.cnd.dwarfdump.dwarfconsts;

import java.util.HashMap;

/**
 *
 * @author ak119685
 */
public enum LNS {
    DW_LNS_extended_op(0x00),
    DW_LNS_copy(0x01),
    DW_LNS_advance_pc(0x02),
    DW_LNS_advance_line(0x03),
    DW_LNS_set_file(0x04),
    DW_LNS_set_column(0x05),
    DW_LNS_negate_stmt(0x06),
    DW_LNS_set_basic_block(0x07),
    DW_LNS_const_add_pc(0x08),
    DW_LNS_fixed_advance_pc(0x09),
    DW_LNS_set_prologue_end(0x0a),
    DW_LNS_set_epilogue_begin(0x0b),
    DW_LNS_set_isa(0x0c);
    
    private final int value;
    private static final HashMap<Integer, LNS> hashmap = new HashMap<Integer, LNS>();
    
    static {
        for (LNS elem : values()) {
            hashmap.put(elem.value, elem);
        }
    }
    
    LNS(int value) {
        this.value = value;
    }
    
    public static LNS get(int val) {
        return hashmap.get(val);
    }
    
    public int value() {
        return value;
    }
}

