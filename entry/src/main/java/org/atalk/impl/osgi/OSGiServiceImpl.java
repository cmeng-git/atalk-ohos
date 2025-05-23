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
package org.atalk.impl.osgi;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import ohos.aafwk.content.Intent;
import ohos.app.Context;
import ohos.bundle.ApplicationInfo;
import ohos.bundle.ElementName;
import ohos.rpc.IRemoteObject;

import org.apache.http.util.TextUtils;
import org.atalk.impl.osgi.framework.AsyncExecutor;
import org.atalk.impl.osgi.framework.launch.FrameworkFactoryImpl;
import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.aTalkApp;
import org.atalk.service.configuration.ConfigurationService;
import org.atalk.service.osgi.BundleContextHolder;
import org.atalk.service.osgi.OSGiService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;
import org.osgi.framework.startlevel.BundleStartLevel;

/**
 * Implements the actual, internal functionality of {@link OSGiService}.
 *
 * @author Eng Chong Meng
 */
public class OSGiServiceImpl {
    private final OSGiServiceBundleContextHolder bundleContextHolder = new OSGiServiceBundleContextHolder();

    private final AsyncExecutor<Runnable> executor = new AsyncExecutor<>(5, TimeUnit.MINUTES);

    /**
     * The <code>org.osgi.framework.launch.Framework</code> instance which represents the OSGi
     * instance launched by this <code>OSGiServiceImpl</code>.
     */
    private Framework framework;

    /**
     * The <code>Object</code> which synchronizes the access to {@link #framework}.
     */
    private final Object frameworkSyncRoot = new Object();

    /**
     * The Ohos Service which uses this instance as its very implementation.
     */
    private final OSGiService service;

    /**
     * Initializes a new <code>OSGiServiceImpl</code> instance which is to be used by a specific
     * Android <code>OSGiService</code> as its very implementation.
     *
     * @param service the Android <code>OSGiService</code> which is to use the new instance as its very implementation
     */
    public OSGiServiceImpl(OSGiService service) {
        this.service = service;
    }

    /**
     * Invoked by the Android system to initialize a communication channel to {@link #service}.
     * Returns an implementation of the public API of the <code>OSGiService</code> i.e.
     * {@link BundleContextHolder} in the form of an {@link IRemoteObject}.
     *
     * @param intent the <code>Intent</code> which was used to bind to <code>service</code>
     *
     * @return an <code>IBinder</code> through which clients may call on to the public API of <code>OSGiService</code>
     */
    public IRemoteObject onConnect(Intent intent) {
        return bundleContextHolder;
    }

    public void onAbilityConnectDone(ElementName element, IRemoteObject object, int result) {

    }

    /**
     * Invoked by the Android system when {@link #service} is first created. Asynchronously starts
     * the OSGi framework (implementation) represented by this instance.
     */
    public void onStart(Intent intent) {
        try {
            setScHomeDir();
        } catch (Throwable t) {
            if (t instanceof ThreadDeath)
                throw (ThreadDeath) t;
        }
        try {
            setJavaUtilLoggingConfigFile();
        } catch (Throwable t) {
            if (t instanceof ThreadDeath)
                throw (ThreadDeath) t;
        }

        executor.execute(new OnCreateCommand());
    }

    /**
     * Invoked by the Android system when {@link #service} is no longer used and is being removed.
     * Asynchronously stops the OSGi framework (implementation) represented by this instance.
     */
    public void onStop() {
        synchronized (executor) {
            executor.execute(new OnDestroyCommand());
            executor.shutdown();
        }
    }

    /**
     * Invoked by the Android system every time a client explicitly starts {@link #service} by
     * calling {@link Context#startAbility(Intent, int)}. Always returns {@link // Service#START_STICKY}.
     *
     * @param intent the <code>Intent</code> supplied to <code>Context.startService(Intent}</code>
     * @param restart additional infoabout the start request
     * @param startId a unique integer which represents this specific request to start
     * <p>
     * //     * @return a value which indicates what semantics the Android system should use for
     * //     * <code>service</code>'s current started state
     */
    public void onCommand(Intent intent, boolean restart, int startId) {
        // return Service.START_STICKY;
    }

    /**
     * Sets up <code>java.util.logging.LogManager</code> by assigning values to the system properties
     * which allow more control over reading the initial configuration.
     */
    private void setJavaUtilLoggingConfigFile() {
    }

