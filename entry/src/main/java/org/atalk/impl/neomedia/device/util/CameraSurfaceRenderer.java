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

import ohos.agp.graphics.TextureHolder;
import ohos.agp.render.opengl.GLES32;
import ohos.agp.render.opengl.GLESExt;
// import ohos.agp.utils.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * Code for rendering a texture onto a surface using OpenGL ES 2.0.
 */
public class CameraSurfaceRenderer {
    /**
     * Float size constant
     */
    private static final int FLOAT_SIZE_BYTES = 4;
    /**
     * Vertices stride size in bytes.
     */
    private static final int TRIANGLE_VERTICES_DATA_STRIDE_BYTES = 5 * FLOAT_SIZE_BYTES;
    /**
     * Position data offset
     */
    private static final int TRIANGLE_VERTICES_DATA_POS_OFFSET = 0;
    /**
     * UV data offset.
     */
    private static final int TRIANGLE_VERTICES_DATA_UV_OFFSET = 3;
    /**
     * Triangle vertices data.
     */
    private final float[] triangleVerticesData = {
            // X, Y, Z, U, V
            -1.0f, -1.0f, 0, 0.f, 0.f,
            1.0f, -1.0f, 0, 1.f, 0.f,
            -1.0f, 1.0f, 0, 0.f, 1.f,
            1.0f, 1.0f, 0, 1.f, 1.f
    };

    /**
     * Triangle vertices.
     */
    private final FloatBuffer triangleVertices;
    private final IntBuffer intBuffer = IntBuffer.allocate(256);
    private final StringBuffer strBuffer = new StringBuffer(512);

    private static final String VERTEX_SHADER = "uniform mat4 uMVPMatrix;\n"
            + "uniform mat4 uSTMatrix;\n"
            + "attribute vec4 aPosition;\n"
            + "attribute vec4 aTextureCoord;\n"
            + "varying vec2 vTextureCoord;\n"
            + "void main() {\n"
            + "  gl_Position = uMVPMatrix * aPosition;\n"
            + "  vTextureCoord = (uSTMatrix * aTextureCoord).xy;\n"
            + "}\n";

    private static final String FRAGMENT_SHADER = "#extension GL_OES_EGL_image_external : require\n"
            + "precision mediump float;\n"
            + "varying vec2 vTextureCoord;\n"
            + "uniform samplerExternalOES sTexture;\n"
            + "void main() {\n"
            + "  gl_FragColor = texture2D(sTexture, vTextureCoord);\n"
            + "}\n";

    private final float[] mvpMatrix = new float[16];
    private final float[] stMatrix = new float[16];

    private int program;
    private int textureID = -12345;
    private int mvpMatrixHandle;
    private int stMatrixHandle;
    private int positionHandle;
    private int textureHandle;

    public CameraSurfaceRenderer() {
        triangleVertices = ByteBuffer
                .allocateDirect(triangleVerticesData.length * FLOAT_SIZE_BYTES)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        triangleVertices.put(triangleVerticesData).position(0);
        // Matrix.setIdentityM(stMatrix, 0);
    }

    public int getTextureId() {
        return textureID;
    }

    public void drawFrame(TextureHolder st) {
        checkGlError("onDrawFrame start");
        st.getMatrixForTransform(stMatrix);

        GLES32.glClearColor(0.0f, 1.0f, 0.0f, 1.0f);
        GLES32.glClear(GLES32.GL_DEPTH_BUFFER_BIT | GLES32.GL_COLOR_BUFFER_BIT);

        GLES32.glUseProgram(program);
        checkGlError("glUseProgram");

        GLES32.glActiveTexture(GLES32.GL_TEXTURE0);
        GLES32.glBindTexture(GLESExt.GL_TEXTURE_EXTERNAL_OES, textureID);

        triangleVertices.position(TRIANGLE_VERTICES_DATA_POS_OFFSET);
        GLES32.glVertexAttribPointer(positionHandle, 3, GLES32.GL_FLOAT, false,
                TRIANGLE_VERTICES_DATA_STRIDE_BYTES, triangleVertices);
        checkGlError("glVertexAttribPointer maPosition");
        GLES32.glEnableVertexAttribArray(positionHandle);
        checkGlError("glEnableVertexAttribArray positionHandle");

        triangleVertices.position(TRIANGLE_VERTICES_DATA_UV_OFFSET);
        GLES32.glVertexAttribPointer(textureHandle, 2, GLES32.GL_FLOAT, false,
                TRIANGLE_VERTICES_DATA_STRIDE_BYTES, triangleVertices);
        checkGlError("glVertexAttribPointer textureHandle");
        GLES32.glEnableVertexAttribArray(textureHandle);
        checkGlError("glEnableVertexAttribArray textureHandle");

        // Matrix.setIdentityM(mvpMatrix, 0);
        GLES32.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix);
        GLES32.glUniformMatrix4fv(stMatrixHandle, 1, false, stMatrix);

