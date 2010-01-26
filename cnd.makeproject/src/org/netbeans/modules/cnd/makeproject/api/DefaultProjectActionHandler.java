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
package org.netbeans.modules.cnd.makeproject.api;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.netbeans.modules.cnd.api.compilers.CompilerSet;
import org.netbeans.modules.cnd.api.compilers.PlatformTypes;
import org.netbeans.modules.cnd.api.compilers.Tool;
import org.netbeans.modules.cnd.api.execution.ExecutionListener;
import org.netbeans.modules.cnd.api.execution.NativeExecutor;
import org.netbeans.modules.cnd.api.remote.HostInfoProvider;
import org.netbeans.modules.cnd.api.remote.ServerList;
import org.netbeans.modules.cnd.api.utils.IpeUtils;
import org.netbeans.modules.cnd.api.utils.PlatformInfo;
import org.netbeans.modules.cnd.makeproject.api.configurations.MakeConfiguration;
import org.netbeans.modules.cnd.makeproject.api.remote.FilePathAdaptor;
import org.netbeans.modules.cnd.makeproject.api.runprofiles.RunProfile;
import org.netbeans.modules.nativeexecution.api.ExecutionEnvironment;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.execution.ExecutorTask;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.openide.util.Utilities;
import org.openide.windows.InputOutput;

public class DefaultProjectActionHandler implements ProjectActionHandler, ExecutionListener {

    private ProjectActionEvent pae;
    private volatile ExecutorTask executorTask;
    private NativeExecutor executor;
    private final List<ExecutionListener> listeners = new CopyOnWriteArrayList<ExecutionListener>();

    // VK: this is just to tie two pieces of logic together:
    // first is in determining the type of console for remote;
    // second is in canCancel
    private static final boolean RUN_REMOTE_IN_OUTPUT_WINDOW = true;

    public void init(ProjectActionEvent pae, ProjectActionEvent[] paes) {
        this.pae = pae;
    }

    /**
     * Will be called to get arguments for the action. Can be overridden.
     */
    public String getArguments() {
        return pae.getProfile().getArgsFlat();
    }

    /**
     * Will be called to get the environment for the action. Can be overridden.
     */
    public String[] getEnvironment() {
        return pae.getProfile().getEnvironment().getenv();
    }

