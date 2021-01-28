package com.nicolasbrailo.vlcfreemote;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.nicolasbrailo.vlcfreemote.local_settings.RememberedServers;
import com.nicolasbrailo.vlcfreemote.model.Server;
import com.nicolasbrailo.vlcfreemote.net_utils.ServerScanner;

import java.util.ArrayList;
import java.util.List;


public class ServerSelectView extends Fragment implements View.OnClickListener {

    public interface ServerSelectionCallback {
        void onNewServerSelected(final Server srv);
        Server getActiveServer();
    }

    private ServerScanner serverScanner = null;
    private Servers_ViewAdapter listViewAdapter;
    private ServerSelectionCallback callback;

    /* Android stuff                                            */
    /************************************************************/

    public ServerSelectView() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.fragment_server_select_view, container, false);

        this.listViewAdapter = new Servers_ViewAdapter(this, getActivity());
        ((ListView) v.findViewById(R.id.wServerSelect_ScannedServersList)).setAdapter(listViewAdapter);
        populateLRUServersList(v);

        v.findViewById(R.id.wServerSelect_ToggleServerScanning).setOnClickListener(this);
        v.findViewById(R.id.wServerSelect_CustomServerSet).setOnClickListener(this);

        updateCurrentServerLabel(v);

        List<String> ips = ServerScanner.getLocalNetworks();
        if (!ips.isEmpty()) {
            ((EditText)v.findViewById(R.id.wServerSelect_CustomServerIp)).setText(ips.get(0));
        }

        return v;
    }

    @Override
    public void onAttach(@NonNull Context activity) {
        super.onAttach(activity);

        try {
            callback = (ServerSelectionCallback) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement onServerSelectCallback");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        callback = null;
    }

    /* Event handlers                                           */
    /************************************************************/

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.wServerSelect_ToggleServerScanning:
                toggleServerScanning();
                break;

            case R.id.wServerSelect_CustomServerSet:
                onCustomServerSelected();
                break;

            case R.id.wServerSelect_ServerForget:
                onForgetServerClicked((Server) v.getTag());
                break;

            case R.id.wServerSelect_ServerRename:
                onServerRename((Server) v.getTag());
                break;

            case R.id.wServerSelect_ScannedServerAddress:
            case R.id.wServerSelect_ScannedServerSelect:
                onServerSelected((Server) v.getTag());
                break;

            default:
                throw new RuntimeException(getClass().getName() + " received an event it doesn't know how to handle.");
        }
    }

    private void onCustomServerSelected() {
        final FragmentActivity activity = getActivity();
        assert activity != null;
        final String ip = ((EditText)activity.findViewById(R.id.wServerSelect_CustomServerIp)).getText().toString();
        final String str_port = ((EditText)activity.findViewById(R.id.wServerSelect_CustomServerPort)).getText().toString();

        // If port is not a valid int, the connection will just fail later on.
        final Server srv = new Server(ip, Integer.parseInt(str_port), null);

        onServerSelected(srv);
    }

    private void onForgetServerClicked(final Server srv) {
        final RememberedServers db = new RememberedServers(getContext());
        db.forget(srv);
        View v = getView();
        if (v != null) populateLRUServersList(v);
    }

    private void onServerRename(final Server srv) {
        // Show a dialog to name a server
        final AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setTitle(getString(R.string.server_select_request_name_dialog_title));
        final EditText input = new EditText(getActivity());
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(srv.getDisplayName());
        builder.setView(input);
        final RememberedServers db = new RememberedServers(getContext());

        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                final String newName = input.getText().toString();
                srv.setDisplayName(newName);
                db.rememberServer(srv);
                updateCurrentServerLabel(getView());
            }
        });

        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    private void onServerSelected(final Server srv) {
        setScanModeOff();
        Log.i(getClass().getSimpleName(), "Selected server " + srv.ip + ":" + srv.vlcPort);

        if (srv.vlcPort == null) {
            CharSequence msg = "You can't use this server: functionality unimplemented (yet).";
            Toast toast = Toast.makeText(requireActivity().getApplicationContext(), msg, Toast.LENGTH_LONG);
            toast.show();
            return;
        }

        // Get last used pass for this server, if known. This server will have some settings saved
        // (Like last used path)
        final RememberedServers db = new RememberedServers(getContext());
        final Server rememberedServer = db.getRememberedServer(srv);
        final Server srvToUse = (rememberedServer != null)? rememberedServer : srv;

        // Show a dialog to confirm the pass (or enter a new one)
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getString(R.string.server_select_request_password_dialog_title));
        final EditText input = new EditText(getActivity());
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setText(srvToUse.getPassword());
        builder.setView(input);

        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                final String password = input.getText().toString();
                srvToUse.setPassword(password);

                db.rememberServer(srvToUse);
                callback.onNewServerSelected(srvToUse);
                updateCurrentServerLabel(getView());
            }
        });

        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    public void onPause() {
        super.onPause();
        setScanModeOff();
    }

    /* UI Logic                                                 */
    /************************************************************/
    void updateCurrentServerLabel(final View v) {
        final String currentServer;
        if (callback == null || callback.getActiveServer() == null) {
            currentServer = "Not connected";
        } else {
            currentServer = callback.getActiveServer().ip + ":" + callback.getActiveServer().vlcPort;
        }

        final TextView currServer = v.findViewById(R.id.wServerSelect_CurrentServer);
        currServer.setText(String.format(getResources().getString(R.string.server_select_current_server), currentServer));
    }

    synchronized private void toggleServerScanning() {
        if (serverScanner != null) {
            Log.i(getClass().getSimpleName(), "Cancelling network scan");
            serverScanner.cancel(true);
            return;
        }

        Log.i(getClass().getSimpleName(), "Start network scan");
        startScanMode();

        serverScanner = new ServerScanner(new ServerScanner.Callback() {
            @Override
            public void onServerDiscovered(Server srv) {
                Log.i(getClass().getSimpleName(), "Found server " + srv.ip);
                listViewAdapter.add(srv);
            }

            @Override
            public void onScanFinished() {
                setScanModeOff();
                Log.i(getClass().getSimpleName(), "Completed network scan");
            }

            @Override
            public void onScanCancelled() {
                setScanModeOff();
                Log.i(getClass().getSimpleName(), "Cancelled network scan");
            }

            @Override
            public void onNoNetworkAvailable() {
                setScanModeOff();

                // If there's no activity we're not being displayed, so it's better not to update the UI
                if (!isAdded()) return;
                CharSequence msg = getResources().getString(R.string.status_no_network_detected);
                Toast toast = Toast.makeText(requireActivity(), msg, Toast.LENGTH_LONG);
                toast.show();
            }
        });
    }

    synchronized private void startScanMode() {
        // If there's no activity we're not being displayed, so it's better not to update the UI
        final FragmentActivity activity = getActivity();
        if (!isAdded() || activity == null) return;

        activity.findViewById(R.id.wServerSelect_ScanningServersIndicator).setVisibility(View.VISIBLE);
        ((Button)activity.findViewById(R.id.wServerSelect_ToggleServerScanning)).setText(R.string.server_select_toggle_scanning_stop);

        // Clean old servers list
        this.listViewAdapter.clear();
    }

    synchronized private void setScanModeOff() {
        if (serverScanner != null) serverScanner.cancel(true);
        serverScanner = null;

        // If there's no activity we're not being displayed, so it's better not to update the UI
        final FragmentActivity activity = getActivity();
        if (!isAdded() || activity == null) return;

        activity.findViewById(R.id.wServerSelect_ScanningServersIndicator).setVisibility(View.GONE);
        ((Button)activity.findViewById(R.id.wServerSelect_ToggleServerScanning))
                .setText(R.string.server_select_toggle_scanning_start);
    }

    private void populateLRUServersList(View v) {
        final Servers_ViewAdapter lruServersListAdapter = new Servers_ViewAdapter(this, getActivity());
        ListView lst = v.findViewById(R.id.wServerSelect_LRUServersList);
        lst.setAdapter(lruServersListAdapter);

        for (final Server srv : (new RememberedServers(getContext())).getLastUsedServers(10)) {
            lruServersListAdapter.add(srv);
        }
    }

    /**
     * Returns the last server the user tried to connect to. A connection attempt is defined as
     * "the user supplied a password for the server", regardless of the attempt success.
     * @param context Since this method may be called before onAttach, there may be no activity
     *                known. Caller should specify activity context.
     * @return last used server
     */
    static public Server getLastUsedServer(Context context) {
        return (new RememberedServers(context)).getLastUsedServer();
    }

    /* List view stuff                                          */
    /************************************************************/

    private static class Servers_ViewAdapter extends ArrayAdapter<Server> {
        private static final int layoutResourceId = R.layout.fragment_server_select_element;
        private final Context context;
        private final View.OnClickListener onClickCallback;

        static class Row {
            Server value;
            ImageView wServerSelect_ServerTypeIcon;
            TextView wServerSelect_ScannedServerAddress;
            ImageButton wServerSelect_ServerForget;
            ImageButton wServerSelect_ScannedServerSelect;
            ImageButton wServerSelect_ServerRename;
        }

        Servers_ViewAdapter(View.OnClickListener onClickCallback, Context context) {
            super(context, layoutResourceId, new ArrayList<Server>());
            this.context = context;
            this.onClickCallback = onClickCallback;
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {

            final View row;
            if (convertView == null) {
                LayoutInflater inflater = ((Activity) context).getLayoutInflater();
                row = inflater.inflate(layoutResourceId, parent, false);
            } else {
                row = convertView;
            }

            Row holder = new Row();
            holder.value = this.getItem(position);

            holder.wServerSelect_ServerTypeIcon = row.findViewById(R.id.wServerSelect_ServerTypeIcon);
            holder.wServerSelect_ServerTypeIcon.setOnClickListener(onClickCallback);
            if (holder.value.vlcPort != null) {
                holder.wServerSelect_ServerTypeIcon.setImageResource(R.mipmap.ic_vlc_icon);
            } else {
                holder.wServerSelect_ServerTypeIcon.setImageResource(R.mipmap.ic_tux_icon);
            }

            holder.wServerSelect_ScannedServerAddress = row.findViewById(R.id.wServerSelect_ScannedServerAddress);
            holder.wServerSelect_ScannedServerAddress.setText(holder.value.getFullDisplayName());
            holder.wServerSelect_ScannedServerAddress.setTag(holder.value);
            holder.wServerSelect_ScannedServerAddress.setOnClickListener(onClickCallback);

            holder.wServerSelect_ScannedServerSelect = row.findViewById(R.id.wServerSelect_ScannedServerSelect);
            holder.wServerSelect_ScannedServerSelect.setTag(holder.value);
            holder.wServerSelect_ScannedServerSelect.setOnClickListener(onClickCallback);

            holder.wServerSelect_ServerForget = row.findViewById(R.id.wServerSelect_ServerForget);
            holder.wServerSelect_ServerForget.setTag(holder.value);
            holder.wServerSelect_ServerForget.setOnClickListener(onClickCallback);

            holder.wServerSelect_ServerRename = row.findViewById(R.id.wServerSelect_ServerRename);
            holder.wServerSelect_ServerRename.setTag(holder.value);
            holder.wServerSelect_ServerRename.setOnClickListener(onClickCallback);

            row.setTag(holder);

            return row;
        }
    }
}
