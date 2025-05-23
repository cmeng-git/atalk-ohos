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
package org.atalk.ohos.gui.account.settings;

import ohos.aafwk.ability.Ability;
import ohos.agp.components.Component;
import ohos.agp.components.ComponentContainer;
import ohos.agp.components.LayoutScatter;
import ohos.agp.components.Text;

import net.java.sip.communicator.service.protocol.JingleNodeDescriptor;
import net.java.sip.communicator.service.protocol.jabber.JabberAccountRegistration;

import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.gui.dialogs.DialogA;

/**
 * Implements list model for Jingle Nodes list of {@link JabberAccountRegistration}.
 *
 * @see ServerItemProvider
 */
public class JingleNodeProvider extends ServerItemProvider {
    /**
     * The {@link JabberAccountRegistration} object that contains Jingle Nodes
     */
    private final JabberAccountRegistration registration;

    /**
     * Creates new instance of {@link JingleNodeProvider}
     *
     * @param parent the parent {@link Ability} used a a context
     * @param registration the registration object that contains Jingle Nodes
     */
    public JingleNodeProvider(Ability parent, JabberAccountRegistration registration) {
        super(parent);
        this.registration = registration;
    }

    public int getCount() {
        return registration.getAdditionalJingleNodes().size();
    }

    public Object getItem(int i) {
        return registration.getAdditionalJingleNodes().get(i);
    }

    /**
     * Creates the dialog fragment that will allow user to edit Jingle Node
     *
     * @param position the position of item to edit
     *
     * @return the Jingle Node edit dialog
     */
    DialogA createItemEditDialog(int position) {
        if (position < 0)
            return new JingleNodeDialog(mContext, this, null).create();
        else
            return new JingleNodeDialog(mContext, this, (JingleNodeDescriptor) getItem(position)).create();
    }

    public Component getComponent(int i, Component view, ComponentContainer viewGroup) {
        LayoutScatter li = LayoutScatter.getInstance(mContext);
        // Component rowView = li.parse(ResourceTable.Layout_server_list_row, viewGroup, false);
        Component component = li.parse(ResourceTable.Layout_simple_list_item, viewGroup, false);
        Text tv = component.findComponentById(ResourceTable.Id_text1);

        JingleNodeDescriptor node = (JingleNodeDescriptor) getItem(i);
        tv.setText(node.getJID() + (node.isRelaySupported() ? " (+Relay support)" : ""));

        return component;
    }

    /**
     * Removes the Jingle Node from the list
     *
     * @param descriptor Jingle Node that shall be removed
     */
    void removeJingleNode(JingleNodeDescriptor descriptor) {
        registration.getAdditionalJingleNodes().remove(descriptor);
        refresh();
    }

    /**
     * Adds new Jingle node to the list
     *
     * @param descriptor the {@link JingleNodeDescriptor} that will be included in this adapter
     */
    void addJingleNode(JingleNodeDescriptor descriptor) {
        registration.addJingleNodes(descriptor);
        refresh();
    }

    /**
     * Updates given Jingle Node
     *
     * @param descriptor the JingleNode that will be updated
     */
    void updateJingleNode(JingleNodeDescriptor descriptor) {
        refresh();
    }
}
