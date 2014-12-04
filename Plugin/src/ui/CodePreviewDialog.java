package ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.components.JBScrollPane;
import org.jetbrains.annotations.Nullable;
import api.Requests;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

/**
 * Created by kirankumar on 13/11/14.
 */
public class CodePreviewDialog extends DialogWrapper {


    private final Document document;
    private final Project project;
    private Timer documentChangeTimer;


    public CodePreviewDialog(@Nullable Project project,Document doc) {
        super(project);
        setTitle("Preview");
        this.project = project;
        this.document = doc;

        init();
        getContentPane().setMinimumSize(new Dimension(700,700));



    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        EditorTextField textField = new EditorTextField(document,project, StdFileTypes.JAVA);
        textField.setOneLineMode(false);
        textField.setRequestFocusEnabled(false);


        JBScrollPane scrollPane = new JBScrollPane(textField,ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        final JLabel picLabel = new JLabel("Loading ...");
        picLabel.setBackground(Color.BLACK);
        JBSplitter splitter = new JBSplitter(false,0.5f);
        splitter.setFirstComponent(scrollPane);
        splitter.setSecondComponent(picLabel);
        populateRenderPreview(picLabel,document.getText());

        picLabel.setPreferredSize(new Dimension(320,480));
        picLabel.setMaximumSize(new Dimension(320,480));
        document.addDocumentListener(new DocumentListener() {
            @Override
            public void beforeDocumentChange(DocumentEvent documentEvent) {

            }

            @Override
            public void documentChanged(DocumentEvent documentEvent) {
                handleDocumentChanged(documentEvent,picLabel);
            }
        });
        return splitter;
    }

    private void handleDocumentChanged(final DocumentEvent documentEvent, final JLabel picLabel) {
        if(documentChangeTimer!=null && documentChangeTimer.isRunning())
        {
            documentChangeTimer.restart();
        }
        else {
            documentChangeTimer = new Timer(500, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    populateRenderPreview(picLabel, document.getText());
                }
            });
            documentChangeTimer.setRepeats(false);
            documentChangeTimer.start();
        }
    }

    private void populateRenderPreview(final JLabel picLabel, final String code) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                final BufferedImage image = Requests.makeImageRequest(code);

                    ApplicationManager.getApplication().invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            if(image!=null) {
                                Border border = BorderFactory.createLineBorder(Color.BLACK,1);
                                picLabel.setText("");
                                picLabel.setIcon(new ImageIcon(image));
                                picLabel.setBorder(border);

                            }
                            else
                            {
                                picLabel.setText("Problem in rendering");

                            }
                        }
                    }, ModalityState.stateForComponent(picLabel));

            }
        };
        new Thread(runnable).start();

    }

}

