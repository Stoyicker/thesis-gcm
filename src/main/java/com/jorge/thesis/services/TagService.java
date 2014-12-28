package com.jorge.thesis.services;

import com.jorge.thesis.datamodel.CEntityTagManager;

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
        resp.getWriter().print(CEntityTagManager.generateAllCurrentTagsAsJSONText());
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String requestType = req.getParameter("type"), deviceId = req.getParameter("id"), paramTags = req
                .getParameter("tags");

        if (requestType != null && paramTags != null) {
            switch (requestType.toLowerCase()) {
                case "update": //Request sent by the file server
                    final StringTokenizer stringTokenizer = new StringTokenizer(paramTags, TAG_SEPARATOR);

                    while (stringTokenizer.hasMoreTokens()) {
                        CEntityTagManager.createTagSyncRequest(stringTokenizer.nextToken());
                    }

                    resp.setStatus(HttpServletResponse.SC_OK);
                    break;
                case "subscribe": //Request sent by a device
                    if (deviceId != null) {
                        //TODO Subscribe device to tags
                    } else
                        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    break;
                case "unsubscribe": //Request sent by a device
                    if (deviceId != null) {
                        //TODO Unsubscribe device from tags
                    } else
                        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    break;
                default:
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            }
        } else
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    }
}
