/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2011 Oracle and/or its affiliates. All rights reserved.
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
 * Contributor(s):
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2010 Sun
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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import org.netbeans.modules.db.dataview.meta.DBColumn;
import org.netbeans.modules.db.dataview.meta.DBConnectionFactory;
import org.netbeans.modules.db.dataview.meta.DBException;
import org.netbeans.modules.db.dataview.meta.DBMetaDataFactory;
import org.netbeans.modules.db.dataview.meta.DBTable;
import org.netbeans.modules.db.dataview.util.DBReadWriteHelper;
import org.netbeans.modules.db.dataview.util.DataViewUtils;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.Cancellable;
import org.openide.util.Mutex;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;

/**
 * This class assumes there will be only one connection which can't be closed.
 *
 * @author Ahimanikya Satapathy
 */
class SQLExecutionHelper {

    private static final Logger LOGGER = Logger.getLogger(SQLExecutionHelper.class.getName());
    private final DataView dataView;
    // the RequestProcessor used for executing statements.
    private final RequestProcessor rp = new RequestProcessor("SQLStatementExecution", 20, true); // NOI18N
    private int resultSetScrollType = ResultSet.TYPE_FORWARD_ONLY;
    private boolean supportesMultipleResultSets = false;

    SQLExecutionHelper(DataView dataView) {
        this.dataView = dataView;
    }

