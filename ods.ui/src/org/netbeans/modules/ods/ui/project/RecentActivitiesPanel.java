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
package org.netbeans.modules.ods.ui.project;

import com.tasktop.c2c.server.profile.domain.activity.BuildActivity;
import com.tasktop.c2c.server.profile.domain.activity.ProjectActivity;
import com.tasktop.c2c.server.profile.domain.activity.ScmActivity;
import com.tasktop.c2c.server.profile.domain.activity.TaskActivity;
import com.tasktop.c2c.server.profile.domain.activity.WikiActivity;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import org.netbeans.modules.ods.api.ODSProject;
import org.netbeans.modules.ods.client.api.ODSClient;
import org.netbeans.modules.ods.client.api.ODSException;
import org.netbeans.modules.ods.ui.utils.Utils;
import org.netbeans.modules.team.ui.spi.ProjectHandle;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;

/**
 *
 * @author jpeska
 */
public class RecentActivitiesPanel extends javax.swing.JPanel {

    private static final RequestProcessor RP = new RequestProcessor(RecentActivitiesPanel.class);
    private final ProjectHandle<ODSProject> projectHandle;
    private final ODSClient client;
    private JCheckBox chbTask;
    private JCheckBox chbWiki;
    private JCheckBox chbBuild;
    private JCheckBox chbScm;
    private List<ProjectActivity> recentActivities = Collections.emptyList();
    private int maxWidth = -1;

    /**
     * Creates new form RecentActivitiesPanel
     */
    public RecentActivitiesPanel(ODSClient client, ProjectHandle<ODSProject> projectHandle) {
        this.client = client;
        this.projectHandle = projectHandle;
        initComponents();
        loadRecentActivities();
    }

