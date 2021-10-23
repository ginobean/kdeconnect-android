/*
 * SPDX-FileCopyrightText: 2015 Aleix Pol Gonzalez <aleixpol@kde.org>
 * SPDX-FileCopyrightText: 2015 Albert Vaca Cintora <albertvaka@gmail.com>
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.kde.kdeconnect.Plugins.RunCommandPlugin;

import android.content.ClipboardManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import org.json.JSONException;
import org.json.JSONObject;
import org.kde.kdeconnect.BackgroundService;
import org.kde.kdeconnect.Device;
import org.kde.kdeconnect.UserInterface.List.ListAdapter;
import org.kde.kdeconnect.UserInterface.ThemeUtil;
import org.kde.kdeconnect_custom.R;
import org.kde.kdeconnect_custom.databinding.ActivityRunCommandBinding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class UnifiedRunCommandActivity extends AppCompatActivity {
    private ActivityRunCommandBinding binding;
//    private String deviceId;
    private final RunCommandPlugin.CommandsChangedCallback commandsChangedCallback = this::updateView;
    private List<CommandEntry> commandItems;


    private void updateView() {
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            Log.d("UnifiedRunCommand", "UnifiedRunCommandActivity is running in UI thread..");

        }
        else {
            Log.d("UnifiedRunCommand", "UnifiedRunCommandActivity is NOT running in UI thread!");
        }

        registerForContextMenu(binding.runCommandsList);

        commandItems = new ArrayList<>();

        for (Device d : BackgroundService.getInstance().getDeviceList()) {
            Log.d("UnifiedRunCommand", "device name = " + d.getName());
            if (! (d.isReachable() && d.isPaired())) {
                Log.d("UnifiedRunCommand", "device " + d.getName() + " is currently inaccessible.");
                continue;
            }
            final RunCommandPlugin plugin = d.getPlugin(RunCommandPlugin.class);
            if (plugin == null) {
                Log.d("UnifiedRunCommand", "device's RunCommandPlugin is currently null! ");
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

        ListAdapter adapter = new ListAdapter(UnifiedRunCommandActivity.this, commandItems);

        binding.runCommandsList.setAdapter(adapter);
        binding.runCommandsList.setOnItemClickListener((adapterView, view1, i, l) -> {
            String command = commandItems.get(i).getKey();
            Log.d("UnifiedRunCommand", "running command " + command);
            commandItems.get(i).getPlugin().runCommand(command);
        });

        if (commandItems.size() == 0) {
            String text = getString(R.string.addcommand_explanation);
//            if (!plugin.canAddCommand()) {
//                text += "\n" + getString(R.string.addcommand_explanation2);
//            }
            binding.addComandExplanation.setText(text);
            binding.addComandExplanation.setVisibility(commandItems.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeUtil.setUserPreferredTheme(this);

        binding = ActivityRunCommandBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbarLayout.toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

//        deviceId = getIntent().getStringExtra("deviceId");

        boolean canAddCommands = false;
/*
        try {
            canAddCommands = BackgroundService.getInstance().getDevice(deviceId).getPlugin(RunCommandPlugin.class).canAddCommand();
        } catch (Exception ignore) {
        }


        if (canAddCommands) {
            binding.addCommandButton.show();
        } else {

        }

        binding.addCommandButton.hide();

        binding.addCommandButton.setOnClickListener(v -> BackgroundService.RunWithPlugin(UnifiedRunCommandActivity.this, deviceId, RunCommandPlugin.class, plugin -> {
            plugin.sendSetupPacket();
             new AlertDialog.Builder(UnifiedRunCommandActivity.this)
                    .setTitle(R.string.add_command)
                    .setMessage(R.string.add_command_description)
                    .setPositiveButton(R.string.ok, null)
                    .show();
        }));

*/
        updateView();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.runcommand_context, menu);
    }

    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        if (item.getItemId() == R.id.copy_url_to_clipboard) {
            CommandEntry entry = (CommandEntry) commandItems.get(info.position);
            RunCommandPlugin plugin = entry.getPlugin();
            String deviceId = plugin.getDevice().getDeviceId();
            String url = "kdeconnect://runcommand/" + deviceId + "/" + entry.getKey();
            Log.d("UnifiedRunCommand", "KDE url request = " + url);
            ClipboardManager cm = ContextCompat.getSystemService(this, ClipboardManager.class);
            cm.setText(url);
            Toast toast = Toast.makeText(this, R.string.clipboard_toast, Toast.LENGTH_SHORT);
            toast.show();
        }
        return false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateView();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }
}
