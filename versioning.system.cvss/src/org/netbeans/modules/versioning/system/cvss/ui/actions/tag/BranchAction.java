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

package org.netbeans.modules.versioning.system.cvss.ui.actions.tag;

import org.netbeans.modules.versioning.system.cvss.ui.actions.AbstractSystemAction;
import org.netbeans.modules.versioning.system.cvss.ui.actions.update.UpdateExecutor;
import org.netbeans.modules.versioning.system.cvss.FileInformation;
import org.netbeans.modules.versioning.system.cvss.CvsVersioningSystem;
import org.netbeans.modules.versioning.system.cvss.ExecutorSupport;
import org.netbeans.modules.versioning.system.cvss.util.Context;
import org.netbeans.lib.cvsclient.command.tag.TagCommand;
import org.netbeans.lib.cvsclient.command.update.UpdateCommand;
import org.netbeans.lib.cvsclient.command.GlobalOptions;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.openide.util.HelpCtx;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.Dialog;
import java.io.File;
import java.text.MessageFormat;

/**
 * Performs the CVS 'tag -b' command on selected nodes.
 * 
 * @author Maros Sandor
 */
public class BranchAction extends AbstractSystemAction {

    private static final int enabledForStatus = FileInformation.STATUS_VERSIONED_MERGE
                    | FileInformation.STATUS_VERSIONED_MODIFIEDINREPOSITORY 
                    | FileInformation.STATUS_VERSIONED_MODIFIEDLOCALLY 
                    | FileInformation.STATUS_VERSIONED_REMOVEDINREPOSITORY
                    | FileInformation.STATUS_VERSIONED_UPTODATE;
    
    protected String getBaseName() {
        return "CTL_MenuItem_Branch";
    }

    protected int getFileEnabledStatus() {
        return enabledForStatus;
    }

    public void performCvsAction(ActionEvent ev) {
        Context context = getContext();

        String title = MessageFormat.format(NbBundle.getBundle(BranchAction.class).getString("CTL_BranchDialog_Title"),
                                     new Object[] { getContextDisplayName() });

        JButton branch = new JButton(NbBundle.getMessage(BranchAction.class, "CTL_BranchDialog_Action_Branch"));
        JButton cancel = new JButton(NbBundle.getMessage(BranchAction.class, "CTL_BranchDialog_Action_Cancel"));
        BranchSettings settings = new BranchSettings(context.getFiles());
        DialogDescriptor descriptor = new DialogDescriptor(
                settings,
                title,
                true,
                new Object [] { branch, cancel },
                branch,
                DialogDescriptor.DEFAULT_ALIGN,
                new HelpCtx(BranchAction.class),
                null);
        descriptor.setClosingOptions(null);

        settings.putClientProperty("org.openide.DialogDescriptor", descriptor);
        Dialog dialog = DialogDisplayer.getDefault().createDialog(descriptor);
        dialog.setVisible(true);
        if (descriptor.getValue() != branch) return;

        settings.saveSettings();

        RequestProcessor.getDefault().post(new BranchExecutor(context, settings));
    }
    
    private static class BranchExecutor implements Runnable {

        private final Context context;
        private final BranchSettings settings;

        public BranchExecutor(Context context, BranchSettings settings) {
            this.context = context;
            this.settings = settings;
        }

        public void run() {
            if (settings.isTaggingBase()) {
                if (!tag(context.getFiles(), settings.computeBaseTagName())) return;
            }
            if (!branch(context.getFiles(), settings.getBranchName())) return;
            if (settings.isCheckingOutBranch()) {
                update(context, settings.getBranchName());
            }
        }

        private void update(Context context, String revision) {
            UpdateCommand cmd = new UpdateCommand();

            GlobalOptions options = new GlobalOptions();
            if (context.getExclusions().size() > 0) {
                options.setExclusions((File[]) context.getExclusions().toArray(new File[context.getExclusions().size()]));
            }
            cmd.setUpdateByRevision(revision);
            cmd.setFiles(context.getRootFiles());
        
            UpdateExecutor [] executors = UpdateExecutor.executeCommand(cmd, CvsVersioningSystem.getInstance(), options);
            ExecutorSupport.notifyError(executors);
        }

        private boolean branch(File[] roots, String branchName) {
            TagCommand cmd = new TagCommand();

            cmd.setMakeBranchTag(true);
            cmd.setFiles(roots);
            cmd.setTag(branchName);
        
            TagExecutor [] executors = TagExecutor.executeCommand(cmd, CvsVersioningSystem.getInstance(), null);
            return ExecutorSupport.wait(executors);
        }

        private boolean tag(File[] roots, String tagName) {
            TagCommand cmd = new TagCommand();
        
            cmd.setFiles(roots);
            cmd.setTag(tagName);
        
            TagExecutor [] executors = TagExecutor.executeCommand(cmd, CvsVersioningSystem.getInstance(), null);
            return ExecutorSupport.wait(executors);
        }
    }
}
