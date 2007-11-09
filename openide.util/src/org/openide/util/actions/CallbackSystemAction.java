/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
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
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2007 Sun
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

package org.openide.util.actions;

import java.awt.Component;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Action;
import javax.swing.ActionMap;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;
import org.openide.util.LookupListener;
import org.openide.util.Mutex;
import org.openide.util.Utilities;
import org.openide.util.WeakListeners;
import org.openide.util.WeakSet;

/** Action that can have a performer of the action attached to it at any time,
* or changed.
* The action will be automatically disabled
* when it has no performer.
* <p>Also may be made sensitive to changes in window focus.
* @author   Ian Formanek, Jaroslav Tulach, Petr Hamernik
*/
public abstract class CallbackSystemAction extends CallableSystemAction implements ContextAwareAction {
    /** action performer */
    private static final String PROP_ACTION_PERFORMER = "actionPerformer"; // NOI18N

    /** a list of all actions that has survive focus change set to false */
    private static final WeakSet<Class<? extends CallbackSystemAction>> notSurviving = new WeakSet<Class<? extends CallbackSystemAction>>(37);

    /** a list of actions surviving focus change */
    private static final WeakSet<Class<? extends CallbackSystemAction>> surviving = new WeakSet<Class<? extends CallbackSystemAction>>(37);

    /** key to access listener */
    private static final Object LISTENER = new Object();
    static final long serialVersionUID = -6305817805474624653L;

    /** logging */
    private static final Logger err = Logger.getLogger(
            "org.openide.util.actions.CallbackSystemAction"
        ); // NOI18N

    /** Initialize the action to have no performer.
    */
    protected void initialize() {
        super.initialize();
        updateEnabled();
        setSurviveFocusChange(false);
    }

    /** Get the current action performer.
    * @return the current action performer, or <code>null</code> if there is currently no performer
    * @deprecated use TopComponent.getActionMap() as described in the javadoc
    */
    @Deprecated
    public ActionPerformer getActionPerformer() {
        return (ActionPerformer) getProperty(PROP_ACTION_PERFORMER);
    }

    /** Set the action performer.
    * The specified value can be <code>null</code>, which means that the action will have no performer
    * and is disabled. ({@link #isEnabled} will return <code>false</code> regardless its previous state.)
    * <P>
    * This method is <em>too dynamic</em> it depends on the actuall order of callers and
    * is for example very fragile with respect to focus switching and correct delivering of
    * focus change events. That is why an alternative based on
    * <a href="http://openide.netbeans.org/proposals/actions/design.html#callback">ActionMap proposal</a>
    * has been developed.
    * <P>
    * So if you are providing a <a href="@org-openide-windows@/org/openide/windows/TopComponent.html">TopComponent</a>
    * and want to provide
    * your own handling of <a href="@org-openide-actions@/org/openide/actions/CopyAction.html">CopyAction</a> use following code:
    * <PRE>
    * TopComponent tc = ...;
    * javax.swing.Action yourCopyAction = ...; // the action to invoke instead of Copy
    *
    * CopyAction globalCopyAction = SystemAction.get (CopyAction.class);
    * Object key = globalCopyAction.getActionMapKey(); // key is a special value defined by all CallbackSystemActions
    *
    * // and finally:
    * tc.getActionMap ().put (key, yourCopyAction);
    * </PRE>
    * This code registers <code>yourCopyAction</code> with <code>tc</code>
    * top component, so whenever a <code>globalCopyAction</code> is invoked,
    * your action is being delegated to.
    *
    * @param performer the new action performer or <code>null</code> to disable
    *
    * @deprecated use TopComponent.getActionMap() as described in the javadoc
    */
    @Deprecated
    public void setActionPerformer(ActionPerformer performer) {
        putProperty(PROP_ACTION_PERFORMER, performer);
        updateEnabled();
    }

