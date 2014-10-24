/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2014 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2014 Sun Microsystems, Inc.
 */
package org.netbeans.modules.javascript2.nodejs;

import java.awt.Component;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JEditorPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.text.BadLocationException;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import org.netbeans.jellytools.EditorOperator;
import org.netbeans.jellytools.JellyTestCase;
import org.netbeans.jellytools.OptionsOperator;
import org.netbeans.jellytools.ProjectsTabOperator;
import org.netbeans.jellytools.modules.editor.CompletionJListOperator;
import org.netbeans.jellytools.nodes.Node;
import org.netbeans.jemmy.EventTool;
import org.netbeans.jemmy.JemmyException;
import org.netbeans.jemmy.JemmyProperties;
import org.netbeans.jemmy.Waitable;
import org.netbeans.jemmy.Waiter;
import org.netbeans.jemmy.operators.JButtonOperator;
import org.netbeans.jemmy.operators.JEditorPaneOperator;
import org.netbeans.jemmy.operators.JLabelOperator;
import org.netbeans.jemmy.operators.JListOperator;
import org.netbeans.jemmy.operators.JPopupMenuOperator;
import org.netbeans.jemmy.operators.JRadioButtonOperator;
import org.netbeans.jemmy.operators.JTabbedPaneOperator;
import org.netbeans.jemmy.operators.JTextFieldOperator;
import org.netbeans.jemmy.operators.Operator;
import org.netbeans.modules.html.editor.api.completion.HtmlCompletionItem;
import org.openide.util.Exceptions;

/**
 *
 * @author vriha
 */
public class GeneralNodeJs extends JellyTestCase {

    protected EventTool evt;
    public static String currentFile = "";
    public static String originalContent = "";
    public final String TEST_BASE_NAME = "nodejs_";
    public static int NAME_ITERATOR = 0;
    public static boolean sourcesDownloaded = false;
    public static int currentLine = -1;

    public GeneralNodeJs(String args) {
        super(args);
        this.evt = new EventTool();
    }

    protected class CompletionInfo {

        public CompletionJListOperator listItself;
        public List listItems;

        public int size() {
            return listItems.size();
        }

        public void hideAll() {
            CompletionJListOperator.hideAll();
        }
    }

    protected String GetWorkDir() {
        return getDataDir().getPath() + File.separator;
    }

    protected void waitCompletionScanning() {

        CompletionJListOperator comp;
        while (true) {
            try {
                comp = new CompletionJListOperator();
                if (null == comp) {
                    return;
                }
                try {
                    Object o = comp.getCompletionItems().get(0);
                    if (!o.toString().contains("No suggestions")
                            && !o.toString().contains("Scanning in progress...")) {
                        return;
                    }
                    evt.waitNoEvent(100);
                } catch (java.lang.Exception ex) {
                    return;
                }
            } catch (JemmyException ex) {
                return;
            }
        }
    }

    protected void type(EditorOperator edit, String code) {
        int iLimit = code.length();
        for (int i = 0; i < iLimit; i++) {
            edit.typeKey(code.charAt(i));
        }
        evt.waitNoEvent(100);
    }

    private class DummyClick implements Runnable {

        private JListOperator list;
        private int index, count;

        public DummyClick(JListOperator l, int i, int j) {
            list = l;
            index = i;
            count = j;
        }

        public void run() {
            list.clickOnItem(index, count);
        }
    }

    protected void clickListItemNoBlock(JListOperator jlList, int iIndex, int iCount) {
        (new Thread(new DummyClick(jlList, iIndex, iCount))).start();
    }

    protected void clickForTextPopup(EditorOperator eo, String menu) {
        JEditorPaneOperator txt = eo.txtEditorPane();
        JEditorPane epane = (JEditorPane) txt.getSource();
        try {
            Rectangle rct = epane.modelToView(epane.getCaretPosition());
            txt.clickForPopup(rct.x, rct.y);
            JPopupMenuOperator popup = new JPopupMenuOperator();
            popup.pushMenu(menu);
        } catch (BadLocationException ex) {
            System.out.println("=== Bad location");
        }
    }

