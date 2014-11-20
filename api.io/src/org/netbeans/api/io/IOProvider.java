/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2014 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2014 Sun Microsystems, Inc.
 */
package org.netbeans.api.io;

import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Set;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.spi.io.InputOutputProvider;
import org.openide.util.Lookup;
import org.openide.util.Parameters;

/**
 * A factory for IO tabs shown in the output window. To create a new tab to
 * write to, call e.g.
 * <code>IOProvider.getDefault().getIO("MyTab", false)</code> (pass true if
 * there may be an existing tab with the same name and you want to write to a
 * new tab).
 *
 * <p>
 * Methods of this class can be called in any thread.
 * </p>
 *
 * @author Jesse Glick, Jaroslav Havlin
 */
public abstract class IOProvider {

    private IOProvider() {
    }

    /**
     * Get the default I/O provider.
     * <p>
     * Normally this is taken from {@link Lookup#getDefault} but if there is no
     * instance in lookup, a fallback instance is created which just uses the
     * standard system I/O streams. This is useful for unit tests and perhaps
     * for standalone usage of various libraries.
     * </p>
     *
     * @return The default instance (never null).
     */
    @NonNull
    public static IOProvider getDefault() {
        InputOutputProvider<?, ?, ?, ?> def
                = Lookup.getDefault().lookup(InputOutputProvider.class);
        if (def != null) {
            return wrapProvider(def);
        } else {
            def = getBridgingDefault();
            if (def != null) {
                return wrapProvider(def);
            } else {
                return wrapProvider(new Trivial());
            }
        }
    }

    private static <IO, OW extends PrintWriter, P, F> IOProvider wrapProvider(
            InputOutputProvider<IO, OW, P, F> provider) {

        return new Impl<IO, OW, P, F>(provider);
    }

    /**
     * Get default provider implementing openide.io SPI and bridge it to api.io
     * SPI.
     */
    private static InputOutputProvider<?,?,?,?> getBridgingDefault() {
        return getBridging("getDefault", //NOI18N
                new Class<?>[0], new Object[0]);
    }

    /**
     * Get provider implementing openide.io SPI and bridge it to api.io SPI.
     *
     * @param name Name of the provider.
     */
    private static InputOutputProvider<?,?,?,?> getBridging(String name) {
        return getBridging("get", //NOI18N
                new Class<?>[]{String.class},
                new Object[]{name});
    }

    /**
     * Invoke method on getter of providers implementing openide.io SPI bridged
     * to api.io SPI.
     *
     * @param methodName Method to invoke on the getter.
     * @param paramTypes Parameters types of the method.
     * @param params Arguments to pass to the method.
     *
     * @return IOProvider bridged to InputOutputProvider returned from specified
     * getter's method, or null if not available or some problem occured.
     */
    private static InputOutputProvider<?,?,?,?> getBridging(String methodName,
            Class<?>[] paramTypes, Object[] params) {

        String className = "org.openide.io.BridgingGetter";             //NOI18N
        ClassLoader cl = Lookup.getDefault().lookup(ClassLoader.class);
        if (cl != null) {
            try {
                Class<? extends Object> c = Class.forName(className, true, cl);
                Object instance = c.newInstance();
                Method m = c.getDeclaredMethod(methodName, paramTypes);
                Object result = m.invoke(instance, params);
                if (result instanceof InputOutputProvider) {
                    return (InputOutputProvider) result;
                }
            } catch (ClassNotFoundException ex) {
            } catch (NoSuchMethodException ex) {
            } catch (SecurityException ex) {
            } catch (InstantiationException ex) {
            } catch (IllegalAccessException ex) {
            } catch (IllegalArgumentException ex) {
            } catch (InvocationTargetException ex) {
            }
        }
        return null;
    }

    /**
     * Gets IOProvider of selected name or delegates to getDefault() if none was
     * found.
     *
     * @param id ID of provider.
     * @return The instance corresponding to provided name or default instance
     * if not found.
     */
    @NonNull
    public static IOProvider get(@NonNull String id) {
        Parameters.notNull("id", id);

        @SuppressWarnings("rawtypes")
        Collection<? extends InputOutputProvider> providers
                = Lookup.getDefault().lookupAll(InputOutputProvider.class);

        for (InputOutputProvider<?, ?, ?, ?> p : providers) {
            if (p.getId().equals(id)) {
                return wrapProvider(p);
            }
        }
        InputOutputProvider<?,?,?,?> bridgingImpl = getBridging(id);
        if (bridgingImpl != null) {
            return wrapProvider(bridgingImpl);
        } else {
            return getDefault();
        }
    }