        GLES32.glDrawArrays(GLES32.GL_TRIANGLE_STRIP, 0, 4);
        checkGlError("glDrawArrays");
        GLES32.glFinish();
    }

    /**
     * Initializes GL state. Call this after the EGL surface has been created and made current.
     */
    public void surfaceCreated() {
        program = createProgram(VERTEX_SHADER, FRAGMENT_SHADER);
        if (program == 0) {
            throw new RuntimeException("failed creating program");
        }
        positionHandle = GLES32.glGetAttribLocation(program, "aPosition");
        checkGlError("glGetAttribLocation aPosition");
        if (positionHandle == -1) {
            throw new RuntimeException("Could not get attrib location for aPosition");
        }
        textureHandle = GLES32.glGetAttribLocation(program, "aTextureCoord");
        checkGlError("glGetAttribLocation aTextureCoord");
        if (textureHandle == -1) {
            throw new RuntimeException("Could not get attrib location for aTextureCoord");
        }

        mvpMatrixHandle = GLES32.glGetUniformLocation(program, "uMVPMatrix");
        checkGlError("glGetUniformLocation uMVPMatrix");
        if (mvpMatrixHandle == -1) {
            throw new RuntimeException("Could not get attrib location for uMVPMatrix");
        }

        stMatrixHandle = GLES32.glGetUniformLocation(program, "uSTMatrix");
        checkGlError("glGetUniformLocation uSTMatrix");
        if (stMatrixHandle == -1) {
            throw new RuntimeException("Could not get attrib location for uSTMatrix");
        }

        int[] textures = new int[1];
        GLES32.glGenTextures(1, textures);

        textureID = textures[0];
        GLES32.glBindTexture(GLESExt.GL_TEXTURE_EXTERNAL_OES, textureID);
        checkGlError("glBindTexture textureID");

        GLES32.glTexParameterf(GLESExt.GL_TEXTURE_EXTERNAL_OES, GLES32.GL_TEXTURE_MIN_FILTER, GLES32.GL_NEAREST);
        GLES32.glTexParameterf(GLESExt.GL_TEXTURE_EXTERNAL_OES, GLES32.GL_TEXTURE_MAG_FILTER, GLES32.GL_LINEAR);
        GLES32.glTexParameteri(GLESExt.GL_TEXTURE_EXTERNAL_OES, GLES32.GL_TEXTURE_WRAP_S, GLES32.GL_CLAMP_TO_EDGE);
        GLES32.glTexParameteri(GLESExt.GL_TEXTURE_EXTERNAL_OES, GLES32.GL_TEXTURE_WRAP_T, GLES32.GL_CLAMP_TO_EDGE);
        checkGlError("glTexParameter");
    }

    private int loadShader(int shaderType, String source) {
        int shader = GLES32.glCreateShader(shaderType);
        checkGlError("glCreateShader type=" + shaderType);
        GLES32.glShaderSource(shader, 512, new String[]{source}, intBuffer);
        GLES32.glCompileShader(shader);
        int[] compiled = new int[1];
        GLES32.glGetShaderiv(shader, GLES32.GL_COMPILE_STATUS, compiled);
        if (compiled[0] == 0) {
            System.err.println("Could not compile shader " + shaderType + ":");
            GLES32.glGetShaderInfoLog(shader, 512, intBuffer, strBuffer);
            System.err.println(strBuffer);
            GLES32.glDeleteShader(shader);
            shader = 0;
        }
        return shader;
    }

    private int createProgram(String vertexSource, String fragmentSource) {
        int vertexShader = loadShader(GLES32.GL_VERTEX_SHADER, vertexSource);
        if (vertexShader == 0) {
            return 0;
        }
        int pixelShader = loadShader(GLES32.GL_FRAGMENT_SHADER, fragmentSource);
        if (pixelShader == 0) {
            return 0;
        }

        int program = GLES32.glCreateProgram();
        checkGlError("glCreateProgram");
        if (program == 0) {
            System.err.println("Could not create program");
        }
        GLES32.glAttachShader(program, vertexShader);
        checkGlError("glAttachShader");
        GLES32.glAttachShader(program, pixelShader);
        checkGlError("glAttachShader");
        GLES32.glLinkProgram(program);
        int[] linkStatus = new int[1];
        GLES32.glGetProgramiv(program, GLES32.GL_LINK_STATUS, linkStatus);
        if (linkStatus[0] != GLES32.GL_TRUE) {
            System.err.println("Could not link program: ");
            GLES32.glGetProgramInfoLog(program, 512, intBuffer, strBuffer);
            System.err.println(strBuffer);
            GLES32.glDeleteProgram(program);
            program = 0;
        }
        return program;
    }

    public void checkGlError(String op) {
        int error;
        if ((error = GLES32.glGetError()) != GLES32.GL_NO_ERROR) {
            System.err.println(op + ": glError " + error);
            throw new RuntimeException(op + ": glError " + error);
        }
    }

    public void release() {
        if (program != 0) {
            GLES32.glDeleteProgram(program);
            program = 0;
        }

        if (textureID != -12345) {
            GLES32.glDeleteTextures(1, new int[]{textureID});
            textureID = -12345;
        }
    }
}
