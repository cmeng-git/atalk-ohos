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
package net.java.sip.communicator.service.contactlist.event;

import java.util.EventListener;

/**
 * A MetaContactListListener can be registered with a MetaContactListService so that it will
 * receive any changes that have occurred in the contact list layout.
 *
 * @author Yana Stamcheva
 * @author Emil Ivov
 * @author Eng Chong Meng
 */
public interface MetaContactListListener extends EventListener
{
    /**
     * Indicates that a MetaContact has been successfully added to the MetaContact list.
     *
     * @param evt the MetaContactListEvent containing the corresponding contact
     */
    void metaContactAdded(MetaContactEvent evt);

    /**
     * Indicates that a MetaContact has been renamed.
     *
     * @param evt the MetaContactListEvent containing the corresponding contact
     */
    void metaContactRenamed(MetaContactRenamedEvent evt);

    /**
     * Indicates that a MetaContact has been modified.
     *
     * @param evt the MetaContactModifiedEvent containing the corresponding contact
     */
    void metaContactModified(MetaContactModifiedEvent evt);

    /**
     * Indicates that a MetaContact has been moved inside the MetaContact list.
     *
     * @param evt the MetaContactListEvent containing the corresponding contact
     */
    void metaContactMoved(MetaContactMovedEvent evt);

    /**
     * Indicates that a MetaContact has been removed from the MetaContact list.
     *
     * @param evt the MetaContactListEvent containing the corresponding contact
     */
    void metaContactRemoved(MetaContactEvent evt);

    /**
     * Indicates that a protocol specific <code>Contact</code> instance has been added to the list of
     * protocol specific buddies in this <code>MetaContact</code>
     *
     * @param evt a reference to the corresponding <code>ProtoContactEvent</code>
     */
    void protoContactAdded(ProtoContactEvent evt);

    /**
     * Indicates that one of the protocol specific <code>Contact</code> instances encapsulated by
     * this <code>MetaContact</code> has been modified in some way. The event added to the list of
     * protocol specific buddies in this <code>MetaContact</code>
     *
     * @param evt a reference to the corresponding <code>ProtoContactEvent</code>
     */
    void protoContactModified(ProtoContactEvent evt);

    /**
     * Indicates that a protocol specific <code>Contact</code> instance has been moved from within
     * one <code>MetaContact</code> to another.
     *
     * @param evt a reference to the <code>ProtoContactMovedEvent</code> instance.
     */
    void protoContactMoved(ProtoContactEvent evt);

    /**
     * Indicates that a protocol specific <code>Contact</code> instance has been removed from the
     * list of protocol specific buddies in this <code>MetaContact</code>
     *
     * @param evt a reference to the corresponding <code>ProtoContactEvent</code>
     */
    void protoContactRemoved(ProtoContactEvent evt);

    /**
     * Indicates that a protocol specific <code>Contact</code> instance has been renamed from within
     * list of protocol specific buddies in this <code>MetaContact</code>
     *
     * @param evt a reference to the corresponding <code>ProtoContactEvent</code>
     */
    void protoContactRenamed(ProtoContactEvent evt);

    //-------------------- events on groups. ----------------------------------

    /**
     * Indicates that a MetaContactGroup has been successfully added to the MetaContact list.
     *
     * @param evt the MetaContactListEvent containing the corresponding contact
     */
    void metaContactGroupAdded(MetaContactGroupEvent evt);

    /**
     * Indicates that a MetaContactGroup has been modified (e.g. a proto contact group was removed).
     *
     * @param evt the MetaContactListEvent containing the corresponding contact
     */
    void metaContactGroupModified(MetaContactGroupEvent evt);

    /**
     * Indicates that a MetaContactGroup has been removed from the MetaContact list.
     *
     * @param evt the MetaContactListEvent containing the corresponding contact
     */
    void metaContactGroupRemoved(MetaContactGroupEvent evt);

    /**
     * Indicates that the order under which the child contacts were ordered
     * inside the source group has changed.
     *
     * @param evt the <code>MetaContactGroupEvent</code> containing details of this event.
     */
    void childContactsReordered(MetaContactGroupEvent evt);

    /**
     * Indicates that a new avatar is available for a <code>MetaContact</code>.
     *
     * @param evt the <code>MetaContactAvatarUpdateEvent</code> containing details of this event
     */
    void metaContactAvatarUpdated(MetaContactAvatarUpdateEvent evt);
}
