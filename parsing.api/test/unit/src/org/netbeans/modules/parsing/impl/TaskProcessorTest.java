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
 * Portions Copyrighted 2008 Sun Microsystems, Inc.
 */

package org.netbeans.modules.parsing.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeListener;
import javax.swing.text.StyledDocument;
import org.netbeans.api.editor.mimelookup.MimePath;
import org.netbeans.api.editor.mimelookup.test.MockMimeLookup;
import org.netbeans.junit.NbTestCase;
import org.netbeans.modules.editor.plain.PlainKit;
import org.netbeans.modules.masterfs.providers.ProvidedExtensions;
import org.netbeans.modules.parsing.api.ParserManager;
import org.netbeans.modules.parsing.api.ResultIterator;
import org.netbeans.modules.parsing.api.Snapshot;
import org.netbeans.modules.parsing.api.Source;
import org.netbeans.modules.parsing.api.Task;
import org.netbeans.modules.parsing.api.UserTask;
import org.netbeans.modules.parsing.impl.indexing.Util;
import org.netbeans.modules.parsing.spi.ParseException;
import org.netbeans.modules.parsing.spi.Parser;
import org.netbeans.modules.parsing.spi.Parser.Result;
import org.netbeans.modules.parsing.spi.ParserFactory;
import org.netbeans.modules.parsing.spi.ParserResultTask;
import org.netbeans.modules.parsing.spi.Scheduler;
import org.netbeans.modules.parsing.spi.SchedulerEvent;
import org.netbeans.modules.parsing.spi.SchedulerTask;
import org.netbeans.modules.parsing.spi.SourceModificationEvent;
import org.openide.cookies.EditorCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.text.NbDocument;
import org.openide.util.Exceptions;

/**
 *
 * @author Tomas Zezula
 */
public class TaskProcessorTest extends NbTestCase {
    
    public TaskProcessorTest(String testName) {
        super(testName);
    }            
    
    public void testWarningWhenRunUserTaskCalledFromAWT() throws Exception {
        this.clearWorkDir();
        final File _wd = this.getWorkDir();
        final FileObject wd = FileUtil.toFileObject(_wd);

        FileUtil.setMIMEType("foo", "text/foo");
        final FileObject foo = wd.createData("file.foo");
        final LogRecord[] warning = new LogRecord[1];
        final String msgTemplate = "ParserManager.parse called in AWT event thread by: {0}";  //NOI18N

        MockMimeLookup.setInstances(MimePath.parse("text/foo"), new FooParserFactory());
        Logger.getLogger(TaskProcessor.class.getName()).addHandler(new Handler() {
            public @Override void publish(LogRecord record) {
                if (record.getMessage().startsWith(msgTemplate)) {
                    warning[0] = record;
                }
            }

            public @Override void flush() {
            }

            public @Override void close() throws SecurityException {
            }
        });

        final StackTraceUserTask stackTraceUserTask = new StackTraceUserTask();
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                try {
                    ParserManager.parse(Collections.singleton(Source.create(foo)), stackTraceUserTask);
                } catch (ParseException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        });

        assertNotNull("No warning when calling ParserManager.parse from AWT", warning[0]);
        assertEquals("Wrong message", msgTemplate, warning[0].getMessage());
        assertEquals("Suspiciosly wrong warning message (is the caller identified correctly?)", stackTraceUserTask.caller, warning[0].getParameters()[0]);
    }
    
    public void testDeadlock() throws Exception {
        FileUtil.setMIMEType("foo", "text/foo");
        MockMimeLookup.setInstances(MimePath.parse("text/foo"), new FooParserFactory(), new PlainKit());
        MockMimeLookup.setInstances(MimePath.parse("text/plain"), new FooParserFactory(), new PlainKit());
        final File workingDir = getWorkDir();        
        final FileObject file = FileUtil.createData(new File(workingDir,"test.foo"));
        final Source src = Source.create(file);
        final DataObject dobj = DataObject.find(file);
        final EditorCookie ec = dobj.getLookup().lookup(EditorCookie.class);
        final StyledDocument doc = ec.openDocument();
        final CountDownLatch start_a = new CountDownLatch(1);
        final CountDownLatch start_b = new CountDownLatch(1);
        final CountDownLatch end = new CountDownLatch(1);
        final CountDownLatch taskEnded = new CountDownLatch(1);
        final Collection<SchedulerTask> tasks = Collections.<SchedulerTask>singleton(
            new ParserResultTask<Parser.Result>() {
                @Override
                public void run(Result result, SchedulerEvent event) {
                    taskEnded.countDown();
                }

                @Override
                public int getPriority() {
                    return 1000;
                }

                @Override
                public Class<? extends Scheduler> getSchedulerClass() {
                    return null;
                }

                @Override
                public void cancel() {
                }                    
            });
        TaskProcessor.addPhaseCompletionTasks(
                tasks,
                SourceAccessor.getINSTANCE().getCache(src),
                true,
                null);
        taskEnded.await();
        final Thread t = new Thread () {
            @Override
            public void run() {
                NbDocument.runAtomic(doc, new Runnable() {
                    @Override
                    public void run() {
                        start_a.countDown();
                        try {
                            start_b.await();
                            synchronized(TaskProcessor.INTERNAL_LOCK) {
                                end.await();
                            }
                        } catch (InterruptedException ex) {
                            Exceptions.printStackTrace(ex);
                        }
                    }
                });                    
            }
        };
        t.start();        
        synchronized(TaskProcessor.INTERNAL_LOCK) {
            start_b.countDown();
            start_a.await();
            SourceAccessor.getINSTANCE().getCache(src).invalidate();
            TaskProcessor.removePhaseCompletionTasks(tasks, src);
        }
        end.countDown();
    }

    private static final class FooParserFactory extends ParserFactory {
        @Override
        public Parser createParser(Collection<Snapshot> snapshots) {
            return new FooParser();
        }
    }

    private static final class FooParser extends Parser {
        private FooParserResult result;

        public @Override void parse(Snapshot snapshot, Task task, SourceModificationEvent event) throws ParseException {
            result = new FooParserResult((snapshot));
        }

        public @Override Result getResult(Task task) throws ParseException {
            return result;
        }

        public @Override void cancel() {
        }

        public @Override void addChangeListener(ChangeListener changeListener) {
        }

        public @Override void removeChangeListener(ChangeListener changeListener) {
        }
    }

    private static final class FooParserResult extends Parser.Result {
        public FooParserResult(Snapshot snapshot) {
            super(snapshot);
        }

        protected @Override void invalidate() {
        }
    }

    private static final class StackTraceUserTask extends UserTask {
        public StackTraceElement caller;
        @Override
        public void run(ResultIterator resultIterator) throws Exception {
            ArrayList<StackTraceElement> filteredStackTrace = new ArrayList<StackTraceElement>();
            StackTraceElement [] stackTrace = Thread.currentThread().getStackTrace();
            boolean active = false;
            for(StackTraceElement e : stackTrace) {
                if (!active) {
                    if (e.getClassName().equals(TaskProcessor.class.getName()) && e.getMethodName().equals("runUserTask")) {
                        active = true;
                    } else {
                        continue;
                    }
                }
                filteredStackTrace.add(e);
            }
            caller = Util.findCaller(filteredStackTrace.toArray(new StackTraceElement[filteredStackTrace.size()]));
        }
    }
}
