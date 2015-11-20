package com.dektar.pi.piswitch;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.text.TextUtils;
import android.util.Log;
import android.widget.RemoteViews;

/**
 * AppWidgetProvider for PiSwitch
 */
public class PiSwitchWidgetProvider extends AppWidgetProvider {

    private static final String ACTION_REFRESH = "action_refresh";

    private static final String HAS_NO_DATA_KEY = "no_data";
    private static final String IS_ON_KEY = "is_on";
    private static final String INTERNAL_TEMP_KEY = "internal_temp";
    private static final String EXTERNAL_TEMP_KEY = "external_temp";

    // Possible intents: auto update, response from Pi, light button pushed, or refresh pushed.
    private static final String BUTTON_PUSHED_KEY = "button_pushed_intent";
    private static final String REFRESH_PUSHED_KEY = "refresh_pushed_intent";
    private static final String PI_CONTROLLER_STATUS_KEY = "pi_controller_status_intent";

    private boolean mIsOn;
    private double mInternalTemp;
    private double mExternalTemp;
    private boolean mHasNoData;
    private boolean mLightButtonClickedIntent;
    private boolean mRefreshButtonClickedIntent;
    private boolean mPiControllerIntent;
    private PiController mPiController;

    @Override
    public void onUpdate(final Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        final int size = appWidgetIds.length;
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(),
                R.layout.app_widget);
        initializePiController(context, appWidgetIds);
        if (mLightButtonClickedIntent) {
            // The button was just clicked. Start the request for toggle & status.
            mPiController.toggle(!mIsOn);
            setRemoteViewsPending(context, remoteViews);
            clearLightPendingIntent(remoteViews);
        } else if (mRefreshButtonClickedIntent) {
            mPiController.refreshAll(context);
            setRemoteViewsPending(context, remoteViews);
        } else if (mPiControllerIntent || mHasNoData) {
            setRemoteViewsUi(mIsOn, mInternalTemp, mExternalTemp, context, remoteViews);
        } else {
            // If the intent wasn't the button click or the response from PiController,
            // then it is time to refresh the data.
            mPiController.refreshAll(context);
        }

        for (int i = 0; i < size; i++) {
            int widgetId = appWidgetIds[i];
            createRefreshIntent(context, appWidgetIds, remoteViews);
            if (!mLightButtonClickedIntent) {
                resetLightPendingIntent(context, appWidgetIds, remoteViews);
            }
            appWidgetManager.updateAppWidget(widgetId, remoteViews);
        }
    }

    private void initializePiController(final Context context, int[] appWidgetIds) {
        final Intent updateIntent = new Intent(context, PiSwitchWidgetProvider.class);
        updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
        updateIntent.putExtra(PI_CONTROLLER_STATUS_KEY, true);
        updateIntent.putExtra(BUTTON_PUSHED_KEY, false);
        updateIntent.putExtra(REFRESH_PUSHED_KEY, false);
        if (mPiController == null) {
            mPiController =
                    new PiController(context, new PiController.OnPiStatusResponseListener() {
                        @Override
                        public void onStatus(boolean isOn, double insideTemp, double outsideTemp) {
                            updateIntent.putExtra(IS_ON_KEY, isOn);
                            updateIntent.putExtra(HAS_NO_DATA_KEY, false);
                            updateIntent.putExtra(INTERNAL_TEMP_KEY, insideTemp);
                            updateIntent.putExtra(EXTERNAL_TEMP_KEY, outsideTemp);
                            context.sendBroadcast(updateIntent);
                        }

                        @Override
                        public void onError() {
                            updateIntent.putExtra(HAS_NO_DATA_KEY, true);
                            context.sendBroadcast(updateIntent);
                        }
                    });
        } else {
            mPiController.refreshOptions(context);
        }
    }

    private void setRemoteViewsPending(Context context, RemoteViews remoteViews) {
        Resources res = context.getResources();
        remoteViews.setInt(R.id.widget_indicator, "setBackgroundColor",
                res.getColor(R.color.widget_indicator_loading));
    }

    private void setRemoteViewsError(Context context, RemoteViews remoteViews) {
        Resources res = context.getResources();
        remoteViews.setInt(R.id.widget_indicator, "setBackgroundColor",
                res.getColor(R.color.widget_indicator_error));
    }

    private void setRemoteViewsUi(boolean isOn, double internalTemp, double externalTemp,
                                  Context context, RemoteViews remoteViews) {
        Resources res = context.getResources();
        if (isOn) {
            remoteViews.setInt(R.id.widget_toggle_button, "setAlpha", 255);
            remoteViews.setInt(R.id.widget_indicator, "setBackgroundColor",
                    res.getColor(R.color.widget_indicator_on));
        } else {
            remoteViews.setInt(R.id.widget_toggle_button, "setAlpha", 100);
            remoteViews.setInt(R.id.widget_indicator, "setBackgroundColor",
                    res.getColor(R.color.widget_indicator_off));
        }
        remoteViews.setTextViewText(R.id.widget_internal_temp,
                String.format(res.getString(R.string.temp_string), internalTemp));
        remoteViews.setTextViewText(R.id.widget_external_temp,
                String.format(res.getString(R.string.temp_string), externalTemp));
    }

    private void clearLightPendingIntent(RemoteViews remoteViews) {
        remoteViews.setOnClickPendingIntent(R.id.widget_toggle_button, null);
    }

    private void resetLightPendingIntent(Context context, int[] appWidgetIds,
                                         RemoteViews remoteViews) {
        Intent lightClickIntent = new Intent(context, PiSwitchWidgetProvider.class);
        lightClickIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        lightClickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
        lightClickIntent.putExtra(BUTTON_PUSHED_KEY, true); // button push intent
        lightClickIntent.putExtra(PI_CONTROLLER_STATUS_KEY, false);
        lightClickIntent.putExtra(REFRESH_PUSHED_KEY, false);
        lightClickIntent.putExtra(IS_ON_KEY, mIsOn);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context,
                0, lightClickIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.widget_toggle_button, pendingIntent);
    }

    private void createRefreshIntent(Context context, int[] appWidgetIds,
                                     RemoteViews remoteViews) {
        Intent refreshIntent = new Intent(context, PiSwitchWidgetProvider.class);
        refreshIntent.setAction(ACTION_REFRESH);
        refreshIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
        refreshIntent.putExtra(REFRESH_PUSHED_KEY, true);
        refreshIntent.putExtra(PI_CONTROLLER_STATUS_KEY, false);
        refreshIntent.putExtra(BUTTON_PUSHED_KEY, false);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context,
                0, refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.widget_refresh_button, pendingIntent);
    }

    @Override
    public void onReceive(final Context context, Intent intent) {
        Log.d("intent action", intent.getAction());
        if (TextUtils.equals(intent.getAction(), ACTION_REFRESH)) {
            mRefreshButtonClickedIntent = true;
            mLightButtonClickedIntent = false;
            intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        } else {
            mRefreshButtonClickedIntent = false;
            mLightButtonClickedIntent = intent.getBooleanExtra(BUTTON_PUSHED_KEY, false);
        }
        mPiControllerIntent = intent.getBooleanExtra(PI_CONTROLLER_STATUS_KEY, false);
        mHasNoData = intent.getBooleanExtra(HAS_NO_DATA_KEY, true);
        mIsOn = intent.getBooleanExtra(IS_ON_KEY, false);
        mInternalTemp = intent.getDoubleExtra(INTERNAL_TEMP_KEY, 0.0);
        mExternalTemp = intent.getDoubleExtra(EXTERNAL_TEMP_KEY, 0.0);
        super.onReceive(context, intent);
    }
}
