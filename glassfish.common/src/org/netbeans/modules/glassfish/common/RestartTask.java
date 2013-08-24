/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.netbeans.modules.glassfish.common;

import java.util.Map;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.tools.ide.GlassFishIdeException;
import org.glassfish.tools.ide.GlassFishStatus;
import static org.glassfish.tools.ide.GlassFishStatus.OFFLINE;
import static org.glassfish.tools.ide.GlassFishStatus.ONLINE;
import static org.glassfish.tools.ide.GlassFishStatus.SHUTDOWN;
import static org.glassfish.tools.ide.GlassFishStatus.STARTUP;
import static org.glassfish.tools.ide.GlassFishStatus.UNKNOWN;
import org.glassfish.tools.ide.TaskEvent;
import org.glassfish.tools.ide.TaskState;
import org.glassfish.tools.ide.TaskStateListener;
import org.glassfish.tools.ide.admin.CommandGetProperty;
import org.glassfish.tools.ide.admin.CommandRestartDAS;
import org.glassfish.tools.ide.admin.CommandSetProperty;
import org.glassfish.tools.ide.admin.CommandStopDAS;
import org.glassfish.tools.ide.admin.ResultMap;
import org.glassfish.tools.ide.admin.ResultString;
import org.glassfish.tools.ide.data.GlassFishServerStatus;
import org.glassfish.tools.ide.utils.ServerUtils;
import static org.netbeans.modules.glassfish.common.BasicTask.START_TIMEOUT;
import static org.netbeans.modules.glassfish.common.BasicTask.STOP_TIMEOUT;
import static org.netbeans.modules.glassfish.common.BasicTask.TIMEUNIT;
import static org.netbeans.modules.glassfish.common.GlassFishState.getStatus;
import org.netbeans.modules.glassfish.spi.CommandFactory;
import org.netbeans.modules.glassfish.spi.GlassfishModule;
import org.netbeans.modules.glassfish.spi.GlassfishModule.ServerState;
import org.openide.util.NbBundle;

/**
 *
 * @author Peter Williams
 * @author Vince Kraemer
 */
public class RestartTask extends BasicTask<TaskState> {

    ////////////////////////////////////////////////////////////////////////////
    // Class attributes                                                       //
    ////////////////////////////////////////////////////////////////////////////

    /** Local logger. */
    private static final Logger LOGGER = GlassFishLogger.get(RestartTask.class);

    ////////////////////////////////////////////////////////////////////////////
    // Instance attributes                                                    //
    ////////////////////////////////////////////////////////////////////////////

    /** How long to wait after stopping server to let OS clean up resources. */
    @SuppressWarnings("FieldNameHidesFieldInSuperclass")
    private static final int RESTART_DELAY = 5000;

    /** Common support object for the server instance being restarted. */
    private final CommonServerSupport support;

    ////////////////////////////////////////////////////////////////////////////
    // Constructors                                                           //
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Constructs an instance of asynchronous GlassFish server restart command
     * execution support object.
     * <p/>
     * @param support       Common support object for the server instance being
     *                      restarted
     * @param stateListener State monitor to track restart progress.
     */
    public RestartTask(CommonServerSupport support, TaskStateListener... stateListener) {
        super(support.getInstance(), stateListener);
        this.support = support;
    }

    ////////////////////////////////////////////////////////////////////////////
    // Methods                                                                //
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Start local server that is offline.
     * <p/>
     * @return State change request about offline remote server start request.
     */
    private StateChange localOfflineStart() {
        Future<TaskState> startTask
                = support.startServer(null, ServerState.RUNNING);
        TaskState startResult = TaskState.FAILED;
        try {
            startResult = startTask.get(START_TIMEOUT, TIMEUNIT);
        } catch (Exception ex) {
            LOGGER.log(Level.FINER,
                    ex.getLocalizedMessage(), ex);
        }
        if (startResult == TaskState.FAILED) {
            return new StateChange(this,
                    TaskState.FAILED, TaskEvent.CMD_FAILED,
                    "RestartTask.localOfflineStart.failed", instanceName);
        }
        return new StateChange(this,
                TaskState.COMPLETED, TaskEvent.CMD_COMPLETED,
                "RestartTask.localOfflineStart.completed", instanceName);
    }

    /**
     * Start remote server that is offline.
     * <p/>
     * This operation is not possible and will always fail.
     * <p/>
     * @return State change request about offline remote server start request.
     */
    private StateChange remoteOfflineStart() {
            return new StateChange(this,
                    TaskState.FAILED, TaskEvent.ILLEGAL_STATE,
                    "RestartTask.remoteOfflineStart.failed", instanceName);
    }
   
