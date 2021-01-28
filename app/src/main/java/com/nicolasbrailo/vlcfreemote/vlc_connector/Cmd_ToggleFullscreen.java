package com.nicolasbrailo.vlcfreemote.vlc_connector;

import com.nicolasbrailo.vlcfreemote.model.VlcStatus;

public class Cmd_ToggleFullscreen extends VlcCommand_ReturnsVlcStatus {

    public Cmd_ToggleFullscreen(VlcStatus.ObserverRegister cb) {
        super(cb);
    }

    @Override
    public String getCommandPath() {
        return "requests/status.xml?command=fullscreen";
    }

    @Override
    public Priority getPriority() { return Priority.CanDelay; }
}
