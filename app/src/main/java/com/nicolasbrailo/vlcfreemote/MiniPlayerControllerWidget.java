package com.nicolasbrailo.vlcfreemote;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.nicolasbrailo.vlcfreemote.local_settings.RememberedServers;
import com.nicolasbrailo.vlcfreemote.model.Server;
import com.nicolasbrailo.vlcfreemote.model.VlcStatus;
import com.nicolasbrailo.vlcfreemote.vlc_connector.Cmd_Next;
import com.nicolasbrailo.vlcfreemote.vlc_connector.Cmd_TogglePlay;
import com.nicolasbrailo.vlcfreemote.vlc_connector.RemoteVlc;
import com.nicolasbrailo.vlcfreemote.vlc_connector.VlcCommand;

/**
 * TODO
 * Note: If the device is asleep when it is time for an update (as defined by updatePeriodMillis),
 * then the device will wake up in order to perform the update. If you don't update more than once
 * per hour, this probably won't cause significant problems for the battery life. If, however, you
 * need to update more frequently and/or you do not need to update while the device is asleep, then
 * you can instead perform updates based on an alarm that will not wake the device. To do so, set an
 * alarm with an Intent that your AppWidgetProvider receives, using the AlarmManager. Set the alarm
 * type to either ELAPSED_REALTIME or RTC, which will only deliver the alarm when the device is awake.
 * Then set updatePeriodMillis to zero ("0").
 */
public class MiniPlayerControllerWidget extends AppWidgetProvider {

    private static final String ACTION_TOGGLE_PLAY = "com.nicolasbrailo.vlcfreemote.ACTION_TOGGLE_PLAY";
    private static final String ACTION_PLAY_NEXT = "com.nicolasbrailo.vlcfreemote.ACTION_PLAY_NEXT";

    private static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        Intent openApp = new Intent(context, MainActivity.class);
        PendingIntent pendingOpenApp = PendingIntent.getActivity(context, 0, openApp, 0);

        Intent togglePlay = new Intent(context, MiniPlayerControllerWidget.class);
        togglePlay.setAction(ACTION_TOGGLE_PLAY);
        PendingIntent pendingTogglePlay = PendingIntent.getBroadcast(context, 0, togglePlay, 0);

        Intent playNext = new Intent(context, MiniPlayerControllerWidget.class);
        playNext.setAction(ACTION_PLAY_NEXT);
        PendingIntent pendingPlayNext = PendingIntent.getBroadcast(context, 0, playNext, 0);

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.mini_player_controller_widget);
        views.setOnClickPendingIntent(R.id.wMiniPlayerController_Open, pendingOpenApp);
        views.setOnClickPendingIntent(R.id.wMiniPlayerController_BtnPlayPause, pendingTogglePlay);
        views.setOnClickPendingIntent(R.id.wMiniPlayerController_BtnNext, pendingPlayNext);

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onEnabled(Context context) {

    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    private static class WidgetVlcCallback implements VlcCommand.GeneralCallback, VlcStatus.Observer {
        private final Context ctx;

        WidgetVlcCallback(Context ctx) {
            this.ctx = ctx;
        }

        void reviveApp() {
            Intent openApp = new Intent(ctx, MainActivity.class);
            PendingIntent pendingOpenApp = PendingIntent.getActivity(ctx, 0, openApp, 0);

            try {
                pendingOpenApp.send();
            } catch (PendingIntent.CanceledException ignored) {
                // Nothing to do...
            }
        }

        // On any error condition, open the app: it will automatically request a new server if the
        // current one has become invalid for whatever reason.
        @Override public void onAuthError() {
            reviveApp();
        }
        @Override public void onConnectionError(final String msg) { reviveApp(); }
        @Override public void onSystemError(Exception e) {
            reviveApp();
        }
        @Override public void onVlcStatusFetchError() { reviveApp(); }
        @Override public void onVlcStatusFetchError(final String msg) { reviveApp(); }

        @Override
        public void onVlcStatusUpdate(VlcStatus results) {
            // Nothing can be done with a status update in a widget, so just drop it
        }

    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
        final WidgetVlcCallback vlcCallback = new WidgetVlcCallback(context);

        final Server srv = (new RememberedServers(context)).getLastUsedServer();
        if (srv == null) {
            vlcCallback.reviveApp();
        } else {
            RemoteVlc vlc = new RemoteVlc(srv, vlcCallback);
            if (ACTION_TOGGLE_PLAY.equals(intent.getAction())) {
                vlc.exec(new Cmd_TogglePlay(vlc));
            } else if (ACTION_PLAY_NEXT.equals(intent.getAction())) {
                vlc.exec(new Cmd_Next(vlc));
            }
        }

        super.onReceive(context, intent);
    }
}

