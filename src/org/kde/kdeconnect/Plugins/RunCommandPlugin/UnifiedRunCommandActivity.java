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
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import org.json.JSONException;
import org.json.JSONObject;
import org.kde.kdeconnect.BackgroundService;
import org.kde.kdeconnect.Device;
import org.kde.kdeconnect.UserInterface.List.ListAdapter;
import org.kde.kdeconnect.UserInterface.ThemeUtil;
import org.kde.kdeconnect_custom.R;
import org.kde.kdeconnect_custom.databinding.UnifiedRunCommandsBinding;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class UnifiedRunCommandActivity extends AppCompatActivity {
    private UnifiedRunCommandsBinding binding;
    private final RunCommandPlugin.CommandsChangedCallback commandsChangedCallback = this::updateView;
    private List<CommandEntry> commandItems;


    private void updateView() {
        runOnUiThread(new Runnable() {
            @Override
                    public void run() {
                Log.d("UnifiedRunCommand", "refreshing Unified RunCommand view..");


                registerForContextMenu(binding.runCommandsList);

                commandItems = new ArrayList<>();
                boolean deviceHasNoRunCommands = false;

                for (Device d : BackgroundService.getInstance().getDeviceList()) {
                    Log.d("UnifiedRunCommand", "device name = " + d.getName());
                    if (!(d.isReachable() && d.isPaired())) {
                        Log.d("UnifiedRunCommand", "device " + d.getName() + " is currently inaccessible.");
                        continue;
                    }
                    final RunCommandPlugin plugin = d.getPlugin(RunCommandPlugin.class);
                    if (plugin == null) {
                        Log.d("UnifiedRunCommand", "device's RunCommandPlugin is currently null! ");
                        continue;
                    }

                    if (plugin.getCommandList().size() == 0) {
                        deviceHasNoRunCommands = true;
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

                binding.addCommandExplanation.setText(getString(R.string.add_command_instructions));
                binding.addCommandExplanation.setVisibility(deviceHasNoRunCommands ? View.VISIBLE : View.GONE);
            }
        });

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeUtil.setUserPreferredTheme(this);

        binding = UnifiedRunCommandsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbarLayout.toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
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

        for (Device d : BackgroundService.getInstance().getDeviceList()) {
            final RunCommandPlugin plugin = d.getPlugin(RunCommandPlugin.class);
            if (plugin == null) {
                continue;
            }

            // the 'remove' step is to ensure idempotency
            plugin.removeCommandsUpdatedCallback(commandsChangedCallback);
            plugin.addCommandsUpdatedCallback(commandsChangedCallback);
        }
        updateView();
    }

    @Override
    protected void onPause() {
        super.onPause();

        for (Device d : BackgroundService.getInstance().getDeviceList()) {
            final RunCommandPlugin plugin = d.getPlugin(RunCommandPlugin.class);
            if (plugin == null) {
                continue;
            }

            plugin.removeCommandsUpdatedCallback(commandsChangedCallback);
        }
    }
}
