package com.nicolasbrailo.vlcfreemote.vlc_connector;

import com.nicolasbrailo.vlcfreemote.model.VlcStatus;

public class Cmd_UpdateStatus extends VlcCommand_ReturnsVlcStatus {

    public Cmd_UpdateStatus(VlcStatus.ObserverRegister cb) {
        super(cb);
    }

    @Override
    public String getCommandPath() {
        return "requests/status.xml";
    }

    @Override
    public Priority getPriority() { return Priority.CanIgnore; }
}
