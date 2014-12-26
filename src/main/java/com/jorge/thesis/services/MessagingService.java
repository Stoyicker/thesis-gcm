package com.jorge.thesis.services;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Produces;
import java.io.IOException;

public final class MessagingService extends HttpServlet {

    private static final long serialVersionUID = -9034267862516901563L;

    @Override
    @Produces("application/json") //Returns the...message id? And, the success of the operation AS STATUS CODE
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        //TODO Get messages
    }

    @Override
    //Returns the success of the operation AS STATUS CODE
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        //TODO Add messages
    }

    @Override
    //Returns the success of the operation AS STATUS CODE
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        //TODO Update messages
    }

    @Override
    //Returns the success of the operation AS STATUS CODE
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        //TODO Delete messages
    }
}
