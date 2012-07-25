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
 * under the [CDDL or GPL Version 2x] license." If you do not indicate a
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
package org.netbeans.modules.c2c.tasks;

import com.tasktop.c2c.server.tasks.domain.Product;
import com.tasktop.c2c.server.tasks.domain.TaskUserProfile;

import org.eclipse.mylyn.internal.tasks.core.data.FileTaskAttachmentSource;

import org.eclipse.mylyn.tasks.core.IRepositoryElement;
import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.data.TaskData;
import org.eclipse.mylyn.tasks.core.RepositoryResponse;
import org.eclipse.mylyn.tasks.core.data.AbstractTaskAttachmentHandler;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;


import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.mylyn.tasks.core.AbstractRepositoryConnector;
import org.netbeans.modules.c2c.tasks.spi.C2CData;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle.Messages;
import org.netbeans.modules.c2c.tasks.util.C2CUtil;

@ActionID(
    category = "Versioning",
id = "org.netbeans.modules.c2c.tasks.CreateCloudedIssueAction")
@ActionRegistration(
    displayName = "#CTL_CreateClouded")
@ActionReference(path = "Menu/Versioning", position = -10, separatorBefore = -60, separatorAfter = 40)
@Messages("CTL_CreateClouded=Create Clouded Issue")
public final class CreateCloudedIssueAction implements ActionListener {
    
    private NullProgressMonitor nullProgressMonitor = new NullProgressMonitor();
    private static final String ATTACHMENT_DATA = "attachment data";
    
