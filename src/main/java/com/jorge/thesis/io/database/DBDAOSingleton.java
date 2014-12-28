package com.jorge.thesis.io.database;

import com.jorge.thesis.datamodel.CEntityTagClass;

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
     * TODO addTag
     */
    public Boolean addTag(String tagName) {
        return Boolean.FALSE;
    }

    /**
     * TODO getRegisteredIds
     */
    public List<String> getRegisteredIds(CEntityTagClass.CEntityTag tag) {
        return null;
    }

    /**
     * TODO removeRegistrationIdFromAllTags
     */
    public Boolean updateRegistrationIdOnAllTags(String oldId, String newId) {
        System.out.println("Replacing id " + oldId + " by new id " + newId);
        return Boolean.FALSE;
    }

    /**
     * TODO removeRegistrationIdFromAllTags
     */
    public Boolean removeRegistrationIdFromAllTags(String regId) {
        System.out.println("Removing registration id " + regId);
        return Boolean.FALSE;
    }
}
