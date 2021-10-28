package org.kde.kdeconnect.Plugins.RunCommandPlugin;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import org.json.JSONException;
import org.json.JSONObject;
import org.kde.kdeconnect.BackgroundService;
import org.kde.kdeconnect.Device;
import org.kde.kdeconnect_custom.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


class RunCommandWidgetDataProvider implements RemoteViewsService.RemoteViewsFactory {

    private final Context mContext;

    private List<CommandEntry> commandItems = new ArrayList<>();


    public RunCommandWidgetDataProvider(Context context, Intent intent) {
        mContext = context;
    }


    @Override
    public void onCreate() {
        updateViewModel();
    }




    @Override
    public void onDataSetChanged() {
        Log.i("UnifiedDataProvider", "onDataSetChanged() : refreshing view..");
        updateViewModel();
    }

    @Override
    public void onDestroy() {
    }


    private void updateViewModel() {
        commandItems.clear();

        for (Device d : BackgroundService.getInstance().getDeviceList()) {
            Log.i("UnifiedDataProvider", "device name = " + d.getName());
            if (!(d.isReachable() && d.isPaired())) {
                Log.i("UnifiedDataProvider", "device " + d.getName() + " is currently inaccessible.");
                continue;
            }
            final RunCommandPlugin plugin = d.getPlugin(RunCommandPlugin.class);
            if (plugin == null) {
                Log.i("UnifiedDataProvider", "device's RunCommandPlugin is currently null! ");
                continue;
            }

            for (JSONObject obj : plugin.getCommandList()) {
                try {
                    commandItems.add(new CommandEntry(plugin, obj.getString("name"),
                                                      obj.getString("command"), obj.getString("key")));
                } catch (JSONException e) {
                    Log.e("RunCommand", "Error parsing JSON", e);
                }
            }

        } // end for (Device..

        Collections.sort(commandItems, Comparator.comparing(CommandEntry::getName));
    }



    @Override
    public int getCount() {
        return commandItems.size();
    }

    @Override
    public RemoteViews getViewAt(int i) {

        RemoteViews remoteView = new RemoteViews(mContext.getPackageName(), R.layout.list_item_entry);

        CommandEntry entry = commandItems.get(i);
        RunCommandPlugin plugin = entry.getPlugin();
        String deviceId = plugin.getDevice().getDeviceId();

        final Intent configIntent = new Intent(mContext, RunCommandWidget.class);
        configIntent.setAction(RunCommandWidget.RUN_COMMAND_ACTION);
        configIntent.putExtra(RunCommandWidget.TARGET_COMMAND, entry.getKey());
        configIntent.putExtra(RunCommandWidget.TARGET_DEVICE, deviceId);

        remoteView.setTextViewText(R.id.list_item_entry_title, entry.getName());
        remoteView.setTextViewText(R.id.list_item_entry_summary, entry.getCommand());
        remoteView.setViewVisibility(R.id.list_item_entry_summary, View.GONE);

        remoteView.setOnClickFillInIntent(R.id.list_item_entry, configIntent);

        return remoteView;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int i) {
        if (i < commandItems.size()) {
            return commandItems.get(i).getKey().hashCode();
        }

        return 0;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }
}
