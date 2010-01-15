/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2009 Sun Microsystems, Inc. All rights reserved.
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
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
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
package org.netbeans.modules.mercurial.ui.log;

import org.netbeans.modules.versioning.spi.VCSContext;
import java.io.File;
import org.netbeans.modules.mercurial.util.HgUtils;
import org.openide.nodes.Node;
import org.openide.util.NbBundle;

/**
 * Log action for mercurial:
 * hg log - show revision history of entire repository or files
 *
 * @author John Rice
 */
public class IncomingAction extends SearchHistoryAction {

    protected String getBaseName(Node[] nodes) {
        return "CTL_MenuItem_ShowIncoming";                             //NOI18N
    }

    @Override
    protected void performContextAction(Node[] nodes) {
        VCSContext context = HgUtils.getCurrentContext(nodes);
        openIncoming(context);
    }

    /**
     * Opens the Seach History panel to view Mercurial Incoming Changesets that will be sent on next Pull from remote repo
     * using: hg incoming - to get the data
     */
    private void openIncoming (VCSContext context) {
        File repositoryRoot = getRepositoryRoot(context);
        if (repositoryRoot == null) {
            return;
        }
        outputSearchContextTab(repositoryRoot, "MSG_LogIncoming_Title");
        SearchHistoryTopComponent tc = new SearchHistoryTopComponent(new File[] {repositoryRoot});
        tc.setDisplayName(NbBundle.getMessage(IncomingAction.class, "MSG_Incoming_TabTitle", repositoryRoot.getName()));
        tc.open();
        tc.requestActive();
        tc.searchIncoming();
    }
}
