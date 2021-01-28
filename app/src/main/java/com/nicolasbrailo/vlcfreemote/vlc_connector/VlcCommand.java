package com.nicolasbrailo.vlcfreemote.vlc_connector;

import com.nicolasbrailo.vlcfreemote.vlc_connector.http_utils.Wget;

/**
 * Encapsulates a Vlc command.
 */
public interface VlcCommand {
    /**
     * A callback to invoke on generic failure conditions (ie connection fail, system fail, etc)
     * The idea is to write command-specific callbacks on each command implementation and have an
     * object with this interface handle generic cases which are not meaningful for any specific
     * command (eg there's no meaningful answer from a DirList command to an OOM error)
     */
    interface GeneralCallback {
        /**
         * Invoked when VLC responds to an http request with a 401 status code (unauthorized)
         */
        void onAuthError();

        /**
         * Invoked whenever the connection to a remote vlc server failed for whatever reason.
         */
        void onConnectionError(final String msg);

        /**
         * Invoked when a command couldn't be completed due to a system error (eg failed to
         * load a dyn library, error on the sql query to create a schema... stuff that means
         * system error or application bug). Usually the reasonable thing to do is log en error
         * and crash, or at least disconnect.
         * @param e: Found exception (may be null if not applicable)
         */
        void onSystemError(Exception e);
    }

    /**
     * Should return a URI for a Vlc http command, with no server. (EG: requests/foo.xml)
     * @return URI
     */
    String getCommandPath();

    /**
     * Constructs a callback to handle the output of a Vlc command
     * @param generalCallback A callback for events that don't pertain this command.
     * @return A callback that wraps the general one and dispatches events as needed
     */
    Wget.Callback getWgetCallback(final GeneralCallback generalCallback);

    /**
     * Priority levels for commands
     */
    enum Priority {
        /**
         * Command must be executed on the next available opportunity
         */
        MustExecute(2),
        /**
         * Command can be delayed until no high-priority commands are waiting
         */
        CanDelay(1),
        /**
         * Command can be dropped or used to replace previous instances of other commands
         * in this category (ie the command queue may hold a single CanIgnore command, the latest)
         */
        CanIgnore(0);

        private final int value;
        Priority(int value) {
            this.value = value;
        }

        public int getValue() { return value; }
    }

    /**
     * Some commands must be executed (eg Play) while others can be delayed (eg getPlaylist)
     * Other commands can just be dropped (eg setVolume) - typically these would be commands
     * originating from a non-discrete controller such as a slide bar. These controllers will
     * spam the queue with lots of events, so holding only the latest is OK.
     * @return The priority level for this command
     */
    Priority getPriority();
}
