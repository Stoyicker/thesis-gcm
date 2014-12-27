package com.jorge.thesis.gcm;

import com.jorge.thesis.EnvVars;
import com.jorge.thesis.datamodel.CEntityTagClass;
import com.jorge.thesis.io.FileReadUtils;
import com.jorge.thesis.io.net.HTTPRequestsSingleton;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class GCMCommunicatorSingleton {

    private static final Object LOCK = new Object();
    private static final Integer DEFAULT_TAG_SYNC_REQUEST_QUEUE_MAX_SIZE = 20;
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final Integer MAX_AMOUNT_OF_IDS_PER_REQUEST = 950; //Must be 1000 or less
    private static volatile GCMCommunicatorSingleton mInstance;
    private final BlockingQueue<CEntityTagClass.CEntityTag> mTagRequestQueue;

    private GCMCommunicatorSingleton() {
        mTagRequestQueue = new LinkedBlockingQueue<>(DEFAULT_TAG_SYNC_REQUEST_QUEUE_MAX_SIZE);
        new TagSyncRequestConsumerRunnable(mTagRequestQueue).run();
    }

    public static GCMCommunicatorSingleton getInstance() {
        GCMCommunicatorSingleton ret = mInstance;
        if (ret == null) {
            synchronized (LOCK) {
                ret = mInstance;
                if (ret == null) {
                    ret = new GCMCommunicatorSingleton();
                    mInstance = ret;
                }
            }
        }
        return ret;
    }

    /**
     * Queues a sync request for a tag.
     *
     * @param tag {@link com.jorge.thesis.datamodel.CEntityTagClass.CEntityTag} Tag whose sync is
     *            requested.
     * @return <value>TRUE</value> if successful, <value>FALSE</value> if a synchronisation for this tag is already
     * queued.
     */
    public synchronized Boolean queueTagSyncRequest(CEntityTagClass.CEntityTag tag) {
        Boolean ret = Boolean.TRUE;

        if (!mTagRequestQueue.contains(tag))
            try {
                mTagRequestQueue.put(tag); //Inserts at tail
            } catch (InterruptedException e) {
                e.printStackTrace(System.err);
                ret = queueTagSyncRequest(tag, 1D);
            }
        else
            return Boolean.FALSE;

        return ret;
    }

    private synchronized Boolean queueTagSyncRequest(CEntityTagClass.CEntityTag tag, Double exponentialWaitSeconds) {
        final Integer EXPONENTIAL_WAIT_INCREASE_FACTOR = 2;
        Boolean ret = Boolean.TRUE;

        try {
            Thread.sleep((long) (exponentialWaitSeconds * 1000));
        } catch (InterruptedException e) {
            e.printStackTrace(System.err);
            //Will never happen
        }

        if (!mTagRequestQueue.contains(tag))
            try {
                mTagRequestQueue.put(tag); // Inserts at tail
            } catch (InterruptedException e) {
                e.printStackTrace(System.err);
                ret = queueTagSyncRequest(tag, Math.pow(exponentialWaitSeconds, EXPONENTIAL_WAIT_INCREASE_FACTOR));
            }
        else
            return Boolean.FALSE;

        return ret;
    }

    private static class TagSyncRequestConsumerRunnable implements Runnable {

        private static final long INTERRUPTED_TAKE_WAIT_MILLIS = 1000L;
        private static final Integer DEFAULT_SYNC_REQUEST_QUEUE_MAX_SIZE = 100;
        private final String GOOGLE_GCM_URL;
        private final BlockingQueue<CEntityTagClass.CEntityTag> mTagRequestQueue;
        private final BlockingQueue<Request> mSyncRequestQueue;

        public TagSyncRequestConsumerRunnable(BlockingQueue<CEntityTagClass.CEntityTag> _queue) {
            mTagRequestQueue = _queue;
            mSyncRequestQueue = new LinkedBlockingQueue<>(DEFAULT_SYNC_REQUEST_QUEUE_MAX_SIZE);
            try {
                GOOGLE_GCM_URL = IOUtils.toString(FileReadUtils.class.getResourceAsStream
                        ("/gcm_server_url"));
            } catch (IOException e) {
                e.printStackTrace(System.err);
                throw new IllegalStateException("Resource /gcm_server_url not properly loaded.");
            }
            new GCMRequestConsumerRunnable(mSyncRequestQueue).run();
        }

        @Override
        public synchronized void run() {
            //noinspection InfiniteLoopStatement
            while (true) {
                try {
                    sendSyncRequestToAllIds(mTagRequestQueue.take());
                } catch (InterruptedException e) {
                    //Report, take a break and keep on going
                    e.printStackTrace(System.err);
                    try {
                        Thread.sleep(INTERRUPTED_TAKE_WAIT_MILLIS);
                    } catch (InterruptedException e1) {
                        e.printStackTrace(System.err);
                        //Will never happen
                    }
                }
            }
        }

        private synchronized void sendSyncRequestToAllIds(CEntityTagClass.CEntityTag tag) {
            List<Request> requests = createSyncRequests(tag);
            //Inserts at tail
            requests.forEach(this::queueRequestForExecution);
        }

        /**
         * Sends a request to as many ids as possible.
         *
         * @param request {@link com.squareup.okhttp.Request} The request to send.
         */
        private synchronized void queueRequestForExecution(Request request) {
            if (!mSyncRequestQueue.contains(request))
                try {
                    mSyncRequestQueue.put(request); // Inserts at tail
                } catch (InterruptedException e) {
                    e.printStackTrace(System.err);
                    queueRequestForExecution(request, 1D);
                }
        }

        private synchronized void queueRequestForExecution(Request request, Double exponentialWaitSeconds) {
            final Integer EXPONENTIAL_WAIT_INCREASE_FACTOR = 2;

            try {
                Thread.sleep((long) (exponentialWaitSeconds * 1000));
            } catch (InterruptedException e) {
                e.printStackTrace(System.err);
                //Will never happen
            }

            if (!mSyncRequestQueue.contains(request))
                try {
                    mSyncRequestQueue.put(request); // Inserts at tail
                } catch (InterruptedException e) {
                    e.printStackTrace(System.err);
                    queueRequestForExecution(request, Math.pow(exponentialWaitSeconds,
                            EXPONENTIAL_WAIT_INCREASE_FACTOR));
                }
        }

        private synchronized List<Request> createSyncRequests(CEntityTagClass.CEntityTag tag) {
            List<String> targetIds = new ArrayList<>();//TODO DAO.getRegisteredIds(tag);
            targetIds.add("1"); //TODO Remove this when DAO.getRegisteredIds(tag); is done
            targetIds.add("2"); //TODO Remove this when DAO.getRegisteredIds(tag); is done
            targetIds.add("3"); //TODO Remove this when DAO.getRegisteredIds(tag); is done
            List<Request> ret = new LinkedList<>();
            for (Integer i = 0; !targetIds.isEmpty(); i++) {
                List<String> thisGroupOfIds;
                final Integer startIndex = i * MAX_AMOUNT_OF_IDS_PER_REQUEST, lastIndex = (i + 1) *
                        MAX_AMOUNT_OF_IDS_PER_REQUEST;
                if (lastIndex < targetIds.size())
                    thisGroupOfIds = targetIds.subList(startIndex, lastIndex);
                else
                    thisGroupOfIds = targetIds.subList(startIndex, targetIds.size() - 1);
                JSONObject body = new JSONObject();
                try {
                    body.put("registration_ids", new JSONArray(thisGroupOfIds));
                    JSONObject data = new JSONObject();
                    data.put("tag", tag.name());
                    body.put("data", data);
                } catch (JSONException e) {
                    e.printStackTrace(System.err);
                    //Will never happen
                }
                ret.add(new Request.Builder().
                        addHeader("Authorization", EnvVars.API_KEY).
                        addHeader("Content-Type", "application/json").
                        url(GOOGLE_GCM_URL).
                        post(RequestBody.create(JSON, body.toString())).build());
            }
            return ret;
        }
    }

    public static class GCMRequestConsumerRunnable implements Runnable {

        private static final long INTERRUPTED_TAKE_WAIT_MILLIS = 1000L;
        private final BlockingQueue<Request> mRequestQueue;

        public GCMRequestConsumerRunnable(BlockingQueue<Request> _requestQueue) {
            mRequestQueue = _requestQueue;
        }

        @Override
        public synchronized void run() {
            //noinspection InfiniteLoopStatement
            while (true) {
                try {
                    HTTPRequestsSingleton.getInstance().performRequest(mRequestQueue.take());
                } catch (InterruptedException e) {
                    //Report, take a break and keep on going
                    e.printStackTrace(System.err);
                    try {
                        Thread.sleep(INTERRUPTED_TAKE_WAIT_MILLIS);
                    } catch (InterruptedException e1) {
                        e.printStackTrace(System.err);
                        //Will never happen
                    }
                }
            }
        }
    }
}