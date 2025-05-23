/*
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.atalk.impl.neomedia.jmfext.media.renderer.video;

import org.atalk.impl.timberlog.TimberLog;
import org.atalk.impl.neomedia.codec.video.SwScale;
import org.atalk.impl.neomedia.jmfext.media.renderer.AbstractRenderer;
import org.atalk.util.OSUtils;
import org.atalk.ohos.agp.components.JComponent;
import org.atalk.util.swing.VideoLayout;

import java.awt.Dimension;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import javax.media.Buffer;
import javax.media.Format;
import javax.media.ResourceUnavailableException;
import javax.media.format.RGBFormat;
import javax.media.format.VideoFormat;
import javax.media.format.YUVFormat;
import javax.media.renderer.VideoRenderer;

import ohos.agp.components.Component;
import ohos.agp.utils.Rect;

import timber.log.Timber;

/**
 * Implements a <code>VideoRenderer</code> which uses JAWT to perform native painting in an AWT or Swing <code>JComponent</code>.
 *
 * @author Eng Chong Meng
 */
public class JAWTRenderer extends AbstractRenderer<VideoFormat> implements VideoRenderer {
    /**
     * The default, initial height and width to set on the <code>JComponent</code>s of
     * <code>JAWTRenderer</code>s before video frames with actual sizes are processed. Introduced to
     * mitigate multiple failures to realize the actual video frame size and/or to properly scale
     * the visual/video <code>JComponent</code>s.
     */
    private static final int DEFAULT_COMPONENT_HEIGHT_OR_WIDTH = 16;

    /**
     * The human-readable <code>PlugIn</code> name of the <code>JAWTRenderer</code> instances.
     */
    private static final String PLUGIN_NAME = "JAWT Renderer";

    /**
     * The array of supported input formats.
     */
    private static final Format[] SUPPORTED_INPUT_FORMATS = new Format[]{
            OSUtils.IS_LINUX ? new YUVFormat(
                    null /* size */,
                    Format.NOT_SPECIFIED /* maxDataLength */,
                    Format.intArray,
                    Format.NOT_SPECIFIED /* frameRate */,
                    YUVFormat.YUV_420,
                    Format.NOT_SPECIFIED /* strideY */,
                    Format.NOT_SPECIFIED /* strideUV */,
                    Format.NOT_SPECIFIED /* offsetY */,
                    Format.NOT_SPECIFIED /* offsetU */,
                    Format.NOT_SPECIFIED /* offsetV */)

                    : OSUtils.IS_ANDROID ? new RGBFormat(
                    null,
                    Format.NOT_SPECIFIED,
                    Format.intArray,
                    Format.NOT_SPECIFIED,
                    32,
                    0x000000ff, 0x0000ff00, 0x00ff0000)

                    : new RGBFormat(
                    null,
                    Format.NOT_SPECIFIED,
                    Format.intArray,
                    Format.NOT_SPECIFIED,
                    32,
                    0x00ff0000, 0x0000ff00, 0x000000ff)
    };

    static {
        System.loadLibrary("jnawtrenderer");
    }

    static native void addNotify(long handle, JComponent component);

    /**
     * Closes the native counterpart of a <code>JAWTRenderer</code> specified by its handle as returned
     * by {@link #open(JComponent)} and rendering into a specific AWT <code>JComponent</code>. Releases
     * the resources which the specified native counterpart has retained during its execution and
     * its handle is considered to be invalid afterwards.
     *
     * @param handle the handle to the native counterpart of a <code>JAWTRenderer</code> as returned by
     * {@link #open(JComponent)} which is to be closed
     * @param component the AWT <code>JComponent</code> into which the <code>JAWTRenderer</code> and its native
     * counterpart are drawing. The platform-specific info of <code>component</code> is not guaranteed to be valid.
     */
    private static native void close(long handle, JComponent component);

