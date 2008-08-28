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
package org.netbeans.modules.db.dataview.meta;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import org.netbeans.api.db.sql.support.SQLIdentifiers;
import org.netbeans.modules.db.dataview.util.DataViewUtils;
import org.openide.util.Exceptions;

/**
 * Extracts database metadata information (table names and constraints, their
 * associated columns, etc.)
 *
 * @author Ahimanikya Satapathy
 */
public final class DBMetaDataFactory {

    public static final int NAME = 0;
    public static final int CATALOG = 1;
    public static final int SCHEMA = 2;
    public static final int TYPE = 3;
    public static final int DB2 = 0;
    public static final int ORACLE = 1;
    public static final int SQLSERVER = 2;
    public static final int JDBC = 3;
    public static final int VSAM_ADABAS_IAM = 4;
    public static final int PostgreSQL = 5;
    public static final int MYSQL = 6;
    public static final int DERBY = 7;
    public static final int SYBASE = 8;
    public static final int AXION = 9;
    public static final String DB2_TEXT = "DB2"; // NOI18N
    public static final String ORACLE_TEXT = "ORACLE"; // NOI18N
    public static final String AXION_TEXT = "AXION"; // NOI18N
    public static final String DERBY_TEXT = "DERBY"; // NOI18N
    public static final String MYSQL_TEXT = "MYSQL"; // NOI18N
    public static final String PostgreSQL_TEXT = "PostgreSQL"; // NOI18N
    public static final String SQLSERVER_TEXT = "SQL SERVER"; // NOI18N
    public static final String SYBASE_TEXT = "SYBASE"; // NOI18N
    public static final String JDBC_TEXT = "JDBC"; // NOI18N
    public static final String VSAM_ADABAS_IAM_TEXT = "VSAM/ADABAS/IAM"; // NOI18N
    /** List of database type display descriptions */
    public static final String[] DBTYPES = {
        DB2_TEXT, ORACLE_TEXT, SQLSERVER_TEXT,
        JDBC_TEXT, VSAM_ADABAS_IAM_TEXT, PostgreSQL_TEXT,
        MYSQL_TEXT, DERBY_TEXT, SYBASE_TEXT, AXION_TEXT
    };
    private Connection dbconn; // db connection
    private int dbType = -1;
    private DatabaseMetaData dbmeta; // db metadata

    public DBMetaDataFactory(Connection dbconn) throws SQLException {
        assert dbconn != null;
        this.dbconn = dbconn;
        dbmeta = dbconn.getMetaData();
        dbType = getDBType();
    }

    public boolean supportsLimit() {
        switch (dbType) {
            case MYSQL:
            case PostgreSQL:
            case AXION:
                return true;
            default:
                return false;
        }
    }

    private String getDBName() {
        String dbname = "";
        try {
            dbname = dbmeta.getDatabaseProductName();
        } catch (SQLException e) {
            Exceptions.printStackTrace(e);
        }
        return dbname;
    }

    public int getDBType() throws SQLException {
        if (dbType != -1) {
            return dbType;
        }
        // get the database type based on the product name converted to lowercase
        if (dbmeta.getURL() != null) {
            return getDBTypeFromURL(dbmeta.getURL());
        }
        return getDBTypeFromURL(getDBName());
    }

    private static int getDBTypeFromURL(String url) {
        int dbtype = -1;

        // get the database type based on the product name converted to lowercase
        url = url.toLowerCase();
        if (url.indexOf("sybase") > -1) { // NOI18N
            dbtype = SYBASE;
        } else if (url.equals("microsoft sql server") || (url.equals("sql server"))) { // NOI18N
            dbtype = SQLSERVER;
        } else if ((url.indexOf("db2") > -1) || (url.equals("as"))) { // NOI18N
            dbtype = DB2;
        } else if ((url.equals("exadas")) || (url.equals("attunity connect driver"))) { // NOI18N
            dbtype = VSAM_ADABAS_IAM;
        } else if (url.indexOf("orac") > -1) { // NOI18N
            dbtype = ORACLE;
        } else if (url.indexOf("axion") > -1) { // NOI18N
            dbtype = AXION;
        } else if (url.indexOf("derby") > -1) { // NOI18N
            dbtype = DERBY;
        } else if (url.indexOf("postgre") > -1) { // NOI18N
            dbtype = PostgreSQL;
        } else if (url.indexOf("mysql") > -1) { // NOI18N
            dbtype = MYSQL;
        } else {
            dbtype = JDBC;
        }

        return dbtype;
    }

