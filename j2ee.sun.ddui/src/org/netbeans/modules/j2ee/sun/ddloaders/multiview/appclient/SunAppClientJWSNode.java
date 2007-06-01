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

package org.netbeans.modules.j2ee.sun.ddloaders.multiview.appclient;

import org.netbeans.modules.j2ee.sun.dd.api.ASDDVersion;
import org.netbeans.modules.j2ee.sun.dd.api.client.SunApplicationClient;
import org.netbeans.modules.j2ee.sun.ddloaders.multiview.BaseSectionNode;
import org.netbeans.modules.j2ee.sun.share.configbean.customizers.other.AppClientJWSPanel;
import org.netbeans.modules.xml.multiview.ui.SectionNodeInnerPanel;
import org.netbeans.modules.xml.multiview.ui.SectionNodeView;
import org.openide.util.NbBundle;

/**
 * @author Peter Williams
 */
public class SunAppClientJWSNode extends BaseSectionNode {

    SunAppClientJWSNode(SectionNodeView sectionNodeView, SunApplicationClient sunAppClient, final ASDDVersion version) {
        super(sectionNodeView, true, sunAppClient, version,
                NbBundle.getMessage(SunAppClientJWSNode.class, "LBL_JWSHeader"), 
                ICON_BASE_MISC_NODE);
        setExpanded(true);
        this.helpProvider = true;
    }

    @Override
    protected SectionNodeInnerPanel createNodeInnerPanel() {
        SectionNodeView sectionNodeView = getSectionNodeView();
        return new AppClientJWSPanel(sectionNodeView, (SunApplicationClient) key, version);
    }
}
