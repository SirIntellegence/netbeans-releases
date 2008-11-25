/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
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
 * Contributor(s):
 *
 * Portions Copyrighted 2007 Sun Microsystems, Inc.
 */
package org.netbeans.modules.csl.hints.infrastructure;

import org.netbeans.modules.csl.api.Rule.UserConfigurableRule;
import org.netbeans.modules.csl.api.RuleContext;
import org.netbeans.spi.editor.hints.ChangeInfo;
import org.netbeans.spi.editor.hints.EnhancedFix;
import org.openide.util.NbBundle;

/**
 *
 * @author Tor Norbye
 */
final class DisableHintFix implements EnhancedFix {
    private final GsfHintsManager manager;
    private final RuleContext context;
    private final UserConfigurableRule rule;
    private final String sortText;
    
    DisableHintFix(GsfHintsManager manager, RuleContext context, UserConfigurableRule rule, String sortText) {
        this.manager = manager;
        this.context = context;
        this.rule = rule;
        this.sortText = sortText;
    }

    public String getText() {
        return NbBundle.getMessage(DisableHintFix.class, "DisableHint");
    }
    
    public ChangeInfo implement() throws Exception {
        HintsSettings.setEnabled(HintsSettings.getPreferences(manager, rule, null), false);

        manager.refreshHints(context);

        return null;
    }

    public CharSequence getSortText() {
        return sortText;
    }
}
