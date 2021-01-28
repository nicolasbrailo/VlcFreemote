package com.nicolasbrailo.vlcfreemote;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.nicolasbrailo.vlcfreemote.vlc_connector.Cmd_CycleAudioTrack;
import com.nicolasbrailo.vlcfreemote.vlc_connector.Cmd_CycleSubtitle;
import com.nicolasbrailo.vlcfreemote.vlc_connector.Cmd_JumpRelativePercent;
import com.nicolasbrailo.vlcfreemote.vlc_connector.Cmd_JumpToPositionPercent;
import com.nicolasbrailo.vlcfreemote.vlc_connector.Cmd_Next;
import com.nicolasbrailo.vlcfreemote.vlc_connector.Cmd_Prev;
import com.nicolasbrailo.vlcfreemote.vlc_connector.Cmd_SetVolume;
import com.nicolasbrailo.vlcfreemote.vlc_connector.Cmd_ToggleFullscreen;
import com.nicolasbrailo.vlcfreemote.vlc_connector.Cmd_TogglePlay;
import com.nicolasbrailo.vlcfreemote.model.VlcStatus;

import java.util.Locale;

import static com.nicolasbrailo.vlcfreemote.model.VlcStatus.HUMAN_READABLE_STATE_PLAYING;

public class PlayerControllerView extends VlcFragment
                                  implements View.OnClickListener,
                                             SeekBar.OnSeekBarChangeListener  {
    private Activity activity;

    /* Android stuff                                            */
    /************************************************************/
    public PlayerControllerView() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_player_controller_view, container, false);

        v.findViewById(R.id.wPlayer_ToggleMoreOptions).setOnClickListener(this);
        v.findViewById(R.id.wPlayer_PlayPosition_JumpBack).setOnClickListener(this);
        v.findViewById(R.id.wPlayer_PlayPosition).setOnClickListener(this);
        v.findViewById(R.id.wPlayer_PlayPosition_JumpForward).setOnClickListener(this);
        v.findViewById(R.id.wPlayer_BtnPrev).setOnClickListener(this);
        v.findViewById(R.id.wPlayer_BtnNext).setOnClickListener(this);
        v.findViewById(R.id.wPlayer_Volume).setOnClickListener(this);
        v.findViewById(R.id.wPlayer_BtnPlayPause).setOnClickListener(this);

        v.findViewById(R.id.wPlayer_AppInfoScreen).setOnClickListener(this);
        v.findViewById(R.id.wPlayer_SetTheme).setOnClickListener(this);
        v.findViewById(R.id.wPlayer_ToggleFullscreen).setOnClickListener(this);
        v.findViewById(R.id.wPlayer_CycleAudioTrack).setOnClickListener(this);
        v.findViewById(R.id.wPlayer_CycleSubtitleTrack).setOnClickListener(this);

        ((SeekBar) v.findViewById(R.id.wPlayer_Volume)).setOnSeekBarChangeListener(this);
        ((SeekBar) v.findViewById(R.id.wPlayer_PlayPosition)).setOnSeekBarChangeListener(this);

        return v;
    }

    @Override
    public void onAttach(@NonNull Context activity) {
        super.onAttach(activity);
        this.activity = (Activity) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        this.activity = null;
    }

    /* Event handlers                                           */
    /************************************************************/
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.wPlayer_ToggleMoreOptions: onToggleMoreOptionsClicked(); break;
            case R.id.wPlayer_PlayPosition_JumpBack: onPlayPosition_JumpBackClicked(); break;
            case R.id.wPlayer_PlayPosition_JumpForward: onPlayPosition_JumpForwardClicked(); break;
            case R.id.wPlayer_BtnPrev: onBtnPrevClicked(); break;
            case R.id.wPlayer_BtnNext: onBtnNextClicked(); break;
            case R.id.wPlayer_BtnPlayPause: onBtnPlayPauseClicked(); break;
            case R.id.wPlayer_AppInfoScreen: showAppInfo(); break;
            case R.id.wPlayer_SetTheme: toggleTheme(); break;
            case R.id.wPlayer_ToggleFullscreen: onToggleFullscreen(); break;
            case R.id.wPlayer_CycleAudioTrack: onCycleAudioTrack(); break;
            case R.id.wPlayer_CycleSubtitleTrack: onCycleSubtitleTrack(); break;
            default:
                throw new RuntimeException(getClass().getName() + " received an event it doesn't know how to handle.");
        }
    }

    @Override public void onStartTrackingTouch(SeekBar seekBar) {}
    @Override public void onStopTrackingTouch(SeekBar seekBar) {}
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (!fromUser) return;
        switch (seekBar.getId()) {
            case R.id.wPlayer_PlayPosition: onPlayPositionClicked(progress); break;
            case R.id.wPlayer_Volume: onVolumeClicked(progress); break;
        }
    }

    private void onToggleMoreOptionsClicked() {
        View panel = this.activity.findViewById(R.id.wPlayer_ExtraOptions);
        if (panel.getVisibility() == View.GONE) {
            panel.setVisibility(View.VISIBLE);
        } else {
            panel.setVisibility(View.GONE);
        }
    }

    /* Vlc interaction                                          */
    /************************************************************/

    private void onPlayPosition_JumpBackClicked() {
        getVlc().exec(new Cmd_JumpRelativePercent(-0.5f, getVlc()));
    }

    private void onPlayPositionClicked(int progress) {
        getVlc().exec(new Cmd_JumpToPositionPercent(progress, getVlc()));
    }

    private void onPlayPosition_JumpForwardClicked() {
        getVlc().exec(new Cmd_JumpRelativePercent(+0.5f, getVlc()));
    }

    private void onBtnPrevClicked() {
        getVlc().exec(new Cmd_Prev(getVlc()));
    }

    private void onBtnNextClicked() {
        getVlc().exec(new Cmd_Next(getVlc()));
    }

    private void onVolumeClicked(int progress) {
        getVlc().exec(new Cmd_SetVolume(progress, getVlc()));
    }

    private void onBtnPlayPauseClicked() {
        getVlc().exec(new Cmd_TogglePlay(getVlc()));
    }

    public void onStatusUpdated(final Activity activity, VlcStatus status) {
        final String currFile = status.getCurrentPlayingFile(activity.getResources());
        final String currState = status.getHumanReadableState();
        final String stateFmt = activity.getResources().getString(R.string.general_vlc_status);

        final TextView statusTxt = activity.findViewById(R.id.wPlayer_CurrentlyPlaying);
        statusTxt.setText(String.format(stateFmt, currState, currFile));

        final SeekBar volumeCtrl = activity.findViewById(R.id.wPlayer_Volume);
        volumeCtrl.setProgress(status.volume);

        final SeekBar posCtrl = activity.findViewById(R.id.wPlayer_PlayPosition);
        posCtrl.setProgress((int) (status.position));

        final String currPos = String.format(Locale.getDefault(), "%d:%02d", status.time / 60, status.time % 60);
        final TextView currentPosTxt = activity.findViewById(R.id.wPlayer_PlayPosition_CurrentPositionText);
        currentPosTxt.setText(currPos);

        final String length = String.format(Locale.getDefault(), "%d:%02d", status.length / 60, status.length % 60);
        final TextView lengthTxt = activity.findViewById(R.id.wPlayer_PlayPosition_Length);
        lengthTxt.setText(length);

        final ImageButton playPauseButton = activity.findViewById(R.id.wPlayer_BtnPlayPause);
        if (currState.equals(HUMAN_READABLE_STATE_PLAYING)) {
            playPauseButton.setImageResource(android.R.drawable.ic_media_pause);
        } else {
            playPauseButton.setImageResource(android.R.drawable.ic_media_play);
        }
    }


    private void onToggleFullscreen() {
        getVlc().exec(new Cmd_ToggleFullscreen(getVlc()));
    }

    private void onCycleSubtitleTrack() {
        getVlc().exec(new Cmd_CycleSubtitle(getVlc()));
    }

    private void onCycleAudioTrack() {
        getVlc().exec(new Cmd_CycleAudioTrack(getVlc()));
    }

    private void showAppInfo() {
        final AlertDialog.Builder alert = new AlertDialog.Builder(this.getContext());
        alert.setTitle(R.string.about_this_app_title)
                .setMessage(R.string.about_this_app_body)
                .setCancelable(true)
                .setPositiveButton(R.string.about_this_app_goto_site, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        final String url = Uri.parse("https://github.com/nicolasbrailo/VlcFreemote")
                                .buildUpon()
                                .build().toString();

                        final Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse(url));
                        startActivity(intent);
                    }
                });
        alert.create().show();
    }

    /* TODO: These actually belong in local settings, they are here simply to prototype theme
             setting (and because there's no 'official' settings view */
    private static SharedPreferences getSharedPrefs(final Context ctx) {
        return ctx.getSharedPreferences("app_startup", Context.MODE_PRIVATE);
    }

    public static boolean shouldUseDarkTheme(final Context ctx) {
        return shouldUseDarkTheme(getSharedPrefs(ctx));
    }

    public static boolean shouldUseDarkTheme(final SharedPreferences cfg) {
        return cfg.getBoolean("UseDarkTheme", false);
    }

    // Adding a theme-toggle in the main menu is somewhat hackish, but the alternative is adding
    // this in a dedicated settings tab. This should be ok for now.
    private void toggleTheme() {
        getSharedPrefs(activity).edit()
                                .putBoolean("UseDarkTheme", ! shouldUseDarkTheme(activity))
                                .apply();

        try {
            activity.setTheme(R.style.DarkTheme);

            final String name = requireContext().getPackageName();
            final Intent intent = requireContext().getPackageManager().getLaunchIntentForPackage(name);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        } catch (Exception ex) {
            final String msg = getString(R.string.status_theme_apply_fail);
            Toast toast = Toast.makeText(getContext(), msg, Toast.LENGTH_LONG);
            toast.show();
        }
    }
}
