/*
 * aTalk, ohos VoIP and Instant Messaging client
 * Copyright 2024 Eng Chong Meng
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.atalk.impl.neomedia.device.util;

import ohos.aafwk.ability.Ability;
import ohos.agp.components.ComponentContainer;
import ohos.agp.components.textureprovider.TextureProvider;
import ohos.agp.graphics.TextureHolder;
import ohos.agp.graphics.TextureHolderListener;
import ohos.agp.render.opengl.EGL;
import ohos.agp.utils.Matrix;
import ohos.agp.utils.Point;
import ohos.agp.utils.RectFloat;

import org.atalk.impl.timberlog.TimberLog;
import org.atalk.ohos.aTalkApp;

import timber.log.Timber;

/**
 * Provider of Open GL context. Currently use to provide 'shared context' for recording/streaming video; and it
 * is used for rendering the local video preview.
 * <p>
 * Note: A TextureView object wraps a SurfaceTexture, responding to callbacks and acquiring new buffers.
 * link: <a href="https://source.android.com/devices/graphics/arch-tv">...</a>
 *
 * @author Eng Chong Meng
 */
public class OpenGlCtxProvider extends ViewDependentProvider<OpenGLContext> implements TextureHolderListener {
    /**
     * The <code>OpenGLContext</code>.
     */
    protected OpenGLContext mGLContext;

    protected AutoFitTextureView mTextureView;

    /**
     * Flag used to inform the <code>SurfaceStream</code> that the <code>onSurfaceTextureUpdated</code> event has occurred.
     */
    public boolean textureUpdated = true;

    /**
     * Creates new instance of <code>OpenGlCtxProvider</code>.
     *
     * @param ability parent <code>Ability</code>.
     * @param container the container that will hold maintained <code>JComponent</code>.
     */
    public OpenGlCtxProvider(Ability ability, ComponentContainer container) {
        super(ability, container);
    }

    @Override
    protected TextureProvider createViewInstance() {
        mTextureView = new AutoFitTextureView(mAbility);
        mTextureView.setTextureHolderListener(this);
        Timber.d("Text created: %s", mTextureView);
        return mTextureView;
    }

    /**
     * setup the TextureView with the given size to take care 4x3 and 16x9 aspect ration video
     *
     * @param width The width of `mTextureView`
     * @param height The height of `mTextureView`
     */
    public void setAspectRatio(int width, int height) {
        if (mTextureView != null) {
            mTextureView.setAspectRatio(width, height);
        }
        else {
            Timber.w("onSurfaceTexture configure transform mTextureView is null");
        }
    }

    /**
     * ConfigureTransform with the previously setup mTextureView size
     */
    public void setTransformMatrix() {
        configureTransform(mTextureView.mRatioWidth, mTextureView.mRatioHeight);
    }

    /**
     * Configures the necessary {@link Matrix} transformation to `mTextureView`.
     * This method should be called after the camera preview size is determined
     * and also the size of `mTextureView` is fixed.
     * <p>
     * Note: The transform is not working when the local preview container is very first setup;
     * Subsequence device rotation work but it also affects change the stream video; so far unable to solve
     * this problem.
     *
     * @param viewWidth The width of `mTextureView`
     * @param viewHeight The height of `mTextureView`
     */
    public void configureTransform(int viewWidth, int viewHeight) {
        RectFloat viewRect = new RectFloat(0, 0, viewWidth, viewHeight);
        Point vPoint = viewRect.getPivot();
        float centerX = vPoint.getPointX();
        float centerY = vPoint.getPointY();
        float scale = Math.max((float) viewWidth / mVideoSize.height, (float) viewHeight / mVideoSize.width);

        // Create an identity matrix
        Matrix matrix = new Matrix();
        int rotation = mAbility.getDisplayOrientation();
        if (CameraUtils.ROTATION_90 == rotation || CameraUtils.ROTATION_270 == rotation) {
            int degree = 90 * (rotation - 2);
            RectFloat bufferRect = new RectFloat(0, 0, mVideoSize.height, mVideoSize.width);
            bufferRect.translateCenterTo(vPoint);

            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(degree, centerX, centerY);
        }
        Timber.d("onSurfaceTexture configure transform: %s => [%sx%s]; scale: %s; rotation: %s",
                mVideoSize, viewWidth, viewHeight, scale, rotation);

        // Not properly rotate when in landscape if proceed
//        else { // else if (CameraUtils.ROTATION_0 == rotation || CameraUtils.ROTATION_180 == rotation) {
//            int degree = (rotation == 0) ? 0 : 180;
//            matrix.postRotate(degree, centerX, centerY);
//        }
        // mTextureView.setTransform(matrix);
    }

