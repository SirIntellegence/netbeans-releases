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

import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.openide.ErrorManager;
import org.openide.windows.TopComponent;
import org.openide.nodes.Node;
import org.netbeans.api.editor.mimelookup.MimeLookup;
import org.netbeans.api.editor.settings.FontColorSettings;
import javax.swing.*;
import javax.swing.text.*;
import java.awt.event.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.*;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.List;
import org.netbeans.api.editor.mimelookup.MimePath;
import org.netbeans.modules.mercurial.kenai.HgKenaiAccessor;
import org.netbeans.modules.mercurial.HgModuleConfig;
import org.netbeans.modules.mercurial.HgProgressSupport;
import org.netbeans.modules.mercurial.Mercurial;
import org.netbeans.modules.mercurial.ui.diff.DiffSetupSource;
import org.netbeans.modules.mercurial.ui.diff.ExportDiffAction;
import org.netbeans.modules.mercurial.ui.rollback.BackoutAction;
import org.netbeans.modules.mercurial.ui.update.RevertModificationsAction;
import org.netbeans.modules.mercurial.util.HgUtils;
import org.netbeans.modules.versioning.util.VCSHyperlinkSupport;
import org.netbeans.modules.versioning.util.VCSHyperlinkSupport.AuthorLinker;
import org.netbeans.modules.versioning.util.VCSHyperlinkSupport.IssueLinker;
import org.netbeans.modules.versioning.util.VCSHyperlinkSupport.StyledDocumentHyperlink;
import org.netbeans.modules.versioning.util.VCSHyperlinkProvider;
import org.netbeans.modules.versioning.util.VCSKenaiAccessor.KenaiUser;
import org.openide.util.Lookup;

/**
 * @author Maros Sandor
 */
/**
 * Shows Search History results in a JList.
 * 
 * @author Maros Sandor
 */
class SummaryView implements MouseListener, ComponentListener, MouseMotionListener, DiffSetupSource {

    private static final String SUMMARY_DIFF_PROPERTY = "Summary-Diff-";
    private static final String SUMMARY_REVERT_PROPERTY = "Summary-Revert-";
    private static final String HLINK_ISSUE_PROPERTY = "Hyperlink-Issue-";
    private static final String SUMMARY_EXPORTDIFFS_PROPERTY = "Summary-ExportDiffs-";

    private final SearchHistoryPanel master;
    
    private JList       resultsList;
    private JScrollPane scrollPane;

    private final List  dispResults;
    private String      message;
    private AttributeSet searchHiliteAttrs;
    private List<RepositoryRevision> results;

    private Map<String, KenaiUser> kenaiUsersMap = null;
    private VCSHyperlinkSupport linkerSupport = new VCSHyperlinkSupport();
    
