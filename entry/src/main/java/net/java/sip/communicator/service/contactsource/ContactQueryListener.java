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
package net.java.sip.communicator.service.contactsource;

/**
 * The <code>ContactQueryListener</code> notifies interested parties of any change
 * in a <code>ContactQuery</code>, e.g. when a new contact has been received or a
 * the query status has changed.
 *
 * @author Yana Stamcheva
 * @author Eng Chong Meng
 */
public interface ContactQueryListener
{
    /**
     * Indicates that a new contact has been received for a search.
     * @param event the <code>ContactQueryEvent</code> containing information
     * about the received <code>SourceContact</code>
     */
    void contactReceived(ContactReceivedEvent event);

    /**
     * Indicates that the status of a search has been changed.
     * @param event the <code>ContactQueryStatusEvent</code> containing information
     * about the status change
     */
    void queryStatusChanged(ContactQueryStatusEvent event);

    /**
     * Indicates that a contact has been removed after a search.
     * @param event the <code>ContactQueryEvent</code> containing information
     * about the received <code>SourceContact</code>
     */
    void contactRemoved(ContactRemovedEvent event);

    /**
     * Indicates that a contact has been updated after a search.
     * @param event the <code>ContactQueryEvent</code> containing information
     * about the updated <code>SourceContact</code>
     */
    void contactChanged(ContactChangedEvent event);
}
