package com.nicolasbrailo.vlcfreemote;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.nicolasbrailo.vlcfreemote.model.VlcStatus;
import com.nicolasbrailo.vlcfreemote.vlc_connector.Cmd_ClearPlaylist;
import com.nicolasbrailo.vlcfreemote.vlc_connector.Cmd_GetPlaylist;
import com.nicolasbrailo.vlcfreemote.vlc_connector.Cmd_RemoveFromPlaylist;
import com.nicolasbrailo.vlcfreemote.vlc_connector.Cmd_StartPlaying;
import com.nicolasbrailo.vlcfreemote.vlc_connector.RemoteVlc;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class PlaylistView extends VlcFragment implements View.OnClickListener, PopupMenu.OnMenuItemClickListener {

    private PlaylistEntry_ViewAdapter playlistViewAdapter;
    private boolean attached = false;

    /* Mostly Android boilerplate                               */
    /************************************************************/
    public PlaylistView() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_playlist, container, false);

        playlistViewAdapter = new PlaylistEntry_ViewAdapter(this, getActivity());
        ((ListView) v.findViewById(R.id.wPlaylist_List)).setAdapter(playlistViewAdapter);
        v.findViewById(R.id.wPlaylist_PopupMenu).setOnClickListener(this);

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        triggerPlaylistUpdate();
    }

    @Override
    public void onAttach(@NonNull Context activity) {
        super.onAttach(activity);
        attached = true;
        triggerPlaylistUpdate();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        attached = false;
    }


    /* UI Logic                                                 */
    /************************************************************/
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.wPlaylist_PopupMenu:
                showPopupMenu();
                return;
        }

        Cmd_GetPlaylist.PlaylistEntry item = (Cmd_GetPlaylist.PlaylistEntry) v.getTag();
        if (item == null)
            throw new RuntimeException(Cmd_GetPlaylist.class.getName() + " received a click event for a view with no playlist item.");

        switch (v.getId()) {
            case R.id.wPlaylistElement_CurrentStatus:
            case R.id.wPlaylistElement_Duration:
            case R.id.wPlaylistElement_Name:
                requestPlay(item.id);
                break;
            case R.id.wPlaylistElement_Remove:
                removeFromPlaylist(item.id);
                triggerPlaylistUpdate();
                break;
            default:
                throw new RuntimeException(Cmd_GetPlaylist.class.getName() + " received a click event for a view which doesn't exist.");
        }

    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.wPlaylist_Clear:
                clearPlaylist();
                break;
            case R.id.wPlaylist_Refresh:
                triggerPlaylistUpdate();
                break;
            default:
                throw new RuntimeException(DirListingView.class.getName() + " received a menu event it can't handle.");
        }

        return true;
    }

    private void showPopupMenu() {
        final View menu = requireActivity().findViewById(R.id.wPlaylist_PopupMenu);
        final PopupMenu popup = new PopupMenu(getContext(), menu);
        popup.getMenuInflater().inflate(R.menu.fragment_playlist_popup_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(this);
        popup.show();
    }

    /* Vlc interaction                                          */
    /************************************************************/

    private void requestPlay(Integer id) {
        getVlc().exec(new Cmd_StartPlaying(id, getVlc()));
    }

    private void clearPlaylist() {
        getVlc().exec(new Cmd_ClearPlaylist(getVlc()));
        playlistViewAdapter.clear();
    }

    private void removeFromPlaylist(int id) {
        getVlc().exec(new Cmd_RemoveFromPlaylist(id, getVlc()));
    }

    public void triggerPlaylistUpdate() {
        // Vlc may be null if the parent activity hasn't attached yet
        if (! attached) return;

        // getVlc() might return null on app start, if called before main's onCreate
        final RemoteVlc vlc = getVlc();
        vlc.exec(new Cmd_GetPlaylist(new Cmd_GetPlaylist.Callback() {
            @Override
            public void onContentAvailable(List<Cmd_GetPlaylist.PlaylistEntry> results) {
                playlistViewAdapter.clear();
                playlistViewAdapter.addAll(results);
            }

            @Override
            public void onContentError() {
                CharSequence msg = getString(R.string.error_on_playlist_retrieve);
                Toast toast = Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG);
                toast.show();
            }
        }));
    }

    public void onVlcStatusUpdate(VlcStatus status) {
        this.playlistViewAdapter.updateVlcStatus(status);
    }

    /* List view stuff                                          */
    /************************************************************/
    private static class PlaylistEntry_ViewAdapter extends ArrayAdapter<Cmd_GetPlaylist.PlaylistEntry> {
        private static final int layoutResourceId = R.layout.fragment_playlist_list_element;
        private final Context context;
        private final View.OnClickListener onClickCallback;
        private String playingTitle;
        private String playingFilename;

        public static class Row {
            Cmd_GetPlaylist.PlaylistEntry values;
            ImageButton wPlaylistElement_CurrentStatus;
            TextView wPlaylistElement_Name;
            TextView wPlaylistElement_Duration;
            ImageButton wPlaylistElement_Remove;
        }

        public PlaylistEntry_ViewAdapter(View.OnClickListener onClickCallback, Context context) {
            super(context, layoutResourceId, new ArrayList<Cmd_GetPlaylist.PlaylistEntry>());
            this.context = context;
            this.onClickCallback = onClickCallback;
            playingTitle = "";
            playingFilename = "";
        }

        public void updateVlcStatus(VlcStatus status) {
            final String newTitle = (status.currentMedia_title != null)? status.currentMedia_title : "";
            final String newFilename = (status.currentMedia_filename != null)? status.currentMedia_filename : "";

            if ((!newTitle.equals(playingTitle)) || (!newFilename.equals(playingFilename))) {
                playingTitle = newTitle;
                playingFilename = newFilename;
                this.notifyDataSetChanged();
            }
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
            holder.values = this.getItem(position);

            holder.wPlaylistElement_CurrentStatus = row.findViewById(R.id.wPlaylistElement_CurrentStatus);
            holder.wPlaylistElement_CurrentStatus.setTag(holder.values);
            holder.wPlaylistElement_CurrentStatus.setOnClickListener(onClickCallback);

            if ((!playingTitle.isEmpty()) && playingTitle.equals(holder.values.name)) {
                holder.wPlaylistElement_CurrentStatus.setVisibility(View.VISIBLE);
            } else if ((!playingFilename.isEmpty()) && playingFilename.equals(holder.values.name)) {
                holder.wPlaylistElement_CurrentStatus.setVisibility(View.VISIBLE);
            } else {
                holder.wPlaylistElement_CurrentStatus.setVisibility(View.INVISIBLE);
            }

            holder.wPlaylistElement_Name = row.findViewById(R.id.wPlaylistElement_Name);
            holder.wPlaylistElement_Name.setText(holder.values.name);
            holder.wPlaylistElement_Name.setTag(holder.values);
            holder.wPlaylistElement_Name.setOnClickListener(onClickCallback);

            holder.wPlaylistElement_Duration = row.findViewById(R.id.wPlaylistElement_Duration);
            if (holder.values.duration >= 0) {
                holder.wPlaylistElement_Duration.
                        setText(String.format(Locale.getDefault(), "%d:%02d", holder.values.duration / 60, holder.values.duration % 60));
            } else {
                holder.wPlaylistElement_Duration.setText(context.getString(R.string.playlist_item_duration_unknown));
            }

            holder.wPlaylistElement_Duration.setTag(holder.values);
            holder.wPlaylistElement_Duration.setOnClickListener(onClickCallback);

            holder.wPlaylistElement_Remove = row.findViewById(R.id.wPlaylistElement_Remove);
            holder.wPlaylistElement_Remove.setTag(holder.values);
            holder.wPlaylistElement_Remove.setOnClickListener(onClickCallback);

            row.setTag(holder);

            return row;
        }
    }
}