    @Override
    public void actionPerformed(ActionEvent e) {
        try {

            AbstractRepositoryConnector rc = DummyUtils.rc;
            TaskRepository taskRepository = DummyUtils.repository;
            C2CData clientData = DummyUtils.getClientData(taskRepository);
            
            // create
            
            TaskData taskData = createIssue(taskRepository, "this is my bug", "a bug", "bug");
            
            printTaskData(taskData); 
            
            // change
            TaskAttribute rta = taskData.getRoot();
            TaskAttribute ta = rta.getMappedAttribute(TaskAttribute.SUMMARY);
            ta.setValue(ta.getValue() + ".2");
            
            RepositoryResponse rr = C2CUtil.postTaskData(rc, taskRepository, taskData);
            String taskId = rr.getTaskId();
            taskData = rc.getTaskData(taskRepository, taskId, nullProgressMonitor);

            printTaskData(taskData); 
            
            // change custom field            
            rta = taskData.getRoot();
            ta = rta.getMappedAttribute(C2CData.CUSTOM_FIELD_PREFIX + clientData.getCustomFields().get(0).getName());
            ta.setValue("custom value");
            
            rr = C2CUtil.postTaskData(rc, taskRepository, taskData);
            taskData = rc.getTaskData(taskRepository, rr.getTaskId(), nullProgressMonitor);

            printTaskData(taskData); 
            C2C.LOG.log(Level.FINE, "   custom field name : " + clientData.getCustomFields().get(0).getName());
            C2C.LOG.log(Level.FINE, "   custom field value : " + taskData.getRoot().getAttribute(C2CData.CUSTOM_FIELD_PREFIX + clientData.getCustomFields().get(0).getName()).getValue());
            
            // reassign
            rta = taskData.getRoot();
            ta = rta.getMappedAttribute(C2CData.ATTR_OWNER);
            ta.setValue(getDifferentUser(ta.getValue(), clientData.getUsers()));
            
            rr = C2CUtil.postTaskData(rc, taskRepository, taskData);
            taskId = rr.getTaskId();
            taskData = rc.getTaskData(taskRepository, taskId, nullProgressMonitor);
            
            printTaskData(taskData);
            
            // create attachment
            File f = File.createTempFile("attachment", "txt");
            FileOutputStream fos = new FileOutputStream(f);
            fos.write(ATTACHMENT_DATA.getBytes());
            fos.flush();
            fos.close();
            
            AbstractTaskAttachmentHandler ah = rc.getTaskAttachmentHandler();
            TaskAttribute attAttribute = new TaskAttribute(taskData.getRoot(),  TaskAttribute.TYPE_ATTACHMENT);
            ta = attAttribute.createMappedAttribute(TaskAttribute.ATTACHMENT_DESCRIPTION);
            ta.setValue("adding attachment");
            ta = attAttribute.createMappedAttribute(TaskAttribute.ATTACHMENT_CONTENT_TYPE);
            ta.setValue("text/plain");
            ta = attAttribute.createMappedAttribute(TaskAttribute.ATTACHMENT_FILENAME);
            ta.setValue(f.getName());
        
            ah.postContent(taskRepository, new ITaskImpl(taskData), new FileTaskAttachmentSource(f), "adding attachment", attAttribute, nullProgressMonitor);
            taskData = rc.getTaskData(taskRepository, rr.getTaskId(), nullProgressMonitor);
            
            printTaskData(taskData);
        
            List<TaskAttribute> attrs = taskData.getAttributeMapper().getAttributesByType(taskData, TaskAttribute.TYPE_ATTACHMENT);
            InputStream is = ah.getContent(taskRepository, new ITaskImpl(taskData), attrs.get(0), nullProgressMonitor);
            
            byte[] b = new byte[ATTACHMENT_DATA.length()];
            is.read(b);
            is.close();
            
            C2C.LOG.log(Level.FINE, "   attachment data : " + new String(b));
            
            // resolve
            rta = taskData.getRoot();
            ta = rta.getMappedAttribute(TaskAttribute.STATUS);
            ta.setValue("RESOLVED");
            
            rr = C2CUtil.postTaskData(rc, taskRepository, taskData);
            taskData = rc.getTaskData(taskRepository, rr.getTaskId(), nullProgressMonitor);
            
            printTaskData(taskData);

            // subtask 
            TaskData taskData2 = createIssue(taskRepository, "this is a subbug", "a subbug", "subbug");
            
            printTaskData(taskData2); 
            
            rta = taskData.getRoot();
            ta = rta.getMappedAttribute(C2CData.ATTR_SUBTASK);
            ta.setValue(taskData2.getTaskId());
            rr = C2CUtil.postTaskData(rc, taskRepository, taskData);
            
            taskData2 = rc.getTaskData(taskRepository, taskData2.getTaskId(), nullProgressMonitor);
            taskData = rc.getTaskData(taskRepository, taskData.getTaskId(), nullProgressMonitor);
            
            printTaskData(taskData); 
            printTaskData(taskData2); 
            
            // get history
//            TaskHistory history = rc.getTaskHistory(taskRepository, new ITaskImpl(taskData), nullProgressMonitor);
//            List<TaskRevision> revisions = history.getRevisions();
//            C2C.LOG.log(Level.FINE, " ************************************************* ");
//            C2C.LOG.log(Level.FINE, " History: ");
//            for (TaskRevision r : revisions) {
//                C2C.LOG.log(Level.FINE, "   rev : " + r.getId());
//                C2C.LOG.log(Level.FINE, "   author : " + r.getAuthor());
//                C2C.LOG.log(Level.FINE, "   date : " + r.getDate());
//                C2C.LOG.log(Level.FINE, "   changes : ");
//                List<Change> changes = r.getChanges();
//                for (Change c : changes) {
//                    C2C.LOG.log(Level.FINE, "    ----------------------------------------- ");
//                    C2C.LOG.log(Level.FINE, "     attr : " + c.getAttributeId());
//                    C2C.LOG.log(Level.FINE, "     field : " + c.getField());
//                    C2C.LOG.log(Level.FINE, "     removed : " + c.getRemoved());
//                    
//                }
//            }
            
        } catch (CoreException ex) {
            Exceptions.printStackTrace(ex);
        } catch (MalformedURLException ex) {
            Exceptions.printStackTrace(ex);
        } catch (Throwable t) {
            Exceptions.printStackTrace(t);
        }
    }
    
