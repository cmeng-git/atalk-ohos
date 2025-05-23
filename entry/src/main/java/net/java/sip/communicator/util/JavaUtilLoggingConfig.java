/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.LogManager;

import ohos.app.Context;

import org.atalk.service.osgi.OSGiService;
import org.atalk.util.OSUtils;
import org.osgi.framework.BundleContext;

/**
 * Implements the class which is to have its name specified to {@link LogManager} via the system property
 * <code>java.util.logging.config.class</code> and which is to read in the initial configuration.
 *
 * @author Eng Chong Meng
 */
public class JavaUtilLoggingConfig
{
    public JavaUtilLoggingConfig()
            throws IOException
    {
        InputStream is = null;
        try {
            String propertyName = "java.util.logging.config.class";
            if (System.getProperty(propertyName) == null) {
                System.setProperty(propertyName, JavaUtilLoggingConfig.class.getName());
            }

            String filePath = System.getProperty("java.util.logging.config.file");
            if (filePath == null)
                filePath = "resources/rawfile/logging.properties";

            if (OSUtils.IS_ANDROID) {
                BundleContext bundleContext = UtilActivator.bundleContext;
                if (bundleContext != null) {
                    Context context = ServiceUtils.getService(bundleContext, OSGiService.class);
                    if (context != null) {
                        is = context.getResourceManager().getRawFileEntry(filePath).openRawFile();
                    }
                }
            }
            else {
                is = Files.newInputStream(Paths.get(filePath));
            }

            if (is != null) {
                LogManager.getLogManager().reset();
                LogManager.getLogManager().readConfiguration(is);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            if (is != null)
                is.close();
        }
    }
}
