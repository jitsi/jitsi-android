/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.impl.osgi.framework;

import android.content.*;
import dalvik.system.*;
import org.jitsi.android.*;

import java.io.*;

/**
 * Because of dex methods limit of 65535 some implementation bundles are packed
 * into separate dex file during build process and later loaded using this class
 * loader.
 *
 * This class loader is used by simplified Android OSGi implementation to load
 * bundle activator classes. Currently only bundles not referenced directly from
 * Android code can be loaded this way.
 *
 * @author Pawel Domas
 */
public class LibDexLoader
{
    /**
     * Instance of <tt>LibDexLoader</tt>
     */
    public static final LibDexLoader instance = new LibDexLoader();

    /**
     * <tt>DexClassLoader</tt> used by this instance to load classes from asset
     * dex file.
     */
    private final DexClassLoader dexClassLoader;

    /**
     * Private constructor for this class.
     */
    private LibDexLoader()
    {
        String assetDexName = "jitsi-bundles-dex.jar";
        // Before the dex file can be processed by the DexClassLoader,
        // it has to be first copied from asset resource to a storage location.
        Context ctx = JitsiApplication.getGlobalContext();
        File dexInternalStoragePath
                = new File(ctx.getDir("dex", Context.MODE_PRIVATE),
                                             assetDexName);

        BufferedInputStream bis = null;
        OutputStream dexWriter = null;

        final int BUF_SIZE = 2 * 1024;
        try
        {
            bis = new BufferedInputStream(
                    ctx.getAssets().open(assetDexName));
            dexWriter = new BufferedOutputStream(
                    new FileOutputStream(dexInternalStoragePath));
            byte[] buf = new byte[BUF_SIZE];
            int len;
            while((len = bis.read(buf, 0, BUF_SIZE)) > 0)
            {
                dexWriter.write(buf, 0, len);
            }
            dexWriter.close();
            bis.close();

            // Internal storage where the DexClassLoader writes
            // the optimized dex file to
            final File optimizedDexOutputPath
                    = ctx.getDir("outdex", Context.MODE_PRIVATE);

            this.dexClassLoader
                    = new DexClassLoader(
                            dexInternalStoragePath.getAbsolutePath(),
                            optimizedDexOutputPath.getAbsolutePath(),
                            null,
                            getClass().getClassLoader());
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Loads class for given name from asset dex file.
     * @param name the name of the class to load.
     * @return class for given name loaded from asset dex file.
     */
    public Class loadClass(String name)
    {
        try
        {
            return dexClassLoader.loadClass(name);
        }
        catch (ClassNotFoundException e)
        {
            throw new RuntimeException(e);
        }
    }
}
