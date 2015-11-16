package com.dektar.pi.piswitch;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;

/**
 * AppWidgetProvider for PiSwitch
 */
public class PiSwitchWidgetProvider extends AppWidgetProvider {

    private static final String HAS_NO_DATA_KEY = "no_data";
    private static final String IS_ON_KEY = "is_on";

    private static final String BUTTON_PUSHED_KEY = "button_pushed_intent";
    private static final String PI_CONTROLLER_STATUS_KEY = "pi_controller_status_intent";

    private boolean mIsOn = false;
    private boolean mHasNoData = true;
    private boolean mButtonClickedIntent = false;
    private boolean mPiControllerIntent = false;
    private PiController mPiController;

    @Override
    public void onUpdate(final Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.d("onUpdate", "received intent");
        final int size = appWidgetIds.length;
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

            RemoteViews remoteViews = new RemoteViews(context.getPackageName(),
                    R.layout.app_widget);
            if (mButtonClickedIntent) {
                setRemoteViewsPending(context, remoteViews);
            } else if (mPiControllerIntent) {
                // This is an update from a status request. Reset the button click intent.
                if (mHasNoData) {
                    setRemoteViewsPending(context, remoteViews);
                } else {
                    setRemoteViewsUi(mIsOn, context, remoteViews);
                }
                Intent clickIntent = new Intent(context, PiSwitchWidgetProvider.class);
                clickIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                clickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
                clickIntent.putExtra(BUTTON_PUSHED_KEY, true); // button push intent
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
        remoteViews.setTextColor(R.id.widget_toggle_button,
                context.getResources().getColor(R.color.button_color_loading));
    }

    private void setRemoteViewsUi(boolean isOn, Context context, RemoteViews remoteViews) {
        if (isOn) {
            remoteViews.setTextViewText(R.id.widget_toggle_button,
                    context.getResources().getString(R.string.button_text_turn_off));
            remoteViews.setTextColor(R.id.widget_toggle_button,
                    context.getResources().getColor(R.color.button_color_turn_off));
        } else {
            remoteViews.setTextViewText(R.id.widget_toggle_button,
                    context.getResources().getString(R.string.button_text_turn_on));
            remoteViews.setTextColor(R.id.widget_toggle_button,
                    context.getResources().getColor(R.color.button_color_turn_on));
        }
    }

    @Override
    public void onReceive(final Context context, Intent intent) {
        mButtonClickedIntent = intent.getBooleanExtra(BUTTON_PUSHED_KEY, mButtonClickedIntent);
        mPiControllerIntent = intent.getBooleanExtra(PI_CONTROLLER_STATUS_KEY, mPiControllerIntent);
        mHasNoData = intent.getBooleanExtra(HAS_NO_DATA_KEY, mHasNoData);
        mIsOn = intent.getBooleanExtra(IS_ON_KEY, mIsOn);
        super.onReceive(context, intent);
    }
}
