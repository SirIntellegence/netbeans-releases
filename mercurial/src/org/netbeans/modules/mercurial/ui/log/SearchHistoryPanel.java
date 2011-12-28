/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
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
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2009 Sun
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

package org.netbeans.modules.mercurial.ui.log;

import java.awt.Color;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.openide.util.NbBundle;
import org.openide.explorer.ExplorerManager;
import org.openide.nodes.Node;
import org.openide.windows.TopComponent;
import org.openide.awt.Mnemonics;
import org.netbeans.modules.versioning.util.NoContentPanel;
import javax.swing.*;
import java.io.File;
import java.util.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.awt.event.KeyEvent;
import java.awt.event.ActionEvent;
import java.awt.Dimension;
import java.awt.EventQueue;
import org.netbeans.modules.mercurial.HgModuleConfig;
import org.netbeans.modules.mercurial.HgProgressSupport;
import org.netbeans.modules.mercurial.Mercurial;
import org.netbeans.modules.mercurial.kenai.HgKenaiAccessor;
import org.netbeans.modules.mercurial.ui.diff.DiffSetupSource;
import org.netbeans.modules.mercurial.ui.diff.Setup;
import org.netbeans.modules.mercurial.ui.log.HgLogMessage.HgRevision;
import org.netbeans.modules.mercurial.ui.log.SummaryView.HgLogEntry;
import org.netbeans.modules.mercurial.util.HgUtils;
import org.netbeans.modules.versioning.util.VCSKenaiAccessor;

/**
 * Contains all components of the Search History panel.
 *
 * @author Maros Sandor
 */
class SearchHistoryPanel extends javax.swing.JPanel implements ExplorerManager.Provider, PropertyChangeListener, DiffSetupSource, DocumentListener {

    private final File[]                roots;
    private final SearchCriteriaPanel   criteria;
    
    private Action                  searchAction;
    private SearchExecutor          currentSearch;
    private Search                  currentAdditionalSearch;

    private boolean                 criteriaVisible;
    private boolean                 searchInProgress;
    private List<RepositoryRevision> results;
    private SummaryView             summaryView;    
    private DiffResultsView         diffView;
    private boolean                 bOutSearch;
    private boolean                 bIncomingSearch;
    private AbstractAction nextAction;
    private AbstractAction prevAction;
    private SearchHistoryTopComponent.DiffResultsViewFactory diffViewFactory;
    private String currentBranch;
    private int showingResults;
    private Map<String, VCSKenaiAccessor.KenaiUser> kenaiUserMap;
    private List<HgLogEntry> logEntries;
    
    private static final Icon ICON_COLLAPSED = UIManager.getIcon("Tree.collapsedIcon"); //NOI18N
    private static final Icon ICON_EXPANDED = UIManager.getIcon("Tree.expandedIcon"); //NOI18N

    /** Creates new form SearchHistoryPanel */
    public SearchHistoryPanel(File [] roots, SearchCriteriaPanel criteria) {
        this.bOutSearch = false;
        this.bIncomingSearch = false;
        this.roots = roots;
        this.criteria = criteria;
        this.diffViewFactory = new SearchHistoryTopComponent.DiffResultsViewFactory();
        criteriaVisible = true;
        explorerManager = new ExplorerManager ();
        initComponents();
        setupComponents();
        aquaBackgroundWorkaround();
        refreshComponents(true);
    }
    
    private void aquaBackgroundWorkaround() {
        if( "Aqua".equals( UIManager.getLookAndFeel().getID() ) ) {             // NOI18N
            Color color = UIManager.getColor("NbExplorerView.background");      // NOI18N
            setBackground(color); 
            jToolBar1.setBackground(color); 
            resultsPanel.setBackground(color); 
            searchCriteriaPanel.setBackground(color); 
            criteria.setBackground(color); 
        }
    }

    /**
     * Sets the factory creating the appropriate DiffResultsView to display.
     * @param fac factory creating the appropriate DiffResultsView to display. If null then a default factory will be created.
     */
    public void setDiffResultsViewFactory(SearchHistoryTopComponent.DiffResultsViewFactory fac) {
        if (fac != null) {
            this.diffViewFactory = fac;
        }
    }

    void setOutSearch() {
        criteria.setForOut();
        bOutSearch = true;
        tbSummary.setToolTipText(NbBundle.getMessage(SearchHistoryPanel.class,  "TT_OutSummary"));
        tbDiff.setToolTipText(NbBundle.getMessage(SearchHistoryPanel.class,  "TT_OutShowDiff"));
    }

