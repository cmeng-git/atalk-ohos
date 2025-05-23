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

package org.atalk.ohos.gui.share;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import ohos.agp.components.BaseItemProvider;
import ohos.agp.components.Component;
import ohos.agp.components.ComponentContainer;
import ohos.agp.components.DirectionalLayout;
import ohos.agp.components.Image;
import ohos.agp.components.LayoutScatter;

import org.atalk.ohos.MyGlideApp;
import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.aTalkApp;
import org.atalk.ohos.gui.chat.ChatAbility;
import org.atalk.persistance.FilePathHelper;

public class MediaPreviewProvider extends BaseItemProvider { //<MediaPreviewProvider.MediaPreviewViewHolder> {
    private final ArrayList<Attachment> mediaPreviews = new ArrayList<>();

    private final ChatAbility mChatAbility;

    private final Image viewHolder;
    private final DirectionalLayout.LayoutConfig layoutConfig;

    public MediaPreviewProvider(ChatAbility fragment, Image imgPreview) {
        mChatAbility = fragment;
        viewHolder = imgPreview;
        int width = aTalkApp.mDisplaySize.width;
        int height = aTalkApp.mDisplaySize.height;
        layoutConfig = new DirectionalLayout.LayoutConfig(width, height);
    }

    @Override
    public MediaPreviewViewHolder onCreateViewHolder(ComponentContainer parent, int viewType) {
        LayoutScatter layoutScatter = LayoutScatter.getInstance(parent.getContext());
        MediaPreviewBinding binding = DataBinding.parse(layoutScatter, ResourceTable.Layout_media_preview, parent, false);
        return new MediaPreviewViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(MediaPreviewViewHolder holder, int position) {
        final Attachment attachment = mediaPreviews.get(position);
        final File file = new File(FilePathHelper.getFilePath(mChatAbility, attachment));
        MyGlideApp.loadImage(holder.binding.mediaPreviewItem, file, true);

        holder.binding.deleteButton.setClickedListener(v ->
        {
            final int pos = mediaPreviews.indexOf(attachment);
            mediaPreviews.remove(pos);
            notifyItemRemoved(pos);

            // update send button mode
            if (mediaPreviews.isEmpty())
                mChatAbility.toggleInputMethod();
        });

        holder.binding.mediaPreviewItem.setClickedListener(v -> {
            viewHolder.setLayoutParams(layoutConfig);
            MyGlideApp.loadImage(viewHolder, file, true);
        });
    }

    public void addMediaPreviews(List<Attachment> attachments) {
        // mediaPreviews.clear(); // Do not remove any existing attachments in the mediaPreviews
        mediaPreviews.addAll(attachments);
        notifyDataChanged();
    }

    @Override
    public int getItemCount() {
        return mediaPreviews.size();
    }

    public boolean hasAttachments() {
        return !mediaPreviews.isEmpty();
    }

    public ArrayList<Attachment> getAttachments() {
        return mediaPreviews;
    }

    public void clearPreviews() {
        this.mediaPreviews.clear();
    }

    @Override
    public int getCount() {
        return 0;
    }

    @Override
    public Object getItem(int i) {
        return null;
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public Component getComponent(int i, Component component, ComponentContainer componentContainer) {
        return null;
    }

    static class MediaPreviewViewHolder extends ComponentContainer {
        private final MediaPreviewBinding binding;

        MediaPreviewViewHolder(MediaPreviewBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
