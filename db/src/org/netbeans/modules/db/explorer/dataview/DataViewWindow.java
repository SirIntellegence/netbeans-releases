/*
 *                 Sun Public License Notice
 *
 * The contents of this file are subject to the Sun Public License
 * Version 1.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://www.sun.com/
 *
 * The Original Code is NetBeans. The Initial Developer of the Original
 * Code is Sun Microsystems, Inc. Portions Copyright 1997-2003 Sun
 * Microsystems, Inc. All Rights Reserved.
 */

package org.netbeans.modules.db.explorer.dataview;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.ObjectStreamException;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

import java.text.MessageFormat;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;

import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.SplittedPanel;
import org.openide.nodes.Node;
import org.openide.nodes.NodeTransfer;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.openide.util.datatransfer.ExTransferable;
import org.openide.util.datatransfer.MultiTransferObject;
import org.openide.windows.TopComponent;

import org.netbeans.modules.db.DatabaseException;
import org.netbeans.modules.db.explorer.infos.ColumnNodeInfo;
import org.netbeans.modules.db.explorer.infos.DatabaseNodeInfo;
import org.netbeans.modules.db.explorer.nodes.ConnectionNode;
import org.netbeans.modules.db.explorer.nodes.RootNode;

public class DataViewWindow extends TopComponent {
    private JTextArea queryarea;
    private JTable jtable;
    private DataModel dbadaptor;
    private JComboBox rcmdscombo;
    private JLabel status;
    private ResourceBundle bundle;
    private Node node;

    static final long serialVersionUID = 6855188441469780252L;

