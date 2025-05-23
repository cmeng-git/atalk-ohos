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
package org.atalk.impl.neomedia.jmfext.media.renderer.video;

import ohos.agp.components.Component;
import ohos.agp.components.surfaceprovider.SurfaceProvider;
import ohos.agp.render.opengl.EGLSurface;
import ohos.app.Context;

import org.atalk.impl.timberlog.TimberLog;
import org.atalk.service.neomedia.ViewAccessor;
import org.atalk.ohos.agp.components.JComponent;

/**
 * Implements <code>JAWTRendererAndroidVideoComponent</code> for <code>JAWTRenderer</code> on device using a {@link SurfaceProvider}.
 *
 * @author Eng Chong Meng
 */
public class JAWTRendererAndroidVideoComponent extends Component implements ViewAccessor {
    /**
     * The <code>SurfaceProvider</code> is the actual visual counterpart of this <code>java.awt.JComponent</code>.
     */
    private SurfaceProvider surfaceProvider;

    /**
     * Initializes a new <code>JAWTRendererAndroidVideoComponent</code> which is to be the visual
     * <code>JComponent</code> of a specific <code>JAWTRenderer</code>.
     *
     * @param renderer the <code>JAWTRenderer</code> which is to use the new instance as its visual <code>JComponent</code>
     */
    public JAWTRendererAndroidVideoComponent(JAWTRenderer renderer) {
        super();
        this.renderer = renderer;
    }

    /**
     * Implements {@link ViewAccessor#getComponent(Context)}. Gets the {@link JComponent} provided by this
     * instance which is to be used in a specific {@link Context}.
     *
     * @param context the <code>Context</code> in which the provided <code>JComponent</code> will be used
     *
     * @return the <code>JComponent</code> provided by this instance which is to be used in a specific <code>Context</code>
     *
     * @see ViewAccessor#getComponent(Context)
     */
    public synchronized SurfaceProvider getComponent(Context context) {
        if ((surfaceProvider == null) && (context != null)) {
            surfaceProvider = new SurfaceProvider(context);
        }
        return surfaceProvider;
    }
}
