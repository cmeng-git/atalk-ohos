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

import ohos.agp.window.service.Display;
import ohos.media.camera.CameraKit;
import ohos.media.camera.device.Camera;
import ohos.media.camera.device.CameraAbility;
import ohos.media.camera.device.CameraInfo;
import ohos.media.camera.device.CameraInfo.FacingType;
import ohos.media.image.common.ImageFormat;
import ohos.media.image.common.Size;

import org.apache.http.util.TextUtils;
import org.atalk.impl.neomedia.device.DeviceConfiguration;
import org.atalk.impl.neomedia.jmfext.media.protocol.ohoscamera.PreviewStream;
import org.atalk.ohos.aTalkApp;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import timber.log.Timber;

import static ohos.media.camera.params.PropertyKey.SENSOR_ORIENTATION;

/**
 * Utility methods for operations on <code>Camera</code> objects. Also shares preview surface provider
 * between <code>MediaRecorder</code> and <code>OhosCamera</code> device systems.
 *
 * @author Eng Chong Meng
 */
public class CameraUtils {
    /**
     * Rotation constant: 0 degree rotation (natural orientation)
     */
    public static final int ROTATION_0 = 0;

    /**
     * Rotation constant: 90 degree rotation.
     */
    public static final int ROTATION_90 = 1;

    /**
     * Rotation constant: 180 degree rotation.
     */
    public static final int ROTATION_180 = 2;

    /**
     * Rotation constant: 270 degree rotation.
     */
    public static final int ROTATION_270 = 3;

    /**
     * Separator use when save camera formats to DB. Do not change
     */
    private static final String FORMAT_SEPARATOR = ", ";

    /**
     * Surface provider used to display camera preview
     */
    private static PreviewSurfaceProvider surfaceProvider;

    /**
     * The list of sizes from which the first supported by the respective Camera is to be
     * chosen as the size of the one and only <code>Format</code> supported by the associated
     * <code>MediaRecorder</code> <code>CaptureDevice</code>.
     * <p>
     * User selectable video resolution. The actual resolution use during video call is adjusted so
     * it is within device capability {@link  #getOptimalPreviewSize(Dimension, List)}
     * Any strides paddings if required, is properly handled in
     * {@link PreviewStream #YUV420PlanarRotate(Image, byte[], int, int)}
     */
    public static final Dimension[] PREFERRED_SIZES = DeviceConfiguration.SUPPORTED_RESOLUTIONS;

    /**
     * Map contains all the phone available cameras and their supported resolution sizes
     * This list is being update at the device start up in.
     *
     * @see org.atalk.impl.neomedia.device.MediaRecorderSystem
     */
    private static final Map<String, List<Size>> cameraSupportSize = new HashMap<>();

    /**
     * Returns <code>true</code> if given <code>size</code> is on the list of preferred sizes.
     *
     * @param size the size to check.
     *
     * @return <code>true</code> if given <code>size</code> is on the list of preferred sizes.
     */
    public static boolean isPreferredSize(Dimension size) {
        for (Dimension s : PREFERRED_SIZES) {
            if (s.width == size.width && s.height == size.height) {
                return true;
            }
        }
        return false;
    }

    public static boolean isPreferredSize(Size size) {
        for (Dimension s : PREFERRED_SIZES) {
            if (s.width == size.width && s.height == size.height) {
                return true;
            }
        }
        return false;
    }

    /**
     * Constructs a <code>String</code> representation of a specific <code>Iterable</code> of
     * <code>Size</code>s. The elements of the specified <code>Iterable</code> are delimited by
     * &quot;, &quot;. The method has been introduced because the <code>Camera.Size</code> class does
     * not provide a <code>String</code> representation which contains the <code>width</code> and the
     * <code>height</code> in human-readable form.
     *
     * @param sizes the <code>Iterable</code> of <code>Size</code>s which is to be represented as a
     * human-readable <code>String</code>
     *
     * @return the human-readable <code>String</code> representation of the specified <code>sizes</code>
     */
    public static String cameraSizesToString(Size[] sizes) {
        StringBuilder s = new StringBuilder();
        for (Size size : sizes) {
            if (s.length() != 0)
                s.append(", ");
            s.append(size.toString());
        }
        return s.toString();
    }

    /**
     * Constructs a <code>String</code> representation of a specific <code>Iterable</code> of
     * <code>Dimension</code>s. The elements of the specified <code>Iterable</code> are delimited by &quot;, &quot;.
     *
     * @param sizes the <code>Iterable</code> of <code>Dimension</code>s which is to be represented as a
     * human-readable <code>String</code>
     *
     * @return the human-readable <code>String</code> representation of the specified <code>sizes</code>
     */
    public static String dimensionsToString(Iterable<Dimension> sizes) {
        StringBuilder s = new StringBuilder();
        for (Dimension size : sizes) {
            if (s.length() != 0)
                s.append(", ");
            s.append(size.width).append('x').append(size.height);
        }
        return s.toString();
    }

    /**
     * Returns the string representation of the formats contained in given list.
     *
     * @param formats the list of image formats integers defined in <code>ImageFormat</code> class.
     *
     * @return the string representation of the formats contained in given list.
     */
    public static String cameraImgFormatsToString(List<Integer> formats) {
        StringBuilder s = new StringBuilder();

        for (int format : formats) {
            if (s.length() != 0)
                s.append(FORMAT_SEPARATOR);

            switch (format) {
                case ImageFormat.NV21:
                    s.append("NV21");
                    break;
                case ImageFormat.JPEG:
                    s.append("JPEG");
                    break;
                case ImageFormat.RAW16:
                    s.append("RAW_SENSOR");
                    break;
                case ImageFormat.YUV420_888:
                    s.append("YUV_420_888");
                    break;
                default:
                    s.append(format);
            }
        }
        return s.toString();
    }

