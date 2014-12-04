package components;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import com.intellij.ide.macro.FileNameMacro;
import utils.Utils;

/**
 * Created by kirankumar on 24/11/14.
 */
public class Drawable {

    private String name;
    private String filePath;
    @SerializedName("sign")
    private String signature;
    private String remotePath;

    public Drawable(String name, String filePath) {
        this.name = name;
        this.filePath = filePath;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getSignature() {
        if(signature==null)
        {
            try {
                String hash = Utils.getChecksum(filePath, "SHA1"); //or "SHA-256", "SHA-384", "SHA-512"
                signature = hash;
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
        return signature;
    }

    public String getRemotePath() {
        return remotePath;
    }

    public void setRemotePath(String remotePath) {
        this.remotePath = remotePath;
    }

    public String getFileExtension()
    {
        return filePath.substring(filePath.lastIndexOf(".")+1);
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        return name.equals(o.toString());
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    public JsonElement getAsJsonObject() {
        JsonObject drawableObj = new JsonObject();
        Drawable drawable = this;
        drawableObj.addProperty("name", drawable.getName());
        drawableObj.addProperty("ext",drawable.getFileExtension());
        drawableObj.addProperty("sign",drawable.getSignature());;
        return drawableObj;
    }
}
