/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.netbeans.modules.javawebstart.anttasks;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

/**
 *
 * @author Tomas Zezula
 */
public class CopyTemplatePageTask extends Task {

    private static final String JNLP_FILE = "${JNLP.FILE}"; //NOI18N
    private static final String JNLP_APPLET_WIDTH = "${JNLP.APPLET.WIDTH}"; //NOI18N
    private static final String JNLP_APPLET_HEIGHT = "${JNLP.APPLET.HEIGHT}";   //NOI18N
    private static final String JNLP_RESOURCES_MAIN_JAR="${JNLP.RESOURCES.MAIN.JAR}";   //NOI18N
    private static final String JNLP_APPLET_CLASS="${JNLP.APPLET.CLASS}";   //NOI18N
    private static final String JNLP_VM_VERSION="${JNLP_VM_VERSION}";   //NOI18N
    private static final String JNLP_APPLET_PARAMS = "${JNLP.APPLET.PARAMS}";   //NOI18N

    //todo: Use ANTLR SringTemplateGroup
    private final Map<String,Callable<String>> toReplace = new HashMap<String, Callable<String>>() {
        {
            put (JNLP_FILE, new BaseNamePropertyValue("jnlp.file", "launch.jnlp")); //NOI18N
            put (JNLP_APPLET_WIDTH, new PropertyValue("jnlp.applet.width","300"));//NOI18N
            put (JNLP_APPLET_HEIGHT, new PropertyValue("jnlp.applet.height","300"));//NOI18N
            put (JNLP_RESOURCES_MAIN_JAR, new BaseNamePropertyValue("dist.jar", ""));   //NOI18N
            put (JNLP_APPLET_CLASS, new PropertyValue("jnlp.applet.class", ""));//NOI18N
            put (JNLP_VM_VERSION, new PropertyValue("javac.target", "1.6"));    //NOI18N
            put (JNLP_VM_VERSION, new PropertyValue("javac.target", "1.6"));    //NOI18N
            put (JNLP_APPLET_PARAMS, new PropertyMap("jnlp.applet.param","name","value",
                    new HashMap<String, String>() {
                        {
                            put("jnlp_href","jnlp.file");   //NOI18N
                        }
            }));   //NOI18N
        }
    };

    private File destFile;
    private File destDir;
    private File template;

    public void setDestfile(File file) {
        this.destFile = file;
    }

    public void setDestDir(File dir) {
        this.destDir = dir;
    }

    public void setTemplate(File file) {
        this.template = file;
    }

        @Override
    public void execute() throws BuildException {
        checkParameters();
        try {
            final BufferedReader in = new BufferedReader(new FileReader(template)); //todo: encoding
            try {
                final PrintWriter out = new PrintWriter (new FileWriter(destFile)); //todo: encoding
                try {
                    copy (in,out);
                } finally {
                    out.close();
                }
            } finally {
                in.close();
            }
        } catch (IOException ioe) {
            throw new BuildException(ioe, getLocation());
        }
    }

    private void checkParameters() {
        if (destFile == null) {
            throw new BuildException("Destination file is not set, template page cannot be copied.");
        }
        if (destDir == null) {
            throw new BuildException("Destination directory is not set, template page cannot be copied.");
        }
        if (template == null) {
            throw new BuildException("Template file is not set, template page cannot be copied.");
        }
    }

    private void copy(final BufferedReader in, final PrintWriter out) throws IOException {
        String line;
        while ((line=in.readLine())!=null) {
            out.println(map(line));
        }
    }

    private String getProperty(String propName, String defaultVal) {
        String propVal = getProject().getProperty(propName);
        if (propVal == null) {
            log("Property " + propName + " is not defined, using default value: " + defaultVal, Project.MSG_VERBOSE);
            return defaultVal;
        }
        return propVal.trim();
    }

    private String map(String line) {
        for (Map.Entry<String,Callable<String>> mapper : toReplace.entrySet()) {
            try {
                if (line.contains(mapper.getKey())) {
                    line = line.replaceAll(Pattern.quote(mapper.getKey()), mapper.getValue().call());
                }
            } catch (Exception ex) {
                throw new BuildException(ex, getLocation());
            }
        }
        return line;
    }

    private static String stripFilename(String path) {
        int sepIndex = path.lastIndexOf('/') == -1 ? path.lastIndexOf('\\') : path.lastIndexOf('/');
        return  path.substring(sepIndex + 1);
    }

    private class PropertyValue implements Callable<String> {
        private final String propName;
        private final String defaultValue;

        public PropertyValue(final String propName, final String defaultValue) {
            this.propName = propName;
            this.defaultValue = defaultValue;
        }

        public String call() throws Exception {
            return getProperty(propName, defaultValue);
        }
    }

    private class BaseNamePropertyValue extends PropertyValue {

        public BaseNamePropertyValue(final String propName, final String defaultValue) {
            super(propName, defaultValue);
        }

        public String call() throws Exception {
            return stripFilename(super.call());
        }
    }

    private class PropertyMap implements Callable<String> {

        private final String prefix;
        private final String key;
        private final String value;
        private final Map<String,String> additionalProps;

        public PropertyMap(final String prefix, final String key, final String value,
                final Map<String,String> additionalProps) {
            this.prefix = prefix;
            this.key = key;
            this.value = value;
            this.additionalProps = additionalProps;
        }

        public String call() throws Exception {
            final StringBuilder sb = new StringBuilder();
            final Pattern p = Pattern.compile(Pattern.quote(prefix) + "\\.(\\d+)\\." + Pattern.quote(key)); //NOI18N

            final Hashtable<String,String> props = getProject().getProperties();
            for (Map.Entry<String,String> entry : props.entrySet()) {
                Matcher m = p.matcher(entry.getKey());
                if (m.matches()) {
                    final String kv = entry.getValue();
                    final String valueKey = prefix + '.'+ m.group(1) +'.' + value;  //NOI18N
                    final String vv = getProperty(valueKey, ""); //NOI18N
                    addProperty(sb,kv,vv);
                }
            }
            for (Map.Entry<String,String> prop : additionalProps.entrySet()) {
                addProperty(sb, prop.getKey(), getProperty(prop.getValue(), "")); //NOI18N
            }
            return sb.toString();
        }

        private void addProperty(final StringBuilder sb, final String kv, final String vv) {
            if (sb.length() > 0) {
                sb.append(", ");    //NOI18N
            }
            sb.append(kv);
            sb.append(':'); //NOI18N
            sb.append('"');
            sb.append(vv);
            sb.append('"'); //NOI18N
        }

    }
}
