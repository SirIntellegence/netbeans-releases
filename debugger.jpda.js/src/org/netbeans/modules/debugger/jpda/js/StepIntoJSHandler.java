/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2013 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
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
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2013 Sun Microsystems, Inc.
 */

package org.netbeans.modules.debugger.jpda.js;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.InternalException;
import com.sun.jdi.ObjectCollectedException;
import com.sun.jdi.StackFrame;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.VMDisconnectedException;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.request.EventRequestManager;
import com.sun.jdi.request.InvalidRequestStateException;
import com.sun.jdi.request.StepRequest;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.api.debugger.ActionsManager;
import org.netbeans.api.debugger.ActionsManagerListener;
import org.netbeans.api.debugger.Breakpoint;
import org.netbeans.api.debugger.DebuggerManager;
import org.netbeans.api.debugger.LazyActionsManagerListener;
import org.netbeans.api.debugger.jpda.CallStackFrame;
import org.netbeans.api.debugger.jpda.ClassVariable;
import org.netbeans.api.debugger.jpda.InvalidExpressionException;
import org.netbeans.api.debugger.jpda.JPDAClassType;
import org.netbeans.api.debugger.jpda.JPDADebugger;
import org.netbeans.api.debugger.jpda.JPDAThread;
import org.netbeans.api.debugger.jpda.LocalVariable;
import org.netbeans.api.debugger.jpda.MethodBreakpoint;
import org.netbeans.api.debugger.jpda.ObjectVariable;
import org.netbeans.api.debugger.jpda.Variable;
import org.netbeans.api.debugger.jpda.event.JPDABreakpointEvent;
import org.netbeans.api.debugger.jpda.event.JPDABreakpointListener;
import org.netbeans.modules.debugger.jpda.js.vars.DebuggerSupport;
import org.netbeans.spi.debugger.ContextProvider;
import org.openide.util.Exceptions;

/**
 * Handler of step into JavaScript code from Java.
 * 
 * @author Martin
 */
@LazyActionsManagerListener.Registration(path="netbeans-JPDASession/Java")
public class StepIntoJSHandler extends LazyActionsManagerListener implements PropertyChangeListener {
    
    private static final String SCRIPT_ACCESS_CLASS = "jdk.nashorn.internal.runtime.ScriptFunctionData";    // NOI18N
    private static final String[] SCRIPT_ACCESS_METHODS = { "invoke", "construct" };        // NOI18N
    // New notifyInvoke API:
    private static final String SCRIPT_NOTIFY_INVOKE_METHOD = "notifyInvoke";   // NOI18N
    private static final String SCRIPT_NOTIFY_INVOKE_METHOD_SIG = "(Ljava/lang/invoke/MethodHandle;)V"; // NOI18N
    private static final String SCRIPT_NOTIFY_INVOKE_METHOD_ARG = "mh";         // NOI18N
    
    private static final Logger logger = Logger.getLogger(StepIntoJSHandler.class.getCanonicalName());
    
    private final JPDADebugger debugger;
    private final MethodBreakpoint[] scriptAccessBPs;
    private final MethodBreakpoint notifyInvokeBP;
    private volatile boolean isNotifyInvoke;
    
