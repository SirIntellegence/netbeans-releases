/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2009 Sun Microsystems, Inc. All rights reserved.
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
 * Portions Copyrighted 2009 Sun Microsystems, Inc.
 */
package org.netbeans.modules.dlight.threadmap.dataprovider;

import java.util.List;
import org.netbeans.modules.dlight.api.datafilter.DataFilter;
import org.netbeans.modules.dlight.core.stack.api.ThreadDump;
import org.netbeans.modules.dlight.core.stack.api.ThreadDumpQuery;
import org.netbeans.modules.dlight.spi.storage.DataStorage;
import org.netbeans.modules.dlight.spi.storage.ServiceInfoDataStorage;
import org.netbeans.modules.dlight.core.stack.dataprovider.ThreadMapDataQuery;
import org.netbeans.modules.dlight.core.stack.api.ThreadMapData;
import org.netbeans.modules.dlight.core.stack.dataprovider.ThreadMapDataProvider;
import org.netbeans.modules.dlight.core.stack.storage.StackDataStorage;
import org.netbeans.modules.dlight.threadmap.storage.ThreadMapDataStorage;

public class ThreadMapDataProviderImpl implements ThreadMapDataProvider {

    private StackDataStorage stackDataStorage;
    private final ThreadMapDataStorage storage = ThreadMapDataStorage.getInstance();

    public void attachTo(ServiceInfoDataStorage serviceInfoDataStorage) {
    }

    public ThreadMapData queryData(ThreadMapDataQuery query) {
        if (storage == null) {
            throw new NullPointerException("No STORAGE"); // NOI18N
        }

        return storage.queryThreadMapData(query);
    }

    public ThreadDump getThreadDump(final ThreadDumpQuery query){
        if (stackDataStorage == null) {
            return null;
        }
        return stackDataStorage.getThreadDump(query);
//      TODO: try the new getThreadSnapshots() method
//        final long timestamp = query.getThreadState().getTimeStamp();
//        final List<ThreadSnapshot> result = stackDataStorage.getThreadSnapshots(
//                new ThreadSnapshotQuery(query.isFullMode(), new ThreadSnapshotQuery.ThreadFilter(query.getShowThreads()), new ThreadSnapshotQuery.TimeFilter(-1, timestamp, ThreadSnapshotQuery.TimeFilter.Mode.LAST)));
//        return new ThreadDump() {
//
//            public long getTimestamp() {
//                return timestamp;
//            }
//
//            public List<ThreadSnapshot> getThreadStates() {
//                return result;
//            }
//        };
    }

    public void attachTo(DataStorage storage) {
        if (storage instanceof StackDataStorage) {
            stackDataStorage = (StackDataStorage) storage;
        } else {
            stackDataStorage = null;
        }
    }

    public void dataFiltersChanged(List<DataFilter> newSet) {
    }

 
}
