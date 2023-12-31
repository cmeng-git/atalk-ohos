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

import net.java.sip.communicator.service.contactlist.MetaContact;
import net.java.sip.communicator.service.contactlist.MetaContactGroup;

/**
 * Fired whenever a meta contact has been moved from one parent group to another. The event
 * contains the old and new parents as well as a reference to the source contact.
 *
 * @author Emil Ivov
 */
public class MetaContactMovedEvent extends MetaContactPropertyChangeEvent
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * Create as an instance of this <code>MetaContactMovedEvent</code> using the specified arguments.
     *
     * @param sourceContact a reference to the <code>MetaContact</code> that this
     * event is about.
     * @param oldParent a reference to the <code>MetaContactGroup</code> that contained <code>sourceContact</code>
     * before it was moved.
     * @param newParent a reference to the <code>MetaContactGroup</code> that contains <code>sourceContact</code>
     * after it was moved.
     */
    public MetaContactMovedEvent(MetaContact sourceContact, MetaContactGroup oldParent, MetaContactGroup newParent)
    {
        super(sourceContact, META_CONTACT_MOVED, oldParent, newParent);
    }

    /**
     * Returns the old parent of this meta contact.
     *
     * @return a reference to the <code>MetaContactGroup</code> that contained the source meta
     * contact before it was moved.
     */
    public MetaContactGroup getOldParent()
    {
        return (MetaContactGroup) getOldValue();
    }

    /**
     * Returns the new parent of this meta contact.
     *
     * @return a reference to the <code>MetaContactGroup</code> that contains the source meta contact
     * after it was moved.
     */
    public MetaContactGroup getNewParent()
    {
        return (MetaContactGroup) getNewValue();
    }
}
