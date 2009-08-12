/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2009 Sun Microsystems, Inc. All rights reserved.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
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
 * Portions Copyrighted 2009 Sun Microsystems, Inc.
 */
package org.netbeans.modules.cnd.tha.actions;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeListener;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPopupMenu;
import org.netbeans.modules.cnd.tha.support.THAConfigurationImpl;
import org.openide.awt.Actions;
import org.openide.awt.Actions.CheckboxMenuItem;
import org.openide.awt.Actions.MenuItem;
import org.openide.awt.DropDownButtonFactory;
import org.openide.util.actions.Presenter;

public final class THAToolbarDropdownAction implements Action, Presenter.Toolbar {

    private final Action defaultAction;
    private Component toolbarPresenter;

    public THAToolbarDropdownAction() {
        defaultAction = THAActionsProvider.getDefault().getStartThreadAnalysisAction();
    }

    public Object getValue(String key) {
        return defaultAction.getValue(key);
    }

    public void putValue(String key, Object value) {
        defaultAction.putValue(key, value);
    }

    public void setEnabled(boolean b) {
        defaultAction.setEnabled(b);
    }

    public boolean isEnabled() {
        return defaultAction.isEnabled();
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        defaultAction.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        defaultAction.removePropertyChangeListener(listener);
    }

    public void actionPerformed(ActionEvent e) {
        defaultAction.actionPerformed(e);
    }

    public Component getToolbarPresenter() {
        if (toolbarPresenter == null) {

            final THAConfigurationImpl config = THAConfigurationImpl.getDefault();
            final CheckboxMenuItem enableRaceDetection = CheckBoxMenuItemFactory.createCheckboxMenuItem(config.getRacesDetectionSwitchAction());
            final CheckboxMenuItem startDataCollectionOnRun = CheckBoxMenuItemFactory.createCheckboxMenuItem(config.getStartOnRunSwitchAction());
            final MenuItem removeInstrumentation = new MenuItem(THAActionsProvider.getDefault().getRemoveInstrumentationAction(), true);

            final JPopupMenu dropdownPopup = new JPopupMenu() {

                @Override
                public void setVisible(boolean b) {
                    removeInstrumentation.setEnabled(THAActionsProvider.getDefault().getRemoveInstrumentationAction().isEnabled());
                    super.setVisible(b);
                }
            };

            dropdownPopup.add(defaultAction);
//            dropdownPopup.add(enableRaceDetection);
  //          dropdownPopup.add(startDataCollectionOnRun);
    //        dropdownPopup.addSeparator();
      //      dropdownPopup.add(removeInstrumentation);

//            final JButton button = AutoDropDownButtonFactory.createDropDownButton(
//                    new ImageIcon(new BufferedImage(24, 24,
//                    BufferedImage.TYPE_INT_ARGB)),
//                    dropdownPopup, defaultAction);

            //toolbarPresenter = button;
            JButton button =   DropDownButtonFactory.createDropDownButton(new ImageIcon(new BufferedImage(24, 24, BufferedImage.TYPE_INT_ARGB)), dropdownPopup);
            Actions.connect(button, defaultAction);
            toolbarPresenter = button;
        }

        return toolbarPresenter;
    }
}

