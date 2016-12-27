package action;

import com.android.tools.idea.AndroidPsiUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.ui.awt.RelativePoint;
import org.apache.commons.lang.StringEscapeUtils;
import ui.CodePreviewDialog;

import javax.annotation.Nullable;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;


/**
 * @author kirankumar
 * @author adityasharat
 */
public class ConvertToJsonAction extends AnAction {

    //public static final String STRING_PREFIX = "@string/";
    //public static final String DIMEN_PREFIX = "@dimen/";
    public static final String INTEGER_PREFIX = "@integer/";
    //public static final String COLOR_PREFIX = "@color/";
    //public static final String STYLE_PREFIX = "@style/";
    //public static final String DRAWABLE_PREFIX = "@drawable/";
    public static final String ID_NEW_PREFIX = "@+id/";
    public static final String ID_PREFIX = "@id/";

    private Set<String> attributesToSkip = new HashSet<String>(Arrays.asList("xmlns:tools", "tools:context", "xmlns:tools", "xmlns:android", "style"));

    public void actionPerformed(AnActionEvent e) {
        try {
            if (e.getProject() == null) {
                error(ERROR_PROJECT_LOAD_NOT_COMPLETE, null, e);
                return;
            }

            VirtualFile currentFile = DataKeys.VIRTUAL_FILE.getData(e.getDataContext());
            if (currentFile == null) {
                error(ERROR_CURRENT_FILE_IS_NULL, null, e);
                return;
            }

            XmlFile xmlFile = (XmlFile) AndroidPsiUtils.getPsiFileSafely(e.getProject(), currentFile);

            if (null == xmlFile) {
                error(ERROR_FILE_READ, null, e);
                return;
            }

            // get the root tag of the xml file
            XmlTag rootTag = xmlFile.getRootTag();

            // convert to json
            JsonObject object = convert(rootTag);

            // convert to pretty json string
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String json = gson.toJson(object);

            // display json layout on a dialog
            Document document = EditorFactory.getInstance().createDocument(json);
            CodePreviewDialog dialog = new CodePreviewDialog(e.getProject(), document);
            dialog.show();
        } catch (Exception ex) {
            error(ERROR_EXCEPTION, ex, e);
        }
    }

    private JsonObject convert(XmlTag element) {
        if (element == null) {
            return null;
        }

        JsonObject json = new JsonObject();
        List<Pair<String, String>> attributes = new ArrayList<Pair<String, String>>();

        for (XmlAttribute attribute : element.getAttributes()) {
            attributes.add(Pair.create(attribute.getLocalName(), attribute.getValue()));
        }

        // get the tag name
        String tag = element.getName();
        // add tag name as type
        json.addProperty("type", StringEscapeUtils.unescapeXml(tag));

        for (Pair<String, String> attribute : attributes) {
            if (attributesToSkip.contains(attribute.first)) {
                continue;
            }
            String name = StringEscapeUtils.unescapeXml(attribute.first);
            String value = StringEscapeUtils.unescapeXml(attribute.second);

            if (value.startsWith(INTEGER_PREFIX)) {
                value = value.substring(INTEGER_PREFIX.length());
            } else if (value.startsWith(ID_PREFIX)) {
                value = value.substring(ID_PREFIX.length());
            } else if (value.startsWith(ID_NEW_PREFIX)) {
                value = value.substring(ID_NEW_PREFIX.length());
            }

            json.addProperty(name, value);
        }

        XmlTag[] subTags = element.getSubTags();
        JsonArray children = new JsonArray();
        for (XmlTag subTag : subTags) {
            JsonObject child = convert(subTag);
            children.add(child);
        }

        if (children.size() > 0) {
            json.add("children", children);
        }

        return json;
    }

    private static final int ERROR_PROJECT_LOAD_NOT_COMPLETE = 0;
    private static final int ERROR_CURRENT_FILE_IS_NULL = 1;
    private static final int ERROR_FILE_READ = 2;
    private static final int ERROR_EXCEPTION = 3;

    private void error(int error, @Nullable Throwable ex, AnActionEvent e) {

        StringBuilder builder = new StringBuilder();
        String message = null;

        switch (error) {
            case ERROR_PROJECT_LOAD_NOT_COMPLETE:
                message = "The project has not loaded. Please wait and try again.\n";
                break;
            case ERROR_CURRENT_FILE_IS_NULL:
                message = "Please open an XML file.\n";
                break;
            case ERROR_FILE_READ:
                message = "Could not read the XML file.\n";
                break;
            case ERROR_EXCEPTION:
                message = "EXCEPTION\n";
        }

        if (null != message) {
            builder.append(message);
        }

        StatusBar statusBar = WindowManager.getInstance().getStatusBar(DataKeys.PROJECT.getData(e.getDataContext()));

        if (null != ex) {
            ex.printStackTrace();
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw);
            builder.append(sw);
        }

        JBPopupFactory.getInstance()
                .createHtmlTextBalloonBuilder(builder.toString(), MessageType.ERROR, null)
                .setFadeoutTime(7500)
                .createBalloon()
                .show(RelativePoint.getCenterOf(statusBar.getComponent()), Balloon.Position.atRight);

    }
}
