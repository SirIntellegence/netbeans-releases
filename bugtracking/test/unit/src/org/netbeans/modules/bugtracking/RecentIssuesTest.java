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
 * Contributor(s):
 * 
 * Portions Copyrighted 2008 Sun Microsystems, Inc.
 */

package org.netbeans.modules.bugtracking;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.util.Map;
import java.util.List;
import java.util.logging.Level;
import org.netbeans.junit.NbTestCase;
import org.netbeans.modules.bugtracking.api.Repository;
import org.netbeans.modules.bugtracking.spi.*;

/**
 *
 * @author tomas
 */
public class RecentIssuesTest extends NbTestCase {

    public RecentIssuesTest(String arg0) {
        super(arg0);
    }

    @Override
    protected Level logLevel() {
        return Level.ALL;
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();        
        System.setProperty("netbeans.user", getWorkDir().getAbsolutePath());
        Field f = BugtrackingManager.class.getDeclaredField("recentIssues");
        f.setAccessible(true);
        List<IssueImpl> ri = (List<IssueImpl>) f.get(BugtrackingManager.getInstance());
        if(ri != null) ri.clear();
    }

    public void testGetRecentIssuesEmptyReturn() throws MalformedURLException, IOException {
        List<IssueImpl> ri = BugtrackingManager.getInstance().getAllRecentIssues();
        assertNotNull(ri);
        assertEquals(0, ri.size());
        
        List<IssueImpl> ri2 = BugtrackingManager.getInstance().getRecentIssues(getRepository(new RITestRepository("test repo")));
        assertNotNull(ri2);
        assertEquals(0, ri.size());        
    }

    public void testAddRecentIssues() throws MalformedURLException, IOException {
        final RITestRepository riTestRepo = new RITestRepository("test repo");
        RepositoryImpl repo = getRepository(riTestRepo);
        IssueImpl issue1 = getIssue(repo, new RITestIssue(riTestRepo, "1"));
        IssueImpl issue2 = getIssue(repo, new RITestIssue(riTestRepo, "2"));

        // add issue1
        BugtrackingManager.getInstance().addRecentIssue(repo, issue1);

        // test for another repo -> nothing is returned
        RepositoryImpl repo2 = getRepository(new RITestRepository("test repo 2"));
        List<IssueImpl>  issues = (List<IssueImpl>) BugtrackingManager.getInstance().getRecentIssues(repo2);
        assertNotNull(issues);
        assertEquals(0, issues.size());

        // getIssues for repo -> issue1 is returned
        issues = (List<IssueImpl>) BugtrackingManager.getInstance().getRecentIssues(repo);
        assertNotNull(issues);
        assertEquals(1, issues.size());
        assertEquals(issue1.getID(), issues.iterator().next().getID());
        
        // getAll -> issue1 is returned
        List<IssueImpl> allIssues = BugtrackingManager.getInstance().getAllRecentIssues();
        assertNotNull(allIssues);
        assertEquals(1, allIssues.size());
        assertTrue(allIssues.get(0).getRepositoryImpl().getId().equals(repo.getId()));
        assertEquals(issue1.getID(), allIssues.get(0).getID());

        // add issue2
        BugtrackingManager.getInstance().addRecentIssue(repo, issue2);

        // getIssues -> issue1 & issue2 are returned
        issues = (List<IssueImpl>) BugtrackingManager.getInstance().getRecentIssues(repo);
        assertNotNull(issues);
        assertEquals(2, issues.size());
        assertEquals(issue2.getID(), issues.get(0).getID());
        assertEquals(issue1.getID(), issues.get(1).getID());

        // getAll -> issue1 & issue2 are returned
        allIssues = BugtrackingManager.getInstance().getAllRecentIssues();
        assertNotNull(allIssues);
        assertEquals(2, allIssues.size());
        assertTrue(allIssues.get(0).getRepositoryImpl().getId().equals(repo.getId()));
        assertTrue(allIssues.get(1).getRepositoryImpl().getId().equals(repo.getId()));
        assertRecentIssues(allIssues, new IssueImpl[] {issue2, issue1});
    }

