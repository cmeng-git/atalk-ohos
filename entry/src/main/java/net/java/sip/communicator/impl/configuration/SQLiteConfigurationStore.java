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
package net.java.sip.communicator.impl.configuration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ohos.app.Context;
import ohos.data.DatabaseHelper;
import ohos.data.rdb.RdbPredicates;
import ohos.data.rdb.RdbStore;
import ohos.data.rdb.ValuesBucket;
import ohos.data.resultset.ResultSet;

import net.java.sip.communicator.service.protocol.AccountID;
import net.java.sip.communicator.util.ServiceUtils;

import org.atalk.impl.configuration.ConfigurationStore;
import org.atalk.impl.configuration.DatabaseConfigurationStore;
import org.atalk.impl.configuration.HashtableConfigurationStore;
import org.atalk.impl.timberlog.TimberLog;
import org.atalk.persistance.DatabaseBackend;
import org.atalk.service.osgi.OSGiService;

import timber.log.Timber;

/**
 * Implements a <code>ConfigurationStore</code> which stores property name-value associations in an
 * SQLite database.
 *
 * @author Lyubomir Marinov
 * @author Eng Chong Meng
 */
public class SQLiteConfigurationStore extends DatabaseConfigurationStore {
    public static final String TABLE_NAME = "properties";
    public static final String COLUMN_NAME = "Name";
    public static final String COLUMN_VALUE = "Value";

    /**
     * aTalk backend SQLite database
     */
    private final DatabaseHelper openHelper;
    private static RdbStore mRdbStore = null;

    /**
     * Initializes a new <code>SQLiteConfigurationStore</code> instance.
     */
    public SQLiteConfigurationStore() {
        this(ServiceUtils.getService(ConfigurationActivator.getBundleContext(), OSGiService.class));
    }

    public SQLiteConfigurationStore(Context context) {
        openHelper = DatabaseBackend.getInstance(context);
        mRdbStore = DatabaseBackend.getRdbStore();
    }

    /**
     * Overrides {@link HashtableConfigurationStore#getProperty(String)}. If this
     * <code>ConfigurationStore</code> contains a value associated with the specified property name,
     * returns it. Otherwise, searches for a system property with the specified name and returns
     * its value. If property name starts with "acc", the look up the value in table
     * AccountID.TBL_PROPERTIES for the specified accountUuid, otherwise use table TABLE_NAME
     *
     * @param name the name of the property to get the value of
     *
     * @return the value in this <code>ConfigurationStore</code> of the property with the specified
     * name; <code>null</code> if the property with the specified name does not have an association
     * with a value in this <code>ConfigurationStore</code>
     *
     * @see ConfigurationStore#getProperty(String)
     */
    @Override
    public Object getProperty(String name) {
        ResultSet resultSet = null;
        Object value = properties.get(name);
        if (value == null) {
            String[] columns = {COLUMN_VALUE};
            synchronized (openHelper) {
                if (name.startsWith(AccountID.ACCOUNT_UUID_PREFIX)) {
                    int idx = name.indexOf(".");
                    if (idx == -1) {
                        value = name;  // just return the accountUuid
                    }
                    else {
                        RdbPredicates rdbPredicates = new RdbPredicates(AccountID.TBL_PROPERTIES)
                                .equalTo(AccountID.ACCOUNT_UUID, name.substring(0, idx))
                                .and().equalTo(COLUMN_NAME, name.substring(idx + 1))
                                .limit(1);
                        resultSet = mRdbStore.query(rdbPredicates, columns);
                    }
                }
                else {
                    RdbPredicates rdbPredicates = new RdbPredicates(TABLE_NAME)
                            .equalTo(COLUMN_NAME, name)
                            .limit(1);
                    resultSet = mRdbStore.query(rdbPredicates, columns);
                }
                if (resultSet != null) {
                    try {
                        if ((resultSet.getRowCount() == 1) && resultSet.goToFirstRow())
                            value = resultSet.getString(0);
                    } finally {
                        resultSet.close();
                    }
                }
            }
            if (value == null)
                value = System.getProperty(name);
        }
        return value;
    }