    void initialDataLoad() throws SQLException {
        assert (! SwingUtilities.isEventDispatchThread()) : "Must be called of the EDT!";

        /**
         * Wrap initializing the SQL result into a runnable. This makes it
         * possible to wait for the result in the main thread and cancel the
         * running statement from the main thread.
         *
         * If no statement is run - Thread.isInterrupted is checked at critical
         * points and allows us to do an early exit.
         *
         * See #159929.
         */
        class Loader implements Runnable, Cancellable {
            // Indicate whether the execution is finished
            public boolean finished = false;
            // Hold an exception if it is thrown in the body of the runnable
            public SQLException ex = null;
            Statement stmt = null;

            @Override
            public void run() {
                try {
                    Connection conn = DBConnectionFactory.getInstance()
                            .getConnection(dataView.getDatabaseConnection());
                    String msg;
                    if (conn == null) {
                        Throwable t = DBConnectionFactory.getInstance()
                                .getLastException();
                        if (t != null) {
                            msg = t.getMessage();
                        } else {
                            msg = NbBundle.getMessage(SQLExecutionHelper.class,
                                    "MSG_connection_failure", //NOI18N
                                    dataView.getDatabaseConnection());
                        }
                        NotifyDescriptor nd = new NotifyDescriptor.Message(msg,
                                NotifyDescriptor.ERROR_MESSAGE);
                        DialogDisplayer.getDefault().notifyLater(nd);
                        LOGGER.log(Level.INFO, msg, t);
                        throw new SQLException(msg, t);
                    }
                    try {
                        if (conn.getMetaData().supportsResultSetType(
                                ResultSet.TYPE_SCROLL_INSENSITIVE)) {
                            resultSetScrollType = ResultSet.TYPE_SCROLL_INSENSITIVE;
                        } else if (conn.getMetaData().supportsResultSetType(
                                ResultSet.TYPE_SCROLL_SENSITIVE)) {
                            resultSetScrollType = ResultSet.TYPE_SCROLL_SENSITIVE;
                        }
                    } catch (Exception ex) {
                        LOGGER.log(Level.WARNING, "Exception while querying database for scrollable resultset support");
                    }
                    try {
                        supportesMultipleResultSets = conn.getMetaData().supportsMultipleResultSets();
                    } catch (SQLException ex) {
                        LOGGER.log(Level.INFO, "Database driver throws exception when checking for multiple resultset support.");
                    }
                    DBMetaDataFactory dbMeta = new DBMetaDataFactory(conn);
                    String sql = dataView.getSQLString();

                    if (Thread.interrupted()) {
                        return;
                    }
                    stmt = prepareSQLStatement(conn, sql, true);

                    if (Thread.interrupted()) {
                        return;
                    }
                    // Read multiple Resultsets
                    boolean resultSet = executeSQLStatement(stmt, sql);

                    // @todo: This needs clearing up => in light of request for
                    // the ability to disable autocommit, this need to go
                    if (dataView.getUpdateCount() != -1) {
                        if (!conn.getAutoCommit()) {
                            conn.commit();
                        }
                        return;
                    }
                    if (Thread.interrupted()) {
                        return;
                    }
                    // @todo: This needs clearing up => in light of request for
                    // the ability to disable autocommit, this need to go
                    if (!resultSet) {
                        if (!conn.getAutoCommit()) {
                            conn.commit();
                        }
                        return;
                    }
                    boolean needReread = false;
                    ResultSet rs = null;
                    int count = 0;

                    do {
                        if (resultSet) {
                            rs = stmt.getResultSet();

                            Collection<DBTable> tables = dbMeta.generateDBTables(
                                    rs, sql, isSelectStatement(sql));
                            DataViewDBTable dvTable = new DataViewDBTable(tables);
                            DataViewPageContext pageContext = dataView.addPageContext(
                                    dvTable);
                            needReread |= resultSetNeedsReloading(dvTable);

                            if (!needReread) {
                                loadDataFrom(pageContext, rs, true);
                            }
                        } else {
                            // @todo: Do somethink intelligent with the updatecounts
                            count = stmt.getUpdateCount();
                            if (count >= 0) {
                            } else {
                            }
                        }
                        if (supportesMultipleResultSets) {
                            resultSet = stmt.getMoreResults();
                        } else {
                            resultSet = false;
                        }
                    } while (resultSet || count != -1);

                    if (needReread) {

                        resultSet = executeSQLStatement(stmt, sql);
                        int res = -1;
                        do {
                            if (resultSet) {
                                res++;
                                rs = stmt.getResultSet();
                                DataViewPageContext pageContext = dataView.getPageContext(
                                        res);
                                if (!needReread) {
                                    loadDataFrom(pageContext, rs, true);
                                }
                            } else {
                                // @todo: Do somethink intelligent with the updatecounts
                                count = stmt.getUpdateCount();
                                if (count >= 0) {
                                } else {
                                }
                            }
                            if (supportesMultipleResultSets) {
                                resultSet = stmt.getMoreResults();
                            } else {
                                resultSet = false;
                            }
                        } while (resultSet || count != -1);
                    }
                    DataViewUtils.closeResources(rs);
                } catch (SQLException sqlEx) {
                    this.ex = sqlEx;
                } finally {
                    DataViewUtils.closeResources(stmt);
                    synchronized (Loader.this) {
                        finished = true;
                        this.notifyAll();
                    }
                }
            }

            @Override
            public boolean cancel() {
                if (stmt != null) {
                    try {
                        stmt.cancel();
                    } catch (SQLException sqlEx) {
                        LOGGER.log(Level.FINE, null, sqlEx);
                        // Ok! The DBMS might not support Statement-Canceling
                    }
                }
                return true;
            }
        }
        Loader l = new Loader();
        Future<?> f = rp.submit(l);
        try {
            f.get();
        } catch (InterruptedException ex) {
            f.cancel(true);
        } catch (ExecutionException ex) {
            throw new RuntimeException(ex.getCause());
        }
        synchronized (l) {
            while (true) {
                if (!l.finished) {
                    try {
                        l.wait();
                    } catch (InterruptedException ex) {
                    }
                } else {
                    break;
                }
            }
        }
        if (l.ex != null) {
            throw l.ex;
        }
    }

