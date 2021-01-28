package com.nicolasbrailo.vlcfreemote;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.nicolasbrailo.vlcfreemote.model.Server;
import com.nicolasbrailo.vlcfreemote.model.VlcStatus;
import com.nicolasbrailo.vlcfreemote.vlc_connector.Cmd_AddToPlaylist;
import com.nicolasbrailo.vlcfreemote.vlc_connector.Cmd_UpdateStatus;
import com.nicolasbrailo.vlcfreemote.vlc_connector.RemoteVlc;
import com.nicolasbrailo.vlcfreemote.vlc_connector.VlcCommand;

import java.io.PrintWriter;
import java.io.StringWriter;

public class MainActivity extends FragmentActivity
                          implements RemoteVlc.ConnectionProvider,
                                     VlcCommand.GeneralCallback,
                                     VlcStatus.Observer,
                                     ServerSelectView.ServerSelectionCallback,
                                     DirListingView.DirListingCallback {

    private static final int PERIODIC_VLC_STATUS_UPDATE_DELAY = 2500;

    private RemoteVlc vlcConnection = null;
    private PlayerControllerView playerControllerView;
    private PlaylistView playlistView;
    private DirListingView dirListView;
    private ServerSelectView serverSelectView;
    private MainMenuNavigation mainMenu;
    private boolean periodicStatusUpdateRequested = false;

    private class MainMenuNavigation extends FragmentPagerAdapter
                                     implements ViewPager.OnPageChangeListener {

        private final PlaylistView playlistView;
        private final DirListingView dirListView;
        private final ServerSelectView serverSelectView;
        private final ViewPager parentView;

        MainMenuNavigation(ViewPager view, FragmentManager fm, PlaylistView playlistView,
                           DirListingView dirListView, ServerSelectView serverSelectView)
        {
            super(fm);
            this.parentView = view;
            this.playlistView = playlistView;
            this.dirListView = dirListView;
            this.serverSelectView = serverSelectView;

            parentView.setAdapter(this);
            parentView.addOnPageChangeListener(this);
        }

        @NonNull
        @Override
        public Fragment getItem(int i) {
            switch (i) {
                case 0: return playlistView;
                case 1: return dirListView;
                case 2: return serverSelectView;
                default: throw new RuntimeException(MainMenuNavigation.class.getName() + " tried to select a page item which doesn't exist.");
            }
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0: return getString(R.string.main_menu_title_playlist);
                case 1: return getString(R.string.main_menu_title_dir_listing);
                case 2: return getString(R.string.main_menu_title_servers);
                default: throw new RuntimeException(MainMenuNavigation.class.getName() + " tried to get a title for a page which doesn't exist.");
            }
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override public void onPageScrolled(int i, float v, int i2) {}
        @Override public void onPageScrollStateChanged(int i) {}

        @Override
        public void onPageSelected(int i) {
            switch (i) {
                case 0: /*playlistView.triggerPlaylistUpdate();*/ return;
                case 1: /*dirListView.triggerCurrentPathListUpdate();*/ return;
                case 2: /*serverSelectView.scanServers();*/ return;
                default: throw new RuntimeException(MainMenuNavigation.class.getName() + " selected a page which doesn't exist.");
            }
        }

        void jumpToServerSelection() { parentView.setCurrentItem(2, true); }
        void jumpToPlaylist() { parentView.setCurrentItem(0, true); }
    }

    private void safePutFragment(final Bundle outState, final String name, Fragment obj) {
        try {
            if (obj.isAdded()) {
                getSupportFragmentManager().putFragment(outState, name, obj);
            }
        } catch (IllegalStateException e) {
            // Some fragments might not be in the fragment manager: if this is the case, just save a null
            // object to give the activity a chance of recreating the fragment when resuming
        }
    }

    @Override
    // This should handle things like device rotation: if state is not saved then the fragment may
    // be recreated and all sort of funny crashes will happen.
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        safePutFragment(outState, PlayerControllerView.class.getName(), playerControllerView);
        safePutFragment(outState, PlaylistView.class.getName(), playlistView);
        safePutFragment(outState, DirListingView.class.getName(), dirListView);
        safePutFragment(outState, ServerSelectView.class.getName(), serverSelectView);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (PlayerControllerView.shouldUseDarkTheme(this)) {
            setTheme(R.style.DarkTheme);
        } else {
            setTheme(R.style.LightTheme);
        }

        // Setting the content must be done after setting the theme
        setContentView(R.layout.activity_main);

        // Create or restore all views
        if (savedInstanceState != null) {
            playerControllerView = (PlayerControllerView) getSupportFragmentManager().getFragment(savedInstanceState, PlayerControllerView.class.getName());
            playlistView = (PlaylistView) getSupportFragmentManager().getFragment(savedInstanceState, PlaylistView.class.getName());
            dirListView = (DirListingView) getSupportFragmentManager().getFragment(savedInstanceState, DirListingView.class.getName());
            serverSelectView = (ServerSelectView) getSupportFragmentManager().getFragment(savedInstanceState, ServerSelectView.class.getName());
        }

        if (this.playerControllerView == null) this.playerControllerView = new PlayerControllerView();
        if (this.playlistView == null) this.playlistView = new PlaylistView();
        if (this.dirListView == null) this.dirListView = new DirListingView();
        if (this.serverSelectView == null) this.serverSelectView = new ServerSelectView();

        this.mainMenu = new MainMenuNavigation(((ViewPager) super.findViewById(R.id.wMainMenu)),
                getSupportFragmentManager(), playlistView, dirListView, serverSelectView);

        final Server srv = ServerSelectView.getLastUsedServer(this);
        if (srv != null) {
            onNewServerSelected(srv);
        } else {
            onConnectionError();
        }
    }

    @Override
    public void onNewServerSelected(final Server srv) {
        if (srv!=null) {
            Log.i(getClass().getSimpleName(), "Connecting to server " + srv.ip + ":" + srv.vlcPort);
            this.vlcConnection = new RemoteVlc(srv, this);
        } else {
            // Connect to dummy server: the first command will fail and prompt a new server select
            this.vlcConnection = new RemoteVlc(new Server("", 0, null), this);
        }

        // This method may be called without an activity attached
        if (playlistView != null && dirListView != null && mainMenu != null) {
            playlistView.triggerPlaylistUpdate();
            dirListView.onServerChanged(srv);
            mainMenu.jumpToPlaylist();
        }

        // Update status on new server
        vlcConnection.exec(new Cmd_UpdateStatus(vlcConnection));
    }

    @Override
    public void onAddToPlaylistRequested(final String uri) {
        Log.i(getClass().getSimpleName(), "Add to playlist: " + uri);
        vlcConnection.exec(new Cmd_AddToPlaylist(uri, vlcConnection));

        if (vlcConnection.getLatestStats().isStopped()) {
            // Start playing with a small delay to give VLC some time to add the files to the playlist
            // Horrible! Maybe add a "when done" callback to Vlc_Commands to make this cleaner?
            (new Handler()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    // vlcConnection.exec(new Cmd_TogglePlay(vlcConnection));
                    // TODO: When stopped, VLC keeps a "pointer" to the last played element, so just
                    // toggling play status is not enough: it's necessary to actually tell VLC which
                    // file to play
                }
            }, 500);
        }

        this.playlistView.triggerPlaylistUpdate();
    }


    @Override
    public synchronized void onVlcStatusUpdate(VlcStatus result) {
        this.playlistView.onVlcStatusUpdate(result);
        this.playerControllerView.onStatusUpdated(this, result);

        if (!periodicStatusUpdateRequested && result.isPlaying()) {
            periodicStatusUpdateRequested = true;
            (new Handler()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    periodicStatusUpdateRequested = false;
                    vlcConnection.exec(new Cmd_UpdateStatus(vlcConnection));
                }
            }, PERIODIC_VLC_STATUS_UPDATE_DELAY);
        }
    }

    @Override
    public Server getActiveServer() {
        return getActiveVlcConnection().getServer();
    }

    @Override
    public RemoteVlc getActiveVlcConnection() {
        // IF this method is somehow called before onCreate, vlcConnection will be null and
        // we should try to create it from scratch
        if (vlcConnection == null) {
            onNewServerSelected(ServerSelectView.getLastUsedServer(this));
        }

        if (vlcConnection == null) {
            onConnectionError();
        }

        return vlcConnection;
    }

    @Override
    public void onAuthError() {
        mainMenu.jumpToServerSelection();

        CharSequence msg = getString(R.string.status_vlc_wrong_password);
        Toast toast = Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG);
        toast.show();
    }

    public void onConnectionError() {
        onConnectionError("");
    }

    @Override
    public void onConnectionError(final String error) {
        mainMenu.jumpToServerSelection();

        final String server;
        if (vlcConnection != null && vlcConnection.getServer() != null) {
            server = vlcConnection.getServer().ip + ':' + vlcConnection.getServer().vlcPort;
        } else {
            server = "unknown address";
        }
        final String msg = getString(R.string.status_vlc_cant_connect);
        final String fmtMsg = String.format(msg, server, error.length() > 0? error : "");
        Toast toast = Toast.makeText(getApplicationContext(), fmtMsg, Toast.LENGTH_LONG);
        toast.show();
    }


    @Override
    public void onVlcStatusFetchError(final String msg) {
        onAppBug(msg);
        onVlcStatusFetchError();
    }

    @Override
    public void onVlcStatusFetchError() {
        // Getting here means there was an error that couldn't be handled. The
        // only reasonable thing to do is to disconnect and try again.
        onConnectionError();
    }


    @Override
    public void onSystemError(Exception e) {
        Log.e(getClass().getSimpleName(), "Programmer error! " + e.toString());

        mainMenu.jumpToServerSelection();

        final String msg = getString(R.string.status_fatal_app_error);
        final String server = vlcConnection.getServer().ip + ':' + vlcConnection.getServer().vlcPort;
        final String fmtMsg = String.format(msg, server);

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);

        final String fmtMsgExtra;
        final String msgExtra = getString(R.string.status_fatal_app_error_extra_info);
        fmtMsgExtra = fmtMsg + String.format(msgExtra, e.getMessage() + sw.toString());

        onAppBug(fmtMsgExtra);
    }

    public void onAppBug(final String bugDetails) {
        final AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(R.string.status_fatal_app_error_title)
                .setMessage(bugDetails)
                .setCancelable(false)
                .setPositiveButton(R.string.app_bug_try_ignore, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // Do nothing
                    }
                })
                .setNegativeButton(R.string.app_bug_submit_bug_report, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        final String url = Uri.parse("https://github.com/nicolasbrailo/VlcFreemote/issues/new")
                                .buildUpon()
                                .appendQueryParameter("title", "Bug report")
                                .appendQueryParameter("body", bugDetails)
                                .build().toString();

                        final Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse(url));
                        startActivity(intent);
                    }
                });
        alert.create().show();
    }
}