    /** Updates the enabled state by checking performer and ActionMap
     */
    private void updateEnabled() {
        Action action = GlobalManager.getDefault().findGlobalAction(
                getActionMapKey(), getSurviveFocusChange()
            );

        if (action != null) {
            setEnabled(action.isEnabled());

            synchronized (LISTENER) {
                ActionDelegateListener l = (ActionDelegateListener) getProperty(LISTENER);

                if ((l == null) || (l.get() != this)) {
                    l = new ActionDelegateListener(this, action);
                    putProperty(LISTENER, l);
                } else {
                    l.attach(action);
                }
            }
        } else {
            if (getActionPerformer() != null) {
                // we have performer
                setEnabled(true);
            } else {
                setEnabled(false);
            }

            clearListener();
        }
    }

    /** Clears the listener.
     */
    private void clearListener() {
        synchronized (LISTENER) {
            // remove listener on any action
            ActionDelegateListener l = (ActionDelegateListener) getProperty(LISTENER);

            if (l != null) {
                l.clear();
                putProperty(LISTENER, null);
            }
        }
    }

    /** Perform the action. Tries the performer and then scans the ActionMap
     * of selected topcomponent.
     */
    public void actionPerformed(final ActionEvent ev) {
        // First try global context action.
        final Action action = GlobalManager.getDefault().findGlobalAction(getActionMapKey(), getSurviveFocusChange());

        if (action != null) {
            if (action.isEnabled()) {
                action.actionPerformed(ev);
            } else {
                Toolkit.getDefaultToolkit().beep();
            }

            return;
        }

        final Object ap = getActionPerformer();

        if (ap != null) {
            org.netbeans.modules.openide.util.ActionsBridge.doPerformAction(
                this,
                new org.netbeans.modules.openide.util.ActionsBridge.ActionRunnable(ev, this, asynchronous ()) {
                    public void run() {
                        if (ap == getActionPerformer()) {
                            getActionPerformer().performAction(CallbackSystemAction.this);
                        }
                    }
                }
            );

            return;
        }

        Toolkit.getDefaultToolkit().beep();
    }

    /** Perform the action.
    * This default implementation calls the assigned action performer if it
    * exists, otherwise does nothing.
     * @deprecated This only uses {@link ActionPerformer}. Use {@link #actionPerformed} instead.
    */
    @Deprecated
    public void performAction() {
        ActionPerformer ap = getActionPerformer();

        if (ap != null) {
            ap.performAction(this);
        }
    }

    /** Getter for action map key, which is used to find action from provided
     * context (i.e. <code>ActionMap</code> provided by the context),
     * which acts as a callback.
     * Override this method in subclasses to provide 'nice' key.
     * @return key which is used to find the action which performs callback,
     *      default returned key is a class name.
     * @since 3.29 */
    public Object getActionMapKey() {
        return getClass().getName();
    }

    /** Test whether the action will survive a change in focus.
    * By default, it will not.
    * @return <code>true</code> if the enabled state of the action survives focus changes
    */
    public boolean getSurviveFocusChange() {
        getProperty(null); // force initialization

        return !notSurviving.contains(getClass());
    }

    /** Implements <code>ContextAwareAction</code> interface method. */
    public Action createContextAwareInstance(Lookup actionContext) {
        return new DelegateAction(this, actionContext);
    }

    /** Set whether the action will survive a change in focus.
    * If <code>false</code>, then the action will be automatically
    * disabled (using {@link #setActionPerformer}) when the window
    * focus changes.
    *
    * @param b <code>true</code> to survive focus changes, <code>false</code> to be sensitive to them
    */
    public void setSurviveFocusChange(boolean b) {
        synchronized (notSurviving) {
            if (b) {
                notSurviving.remove(getClass());
                surviving.add(getClass());
            } else {
                notSurviving.add(getClass());
                surviving.remove(getClass());
            }
        }
    }

    /** Array of actions from a set of classes.
     */
    private static List<CallbackSystemAction> toInstances(java.util.Set<Class<? extends CallbackSystemAction>> s) {
        List<CallbackSystemAction> actions;

        synchronized (notSurviving) {
            actions = new ArrayList<CallbackSystemAction>(s.size());

            for (Class<? extends CallbackSystemAction> c : s) {

                CallbackSystemAction a = SystemAction.findObject(c, false);

                if (a != null) {
                    actions.add(a);
                }
            }
        }

        return actions;
    }

