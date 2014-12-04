package api;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import components.Drawable;
import config.Config;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by kirankumar on 24/11/14.
 */
public class Requests {

    public static UploadResponse makePreUploadRequest(List<Drawable> drawables) throws IOException {
        HttpClient httpclient = HttpClients.createDefault();

        HttpPost httppost = new HttpPost(Config.RES_CHECK_URL);

        Gson gson = new Gson();
        JsonArray jsonArray = new JsonArray();
        for (Drawable drawable : drawables) {

            jsonArray.add(drawable.getAsJsonObject());
        }
        String json = gson.toJson(jsonArray);

        List<NameValuePair> params = new ArrayList<NameValuePair>(1);
        params.add(new BasicNameValuePair("resources", json));

        try {
            httppost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        HttpResponse response = null;
            response = httpclient.execute(httppost);

        HttpEntity entity = null;
        if (response != null) {
            entity = response.getEntity();
        }

        if (entity != null) {
            InputStream instream = null;
            try {
                instream = entity.getContent();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                // do something useful
                final BufferedReader reader =
                        new BufferedReader(new InputStreamReader(instream));
                UploadResponse uploadResponse = gson.fromJson(reader, UploadResponse.class);
                return uploadResponse;
            } catch (Exception e) {
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

    public static BufferedImage makeImageRequest(String code) {
        HttpClient httpclient = HttpClients.createDefault();

        HttpPost httppost = new HttpPost(Config.RENDER_URL);

        List<NameValuePair> params = new ArrayList<NameValuePair>(1);
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

    public static UploadResponse makeUploadRequest(List<Drawable> drawables) {
        HttpClient httpclient = HttpClients.createDefault();

        HttpPost httppost = new HttpPost(Config.RES_UPLOAD_URL);

        MultipartEntityBuilder entity = MultipartEntityBuilder.create();
        Gson gson = new Gson();
        for (Drawable drawable : drawables) {
            entity.addBinaryBody(drawable.getSignature() , new File(drawable.getFilePath()));
            String json = gson.toJson(drawable.getAsJsonObject());
            entity.addTextBody(drawable.getSignature(),json);
        }

        httppost.setEntity(entity.build());

        HttpResponse response = null;
        try {
            response = httpclient.execute(httppost);
        } catch (IOException e) {
            e.printStackTrace();
        }
        HttpEntity entity2 = response.getEntity();

        if (entity != null) {
            InputStream instream = null;
            try {
                instream = entity2.getContent();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                final BufferedReader reader =
                        new BufferedReader(new InputStreamReader(instream));
                UploadResponse uploadResponse = gson.fromJson(reader, UploadResponse.class);
                return uploadResponse;
            } catch (Exception e) {
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