    /**
     * Opens a handle to a native counterpart of a <code>JAWTRenderer</code> which is to draw into a
     * specific AWT <code>JComponent</code>.
     *
     * @param component the AWT <code>JComponent</code> into which a <code>JAWTRenderer</code> and the native
     * counterpart to be opened are to draw. The platform-specific info of <code>component</code>
     * is not guaranteed to be valid.
     *
     * @return a handle to a native counterpart of a <code>JAWTRenderer</code> which is to draw into
     * the specified AWT <code>JComponent</code>
     *
     * @throws ResourceUnavailableException if there is a problem during opening
     */
    private static native long open(JComponent component)
            throws ResourceUnavailableException;

    /**
     * Paints a specific <code>JComponent</code> which is the AWT <code>JComponent</code> of a
     * <code>JAWTRenderer</code> specified by the handle to its native counterpart.
     *
     * @param handle the handle to the native counterpart of a <code>JAWTRenderer</code> which is to draw into
     * the specified AWT <code>JComponent</code>
     * @param component the AWT <code>JComponent</code> into which the <code>JAWTRenderer</code> and its native
     * counterpart specified by <code>handle</code> are to draw. The platform-specific info of
     * <code>component</code> is guaranteed to be valid only during the execution of <code>paint</code>.
     * @param g the <code>Graphics</code> context into which the drawing is to be performed
     * @param zOrder Layer order
     *
     * @return <code>true</code> if the native counterpart of a <code>JAWTRenderer</code> wants to continue
     * receiving the <code>paint</code> calls on the AWT <code>JComponent</code>; otherwise, false.
     * For example, after the native counterpart has been able to acquire the native handle
     * of the AWT <code>JComponent</code>, it may be able to determine when the native handle
     * needs painting without waiting for AWT to call <code>paint</code> on the
     * <code>JComponent</code>. In such a scenario, the native counterpart may indicate with
     * <code>false</code> that it does not need further <code>paint</code> deliveries.
     */
    static native boolean paint(long handle, JComponent component, Graphics g, int zOrder);

    /**
     * Processes the data provided in a specific <code>int</code> array with a specific offset and
     * length and renders it to the output device represented by a <code>JAWTRenderer</code> specified
     * by the handle to it native counterpart.
     *
     * @param handle the handle to the native counterpart of a <code>JAWTRenderer</code> to process the
     * specified data and render it
     * @param component the <code>AWT</code> component into which the specified <code>JAWTRenderer</code> and its
     * native counterpart draw
     * @param data an <code>int</code> array which contains the data to be processed and rendered
     * @param offset the index in <code>data</code> at which the data to be processed and rendered starts
     * @param length the number of elements in <code>data</code> starting at <code>offset</code> which represent
     * the data to be processed and rendered
     * @param width the width of the video frame in <code>data</code>
     * @param height the height of the video frame in <code>data</code>
     *
     * @return <code>true</code> if data has been successfully processed
     */
    static native boolean process(long handle, JComponent component, int[] data, int offset,
            int length, int width, int height);

    static native void removeNotify(long handle, JComponent component);

    private static native String sysctlbyname(String name);

    /**
     * The AWT <code>JComponent</code> into which this <code>VideoRenderer</code> draws.
     */
    private JComponent component;

    /**
     * The handle to the native counterpart of this <code>JAWTRenderer</code>.
     */
    private long handle = 0;

    /**
     * The last known height of the input processed by this <code>JAWTRenderer</code>.
     */
    private int height = 0;

    /**
     * The <code>Runnable</code> which is executed to bring the invocations of
     * {@link #reflectInputFormatOnComponent()} into the AWT event dispatching thread.
     */
    private final Runnable reflectInputFormatOnComponentInEventDispatchThread
            = this::reflectInputFormatOnComponentInEventDispatchThread;

    /**
     * The last known width of the input processed by this <code>JAWTRenderer</code>.
     */
    private int width = 0;

    /**
     * Initializes a new <code>JAWTRenderer</code> instance.
     */
    public JAWTRenderer() {
    }