    /** Clears all action performers for those that has setSurviveFocusChange
     * on true.
     */
    private static void clearActionPerformers() {
        List<CallbackSystemAction> actions = toInstances(notSurviving);

        // clear the performers out of any loop
        for (CallbackSystemAction a : actions) {
            a.setActionPerformer(null);
        }

        actions = toInstances(surviving);

        // clear the performers out of any loop
        for (CallbackSystemAction a : actions) {

            if (err.isLoggable(Level.FINE)) {
                err.fine("updateEnabled: " + a); // NOI18N
            }

            a.updateEnabled();
        }
    }

    /** Listener on a global context.
     */
    private static final class GlobalManager implements LookupListener {
        private static GlobalManager instance;
        private Lookup.Result<ActionMap> result;
        private List<Reference<ActionMap>> actionMaps = new ArrayList<Reference<ActionMap>>(2);
        private final ActionMap survive = new ActionMap();

        private GlobalManager() {
            result = Utilities.actionsGlobalContext().lookup(new Lookup.Template<ActionMap>(ActionMap.class));
            result.addLookupListener(this);
            resultChanged(null);
        }

        public synchronized static GlobalManager getDefault() {
            if (instance != null) {
                return instance;
            }

            instance = new GlobalManager();

            return instance;
        }

        public Action findGlobalAction(Object key, boolean surviveFocusChange) {
            // search action in all action maps from global context
            Action a = null;
            for (Reference<ActionMap> ref : actionMaps) {
                ActionMap am = ref.get();
                a = am == null ? null : am.get(key);
                if (a != null) {
                    break;
                }
            }

            if (surviveFocusChange) {
                if (a == null) {
                    a = survive.get(key);

                    if (a != null) {
                        a = ((WeakAction) a).getDelegate();
                    }

                    if (err.isLoggable(Level.FINE)) {
                        err.fine("No action for key: " + key + " using delegate: " + a); // NOI18N
                    }
                } else {
                    if (err.isLoggable(Level.FINE)) {
                        err.fine("New action for key: " + key + " put: " + a);
                    }

                    survive.put(key, new WeakAction(a));
                }
            }

            if (err.isLoggable(Level.FINE)) {
                err.fine("Action for key: " + key + " is: " + a); // NOI18N
            }

            return a;
        }

        /** Change all that do not survive ActionMap change */
        public void resultChanged(org.openide.util.LookupEvent ev) {
            Collection<? extends ActionMap> ams = result.allInstances();

            if (err.isLoggable(Level.FINE)) {
                err.fine("changed maps : " + ams); // NOI18N
                err.fine("previous maps: " + actionMaps); // NOI18N
            }

            // do nothing if maps are actually the same
            if (ams.size() == actionMaps.size()) {
                boolean theSame = true;
                int i = 0;
                for (Iterator<? extends ActionMap> newMaps = ams.iterator(); newMaps.hasNext(); i++) {
                    ActionMap oldMap = actionMaps.get(i).get();
                    if (oldMap == null || oldMap != newMaps.next()) {
                        theSame = false;
                        break;
                    }
                }
                if (theSame) {
                    return;
                }
            }

            // update actionMaps
            actionMaps.clear();
            for (ActionMap actionMap : ams) {
                actionMaps.add(new WeakReference<ActionMap>(actionMap));
            }

            if (err.isLoggable(Level.FINE)) {
                err.fine("clearActionPerformers"); // NOI18N
            }

            Mutex.EVENT.readAccess(new Runnable() {
                public void run() {
                    clearActionPerformers();
                }
            });
        }
    }
     // end of LookupListener

    /** An action that holds a weak reference to other action.
     */
    private static final class WeakAction extends WeakReference<Action> implements Action {
        public WeakAction(Action delegate) {
            super(delegate);
        }

