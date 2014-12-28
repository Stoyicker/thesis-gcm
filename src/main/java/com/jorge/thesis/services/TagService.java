package com.jorge.thesis.services;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Produces;
import java.io.IOException;

public final class TagService extends HttpServlet {

    private static final long serialVersionUID = -9034267862516901563L;

    @Override
    @Produces("application/json")
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        //TODO Get tag (or list of tags) with its (their) last update stamp
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        //TODO One or more tags have been updated
    }
}
