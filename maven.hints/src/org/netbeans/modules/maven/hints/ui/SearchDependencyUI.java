/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2010 Oracle and/or its affiliates. All rights reserved.
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
 * Contributor(s): theanuradha@netbeans.org
 *
 * Portions Copyrighted 2008 Sun Microsystems, Inc.
 */
package org.netbeans.modules.maven.hints.ui;

import java.awt.Image;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import javax.swing.JButton;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.apache.lucene.search.BooleanQuery;
import org.apache.maven.model.Dependency;
import org.netbeans.modules.maven.hints.ui.nodes.ArtifactNode;
import org.netbeans.modules.maven.hints.ui.nodes.VersionNode;
import org.netbeans.modules.maven.indexer.api.NBVersionInfo;
import org.netbeans.modules.maven.indexer.api.RepositoryQueries;
import org.netbeans.modules.maven.api.NbMavenProject;
import org.netbeans.api.project.Project;
import org.netbeans.modules.maven.indexer.api.QueryRequest;
import org.netbeans.modules.maven.indexer.api.RepositoryPreferences;
import org.openide.explorer.ExplorerManager;
import org.openide.explorer.view.BeanTreeView;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.FilterNode;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;

/**
 *
 * @author  Anuradha G
 */
public class SearchDependencyUI extends javax.swing.JPanel implements ExplorerManager.Provider, Observer {

    private ExplorerManager explorerManager = new ExplorerManager();
    private JButton addButton = new JButton(NbBundle.getMessage(SearchDependencyUI.class, "BTN_Add"));
    private BeanTreeView beanTreeView;
    private NBVersionInfo nbvi;
    private static RequestProcessor.Task task = null;
    private static final RequestProcessor RP = new RequestProcessor(SearchDependencyUI.class.getName(),10);
    private boolean retrigger = false;
    private Project project;
    
    private QueryRequest queryRequest;
    private ResultsRootNode resultsRootNode;
    
