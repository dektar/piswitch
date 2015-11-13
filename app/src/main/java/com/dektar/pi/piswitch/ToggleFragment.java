package com.dektar.pi.piswitch;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ToggleButton;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

/**
 * Toggle fragment.
 */
public class ToggleFragment extends android.support.v4.app.Fragment {
    private static final String INTERNAL_URL = "http://10.1.10.12";
    private static final String EXTERNAL_URL = "http://pi.dektar.com";
    private static final String HTTP_ON = "cgi-bin/on.py";
    private static final String HTTP_OFF = "cgi-bin/off.py";
    private static final String HTTP_STATUS = "status.php";

    private static final String TAG = "request_tag";

    private ToggleButton toggleButton;
    private boolean isCurrentlyOn;
    private RequestQueue mRequestQueue;
    private String mUrlBase;

    private boolean mTryTurningOn = false;
    private boolean mTryTurningOff = false;

    public void tryTurningOn() {
        mTryTurningOn = true;
    }

    public void tryTurningOff() {
        mTryTurningOff = true;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.toggle_fragment, container, false);

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        boolean isInternalIp = sharedPref.getBoolean(
                getResources().getString(R.string.pref_internal_url_id), true);
        mUrlBase = isInternalIp ? INTERNAL_URL : EXTERNAL_URL;

        toggleButton = (ToggleButton) rootView.findViewById(R.id.toggle_button);
        toggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resetButtonUi();
                sendRequest(mUrlBase + "/" + (isCurrentlyOn ? HTTP_OFF : HTTP_ON));
            }
        });

        mRequestQueue = Volley.newRequestQueue(getActivity());
        resetButtonUi();
        if (mTryTurningOn) {
            sendRequest(mUrlBase + "/" + HTTP_ON);
            mTryTurningOn = false;
        } else if (mTryTurningOff) {
            sendRequest(mUrlBase + "/" + HTTP_OFF);
            mTryTurningOff = false;
        } else {
            sendStatusRequest();
        }

        return rootView;
    }

    @Override
    public void onDestroy() {
        mRequestQueue.cancelAll(TAG);
        mRequestQueue.stop();
        mRequestQueue = null;
        super.onDestroy();
    }

    private void resetButtonUi() {
        toggleButton.setEnabled(false);
        toggleButton.setBackgroundColor(getResources().getColor(R.color.button_color_loading));
        toggleButton.setText(getResources().getString(R.string.button_text_loading));
    }

    private void updateButtonState() {
        toggleButton.setEnabled(true);
        if (isCurrentlyOn) {
            toggleButton.setText(getResources().getString(R.string.button_text_turn_off));
            toggleButton.setBackgroundColor(getResources().getColor(R.color.button_color_turn_off));
        } else {
            toggleButton.setText(getResources().getString(R.string.button_text_turn_on));
            toggleButton.setBackgroundColor(getResources().getColor(R.color.button_color_turn_on));
        }
    }

    private void sendStatusRequest() {
        String url = mUrlBase + "/" + HTTP_STATUS;

        // Request a string response from the provided URL.
        StringRequest statusRequest = new StringRequest(com.android.volley.Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        isCurrentlyOn = parseResponse(response);
                        updateButtonState();
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
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
                        sendStatusRequest();
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("Error", ": " + error.getMessage());
            }
        });
        request.setTag(TAG);
        mRequestQueue.add(request);
    }

    private boolean parseResponse(String response) {
        // TODO: Get JSON and parse that instead!
        if (response.contains("\"lamps\":[{\"id\":\"1\",\"name\":\"living room\",\"state\":\"1\"}")) {
            return true;
        }
        return false;
    }

}
