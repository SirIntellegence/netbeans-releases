/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 1997-2009 Sun Microsystems, Inc. All rights reserved.
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

package org.netbeans.modules.db.explorer.node;

import java.awt.datatransfer.Transferable;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import org.netbeans.api.db.explorer.DatabaseException;
import org.netbeans.api.db.explorer.DatabaseMetaDataTransfer;
import org.netbeans.modules.db.explorer.DatabaseConnection;
import org.netbeans.api.db.explorer.node.BaseNode;
import org.netbeans.api.db.explorer.node.ChildNodeFactory;
import org.netbeans.api.db.explorer.node.NodeProvider;
import org.netbeans.lib.ddl.adaptors.DefaultAdaptor;
import org.netbeans.lib.ddl.impl.Specification;
import org.netbeans.modules.db.explorer.ConnectionList;
import org.netbeans.modules.db.explorer.DatabaseConnectionAccessor;
import org.netbeans.modules.db.explorer.DatabaseMetaDataTransferAccessor;
import org.netbeans.modules.db.metadata.model.api.MetadataModel;
import org.netbeans.modules.db.metadata.model.api.MetadataModels;
import org.openide.util.Exceptions;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.openide.util.datatransfer.ExTransferable;

/**
 *
 * @author Rob Englander
 */
public class ConnectionNode extends BaseNode {
    
    private static final String CONNECTEDICONBASE = "org/netbeans/modules/db/resources/connection.gif"; // NOI18N
    private static final String DISCONNECTEDICONBASE = "org/netbeans/modules/db/resources/connectionDisconnected.gif"; // NOI18N
    private static final String FOLDER = "Connection"; // NOI18N
    
    /** 
     * Create an instance of ConnectionNode.
     * 
     * @param dataLookup the lookup to use when creating node providers
     * @return the ConnectionNode instance
     */
    public static ConnectionNode create(NodeDataLookup dataLookup, NodeProvider provider) {
        ConnectionNode node = new ConnectionNode(dataLookup, provider);
        node.setup();
        return node;
    }
    
    // the connection
    private final DatabaseConnection connection;

    /**
     * Constructor
     * 
     * @param lookup the associated lookup
     */
    private ConnectionNode(NodeDataLookup lookup, NodeProvider provider) {
        super(new ChildNodeFactory(lookup), lookup, FOLDER, provider);
        connection = getLookup().lookup(DatabaseConnection.class);
        lookup.add(DatabaseConnectionAccessor.DEFAULT.createDatabaseConnection(connection));
    }

