package com.jorge.thesis.io.database;

import com.jorge.thesis.datamodel.CEntityTagClass;

import java.util.LinkedList;
import java.util.List;

public final class DBDAOSingleton {

    private static final Object LOCK = new Object();
    private static final Object DB_ACCESS_LOCK = new Object();
    private static volatile DBDAOSingleton mInstance;

    private DBDAOSingleton() {
    }

    public static DBDAOSingleton getInstance() {
        DBDAOSingleton ret = mInstance;
        if (ret == null) {
            synchronized (LOCK) {
                ret = mInstance;
                if (ret == null) {
                    ret = new DBDAOSingleton();
                    mInstance = ret;
                }
            }
        }
        return ret;
    }

    /**
     * TODO Add a tag
     */
    public Boolean addTag(String tagName) {
        synchronized (DB_ACCESS_LOCK) {
            System.out.println("Added tag " + tagName + " to database..");
            return Boolean.FALSE;
        }
    }

    /**
     * TODO Get ids currently registered for a tag
     */
    public List<String> getRegisteredIds(CEntityTagClass.CEntityTag tag) {
        synchronized (DB_ACCESS_LOCK) {
            List<String> ret = new LinkedList<>();

            System.out.println("Requested ids registered for tag " + tag.name() + ": " + ret.toString());
            return ret;
        }
    }

    /**
     * TODO Update registration id on all tags
     */
    public Boolean updateRegistrationIdOnAllTags(String oldId, String newId) {
        synchronized (DB_ACCESS_LOCK) {
            System.out.println("Replacing id " + oldId + " by new id " + newId);
            return Boolean.FALSE;
        }
    }

    /**
     * TODO Remove registration id on all tags
     */
    public Boolean removeRegistrationIdFromAllTags(String regId) {
        synchronized (DB_ACCESS_LOCK) {
            System.out.println("Removing registration id " + regId);
            return Boolean.FALSE;
        }
    }

    /**
     * TODO Get all current tags
     */
    public List<String> getTagsNow() {
        synchronized (DB_ACCESS_LOCK) {
            List<String> ret = new LinkedList<>();

            System.out.println("Retrieved database tags " + ret.toString());
            return ret;
        }
    }
}
