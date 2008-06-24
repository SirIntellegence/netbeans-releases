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
import java.util.List;
import javax.swing.JButton;
import java.util.Iterator;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.JPanel;
import org.netbeans.api.db.explorer.DatabaseConnection;
import org.openide.awt.StatusDisplayer;

/**
 * DataView to show data of a given sql query string, provides static method to create 
 * the DataView Pannel from a given sql query string and a connection. 
 *
 * @author Ahimanikya Satapathy
 */
public class DataView extends JPanel {

    private static Logger mLogger = Logger.getLogger(DataView.class.getName());
    public static final int VERTICAL_TOOLBAR = 0;
    public static final int HORIZONTAL_TOOLBAR = 1; // Default

    private DatabaseConnection dbConn;
    private List<String> errMessages = new ArrayList<String>();
    private String queryString; // Once Set, Data View assumes it will never change

    DataViewDBTable tblMeta;
    private SQLStatementGenerator stmtGenerator;
    private SQLExecutionHelper execHelper;
    private DataViewPageContext dataPage;
    private DataViewUI dataViewUI;
    private int toolbarType = HORIZONTAL_TOOLBAR;

    /**
     * Create and populate a DataView Object. Populates 1st data page of default size.
     * The caller can run this in background thread and then create the GUI components 
     * to render to render the DataView by calling DataView.createComponent()
     * 
     * @param dbConn instance of DBExplorer DatabaseConnection 
     * @param queryString SQL query string 
     * @return a new DataView instance
     */
    public static DataView create(DatabaseConnection dbConn, String queryString) {
        assert dbConn!= null : "DatabaseConnection can't be null";
        assert  isSelectStatement(queryString) : "Invalid query statement";
                
        DataView dv = new DataView();
        dv.dbConn = dbConn;
        dv.queryString = queryString.trim();
        dv.toolbarType = HORIZONTAL_TOOLBAR;
        try {
            dv.dataPage = new DataViewPageContext();
            dv.execHelper = new SQLExecutionHelper(dv, dbConn);
            SQLExecutionHelper.initialDataLoad(dv, dbConn, dv.execHelper);
            dv.stmtGenerator = new SQLStatementGenerator(dv.tblMeta);
        } catch (Exception ex) {
            dv.setErrorStatusText(ex.getMessage());
        }
        return dv;
    }

    /**
     * Create the UI component and renders the data fetched from database on create()
     * 
     * @param dataView DataView Object created using create()
     * @return a JComponent that after rending the given dataview
     */
    public static JComponent createComponent(DataView dataView) {
        assert dataView != null : "Should have called create()";
        synchronized (dataView) {
            dataView.dataViewUI = new DataViewUI(dataView, dataView.toolbarType);
            dataView.setRowsInTableModel();
            dataView.dataViewUI.setEditable(dataView.tblMeta.hasOneTable());
            dataView.resetToolbar(dataView.hasException());
        }
        return dataView;
    }

    /**
     * Default is set to HORIZONTAL_TOOLBAR
     * 
     * @param toolbarType VERTICAL_TOOLBAR or HORIZONTAL_TOOLBAR
     */
    public void setToolbarType(int toolbarType) {
        this.toolbarType = toolbarType;
    }

    /**
     * Returns true if there were any expection in the last database call.
     * 
     * @return true if error occurred in last database call, false otherwise.
     */
    public boolean hasException() {
        return !errMessages.isEmpty();
    }

    /**
     * Returns iterator of a error messages of String type, if there were any 
     * expection in the last database call, empty otherwise
     * 
     * @return Iterator<String>
     */
    public Iterator<String> getExceptions() {
        return errMessages.iterator();
    }

    /**
     * Returns editing tool bar items.
     * 
     * @return an array of JButton
     */
    public JButton[] getVerticalToolBar() {
        return dataViewUI.getVerticalToolBar();
    }

    static boolean isSelectStatement(String queryString){
        return queryString.trim().toUpperCase().startsWith("SELECT");
    }
    
    void clearErrorMessages() {
        errMessages.clear();
    }

    DataViewDBTable getDataViewDBTable() {
        return tblMeta;
    }

    DataViewPageContext getDataViewPageContext() {
        return dataPage;
    }

    DatabaseConnection getDatabaseConnection() {
        return dbConn;
    }

    String getQueryString() {
        return queryString;
    }

    UpdatedRowContext getResultSetUpdatedRowContext() {
        return dataViewUI.getResultSetRowContext();
    }

    SQLExecutionHelper getSQLExecutionHelper() {
        return execHelper;
    }

    SQLStatementGenerator getSQLStatementGenerator() {
        return stmtGenerator;
    }

    void disableButtons() {
        assert dataViewUI != null : "Should have called createComponent()";
        if (dataViewUI != null) {
            dataViewUI.disableButtons();
        }
    }

    void setEditable(boolean editable) {
        synchronized (dataViewUI) {
            dataViewUI.setEditable(editable);
        }
    }

    boolean isEditable() {
        return dataViewUI.isEditable();
    }

    void setInfoStatusText(String statusText) {
        if (statusText != null) {
            StatusDisplayer.getDefault().setStatusText(statusText);
        }
    }

    void setErrorStatusText(String errorMsg) {
        if (errorMsg != null && !errorMsg.equals("")) {
            errMessages.add(errorMsg);
            StatusDisplayer.getDefault().setStatusText("ERROR: " + errorMsg);
            mLogger.severe(errorMsg);
        }
    }

    void resetToolbar(boolean wasError) {
        assert dataViewUI != null : "Should have called createComponent()";
        dataViewUI.resetToolbar(wasError);
    }

    void setRowsInTableModel() {
        assert dataViewUI != null : "Should have called createComponent()";
        assert dataPage != null : "Should have called create()";

        if (dataPage.getCurrentRows() != null) {
            dataViewUI.setDataRows(dataPage.getCurrentRows());
            dataViewUI.setTotalCount(dataPage.getTotalRows());
        }
    }

    private DataView() {
    }
}
