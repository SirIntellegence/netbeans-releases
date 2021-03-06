/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2007-2010 Oracle and/or its affiliates. All rights reserved.
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
 * Contributor(s):
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
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
 */

package org.netbeans.modules.profiler.snaptracer.impl.timeline;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.Rectangle;
import org.netbeans.lib.profiler.charts.ChartContext;
import org.netbeans.lib.profiler.charts.ChartDecorator;
import org.netbeans.lib.profiler.charts.swing.Utils;
import org.netbeans.lib.profiler.ui.UIUtils;

/**
 *
 * @author Jiri Sedlacek
 */
final class RowForegroundDecorator implements ChartDecorator {

    private static final Color SELECTED_FILTER = new Color(0, 0, 200, 25);
    private static final float[] FRACTIONS = new float[] { 0.0f, 0.49f, 0.51f, 1.0f };
    private static final Color[] COLORS = !UIUtils.isDarkResultsBackground() ?
        new Color[] { new Color(250, 251, 252, 120),
                      new Color(237, 240, 242, 120),
                      new Color(229, 233, 236, 125),
                      new Color(215, 221, 226, 130) } :
        new Color[] { new Color(050, 051, 052, 110),
                      new Color(037, 040, 042, 110),
                      new Color(29, 033, 036, 115),
                      new Color(015, 021, 026, 120) };

    private final TimelineChart chart;
    private final boolean gradient;
    private final boolean selection;


    RowForegroundDecorator(TimelineChart chart, boolean gradient, boolean selection) {
        this.chart = chart;
        this.gradient = gradient;
        this.selection = selection;
    }


    public void paint(Graphics2D g, Rectangle dirtyArea, ChartContext context) {
        if (gradient || selection) {
            int rowsCount = chart.getRowsCount();
            for (int i = 0; i < rowsCount; i++) {
                TimelineChart.Row row = chart.getRow(i);
                ChartContext rowContext = row.getContext();

                int y = Utils.checkedInt(rowContext.getViewportOffsetY());
                int h = Utils.checkedInt(rowContext.getViewportHeight() - 1);

                if (gradient) {
                    g.setPaint(new LinearGradientPaint(0, y, 0, y + h, FRACTIONS, COLORS));
                    g.fillRect(0, y, chart.getWidth(), h);
                }

                if (selection && chart.isRowSelected(row)) {
                    g.setColor(SELECTED_FILTER);
                    g.fillRect(0, y, chart.getWidth(), h);
                }
            }
        }
    }

}
