package com.nicolasbrailo.vlcfreemote.vlc_connector;

import com.nicolasbrailo.vlcfreemote.model.VlcStatus;

public class Cmd_JumpRelativePercent extends VlcCommand_ReturnsVlcStatus {
    private static final String URL_ENCODED_PERCENT = "%25";
    private final String jump_value;

    public Cmd_JumpRelativePercent(float percent, VlcStatus.ObserverRegister cb) {
        super(cb);

        if ((percent > 100) || (percent < -100)) {
            throw new IllegalArgumentException("Jump percent must be between -100 and 100");
        }

        // The jump value, if relative, must include its sign - otherwise it'll be considered abs
        final char sign = (percent > 0)? '+' : '-';
        final String pct = String.format(java.util.Locale.US,"%.2f", Math.abs(percent));
        this.jump_value = sign + pct + URL_ENCODED_PERCENT;
    }

    @Override
    public String getCommandPath() {
        return "requests/status.xml?command=seek&val=" + jump_value;
    }

    @Override
    public Priority getPriority() { return Priority.CanIgnore; }
}
