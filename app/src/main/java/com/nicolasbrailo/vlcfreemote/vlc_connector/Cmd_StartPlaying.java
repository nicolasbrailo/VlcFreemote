package com.nicolasbrailo.vlcfreemote.vlc_connector;

import com.nicolasbrailo.vlcfreemote.model.VlcStatus;

public class Cmd_StartPlaying extends VlcCommand_ReturnsVlcStatus {
    private final String id;

    public Cmd_StartPlaying(Integer id, VlcStatus.ObserverRegister cb) {
        super(cb);
        this.id = id.toString();
    }

    @Override
    public String getCommandPath() {
        return "requests/status.xml?command=pl_play&id=" + id;
    }

    @Override
    public Priority getPriority() { return Priority.MustExecute; }
}
