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
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2007 Sun
 * Microsystems, Inc. All Rights Reserved.
 */

 
package org.netbeans.modules.iep.editor.ps;

import org.netbeans.modules.iep.editor.designer.JTextFieldFilter;
import org.netbeans.modules.iep.editor.model.AttributeMetadata;
import org.netbeans.modules.iep.editor.model.Plan;
import org.netbeans.modules.iep.editor.model.Schema;
import org.netbeans.modules.iep.editor.share.SharedConstants;
import org.netbeans.modules.iep.editor.tcg.table.DefaultMoveableRowTableModel;
import org.netbeans.modules.iep.editor.tcg.table.MoveableRowTable;
import org.netbeans.modules.iep.editor.tcg.model.TcgComponent;
import org.netbeans.modules.iep.editor.tcg.util.ArrayHashMap;
import org.netbeans.modules.iep.editor.tcg.util.ListMap;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumnModel;
import org.openide.util.NbBundle;

/**
 * SelectPanel.java
 *
 * Created on November 1, 2006, 1:52 PM
 *
 * @author Bing Lu
 */
public class SelectPanel extends JPanel implements SharedConstants {
    private static final Logger mLog = Logger.getLogger(SelectPanel.class.getName());
    private static String[] SQL_TYPE_NAMES = new String[] {
//        SQL_TYPE_BIT,
//        SQL_TYPE_TINYINT,
//        SQL_TYPE_SMALLINT,
        SQL_TYPE_INTEGER,
        SQL_TYPE_BIGINT,
//       SQL_TYPE_REAL,
//        SQL_TYPE_FLOAT,
        SQL_TYPE_DOUBLE,
//        SQL_TYPE_DECIMAL,
//        SQL_TYPE_NUMERIC,
//        SQL_TYPE_CHAR,
        SQL_TYPE_VARCHAR,
//        SQL_TYPE_LONGVARCHAR,
        SQL_TYPE_DATE,
        SQL_TYPE_TIME,
        SQL_TYPE_TIMESTAMP,
//        SQL_TYPE_BINARY,
//        SQL_TYPE_VARBINARY,
//        SQL_TYPE_LONGVARBINARY,
//        SQL_TYPE_BLOB,
//        SQL_TYPE_CLOB,
//        "ARRAY",
//        "REF",
//        "STRUCT",
    };
    
    private static Set QUANTITY_TYPES = new HashSet();
    static {
//        QUANTITY_TYPES.add(SQL_TYPE_TINYINT);
//        QUANTITY_TYPES.add(SQL_TYPE_SMALLINT);
        QUANTITY_TYPES.add(SQL_TYPE_INTEGER);
        QUANTITY_TYPES.add(SQL_TYPE_BIGINT);
//        QUANTITY_TYPES.add(SQL_TYPE_REAL);
//        QUANTITY_TYPES.add(SQL_TYPE_FLOAT);
        QUANTITY_TYPES.add(SQL_TYPE_DOUBLE);
        //QUANTITY_TYPES.add(SQL_TYPE_DECIMAL);
//        QUANTITY_TYPES.add(SQL_TYPE_NUMERIC);
        QUANTITY_TYPES.add(SQL_TYPE_DATE);
        QUANTITY_TYPES.add(SQL_TYPE_TIMESTAMP);
        QUANTITY_TYPES.add(SQL_TYPE_TIME);
    }
    
    private static int mAcceptableActions = DnDConstants.ACTION_COPY;
    private static DefaultCellEditor mCellEditorNumeric;
    private static DefaultCellEditor mCellEditorANU;
    private static DefaultCellEditor mCellEditorAny;
    private static DefaultCellEditor mCellEditorSqlType;
    private static JTextField mTextFieldNumeric;
    private static JTextField mTextFieldANU;
    private static JTextField mTextFieldAny;
    private static SmartTextField mTextFieldExpression;
    private static JComboBox mComboBoxSqlType;
    private static Vector mSqlType;
    
    
    static  {
        mTextFieldANU = new JTextField();
        mTextFieldANU.setDocument(JTextFieldFilter.newAlphaNumericUnderscore());
        mCellEditorANU = new DefaultCellEditor(mTextFieldANU);
        
        mTextFieldNumeric = new JTextField();
        mTextFieldNumeric.setDocument(JTextFieldFilter.newNumeric());
        mCellEditorNumeric = new DefaultCellEditor(mTextFieldNumeric);
        
        mTextFieldAny = new JTextField();
        mCellEditorAny = new DefaultCellEditor(mTextFieldAny);
        
        boolean truncateColumn = false;
        mTextFieldExpression = new SmartTextField(truncateColumn);
        
        mSqlType = new Vector();
        mSqlType.add("");
        for(int i = 0; i < SQL_TYPE_NAMES.length; i++)
            mSqlType.add(SQL_TYPE_NAMES[i]);
        
        mComboBoxSqlType = new JComboBox(mSqlType);
        mCellEditorSqlType = new DefaultCellEditor(mComboBoxSqlType);
    }
    