    /**
     * The method has problem to get the surface image to fill the local container size.
     */
    private void configureTransform2(int viewWidth, int viewHeight) {
        RectFloat viewRect = new RectFloat(0, 0, viewWidth, viewHeight);
        Point vPoint = viewRect.getPivot();
        float centerX = vPoint.getPointX();
        float centerY = vPoint.getPointY();

        int bufferWidth;
        int bufferHeight;
        int degree;

        if (aTalkApp.isPortrait) {
            bufferWidth = mVideoSize.height;
            bufferHeight = mVideoSize.width;
        }
        else {
            bufferWidth = mVideoSize.width;
            bufferHeight = mVideoSize.height;
        }

        // int rotation = mAbility.getWindowManager().getDefaultDisplay().getRotation();
        int rotation = mAbility.getDisplayOrientation();
        if (CameraUtils.ROTATION_90 == rotation || CameraUtils.ROTATION_270 == rotation) {
            degree = 90 * (rotation - 2);
        }
        else { // else if (CameraUtils.ROTATION_0 == rotation || CameraUtils.ROTATION_180 == rotation) {
            degree = (rotation == 0) ? 0 : 180;
        }
        RectFloat bufferRect = new RectFloat(0, 0, bufferWidth, bufferHeight);
        bufferRect.translateCenterTo(vPoint);

        Matrix matrix = new Matrix();
        float scale = Math.max((float) viewWidth / mVideoSize.width, (float) viewHeight / mVideoSize.height);

        matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
        matrix.postScale(scale, scale, centerX, centerY);
        matrix.postRotate(degree, centerX, centerY);

        Timber.d("onSurfaceTexture configure transform: [%sx%s] => [%sx%s]; scale=%s (%s/%s); rotation: %s (%s)",
                bufferWidth, bufferHeight, viewWidth, viewHeight, scale, scale * bufferWidth, scale * bufferHeight, rotation, degree);
        // mTextureView.setTransform(matrix);
    }

    // ========= TextureHolderListener implementation ========= //
    @Override
    synchronized public void onTextureHolderAvailable(TextureHolder surface, int width, int height) {
        mGLContext = new OpenGLContext(false, surface, EGL.EGL_NO_CONTEXT);
        onObjectCreated(mGLContext);
        Timber.d("onSurfaceTexture Available with dimension: [%s x %s] (%s)", width, height, mVideoSize);
    }

    @Override
    synchronized public void onTextureHolderDestroyed(TextureHolder surface) {
        onObjectDestroyed();
        // Release context only when the JComponent is destroyed
        if (mGLContext != null) {
            mGLContext.release();
            mGLContext = null;
        }
    }

    @Override
    public void onTextureHolderSizeChanged(TextureHolder surface, int width, int height) {
        Timber.d("onSurfaceTexture SizeChanged: [%s x %s]", width, height);
        configureTransform(width, height);
    }

    @Override
    public void onTextureHolderUpdated(TextureHolder surface) {
        Timber.log(TimberLog.FINER, "onSurfaceTextureUpdated");
        textureUpdated = true;
    }
}
