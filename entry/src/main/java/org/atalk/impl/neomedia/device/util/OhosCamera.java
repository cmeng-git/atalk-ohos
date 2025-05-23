/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package org.atalk.impl.neomedia.device.util;

import org.atalk.impl.neomedia.MediaServiceImpl;
import org.atalk.impl.neomedia.NeomediaActivator;
import org.atalk.impl.neomedia.device.DeviceConfiguration;
import org.atalk.service.neomedia.MediaUseCase;

import java.util.Comparator;
import java.util.List;

import javax.media.CaptureDeviceInfo;
import javax.media.Format;
import javax.media.MediaLocator;

import ohos.media.camera.device.CameraInfo;

import timber.log.Timber;

/**
 * Class used to represent camera device in Android device systems.
 *
 * @author Pawel Domas
 * @author Eng Chong Meng
 */
public class OhosCamera extends CaptureDeviceInfo {
    /**
     * The facing of the camera is opposite to that of the screen.
     */
    public static final int FACING_BACK = CameraInfo.FacingType.CAMERA_FACING_BACK;

    /**
     * The facing of the camera is the same as that of the screen.
     */
    public static final int FACING_FRONT = CameraInfo.FacingType.CAMERA_FACING_FRONT;

    /**
     * Creates a new instance of <code>OhosCamera</code>
     *
     * @param name human readable name of the camera e.g. front camera.
     * @param locator the <code>MediaLocator</code> identifying the camera and it's system.
     * @param formats list of supported formats.
     */
    public OhosCamera(String name, MediaLocator locator, Format[] formats) {
        super(name, locator, formats);
    }

    /**
     * Creates <code>MediaLocator</code> for given parameters.
     *
     * @param locatorProtocol locator protocol that identifies device system.
     * @param cameraId ID of camera that identifies the camera.
     * @param facing direction of the corresponding to given <code>cameraId</code>.
     *
     * @return camera <code>MediaLocator</code> for given parameters.
     */
    public static MediaLocator constructLocator(String locatorProtocol, String cameraId, int facing) {
        return new MediaLocator(locatorProtocol + ":" + cameraId + "/" + facing);
    }

    /**
     * Returns the protocol part of <code>MediaLocator</code> that identifies camera device system.
     *
     * @return the protocol part of <code>MediaLocator</code> that identifies camera device system.
     */
    public String getCameraProtocol() {
        return locator.getProtocol();
    }

    /**
     * Returns camera facing direction.
     *
     * @return camera facing direction.
     */
    public int getCameraFacing() {
        return getCameraFacing(locator);
    }

    /**
     * Extracts camera id from given <code>locator</code>.
     *
     * @param locator the <code>MediaLocator</code> that identifies the camera.
     *
     * @return extracted camera id from given <code>locator</code>.
     */
    public static String getCameraId(MediaLocator locator) {
        String remainder = locator.getRemainder();
        return remainder.substring(0, remainder.indexOf("/"));
    }

    /**
     * Extracts camera facing from given <code>locator</code>.
     *
     * @param locator the <code>MediaLocator</code> that identifies the camera.
     *
     * @return extracted camera facing from given <code>locator</code>.
     */
    public static int getCameraFacing(MediaLocator locator) {
        String remainder = locator.getRemainder();
        return Integer.parseInt(remainder.substring(remainder.indexOf("/") + 1));
    }

    /**
     * Returns array of cameras available in the system.
     *
     * @return array of cameras available in the system.
     */
    public static OhosCamera[] getCameras() {
        DeviceConfiguration devConfig = NeomediaActivator.getMediaServiceImpl().getDeviceConfiguration();
        List<CaptureDeviceInfo> videoDevices = devConfig.getAvailableVideoCaptureDevices(MediaUseCase.CALL);
        videoDevices.sort(new SortByName());

        OhosCamera[] cameras = new OhosCamera[videoDevices.size()];
        for (int i = 0; i < videoDevices.size(); i++) {
            CaptureDeviceInfo device = videoDevices.get(i);
            // All cameras have to be of type OhosCamera on Android
            cameras[i] = (OhosCamera) device;
        }
        return cameras;
    }

    // Used for sorting in ascending order CaptureDeviceInfo by name
    static class SortByName implements Comparator<CaptureDeviceInfo> {
        public int compare(CaptureDeviceInfo a, CaptureDeviceInfo b) {
            return a.getName().compareTo(b.getName());
        }
    }

    /**
     * Finds camera with given <code>facing</code> from the same device system as currently selected camera.
     *
     * @param facing the facing direction of camera to find.
     *
     * @return camera with given <code>facing</code> from the same device system as currently selected camera.
     */
    public static OhosCamera getCameraFromCurrentDeviceSystem(int facing) {
        OhosCamera currentCamera = getSelectedCameraDevInfo();
        String currentProtocol = (currentCamera != null) ? currentCamera.getCameraProtocol() : null;

        OhosCamera[] cameras = getCameras();
        for (OhosCamera camera : cameras) {
            // Match facing
            if (camera.getCameraFacing() == facing) {
                // Now match the protocol if any
                if (currentProtocol != null) {
                    if (currentProtocol.equals(camera.getCameraProtocol())) {
                        return camera;
                    }
                }
                else {
                    return camera;
                }
            }
        }
        return null;
    }

    /**
     * Returns device info of the currently selected camera.
     *
     * @return device info of the currently selected camera.
     */
    public static OhosCamera getSelectedCameraDevInfo() {
        MediaServiceImpl mediaServiceImpl = NeomediaActivator.getMediaServiceImpl();
        return (mediaServiceImpl == null) ? null
                : (OhosCamera) mediaServiceImpl.getDeviceConfiguration().getVideoCaptureDevice(MediaUseCase.CALL);
    }

    /**
     * Selects the camera identified by given locator to be used by the system.
     *
     * @param cameraLocator camera device locator that will be used.
     */
    public static OhosCamera setSelectedCamera(MediaLocator cameraLocator) {
        DeviceConfiguration devConfig = NeomediaActivator.getMediaServiceImpl().getDeviceConfiguration();
        List<CaptureDeviceInfo> videoDevices = devConfig.getAvailableVideoCaptureDevices(MediaUseCase.CALL);

        OhosCamera selectedCamera = null;
        for (CaptureDeviceInfo deviceInfo : videoDevices) {
            if (deviceInfo.getLocator().equals(cameraLocator)) {
                selectedCamera = (OhosCamera) deviceInfo;
                break;
            }
        }
        if (selectedCamera != null) {
            devConfig.setVideoCaptureDevice(selectedCamera, true);
            return selectedCamera;
        }
        else {
            Timber.w("No camera found for name: %s", cameraLocator);
            return null;
        }
    }
}