    public StepIntoJSHandler(ContextProvider lookupProvider) {
        debugger = lookupProvider.lookupFirst(null, JPDADebugger.class);
        debugger.addPropertyChangeListener(JPDADebugger.PROP_CURRENT_CALL_STACK_FRAME, new CurrentSFTracker());
        ScriptBPListener sbl = new ScriptBPListener();
        int mbn = SCRIPT_ACCESS_METHODS.length;
        scriptAccessBPs = new MethodBreakpoint[mbn];
        for (int i = 0; i < mbn; i++) {
            String method = SCRIPT_ACCESS_METHODS[i];
            MethodBreakpoint mb = MethodBreakpoint.create(SCRIPT_ACCESS_CLASS, method);
            mb.setHidden(true);
            mb.setSuspend(debugger.getSuspend());
            mb.setSession(debugger);
            mb.disable();
            mb.addJPDABreakpointListener(sbl);
            DebuggerManager.getDebuggerManager().addBreakpoint(mb);
            scriptAccessBPs[i] = mb;
        }
        ScriptInvokeBPListener sibl = new ScriptInvokeBPListener();
        notifyInvokeBP = MethodBreakpoint.create(DebuggerSupport.DEBUGGER_SUPPORT_CLASS,
                                                 SCRIPT_NOTIFY_INVOKE_METHOD);
        notifyInvokeBP.setMethodSignature(SCRIPT_NOTIFY_INVOKE_METHOD_SIG);
        notifyInvokeBP.setHidden(true);
        notifyInvokeBP.setSuspend(debugger.getSuspend());
        notifyInvokeBP.setSession(debugger);
        notifyInvokeBP.disable();
        notifyInvokeBP.addJPDABreakpointListener(sibl);
        DebuggerManager.getDebuggerManager().addBreakpoint(notifyInvokeBP);
        notifyInvokeBP.addPropertyChangeListener(Breakpoint.PROP_VALIDITY, new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (Breakpoint.VALIDITY.VALID.equals(notifyInvokeBP.getValidity())) {
                    // notifyInvoke is available we can remove the script access breakpoints
                    logger.log(Level.FINE, "{0} is valid => we can disable breakpoints on "+SCRIPT_ACCESS_CLASS, notifyInvokeBP);
                    for (MethodBreakpoint mb : scriptAccessBPs) {
                        logger.log(Level.FINE, "{0} disable", mb);
                        mb.disable();
                        DebuggerManager.getDebuggerManager().removeBreakpoint(mb);
                    }
                    isNotifyInvoke = true;
                }
            }
        });
    }

    @Override
    protected void destroy() {
        logger.fine("\nStepIntoJSHandler.destroy()");
        if (!isNotifyInvoke) {
            for (MethodBreakpoint mb : scriptAccessBPs) {
                logger.log(Level.FINE, "{0} disable", mb);
                mb.disable();
                DebuggerManager.getDebuggerManager().removeBreakpoint(mb);
            }
        }
        logger.log(Level.FINE, "{0} disable", notifyInvokeBP);
        notifyInvokeBP.disable();
        DebuggerManager.getDebuggerManager().removeBreakpoint(notifyInvokeBP);
    }

    @Override
    public String[] getProperties() {
        return new String[] { ActionsManagerListener.PROP_ACTION_PERFORMED, "actionToBeRun" };
    }

    @Override
    public void actionPerformed(Object action) {
        if (ActionsManager.ACTION_STEP_INTO.equals(action)) {
            //scriptBP.disable(); - no, the action may end too soon, some work
            //                      can continue on background
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if ("actionToBeRun".equals(evt.getPropertyName())) {
            Object action = evt.getNewValue();
            if (ActionsManager.ACTION_STEP_INTO.equals(action)) {
                if (!isNotifyInvoke) {
                    for (MethodBreakpoint mb : scriptAccessBPs) {
                        logger.log(Level.FINE, "{0} enable", mb);
                        mb.enable();
                    }
                }
                logger.log(Level.FINE, "{0} enable", notifyInvokeBP);
                notifyInvokeBP.enable();
                //scriptBP.enable();
            }
        }
    }
    
    private class CurrentSFTracker implements PropertyChangeListener {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (evt.getNewValue() == null) {
                // Ignore resume.
                return ;
            }
            logger.fine("Current frame changed>");
            if (!isNotifyInvoke) {
                for (MethodBreakpoint mb : scriptAccessBPs) {
                    logger.log(Level.FINE, " {0} disable", mb);
                    mb.disable();
                }
            }
            logger.log(Level.FINE, " {0} disable", notifyInvokeBP);
            notifyInvokeBP.disable();
        }
    }
    
    private void scriptToBeInvoked(ObjectVariable mh) {
        ObjectVariable member = (ObjectVariable) mh.getField("member");
        if (!(member instanceof ObjectVariable)) {
            logger.info("Variable "+mh+" does not have member field: "+member);
            return ;
        }
        ObjectVariable clazz = (ObjectVariable) member.getField("clazz");
        if (!(clazz instanceof ClassVariable)) {
            logger.info("Variable "+mh+" does not have clazz field: "+clazz);
            return ;
        }
        //JPDAClassType classType = ((ClassVariable) clazz).getReflectedType();
        JPDAClassType classType;
        try {
            classType = (JPDAClassType) clazz.getClass().getMethod("getReflectedType").invoke(clazz);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException ex) {
            Exceptions.printStackTrace(ex);
            return ;
        }
        String className = classType.getName();

        MethodBreakpoint mb = MethodBreakpoint.create(className, "");
        mb.setHidden(true);
        mb.setSuspend(debugger.getSuspend());
        mb.setSession(debugger);
        mb.addJPDABreakpointListener(new InScriptBPListener(mb));
        DebuggerManager.getDebuggerManager().addBreakpoint(mb);
        logger.log(Level.FINE, "Created {0} for any method in {1}", new Object[]{mb, className});
    }
    
    private class ScriptBPListener implements JPDABreakpointListener {
        
        @Override
        public void breakpointReached(JPDABreakpointEvent event) {
            // Call MethodHandle mh = getGenericInvoker();
            // mh.member.clazz is the class that is going to be called
            // mh.member.name is the method name.
            logger.fine("ScriptBPListener.breakpointReached()");
            try {
                setAltCSF(event.getThread());
                Variable mh;
                if (event.getSource() == scriptAccessBPs[0]) {
                    mh = debugger.evaluate("getGenericInvoker()");
                } else {
                    mh = debugger.evaluate("getGenericConstructor()");
                }
                if (!(mh instanceof ObjectVariable)) {
                    logger.info("getGenericInvoker/Constructor returned "+mh+", which is not an object.");
                    return ;
                }
                scriptToBeInvoked((ObjectVariable) mh);
                
            } catch (InvalidExpressionException iex) {
                
            } finally {
                setAltCSF(null);
                event.resume();
            }
        }
        
        private void setAltCSF(JPDAThread thread) {
            try {
                StackFrame sf;
                if (thread != null) {
                    ThreadReference tr = (ThreadReference) thread.getClass().getMethod("getThreadReference").invoke(thread);
                    sf = tr.frame(0);
                } else {
                    sf = null;
                }
                debugger.getClass().getMethod("setAltCSF", StackFrame.class).invoke(debugger, sf);
            } catch (com.sun.jdi.IncompatibleThreadStateException e) {
            } catch (ObjectCollectedException e) {
            } catch (IllegalThreadStateException e) {
                // Let it go, the thread is dead.
            } catch (java.lang.IndexOutOfBoundsException e) {
                // No frame in case of Thread and "Main" class breakpoints, PATCH 56540
            } catch (VMDisconnectedException vmdex) {
            } catch (InternalException iex) {
            } catch (NoSuchMethodException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | SecurityException ex) {
                
            }
        }
        
    }
    
    private class ScriptInvokeBPListener implements JPDABreakpointListener {

        @Override
        public void breakpointReached(JPDABreakpointEvent event) {
            // We have the method handle as an argument
            logger.fine("ScriptInvokeBPListener.breakpointReached()");
            try {
                CallStackFrame frame;
                try {
                    CallStackFrame[] callStack = event.getThread().getCallStack(0, 1);
                    if (callStack.length > 0) {
                        frame = callStack[0];
                    } else {
                        return ;
                    }
                } catch (AbsentInformationException ex) {
                    // No info => no debugging
                    logger.log(Level.WARNING, "No debug info", ex);
                    return ;
                }
                ObjectVariable mh = null;
                try {
                    LocalVariable[] localVariables = frame.getLocalVariables();
                    for (LocalVariable lv : localVariables) {
                        if (SCRIPT_NOTIFY_INVOKE_METHOD_ARG.equals(lv.getName())) {
                            mh = (ObjectVariable) lv;
                        }
                    }
                } catch (AbsentInformationException aiex) {}
                if (mh == null) {
                    try {
                        LocalVariable[] methodArguments = (LocalVariable[]) frame.getClass().getMethod("getMethodArguments").invoke(frame);
                        if (methodArguments.length > 0) {
                            mh = (ObjectVariable) methodArguments[0];
                        }
                    } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException ex) {
                        logger.log(Level.CONFIG, "Obtaining method argumnets", ex);
                    }
                }
                if (mh != null) {
                    scriptToBeInvoked(mh);
                } else {
                    logger.info("Unable to retrieve the method handle");
                }
            } finally {
                event.resume();
            }
        }
        
    }
    
    private class InScriptBPListener implements JPDABreakpointListener {
        
        private MethodBreakpoint mb;
        
        InScriptBPListener(MethodBreakpoint mb) {
            this.mb = mb;
        }

        @Override
        public void breakpointReached(JPDABreakpointEvent event) {
            logger.log(Level.FINE, "InScriptBPListener.breakpointReached(), removing {0}", mb);
            mb.disable();
            mb.removeJPDABreakpointListener(this);
            DebuggerManager.getDebuggerManager().removeBreakpoint(mb);
            // We're in the script.
            // Disable any pending step requests:
            disableStepRequests(event.getThread());
        }

        private void disableStepRequests(JPDAThread thread) {
            ThreadReference tr;
            try {
                tr = (ThreadReference) thread.getClass().getMethod("getThreadReference").invoke(thread);
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException |
                     NoSuchMethodException | SecurityException ex) {
                Exceptions.printStackTrace(ex);
                return ;
            }
            try {
                VirtualMachine vm = tr.virtualMachine();
                if (vm == null) return;
                EventRequestManager erm = vm.eventRequestManager();
                List<StepRequest> l = erm.stepRequests();
                for (StepRequest stepRequest : l) {
                    if (stepRequest.thread().equals(tr)) {
                        try {
                            stepRequest.disable();
                        } catch (InvalidRequestStateException ex) {}
                    }
                }
            } catch (VMDisconnectedException | InternalException |
                     IllegalThreadStateException | InvalidRequestStateException e) {
            }
        }
        
    }
    
}
