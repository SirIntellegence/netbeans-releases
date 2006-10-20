/*
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at http://www.netbeans.org/cddl.html
 * or http://www.netbeans.org/cddl.txt.
 
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


package org.netbeans.modules.bpel.design.model.elements;

import java.awt.Graphics2D;
import org.netbeans.modules.bpel.design.geom.FInsets;
import org.netbeans.modules.bpel.design.geom.FRoundRectangle;
import org.netbeans.modules.bpel.design.geom.FShape;


public class NullBorder extends BorderElement{
    
    public NullBorder() {
        super(SHAPE, INSETS);
    }
    
    
    private static final FInsets INSETS = new FInsets(4);
    private static final FShape SHAPE = new FRoundRectangle(8, 8);
    
    public void paint(Graphics2D g2) {
        //NO_OP
    }
}

