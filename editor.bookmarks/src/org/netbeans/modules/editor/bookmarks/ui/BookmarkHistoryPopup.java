/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2012 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2012 Sun Microsystems, Inc.
 */
package org.netbeans.modules.editor.bookmarks.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import org.netbeans.modules.editor.bookmarks.BookmarkHistory;
import org.netbeans.modules.editor.bookmarks.BookmarkInfo;
import org.netbeans.modules.editor.bookmarks.BookmarkUtils;
import org.openide.util.Utilities;
import org.openide.windows.WindowManager;

/**
 * Popup switcher for bookmarks history.
 *
 * @author Miloslav Metelka
 */
public final class BookmarkHistoryPopup implements KeyListener {
    
    private static BookmarkHistoryPopup INSTANCE = new BookmarkHistoryPopup();
    
    public static BookmarkHistoryPopup get() {
        return INSTANCE;
    }

    private Popup popup;
    
    private JTable table;
    
    private BookmarksTableModel tableModel;
    
    private int selectedEntryIndex;
    
    private JLabel descriptionLabel;
    
    private Component lastFocusedComponent;
    
    private BookmarkHistoryPopup() {
    }
    
    public void show(boolean chooseLastUsedBookmark) {
        if (popup != null) { // Refresh in case it already exists
            hide();
        }
        lastFocusedComponent = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        descriptionLabel = new JLabel();
        descriptionLabel.setBorder(new EmptyBorder(2, 2, 2, 2));
        Font font = descriptionLabel.getFont();
        font = new Font(font.getName(), font.getStyle(), (int) (0.9f * font.getSize()));
        descriptionLabel.setFont(font);
        selectedEntryIndex = -1;
        table = createTable();
        Rectangle screenBounds = Utilities.getUsableScreenBounds();
        initTable(screenBounds);
        // At least one entry -> select either first or last entry
        selectEntry(chooseLastUsedBookmark ? 0 : tableModel.getEntryCount() - 1);

        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());

        GridBagConstraints constraints;
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = GridBagConstraints.RELATIVE;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weightx = 1.0;
        constraints.weighty = 1.0;
        panel.add(table, constraints);
        
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = GridBagConstraints.RELATIVE;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1.0;
        panel.add(new JSeparator(SwingConstants.HORIZONTAL), constraints);

        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = GridBagConstraints.RELATIVE;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1.0;
        panel.add(descriptionLabel, constraints);

        panel.setBorder(new LineBorder(Color.black, 1));