    private Plan mPlan;
    private TcgComponent mComponent;
    private DefaultMoveableRowTableModel mTableModel;
    private MoveableRowTable mTable;
    private SmartCellEditor mCellEditorExpression;
    private ListMap mColumnMetadataTable;
    private DropTarget mDropTarget;
    private boolean mReadOnly;
    private boolean mHasExpressionColumn;
    private int mNameCol;
    private SelectPanelTableCellRenderer  spTCRenderer;
    
    public SelectPanel(Plan plan, TcgComponent component) {
        mPlan = plan;
        mComponent = component;
        try {
            boolean isSchemaOwner = component.getProperty(IS_SCHEMA_OWNER_KEY).getBoolValue();
            String inputType = component.getProperty(INPUT_TYPE_KEY).getStringValue();
            mReadOnly = !isSchemaOwner;
            mHasExpressionColumn = isSchemaOwner && !inputType.equals(IO_TYPE_NONE);
            mNameCol = mHasExpressionColumn? 1 : 0;
        } catch (Exception e) {
            mHasExpressionColumn = false;
            mNameCol = 0;
        }
        
        initAttributeMetadataTables();
        mCellEditorExpression = new SmartCellEditor(mTextFieldExpression);
        initComponents();
    }
    
    private void initAttributeMetadataTables() {
        String opName = "";
        mColumnMetadataTable = new ArrayHashMap();
        try {
            opName = mComponent.getProperty(NAME_KEY).getStringValue();
            List inputIdList = mComponent.getProperty(INPUT_ID_LIST_KEY).getListValue();
            for(int i = 0,I = inputIdList.size(); i < I; i++) {
                String id = (String)inputIdList.get(i);
                TcgComponent input = mPlan.getOperatorById(id);
                if(input != null) {
                    String inputName = input.getProperty(NAME_KEY).getStringValue();
                    ListMap lm = new ArrayHashMap();
                    mColumnMetadataTable.put(inputName, lm);
                    String outputSchemaId = input.getProperty(OUTPUT_SCHEMA_ID_KEY).getStringValue();
                    Schema schema = mPlan.getSchema(outputSchemaId);
                    if(schema != null) {
                        for(int j = 0, J = schema.getAttributeCount(); j < J; j++) {
                            AttributeMetadata cm = schema.getAttributeMetadata(j);
                            lm.put(cm.getAttributeName(), cm);
                        }
                    }
                }
            }
            List staticInputIdList = mComponent.getProperty(STATIC_INPUT_ID_LIST_KEY).getListValue();
            for(int i = 0, I = staticInputIdList.size(); i < I; i++) {
                String id = (String)staticInputIdList.get(i);
                TcgComponent input = mPlan.getOperatorById(id);
                if(input != null) {
                    String inputName = input.getProperty(NAME_KEY).getStringValue();
                    ListMap lm = new ArrayHashMap();
                    mColumnMetadataTable.put(inputName, lm);
                    String outputSchemaId = input.getProperty(OUTPUT_SCHEMA_ID_KEY).getStringValue();
                    Schema schema = mPlan.getSchema(outputSchemaId);
                    if(schema != null) {
                        for(int j = 0, J = schema.getAttributeCount(); j < J; j++) {
                            AttributeMetadata cm = schema.getAttributeMetadata(j);
                            lm.put(cm.getAttributeName(), cm);
                        }
                    }
                }
            }
        } catch(Exception e) {
            mLog.log(Level.SEVERE,
                    NbBundle.getMessage(InputSchemaTreeModel.class,
                    "SelectPanel.FAIL_TO_BUILD_COLUMN_TABLES_FOR_OPERATOR",
                    opName),
                    e);
        }
        
    }
    
