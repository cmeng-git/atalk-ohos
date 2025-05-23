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
package org.atalk.service.neomedia;

import ohos.agp.components.surfaceprovider.SurfaceProvider;
import ohos.app.Context;

import org.atalk.ohos.agp.components.JComponent;

/**
 * Declares the interface to be supported by providers of access to JComponent.
 *
 * @author Eng Chong Meng
 */
public interface ViewAccessor {
    /**
     * Gets the JComponent provided by this instance which is to be used in a specific {@link Context}.
     *
     * @param context the <code>Context</code> in which the provided <code>JComponent</code> will be used
     *
     * @return the <code>JComponent</code> provided by this instance which is to be used in a specific <code>Context</code>
     */
    SurfaceProvider getComponent(Context context);
}