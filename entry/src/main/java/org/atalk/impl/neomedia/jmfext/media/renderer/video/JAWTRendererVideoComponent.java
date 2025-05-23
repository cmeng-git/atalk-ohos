/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package org.atalk.impl.neomedia.jmfext.media.renderer.video;

import ohos.agp.render.Canvas;
import ohos.agp.render.Paint;

import org.atalk.impl.neomedia.codec.video.SwScale;
import org.atalk.util.OSUtils;
import org.atalk.ohos.agp.components.JComponent;

/**
 * Implements an AWT <code>JComponent</code> in which <code>JAWTRenderer</code> paints.
 *
 * @author Eng Chong Meng
 */
public class JAWTRendererVideoComponent extends Canvas {
    /**
     * The serial version UID of the <code>JAWTRendererVideoComponent</code> class defined to silence a
     * serialization compile-time warning.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The <code>JAWTRenderer</code> which paints in this <code>JAWTRendererVideoComponent</code>.
     */
    protected final JAWTRenderer renderer;

    /**
     * The indicator which determines whether the native counterpart of this <code>JAWTRenderer</code>
     * wants <code>paint</code> calls on its AWT <code>JComponent</code> to be delivered. For example, after
     * the native counterpart has been able to acquire the native handle of the AWT
     * <code>JComponent</code>, it may be able to determine when the native handle needs painting
     * without waiting for AWT to call <code>paint</code> on the <code>JComponent</code>. In such a
     * scenario, the native counterpart may indicate with <code>false</code> that it does not need
     * further <code>paint</code> deliveries.
     */
    private boolean wantsPaint = true;

    /**
     * Initializes a new <code>JAWTRendererVideoComponent</code> instance.
     *
     * @param renderer
     */
    public JAWTRendererVideoComponent(JAWTRenderer renderer) {
        this.renderer = renderer;
    }

    /**
     * Overrides {@link JComponent#addNotify()} to reset the indicator which determines whether the
     * native counterpart of this <code>JAWTRenderer</code> wants <code>paint</code> calls on its AWT
     * <code>JComponent</code> to be delivered.
     */
    @Override
    public void addNotify() {
        super.addNotify();
        wantsPaint = true;

        synchronized (getHandleLock()) {
            long handle;

            if ((handle = getHandle()) != 0) {
                try {
                    JAWTRenderer.addNotify(handle, this);
                } catch (UnsatisfiedLinkError uler) {
                    // The function/method has been introduced in a revision of the JAWTRenderer
                    // API and may not be available in the binary.
                }
                // The first task of the method paint(Graphics) is to attach to the native
                // view/widget/window of this Canvas. The sooner, the better. Technically, it
                // should be possible to do it immediately after the method addNotify().
                try {
                    drawPaint(null);
                } finally {
                    // Well, we explicitly invoked the method paint(Graphics) which is kind of extraordinary.
                    wantsPaint = true;
                }
            }
        }
    }

    /**
     * Gets the handle of the native counterpart of the <code>JAWTRenderer</code> which paints in this
     * <code>AWTVideoComponent</code>.
     *
     * @return the handle of the native counterpart of the <code>JAWTRenderer</code> which paints in
     * this <code>AWTVideoComponent</code>
     */
    protected long getHandle() {
        return renderer.getHandle();
    }

    /**
     * Gets the synchronization lock which protects the access to the <code>handle</code> property of
     * this <code>AWTVideoComponent</code>.
     *
     * @return the synchronization lock which protects the access to the <code>handle</code>
     * property of this <code>AWTVideoComponent</code>
     */
    protected Object getHandleLock() {
        return renderer.getHandleLock();
    }

    /**
     * Overrides {@link Canvas#drawPaint(Paint)} to paint this <code>JComponent</code> in the native
     * counterpart of its associated <code>JAWTRenderer</code>.
     */
    @Override
    public void drawPaint(Paint g) {
        // XXX If the size of this JComponent is tiny enough to crash sws_scale, then it may cause
        // issues with other functionality as well. Stay on the safe side.
        if (wantsPaint
                && getWidth() >= SwScale.MIN_SWS_SCALE_HEIGHT_OR_WIDTH
                && getHeight() >= SwScale.MIN_SWS_SCALE_HEIGHT_OR_WIDTH) {
            synchronized (getHandleLock()) {
                long handle;

                if ((handle = getHandle()) != 0) {
                    ComponentContainer parent = getParent();
                    int zOrder;

                    if (parent == null) {
                        zOrder = -1;
                    }
                    else {
                        zOrder = parent.getComponentZOrder(this);
                        // CALayer is used in the implementation of JAWTRenderer
                        // on OS X and its zPosition is the reverse of AWT's
                        // componentZOrder (in terms of what appears above and bellow).
                        if (OSUtils.IS_MAC && (zOrder != -1))
                            zOrder = parent.getComponentCount() - 1 - zOrder;
                    }

                    wantsPaint = JAWTRenderer.paint(handle, this, g, zOrder);
                }
            }
        }
    }

    /**
     * Overrides {@link JComponent#removeNotify()} to reset the indicator which determines whether
     * the native counterpart of this <code>JAWTRenderer</code> wants <code>paint</code> calls on its AWT
     * <code>JComponent</code> to be delivered.
     */
    @Override
    public void removeNotify() {
        synchronized (getHandleLock()) {
            long handle;

            if ((handle = getHandle()) != 0) {
                try {
                    JAWTRenderer.removeNotify(handle, this);
                } catch (UnsatisfiedLinkError uler) {
                    // The function/method has been introduced in a revision of
                    // the JAWTRenderer API and may not be available in the binary.
                }
            }
        }

        // In case the associated JAWTRenderer has said that it does not want paint
        // events/notifications, ask it again next time because the native
        // handle of this Canvas may be recreated.
        wantsPaint = true;
        super.removeNotify();
    }

    /**
     * Overrides {@link Canvas#update(Graphics)} to skip the filling with the background color in
     * order to prevent flickering.
     */
    @Override
    public void update(Graphics g) {
        synchronized (getHandleLock()) {
            if (!wantsPaint || getHandle() == 0) {
                super.update(g);
                return;
            }
        }

        // Skip the filling with the background color because it causes flickering.
        paint(g);
    }
}
