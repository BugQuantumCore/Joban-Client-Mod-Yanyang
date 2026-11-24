package com.lx862.mtrscripting.core.util.model;

import com.mojang.blaze3d.systems.RenderSystem;

public class DynamicModelHolderJS {
    /* volatile: close() runs on script-executor threads while the deferred
       upload callback runs on the render thread. */
    private volatile ModelJS uploadedModel;
    private volatile boolean closed;

    public DynamicModelHolderJS() {
    }

    public void uploadLater(RawModelJS modelData) {
        if(modelData == null) throw new IllegalArgumentException("modelData cannot be null!");

        RenderSystem.recordRenderCall(() -> {
            /* The holder may have been disposed while this upload was queued
               (dispose() runs on the script-executor thread; the queued render
               call lands afterwards). Uploading then would leak the new GPU
               model forever — nothing references it anymore. */
            if(closed) return;
            ModelJS lastUploadedModel = uploadedModel;
            uploadedModel = ModelJS.uploadRawModel(modelData);
            if (lastUploadedModel != null) lastUploadedModel.close();
        });
    }

    public ModelJS getUploadedModel() {
        return uploadedModel;
    }

    public void close() {
        closed = true;
        /* Null-safe: close() can run before any (deferred) upload landed. */
        final ModelJS model = uploadedModel;
        if(model != null) model.close();
    }
}