    RequestProcessor.Task executeInsertRow(final DataViewPageContext pageContext,
            final DBTable table,
            final String insertSQL,
            final Object[] insertedRow) {
        dataView.setEditable(false);

        String title = NbBundle.getMessage(SQLExecutionHelper.class, "LBL_sql_insert");
        SQLStatementExecutor executor = new SQLStatementExecutor(dataView, title, "") {

            @Override
            public void execute() throws SQLException, DBException {
                dataView.setEditable(false);
                List<DBColumn> columns = table.getColumnList();
                PreparedStatement pstmt = conn.prepareStatement(insertSQL);
                try {
                    int pos = 1;
                    for (int i = 0; i < insertedRow.length; i++) {
                        Object val = insertedRow[i];

                        // Check for Constant e.g <NULL>, <DEFAULT>, <CURRENT_TIMESTAMP> etc
                        if (DataViewUtils.isSQLConstantString(val, columns.get(i))) {
                            continue;
                        }

                        // literals
                        int colType = columns.get(i).getJdbcType();
                        DBReadWriteHelper.setAttributeValue(pstmt, pos++, colType, val);
                    }

                    executePreparedStatement(pstmt);
                    int rows = dataView.getUpdateCount();
                    if (rows != 1) {
                        error = true;
                        errorMsg = NbBundle.getMessage(SQLExecutionHelper.class, "MSG_failure_insert_rows");
                    }
                } finally {
                    DataViewUtils.closeResources(pstmt);
                }
            }

            @Override
            public void finished() {
                dataView.resetEditable();
                commitOrRollback(NbBundle.getMessage(SQLExecutionHelper.class, "LBL_insert_command"));
            }

            @Override
            protected void executeOnSucess() {
                // refresh when required
                Boolean needRequery = Mutex.EVENT.readAccess(new Mutex.Action<Boolean>() {
                    @Override
                    public Boolean run() {
                        if (pageContext.getTotalRows() < 0) {
                            pageContext.setTotalRows(0);
                            pageContext.first();
                        }
                        pageContext.incrementRowSize(1);
                        return pageContext.refreshRequiredOnInsert();
                    };
                });
                if(needRequery) {
                    SQLExecutionHelper.this.executeQuery();
                } else {
                    Mutex.EVENT.readAccess(new Runnable() {
                        @Override
                        public void run() {
                            reinstateToolbar();
                        }
                    });
                }
            }
        };
        RequestProcessor.Task task = rp.create(executor);
        executor.setTask(task);
        task.schedule(0);
        return task;
    }

    void executeDeleteRow(final DataViewPageContext pageContext, final DBTable table, final DataViewTableUI rsTable) {
        dataView.setEditable(false);

        SQLStatementGenerator generator = dataView.getSQLStatementGenerator();
        String title = NbBundle.getMessage(SQLExecutionHelper.class, "LBL_sql_delete");

        class DeleteElement {
            public List<Object> values = new ArrayList<Object>();
            public List<Integer> types = new ArrayList<Integer>();
            public String sql;
        }

        final List<DeleteElement> rows = new ArrayList<DeleteElement>();
        for(int viewRow: rsTable.getSelectedRows()) {
            int modelRow = rsTable.convertRowIndexToModel(viewRow);
            DeleteElement de = new DeleteElement();
            de.sql = generator.generateDeleteStatement(table, de.types, de.values, modelRow, rsTable.getModel());
            rows.add(de);
        }

        SQLStatementExecutor executor = new SQLStatementExecutor(dataView, title, "") {
            @Override
            public void execute() throws SQLException, DBException {
                dataView.setEditable(false);
                for (DeleteElement de: rows) {
                    if (Thread.currentThread().isInterrupted() || error) {
                        break;
                    }
                    deleteARow(de);
                }
            }

            private void deleteARow(DeleteElement deleteRow) throws SQLException, DBException {
                PreparedStatement pstmt = conn.prepareStatement(deleteRow.sql);
                try {
                    int pos = 1;
                    for (Object val : deleteRow.values) {
                        DBReadWriteHelper.setAttributeValue(pstmt, pos, deleteRow.types.get(pos - 1), val);
                        pos++;
                    }

                    executePreparedStatement(pstmt);
                    int rows = dataView.getUpdateCount();
                    if (rows == 0) {
                        error = true;
                        errorMsg = errorMsg + NbBundle.getMessage(SQLExecutionHelper.class, "MSG_no_match_to_delete");
                    } else if (rows > 1) {
                        error = true;
                        errorMsg = errorMsg + NbBundle.getMessage(SQLExecutionHelper.class, "MSG_no_unique_row_for_match");
                    }
                } finally {
                    DataViewUtils.closeResources(pstmt);
                }
            }

            @Override
            public void finished() {
                dataView.resetEditable();
                commitOrRollback(NbBundle.getMessage(SQLExecutionHelper.class, "LBL_delete_command"));
            }

            @Override
            protected void executeOnSucess() {
                pageContext.decrementRowSize(rows.size());
                SQLExecutionHelper.this.executeQuery();
            }
        };

        RequestProcessor.Task task = rp.create(executor);
        executor.setTask(task);
        task.schedule(0);
    }