    /**
     * Wait for local server currently shutting down and start it up.
     * <p/>
     * @return State change request about local server (that is shutting down)
     *         start request.
     */
    private StateChange localShutdownStart() {
        StateChange stateChange = waitShutDown();
        if (stateChange != null) {
            return stateChange;
        }
        GlassFishServerStatus status = getStatus(instance);
        switch(status.getStatus()) {
            case UNKNOWN: case ONLINE: case SHUTDOWN: case STARTUP:
                return new StateChange(this,
                    TaskState.FAILED, TaskEvent.ILLEGAL_STATE,
                    "RestartTask.localShutdownStart.notOffline",
                    instanceName);
            default:
                if (!ServerUtils.isDASRunning(instance)) {
                    return localOfflineStart();
                } else {
                return new StateChange(this,
                    TaskState.FAILED, TaskEvent.ILLEGAL_STATE,
                    "RestartTask.localShutdownStart.portOccupied",
                    instanceName);                    
                }
        }
    }

    /**
     * Wait for remote server currently shutting down and start it up.
     * <p/>
     * This operation is not possible and will always fail.
     * <p/>
     * @return State change request about remote server (that is shutting down)
     *         start request.
     */
    private StateChange remoteShutdownStart() {
            return new StateChange(this,
                    TaskState.FAILED, TaskEvent.ILLEGAL_STATE,
                    "RestartTask.remoteShutdownStart.failed", instanceName);        
    }

    /**
     * Wait for server to start up.
     * <p/>
     * @return State change request.
     */
    private StateChange startupWait() {
        StartStateListener listener = prepareStartMonitoring(true);
        if (listener == null) {
            return new StateChange(this,
                    TaskState.FAILED, TaskEvent.ILLEGAL_STATE,
                    "RestartTask.startupWait.listenerError",
                    instanceName);
        }
        long start = System.currentTimeMillis();
        try {
            synchronized(listener) {
                while (!listener.isWakeUp()
                        && (System.currentTimeMillis()
                        - start < START_TIMEOUT)) {
                    listener.wait(System.currentTimeMillis() - start);
                }
            }
        } catch (InterruptedException ie) {
            LOGGER.log(Level.INFO, NbBundle.getMessage(RestartTask.class,
                    "RestartTask.startupWait.interruptedException",
                    new String[] {
                        instance.getName(), ie.getLocalizedMessage()}));
            
        } finally {
            GlassFishStatus.removeListener(instance, listener);
        }
        if (GlassFishState.isOnline(instance)) {
              return new StateChange(this,
                      TaskState.COMPLETED, TaskEvent.CMD_COMPLETED,
                      "RestartTask.startupWait.completed", instanceName);
        } else {
              return new StateChange(this,
                      TaskState.FAILED, TaskEvent.ILLEGAL_STATE,
                      "RestartTask.startupWait.failed", instanceName);
        }
    }

    /**
     * Full restart of local online server.
     * <p/>
     * @return State change request.
     */
    private StateChange localRestart() {
        if (GlassFishStatus.shutdown(instance)) {
            ResultString result = CommandStopDAS.stopDAS(instance);
            if (result.getState() == TaskState.COMPLETED) {
                return localShutdownStart();
            } else {
                // TODO: Reset server status monitoring
                return new StateChange(this,
                        TaskState.FAILED, TaskEvent.CMD_FAILED,
                        "RestartTask.localRestart.cmdFailed", instanceName);
            }
        } else {
            return new StateChange(this,
                    TaskState.FAILED, TaskEvent.ILLEGAL_STATE,
                    "RestartTask.localRestart.failed", instanceName);
        }
    }

    /**
     * Update server debug options before restart.
     */
    private boolean updateDebugOptions(final int debugPort) {
        boolean updateResult = false;
        try {
            ResultMap<String, String> result
                    = CommandGetProperty.getProperties(instance,
                    "configs.config.server-config.java-config.debug-options");
            if (result.getState() == TaskState.COMPLETED) {
                Map<String, String> values = result.getValue();
                if (values != null && !values.isEmpty()) {
                    CommandFactory commandFactory =
                            instance.getInstanceProvider().getCommandFactory();
                    String oldValue = values.get(
                            "configs.config.server-config.java-config.debug-options");
                    CommandSetProperty setCmd =
                            commandFactory.getSetPropertyCommand(
                            "configs.config.server-config.java-config.debug-options",
                            oldValue.replace("transport=dt_shmem", "transport=dt_socket").
                            replace("address=[^,]+", "address=" + debugPort));
                    try {
                        CommandSetProperty.setProperty(instance, setCmd);
                        updateResult = true;
                    } catch (GlassFishIdeException gfie) {
                        LOGGER.log(Level.INFO, debugPort + "", gfie);
                    }
                }
            }
        } catch (GlassFishIdeException gfie) {
            LOGGER.log(Level.INFO,
                    "Could not retrieve property from server.", gfie);
        }
        return updateResult;
    }

