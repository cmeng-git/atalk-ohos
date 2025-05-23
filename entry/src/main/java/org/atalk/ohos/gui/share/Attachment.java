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
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import ohos.aafwk.content.Intent;
import ohos.app.Context;
import ohos.miscservices.pasteboard.SystemPasteboard;
import ohos.utils.Parcel;
import ohos.utils.net.Uri;

import org.atalk.persistance.FileBackend;

import timber.log.Timber;

public class Attachment extends Parcel {
    private final Uri uri;
    private final Type type;
    private final UUID uuid;
    private final String mime;

    Attachment(Parcel in) {
        uri = (Uri) in.readParcelableEx(Uri.class.getClassLoader());
        mime = in.readString();
        uuid = UUID.fromString(in.readString());
        type = Type.valueOf(in.readString());
    }

//    @Override
//    public void writeParcelableEx(ParcelableEx dest) {
//        dest.writeParcelableEx((ParcelableEx) uri);
//        dest.writeString(mime);
//        dest.writeString(uuid.toString());
//        dest.writeString(type.toString());
//    }

    public String getMime() {
        return mime;
    }

    public Type getType() {
        return type;
    }

    public enum Type {
        FILE, IMAGE, LOCATION, RECORDING
    }

    private Attachment(UUID uuid, Uri uri, Type type, String mime) {
        this.uri = uri;
        this.type = type;
        this.mime = mime;
        this.uuid = uuid;
    }

    private Attachment(Uri uri, Type type, String mime) {
        this.uri = uri;
        this.type = type;
        this.mime = mime;
        this.uuid = UUID.randomUUID();
    }

    public static boolean canBeSendInband(final List<Attachment> attachments) {
        for (Attachment attachment : attachments) {
            if (attachment.type != Type.LOCATION) {
                return false;
            }
        }
        return true;
    }

    public static List<Attachment> of(final Context context, Uri uri, Type type) {
        final String mime = type == Type.LOCATION ? null : FileBackend.getMimeType(context, uri);
        return Collections.singletonList(new Attachment(uri, type, mime));
    }

    public static List<Attachment> of(final Context context, List<Uri> uris) {
        List<Attachment> attachments = new ArrayList<>();
        for (Uri uri : uris) {
            final String mime = FileBackend.getMimeType(context, uri);
            attachments.add(new Attachment(uri, mime != null && mime.startsWith("image/") ? Type.IMAGE : Type.FILE, mime));
        }
        return attachments;
    }

    public static Attachment of(UUID uuid, final File file, String mime) {
        return new Attachment(uuid, Uri.getUriFromFile(file), mime != null && (mime.startsWith("image/") || mime.startsWith("video/")) ? Type.IMAGE : Type.FILE, mime);
    }

    public static List<Attachment> extractAttachments(final Context context, final Intent intent, Type type) {
        List<Attachment> attachments = new ArrayList<>();
        if (intent == null) {
            return attachments;
        }

        final String contentType = intent.getType();
        final Uri data = intent.getData();
        if (data == null) {
            final SystemPasteboard clipData =  intent.getPgetgetClipData();
            if (clipData != null) {
                for (int i = 0; i < clipData.getItemCount(); ++i) {
                    final Uri uri = clipData.getItemAt(i).getUri();
                    final String mime = FileBackend.getMimeType(context, uri, contentType);
                    Timber.d("uri = %s; contentType = %s; mime = %s", uri, contentType, mime);
                    attachments.add(new Attachment(uri, type, mime));
                }
            }
        }
        else {
            // final String mime = MimeUtils.guessMimeTypeFromUriAndMime(context, data, contentType);
            String mime = FileBackend.getMimeType(context, data, contentType);
            attachments.add(new Attachment(data, type, mime));
        }
        return attachments;
    }

    public boolean renderThumbnail() {
        return type == Type.IMAGE || (type == Type.FILE && mime != null && (mime.startsWith("video/") || mime.startsWith("image/")));
    }

    public Uri getUri() {
        return uri;
    }

    public UUID getUuid() {
        return uuid;
    }
}
