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
package org.netbeans.api.visual.action;

import org.netbeans.api.visual.widget.Widget;

import java.awt.*;
import java.util.Collection;

/**
 * This interface is used for collecting regions for which the moving widget has to be checked.
 *
 * @author David Kaspar
 */
public interface AlignWithWidgetCollector {

    /**
     * Returns a collection of regions (in scene coordination system) for a specified moving widget.
     * @param movingWidget the moving widget
     * @return the collection of regions
     */
    Collection<Rectangle> getRegions (Widget movingWidget);

}
