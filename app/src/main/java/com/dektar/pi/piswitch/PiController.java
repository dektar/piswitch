package com.dektar.pi.piswitch;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Class for communicating with and controlling the Pi.
 */
public class PiController {
    public interface OnPiStatusResponseListener {
        void onStatus(boolean isOn, double insideTemp, double outsideTemp);
        void onError();
    }

    private static final String HTTP_ON = "cgi-bin/on.py";
    private static final String HTTP_OFF = "cgi-bin/off.py";
    private static final String HTTP_STATUS = "status.php";

    private static final String TAG = "request_tag";

    private RequestQueue mRequestQueue;
    private OnPiStatusResponseListener mStatusListener;
    private String mUrlBase;

    private boolean mIsInternalIp;
    private String mInternalUrl;
    private String mExternalUrl;

    public PiController(Context context, OnPiStatusResponseListener listener) {
        mStatusListener = listener;
        loadPreferences(context);
        mRequestQueue = Volley.newRequestQueue(context);
    }

    public void onDestroy() {
        mRequestQueue.cancelAll(TAG);
        mRequestQueue.stop();
        mRequestQueue = null;
        mStatusListener = null;
    }

    public void toggle(boolean turnOn) {
        sendRequest(mUrlBase + "/" + (turnOn ? HTTP_ON : HTTP_OFF));
    }

    public void refreshAll(Context context) {
        loadPreferences(context);
        updateStatus();
    }

    public void refreshOptions(Context context) {
        loadPreferences(context);
    }

    public void updateStatus() {
        String url = mUrlBase + "/" + HTTP_STATUS;

        // Request a string response from the provided URL.
        JsonObjectRequest statusRequest = new JsonObjectRequest(
                com.android.volley.Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            boolean isCurrentlyOn = getLampState(response);
                            double insideTemp = getInsideTemp(response);
                            double outsideTemp = getOutsideTemp(response);
                            mStatusListener.onStatus(isCurrentlyOn, insideTemp, outsideTemp);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                mStatusListener.onError();
                Log.d("Error", error.getMessage());
            }
        });
        statusRequest.setTag(TAG);
        // Add the request to the RequestQueue.
        mRequestQueue.add(statusRequest);
    }

    private void sendRequest(String url) {
        StringRequest request = new StringRequest(com.android.volley.Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        updateStatus();
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                mStatusListener.onError();
                Log.d("Error", ": " + error.getMessage());
            }
        });
        request.setTag(TAG);
        mRequestQueue.add(request);
    }

    private boolean getLampState(JSONObject response) throws JSONException {
        JSONArray lamps = response.getJSONArray("lamps");
        for (int i = 0; i < lamps.length(); i++) {
            JSONObject lamp = lamps.getJSONObject(i);
            if (TextUtils.equals(lamp.getString("name"), "living room")) {
                return lamp.getInt("state") == 1;
            }
        }
        return false;
    }

    private double getInsideTemp(JSONObject response) throws JSONException {
        return getTemp(response, "current_temp");
    }

    private double getOutsideTemp(JSONObject response) throws JSONException {
        return getTemp(response, "ext_temp");
    }

    private double getTemp(JSONObject response, String tempType) throws JSONException {
        JSONArray heating = response.getJSONArray("heating");
        for (int i = 0; i < heating.length(); i++) {
            JSONObject state = heating.getJSONObject(i);
            return state.getDouble(tempType);
        }
        return -1;
    }

    private void loadPreferences(Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        Resources resources = context.getResources();
        mIsInternalIp = sharedPref.getBoolean(resources.getString(R.string.pref_which_url_id),
                true);
        mInternalUrl = sharedPref.getString(resources.getString(R.string.pref_internal_url_id),
                resources.getString(R.string.default_internal_url));
        mExternalUrl = sharedPref.getString(resources.getString(R.string.pref_external_url_id),
                resources.getString(R.string.default_external_url));
        mUrlBase = mIsInternalIp ? mInternalUrl : mExternalUrl;
    }
}
