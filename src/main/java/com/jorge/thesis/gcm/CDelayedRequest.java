package com.jorge.thesis.gcm;

import com.jorge.thesis.util.TimeUtils;
import com.squareup.okhttp.Request;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

public class CDelayedRequest implements Delayed {

    private final Long mDelay;
    private final TimeUnit mDelayUnit;
    private final Request mRequest;

    public CDelayedRequest(CDelayedRequest _request, Long _delay, TimeUnit _unit) {
        this(_request.getPureRequest(), _delay, _unit);
    }

    public CDelayedRequest(Request _request, Long _delay, TimeUnit _unit) {
        mRequest = _request;
        mDelay = _delay;
        mDelayUnit = _unit;
    }

    @Override
    public long getDelay(@SuppressWarnings("NullableProblems") TimeUnit unit) {
        return TimeUtils.convertTimeTo(mDelay, mDelayUnit, unit);
    }

    public Request getPureRequest() {
        return mRequest;
    }

    @Override
    public int compareTo(@SuppressWarnings("NullableProblems") Delayed o) {
        if (o instanceof CDelayedRequest) {
            return TimeUtils.convertTimeTo(mDelay, mDelayUnit, TimeUnit.MILLISECONDS).compareTo(o.getDelay(TimeUnit
                    .MILLISECONDS));
        } else
            throw new UnsupportedOperationException(getClass().getName() + " objects can only be compared as " +
                    "CDelayedRequests " +
                    "among " +
                    "themselves.");
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof CDelayedRequest))
            throw new UnsupportedOperationException(getClass().getName() + " objects can only be compared for " +
                    "equality as " +
                    "CDelayedRequests " +
                    "among " +
                    "themselves.");
        else
            return getPureRequest().equals(((CDelayedRequest) obj).getPureRequest());
    }
}
