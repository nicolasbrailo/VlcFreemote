package com.nicolasbrailo.vlcfreemote.vlc_connector;

import com.nicolasbrailo.vlcfreemote.model.VlcStatus;

public class Cmd_CycleSubtitle extends VlcCommand_ReturnsVlcStatus {

    public Cmd_CycleSubtitle(VlcStatus.ObserverRegister cb) {
        super(cb);
    }

    @Override
    public String getCommandPath() { return "requests/status.xml?command=key&val=subtitle-track"; }

    @Override
    public Priority getPriority() { return Priority.CanDelay; }
}