    void update() {
        loadRecentActivities();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        lblError = new javax.swing.JLabel();
        lblEmptyContent = new javax.swing.JLabel();
        pnlTitle = new TitlePanel();
        lblTitle = new javax.swing.JLabel();
        pnlShow = new javax.swing.JPanel();
        lblShow = new javax.swing.JLabel();
        pnlContent = new javax.swing.JPanel();
        lblLoading = new javax.swing.JLabel();

        lblError.setForeground(new java.awt.Color(255, 0, 0));
        lblError.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/netbeans/modules/ods/ui/resources/error.png"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(lblError, org.openide.util.NbBundle.getMessage(RecentActivitiesPanel.class, "RecentActivitiesPanel.lblError.text")); // NOI18N

        lblEmptyContent.setFont(new java.awt.Font("Tahoma", 2, 11)); // NOI18N
        lblEmptyContent.setForeground(new java.awt.Color(102, 102, 102));
        org.openide.awt.Mnemonics.setLocalizedText(lblEmptyContent, org.openide.util.NbBundle.getMessage(RecentActivitiesPanel.class, "RecentActivitiesPanel.lblEmptyContent.text")); // NOI18N

        setBackground(new java.awt.Color(255, 255, 255));

        lblTitle.setFont(lblTitle.getFont().deriveFont(lblTitle.getFont().getStyle() | java.awt.Font.BOLD));
        org.openide.awt.Mnemonics.setLocalizedText(lblTitle, org.openide.util.NbBundle.getMessage(RecentActivitiesPanel.class, "RecentActivitiesPanel.lblTitle.text")); // NOI18N

        pnlShow.setOpaque(false);
        pnlShow.setLayout(new java.awt.GridBagLayout());

        org.openide.awt.Mnemonics.setLocalizedText(lblShow, org.openide.util.NbBundle.getMessage(RecentActivitiesPanel.class, "RecentActivitiesPanel.lblShow.text")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 7);
        pnlShow.add(lblShow, gridBagConstraints);

        javax.swing.GroupLayout pnlTitleLayout = new javax.swing.GroupLayout(pnlTitle);
        pnlTitle.setLayout(pnlTitleLayout);
        pnlTitleLayout.setHorizontalGroup(
            pnlTitleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlTitleLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lblTitle)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 247, Short.MAX_VALUE)
                .addComponent(pnlShow, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        pnlTitleLayout.setVerticalGroup(
            pnlTitleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlTitleLayout.createSequentialGroup()
                .addGap(5, 5, 5)
                .addComponent(lblTitle)
                .addGap(3, 3, 3))
            .addComponent(pnlShow, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        pnlContent.setOpaque(false);
        pnlContent.setLayout(new java.awt.GridBagLayout());

        lblLoading.setFont(new java.awt.Font("Tahoma", 2, 11)); // NOI18N
        lblLoading.setForeground(new java.awt.Color(102, 102, 102));
        org.openide.awt.Mnemonics.setLocalizedText(lblLoading, org.openide.util.NbBundle.getMessage(RecentActivitiesPanel.class, "RecentActivitiesPanel.lblLoading.text")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.weighty = 0.1;
        gridBagConstraints.insets = new java.awt.Insets(100, 0, 0, 0);
        pnlContent.add(lblLoading, gridBagConstraints);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(pnlTitle, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(pnlContent, javax.swing.GroupLayout.DEFAULT_SIZE, 390, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(pnlTitle, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 333, Short.MAX_VALUE))
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                    .addGap(24, 24, 24)
                    .addComponent(pnlContent, javax.swing.GroupLayout.DEFAULT_SIZE, 331, Short.MAX_VALUE)))
        );
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel lblEmptyContent;
    private javax.swing.JLabel lblError;
    private javax.swing.JLabel lblLoading;
    private javax.swing.JLabel lblShow;
    private javax.swing.JLabel lblTitle;
    private javax.swing.JPanel pnlContent;
    private javax.swing.JPanel pnlShow;
    private javax.swing.JPanel pnlTitle;
    // End of variables declaration//GEN-END:variables

    private void loadRecentActivities() {
        RP.post(new Runnable() {
            @Override
            public void run() {
                try {
                    final List<ProjectActivity> recentActivities = client.getRecentActivities(projectHandle.getTeamProject().getId());
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            if (recentActivities != null && !recentActivities.isEmpty()) {
                                RecentActivitiesPanel.this.recentActivities = recentActivities;
                                createShowButtons();
                                showRecentActivities();
                            } else {
                                RecentActivitiesPanel.this.recentActivities = Collections.emptyList();
                                showEmptyContent();
                            }
                        }
                    });
                } catch (ODSException ex) {
                    Utils.getLogger().log(Level.SEVERE, ex.getMessage());
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            showError();
                        }
                    });
                }
            }
        });
    }

    private void createShowButtons() {

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.EAST;
        gbc.gridheight = GridBagConstraints.REMAINDER;

        if (chbTask != null) {
            pnlShow.remove(chbTask);
        }
        chbTask = new JCheckBox(NbBundle.getMessage(RecentActivitiesPanel.class, "LBL_Tasks"), Utils.Settings.isShowTasks());
        chbTask.setOpaque(false);
        chbTask.addActionListener(new ShowActionListener());
        pnlShow.add(chbTask, gbc);

        if (chbWiki != null) {
            pnlShow.remove(chbWiki);
        }
        chbWiki = new JCheckBox(NbBundle.getMessage(RecentActivitiesPanel.class, "LBL_Wiki"), Utils.Settings.isShowWiki());
        chbWiki.setOpaque(false);
        chbWiki.addActionListener(new ShowActionListener());
        pnlShow.add(chbWiki, gbc);

        if (chbScm != null) {
            pnlShow.remove(chbScm);
        }
        chbScm = new JCheckBox(NbBundle.getMessage(RecentActivitiesPanel.class, "LBL_Commits"), Utils.Settings.isShowScm());
        chbScm.setOpaque(false);
        chbScm.addActionListener(new ShowActionListener());
        pnlShow.add(chbScm, gbc);

        if (chbBuild != null) {
            pnlShow.remove(chbBuild);
        }
        chbBuild = new JCheckBox(NbBundle.getMessage(RecentActivitiesPanel.class, "LBL_Builds"), Utils.Settings.isShowBuilds());
        chbBuild.setOpaque(false);
        chbBuild.addActionListener(new ShowActionListener());
        pnlShow.add(chbBuild, gbc);

        pnlShow.revalidate();
    }

    private void showRecentActivities() {
        Date lastDate = null;
        DateSeparatorPanel currentDatePanel = null;
        pnlContent.removeAll();
        updateShowButtons();
        for (ProjectActivity activity : recentActivities) {
            if (!isActivityAllowed(activity)) {
                continue;
            }
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(0, 3, 0, 0);
            gbc.anchor = GridBagConstraints.NORTHWEST;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1.0;
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            Date currentDate = activity.getActivityDate();
            // group activities by days
            if (isAnotherDay(lastDate, currentDate)) {
                GridBagConstraints gbc1 = new GridBagConstraints();
                gbc1.insets = new Insets(3, 3, 0, 3);
                gbc1.anchor = GridBagConstraints.NORTHWEST;
                gbc1.fill = GridBagConstraints.HORIZONTAL;
                gbc1.weightx = 1.0;
                gbc1.gridwidth = GridBagConstraints.REMAINDER;
                currentDatePanel = new DateSeparatorPanel(currentDate);
                currentDatePanel.addMouseListener(new ExpandableMouseListener(currentDatePanel, this));
                pnlContent.add(currentDatePanel, gbc1);
            }
            if (maxWidth == -1) {
                maxWidth = this.getVisibleRect().width - 150;
            }

            final ActivityPanel activityPanel = new ActivityPanel(activity, projectHandle, maxWidth);
            if (activityPanel.hasDetails()) {
                activityPanel.addMouseListener(new ExpandableMouseListener(activityPanel, this));
            }
            currentDatePanel.addActivityPanel(activityPanel, gbc);
            lastDate = currentDate;
        }

        GridBagConstraints gbc1 = new GridBagConstraints();
        gbc1.weighty = 1.0;
        gbc1.fill = GridBagConstraints.VERTICAL;
        pnlContent.add(new JLabel(), gbc1);
        pnlContent.revalidate();
        this.repaint();
    }

    private boolean isActivityAllowed(ProjectActivity activity) {
        return activity instanceof TaskActivity && chbTask.isSelected()
                || activity instanceof ScmActivity && chbScm.isSelected()
                || activity instanceof WikiActivity && chbWiki.isSelected()
                || activity instanceof BuildActivity && chbBuild.isSelected();
    }

    private void updateShowButtons() {
        boolean taskEnable = isTaskEnable(recentActivities);
        chbTask.setSelected(Utils.Settings.isShowTasks() && taskEnable);
        chbTask.setVisible(taskEnable);

        boolean wikiEnable = isWikiEnable(recentActivities);
        chbWiki.setSelected(Utils.Settings.isShowWiki() && wikiEnable);
        chbWiki.setVisible(wikiEnable);

        boolean scmEnable = isScmEnable(recentActivities);
        chbScm.setSelected(Utils.Settings.isShowScm() && scmEnable);
        chbScm.setVisible(scmEnable);

        boolean buildEnable = isBuildEnable(recentActivities);
        chbBuild.setSelected(Utils.Settings.isShowBuilds() && buildEnable);
        chbBuild.setVisible(buildEnable);
    }

    private boolean isTaskEnable(List<ProjectActivity> recentActivities) {
        for (ProjectActivity activity : recentActivities) {
            if (activity instanceof TaskActivity) {
                return true;
            }
        }
        return false;
    }

    private boolean isScmEnable(List<ProjectActivity> recentActivities) {
        for (ProjectActivity activity : recentActivities) {
            if (activity instanceof ScmActivity) {
                return true;
            }
        }
        return false;
    }

    private boolean isWikiEnable(List<ProjectActivity> recentActivities) {
        for (ProjectActivity activity : recentActivities) {
            if (activity instanceof WikiActivity) {
                return true;
            }
        }
        return false;
    }

    private boolean isBuildEnable(List<ProjectActivity> recentActivities) {
        for (ProjectActivity activity : recentActivities) {
            if (activity instanceof BuildActivity) {
                return true;
            }
        }
        return false;
    }

    private void showEmptyContent() {
        pnlContent.removeAll();
        pnlContent.add(lblEmptyContent, new GridBagConstraints());
        this.repaint();
    }

    private void showError() {
        pnlContent.removeAll();
        pnlContent.add(lblError, new GridBagConstraints());
        this.repaint();
    }

    private boolean isAnotherDay(Date lastDate, Date newDate) {
        if (lastDate == null) {
            return true;
        }
        return lastDate.getYear() != newDate.getYear() || lastDate.getMonth() != newDate.getMonth() || lastDate.getDay() != newDate.getDay();
    }

    private void persistShowSettings() {
        Utils.Settings.setShowBuilds(chbBuild.isSelected());
        Utils.Settings.setShowScm(chbScm.isSelected());
        Utils.Settings.setShowTasks(chbTask.isSelected());
        Utils.Settings.setShowWiki(chbWiki.isSelected());
    }

    private class ShowActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            persistShowSettings();
            showRecentActivities();
        }
    }
}
