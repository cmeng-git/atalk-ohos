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
package org.atalk.ohos.gui.contactlist;

import ohos.agp.components.Component;
import ohos.agp.components.LayoutScatter;
import ohos.app.Context;

import net.java.sip.communicator.service.contactlist.MetaContactGroup;
import net.java.sip.communicator.service.contactlist.MetaContactListException;
import net.java.sip.communicator.service.contactlist.MetaContactListService;

import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.aTalkApp;
import org.atalk.ohos.gui.AppGUIActivator;
import org.atalk.ohos.gui.dialogs.DialogH;
import org.atalk.ohos.gui.util.event.EventListener;
import org.atalk.ohos.util.ComponentUtil;

import timber.log.Timber;

/**
 * Dialog allowing user to create new contact group.
 *
 * @author Eng Chong Meng
 */
public class AddGroupDialog extends Component {
    private final Context mContext;
    private final Component mDialog;

    /**
     * Displays create contact group dialog. If the source wants to be notified about the result
     * should pass the listener here or <code>null</code> otherwise.
     *
     * @param context the parent <code>Ability</code>
     */
    public AddGroupDialog(Context context) {
        super(context);
        mContext = context;

        LayoutScatter inflater = LayoutScatter.getInstance(getContext());
        mDialog = inflater.parse(ResourceTable.Layout_create_group, null, false);
    }

    /**
     * Displays create contact group dialog. If the source wants to be notified about the result
     * should pass the listener here or <code>null</code> otherwise.
     *
     * @param createListener listener for contact group created event that will receive newly created instance of
     * the contact group or <code>null</code> in case user cancels the dialog.
     */
    public void show(EventListener<MetaContactGroup> createListener) {
        DialogH.getInstance(mContext).showCustomDialog(mContext,
                mContext.getString(ResourceTable.String_create_group),
                mDialog, mContext.getString(ResourceTable.String_create),
                new DialogListenerImpl(createListener), null);
    }

    /**
     * Implements <code>DialogH.DialogListener</code> interface and handles contact group creation process.
     */
    class DialogListenerImpl implements DialogH.DialogListener {
        /**
         * Contact created event listener.
         */
        private final EventListener<MetaContactGroup> listener;

        /**
         * Newly created contact group.
         */
        private MetaContactGroup newMetaGroup;

        /**
         * Thread that runs create group process.
         */
        private Thread createThread;

        /**
         * Creates new instance of <code>DialogListenerImpl</code>.
         *
         * @param createListener create group listener if any.
         */
        public DialogListenerImpl(EventListener<MetaContactGroup> createListener) {
            this.listener = createListener;
        }

        // private ProgressDialog progressDialog;

        @Override
        public boolean onConfirmClicked(DialogH dialog) {
            if (createThread != null)
                return false;

            String groupName = ComponentUtil.toString(mDialog.findComponentById(ResourceTable.Id_editText));
            if (groupName == null) {
                showErrorMessage(mContext.getString(ResourceTable.String_add_group_error_empty_name));
                return false;
            }
            else {
                // TODO: in progress dialog removed for simplicity
                // Add it here if operation will be taking too much time (seems to finish fast for now)
                // displayOperationInProgressDialog(dialog);

                this.createThread = new CreateGroup(AppGUIActivator.getContactListService(), groupName);
                createThread.start();

                try {
                    // Wait for create group thread to finish
                    createThread.join();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                if (listener != null)
                    listener.onChangeEvent(newMetaGroup);

                return true;
            }
        }

        /**
         * Shows given error message as an alert.
         *
         * @param errorMessage the error message to show.
         */
        private void showErrorMessage(String errorMessage) {
            Context ctx = aTalkApp.getInstance();
            DialogH.getInstance(ctx).showDialog(ctx, ctx.getString(ResourceTable.String_error), errorMessage);
        }

        @Override
        public void onDialogCancelled(DialogH dialog) {
            dialog.destroy();
        }

        /**
         * Creates a new meta contact group in a separate thread.
         */
        private class CreateGroup extends Thread {
            /**
             * Contact list instance.
             */
            MetaContactListService mcl;

            /**
             * Name of the contact group to create.
             */
            String groupName;

            /**
             * Creates new instance of <code>AddGroupDialog</code>.
             *
             * @param mcl contact list service instance.
             * @param groupName name of the contact group to create.
             */
            CreateGroup(MetaContactListService mcl, String groupName) {
                this.mcl = mcl;
                this.groupName = groupName;
            }

            @Override
            public void run() {
                try {
                    newMetaGroup = mcl.createMetaContactGroup(mcl.getRoot(), groupName);
                } catch (MetaContactListException ex) {
                    Timber.e(ex);
                    Context ctx = aTalkApp.getInstance();

                    int errorCode = ex.getErrorCode();

                    if (errorCode == MetaContactListException.CODE_GROUP_ALREADY_EXISTS_ERROR) {
                        showErrorMessage(ctx.getString(ResourceTable.String_add_group_error_exist,
                                groupName));
                    }
                    else if (errorCode == MetaContactListException.CODE_LOCAL_IO_ERROR) {
                        showErrorMessage(ctx.getString(ResourceTable.String_add_group_error_local,
                                groupName));
                    }
                    else if (errorCode == MetaContactListException.CODE_NETWORK_ERROR) {
                        showErrorMessage(ctx.getString(ResourceTable.String_add_group_error_network,
                                groupName));
                    }
                    else {
                        showErrorMessage(ctx.getString(ResourceTable.String_add_group_failed,
                                groupName));
                    }
                }
                /*
                 * finally { hideOperationInProgressDialog(); }
                 */
            }
        }
    }
}