    public Map<Integer, String> buildDBSpecificDatatypeMap() throws SQLException {
        Map<Integer, String> typeInfoMap = new HashMap<Integer, String>();
        ResultSet typeInfo = dbmeta.getTypeInfo();
        String typeName = null;
        Integer type = null;
        int jdbcType = 0;
        while (typeInfo.next()) {
            typeName = typeInfo.getString("TYPE_NAME");
            jdbcType = typeInfo.getInt("DATA_TYPE");
            type = new Integer(jdbcType);
            if (!typeInfoMap.containsKey(type)) {
                typeInfoMap.put(type, typeName);
            }
        }
        return typeInfoMap;
    }

    private DBPrimaryKey getPrimaryKeys(String tcatalog, String tschema, String tname) {
        ResultSet rs = null;
        try {
            rs = dbmeta.getPrimaryKeys(setToNullIfEmpty(tcatalog), setToNullIfEmpty(tschema), tname);
            return new DBPrimaryKey(rs);
        } catch (SQLException e) {
            return null;
        } finally {
            DataViewUtils.closeResources(rs);
        }
    }

    private Map<String, DBForeignKey> getForeignKeys(DBTable table) {
        Map<String, DBForeignKey> fkList = Collections.emptyMap();
        ResultSet rs = null;
        try {
            rs = dbmeta.getImportedKeys(setToNullIfEmpty(table.getCatalog()), setToNullIfEmpty(table.getSchema()), table.getName());
            fkList = DBForeignKey.createForeignKeyColumnMap(table, rs);
        } catch (SQLException e) {
            Exceptions.printStackTrace(e);
        } finally {
            DataViewUtils.closeResources(rs);
        }
        return fkList;
    }

    public synchronized Collection<DBTable> generateDBTables(ResultSet rs, String sql, boolean isSelect) throws SQLException {
        Map<String, DBTable> tables = new LinkedHashMap<String, DBTable>();
        String noTableName = "UNKNOWN"; // NOI18N
        // get table column information
        ResultSetMetaData rsMeta = rs.getMetaData();
        for (int i = 1; i <= rsMeta.getColumnCount(); i++) {
            String tableName = rsMeta.getTableName(i);
            String schemaName = rsMeta.getSchemaName(i);
            String catalogName = rsMeta.getCatalogName(i);
            String key = catalogName + schemaName + tableName;
            if (key.equals("")) {
                key = noTableName;
            }
            DBTable table = tables.get(key);
            if (table == null) {
                table = new DBTable(tableName, schemaName, catalogName);
                tables.put(key, table);
            }

            int sqlTypeCode = rsMeta.getColumnType(i);
            if (sqlTypeCode == java.sql.Types.OTHER && dbType == ORACLE) {
                String sqlTypeStr = rsMeta.getColumnTypeName(i);
                if (sqlTypeStr.startsWith("TIMESTAMP")) { // NOI18N
                    sqlTypeCode = java.sql.Types.TIMESTAMP;
                } else if (sqlTypeStr.startsWith("FLOAT")) { // NOI18N
                    sqlTypeCode = java.sql.Types.FLOAT;
                } else if (sqlTypeStr.startsWith("REAL")) { // NOI18N
                    sqlTypeCode = java.sql.Types.REAL;
                } else if (sqlTypeStr.startsWith("BLOB")) { // NOI18N
                    sqlTypeCode = java.sql.Types.BLOB;
                } else if (sqlTypeStr.startsWith("CLOB")) { // NOI18N
                    sqlTypeCode = java.sql.Types.CLOB;
                }
            }

            String colName = rsMeta.getColumnName(i);
            int position = i;
            int scale = rsMeta.getScale(i);
            int precision = rsMeta.getPrecision(i);

            boolean isNullable = (rsMeta.isNullable(i) == rsMeta.columnNullable);
            String displayName = rsMeta.getColumnLabel(i);
            int displaySize = rsMeta.getColumnDisplaySize(i);
            boolean autoIncrement = rsMeta.isAutoIncrement(i);

            //Oracle DATE type needs to be retrieved as full date and time
            if (sqlTypeCode == java.sql.Types.DATE && dbType == ORACLE) {
                sqlTypeCode = java.sql.Types.TIMESTAMP;
                displaySize = 22;
            }

            // create a table column and add it to the vector
            DBColumn col = new DBColumn(table, colName, sqlTypeCode, scale, precision, isNullable, autoIncrement);
            col.setOrdinalPosition(position);
            col.setDisplayName(displayName);
            col.setDisplaySize(displaySize);
            table.addColumn(col);
            table.setQuoter(SQLIdentifiers.createQuoter(dbmeta));
        }
        
        // Oracle does not return table name for resultsetmetadata.getTableName()
        DBTable table =  tables.get(noTableName);
        if(tables.size() == 1 && table != null && isSelect) {
            adjustTableMetadata(sql, tables.get(noTableName));
            for(DBColumn col: table.getColumns().values()){
                col.setEditable(!table.getName().equals("") && !col.isGenerated());
            }
        }

        for (DBTable tbl : tables.values()) {
            if (DataViewUtils.isNullString(tbl.getName())) {
                continue;
            }
            checkPrimaryKeys(tbl);
            checkForeignKeys(tbl);
        }

        return tables.values();
    }
    
