package com.nicolasbrailo.vlcfreemote.vlc_connector;

import com.nicolasbrailo.vlcfreemote.model.VlcStatus;

public class Cmd_CycleAudioTrack extends VlcCommand_ReturnsVlcStatus {

    public Cmd_CycleAudioTrack(VlcStatus.ObserverRegister cb) {
        super(cb);
    }

    @Override
    public String getCommandPath() { return "requests/status.xml?command=key&val=audio-track"; }

    @Override
    public Priority getPriority() { return Priority.CanDelay; }
}
