package org.netbeans.modules.dlight.terminal.action;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.netbeans.lib.terminalemulator.Term;
import org.netbeans.modules.nativeexecution.api.ExecutionEnvironment;
import org.netbeans.modules.nativeexecution.api.ExecutionEnvironmentFactory;
import org.netbeans.modules.nativeexecution.api.util.HostInfoUtils;
import org.netbeans.modules.terminal.spi.ExternalCommandActionProvider;
import org.netbeans.modules.terminal.support.OpenInEditorAction;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.filesystems.URLMapper;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author igromov
 */
@ServiceProvider(service = ExternalCommandActionProvider.class)
public class OpenInEditorActionProvider extends ExternalCommandActionProvider {

    private static final String IDE_OPEN = Term.ExternalCommandsConstants.IDE_OPEN;

    @Override
    public boolean canHandle(String command) {
        return command != null && command.trim().startsWith(IDE_OPEN);
    }

    @Override
    public void handle(String command, Lookup lookup) {

        command = command.substring(Term.ExternalCommandsConstants.IDE_OPEN.length() + 1).trim();

        List<String> paths = new ArrayList<String>();
        Matcher m = Pattern.compile("([^\"]\\S*|\".+?\")\\s*").matcher(command); //NOI18N
        while (m.find()) {
            paths.add(m.group(1));
        }

        for (String path : paths) {
            int lineNumber = -1;
            String filePath = path;

            int colonIdx = command.lastIndexOf(':');
            // Shortest file path
            if (colonIdx > 2) {
                try {
                    lineNumber = Integer.parseInt(command.substring(colonIdx + 1));
                    filePath = command.substring(0, colonIdx);
                } catch (NumberFormatException x) {
                }
            }

            if (!filePath.startsWith("/") && !filePath.startsWith("~")) { //NOI18N
                filePath = lookup.lookup(String.class) + "/" + filePath; //NOI18N
            }

            Object key = lookup.lookup(Term.class).getClientProperty(Term.ExternalCommandsConstants.EXECUTION_ENV_PROPERTY_KEY);
            FileObject fo = null;
            if (key != null && (key instanceof String)) {
                try {
                    ExecutionEnvironment fromUniqueID = ExecutionEnvironmentFactory.fromUniqueID((String) key);
                    if (fromUniqueID.isRemote()) {
                        fo = URLMapper.findFileObject(new URL("rfs://" + key + filePath)); //NOI18N
                    } else {
                        fo = FileUtil.toFileObject(new File(filePath));
                    }
                } catch (Exception ex) {
                    // ignore
                }
            }
            if (fo == null) {
                fo = FileUtil.toFileObject(new File(filePath));
            }

            // XXX blocker for a remote case is https://netbeans.org/bugzilla/show_bug.cgi?id=258890
            OpenInEditorAction.post(fo, lineNumber);
        }
    }

}
