package com.nicolasbrailo.vlcfreemote.vlc_connector.http_utils;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class XmlListReader<T> extends XmlMogrifier<T> {

    public XmlListReader(String src, String interestingTag, XmlKeyValReader<T> xmlKeyValReader, Callback<T> callback) {
        super(src, interestingTag, xmlKeyValReader, callback);
    }

    @Override
    protected List<T> xmlParseImpl(final XmlPullParser xpp, final XmlKeyValReader<T> objDeserializer,
                                   final String interestingTag)
            throws CantParseXmlResponse, IllegalAccessException, InstantiationException
    {
        List<T> foundObjects = new ArrayList<>();

        try {
            int eventType = xpp.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if(eventType == XmlPullParser.START_TAG && (xpp.getName().equals(interestingTag))) {
                    objDeserializer.reset();

                    for (int i=0; i < xpp.getAttributeCount(); ++i) {
                        objDeserializer.parseValue(objDeserializer.getParsedObject(), xpp.getAttributeName(i),
                                                    xpp.getAttributeValue(i));
                    }

                    foundObjects.add(objDeserializer.getParsedObject());
                }

                eventType = xpp.next();
            }
        } catch (XmlPullParserException | IOException e) {
            throw new CantParseXmlResponse();
        }

        return foundObjects;
    }
}
