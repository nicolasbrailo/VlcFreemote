package com.nicolasbrailo.vlcfreemote.vlc_connector.http_utils;

import android.os.AsyncTask;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.StringReader;
import java.util.Arrays;
import java.util.List;


/**
 * Transform an XML message into a list of objects of type T. Will work on a background thread, then
 * invoke a callback once the result has been processed.
 *
 * @param <T> Expected type
 */
public abstract class XmlMogrifier<T> extends AsyncTask<String, Void, List<T>> {

    /**
     * Callback to be invoked once the XML message has been processed.
     * @param <X> I hope this won't compile if X != T
     */
    public interface Callback<X> {
        void onXmlSystemError(Exception e);
        void onXmlDecodingError();
        void onResult(List<X> results);
    }

    /**
     * Transforms a set of key=>values to an object of type T
     * @param <X> I hope this won't compile if X != T
     */
    public static abstract class XmlKeyValReader<X> {
        private final Class<X> clazz;
        X object;

        public XmlKeyValReader(Class<X> clazz) {
            this.clazz = clazz;
        }

        public void reset() throws IllegalAccessException, InstantiationException {
            object = clazz.newInstance();
        }

        public X getParsedObject() { return object; }

        protected abstract void parseValue(X object, final String key, final String value);
    }

    /**
     * Convert $src into a [list] of T's by using $xmlKeyValReader to map its values.
     * @param src Message to be parsed
     * @param interestingTag Tag under which interesting objects will be found
     * @param xmlKeyValReader A key=>value converter. @see XmlKeyValReader
     * @param callback Callback to invoke after processing.
     */
    XmlMogrifier(final String src, final String interestingTag,
                 final XmlKeyValReader<T> xmlKeyValReader, final Callback<T> callback) {

        if (xmlParserFactory == null) try {
            xmlParserFactory = XmlPullParserFactory.newInstance();
        } catch (XmlPullParserException e) {
            callback.onXmlSystemError(e);
        }

        this.callback = callback;
        this.xmlKeyValReader = xmlKeyValReader;
        this.interestingTag = interestingTag;
        this.execute(src);
    }

    @Override
    protected List<T> doInBackground(String... strings) {
        final String src = strings[0];
        try {
            final XmlPullParser xpp = createXmlParserFor(src);
            return xmlParseImpl(xpp, xmlKeyValReader, this.interestingTag);
        } catch (CantCreateXmlParser|CantParseXmlResponse|InstantiationException|IllegalAccessException e) {
            this.request_exception = e;
            return null;
        }
    }

    @Override
    protected void onPostExecute(List<T> result) {
        if (this.request_exception != null) {
            final Class[] sys_errs = {CantCreateXmlParser.class, InstantiationException.class,
                                      IllegalAccessException.class};
            if (Arrays.asList(sys_errs).contains(request_exception.getClass())) {
                callback.onXmlSystemError(request_exception);
                return;
            }

            callback.onXmlDecodingError();
            return;
        }

        callback.onResult(result);
    }

    private static class CantCreateXmlParser extends Exception {
        @Override
        public String getMessage() { return "Can't create an XML parser"; }
    }

    static class CantParseXmlResponse extends Exception {
        @Override
        public String getMessage() { return "The XML response was invalid"; }
    }

    private static XmlPullParser createXmlParserFor(final String msg) throws CantCreateXmlParser {
        final XmlPullParser xpp;
        try {
            xpp = xmlParserFactory.newPullParser();
            if (xpp == null) throw new CantCreateXmlParser();

            xpp.setInput( new StringReader(msg) );
            return xpp;

        } catch (XmlPullParserException e) {
            throw new CantCreateXmlParser();
        }
    }

    protected abstract List<T> xmlParseImpl(final XmlPullParser xpp, final XmlKeyValReader<T> xmlKeyValReader,
                                            final String interestingTag)
                throws CantParseXmlResponse, IllegalAccessException, InstantiationException;

    private static XmlPullParserFactory xmlParserFactory = null;
    private final Callback<T> callback;
    private final XmlKeyValReader<T> xmlKeyValReader;
    private final String interestingTag;
    private Exception request_exception = null;
}


