package com.nicolasbrailo.vlcfreemote.vlc_connector;

import com.nicolasbrailo.vlcfreemote.model.VlcStatus;

public class Cmd_JumpToPositionPercent extends VlcCommand_ReturnsVlcStatus {
    private static final String URL_ENCODED_PERCENT = "%25";
    private final String pos;

    public Cmd_JumpToPositionPercent(float percent, VlcStatus.ObserverRegister cb) {
        super(cb);

        if ((percent > 100) || (percent < 0)) {
            throw new IllegalArgumentException("Position must be between 0% and 100%");
        }

        this.pos = String.valueOf(percent) + URL_ENCODED_PERCENT;
    }

    @Override
    public String getCommandPath() { return "requests/status.xml?command=seek&val=" + pos; }

    @Override
    public Priority getPriority() { return Priority.CanIgnore; }
}
