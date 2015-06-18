/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
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
package org.jitsi.impl.osgi;

import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.content.res.*;
import android.os.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import org.jitsi.impl.osgi.framework.*;
import org.jitsi.impl.osgi.framework.launch.*;
import org.jitsi.service.configuration.*;
import org.jitsi.service.osgi.*;

import org.jitsi.util.*;
import org.osgi.framework.*;
import org.osgi.framework.launch.*;
import org.osgi.framework.startlevel.*;

/**
 * Implements the actual, internal functionality of {@link OSGiService}.
 *
 * @author Lyubomir Marinov
 */
public class OSGiServiceImpl
{
    private final OSGiServiceBundleContextHolder bundleContextHolder
        = new OSGiServiceBundleContextHolder();

    private final AsyncExecutor<Runnable> executor
        = new AsyncExecutor<Runnable>(5, TimeUnit.MINUTES);

    /**
     * The <tt>org.osgi.framework.launch.Framework</tt> instance which
     * represents the OSGi instance launched by this <tt>OSGiServiceImpl</tt>.
     */
    private Framework framework;

    /**
     * The <tt>Object</tt> which synchronizes the access to {@link #framework}.
     */
    private final Object frameworkSyncRoot = new Object();

    /**
     * The Android {@link Service} which uses this instance as its very
     * implementation.
     */
    private final OSGiService service;

    /**
     * Initializes a new <tt>OSGiServiceImpl</tt> instance which is to be used
     * by a specific Android <tt>OSGiService</tt> as its very implementation.
     *
     * @param service the Android <tt>OSGiService</tt> which is to use the new
     * instance as its very implementation
     */
    public OSGiServiceImpl(OSGiService service)
    {
        this.service = service;
    }

    /**
     * Invoked by the Android system to initialize a communication channel to
     * {@link #service}. Returns an implementation of the public API of the
     * <tt>OSGiService</tt> i.e. {@link BundleContextHolder} in the form of an
     * {@link IBinder}.
     *
     * @param intent the <tt>Intent</tt> which was used to bind to
     * <tt>service</tt>
     * @return an <tt>IBinder</tt> through which clients may call on to the
     * public API of <tt>OSGiService</tt> 
     * @see Service#onBind(Intent)
     */
    public IBinder onBind(Intent intent)
    {
        return bundleContextHolder;
    }

    /**
     * Invoked by the Android system when {@link #service} is first created.
     * Asynchronously starts the OSGi framework (implementation) represented by
     * this instance.
     *
     * @see Service#onCreate()
     */
    public void onCreate()
    {
        try
        {
            setScHomeDir();
        }
        catch (Throwable t)
        {
            if (t instanceof ThreadDeath)
                throw (ThreadDeath) t;
        }
        try
        {
            setJavaUtilLoggingConfigFile();
        }
        catch (Throwable t)
        {
            if (t instanceof ThreadDeath)
                throw (ThreadDeath) t;
        }

        executor.execute(new OnCreateCommand());
    }

    /**
     * Invoked by the Android system when {@link #service} is no longer used and
     * is being removed. Asynchronously stops the OSGi framework
     * (implementation) represented by this instance.
     *
     * @see Service#onDestroy()
     */
    public void onDestroy()
    {
        synchronized (executor)
        {
            executor.execute(new OnDestroyCommand());
            executor.shutdown();
        }
    }

