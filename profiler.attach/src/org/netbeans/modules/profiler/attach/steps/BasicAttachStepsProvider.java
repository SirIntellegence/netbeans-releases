/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2012 Oracle and/or its affiliates. All rights reserved.
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
package org.netbeans.modules.profiler.attach.steps;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.JFileChooser;
import org.netbeans.lib.profiler.common.AttachSettings;
import org.netbeans.lib.profiler.common.integration.IntegrationUtils;
import org.netbeans.modules.profiler.api.ProfilerDialogs;
import org.netbeans.modules.profiler.attach.providers.RemotePackExporter;
import org.netbeans.modules.profiler.attach.spi.AttachStepsProvider;
import org.openide.util.RequestProcessor;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
@ServiceProvider(service = AttachStepsProvider.class)
public class BasicAttachStepsProvider extends AttachStepsProvider {
    
    protected static final String LINK_CLIPBOARD = "file:/clipboard"; // NOI18N
    protected static final String LINK_REMOTEPACK = "file:/remotepack"; // NOI18N
    
    
    public String getSteps(AttachSettings settings) {
        if (settings.isRemote()) return remoteDirectSteps(settings);
        else if (settings.isDirect()) return localDirectSteps(settings);
        else return localDynamicSteps(settings);
    }
    
    public void handleAction(String action, AttachSettings settings) {
        if (LINK_CLIPBOARD.equals(action)) copyParameters(settings);
        else if (LINK_REMOTEPACK.equals(action)) createRemotePack(settings);
    }
    
    
    protected String localDynamicSteps(AttachSettings settings) {
        StringBuilder b = new StringBuilder();
        b.append("<div>");
        b.append("<b>Step 1: </b>");
        b.append("Make sure the target application has been started by user ");
        b.append(System.getProperty("user.name"));
        b.append(" and is running using Java 6+.");
        b.append("</div>");
        b.append("<br/>");
        b.append("<div>");
        b.append("<b>Step 2: </b>");
        b.append("Submit this dialog and click the Attach button to select the target application process.");
        b.append("</div>");
        return b.toString();
    }
    
    protected String localDirectSteps(AttachSettings settings) {
        StringBuilder b = new StringBuilder();
        b.append("<div>");
        b.append("<b>Step 1: </b>");
        b.append("Make sure the target application is configured to run using Java 6+. Click the Help button for information on how to profile Java 5 applications.");
        b.append("</div>");
        b.append("<br/>");
        b.append("<div>");
        b.append("<b>Step 2: </b>");
        b.append("Add the following parameter(s) to the application startup script (<a href='");
        b.append(LINK_CLIPBOARD);
        b.append("'>copy to clipboard</a>):");
        b.append("</div>");
        b.append("<pre>");
        b.append(parameters(settings));
        b.append("</pre>");
        b.append("<br/>");
        b.append("<div>");
        b.append("<b>Step 3: </b>");
        b.append("Start the target application. The process will wait for the profiler to connect.");
        b.append("</div>");
        b.append("<br/>");
        b.append("<div>");
        b.append("<b>Step 4: </b>");
        b.append("Submit this dialog and click the Attach button to connect to the target application and resume its execution.");
        b.append("</div>");
        return b.toString();
    }
    
    protected String remoteDirectSteps(AttachSettings settings) {
        StringBuilder b = new StringBuilder();
        b.append("<div>");
        b.append("<b>Step 1: </b>");
        b.append("If you have not done it before <a href='");
        b.append(LINK_REMOTEPACK);
        b.append("'>create a Remote profiling pack</a> for the selected OS & JVM and upload it to the remote system. Remote profiling pack root directory will be referred to as <code>&lt;remote&gt;</code>.");
        b.append("</div>");
        b.append("<br/>");
        b.append("<div>");
        b.append("<b>Step 2: </b>");
        b.append("If you have not run profiling on the remote system yet, run the <code>");
        b.append(IntegrationUtils.getRemoteCalibrateCommandString(settings.getHostOS(), IntegrationUtils.PLATFORM_JAVA_60));
        b.append("</code> script first to calibrate the profiler.");
        b.append("</div>");
        b.append("<br/>");
        b.append("<div>");
        b.append("<b>Step 3: </b>");
        b.append("Make sure the target application is configured to run using Java 6+. Click the Help button for information on how to profile Java 5 applications.");
        b.append("</div>");
        b.append("<br/>");
        b.append("<div>");
        b.append("<b>Step 4: </b>");
        b.append("Add the following parameter(s) to the application startup script (<a href='");
        b.append(LINK_CLIPBOARD);
        b.append("'>copy to clipboard</a>):");
        b.append("</div>");
        b.append("<pre>");
        b.append(parameters(settings));
        b.append("</pre>");
        b.append("<br/>");
        b.append("<div>");
        b.append("<b>Step 5: </b>");
        b.append("Start the target application. The process will wait for the profiler to connect.");
        b.append("</div>");
        b.append("<br/>");
        b.append("<div>");
        b.append("<b>Step 6: </b>");
        b.append("Submit this dialog and click the Attach button to connect to the target application and resume its execution.");
        b.append("</div>");
        return b.toString();
    }
    
    protected String parameters(AttachSettings settings) {
        return IntegrationUtils.getProfilerAgentCommandLineArgs(settings.getHostOS(),
                IntegrationUtils.PLATFORM_JAVA_60, settings.isRemote(), settings.getPort());
    }
    
    protected void copyParameters(AttachSettings settings) {
        String parameters = parameters(settings);
        parameters = parameters.replace("&lt;", "<").replace("&gt;", ">");
        StringSelection s = new StringSelection(parameters);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(s, s);
        ProfilerDialogs.displayInfo("Profiler parameter(s) copied to clipboard");
    }
    
    protected void createRemotePack(final AttachSettings settings) {
        try {
            final JFileChooser chooser = new JFileChooser();
            final File tmpDir = new File(System.getProperty("java.io.tmpdir")); // NOI18N
            chooser.setDialogTitle("Choose Target Folder");
            chooser.setAcceptAllFileFilterUsed(false);
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setSelectedFile(tmpDir);
            chooser.setCurrentDirectory(tmpDir);
            chooser.setMultiSelectionEnabled(false);
            if ((JFileChooser.CANCEL_OPTION & chooser.showSaveDialog(chooser)) == 0) {
                String packPath = exportRemotePack(chooser.getSelectedFile().getAbsolutePath(), settings);
                ProfilerDialogs.displayInfo("Remote profiling pack saved to " + packPath);
            }
        } catch (IOException ex) {
            System.err.println(">>> Exception creating remote pack: " + ex); // NOI18N
        }
    }
    
    private static final AtomicBoolean exportRunning = new AtomicBoolean(false);
    private static String exportRemotePack(String path, AttachSettings settings) throws IOException {
        if (exportRunning.compareAndSet(false, true)) {
            try {
                return RemotePackExporter.getInstance().export(
                        path, settings.getHostOS(), IntegrationUtils.PLATFORM_JAVA_60);
            } finally {
                exportRunning.compareAndSet(true, false);
            }
        } else {
            throw new IOException();
        }
    }
    
}
