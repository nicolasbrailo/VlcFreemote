package com.nicolasbrailo.vlcfreemote;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import com.nicolasbrailo.vlcfreemote.model.VlcPath;
import com.nicolasbrailo.vlcfreemote.model.Server;
import com.nicolasbrailo.vlcfreemote.vlc_connector.Cmd_DirList;
import com.nicolasbrailo.vlcfreemote.vlc_connector.RemoteVlc;
import com.nicolasbrailo.vlcfreemote.vlc_connector.VlcCommand;

import java.util.ArrayList;
import java.util.List;


public class DirListingView extends VlcFragment
                            implements View.OnClickListener,
                                       View.OnLongClickListener,
                                       VlcPath.UICallback,
                                       PopupMenu.OnMenuItemClickListener {

    public interface DirListingCallback {
        void onAddToPlaylistRequested(final String uri);
    }

    private DirListEntry_ViewAdapter dirViewAdapter;
    private DirListingCallback callback = null;
    private VlcPath vlcPath = null;
    private VlcCommand.GeneralCallback systemFailCallback = null;

    /* Mostly Android boilerplate                               */
    /************************************************************/
    public DirListingView() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_dir_listing_view, container, false);
        dirViewAdapter = new DirListEntry_ViewAdapter(this, this, getActivity());
        ((ListView) v.findViewById(R.id.wDirListing_List)).setAdapter(dirViewAdapter);
        v.findViewById(R.id.wDirListing_PopupMenu).setOnClickListener(this);
        return v;

    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        try {
            this.callback = (DirListingCallback) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement DirListingCallback");
        }

        try {
            this.vlcPath = new VlcPath((RemoteVlc.ConnectionProvider) context, context, this);
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement RemoteVlc.ConnectionProvider");
        }

        try {
            this.systemFailCallback = (VlcCommand.GeneralCallback) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement VlcCommand.GeneralCallback");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        this.callback = null;
        this.vlcPath = null;
        this.systemFailCallback = null;
    }

    /* Display & event handling                                 */
    /************************************************************/

    @Override
    public void onResume() {
        super.onResume();
        triggerCurrentPathListUpdate();
    }

    @Override
    public void onClick(View v) {
        Cmd_DirList.DirListEntry item = (Cmd_DirList.DirListEntry) v.getTag();

        switch (v.getId()) {
            case R.id.wDirListElement_Action:
                if (item == null) throw new RuntimeException(DirListingView.class.getName() + " received a menu item with no tag");
                onAddToPlaylistRequested(item);
                break;

            case R.id.wDirListElement_Name:
                if (item == null) throw new RuntimeException(DirListingView.class.getName() + " received a menu item with no tag");
                if (item.isDirectory) {
                    vlcPath.cd(item.path, item.human_friendly_path);
                    triggerCurrentPathListUpdate();
                } else {
                    onAddToPlaylistRequested(item);
                }

                break;

            case R.id.wDirListElement_AlreadySeen:
                toggleItemSeen(item);
                break;

            case R.id.wDirListing_PopupMenu:
                showPopupMenu();
                break;

            default:
                throw new RuntimeException(DirListingView.class.getName() + " received a click event it can't handle.");
        }
    }

    @Override
    public boolean onLongClick(View v) {
        // If a user long-pressed a file marked as viewed, unmark it
        Cmd_DirList.DirListEntry item = (Cmd_DirList.DirListEntry) v.getTag();
        return toggleItemSeen(item);
    }

    boolean toggleItemSeen(final Cmd_DirList.DirListEntry item) {
        if (item == null) throw new RuntimeException(DirListingView.class.getName() + " long-pressed a menu item with no tag");
        if (item.isDirectory) {
            // Nothing to do with dirs
            return false;
        }

        item.wasPlayedBefore = ! item.wasPlayedBefore;
        this.dirViewAdapter.notifyDataSetChanged();

        // Save to DB
        vlcPath.toggleSeen(item.path, item.wasPlayedBefore);

        if (!item.wasPlayedBefore) {
            // If item is marked as not played before it means the user toggled it
            // Add a pop up notification to let the user know what this feature is
            CharSequence msg = getString(R.string.playlist_item_marked_as_unseen);
            Toast toast = Toast.makeText(getContext(), msg, Toast.LENGTH_LONG);
            toast.show();
        }

        return true;
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.wDirListing_Bookmark:
                saveCurrentPathAsBookmark();
                break;

            case R.id.wDirListing_JumpToBookmark:
                jumpToBookmark();
                break;

            case R.id.wDirListing_ManageBookmark:
                deleteBookmark();
                break;

            case R.id.wDirListing_PlayRandom:
                playRandomSubDir();
                break;

            default:
                throw new RuntimeException(DirListingView.class.getName() + " received a menu event it can't handle.");
        }

        return true;
    }


    /* UI stuff                                                 */
    /************************************************************/

    private void showPopupMenu() {
        final View menu = requireView().findViewById(R.id.wDirListing_PopupMenu);
        final PopupMenu popup = new PopupMenu(getContext(), menu);
        popup.getMenuInflater().inflate(R.menu.fragment_dir_listing_popup_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(this);
        popup.show();
    }

    private void triggerCurrentPathListUpdate() {
        // If there's no activity we're not being displayed, so it's better not to update the UI
        final FragmentActivity activity = getActivity();
        if (!isAdded() || activity == null) return;

        vlcPath.updateDirContents();

        dirViewAdapter.clear();
        ((TextView) activity.findViewById(R.id.wDirListing_CurrentPath)).setText(vlcPath.getPrettyCWD());
        activity.findViewById(R.id.wDirListing_List).setEnabled(false);
        activity.findViewById(R.id.wDirListing_LoadingIndicator).setVisibility(View.VISIBLE);
    }

    @Override
    public void onNewDirListAvailable(List<Cmd_DirList.DirListEntry> results) {
        // If there's no activity we're not being displayed, so it's better not to update the UI
        final FragmentActivity activity = getActivity();
        if (!isAdded() || activity == null) return;

        activity.findViewById(R.id.wDirListing_List).setEnabled(true);
        activity.findViewById(R.id.wDirListing_LoadingIndicator).setVisibility(View.GONE);

        dirViewAdapter.clear();
        dirViewAdapter.addAll(results);
    }

    private void onAddToPlaylistRequested(final Cmd_DirList.DirListEntry path) {
        // The model (VlcPath) will update the "played before" flag in the database, but we also
        // need to update the UI right now, otherwise the user will need to refresh the directory
        // to see the "seen" flag
        path.wasPlayedBefore = true;
        this.dirViewAdapter.notifyDataSetChanged();

        callback.onAddToPlaylistRequested(path.path);

        if (! path.isDirectory) {
            vlcPath.toggleSeen(path.path, true);
        }
    }

    private void playRandomSubDir() {
        vlcPath.getRandomSubdir(new VlcPath.RandomSubdirCallback() {
            @Override
            public void onRandomSubdirAvailable(String path) {
                callback.onAddToPlaylistRequested(path);
            }

            @Override
            public void onError() {
                final String msg = getResources().getString(R.string.dir_listing_random_play_failed);
                Toast toast = Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT);
                toast.show();
            }
        });
    }

    @Override
    public void onDirListFatalFailure(VlcPath.VlcPath_ApplicationError ex) {
        systemFailCallback.onSystemError(ex);
    }

    public void onServerChanged(final Server srv) {
        if (vlcPath != null) {
            vlcPath.onServerChanged(srv);
            if (dirViewAdapter != null) dirViewAdapter.clear();
        }
    }

    private void saveCurrentPathAsBookmark() {
        vlcPath.bookmarkCurrentDirectory();

        final String msg = String.format(getResources().getString(R.string.dir_listing_saved_bookmark), vlcPath.getPrettyCWD());
        Toast toast = Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT);
        toast.show();
    }

    // TODO: Move to a Fragment?
    private interface BookmarkCallback {
        void onBookmarkSelected(final String uri, final String prettyName);
    }

    private void jumpToBookmark() {
        displayBookmarkPicker(R.string.R_string_dir_listing_goto_bookmark_title, new BookmarkCallback() {
            @Override
            public void onBookmarkSelected(String uri, String prettyName) {
                vlcPath.cd(uri, prettyName);
                triggerCurrentPathListUpdate();
            }
        });
    }

    private void deleteBookmark() {
        displayBookmarkPicker(R.string.R_string_dir_listing_delete_bookmark_title, new BookmarkCallback() {
            @Override
            public void onBookmarkSelected(String uri, String prettyName) {
                vlcPath.deleteBookmark(uri);
            }
        });
    }

    private void displayBookmarkPicker(int titleStringId, final BookmarkCallback cb) {
        List<String> bookmarks = vlcPath.getBookmarks();

        final List<String> pathDisplayNames = new ArrayList<>();
        final List<String> pathUris = new ArrayList<>();
        for (String bookmark : bookmarks) {
            pathUris.add(bookmark);
            pathDisplayNames.add(bookmark);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(requireActivity().getString(titleStringId));
        builder.setItems(pathDisplayNames.toArray(new String[0]), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final String uri = pathUris.get(which);
                final String prettyName = pathDisplayNames.get(which);
                cb.onBookmarkSelected(uri, prettyName);
            }
        });

        builder.show();
    }


    /* List view stuff                                          */
    /************************************************************/
    private static class DirListEntry_ViewAdapter extends ArrayAdapter<Cmd_DirList.DirListEntry> {
        private static final int layoutResourceId = R.layout.fragment_dir_listing_list_element;

        final private LayoutInflater inflater;
        final private View.OnClickListener onClickCallback;
        final private View.OnLongClickListener onLongClickCallback;

        DirListEntry_ViewAdapter(View.OnClickListener onClickCallback,
                                 View.OnLongClickListener onLongClickCallback, Context context) {
            super(context, layoutResourceId, new ArrayList<Cmd_DirList.DirListEntry>());
            this.inflater = ((Activity) context).getLayoutInflater();
            this.onClickCallback = onClickCallback;
            this.onLongClickCallback = onLongClickCallback;
        }

        static class Row {
            Cmd_DirList.DirListEntry values;
            ImageView dirOrFile;
            TextView fName;
            ImageView alreadySeen;
            ImageButton actionButton;
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            final View row;
            if (convertView == null) {
                row = inflater.inflate(layoutResourceId, parent, false);
            } else {
                row = convertView;
            }

            Row holder = new Row();
            holder.values = this.getItem(position);

            holder.dirOrFile = row.findViewById(R.id.wDirListElement_DirOrFile);
            if (!holder.values.isDirectory) {
                holder.dirOrFile.setVisibility(View.INVISIBLE);
            } else {
                holder.dirOrFile.setVisibility(View.VISIBLE);
            }

            holder.fName = row.findViewById(R.id.wDirListElement_Name);
            holder.fName.setText(holder.values.name);
            holder.fName.setTag(holder.values);
            holder.fName.setOnClickListener(onClickCallback);
            holder.fName.setOnLongClickListener(onLongClickCallback);
            holder.fName.setLongClickable(true);

            holder.alreadySeen = row.findViewById(R.id.wDirListElement_AlreadySeen);
            holder.alreadySeen.setTag(holder.values);
            holder.alreadySeen.setOnClickListener(onClickCallback);
            holder.alreadySeen.setOnLongClickListener(onLongClickCallback);
            holder.alreadySeen.setLongClickable(true);
            if (holder.values.wasPlayedBefore) {
                holder.alreadySeen.setVisibility(View.VISIBLE);
            } else {
                holder.alreadySeen.setVisibility(View.INVISIBLE);
            }

            holder.actionButton = row.findViewById(R.id.wDirListElement_Action);
            holder.actionButton.setTag(holder.values);
            holder.actionButton.setOnClickListener(onClickCallback);
            holder.actionButton.setOnLongClickListener(onLongClickCallback);
            holder.actionButton.setLongClickable(true);

            row.setTag(holder);

            return row;
        }
    }
}