    public TaskData createIssue(TaskRepository repository, String summary, String desc, String typeName) throws CoreException, MalformedURLException {
        AbstractRepositoryConnector cfcrc = C2C.getInstance().getRepositoryConnector();
        TaskData data = C2CUtil.createTaskData(repository);
        
        C2CData clientData = DummyUtils.getClientData(repository);
        
        TaskAttribute rta = data.getRoot();
        TaskAttribute ta = rta.getMappedAttribute(TaskAttribute.SUMMARY);
        ta.setValue(summary);
        ta = rta.getMappedAttribute(TaskAttribute.DESCRIPTION);
        ta.setValue(desc);
        
        ta = rta.getMappedAttribute(C2CData.ATTR_TASK_TYPE);
        ta.setValue(clientData.getTaskTypes().iterator().next());
        
        Product product = clientData.getProducts().get(0);
        ta = rta.getMappedAttribute(TaskAttribute.PRODUCT);
        ta.setValue(product.getName());
        
        ta = rta.getMappedAttribute(TaskAttribute.COMPONENT);
        ta.setValue(product.getComponents().get(0).getName());
        
        ta = rta.getMappedAttribute(C2CData.ATTR_MILESTONE);
        ta.setValue(product.getMilestones().get(0).getValue());
        
        ta = rta.getMappedAttribute(C2CData.ATTR_ITERATION);
        Collection<String> c = clientData.getActiveIterations();
        if(!c.isEmpty()) {
            ta.setValue(c.iterator().next());
        }
        
        ta = rta.getMappedAttribute(TaskAttribute.PRIORITY);
        ta.setValue(clientData.getPriorities().get(0).getValue());
        
        ta = rta.getMappedAttribute(TaskAttribute.SEVERITY);
        ta.setValue(clientData.getSeverities().get(0).getValue());
        
        ta = rta.getMappedAttribute(TaskAttribute.STATUS);
        ta.setValue(clientData.getStatusByValue("UNCONFIRMED").getValue());
        
        RepositoryResponse rr = C2CUtil.postTaskData(cfcrc, repository, data);
        String taskId = rr.getTaskId();
        data = cfcrc.getTaskData(repository, taskId, nullProgressMonitor);
        C2C.LOG.log(Level.FINE, " dataRoot after get {0}", data.getRoot().toString());
        return data;
    }    

    private String getDifferentUser(String user, List<TaskUserProfile> users) {
        
        for (TaskUserProfile tup : users) {
            if(tup.getRealname().toLowerCase().contains("stupka")) {
                return tup.getLoginName();
            }
        }
        return null;
    }

    private void printTaskData(TaskData data) {
        C2C.LOG.log(Level.INFO, " *************************************************** ");
        C2C.LOG.log(Level.INFO, " id : " + data.getTaskId());
        C2C.LOG.log(Level.INFO, "   summary : " + data.getRoot().getAttribute(TaskAttribute.SUMMARY).getValue());
        C2C.LOG.log(Level.INFO, "   owner : " + data.getRoot().getAttribute(C2CData.ATTR_OWNER).getValue());
        C2C.LOG.log(Level.INFO, "   status : " + data.getRoot().getAttribute(TaskAttribute.STATUS).getValue());
        C2C.LOG.log(Level.INFO, "   parent : " + data.getRoot().getAttribute(C2CData.ATTR_PARENT).getValue());
        C2C.LOG.log(Level.INFO, "   subtask : " + data.getRoot().getAttribute(C2CData.ATTR_SUBTASK).getValue());
        
        List<TaskAttribute> attrs = data.getAttributeMapper().getAttributesByType(data, TaskAttribute.TYPE_ATTACHMENT);
        C2C.LOG.log(Level.INFO, "   attachmnets : " + (attrs != null ? attrs.size() : "null"));
    }
    
    private class ITaskImpl implements ITask {

        private final TaskData data;

        public ITaskImpl(TaskData data) {
            this.data = data;
        }

        @Override
        public String getAttribute(String id) {
            TaskAttribute rta = data.getRoot();
            return rta.getMappedAttribute(id).getValue();
        }

        @Override
        public void setAttribute(String id, String value) {
            TaskAttribute rta = data.getRoot();
            rta.getMappedAttribute(id).setValue(value);
        }
        
        @Override
        public String getTaskId() {
            return data.getTaskId();
        }        
        
        @Override
        public Date getCompletionDate() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String getConnectorKind() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Date getCreationDate() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Date getDueDate() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String getHandleIdentifier() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Date getModificationDate() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String getOwner() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String getPriority() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String getRepositoryUrl() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String getSummary() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public SynchronizationState getSynchronizationState() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String getTaskKey() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String getTaskKind() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean isActive() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean isCompleted() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setCompletionDate(Date date) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setCreationDate(Date date) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setDueDate(Date date) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setModificationDate(Date date) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setOwner(String string) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setPriority(String string) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setSummary(String string) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setTaskKind(String string) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setUrl(String string) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setTaskKey(String string) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String getUrl() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public int compareTo(IRepositoryElement o) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Object getAdapter(Class type) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Map<String, String> getAttributes() {
            throw new UnsupportedOperationException("Not supported yet.");
        }
        
    }
}
