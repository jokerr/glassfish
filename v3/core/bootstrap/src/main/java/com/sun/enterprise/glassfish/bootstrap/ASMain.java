/*
 * The contents of this file are subject to the terms 
 * of the Common Development and Distribution License 
 * (the License).  You may not use this file except in
 * compliance with the License.
 * 
 * You can obtain a copy of the license at 
 * https://glassfish.dev.java.net/public/CDDLv1.0.html or
 * glassfish/bootstrap/legal/CDDLv1.0.txt.
 * See the License for the specific language governing 
 * permissions and limitations under the License.
 * 
 * When distributing Covered Code, include this CDDL 
 * Header Notice in each file and include the License file 
 * at glassfish/bootstrap/legal/CDDLv1.0.txt.  
 * If applicable, add the following below the CDDL Header, 
 * with the fields enclosed by brackets [] replaced by
 * you own identifying information: 
 * "Portions Copyrighted [year] [name of copyright owner]"
 * 
 * Copyright 2006 Sun Microsystems, Inc. All rights reserved.
 */

package com.sun.enterprise.glassfish.bootstrap;

import com.sun.enterprise.module.bootstrap.StartupContext;
import java.io.*;
import java.util.*;
import java.util.logging.*;
import com.sun.enterprise.module.bootstrap.ArgumentManager;

/**
 * Tag Main to get the manifest file 
 */
public class ASMain {

    /*
     * Most of the code in this file has been moved to ASMainHelper
     *and  ASMainOSGi
     */
    final static Logger logger = Logger.getAnonymousLogger();

    private final static String PLATFORM_PROPERTY_KEY = "GlassFish_Platform";

    // We add both KnopflerFish and Knopflerfish for backward compatibility
    // between tp2 and v3 trunk.
    private enum Platform {HK2, Felix, Knopflerfish, KnopflerFish, Equinox, Static}

    public static void main(final String args[]) {
        setStartupContextProperties(args);
        Platform platform = Platform.Felix; // default is Felix

        // first check the system props
        String temp = System.getProperty(PLATFORM_PROPERTY_KEY);
        if (temp == null || temp.trim().length() <= 0) {
            // not in sys props -- check environment
            temp = System.getenv(PLATFORM_PROPERTY_KEY);
        }

        if (temp != null && temp.trim().length() != 0) {
            platform = Platform.valueOf(temp.trim());
        }

        // Set the system property if downstream code wants to know about it
        System.setProperty(PLATFORM_PROPERTY_KEY, platform.toString());

        AbstractMain delegate=null;
        switch (platform) {
            case Felix:
                logger.info("Launching GlassFish on Apache Felix OSGi platform");
                delegate = new ASMainFelix();
                break;
            case Equinox:
                logger.info("Launching GlassFish on Equinox OSGi platform");
                delegate = new ASMainEquinox();
                break;
            case Knopflerfish:
            case KnopflerFish:
                logger.info("Launching GlassFish on Knopflerfish OSGi platform");
                delegate = new ASMainKnopflerFish();
                break;
            case HK2:
                throw new RuntimeException("GlassFish does not run on the HK2 platform anymore");
            case Static:
                delegate = new ASMainStatic();
                break;
            default:
                throw new RuntimeException("Platform not yet supported");
        }
        if (delegate!=null) {
            try {
                delegate.run(logger, args);
            } catch(Exception e) {
                logger.log(Level.SEVERE, e.getMessage(), e);
            }

        }
    }

    /**
     * Save the args in a system property
     */

    private static void setStartupContextProperties(String... args)
    {
        Properties p = ArgumentManager.argsToMap(args);
        p.put(StartupContext.TIME_ZERO_NAME, (new Long(System.currentTimeMillis())).toString());
        addRawStartupInfo(args, p);

        try {
            Writer writer = new StringWriter();
            p.store(writer, null);
            System.setProperty(ARGS_KEY, writer.toString());
        }
        catch (IOException e) {
            logger.info("Could not save startup parameters, will start with none");
            System.setProperty(ARGS_KEY, "");
        }
    }

    /**
     * Need the raw unprocessed args for RestartDomainCommand in case we were NOT started 
     * by CLI
     *
     * @param args raw args to this main()
     * @param p the properties to save as a system property
     */
    private static void addRawStartupInfo(final String[] args, final Properties p) {
        //package the args...
        StringBuilder sb = new StringBuilder();

        for(int i = 0; i < args.length; i++) {
            if(i > 0)
                sb.append(ARG_SEP);

            sb.append(args[i]);
        }

        if(!wasStartedByCLI(p)) {
            // no sense doing this if we were started by CLI...
            p.put(ORIGINAL_CP, System.getProperty("java.class.path"));
            p.put(ORIGINAL_CN, ASMain.class.getName());
            p.put(ORIGINAL_ARGS, sb.toString());
        }
    }

    private static boolean wasStartedByCLI(final Properties props) {
        // if we were started by CLI there will be some special args set...

        return
            props.getProperty("-asadmin-classpath") != null &&
            props.getProperty("-asadmin-classname") != null &&
            props.getProperty("-asadmin-args")      != null;
    }

    private static final String ARGS_KEY        = "hk2.startup.context.args";
    private static final String ORIGINAL_CP     = "-startup-classpath";
    private static final String ORIGINAL_CN     = "-startup-classname";
    private static final String ORIGINAL_ARGS   = "-startup-args";
    private static final String ARG_SEP         = ",,,";
}
