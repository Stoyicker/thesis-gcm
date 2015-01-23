package com.jorge.thesis;

import com.jorge.thesis.datamodel.CEntityTagManager;
import com.jorge.thesis.io.database.DBDAOSingleton;
import com.jorge.thesis.services.TagService;
import com.jorge.thesis.util.EnvVars;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import java.text.MessageFormat;

public class Main {

    private static final Integer DEFAULT_PORT = 60000, MINIMUM_PORT = 50000, MAXIMUM_PORT = 65000;

    public static void main(String[] args) throws Exception {
        Integer webPort;
        try {
            webPort = Integer.valueOf(EnvVars.PORT);
            if (webPort < MINIMUM_PORT || webPort > MAXIMUM_PORT) {
                throw new NumberFormatException(MessageFormat.format("Invalid port, use a number between {0} and " +
                        "{1}", MINIMUM_PORT, MAXIMUM_PORT));
            }
        } catch (NumberFormatException ex) {
            webPort = DEFAULT_PORT;
        }

        DBDAOSingleton.getInstance().createDatabaseEnvironment();

        if (CEntityTagManager.instantiateTagSet()) {
            System.out.println("Initialised tags: ");
            for (Object x : CEntityTagManager.CEntityTag.values())
                System.out.println(x);
        } else System.out.println("No tags were loaded");

        Server server = new Server(webPort);
        ServletContextHandler context = new ServletContextHandler(
                ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);
        context.addServlet(new ServletHolder(new TagService()),
                "/tags");

        System.out.print("Requesting server start...");
        server.start();
        System.out.println("done.");

        server.join();
    }
}
