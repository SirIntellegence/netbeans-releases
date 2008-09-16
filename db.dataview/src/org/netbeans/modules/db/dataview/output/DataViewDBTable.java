/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
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
package org.netbeans.modules.db.dataview.output;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.netbeans.modules.db.dataview.meta.DBColumn;
import org.netbeans.modules.db.dataview.meta.DBTable;
import org.netbeans.modules.db.dataview.util.DataViewUtils;

/**
 * Wrapper class provides ordered columns and tooltips
 *
 * @author Ahimanikya Satapathy
 */
class DataViewDBTable {

    private final DBTable[] dbTables;
    private final List<DBColumn> columns;
    private String[] columnTooltipStr;

    public DataViewDBTable(Collection<DBTable> tables) {
        assert tables != null;

        dbTables = new DBTable[tables.size()];
        List<DBColumn> cols = new ArrayList<DBColumn>();

        for (DBTable tbl : tables.toArray(dbTables)) {
            cols.addAll(tbl.getColumnList());
        }
        Collections.sort(cols, new ColumnOrderComparator());
        columns = Collections.unmodifiableList(cols);        
    }
    
    public DBTable geTable(int index) {
        return dbTables[index];
    }

    public int geTableCount() {
        return dbTables.length;
    }

    public boolean hasOneTable() {
        return dbTables != null && dbTables.length == 1 && !dbTables[0].getName().equals("");
    }

    public String getFullyQualifiedName(int index) {
        return dbTables[index].getFullyQualifiedName();
    }

    public DBColumn getColumn(int index) {
        return columns.get(index);
    }

    public int getColumnType(int index) {
        return columns.get(index).getJdbcType();
    }

    public String getColumnName(int index) {
        return columns.get(index).getName();
    }

    public String getQualifiedName(int index) {
        return columns.get(index).getQualifiedName();
    }

    public int getColumnCount() {
        return columns.size();
    }

    public synchronized Map getColumns() {
        Map<String, DBColumn> colMap = new HashMap<String, DBColumn>();
        for (DBTable tbl : dbTables) {
            colMap.putAll(tbl.getColumns());
        }
        return Collections.unmodifiableMap(colMap);
    }

    public synchronized String[] getColumnToolTips() {
        if (columnTooltipStr == null) {
            columnTooltipStr = new String[columns.size()];
            int i = 0;
            for (DBColumn column : columns) {
                columnTooltipStr[i++] = DataViewUtils.getColumnToolTip(column);
            }
        }
        return columnTooltipStr;
    }
    
    final class ColumnOrderComparator implements Comparator<DBColumn> {

        private ColumnOrderComparator() {
        }

        public int compare(DBColumn col1, DBColumn col2) {
            return col1.getOrdinalPosition() - col2.getOrdinalPosition();
        }
    }    
}
