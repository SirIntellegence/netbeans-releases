/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2008 Sun Microsystems, Inc. All rights reserved.
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
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2008 Sun
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
package org.netbeans.modules.subversion.client.cli;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.*;
import java.util.ArrayList;
import org.netbeans.modules.subversion.client.cli.Parser.Line;
import org.netbeans.modules.subversion.config.SvnConfigFiles;
import org.tigris.subversion.svnclientadapter.ISVNNotifyListener;
import org.tigris.subversion.svnclientadapter.SVNBaseDir;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNUrl;

/**
 * Encapsulates a command given to a svn client. 
 * 
 * @author Maros Sandor
 */
public abstract class SvnCommand implements CommandNotificationListener {
        
    private ISVNNotifyListener listener;
        
    private final List<String> cmdError = new ArrayList<String>(10);

    private File commandWorkingDirectory;
        
    private String stringValue;
    
    /**
     * If the command throws an execption, this is it.
     */
    private Exception thrownException;

    /**
     * True if the command produced errors (messages in error stream), false otherwise.
     */
    private boolean hasFailed;

    /**
     * Internal check mechanism to prevent commands reuse.
     */
    private boolean commandExecuted;
    private Arguments arguments;

    protected SvnCommand() {
        arguments = new Arguments();        
    }

    public void setListener(ISVNNotifyListener listener) {
        this.listener = listener;
    }    
    
    void prepareCommand() throws IOException {
        prepareCommand(arguments);
    }
    
    /**
     * Prepare the command: fill list of arguments to cleartool and compute commandWorkingDirectory.
     * 
     * @param arguments 
     * @throws ClearcaseException
     */
    public abstract void prepareCommand(Arguments arguments) throws IOException;

    public void setCommandWorkingDirectory(File... files) {
        commandWorkingDirectory = SVNBaseDir.getBaseDir(files);
        if(commandWorkingDirectory == null) {
            commandWorkingDirectory = new File(".");
        }        
    }
    
    public void commandStarted() {
        assert !commandExecuted : "Command re-use is not supported";
        commandExecuted = true;
    }

    public void outputText(String lineString) {
        Line line = Parser.getInstance().parse(lineString);
        if(line != null) {
            if(listener != null && line.getPath() != null) {
                File f = getAbsoluteFile(line.getPath());
                listener.onNotify(f, null);
            }
            notify(line);
        }
    }

    public void errorText(String line) {
        cmdError.add(line);
        if (isErrorMessage(line)) hasFailed = true;
    }

    public void commandFinished() {
    }
    
    public boolean hasFailed() {
        return hasFailed;
    }

    public List<String> getCmdError() {
        return cmdError;
    }

    public void setException(Exception e) {
        thrownException = e;
    }

    public Exception getThrownException() {
        return thrownException;
    }

    protected void notify(Line line) {
        
    }
    
    /**
     * Tests if the given message printed to the error stream indicates an actual command error.
     * Commands sometimes print diagnostic messages to error stream which are not errors and should not be reported as such. 
     * 
     * @param s a message printed to the output stream
     * @return true if the message is an error that should be reported, false otherwise
     */
    protected boolean isErrorMessage(String s) {
        return true;
    }   
            
    public String getStringCommand() throws IOException {
        if(stringValue == null) {
            // XXX add user, psswd
            stringValue = toString(arguments).toString();
        }
        return stringValue;
    }

    String[] getCliArguments(String executable) {
        List<String> l = new ArrayList<String>(arguments.size() + 1);
        l.add(executable);
        for (String arg : arguments.toArray()) {
            l.add(arg);
        }
        return l.toArray(new String[l.size()]);
    }        
    
    private static StringBuilder toString(Arguments args) {
        StringBuilder cmd = new StringBuilder(100);
        for (String arg : args) {
            cmd.append(arg);
            cmd.append(' ');
        }
        cmd.delete(cmd.length() - 1, cmd.length());
        return cmd;
    }        
        
    protected static String createTempCommandFile(String value) throws IOException {
        return createTempCommandFile(new String[] {value});
    }   
    
    protected static String createTempCommandFile(File[] files) throws IOException {
        String[] lines = new String[files.length];
        for (int i = 0; i < files.length; i++) {
            lines[i] = files[i].getAbsolutePath();            
        }
        return createTempCommandFile(lines);
    }
    
    protected static String createTempCommandFile(String[] lines) throws IOException {
        String tmp = System.getProperty("java.io.tmpdir");
        File targetFile;
        targetFile = File.createTempFile("svn_", "", new File(tmp));
        targetFile.deleteOnExit();

        PrintWriter writer = null; 
        try {
            writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(targetFile))); // NOI18N           
            for (int i = 0; i < lines.length; i++) {
                writer.print(i < lines.length -1 ? lines[i] + "\n" : lines[i]);
            }
        } finally {
            if(writer!=null) {
                writer.flush();
                writer.close();    
            }
        }
        return targetFile.getAbsolutePath();
    }

    File getAbsoluteFile(String path) {
        File file = new File(path);
        if(file.isAbsolute()) {
            return file;
        } else {
            return new File(commandWorkingDirectory, path);   
        }        
    }

    public final class Arguments implements Iterable<String> {

        private final List<String> args = new ArrayList<String>(5);

        public Arguments() {
            addConfigDir();
        }
        
        public void add(String argument) {
            if (argument.indexOf(' ') == -1) {
                args.add(argument);
            } else {
                args.add("'" + argument + "'");
            }
        }

        public void add(File argument) {
            add(argument.getAbsolutePath());
        }
        
        public void add(SVNUrl url) {
            if(url != null) {
                add(url.toString());   
            }            
        }

        public void add(SVNRevision rev1, SVNRevision rev2) {
            add("-r");   
            add( (rev1 == null || rev1.toString().trim().equals("") ? "HEAD" : rev1.toString() ) + 
                 ":" +
                 (rev2 == null || rev2.toString().trim().equals("") ? "HEAD" : rev2.toString() ) ); 
        }
        
        public void add(SVNUrl url, SVNRevision pegging) {
            if(url != null) {
                add(url.toString() + "@" + (pegging == null ? "HEAD" : pegging));   
            }            
        }
        
        public void add(SVNRevision revision) {
            add("-r");   
            add(revision == null || revision.toString().trim().equals("") ? "HEAD" : revision.toString());
        }                    

        public void addPathArguments(String... paths) throws IOException {        
            add("--targets");
            add(createTempCommandFile(paths));
        }
        
        public void addFileArguments(File... files) throws IOException {        
            add("--targets");
            add(createTempCommandFile(files));
        }

        public void addUrlArguments(SVNUrl... urls) throws IOException {        
            String[] paths = new String[urls.length];
            for (int i = 0; i < urls.length; i++) {
                paths[i] = urls[i].toString();                
            }
            add("--targets");
            add(createTempCommandFile(paths));
        }
        
        public void addMessage(String message) throws IOException {
            if(message == null) {
                return;
            }
            add("--force-log");
            add("-F");                
            String msgFile = createTempCommandFile((message != null) ? message : "");
            add(msgFile);                               		
        }

        private void addConfigDir() {
            String configDir = SvnConfigFiles.getNBConfigPath();
            if (configDir != null) {
                add("--config-dir");
                add(configDir);
            }
        }         
    
        public Iterator<String> iterator() {
            return args.iterator();
        }
        
        String[] toArray() {
            return args.toArray(new String[args.size()]);
        }
        
        int size() {
            return args.size();
        }
    }       
                    
}
