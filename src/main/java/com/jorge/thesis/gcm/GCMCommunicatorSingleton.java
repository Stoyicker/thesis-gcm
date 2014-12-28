package com.jorge.thesis.gcm;

import com.jorge.thesis.datamodel.CEntityTagClass;
import com.jorge.thesis.io.database.DBDAOSingleton;
import com.jorge.thesis.io.file.FileReadUtils;
import com.jorge.thesis.io.net.HTTPRequestsSingleton;
import com.jorge.thesis.util.EnvVars;
import com.jorge.thesis.util.TimeUtils;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;

public final class GCMCommunicatorSingleton {

    private static final Object LOCK = new Object();
    private static final Integer DEFAULT_TAG_SYNC_REQUEST_QUEUE_MAX_SIZE = 200;
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final Integer MAX_AMOUNT_OF_IDS_PER_REQUEST = 950; //Must be 1000 or less
    private static final Long TAG_SYNC_REQUEST_INITIAL_DELAY = 55L;
    private static final Integer EXPONENTIAL_WAIT_INCREASE_FACTOR = 2;
    private static volatile GCMCommunicatorSingleton mInstance;
    private final BlockingQueue<CDelayedTag> mTagRequestQueue;
    private final TagSyncRequestConsumerRunnable mTagSyncRequestConsumer;

    private GCMCommunicatorSingleton() {
        mTagRequestQueue = new LinkedBlockingQueue<>(DEFAULT_TAG_SYNC_REQUEST_QUEUE_MAX_SIZE);
        mTagSyncRequestConsumer = new TagSyncRequestConsumerRunnable(mTagRequestQueue);
        mTagSyncRequestConsumer.run();
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
        final CDelayedTag wrapper = new CDelayedTag(tag, TAG_SYNC_REQUEST_INITIAL_DELAY, TimeUnit
                .MILLISECONDS);

        if (!mTagRequestQueue.contains(wrapper))
            try {
                mTagRequestQueue.put(wrapper); //Inserts at tail
            } catch (InterruptedException e) {
                e.printStackTrace(System.err);
                ret = queueDelayedTagSyncRequest(wrapper);
            }
        else
            return Boolean.FALSE;

        return ret;
    }

    private synchronized Boolean queueDelayedTagSyncRequest(CDelayedTag tag) {
        Boolean ret = Boolean.TRUE;

        if (!mTagRequestQueue.contains(tag))
            try {
                mTagRequestQueue.put(tag); // Inserts at tail
            } catch (InterruptedException e) {
                e.printStackTrace(System.err);
                ret = queueDelayedTagSyncRequest(new CDelayedTag(tag, (long) Math.pow(tag.getDelay(TimeUnit
                                .MILLISECONDS),
                        EXPONENTIAL_WAIT_INCREASE_FACTOR), TimeUnit.MILLISECONDS));
            }
        else
            return Boolean.FALSE;

        return ret;
    }

    public void delayAndQueueRequestForExecution(CDelayedRequest delayedRequest) {
        mTagSyncRequestConsumer.delayAndQueueRequestForExecution(delayedRequest);
    }

    private static class CDelayedTag implements Delayed {
        private CEntityTagClass.CEntityTag mTag;
        private Long mDelay;
        private TimeUnit mDelayUnit;

        public CDelayedTag(CDelayedTag _tag, Long _delay, TimeUnit _unit) {
            this(_tag.getPureTag(), _delay, _unit);
        }

        public CDelayedTag(CEntityTagClass.CEntityTag _tag, Long _delay, TimeUnit _unit) {
            mTag = _tag;
            mDelay = _delay;
            mDelayUnit = _unit;
        }

        @Override
        public int compareTo(@SuppressWarnings("NullableProblems") Delayed o) {
            if (o instanceof CDelayedTag) {
                return TimeUtils.convertTimeTo(mDelay, mDelayUnit, TimeUnit.MILLISECONDS).compareTo(o.getDelay(TimeUnit
                        .MILLISECONDS));
            } else
                throw new UnsupportedOperationException(getClass().getName() + " objects can only be compared as " +
                        "CEntityDelayedTag " +
                        "among " +
                        "themselves.");
        }

        @Override
        public long getDelay(@SuppressWarnings("NullableProblems") TimeUnit unit) {
            return TimeUtils.convertTimeTo(mDelay, mDelayUnit, unit);
        }

