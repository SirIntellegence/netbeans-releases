/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2009 Sun Microsystems, Inc. All rights reserved.
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
 * Contributor(s):
 *
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

package org.netbeans.editor;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;
import javax.swing.text.JTextComponent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import javax.swing.text.Document;
import org.netbeans.api.editor.EditorRegistry;
import org.openide.modules.PatchedPublic;

/**
 * All the documents and components register here so that
 * they become available to the processing that crosses
 * different components and documents such as cross document
 * position stack or word matching.
 *
 * @author Miloslav Metelka
 * @version 1.00
 * @deprecated Use <code>EditorRegistry</code> from 
 *   <a href="@org-netbeans-modules-editor-lib2@/overview-summary.html">Editor Library 2</a> instead.
 */
public class Registry {

    /** List of the registered changes listeners */
    private static final WeakEventListenerList listenerList
	= new WeakEventListenerList();

    private static PropertyChangeListener editorRegistryListener = null;
    
    /** Add weak listener to listen to change of activity of documents or components.
     * The caller must
     * hold the listener object in some instance variable to prevent it
     * from being garbage collected.
     * @param l listener to add
     */
    public static void addChangeListener(ChangeListener l) {
        registerListener();
        listenerList.add(ChangeListener.class, l);
    }

    /** Remove listener for changes in activity. It's optional
     * to remove the listener. It would be done automatically
     * if the object holding the listener would be garbage collected.
     * @param l listener to remove
     */
    public static void removeChangeListener(ChangeListener l) {
        registerListener();
        listenerList.remove(ChangeListener.class, l);
    }

    /** Get document ID from the document.
     * @return document id or -1 if document was not yet added to the registry
     *  by <code>addDocument()</code>.
     */
    @PatchedPublic
    private static int getID(BaseDocument doc) {
        registerListener();
        return -1;
    }
	
    /** Get component ID from the component.
     * @return component id or -1 if component was not yet added to the registry
     *  by <code>addComponent()</code>.
     */
    @PatchedPublic
    private static int getID(JTextComponent c) {
        registerListener();
        return -1;
    }

    /** Get document when its ID is known.
     * It's rather cheap operation.
     * @param docID document ID. It can be retrieved from the document
     *  by <code>getID(doc)</code>.
     * @return document instance or null when document no longer exists
     */
    @PatchedPublic
    private static BaseDocument getDocument(int docID) {
        registerListener();
        return null;
    }

    /** Get component when its ID is known.
     * It's rather cheap operation.
     * @param compID component ID. It can be retrieved from the component
     *  by <code>getID(c)</code>.
     * @return component instance or null when document no longer exists
     */
    @PatchedPublic
    private static JTextComponent getComponent(int compID) {
        registerListener();
        return null;
    }

    /** Add document to registry. Doesn't search for repetitive
     * adding.
     * @return registry unique ID of the document
     */
    @PatchedPublic
    private static int addDocument(BaseDocument doc) {
        registerListener();
        return -1;
    }

    /** Add component to registry. If the component is already registered
     * it returns the existing} ID. The document that is currently assigned
     * to the component is _not_ registered automatically.
     * @return ID of the component
     */
    @PatchedPublic
    private static int addComponent(JTextComponent c) {
        registerListener();
        return -1;
    }

    /** Remove component from registry. It's usually done when
     * the UI of the component is being deinstalled.
     * @return ID that the component had in the registry. The possible
     *  new ID will be different from this one. -1 will be returned
     *  if the component was not yet added to the registry.
     */
    @PatchedPublic
    private static int removeComponent(JTextComponent c) {
        registerListener();
        return -1;
    }

    /** Put the component to the first position in the array of last accessed
    * components. The activate of document is also called automatically.
    */
    @PatchedPublic
    private static void activate(JTextComponent c) {
        registerListener();
    }

    /** Put the document to the first position in the array of last accessed
     * documents. The document must be registered otherwise nothing
     * is done.
     * @param doc document to be activated
     */
    @PatchedPublic
    private static void activate(BaseDocument doc) {
        registerListener();
    }
    
    public static BaseDocument getMostActiveDocument() {
        registerListener();
        JTextComponent jtc = getMostActiveComponent();
        return jtc == null ? null : Utilities.getDocument(jtc);
    }

    public static BaseDocument getLeastActiveDocument() {
        registerListener();
        JTextComponent jtc = getLeastActiveComponent();
        return jtc == null ? null : Utilities.getDocument(jtc);
    }

    @PatchedPublic
    private static BaseDocument getLessActiveDocument(BaseDocument doc) {
        registerListener();
        return null;
    }

    @PatchedPublic
    private static BaseDocument getLessActiveDocument(int docID) {
        registerListener();
        return null;
    }

    @PatchedPublic
    private static BaseDocument getMoreActiveDocument(BaseDocument doc) {
        registerListener();
        return null;
    }

    @PatchedPublic
    private static BaseDocument getMoreActiveDocument(int docID) {
        registerListener();
        return null;
    }

    /** Get the iterator over the active documents. It starts with
     * the most active document till the least active document.
     * It's just the current snapshot so the iterator will
     * not reflect future changes.
     */
    public static Iterator<? extends Document> getDocumentIterator() {
        registerListener();
        final Iterator<? extends JTextComponent> cIt = getComponentIterator();
        return new Iterator<Document>() {
            public boolean hasNext() {
                return cIt.hasNext();
            }

            public Document next() {
                return cIt.next().getDocument();
            }

            public void remove() {
                cIt.remove();
            }
        };
    }

    public static JTextComponent getMostActiveComponent() {
        registerListener();
        return EditorRegistry.focusedComponent();
    }

    public static JTextComponent getLeastActiveComponent() {
        registerListener();
        List<? extends JTextComponent> l = EditorRegistry.componentList();
        return l.size() > 0 ? l.get(l.size() - 1) : null;
    }

    @PatchedPublic
    private static JTextComponent getLessActiveComponent(JTextComponent c) {
        registerListener();
        return null;
    }

    @PatchedPublic
    private static JTextComponent getLessActiveComponent(int compID) {
        registerListener();
        return null;
    }

    @PatchedPublic
    private static JTextComponent getMoreActiveComponent(JTextComponent c) {
        registerListener();
        return null;
    }

    @PatchedPublic
    private static JTextComponent getMoreActiveComponent(int compID) {
        registerListener();
        return null;
    }

    /** Get the iterator over the active components. It starts with
    * the most active component till the least active component.
    */
    public static Iterator<? extends JTextComponent> getComponentIterator() {
        registerListener();
        return EditorRegistry.componentList().iterator();
    }

    private static synchronized void registerListener() {
        if (editorRegistryListener == null) {
            editorRegistryListener = new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent evt) {
                    fireChange();
                }
            };
            EditorRegistry.addPropertyChangeListener(editorRegistryListener);
        }
    }

    private static void fireChange() {
	ChangeListener[] listeners
	    = (ChangeListener[])listenerList.getListeners(ChangeListener.class);
	ChangeEvent evt = new ChangeEvent(Registry.class);
	for (int i = 0; i < listeners.length; i++) {
	    listeners[i].stateChanged(evt);
	}
    }

    /** Debug the registry into string. */
    public static String registryToString() {
        registerListener();
        try {
            Method m = EditorRegistry.class.getDeclaredMethod("dumpItemList"); //NOI18N
            return (String) m.invoke(null);
        } catch (Exception e) {
            // ignore;
            return ""; //NOI18N
        }
    }

}
