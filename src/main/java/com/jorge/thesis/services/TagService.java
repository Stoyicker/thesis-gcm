package com.jorge.thesis.services;

import com.jorge.thesis.datamodel.CEntityTagManager;
import com.jorge.thesis.io.database.DBDAOSingleton;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Produces;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

public final class TagService extends HttpServlet {

    private static final long serialVersionUID = -9034267862516901563L;
    private static final String TAG_SEPARATOR = "+";

    @Override
    @Produces("application/json")
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        final String requestType = req.getParameter("type");
        if (requestType == null)
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        else {
            if (requestType.toLowerCase().contentEquals("list")) {
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().print(CEntityTagManager.generateAllCurrentTagsAsJSONArray());
            } else {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        final String requestType = req.getParameter("type"), deviceId = req.getParameter("id"), paramTags = req
                .getParameter("tags");

        if (requestType != null && paramTags != null) {
            final List<String> tagList = new LinkedList<>();
            final StringTokenizer allTagsTokenizer = new StringTokenizer(paramTags, TAG_SEPARATOR);
            final Pattern tagFormatPattern = Pattern.compile("[a-z0-9_]+");
            while (allTagsTokenizer.hasMoreTokens()) {
                final String token = allTagsTokenizer.nextToken().toLowerCase().trim();
                if (!tagFormatPattern.matcher(token).matches()) {
                    resp.setStatus(HttpServletResponse.SC_EXPECTATION_FAILED);
                    return;
                } else
                    tagList.add(token);
            }
            switch (requestType) {
                case "sync": //Request sent by the file server
                    tagList.forEach(CEntityTagManager::createTagSyncRequest);

                    resp.setStatus(HttpServletResponse.SC_OK);
                    break;
                case "subscribe": //Request sent by a device
                    if (deviceId != null) {
                        if (DBDAOSingleton.getInstance().addSubscriptions(deviceId, tagList))
                            resp.setStatus(HttpServletResponse.SC_OK);
                        else
                            resp.setStatus(HttpServletResponse.SC_GONE);
                    } else
                        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    break;
                case "unsubscribe": //Request sent by a device
                    if (deviceId != null) {
                        if (DBDAOSingleton.getInstance().removeSubscriptions(deviceId, tagList))
                            resp.setStatus(HttpServletResponse.SC_OK);
                        else
                            resp.setStatus(HttpServletResponse.SC_GONE);
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
