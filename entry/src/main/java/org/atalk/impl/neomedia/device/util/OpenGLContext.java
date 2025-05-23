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

import ohos.agp.components.textureprovider.TextureProvider;
import ohos.agp.render.opengl.EGL;
import ohos.agp.render.opengl.EGLConfig;
import ohos.agp.render.opengl.EGLContext;
import ohos.agp.render.opengl.EGLDisplay;
import ohos.agp.render.opengl.EGLSurface;

import org.atalk.ohos.aTalkApp;

import timber.log.Timber;

/**
 * Code for EGL context handling
 */
public class OpenGLContext extends TextureProvider {
    private static final int EGL_RECORDABLE_ANDROID = 0x3142;
    private static final int EGL_OPENGL_ES2_BIT = 0x0004;
    private static final int EGL_DRAW = 0x3059;
    private EGLDisplay mEGLDisplay;
    private EGLContext mEGLContext;
    private EGLSurface mEGLSurface;

    /**
     * Prepares EGL. We want a GLES 2.0 context and a surface that supports recording.
     */
    public OpenGLContext(boolean recorder, Object objSurface, EGLContext sharedContext) {
        super(aTalkApp.getInstance());
        mEGLDisplay = EGL.eglGetDisplay(EGL.EGL_DEFAULT_DISPLAY);
        if (mEGLDisplay == EGL.EGL_NO_DISPLAY) {
            throw new RuntimeException("unable to get EGL display");
        }

        int[] majorVersion = new int[]{0};
        int[] minorVersion = new int[]{0};
        if (!EGL.eglInitialize(mEGLDisplay, majorVersion,minorVersion)) {
            mEGLDisplay = EGL.EGL_NO_DISPLAY;
            throw new RuntimeException("unable to initialize EGL");
        }
        Timber.i("EGL version: %s.%s", majorVersion[0], minorVersion[0]);

        EGLConfig eglConfig = chooseEglConfig(mEGLDisplay, recorder);

        // Configure context for OpenGL ES 2.0.
        int[] attrib_list = {EGL.EGL_VERSION, 3, EGL.EGL_NONE};
        mEGLContext = EGL.eglCreateContext(mEGLDisplay, eglConfig, sharedContext, attrib_list);
        checkEglError("eglCreateContext");

        // Create a window surface, and attach it to the Surface we received.
        int[] surfaceAttribs = {EGL.EGL_NONE};
        mEGLSurface = EGL.eglCreateWindowSurface(mEGLDisplay, eglConfig, objSurface, surfaceAttribs);
        checkEglError("eglCreateWindowSurface");
    }

    private EGLConfig chooseEglConfig(EGLDisplay eglDisplay, boolean recorder) {
        EGLConfig[] configs = new EGLConfig[1];
        int[] attribList;

        if (recorder) {
            // Configure EGL for recording and OpenGL ES 2.0.
            attribList = new int[]{EGL.EGL_RED_SIZE, 8, EGL.EGL_GREEN_SIZE, 8, EGL.EGL_BLUE_SIZE, 8,
                    EGL.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT, EGL_RECORDABLE_ANDROID, 1, EGL.EGL_NONE};
        }
        else {
            // Configure EGL for OpenGL ES 2.0 only.
            attribList = new int[]{EGL.EGL_RED_SIZE, 8, EGL.EGL_GREEN_SIZE, 8, EGL.EGL_BLUE_SIZE, 8,
                    EGL.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT, EGL.EGL_NONE};
        }

        int[] numconfigs = new int[1];
        if (!EGL.eglChooseConfig(eglDisplay, attribList, configs, configs.length, numconfigs)) {
            throw new IllegalArgumentException("eglChooseConfig failed " + EGL.eglGetError());
        }
        else if (numconfigs[0] <= 0) {
            throw new IllegalArgumentException("eglChooseConfig failed " + EGL.eglGetError());
        }
        return configs[0];
    }

    /**
     * Discards all resources held by this class, notably the EGL context. Also releases the Surface
     * that was passed to our constructor.
     */
    public void release() {
        if (mEGLDisplay != EGL.EGL_NO_DISPLAY) {
            // Android is unusual in that it uses a reference-counted EGLDisplay.
            // So for every eglInitialize() we need an eglTerminate().
            EGL.eglMakeCurrent(mEGLDisplay, EGL.EGL_NO_SURFACE, EGL.EGL_NO_SURFACE, EGL.EGL_NO_CONTEXT);
            EGL.eglDestroySurface(mEGLDisplay, mEGLSurface);
            EGL.eglDestroyContext(mEGLDisplay, mEGLContext);
            EGL.eglReleaseThread();
            EGL.eglTerminate(mEGLDisplay);
        }
        mEGLDisplay = EGL.EGL_NO_DISPLAY;
        mEGLContext = EGL.EGL_NO_CONTEXT;
        mEGLSurface = EGL.EGL_NO_SURFACE;
    }

    public void makeCurrent() {
        EGLContext ctx = EGL.eglGetCurrentContext();
        EGLSurface surface = EGL.eglGetCurrentSurface(EGL_DRAW);
        if (!mEGLContext.equals(ctx) || !mEGLSurface.equals(surface)) {
            if (!EGL.eglMakeCurrent(mEGLDisplay, mEGLSurface, mEGLSurface, mEGLContext)) {
                throw new RuntimeException("eglMakeCurrent failed " + EGL.eglGetError());
            }
        }
    }

    /**
     * Sets "no surface" and "no context" on the current display.
     */
    public void releaseEGLSurfaceContext() {
        if (mEGLDisplay != EGL.EGL_NO_DISPLAY) {
            EGL.eglMakeCurrent(mEGLDisplay, EGL.EGL_NO_SURFACE, EGL.EGL_NO_SURFACE, EGL.EGL_NO_CONTEXT);
        }
    }

    /**
     * Calls eglSwapBuffers. Use this to "publish" the current frame.
     */
    public void swapBuffers() {
        if (!EGL.eglSwapBuffers(mEGLDisplay, mEGLSurface)) {
            throw new RuntimeException("Cannot swap buffers");
        }
        checkEglError("opSwapBuffers");
    }

    /**
     * Sends the presentation time stamp to EGL. Time is expressed in nanoseconds.
     */
    public void setPresentationTime(long nsecs) {
        // EGLExt.eglPresentationTimeANDROID(mEGLDisplay, mEGLSurface, nsecs);
        checkEglError("eglPresentationTimeANDROID");
    }

    /**
     * Checks for EGL errors. Throws an exception if one is found.
     */
    private void checkEglError(String msg) {
        int error;
        if ((error = EGL.eglGetError()) != EGL.EGL_SUCCESS) {
            throw new RuntimeException(msg + ": EGL error: 0x" + Integer.toHexString(error));
        }
    }

    public EGLContext getEGLContext() {
        return mEGLContext;
    }
}
