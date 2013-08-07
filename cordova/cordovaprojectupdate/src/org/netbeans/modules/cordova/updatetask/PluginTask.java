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
package org.netbeans.modules.cordova.updatetask;

import java.io.IOException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.ExecTask;
import org.apache.tools.ant.types.Resource;

/**
 *
 * @author Jan Becicka
 */
public class PluginTask extends Task {

    public static boolean isWin() {
        boolean isWin = System.getProperty("os.name").toLowerCase().indexOf("win") >= 0;
        return isWin;
    }
    
    @Override
    public void execute() throws BuildException {
        try {
            if (!initRequested()) {
                return;
            }
            initCurrent();
            
            HashSet<CordovaPlugin> pluginsToInstall = (HashSet<CordovaPlugin>) requestedPlugins.clone();
            
            //plugins to install
            pluginsToInstall.removeAll(currentPlugins);
            
            //plugins to remove
            currentPlugins.removeAll(requestedPlugins);
            
            installPlugins(pluginsToInstall);
            uninstallPlugins(currentPlugins);
            
        } catch (IOException ex) {
            throw new BuildException(ex);
        }
    }
    
    private HashSet<CordovaPlugin> requestedPlugins;
    private HashSet<CordovaPlugin> currentPlugins;

    private boolean initRequested() throws IOException {
        requestedPlugins = new HashSet<CordovaPlugin>();
        Resource resource = getProject().getResource("nbproject/plugins.properties");
        if (resource == null || !resource.isExists()) {
            return false;
        }
        Properties props = new Properties();
        props.load(resource.getInputStream());
        for (String name:props.stringPropertyNames()) {
            requestedPlugins.add(new CordovaPlugin(name, props.getProperty(name)));
        }
        return true;
    }
    
    public static String getCordovaCommand() {
         boolean isWin = isWin();
         return isWin?"cordova.cmd":"cordova";
    }

    private void initCurrent() {
        currentPlugins = new HashSet<CordovaPlugin>();
        
        log(getCordovaCommand() + " plugins ");

        ExecTask exec = (ExecTask) getProject().createTask("exec");
        exec.setExecutable(getCordovaCommand());
        exec.createArg().setValue("plugins");
        exec.setOutputproperty("cordova.current.plugins");
        exec.execute();

        String plugins = getProject().getProperty("cordova.current.plugins");
        final int startPar = plugins.indexOf("[");
        if (startPar < 0) {
            //empty
            return;
        }
        plugins = plugins.substring(startPar+1, plugins.lastIndexOf("]")).trim();
        StringTokenizer tokenizer = new StringTokenizer(plugins, ",");
        while (tokenizer.hasMoreTokens()) {
            String name = tokenizer.nextToken().trim();
            currentPlugins.add(new CordovaPlugin(name.substring(name.indexOf("'")+1, name.lastIndexOf("'")), ""));
        }
    }

    private void installPlugins(Set<CordovaPlugin> pluginsToInstall) {
        for (CordovaPlugin plugin : pluginsToInstall) {
            log(getCordovaCommand() + " -d plugin add " + plugin.getUrl());
            ExecTask exec = (ExecTask) getProject().createTask("exec");
            exec.setExecutable(getCordovaCommand());
            exec.createArg().setValue("-d");
            exec.createArg().setValue("plugin");
            exec.createArg().setValue("add");
            exec.createArg().setValue(plugin.getUrl());
            exec.setFailonerror(true);
            exec.execute();
        }
    }

    private void uninstallPlugins(Set<CordovaPlugin> pluginsToUninstall) {
        for (CordovaPlugin plugin : pluginsToUninstall) {
            log(getCordovaCommand() + " -d plugin remove " + plugin.getId());
            ExecTask exec = (ExecTask) getProject().createTask("exec");
            exec.setExecutable(getCordovaCommand());
            exec.createArg().setValue("-d");
            exec.createArg().setValue("plugin");
            exec.createArg().setValue("remove");
            exec.createArg().setValue(plugin.getId());
            exec.execute();
        }
    }
    
    
    private class CordovaPlugin {
        private String id;
        private String url;

        public CordovaPlugin(String id, String url) {
            this.id = id;
            this.url = url;
        }

        public String getId() {
            return id;
        }

        public String getUrl() {
            return url;
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 29 * hash + (this.id != null ? this.id.hashCode() : 0);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final CordovaPlugin other = (CordovaPlugin) obj;
            if ((this.id == null) ? (other.id != null) : !this.id.equals(other.id)) {
                return false;
            }
            return true;
        }
        
        
    }
}
