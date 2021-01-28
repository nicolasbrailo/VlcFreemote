package com.nicolasbrailo.vlcfreemote.vlc_connector;

import com.nicolasbrailo.vlcfreemote.model.VlcStatus;

public class Cmd_TogglePlay extends VlcCommand_ReturnsVlcStatus {

    public Cmd_TogglePlay(VlcStatus.ObserverRegister cb) {
        super(cb);
    }

    @Override
    public String getCommandPath() {
        return "requests/status.xml?command=pl_pause";
    }

    @Override
    public Priority getPriority() { return Priority.CanDelay; }
}
