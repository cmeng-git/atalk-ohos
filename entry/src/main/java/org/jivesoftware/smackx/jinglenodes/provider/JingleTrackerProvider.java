package org.jivesoftware.smackx.jinglenodes.provider;

import static org.jivesoftware.smackx.jinglenodes.element.JingleTrackerIQ.ATTR_ADDRESS;
import static org.jivesoftware.smackx.jinglenodes.element.JingleTrackerIQ.ATTR_POLICY;
import static org.jivesoftware.smackx.jinglenodes.element.JingleTrackerIQ.ATTR_PROTOCOL;
import static org.jivesoftware.smackx.jinglenodes.element.JingleTrackerIQ.ATTR_VERIFIED;

import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smackx.jinglenodes.element.JingleTrackerIQ;
import org.jxmpp.jid.impl.JidCreate;
import org.jivesoftware.smackx.jinglenodes.TrackerEntry;

import java.io.IOException;

public class JingleTrackerProvider extends IQProvider<JingleTrackerIQ>
{
    @Override
    public JingleTrackerIQ parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment)
            throws org.jivesoftware.smack.xml.XmlPullParserException, IOException, SmackParsingException
    {
        JingleTrackerIQ iq = new JingleTrackerIQ();

        boolean done = false;
        XmlPullParser.Event eventType;
        String elementName;

        while (!done) {
            eventType = parser.getEventType();
            elementName = parser.getName();

            if (eventType == XmlPullParser.Event.START_ELEMENT) {
                final TrackerEntry.Type type;
                if (elementName.equals(TrackerEntry.Type.relay.toString())) {
                    type = TrackerEntry.Type.relay;
                }
                else if (elementName.equals(TrackerEntry.Type.tracker.toString())) {
                    type = TrackerEntry.Type.tracker;
                }
                else {
                    parser.next();
                    continue;
                }

                final String protocol = parser.getAttributeValue(null, ATTR_PROTOCOL);
                final TrackerEntry.Policy policy = TrackerEntry.Policy.valueOf("_"
                        + parser.getAttributeValue(null, ATTR_POLICY));
                final String address = parser.getAttributeValue(null,  ATTR_ADDRESS);
                final String verified = parser.getAttributeValue(null, ATTR_VERIFIED);

                if (address != null && address.length() > 0) {
                    final TrackerEntry entry = new TrackerEntry(type, policy, JidCreate.from(address), protocol);
                    if (verified != null && verified.equals("true")) {
                        entry.setVerified(true);
                    }
                    iq.addEntry(entry);
                }
            }
            else if (eventType == XmlPullParser.Event.END_ELEMENT) {
                if (elementName.equals(JingleTrackerIQ.ELEMENT)) {
                    done = true;
                }
            }
            if (!done) {
                parser.next();
            }
        }
        return iq;
    }
}
