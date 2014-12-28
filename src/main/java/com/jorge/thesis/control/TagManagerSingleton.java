package com.jorge.thesis.control;

import com.jorge.thesis.datamodel.CEntityTagClass;
import com.jorge.thesis.gcm.GCMCommunicatorSingleton;
import com.jorge.thesis.io.enumrefl.EnumBuster;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedList;
import java.util.List;

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
            final EnumBuster<CEntityTagClass.CEntityTag> buster =
                    new EnumBuster<>(CEntityTagClass.CEntityTag.class,
                            CEntityTagClass.class);

            final List<String> l = new LinkedList<>();
            l.add(s);

            if (CEntityTagClass.createTagsFromStringList(buster, l) == 1) { //Return is amount of newly added tags
                //TODO add it to the database
            }
            GCMCommunicatorSingleton.getInstance().queueTagSyncRequest(CEntityTagClass.CEntityTag.valueOf(s));
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
