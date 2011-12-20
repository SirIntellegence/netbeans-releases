/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2011 Sun Microsystems, Inc.
 */
package org.netbeans.modules.debugger.jpda.ui.breakpoints;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;
import java.util.TreeSet;
import org.netbeans.api.debugger.Breakpoint;
import org.netbeans.api.debugger.Breakpoint.GroupProperties;
import org.netbeans.api.debugger.DebuggerManager;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.modules.debugger.jpda.ui.breakpoints.OutlineComboBox.PopupMenuItem;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileStateInvalidException;
import org.openide.filesystems.FileUtil;
import org.openide.filesystems.URLMapper;
import org.openide.util.NbBundle;

/**
 * Representation of an expandable group category in ActionsPanel
 * 
 * @author Martin
 */
abstract class BreakpointsExpandableGroup<T> implements OutlineComboBox.Expandable {
    
    private boolean expanded = false;
    private T[] items;

    @Override
    public boolean isExpanded() {
        return expanded;
    }

    @Override
    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }
    
    @Override
    public T[] getItems() {
        if (items == null) {
            items = createItems();
        }
        return items;
    }
    
    protected abstract T[] createItems();
    
    static class FilesGroup extends BreakpointsExpandableGroup<FileItem> {
        
        @Override
        public FileItem[] createItems() {
            Set<FileItem> items = new TreeSet<FileItem>();
            Breakpoint[] bs = DebuggerManager.getDebuggerManager ().getBreakpoints ();
            for (int i = 0; i < bs.length; i++) {
                GroupProperties groupProperties = bs[i].getGroupProperties();
                if (groupProperties != null) {
                    FileObject[] files = groupProperties.getFiles();
                    if (files != null) {
                        for (FileObject fo : files) {
                            items.add(new FileItem(fo));
                        }
                    }
                }
            }
            return items.toArray(new FileItem[]{});
        }

        @Override
        public String toString() {
            return NbBundle.getMessage(ActionsPanel.class, "LBL_FilesGroup");
        }
        
    }
    
    static class ProjectsGroup extends BreakpointsExpandableGroup<ProjectItem> {

        @Override
        public ProjectItem[] createItems() {
            Set<ProjectItem> items = new TreeSet<ProjectItem>();
            Breakpoint[] bs = DebuggerManager.getDebuggerManager ().getBreakpoints ();
            for (int i = 0; i < bs.length; i++) {
                GroupProperties groupProperties = bs[i].getGroupProperties();
                if (groupProperties != null) {
                    Project[] projects = groupProperties.getProjects();
                    if (projects != null) {
                        for (Project p : projects) {
                            items.add(new ProjectItem(p));
                        }
                    }
                }
            }
            return items.toArray(new ProjectItem[]{});
        }

        @Override
        public String toString() {
            return NbBundle.getMessage(ActionsPanel.class, "LBL_ProjectsGroup");
        }
        
    }
    
    static class TypesGroup extends BreakpointsExpandableGroup<TypeItem> {

        @Override
        public TypeItem[] createItems() {
            Set<TypeItem> items = new TreeSet<TypeItem>();
            Breakpoint[] bs = DebuggerManager.getDebuggerManager ().getBreakpoints ();
            for (int i = 0; i < bs.length; i++) {
                GroupProperties groupProperties = bs[i].getGroupProperties();
                if (groupProperties != null) {
                    String type = groupProperties.getType();
                    if (type != null) {
                        items.add(new TypeItem(type));
                    }
                }
            }
            return items.toArray(new TypeItem[]{});
        }

        @Override
        public String toString() {
            return NbBundle.getMessage(ActionsPanel.class, "LBL_TypesGroup");
        }
        
    }
    
    static class FileItem implements Comparable<FileItem>, PopupMenuItem {
        
        private FileObject fo;
        
        public FileItem(FileObject fo) {
            this.fo = fo;
        }
        
        public FileObject getFileObject() {
            return fo;
        }

        @Override
        public int hashCode() {
            return fo.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return (obj instanceof FileItem) && ((FileItem) obj).fo.equals(fo);
        }
        
        @Override
        public int compareTo(FileItem o) {
            return o.toString().compareTo(toString());
        }
        
        @Override
        public String toString() {
            try {
                return fo.getURL().toExternalForm();
            } catch (FileStateInvalidException ex) {
                return "file://"+fo.getPath();
            }
        }

        @Override
        public String toPopupMenuString() {
            //return FileUtil.getFileDisplayName(fo);
            return fo.getNameExt();
        }

        public static Object valueOf(String newString) {
            URL url;
            try {
                url = new URL(newString);
            } catch (MalformedURLException ex) {
                return newString;
            }
            FileObject fo = URLMapper.findFileObject(url);
            if (fo != null) {
                return new FileItem(fo);
            } else {
                return newString;
            }
        }

    }
    
    static class ProjectItem implements Comparable<ProjectItem>, PopupMenuItem {
        
        private Project p;
        
        public ProjectItem(Project p) {
            this.p = p;
        }
        
        public Project getProject() {
            return p;
        }

        @Override
        public int hashCode() {
            return p.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return (obj instanceof ProjectItem) && ((ProjectItem) obj).p.equals(p);
        }
        
        @Override
        public int compareTo(ProjectItem o) {
            return o.toString().compareTo(toString());
        }

        @Override
        public String toString() {
            return NbBundle.getMessage(ActionsPanel.class, "LBL_ProjectGroupItem", p.getProjectDirectory().getPath());
        }

        @Override
        public String toPopupMenuString() {
            return ProjectUtils.getInformation(p).getDisplayName();
        }
        
        public static Object valueOf(String newString) {
            String format = NbBundle.getMessage(ActionsPanel.class, "LBL_ProjectGroupItem", "");
            String dir = getFormattedValue(format, newString);
            if (dir != null) {
                FileObject fo = FileUtil.toFileObject(new java.io.File(dir));
                if (fo != null) {
                    try {
                        Project project = ProjectManager.getDefault().findProject(fo);
                        if (project != null) {
                            return new ProjectItem(project);
                        }
                    } catch (IOException ex) {
                    } catch (IllegalArgumentException ex) {
                    }
                }
            }
            return newString;
        }
        
    }
    
    static class TypeItem implements Comparable<TypeItem>, PopupMenuItem {
        
        private String type;
        
        public TypeItem(String type) {
            this.type = type;
        }
        
        public String getType() {
            return type;
        }

        @Override
        public int hashCode() {
            return type.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return (obj instanceof TypeItem) && ((TypeItem) obj).type.equals(type);
        }
        
        @Override
        public int compareTo(TypeItem o) {
            return o.toString().compareTo(toString());
        }

        @Override
        public String toString() {
            return NbBundle.getMessage(ActionsPanel.class, "LBL_TypeGroupItem", type);
        }

        @Override
        public String toPopupMenuString() {
            return type;
        }
        
        public static Object valueOf(String newString) {
            String format = NbBundle.getMessage(ActionsPanel.class, "LBL_ProjectGroupItem", "");
            String type = getFormattedValue(format, newString);
            if (type != null) {
                return new TypeItem(type);
            }
            return newString;
        }
    }
    
    private static String getFormattedValue(String format, String newString) {
        int i;
        for (i = 0; i < newString.length() && i < format.length(); i++) {
            if (Character.toUpperCase(newString.charAt(i)) !=
                Character.toUpperCase(format.charAt(i))) {

                break;
            }
        }
        int i2 = format.length() - 1;
        int i2ns = newString.length() - 1;
        for (; i2 > i && i2ns > i; i2--, i2ns--) {
            if (Character.toUpperCase(newString.charAt(i2ns)) !=
                Character.toUpperCase(format.charAt(i2))) {

                break;
            }
        }
        if (i >= i2 && i <= i2ns) {
            return newString.substring(i, i2ns + 1);
        } else {
            return null;
        }
    }
    
}
