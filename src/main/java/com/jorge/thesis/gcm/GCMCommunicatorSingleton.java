package com.jorge.thesis.gcm;

import com.jorge.thesis.datamodel.CEntityTagClass;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class GCMCommunicatorSingleton {

    private static final Object LOCK = new Object();
    private static final Integer DEFAULT_SYNC_REQUEST_MAX_SIZE = 20;
    private static volatile GCMCommunicatorSingleton instance;
    private final BlockingQueue<CEntityTagClass.CEntityTag> syncRequestQueue;

    private GCMCommunicatorSingleton() {
        syncRequestQueue = new LinkedBlockingQueue<>(DEFAULT_SYNC_REQUEST_MAX_SIZE);
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

    public synchronized Boolean queueTagSyncRequest(CEntityTagClass.CEntityTag tag) {
        Boolean ret = Boolean.TRUE;

        if (!syncRequestQueue.contains(tag))
            try {
                syncRequestQueue.put(tag); // Inserts at tail
            } catch (InterruptedException e) {
                e.printStackTrace(System.err);
                ret = Boolean.FALSE;
            }

        return ret;
    }
}
