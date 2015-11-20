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
    private ToggleButton toggleButton;
    private PiController mPiController;
    private PiController.OnPiStatusResponseListener statusListener;

    private boolean mIsCurrentlyOn;

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

        toggleButton = (ToggleButton) rootView.findViewById(R.id.toggle_button);
        toggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resetButtonUi();
                mPiController.toggle(!mIsCurrentlyOn);
            }
        });

        statusListener = new PiController.OnPiStatusResponseListener() {
            @Override
            public void onStatus(boolean isOn, double insideTemp, double outsideTemp) {
                mIsCurrentlyOn = isOn;
                updateButtonState();
            }

            @Override
            public void onError() {
                resetButtonUi();
            }
        };
        resetButtonUi();

        mPiController = new PiController(getActivity(), statusListener);

        if (mTryTurningOn) {
            mPiController.toggle(true);
            mTryTurningOn = false;
        } else if (mTryTurningOff) {
            mPiController.toggle(false);
            mTryTurningOff = false;
        } else {
            mPiController.updateStatus();
        }

        return rootView;
    }

    @Override
    public void onResume() {
        mPiController.refreshAll(getActivity());
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        mPiController.onDestroy();
        super.onDestroy();
    }

    private void resetButtonUi() {
        toggleButton.setEnabled(false);
        toggleButton.setBackgroundColor(getResources().getColor(R.color.button_color_loading));
        toggleButton.setText(getResources().getString(R.string.button_text_loading));
    }

    private void updateButtonState() {
        toggleButton.setEnabled(true);
        if (mIsCurrentlyOn) {
            toggleButton.setText(getResources().getString(R.string.button_text_turn_off));
            toggleButton.setBackgroundColor(getResources().getColor(R.color.button_color_turn_off));
        } else {
            toggleButton.setText(getResources().getString(R.string.button_text_turn_on));
            toggleButton.setBackgroundColor(getResources().getColor(R.color.button_color_turn_on));
        }
    }
}
