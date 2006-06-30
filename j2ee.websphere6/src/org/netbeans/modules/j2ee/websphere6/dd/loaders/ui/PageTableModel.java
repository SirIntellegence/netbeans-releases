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
package org.netbeans.modules.j2ee.websphere6.dd.loaders.ui;

// Netbeans
import java.util.List;
import org.openide.util.NbBundle;
import org.netbeans.modules.j2ee.websphere6.dd.beans.*;
import org.netbeans.modules.schema2beans.*;
import org.netbeans.modules.xml.multiview.*;

public class PageTableModel extends org.netbeans.modules.j2ee.websphere6.dd.loaders.ui.InnerTableModel {

    private List children;
    private BaseBean parent;
    private static final String[] columnNames = {
        NbBundle.getMessage(PageTableModel.class,"TTL_PageId"),
        NbBundle.getMessage(PageTableModel.class,"TTL_PageName"),
        NbBundle.getMessage(PageTableModel.class,"TTL_PageUri")        
    };
    
    private static final int [] columnWidths= {200, 160, 180};
    
    public PageTableModel(XmlMultiViewDataSynchronizer synchronizer)    {
        super(synchronizer,columnNames,columnWidths);
    }
    
    
    
    protected String[] getColumnNames() {
        return columnNames;
    }
    
    protected BaseBean getParent() {
        return parent;
    }
    
    protected List getChildren() {
        return children;
    }
    
    public int getColumnCount() {
        return getColumnNames().length;
    }
    
    public int getRowCount() {
        if (children != null) {
            return (children.size());
        } else {
            return (0);
        }
    }
    
    
    public String getColumnName(int column) {
        return getColumnNames()[column];
    }
    
    public boolean isCellEditable(int row, int column) {
        return (false);
    }
    
    public int getRowWithValue(int column, Object value) {
        for(int row = 0; row < getRowCount(); row++) {
            Object obj = getValueAt(row, column);
            if (obj.equals(value)) {
                return (row);
            }
        }
        
        return (-1);
    }
    
    
    
    public void setData(BaseBean parent,BaseBean[] children) {
        this.parent = parent;
        this.children = new java.util.ArrayList();
        if (children==null) return;
        for(int i=0;i<children.length;i++)
            this.children.add(children[i]);
        fireTableDataChanged();
    }
    
    
    public void setValueAt(Object value, int row, int column) {
        PageType page = (PageType)getChildren().get(row);
        
        if (column == 0) page.setXmiId((String)value);
        else if(column==1) page.setName((String)value);
        else page.setUri((String)value);
    }
    
    
    public Object getValueAt(int row, int column) {
        PageType page = (PageType)getChildren().get(row);
        
        if (column == 0) return page.getXmiId();
        else if(column == 1) return page.getName();
        else if(column == 2) return page.getUri();
        return null;
    }
    
    public BaseBean addRow(Object[] values) {
        //try {
            PageType page = new PageType();
            page.setXmiId((String)values[0]);
            page.setName((String)values[1]);
            page.setUri((String)values[2]);
            ((MarkupLanguagesType)getParent()).addPages(page);
            getChildren().add(page);
            fireTableRowsInserted(getRowCount() - 1, getRowCount() - 1);
            return page;
        //} //catch (ClassNotFoundException ex) {}
        
        //return null;
    }
    
    public int addRow() {
        return -1;
    }
    
    
    public void editRow(int row, Object[] values) {
        PageType page = (PageType)getChildren().get(row);
        page.setXmiId((String)values[0]);
        page.setName((String)values[1]);
        page.setUri((String)values[2]);
        fireTableRowsUpdated(row,row);
    }
    
    public void removeRow(int row) {
        ((MarkupLanguagesType)getParent()).removePages((PageType)getChildren().get(row));
        getChildren().remove(row);
        fireTableRowsDeleted(row, row);
        
    }
}