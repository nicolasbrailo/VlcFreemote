package com.nicolasbrailo.vlcfreemote.vlc_connector;

import com.nicolasbrailo.vlcfreemote.vlc_connector.http_utils.Wget;
import com.nicolasbrailo.vlcfreemote.vlc_connector.http_utils.XmlListReader;
import com.nicolasbrailo.vlcfreemote.vlc_connector.http_utils.XmlMogrifier;

import java.util.List;

public class Cmd_GetPlaylist implements VlcCommand {

    public static class PlaylistEntry {
        public String name;
        public String uri;
        public Integer id;
        public Integer duration;
    }

    public interface Callback {
        void onContentAvailable(List<PlaylistEntry> results);
        void onContentError();
    }

    private final Callback cb;

    public Cmd_GetPlaylist(Callback cb) {
        this.cb = cb;
    }

    @Override
    public String getCommandPath() {
        return "requests/playlist.xml";
    }

    @Override
    public Priority getPriority() { return Priority.CanDelay; }

    @Override
    public Wget.Callback getWgetCallback(final VlcCommand.GeneralCallback generalCallback) {
        return new Wget.Callback() {
            @Override
            public void onConnectionError(final String msg) { generalCallback.onConnectionError(msg); }

            @Override
            public void onAuthFailure() {
                generalCallback.onAuthError();
            }

            @Override
            public void onHttpNotOkResponse() {
                cb.onContentError();
            }

            @Override
            public void onResponse(String result) {
                XmlMogrifier.XmlKeyValReader<PlaylistEntry> keyValReader;
                keyValReader = new XmlMogrifier.XmlKeyValReader<PlaylistEntry>(PlaylistEntry.class) {
                    @Override
                    protected void parseValue(PlaylistEntry object, String key, String value) {
                        switch (key) {
                            case "uri": object.uri = value; break;
                            case "name": object.name = value; break;
                            case "id": object.id = Integer.parseInt(value); break;
                            case "duration": object.duration = Integer.parseInt(value); break;
                        }
                    }
                };

                new XmlListReader<>(result, "leaf", keyValReader, new XmlMogrifier.Callback<PlaylistEntry>() {
                    @Override
                    public void onXmlSystemError(Exception e) {
                        generalCallback.onSystemError(e);
                    }

                    @Override
                    public void onXmlDecodingError() {
                        cb.onContentError();
                    }

                    @Override
                    public void onResult(List<PlaylistEntry> results) {
                        cb.onContentAvailable(results);
                    }
                });
            }
        };
    }
}