    void executeUpdateRow(final DBTable table, final DataViewTableUI rsTable, final boolean selectedOnly) {
        dataView.setEditable(false);

        SQLStatementGenerator generator = dataView.getSQLStatementGenerator();
        final DataViewTableUIModel dataViewTableUIModel = rsTable.getModel();
        String title = NbBundle.getMessage(SQLExecutionHelper.class, "LBL_sql_update");

        class UpdateElement {
            public List<Object> values = new ArrayList<Object>();
            public List<Integer> types = new ArrayList<Integer>();
            public String sql;
            public Integer key;
        }

        final List<UpdateElement> updateSet = new ArrayList<UpdateElement>();

        int[] viewRows = rsTable.getSelectedRows();
        List<Integer> modelRows = new ArrayList<Integer>();
        for(Integer viewRow: viewRows) {
            modelRows.add(rsTable.convertRowIndexToModel(viewRow));
        }

        for (Integer key : dataViewTableUIModel.getUpdateKeys()) {
            if (modelRows.contains(key) || (!selectedOnly)) {
                UpdateElement ue = new UpdateElement();
                try {
                    ue.key = key;
                    ue.sql = generator.generateUpdateStatement(table, key,
                            dataViewTableUIModel.getChangedData(key), ue.values, ue.types,
                            rsTable.getModel());
                    updateSet.add(ue);
                } catch (DBException ex) {
                    // The model protects against illegal values, so rethrow
                    throw new RuntimeException(ex);
                }
            }
        }

        SQLStatementExecutor executor = new SQLStatementExecutor(dataView, title, "") {

            private PreparedStatement pstmt;
            Set<Integer> keysToRemove = new HashSet<Integer>();

            @Override
            public void execute() throws SQLException, DBException {
                for (UpdateElement ue : updateSet) {
                    if(Thread.interrupted()) {
                        break;
                    }
                    updateARow(ue);
                    keysToRemove.add(ue.key);
                }
            }

            private void updateARow(UpdateElement ue) throws SQLException, DBException {
                pstmt = conn.prepareStatement(ue.sql);
                int pos = 1;
                for (Object val : ue.values) {
                    DBReadWriteHelper.setAttributeValue(pstmt, pos, ue.types.get(pos - 1), val);
                    pos++;
                }

                try {
                    executePreparedStatement(pstmt);
                    int rows = dataView.getUpdateCount();
                    if (rows == 0) {
                        error = true;
                        errorMsg = errorMsg + NbBundle.getMessage(SQLExecutionHelper.class, "MSG_no_match_to_update");
                    } else if (rows > 1) {
                        error = true;
                        errorMsg = errorMsg + NbBundle.getMessage(SQLExecutionHelper.class, "MSG_no_unique_row_for_match");
                    }
                } finally {
                    DataViewUtils.closeResources(pstmt);
                }
            }

            @Override
            public void finished() {
                dataView.resetEditable();
                commitOrRollback(NbBundle.getMessage(SQLExecutionHelper.class, "LBL_update_command"));
            }

            @Override
            protected void executeOnSucess() {
                Mutex.EVENT.writeAccess(new Runnable() {
                    @Override
                    public void run() {
                        DataViewTableUIModel tblContext = rsTable.getModel();
                        for (Integer key : keysToRemove) {
                            tblContext.removeUpdateForSelectedRow(key, false);
                        }
                        reinstateToolbar();
                    }
                });
            }
        };
        RequestProcessor.Task task = rp.create(executor);
        executor.setTask(task);
        task.schedule(0);
    }