    private void adjustTableMetadata(String sql, DBTable table) {
        String tableName = "";
        if(sql.toUpperCase().contains("FROM")) {
            // User may type "FROM" in either lower, upper or mixed case
            String[] splitByFrom = sql.toUpperCase().split("FROM"); // NOI18N
            String fromsql = sql.substring(sql.length() - splitByFrom[1].length()); 
            if(fromsql.toUpperCase().contains("WHERE")){
                splitByFrom = fromsql.toUpperCase().split("WHERE"); // NOI18N
                fromsql = fromsql.substring(0, splitByFrom[0].length()); 
            }
            StringTokenizer t = new StringTokenizer(fromsql, " ,");

            if(t.hasMoreTokens()) {
                tableName = t.nextToken();
            }

            if(t.hasMoreTokens()) {
                tableName = "";
            }
        }
        String[] splitByDot = tableName.split("\\.");
        if(splitByDot.length == 3){
            table.setCatalogName(unQuoteIfNeeded(splitByDot[0]));
            table.setSchemaName(unQuoteIfNeeded(splitByDot[1]));
            table.setName(unQuoteIfNeeded(splitByDot[2]));
        } else if(splitByDot.length == 2){
            table.setSchemaName(unQuoteIfNeeded(splitByDot[0]));
            table.setName(unQuoteIfNeeded(splitByDot[1]));
        } else if(splitByDot.length == 1){
            table.setName(unQuoteIfNeeded(splitByDot[0]));
        } 
    }
    
    private String unQuoteIfNeeded(String id){
        String quoteStr = "\"";
        try {
            quoteStr = dbmeta.getIdentifierQuoteString().trim();
        } catch ( SQLException e ) {
        }
        return id.replaceAll(quoteStr, "");
    }

    private void checkPrimaryKeys(DBTable newTable) {
        DBPrimaryKey keys = getPrimaryKeys(newTable.getCatalog(), newTable.getSchema(), newTable.getName());
        if (keys != null && keys.getColumnCount() != 0) {
            newTable.setPrimaryKey(keys);

            // now loop through all the columns flagging the primary keys
            List columns = newTable.getColumnList();
            if (columns != null) {
                for (int i = 0; i < columns.size(); i++) {
                    DBColumn col = (DBColumn) columns.get(i);
                    if (keys.contains(col.getName())) {
                        col.setPrimaryKey(true);
                    }
                }
            }
        }
    }

    private void checkForeignKeys(DBTable newTable) {
        // get the foreing keys
        Map<String, DBForeignKey> foreignKeys = getForeignKeys(newTable);
        if (foreignKeys != null) {
            newTable.setForeignKeyMap(foreignKeys);

            // create a hash set of the keys
            Set<String> foreignKeysSet = new HashSet<String>();
            Iterator<DBForeignKey> it = foreignKeys.values().iterator();
            while (it.hasNext()) {
                DBForeignKey key = it.next();
                if (key != null) {
                    foreignKeysSet.addAll(key.getColumnNames());
                }
            }

            // now loop through all the columns flagging the foreign keys
            List columns = newTable.getColumnList();
            if (columns != null) {
                for (int i = 0; i < columns.size(); i++) {
                    DBColumn col = (DBColumn) columns.get(i);
                    if (foreignKeysSet.contains(col.getName())) {
                        col.setForeignKey(true);
                    }
                }
            }
        }
    }

    private String setToNullIfEmpty(String source) {
        if (source != null && source.equals("")) {
            source = null;
        }
        return source;
    }
}
