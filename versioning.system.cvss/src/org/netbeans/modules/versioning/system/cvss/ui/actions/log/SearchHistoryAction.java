/*
 *                 Sun Public License Notice
 *
 * The contents of this file are subject to the Sun Public License
 * Version 1.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://www.sun.com/
 *
 * The Original Code is NetBeans. The Initial Developer of the Original
 * Code is Sun Microsystems, Inc. Portions Copyright 1997-2005 Sun
 * Microsystems, Inc. All Rights Reserved.
 */

package org.netbeans.modules.versioning.system.cvss.ui.actions.log;

import org.netbeans.modules.versioning.system.cvss.ui.actions.AbstractSystemAction;
import org.netbeans.modules.versioning.system.cvss.ui.history.SearchHistoryTopComponent;
import org.netbeans.modules.versioning.system.cvss.FileInformation;
import org.netbeans.modules.versioning.system.cvss.util.Utils;
import org.netbeans.modules.versioning.system.cvss.util.Context;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ui.OpenProjects;
import org.openide.util.NbBundle;
import org.openide.nodes.Node;

import javax.swing.*;
import java.util.*;
import java.io.File;

/**
 * Search History action.
 *
 * @author Maros Sandor
 */
public class SearchHistoryAction extends AbstractSystemAction  {

    protected String getBaseName() {
        return "CTL_MenuItem_SearchHistory";
    }

    protected int getFileEnabledStatus() {
        return FileInformation.STATUS_IN_REPOSITORY;
    }

    protected int getDirectoryEnabledStatus() {
        return FileInformation.STATUS_MANAGED & ~FileInformation.STATUS_NOTVERSIONED_EXCLUDED & ~FileInformation.STATUS_NOTVERSIONED_NEWLOCALLY;
    }

    protected boolean asynchronous() {
        return false;
    }

    public void performCvsAction(Node[] nodes) {
        String title = NbBundle.getMessage(SearchHistoryAction.class, "CTL_SearchHistory_Title", getContextDisplayName());
        openHistory(getContext(nodes), title);
    }

    private void openHistory(final Context context, final String title) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                SearchHistoryTopComponent tc = new SearchHistoryTopComponent(context);
                tc.setDisplayName(title);
                tc.open();
                tc.requestActive();
                File [] files = context.getFiles();
                if (files.length == 1 && files[0].isFile()) {
                    tc.search();
                }
            }
        });
    }

    /**
     * Opens the Seach History panel with given pre-filled values. The search is executed in default context
     * (all open projects). 
     * 
     * @param title title of the search
     * @param commitMessage commit message to search for
     * @param username user name to search for
     * @param date date of the change in question
     */ 
    public static void openSearch(String title, String commitMessage, String username, Date date) {
        openSearch(getDefaultContext(), title, commitMessage, username, date);
    }

    public static void openSearch(Context context, String title, String commitMessage, String username, Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        // annotations do not include time information, we must search whole day
        c.add(Calendar.DATE, 1);
        Date to = c.getTime();
        c.setTime(date);
        c.add(Calendar.DATE, -1);
        Date from = c.getTime();
        
        if (commitMessage != null && commitMessage.indexOf('\n') != -1) {
            commitMessage = commitMessage.substring(0, commitMessage.indexOf('\n'));
        }
        SearchHistoryTopComponent tc = new SearchHistoryTopComponent(context, commitMessage, username, from, to);
        String tcTitle = NbBundle.getMessage(SearchHistoryAction.class, "CTL_SearchHistory_Title", title);
        tc.setDisplayName(tcTitle);
        tc.open();
        tc.requestActive();
        tc.search();
    }

    private static Context getDefaultContext() {
        Project [] projects = OpenProjects.getDefault().getOpenProjects();
        return Utils.getProjectsContext(projects);
    }
}
