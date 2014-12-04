package action;

import com.android.ide.common.rendering.api.ItemResourceValue;
import com.android.ide.common.rendering.api.ResourceValue;
import com.android.ide.common.rendering.api.StyleResourceValue;
import com.android.ide.common.resources.configuration.DensityQualifier;
import com.android.ide.common.resources.configuration.FolderConfiguration;
import com.android.resources.Density;
import com.android.resources.ResourceType;
import com.android.tools.idea.AndroidPsiUtils;
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
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.xml.XmlAttributeImpl;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.ui.awt.RelativePoint;
import com.sun.deploy.xml.XMLAttribute;
import components.Drawable;
import api.UploadResponse;
import org.apache.commons.lang.StringEscapeUtils;
import org.jetbrains.generate.tostring.util.StringUtil;
import ui.CodePreviewDialog;
import api.Requests;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;


/**
 * Created by kirankumar on 12/11/14.
 */
public class ConvertToJsonAction extends AnAction {

    public static final String STRING_PREFIX = "@string/";
    public static final String DIMEN_PREFIX = "@dimen/";
    public static final String INTEGER_PREFIX = "@integer/";
    public static final String COLOR_PREFIX = "@color/";
    public static final String STYLE_PREFIX = "@style/";
    private static final boolean FORCE_PROJECT_RESOURCE_LOADING = true;
    private static final String DRAWABLE_PREFIX = "@drawable/";
    private Set<String> skipAttributes = new HashSet<String>(Arrays.asList("xmlns:tools", "tools:context", "xmlns:tools", "xmlns:android","style"));
    private List<Drawable> remoteDrawables = new ArrayList<Drawable>();
    private Project project;

