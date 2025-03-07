/*
 * Copyright 2024 Codetoil
 * Copyright 2012-2023 MultiMC Contributors
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

package org.multimc.onesix;

import org.multimc.*;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class OneSixLauncher implements Launcher
{
    private List<String> mcparams;
    private List<String> traits;
    private String mainClass;

    // secondary parameters
    private int winSizeW;
    private int winSizeH;
    private boolean maximize;
    private String cwd;

    private String serverAddress;
    private String serverPort;
    private boolean useQuickPlay;

    private String joinWorld;

    // the much abused system classloader, for convenience (for further abuse)
    private ClassLoader cl;

    private void processParams(ParamBucket params) throws NotFoundException
    {
        // parameters, separated from ParamBucket
        mcparams = params.allSafe("param", new ArrayList<>() );
        mainClass = params.firstSafe("mainClass", "net.minecraft.client.Minecraft");
        traits = params.allSafe("traits", new ArrayList<>());
        String windowParams = params.firstSafe("windowParams", "854x480");

        String instanceTitle = params.firstSafe("instanceTitle", "Minecraft");
        String instanceIconId = params.firstSafe("instanceIconId", "default");

        // NOTE: this is included for the CraftPresence mod
        System.setProperty("multimc.instance.title", instanceTitle);
        System.setProperty("multimc.instance.icon", instanceIconId);

        serverAddress = params.firstSafe("serverAddress", null);
        serverPort = params.firstSafe("serverPort", null);
        useQuickPlay = params.firstSafe("useQuickPlay").startsWith("1");
        joinWorld = params.firstSafe("joinWorld", null);

        cwd = System.getProperty("user.dir");

        winSizeW = 854;
        winSizeH = 480;
        maximize = false;

        String[] dimStrings = windowParams.split("x");

        if (windowParams.equalsIgnoreCase("max"))
        {
            maximize = true;
        }
        else if (dimStrings.length == 2)
        {
            try
            {
                winSizeW = Integer.parseInt(dimStrings[0]);
                winSizeH = Integer.parseInt(dimStrings[1]);
            } catch (NumberFormatException ignored) {}
        }
    }

    int legacyLaunch()
    {
        // Get the Minecraft Class and set the base folder
        Class<?> mc;
        try
        {
            mc = cl.loadClass(mainClass);

            Field f = Utils.getMCPathField(mc);

            if (f == null)
            {
                System.err.println("Could not find Minecraft path field.");
            }
            else
            {
                f.setAccessible(true);
                f.set(null, new File(cwd));
            }
        } catch (Exception e)
        {
            System.err.println("Could not set base folder. Failed to find/access Minecraft main class:");
            e.printStackTrace(System.err);
            return -1;
        }

        // init params for the main method to chomp on.
        String[] paramsArray = mcparams.toArray(new String[0]);
        try
        {
            Method meth = mc.getMethod("main", String[].class);
            meth.setAccessible(true);
            meth.invoke(null, (Object) paramsArray);
            return 0;
        } catch (Exception e)
        {
            Utils.log("Failed to invoke the Minecraft main class:", "Fatal");
            (e instanceof InvocationTargetException ? e.getCause() : e).printStackTrace(System.err);
            return -1;
        }
    }

    int launchWithMainClass()
    {
        // window size, title and state, onesix
        // FIXME: there is no good way to maximize the minecraft window in onesix.
        if (!maximize)
        {
            mcparams.add("--width");
            mcparams.add(Integer.toString(winSizeW));
            mcparams.add("--height");
            mcparams.add(Integer.toString(winSizeH));
        }

        if (joinWorld != null)
        {
            mcparams.add("--quickPlaySingleplayer");
            mcparams.add(joinWorld);
        }

        if (serverAddress != null)
        {
            if (useQuickPlay)
            {
                mcparams.add("--quickPlayMultiplayer");
                mcparams.add(serverAddress + ":" + serverPort);
            }
            else
            {
                mcparams.add("--server");
                mcparams.add(serverAddress);
                mcparams.add("--port");
                mcparams.add(serverPort);
            }
        }

        // Get the Minecraft Class.
        Class<?> mc;
        try
        {
            mc = cl.loadClass(mainClass);
        } catch (ClassNotFoundException e)
        {
            System.err.println("Failed to find Minecraft main class:");
            e.printStackTrace(System.err);
            return -1;
        }

        // get the main method.
        Method meth;
        try
        {
            meth = mc.getMethod("main", String[].class);
            meth.setAccessible(true);
        } catch (NoSuchMethodException | SecurityException e)
        {
            System.err.println("Failed to acquire the main method:");
            e.printStackTrace(System.err);
            return -1;
        }

        // init params for the main method to chomp on.
        String[] paramsArray = mcparams.toArray(new String[0]);
        try
        {
            // static method doesn't have an instance
            meth.invoke(null, (Object) paramsArray);
        } catch (Exception e)
        {
            System.err.println("Failed to start Minecraft:");
            (e instanceof InvocationTargetException ? e.getCause() : e).printStackTrace(System.err);
            return -1;
        }
        return 0;
    }

    @Override
    public int launch(ParamBucket params)
    {
        // get and process the launch script params
        try
        {
            processParams(params);
        } catch (NotFoundException e)
        {
            System.err.println("Not enough arguments.");
            e.printStackTrace(System.err);
            return -1;
        }

        // grab the system classloader and ...
        cl = ClassLoader.getSystemClassLoader();

        if (traits.contains("legacyLaunch") || traits.contains("alphaLaunch") )
        {
            // legacy launch uses the applet wrapper
            return legacyLaunch();
        }
        else
        {
            // normal launch just calls main()
            return launchWithMainClass();
        }
    }
}
