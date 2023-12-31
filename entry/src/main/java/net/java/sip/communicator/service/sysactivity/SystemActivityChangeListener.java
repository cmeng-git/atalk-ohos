/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
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
package net.java.sip.communicator.service.sysactivity;

import net.java.sip.communicator.service.sysactivity.event.SystemActivityEvent;

import java.util.EventListener;

/**
 * The <code>SystemActivityChangeListener</code> is notified any time an event
 * in the operating system occurs.
 *
 * @author Damian Minkov
 * @author Eng Chong Meng
 */
public interface SystemActivityChangeListener extends EventListener
{
    /**
     * This method gets called when a notification action for a particular event
     * type has been changed (for example the corresponding descriptor has
     * changed).
     *
     * @param event the <code>NotificationActionTypeEvent</code>, which is
     * dispatched when an action has been changed.
     */
    void activityChanged(SystemActivityEvent event);
}