    public DataViewWindow(DatabaseNodeInfo info, String query) throws SQLException {
        node = info.getNode();

        try {
            bundle = NbBundle.getBundle("org.netbeans.modules.db.resources.Bundle"); //NOI18N

            this.getAccessibleContext().setAccessibleDescription(bundle.getString("ACS_DataViewWindowA11yDesc")); //NOI18N

            Node tempNode = node;
            while(!(tempNode instanceof ConnectionNode))
                tempNode = tempNode.getParentNode();
            setName(bundle.getString("CommandEditorTitle") + " " + tempNode.getDisplayName()); //NOI18N
            GridBagLayout layout = new GridBagLayout();
            GridBagConstraints con = new GridBagConstraints ();
            setLayout (layout);

            // Data model
            dbadaptor = new DataModel(info);

            // Query area and button
            JPanel subpane = new JPanel();
            GridBagLayout sublayout = new GridBagLayout();
            GridBagConstraints subcon = new GridBagConstraints ();
            subpane.setLayout(sublayout);

            // query label
            subcon.fill = GridBagConstraints.HORIZONTAL;
            subcon.weightx = 0.0;
            subcon.weighty = 0.0;
            subcon.gridx = 0;
            subcon.gridy = 0;
            subcon.gridwidth = 3;
            subcon.insets = new Insets (0, 0, 5, 0);
            subcon.anchor = GridBagConstraints.SOUTH;
            JLabel queryLabel = new JLabel(bundle.getString("QueryLabel")); //NOI18N
            queryLabel.setDisplayedMnemonic(bundle.getString("QueryLabel_Mnemonic").charAt(0)); //NOI18N
            queryLabel.getAccessibleContext().setAccessibleDescription(bundle.getString("ACS_DataViewQueryLabelA11yDesc")); //NOI18N
            sublayout.setConstraints(queryLabel, subcon);
            subpane.add(queryLabel);

            // query area
            subcon.fill = GridBagConstraints.BOTH;
            subcon.weightx = 1.0;
            subcon.weighty = 1.0;
            subcon.gridx = 0;
            subcon.gridwidth = 3;
            subcon.gridy = 1;
            queryarea = new JTextArea(query, 3, 70);
            queryarea.setLineWrap(true);
            queryarea.setWrapStyleWord(true);
            queryarea.setDropTarget(new DropTarget(queryarea, new ViewDropTarget()));
            queryarea.getAccessibleContext().setAccessibleName(bundle.getString("ACS_DataViewTextAreaA11yName")); //NOI18N
            queryarea.getAccessibleContext().setAccessibleDescription(bundle.getString("ACS_DataViewTextAreaA11yDesc")); //NOI18N
            queryarea.setToolTipText(bundle.getString("ACS_DataViewTextAreaA11yDesc")); //NOI18N
            queryLabel.setLabelFor(queryarea);

            JScrollPane scrollpane = new JScrollPane(queryarea);
            subcon.insets = new Insets (0, 0, 5, 0);
            sublayout.setConstraints(scrollpane, subcon);
            subpane.add(scrollpane);

            // combo label
            subcon.fill = GridBagConstraints.HORIZONTAL;
            subcon.weightx = 0.0;
            subcon.weighty = 0.0;
            subcon.gridx = 0;
            subcon.gridy = 2;
            subcon.gridwidth = 1;
            subcon.insets = new Insets (0, 0, 5, 5);
            subcon.anchor = GridBagConstraints.CENTER;
            JLabel comboLabel = new JLabel(bundle.getString("HistoryLabel")); //NOI18N
            comboLabel.setDisplayedMnemonic(bundle.getString("HistoryLabel_Mnemonic").charAt(0)); //NOI18N
            comboLabel.getAccessibleContext().setAccessibleDescription(bundle.getString("ACS_DataViewHistoryLabelA11yDesc")); //NOI18N
            sublayout.setConstraints(comboLabel, subcon);
            subpane.add(comboLabel);

            // Combo recent commands
            subcon.fill = GridBagConstraints.HORIZONTAL;
            subcon.weightx = 1.0;
            subcon.weighty = 0.0;
            subcon.gridx = 1;
            subcon.gridy = 2;
            subcon.gridwidth = 1;
            subcon.insets = new Insets (0, 0, 5, 5);
            subcon.anchor = GridBagConstraints.SOUTH;
            rcmdscombo = new JComboBox(new ComboModel());
            rcmdscombo.getAccessibleContext().setAccessibleName(bundle.getString("ACS_DataViewComboBoxA11yName")); //NOI18N
            rcmdscombo.getAccessibleContext().setAccessibleDescription(bundle.getString("ACS_DataViewComboBoxA11yDesc")); //NOI18N
            rcmdscombo.setToolTipText(bundle.getString("ACS_DataViewComboBoxA11yDesc")); //NOI18N
            comboLabel.setLabelFor(rcmdscombo);
            sublayout.setConstraints(rcmdscombo, subcon);
            subpane.add(rcmdscombo);
            rcmdscombo.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    JComboBox source = (JComboBox)e.getSource();
                    RecentCommand cmd = (RecentCommand)source.getSelectedItem();
                    if (cmd != null)
                        setCommand(cmd.getCommand());
                }
            });

            // Button Execute
            subcon.gridx = 2;
            subcon.gridy = 2;
            subcon.weightx = 0.0;
            subcon.weighty = 0.0;
            subcon.insets = new Insets (0, 0, 5, 0);
            subcon.fill = GridBagConstraints.HORIZONTAL;
            subcon.anchor = GridBagConstraints.SOUTH;
            JButton fetchbtn = new JButton(bundle.getString("ExecuteButton")); //NOI18N
            fetchbtn.setToolTipText(bundle.getString("ACS_ExecuteButtonA11yDesc")); //NOI18N
            fetchbtn.setMnemonic(bundle.getString("ExecuteButton_Mnemonic").charAt(0)); //NOI18N
            sublayout.setConstraints(fetchbtn, subcon);
            subpane.add(fetchbtn);
            fetchbtn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    RequestProcessor.getDefault().post(new Runnable() {
                        public void run () {
                            executeCommand();
                        }
                    }, 0);
                }
            });

            // status line
            subcon.fill = GridBagConstraints.HORIZONTAL;
            subcon.weightx = 1.0;
            subcon.weighty = 0.0;
            subcon.gridx = 0;
            subcon.gridy = 3;
            subcon.gridwidth = 3;
            subcon.insets = new Insets (0, 0, 5, 0);
            subcon.anchor = GridBagConstraints.SOUTH;
            status = new JLabel(" "); //NOI18N
            status.setBorder(new javax.swing.border.LineBorder(java.awt.Color.gray));
            status.getAccessibleContext().setAccessibleDescription(bundle.getString("ACS_DataViewStatusLabelA11yDesc")); //NOI18N
            sublayout.setConstraints(status, subcon);
            subpane.add(status);

            JPanel subpane2 = new JPanel();
            GridBagLayout sublayout2 = new GridBagLayout();
            GridBagConstraints subcon2 = new GridBagConstraints ();
            subpane2.setLayout(sublayout2);

            // table label
            subcon2.fill = GridBagConstraints.HORIZONTAL;
            subcon2.weightx = 0.0;
            subcon2.weighty = 0.0;
            subcon2.gridx = 0;
            subcon2.gridy = 0;
            subcon2.gridwidth = 1;
            subcon2.insets = new Insets (5, 0, 0, 0);
            subcon2.anchor = GridBagConstraints.SOUTH;
            JLabel tableLabel = new JLabel(bundle.getString("ResultsLabel")); //NOI18N
            tableLabel.setDisplayedMnemonic(bundle.getString("ResultsLabel_Mnemonic").charAt(0)); //NOI18N
            tableLabel.getAccessibleContext().setAccessibleDescription(bundle.getString("ACS_DataViewResultsLabelA11yDesc")); //NOI18N
            sublayout2.setConstraints(tableLabel, subcon2);
            subpane2.add(tableLabel);

            // Table with results
            //      TableSorter sorter = new TableSorter();
            jtable = new JTable(dbadaptor/*sorter*/);
            jtable.getAccessibleContext().setAccessibleName(bundle.getString("ACS_DataViewTableA11yName")); //NOI18N
            jtable.getAccessibleContext().setAccessibleDescription(bundle.getString("ACS_DataViewTableA11yDesc")); //NOI18N
            jtable.setToolTipText(bundle.getString("ACS_DataViewTableA11yDesc")); //NOI18N
            jtable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            //    	sorter.addMouseListenerToHeaderInTable(table);
            tableLabel.setLabelFor(jtable);

            scrollpane = new JScrollPane(jtable);
            subcon2.fill = GridBagConstraints.BOTH;
            subcon2.weightx = 1.0;
            subcon2.weighty = 1.0;
            subcon2.gridx = 0;
            subcon2.gridy = 1;
            subcon2.gridwidth = 1;
            sublayout2.setConstraints(scrollpane, subcon2);
            subpane2.add(scrollpane);

            // Add it into splitview
            con.weightx = 1.0;
            con.weighty = 1.0;
            con.fill = GridBagConstraints.BOTH;
            con.gridx = 0;
            con.gridwidth = 1;
            con.gridy = 1;
            con.insets = new Insets (12, 12, 11, 11);

            SplittedPanel split = new SplittedPanel();
            split.setSplitType(SplittedPanel.VERTICAL);
            split.setSplitTypeChangeEnabled(false);
            split.setSplitAbsolute(false);
            split.setSplitPosition(26);
            split.add(subpane, SplittedPanel.ADD_LEFT);
            split.add(subpane2, SplittedPanel.ADD_RIGHT);
            layout.setConstraints(split, con);
            add(split);
        } catch (MissingResourceException e) {
            //    	e.printStackTrace();
        }
    }

    /** Returns query used by panel.
    */
    public String getCommand() {
        return queryarea.getText();
    }

    /** Sets query used by panel.
    */
    public void setCommand(String command) {
        queryarea.setText(command);
    }

    public boolean executeCommand() {
        String command = queryarea.getText().trim();
        boolean ret;

        try {
            status.setText(bundle.getString("CommandRunning")); //NOI18N
            dbadaptor.execute(command);

            RecentCommand rcmd = new RecentCommand(command);
            ((ComboModel)rcmdscombo.getModel()).addElement(rcmd);
            ret = true;
        } catch (Exception exc) {
            ret = false;
            status.setText(bundle.getString("CommandFailed")); //NOI18N
            org.openide.DialogDisplayer.getDefault().notify(new NotifyDescriptor.Message(bundle.getString("DataViewFetchErrorPrefix") + exc.getMessage(), NotifyDescriptor.ERROR_MESSAGE)); //NOI18N
        }

        return ret;
    }

    class ColDef {
        private String name;
        private boolean writable;
        private boolean bric;
        int datatype;

        public ColDef(String name, boolean flag) {
            this.name = name;
            writable = flag;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getDataType() {
            return datatype;
        }

        public void setDataType(int type) {
            datatype = type;
        }

        public boolean isWritable() {
            return writable;
        }

        public void setWritable(boolean flag) {
            writable = flag;
        }

        public boolean isBestRowIdentifierColumn() {
            return bric;
        }

        public void setBestRowIdentifierColumn(boolean flag) {
            bric = flag;
        }
    }

    static int tstrg = 0;
    static int gtcmd = 0;

    class RecentCommand {
        private String command;

        /** The command with no new lines */
        private String shortCommand;

        public RecentCommand(String cmd) {
            command = cmd;
            shortCommand = getShortCommand();
        }

        public String toString() {
            return shortCommand;
        }

        public String getCommand() {
            return command;
        }

        public boolean equals(Object obj) {
            if (obj instanceof RecentCommand)
                return ((RecentCommand)obj).getShortCommand().equals(shortCommand);

            return super.equals(obj);
        }

        /**
         * Gets the command String for display in the JComboBox without
         * new lines.
         *
         * @return the command for display in the JComboBox
         */
         private String getShortCommand()  {
            StringTokenizer tokenizer = new StringTokenizer(command);
            StringBuffer buffer = new StringBuffer();
            while (tokenizer.hasMoreElements()) {
                buffer.append(tokenizer.nextElement());
                buffer.append(" ");
            }
            return buffer.toString();
        }
    }

    class ComboModel extends AbstractListModel implements MutableComboBoxModel{
        Vector commands;
        Object selected;

        static final long serialVersionUID =-5831993904798984334L;
        public ComboModel() {
            this(new Vector(1));
        }

        public ComboModel(Vector elems) {
            commands = elems;
        }

        public Object getSelectedItem() {
            return selected;
        }

        public void setSelectedItem(Object anItem) {
            selected = anItem;
            fireContentsChanged(this,-1,-1);
        }

        public void addElement(Object obj) {
            if (!commands.contains(obj)) {
                commands.add(obj);
                fireContentsChanged(this,-1,-1);
            }
        }

        public void removeElement(Object obj) {
            commands.removeElement(obj);
            fireContentsChanged(this,-1,-1);
        }

        public void insertElementAt(Object obj, int index) {
            if (!commands.contains(obj)) {
                commands.insertElementAt(obj, index);
                fireContentsChanged(this,-1,-1);
            }
        }

        public void removeElementAt(int index) {
            commands.removeElementAt(index);
            fireContentsChanged(this,-1,-1);
        }

        public int getSize() {
            return commands.size();
        }

        public Object getElementAt(int index) {
            return commands.get(index);
        }
    }

    class ViewDropTarget implements DropTargetListener {
        /** User is starting to drag over us */
        public void dragEnter (DropTargetDragEvent dtde) {
            dtde.acceptDrag(dtde.getDropAction());
        }

        /** User drags over us */
        public void dragOver (DropTargetDragEvent dtde) {
        }

        public void dropActionChanged (DropTargetDragEvent dtde) {
        }

        /** User exits the dragging */
        public void dragExit (DropTargetEvent dte) {
        }

        private ColumnNodeInfo getNodeInfo(Transferable t) {
            Node n = NodeTransfer.node(t, NodeTransfer.MOVE);
            if (n != null)
                return (ColumnNodeInfo)n.getCookie(ColumnNodeInfo.class);

            n = NodeTransfer.node(t, NodeTransfer.COPY);
            if (n != null)
                return (ColumnNodeInfo)n.getCookie(ColumnNodeInfo.class);

            return null;
        }

        /** Performs the drop action */
        public void drop (DropTargetDropEvent dtde) {
            String query = null;
            Transferable t = dtde.getTransferable();
            StringBuffer buff = new StringBuffer();

            try {
                DataFlavor multiFlavor = new DataFlavor (
                    NbBundle.getBundle(ExTransferable.class).getString("MultiNodeMimeType"), //NOI18N
                    NbBundle.getBundle (ExTransferable.class).getString ("transferFlavorsMultiFlavorName") //NOI18N
                );

                if (t.isDataFlavorSupported(multiFlavor)) {
                    MultiTransferObject mobj = (MultiTransferObject)t.getTransferData(ExTransferable.multiFlavor);
                    int count = mobj.getCount();
                    int tabidx = 0;
                    HashMap tabidxmap = new HashMap();
                    for (int i = 0; i < count; i++) {
                        ColumnNodeInfo nfo = getNodeInfo(mobj.getTransferableAt(i));
                        if (nfo != null) {
                            String tablename = nfo.getTable();
                            Integer tableidx = (Integer)tabidxmap.get(tablename);
                            if (tableidx == null) tabidxmap.put(tablename, tableidx = new Integer(tabidx++));
                            if (buff.length()>0) buff.append(", "); //NOI18N
                            buff.append("t"+tableidx+"."+nfo.getName()); //NOI18N
                        }
                    }

                    StringBuffer frombuff = new StringBuffer();
                    Iterator iter = tabidxmap.keySet().iterator();
                    while (iter.hasNext()) {
                        String tab = (String)iter.next();
                        if (frombuff.length()>0) frombuff.append(", "); //NOI18N
                        frombuff.append(tab + " t"+tabidxmap.get(tab)); //NOI18N
                    }

                    query = "select "+buff.toString()+" from "+frombuff.toString(); //NOI18N

                } else {
                    ColumnNodeInfo nfo = getNodeInfo(t);
                    if (nfo != null) query = "select "+nfo.getName()+" from "+nfo.getTable(); //NOI18N
                }

                if (query != null)
                    setCommand(query);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    class DataModel extends AbstractTableModel {
        DatabaseNodeInfo node_info;
        Vector coldef = new Vector();
        Vector data = new Vector();
        boolean editable = false;

        static final long serialVersionUID =7729426847826999963L;

        /** Constructor */
        public DataModel(DatabaseNodeInfo node_info) throws SQLException {
            this.node_info = node_info;
        }

        /** Executes command
        * @param command SQL Expression
        */
        synchronized public void execute(String command) throws Exception {
            if (command.length() == 0)
                return;

            Connection con;
            Statement stat;
            try {
                con = node_info.getConnection();
                stat = con.createStatement();
            } catch ( Exception exc ) {
                String message = MessageFormat.format(bundle.getString("EXC_ConnectionIsBroken"), new String[] {exc.getMessage()}); // NOI18N
                throw new DatabaseException(message);
            }

            ResultSet rs;

            if (command.toLowerCase().startsWith("select")) { //NOI18N
                rs = stat.executeQuery(command);

                ResultSetMetaData mdata = rs.getMetaData();

                int cols = mdata.getColumnCount();
                coldef.clear();
                for(int column = 1; column <= cols; column++) {
                    boolean writable;
                    try {
                        writable = mdata.isWritable(column);
                    } catch (SQLException exc) {
                        //patch for FireBirdSQL (isWritable has not been implemented yet)
                        writable = false;
                    }
                    ColDef cd = new ColDef(mdata.getColumnLabel(column), writable);
                    cd.setDataType(mdata.getColumnType(column));
                    coldef.add(cd);
                }

                // Get all rows.
                // In future implementations should be more careful
                int rcounter = 0;
                int limit = RootNode.getOption().getFetchLimit();
                int step = RootNode.getOption().getFetchStep();
                data.clear();
                while (rs.next()) {
                    Vector row = new Vector(cols);
                    for (int column = 1; column <= cols; column++)
                        row.add(rs.getObject(column));
                    data.addElement(row);

                    // Catch row count
                    if (++rcounter >= limit) {
                        String[] arr = new String[] {
                            (new Integer(rcounter)).toString(),
                            (new Integer(step)).toString()
                        };
                        String cancel = bundle.getString("DataViewCancelButton"); //NOI18N
                        String nextset = bundle.getString("DataViewNextFetchButton"); //NOI18N
                        String allset = bundle.getString("DataViewAllFetchButton"); //NOI18N
                        String message = MessageFormat.format(bundle.getString("DataViewMessage"), arr); //NOI18N
                        NotifyDescriptor ndesc = new NotifyDescriptor(message, bundle.getString("FetchDataTitle"), NotifyDescriptor.YES_NO_CANCEL_OPTION, NotifyDescriptor.QUESTION_MESSAGE, new Object[] {nextset, allset, cancel}, NotifyDescriptor.CANCEL_OPTION); //NOI18N
                        String retv = (String) DialogDisplayer.getDefault().notify(ndesc);
                        if (retv.equals(allset))
                            limit = Integer.MAX_VALUE;
                        else
                            if (retv.equals(nextset))
                                limit = limit + step;
                            else
                                break;
                    }
                }
                rs.close();
                fireTableChanged(null);
            } else {
                if (command.toLowerCase().startsWith("delete") || command.toLowerCase().startsWith("insert") || command.toLowerCase().startsWith("update")) //NOI18N
                    stat.executeUpdate(command);
                else {
                    stat.execute(command);

                    //refresh DBExplorer nodes
                    while (!(node instanceof ConnectionNode))
                        node = node.getParentNode();
                    Enumeration nodes = node.getChildren().nodes();
                    while (nodes.hasMoreElements())
                        ((DatabaseNodeInfo)((Node)nodes.nextElement()).getCookie(DatabaseNodeInfo.class)).refreshChildren();
                }
            }
            status.setText(bundle.getString("CommandExecuted")); //NOI18N
            stat.close();
        }

        /** Returns column name
        * @param column Column index
        */
        public String getColumnName(int column) {
            synchronized (coldef) {
                if (column < coldef.size()) {
                    String cname = ((ColDef)coldef.elementAt(column)).getName();
                    return cname;
                }

                return ""; //NOI18N
            }
        }

        /** Returns column renderer/editor class
        * @param column Column index
        */
        public Class getColumnClass(int column) {
            synchronized (coldef) {
                if (column < coldef.size()) {
                    int coltype = ((ColDef)coldef.elementAt(column)).getDataType();
                    switch (coltype) {
                        case Types.CHAR:
                        case Types.VARCHAR:
                        case Types.LONGVARCHAR: return String.class;
                        case Types.BIT: return Boolean.class;
                        case Types.TINYINT:
                        case Types.SMALLINT:
                        case Types.INTEGER: return Integer.class;
                        case Types.BIGINT: return Long.class;
                        case Types.FLOAT:
                        case Types.DOUBLE: return Double.class;
                        case Types.DATE: return java.sql.Date.class;
                    }
                }

                return Object.class;
            }
        }

        /** Returns true, if cell is editable
        */
        public boolean isCellEditable(int row, int column) {
            synchronized (coldef) {
                if (!editable)
                    return false;

                if (column < coldef.size())
                    return ((ColDef)coldef.elementAt(column)).isWritable();

                return false;
            }
        }

        /** Returns colun count
        */
        public int getColumnCount() {
            synchronized (coldef) {
                return coldef.size();
            }
        }

        /** Returns row count
        */
        public int getRowCount() {
            synchronized (data) {
                return data.size();
            }
        }

        /** Returns value at specified position
        */
        public Object getValueAt(int aRow, int aColumn) {
            synchronized (data) {
                Vector row = new Vector();
                if (aRow < data.size())
                    row = (Vector) data.elementAt(aRow);
                if (row != null && aColumn < row.size())
                    return row.elementAt(aColumn);

                return null;
            }
        }

        private String format(Object value, int type) {
            if (value == null)
                return "null"; //NOI18N

            switch(type) {
                case Types.INTEGER:
                case Types.DOUBLE:
                case Types.FLOAT: return value.toString();
                case Types.BIT: return ((Boolean)value).booleanValue() ? "1" : "0"; //NOI18N
                case Types.DATE: return value.toString();
                default: return "\""+value.toString()+"\""; //NOI18N
            }
        }

        public void setValueAt(Object value, int row, int column) {
            synchronized (coldef) {
                int enucol = 0;
                StringBuffer where = new StringBuffer();
                Enumeration enu = coldef.elements();
                while (enu.hasMoreElements()) {
                    ColDef cd = (ColDef)enu.nextElement();
                    if (cd.isBestRowIdentifierColumn()) {
                        String key = cd.getName();
                        String val = format(getValueAt(row,enucol), cd.getDataType());
                        if (where.length()>0)
                            where.append(" and "); //NOI18N
                        where.append(key+" = "+val); //NOI18N
                    }
                    enucol++;
                }
            }
        }
    }

    protected Object writeReplace() throws ObjectStreamException {
        return null;
    }
}
