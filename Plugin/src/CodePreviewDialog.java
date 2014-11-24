import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.textarea.TextAreaDocument;
import com.intellij.openapi.fileTypes.FileTypes;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.xml.actions.xmlbeans.UIUtils;
import groovy.io.FileType;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.jetbrains.android.uipreview.AndroidLayoutPreviewPanel;
import org.jetbrains.android.uipreview.AndroidLayoutPreviewToolWindowManager;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.text.StringContent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

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
                final BufferedImage image = createHttpRequest(code);

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

    private BufferedImage createHttpRequest(String code) {
        HttpClient httpclient = HttpClients.createDefault();
        HttpPost httppost = new HttpPost("http://localhost/layoutengine/render.php");

        List<NameValuePair> params = new ArrayList<NameValuePair>(2);
        params.add(new BasicNameValuePair("layout", code));

        try {
            httppost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        HttpResponse response = null;
        try {
            response = httpclient.execute(httppost);
        } catch (IOException e) {
            e.printStackTrace();
        }
        HttpEntity entity = response.getEntity();

        if (entity != null) {
            InputStream instream = null;
            try {
                instream = entity.getContent();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                // do something useful
                BufferedImage bufferedImage = ImageIO.read(instream);
                return bufferedImage;
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    instream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }
}

