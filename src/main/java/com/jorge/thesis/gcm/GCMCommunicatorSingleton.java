package com.jorge.thesis.gcm;

import com.jorge.thesis.datamodel.CEntityTagClass;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class GCMCommunicatorSingleton {

    private static final Object LOCK = new Object();
    private static final Integer DEFAULT_SYNC_REQUEST_MAX_SIZE = 20;
    private static volatile GCMCommunicatorSingleton instance;
    private final BlockingQueue<CEntityTagClass.CEntityTag> syncRequestQueue;

    private GCMCommunicatorSingleton() {
        syncRequestQueue = new ArrayBlockingQueue<>(DEFAULT_SYNC_REQUEST_MAX_SIZE);
    }

    public static GCMCommunicatorSingleton getInstance() {
        GCMCommunicatorSingleton ret = instance;
        if (ret == null) {
            synchronized (LOCK) {
                ret = instance;
                if (ret == null) {
                    ret = new GCMCommunicatorSingleton();
                    instance = ret;
                }
            }
        }
        return ret;
    }

    public void queueTagSyncRequest(CEntityTagClass tag) {
    }
}