    boolean isOutSearch() {
        return bOutSearch;
    }

    boolean isShowInfo() {
        return fileInfoCheckBox.isSelected();
    }

    
    void setIncomingSearch() {
        criteria.setForIncoming();
        bIncomingSearch = true;
        tbDiff.setVisible(false);
        bNext.setVisible(false);
        bPrev.setVisible(false);
        tbSummary.setToolTipText(NbBundle.getMessage(SearchHistoryPanel.class,  "TT_IncomingSummary"));
    }
    
    boolean isIncomingSearch() {
        return bIncomingSearch;
    }

    void setSearchCriteria(boolean b) {
        criteriaVisible = b;
        refreshComponents(false);
    }

    private void cancelBackgroundSearch () {
        if (currentSearch != null) {
            currentSearch.cancel();
        }
        if (currentAdditionalSearch != null) {
            currentAdditionalSearch.cancel();
        }
    }

    private void setupComponents() {
        searchCriteriaPanel.add(criteria);
        searchAction = new AbstractAction(NbBundle.getMessage(SearchHistoryPanel.class,  "CTL_Search")) { // NOI18N
            {
                putValue(Action.SHORT_DESCRIPTION, NbBundle.getMessage(SearchHistoryPanel.class, "TT_Search")); // NOI18N
            }
            @Override
            public void actionPerformed(ActionEvent e) {
                search();
            }
        };
        searchCriteriaPanel.getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "search"); // NOI18N
        searchCriteriaPanel.getActionMap().put("search", searchAction); // NOI18N
        bSearch.setAction(searchAction);
        Mnemonics.setLocalizedText(bSearch, NbBundle.getMessage(SearchHistoryPanel.class,  "CTL_Search")); // NOI18N
        
        Dimension d1 = tbSummary.getPreferredSize();
        Dimension d2 = tbDiff.getPreferredSize();
        if (d1.width > d2.width) {
            tbDiff.setPreferredSize(d1);
        }
        
        nextAction = new AbstractAction(null, new javax.swing.ImageIcon(getClass().getResource("/org/netbeans/modules/mercurial/resources/icons/diff-next.png"))) { // NOI18N
            {
                putValue(Action.SHORT_DESCRIPTION, java.util.ResourceBundle.getBundle("org/netbeans/modules/mercurial/ui/diff/Bundle"). // NOI18N
                                                   getString("CTL_DiffPanel_Next_Tooltip")); // NOI18N
            }
            @Override
            public void actionPerformed(ActionEvent e) {
                diffView.onNextButton();
            }
        };
        prevAction = new AbstractAction(null, new javax.swing.ImageIcon(getClass().getResource("/org/netbeans/modules/mercurial/resources/icons/diff-prev.png"))) { // NOI18N
            {
                putValue(Action.SHORT_DESCRIPTION, java.util.ResourceBundle.getBundle("org/netbeans/modules/mercurial/ui/diff/Bundle"). // NOI18N
                                                   getString("CTL_DiffPanel_Prev_Tooltip")); // NOI18N
            }
            @Override
            public void actionPerformed(ActionEvent e) {
                diffView.onPrevButton();
            }
        };
        bNext.setAction(nextAction);
        bPrev.setAction(prevAction);

        criteria.tfFrom.getDocument().addDocumentListener(this);
        criteria.tfTo.getDocument().addDocumentListener(this);
        
        getActionMap().put("jumpNext", nextAction); // NOI18N
        getActionMap().put("jumpPrev", prevAction); // NOI18N
        
