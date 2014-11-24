import com.android.ide.common.rendering.api.ResourceValue;
import com.android.ide.common.resources.configuration.FolderConfiguration;
import com.android.resources.ResourceType;
import com.android.tools.idea.AndroidPsiUtils;
import com.android.tools.idea.folding.ResourceFoldingBuilder;
import com.android.tools.idea.rendering.AppResourceRepository;
import com.android.tools.idea.rendering.LocalResourceRepository;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.ui.awt.RelativePoint;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;


/**
 * Created by kirankumar on 12/11/14.
 */
public class layoutEngineConvert extends AnAction {

    public static final String STRING_PREFIX = "@string/";
    public static final String DIMEN_PREFIX = "@dimen/";
    public static final String INTEGER_PREFIX = "@integer/";
    public static final String COLOR_PREFIX = "@color/";
    private static final boolean FORCE_PROJECT_RESOURCE_LOADING = true;
    private Set<String> skipAttributes = new HashSet<String>(Arrays.asList("xmlns:tools", "tools:context", "xmlns:tools","xmlns:android"));


    public void actionPerformed(AnActionEvent e) {
        // TODO: insert action logic here
        Project project = e.getProject();
        StatusBar statusBar = WindowManager.getInstance()
                .getStatusBar(DataKeys.PROJECT.getData(e.getDataContext()));
        final MessageType messageType= MessageType.ERROR;

        JBPopupFactory.getInstance()
                .createHtmlTextBalloonBuilder("Plugin works, woohoo", messageType, null)
                .setFadeoutTime(7500)
                .createBalloon()
                .show(RelativePoint.getCenterOf(statusBar.getComponent()),
                        Balloon.Position.atRight);
        //Document currentDoc = FileEditorManager.getInstance(project).getSelectedTextEditor().getDocument();
        //VirtualFile currentFile = FileDocumentManager.getInstance().getFile(currentDoc);

        VirtualFile currentFile = DataKeys.VIRTUAL_FILE.getData(e.getDataContext());
        //Editor editor = DataKeys.EDITOR.getData(e.getDataContext());


        XmlFile myXmlFile = (XmlFile) AndroidPsiUtils.getPsiFileSafely(e.getProject(), currentFile);


        XmlTag rootTag = myXmlFile.getRootTag();

        JsonObject object = recurse(rootTag);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(object);
        Document jsonDocument = EditorFactory.getInstance().createDocument(json);
        CodePreviewDialog dialog = new CodePreviewDialog(e.getProject(),jsonDocument);
        dialog.show();



    }

    private JsonObject recurse(XmlTag element) {
        if(element!=null)
        {

            JsonObject json = new JsonObject();
            XmlAttribute[] attributes = element.getAttributes();
            String elementName = element.getName();
            //System.out.println("++++ "+elementName+" ++++");
            json.addProperty("view",elementName);

            for (XmlAttribute attribute : attributes) {
                //String name = attribute.getName();
                if(skipAttributes.contains(attribute.getName()))
                {

                    continue;
                }
                String localName = attribute.getLocalName();
                String value = attribute.getValue();
                System.out.println(""+localName+" = "+value);


                ResourceFoldingBuilder resourceFoldingBuilder = new ResourceFoldingBuilder();

                LocalResourceRepository appResources = getAppResources(attribute.getValueElement());
                FolderConfiguration referenceConfig = new FolderConfiguration();


                ResourceType type = null;
                String key = null;
                if (value.startsWith(STRING_PREFIX)) {
                    key = value.substring(STRING_PREFIX.length());
                    type = ResourceType.STRING;
                } else if (value.startsWith(DIMEN_PREFIX)) {
                    key = value.substring(DIMEN_PREFIX.length());
                    type = ResourceType.DIMEN;
                } else if (value.startsWith(INTEGER_PREFIX)) {
                    key = value.substring(INTEGER_PREFIX.length());
                    type = ResourceType.INTEGER;
                }
                else if (value.startsWith(COLOR_PREFIX)) {
                    key = value.substring(COLOR_PREFIX.length());
                    type = ResourceType.COLOR;
                }

                if(type!=null && key!=null) {
                    ResourceValue configuredValue = appResources.getConfiguredValue(type, key, referenceConfig);
                    if (configuredValue != null) {
                        System.out.println("resolved string " + configuredValue.getValue());
                        value = configuredValue.getValue();
                    }
                }
                json.addProperty(localName,value);


            }
            XmlTag[] subTags = element.getSubTags();
            JsonArray children = new JsonArray();
            for (XmlTag subTag : subTags) {
                JsonObject child = recurse(subTag);
                children.add(child);
            }
            if(children.size()>0) {
                json.add("children", children);
            }

        return json;
        }
        return null;
    }

    private static LocalResourceRepository getAppResources(PsiElement element) {
        Module module = ModuleUtilCore.findModuleForPsiElement(element);
        if (module == null) {
            return null;
        }

        return AppResourceRepository.getAppResources(module, FORCE_PROJECT_RESOURCE_LOADING);
    }
}
