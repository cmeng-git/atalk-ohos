/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package org.atalk.impl.neomedia.recording;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import ohos.utils.zson.ZSONObject;

import org.atalk.service.neomedia.recording.RecorderEvent;
import org.atalk.service.neomedia.recording.RecorderEventHandler;
import org.atalk.util.MediaType;

import timber.log.Timber;

/**
 * Implements a <code>RecorderEventHandler</code> which handles <code>RecorderEvents</code> by writing them
 * to a file in ZSON format.
 *
 * @author Boris Grozev
 * @author Eng Chong Meng
 */
public class RecorderEventHandlerZSONImpl implements RecorderEventHandler {
    /**
     * Compares <code>RecorderEvent</code>s by their instant (e.g. timestamp).
     */
    private static final Comparator<RecorderEvent> eventComparator = new Comparator<RecorderEvent>() {
        @Override
        public int compare(RecorderEvent a, RecorderEvent b) {
            return Long.compare(a.getInstant(), b.getInstant());
        }
    };

    File file;
    private boolean closed = false;

    private final List<RecorderEvent> audioEvents = new LinkedList<>();

    private final List<RecorderEvent> videoEvents = new LinkedList<>();

    /**
     * {@inheritDoc}
     */
    public RecorderEventHandlerZSONImpl(String filename)
            throws IOException {
        file = new File(filename);
        if (!file.createNewFile())
            throw new IOException("File exists or cannot be created: " + file);

        if (!file.canWrite())
            throw new IOException("Cannot write to file: " + file);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized boolean handleEvent(RecorderEvent ev) {
        if (closed)
            return false;

        MediaType mediaType = ev.getMediaType();
        RecorderEvent.Type type = ev.getType();
        long duration = ev.getDuration();
        long ssrc = ev.getSsrc();

        /*
         * For a RECORDING_ENDED event without a valid instant, find it's associated (i.e. with the
         * same SSRC) RECORDING_STARTED event and compute the RECORDING_ENDED instance based on its
         * duration.
         */
        if (RecorderEvent.Type.RECORDING_ENDED.equals(type) && ev.getInstant() == -1
                && duration != -1) {
            List<RecorderEvent> events = MediaType.AUDIO.equals(mediaType) ? audioEvents
                    : videoEvents;

            RecorderEvent start = null;
            for (RecorderEvent e : events) {
                if (RecorderEvent.Type.RECORDING_STARTED.equals(e.getType()) && e.getSsrc() == ssrc) {
                    start = e;
                    break;
                }
            }

            if (start != null)
                ev.setInstant(start.getInstant() + duration);
        }

        if (MediaType.AUDIO.equals(mediaType))
            audioEvents.add(ev);
        else if (MediaType.VIDEO.equals(mediaType))
            videoEvents.add(ev);

        try {
            writeAllEvents();
        } catch (IOException ioe) {
            Timber.w("Failed to write recorder events to file: %s", ioe.getMessage());
            return false;
        }

        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void close() {
        // XXX do we want to write everything again?
        try {
            writeAllEvents();
        } catch (IOException ioe) {
            Timber.w("Failed to write recorder events to file: %s", ioe.getMessage());
        } finally {
            closed = true;
        }
    }

    private void writeAllEvents()
            throws IOException {
        audioEvents.sort(eventComparator);
        videoEvents.sort(eventComparator);
        int nbAudio = audioEvents.size();
        int nbVideo = videoEvents.size();

        if (nbAudio + nbVideo > 0) {
            FileWriter writer = new FileWriter(file, false);

            writer.write("{\n");

            if (nbAudio > 0) {
                writer.write("  \"audio\" : [\n");
                writeEvents(audioEvents, writer);

                if (nbVideo > 0)
                    writer.write("  ],\n\n");
                else
                    writer.write("  ]\n\n");
            }

            if (nbVideo > 0) {
                writer.write("  \"video\" : [\n");
                writeEvents(videoEvents, writer);
                writer.write("  ]\n");
            }

            writer.write("}\n");

            writer.close();
        }
    }

    private void writeEvents(List<RecorderEvent> events, FileWriter writer)
            throws IOException {
        int idx = 0;
        int size = events.size();
        for (RecorderEvent ev : events) {
            if (++idx == size)
                writer.write("    " + getZSON(ev) + "\n");
            else
                writer.write("    " + getZSON(ev) + ",\n");
        }
    }

    private String getZSON(RecorderEvent ev) {
        ZSONObject zson = new ZSONObject();
        zson.put("instant", ev.getInstant());
        zson.put("type", ev.getType().toString());

        MediaType mediaType = ev.getMediaType();
        if (mediaType != null)
            zson.put("mediaType", mediaType.toString());

        zson.put("ssrc", ev.getSsrc());

        long audioSsrc = ev.getAudioSsrc();
        if (audioSsrc != -1)
            zson.put("audioSsrc", audioSsrc);

        RecorderEvent.AspectRatio aspectRatio = ev.getAspectRatio();
        if (aspectRatio != RecorderEvent.AspectRatio.ASPECT_RATIO_UNKNOWN)
            zson.put("aspectRatio", aspectRatio.toString());

        long rtpTimestamp = ev.getRtpTimestamp();
        if (rtpTimestamp != -1)
            zson.put("rtpTimestamp", rtpTimestamp);

        String endpointId = ev.getEndpointId();
        if (endpointId != null)
            zson.put("endpointId", endpointId);

        String filename = ev.getFilename();
        if (filename != null) {
            String bareFilename = filename;
            int idx = filename.lastIndexOf('/');
            int len = filename.length();
            if (idx != -1 && idx != len - 1)
                bareFilename = filename.substring(1 + idx, len);

            zson.put("filename", bareFilename);
        }
        return zson.toString();
    }
}
