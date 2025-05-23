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

import ohos.agp.components.Checkbox;
import ohos.agp.components.Component;
import ohos.agp.components.LayoutScatter;
import ohos.agp.components.Text;
import ohos.app.Context;

import net.java.sip.communicator.service.protocol.JingleNodeDescriptor;

import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.aTalkApp;
import org.atalk.ohos.gui.dialogs.DialogA;
import org.atalk.ohos.util.ComponentUtil;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

/**
 * The Jingle Node edit dialog. It used to edit or create new {@link JingleNodeDescriptor}.
 * It serves as a "create new" dialog when <code>null</code> is passed as a descriptor argument.
 *
 * @author Eng Chong Meng
 */
public class JingleNodeDialog {
    private final Context mContext;

    /**
     * Parent {@link JingleNodeProvider} that will be notified about any change to the Jingle Node
     */
    private final JingleNodeProvider mListener;

    /**
     * Edited Jingle Node descriptor
     */
    private JingleNodeDescriptor mDescriptor;

    /**
     * Creates new instance of {@link JingleNodeDialog}
     *
     * @param listener parent {@link JingleNodeProvider}
     * @param descriptor the {@link JingleNodeDescriptor} to edit or <code>null</code> if a new node shall be created
     */
    public JingleNodeDialog(Context context, JingleNodeProvider listener, JingleNodeDescriptor descriptor) {
        if (listener == null)
            throw new NullPointerException();

        mContext = context;
        mListener = listener;
        mDescriptor = descriptor;
    }

    public DialogA create() {
        LayoutScatter inflater = LayoutScatter.getInstance(mContext);
        Component component = inflater.parse(ResourceTable.Layout_jingle_node_dialog, null, false);

        // Builds the dialog
        DialogA.Builder builder = new DialogA.Builder(mContext);
        builder = builder.setTitle(ResourceTable.String_jbr_jingle_nodes);
        builder = builder.setComponent(component)
                .setNeutralButton(ResourceTable.String_cancel, null)
                .setPositiveButton(ResourceTable.String_save, dialog -> {
                    if (saveChanges(component))
                        dialog.remove();
                });

        if (mDescriptor != null) {
            Text jidAdrTextView = component.findComponentById(ResourceTable.Id_jidAddress);
            jidAdrTextView.setText(mDescriptor.getJID().toString());

            Checkbox useRelayCbox = component.findComponentById(ResourceTable.Id_relaySupportCheckbox);
            useRelayCbox.setChecked(mDescriptor.isRelaySupported());

            // Add remove button if it''s not "create new" dialog
            builder = builder.setNegativeButton(ResourceTable.String_remove, dialog -> {
                mListener.removeJingleNode(mDescriptor);
                dialog.remove();
            });
        }
        return builder.create();
    }

    /**
     * Saves the changes if all data is correct
     *
     * @return <code>true</code> if all data is correct and changes have been stored in descriptor
     */
    boolean saveChanges(Component dialog) {
        boolean relaySupport = ((Checkbox) dialog.findComponentById(ResourceTable.Id_relaySupportCheckbox)).isChecked();
        String jingleAddress = ComponentUtil.toString(dialog.findComponentById(ResourceTable.Id_jidAddress));

        if (jingleAddress == null) {
            aTalkApp.showToastMessage("The Jid address can not be empty");
            return false;
        }

        Jid jidAddress = null;
        try {
            jidAddress = JidCreate.from(jingleAddress);
        } catch (XmppStringprepException | IllegalArgumentException e) {
            e.printStackTrace();
        }

        if (mDescriptor == null) {
            // Create new descriptor
            mDescriptor = new JingleNodeDescriptor(jidAddress, relaySupport);
            mListener.addJingleNode(mDescriptor);
        }
        else {
            mDescriptor.setAddress(jidAddress);
            mDescriptor.setRelay(relaySupport);
            mListener.updateJingleNode(mDescriptor);
        }
        return true;
    }
}
