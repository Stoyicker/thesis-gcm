package com.jorge.thesis.control;

import com.jorge.thesis.datamodel.CEntityTagClass;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public final class TagManagerSingleton {

    private static final Object LOCK = new Object();
    private static final Object TAG_ACCESS_LOCK = new Object();
    private static volatile TagManagerSingleton mInstance;

    private TagManagerSingleton() {
    }

    public static TagManagerSingleton getInstance() {
        TagManagerSingleton ret = mInstance;
        if (ret == null) {
            synchronized (LOCK) {
                ret = mInstance;
                if (ret == null) {
                    ret = new TagManagerSingleton();
                    mInstance = ret;
                }
            }
        }
        return ret;
    }

    public synchronized void createTagSyncRequest(String s) {
        synchronized (TAG_ACCESS_LOCK) {
        /*TODO
        if it doesn't exist in the enum, then {
            add it to the enum
            add it to the database
        }
        make the sync request
        */
        }
    }

    public synchronized String generateAllCurrentTagsAsJSONText() {
        synchronized (TAG_ACCESS_LOCK) {
            JSONObject ret = new JSONObject();
            JSONArray array = new JSONArray();

            array.put(CEntityTagClass.CEntityTag.values());

            try {
                ret.put("tags", array);
            } catch (JSONException e) {
                e.printStackTrace(System.err);
                //Should never happen
            }

            return ret.toString();
        }
    }
}