    protected void setProxy() {
        OptionsOperator optionsOper = OptionsOperator.invoke();
        optionsOper.selectGeneral();
        // "Manual Proxy Setting"
        new JRadioButtonOperator(optionsOper, "Manual").push();
        // "HTTP Proxy:"
        JLabelOperator jloHost = new JLabelOperator(optionsOper, "HTTP Proxy");
        new JTextFieldOperator((JTextField) jloHost.getLabelFor()).typeText("emea-proxy.uk.oracle.com"); // NOI18N
        // "Port:"
        JLabelOperator jloPort = new JLabelOperator(optionsOper, "Port");
        new JTextFieldOperator((JTextField) jloPort.getLabelFor()).setText("80"); // NOI18N
        optionsOper.ok();
    }

    protected void downloadGlobalNodeJS() {
        if (GeneralNodeJs.sourcesDownloaded) {
            return;
        }
        setProxy();
        OptionsOperator optionsOper = OptionsOperator.invoke();
        optionsOper.selectMiscellaneous();
        Component findComponent = optionsOper.findSubComponent(new JTabbedPaneOperator.JTabbedPaneFinder());
        JTabbedPaneOperator tabbedPane = new JTabbedPaneOperator((JTabbedPane) findComponent);
        tabbedPane.selectPage("Node.js");
        JButtonOperator downloadBtn = new JButtonOperator(tabbedPane, "Download Sources");
        downloadBtn.press();
        evt.waitNoEvent(500);
        long defaultTimeout = JemmyProperties.getCurrentTimeout("ComponentOperator.WaitComponentEnabledTimeout");

        try {
            JemmyProperties.setCurrentTimeout("ComponentOperator.WaitComponentEnabledTimeout", 30000);
            downloadBtn = new JButtonOperator(tabbedPane, "Download Sources");
            Waiter waiter = new Waiter(new Waitable() {
                public Object actionProduced(Object obj) {
                    if (((JButtonOperator) obj).isEnabled()) {
                        return (obj);
                    } else {
                        return (null);
                    }
                }

                public String getDescription() {
                    return "Component enabled: ";
                }
            });
            waiter.setOutput(downloadBtn.getOutput());
            waiter.setTimeoutsToCloneOf(downloadBtn.getTimeouts(), "ComponentOperator.WaitComponentEnabledTimeout");
            waiter.waitAction(downloadBtn);

            JemmyProperties.setCurrentTimeout("ComponentOperator.WaitComponentEnabledTimeout", defaultTimeout);
        } catch (InterruptedException ex) {
            JemmyProperties.setCurrentTimeout("ComponentOperator.WaitComponentEnabledTimeout", defaultTimeout);
            Exceptions.printStackTrace(ex);
        }
        optionsOper.ok();
        evt.waitNoEvent(5000);
        waitCompletionScanning();
        GeneralNodeJs.sourcesDownloaded = true;
    }

    protected void checkResult(EditorOperator eo, String sCheck) {
        checkResult(eo, sCheck, 0);
    }

    protected void checkResult(EditorOperator eo, String sCheck, int iOffset) {
        String sText = eo.getText(eo.getLineNumber() + iOffset);
        if (-1 == sText.indexOf(sCheck)) {
            log("Trace wrong completion:");
            String text = eo.getText(eo.getLineNumber() + iOffset).replace("\r\n", "").replace("\n", "");
            int count = 0;
            while (!text.isEmpty() && count < 20) {
                eo.pushKey(KeyEvent.VK_Z, KeyEvent.CTRL_MASK);
                text = eo.getText(eo.getLineNumber() + iOffset).replace("\r\n", "").replace("\n", "");;
                log(">>" + text + "<<");
                count++;
            }
            fail("Invalid completion: \"" + sText + "\", should be: \"" + sCheck + "\"");

        }
    }

    protected CompletionInfo getCompletion() {
        CompletionInfo result = new CompletionInfo();
        result.listItself = null;
        int iRedo = 10;
        while (true) {
            try {
                result.listItself = new CompletionJListOperator();
                try {
                    result.listItems = result.listItself.getCompletionItems();
                    Object o = result.listItems.get(0);
                    if (!o.toString().contains("Scanning in progress...")) {
                        return result;
                    }
                    evt.waitNoEvent(1000);
                } catch (java.lang.Exception ex) {
                    return null;
                }
            } catch (JemmyException ex) {
                System.out.println("Wait completion timeout.");
                ex.printStackTrace();
                if (0 == --iRedo) {
                    return null;
                }
            }
        }
    }

