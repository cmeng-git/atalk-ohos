package org.atalk.ohos.gui.dialogs;

import org.atalk.ohos.ResourceTable;

/**
 * The <code>AttachOptionItem</code> gives list items for optional attachments.
 *
 * @author Eng Chong Meng
 */
public enum AttachOptionItem {
    pic(ResourceTable.String_attach_picture, ResourceTable.Media_ic_attach_photo),
    video(ResourceTable.String_attach_video, ResourceTable.Media_ic_attach_video),
    camera(ResourceTable.String_attach_take_picture, ResourceTable.Media_ic_attach_camera),
    video_record(ResourceTable.String_attach_record_video, ResourceTable.Media_ic_attach_video_record),
    // audio_record(ResourceTable.String_attachOptionDialog_audioRecord, ResourceTable.Media_ic_action_audio_record),
    // share_contact(ResourceTable.String_attachOptionDialog_shareContact, ResourceTable.Media_ic_attach_contact),
    share_file(ResourceTable.String_attach_file, ResourceTable.Media_ic_attach_file);

    private final int mIconId;
    private final int mTextId;

    AttachOptionItem(int textId, int iconId) {
        this.mTextId = textId;
        this.mIconId = iconId;
    }

    public int getTextId() {
        return mTextId;
    }

    public int getIconId() {
        return mIconId;
    }
}