    // Truncate is allowed only when there is single table used in the query.
    void executeTruncate(final DataViewPageContext pageContext, final DBTable dbTable) {
        String msg = NbBundle.getMessage(SQLExecutionHelper.class, "MSG_truncate_table_progress");
        String title = NbBundle.getMessage(SQLExecutionHelper.class, "LBL_sql_truncate");
        SQLStatementExecutor executor = new SQLStatementExecutor(dataView, title, msg) {

            private PreparedStatement stmt = null;

            @Override
            public void execute() throws SQLException, DBException {
                String truncateSql = "TRUNCATE TABLE " + dbTable.getFullyQualifiedName(true); // NOI18N

                try {
                    stmt = conn.prepareStatement(truncateSql);
                    executePreparedStatement(stmt);
                } catch (SQLException sqe) {
                    LOGGER.log(Level.FINE, "TRUNCATE Not supported...will try DELETE * \n"); // NOI18N
                    truncateSql = "DELETE FROM " + dbTable.getFullyQualifiedName(true); // NOI18N
                    stmt = conn.prepareStatement(truncateSql);
                    executePreparedStatement(stmt);
                } finally {
                    DataViewUtils.closeResources(stmt);
                }
            }

            @Override
            public void finished() {
                commitOrRollback(NbBundle.getMessage(SQLExecutionHelper.class, "LBL_truncate_command"));
            }

            @Override
            protected void executeOnSucess() {
                pageContext.setTotalRows(0);
                pageContext.first();
                SQLExecutionHelper.this.executeQuery();
            }
        };

        RequestProcessor.Task task = rp.create(executor);
        executor.setTask(task);
        task.schedule(0);
    }

    void executeQueryOffEDT() {
        rp.post(new Runnable() {
            @Override
            public void run() {
                executeQuery();
            }
        });
    }

    // Once Data View is created the it assumes the query never changes.
    void executeQuery() {
        assert (! SwingUtilities.isEventDispatchThread());

        String title = NbBundle.getMessage(SQLExecutionHelper.class, "LBL_sql_executequery");
        SQLStatementExecutor executor = new SQLStatementExecutor(dataView, title, dataView.getSQLString()) {

            private Statement stmt = null;
            boolean lastEditState = dataView.isEditable();

            // Execute the Select statement
            @Override
            public void execute() throws SQLException, DBException {
                dataView.setEditable(false);
                String sql = dataView.getSQLString();
                if (Thread.interrupted()) {
                    return;
                }
                boolean getTotal = false;

                // Get total row count
                for (DataViewPageContext pageContext : dataView.getPageContexts()) {
                    if (pageContext.getTotalRows() == -1) {
                        getTotal = true;
                        break;
                    }
                }

                // Get total row count
                stmt = prepareSQLStatement(conn, sql, getTotal);

                // Execute the query and retrieve all resultsets
                // using: http://www.xyzws.com/Javafaq/how-to-use-methods-getresultset-or-getupdatecount-to-retrieve-the-results/175
                try {
                    if (Thread.interrupted()) {
                        return;
                    }
                    boolean resultSet = executeSQLStatement(stmt, sql);

                    ResultSet rs = null;
                    int res = -1;
                    int count = 0;

                    do {
                        if (resultSet) {
                            res++;
                            DataViewPageContext pageContext = dataView.getPageContext(
                                    res);
                            rs = stmt.getResultSet();
                            loadDataFrom(pageContext, rs, getTotal);
                        } else {
                            // @todo: Do somethink intelligent with the updatecounts
                            count = stmt.getUpdateCount();
                            if (count >= 0) {
                            } else {
                            }
                        }
                        if(supportesMultipleResultSets) {
                            resultSet = stmt.getMoreResults();
                        } else {
                            resultSet = false;
                        }
                    } while (resultSet || count != -1);

                    DataViewUtils.closeResources(rs);
                } catch (SQLException sqlEx) {
                    String title = NbBundle.getMessage(SQLExecutionHelper.class, "MSG_error");
                    String msg = NbBundle.getMessage(SQLExecutionHelper.class, "Confirm_Close");
                    NotifyDescriptor nd = new NotifyDescriptor.Confirmation(sqlEx.getMessage() + "\n" + msg, title,
                            NotifyDescriptor.OK_CANCEL_OPTION, NotifyDescriptor.QUESTION_MESSAGE);
                    DialogDisplayer.getDefault().notify(nd);
                    if (nd.getValue().equals(NotifyDescriptor.YES_OPTION)) {
                        dataView.removeComponents();
                    }
                    throw sqlEx;
                }
            }

            @Override
            public void finished() {
                DataViewUtils.closeResources(stmt);
                dataView.resetEditable();
                synchronized (dataView) {
                    if (error) {
                        dataView.setErrorStatusText(ex);
                    }
                    dataView.resetToolbar(error);
                }
            }

            @Override
            public boolean cancel() {
                boolean superResult = super.cancel();
                if (stmt != null) {
                    try {
                        stmt.cancel();
                    } catch (SQLException sqlEx) {
                        LOGGER.log(Level.FINEST, null, sqlEx);
                        // Ok! The DBMS might not support Statement-Canceling
                    }
                }
                return superResult;
            }
        };
        RequestProcessor.Task task = rp.create(executor);
        executor.setTask(task);
        task.schedule(0);
    }

