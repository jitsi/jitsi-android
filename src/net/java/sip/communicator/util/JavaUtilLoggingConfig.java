/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.util;

import java.io.*;
import java.util.logging.*;

import org.jitsi.service.osgi.*;
import org.jitsi.util.*;

import org.osgi.framework.*;

import android.content.*;
import android.content.res.*;

/**
 * Implements the class which is to have its name specified to
 * {@link LogManager} via the system property
 * <tt>java.util.logging.config.class</tt> and which is to read in the initial
 * configuration.
 *
 * @author Lyubomir Marinov
 */
public class JavaUtilLoggingConfig
{
    public JavaUtilLoggingConfig()
        throws IOException
    {
        InputStream is = null;
        try
        {
            String propertyName = "java.util.logging.config.class";

            if (System.getProperty(propertyName) == null)
            {
                System.setProperty(
                        propertyName,
                        JavaUtilLoggingConfig.class.getName());
            }

            String fileName
                = System.getProperty("java.util.logging.config.file");

            if (fileName == null)
                fileName = "lib/logging.properties";

            if (OSUtils.IS_ANDROID)
            {
                BundleContext bundleContext = UtilActivator.bundleContext;

                if (bundleContext != null)
                {
                    Context context
                        = ServiceUtils.getService(
                                bundleContext,
                                OSGiService.class);

                    if (context != null)
                    {
                        is
                            = context.getAssets().open(
                                    fileName,
                                    AssetManager.ACCESS_UNKNOWN);
                    }
                }
            }
            else
            {
                is = new FileInputStream(fileName);
            }

            if (is != null)
            {
                LogManager.getLogManager().reset();
                LogManager.getLogManager().readConfiguration(is);
            }
        }
        catch(Throwable t)
        {
            t.printStackTrace();
        }
        finally
        {
            if (is != null)
                is.close();
        }
    }
}