    /**
     * Invoked by the Android system every time a client explicitly starts
     * {@link #service} by calling {@link Context#startService(Intent)}. Always
     * returns {@link Service#START_STICKY}.
     *
     * @param intent the <tt>Intent</tt> supplied to
     * <tt>Context.startService(Intent}</tt>
     * @param flags additional data about the start request
     * @param startId a unique integer which represents this specific request
     * to start
     * @return a value which indicates what semantics the Android system should
     * use for <tt>service</tt>'s current started state
     * @see Service#onStartCommand(Intent, int, int)
     */
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        return Service.START_STICKY;
    }

    /**
     * Sets up <tt>java.util.logging.LogManager</tt> by assigning values to the
     * system properties which allow more control over reading the initial
     * configuration.
     */
    private void setJavaUtilLoggingConfigFile()
    {
    }

    private void setScHomeDir()
    {
        String name = null;

        if (System.getProperty(ConfigurationService.PNAME_SC_HOME_DIR_LOCATION)
                == null)
        {
            File filesDir = service.getFilesDir();
            String location = filesDir.getParentFile().getAbsolutePath();

            name = filesDir.getName();

            System.setProperty(
                    ConfigurationService.PNAME_SC_HOME_DIR_LOCATION,
                    location);
        }
        if (System.getProperty(ConfigurationService.PNAME_SC_HOME_DIR_NAME)
                == null)
        {
            if ((name == null) || (name.length() == 0))
            {
                ApplicationInfo info = service.getApplicationInfo();

                name = info.name;
                if ((name == null) || (name.length() == 0))
                    name = "Jitsi";
            }

            System.setProperty(
                    ConfigurationService.PNAME_SC_HOME_DIR_NAME,
                    name);
        }

        // Set log dir location to PNAME_SC_HOME_DIR_LOCATION
        if (System.getProperty(ConfigurationService.PNAME_SC_LOG_DIR_LOCATION)
                == null)
        {
            String homeDir = System.getProperty(
                    ConfigurationService.PNAME_SC_HOME_DIR_LOCATION, null);

            System.setProperty(
                ConfigurationService.PNAME_SC_LOG_DIR_LOCATION, homeDir);
        }
        // Set cache dir location to Context.getCacheDir()
        if (System.getProperty(ConfigurationService.PNAME_SC_CACHE_DIR_LOCATION)
                == null)
        {
            File cacheDir = service.getCacheDir();
            String location = cacheDir.getParentFile().getAbsolutePath();

            System.setProperty(
                ConfigurationService.PNAME_SC_CACHE_DIR_LOCATION,
                location);
        }

        /*
         * Set the System property user.home as well because it may be relied
         * upon (e.g. FMJ).
         */
        String location
            = System.getProperty(
                    ConfigurationService.PNAME_SC_HOME_DIR_LOCATION);

        if ((location != null) && (location.length() != 0))
        {
            name
                = System.getProperty(
                        ConfigurationService.PNAME_SC_HOME_DIR_NAME);
            if ((name != null) && (name.length() != 0))
            {
                System.setProperty(
                        "user.home",
                        new File(location, name).getAbsolutePath());
            }
        }
    }

    /**
     * Asynchronously starts the OSGi framework (implementation) represented by
     * this instance.
     */
    private class OnCreateCommand
        implements Runnable
    {
        public void run()
        {
            FrameworkFactory frameworkFactory = new FrameworkFactoryImpl();
            Map<String, String> configuration = new HashMap<String, String>();

            TreeMap<Integer, List<String>> BUNDLES = getBundlesConfig(service);

            configuration.put(
                    Constants.FRAMEWORK_BEGINNING_STARTLEVEL,
                    Integer.toString(BUNDLES.lastKey()));

            Framework framework = frameworkFactory.newFramework(configuration);

            try
            {
                framework.init();

                BundleContext bundleContext = framework.getBundleContext();

                bundleContext.registerService(OSGiService.class, service, null);
                bundleContext.registerService(
                        BundleContextHolder.class,
                        bundleContextHolder,
                        null);

                for(Map.Entry<Integer, List<String>> startLevelEntry
                        : BUNDLES.entrySet())
                {
                    int startLevel = startLevelEntry.getKey();

                    for (String location : startLevelEntry.getValue())
                    {
                        org.osgi.framework.Bundle bundle
                            = bundleContext.installBundle(location);

                        if (bundle != null)
                        {
                            BundleStartLevel bundleStartLevel
                                = bundle.adapt(BundleStartLevel.class);

                            if (bundleStartLevel != null)
                                bundleStartLevel.setStartLevel(startLevel);
                        }
                    }
                }

                framework.start();
            }
            catch (BundleException be)
            {
                throw new RuntimeException(be);
            }

            synchronized (frameworkSyncRoot)
            {
                OSGiServiceImpl.this.framework = framework;
            }

            service.onOSGiStarted();
        }

        /**
         * Loads bundles configuration from the configured or default file name
         * location.
         *
         * @param context the context to use
         * @return the locations of the OSGi bundles (or rather of the class
         * files of their <tt>BundleActivator</tt> implementations) comprising
         * the Jitsi core/library and the application which is currently using
         * it. And the corresponding start levels.
         */
        private TreeMap<Integer, List<String>> getBundlesConfig(Context context)
        {
            String fileName = System.getProperty("osgi.config.properties");

            if (fileName == null)
                fileName = "lib/osgi.client.run.properties";

            InputStream is = null;
            Properties props = new Properties();

            try
            {
                if (OSUtils.IS_ANDROID)
                {
                    if (context != null)
                    {
                        is
                            = context.getAssets().open(
                                    fileName,
                                    AssetManager.ACCESS_UNKNOWN);
                    }
                }
                else
                {
                    is = new FileInputStream(fileName);
                }

                if (is != null)
                    props.load(is);
            }
            catch(IOException ioe)
            {
                throw new RuntimeException(ioe);
            }
            finally
            {
                try
                {
                    if (is != null)
                        is.close();
                }
                catch(IOException ioe) {}
            }

            TreeMap<Integer, List<String>> startLevels
                = new TreeMap<Integer, List<String>>();

            for (Map.Entry<Object, Object> e : props.entrySet())
            {
                String prop = e.getKey().toString().trim();
                Object value;

                if(prop.contains("auto.start.")
                        && ((value = e.getValue()) != null))
                {
                    String startLevelStr
                        = prop.substring("auto.start.".length());

                    try
                    {
                        int startLevelInt = Integer.parseInt(startLevelStr);

                        StringTokenizer classTokens
                            = new StringTokenizer(value.toString(), " ");
                        List<String> classNames = new ArrayList<String>();

                        while(classTokens.hasMoreTokens())
                        {
                            String className = classTokens.nextToken().trim();

                            if((className != null)
                                    && (className.length() > 0)
                                    && !className.startsWith("#"))
                                classNames.add(className);
                        }

                        if (!classNames.isEmpty())
                            startLevels.put(startLevelInt, classNames);
                    }
                    catch(Throwable t)
                    {
                        if (t instanceof ThreadDeath)
                            throw (ThreadDeath) t;
                    }
                }
            }

            return startLevels;
        }
    }

    /**
     * Asynchronously stops the OSGi framework (implementation) represented by
     * this instance.
     */
    private class OnDestroyCommand
        implements Runnable
    {
        public void run()
        {
            Framework framework;

            synchronized (frameworkSyncRoot)
            {
                framework = OSGiServiceImpl.this.framework;
                OSGiServiceImpl.this.framework = null;
            }

            if (framework != null)
                try
                {
                    framework.stop();
                }
                catch (BundleException be)
                {
                    throw new RuntimeException(be);
                }
        }
    }
}
