/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved.
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
 * Portions Copyrighted 2008 Sun Microsystems, Inc.
 */

package org.netbeans.modules.dlight.dtrace.collector.support;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import org.netbeans.modules.dlight.api.storage.DataRow;
import org.netbeans.modules.dlight.api.storage.DataTableMetadata;
import org.netbeans.modules.dlight.api.storage.DataTableMetadata.Column;
import org.netbeans.modules.dlight.core.stack.storage.StackDataStorage;
import org.netbeans.modules.dlight.management.api.impl.DataStorageManager;
import org.netbeans.modules.dlight.spi.support.DataStorageTypeFactory;
import org.netbeans.modules.dlight.util.DLightLogger;

/**
 *
 * Parses the output of form
 * - data row - should NOT has leading space
 * - ustack - each row should have a leading space
 * - empty line - notifies that the stack is over
 *
 */

final class DtraceDataAndStackParser extends DtraceParser {

    private static final boolean TRACE = Boolean.getBoolean("dlight.dns.parser.trace");
    private static PrintStream traceStream;
    static {
        if (TRACE) {
            try {
                traceStream = new PrintStream("/tmp/dsp.log");
            } catch (FileNotFoundException ex) {
                ex.printStackTrace();
                traceStream = System.err;
            }
        }
    }

    private static enum State {
        WAITING_DATA,   // we are waiting for a data row
        WAITING_STACK,  // we are waiting for first row of ustack
        IN_STACK        // we are waiting for subsequent row of ustack
    }

    private State state;

    List<String> currData;
    int currCpu;
    int currThread;
    long currTimeStamp;
    long prevTimeStamp;
    private List<CharSequence> currStack = new ArrayList<CharSequence>(32);

    private List<String> colNames;
    private int colCount;
    private final boolean isProfiler;

    public DtraceDataAndStackParser(DataTableMetadata metadata) {
        super(metadata);
        state = State.WAITING_DATA;
        colNames = new ArrayList<String>(metadata.getColumnsCount());
        for (Column c : metadata.getColumns()) {
          colNames.add(c.getColumnName());
        }
        colCount = metadata.getColumnsCount();
        isProfiler = metadata.getName().equals("CallStack");
    }

    /** override of you need more smart data processing  */
    protected List<String> processDataLine(String line) {
        return super.parse(line, colCount-1);
    }

    @Override
    public DataRow process(String line) {
        if (TRACE) { traceStream.printf("%s\t%s\n", line, state); traceStream.flush(); }
        switch (state) {
            case WAITING_DATA:
                if (line.length() == 0) {
                    // ignore empty lines in this mode
                    return null;
                }
                //TODO:error-processing
                DLightLogger.assertTrue(currStack.isEmpty());
                DLightLogger.assertFalse(Character.isWhitespace(line.charAt(0)), "Data row shouldn't start with ' '");
                currData = processDataLine(line);
                DLightLogger.assertTrue(currData != null, "could not parse line " + line);
                //currStack.clear();
                if (!isProfiler) {
                    state = State.WAITING_STACK;
                    return null;
                }
                // fallthrough
            case WAITING_STACK:
                if (line.length() == 0) {
                    state = State.WAITING_DATA;
                    return null;
                }
                String[] stackData = line.split("[ \t]+");
                if (!isProfiler) {
                    DLightLogger.assertTrue(stackData.length == 3, "stack marker should consist of CPU-id, thread-id and timestamp");
                }
                try {
                    currCpu = Integer.parseInt(stackData[0]);
                    currThread = Integer.parseInt(stackData[1]);
                    currTimeStamp = Long.parseLong(stackData[2]);
                } catch (NumberFormatException nfe) {
                    DLightLogger.instance.log(Level.WARNING, "error parsing line " + line, nfe); //TODO:error-processing
                }
                state = State.IN_STACK;
                return null;
            case IN_STACK:
                if (line.length() > 0) {
                    //TODO:error-processing
                    DLightLogger.assertTrue(Character.isWhitespace(line.charAt(0)), "Stack row should start with ' '");
                    line = line.trim();
                    if (isProfiler || !line.startsWith("libc.so.")) { //NOI18N
                        currStack.add(line);
                    }
                    return null;
                } else {
                    StackDataStorage sds = (StackDataStorage)DataStorageManager.getInstance().getDataStorage(DataStorageTypeFactory.getInstance().getDataStorageType(StackDataStorage.STACK_DATA_STORAGE_TYPE_ID));
                    DLightLogger.assertTrue(sds != null); //TODO:error-processing
                    Collections.reverse(currStack);
                    int stackId;
                    long sampleDuration = (isProfiler && 0 < prevTimeStamp)? currTimeStamp - prevTimeStamp : 0;
                    stackId = sds.putStack(currStack, sampleDuration);
                    prevTimeStamp = currTimeStamp;
                    currStack.clear();
                    //colNames.get(colNames.size()-1);
                    state = State.WAITING_DATA;
                    currData.add(Integer.toString(stackId));
                    return new DataRow(colNames, currData);
                }
        }
        return null;
    }
}