    public SummaryView(SearchHistoryPanel master, List<RepositoryRevision> results) {
        this.master = master;
        this.results = results;
        this.dispResults = expandResults(results);
        FontColorSettings fcs = (FontColorSettings) MimeLookup.getLookup(MimePath.get("text/x-java")).lookup(FontColorSettings.class); // NOI18N
        searchHiliteAttrs = fcs.getFontColors("highlight-search"); // NOI18N
        message = master.getCriteria().getCommitMessage();
        resultsList = new JList(new SummaryListModel());
        resultsList.setFixedCellHeight(-1);
        resultsList.addMouseListener(this);
        resultsList.addMouseMotionListener(this);
        resultsList.setCellRenderer(new SummaryCellRenderer());
        resultsList.getAccessibleContext().setAccessibleName(NbBundle.getMessage(SummaryView.class, "ACSN_SummaryView_List")); // NOI18N
        resultsList.getAccessibleContext().setAccessibleDescription(NbBundle.getMessage(SummaryView.class, "ACSD_SummaryView_List")); // NOI18N
        scrollPane = new JScrollPane(resultsList, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        master.addComponentListener(this);
        resultsList.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT ).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_F10, KeyEvent.SHIFT_DOWN_MASK ), "org.openide.actions.PopupAction");
        resultsList.getActionMap().put("org.openide.actions.PopupAction", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onPopup(org.netbeans.modules.versioning.util.Utils.getPositionForPopup(resultsList));
            }
        });

        if(results.size() > 0) {
            String url = HgUtils.getRemoteRepository(results.get(0).getRepositoryRoot());
            boolean isKenaiRepository = url != null && HgKenaiAccessor.getInstance().isKenai(url);
            if(isKenaiRepository) {
                kenaiUsersMap = new HashMap<String, KenaiUser>();
                for (RepositoryRevision repositoryRevision : results) {
                    String author = repositoryRevision.getLog().getAuthor();
                    String username = repositoryRevision.getLog().getUsername();
                    if(author != null && !author.equals("") && username != null && !"".equals(username)) {
                        if(!kenaiUsersMap.keySet().contains(author)) {
                            KenaiUser kenaiUser = HgKenaiAccessor.getInstance().forName(username, url);
                            if(kenaiUser != null) {
                                kenaiUsersMap.put(author, kenaiUser);
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void componentResized(ComponentEvent e) {
        int [] selection = resultsList.getSelectedIndices();
        resultsList.setModel(new SummaryListModel());
        resultsList.setSelectedIndices(selection);
    }

    @Override
    public void componentHidden(ComponentEvent e) {
        // not interested
    }

    @Override
    public void componentMoved(ComponentEvent e) {
        // not interested
    }

    @Override
    public void componentShown(ComponentEvent e) {
        // not interested
    }
    
    @SuppressWarnings("unchecked")
    private List expandResults(List<RepositoryRevision> results) {
        ArrayList newResults = new ArrayList(results.size());
        for (RepositoryRevision repositoryRevision : results) {
            newResults.add(repositoryRevision);
            List<RepositoryRevision.Event> events = repositoryRevision.getEvents();
            for (RepositoryRevision.Event event : events) {
                newResults.add(event);
            }
        }
        return newResults;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        int idx = resultsList.locationToIndex(e.getPoint());
        if (idx == -1) return;
        Rectangle rect = resultsList.getCellBounds(idx, idx);
        Point p = new Point(e.getX() - rect.x, e.getY() - rect.y);
        Rectangle diffBounds = (Rectangle) resultsList.getClientProperty(SUMMARY_DIFF_PROPERTY + idx); // NOI18N
        if (diffBounds != null && diffBounds.contains(p)) {
            diffPrevious(idx);
        }
        diffBounds = (Rectangle) resultsList.getClientProperty(SUMMARY_REVERT_PROPERTY + idx); // NOI18N
        if (diffBounds != null && diffBounds.contains(p)) {
            revertModifications(new int [] { idx });
        }
        diffBounds = (Rectangle) resultsList.getClientProperty(SUMMARY_EXPORTDIFFS_PROPERTY + idx); // NOI18N
        if (diffBounds != null && diffBounds.contains(p)) {
            exportDiffs(idx);
        }
        linkerSupport.mouseClicked(p, idx);

    }
    
    @Override
    public void mouseEntered(MouseEvent e) {
        // not interested
    }

    @Override
    public void mouseExited(MouseEvent e) {
        // not interested
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (!master.isIncomingSearch() && e.isPopupTrigger()) {
            onPopup(e);
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (!master.isIncomingSearch() && e.isPopupTrigger()) {
            onPopup(e);
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        int idx = resultsList.locationToIndex(e.getPoint());
        if (idx == -1) return;

        resultsList.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        resultsList.setToolTipText("");

        Rectangle rect = resultsList.getCellBounds(idx, idx);
        Point p = new Point(e.getX() - rect.x, e.getY() - rect.y);
        Rectangle diffBounds = (Rectangle) resultsList.getClientProperty(SUMMARY_DIFF_PROPERTY + idx); // NOI18N
        if (diffBounds != null && diffBounds.contains(p)) {
            resultsList.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            return;
        }
        diffBounds = (Rectangle) resultsList.getClientProperty(SUMMARY_REVERT_PROPERTY + idx); // NOI18N
        if (diffBounds != null && diffBounds.contains(p)) {
            resultsList.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            return;
        }
        diffBounds = (Rectangle) resultsList.getClientProperty(SUMMARY_EXPORTDIFFS_PROPERTY + idx); // NOI18N
        if (diffBounds != null && diffBounds.contains(p)) {
            resultsList.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            return;
        }
        linkerSupport.mouseMoved(p, resultsList, idx);
    }

    @Override
    public Collection getSetups() {
        Node [] nodes = TopComponent.getRegistry().getActivatedNodes();
        if (nodes.length == 0) {
            return master.getSetups(results.toArray(new RepositoryRevision[results.size()]), new RepositoryRevision.Event[0]);
        }
    
        Set<RepositoryRevision.Event> events = new HashSet<RepositoryRevision.Event>();
        Set<RepositoryRevision> revisions = new HashSet<RepositoryRevision>();

        int [] sel = resultsList.getSelectedIndices();
        for (int i : sel) {
            Object revCon = dispResults.get(i);            
            if (revCon instanceof RepositoryRevision) {
                revisions.add((RepositoryRevision) revCon);
            } else {
                events.add((RepositoryRevision.Event) revCon);
            }
        }
        return master.getSetups(revisions.toArray(new RepositoryRevision[revisions.size()]), events.toArray(new RepositoryRevision.Event[events.size()]));
    }

    @Override
    public String getSetupDisplayName() {
        return null;
    }

    private void onPopup(MouseEvent e) {
        onPopup(e.getPoint());
    }
    
    private void onPopup(Point p) {
        int [] sel = resultsList.getSelectedIndices();
        if (sel.length == 0) {
            int idx = resultsList.locationToIndex(p);
            if (idx == -1) return;
            resultsList.setSelectedIndex(idx);
            sel = new int [] { idx };
        }
        final int [] selection = sel;

        JPopupMenu menu = new JPopupMenu();
        
        String previousRevision = null;
        RepositoryRevision container = null;
        final RepositoryRevision.Event[] drev;

        Object revCon = dispResults.get(selection[0]);
        
        
        boolean noExDeletedExistingFiles = true;        
        boolean revisionSelected;
        boolean missingFile = false;        
        boolean oneRevisionMultiselected = true;
        
        if (revCon instanceof RepositoryRevision) {
            revisionSelected = true;
            container = (RepositoryRevision) dispResults.get(selection[0]);
            drev = new RepositoryRevision.Event[0];
            oneRevisionMultiselected = true;
            noExDeletedExistingFiles = true;
        } else {
            revisionSelected = false;
            drev = new RepositoryRevision.Event[selection.length];

            for(int i = 0; i < selection.length; i++) {
                drev[i] = (RepositoryRevision.Event) dispResults.get(selection[i]);
                
                if(!missingFile && drev[i].getFile() == null) {
                    missingFile = true;
                }
                if(oneRevisionMultiselected && i > 0 && 
                   drev[0].getLogInfoHeader().getLog().getRevisionNumber().equals(drev[i].getLogInfoHeader().getLog().getRevisionNumber())) 
                {
                    oneRevisionMultiselected = false;
                }                
                if(drev[i].getFile() != null && drev[i].getFile().exists() && drev[i].getChangedPath().getAction() == 'D') {
                    noExDeletedExistingFiles = false;
                }    
            }                
            container = drev[0].getLogInfoHeader();
        }
        long revision = Long.parseLong(container.getLog().getRevisionNumber());

        final boolean revertToEnabled = !missingFile && !revisionSelected && oneRevisionMultiselected;
        final boolean backoutChangeEnabled = !missingFile && oneRevisionMultiselected && (drev.length == 0); // drev.length == 0 => the whole revision was selected
        final boolean viewEnabled = selection.length == 1 && !revisionSelected && drev[0].getFile() != null && !drev[0].getFile().isDirectory();
        final boolean annotationsEnabled = viewEnabled && drev[0].getChangedPath().getAction() != HgLogMessage.HgDelStatus;
        final boolean diffToPrevEnabled = selection.length == 1;
        
        if (revision > 0) {
            menu.add(new JMenuItem(new AbstractAction(NbBundle.getMessage(SummaryView.class, "CTL_SummaryView_DiffToPrevious", "" + previousRevision )) { // NOI18N
                {
                    setEnabled(diffToPrevEnabled);
                }
                @Override
                public void actionPerformed(ActionEvent e) {
                    diffPrevious(selection[0]);
                }
            }));
        }

        if (revisionSelected) {
            menu.add(new JMenuItem(new AbstractAction(NbBundle.getMessage(SummaryView.class, "CTL_SummaryView_RollbackChange")) { // NOI18N

                {
                    setEnabled(backoutChangeEnabled);
                }

                @Override
                public void actionPerformed(ActionEvent e) {
                    backout(selection[0]);
                }
            }));
        }else{
            menu.add(new JMenuItem(new AbstractAction(NbBundle.getMessage(SummaryView.class, "CTL_SummaryView_RollbackTo", "" + revision)) { // NOI18N
                {                    
                    setEnabled(revertToEnabled);
                }
                @Override
                public void actionPerformed(ActionEvent e) {
                    revertModifications(selection);
                }                
            }));
            
            menu.add(new JMenuItem(new AbstractAction(NbBundle.getMessage(SummaryView.class, "CTL_SummaryView_View")) { // NOI18N
                {
                    setEnabled(viewEnabled);
                }
                @Override
                public void actionPerformed(ActionEvent e) {
                    Mercurial.getInstance().getParallelRequestProcessor().post(new Runnable() {
                        @Override
                        public void run() {
                            view(selection[0], false);
                        }
                    });
                }
            }));
            menu.add(new JMenuItem(new AbstractAction(NbBundle.getMessage(SummaryView.class, "CTL_SummaryView_ShowAnnotations")) { // NOI18N
                {
                    setEnabled(annotationsEnabled);
                }
                @Override
                public void actionPerformed(ActionEvent e) {
                    Mercurial.getInstance().getParallelRequestProcessor().post(new Runnable() {
                        @Override
                        public void run() {
                            view(selection[0], true);
                        }
                    });
                }
            }));
            menu.add(new JMenuItem(new AbstractAction(NbBundle.getMessage(SummaryView.class, "CTL_SummaryView_ExportFileDiff")) { // NOI18N
                {
                    setEnabled(viewEnabled);
                }
                @Override
                public void actionPerformed(ActionEvent e) {
                    exportFileDiff(selection[0]);
                }
            }));
        }

        menu.show(resultsList, p.x, p.y);
    }
    
    /**
     * Rollback this changeset only
     *
     * @param event
     */
    private void backout(int idx) {
        Object o = dispResults.get(idx);
        if (o instanceof RepositoryRevision) {
            RepositoryRevision repoRev = (RepositoryRevision) o;
            BackoutAction.backout(repoRev);
        }        
    }
    
    static void backout(final RepositoryRevision.Event event) {
        RepositoryRevision repoRev = event.getLogInfoHeader();
        BackoutAction.backout(repoRev);
    }

    static void revertModifications(final RepositoryRevision.Event event) {
        Set<RepositoryRevision.Event> events = new HashSet<RepositoryRevision.Event>();
        events.add(event);
        revert(null, (RepositoryRevision.Event[]) events.toArray(new RepositoryRevision.Event[events.size()]));
    }

    public void revertModifications(int[] selection) {
        Set<RepositoryRevision.Event> events = new HashSet<RepositoryRevision.Event>();
        Set<RepositoryRevision> revisions = new HashSet<RepositoryRevision>();
        for (int idx : selection) {
            Object o = dispResults.get(idx);
            if (o instanceof RepositoryRevision) {
                revisions.add((RepositoryRevision) o);
            } else {
                events.add((RepositoryRevision.Event) o);
            }
        }
        revert(revisions.toArray(new RepositoryRevision[revisions.size()]), (RepositoryRevision.Event[]) events.toArray(new RepositoryRevision.Event[events.size()]));
    }

    static void revert(final RepositoryRevision [] revisions, final RepositoryRevision.Event [] events) {
        File root;
        if(revisions == null || revisions.length == 0){
            if(events == null || events.length == 0 || events[0].getLogInfoHeader() == null) return;
            root = events[0].getLogInfoHeader().getRepositoryRoot();
        }else{
            root = revisions[0].getRepositoryRoot();
        }
        
        RequestProcessor rp = Mercurial.getInstance().getRequestProcessor(root);
        HgProgressSupport support = new HgProgressSupport() {
            @Override
            public void perform() {
                revertImpl(revisions, events, this);
            }
        };
        support.start(rp, root, NbBundle.getMessage(SummaryView.class, "MSG_Revert_Progress")); // NOI18N
    }

    private static void revertImpl(RepositoryRevision[] revisions, RepositoryRevision.Event[] events, HgProgressSupport progress) {
        List<File> revertFiles = new ArrayList<File>();
        boolean doBackup = HgModuleConfig.getDefault().getBackupOnRevertModifications();
        if (revisions != null) {
            for (RepositoryRevision revision : revisions) {
                File root = revision.getRepositoryRoot();
                for (RepositoryRevision.Event event : revision.getEvents()) {
                    if (event.getFile() == null) {
                        continue;
                    }
                    revertFiles.add(event.getFile());
                }
                RevertModificationsAction.performRevert(
                        root, revision.getLog().getRevisionNumber(), revertFiles, doBackup, progress.getLogger());
                revertFiles.clear();
            }
        }
        
        Map<File, List<RepositoryRevision.Event>> revertMap = new HashMap<File, List<RepositoryRevision.Event>>();
        for (RepositoryRevision.Event event : events) {
            if (event.getFile() == null) continue;
         
            File root = Mercurial.getInstance().getRepositoryRoot(event.getFile());
            if(revertMap == null){
                revertMap = new HashMap<File, List<RepositoryRevision.Event>>();    
            }
            List<RepositoryRevision.Event> revEvents = revertMap.get(root);
            if(revEvents == null){
                revEvents = new ArrayList<RepositoryRevision.Event>();
                revertMap.put(root, revEvents);
            }
            revEvents.add(event);            
        }
        if (events != null && events.length >0 && revertMap != null && !revertMap.isEmpty()){
            Set<File> roots = revertMap.keySet();
            for(File root: roots){
                List<RepositoryRevision.Event> revEvents = revertMap.get(root);
                for(RepositoryRevision.Event event: revEvents){
                    if (event.getFile() == null) continue;
                    revertFiles.add(event.getFile());
                }
                if(revEvents != null && !revEvents.isEmpty()){
                    // Assuming all files in a given repository reverting to same revision
                    RevertModificationsAction.performRevert(
                        root, revEvents.get(0).getLogInfoHeader().getLog().getRevisionNumber(), revertFiles, doBackup, progress.getLogger());
                }
            }                       
        }
        
    }

    private void view (int idx, boolean showAnnotations) {
        Object o = dispResults.get(idx);
        if (o instanceof RepositoryRevision.Event) {
            try {
                final RepositoryRevision.Event drev = (RepositoryRevision.Event) o;
                HgUtils.openInRevision(drev.getFile(), drev.getLogInfoHeader().getLog().getHgRevision(), showAnnotations);
            } catch (IOException ex) {
                // Ignore if file not available in cache
            }
        }
    }
    private void diffPrevious(int idx) {
        Object o = dispResults.get(idx);
        if (o instanceof RepositoryRevision.Event) {
            RepositoryRevision.Event drev = (RepositoryRevision.Event) o;
            master.showDiff(drev);
        } else {
            RepositoryRevision container = (RepositoryRevision) o;
            master.showDiff(container);
        }
    }

    private void exportDiffs(int idx) {
        Object o = dispResults.get(idx);
        if (o instanceof RepositoryRevision) {
            RepositoryRevision repoRev = (RepositoryRevision) o;
            ExportDiffAction.exportDiffRevision(repoRev, master.getRoots());
        }        
    }
    
    private void exportFileDiff(int idx) {
        Object o = dispResults.get(idx);
        if (o instanceof RepositoryRevision.Event) {
            RepositoryRevision.Event drev = (RepositoryRevision.Event) o;
            ExportDiffAction.exportDiffFileRevision(drev);
        }
    }

    public JComponent getComponent() {
        return scrollPane;
    }

    private class SummaryListModel extends AbstractListModel {

        @Override
        public int getSize() {
            return dispResults.size();
        }

        @Override
        public Object getElementAt(int index) {
            return dispResults.get(index);
        }
    }
    
    private class SummaryCellRenderer implements ListCellRenderer {

        private static final String FIELDS_SEPARATOR = "        "; // NOI18N

        private RevisionRenderer rr = new RevisionRenderer();
        private ChangepathRenderer cpr = new ChangepathRenderer();

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            if (value instanceof RepositoryRevision) {
                return rr.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            } else {
                return cpr.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            }
        }

        private class RevisionRenderer extends JPanel implements ListCellRenderer {
            private static final double DARKEN_FACTOR = 0.95;

            private Style selectedStyle;
            private Style normalStyle;
            private Style filenameStyle;
            private Style indentStyle;
            private Style noindentStyle;
            private Style hiliteStyle;
            private Style issueHyperlinkStyle;
            private final Style authorStyle;

            private Color selectionBackground;
            private Color selectionForeground;

            private JTextPane textPane = new JTextPane();
            private JPanel    actionsPane = new JPanel();

            private DateFormat defaultFormat;

            private int             index;
            private HyperlinkLabel  diffLink;
            private HyperlinkLabel  revertLink;
            private HyperlinkLabel  exportDiffsLink;

            public RevisionRenderer() {
                selectionBackground = new JList().getSelectionBackground();
                selectionForeground = new JList().getSelectionForeground();

                selectedStyle = textPane.addStyle("selected", null); // NOI18N
                StyleConstants.setForeground(selectedStyle, selectionForeground);
                StyleConstants.setBackground(selectedStyle, selectionBackground);
                normalStyle = textPane.addStyle("normal", null); // NOI18N
                StyleConstants.setForeground(normalStyle, UIManager.getColor("List.foreground")); // NOI18N
                filenameStyle = textPane.addStyle("filename", normalStyle); // NOI18N
                StyleConstants.setBold(filenameStyle, true);
                indentStyle = textPane.addStyle("indent", null); // NOI18N
                StyleConstants.setLeftIndent(indentStyle, 50);
                noindentStyle = textPane.addStyle("noindent", null); // NOI18N
                StyleConstants.setLeftIndent(noindentStyle, 0);
                defaultFormat = DateFormat.getDateTimeInstance();

                issueHyperlinkStyle = textPane.addStyle("issuehyperlink", normalStyle); //NOI18N
                StyleConstants.setForeground(issueHyperlinkStyle, Color.BLUE);
                StyleConstants.setUnderline(issueHyperlinkStyle, true);

                authorStyle = textPane.addStyle("author", normalStyle); //NOI18N
                StyleConstants.setForeground(authorStyle, Color.BLUE);

                hiliteStyle = textPane.addStyle("hilite", normalStyle); // NOI18N
                Color c = (Color) searchHiliteAttrs.getAttribute(StyleConstants.Background);
                if (c != null) StyleConstants.setBackground(hiliteStyle, c);
                c = (Color) searchHiliteAttrs.getAttribute(StyleConstants.Foreground);
                if (c != null) StyleConstants.setForeground(hiliteStyle, c);

                setLayout(new BorderLayout());
                add(textPane);
                add(actionsPane, BorderLayout.PAGE_END);
                actionsPane.setLayout(new FlowLayout(FlowLayout.TRAILING, 2, 5));

                diffLink = new HyperlinkLabel();
                diffLink.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 8));
                actionsPane.add(diffLink);

                revertLink = new HyperlinkLabel();
                revertLink.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 8));
                actionsPane.add(revertLink);

                exportDiffsLink = new HyperlinkLabel();
                actionsPane.add(exportDiffsLink);

                textPane.setBorder(null);
            }

            public Color darker(Color c) {
                return new Color(Math.max((int)(c.getRed() * DARKEN_FACTOR), 0),
                     Math.max((int)(c.getGreen() * DARKEN_FACTOR), 0),
                     Math.max((int)(c.getBlue() * DARKEN_FACTOR), 0));
            }

            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                renderContainer(list, (RepositoryRevision) value, index, isSelected);
                return this;
            }

            private void renderContainer(JList list, RepositoryRevision container, int index, boolean isSelected) {

                StyledDocument sd = textPane.getStyledDocument();

                Style style;
                Color backgroundColor;
                Color foregroundColor;

                if (isSelected) {
                    foregroundColor = selectionForeground;
                    backgroundColor = selectionBackground;
                    style = selectedStyle;
                } else {
                    foregroundColor = UIManager.getColor("List.foreground"); // NOI18N
                    backgroundColor = UIManager.getColor("List.background"); // NOI18N
                    backgroundColor = darker(backgroundColor);
                    style = normalStyle;
                }
                textPane.setBackground(backgroundColor);
                actionsPane.setBackground(backgroundColor);

                this.index = index;

                // XXX cache
                Lookup.Result<VCSHyperlinkProvider> hpResult = Lookup.getDefault().lookupResult(VCSHyperlinkProvider.class);
                Collection<VCSHyperlinkProvider> hpInstances = (Collection<VCSHyperlinkProvider>) hpResult.allInstances();

                try {
                    // clear document
                    sd.remove(0, sd.getLength());
                    sd.setParagraphAttributes(0, sd.getLength(), noindentStyle, false);

                    // add revision
                    sd.insertString(0, container.getLog().getRevisionNumber() +
                            " (" + container.getLog().getCSetShortID() + ")", null); // NOI18N
                    sd.setCharacterAttributes(0, sd.getLength(), filenameStyle, false);

                    // add author
                    sd.insertString(sd.getLength(), FIELDS_SEPARATOR, style);
                    String author = container.getLog().getAuthor();
                    StyledDocumentHyperlink l = linkerSupport.getLinker(AuthorLinker.class, index);
                    if(l == null) {
                        if(kenaiUsersMap != null && author != null && !author.equals("")) {
                            KenaiUser kenaiUser = kenaiUsersMap.get(author);
                            if(kenaiUser != null) {
                                l = new AuthorLinker(kenaiUser, authorStyle, sd, author);
                                linkerSupport.add(l, index);
                            }
                        }
                    }
                    if(l != null) {
                        l.insertString(sd, isSelected ? style : null);
                    } else {
                        sd.insertString(sd.getLength(), author, style);
                    }

                    // add date
                    sd.insertString(sd.getLength(), FIELDS_SEPARATOR + defaultFormat.format(container.getLog().getDate()), null);

                    // add commit msg
                    String commitMessage = container.getLog().getMessage();
                    if (commitMessage.endsWith("\n")) commitMessage = commitMessage.substring(0, commitMessage.length() - 1); // NOI18N
                    sd.insertString(sd.getLength(), "\n", null);

                    // compute issue hyperlinks
                    l = linkerSupport.getLinker(IssueLinker.class, index);
                    if(l == null) {
                        for (VCSHyperlinkProvider hp : hpInstances) {
                            l = IssueLinker.create(hp, issueHyperlinkStyle, master.getRoots()[0], sd, commitMessage);
                            if(l != null) {
                                linkerSupport.add(l, index);
                                break; // get the first one
                            }
                        }
                    }
                    if(l != null) {
                        l.insertString(sd, style);
                    } else {
                        sd.insertString(sd.getLength(), commitMessage, style);
                    }

                    int msglen = commitMessage.length();
                    int doclen = sd.getLength();
                    if (message != null && !isSelected) {
                        int idx = commitMessage.indexOf(message);
                        if (idx != -1) {
                            sd.setCharacterAttributes(doclen - msglen + idx, message.length(), hiliteStyle, true);
                        }
                    }

                    resizePane(commitMessage, list.getFontMetrics(list.getFont()));
                    if(isSelected) {
                        sd.setCharacterAttributes(0, Integer.MAX_VALUE, style, false);
                    }
                } catch (BadLocationException e) {
                    ErrorManager.getDefault().notify(e);
                }

                actionsPane.setVisible(true);
                if(!master.isIncomingSearch()){
                    diffLink.set(NbBundle.getMessage(SummaryView.class, "CTL_Action_Diff"), foregroundColor, backgroundColor);// NOI18N
                    revertLink.set(NbBundle.getMessage(SummaryView.class, "CTL_Action_Revert"), foregroundColor, backgroundColor); // NOI18N
                    exportDiffsLink.set(NbBundle.getMessage(SummaryView.class, "CTL_Action_ExportDiffs"), foregroundColor, backgroundColor); // NOI18N
                }
            }

            @SuppressWarnings("empty-statement")
            private void resizePane(String text, FontMetrics fm) {
                if(text == null) {
                    text = "";
                }
                int width = master.getWidth();
                if (width > 0) {
                    Rectangle2D rect = fm.getStringBounds(text, textPane.getGraphics());
                    int nlc, i;
                    for (nlc = -1, i = 0; i != -1 ; i = text.indexOf('\n', i + 1), nlc++);
                    nlc++;
                    int lines = (int) (rect.getWidth() / (width - 80) + 1);
                    int ph = fm.getHeight() * (lines + nlc) + 0;
                    textPane.setPreferredSize(new Dimension(width - 50, ph));
                }
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (index == -1) return;
                Rectangle apb = actionsPane.getBounds();

                {
                    Rectangle bounds = diffLink.getBounds();
                    bounds.setBounds(bounds.x, bounds.y + apb.y, bounds.width, bounds.height);
                    resultsList.putClientProperty(SUMMARY_DIFF_PROPERTY + index, bounds); // NOI18N
                }

                Rectangle bounds = revertLink.getBounds();
                bounds.setBounds(bounds.x, bounds.y + apb.y, bounds.width, bounds.height);
                resultsList.putClientProperty(SUMMARY_REVERT_PROPERTY + index, bounds); // NOI18N

                Rectangle edBounds = exportDiffsLink.getBounds();
                edBounds.setBounds(edBounds.x, edBounds.y + apb.y, edBounds.width, edBounds.height);
                resultsList.putClientProperty(SUMMARY_EXPORTDIFFS_PROPERTY + index, edBounds); // NOI18N

                linkerSupport.computeBounds(textPane, index);
            }
        }

        private class ChangepathRenderer extends DefaultListCellRenderer {
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                RepositoryRevision.Event revisionEvent = (RepositoryRevision.Event) value;
                StringBuilder sb = new StringBuilder();
                sb.append(FIELDS_SEPARATOR);
                sb.append(String.valueOf(revisionEvent.getChangedPath().getAction()));
                sb.append(FIELDS_SEPARATOR);
                sb.append(revisionEvent.getChangedPath().getPath());
                Component renderer = super.getListCellRendererComponent(list, sb.toString(), index, isSelected, isSelected);
                if(renderer instanceof JLabel) {
                    ((JLabel) renderer).setToolTipText(sb.toString());
                }
                return renderer;
            }
        }
    }

    private static class HyperlinkLabel extends JLabel {

        public HyperlinkLabel() {
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        public void set(String text, Color foreground, Color background) {
            StringBuilder sb = new StringBuilder(100);
            if (foreground.equals(UIManager.getColor("List.foreground"))) { // NOI18N
                sb.append("<html><a href=\"\">"); // NOI18N
                sb.append(text);
                sb.append("</a>"); // NOI18N
            } else {
                sb.append("<html><a href=\"\" style=\"color:"); // NOI18N
                sb.append("rgb("); // NOI18N
                sb.append(foreground.getRed());
                sb.append(","); // NOI18N
                sb.append(foreground.getGreen());
                sb.append(","); // NOI18N
                sb.append(foreground.getBlue());
                sb.append(")"); // NOI18N
                sb.append("\">"); // NOI18N
                sb.append(text);
                sb.append("</a>"); // NOI18N
            }
            setText(sb.toString());
            setBackground(background);
        }
    }
}
