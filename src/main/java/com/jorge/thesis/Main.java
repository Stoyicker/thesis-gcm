package com.jorge.thesis;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import com.jorge.thesis.services.MessagingService;

public class Main {

    public static void main(String[] args) throws Exception {
        String webPort = System.getenv("PORT");
        if ((webPort == null) || webPort.isEmpty()) {
            webPort = "8080";
        }

        Server server = new Server(Integer.valueOf(webPort));
        ServletContextHandler context = new ServletContextHandler(
                ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);
        context.addServlet(new ServletHolder(new MessagingService()),
                "/services/messages");

        System.out.print("Requesting server start...");
        server.start();
        System.out.println("done.");


        server.join();
    }
}
