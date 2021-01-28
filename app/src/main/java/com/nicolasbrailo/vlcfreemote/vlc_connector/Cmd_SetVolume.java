package com.nicolasbrailo.vlcfreemote.vlc_connector;

import com.nicolasbrailo.vlcfreemote.model.VlcStatus;

public class Cmd_SetVolume extends VlcCommand_ReturnsVlcStatus {
    private final String vol_value;

    public Cmd_SetVolume(int percent, VlcStatus.ObserverRegister cb) {
        super(cb);

        if ((percent > 100) || (percent < 0)) {
            throw new IllegalArgumentException("Volume must be between 0% and 100%");
        }

        this.vol_value = String.valueOf(VlcStatus.normalizeVolumeFrom0_100ToVlc(percent));
    }

    @Override
    public String getCommandPath() {
        return "requests/status.xml?command=volume&val=" + vol_value;
    }

    @Override
    public Priority getPriority() { return Priority.CanIgnore; }
}
