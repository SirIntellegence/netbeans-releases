/*
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at http://www.netbeans.org/cddl.html
 * or http://www.netbeans.org/cddl.txt.
 *
 * When distributing Covered Code, include this CDDL Header Notice in each file
 * and include the License file at http://www.netbeans.org/cddl.txt.
 * If applicable, add the following below the CDDL Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 */

package org.netbeans.core.windows.view.ui;

import org.openide.awt.StatusDisplayer;
import org.openide.util.WeakListeners;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;

/** The status line component of the main window.
*
* @author Jaroslav Tulach, Jesse Glick
*/
final class StatusLine extends JLabel implements ChangeListener, Runnable {
    private StatusDisplayer d = StatusDisplayer.getDefault();
    /** Minimum size of the status line */
    private static final Dimension PREF_SIZE = new Dimension(100, 0);
    /** Minimum size of the status line */
    private static final Dimension MIN_SIZE = new Dimension(0, 0);

    /** Creates a new StatusLine */
    public StatusLine () {
        run();
        d.addChangeListener(WeakListeners.change(this, d));
    }
    
    public void updateUI() {
        super.updateUI();
        Font f = UIManager.getFont ("controlFont"); //NOI18N
        if (f == null) {
            f = UIManager.getFont ("Tree.font"); //NOI18N
        }
        if (f != null) {
            setFont(f);
        }
    }

    public void stateChanged(ChangeEvent e) {
        if(SwingUtilities.isEventDispatchThread()) {
            run();
        } else {
            SwingUtilities.invokeLater (this);
        }
    }
    
    /** Called in event queue, should update the status text.
    */
    public void run () {
        setText (d.getStatusText ());
    }
    
    /** #62967: Pref size so that status line is able to shrink as much as possible.
     */
    public Dimension getPreferredSize() {
        return PREF_SIZE;
    }
    
    /** #62967: Minimum size so that status line is able to shrink as much as possible.
     */
    public Dimension getMinimumSize() {
        return MIN_SIZE;
    }

}
