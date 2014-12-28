package com.jorge.thesis.services;

import com.jorge.thesis.control.TagManagerSingleton;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Produces;
import java.io.IOException;
import java.util.StringTokenizer;

public final class TagService extends HttpServlet {

    private static final long serialVersionUID = -9034267862516901563L;
    private static final String TAG_SEPARATOR = ",";

    @Override
    @Produces("application/json")
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.getWriter().print(TagManagerSingleton.getInstance().generateAllCurrentTagsAsJSONText());
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String paramTags = req.getParameter("tags");
        if (paramTags != null) {
            final StringTokenizer stringTokenizer = new StringTokenizer(paramTags, TAG_SEPARATOR);

            while (stringTokenizer.hasMoreTokens()) {
                TagManagerSingleton.getInstance().createTagSyncRequest(stringTokenizer.nextToken());
            }

            resp.setStatus(HttpServletResponse.SC_OK);
        } else
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    }
}
