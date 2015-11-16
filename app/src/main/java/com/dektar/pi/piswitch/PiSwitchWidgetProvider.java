package com.dektar.pi.piswitch;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

/**
 * AppWidgetProvider for PiSwitch
 */
public class PiSwitchWidgetProvider extends AppWidgetProvider {

    private static final String HAS_NO_DATA_KEY = "no_data";
    private static final String IS_ON_KEY = "is_on";

    // If not button_pushed or pi_controller_status, this was an auto onUpdate.
    private static final String BUTTON_PUSHED_KEY = "button_pushed_intent";
    private static final String PI_CONTROLLER_STATUS_KEY = "pi_controller_status_intent";

    private boolean mIsOn;
    private boolean mHasNoData;
    private boolean mButtonClickedIntent;
    private boolean mPiControllerIntent;
    private PiController mPiController;

    @Override
    public void onUpdate(final Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        final int size = appWidgetIds.length;
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(),
                R.layout.app_widget);
        initializePiController(context, appWidgetIds);
        if (mButtonClickedIntent) {
            // The button was just clicked. Start the request for toggle & status.
            mPiController.toggle(!mIsOn);
        } else if (!mPiControllerIntent) {
            // If the intent wasn't the button click or the response from PiController,
            // then it is time to refresh the data.
            mPiController.refreshAll(context);
        }

        for (int i = 0; i < size; i++) {
            int widgetId = appWidgetIds[i];
            if (mButtonClickedIntent) {
                setRemoteViewsPending(context, remoteViews);
            } else {
                if (mPiControllerIntent || mHasNoData) {
                    setRemoteViewsUi(mIsOn, context, remoteViews);
                } else  {
                    // This is an update from a status request, or the Pi had no data.
                    // Reset the button click intent.
                    setRemoteViewsPending(context, remoteViews);
                }
                Intent clickIntent = new Intent(context, PiSwitchWidgetProvider.class);
                clickIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                clickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
                clickIntent.putExtra(BUTTON_PUSHED_KEY, true); // button push intent
                clickIntent.putExtra(PI_CONTROLLER_STATUS_KEY, false);
                clickIntent.putExtra(IS_ON_KEY, mIsOn);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(context,
                        0, clickIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                remoteViews.setOnClickPendingIntent(R.id.widget_toggle_button, pendingIntent);
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
        if (mPiController == null) {
            mPiController =
                    new PiController(context, new PiController.OnPiStatusResponseListener() {
                        @Override
                        public void onStatus(boolean isOn) {
                            updateIntent.putExtra(IS_ON_KEY, isOn);
                            updateIntent.putExtra(HAS_NO_DATA_KEY, false);
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
        remoteViews.setTextViewText(R.id.widget_toggle_button,
                context.getResources().getString(R.string.button_text_loading));
        remoteViews.setInt(R.id.widget_indicator, "setBackgroundColor",
                context.getResources().getColor(R.color.widget_indicator_error));
    }

    private void setRemoteViewsUi(boolean isOn, Context context, RemoteViews remoteViews) {
        if (isOn) {
            remoteViews.setTextViewText(R.id.widget_toggle_button,
                    context.getResources().getString(R.string.button_text_turn_off));
            remoteViews.setInt(R.id.widget_indicator, "setBackgroundColor",
                    context.getResources().getColor(R.color.widget_indicator_on));
        } else {
            remoteViews.setTextViewText(R.id.widget_toggle_button,
                    context.getResources().getString(R.string.button_text_turn_on));
            remoteViews.setInt(R.id.widget_indicator, "setBackgroundColor",
                    context.getResources().getColor(R.color.widget_indicator_off));
        }
    }

    @Override
    public void onReceive(final Context context, Intent intent) {
        mButtonClickedIntent = intent.getBooleanExtra(BUTTON_PUSHED_KEY, false);
        mPiControllerIntent = intent.getBooleanExtra(PI_CONTROLLER_STATUS_KEY, false);
        mHasNoData = intent.getBooleanExtra(HAS_NO_DATA_KEY, true);
        mIsOn = intent.getBooleanExtra(IS_ON_KEY, false);
        super.onReceive(context, intent);
    }
}