        Dimension prefSize = panel.getPreferredSize();
        int x = (screenBounds.width - prefSize.width) / 2;
        int y = (screenBounds.height - prefSize.height) / 2;
        popup = PopupFactory.getSharedInstance().getPopup(WindowManager.getDefault().getMainWindow(), panel, x, y);
        popup.show();
        table.requestFocusInWindow();
    }
    
    public void hide() {
        if (popup != null) {
            table.removeKeyListener(this);
            popup.hide();
            popup = null;
            table = null;
            tableModel = null;
            descriptionLabel = null;
        }
    }
    
    private JTable createTable() {
        List<BookmarkInfo> historyBookmarks = BookmarkHistory.get().historyBookmarks();
        BookmarksNodeTree nodeTree = new BookmarksNodeTree();
        nodeTree.rebuild(null);
        Map<BookmarkInfo, BookmarkNode> bookmark2NodeMap = nodeTree.bookmark2NodeMap();
        List<BookmarkNode> entries = new ArrayList<BookmarkNode>(historyBookmarks.size() + 1);
        entries.add(bookmark2NodeMap.get(BookmarkInfo.BOOKMARKS_WINDOW));
        for (BookmarkInfo bookmark : historyBookmarks) {
            entries.add(bookmark2NodeMap.get(bookmark));
        }
        Collections.reverse(entries);
        tableModel = new BookmarksTableModel(true);
        tableModel.setEntries(entries);
        return new JTable(tableModel);
    }
    
    private void initTable(Rectangle maxBounds) {
        table.setShowGrid(false);
        table.setCellSelectionEnabled(true);
        table.setAutoscrolls(false);
        // Get Graphics resp. FontRenderContext from an off-screen image
        BufferedImage image = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = (Graphics2D) image.getGraphics();
        Font font = table.getFont();
        FontMetrics fm = g.getFontMetrics(font);
        int maxWidth = 1;
        int maxHeight = fm.getHeight() + 2; // Extra two pixels for spaces between lines
        for (int i = tableModel.getEntryCount() - 1; i >= 0; i--) {
            String value = (String) tableModel.getValueAt(i, 0);
            int stringWidth = fm.stringWidth(value);
            maxWidth = Math.max(maxWidth, stringWidth);
        }
        maxWidth += 16; // Add icon width
        maxWidth += 12; // Add extra space occupied by cell borders? etc.
        int columnEntryCount = maxBounds.height / maxHeight;
        int columnCount = tableModel.setColumnEntryCount(columnEntryCount);
        table.setTableHeader(null);
        TableCellRenderer cellRenderer = new BookmarkNodeRenderer(true);
        TableColumnModel columnModel = table.getColumnModel(); // 1 column by default
        TableColumn column = columnModel.getColumn(0);
        column.setCellRenderer(cellRenderer);
        column.setPreferredWidth(maxWidth);
        while (columnModel.getColumnCount() < columnCount) {
            column = new TableColumn(columnModel.getColumnCount());
            column.setPreferredWidth(maxWidth);
            column.setCellRenderer(cellRenderer);
            columnModel.addColumn(column);
        }

        table.addKeyListener(this);
    }
    
    private void selectEntry(int entryIndex) {
        assert (entryIndex >= 0) : "entryIndex=" + entryIndex + " < 0"; // NOI18N
        int[] rowColumn = new int[2];
        if (selectedEntryIndex != -1) {
            tableModel.entryIndex2rowColumn(selectedEntryIndex, rowColumn);
            table.changeSelection(rowColumn[0], rowColumn[1], true, false);
        }
        this.selectedEntryIndex = entryIndex;
        tableModel.entryIndex2rowColumn(selectedEntryIndex, rowColumn);
        table.changeSelection(rowColumn[0], rowColumn[1], true, false);
        descriptionLabel.setText(tableModel.getToolTipText(rowColumn[0], rowColumn[1]));
        Dimension labelSize = descriptionLabel.getSize();
        if (labelSize != null) {
            if (descriptionLabel.getPreferredSize().width > labelSize.width) {
                descriptionLabel.revalidate();
            }
        }
    }
    
    private void selectNext() {
        int nextIndex = (selectedEntryIndex + 1) % tableModel.getEntryCount();
        selectEntry(nextIndex);
    }
    
    private void selectPrevious() {
        int prevIndex = (selectedEntryIndex <= 0) ? (tableModel.getEntryCount() - 1) : selectedEntryIndex - 1;
        selectEntry(prevIndex);
    }
    
    private void openBookmark(BookmarkInfo bookmark) {
        if (bookmark != null) {
            if (bookmark == BookmarkInfo.BOOKMARKS_WINDOW) {
                BookmarksView.openView();
            } else {
                BookmarkUtils.postOpenEditor(bookmark);
            }
        }
    }
    
    private BookmarkInfo getSelectedBookmark() {
        return (selectedEntryIndex != -1) ? tableModel.getEntry(selectedEntryIndex).getBookmarkInfo() : null;
    }
    
    private void returnFocus() {
        if (lastFocusedComponent != null) {
            lastFocusedComponent.requestFocus();
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_ENTER:
                BookmarkInfo selectedBookmark = getSelectedBookmark();
                hide();
                openBookmark(selectedBookmark);
                e.consume();
                break;

            case KeyEvent.VK_ESCAPE:
                e.consume();
                hide();
                returnFocus();
                break;
                
            case KeyEvent.VK_COMMA:
                e.consume();
                selectNext();
                break;
                
            case KeyEvent.VK_PERIOD:
                e.consume();
                selectPrevious();
                break;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        // Check if Ctrl and Shift are still pressed
        int pressedMods = (KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK);
        if ((e.getModifiersEx() & pressedMods) != pressedMods) {
            e.consume();
            BookmarkInfo selectedBookmark = getSelectedBookmark();
            hide();
            openBookmark(selectedBookmark);
        }
    }
    
}
