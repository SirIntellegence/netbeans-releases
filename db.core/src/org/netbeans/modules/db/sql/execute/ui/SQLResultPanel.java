/*
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at http://www.netbeans.org/cddl.html
 * or http://www.netbeans.org/cddl.txt.
 *
 * When distributing Covered Code, include this CDDL Header Notice in each file
 * and include the License file at http://www.netbeans.org/cddl.txt.
 * If applicable, add the following below the CDDL Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 */

package org.netbeans.modules.db.sql.execute.ui;

import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseEvent;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.DateFormat;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import org.netbeans.modules.db.sql.execute.NullValue;
import org.openide.util.Lookup;
import org.netbeans.modules.db.sql.execute.SQLExecutionResults;
import org.openide.util.datatransfer.ExClipboard;

/**
 *
 * @author  Andrei Badea
 */
public class SQLResultPanel extends javax.swing.JPanel {
    
    private SQLExecutionResults executionResults;
    
    public SQLResultPanel() {
        initComponents();
    }
    
    public void setModel(SQLResultPanelModel model) {
        TableModel resultSetModel = null;
        
        if (model != null) {
            if (model.getResultSetModel() != null) {
                resultSetModel = model.getResultSetModel();
            } else {
                resultSetModel = new DefaultTableModel(0, 0);
            }
        } else {
            resultSetModel = new DefaultTableModel(0, 0);
        }
        
        resultTable.setModel(resultSetModel);
    }
    
    private void setClipboard(String contents) {
        ExClipboard clipboard = (ExClipboard) Lookup.getDefault().lookup (ExClipboard.class);
        StringSelection strSel = new StringSelection(contents);
        clipboard.setContents(strSel, strSel);        
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
        tablePopupMenu = new javax.swing.JPopupMenu();
        copyCellValueMenuItem = new javax.swing.JMenuItem();
        copyRowValuesMenuItem = new javax.swing.JMenuItem();
        resultScrollPane = new javax.swing.JScrollPane();
        resultTable = new SQLResultTable();

        copyCellValueMenuItem.setText(org.openide.util.NbBundle.getMessage(SQLResultPanel.class, "LBL_CopyCellValue"));
        copyCellValueMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                copyCellValueMenuItemActionPerformed(evt);
            }
        });

        tablePopupMenu.add(copyCellValueMenuItem);
        copyCellValueMenuItem.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(SQLResultPanel.class, "ACSD_CopyCellValue"));

        copyRowValuesMenuItem.setText(org.openide.util.NbBundle.getMessage(SQLResultPanel.class, "LBL_CopyRowValues"));
        copyRowValuesMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                copyRowValuesMenuItemActionPerformed(evt);
            }
        });

        tablePopupMenu.add(copyRowValuesMenuItem);
        copyRowValuesMenuItem.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(SQLResultPanel.class, "ACSD_CopyRowValues"));

        setLayout(new java.awt.BorderLayout());

        resultScrollPane.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        resultScrollPane.getViewport().setBackground(UIManager.getDefaults().getColor("Table.background"));
        resultTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {

            }
        ));
        resultTable.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_OFF);
        resultTable.setOpaque(false);
        resultTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                resultTableMouseReleased(evt);
            }
        });

        resultScrollPane.setViewportView(resultTable);

        add(resultScrollPane, java.awt.BorderLayout.CENTER);

    }// </editor-fold>//GEN-END:initComponents

    private void copyRowValuesMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_copyRowValuesMenuItemActionPerformed
        int[] rows = resultTable.getSelectedRows();
        StringBuffer output = new StringBuffer();
        for (int i = 0; i < rows.length; i++) {
            for (int col = 0; col < resultTable.getColumnCount(); col++) {
                if (col > 0) {
                    output.append('\t');
                }
                Object o = resultTable.getValueAt(rows[i], col);
                output.append(o.toString());
            }
            output.append('\n');
        }
        setClipboard(output.toString());
    }//GEN-LAST:event_copyRowValuesMenuItemActionPerformed

    private void copyCellValueMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_copyCellValueMenuItemActionPerformed
        Object o = resultTable.getValueAt(resultTable.getSelectedRow(), resultTable.getSelectedColumn());
        setClipboard(o.toString());
    }//GEN-LAST:event_copyCellValueMenuItemActionPerformed

    private void resultTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_resultTableMouseReleased
        if (evt.getButton() != MouseEvent.BUTTON3) {
            return;
        }
        int row = resultTable.rowAtPoint(evt.getPoint());
        int column = resultTable.columnAtPoint(evt.getPoint());
        boolean inSelection = false;
        int[] rows = resultTable.getSelectedRows();
        for (int i = 0; i < rows.length; i++) {
            if (rows[i] == row) {
                inSelection = true;
                break;
            }
        }
        if (!inSelection) {
            resultTable.changeSelection (row, column, false, false);
        }
        tablePopupMenu.show(resultTable, evt.getX(), evt.getY());
    }//GEN-LAST:event_resultTableMouseReleased
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem copyCellValueMenuItem;
    private javax.swing.JMenuItem copyRowValuesMenuItem;
    private javax.swing.JScrollPane resultScrollPane;
    private javax.swing.JTable resultTable;
    private javax.swing.JPopupMenu tablePopupMenu;
    // End of variables declaration//GEN-END:variables

    static final class SQLResultTable extends JTable {
        
        private final NullRenderer nullRenderer = new NullRenderer();
        
        public SQLResultTable() {
            // issue 70521: rendering java.sql.Timestamp as date and time
            setDefaultRenderer(Timestamp.class, new DateTimeRenderer());
            // issue 72607: rendering java.sql.Time as time
            setDefaultRenderer(Time.class, new TimeRenderer());
        }
        
        /**
         * Overrinding in order to provide a valid renderer for NullValue.
         * NullValue can appear in any column and causes formatting exceptions.
         * See issue 62622.
         */
        public TableCellRenderer getCellRenderer(int row, int column) {
            Object value = getValueAt(row, column);
            if (value instanceof NullValue) {
                return nullRenderer;
            } else {
                return super.getCellRenderer(row, column);
            }
        }
        
        /**
         * Renderer which renders both the date and time part of a Date.
         */
        private static final class DateTimeRenderer extends DefaultTableCellRenderer.UIResource {
            
            DateFormat formatter;
            
            public DateTimeRenderer() { 
                super(); 
            }

            public void setValue(Object value) {
                if (formatter == null) {
                    formatter = DateFormat.getDateTimeInstance();
                }
                setText((value == null) ? "" : formatter.format(value)); // NOI18N
            }
        }
        
        private static final class TimeRenderer extends DefaultTableCellRenderer.UIResource {
            
            DateFormat formatter;
            
            public TimeRenderer() {
                super();
            }
            
            public void setValue(Object value) {
                if (formatter == null) {
                    formatter = DateFormat.getTimeInstance();
                }
                setText((value == null) ? "" : formatter.format(value)); // NOI18N
            }
        }
        
        private static final class NullRenderer extends DefaultTableCellRenderer.UIResource {
            
            public void setValue(Object value) {
                setText(((NullValue)value).toString());
            }
        }
    }
}