    /**
     * Closes this <code>PlugIn</code> and releases the resources it has retained during its execution.
     * No more data will be accepted by this <code>PlugIn</code> afterwards. A closed <code>PlugIn</code>
     * can be reinstated by calling <code>open</code> again.
     */
    @Override
    public synchronized void close() {
        if (handle != 0) {
            close(handle, component);
            handle = 0;
        }
    }

    /**
     * Gets the region in the component of this <code>VideoRenderer</code> where the video is rendered.
     * <code>JAWTRenderer</code> always uses the entire component i.e. always returns <code>null</code>.
     *
     * @return the region in the component of this <code>VideoRenderer</code> where the video is
     * rendered; <code>null</code> if the entire component is used
     */
    @Override
    public Rect getBounds() {
        return null;
    }

    /**
     * Gets the AWT <code>JComponent</code> into which this <code>VideoRenderer</code> draws.
     *
     * @return the AWT <code>JComponent</code> into which this <code>VideoRenderer</code> draws
     */
    @Override
    public synchronized JComponent getComponent() {
        if (component == null) {
            StringBuilder componentClassName = new StringBuilder();

            componentClassName.append(
                    "org.atalk.impl.neomedia.jmfext.media.renderer.video.JAWTRenderer");
            if (OSUtils.IS_ANDROID)
                componentClassName.append("Android");
            componentClassName.append("VideoComponent");

            Throwable reflectiveOperationException = null;

            try {
                Class<?> componentClass = Class.forName(componentClassName.toString());
                Constructor<?> componentConstructor = componentClass.getConstructor(JAWTRenderer.class);
                component = (JComponent) componentConstructor.newInstance(this);
            } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException
                     | InstantiationException | IllegalAccessException cnfe) {
                reflectiveOperationException = cnfe;
            }
            if (reflectiveOperationException != null)
                throw new RuntimeException(reflectiveOperationException);

            // Make sure to have non-zero height and width because actual video
            // frames may have not been processed yet.
            component.setComponentSize(
                    DEFAULT_COMPONENT_HEIGHT_OR_WIDTH,
                    DEFAULT_COMPONENT_HEIGHT_OR_WIDTH);
            // XXX The component has not been exposed outside of this instance
            // yet so it seems relatively safe to set its properties outside the
            // AWT event dispatching thread.
            reflectInputFormatOnComponentInEventDispatchThread();
        }
        return component;
    }

    /**
     * Gets the handle to the native counterpart of this <code>JAWTRenderer</code>.
     *
     * @return the handle to the native counterpart of this <code>JAWTRenderer</code>
     */
    public long getHandle() {
        return handle;
    }

    /**
     * Gets the <code>Object</code> which synchronizes the access to the handle to the native
     * counterpart of this <code>JAWTRenderer</code>.
     *
     * @return the <code>Object</code> which synchronizes the access to the handle to the native
     * counterpart of this <code>JAWTRenderer</code>
     */
    public Object getHandleLock() {
        return this;
    }

    /**
     * Gets the human-readable name of this <code>PlugIn</code>.
     *
     * @return the human-readable name of this <code>PlugIn</code>
     */
    @Override
    public String getName() {
        return PLUGIN_NAME;
    }

    /**
     * Gets the list of input <code>Format</code>s supported by this <code>Renderer</code>.
     *
     * @return an array of <code>Format</code> elements which represent the input <code>Format</code>s
     * supported by this <code>Renderer</code>
     */
    @Override
    public Format[] getSupportedInputFormats() {
        return SUPPORTED_INPUT_FORMATS.clone();
    }

    /**
     * Opens this <code>PlugIn</code> and acquires the resources that it needs to operate. The input
     * format of this <code>Renderer</code> has to be set before <code>open</code> is called. Buffers
     * should not be passed into this <code>PlugIn</code> without first calling <code>open</code>.
     *
     * @throws ResourceUnavailableException if there is a problem during opening
     */
    @Override
    public void open()
            throws ResourceUnavailableException {
        boolean addNotify;
        final JComponent component;

        synchronized (this) {
            if (handle == 0) {
                // If this JAWTRenderer gets opened after its visual/video
                // JComponent has been created, send addNotify to the JComponent
                // once this JAWTRenderer gets opened so that the JComponent may
                // use the handle if it needs to.
                addNotify = (this.component != null) && (this.component.getParent() != null);
                component = getComponent();

                handle = open(component);
                if (handle == 0) {
                    throw new ResourceUnavailableException("Failed to open the native JAWTRenderer.");
                }
            }
            else {
                addNotify = false;
                component = null;
            }
        }
        // The #addNotify() invocation, if any, should happen outside the synchronized block in order to avoid a deadlock.
        if (addNotify) {
            SwingUtilities.invokeLater(component::addNotify);
        }
    }

    /**
     * Processes the data provided in a specific <code>Buffer</code> and renders it to the output
     * device represented by this <code>Renderer</code>.
     *
     * @param buffer a <code>Buffer</code> containing the data to be processed and rendered
     *
     * @return <code>BUFFER_PROCESSED_OK</code> if the processing is successful; otherwise, the other
     * possible return codes defined in the <code>PlugIn</code> interface
     */
    @Override
    public synchronized int process(Buffer buffer) {
        if (buffer.isDiscard())
            return BUFFER_PROCESSED_OK;

        int bufferLength = buffer.getLength();
        if (bufferLength == 0)
            return BUFFER_PROCESSED_OK;

        Format format = buffer.getFormat();
        if ((format != null)
                && (format != this.inputFormat)
                && !format.equals(this.inputFormat)
                && (setInputFormat(format) == null)) {
            return BUFFER_PROCESSED_FAILED;
        }

        if (handle == 0)
            return BUFFER_PROCESSED_FAILED;
        else {
            Dimension size = null;

            if (format != null)
                size = ((VideoFormat) format).getSize();
            if (size == null) {
                size = this.inputFormat.getSize();
                if (size == null)
                    return BUFFER_PROCESSED_FAILED;
            }

            // XXX If the size of the video frame to be displayed is tiny enough
            // to crash sws_scale, then it may cause issues with other
            // functionality as well. Stay on the safe side.
            if ((size.width >= SwScale.MIN_SWS_SCALE_HEIGHT_OR_WIDTH)
                    && (size.height >= SwScale.MIN_SWS_SCALE_HEIGHT_OR_WIDTH)) {
                JComponent component = getComponent();
                boolean repaint = process(handle, component, (int[]) buffer.getData(),
                        buffer.getOffset(), bufferLength, size.width, size.height);

                if (repaint)
                    component.repaint();
            }
            return BUFFER_PROCESSED_OK;
        }
    }

    /**
     * Sets properties of the AWT <code>JComponent</code> of this <code>Renderer</code> which depend on the
     * properties of the <code>inputFormat</code> of this <code>Renderer</code>. Makes sure that the
     * procedure is executed on the AWT event dispatching thread because an AWT <code>JComponent</code>'s
     * properties (such as <code>preferredSize</code>) should be accessed in the AWT event dispatching thread.
     */
    private void reflectInputFormatOnComponent() {
        if (SwingUtilities.isEventDispatchThread()) {
            reflectInputFormatOnComponentInEventDispatchThread();
        }
        else {
            SwingUtilities.invokeLater(reflectInputFormatOnComponentInEventDispatchThread);
        }
    }

    /**
     * Sets properties of the AWT <code>JComponent</code> of this <code>Renderer</code> which depend on the
     * properties of the <code>inputFormat</code> of this <code>Renderer</code>. The invocation is presumed
     * to be performed on the AWT event dispatching thread.
     */
    private void reflectInputFormatOnComponentInEventDispatchThread() {
        // Reflect the width and height of the input onto the prefSize of our
        // AWT JComponent (if necessary).
        if ((component != null) && (width > 0) && (height > 0)) {
            Dimension prefSize = component.getPreferredSize();

            // Apart from the simplest of cases in which the component has no
            // prefSize, it is also necessary to reflect the width and height of
            // the input onto the prefSize when the ratio of the input is
            // different than the ratio of the prefSize. It may also be argued
            // that the component needs to know of the width and height of the
            // input if its prefSize is with the same ratio but is smaller.
            if ((prefSize == null) || (prefSize.width < 1) || (prefSize.height < 1)
                    || !VideoLayout.areAspectRatiosEqual(prefSize, width, height)
                    || (prefSize.width < width) || (prefSize.height < height)) {
                component.setPreferredSize(new Dimension(width, height));
            }

            // If the component does not have a size, it looks strange given
            // that we know a prefSize for it. However, if the component has
            // already been added into a Container, the Container will dictate
            // the size as part of its layout logic.
            if (component.isPreferredSizeSet() && (component.getParent() == null)) {
                Dimension size = component.getSize();

                prefSize = component.getPreferredSize();
                if ((size.width < 1) || (size.height < 1)
                        || !VideoLayout.areAspectRatiosEqual(size, prefSize.width,
                        prefSize.height)) {
                    component.setComponentSize(prefSize.width, prefSize.height);
                }
            }
        }
    }

    /**
     * Sets the region in the component of this <code>VideoRenderer</code> where the video is to be
     * rendered. <code>JAWTRenderer</code> always uses the entire component and, consequently, the
     * method does nothing.
     *
     * @param bounds the region in the component of this <code>VideoRenderer</code> where the video is to be
     * rendered; <code>null</code> if the entire component is to be used
     */
    @Override
    public void setBounds(Rect bounds) {
    }

    @Override
    public boolean setComponent(java.awt.Component component) {
        return false;
    }

    /**
     * Sets the AWT <code>JComponent</code> into which this <code>VideoRenderer</code> is to draw.
     * <code>JAWTRenderer</code> cannot draw into any other AWT <code>JComponent</code> but its own so it
     * always returns <code>false</code>.
     *
     * @param component the AWT <code>JComponent</code> into which this <code>VideoRenderer</code> is to draw
     *
     * @return <code>true</code> if this <code>VideoRenderer</code> accepted the specified
     * <code>component</code> as the AWT <code>JComponent</code> into which it is to draw; <code>false</code>, otherwise
     */
    @Override
    public boolean setComponent(Component component) {
        return false;
    }

    /**
     * Sets the <code>Format</code> of the input to be processed by this <code>Renderer</code>.
     *
     * @param format the <code>Format</code> to be set as the <code>Format</code> of the input to be processed by
     * this <code>Renderer</code>
     *
     * @return the <code>Format</code> of the input to be processed by this <code>Renderer</code> if the
     * specified <code>format</code> is supported or <code>null</code> if the specified
     * <code>format</code> is not supported by this <code>Renderer</code>. Typically, it is the
     * supported input <code>Format</code> which most closely matches the specified <code>Format</code>.
     */
    @Override
    public synchronized Format setInputFormat(Format format) {
        VideoFormat oldInputFormat = inputFormat;
        Format newInputFormat = super.setInputFormat(format);

        // Short-circuit because we will be calculating a lot and we do not want
        // to do that unless necessary.
        if (oldInputFormat == inputFormat)
            return newInputFormat;

        Timber.log(TimberLog.FINER, "%s %08x set to input in %s", getClass().getName(), hashCode(), inputFormat);

        // Know the width and height of the input because we'll be depicting it
        // and we may want, for example, to report them as the preferred size of
        // our AWT JComponent. More importantly, know them because they determine
        // certain arguments to be passed to the native counterpart of this
        // JAWTRenderer i.e. handle.
        Dimension size = inputFormat.getSize();

        if (size == null) {
            width = height = 0;
        }
        else {
            width = size.width;
            height = size.height;
        }

        reflectInputFormatOnComponent();

        return newInputFormat;
    }

    /**
     * Starts the rendering process. Begins rendering any data available in the internal buffers of this <code>Renderer</code>.
     */
    @Override
    public void start() {
    }

    /**
     * Stops the rendering process.
     */
    @Override
    public void stop() {
    }
}
