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
 * Portions Copyrighted 2007 Sun Microsystems, Inc.
 */

package org.openide.loaders;

import java.awt.Dialog;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import javax.swing.JEditorPane;
import javax.swing.SwingUtilities;
import org.netbeans.api.actions.Closable;
import org.netbeans.api.actions.Openable;
import org.netbeans.junit.Log;
import org.netbeans.junit.MockServices;
import org.netbeans.junit.NbTestCase;
import org.netbeans.junit.RandomlyFails;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.cookies.EditorCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileSystem;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;
import org.openide.util.Mutex;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Jaroslav Tulach <jtulach@netbeans.org>
 */
@RandomlyFails // sometimes blocks forever in waitQuery in NB-Core-Build
public class DefaultDataObjectMissingBinaryTest extends NbTestCase {
    static {
        System.setProperty("org.openide.windows.DummyWindowManager.VISIBLE", "false");
    }

    private FileSystem lfs;
    private DataObject obj;
    
    public DefaultDataObjectMissingBinaryTest(String testName) {
        super(testName);
    }

    @Override
    protected Level logLevel() {
        return Level.FINE;
    }

    @Override
    protected void setUp() throws Exception {
        MockServices.setServices(FirstDD.class);
        
        clearWorkDir();

        String fsstruct [] = new String [] {
            "AA/a.test"
        };
        

        lfs = TestUtilHid.createLocalFileSystem(getWorkDir(), fsstruct);

        FileObject fo = lfs.findResource("AA/a.test");
        assertNotNull("file not found", fo);
        obj = DataObject.find(fo);

        assertEquals("The right class", obj.getClass(), DefaultDataObject.class);

        assertFalse("Designed to run outside of AWT", SwingUtilities.isEventDispatchThread());
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testOpenEditorOnTextBinaryAndMeanwhileDelete() throws Exception {
        writeContent("Ahoj\n");
        
        final EditorCookie ec = obj.getLookup().lookup(EditorCookie.class);
        Openable open = obj.getLookup().lookup(Openable.class);
        open.open();
        
        Closable close = obj.getLookup().lookup(Closable.class);
        close.close();
        
        writeLongContent(3 * 1000 * 1000);

        CharSequence log = Log.enable("org.openide.util", Level.SEVERE);
        
        final Openable open2 = obj.getLookup().lookup(Openable.class);
        Mutex.EVENT.readAccess(new Runnable() {
            @Override
            public void run() {
                open2.open();
            }
        });
        
        FirstDD dd = (FirstDD)FirstDD.getDefault();
        NotifyDescriptor query = dd.waitQuery();
        
        File realFile = FileUtil.toFile(obj.getPrimaryFile());
        assertNotNull("File exists", realFile);
        assertTrue("Delete is OK", realFile.delete());
        
        RequestProcessor.getDefault().post(new Runnable() {
            @Override
            public void run() {
                obj.getPrimaryFile().refresh();
            }
        }).waitFinished(3000);
        
        assertFalse("DataObject becomes invalid", obj.isValid());
        
        assertEquals("There are two options", 2, query.getOptions().length);
        assertEquals("OK is the first option", NotifyDescriptor.OK_OPTION, query.getOptions()[0]);
        dd.provideAnswer(query.getOptions()[0]);
        
        JEditorPane[] arr = Mutex.EVENT.readAccess(new Mutex.Action<JEditorPane[]>() {
            @Override
            public JEditorPane[] run() {
                return ec.getOpenedPanes();
            }
        });
        
        assertNull("No pane opened", arr);
        if (log.length() > 0) {
            fail("No warnings, but was:\n" + log);
        }
    }

    private void writeContent(String cnt) throws IOException {
        OutputStream os = obj.getPrimaryFile().getOutputStream();
        os.write(cnt.getBytes());
        os.close();
    }

    private void writeLongContent(long size) throws IOException {
        OutputStream os = obj.getPrimaryFile().getOutputStream();
        for (long i = 0; i < size; i++) {
            os.write((byte) (i % 128));
        }
        os.close();
    }


    public static final class FirstDD extends DialogDisplayer {
        NotifyDescriptor query;
        Object answer;
        
        synchronized NotifyDescriptor waitQuery() throws InterruptedException {
            while (query == null) {
                wait();
            }
            return query;
        }
        
        synchronized void provideAnswer(Object obj) {
            answer = obj;
            notifyAll();
        }

        @Override
        public synchronized Object notify(NotifyDescriptor descriptor) {
            query = descriptor;
            notifyAll();
            while (answer == null) {
                try {
                    wait();
                } catch (InterruptedException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
            return answer;
        }

        @Override
        public Dialog createDialog(DialogDescriptor descriptor) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

    }
}
