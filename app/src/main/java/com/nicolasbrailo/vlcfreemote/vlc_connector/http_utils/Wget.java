package com.nicolasbrailo.vlcfreemote.vlc_connector.http_utils;

import android.os.AsyncTask;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Similar to running wget $url, Wget will run a background task to retrieve
 * an http resource and invoke a callback once the contents are available.
 */
public class Wget extends AsyncTask<String, Void, String> {

    public interface Callback {
        void onConnectionError(final String message);
        void onResponse(final String result);
        void onAuthFailure();
        void onHttpNotOkResponse();
    }

    /**
     * A callback for an observer that's only interested in the task's status and not its result
     */
    public interface CallbackWhenTaskFinished {
        void onTaskFinished();
    }

    private static final int CONN_TIMEOUT_MS = 10000;
    private static final int HTTP_RESPONSE_UNAUTHORIZED = 401;
    private static final int HTTP_RESPONSE_OK = 200;

    private final String auth;
    private final Callback callback;
    private final CallbackWhenTaskFinished cb2;
    private Exception request_exception;
    private int httpRetCode;

    public Wget(final String url, final String auth, Callback callback, CallbackWhenTaskFinished cb2) {
        this.auth = auth;
        this.callback = callback;
        this.cb2 = cb2;
        this.request_exception = null;
        this.execute(url);
    }

    private HttpURLConnection getConnection(final String url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) (new URL(url)).openConnection();
        conn.setReadTimeout(CONN_TIMEOUT_MS);
        conn.setConnectTimeout(CONN_TIMEOUT_MS);
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", auth);
        conn.setDoInput(true);
        conn.connect();
        return conn;
    }

    private String readAll(HttpURLConnection conn) throws IOException {
        InputStream is = null;

        try {
            is = conn.getInputStream();
            java.util.Scanner s = new java.util.Scanner(is, "UTF-8").useDelimiter("\\A");
            return s.hasNext() ? s.next() : "";
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    // Not much we can do
                }
            }
        }
    }

    @Override
    protected String doInBackground(String... urls) {
        try {
            HttpURLConnection conn = getConnection(urls[0]);
            httpRetCode = conn.getResponseCode();
            return readAll(conn);

        } catch (IOException e) {
            request_exception = e;
            return null;
        }
    }

    @Override
    protected void onPostExecute(String result) {
        // Let an observer know they're free to schedule new tasks
        cb2.onTaskFinished();

        if (request_exception != null) {
            callback.onConnectionError(request_exception.getMessage());
        } else if (httpRetCode == HTTP_RESPONSE_UNAUTHORIZED) {
            callback.onAuthFailure();
        } else if (httpRetCode == HTTP_RESPONSE_OK) {
            callback.onResponse(result);
        } else {
            callback.onHttpNotOkResponse();
        }
    }
}
