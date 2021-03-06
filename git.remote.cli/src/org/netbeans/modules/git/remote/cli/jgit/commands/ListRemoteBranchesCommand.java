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
 * Portions Copyrighted 2010 Sun Microsystems, Inc.
 */

package org.netbeans.modules.git.remote.cli.jgit.commands;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import org.netbeans.modules.git.remote.cli.GitBranch;
import org.netbeans.modules.git.remote.cli.GitConstants;
import org.netbeans.modules.git.remote.cli.GitException;
import org.netbeans.modules.git.remote.cli.jgit.GitRef;
import org.netbeans.modules.git.remote.cli.jgit.GitClassFactory;
import org.netbeans.modules.git.remote.cli.jgit.JGitRepository;
import org.netbeans.modules.git.remote.cli.jgit.Utils;
import org.netbeans.modules.git.remote.cli.progress.ProgressMonitor;
import org.netbeans.modules.remotefs.versioning.api.ProcessUtils;
import org.netbeans.modules.versioning.core.api.VCSFileProxy;

/**
 * @author ondra
 */
public class ListRemoteBranchesCommand extends TransportCommand {
    private LinkedHashMap<String, GitBranch> remoteBranches;
    private final String remoteUrl;
    private Collection<GitRef> refs;
    private final ProgressMonitor monitor;

    public ListRemoteBranchesCommand (JGitRepository repository, GitClassFactory gitFactory, String remoteRepositoryUrl, ProgressMonitor monitor) {
        super(repository, gitFactory, remoteRepositoryUrl, monitor);
        this.remoteUrl = remoteRepositoryUrl;
        this.monitor = monitor;
    }

    public Map<String, GitBranch> getBranches () {
        return remoteBranches;
    }

    private void processRefs () {
        remoteBranches = new LinkedHashMap<String, GitBranch>();
        remoteBranches.putAll(Utils.refsToBranches(refs, GitConstants.R_HEADS, getClassFactory()));
    }
    
    
    @Override
    protected void prepare() throws GitException {
        super.prepare();
        addArgument(0, "ls-remote"); //NOI18N
        addArgument(0, "--heads"); //NOI18N
        addArgument(0, remoteUrl);
    }

    @Override
    protected final void runTransportCommand () throws GitException {
        ProcessUtils.Canceler canceled = new ProcessUtils.Canceler();
        if (monitor != null) {
            monitor.setCancelDelegate(canceled);
        }
        try {
            refs = new ArrayList<>();
            new Runner(canceled, 0){

                @Override
                public void outputParser(String output) throws GitException {
                    parseListBranchesOutput(output);
                }
            }.runCLI();
            processRefs();
        } catch (GitException t) {
            throw t;
        } catch (Throwable t) {
            if (canceled.canceled()) {
            } else {
                throw new GitException(t);
            }
        }
    }
    
    private void parseListBranchesOutput(String output) {
        //899d3bd2446e6a6e1e21aeff1018391f2e140602	refs/heads/master
        //b59d2ca45b43d3fb912dae160fef773a940bf97d	refs/heads/new_branch
        for (String line : output.split("\n")) { //NOI18N
            String[] s = line.split("\\s");
            if (s.length >= 2) {
                String revision = s[0];
                String name = s[1];
                refs.add(new GitRef(name, revision));
            }
        }
    }
}
