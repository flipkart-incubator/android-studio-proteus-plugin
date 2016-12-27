package com.flipkart.proteus.plugin.ui;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.components.JBScrollPane;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * @author kirankumar
 * @author adityasharat
 */
public class CodePreviewDialog extends DialogWrapper {

    private final Document document;
    private final Project project;

    public CodePreviewDialog(@Nullable Project project, Document doc) {
        super(project);
        setTitle("XML to JSON");
        this.project = project;
        this.document = doc;
        init();
        getContentPane().setMinimumSize(new Dimension(400, 700));
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        document.addDocumentListener(new DocumentListener() {
            @Override
            public void beforeDocumentChange(DocumentEvent documentEvent) {
            }

            @Override
            public void documentChanged(DocumentEvent documentEvent) {
            }
        });

        EditorTextField editor = new EditorTextField(document, project, StdFileTypes.JAVA);
        editor.setOneLineMode(false);
        editor.setRequestFocusEnabled(false);

        return new JBScrollPane(editor, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
    }
}