    protected Object[] getAnnotations(EditorOperator eOp, int limit) {
        eOp.makeComponentVisible();
        evt.waitNoEvent(1000);
        try {
            final EditorOperator eo = new EditorOperator(eOp.getName());
            final int _limit = limit;
            new Waiter(new Waitable() {
                @Override
                public Object actionProduced(Object oper) {
                    return eo.getAnnotations().length > _limit ? Boolean.TRUE : null;
                }

                @Override
                public String getDescription() {
                    return ("Wait parser annotations."); // NOI18N
                }
            }).waitAction(null);
        } catch (InterruptedException ex) {
            Exceptions.printStackTrace(ex);
        }
        Object[] anns = eOp.getAnnotations();
        return anns;
    }

    protected void checkCompletionItems(CompletionJListOperator jlist, String[] asIdeal) {
        String completionList = "";
        StringBuilder sb = new StringBuilder(":");
        for (String sCode : asIdeal) {
            int iIndex = jlist.findItemIndex(sCode, new CFulltextStringComparator());
            if (-1 == iIndex) {
                sb.append(sCode).append(",");
                if (completionList.length() < 1) {
                    try {
                        List list = jlist.getCompletionItems();
                        for (int i = 0; i < list.size(); i++) {

                            completionList += list.get(i) + "\n";
                        }
                    } catch (java.lang.Exception ex) {
                        System.out.println("#" + ex.getMessage());
                    }
                }
            }
        }
        if (sb.toString().length() > 1) {
            fail("Unable to find items " + sb.toString() + ". Completion list is " + completionList);
        }
    }

    protected void checkCompletionItems(HashSet<String> data, String[] asIdeal) {
        String completionList = "";
        StringBuilder sb = new StringBuilder(":");
        for (String sCode : asIdeal) {
            if (!data.contains(sCode)) {
                sb.append(sCode).append(",");
                if (completionList.length() < 1) {
                    try {
                        Iterator<String> it = data.iterator();
                        while (it.hasNext()) {
                            completionList += it.next() + "\n";
                        }
                    } catch (java.lang.Exception ex) {
                        System.out.println("#" + ex.getMessage());
                    }
                }
            }
        }
        if (sb.toString().length() > 1) {
            fail("Unable to find items " + sb.toString() + ". Completion list is " + completionList);
        }
    }

    protected void checkCompletionMatchesPrefix(List list, String prefix) {
        StringBuilder sb = new StringBuilder();
        prefix = prefix.toLowerCase();
        String item = "";
        for (int i = 0; i < list.size(); i++) {
            item = list.get(i).toString().toLowerCase();
            if (!item.startsWith(prefix)) {
                sb.append(item).append("\n");
            }
        }

        if (sb.toString().length() > 1) {
            fail("Completion contains nonmatching items for prefix " + prefix + ". Completion list is " + sb.toString());
        }
    }

    protected void checkCompletionDoesntContainItems(CompletionJListOperator jlist, String[] invalidList) {
        for (String sCode : invalidList) {
            int iIndex = jlist.findItemIndex(sCode, new CFulltextStringComparator());
            if (-1 != iIndex) {
                fail("Completion list contains invalid item:" + sCode);
            }
        }
    }

    protected String findNonmatchingItems(CompletionJListOperator jlist, String[] invalidList) {
        StringBuilder sb = new StringBuilder();
        for (String sCode : invalidList) {
            int iIndex = jlist.findItemIndex(sCode, new CFulltextStringComparator());
            if (-1 != iIndex) {
                sb.append(sCode).append(",");

            }
        }
        return sb.toString();
    }

    protected boolean isSingleOption(String pattern, CompletionJListOperator jList) {
        try {
            pattern = pattern.toLowerCase();
            List items = jList.getCompletionItems();
            Object item;
            int matches = 0;
            for (int i = 0; i < items.size(); i++) {
                item = items.get(i);
                if (item instanceof HtmlCompletionItem) {
                    if (((HtmlCompletionItem) item).getItemText().toLowerCase().startsWith(pattern)) {
                        matches++;
                    }
                } else if (item.toString().toLowerCase().startsWith(pattern)) {
                    matches++;
                }
            }
            return matches == 1;
        } catch (Exception ex) {
            return false;
        }
    }

    public void openFile(String fileName, String projectName) {
        if (projectName == null) {
            throw new IllegalStateException("YOU MUST OPEN PROJECT FIRST");
        }
        Logger.getLogger(GeneralNodeJs.class.getName()).log(Level.INFO, "Opening file {0}", fileName);
        Node rootNode = new ProjectsTabOperator().getProjectRootNode(projectName);
        Node node = new Node(rootNode, "Sources|" + fileName);
        node.select();
        node.performPopupAction("Open");
    }

