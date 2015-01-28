package com.jorge.thesis.services;

import com.jorge.thesis.datamodel.CEntityTagManager;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

public final class TagService extends HttpServlet {

    private static final String TAG_SEPARATOR = "-";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        synchronized (this) {
            final String requestType = req.getParameter("type"); //Request sent by a device to retrieve all tags
            if (requestType == null)
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            else {
                if (requestType.toLowerCase(Locale.ENGLISH).contentEquals("list")) {
                    resp.setStatus(HttpServletResponse.SC_OK);
                    resp.getWriter().print(CEntityTagManager.generateAllCurrentTagsAsJSONArray());
                } else {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                }
            }

            resp.setContentType("application/json");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        synchronized (this) {
            final String requestType = req.getParameter("type"), deviceId = req.getParameter("id"), paramTags = req
                    .getParameter("tags");

            if (requestType != null && paramTags != null) {
                final List<String> tagList = new LinkedList<>();
                final StringTokenizer allTagsTokenizer = new StringTokenizer(paramTags, TAG_SEPARATOR);
                final Pattern tagFormatPattern = Pattern.compile("[a-z0-9_]+");
                while (allTagsTokenizer.hasMoreTokens()) {
                    final String token = allTagsTokenizer.nextToken().toLowerCase(Locale.ENGLISH).trim();
                    if (!tagFormatPattern.matcher(token).matches()) {
                        System.err.println("Expectation failed for tag " + token);
                        resp.setStatus(HttpServletResponse.SC_EXPECTATION_FAILED);
                        return;
                    } else if (!tagList.contains(token))
                        tagList.add(token);
                }
                switch (requestType) {
                    case "sync": //Request sent by the file server
                        System.out.println("Sync requested for tags " + tagList);
                        resp.setStatus(deviceId == null || CEntityTagManager.subscribeRegistrationIdToTags(deviceId, tagList) ? HttpServletResponse.SC_OK : HttpServletResponse.SC_GONE);
                        tagList.forEach(CEntityTagManager::createTagSyncRequest);
                        break;
                    case "subscribe": //Request sent by a device
                        if (deviceId != null) {
                            if (CEntityTagManager.subscribeRegistrationIdToTags(deviceId, tagList))
                                resp.setStatus(HttpServletResponse.SC_OK);
                            else
                                resp.setStatus(HttpServletResponse.SC_GONE);
                        } else
                            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        break;
                    case "unsubscribe": //Request sent by a device
                        if (deviceId != null) {
                            if (CEntityTagManager.unsubscribeRegistrationIdFromTags(deviceId, tagList))
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
}