    /**
     * Gets identifier of this provider.
     *
     * @return Name of this provider.
     */
    @NonNull
    public abstract String getId();

    /**
     * Get a named instance of InputOutput, which represents an output tab in
     * the output window. Streams for reading/writing can be accessed via
     * getters on the returned instance.
     *
     * @param name A localised display name for the tab.
     * @param newIO If <tt>true</tt>, a new <code>InputOutput</code> is
     * returned, else an existing <code>InputOutput</code> of the same name may
     * be returned.
     * @return An <code>InputOutput</code> instance for accessing the new tab.
     * @see InputOutput
     */
    @NonNull
    public abstract InputOutput getIO(@NonNull String name, boolean newIO);

    /**
     * Get a named instance of InputOutput, which represents an output tab in
     * the output window. Streams for reading/writing can be accessed via
     * getters on the returned instance.
     *
     * @param name A localised display name for the tab.
     * @param newIO If <tt>true</tt>, a new <code>InputOutput</code> is
     * returned, else an existing <code>InputOutput</code> of the same name may
     * be returned.
     * @param lookup Lookup which may contain additional information for various
     * implementations of output window.
     * @return An <code>InputOutput</code> instance for accessing the new tab.
     * @see InputOutput
     */
    @NonNull
    public abstract InputOutput getIO(@NonNull String name, boolean newIO,
            @NonNull Lookup lookup);

    /**
     * Implementation of IOProvider that uses {@link InputOutputProvider} SPI
     * internally.
     *
     * @param <IO>
     * @param <OW>
     * @param <POS>
     */
    private static class Impl<IO, OW extends PrintWriter, P, F>
            extends IOProvider {

        private final InputOutputProvider<IO, OW, P, F> impl;

        public Impl(InputOutputProvider<IO, OW, P, F> impl) {
            this.impl = impl;
        }

        @Override
        public String getId() {
            return impl.getId();
        }

        @Override
        public InputOutput getIO(String name, boolean newIO) {
            return getIO(name, newIO, Lookup.EMPTY);
        }

        @Override
        public InputOutput getIO(String name, boolean newIO, Lookup lookup) {
            Parameters.notNull("name", name);
            Parameters.notNull("lookup", lookup);
            return InputOutput.create(impl, impl.getIO(name, newIO, lookup));
        }
    }

    /**
     * Trivial implementation of {@link IOProvider} that uses system input,
     * output and error streams.
     */
    private static class Trivial
            implements InputOutputProvider<Object, PrintWriter, Void, Void> {

        @Override
        public String getId() {
            return "Trivial";
        }

        @Override
        public Object getIO(String name, boolean newIO, Lookup lookup) {
            return this;
        }

        @Override
        public Reader getIn(Object io) {
            return new InputStreamReader(System.in);
        }

        @Override
        public PrintWriter getOut(Object io) {
            return new PrintWriter(System.out);
        }

        @Override
        public PrintWriter getErr(Object io) {
            return new PrintWriter(System.err);
        }

        @Override
        public void print(Object io, PrintWriter writer, String text,
                Hyperlink link, OutputColor color, boolean printLineEnd) {
            writer.print(text);
            if (printLineEnd) {
                writer.println();
            }
        }

        @Override
        public Lookup getIOLookup(Object io) {
            return Lookup.EMPTY;
        }

        @Override
        public void resetIO(Object io) {
        }

        @Override
        public void showIO(Object io,
                Set<ShowOperation> operations) {
        }

        @Override
        public void closeIO(Object io) {
        }

        @Override
        public boolean isIOClosed(Object io) {
            return false;
        }

        @Override
        public Void getCurrentPosition(Object io, PrintWriter writer) {
            return null;
        }

        @Override
        public void scrollTo(Object io, PrintWriter writer, Void position) {
        }

        @Override
        public Void startFold(Object io, PrintWriter writer, boolean expanded) {
            return null;
        }

        @Override
        public void endFold(Object io, PrintWriter writer, Void fold) {
        }

        @Override
        public void setFoldExpanded(Object io, PrintWriter writer, Void fold,
                boolean expanded) {
        }

        @Override
        public String getIODescription(Object io) {
            return null;
        }

        @Override
        public void setIODescription(Object io, String description) {
        }
    }
}
