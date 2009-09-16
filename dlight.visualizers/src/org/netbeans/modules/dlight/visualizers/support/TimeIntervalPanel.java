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

/*
 * TimeIntervalPanel.java
 *
 * Created on Sep 2, 2009, 2:52:08 PM
 */
package org.netbeans.modules.dlight.visualizers.support;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.List;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.netbeans.modules.dlight.api.datafilter.DataFilter;
import org.netbeans.modules.dlight.api.datafilter.DataFilterListener;
import org.netbeans.modules.dlight.api.datafilter.DataFilterManager;
import org.netbeans.modules.dlight.api.datafilter.support.TimeIntervalDataFilter;
import org.netbeans.modules.dlight.api.datafilter.support.TimeIntervalDataFilterFactory;
import org.netbeans.modules.dlight.util.Range;

/**
 *
 * @author mt154047
 */
public final class TimeIntervalPanel extends javax.swing.JPanel implements DataFilterListener {

    private static final long NANOSECONDS_PER_SECOND = 1000000000;
    private DataFilterManager manager;

    /** Creates new form TimeIntervalPanel */
    public TimeIntervalPanel(DataFilterManager manager) {
        initComponents();
        this.manager = manager;
        if (manager != null){
            this.manager.addDataFilterListener(this);
            update(manager.getDataFilter(TimeIntervalDataFilter.class));
        }
        applyButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (TimeIntervalPanel.this.manager == null) {
                    applyButton.setEnabled(false);
                    return;
                }
                TimeIntervalPanel.this.manager.addDataFilter(TimeIntervalDataFilterFactory.create(getSelectedInterval()), false);

            }
        });
        removeFilterButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (TimeIntervalPanel.this.manager == null) {
                    return;
                }
                TimeIntervalPanel.this.manager.cleanAllDataFilter(TimeIntervalDataFilter.class);
            }
        });
        startTimeSpinner.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                //if end time more then start re-set end
                if (((Long)startTimeSpinner.getValue()) >= (Long)endTimeSpinner.getValue())
                    endTimeSpinner.setValue((Long)startTimeSpinner.getValue() + 1);
            }
        });
    }

    public void update(DataFilterManager manager) {
        if (this.manager != null) {
            this.manager.removeDataFilterListener(this);
        }

        this.manager = manager;
        

        
        applyButton.setEnabled(manager != null);
        
        if (this.manager != null){
            this.manager.addDataFilterListener(this);
            update(manager.getDataFilter(TimeIntervalDataFilter.class));
        }

    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        startTimeSpinner = new javax.swing.JSpinner();
        endTimeSpinner = new javax.swing.JSpinner();
        jLabel2 = new javax.swing.JLabel();
        applyButton = new javax.swing.JButton();
        removeFilterButton = new javax.swing.JButton();
        jLabel3 = new javax.swing.JLabel();

        jLabel1.setText(org.openide.util.NbBundle.getMessage(TimeIntervalPanel.class, "TimeIntervalPanel.jLabel1.text")); // NOI18N

        startTimeSpinner.setModel(new javax.swing.SpinnerNumberModel(Long.valueOf(0L), Long.valueOf(0L), null, Long.valueOf(1L)));

        endTimeSpinner.setModel(new javax.swing.SpinnerNumberModel(Long.valueOf(1L), Long.valueOf(1L), null, Long.valueOf(1L)));

        jLabel2.setText(org.openide.util.NbBundle.getMessage(TimeIntervalPanel.class, "TimeIntervalPanel.jLabel2.text")); // NOI18N

        applyButton.setText(org.openide.util.NbBundle.getMessage(TimeIntervalPanel.class, "TimeIntervalPanel.applyButton.text")); // NOI18N

        removeFilterButton.setText(org.openide.util.NbBundle.getMessage(TimeIntervalPanel.class, "TimeIntervalPanel.removeFilterButton.text")); // NOI18N

        jLabel3.setText(org.openide.util.NbBundle.getMessage(TimeIntervalPanel.class, "TimeIntervalPanel.jLabel3.text")); // NOI18N

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(jLabel1)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(startTimeSpinner, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 76, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jLabel2)
                .add(1, 1, 1)
                .add(endTimeSpinner, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 76, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(4, 4, 4)
                .add(jLabel3)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(applyButton)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(removeFilterButton)
                .addContainerGap(27, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel1)
                    .add(startTimeSpinner, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(endTimeSpinner, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel2)
                    .add(jLabel3)
                    .add(applyButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 26, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(removeFilterButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 26, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton applyButton;
    private javax.swing.JSpinner endTimeSpinner;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JButton removeFilterButton;
    private javax.swing.JSpinner startTimeSpinner;
    // End of variables declaration//GEN-END:variables

    public Range<Long> getSelectedInterval() {
        return new Range<Long>((Long) startTimeSpinner.getValue() * NANOSECONDS_PER_SECOND, (Long) endTimeSpinner.getValue() * NANOSECONDS_PER_SECOND);
    }

    public void dataFiltersChanged(List<DataFilter> newSet, boolean isAdjusting) {
        update(manager.getDataFilter(TimeIntervalDataFilter.class));
    }

    private final void update(Collection<TimeIntervalDataFilter> filters){
        if (filters == null || filters.isEmpty()) {
            return;
        }
        TimeIntervalDataFilter filter = null;
        for (DataFilter f : filters) {
            if (f instanceof TimeIntervalDataFilter) {
                filter = (TimeIntervalDataFilter) f;
                break;
            }
        }
        if (filter == null) {
            return;
        }
        endTimeSpinner.setValue((long)(filter.getInterval().getEnd()/NANOSECONDS_PER_SECOND));
        startTimeSpinner.setValue((long)filter.getInterval().getStart()/NANOSECONDS_PER_SECOND);
        
    }


}