    public void testAddRecentIssuesMoreThan5() throws MalformedURLException, IOException {
        RITestRepository riTestRepo1 = new RITestRepository("test repo");
        RepositoryImpl repo1 = getRepository(new RITestRepository("test repo"));
        IssueImpl repo1issue1 = getIssue(repo1, new RITestIssue(riTestRepo1, "r1i1"));
        IssueImpl repo1issue2 = getIssue(repo1, new RITestIssue(riTestRepo1, "r1i2"));
        IssueImpl repo1issue3 = getIssue(repo1, new RITestIssue(riTestRepo1, "r1i3"));
        IssueImpl repo1issue4 = getIssue(repo1, new RITestIssue(riTestRepo1, "r1i4"));
        IssueImpl repo1issue5 = getIssue(repo1, new RITestIssue(riTestRepo1, "r1i5"));
        IssueImpl repo1issue6 = getIssue(repo1, new RITestIssue(riTestRepo1, "r1i6"));
        IssueImpl repo1issue7 = getIssue(repo1, new RITestIssue(riTestRepo1, "r1i7"));

        // add repo1 issues 1, 2, 3, 4, 5, 6, 7,
        BugtrackingManager.getInstance().addRecentIssue(repo1, repo1issue1);
        BugtrackingManager.getInstance().addRecentIssue(repo1, repo1issue2);
        BugtrackingManager.getInstance().addRecentIssue(repo1, repo1issue3);
        BugtrackingManager.getInstance().addRecentIssue(repo1, repo1issue4);
        BugtrackingManager.getInstance().addRecentIssue(repo1, repo1issue5);
        BugtrackingManager.getInstance().addRecentIssue(repo1, repo1issue6);
        BugtrackingManager.getInstance().addRecentIssue(repo1, repo1issue7);

        // getIssues for repo1 -> repo1 issues 1..7 are returned
        List<IssueImpl> issues = (List<IssueImpl>) BugtrackingManager.getInstance().getRecentIssues(repo1);
        assertNotNull(issues);
        assertEquals(7, issues.size());
        assertEquals(repo1issue7.getID(), issues.get(0).getID());
        assertEquals(repo1issue6.getID(), issues.get(1).getID());
        assertEquals(repo1issue5.getID(), issues.get(2).getID());
        assertEquals(repo1issue4.getID(), issues.get(3).getID());
        assertEquals(repo1issue3.getID(), issues.get(4).getID());
        assertEquals(repo1issue2.getID(), issues.get(5).getID());
        assertEquals(repo1issue1.getID(), issues.get(6).getID());

        RITestRepository riTestrepo2 = new RITestRepository("test repo2");
        RepositoryImpl repo2 = getRepository(riTestrepo2);
        IssueImpl repo2issue1 = getIssue(repo2, new RITestIssue(riTestrepo2, "r2i1"));
        IssueImpl repo2issue2 = getIssue(repo2, new RITestIssue(riTestrepo2, "r2i2"));
        IssueImpl repo2issue3 = getIssue(repo2, new RITestIssue(riTestrepo2, "r2i3"));
        IssueImpl repo2issue4 = getIssue(repo2, new RITestIssue(riTestrepo2, "r2i4"));
        IssueImpl repo2issue5 = getIssue(repo2, new RITestIssue(riTestrepo2, "r2i5"));
        IssueImpl repo2issue6 = getIssue(repo2, new RITestIssue(riTestrepo2, "r2i6"));
        IssueImpl repo2issue7 = getIssue(repo2, new RITestIssue(riTestrepo2, "r2i7"));

        // add repo2 issues 1, 2, 3, 4, 5, 6, 7,
        BugtrackingManager.getInstance().addRecentIssue(repo2, repo2issue1);
        BugtrackingManager.getInstance().addRecentIssue(repo2, repo2issue2);
        BugtrackingManager.getInstance().addRecentIssue(repo2, repo2issue3);
        BugtrackingManager.getInstance().addRecentIssue(repo2, repo2issue4);
        BugtrackingManager.getInstance().addRecentIssue(repo2, repo2issue5);
        BugtrackingManager.getInstance().addRecentIssue(repo2, repo2issue6);
        BugtrackingManager.getInstance().addRecentIssue(repo2, repo2issue7);

        // getIssues for repo2 -> repo2 issues 1..7 are returned
        issues = BugtrackingManager.getInstance().getRecentIssues(repo2);
        assertNotNull(issues);
        assertEquals(7, issues.size());
        assertEquals(repo2issue7.getID(), issues.get(0).getID());
        assertEquals(repo2issue6.getID(), issues.get(1).getID());
        assertEquals(repo2issue5.getID(), issues.get(2).getID());
        assertEquals(repo2issue4.getID(), issues.get(3).getID());
        assertEquals(repo2issue3.getID(), issues.get(4).getID());
        assertEquals(repo2issue2.getID(), issues.get(5).getID());
        assertEquals(repo2issue1.getID(), issues.get(6).getID());
    }

    private void assertRecentIssues(List<IssueImpl> recent, IssueImpl[] issues) {
        assertEquals(recent.size(), issues.length);
        for (int i = 0; i < issues.length; i++) {
            assertEquals(issues[i].getID(), recent.get(i).getID());
        }
    }

    private class RITestRepository extends TestRepository {
        private final String name;
        private RepositoryInfo info;

        public RITestRepository(String name) {
            this.name = name;
            info = new RepositoryInfo(name, name, null, name, name, null, null, null, null);
        }

        @Override
        public RepositoryInfo getInfo() {
            return info;
        }
    }
        
    private class RITestIssue extends TestIssue {
        private final String name;
        private final RITestRepository repository;
        public RITestIssue(RITestRepository repository, String name) {
            this.repository = repository;
            this.name = name;
        }
        public String getDisplayName() {
            return name;
        }
        public String getTooltip() {
            return name;
        }
        public boolean isNew() {
            return false;
        }
        public boolean isFinished() {
            return false;
        }
        public String getSummary() {
            return "This is" + name;
        }
        public String getID() {
            return name;
        }
    }

    private class RITestConector implements BugtrackingConnector {
        @Override
        public Repository createRepository(RepositoryInfo info) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
        @Override
        public Repository createRepository() {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }
    
    private RepositoryImpl getRepository(RITestRepository repo) {
        return TestKit.getRepository(repo);
    }
    
    private IssueImpl getIssue(RepositoryImpl repo2, RITestIssue riTestIssue) {
        return TestKit.getIssue(repo2, riTestIssue);
    }
    
}
