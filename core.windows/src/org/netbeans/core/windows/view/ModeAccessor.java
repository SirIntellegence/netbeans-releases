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


package org.netbeans.core.windows.view;


import org.netbeans.core.windows.ModeImpl;
import org.openide.windows.TopComponent;

import java.awt.*;


/**
 * Class which is used as an access point to data wchih View is responsible
 * to process.
 *
 * @author  Peter Zavadsky
 */
interface ModeAccessor extends ElementAccessor {

    public String getName();

    public int getState();

    public int getKind();

    public Rectangle getBounds();

    public int getFrameState();

    public TopComponent getSelectedTopComponent();
    
    public TopComponent[] getOpenedTopComponents();
    
    public double getResizeWeight();
    
    public ModeImpl getMode();
    
}

