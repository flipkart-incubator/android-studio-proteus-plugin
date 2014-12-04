package api;

import components.Drawable;

import java.util.List;

/**
 * Created by kirankumar on 24/11/14.
 */
public class UploadResponse {
    private List<Drawable> resources;
    private boolean requiresUpload;

    public List<Drawable> getResources() {
        return resources;
    }

    public void setResources(List<Drawable> resources) {
        this.resources = resources;
    }

    public boolean isRequiresUpload() {
        return requiresUpload;
    }

    public void setRequiresUpload(boolean requiresUpload) {
        this.requiresUpload = requiresUpload;
    }
}
