/*
 *                 Sun Public License Notice
 * 
 * The contents of this file are subject to the Sun Public License
 * Version 1.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://www.sun.com/
 * 
 * The Original Code is NetBeans. The Initial Developer of the Original
 * Code is Sun Microsystems, Inc. Portions Copyright 1997-2000 Sun
 * Microsystems, Inc. All Rights Reserved.
 */

package org.netbeans.modules.web.core.jsploader;

import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.cookies.EditorCookie;
import org.openide.util.actions.CookieAction;
import org.openide.util.NbBundle;

/**
* Edit an object.
* @see EditCookie
*
* @author Jaroslav Tulach
*/
public class EditServletAction extends CookieAction {

    /** serialVersionUID */
    private static final long serialVersionUID = 183706095337315796L;

    /* Returns false - action should be disabled when a window with no
    * activated nodes is selected.
    *
    * @return false do not survive the change of focus
    */
    protected boolean surviveFocusChange () {
        return false;
    }

    /* Human presentable name of the action. This should be
    * presented as an item in a menu.
    * @return the name of the action
    */
    public String getName () {
        return NbBundle.getBundle(EditServletAction.class).getString("EditServlet");
    }

    /* Help context where to find more about the action.
    * @return the help context for this action
    */
    public HelpCtx getHelpCtx () {
        return new HelpCtx (EditServletAction.class);
    }
    
    /*
     * We always enable View Servlet action, but show an error message 
     * in case when JSP has not been compiled yet.
     */ 
    protected boolean enable(Node[] activatedNodes) {
/*        
        if (!super.enable(activatedNodes))
            return false;
        for (int i = 0; i < activatedNodes.length; i++) {
            JspDataObject jspdo = (JspDataObject)activatedNodes[i].getCookie(JspDataObject.class);
            if (jspdo != null) {
                jspdo.refreshPlugin(true);
                EditorCookie cook = jspdo.getServletEditor();
                if (cook != null)
                    return true;
            }
        }
        return false;
*/
        return true;
    }

    /* @return the mode of action. */
    protected int mode() {
        return MODE_ANY;
    }

    /* Creates a set of classes that are tested by this cookie.
    * Here only HtmlDataObject class is tested.
    *
    * @return list of classes the that this cookie tests
    */
    protected Class[] cookieClasses () {
        return new Class[] { JspDataObject.class };
    }

    /* Actually performs the action.
    * Calls edit on all activated nodes which supports
    * HtmlDataObject cookie.
    */
    protected void performAction (final Node[] activatedNodes) {
        for (int i = 0; i < activatedNodes.length; i++) {
            JspDataObject jspdo = (JspDataObject)activatedNodes[i].getCookie(JspDataObject.class);
            if (jspdo != null) {
                jspdo.refreshPlugin(true);
                EditorCookie cook = jspdo.getServletEditor();
                if (cook != null)
                    cook.open ();
                else {
                    //show error dialog
                    String msg = NbBundle.getMessage(EditServletAction.class, "ERR_CantEditServlet");
                    String title = NbBundle.getMessage(EditServletAction.class, "EditServlet");
                    NotifyDescriptor descriptor = new NotifyDescriptor(msg, title,
                            NotifyDescriptor.DEFAULT_OPTION, NotifyDescriptor.ERROR_MESSAGE,
                            new Object[]{NotifyDescriptor.OK_OPTION}, null);
                    DialogDisplayer.getDefault().notify(descriptor);
                }
            }
        }
    }
    
    protected boolean asynchronous() {
        return false;
    }
    
}
