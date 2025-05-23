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
package org.atalk.util.event;

import java.util.EventObject;

import ohos.agp.components.ComponentContainer;

import org.atalk.ohos.agp.components.JComponent;

public class ContainerEvent extends EventObject {
    private static final long serialVersionUID = -4114942250539772041L;
   public static final int COMPONENT_ADDED = 300;
    public static final int COMPONENT_REMOVED = 301;

    private final int mId;
    private final JComponent mChild;

    public ContainerEvent(JComponent source, int id, JComponent child) {
        super(source);
        mId = id;
        mChild = child;
    }

    public ComponentContainer getContainer() {
        return (ComponentContainer) source;
    }

    public int getId() {
        return mId;
    }
    public JComponent getChild() {
        return mChild;
    }
}