        public Action getDelegate() {
            return get();
        }

        public Object getValue(String key) {
            throw new UnsupportedOperationException();
        }

        public void putValue(String key, Object value) {
            throw new UnsupportedOperationException();
        }

        public void actionPerformed(ActionEvent e) {
            throw new UnsupportedOperationException();
        }

        public void removePropertyChangeListener(PropertyChangeListener listener) {
            throw new UnsupportedOperationException();
        }

        public void addPropertyChangeListener(PropertyChangeListener listener) {
            throw new UnsupportedOperationException();
        }

        public void setEnabled(boolean b) {
            throw new UnsupportedOperationException();
        }

        public boolean isEnabled() {
            throw new UnsupportedOperationException();
        }
    }

    /** A class that listens on changes in enabled state of an action
     * and updates the state of the action according to it.
     */
    private static final class ActionDelegateListener extends WeakReference<CallbackSystemAction> implements PropertyChangeListener {
        private Reference<Action> delegate;

        public ActionDelegateListener(CallbackSystemAction c, Action delegate) {
            super(c);
            this.delegate = new WeakReference<Action>(delegate);
            delegate.addPropertyChangeListener(this);
        }

        public void clear() {
            Action a;

            Reference<Action> d = delegate;
            a = d == null ? null : d.get();

            if (a == null) {
                return;
            }

            delegate = null;

            a.removePropertyChangeListener(this);
        }

        public void attach(Action action) {
            Reference<Action> d = delegate;

            if ((d != null) && (d.get() == action)) {
                return;
            }

            Action prev = d.get();

            // reattaches to different action
            if (prev != null) {
                prev.removePropertyChangeListener(this);
            }

            this.delegate = new WeakReference<Action>(action);
            action.addPropertyChangeListener(this);
        }

        public void propertyChange(java.beans.PropertyChangeEvent evt) {
            synchronized (LISTENER) {
                Reference<Action> d = delegate;

                if ((d == null) || (d.get() == null)) {
                    return;
                }
            }

            CallbackSystemAction c = get();

            if (c != null) {
                c.updateEnabled();
            }
        }
    }