    private void setScHomeDir() {
        String name = null;

        if (System.getProperty(ConfigurationService.PNAME_SC_HOME_DIR_LOCATION) == null) {
            File filesDir = service.getFilesDir();
            String location = filesDir.getParentFile().getAbsolutePath();

            name = filesDir.getName();
            System.setProperty(ConfigurationService.PNAME_SC_HOME_DIR_LOCATION, location);
        }
        if (System.getProperty(ConfigurationService.PNAME_SC_HOME_DIR_NAME) == null) {
            if (TextUtils.isEmpty(name)) {
                ApplicationInfo info = service.getApplicationInfo();
                name = info.name;
                if (TextUtils.isEmpty(name))
                    name = aTalkApp.getResString(ResourceTable.String_app_name);
            }
            System.setProperty(ConfigurationService.PNAME_SC_HOME_DIR_NAME, name);
        }

        // Set log dir location to PNAME_SC_HOME_DIR_LOCATION
        if (System.getProperty(ConfigurationService.PNAME_SC_LOG_DIR_LOCATION) == null) {
            String homeDir = System.getProperty(ConfigurationService.PNAME_SC_HOME_DIR_LOCATION, null);

            System.setProperty(ConfigurationService.PNAME_SC_LOG_DIR_LOCATION, homeDir);
        }
        // Set cache dir location to Context.getCacheDir()
        if (System.getProperty(ConfigurationService.PNAME_SC_CACHE_DIR_LOCATION) == null) {
            File cacheDir = service.getCacheDir();
            String location = cacheDir.getParentFile().getAbsolutePath();
            System.setProperty(ConfigurationService.PNAME_SC_CACHE_DIR_LOCATION, location);
        }

        /*
         * Set the System property user.home as well because it may be relied upon (e.g. FMJ).
         */
        String location = System.getProperty(ConfigurationService.PNAME_SC_HOME_DIR_LOCATION);

        if ((location != null) && (location.length() != 0)) {
            name = System.getProperty(ConfigurationService.PNAME_SC_HOME_DIR_NAME);
            if ((name != null) && (name.length() != 0)) {
                System.setProperty("user.home", new File(location, name).getAbsolutePath());
            }
        }
    }

    /**
     * Asynchronously starts the OSGi framework (implementation) represented by this instance.
     */
    private class OnCreateCommand implements Runnable {
        public void run() {
            FrameworkFactory frameworkFactory = new FrameworkFactoryImpl();
            Map<String, String> configuration = new HashMap<>();

            TreeMap<Integer, List<String>> BUNDLES = getBundlesConfig(service);
            configuration.put(Constants.FRAMEWORK_BEGINNING_STARTLEVEL, Integer.toString(BUNDLES.lastKey()));
            Framework framework = frameworkFactory.newFramework(configuration);

            try {
                framework.init();
                BundleContext bundleContext = framework.getBundleContext();
                bundleContext.registerService(OSGiService.class, service, null);
                bundleContext.registerService(BundleContextHolder.class, bundleContextHolder, null);

                for (Map.Entry<Integer, List<String>> startLevelEntry : BUNDLES.entrySet()) {
                    int startLevel = startLevelEntry.getKey();

                    for (String location : startLevelEntry.getValue()) {
                        org.osgi.framework.Bundle bundle = bundleContext.installBundle(location);
                        if (bundle != null) {
                            BundleStartLevel bundleStartLevel = bundle.adapt(BundleStartLevel.class);

                            if (bundleStartLevel != null)
                                bundleStartLevel.setStartLevel(startLevel);
                        }
                    }
                }
                framework.start();
            } catch (BundleException be) {
                throw new RuntimeException(be);
            }

            synchronized (frameworkSyncRoot) {
                OSGiServiceImpl.this.framework = framework;
            }

            service.onOSGiStarted();
        }

        /**
         * Loads bundles configuration from the configured or default file name location.
         *
         * @param context the context to use
         *
         * @return the locations of the OSGi bundles (or rather of the class files of their
         * <code>BundleActivator</code> implementations) comprising the Jitsi core/library and the
         * application which is currently using it. And the corresponding start levels.
         */
        private TreeMap<Integer, List<String>> getBundlesConfig(Context context) {
            String filePath = System.getProperty("osgi.config.properties");
            if (filePath == null)
                filePath = "lib/osgi.client.run.properties";

            InputStream is = null;
            Properties props = new Properties();

            try {
                if (context != null) {
                    is = context.getResourceManager().getRawFileEntry(filePath).openRawFile();
                }

                if (is != null)
                    props.load(is);
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            } finally {
                try {
                    if (is != null)
                        is.close();
                } catch (IOException ignore) {
                }
            }

            TreeMap<Integer, List<String>> startLevels = new TreeMap<>();

            for (Map.Entry<Object, Object> e : props.entrySet()) {
                String prop = e.getKey().toString().trim();
                Object value;

                if (prop.contains("auto.start.") && ((value = e.getValue()) != null)) {
                    String startLevelStr = prop.substring("auto.start.".length());
                    try {
                        int startLevelInt = Integer.parseInt(startLevelStr);

                        StringTokenizer classTokens = new StringTokenizer(value.toString(), " ");
                        List<String> classNames = new ArrayList<>();

                        while (classTokens.hasMoreTokens()) {
                            String className = classTokens.nextToken().trim();

                            if (!TextUtils.isEmpty(className) && !className.startsWith("#"))
                                classNames.add(className);
                        }
                        if (!classNames.isEmpty())
                            startLevels.put(startLevelInt, classNames);
                    } catch (Throwable t) {
                        if (t instanceof ThreadDeath)
                            throw (ThreadDeath) t;
                    }
                }
            }
            return startLevels;
        }
    }

    /**
     * Asynchronously stops the OSGi framework (implementation) represented by this instance.
     */
    private class OnDestroyCommand implements Runnable {
        public void run() {
            Framework framework;
            synchronized (frameworkSyncRoot) {
                framework = OSGiServiceImpl.this.framework;
                OSGiServiceImpl.this.framework = null;
            }

            if (framework != null)
                try {
                    framework.stop();
                } catch (BundleException be) {
                    throw new RuntimeException(be);
                }
        }
    }
}
