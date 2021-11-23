package org.kde.kdeconnect.Plugins.RunCommandPlugin;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import org.kde.kdeconnect.BackgroundService;
import org.kde.kdeconnect.Device;
import org.kde.kdeconnect_custom.R;

public class RunCommandWidget extends AppWidgetProvider {

    public static final String RUN_COMMAND_ACTION = "RUN_COMMAND_ACTION";
    public static final String TARGET_COMMAND = "TARGET_COMMAND";
    public static final String TARGET_DEVICE = "TARGET_DEVICE";
    private static final String SET_CURRENT_DEVICE = "SET_CURRENT_DEVICE";

    private static String currentDeviceId;

    @Override
    public void onReceive(Context context, Intent intent) {

        super.onReceive(context, intent);

        if (intent != null && intent.getAction() != null && intent.getAction().equals(RUN_COMMAND_ACTION)) {

            final String targetCommand = intent.getStringExtra(TARGET_COMMAND);
            final String targetDevice = intent.getStringExtra(TARGET_DEVICE);

            BackgroundService.RunCommand(context, service -> {
                RunCommandPlugin plugin = service.getDevice(targetDevice).getPlugin(RunCommandPlugin.class);

                if (plugin != null) {
                    try {
                        Log.i("RunCommandWidget", "running command " + targetCommand);
                        plugin.runCommand(targetCommand);
                    } catch (Exception ex) {
                        Log.e("RunCommandWidget", "Error running command", ex);
                    }
                }
            });
        }

        final Intent newIntent = new Intent(context, RunCommandWidgetDataProviderService.class);
        context.startService(newIntent);
        updateWidget(context);

    }


    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        updateWidget(context);
    }

    private void updateWidget(final Context context) {
        updateWidgetImpl(context);
    }

    private void updateWidgetImpl(Context context) {

        try {

            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, RunCommandWidget.class));
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_remotecommandplugin);

            PendingIntent pendingIntent;
            Intent intent;

            intent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
            intent.setAction("unifiedRunView-" + Long.toString(System.currentTimeMillis()));
            pendingIntent = PendingIntent.getActivity(context, 0, intent,
                                                      PendingIntent.FLAG_UPDATE_CURRENT);
            views.setOnClickPendingIntent(R.id.runcommandWidgetTitleHeader, pendingIntent);
            views.setTextViewText(R.id.runcommandWidgetTitle, context.getString(R.string.refresh));
            views.setViewVisibility(R.id.run_commands_list, View.VISIBLE);
            views.setViewVisibility(R.id.not_reachable_message, View.GONE);

            for (int appWidgetId : appWidgetIds) {

                intent = new Intent(context, RunCommandWidgetDataProviderService.class);
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
                views.setRemoteAdapter(R.id.run_commands_list, intent);

                intent = new Intent(context, RunCommandWidget.class);
                pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                views.setPendingIntentTemplate(R.id.run_commands_list, pendingIntent);

                AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId, views);
                AppWidgetManager.getInstance(context).notifyAppWidgetViewDataChanged(appWidgetId, R.id.run_commands_list);

            }

        } catch (Exception ex) {
            Log.e("RunCommandWidget", "Error updating widget", ex);
        }

        if (BackgroundService.getInstance() != null) {
            BackgroundService.getInstance().addDeviceListChangedCallback("RunCommandWidget", () -> {
                Intent updateWidget = new Intent(context, RunCommandWidget.class);
                context.sendBroadcast(updateWidget);
            });
        }
    }
}