    public void actionPerformed(AnActionEvent e) {


        try {
            VirtualFile currentFile = DataKeys.VIRTUAL_FILE.getData(e.getDataContext());
            this.project = e.getProject();
            XmlFile myXmlFile = (XmlFile) AndroidPsiUtils.getPsiFileSafely(e.getProject(), currentFile);

            XmlTag rootTag = myXmlFile.getRootTag();


            LocalResourceRepository appResources = getAppResources(rootTag);
            FolderConfiguration referenceConfig = new FolderConfiguration();

            referenceConfig.createDefault();
            referenceConfig.setDensityQualifier(new DensityQualifier(Density.MEDIUM));



            JsonObject object = recurse(rootTag,appResources,referenceConfig);
            if(remoteDrawables.size()>0)
            {
                processRemoteDrawables();
                object = recurse(rootTag,appResources,referenceConfig);
            }
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String json = gson.toJson(object);
            Document jsonDocument = EditorFactory.getInstance().createDocument(json);
            CodePreviewDialog dialog = new CodePreviewDialog(e.getProject(), jsonDocument);
            dialog.show();
        } catch (Exception ex) {
            StatusBar statusBar = WindowManager.getInstance()
                    .getStatusBar(DataKeys.PROJECT.getData(e.getDataContext()));
            final MessageType messageType = MessageType.ERROR;
            ex.printStackTrace();
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw);
            sw.toString();
            JBPopupFactory.getInstance()
                    .createHtmlTextBalloonBuilder("Make sure that a XML layout file is open and also that gradle is synced. Error "+sw, messageType, null)
                    .setFadeoutTime(7500)
                    .createBalloon()
                    .show(RelativePoint.getCenterOf(statusBar.getComponent()),
                            Balloon.Position.atRight);
        }


    }

    private void processRemoteDrawables() throws IOException {

        if (remoteDrawables.size() > 0) {

            remoteDrawables = new ArrayList<Drawable>(new LinkedHashSet<Drawable>(remoteDrawables)); //remove duplicates

            UploadResponse preUploadResponse = Requests.makePreUploadRequest(remoteDrawables);
            if (preUploadResponse != null) {
                    List<Drawable> resourceListFromServer = preUploadResponse.getResources();
                    List<Drawable> requiringUpload = new ArrayList<Drawable>();
                    for (Drawable resource : resourceListFromServer) {
                        if (resource.getRemotePath() == null || StringUtil.isEmpty(resource.getRemotePath())) {

                            if (remoteDrawables.contains(resource)) {
                                int indexOf = remoteDrawables.indexOf(resource);
                                if (indexOf >= 0) {
                                    Drawable drawable = remoteDrawables.get(indexOf);
                                    //populate file path as list from server doesnt contain it
                                    resource.setFilePath(drawable.getFilePath());
                                }
                            }

                            //populate list of files to be uploaded.
                            requiringUpload.add(resource);

                        } else {
                            int indexOf = remoteDrawables.indexOf(resource);
                            if (indexOf >= 0) {
                                Drawable drawable = remoteDrawables.get(indexOf);
                                drawable.setRemotePath(resource.getRemotePath());
                            }
                        }
                    }


                    if (requiringUpload.size() > 0) {
                        UploadResponse uploadResponse = Requests.makeUploadRequest(requiringUpload);
                        if (uploadResponse != null) {
                            List<Drawable> resources = uploadResponse.getResources();
                            for (Drawable resource : resources) {
                                int indexOf = remoteDrawables.indexOf(resource);
                                if (indexOf >= 0) {
                                    Drawable drawable = remoteDrawables.get(indexOf);
                                    drawable.setRemotePath(resource.getRemotePath());
                                }
                            }

                        }
                    }

            }


        }


    }

    private JsonObject recurse(XmlTag element, LocalResourceRepository appResources, FolderConfiguration referenceConfig) {
        if (element != null) {

            JsonObject json = new JsonObject();
            XmlAttribute[] attributesTemp = element.getAttributes();
            List<Pair<String,String>> attributes = new ArrayList<Pair<String, String>>();
            for (XmlAttribute xmlAttribute : attributesTemp) {

                attributes.add(new Pair<String,String>(xmlAttribute.getLocalName(),xmlAttribute.getValue()));
            }

            String elementName = element.getName();

            json.addProperty("type", StringEscapeUtils.unescapeXml(elementName));



            XmlAttribute style = element.getAttribute("style");
            if(style!=null)
            {
                String styleValue = style.getValue();
                String key = styleValue.substring(STYLE_PREFIX.length());
                while(key!=null) {
                    StyleResourceValue configuredValue = (StyleResourceValue) appResources.getConfiguredValue(ResourceType.STYLE, key, referenceConfig);
                    if (configuredValue != null) {

                        List<String> names = configuredValue.getNames();
                        for (String name : names) {
                            ResourceValue value = configuredValue.findValue(name, true);
                            if(value!=null) {
                                System.out.println("style = " + name + " : " + value.getValue());

                                attributes.add(0, new Pair<String, String>(name, value.getValue()));
                            }
                        }
                        key = configuredValue.getParentStyle();
                        System.out.println("style = " + configuredValue.getValue());
                    }
                    else
                    {
                        key = null;
                    }

                }
            }


            for (Pair<String,String> attribute : attributes) {
                if (skipAttributes.contains(attribute.first)) {

                    continue;
                }
                String localName = attribute.first;
                String value = attribute.second;


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
                } else if (value.startsWith(COLOR_PREFIX)) {
                    key = value.substring(COLOR_PREFIX.length());
                    type = ResourceType.COLOR;
                } else if (value.startsWith(DRAWABLE_PREFIX)) {
                    key = value.substring(DRAWABLE_PREFIX.length());
                    type = ResourceType.DRAWABLE;
                }

                if (type != null && key != null) {
                    ResourceValue configuredValue = appResources.getConfiguredValue(type, key, referenceConfig);
                    if (configuredValue != null) {
                        System.out.println("resolved string " + configuredValue.getValue());
                        value = configuredValue.getValue();
                    }
                    if (type == ResourceType.DRAWABLE) if (value.endsWith(".xml")) {
                        VirtualFile resourceFile = VfsUtil.findFileByIoFile(new File(configuredValue.getValue()), false);
                        XmlFile myXmlFile = (XmlFile) AndroidPsiUtils.getPsiFileSafely(project,resourceFile);

                        XmlTag rootTag = myXmlFile.getRootTag();
                        JsonObject jsonObject = recurse(rootTag, appResources, referenceConfig);
                        json.add(StringEscapeUtils.unescapeXml(localName), jsonObject);
                        continue;

                    } else {
                        Drawable d = new Drawable(key, value);
                        if (remoteDrawables.contains(d)) {
                            int indexOf = remoteDrawables.indexOf(d);
                            Drawable drawable = remoteDrawables.get(indexOf);
                            value = drawable.getRemotePath();
                        } else {
                            remoteDrawables.add(d);
                        }
                    }
                }

                json.addProperty(StringEscapeUtils.unescapeXml(localName), StringEscapeUtils.unescapeXml(value));



            }
            XmlTag[] subTags = element.getSubTags();
            JsonArray children = new JsonArray();
            for (XmlTag subTag : subTags) {
                JsonObject child = recurse(subTag, appResources, referenceConfig);
                children.add(child);
            }
            if (children.size() > 0) {
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
