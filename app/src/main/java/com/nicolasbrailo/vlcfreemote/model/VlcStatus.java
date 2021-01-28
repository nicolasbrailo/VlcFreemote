package com.nicolasbrailo.vlcfreemote.model;

import android.content.res.Resources;

import com.nicolasbrailo.vlcfreemote.R;

public class VlcStatus {
    public interface ObserverRegister {
        Observer getVlcStatusObserver();
    }

    public interface Observer {
        void onVlcStatusUpdate(VlcStatus results);
        void onVlcStatusFetchError();
        void onVlcStatusFetchError(String msg);
    }

    public int length;
    public float position; // Progress % of the current file
    public int volume;
    public int time;
    public float rate;
    public float audiodelay;
    public float subtitledelay;
    public boolean repeat;
    public boolean loop;
    public boolean random;
    public boolean fullscreen;
    public String state;
    public String currentMedia_filename;
    public String currentMedia_album;
    public String currentMedia_title;
    public String currentMedia_artist;
    public int currentMedia_trackNumber;
    public int currentMedia_tracksTotal;

    public static final String HUMAN_READABLE_STATE_PAUSED = "Paused";
    public static final String HUMAN_READABLE_STATE_PLAYING = "Playing";
    public static final String HUMAN_READABLE_STATE_STOPPED = "Stopped";
    public static final String HUMAN_READABLE_STATE_UNKNOW = "?";

    public VlcStatus() { state = ""; }

    public boolean isStopped() { return state.equals("stopped"); }

    public boolean isPlaying() { return state.equals("playing"); }

    public String getHumanReadableState() {
        switch (state) {
            case "paused":  return HUMAN_READABLE_STATE_PAUSED;
            case "playing": return HUMAN_READABLE_STATE_PLAYING;
            case "stopped": return HUMAN_READABLE_STATE_STOPPED;
            default:        return HUMAN_READABLE_STATE_UNKNOW;
        }
    }

    public String getCurrentPlayingFile(Resources resources) {
        if (currentMedia_title != null) return currentMedia_title;
        if (currentMedia_filename != null) return currentMedia_filename;
        return String.format(resources.getString(R.string.playing_track_status),
                                  currentMedia_trackNumber, currentMedia_tracksTotal);
    }

    // Vlc's volume seems to go from 0 to 400 (?)
    static public int normalizeVolumeFrom0_100ToVlc(int vlcVolume) { return 4*vlcVolume; }
    static public int normalizeVolumeFromVlcTo0_100(int vlcVolume) { return vlcVolume/4; }

    static public float normalizePosFromVlcTo0_100(float pos) { return 100*pos; }
}