        fileInfoCheckBox.setSelected(HgModuleConfig.getDefault().getShowFileInfo());
    }

    private ExplorerManager             explorerManager;

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (ExplorerManager.PROP_SELECTED_NODES.equals(evt.getPropertyName())) {
            TopComponent tc = (TopComponent) SwingUtilities.getAncestorOfClass(TopComponent.class, this);
            if (tc == null) return;
            tc.setActivatedNodes((Node[]) evt.getNewValue());
        }
    }

    @Override
    public void addNotify() {
        super.addNotify();
        explorerManager.addPropertyChangeListener(this);
    }

    @Override
    public void removeNotify() {
        explorerManager.removePropertyChangeListener(this);
        super.removeNotify();
    }
    
    @Override
    public ExplorerManager getExplorerManager () {
        return explorerManager;
    }
    
    final void refreshComponents(boolean refreshResults) {
        if (refreshResults) {
            resultsPanel.removeAll();
            if (results == null) {
                String branches = criteria.getBranch();
                if (searchInProgress) {
                    resultsPanel.add(new NoContentPanel(NbBundle.getMessage(SearchHistoryPanel.class,
                            branches.isEmpty() ? "LBL_SearchHistory_Searching" : "LBL_SearchHistory_Searching.branch", branches))); // NOI18N
                } else {
                    resultsPanel.add(new NoContentPanel(NbBundle.getMessage(SearchHistoryPanel.class,
                            branches.isEmpty() ? "LBL_SearchHistory_NoResults" : "LBL_SearchHistory_NoResults.branch", branches))); // NOI18N
                }
            } else {
                if (tbSummary.isSelected()) {
                    if (summaryView == null) {
                        summaryView = new SummaryView(this, logEntries = createLogEntries(results), kenaiUserMap);
                    }
                    resultsPanel.add(summaryView.getComponent());
                    summaryView.requestFocusInWindow();
                } else {
                    if (diffView == null) {
                        diffView = diffViewFactory.createDiffResultsView(this, results);
                    }
                    resultsPanel.add(diffView.getComponent());
                }
            }
            resultsPanel.revalidate();
            resultsPanel.repaint();
        }
        updateActions();

        searchCriteriaPanel.setVisible(criteriaVisible);
        expandCriteriaButton.setIcon(criteriaVisible ? ICON_EXPANDED : ICON_COLLAPSED);
        if (criteria.getLimit() <= 0) {
            criteria.setLimit(SearchExecutor.DEFAULT_LIMIT);
        }
        revalidate();
        repaint();
    }
 
    final void updateActions () {
        nextAction.setEnabled(!tbSummary.isSelected() && diffView != null && diffView.isNextEnabled());
        prevAction.setEnabled(!tbSummary.isSelected() && diffView != null && diffView.isPrevEnabled());
    }
    public void setResults(List<RepositoryRevision> newResults, Map<String, VCSKenaiAccessor.KenaiUser> kenaiUserMap, int limit) {
        setResults(newResults, kenaiUserMap, false, limit);
    }

    private void setResults(List<RepositoryRevision> newResults, Map<String, VCSKenaiAccessor.KenaiUser> kenaiUserMap, boolean searching, int limit) {
        this.results = newResults == null ? null : filter(newResults);
        this.kenaiUserMap = kenaiUserMap;
        this.searchInProgress = searching;
        showingResults = limit;
        if (newResults != null && newResults.size() < limit) {
            showingResults = -1;
        }
        summaryView = null;
        diffView = null;
        refreshComponents(true);
    }

    public File[] getRoots() {
        return roots;
    }

    public SearchCriteriaPanel getCriteria() {
        return criteria;
    }

    private synchronized void search() {
        cancelBackgroundSearch();
        setResults(null, null, true, -1);
        HgModuleConfig.getDefault().setShowHistoryMerges(criteria.isIncludeMerges());
        currentSearch = new SearchExecutor(this);
        currentSearch.start();
    }
    
    void executeSearch() {
        search();
    }

    void showDiff(RepositoryRevision.Event revision) {
        tbDiff.setSelected(true);
        refreshComponents(true);
        diffView.select(revision);
    }

    public void showDiff(RepositoryRevision container) {
        tbDiff.setSelected(true);
        refreshComponents(true);
        diffView.select(container);
    }

    /**
     * Return diff setup describing shown history.
     * It return empty collection on non-atomic
     * revision ranges. XXX move this logic to clients?
     */
    @Override
    public Collection getSetups() {
        if (results == null) {
            return Collections.EMPTY_SET;
        }
        if (tbDiff.isSelected()) {
            return diffView.getSetups();
        } else {
            return summaryView.getSetups();
        }
    }
    
    Collection getSetups(RepositoryRevision [] revisions, RepositoryRevision.Event [] events) {
        long fromRevision = Long.MAX_VALUE;
        long toRevision = Long.MIN_VALUE;
        HgLogMessage from = null;
        HgLogMessage to = null;
        Set<File> filesToDiff = new HashSet<File>();
        
        for (RepositoryRevision revision : revisions) {
            long rev = Long.parseLong(revision.getLog().getRevisionNumber());
            if (rev > toRevision) {
                toRevision = rev;
                to = revision.getLog();
            }
            if (rev < fromRevision) {
                fromRevision = rev;
                from = revision.getLog();
            }
            List<RepositoryRevision.Event> evs = revision.getEvents();
            for (RepositoryRevision.Event event : evs) {
                File file = event.getFile();
                if (file != null) {
                    filesToDiff.add(file);
                }
            }
        }

        for (RepositoryRevision.Event event : events) {
            long rev = Long.parseLong(event.getLogInfoHeader().getLog().getRevisionNumber());
            if (rev > toRevision) {
                toRevision = rev;
                to = event.getLogInfoHeader().getLog();
            }
            if (rev < fromRevision) {
                fromRevision = rev;
                from = event.getLogInfoHeader().getLog();
            }
            if (event.getFile() != null) {
                filesToDiff.add(event.getFile());
            }
        }

        List<Setup> setups = new ArrayList<Setup>();
        for (File file : filesToDiff) {
            HgRevision fromHgRevision = from.getHgRevision();
            if (from.getRevisionNumber().equals(to.getRevisionNumber())) {
                fromHgRevision = from.getAncestor(file);
            }
            Setup setup = new Setup(file, fromHgRevision, to.getHgRevision(), null, false);
            setups.add(setup);
        }
        return setups;
    }
    
    @Override
    public String getSetupDisplayName() {
        return null;
    }

    public static int compareRevisions(String r1, String r2) {
        StringTokenizer st1 = new StringTokenizer(r1, "."); // NOI18N
        StringTokenizer st2 = new StringTokenizer(r2, "."); // NOI18N
        for (;;) {
            if (!st1.hasMoreTokens()) {
                return st2.hasMoreTokens() ? -1 : 0;
            }
            if (!st2.hasMoreTokens()) {
                return st1.hasMoreTokens() ? 1 : 0;
            }
            int n1 = Integer.parseInt(st1.nextToken());
            int n2 = Integer.parseInt(st2.nextToken());
            if (n1 != n2) return n2 - n1;
        }
    }
    
    void setCurrentBranch (String branchName) {
        this.currentBranch = branchName;
    }

    String getCurrentBranch () {
        return currentBranch == null ? "" : currentBranch;
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroup1 = new javax.swing.ButtonGroup();
        bSearch = new javax.swing.JButton();
        jToolBar1 = new javax.swing.JToolBar();
        tbSummary = new javax.swing.JToggleButton();
        tbDiff = new javax.swing.JToggleButton();
        jSeparator2 = new javax.swing.JSeparator();
        jSeparator3 = new javax.swing.JToolBar.Separator();
        resultsPanel = new javax.swing.JPanel();
        searchCriteriaPanel = new javax.swing.JPanel();
        expandCriteriaButton = new org.netbeans.modules.versioning.history.LinkButton();

        setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 8, 0, 8));

        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("org/netbeans/modules/mercurial/ui/log/Bundle"); // NOI18N
        bSearch.setToolTipText(bundle.getString("TT_Search")); // NOI18N

        jToolBar1.setFloatable(false);
        jToolBar1.setRollover(true);

        buttonGroup1.add(tbSummary);
        tbSummary.setSelected(true);
        org.openide.awt.Mnemonics.setLocalizedText(tbSummary, bundle.getString("CTL_ShowSummary")); // NOI18N
        tbSummary.setToolTipText(bundle.getString("TT_Summary")); // NOI18N
        tbSummary.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                onViewToggle(evt);
            }
        });
        jToolBar1.add(tbSummary);
        tbSummary.getAccessibleContext().setAccessibleName(org.openide.util.NbBundle.getMessage(SearchHistoryPanel.class, "CTL_ShowSummary")); // NOI18N

        buttonGroup1.add(tbDiff);
        org.openide.awt.Mnemonics.setLocalizedText(tbDiff, bundle.getString("CTL_ShowDiff")); // NOI18N
        tbDiff.setToolTipText(bundle.getString("TT_ShowDiff")); // NOI18N
        tbDiff.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                onViewToggle(evt);
            }
        });
        jToolBar1.add(tbDiff);

        jSeparator2.setOrientation(javax.swing.SwingConstants.VERTICAL);
        jSeparator2.setMaximumSize(new java.awt.Dimension(2, 32767));
        jToolBar1.add(jSeparator2);
        jToolBar1.add(bNext);
        bNext.getAccessibleContext().setAccessibleName(bundle.getString("ACSN_NextDifference")); // NOI18N
        bNext.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(SearchHistoryPanel.class, "ACSD_NextDifference")); // NOI18N

        jToolBar1.add(bPrev);
        bPrev.getAccessibleContext().setAccessibleName(bundle.getString("ACSN_PrevDifference")); // NOI18N
        bPrev.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(SearchHistoryPanel.class, "ACSD_PrevDifference")); // NOI18N

        jToolBar1.add(jSeparator3);

        org.openide.awt.Mnemonics.setLocalizedText(fileInfoCheckBox, org.openide.util.NbBundle.getMessage(SearchHistoryPanel.class, "LBL_SearchHistoryPanel_AllInfo")); // NOI18N
        fileInfoCheckBox.setToolTipText(org.openide.util.NbBundle.getMessage(SearchHistoryPanel.class, "LBL_TT_SearchHistoryPanel_AllInfo")); // NOI18N
        fileInfoCheckBox.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        fileInfoCheckBox.setOpaque(false);
        fileInfoCheckBox.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        fileInfoCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fileInfoCheckBoxActionPerformed(evt);
            }
        });
        jToolBar1.add(fileInfoCheckBox);

        resultsPanel.setLayout(new java.awt.BorderLayout());

        searchCriteriaPanel.setLayout(new java.awt.BorderLayout());

        org.openide.awt.Mnemonics.setLocalizedText(expandCriteriaButton, org.openide.util.NbBundle.getMessage(SearchHistoryPanel.class, "CTL_expandCriteriaButton.text")); // NOI18N
        expandCriteriaButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                expandCriteriaButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jToolBar1, javax.swing.GroupLayout.DEFAULT_SIZE, 432, Short.MAX_VALUE)
            .addComponent(resultsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(bSearch))
            .addComponent(searchCriteriaPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(layout.createSequentialGroup()
                .addComponent(expandCriteriaButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jToolBar1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(resultsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 313, Short.MAX_VALUE)
                .addGap(18, 18, 18)
                .addComponent(expandCriteriaButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(searchCriteriaPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 17, Short.MAX_VALUE)
                .addGap(18, 18, 18)
                .addComponent(bSearch)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void onViewToggle(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_onViewToggle
        refreshComponents(true);
    }//GEN-LAST:event_onViewToggle

private void fileInfoCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fileInfoCheckBoxActionPerformed
        HgModuleConfig.getDefault().setShowFileInfo( fileInfoCheckBox.isSelected() && fileInfoCheckBox.isEnabled());
        if (summaryView != null) {
            summaryView.refreshView();
        }
}//GEN-LAST:event_fileInfoCheckBoxActionPerformed

    private void expandCriteriaButtonActionPerformed (java.awt.event.ActionEvent evt) {//GEN-FIRST:event_expandCriteriaButtonActionPerformed
        searchCriteriaPanel.setVisible(!searchCriteriaPanel.isVisible());
        expandCriteriaButton.setIcon(searchCriteriaPanel.isVisible() ? ICON_EXPANDED : ICON_COLLAPSED);
        criteriaVisible = searchCriteriaPanel.isVisible();
    }//GEN-LAST:event_expandCriteriaButtonActionPerformed

    @Override
    public void insertUpdate(DocumentEvent e) {
        validateUserInput();
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        validateUserInput();        
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
        validateUserInput();        
    }
    
    private void validateUserInput() {
        String from = criteria.getFrom();
        if(from == null && criteria.tfFrom.getText().trim().length() > 0) {
            bSearch.setEnabled(false);
            return;
        }
        String to = criteria.getTo();
        if(to == null && criteria.tfTo.getText().trim().length() > 0) {
            bSearch.setEnabled(false);
            return;
        }        
        bSearch.setEnabled(true);
    }    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    final javax.swing.JButton bNext = new javax.swing.JButton();
    final javax.swing.JButton bPrev = new javax.swing.JButton();
    private javax.swing.JButton bSearch;
    private javax.swing.ButtonGroup buttonGroup1;
    private org.netbeans.modules.versioning.history.LinkButton expandCriteriaButton;
    final javax.swing.JCheckBox fileInfoCheckBox = new javax.swing.JCheckBox();
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JToolBar.Separator jSeparator3;
    private javax.swing.JToolBar jToolBar1;
    private javax.swing.JPanel resultsPanel;
    private javax.swing.JPanel searchCriteriaPanel;
    private javax.swing.JToggleButton tbDiff;
    private javax.swing.JToggleButton tbSummary;
    // End of variables declaration//GEN-END:variables

    void getMoreRevisions (PropertyChangeListener callback, int count) {
        if (currentSearch == null) {
            throw new IllegalStateException("No search task active");
        }
        if (currentAdditionalSearch != null) {
            currentAdditionalSearch.cancel();
        }
        if (count < 0 || showingResults < 0) {
            count = -1;
        } else {
            count += showingResults;
        }
        currentAdditionalSearch = new Search(count);
        currentAdditionalSearch.start(Mercurial.getInstance().getParallelRequestProcessor(), 
                Mercurial.getInstance().getRepositoryRoot(roots[0]), "Getting more revisions");
    }

    List<RepositoryRevision> getResults () {
        return results;
    }

    boolean hasMoreResults () {
        return showingResults > -1;
    }

    void windowClosed () {
        cancelBackgroundSearch();
    }

    private List<RepositoryRevision> filter (List<RepositoryRevision> results) {
        String username = currentSearch.getUsername();
        String msg = currentSearch.getMessage();
        List<RepositoryRevision> newResults = new ArrayList<RepositoryRevision>(results.size());
        for (RepositoryRevision rev : results) {
            if (username != null && !rev.getLog().getAuthor().contains(username)) continue;
            if (msg != null && !rev.getLog().getMessage().contains(msg)) continue;
            newResults.add(rev);
        }
        return newResults;
    }
    
    private class Search extends HgProgressSupport {
        private final int count;
        private final SearchExecutor executor;

        private Search (int count) {
            this.count = count;
            this.executor = currentSearch;
        }

        @Override
        protected void perform () {
            final List<RepositoryRevision> newResults = executor.search(count, this);
            final Map<String, VCSKenaiAccessor.KenaiUser> additionalUsersMap = createKenaiUsersMap(newResults);
            if (!isCanceled()) {
                EventQueue.invokeLater(new Runnable() {
                    @Override
                    public void run () {
                        if (!isCanceled()) {
                            Set<String> visibleRevisions = new HashSet<String>(results.size());
                            for (RepositoryRevision rev : results) {
                                visibleRevisions.add(rev.getLog().getCSetShortID());
                            }
                            
                            List<RepositoryRevision> toAdd = new ArrayList<RepositoryRevision>(newResults.size());
                            for (RepositoryRevision rev : filter(newResults)) {
                                if (!visibleRevisions.contains(rev.getLog().getCSetShortID())) {
                                    toAdd.add(rev);
                                }
                            }
                            results.addAll(toAdd);
                            if (count == -1) {
                                showingResults = -1;
                            } else {
                                showingResults = count;
                            }
                            if (showingResults > newResults.size()) {
                                showingResults = -1;
                            }
                            logEntries = createLogEntries(results);
                            kenaiUserMap.putAll(additionalUsersMap);
                            if (diffView != null) {
                                diffView.refreshResults(results);
                            }
                            if (summaryView != null) {
                                summaryView.entriesChanged(logEntries);
                            }
                        }
                    }
                });
            }
        }
    }

    static Map<String, VCSKenaiAccessor.KenaiUser> createKenaiUsersMap (List<RepositoryRevision> results) {
        Map<String, VCSKenaiAccessor.KenaiUser> kenaiUsersMap = new HashMap<String, VCSKenaiAccessor.KenaiUser>();
        if(results.size() > 0) {
            String url = HgUtils.getRemoteRepository(results.get(0).getRepositoryRoot());
            boolean isKenaiRepository = url != null && HgKenaiAccessor.getInstance().isKenai(url);
            if(isKenaiRepository) {
                for (RepositoryRevision repositoryRevision : results) {
                    String author = repositoryRevision.getLog().getAuthor();
                    String username = repositoryRevision.getLog().getUsername();
                    if(author != null && !author.equals("")) {
                        if (username == null || username.isEmpty()) {
                            username = author;
                        }
                        if(!kenaiUsersMap.keySet().contains(author)) {
                            VCSKenaiAccessor.KenaiUser kenaiUser = HgKenaiAccessor.getInstance().forName(username, url);
                            if(kenaiUser != null) {
                                kenaiUsersMap.put(author, kenaiUser);
                            }
                        }
                    }
                }
            }
        }
        return kenaiUsersMap;
    }

    List<HgLogEntry> createLogEntries(List<RepositoryRevision> results) {
        List<HgLogEntry> ret = new LinkedList<HgLogEntry>();
        for (RepositoryRevision repositoryRevision : results) {
            ret.add(new SummaryView.HgLogEntry(repositoryRevision, this));
        }
        return ret;
    }
}
