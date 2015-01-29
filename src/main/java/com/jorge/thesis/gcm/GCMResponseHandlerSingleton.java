package com.jorge.thesis.gcm;

import com.jorge.thesis.io.database.DBDAOSingleton;
import com.jorge.thesis.io.net.HTTPRequestsSingleton;
import com.squareup.okhttp.Response;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Objects;

public final class GCMResponseHandlerSingleton {

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
                        final String failure = body.get("failure").toString();
                        final String canonicalIds = body.get("canonical_ids") //(1)
                                .toString();
                        if (((!failure.contentEquals("0") || !canonicalIds.contentEquals("0")))) {
                            System.err.println("Sending not ideal. Some work has to be done.");
                            final JSONArray results = body.getJSONArray("results"); //(2)
                            for (int i = 0; i < results.length(); i++) {
                                final JSONObject obj = results.getJSONObject(i); //(3)
                                if (obj.has("message_id")) {
                                    if (obj.has("registration_id")) {
                                        final String new_reg_id = obj.getString
                                                ("registration_id"), old_reg_id = new JSONObject(
                                                (delayedRequest.getPureRequest().body().toString()))
                                                .getJSONArray("registration_ids").getString(i);
                                        System.err.println("Registration id update requested (" + old_reg_id + ") by (" + new_reg_id + ")");
                                        DBDAOSingleton.getInstance().updateRegistrationIdOnAllTags(old_reg_id, new_reg_id);
                                    } else {
                                        System.out.println("GCM response normal.");
                                    }
                                } else {
                                    switch (obj.getString("error")) { // (4)
                                        case "Unavailable":
                                            System.err.println("Server unavailable\nRetrying with exponential " +
                                                    "back-off...");
                                            GCMCommunicatorSingleton.getInstance().delayAndQueueRequestForExecution
                                                    (delayedRequest);
                                            break;
                                        case "NotRegistered":
                                            System.out.println("Detected unregistered device. Removing from database." +
                                                    "..");
                                            DBDAOSingleton.getInstance().removeRegistrationIdFromAllTags(new JSONObject(
                                                    (delayedRequest.getPureRequest().body().toString()))
                                                    .getJSONArray("registration_ids").getString(i)); //(5)
                                            break;
                                        case "MissingRegistration":
                                            //Do nothing, we wanted no targets and so it be
                                            System.out.println("Message requested to no targets.");
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
                                            System.err.println("Unexpected error identifier " + obj.getString
                                                    ("error") + " received. Deleting " +
                                                    "id from database...");
                                            DBDAOSingleton.getInstance().removeRegistrationIdFromAllTags(new JSONObject(
                                                    (delayedRequest.getPureRequest().body().toString()))
                                                    .getJSONArray("registration_ids").getString(i));
                                    }
                                }
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace(System.err);
                        //Will never happen:
                        // (1) If the status code is 200 the fields "failure" and "canonical_ids" always exist
                        // (2) If the status code is 200 and either "failure" or "canonical_ids" are not
                        // zero, then the "results" field exists
                        // (3) Standard traversing of a JSONArray using its size as delimiter and never modifying it, so
                        // never going beyond bounds
                        // (4) If the status code is 200 and the field "message_id" does not exist, then the field
                        // "error" always exist
                        // (5) Gotten to this point, we know that the response is JSON and that it contains a
                        // "registration_ids" array
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
