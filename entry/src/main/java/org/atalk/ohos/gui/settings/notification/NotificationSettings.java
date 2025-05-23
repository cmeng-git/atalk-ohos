/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package org.atalk.ohos.gui.settings.notification;

import ohos.aafwk.content.Intent;
import ohos.agp.components.BaseItemProvider;
import ohos.agp.components.Checkbox;
import ohos.agp.components.Component;
import ohos.agp.components.ComponentContainer;
import ohos.agp.components.LayoutScatter;
import ohos.agp.components.ListContainer;
import ohos.agp.components.Text;

import net.java.sip.communicator.service.notification.NotificationChangeListener;
import net.java.sip.communicator.service.notification.NotificationService;
import net.java.sip.communicator.service.notification.event.NotificationActionTypeEvent;
import net.java.sip.communicator.service.notification.event.NotificationEventTypeEvent;
import net.java.sip.communicator.util.ServiceUtils;
import net.java.sip.communicator.util.UtilActivator;

import org.atalk.ohos.BaseAbility;
import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.gui.AppGUIActivator;
import org.atalk.service.resources.ResourceManagementService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * The <code>Ability</code> lists all notification events. When user selects one of them the details screen is opened.
 *
 * @author Pawel Domas
 * @author Eng Chong Meng
 */
public class NotificationSettings extends BaseAbility {
    public static final String NOTICE_PREFIX = "notificationconfig.event.";

    /**
     * Notifications adapter.
     */
    private NotificationItemProvider adapter;

    /**
     * Notification service instance.
     */
    private NotificationService notificationService;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onStart(Intent intent) {
        super.onStart(intent);
        this.notificationService = ServiceUtils.getService(AppGUIActivator.bundleContext, NotificationService.class);
        setUIContent(ResourceTable.Layout_list_layout);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onActive() {
        super.onActive();
        // Refresh the list each time is displayed
        adapter = new NotificationItemProvider();
        ListContainer listView = findComponentById(ResourceTable.Id_list);
        listView.setItemProvider(adapter);
        // And start listening for updates
        notificationService.addNotificationChangeListener(adapter);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onInactive() {
        super.onInactive();
        // Do not listen for changes when paused
        notificationService.removeNotificationChangeListener(adapter);
        adapter = null;
    }

    /**
     * Adapter lists all notification events.
     */
    class NotificationItemProvider extends BaseItemProvider implements NotificationChangeListener {
        /**
         * List of event types
         */
        private final ArrayList<String> events = new ArrayList<>();

        /**
         * Map of events => eventType : eventName in ascending order by eventName
         */
        private final Map<String, String> sortedEvents = new TreeMap<>();

        /**
         * Creates new instance of <code>NotificationItemProvider</code>;
         * Values are sorted in ascending order by eventNames for user easy reference.
         */
        NotificationItemProvider() {
            ResourceManagementService rms = UtilActivator.getResources();
            Map<String, String> unSortedMap = new HashMap<>();
            for (String event : notificationService.getRegisteredEvents()) {
                unSortedMap.put(rms.getI18NString(NOTICE_PREFIX + event), event);
            }

            // sort and save copies in sortedEvents and events
            Map<String, String> sortedMap = new TreeMap<>(unSortedMap);
            for (Map.Entry<String, String> entry : sortedMap.entrySet()) {
                sortedEvents.put(entry.getValue(), entry.getKey());
                events.add(entry.getValue());
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getCount() {
            return events.size();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object getItem(int position) {
            return sortedEvents.get(events.get(position));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public long getItemId(int position) {
            return position;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Component getComponent(int position, Component rowView, ComponentContainer parent) {
            //if (rowView == null) cmeng would not update properly the status on enter/return
            rowView = LayoutScatter.getInstance(getContext()).parse(ResourceTable.Layout_notification_item, parent, false);

            String eventType = events.get(position);
            rowView.setClickedListener(v -> {
                Intent details = NotificationDetails.getIntent(getContext(), eventType);
                startAbility(details);
            });

            Text textView = rowView.findComponentById(ResourceTable.Id_descriptor);
            textView.setText((String) getItem(position));

            Checkbox enableBtn = rowView.findComponentById(ResourceTable.Id_enable);
            enableBtn.setChecked(notificationService.isActive(eventType));

            enableBtn.setCheckedStateChangedListener((buttonView, isChecked)
                    -> notificationService.setActive(eventType, isChecked));
            return rowView;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void eventTypeAdded(final NotificationEventTypeEvent event) {
            runOnUiThread(() -> {
                events.add(event.getEventType());
                notifyDataChanged();
            });
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void eventTypeRemoved(final NotificationEventTypeEvent event) {
            runOnUiThread(() -> {
                events.remove(event.getEventType());
                notifyDataChanged();
            });
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void actionAdded(NotificationActionTypeEvent event) {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void actionRemoved(NotificationActionTypeEvent event) {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void actionChanged(NotificationActionTypeEvent event) {
        }
    }
}