    /** Creates new form SearchDependencyUI */
    public SearchDependencyUI(String clazz, Project mavProj) {
        initComponents();
        project = mavProj;
        beanTreeView = (BeanTreeView) treeView;
        beanTreeView.setPopupAllowed(false);
        beanTreeView.setRootVisible(false);
        beanTreeView.setDefaultActionAllowed(true);
        beanTreeView.setUseSubstringInQuickSearch(true);
        addButton.setEnabled(false);

        txtClassName.setText(clazz);
        txtClassName.selectAll();
        explorerManager.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent arg0) {
                if (arg0.getPropertyName().equals(ExplorerManager.PROP_SELECTED_NODES)) {//NOI18N

                    Node[] selectedNodes = explorerManager.getSelectedNodes();
                   
                    for (Node node : selectedNodes) {
                        if (node instanceof VersionNode) {

                            nbvi=((VersionNode) node).getNBVersionInfo();
        
                            
                            break;

                        }else if(node instanceof ArtifactNode){
                            ArtifactNode an=(ArtifactNode) node;
                            List<NBVersionInfo> infos = an.getVersionInfos();
                            nbvi = infos.isEmpty() ? null : infos.get(0);
                        }
                    }
                    if(nbvi!=null){
                     lblSelected.setText(nbvi.getGroupId()+" : "+nbvi.getArtifactId()
                             +" : "+nbvi.getVersion()+ " [ " + nbvi.getType() 
                             + (nbvi.getClassifier() != null ? ("," + nbvi.getClassifier()) : "")+" ]");
                    }else{
                     lblSelected.setText(null);
                    }
                    addButton.setEnabled(nbvi!=null);

                }
            }
        });
        queryRequest = null;
        resultsRootNode = new ResultsRootNode();
        explorerManager.setRootContext(resultsRootNode);
        createSearchTask();
        load();
        txtClassName.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                load();
            }

            public void removeUpdate(DocumentEvent e) {
                load();
            }

            public void changedUpdate(DocumentEvent e) {
                load();
            }
            
        });
    }

    public NBVersionInfo getSelectedVersion() {
        
        return nbvi;
    }

    public JButton getAddButton() {
        return addButton;
    }

    private void createSearchTask() {
        if (task != null) {
            task.cancel();
        }
        final Observer observer = this;
        task = RP.create(new Runnable() {
            public void run() {
                final String[] search = new String[1];
                try {
                    SwingUtilities.invokeAndWait(new Runnable() {
                        public void run() {
                            lblSelected.setText(null);
                            search[0] = getClassSearchName();
//for debugging purposes only lblMatchingArtifacts.setText(search[0]);
                        }
                    });
                } catch (Exception ex) {
                    Exceptions.printStackTrace(ex);
                }

                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        resultsRootNode.setOneChild(getSearchingNode());
                    }
                });

                if (search[0].length() > 0) {
                    
                    queryRequest = new QueryRequest(search[0], RepositoryPreferences.getInstance().getRepositoryInfos(), observer);
                
                    try {
                        RepositoryQueries.findVersionsByClass(queryRequest);
                    } catch (BooleanQuery.TooManyClauses exc) {
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                resultsRootNode.setOneChild(getTooGeneralNode());
                            }
                        });
                    } catch (OutOfMemoryError oome) {
                        // running into OOME may still happen in Lucene despite the fact that
                        // we are trying hard to prevent it in NexusRepositoryIndexerImpl
                        // (see #190265)
                        // in the bad circumstances theoretically any thread may encounter OOME
                        // but most probably this thread will be it
                        // trying to indicate the condition to the user here
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                resultsRootNode.setOneChild(getTooGeneralNode());
                            }
                        });
                    }
                    //the actual lucene query takes much longer than our queue
                    // timeout, we should not start new tasks until this one is
                    //finished.. and this one should either finish with correct data
                    // or immediately retrigger a new search.
                    synchronized (SearchDependencyUI.this) {
                        if (retrigger) {
                            retrigger = false;
                            task.schedule(20);
                            return;
                        }
                    }
                } else {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            resultsRootNode.setOneChild(getNoResultsNode());
                        }
                    });
                }
            }
        }, true);
    }
    
    public synchronized void load() {
        if (!task.isFinished() && task.getDelay() == 0) {
            //if running, just flag the 'retrigger' variable,
            //chances are the task is currently doing a lucene search..
            retrigger = true;
            // stop waiting for results of the previous search
            if (null != queryRequest) {
                queryRequest.deleteObserver(this);
            }                
        } else {
            task.schedule(500);
        }
    }

    public String getClassSearchName() {
        return txtClassName.getText().trim();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        lblClassName = new javax.swing.JLabel();
        txtClassName = new javax.swing.JTextField();
        treeView = new BeanTreeView();
        lblMatchingArtifacts = new javax.swing.JLabel();
        lblSelected = new javax.swing.JLabel();

        lblClassName.setText(org.openide.util.NbBundle.getMessage(SearchDependencyUI.class, "LBL_Class_Name")); // NOI18N

        treeView.setBorder(javax.swing.BorderFactory.createEtchedBorder(null, javax.swing.UIManager.getDefaults().getColor("ComboBox.selectionBackground")));

        lblMatchingArtifacts.setText(org.openide.util.NbBundle.getMessage(SearchDependencyUI.class, "LBL_Matching_artifacts")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(lblSelected, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 430, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(treeView, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 430, Short.MAX_VALUE)
                    .addComponent(txtClassName, javax.swing.GroupLayout.DEFAULT_SIZE, 430, Short.MAX_VALUE)
                    .addComponent(lblMatchingArtifacts, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 430, Short.MAX_VALUE)
                    .addComponent(lblClassName, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 430, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lblClassName)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txtClassName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblMatchingArtifacts)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(treeView, javax.swing.GroupLayout.DEFAULT_SIZE, 214, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblSelected, javax.swing.GroupLayout.PREFERRED_SIZE, 14, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel lblClassName;
    private javax.swing.JLabel lblMatchingArtifacts;
    private javax.swing.JLabel lblSelected;
    private javax.swing.JScrollPane treeView;
    private javax.swing.JTextField txtClassName;
    // End of variables declaration//GEN-END:variables

    public ExplorerManager getExplorerManager() {
        return explorerManager;
    }

    private class ResultsRootNode extends AbstractNode {

        private ResultsRootChildren resultsChildren;

        public ResultsRootNode() {
            this(new InstanceContent());
        }

        private ResultsRootNode(InstanceContent content) {
            super (new ResultsRootChildren(), new AbstractLookup(content));
            content.add(this);
            this.resultsChildren = (ResultsRootChildren) getChildren();
        }

        public void setOneChild(Node n) {
            List<Node> ch = new ArrayList<Node>(1);
            ch.add(n);
            setNewChildren(ch);
        }
        
        public void setNewChildren(List<Node> ch) {
            resultsChildren.setNewChildren (ch);
        }
    }
    
    private class ResultsRootChildren extends Children.Keys<Node> {
        
        List<Node> myNodes;

        public ResultsRootChildren() {
            myNodes = Collections.EMPTY_LIST;
        }

        private void setNewChildren(List<Node> ch) {
            myNodes = ch;
            refreshList();
        }

        @Override
        protected void addNotify() {
            refreshList();
        }

        private void refreshList() {
            List<Node> keys = new ArrayList();
            for (Node node : myNodes) {
                keys.add(node);
            }
            setKeys(keys);
        }

        @Override
        protected Node[] createNodes(Node key) {
            return new Node[] { key };
        }

    }

    private static Node noResultsNode, searchingNode, tooGeneralNode;
    
    private static Node getSearchingNode() {
        if (searchingNode == null) {
            AbstractNode nd = new AbstractNode(Children.LEAF) {

                @Override
                public Image getIcon(int arg0) {
                    return ImageUtilities.loadImage("org/netbeans/modules/maven/hints/wait.gif"); //NOI18N
                }

                @Override
                public Image getOpenedIcon(int arg0) {
                    return getIcon(arg0);
                }
            };
            nd.setName("Searching"); //NOI18N

            nd.setDisplayName(NbBundle.getMessage(SearchDependencyUI.class, "Node_Loading")); //NOI18N
            
            searchingNode = nd;
        }
        return new FilterNode (searchingNode, Children.LEAF);
    }

    public static Node getNoResultsNode() {
        if (noResultsNode == null) {
            AbstractNode nd = new AbstractNode(Children.LEAF) {

                @Override
                public Image getIcon(int arg0) {
                    return ImageUtilities.loadImage("org/netbeans/modules/maven/hints/empty.png"); //NOI18N
                }

                @Override
                public Image getOpenedIcon(int arg0) {
                    return getIcon(arg0);
                }
            };
            nd.setName("Empty"); //NOI18N

            nd.setDisplayName(NbBundle.getMessage(SearchDependencyUI.class, "Node_Empty"));
            
            noResultsNode = nd;
        }
        return new FilterNode (noResultsNode, Children.LEAF);
    }

    private static Node getTooGeneralNode() {
        if (tooGeneralNode == null) {
            AbstractNode nd = new AbstractNode(Children.LEAF) {

                @Override
                public Image getIcon(int arg0) {
                    return ImageUtilities.loadImage("org/netbeans/modules/maven/hints/empty.png"); //NOI18N
                    }

                @Override
                public Image getOpenedIcon(int arg0) {
                    return getIcon(arg0);
                }
            };
            nd.setName("Too General"); //NOI18N

            nd.setDisplayName(NbBundle.getMessage(SearchDependencyUI.class, "Node_TooGeneral")); //NOI18N

            tooGeneralNode = nd;
        }

        return new FilterNode (tooGeneralNode, Children.LEAF);
    }
    
    @Override
    public void update(Observable o, Object arg) {

        if (null == o || !(o instanceof QueryRequest)) {
            return;
        }

        List<NBVersionInfo> infos = ((QueryRequest) o).getResults();

        final Map<String, List<NBVersionInfo>> map = new HashMap<String, List<NBVersionInfo>>();

        for (NBVersionInfo nbvi : infos) {
            String key = nbvi.getGroupId() + " : " + nbvi.getArtifactId();
            List<NBVersionInfo> get = map.get(key);
            if (get == null) {
                get = new ArrayList<NBVersionInfo>();
                map.put(key, get);
            }
            get.add(nbvi);
        }
        final List<String> keyList = new ArrayList<String>(map.keySet());
        // sort specially using our comparator, see compare method
        Collections.sort(keyList, new HeuristicsComparator());

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                updateResultNodes(keyList, map);
            }
        });
    }
    
    private void updateResultNodes(List<String> keyList, Map<String, List<NBVersionInfo>> map) {

        if (keyList.size() > 0) { // some results available

            Map<String, Node> currentNodes = new HashMap<String, Node>();
            for (Node nd : resultsRootNode.getChildren().getNodes()) {
                currentNodes.put(nd.getName(), nd);
            }
            List<Node> newNodes = new ArrayList<Node>(keyList.size());

            // still searching?
            if (null != queryRequest && !queryRequest.isFinished()) {
                newNodes.add(getSearchingNode());
            }

            for (String key : keyList) {
                Node nd;
                nd = currentNodes.get(key);
                if (null != nd) {
                    ((ArtifactNode)nd).setVersionInfos(map.get(key));
                } else {
                    nd = new ArtifactNode(key, map.get(key));
                }
                newNodes.add(nd);
            }

            resultsRootNode.setNewChildren(newNodes);
        } else if (null != queryRequest && !queryRequest.isFinished()) { // still searching, no results yet
            resultsRootNode.setOneChild(getSearchingNode());
        } else { // finished searching with no results
            resultsRootNode.setOneChild(getNoResultsNode());
        }
    }
    
    //TODO
    // for netbeans projects, org.netbeans.api is the prefered item in the list
    // for web/ejb/ear projects, javax.* are probably preferred.
    // possibly items from groupids that are already present in the pom should also be
    // put up front.
    private class HeuristicsComparator implements Comparator<String> {
        private Set<String> privilegedGroupIds = new HashSet<String>();
        
        private HeuristicsComparator() {
            String packaging = project.getLookup().lookup(NbMavenProject.class).getPackagingType();
            if (NbMavenProject.TYPE_NBM.equalsIgnoreCase(packaging)) {
                privilegedGroupIds.add("org.netbeans.api"); //NOI18N
            }
            if (NbMavenProject.TYPE_WAR.equalsIgnoreCase(packaging) || 
                NbMavenProject.TYPE_EAR.equalsIgnoreCase(packaging) || 
                NbMavenProject.TYPE_EJB.equalsIgnoreCase(packaging)) {
                privilegedGroupIds.add("javax.activation");//NOI18N
                privilegedGroupIds.add("javax.ejb");//NOI18N
                privilegedGroupIds.add("javax.faces");//NOI18N
                privilegedGroupIds.add("javax.j2ee");//NOI18N
                privilegedGroupIds.add("javax.jdo");//NOI18N
                privilegedGroupIds.add("javax.jms");//NOI18N
                privilegedGroupIds.add("javax.mail");//NOI18N
                privilegedGroupIds.add("javax.management");//NOI18N
                privilegedGroupIds.add("javax.naming");//NOI18N
                privilegedGroupIds.add("javax.persistence");//NOI18N
                privilegedGroupIds.add("javax.portlet");//NOI18N
                privilegedGroupIds.add("javax.resource");//NOI18N
                privilegedGroupIds.add("javax.security");//NOI18N
                privilegedGroupIds.add("javax.servlet");//NOI18N
                privilegedGroupIds.add("javax.sql");//NOI18N
                privilegedGroupIds.add("javax.transaction");//NOI18N
                privilegedGroupIds.add("javax.xml");//NOI18N
            }
            //TODO add some more heuristics
            NbMavenProject mavenproject = project.getLookup().lookup(NbMavenProject.class);
            List<Dependency> deps = mavenproject.getMavenProject().getDependencies();
            for (Dependency d : deps) {
                privilegedGroupIds.add(d.getGroupId());
            }
        }

        public int compare(String s1, String s2) {
            String[] split1 = s1.split(":");
            String[] split2 = s2.split(":");
            boolean b1 = privilegedGroupIds.contains(split1[0].trim());
            boolean b2 = privilegedGroupIds.contains(split2[0].trim());
            if (b1 && !b2) {
                return -1;
            }
            if (!b1 && b2) {
                return 1;
            }
            return s1.compareTo(s2);
        }
        
    }
            
}
