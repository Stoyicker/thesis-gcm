package com.jorge.thesis.gcm;

import com.jorge.thesis.io.net.HTTPRequestsSingleton;
import com.squareup.okhttp.Response;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Objects;

public class GCMResponseHandlerSingleton {

    private static final Object LOCK = new Object();
    private static volatile GCMResponseHandlerSingleton mInstance;

    private GCMResponseHandlerSingleton() {
    }

    static GCMResponseHandlerSingleton getInstance() {
        GCMResponseHandlerSingleton ret = mInstance;
        if (ret == null) {
            synchronized (LOCK) {
                ret = mInstance;
                if (ret == null) {
                    ret = new GCMResponseHandlerSingleton();
                    mInstance = ret;
                }
            }
        }
        return ret;
    }

    void handleGCMResponse(CDelayedRequest delayedRequest, Response response) {
        Integer responseCode = response.code();
        if (Objects.equals(responseCode, HTTPRequestsSingleton.IN_PLACE_ERROR_STATUS_CODE)) {
            GCMCommunicatorSingleton.getInstance().delayAndQueueRequestForExecution(delayedRequest);
        } else {
            try {
                final JSONObject body;
                try {
                    body = new JSONObject(response.body().string());
                } catch (JSONException e) {
                    e.printStackTrace(System.err);
                    //Will never happen
                    return; //Let the compiler know that "body" is always going to be alright if used
                }
                if (responseCode == 200) {
                    try {
                        if (((!body.get("failure").toString().contentEquals("0") || !body.get("canonical_ids")
                                .toString()
                                .contentEquals("0")))) {
                            final JSONArray results = body.getJSONArray("results");
                            for (int i = 0; i < results.length(); i++) {
                                final JSONObject obj = results.getJSONObject(i);
                                try {
                                    final String message_id = obj.getString("message_id");
                                } catch (JSONException e) {
                                    switch (obj.getString("error")) {
                                        case "Unavailable":
                                            System.err.println("Server unavailable\nRetrying with exponential " +
                                                    "back-off...");
                                            GCMCommunicatorSingleton.getInstance().delayAndQueueRequestForExecution
                                                    (delayedRequest);
                                            break;
                                        case "NotRegistered":
                                            //TODO Remove registration_id from the database
                                            break;
                                        case "MissingRegistration":
                                            //Do nothing, we wanted no targets and so it be
                                            break;
                                        case "InvalidRegistration":
                                            throw new IllegalStateException("GCM Error response InvalidRegistration -" +
                                                    " See" +
                                                    " " +
                                                    "https://developer.android.com/google/gcm/http.html#error_codes " +
                                                    "for " +
                                                    "more " +
                                                    "information.");
                                        case "MismatchSenderId":
                                            throw new IllegalStateException("GCM Error response MismatchSenderId - " +
                                                    "See " +
                                                    "https://developer.android.com/google/gcm/http.html#error_codes " +
                                                    "for " +
                                                    "more " +
                                                    "information.");
                                        case "MessageTooBig":
                                            throw new IllegalStateException("GCM Error response MessageTooBig - See " +
                                                    "https://developer.android.com/google/gcm/http.html#error_codes " +
                                                    "for " +
                                                    "more " +
                                                    "information.");
                                        case "InvalidDataKey":
                                            throw new IllegalStateException("GCM Error response InvalidDataKey - See " +
                                                    "https://developer.android.com/google/gcm/http.html#error_codes " +
                                                    "for " +
                                                    "more " +
                                                    "information.");
                                        case "InvalidTtl":
                                            throw new IllegalStateException("GCM Error response InvalidTtl - See " +
                                                    "https://developer.android.com/google/gcm/http.html#error_codes " +
                                                    "for " +
                                                    "more " +
                                                    "information.");
                                        case "InternalServerError":
                                            throw new IllegalStateException("GCM Error response InternalServerError -" +
                                                    " See" +
                                                    " " +
                                                    "https://developer.android.com/google/gcm/http.html#error_codes " +
                                                    "for " +
                                                    "more " +
                                                    "information.");
                                        case "InvalidPackageName":
                                            throw new IllegalStateException("GCM Error response InvalidPackageName - " +
                                                    "See " +
                                                    "https://developer.android.com/google/gcm/http.html#error_codes " +
                                                    "for " +
                                                    "more " +
                                                    "information.");
                                        case "DeviceMessageRateExceeded":
                                            throw new IllegalStateException("GCM Error response " +
                                                    "DeviceMessageRateExceeded" +
                                                    " - " +
                                                    "See " +
                                                    "https://developer.android.com/google/gcm/http.html#error_codes " +
                                                    "for " +
                                                    "more " +
                                                    "information.");
                                        default:
                                            //TODO Remove registration_id from the database
                                    }
                                }
                                try {
                                    final String registration_id = obj.getString
                                            ("registration_id");
                                    //TODO Update registration_id in the database
                                } catch (JSONException e) {
                                    //Everything went fine and update is not needed (message_id is set
                                    // and registration_id not) or there was error (message_id was not set)
                                }
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace(System.err);
                        //Will never happen:
                        // (1) If the status code is 200 the fields "failure" and "canonical_ids" always exist
                        // (2) If the status code is 200 and either "failure" or "canonical_ids" are not
                        // zero, then the "results" field exists.
                        // (3) Standard traversing of a JSONArray using its size as delimiter and never modifying it, so
                        // never going beyond bounds
                        // (4) If the status code is 200 and the field "message_id" does not exist, then the field
                        // "error" always exist
                        // See https://developer.android.com/google/gcm/http
                        // .html#error_codes for more information
                    }
                } else if (responseCode == 401) {
                    throw new IllegalStateException("GCM Error response Authentication Error - See " +
                            "https://developer.android.com/google/gcm/http.html#error_codes for more " +
                            "information.");
                } else if (responseCode == 500) {
                    System.err.println("Obtained abnormal GCM response code " + responseCode + "\nRetrying with " +
                            "exponential " +
                            "back-off...");
                    GCMCommunicatorSingleton.getInstance().delayAndQueueRequestForExecution(delayedRequest);
                } else if (responseCode > 500 && responseCode < 600) {
                    System.err.println("Obtained abnormal GCM response code " + responseCode + "\nRetrying with " +
                            "exponential " +
                            "back-off...");
                    GCMCommunicatorSingleton.getInstance().delayAndQueueRequestForExecution(delayedRequest);
                } else {
                    System.err.println("Obtained unexpected GCM response code " + responseCode + "\nRetrying with " +
                            "exponential " +
                            "back-off...");
                    GCMCommunicatorSingleton.getInstance().delayAndQueueRequestForExecution(delayedRequest);
                }
            } catch (IOException e) {
                e.printStackTrace(System.err);
                GCMCommunicatorSingleton.getInstance().delayAndQueueRequestForExecution(delayedRequest);
            }
        }
    }
}
