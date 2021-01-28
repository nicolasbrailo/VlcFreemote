package com.nicolasbrailo.vlcfreemote.vlc_connector.http_utils;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class XmlObjectReader<T> extends XmlMogrifier<T> {
    public XmlObjectReader(String src, XmlKeyValReader<T> xmlKeyValReader, Callback<T> callback) {
        super(src, null, xmlKeyValReader, callback);
    }

    @Override
    protected List<T> xmlParseImpl(XmlPullParser xpp, XmlKeyValReader<T> xmlKeyValReader, String interestingTag)
            throws CantParseXmlResponse, IllegalAccessException, InstantiationException {

        xmlKeyValReader.reset();

        try {
            String currentTag = null;
            String currentMetaInfo = null;
            int eventType = xpp.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                switch (eventType) {
                    case XmlPullParser.START_TAG:
                        currentTag = xpp.getName();
                        currentMetaInfo = xpp.getAttributeValue(null, "name");
                        break;

                    case XmlPullParser.TEXT:
                        if (currentTag != null) {

                            // VLC returns a list of metadata for the current item being played with
                            // the format <info name='$META_VAR'>$VALUE>, so instead of using the
                            // name of the tag element, we need to use its first attribute
                            if (currentMetaInfo != null) {
                                xmlKeyValReader.parseValue(xmlKeyValReader.getParsedObject(), currentMetaInfo, xpp.getText());
                            } else {
                                xmlKeyValReader.parseValue(xmlKeyValReader.getParsedObject(), currentTag, xpp.getText());
                            }
                        }
                        break;

                    case XmlPullParser.END_TAG:
                        currentTag = null;
                        break;
                }

                eventType = xpp.next();
            }
        } catch (XmlPullParserException | IOException e) {
            throw new CantParseXmlResponse();
        }

        ArrayList<T> l = new ArrayList<>();
        l.add(xmlKeyValReader.getParsedObject());
        return l;
    }
}
