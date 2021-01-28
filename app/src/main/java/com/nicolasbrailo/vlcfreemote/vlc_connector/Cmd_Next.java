package com.nicolasbrailo.vlcfreemote.vlc_connector;

import com.nicolasbrailo.vlcfreemote.model.VlcStatus;

public class Cmd_Next extends VlcCommand_ReturnsVlcStatus {

    public Cmd_Next(VlcStatus.ObserverRegister cb) {
        super(cb);
    }

    @Override
    public String getCommandPath() {
        return "requests/status.xml?command=pl_next";
    }

    @Override
    public Priority getPriority() { return Priority.MustExecute; }
}