    /** A delegate action that is usually associated with a specific lookup and
     * extract the nodes it operates on from it. Otherwise it delegates to the
     * regular NodeAction.
     */
    private static final class DelegateAction extends Object implements Action,
        LookupListener, Presenter.Menu, Presenter.Popup, Presenter.Toolbar, PropertyChangeListener {
        /** action to delegate too */
        private CallbackSystemAction delegate;

        /** lookup we are associated with (or null) */
        private Lookup.Result<ActionMap> result;

        /** previous state of enabled */
        private boolean enabled;

        /** support for listeners */
        private PropertyChangeSupport support = new PropertyChangeSupport(this);

        /** listener to check listen on state of action(s) we delegate to */
        private PropertyChangeListener weakL;

        /** last action we were listening to */
        private Reference<Action> lastRef;

        public DelegateAction(CallbackSystemAction a, Lookup actionContext) {
            this.delegate = a;
            this.weakL = org.openide.util.WeakListeners.propertyChange(this, null);
            this.enabled = a.getActionPerformer() != null;

            this.result = actionContext.lookup(new Lookup.Template<ActionMap>(ActionMap.class));
            this.result.addLookupListener(WeakListeners.create(LookupListener.class, this, this.result));
            resultChanged(null);
        }

        /** Overrides superclass method, adds delegate description. */
        public String toString() {
            return super.toString() + "[delegate=" + delegate + "]"; // NOI18N
        }

        /** Invoked when an action occurs.
         */
        public void actionPerformed(final java.awt.event.ActionEvent e) {
            final Action a = findAction();

            if (a != null) {
                org.netbeans.modules.openide.util.ActionsBridge.ActionRunnable run;
                run = new org.netbeans.modules.openide.util.ActionsBridge.ActionRunnable(e, delegate, delegate.asynchronous()) {
                            public void run() {
                                a.actionPerformed(e);
                            }
                        };

                org.netbeans.modules.openide.util.ActionsBridge.doPerformAction(delegate, run);
            } else {
                // XXX #30303 if the action falls back to the old behaviour
                // it may not be performed in case it is in dialog and
                // is not transmodal. 
                // This is just a hack, see TopComponent.processKeyBinding.
                Object source = e.getSource();

                if (
                    source instanceof Component &&
                        javax.swing.SwingUtilities.getWindowAncestor((Component) source) instanceof java.awt.Dialog
                ) {
                    Object value = delegate.getValue("OpenIDE-Transmodal-Action"); // NOI18N

                    if (!Boolean.TRUE.equals(value)) {
                        return;
                    }
                }

                delegate.actionPerformed(e);
            }
        }

        public boolean isEnabled() {
            Action a = findAction();

            if (a == null) {
                a = delegate;
            }

            // 40915 - hold last action weakly
            Action last = lastRef == null ? null : lastRef.get();

            if (a != last) {
                if (last != null) {
                    last.removePropertyChangeListener(weakL);
                }

                lastRef = new WeakReference<Action>(a);
                a.addPropertyChangeListener(weakL);
            }

            return a.isEnabled();
        }

        public void addPropertyChangeListener(PropertyChangeListener listener) {
            support.addPropertyChangeListener(listener);
        }

        public void removePropertyChangeListener(PropertyChangeListener listener) {
            support.removePropertyChangeListener(listener);
        }

        public void putValue(String key, Object o) {
        }

        public Object getValue(String key) {
            return delegate.getValue(key);
        }

        public void setEnabled(boolean b) {
        }

        public void resultChanged(org.openide.util.LookupEvent ev) {
            boolean newEnabled = isEnabled();

            if (newEnabled != enabled) {
                support.firePropertyChange(PROP_ENABLED, enabled, newEnabled);
                enabled = newEnabled;
            }
        }

        public void propertyChange(PropertyChangeEvent evt) {
            resultChanged(null);
        }

        /*** Finds an action that we should delegate to
         * @return the action or null
         */
        private Action findAction() {
            Collection<? extends ActionMap> c = result != null ? result.allInstances() : Collections.<ActionMap>emptySet();

            if (!c.isEmpty()) {
                Object key = delegate.getActionMapKey();
                for (ActionMap map : c) {
                    Action action = map.get(key);
                    if (action != null) {
                        return action;
                    }
                }
            }

            return null;
        }

        public javax.swing.JMenuItem getMenuPresenter() {
            if (isMethodOverridden(delegate, "getMenuPresenter")) { // NOI18N

                return delegate.getMenuPresenter();
            } else {
                return org.netbeans.modules.openide.util.AWTBridge.getDefault().createMenuPresenter(this);
            }
        }

        public javax.swing.JMenuItem getPopupPresenter() {
            if (isMethodOverridden(delegate, "getPopupPresenter")) { // NOI18N

                return delegate.getPopupPresenter();
            } else {
                return org.netbeans.modules.openide.util.AWTBridge.getDefault().createPopupPresenter(this);
            }
        }

        public java.awt.Component getToolbarPresenter() {
            if (isMethodOverridden(delegate, "getToolbarPresenter")) { // NOI18N

                return delegate.getToolbarPresenter();
            } else {
                return org.netbeans.modules.openide.util.AWTBridge.getDefault().createToolbarPresenter(this);
            }
        }

        private boolean isMethodOverridden(CallableSystemAction d, String name) {
            try {
                java.lang.reflect.Method m = d.getClass().getMethod(name, new Class[0]);

                return m.getDeclaringClass() != CallableSystemAction.class;
            } catch (java.lang.NoSuchMethodException ex) {
                ex.printStackTrace();
                throw new IllegalStateException("Error searching for method " + name + " in " + d); // NOI18N
            }
        }

        protected void finalize() {
            Action last = lastRef == null ? null : lastRef.get();

            if (last != null) {
                last.removePropertyChangeListener(weakL);
            }
        }
    }
     // end of DelegateAction
}
