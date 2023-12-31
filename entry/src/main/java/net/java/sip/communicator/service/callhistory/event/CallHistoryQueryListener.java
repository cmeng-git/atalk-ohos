/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.service.callhistory.event;

/**
 * The <code>CallHistoryQueryListener</code> listens for changes in the result of
 * a given <code>CallHistoryQuery</code>. When a query to the call history is
 * started, this listener would be notified every time new results are available for this query.
 *
 * @author Yana Stamcheva
 */
public interface CallHistoryQueryListener
{
    /**
     * Indicates that new <code>CallRecord</code> is received as a result of the query.
     *
     * @param event the <code>CallRecordsEvent</code> containing information about the query results.
     */
    public void callRecordReceived(CallRecordEvent event);

    /**
     * Indicates that the status of the history has changed.
     *
     * @param event the <code>HistoryQueryStatusEvent</code> containing information about the status change
     */
    public void queryStatusChanged(CallHistoryQueryStatusEvent event);
}