    /**
     * Overrides {@link HashtableConfigurationStore#getPropertyNames(String)}. Gets the names of
     * the properties which have values associated in this <code>ConfigurationStore</code>.
     *
     * @return an array of <code>String</code>s which specify the names of the properties that have
     * values associated in this <code>ConfigurationStore</code>; an empty array if this instance
     * contains no property values
     *
     * @see ConfigurationStore#getPropertyNames(String)
     */
    @Override
    public String[] getPropertyNames(String name) {
        List<String> propertyNames = new ArrayList<>();
        String tableName;

        synchronized (openHelper) {
            if (name.startsWith(AccountID.ACCOUNT_UUID_PREFIX)) {
                tableName = AccountID.TBL_PROPERTIES;
            }
            else {
                tableName = TABLE_NAME;
            }

            RdbPredicates rdbPredicates = new RdbPredicates(tableName)
                    .orderByAsc(COLUMN_NAME);
            ResultSet resultSet = mRdbStore.query(rdbPredicates, new String[]{COLUMN_NAME});

            while (resultSet.goToNextRow()) {
                propertyNames.add(resultSet.getString(0));
            }
        }
        return propertyNames.toArray(new String[0]);
    }

    /**
     * Removes all property name-value associations currently present in this
     * <code>ConfigurationStore</code> instance and de-serializes new property name-value
     * associations from its underlying database (storage).
     *
     * @throws IOException if there is an input error while reading from the underlying database (storage)
     */

    protected void reloadConfiguration()
            throws IOException {
        // TODO Auto-generated method stub
    }

    /**
     * Overrides {@link HashtableConfigurationStore#removeProperty(String)}. Removes the value
     * association in this <code>ConfigurationStore</code> of the property with a specific name. If
     * the property with the specified name is not associated with a value in this
     * <code>ConfigurationStore</code>, does nothing.
     *
     * @param name the name of the property which is to have its value association in this
     * <code>ConfigurationStore</code> removed
     *
     * @see ConfigurationStore#removeProperty(String)
     */
    public void removeProperty(String name) {
        super.removeProperty(name);
        synchronized (openHelper) {
            if (name.startsWith(AccountID.ACCOUNT_UUID_PREFIX)) {
                int idx = name.indexOf(".");
                // remove user account if only accountUuid is specified
                if (idx == -1) {
                    mRdbStore.delete(new RdbPredicates(AccountID.TABLE_NAME)
                            .equalTo(AccountID.ACCOUNT_UUID, name));
                }
                // Otherwise, remove the accountProperty from the AccountID.TBL_PROPERTIES
                else {
                    RdbPredicates rdbPredicates = new RdbPredicates(AccountID.TBL_PROPERTIES)
                            .equalTo(AccountID.ACCOUNT_UUID, name.substring(0, idx))
                            .and().equalTo(COLUMN_NAME, name.substring(idx + 1));
                    mRdbStore.delete(rdbPredicates);
                }
            }
            else {
                mRdbStore.delete(new RdbPredicates(AccountID.TABLE_NAME)
                        .equalTo(AccountID.COLUMN_NAME, name));
            }
        }
        Timber.log(TimberLog.FINER, "### Remove property from table: %s", name);
    }

    /**
     * Overrides {@link HashtableConfigurationStore#setNonSystemProperty(String, Object)}.
     *
     * @param name the name of the non-system property to be set to the specified value in this
     * <code>ConfigurationStore</code>
     * @param value the value to be assigned to the non-system property with the specified name in this
     * <code>ConfigurationStore</code>
     *
     * @see ConfigurationStore#setNonSystemProperty(String, Object)
     */
    @Override
    public void setNonSystemProperty(String name, Object value) {
        synchronized (openHelper) {
            String tableName = TABLE_NAME;

            ValuesBucket Values = new ValuesBucket();
            Values.putString(COLUMN_VALUE, value.toString());

            if (name.startsWith(AccountID.ACCOUNT_UUID_PREFIX)) {
                int idx = name.indexOf(".");
                Values.putString(AccountID.ACCOUNT_UUID, name.substring(0, idx));
                Values.putString(COLUMN_NAME, name.substring(idx + 1));
                tableName = AccountID.TBL_PROPERTIES;
            }
            else {
                Values.putString(COLUMN_NAME, name);
            }

            // Insert the properties in DB, replace if exist
            long rowId = mRdbStore.replace(tableName, Values);
            if (rowId == -1)
                Timber.e("Failed to set non-system property: %s: %s <= %s", tableName, name, value);

            Timber.log(TimberLog.FINER, "### Set non-system property: %s: %s <= %s", tableName, name, value);
        }
        // To take care of cached properties and accountProperties
        super.setNonSystemProperty(name, value);
    }
}