    /**
     * Wait for debug port to become active.
     * <p/>
     * @return Value of <code>true</code> if port become active before timeout
     *         or <code>false</code> otherwise.
     */
    @SuppressWarnings("SleepWhileInLoop")
    private boolean vaitForDebugPort(final String host, final int port) {
        boolean result = ServerUtils.isRunningRemote(host, port);
        if (!result) {
            long tmStart = System.currentTimeMillis();
            while (!result
                    && System.currentTimeMillis() - tmStart
                    < START_ADMIN_PORT_TIMEOUT) {
                try {
                    Thread.sleep(PORT_CHECK_IDLE);
                } catch (InterruptedException ex) {}
                result = ServerUtils.isRunningRemote(host, port);
            }
        }
        return result;
    }

    /**
     * Full restart of remote online server.
     * <p/>
     * @return State change request.
     */
    private StateChange remoteRestart() {
        boolean debugMode = instance.getJvmMode() == GlassFishJvmMode.DEBUG;
        // Wrong scenario as default.
        boolean debugPortActive = true;
        int debugPort = -1;
        if (debugMode) {
            debugPort = instance.getDebugPort();
            debugMode = updateDebugOptions(debugPort);
            debugPortActive = ServerUtils.isRunningRemote(
                    instance.getHost(), debugPort);
        }
        ResultString result
                = CommandRestartDAS.restartDAS(instance, debugMode);
        LogViewMgr.removeLog(instance);
        LogViewMgr logger = LogViewMgr.getInstance(
                instance.getProperty(GlassfishModule.URL_ATTR));
        logger.stopReaders();                
        switch (result.getState()) {
            case COMPLETED:
                if (debugMode && !debugPortActive) {
                    vaitForDebugPort(instance.getHost(), debugPort);
                    waitStartUp(true, false);
// This probably won't be needed.
//                } else {
//                    try {
//                        Thread.sleep(RESTART_DELAY);
//                    } catch (InterruptedException ex) {}
                }
                return new StateChange(this,
                        result.getState(), TaskEvent.CMD_COMPLETED,
                        "RestartTask.remoteRestart.completed", instanceName);
            default:
                return new StateChange(this,
                        result.getState(), TaskEvent.CMD_COMPLETED,
                        "RestartTask.remoteRestart.failed", new String[] {
                            instanceName, result.getValue()});
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    // ExecutorService call() Method                                          //
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Restart GlassFish server.
     * <p/>
     * Possible states are <code>UNKNOWN</code>, <code>OFFLINE</code>,
     * <code>STARTUP</code>, <code>ONLINE</code> and <code>SHUTDOWN</code>:
     * <p/>
     * <code>UNKNOWN</code>:  Do nothing. UI shall not allow restarting while
     *                        server status is unknown.
     * <code>OFFLINE</code>:  Server is already offline, let's start it
     *                        if administrator port is not occupied.
     * <code>STARTUP</code>:  We are already in the middle of startup process.
     *                        Let's just wait for sever to start.
     * <code>ONLINE</code>:   Full restart is needed.
     * <code>SHUTDOWN</code>: Shutdown process has already started, let's wait
     *                        for it to finish. Server will be started after
     *                        that.
     */
    @Override
    public TaskState call() {
        GlassFishStatus state = GlassFishState.getStatus(instance).getStatus();
        StateChange change;
        switch (state) {
            case UNKNOWN:
                return fireOperationStateChanged(
                        TaskState.FAILED, TaskEvent.ILLEGAL_STATE,
                        "RestartTask.call.unknownState", instanceName);
            case OFFLINE:
                change = instance.isRemote()
                        ? remoteOfflineStart() : localOfflineStart();
                return change.fireOperationStateChanged();
            case STARTUP:
                change = startupWait();
                return change.fireOperationStateChanged();
            case ONLINE:
                change = instance.isRemote()
                        ? remoteRestart() : localRestart();
                return change.fireOperationStateChanged();
            case SHUTDOWN:
                change = instance.isRemote()
                        ? remoteShutdownStart() : localShutdownStart();
                return change.fireOperationStateChanged();
            // This shall be unrechable, all states should have
            // own case handlers.
            default:
                return fireOperationStateChanged(
                        TaskState.FAILED, TaskEvent.ILLEGAL_STATE,
                        "RestartTask.call.unknownState", instanceName);                
        }
    }

    /**
     * Restart operation:
     *
     * RUNNING -> stop server
     *            start server
     *
     * STARTING -> wait for state == STOPPED or RUNNING.
     *
     * STOPPED -> start server
     *
     * STOPPING -> wait for state == STOPPED
     *             start server
     *
     * For all of the above, command succeeds if state == RUNNING at the end.
     * 
     */
    @SuppressWarnings("SleepWhileInLoop")
//    @Override
    public TaskState call2() {
        Logger.getLogger("glassfish").log(Level.FINEST,
                "RestartTask.call() called on thread \"{0}\"",
                Thread.currentThread().getName());
        fireOperationStateChanged(TaskState.RUNNING, TaskEvent.CMD_RUNNING,
                "MSG_RESTART_SERVER_IN_PROGRESS", instanceName);

        //ServerState state = support.getServerState();
        GlassFishStatus state = GlassFishState.getStatus(instance).getStatus();

        if (state == GlassFishStatus.STARTUP) {
            // wait for start to finish, we are done.
            GlassFishStatus currentState = state;
            int steps = (START_TIMEOUT / DELAY);
            int count = 0;
            while (currentState == GlassFishStatus.STARTUP && count++ < steps) {
                try {
                    Thread.sleep(DELAY);
                } catch (InterruptedException ex) {
                    Logger.getLogger("glassfish").log(Level.FINER,
                            ex.getLocalizedMessage(), ex);
                }
                currentState = GlassFishState.getStatus(instance).getStatus();
            }

            if (!GlassFishState.isOnline(instance)) {
                return fireOperationStateChanged(TaskState.FAILED,
                        TaskEvent.CMD_FAILED,
                        "MSG_RESTART_SERVER_FAILED_WONT_START", instanceName);
            }
        } else {
            boolean postStopDelay = true;
            if (state == GlassFishStatus.ONLINE) {
                    Future<TaskState> stopTask = support.stopServer(null);
                    TaskState stopResult = TaskState.FAILED;
                    try {
                        stopResult = stopTask.get(STOP_TIMEOUT, TIMEUNIT);
                    } catch (Exception ex) {
                        Logger.getLogger("glassfish").log(Level.FINER,
                                ex.getLocalizedMessage(), ex);
                    }

                    if (stopResult == TaskState.FAILED) {
                        return fireOperationStateChanged(TaskState.FAILED,
                                TaskEvent.CMD_FAILED,
                                "MSG_RESTART_SERVER_FAILED_WONT_STOP",
                                instanceName);
                    }
            } else if (state == GlassFishStatus.SHUTDOWN) {
                // wait for server to stop.
                GlassFishStatus currentState = state;
                int steps = (STOP_TIMEOUT / DELAY);
                int count = 0;
                while (currentState == GlassFishStatus.SHUTDOWN && count++ < steps) {
                    try {
                        Thread.sleep(DELAY);
                    } catch (InterruptedException ex) {
                        Logger.getLogger("glassfish").log(Level.FINER,
                                ex.getLocalizedMessage(), ex);
                    }
                    currentState = GlassFishState.getStatus(instance).getStatus();
                }

                if (!GlassFishState.isOffline(instance)) {
                    return fireOperationStateChanged(TaskState.FAILED,
                            TaskEvent.CMD_FAILED,
                            "MSG_RESTART_SERVER_FAILED_WONT_STOP",
                            instanceName);
                }
            } else {
                postStopDelay = false;
            }
            
            if (postStopDelay) {
                // If we stopped the server (or it was already stopping), delay
                // start for a few seconds to let system clean up ports.
                support.setServerState(ServerState.STARTING);
                try {
                    Thread.sleep(RESTART_DELAY);
                } catch (InterruptedException ex) {
                    // ignore
                }
            }

            // Server should be stopped. Start it.
            Object o = support.setEnvironmentProperty(
                    GlassfishModule.JVM_MODE,
                    GlassfishModule.NORMAL_MODE, false);
            if (GlassfishModule.PROFILE_MODE.equals(o)) {
                support.setEnvironmentProperty(GlassfishModule.JVM_MODE,
                        GlassfishModule.NORMAL_MODE, false);
            }
            Future<TaskState> startTask = support.startServer(null, ServerState.RUNNING);
            TaskState startResult = TaskState.FAILED;
            try {
                startResult = startTask.get(START_TIMEOUT, TIMEUNIT);
            } catch (Exception ex) {
                Logger.getLogger("glassfish").log(Level.FINER,
                        ex.getLocalizedMessage(), ex); // NOI18N
            }
            
            if (startResult == TaskState.FAILED) {
                return fireOperationStateChanged(TaskState.FAILED,
                        TaskEvent.CMD_FAILED,
                        "MSG_RESTART_SERVER_FAILED_WONT_START",
                        instanceName);
            }
            
            if (!support.isRemote()
                    && support.getServerState() != ServerState.RUNNING) {
                return fireOperationStateChanged(TaskState.FAILED,
                        TaskEvent.CMD_FAILED,
                        "MSG_RESTART_SERVER_FAILED_REASON_UNKNOWN",
                        instanceName);
            }
        }
        
        return fireOperationStateChanged(TaskState.COMPLETED,
                TaskEvent.CMD_COMPLETED,
                "MSG_SERVER_RESTARTED", instanceName);
    }
}
