/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package org.atalk.ohos.gui.account.settings;

import ohos.aafwk.ability.Ability;
import ohos.agp.components.Component;
import ohos.agp.components.ComponentContainer;
import ohos.agp.components.LayoutScatter;
import ohos.agp.components.Text;

import net.java.sip.communicator.service.protocol.StunServerDescriptor;
import net.java.sip.communicator.service.protocol.jabber.JabberAccountRegistration;

import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.aTalkApp;
import org.atalk.ohos.gui.dialogs.DialogA;

/**
 * List model for STUN servers. Is used to edit STUN servers preferences of Jabber account. It's also responsible for
 * creating list row <code>Component.</code>s and implements {@link ServerItemProvider#createItemEditDialog(int)} to
 * provide item edit dialog.
 *
 * @author Eng Chong Meng
 * See {@link ServerListAbility}
 */
public class StunServerProvider extends ServerItemProvider {
    /**
     * The {@link JabberAccountRegistration} that contains the original list
     */
    protected final JabberAccountRegistration registration;

    /**
     * Creates new instance of {@link StunServerProvider}
     *
     * @param parent the parent {@link Ability} used as a context
     * @param registration the registration object that holds the STUN server list
     */
    public StunServerProvider(Ability parent, JabberAccountRegistration registration) {
        super(parent);
        this.registration = registration;
    }

    public int getCount() {
        return registration.getAdditionalStunServers().size();
    }

    public Object getItem(int i) {
        return registration.getAdditionalStunServers().get(i);
    }

    public Component getComponent(int i, Component view, ComponentContainer viewGroup) {
        LayoutScatter li = LayoutScatter.getInstance(mContext);
        Component component = li.parse(ResourceTable.Layout_simple_list_item, viewGroup, false);
        Text tv = component.findComponentById(ResourceTable.Id_text1);

        StunServerDescriptor server = (StunServerDescriptor) getItem(i);
        String descriptor = aTalkApp.getResString(ResourceTable.String_server_stun_descriptor,
                server.getAddress(), server.getPort(), (server.isTurnSupported() ? "(+TURN)" : ""));
        tv.setText(descriptor);

        return component;
    }

    /**
     * Removes the server from the list.
     *
     * @param descriptor the server descriptor to be removed
     */
    void removeServer(StunServerDescriptor descriptor) {
        registration.getAdditionalStunServers().remove(descriptor);
        refresh();
    }

    /**
     * Add new STUN server descriptor to the list
     *
     * @param descriptor the server descriptor
     */
    void addServer(StunServerDescriptor descriptor) {
        registration.addStunServer(descriptor);
        refresh();
    }

    /**
     * Updates given server description
     *
     * @param descriptor the server to be updated
     */
    void updateServer(StunServerDescriptor descriptor) {
        refresh();
    }

    DialogA createItemEditDialog(int position) {
        if (position < 0)
            return new StunTurnDialog(mContext,this, null).create();
        else
            return new StunTurnDialog(mContext,this, (StunServerDescriptor) getItem(position)).create();
    }
}
