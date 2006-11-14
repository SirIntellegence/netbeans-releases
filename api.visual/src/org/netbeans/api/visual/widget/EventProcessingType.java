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
package org.netbeans.api.visual.widget;

/**
 * The enum represents allowed types of event processing the is used to process the Swing event coming from a view component
 * and that should be delegated to widgets on the scene.
 *
 * @author David Kaspar
 */
public enum EventProcessingType {

    /**
     * Means that an event is processed by all widgets in whole scene. The order follows the tree hierarchy of a scene.
     */
    ALL_WIDGETS,

    /**
     * Means that an event is processed by a focused widget of a scene and then by its parents only.
     * If no focus widget is set, then this type does not do anything.
     */
    FOCUSED_WIDGET_AND_ITS_PARENTS,

//    FOCUSED_AND_ITS_CHILDREN

}