    public void execute(InputOutput io) {
        String rcfile = null;
        if (pae.getType() == ProjectActionEvent.Type.RUN ||
                pae.getType() == ProjectActionEvent.Type.BUILD ||
                pae.getType() == ProjectActionEvent.Type.CLEAN) {
            String exe = pae.getExecutable();
            String args = getArguments();
            String[] env = getEnvironment();
            boolean showInput = pae.getType() == ProjectActionEvent.Type.RUN;
            MakeConfiguration conf = pae.getConfiguration();
            ExecutionEnvironment execEnv = conf.getDevelopmentHost().getExecutionEnvironment();

            String runDirectory = pae.getProfile().getRunDirectory();

            PlatformInfo pi = PlatformInfo.getDefault(conf.getDevelopmentHost().getExecutionEnvironment());

            boolean unbuffer = false;
            if (pae.getType() == ProjectActionEvent.Type.RUN) {
                int conType = pae.getProfile().getConsoleType().getValue();
                if (pae.getProfile().getTerminalType() == null || pae.getProfile().getTerminalPath() == null) {
                    String errmsg;
                    if (Utilities.isMac()) {
                        errmsg = getString("Err_NoTermFoundMacOSX");
                    } else {
                        errmsg = getString("Err_NoTermFound");
                    }
                    DialogDisplayer.getDefault().notify(new NotifyDescriptor.Message(errmsg));
                    conType = RunProfile.CONSOLE_TYPE_OUTPUT_WINDOW;
                }
                if (!conf.getDevelopmentHost().isLocalhost()) {
                    if (RUN_REMOTE_IN_OUTPUT_WINDOW) {
                        //TODO: only output window for remote for now
                        conType = RunProfile.CONSOLE_TYPE_OUTPUT_WINDOW;
                    }
                }
                if (conType == RunProfile.CONSOLE_TYPE_OUTPUT_WINDOW) {
                    if (HostInfoProvider.getPlatform(execEnv) == PlatformTypes.PLATFORM_WINDOWS) {
                        // we need to run the application under cmd on windows
                        exe = "cmd.exe"; // NOI18N
                        // exe path naturalization is needed for cmd on windows, see issue 149404
                        args = "/c " + IpeUtils.quoteIfNecessary(FilePathAdaptor.naturalize(pae.getExecutable())) // NOI18N
                                + " " + getArguments(); // NOI18N
                    } else if (conf.getDevelopmentHost().isLocalhost()) {
                        exe = IpeUtils.toAbsolutePath(pae.getProfile().getRunDir(), pae.getExecutable());
                    }
                    unbuffer = true;
                } else {
                    showInput = false;
                    if (conType == RunProfile.CONSOLE_TYPE_DEFAULT) {
                        conType = RunProfile.getDefaultConsoleType();
                    }
                    if (conType == RunProfile.CONSOLE_TYPE_EXTERNAL) {
                        try {
                            rcfile = File.createTempFile("nbcnd_rc", "").getAbsolutePath(); // NOI18N
                        } catch (IOException ex) {
                        }
                        String args2;
                        if (pae.getProfile().getTerminalPath().indexOf("gnome-terminal") != -1) { // NOI18N
                                /* gnome-terminal has differnt quoting rules... */
                            StringBuilder b = new StringBuilder();
                            for (int i = 0; i < args.length(); i++) {
                                if (args.charAt(i) == '"') {
                                    b.append("\\\""); // NOI18N
                                } else {
                                    b.append(args.charAt(i));
                                }
                            }
                            args2 = b.toString();
                        } else {
                            args2 = "";
                        }
                        if (pae.getType() == ProjectActionEvent.Type.RUN &&
                                pae.getConfiguration().isApplicationConfiguration() &&
                                HostInfoProvider.getPlatform(execEnv) == PlatformTypes.PLATFORM_WINDOWS &&
                                !exe.endsWith(".exe")) { // NOI18N
                            exe = exe + ".exe"; // NOI18N
                        }
                        // fixing #178201 Run fails if 'Show profiling indicators' is off and the project is created in folder with space
                        exe = IpeUtils.quoteIfNecessary(exe);
                        StringBuilder b = new StringBuilder();
                        for (int i = 0; i < exe.length(); i++) {
                            if (exe.charAt(i) == '"') {
                                b.append("\\\""); // NOI18N
                            } else {
                                b.append(exe.charAt(i));
                            }
                        }
                        String exe2 = b.toString();

                        args = MessageFormat.format(pae.getProfile().getTerminalOptions(), rcfile, exe, args, args2, exe2);
                        exe = pae.getProfile().getTerminalPath();
                    }
                }
                // Append compilerset base to run path. (IZ 120836)
                ArrayList<String> env1 = new ArrayList<String>();
                CompilerSet cs = conf.getCompilerSet().getCompilerSet();
                if (cs != null) {
                    String csdirs = cs.getDirectory();
                    String commands = cs.getCompilerFlavor().getCommandFolder(conf.getDevelopmentHost().getBuildPlatform());
                    if (commands != null && commands.length() > 0) {
                        // Also add msys to path. Thet's where sh, mkdir, ... are.
                        csdirs = csdirs + pi.pathSeparator() + commands;
                    }
                    boolean gotpath = false;
                    String pathname = pi.getPathName() + '=';
                    int i;
                    for (i = 0; i < env.length; i++) {
                        if (env[i].startsWith(pathname)) {
                            env1.add(env[i] + pi.pathSeparator() + csdirs); // NOI18N
                            gotpath = true;
                        } else {
                            env1.add(env[i]);
                        }
                    }
                    if (!gotpath) {
                        env1.add(pathname + pi.getPathAsString() + pi.pathSeparator() + csdirs);
                    }
                    env = env1.toArray(new String[env1.size()]);
                }
            } else { // Build or Clean
                String[] env1 = new String[env.length + 1];
                String csdirs = conf.getCompilerSet().getCompilerSet().getDirectory();
                String commands = conf.getCompilerSet().getCompilerSet().getCompilerFlavor().getCommandFolder(conf.getDevelopmentHost().getBuildPlatform());
                    if (commands != null && commands.length()>0) {
                    // Also add msys to path. Thet's where sh, mkdir, ... are.
                    csdirs = csdirs + pi.pathSeparator() + commands;
                }
                boolean gotpath = false;
                String pathname = pi.getPathName() + '=';
                int i;
                for (i = 0; i < env.length; i++) {
                    if (env[i].startsWith(pathname)) {
                        env1[i] = pathname + csdirs + pi.pathSeparator() + env[i].substring(5); // NOI18N
                        gotpath = true;
                    } else {
                        env1[i] = env[i];
                    }
                }
                if (!gotpath) {
                    String defaultPath = conf.getPlatformInfo().getPathAsString();
                    env1[i] = pathname + csdirs + pi.pathSeparator() + defaultPath;
                }
                env = env1;
                // Pass QMAKE from compiler set to the Makefile (IZ 174731)
                if (conf.isQmakeConfiguration()) {
                    String qmakePath = conf.getCompilerSet().getCompilerSet().getTool(Tool.QMakeTool).getPath();
                    qmakePath = conf.getCompilerSet().getCompilerSet().normalizeDriveLetter(qmakePath.replace('\\', '/')); // NOI18N
                    args += " QMAKE=" + IpeUtils.escapeOddCharacters(qmakePath); // NOI18N
                }
            }
            executor = new NativeExecutor(
                    execEnv,
                    runDirectory,
                    exe, args, env,
                    pae.getTabName(),
                    pae.getActionName(),
                    pae.getType() == ProjectActionEvent.Type.BUILD,
                    showInput,
                    unbuffer);
            //if (pae.getType() == ProjectActionEvent.Type.RUN)
            switch (pae.getType()) {
                case DEBUG:
                case RUN:
                    if (ServerList.get(execEnv).getX11Forwarding() && !contains(env, "DISPLAY")) { //NOI18N if DISPLAY is set, let it do its work
                        executor.setX11Forwarding(true);
                    }
            }
            executor.addExecutionListener(this);
            if (rcfile != null) {
                executor.setExitValueOverride(rcfile);
            }
            try {
                executorTask = executor.execute(io);
            } catch (java.io.IOException ioe) {
                ioe.printStackTrace();
            }
        } else {
            assert false;
        }
    }

    private boolean contains(String[] env, String var) {
        for (String v : env) {
            int pos = v.indexOf('='); //NOI18N
            if (pos > 0) {
                if (v.substring(0, pos).equals(var)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void addExecutionListener(ExecutionListener l) {
        if (!listeners.contains(l)) {
            listeners.add(l);
        }
    }

    public void removeExecutionListener(ExecutionListener l) {
        listeners.remove(l);
    }

    public boolean canCancel() {
        return true;
    }

    public void cancel() {
        RequestProcessor.getDefault().post(new Runnable() {
            public void run() {
                ExecutorTask et = executorTask;
                if (et != null) {
                    executorTask.stop();
                }
                NativeExecutor ne = executor;
                if (ne != null) {
                    ne.stop(); // "kontrolny" ;)
                }
            }
        });
    }

    public void executionStarted(int pid) {
        for (ExecutionListener l : listeners) {
            l.executionStarted(pid);
        }
    }

    public void executionFinished(int rc) {
        for (ExecutionListener l : listeners) {
            l.executionFinished(rc);
        }
    }

    /** Look up i18n strings here */
    private static String getString(String s) {
        return NbBundle.getBundle(DefaultProjectActionHandler.class).getString(s);
    }

}