    protected void initialize() {
        // listen for change events
        connection.addPropertyChangeListener(
            new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent evt) {
                    if (evt.getPropertyName().equals("connectionComplete") || // NOI18N
                            evt.getPropertyName().equals("disconnected")) { // NOI18N
                        updateModel();
                    }
                }
            }
        );

        updateModel();
    }

    @Override
    public void setPropertyValue(Property nps, Object val) {
        super.setPropertyValue(nps, val);

        boolean refreshNode = true;

        if (nps.getName().equals(USER)) {
            connection.setUser(val.toString());
        } else if (nps.getName().equals(REMEMBERPW)) {
            connection.setRememberPassword((Boolean)val);
            refreshNode = false;
        } else if (nps.getName().equals(DATABASEURL)) {
            connection.setDatabase(val.toString());
        } else if (nps.getName().equals(DRIVER)) {
            connection.setDriver(val.toString());
        } else if (nps.getName().equals(SCHEMA)) {
            connection.setSchema(val.toString());
        } else if (nps.getName().equals(PROP_DEFSCHEMA)) {
            connection.setSchema(val.toString());
        } else if (nps.getName().equals(SCHEMA)) {
            connection.setSchema(val.toString());
        } else if (nps.getName().equals(DISPLAYNAME)) {
            connection.setDisplayName(val.toString());
            setDisplayName(val.toString());
            refreshNode = false;
        }

        if (refreshNode) {
            refresh();
        }
    }

    private void updateLocalProperties() {
        try {
            clearProperties();
            boolean connected = !connection.getConnector().isDisconnected();

            addProperty(DISPLAYNAME, DISPLAYNAMEDESC, String.class, true, connection.getDisplayName());
            addProperty(DATABASEURL, DATABASEURLDESC, String.class, !connected, connection.getDatabase());
            addProperty(DRIVER, DRIVERDESC, String.class, !connected, connection.getDriver());
            addProperty(SCHEMA, SCHEMADESC, String.class, false, connection.getSchema());
            addProperty(USER, USERDESC, String.class, !connected, connection.getUser());
            addProperty(REMEMBERPW, REMEMBERPWDESC,
                    Boolean.class, !connected, connection.rememberPassword());

            if (connected) {
                Specification spec = connection.getConnector().getDatabaseSpecification();
                DatabaseMetaData md = spec.getMetaData();

                addProperty(DefaultAdaptor.PROP_PRODUCTNAME, null, String.class, false, md.getDatabaseProductName());
                addProperty(DefaultAdaptor.PROP_PRODUCTVERSION, null, String.class, false, md.getDatabaseProductVersion());

                addProperty(DefaultAdaptor.PROP_READONLY, null, Boolean.class, false, md.isReadOnly());

                addProperty(DefaultAdaptor.PROP_MIXEDCASE_IDENTIFIERS, null, Boolean.class, false, md.supportsMixedCaseIdentifiers());
                addProperty(DefaultAdaptor.PROP_MIXEDCASE_QUOTED_IDENTIFIERS, null, Boolean.class, false, md.supportsMixedCaseQuotedIdentifiers());
                addProperty(DefaultAdaptor.PROP_ALTER_ADD, null, Boolean.class, false, md.supportsAlterTableWithAddColumn());
                addProperty(DefaultAdaptor.PROP_ALTER_DROP, null, Boolean.class, false, md.supportsAlterTableWithDropColumn());
                addProperty(DefaultAdaptor.PROP_CONVERT, null, Boolean.class, false, md.supportsConvert());
                addProperty(DefaultAdaptor.PROP_TABLE_CORRELATION_NAMES, null, Boolean.class, false, md.supportsTableCorrelationNames());
                addProperty(DefaultAdaptor.PROP_TABLE_CORRELATION_NAMES, null, Boolean.class, false, md.supportsDifferentTableCorrelationNames());
                addProperty(DefaultAdaptor.PROP_EXPRESSIONS_IN_ORDERBY, null, Boolean.class, false, md.supportsExpressionsInOrderBy());
                addProperty(DefaultAdaptor.PROP_ORDER_BY_UNRELATED, null, Boolean.class, false, md.supportsOrderByUnrelated());
                addProperty(DefaultAdaptor.PROP_GROUP_BY, null, Boolean.class, false, md.supportsGroupBy());
                addProperty(DefaultAdaptor.PROP_UNRELATED_GROUP_BY, null, Boolean.class, false, md.supportsGroupByUnrelated());
                addProperty(DefaultAdaptor.PROP_BEYOND_GROUP_BY, null, Boolean.class, false, md.supportsGroupByBeyondSelect());
                addProperty(DefaultAdaptor.PROP_ESCAPE_LIKE, null, Boolean.class, false, md.supportsLikeEscapeClause());
                addProperty(DefaultAdaptor.PROP_MULTIPLE_RS, null, Boolean.class, false, md.supportsMultipleResultSets());
                addProperty(DefaultAdaptor.PROP_MULTIPLE_TRANSACTIONS, null, Boolean.class, false, md.supportsMultipleTransactions());
                addProperty(DefaultAdaptor.PROP_NON_NULL_COLUMNSS, null, Boolean.class, false, md.supportsNonNullableColumns());
                addProperty(DefaultAdaptor.PROP_MINUMUM_SQL_GRAMMAR, null, Boolean.class, false, md.supportsMinimumSQLGrammar());
                addProperty(DefaultAdaptor.PROP_CORE_SQL_GRAMMAR, null, Boolean.class, false, md.supportsCoreSQLGrammar());
                addProperty(DefaultAdaptor.PROP_EXTENDED_SQL_GRAMMAR, null, Boolean.class, false, md.supportsExtendedSQLGrammar());
                addProperty(DefaultAdaptor.PROP_ANSI_SQL_GRAMMAR, null, Boolean.class, false, md.supportsANSI92EntryLevelSQL());
                addProperty(DefaultAdaptor.PROP_INTERMEDIATE_SQL_GRAMMAR, null, Boolean.class, false, md.supportsANSI92IntermediateSQL());
                addProperty(DefaultAdaptor.PROP_FULL_SQL_GRAMMAR, null, Boolean.class, false, md.supportsANSI92FullSQL());
                addProperty(DefaultAdaptor.PROP_INTEGRITY_ENHANCEMENT, null, Boolean.class, false, md.supportsIntegrityEnhancementFacility());
                addProperty(DefaultAdaptor.PROP_OUTER_JOINS, null, Boolean.class, false, md.supportsOuterJoins());
                addProperty(DefaultAdaptor.PROP_FULL_OUTER_JOINS, null, Boolean.class, false, md.supportsFullOuterJoins());
                addProperty(DefaultAdaptor.PROP_LIMITED_OUTER_JOINS, null, Boolean.class, false, md.supportsLimitedOuterJoins());
                addProperty(DefaultAdaptor.PROP_SCHEMAS_IN_DML, null, Boolean.class, false, md.supportsSchemasInDataManipulation());
                addProperty(DefaultAdaptor.PROP_SCHEMAS_IN_PROCEDURE_CALL, null, Boolean.class, false, md.supportsSchemasInProcedureCalls());
                addProperty(DefaultAdaptor.PROP_SCHEMAS_IN_TABLE_DEFINITION, null, Boolean.class, false, md.supportsSchemasInTableDefinitions());
                addProperty(DefaultAdaptor.PROP_SCHEMAS_IN_INDEX, null, Boolean.class, false, md.supportsSchemasInIndexDefinitions());
                addProperty(DefaultAdaptor.PROP_SCHEMAS_IN_PRIVILEGE_DEFINITION, null, Boolean.class, false, md.supportsSchemasInPrivilegeDefinitions());
                addProperty(DefaultAdaptor.PROP_CATALOGS_IN_DML, null, Boolean.class, false, md.supportsCatalogsInDataManipulation());
                addProperty(DefaultAdaptor.PROP_CATALOGS_IN_PROCEDURE_CALL, null, Boolean.class, false, md.supportsCatalogsInProcedureCalls());
                addProperty(DefaultAdaptor.PROP_CATALOGS_IN_TABLE_DEFINITION, null, Boolean.class, false, md.supportsCatalogsInTableDefinitions());
                addProperty(DefaultAdaptor.PROP_CATALOGS_IN_INDEX, null, Boolean.class, false, md.supportsCatalogsInIndexDefinitions());
                addProperty(DefaultAdaptor.PROP_CATALOGS_IN_PRIVILEGE_DEFINITION, null, Boolean.class, false, md.supportsCatalogsInPrivilegeDefinitions());
                addProperty(DefaultAdaptor.PROP_POSITIONED_DELETE, null, Boolean.class, false, md.supportsPositionedDelete());
                addProperty(DefaultAdaptor.PROP_POSITIONED_UPDATE, null, Boolean.class, false, md.supportsPositionedUpdate());
                addProperty(DefaultAdaptor.PROP_SELECT_FOR_UPDATE, null, Boolean.class, false, md.supportsSelectForUpdate());
                addProperty(DefaultAdaptor.PROP_STORED_PROCEDURES, null, Boolean.class, false, md.supportsStoredProcedures());
                addProperty(DefaultAdaptor.PROP_SUBQUERY_IN_COMPARSIONS, null, Boolean.class, false, md.supportsSubqueriesInComparisons());
                addProperty(DefaultAdaptor.PROP_SUBQUERY_IN_EXISTS, null, Boolean.class, false, md.supportsSubqueriesInExists());
                addProperty(DefaultAdaptor.PROP_SUBQUERY_IN_INS, null, Boolean.class, false, md.supportsSubqueriesInIns());
                addProperty(DefaultAdaptor.PROP_SUBQUERY_IN_QUANTIFIEDS, null, Boolean.class, false, md.supportsSubqueriesInQuantifieds());
                addProperty(DefaultAdaptor.PROP_CORRELATED_SUBQUERIES, null, Boolean.class, false, md.supportsCorrelatedSubqueries());
                addProperty(DefaultAdaptor.PROP_UNION, null, Boolean.class, false, md.supportsUnion());
                addProperty(DefaultAdaptor.PROP_UNION_ALL, null, Boolean.class, false, md.supportsUnionAll());
                addProperty(DefaultAdaptor.PROP_OPEN_CURSORS_ACROSS_COMMIT, null, Boolean.class, false, md.supportsOpenCursorsAcrossCommit());
                addProperty(DefaultAdaptor.PROP_OPEN_CURSORS_ACROSS_ROLLBACK, null, Boolean.class, false, md.supportsOpenCursorsAcrossRollback());
                addProperty(DefaultAdaptor.PROP_OPEN_STATEMENTS_ACROSS_COMMIT, null, Boolean.class, false, md.supportsOpenStatementsAcrossCommit());
                addProperty(DefaultAdaptor.PROP_OPEN_STATEMENTS_ACROSS_ROLLBACK, null, Boolean.class, false, md.supportsOpenStatementsAcrossRollback());
                addProperty(DefaultAdaptor.PROP_TRANSACTIONS, null, Boolean.class, false, md.supportsTransactions());
                addProperty(DefaultAdaptor.PROP_DDL_AND_DML_TRANSACTIONS, null, Boolean.class, false, md.supportsDataDefinitionAndDataManipulationTransactions());
                addProperty(DefaultAdaptor.PROP_DML_TRANSACTIONS_ONLY, null, Boolean.class, false, md.supportsDataManipulationTransactionsOnly());
                addProperty(DefaultAdaptor.PROP_BATCH_UPDATES, null, Boolean.class, false, md.supportsBatchUpdates());
                addProperty(DefaultAdaptor.PROP_CATALOG_AT_START, null, Boolean.class, false, md.isCatalogAtStart());
                addProperty(DefaultAdaptor.PROP_COLUMN_ALIASING, null, Boolean.class, false, md.supportsColumnAliasing());
                addProperty(DefaultAdaptor.PROP_DDL_CAUSES_COMMIT, null, Boolean.class, false, md.dataDefinitionCausesTransactionCommit());
                addProperty(DefaultAdaptor.PROP_DDL_IGNORED_IN_TRANSACTIONS, null, Boolean.class, false, md.dataDefinitionIgnoredInTransactions());
                addProperty(DefaultAdaptor.PROP_DIFF_TABLE_CORRELATION_NAMES, null, Boolean.class, false, md.supportsDifferentTableCorrelationNames());
                addProperty(DefaultAdaptor.PROP_LOCAL_FILES, null, Boolean.class, false, md.usesLocalFiles());
                addProperty(DefaultAdaptor.PROP_FILE_PER_TABLE, null, Boolean.class, false, md.usesLocalFilePerTable());
                addProperty(DefaultAdaptor.PROP_ROWSIZE_INCLUDING_BLOBS, null, Boolean.class, false, md.doesMaxRowSizeIncludeBlobs());
                addProperty(DefaultAdaptor.PROP_NULL_PLUS_NULL_IS_NULL, null, Boolean.class, false, md.nullPlusNonNullIsNull());
                addProperty(DefaultAdaptor.PROP_PROCEDURES_ARE_CALLABLE, null, Boolean.class, false, md.allProceduresAreCallable());
                addProperty(DefaultAdaptor.PROP_TABLES_ARE_SELECTABLE, null, Boolean.class, false, md.allTablesAreSelectable());

                addProperty(DefaultAdaptor.PROP_MAX_BINARY_LITERAL_LENGTH, null, Integer.class, false, md.getMaxBinaryLiteralLength());
                addProperty(DefaultAdaptor.PROP_MAX_CHAR_LITERAL_LENGTH, null, Integer.class, false, md.getMaxCharLiteralLength());
                addProperty(DefaultAdaptor.PROP_MAX_COLUMN_NAME_LENGTH, null, Integer.class, false, md.getMaxColumnNameLength());
                addProperty(DefaultAdaptor.PROP_MAX_COLUMNS_IN_GROUPBY, null, Integer.class, false, md.getMaxColumnsInGroupBy());
                addProperty(DefaultAdaptor.PROP_MAX_COLUMNS_IN_INDEX, null, Integer.class, false, md.getMaxColumnsInIndex());
                addProperty(DefaultAdaptor.PROP_MAX_COLUMNS_IN_ORDERBY, null, Integer.class, false, md.getMaxColumnsInOrderBy());
                addProperty(DefaultAdaptor.PROP_MAX_COLUMNS_IN_SELECT, null, Integer.class, false, md.getMaxColumnsInSelect());
                addProperty(DefaultAdaptor.PROP_MAX_COLUMNS_IN_TABLE, null, Integer.class, false, md.getMaxColumnsInTable());
                addProperty(DefaultAdaptor.PROP_MAX_CONNECTIONS, null, Integer.class, false, md.getMaxConnections());
                addProperty(DefaultAdaptor.PROP_MAX_CURSORNAME_LENGTH, null, Integer.class, false, md.getMaxCursorNameLength());
                addProperty(DefaultAdaptor.PROP_MAX_INDEX_LENGTH, null, Integer.class, false, md.getMaxIndexLength());
                addProperty(DefaultAdaptor.PROP_MAX_SCHEMA_NAME, null, Integer.class, false, md.getMaxSchemaNameLength());
                addProperty(DefaultAdaptor.PROP_MAX_PROCEDURE_NAME, null, Integer.class, false, md.getMaxProcedureNameLength());
                addProperty(DefaultAdaptor.PROP_MAX_CATALOG_NAME, null, Integer.class, false, md.getMaxCatalogNameLength());
                addProperty(DefaultAdaptor.PROP_MAX_ROW_SIZE, null, Integer.class, false, md.getMaxRowSize());
                addProperty(DefaultAdaptor.PROP_MAX_STATEMENT_LENGTH, null, Integer.class, false, md.getMaxStatementLength());
                addProperty(DefaultAdaptor.PROP_MAX_STATEMENTS, null, Integer.class, false, md.getMaxStatements());
                addProperty(DefaultAdaptor.PROP_MAX_TABLENAME_LENGTH, null, Integer.class, false, md.getMaxTableNameLength());
                addProperty(DefaultAdaptor.PROP_MAX_TABLES_IN_SELECT, null, Integer.class, false, md.getMaxTablesInSelect());
                addProperty(DefaultAdaptor.PROP_MAX_USERNAME, null, Integer.class, false, md.getMaxUserNameLength());
                addProperty(DefaultAdaptor.PROP_DEFAULT_ISOLATION, null, Integer.class, false, md.getDefaultTransactionIsolation());

                addProperty(DefaultAdaptor.PROP_DRIVERNAME, null, String.class, false, md.getDriverName());
                addProperty(DefaultAdaptor.PROP_DRIVER_VERSION, null, String.class, false, md.getDriverVersion());
                addProperty(DefaultAdaptor.PROP_DRIVER_MAJOR_VERSION, null, Integer.class, false, md.getDriverMajorVersion());
                addProperty(DefaultAdaptor.PROP_DRIVER_MINOR_VERSION, null, Integer.class, false, md.getDriverMinorVersion());

                addProperty(DefaultAdaptor.PROP_IDENTIFIER_QUOTE, null, String.class, false, md.getIdentifierQuoteString());
                addProperty(DefaultAdaptor.PROP_SQL_KEYWORDS, null, String.class, false, md.getSQLKeywords());

                addProperty(DefaultAdaptor.PROP_NUMERIC_FUNCTIONS, null, String.class, false, md.getNumericFunctions());
                addProperty(DefaultAdaptor.PROP_STRING_FUNCTIONS, null, String.class, false, md.getStringFunctions());
                addProperty(DefaultAdaptor.PROP_SYSTEM_FUNCTIONS, null, String.class, false, md.getSystemFunctions());
                addProperty(DefaultAdaptor.PROP_TIME_FUNCTIONS, null, String.class, false, md.getTimeDateFunctions());
                addProperty(DefaultAdaptor.PROP_STRING_ESCAPE, null, String.class, false, md.getSearchStringEscape());
                addProperty(DefaultAdaptor.PROP_EXTRA_CHARACTERS, null, String.class, false, md.getExtraNameCharacters());
                addProperty(DefaultAdaptor.PROP_SCHEMA_TERM, null, String.class, false, md.getSchemaTerm());
                addProperty(DefaultAdaptor.PROP_PROCEDURE_TERM, null, String.class, false, md.getProcedureTerm());
                addProperty(DefaultAdaptor.PROP_CATALOG_TERM, null, String.class, false, md.getCatalogTerm());
                addProperty(DefaultAdaptor.PROP_CATALOGS_SEPARATOR, null, String.class, false, md.getCatalogSeparator());
            }
        } catch (Exception e) {
            Exceptions.printStackTrace(e);
        }
    }

    public DatabaseConnection getDatabaseConnection() {
        return connection;
    }
    
    private synchronized void updateModel() {
        RequestProcessor.getDefault().post(
            new Runnable() {
                public void run() {
                    boolean connected = !connection.getConnector().isDisconnected();

                    if (connected) {
                        MetadataModel model = MetadataModels.createModel(connection.getConnection(), connection.getSchema());
                        connection.setMetadataModel(model);
                        refresh();

                    } else {
                        connection.setMetadataModel(null);
                        ConnectionNode.this.getNodeRegistry().removeAllNodes();  //#170935 - workaround
                        refresh();
                    }

                    updateLocalProperties();

                }
            }
        );
    }

    @Override
    public boolean canDestroy() {
        boolean result = true;
        
        Connection conn = connection.getJDBCConnection();
        if (conn != null) {
            result = ! DatabaseConnection.isVitalConnection(conn, connection);
        }
        
        return result;
    }
    
    @Override
    public void destroy() {
        RequestProcessor.getDefault().post(
            new Runnable() {
                public void run() {
                    try {
                        ConnectionList.getDefault().remove(connection);
                    } catch (DatabaseException e) {
                        Exceptions.printStackTrace(e);
                    }
                }
            }
        );
    }
    
    public String getName() {
        return connection.getName();
    }

    @Override
    public String getDisplayName() {
        return connection.getDisplayName();
    }
 
    public String getIconBase() {
        boolean disconnected = ! DatabaseConnection.isVitalConnection(connection.getConnection(), null);

        if (disconnected) {
            return DISCONNECTEDICONBASE;
        }
        else {
            return CONNECTEDICONBASE;
        }
    }

    @Override
    public boolean canCopy() {
        return true;
    }

    @Override
    public Transferable clipboardCopy() throws IOException {
        ExTransferable result = ExTransferable.create(super.clipboardCopy());
        result.put(new ExTransferable.Single(DatabaseMetaDataTransfer.CONNECTION_FLAVOR) {
            protected Object getData() {
                return DatabaseMetaDataTransferAccessor.DEFAULT.createConnectionData(connection.getDatabaseConnection(),
                        connection.findJDBCDriver());
            }
        });
        return result;
    }

    @Override
    public String getShortDescription() {
        if (!getName().equals(getDisplayName())) {
            return getName();
        } else {
            return NbBundle.getMessage (ConnectionNode.class, "ND_Connection"); //NOI18N
        }
    }

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx(ConnectionNode.class);
    }

}