    public static List<Integer> stringToCameraFormat(String sFormat) {
        List<Integer> sFormats = new ArrayList<>();
        if (!TextUtils.isEmpty(sFormat)) {
            String[] pfs = sFormat.split(FORMAT_SEPARATOR);
            for (String cfx : pfs) {
                switch (cfx) {
                    case "NV21":
                        sFormats.add(ImageFormat.NV21);
                        break;
                    case "JPEG":
                        sFormats.add(ImageFormat.JPEG);
                        break;
                    case "RAW_SENSOR":
                        sFormats.add(ImageFormat.RAW16);
                        break;
                    case "YUV_420_888":
                        sFormats.add(ImageFormat.YUV420_888);
                        break;
                    default:
                        try {
                            sFormats.add(Integer.valueOf(cfx));
                        } catch (NumberFormatException nfe) {
                            Timber.w("Number Format Exception in Camera Format: %s", cfx);
                        }
                }
            }
        }
        return sFormats;
    }

    /**
     * Sets the {@link PreviewSurfaceProvider} that will be used with camera
     *
     * @param provider the surface provider to set
     */
    public static void setPreviewSurfaceProvider(PreviewSurfaceProvider provider) {
        surfaceProvider = provider;
    }

    /**
     * Calculates preview orientation for the {@link Display}'s <code>rotation</code>
     * in degrees for the selected cameraId, also taking into account of the device orientation.
     * valid camera orientation: 0 or 90
     * valid displayRotation: 0, 90, 180
     *
     * @return camera preview orientation value in degrees that can be used to adjust the preview orientation
     */
    public static int getPreviewOrientation(String cameraId) {
        int displayRotation = surfaceProvider.getDisplayRotation();
        int previewOrientation = 0;

        CameraKit cameraManager = aTalkApp.getCameraManager();
        CameraInfo cameraInfo = cameraManager.getCameraInfo(cameraId);
        int facing = cameraInfo.getFacingType();

        CameraAbility cameraAbility = cameraManager.getCameraAbility(cameraId);
        int sensorOrientation = cameraAbility.getPropertyValue(SENSOR_ORIENTATION);

        int degrees = 0;
        switch (displayRotation) {
            case CameraUtils.ROTATION_0:
                degrees = 0;
                break;
            case CameraUtils.ROTATION_90:
                degrees = 90;
                break;
            case CameraUtils.ROTATION_180:
                degrees = 180;
                break;
            case CameraUtils.ROTATION_270:
                degrees = 270;
                break;
        }

        // front-facing camera
        if (FacingType.CAMERA_FACING_FRONT == facing) {
            previewOrientation = (sensorOrientation + degrees) % 360;
            previewOrientation = (360 - previewOrientation) % 360; // compensate for the mirroring
        }
        // back-facing camera
        else {
            previewOrientation = (sensorOrientation - degrees + 360) % 360;
        }
        return previewOrientation;
    }

    /**
     * Get the optimize size that is supported by the camera resolution capability
     * closely match to the preview size requested.
     * Note: Camera native natural orientation is always in landscape mode
     *
     * @param previewSize requested preview size
     * @param sizes List of camera supported sizes
     *
     * @return optimized preview size based on camera capability
     */
    public static Dimension getOptimalPreviewSize(Dimension previewSize, List<Size> sizes) {
        if (sizes == null)
            return previewSize;

        int w = previewSize.width;
        int h = previewSize.height;

        final double ASPECT_TOLERANCE = 0.05;
        double targetRatio = (double) w / h;

        Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        // obtain the highest possible resolution
        for (Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
                continue;

            if (Math.abs(size.height - h) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - h);
            }
        }

        // If none found, then get the closer size disregard the aspect ratio.
        if (optimalSize == null) {
            optimalSize = new Size(w, h);
            for (Size size : sizes) {
                if (Math.abs(size.height - h) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - h);
                }
            }
        }
        return new Dimension(optimalSize.width, optimalSize.height);
    }

    /**
     * Store the supported video resolution by camera cameraId
     *
     * @param cameraId camera ID
     * @param sizes list of camera support video resolutions
     */
    public static void setCameraSupportSize(String cameraId, List<Size> sizes) {
        cameraSupportSize.put(cameraId, sizes);
    }

    /**
     * Get the list of camera video resolutions supported by cameraId
     *
     * @param cameraId the request camera Id resolutions
     *
     * @return List of camera video resolutions supported by cameraId
     */
    public static List<Size> getSupportSizeForCameraId(String cameraId) {
        return cameraSupportSize.get(cameraId);
    }

    public static boolean getSupportedSizes(String vs, List<Dimension> sizes) {
        if (!TextUtils.isEmpty(vs)) {
            String[] videoSizes = vs.split(FORMAT_SEPARATOR);
            for (String videoSize : videoSizes) {
                if (!TextUtils.isEmpty(videoSize) && videoSize.contains("x")) {
                    String[] wh = videoSize.split("x");
                    Dimension candidate = new Dimension(Integer.parseInt(wh[0]), Integer.parseInt(wh[1]));
                    if (CameraUtils.isPreferredSize(candidate)) {
                        sizes.add(candidate);
                    }
                }
            }
            return true;
        }
        return false;
    }
}
