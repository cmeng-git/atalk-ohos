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
package org.atalk.impl.neomedia.device;

import ohos.agp.graphics.Surface;
import ohos.media.camera.CameraKit;
import ohos.media.camera.device.CameraAbility;
import ohos.media.camera.device.CameraInfo;
import ohos.media.camera.device.CameraInfo.FacingType;
import ohos.media.image.common.ImageFormat;
import ohos.media.image.common.Size;

import net.java.sip.communicator.util.UtilActivator;

import org.apache.http.util.TextUtils;
import org.atalk.impl.neomedia.codec.video.MediaEncoder;
import org.atalk.impl.neomedia.device.util.CameraUtils;
import org.atalk.impl.neomedia.device.util.OhosCamera;
import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.aTalkApp;
import org.atalk.service.configuration.ConfigurationService;
import org.atalk.service.neomedia.codec.Constants;
import org.atalk.util.MediaType;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import javax.media.CaptureDeviceManager;
import javax.media.Format;
import javax.media.MediaLocator;
import javax.media.format.VideoFormat;
import javax.media.format.YUVFormat;
import timber.log.Timber;

/**
 * Device system that provides YUV and Surface format camera data source. YUV frames are captured
 * using camera preview callback. Surface is passed directly through static methods to encoders.
 *
 * @author Eng Chong Meng
 */
public class OhosCameraSystem extends DeviceSystem {
    private static final String VIDEO_SIZE = ".video.size";
    public static final String PREVIEW_FORMAT = ".preview.format";

    /**
     * Supported preview sizes by android camera for user selection
     */
    public static Dimension[] SUPPORTED_SIZES = new Dimension[]{};

    public static boolean isCameraInitialized = false;

    /**
     * Creates a new instance of <code>OhosCameraSystem</code>.
     *
     * @throws Exception from super
     */
    public OhosCameraSystem()
            throws Exception {
        super(MediaType.VIDEO, LOCATOR_PROTOCOL_OHOSCAMERA);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doInitialize() {
        if (isCameraInitialized) {
            return;
        }
        CameraKit cameraManager = aTalkApp.getCameraManager();
        String[] cameraIdList = cameraManager.getCameraIds();
        if (cameraIdList.length == 0) {
            return;
        }

        ConfigurationService mConfig = UtilActivator.getConfigurationService();
        for (String cameraId : cameraIdList) {

            // create a locator with camera id and its facing direction
            CameraInfo cameraInfo = cameraManager.getCameraInfo(cameraId);
            int facing = cameraInfo.getFacingType();
            MediaLocator locator = OhosCamera.constructLocator(LOCATOR_PROTOCOL_OHOSCAMERA, cameraId, facing);

            // Retrieve the camera formats supported by this cameraId from DB
            String sFormat = mConfig.getString(locator + PREVIEW_FORMAT, null);
            List<Integer> cameraFormats = CameraUtils.stringToCameraFormat(sFormat);

            // List of preferred resolutions which is supported by the Camera.
            List<Dimension> sizes = new ArrayList<>();
            String vSize = mConfig.getString(locator + VIDEO_SIZE, null);
            if (TextUtils.isEmpty(sFormat) || !CameraUtils.getSupportedSizes(vSize, sizes)) {
                CameraAbility cameraAbility = cameraManager.getCameraAbility(cameraId);
                List<Size> previewSizes = cameraAbility.getSupportedSizes(Surface.class);
                if (previewSizes == null) {
                    /*
                     * The video size is the same as the preview size.
                     * MediaRecorder.setVideoSize(int,int) will most likely fail, print a line in
                     * logcat and not throw an exception (in DataSource.doStart()).
                     */
                    Timber.w("Output Preview Sizes returned null for camera: %s", cameraId);
                    continue;
                }

                vSize = CameraUtils.cameraSizesToString(previewSizes);
                // Save to DB and keep a copy of the video resolution supportSizes for cameraId
                mConfig.setProperty(locator + VIDEO_SIZE, vSize);
                CameraUtils.setCameraSupportSize(cameraId, previewSizes);

                // Selects only compatible dimensions
                sizes.clear();
                for (Size candidate : previewSizes) {
                    if (CameraUtils.isPreferredSize(candidate)) {
                        sizes.add(new Dimension(candidate.width, candidate.height));
                    }
                }

                cameraFormats = cameraAbility.getSupportedFormats();
                sFormat = CameraUtils.cameraImgFormatsToString(cameraFormats);
                mConfig.setProperty(locator + PREVIEW_FORMAT, sFormat);
            }

            Timber.i("#Video supported: %s (%s)\nsupported: %s\npreferred: %s", locator, sFormat,
                    vSize, CameraUtils.dimensionsToString(sizes));

            int count = sizes.size();
            if (count == 0)
                continue;

            // Saves supported video sizes
            Dimension[] array = new Dimension[count];
            sizes.toArray(array);
            SUPPORTED_SIZES = array;

            // Surface format
            List<Format> formats = new ArrayList<>();
            if (MediaEncoder.isDirectSurfaceEnabled()) {
                // TODO: camera will not be detected if only surface format is reported
                for (Dimension size : sizes) {
                    formats.add(new VideoFormat(
                            Constants.OHOS_SURFACE,
                            size,
                            Format.NOT_SPECIFIED,
                            Surface.class,
                            Format.NOT_SPECIFIED));
                }
            }

            // Add only if YUV420_888 is supported.
            if (cameraFormats.contains(ImageFormat.YUV420_888)) {
                // Image formats
                for (Dimension size : sizes) {
                    formats.add(new YUVFormat(size,
                            Format.NOT_SPECIFIED,
                            Format.byteArray,
                            YUVFormat.YUV_420,
                            Format.NOT_SPECIFIED,
                            Format.NOT_SPECIFIED,
                            Format.NOT_SPECIFIED,
                            Format.NOT_SPECIFIED,
                            Format.NOT_SPECIFIED,
                            Format.NOT_SPECIFIED)
                    );
                }
            }

            // Construct display name
            String name = (facing == FacingType.CAMERA_FACING_FRONT)
                    ? aTalkApp.getResString(ResourceTable.String_settings_camera_front)
                    : aTalkApp.getResString(ResourceTable.String_settings_camera_back);
            name += " (OhosCamera#" + cameraId + ")";
            if (formats.isEmpty()) {
                Timber.e("No supported formats reported by camera: %s", locator);
                continue;
            }
            OhosCamera device = new OhosCamera(name, locator, formats.toArray(new Format[0]));
            CaptureDeviceManager.addDevice(device);
        }
        isCameraInitialized = true;
    }
}