        public CEntityTagClass.CEntityTag getPureTag() {
            return mTag;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof CDelayedTag))
                throw new UnsupportedOperationException(getClass().getName() + " objects can only be compared for " +
                        "equality as " +
                        "CDelayedTag " +
                        "among " +
                        "themselves.");
            else
                return getPureTag().equals(((CDelayedTag) obj).getPureTag());
        }
    }

    private static class TagSyncRequestConsumerRunnable implements Runnable {

        private static final long INTERRUPTED_TAKE_WAIT_MILLIS = 1000L;
        private final String GOOGLE_GCM_URL;
        private final BlockingQueue<CDelayedTag> mTagRequestQueue;
        private final BlockingQueue<CDelayedRequest> mSyncRequestQueue;

        public TagSyncRequestConsumerRunnable(BlockingQueue<CDelayedTag> _queue) {
            mTagRequestQueue = _queue;
            mSyncRequestQueue = new DelayQueue<>();
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

        private synchronized void sendSyncRequestToAllIds(CDelayedTag tag) {
            List<CDelayedRequest> requests = createSyncRequests(tag);
            //Inserts at tail
            requests.forEach(this::delayAndQueueRequestForExecution);
        }

        /**
         * Queues a delayed request to be sent to as many ids as possible.
         *
         * @param request {@link CDelayedRequest} The request to send.
         */
        synchronized void delayAndQueueRequestForExecution(CDelayedRequest request) {
            request = new CDelayedRequest(request, (long) Math.pow(request.getDelay
                            (TimeUnit
                                    .MILLISECONDS),
                    EXPONENTIAL_WAIT_INCREASE_FACTOR), TimeUnit.MILLISECONDS);

            if (!mSyncRequestQueue.contains(request))
                try {
                    mSyncRequestQueue.put(request); // Inserts at tail
                } catch (InterruptedException e) {
                    e.printStackTrace(System.err);
                    delayAndQueueRequestForExecution(new CDelayedRequest(request, (long) Math.pow(request.getDelay
                                    (TimeUnit
                                            .MILLISECONDS),
                            EXPONENTIAL_WAIT_INCREASE_FACTOR), TimeUnit.MILLISECONDS));
                }
        }

        private synchronized List<CDelayedRequest> createSyncRequests(CDelayedTag tag) {
            List<String> targetIds = DBDAOSingleton.getInstance().getRegisteredIds(tag.getPureTag());
            List<CDelayedRequest> ret = new LinkedList<>();
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
                    data.put("tag", tag.getPureTag().name());
                    body.put("data", data);
                } catch (JSONException e) {
                    e.printStackTrace(System.err);
                    //Will never happen
                }
                ret.add(new CDelayedRequest(new Request.Builder().
                        addHeader("Authorization", EnvVars.API_KEY).
                        addHeader("Content-Type", "application/json").
                        url(GOOGLE_GCM_URL).
                        post(RequestBody.create(JSON, body.toString())).build(), tag.getDelay(TimeUnit.MILLISECONDS),
                        TimeUnit.MILLISECONDS));
            }
            return ret;
        }
    }

    public static class GCMRequestConsumerRunnable implements Runnable {

        private static final long INTERRUPTED_TAKE_WAIT_MILLIS = 1000L;
        private final BlockingQueue<CDelayedRequest> mRequestQueue;

        public GCMRequestConsumerRunnable(BlockingQueue<CDelayedRequest> _requestQueue) {
            mRequestQueue = _requestQueue;
        }

        @Override
        public synchronized void run() {
            //noinspection InfiniteLoopStatement
            while (true) {
                try {
                    CDelayedRequest thisRequest = mRequestQueue.take();
                    new GCMRequestExecutor(thisRequest).run();
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

        private static class GCMRequestExecutor implements Runnable {
            CDelayedRequest mDelayedRequest;

            private GCMRequestExecutor(CDelayedRequest _request) {
                mDelayedRequest = _request;
            }

            @Override
            public void run() {
                GCMResponseHandlerSingleton.getInstance().handleGCMResponse(mDelayedRequest, HTTPRequestsSingleton
                        .getInstance().performRequest(mDelayedRequest.getPureRequest()));
            }
        }
    }
}