    private void loadDataFrom(final DataViewPageContext pageContext, ResultSet rs, boolean getTotal) throws SQLException {
        if (rs == null) {
            return;
        }

        int pageSize = pageContext.getPageSize();
        int startFrom = pageContext.getCurrentPos();

        final List<Object[]> rows = new ArrayList<Object[]>();
        int colCnt = pageContext.getTableMetaData().getColumnCount();
        try {
            boolean hasNext = false;
            boolean needSlowSkip = true;

            if (rs.getType() == ResultSet.TYPE_SCROLL_INSENSITIVE
                    || rs.getType() == ResultSet.TYPE_SCROLL_SENSITIVE) {
                try {
                    hasNext = rs.absolute(startFrom);
                    needSlowSkip = false;
                } catch (SQLException ex) {
                    LOGGER.log(Level.FINE, "Absolute positioning failed", ex); // NOI18N
                }
            }

            if (needSlowSkip) {
                // Skip till current position
                hasNext = rs.next();
                int curRowPos = 1;
                while (hasNext && curRowPos < startFrom) {
                    if (Thread.currentThread().isInterrupted()) {
                        return;
                    }
                    hasNext = rs.next();
                    curRowPos++;
                }
            }

            // Get next page
            int rowCnt = 0;
            while (((pageSize == -1) || (pageSize > rowCnt)) && (hasNext)) {
                if (Thread.currentThread().isInterrupted()) {
                    return;
                }

                Object[] row = new Object[colCnt];
                for (int i = 0; i < colCnt; i++) {
                    row[i] = DBReadWriteHelper.readResultSet(rs,
                            pageContext.getTableMetaData().getColumn(i), i + 1);
                }
                rows.add(row);
                rowCnt++;
                try {
                    hasNext = rs.next();
                } catch (SQLException x) {
                    LOGGER.log(Level.INFO, "Failed to forward to next record, cause: " + x.getLocalizedMessage(), x);
                    hasNext = false;
                }
            }

            if (getTotal) {
                Integer result = null;

                if (rs.getType() == ResultSet.TYPE_SCROLL_INSENSITIVE
                        || rs.getType() == ResultSet.TYPE_SCROLL_SENSITIVE) {
                    try {
                        rs.last();
                        result = rs.getRow();
                    } catch (SQLException ex) {
                        LOGGER.log(Level.INFO,
                                "Failed to jump to end of SQL Statement '{0}' - cause: {1}",
                                new Object[]{dataView.getSQLString(), ex});
                    }
                }

                pageContext.setTotalRows(result);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to set up table model.", e); // NOI18N
            throw e;
        } finally {
            Mutex.EVENT.writeAccess(new Mutex.Action<Void>() {
                @Override
                public Void run() {
                    pageContext.getModel().setData(rows);
                    return null;
                }
            });
        }
    }

    private Statement prepareSQLStatement(Connection conn, String sql, boolean needTotal) throws SQLException {
        Statement stmt = null;
        if (sql.startsWith("{")) { // NOI18N
            stmt = conn.prepareCall(sql, resultSetScrollType, ResultSet.CONCUR_READ_ONLY);
        } else if (isSelectStatement(sql)) {
            stmt = conn.createStatement(resultSetScrollType, ResultSet.CONCUR_READ_ONLY);

            // set a reasonable fetchsize
            setFetchSize(stmt, 50);

            // hint to only query a certain number of rows -> potentially
            // improve performance for low page numbers
            // only usable for "non-total" resultsets
            if (!needTotal) {
                try {
                    Integer maxRows = 0;
                    for (DataViewPageContext pageContext : dataView.getPageContexts()) {
                        int currentRows = pageContext.getCurrentPos();
                        int pageSize = pageContext.getPageSize();
                        if (pageSize <= 0) {
                            maxRows = 0;
                            break;
                        } else {
                            maxRows = Math.max(maxRows, currentRows + pageSize);
                        }
                    }
                    stmt.setMaxRows(maxRows);
                } catch (SQLException exc) {
                    LOGGER.log(Level.WARNING, "Unable to set Max row count", exc); // NOI18N
                    try {
                        stmt.setMaxRows(0);
                    } catch (SQLException ex) {}
                }
            }
        } else {
            stmt = conn.createStatement(resultSetScrollType, ResultSet.CONCUR_READ_ONLY);
        }
        return stmt;
    }

    private boolean executeSQLStatement(Statement stmt, String sql) throws SQLException {
        LOGGER.log(Level.FINE, "Statement: {0}", sql); // NOI18N
        dataView.setInfoStatusText(NbBundle.getMessage(SQLExecutionHelper.class, "LBL_sql_executestmt") + sql);

        long startTime = System.currentTimeMillis();
        boolean isResultSet = false;
        if (stmt instanceof PreparedStatement) {
            isResultSet = ((PreparedStatement) stmt).execute();
        } else {
            try {
                isResultSet = stmt.execute(sql);
            } catch (NullPointerException ex) {
                LOGGER.log(Level.SEVERE, "Failed to execute SQL Statement [{0}], cause: {1}", new Object[] {sql, ex});
                throw new SQLException(ex);
            } catch (SQLException sqlExc) {
                LOGGER.log(Level.SEVERE, "Failed to execute SQL Statement [{0}], cause: {1}", new Object[]{sql, sqlExc});
                throw sqlExc;
            }
        }

        long executionTime = System.currentTimeMillis() - startTime;
        synchronized (dataView) {
            dataView.setUpdateCount(stmt.getUpdateCount());
            dataView.setExecutionTime(executionTime);
        }
        return isResultSet;
    }

    private boolean executePreparedStatement(PreparedStatement stmt) throws SQLException {
        long startTime = System.currentTimeMillis();
        boolean isResultSet = stmt.execute();

        long executionTime = System.currentTimeMillis() - startTime;
        String execTimeStr = SQLExecutionHelper.millisecondsToSeconds(executionTime);
        dataView.setInfoStatusText(NbBundle.getMessage(SQLExecutionHelper.class, "MSG_execution_success", execTimeStr));

        synchronized (dataView) {
            dataView.setUpdateCount(stmt.getUpdateCount());
            dataView.setExecutionTime(executionTime);
        }
        return isResultSet;
    }

    private boolean isSelectStatement(String queryString) {
        return queryString.trim().toUpperCase().startsWith("SELECT") && queryString.trim().toUpperCase().indexOf("INTO") == -1; // NOI18N
    }

    static String millisecondsToSeconds(long ms) {
        NumberFormat fmt = NumberFormat.getInstance();
        fmt.setMaximumFractionDigits(3);
        return fmt.format(ms / 1000.0);
    }

    /**
     * Check whether the result set needs reloading. Some databases close result
     * streams when reading database metadata (e.g. Oracle DB and
     * DatabaseMetaData.getPrimaryKeys). If there are some streamed values, the
     * result set needs to be reloaded after meta data have been read. See
     * #179959.
     *
     * @return True if and only if the result set needs to be reloaded.
     */
    private boolean resultSetNeedsReloading(DataViewDBTable metadata) {
        if (!dataView.getDatabaseConnection().getDriverClass().contains(
                "oracle")) {                                            //NOI18N
            return false;
        }
        int colCnt = metadata.getColumnCount();
        for (int i = 0; i < colCnt; i++) {
            DBColumn column = metadata.getColumn(i);
            int jdbcType = column.getJdbcType();
            if (jdbcType == Types.LONGVARCHAR || jdbcType == Types.LONGNVARCHAR
                    || jdbcType == Types.LONGVARBINARY || jdbcType == Types.BLOB
                    || jdbcType == Types.CLOB || jdbcType == Types.NCLOB) {
                return true;
            }
        }
        return false;
    }

    /**
     * Guarded version of setFetchSize. See #227756.
     */
    private static void setFetchSize(Statement stmt, int fetchSize) {
        try {
            stmt.setFetchSize(fetchSize);
        } catch (SQLException e) {
            // ignore -  used only as a hint to the driver to optimize
            LOGGER.log(Level.INFO, "Unable to set Fetch size", e); // NOI18N
            // But try to reset to default behaviour
            try {
                stmt.setFetchSize(0);
            } catch (SQLException ex) {
            }
        }
    }
}