    private void initComponents() {
        setLayout(new BorderLayout());
        if (mHasExpressionColumn) {
            setBorder(BorderFactory.createTitledBorder("SELECT"));
        }
        JPanel topPane = new JPanel();
        topPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        topPane.setLayout(new BorderLayout(5, 5));
        add(topPane, BorderLayout.CENTER);
        JPanel pane = new JPanel();
        pane.setLayout(new BorderLayout(5, 5));
        mTableModel = new DefaultMoveableRowTableModel();
        if (mReadOnly) {
            mTable = new MoveableRowTable(mTableModel) {
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
                public void dragGestureRecognized(DragGestureEvent dge) {
                    return;
                }
            };
        } else {
            mTable = new MoveableRowTable(mTableModel) {
              public boolean isCellEditable(int row, int column) {
                  //Working on this code fragment.
                   /* if(column == 2 ) { // to ensure non editability of scale and size column in case of non varchar.
                        if(mTableModel.getValueAt(row,1) != null && mTableModel.getValueAt(row,1).toString().equals(SQL_TYPE_VARCHAR)) {
                            return true;
                        } else {
                            return false;
                        } 
                    }
                    */ 
                    return true;
                    }
                     
                   
            };
        }
        mDropTarget = new DropTarget(mTable, new MyDropTargetAdapter());
        Vector data = new Vector();
        try {
            String schemaId = mComponent.getProperty(OUTPUT_SCHEMA_ID_KEY).getStringValue();
            if(!schemaId.trim().equals("")) {
                Schema schema = mPlan.getSchema(schemaId);
                java.util.List attributeMetadataList = new ArrayList(schema.getAttributeMetadataAsList());
                if (mHasExpressionColumn) {
                    java.util.List fromColumnList = mComponent.getProperty(FROM_COLUMN_LIST_KEY).getListValue();
                    for(int i = 0, I = attributeMetadataList.size(), j = 0, J = fromColumnList.size(); i < I && j < J; i+=5, j++) {
                        Vector r = new Vector();
                        r.add(fromColumnList.get(j));
                        r.add(attributeMetadataList.get(i));
                        if(i + 1 < attributeMetadataList.size()) {
                            r.add(attributeMetadataList.get(i + 1));
                        } else {
                            r.add("");
                        }
                        if(i + 2 < attributeMetadataList.size()) {
                            r.add(attributeMetadataList.get(i + 2));
                        } else {
                            r.add("");
                        }
                        if(i + 3 < attributeMetadataList.size()) {
                            r.add(attributeMetadataList.get(i + 3));
                        } else {
                            r.add("");
                        }
                        if(i + 4 < attributeMetadataList.size()) {
                            r.add(attributeMetadataList.get(i + 4));
                        } else {
                            r.add("");
                        }
                        data.add(r);
                    }
                } else {
                    for(int i = 0, I = attributeMetadataList.size(); i < I; i+=5) {
                        Vector r = new Vector();
                        r.add(attributeMetadataList.get(i));
                        if(i + 1 < attributeMetadataList.size()) {
                            r.add(attributeMetadataList.get(i + 1));
                        } else {
                            r.add("");
                        }
                        if(i + 2 < attributeMetadataList.size()) {
                            r.add(attributeMetadataList.get(i + 2));
                        } else {
                            r.add("");
                        }
                        if(i + 3 < attributeMetadataList.size()) {
                            r.add(attributeMetadataList.get(i + 3));
                        } else {
                            r.add("");
                        }
                        if(i + 4 < attributeMetadataList.size()) {
                            r.add(attributeMetadataList.get(i + 4));
                        } else {
                            r.add("");
                        }
                        data.add(r);
                    }
                }
                if(data.size() == 0) {
                    Vector r = new Vector();
                    if (mHasExpressionColumn) {
                        r.add("");
                    }
                    r.add("");
                    r.add("");
                    r.add("");
                    r.add("");
                    r.add("");
                    data.add(r);
                }
            } else {
                Vector r = new Vector();
                if (mHasExpressionColumn) {
                    r.add("");
                }
                r.add("");
                r.add("");
                r.add("");
                r.add("");
                r.add("");
                data.add(r);
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        Vector colTitle = new Vector();
        if (mHasExpressionColumn) {
            colTitle.add(NbBundle.getMessage(SelectPanel.class, "SelectPanel.EXPRESSION"));
        }
        colTitle.add(NbBundle.getMessage(SelectPanel.class, "SelectPanel.ATTRIBUTE_NAME"));
        colTitle.add(NbBundle.getMessage(SelectPanel.class, "SelectPanel.DATA_TYPE"));
        colTitle.add(NbBundle.getMessage(SelectPanel.class, "SelectPanel.SIZE"));
        colTitle.add(NbBundle.getMessage(SelectPanel.class, "SelectPanel.SCALE"));
        colTitle.add(NbBundle.getMessage(SelectPanel.class, "SelectPanel.COMMENT"));
        mTableModel.setDataVector(data, colTitle);
        TableColumnModel tcm = mTable.getColumnModel();
        spTCRenderer = new SelectPanelTableCellRenderer();
        try {
            if (mHasExpressionColumn) {
                mCellEditorExpression.addCellEditorListener(new ExpressionCellEditorListener());
                tcm.getColumn(0).setCellEditor(mCellEditorExpression);
                tcm.getColumn(0).setPreferredWidth(180);
                tcm.getColumn(0).setCellRenderer(spTCRenderer);
                
            }
            tcm.getColumn(mNameCol).setCellEditor(mCellEditorANU);
            tcm.getColumn(mNameCol + 1).setCellEditor(mCellEditorSqlType);
            //mComboBoxSqlType.addActionListener(new SQLTypeComboBoxActionListener());
            tcm.getColumn(mNameCol + 2).setCellEditor(mCellEditorNumeric);
            tcm.getColumn(mNameCol + 3).setCellEditor(mCellEditorNumeric);
            tcm.getColumn(mNameCol + 4).setCellEditor(mCellEditorAny);
            
            // setting up renderer
            tcm.getColumn(mNameCol).setCellRenderer(spTCRenderer);
            tcm.getColumn(mNameCol + 1).setCellRenderer(spTCRenderer);
            tcm.getColumn(mNameCol + 2).setCellRenderer(spTCRenderer);
            tcm.getColumn(mNameCol + 3).setCellRenderer(spTCRenderer);
            tcm.getColumn(mNameCol + 4).setCellRenderer(spTCRenderer);
            
            
        } catch(Exception ex) {
            ex.printStackTrace();
        }
        mTable.setPreferredScrollableViewportSize(new Dimension(700, 300));
        pane.add(new JScrollPane(mTable), BorderLayout.CENTER);
        if (!mReadOnly) {
            JPanel cp = new JPanel();
            cp.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            cp.setLayout(new GridLayout(1, 5, 10, 10));
            String lbl = NbBundle.getMessage(SelectPanel.class, "SelectPanel.ADD_ATTRIBUTE");
            JButton btnAdd = new JButton(lbl);
            btnAdd.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    mTableModel.addRow(mHasExpressionColumn?
                        new Object[] {"", "", "", "", "", ""} :
                        new Object[] {"", "", "", "", ""});
                    int rcount = mTable.getRowCount();
                    mTable.setRowSelectionInterval(rcount - 1, rcount - 1);
                }
            });
            lbl = NbBundle.getMessage(SelectPanel.class, "SelectPanel.DELETE");
            JButton btnDel = new JButton(lbl);
            btnDel.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    int r[] = mTable.getSelectedRows();
                    int firstSelectedRow = 0;
                    if(r != null && r.length > 0) {
                        Arrays.sort(r);
                        firstSelectedRow = r[0];
                        for(int i = r.length - 1; i >= 0; i--)
                            mTableModel.removeRow(r[i]);
                        
                    }
                    int rcount = mTable.getRowCount();
                    if(rcount > 0) {
                        if(firstSelectedRow < rcount) {
                            mTable.setRowSelectionInterval(firstSelectedRow, firstSelectedRow);
                        } else {
                            if(firstSelectedRow == rcount) {
                                mTable.setRowSelectionInterval(firstSelectedRow - 1, firstSelectedRow - 1);
                            }
                        }
                    }
                }
            });
            lbl = NbBundle.getMessage(SelectPanel.class, "SelectPanel.MOVE_UP");
            JButton btnMoveUp = new JButton(lbl);
            btnMoveUp.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    int r = mTable.getSelectedRow();
                    if(r > 0) {
                        mTableModel.moveRow(r, r, r - 1);
                        mTable.setRowSelectionInterval(r - 1, r - 1);
                    }
                }
            });
            lbl = NbBundle.getMessage(SelectPanel.class, "SelectPanel.MOVE_DOWN");
            JButton btnMoveDown = new JButton(lbl);
            btnMoveDown.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    int r = mTable.getSelectedRow();
                    int rcount = mTable.getRowCount();
                    if(r >= 0 && rcount - 1 > r) {
                        mTableModel.moveRow(r, r, r + 1);
                        mTable.setRowSelectionInterval(r + 1, r + 1);
                    }
                }
            });
            Dimension s = btnAdd.getPreferredSize();
            int maxW = s.width;
            int maxH = s.height;
            s = btnDel.getPreferredSize();
            maxW = Math.max(s.width, maxW);
            maxH = Math.max(s.height, maxH);
            s = btnMoveUp.getPreferredSize();
            maxW = Math.max(s.width, maxW);
            maxH = Math.max(s.height, maxH);
            s = btnMoveDown.getPreferredSize();
            maxW = Math.max(s.width, maxW);
            maxH = Math.max(s.height, maxH);
            s = new Dimension(maxW, maxH);
            btnAdd.setPreferredSize(s);
            btnDel.setPreferredSize(s);
            btnMoveUp.setPreferredSize(s);
            btnMoveDown.setPreferredSize(s);
            cp.add(btnAdd);
            cp.add(btnDel);
            cp.add(btnMoveUp);
            cp.add(btnMoveDown);
            cp.add(Box.createHorizontalGlue());
            pane.add(cp, BorderLayout.SOUTH);
        }
        topPane.add(pane, BorderLayout.CENTER);
    }
    
    public List getAttributeMetadataAsList() {
        List attributeMetadataList = new ArrayList();
        Vector r = mTableModel.getDataVector();
        for (int i = 0, I = r.size(); i < I; i++) {
            Vector c = (Vector) r.elementAt(i);
            if (!(c.elementAt(mNameCol) == null) && !(c.elementAt(mNameCol).equals(""))) {
                attributeMetadataList.add(c.elementAt(mNameCol));
                attributeMetadataList.add(c.elementAt(mNameCol + 1));
                attributeMetadataList.add(c.elementAt(mNameCol + 2));
                attributeMetadataList.add(c.elementAt(mNameCol + 3));
                attributeMetadataList.add(c.elementAt(mNameCol + 4));
            }
        }
        return attributeMetadataList;
    }
    
    public boolean hasExpressionList() {
        return mHasExpressionColumn;
    }
    
    public List getExpressionList() {
        List expList = new ArrayList();
        if (!mHasExpressionColumn) {
            return expList;
        }
        Vector r = mTableModel.getDataVector();
        for (int i = 0, I = r.size(); i < I; i++) {
            Vector c = (Vector) r.elementAt(i);
            if (!(c.elementAt(1) == null) && !(c.elementAt(1).equals(""))) {
                expList.add(c.elementAt(0));
            }
        }
        return expList;
    }
    
    public List getToColumnList() {
        List toList = new ArrayList();
        if (!mHasExpressionColumn) {
            return toList;
        }
        Vector r = mTableModel.getDataVector();
        for (int i = 0, I = r.size(); i < I; i++) {
            Vector c = (Vector) r.elementAt(i);
            if (!(c.elementAt(1) == null) && !(c.elementAt(1).equals(""))) {
                toList.add(c.elementAt(1));
            }
        }
        return toList;
    }
    
    public boolean hasInput(String inputName) {
        return mColumnMetadataTable.containsKey(inputName);
    }
    
    public boolean hasInputAttribute(String inputAttributeName) {
        return getAttributeMetadata(inputAttributeName) != null;
    }
    
    /**
     * @param inputAttributeName input.attribute or attribute (when one and only one input)
     *
     * @return null if not found
     */
    public AttributeMetadata getAttributeMetadata(String inputAttributeName) {
        int index = inputAttributeName.indexOf(".");
        if (index < 1) {
            String attributeName = inputAttributeName;
            if (mColumnMetadataTable.size() != 1) {
                return null;
            }
            ListMap lm = (ListMap)mColumnMetadataTable.get(0);
            if (!lm.containsKey(attributeName)) {
                return null;
            }
            return (AttributeMetadata)lm.get(attributeName);
        }
        String inputName = inputAttributeName.substring(0, index);
        if (!mColumnMetadataTable.containsKey(inputName)) {
            return null;
        }
        String attributeName = inputAttributeName.substring(index + 1);
        ListMap lm = (ListMap)mColumnMetadataTable.get(inputName);
        if (!lm.containsKey(attributeName)) {
            return null;
        }
        return (AttributeMetadata)lm.get(attributeName);
    }
    
    public List getQuantityAttributeList() {
        List attributeList = new ArrayList();
        Vector r = mTableModel.getDataVector();
        for (int i = 0, I = r.size(); i < I; i++) {
            Vector c = (Vector) r.elementAt(i);
            String name = (String)c.elementAt(mNameCol);
            String type = (String)c.elementAt(mNameCol + 1);
            if ( type != null && QUANTITY_TYPES.contains(type)) {
                attributeList.add(name);
            }
        }
        return attributeList;
    }
    
    public void validateContent(PropertyChangeEvent evt) throws PropertyVetoException {
        if (mReadOnly) {
            return;
        }
        
        int rowCount = mTableModel.getRowCount();
        // at least one attribute must be defined
        if (rowCount < 1) {
            String msg = NbBundle.getMessage(SelectPanel.class,
                    "SelectPanel.AT_LEAST_ONE_ATTRIBUTE_MUST_BE_DEFINED");
            throw new PropertyVetoException(msg, evt);
        }
        
        if (mHasExpressionColumn) {
            // for each attribute: expression must be defined
            for (int i = 0; i < rowCount; i++) {
                String exp = (String)mTableModel.getValueAt(i, 0);
                if (exp == null || exp.trim().equals("")) {
                    String msg = NbBundle.getMessage(SelectPanel.class,
                            "SelectPanel.EXPRESSION_MUST_BE_SPECIFIED");
                    throw new PropertyVetoException(msg, evt);
                }
            }
        }
        
        // for each attribute: name and type must be defined
        for (int i = 0; i < rowCount; i++) {
            String colName = (String)mTableModel.getValueAt(i, mNameCol);
            if (colName == null || colName.trim().equals("")) {
                String msg = NbBundle.getMessage(SelectPanel.class,
                        "SelectPanel.ATTRIBUTE_NAME_MUST_BE_DEFINED");
                throw new PropertyVetoException(msg, evt);
            }
            String colType = (String)mTableModel.getValueAt(i, mNameCol + 1);
            if (colType == null || colType.trim().equals("")) {
                String msg = NbBundle.getMessage(SelectPanel.class,
                        "SelectPanel.ATTRIBUTE_TYPE_MUST_BE_DEFINED");
                throw new PropertyVetoException(msg, evt);
            }
        }
        
        // attribute name must be unique
        Set nameSet = new HashSet();
        for (int i = 0; i < rowCount; i++) {
            String colName = (String)mTableModel.getValueAt(i, mNameCol);
            if (nameSet.contains(colName)) {
                String msg = NbBundle.getMessage(SelectPanel.class,
                        "SelectPanel.ATTRIBUTE_NAME_MUST_BE_UNIQUE");
                throw new PropertyVetoException(msg, evt);
            }
            nameSet.add(colName);
        }
    }
    
    class MyDropTargetAdapter extends DropTargetAdapter {
        public void dragEnter(DropTargetDragEvent e) {
            if(!isDragAcceptable(e)) {
                e.rejectDrag();
                return;
            }
            SelectPanel.this.mTable.grabFocus();
            int row = SelectPanel.this.mTable.rowAtPoint(e.getLocation());
            int column =  SelectPanel.this.mTable.columnAtPoint(e.getLocation());
            if (column == 0 && row >= 0) {
                SelectPanel.this.mTable.editCellAt(row, column);
                e.acceptDrag(mAcceptableActions);
                return;
            }
            e.rejectDrag();
        }
        
        public void dragOver(DropTargetDragEvent e) {
            // When dragOver is called, the mouse is not in the previous
            // activated smart text-field
            if(!isDragAcceptable(e)) {
                e.rejectDrag();
                return;
            }
            SelectPanel.this.mTable.grabFocus();
            // Retrieve the working cell editor.
            TableCellEditor editor = SelectPanel.this.mTable.getCellEditor();
            if (editor != null) {
                editor.stopCellEditing();
            }
            int row = SelectPanel.this.mTable.rowAtPoint(e.getLocation());
            int column =  SelectPanel.this.mTable.columnAtPoint(e.getLocation());
            if (column == 0 && row >= 0) {
                SelectPanel.this.mTable.editCellAt(row, column);
                e.acceptDrag(mAcceptableActions);
                return;
            }
            e.rejectDrag();
        }
        
        public void drop(DropTargetDropEvent e) {
            // Never happen
        }
        
        public void dropActionChanged(DropTargetDragEvent e) {
            if(!isDragAcceptable(e)) {
                e.rejectDrag();
                return;
            } else {
                e.acceptDrag(mAcceptableActions);
                return;
            }
        }
        
        private boolean isDragAcceptable(DropTargetDragEvent e) {
            return e.isDataFlavorSupported(AttributeInfoDataFlavor.ATTRIBUTE_INFO_FLAVOR);
        }
    }
    
    class SmartCellEditor extends DefaultCellEditor {
        int mRow;
        int mColumn;
        
        public SmartCellEditor(SmartTextField stf) {
            super(stf);
        }
        
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            mRow = row;
            mColumn = column;
            return super.getTableCellEditorComponent(table, value, isSelected, row, column);
        }
    }
    
  /*  class SQLTypeComboBoxActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
        JComboBox cb = (JComboBox)e.getSource();
        String selectedSQLType = (String)cb.getSelectedItem();
        int r = SelectPanel.this.mTable.getSelectedRow();
        int c = 3;
        if(selectedSQLType.equalsIgnoreCase(SQL_TYPE_VARCHAR)) {
            SelectPanel.this.mTableModel.setValueAt("16",r,2);
        } else {
            SelectPanel.this.mTableModel.setValueAt("",r,2);
        }
        
    }

    }
   **/
    
    class ExpressionCellEditorListener implements CellEditorListener {
        // This tells the listeners the editor has canceled editing
        public void editingCanceled(ChangeEvent e) {
            return;
        }
        // This tells the listeners the editor has ended editing
        public void editingStopped(ChangeEvent e) {
            String opName = "";
            String columnName = "";
            try {
                opName = SelectPanel.this.mComponent.getProperty(NAME_KEY).getStringValue();
                int row = SelectPanel.this.mCellEditorExpression.mRow;
                int column = SelectPanel.this.mCellEditorExpression.mColumn;
                String exp = (String)SelectPanel.this.mTable.getValueAt(row, 0);
                String name = (String)SelectPanel.this.mTable.getValueAt(row, 1);
                String type = (String)SelectPanel.this.mTable.getValueAt(row, 2);
                String size = (String)SelectPanel.this.mTable.getValueAt(row, 3);
                String scale = (String)SelectPanel.this.mTable.getValueAt(row, 4);
                if (!(name.trim().equals("") && type.trim().equals("") && size.trim().equals("") && scale.trim().equals(""))) {
                    return;
                }
                exp = exp.trim();
                if (exp.equals("")) {
                    return;
                }
                AttributeMetadata cm = SelectPanel.this.getAttributeMetadata(exp);
                if (cm == null) {
                    return;
                }
                SelectPanel.this.mTable.setValueAt(cm.getAttributeName(), row, 1);
                SelectPanel.this.mTable.setValueAt(cm.getAttributeType(), row, 2);
                SelectPanel.this.mTable.setValueAt(cm.getAttributeSize(), row, 3);
                SelectPanel.this.mTable.setValueAt(cm.getAttributeScale(), row, 4);
            } catch (Exception ex) {
                mLog.log(Level.WARNING,
                        NbBundle.getMessage(SelectPanel.class,
                        "SelectPanel.FAIL_TO_AUTOFILL_COLUMN_METADATA_FOR_COLUMN_OF_OPERATOR",
                        columnName,
                        opName),
                        ex);
            }
            
        }
        
    }
    
}