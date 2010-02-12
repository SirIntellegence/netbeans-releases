/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2010 Sun Microsystems, Inc. All rights reserved.
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
package org.netbeans.modules.nativeexecution;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import org.netbeans.modules.nativeexecution.api.ExecutionEnvironment;
import org.netbeans.modules.nativeexecution.api.pty.PtySupport;
import org.netbeans.modules.nativeexecution.api.pty.PtySupport.Pty;
import org.netbeans.modules.nativeexecution.pty.PtyProcessStartUtility;
import org.openide.util.Exceptions;

/**
 *
 * @author ak119685
 */
public final class PtyNativeProcess extends AbstractNativeProcess {

    private final Pty pty;
    private AbstractNativeProcess delegate = null;

    public PtyNativeProcess(NativeProcessInfo info) {
        super(info);

        ExecutionEnvironment env = info.getExecutionEnvironment();
        Pty _pty = info.getPty();

        if (_pty == null) {
            try {
                _pty = PtySupport.allocate(env);
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }

        pty = _pty;
    }

    public Pty getPty() {
        return pty;
    }

    @Override
    protected void create() throws Throwable {
        ExecutionEnvironment env = info.getExecutionEnvironment();


        String executable = PtyProcessStartUtility.getInstance().getPath(env);

        List<String> newArgs = new ArrayList<String>();
        newArgs.add("-pty"); // NOI18N
        newArgs.add(pty.getSlaveName());
        newArgs.add(info.getExecutable());
        newArgs.addAll(info.getArguments());

        // TODO: Clone Info!!!!
        info.setExecutable(executable);
        info.setArguments(newArgs.toArray(new String[0]));

        if (env.isLocal()) {
            delegate = new LocalNativeProcess(info);
        } else {
            delegate = new RemoteNativeProcess(info);
        }

        delegate.createAndStart();


        ByteArrayInputStream bis = new ByteArrayInputStream((Integer.toString(delegate.getPID())).getBytes());
        readPID(bis);
    }

    @Override
    protected void cancel() {
        delegate.destroy();
    }

    @Override
    protected int waitResult() throws InterruptedException {
        if (delegate == null) {
            return -1;
        }

        int result = -1;
        
        while (true) {
            // This sleep is to avoid lost interrupted exception...
            try {
                Thread.sleep(200);
                // 200 - to make this check not so often...
                // actually, to avoid the problem, 1 is OK.
            } catch (InterruptedException ex) {
                throw ex;
            }

            try {
                result = delegate.exitValue();
            } catch (IllegalThreadStateException ex) {
                continue;
            }

            break;
        }

        if (getState() == State.CANCELLED) {
            throw new InterruptedException();
        }

        return result;
    }

    @Override
    public OutputStream getOutputStream() {
        return delegate.getOutputStream();
    }

    @Override
    public InputStream getInputStream() {
        return delegate.getInputStream();
    }

    @Override
    public InputStream getErrorStream() {
        return new InputStream() {

            @Override
            public int read() throws IOException {
                return -1;
            }
        };
    }
}
