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
package org.jitsi.impl.androidresources;

import android.content.*;
import android.content.res.*;
import android.util.*;
import android.view.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;
import org.jitsi.service.osgi.*;
import org.osgi.framework.*;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * An Android implementation of the
 * {@link org.jitsi.service.resources.ResourceManagementService}.<br/>
 * <br/> 
 * Strings - requests are redirected to the strings defined in "strings.xml"
 * file, but in case the string is not found it will try to look for strings
 * defined in default string resources.<br/> 
 * <br/> 
 * Dots in keys are replaced with "_", as they can not be used for string
 * names in "strings.xml". For exmaple the string for key "service.gui.CLOSE"
 * should be declared as:<br/>
 * &lt;string name="service_gui_CLOSE"&gt;Close&lt;/string&gt; <br/> 
 * <br/> 
 * Requests for other locales are redirected to 
 * corresponding folders as it's defined in Android localization mechanism.<br/>
 * <br/> 
 * Colors - mapped directly to those defined in /res/values/colors.xml<br/>
 * <br/> 
 * Sounds - are stored in res/raw folder. The mappings are read from 
 * the sounds.properties or other SoundPack's provided. Properties should point
 * to sound file names without the extension. For example:<br/>
 * BUSY=busy (points to /res/raw/busy.wav)<br/>
 * <br/> 
 * Images - images work the same as sounds except they are stored in drawable 
 * folders.<br/> 
 * <br/> 
 * For parts of Jitsi source that directly refere to image paths it will map 
 * the requests to the drawable Android application resource names, so that we
 * can take advantage of built-in image size resolving mechanism. The mapping 
 * must be specified in file {@link #IMAGE_PATH_RESOURCE}.properties.
 * <br/>
 * Sample entries:<br/>
 * resources/images/protocol/sip/sip16x16.png=sip_logo<br/>
 * resources/images/protocol/sip/sip32x32.png=sip_logo<br/>
 * resources/images/protocol/sip/sip48x48.png=sip_logo<br/>
 * resources/images/protocol/sip/sip64x64.png=sip_logo<br/>
 * <br/>
 * 
 * @author Pawel Domas
 */
public class AndroidResourceServiceImpl
    extends AbstractResourcesService
{
    /**
     * The <tt>Logger</tt> used by the <tt>AndroidResourceServiceImpl</tt>
     * class and its instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(AndroidResourceServiceImpl.class);

    /**
     * Path to the .properties file containing image path's
     * translations to android drawable resources
     */
    private static final String IMAGE_PATH_RESOURCE
            = "resources.images.image_path";
    /**
     * Android image path translation resource
     * TODO: Remove direct path requests for resources
     */
    private ResourceBundle androidImagePathPack = null;

    /**
     * The {@link Resources} object for application context
     */
    private static Resources resources = null;

    /**
     * The application package name(org.jitsi)
     */
    private String packageName = null;

    /**
     * The Android application context
     */
    private Context androidContext = null;

    /**
     * The {@link Resources} cache for language other than default
     */
    private Resources cachedLocaleResources = null;

    /**
     * The {@link Locale} of cached locale resources
     */
    private Locale cachedResLocale = null;
    
    private static boolean factorySet = false;

    /**
     * Initializes already registered default resource packs.
     */
    AndroidResourceServiceImpl()
    {
        super(AndroidResourceManagementActivator.bundleContext);

        androidImagePathPack = ResourceBundle.getBundle(IMAGE_PATH_RESOURCE);
        logger.trace("Loaded image path resource: " + androidImagePathPack);

        BundleContext bundleContext = 
                AndroidResourceManagementActivator.bundleContext;
        ServiceReference<OSGiService> serviceRef = 
                bundleContext.getServiceReference(OSGiService.class); 
        OSGiService osgiService = bundleContext.getService(serviceRef);

        resources = osgiService.getResources();
        packageName = osgiService.getPackageName();
        androidContext = osgiService.getApplicationContext();

        if(!factorySet)
        {
            URL.setURLStreamHandlerFactory(
                new AndroidResourceURLHandlerFactory());
            factorySet = true;
        }
    }

    @Override
    protected void onSkinPackChanged() 
    {
        // Not interested (at least for now)
    }

    /**
     * Gets the resource ID for given color <tt>strKey</tt>.
     *
     * @param strKey the color text identifier that has to be resolved
     *
     * @return the resource ID for given color <tt>strKey</tt>
     */
    private int getColorId(String strKey)
    {
        return getResourceId("color", strKey);
    }

    /**
     * Returns the int representation of the color corresponding to the
     * given key.
     *
     * @param key The key of the color in the colors properties file.
     * @return the int representation of the color corresponding to the
     * given key.
     */
    public int getColor(String key) 
    {
        int id = getColorId(key); 
        if(id == 0)
        {
            return 0xFFFFFFFF;
        }
        return resources.getColor(id);
    }

    /**
     * Returns the string representation of the color corresponding to the
     * given key.
     *
     * @param key The key of the color in the colors properties file.
     * @return the string representation of the color corresponding to the
     * given key.
     */
    public String getColorString(String key)
    {
        int id = getColorId(key); 
        if(id == 0)
        {
            return "0xFFFFFFFF";
        }
        return resources.getString(id);
    }

    /**
     * Returns a drawable resource id for given name.
     *
     * @param key the name of drawable
     */
    private int getDrawableId(String key)
    {
        return getResourceId("drawable", key);
    }

    /**
     * Returns the resource id for the given name of specified type.
     * 
     * @param typeName the type name (color, drawable, raw, string ...)
     * @param key the resource name
     *
     * @return the resource id for the given name of specified type
     */
    private int getResourceId(String typeName, String key)
    {
        int id = resources.getIdentifier(key, typeName, packageName);
        if(id == 0)
            logger.error("Unresolved "+typeName+" key: "+key);
        return id;
    }

    /**
     * Returns the <tt>InputStream</tt> of the image corresponding to the given
     * path.
     *
     * @param path The path to the image file.
     * @return the <tt>InputStream</tt> of the image corresponding to the given
     * path.
     */
    public InputStream getImageInputStreamForPath(String path)
    {
        if(logger.isTraceEnabled())
            logger.trace("Request for resource path: " + path);

        if(androidImagePathPack.containsKey(path))
        {
            String translatedPath = androidImagePathPack.getString(path);

            if(logger.isTraceEnabled())
                logger.trace("Translated path: " + translatedPath);

            if(translatedPath != null)
            {
                return getImageInputStream(translatedPath);
            }
        }
        return null;
    }

    /**
     * Returns the <tt>InputStream</tt> of the image corresponding to the given
     * key.
     *
     * @param key The identifier of the image in the resource properties
     * file.
     * @return the <tt>InputStream</tt> of the image corresponding to the given
     * key.
     */
    public InputStream getImageInputStream(String key) 
    {
        // Try to lookup images.properties for key mapping
        String resolvedPath = super.getImagePath(key);
        if(resolvedPath != null)
        {
            key = resolvedPath;
        }

        int id = getDrawableId(key);
        if(id != 0)
        {
            return resources.openRawResource(id);
        }
        return null;
    }

    /**
     * Returns the <tt>URL</tt> of the image corresponding to the given key.
     *
     * @param key The identifier of the image in the resource properties file.
     * @return the <tt>URL</tt> of the image corresponding to the given key
     */
    public URL getImageURL(String key) 
    {
       return getImageURLForPath(getImagePath(key)); 
    }

    /**
     * Returns the <tt>URL</tt> of the image corresponding to the given path.
     *
     * @param path The path to the given image file.
     * @return the <tt>URL</tt> of the image corresponding to the given path.
     */
    public URL getImageURLForPath(String path) 
    {
        if(path == null)
            return null;

        try
        {
            return new URL(path);
        }
        catch (MalformedURLException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the image path corresponding to the given key.
     *
     * @param key The identifier of the image in the resource properties file.
     * @return the image path corresponding to the given key.
     */
    public String getImagePath(String key) 
    {
        String reference = super.getImagePath(key);
        if(reference == null)
        {
            // If no mapping found use key directly
            reference = key;
        }

        int id = getDrawableId(reference);
        if(id == 0)
            return null;

        return AndroidResourceURLHandlerFactory.PROTOCOL + "://" + id;
    }

    /**
     * Returns the string resource id for given <tt>key</tt>.
     * 
     * @param key the name of string resource as defined in "strings.xml"
     * @return the string value for given <tt>key</tt>
     */
    private int getStringId(String key)
    {
        return getResourceId("string", key);
    }

    @Override
    protected String doGetI18String(String key, Locale locale) 
    {
        Resources usedRes = resources;
        Locale resourcesLocale = usedRes.getConfiguration().locale;
        if(locale != null && !locale.equals(resourcesLocale))
        {
            if(!locale.equals(cachedResLocale))
            {
                // Create the Resources object for recently requested locale
                // and caches it in case another request may come up
                Configuration conf = resources.getConfiguration();
                conf.locale = locale;
                AssetManager assets = androidContext.getAssets();
                DisplayMetrics metrics = new DisplayMetrics();
                WindowManager wm = (WindowManager) androidContext
                        .getSystemService(Context.WINDOW_SERVICE);
                wm.getDefaultDisplay().getMetrics(metrics);
                cachedLocaleResources = new Resources(assets, metrics, conf);
                cachedResLocale = locale;
            }
            usedRes = cachedLocaleResources;
        }

        /**
         * Does replace the "." with "_" as they do not work in strings.xml,
         * they are replaced anyway during the resources generation process 
         */
        int id = getStringId(key.replace(".", "_"));
        if (id == 0)
        {
            // If not found tries to get from resources.properties
            return super.doGetI18String(key, locale);
        }

        return usedRes.getString(id);
    }

    /**
     * The sound resource identifier. Sounds are stored in res/raw folder.
     * 
     * @param key the name of sound, for busy.wav it will be just busy
     * @return the sound resource id for given <tt>key</tt>
     */
    private int getSoundId(String key)
    {
        return getResourceId("raw", key);
    }

    /**
     * Returns the <tt>URL</tt> of the sound corresponding to the given
     * property key.
     *
     * @param key the key string
     * @return the <tt>URL</tt> of the sound corresponding to the given
     * property key.
     */
    public URL getSoundURL(String key)
    {
        try
        {
            String path = getSoundPath(key);
            if(path == null)
                return null;

            return new URL(path);
        }
        catch (MalformedURLException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the <tt>URL</tt> of the sound corresponding to the given path.
     *
     * @param path the path, for which we're looking for a sound URL
     * @return the <tt>URL</tt> of the sound corresponding to the given path.
     */
    public URL getSoundURLForPath(String path) 
    {
        return getSoundURL(path);
    }

    /**
     * Returns the path for given <tt>soundKey</tt>. It's formatted with
     * protocol name in the URI format.
     * 
     * @param soundKey the key, for the sound path
     */
    @Override
    public String getSoundPath(String soundKey) 
    {
        String reference = super.getSoundPath(soundKey);
        if(reference == null)
        {
            // If there's no definition in .properties
            // try to access directly by the name
            reference = soundKey;
        }

        int id = getSoundId(reference);
        if(id == 0)
        {
            logger.error("No sound defined for: "+soundKey);
            return null;
        }

        return AndroidResourceURLHandlerFactory.PROTOCOL + "://" + id;
    }

    /**
     * Not supported at the moment.
     * 
     * @param file the zip file from which we prepare a skin
     * @return the prepared file
     * @throws Exception
     */
    public File prepareSkinBundleFromZip(File file) 
        throws Exception
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Some kind of hack to be able to produce URLs pointing to Android
     * resources. It allows to produce URL with protocol name of 
     * {@link #PROTOCOL} that will be later handled by this factory.
     */
    static private class AndroidResourceURLHandlerFactory
        implements URLStreamHandlerFactory
    {
        public static final String PROTOCOL = "jitsi.resource";

        public URLStreamHandler createURLStreamHandler(String s) 
        {
            if(s.equals(PROTOCOL))
            {
                return new AndroidResourceURlHandler();
            }
            return null;
        }
    }

    /**
     * The URL handler that handles Android resource paths redirected to Android
     * resources.
     */
    static private class AndroidResourceURlHandler
        extends URLStreamHandler
    {
        @Override
        protected URLConnection openConnection(URL url)
            throws IOException 
        {
            return new AndroidURLConnection(url);
        }
    }

    /**
     * It does open {@link InputStream} from URLs that were produced for 
     * {@link AndroidResourceURLHandlerFactory#PROTOCOL} protocol.
     */
    static private class AndroidURLConnection
        extends URLConnection
    {

        private int id = 0;

        protected AndroidURLConnection(URL url) 
        {
            super(url);
        }

        @Override
        public void connect() 
            throws IOException 
        {

        }

        @Override
        public InputStream getInputStream() 
            throws IOException 
        {
            String idStr = super.getURL().getHost();
            try
            {
                this.id = Integer.parseInt(idStr);
                return resources.openRawResource(id);
            }
            catch(NumberFormatException exc)
            {
                throw new IOException("Invalid resource id: "+idStr);
            }
        }
    }
}