import com.intellij.icons.AllIcons;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.editor.textarea.TextAreaDocument;
import com.intellij.openapi.fileTypes.FileTypes;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.components.JBScrollPane;
import groovy.io.FileType;
import org.jetbrains.android.uipreview.AndroidLayoutPreviewPanel;
import org.jetbrains.android.uipreview.AndroidLayoutPreviewToolWindowManager;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.text.StringContent;
import java.awt.*;

/**
 * Created by kirankumar on 13/11/14.
 */
public class CodePreviewDialog extends DialogWrapper {


    private final String code;
    private final Project project;


    public CodePreviewDialog(@Nullable Project project,String code) {
        super(project);
        setTitle("Preview");
        this.project = project;
        this.code = code;
        init();


    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        EditorTextField textField = new EditorTextField(code,project, StdFileTypes.JAVA);
        textField.setOneLineMode(false);
        textField.setRequestFocusEnabled(false);
        //AndroidLayoutPreviewToolWindowManager previewToolWindowManager = AndroidLayoutPreviewToolWindowManager.getInstance(project);
        //JPanel contentPanel = previewToolWindowManager.getToolWindowForm().getContentPanel();
        JBScrollPane scrollPane = new JBScrollPane(textField,ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        //scrollPane.setRequestFocusEnabled(true);
        return scrollPane;
    }
}
