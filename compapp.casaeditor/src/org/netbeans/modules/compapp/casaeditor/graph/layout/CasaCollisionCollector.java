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
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2007 Sun
 * Microsystems, Inc. All Rights Reserved.
 */
package org.netbeans.modules.compapp.casaeditor.graph.layout;

import java.awt.Point;
import java.awt.Rectangle;
import org.netbeans.api.visual.widget.ConnectionWidget;
import org.netbeans.api.visual.widget.LayerWidget;
import org.netbeans.api.visual.widget.Widget;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.netbeans.api.visual.router.ConnectionWidgetCollisionsCollector;
import org.netbeans.modules.compapp.casaeditor.design.CasaModelGraphScene;
import org.netbeans.modules.compapp.casaeditor.graph.CasaNodeWidget;
import org.netbeans.modules.compapp.casaeditor.model.casa.CasaComponent;

/**
 * Modified to only register collisions from:
 * - connection widgets with no shared endpoints with another connection widget
 * - CasaNodeWidget widgets
 *
 * @author Josh Sandusky
 */
public class CasaCollisionCollector implements ConnectionWidgetCollisionsCollector {

    public static final int MAX_ORTHOGONAL_CONNECTIONS = 15;
    public static final int MAX_ORTHOGONAL_NODES       = 15;
    
    private static final int SPACING_EDGE = 4;
    private static final int SPACING_NODE = 6;
    private LayerWidget[] layers;

    
    public CasaCollisionCollector (LayerWidget... layers) {
        this.layers = layers;
    }

    
    public void collectCollisions (
            ConnectionWidget connectionWidget, 
            List<Rectangle> verticalCollisions, 
            List<Rectangle> horizontalCollisions)
    {
        CasaModelGraphScene scene = (CasaModelGraphScene) connectionWidget.getScene();
        CasaComponent component = (CasaComponent) scene.findObject(connectionWidget);
        if (component == null || !component.isInDocumentModel()) {
            return;
        }
        
        CasaComponent source = scene.getEdgeSource(component);
        CasaComponent target = scene.getEdgeTarget(component);
        
        for (Widget widget : getWidgets ()) {
            
            if (!widget.isValidated ()) {
                continue;
            }
            
            if (widget == connectionWidget) {
                continue;
            }
            
            if (widget instanceof ConnectionWidget) {
                ConnectionWidget iterConnection = (ConnectionWidget) widget;
                if (!iterConnection.isRouted ()) {
                    continue;
                }
                
                CasaComponent iterComponent = (CasaComponent) scene.findObject(iterConnection);
                if (iterComponent == null || !iterComponent.isInDocumentModel()) {
                    return;
                }
                
                // If there are any shared endpoints, then do not register the connection
                // as colliding with the given iterConnection.
                if (source != null) {
                    CasaComponent iterSource = scene.getEdgeSource(iterComponent);
                    if (source == iterSource) {
                        continue;
                    }
                }
                if (target != null) {
                    CasaComponent iterTarget = scene.getEdgeTarget(iterComponent);
                    if (target == iterTarget) {
                        continue;
                    }
                }
                
                List<Point> controlPoints = iterConnection.getControlPoints ();
                int last = controlPoints.size () - 1;
                for (int i = 0; i < last; i ++) {
                    Point point1 = controlPoints.get (i);
                    Point point2 = controlPoints.get (i + 1);
                    if (point1.x == point2.x) {
                        Rectangle rectangle = new Rectangle (point1.x, Math.min (point1.y, point2.y), 0, Math.abs (point2.y - point1.y));
                        rectangle.grow (SPACING_EDGE, SPACING_EDGE);
                        verticalCollisions.add (rectangle);
                    } else if (point1.y == point2.y) {
                        Rectangle rectangle = new Rectangle (Math.min (point1.x, point2.x), point1.y, Math.abs (point2.x - point1.x), 0);
                        rectangle.grow (SPACING_EDGE, SPACING_EDGE);
                        horizontalCollisions.add (rectangle);
                    }
                }
                
            // Check that the widget is a node widget.
            // This allows lines to go through other widgets, such as region labels.
            } else if (widget instanceof CasaNodeWidget) {
                Rectangle bounds = widget.getBounds ();
                Rectangle rectangle = widget.convertLocalToScene (bounds);
                rectangle.grow (SPACING_NODE, SPACING_NODE);
                verticalCollisions.add (rectangle);
                horizontalCollisions.add (rectangle);
            }
        }
    }

    protected Collection<Widget> getWidgets () {
        ArrayList<Widget> list = new ArrayList<Widget> ();
        for (LayerWidget layer : layers)
            list.addAll (layer.getChildren ());
        return list;
    }
}