    protected void checkCompletionItems(
            CompletionInfo jlist,
            String[] asIdeal) {
        checkCompletionItems(jlist.listItself, asIdeal);
    }

    public void testCompletion(EditorOperator eo, int lineNumber) throws Exception {
        waitScanFinished();
        GeneralNodeJs.currentLine = lineNumber + 1;
        String rawLine = eo.getText(lineNumber);
        int start = rawLine.indexOf("//cc;");
        String rawConfig = rawLine.substring(start + 2);
        String[] config = rawConfig.split(";");
        eo.setCaretPosition(lineNumber + 1, Integer.parseInt(config[1]));
        type(eo, config[2]);
        eo.pressKey(KeyEvent.VK_ESCAPE);
        int back = Integer.parseInt(config[3]);
        for (int i = 0; i < back; i++) {
            eo.pressKey(KeyEvent.VK_LEFT);
        }

        eo.typeKey(' ', InputEvent.CTRL_MASK);
        CompletionInfo completion = getCompletion();
        CompletionJListOperator cjo = completion.listItself;
        checkCompletionItems(cjo, config[4].split(","));
        completion.listItself.hideAll();

        eo.pressKey(KeyEvent.VK_ESCAPE);
        eo.typeKey(' ', InputEvent.CTRL_MASK);
        completion = getCompletion();
        cjo = completion.listItself;
        String negResult = findNonmatchingItems(cjo, config[7].split(","));

        eo.pressKey(KeyEvent.VK_ESCAPE);
        if (config[5].length() > 0 && config[6].length() > 0) {
            String prefix = Character.toString(config[5].charAt(0));
            type(eo, prefix);
            evt.waitNoEvent(50);
            eo.typeKey(' ', InputEvent.CTRL_MASK);
            evt.waitNoEvent(20);
            if (!isSingleOption(prefix, cjo)) {
                completion = getCompletion();
                cjo = completion.listItself;
                checkCompletionMatchesPrefix(cjo.getCompletionItems(), prefix);
                evt.waitNoEvent(500);
                cjo.clickOnItem(config[5]);
                eo.pressKey(KeyEvent.VK_ENTER);
            }

            assertTrue("Wrong completion result: '" + eo.getText(lineNumber + 1) + "'", eo.getText(lineNumber + 1).contains(config[6].replaceAll("\\|", "")));
            completion.listItself.hideAll();
        }

        if (negResult.length() > 0) {
            fail("Completion list contains invalid items: " + negResult);
        }

    }

    public void testGoToDeclaration(EditorOperator eo, int lineNumber) throws Exception {
        waitScanFinished();
        String rawLine = eo.getText(lineNumber);
        int start = rawLine.indexOf("//gt;");
        String rawConfig = rawLine.substring(start + 2);
        String[] config = rawConfig.split(";");
        eo.setCaretPosition(lineNumber, Integer.parseInt(config[1]));

        evt.waitNoEvent(200);
        new org.netbeans.jellytools.actions.Action(null, null, KeyStroke.getKeyStroke(KeyEvent.VK_B, 2)).performShortcut(eo);
        evt.waitNoEvent(500);
        long defaultTimeout = JemmyProperties.getCurrentTimeout("ComponentOperator.WaitComponentTimeout");
        try {
            JemmyProperties.setCurrentTimeout("ComponentOperator.WaitComponentTimeout", 3000);
            EditorOperator ed = new EditorOperator(config[2]);
            JemmyProperties.setCurrentTimeout("ComponentOperator.WaitComponentTimeout", defaultTimeout);
            int position = ed.txtEditorPane().getCaretPosition();
            ed.setCaretPosition(Integer.valueOf(config[3]), Integer.valueOf(config[4].trim()));
            int expectedPosition = ed.txtEditorPane().getCaretPosition();
            assertTrue("Incorrect caret position. Expected position " + expectedPosition + " but was " + position, position == expectedPosition);
            ed.close(false);
        } catch (Exception e) {
            JemmyProperties.setCurrentTimeout("ComponentOperator.WaitComponentTimeout", defaultTimeout);
            fail(e.getMessage());
        }

    }

    public class CFulltextStringComparator implements Operator.StringComparator {

        public boolean equals(java.lang.String caption, java.lang.String match) {
            return caption.equals(match);
        }
    }

    public class CStartsStringComparator implements Operator.StringComparator {

        public boolean equals(java.lang.String caption, java.lang.String match) {
            return caption.startsWith(match);
        }
    }

}
