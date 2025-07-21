/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.samples.comparison.urlsfetcher;

import android.os.AsyncTask;

import com.facebook.common.internal.Objects;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.logging.FLog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.XMLReader;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nullable;

/**
 * Helper that asynchronously fetches the list of image URIs from Imgur.
 */
public class ImageUrlsFetcher {

    /**
     * Imgur license key for use by the Fresco project.
     *
     * <p>The rest of this class may be used freely according to the licence file, with the sole
     * exception of this variable. Any fork of this code or use in any other application, whether
     * open- or closed-source, must use a different client ID obtained from Imgur. See the <a
     * href="https://api.imgur.com/#register">Imgur API documentation</a>.
     */
    private static final String IMGUR_CLIENT_ID = "Client-ID ccc6ca6a65ecdd8";

    private static final String TAG = "FrescoSample";

    public interface Callback {
        public void onFinish(List<String> results);
    }

    public static void getImageUrls(final ImageUrlsRequest request, final Callback callback) {
        new AsyncTask<Void, Void, List<String>>() {
            @Override
            protected List<String> doInBackground(Void... params) {
                return getImageUrls3(request);
            }

            @Override
            protected void onPostExecute(List<String> result) {
                callback.onFinish(result);
            }
        }.execute();
    }

    private static List<String> getImageUrls(ImageUrlsRequest request) {
        List<String> urls = new ArrayList<String>();
        try {
            String rawJson = downloadContentAsString(request.getEndpointUrl());
            if (rawJson == null) {
                return urls;
            }
            JSONObject json = new JSONObject(rawJson);
            JSONArray data = json.getJSONArray("data");
            for (int i = 0; i < data.length(); i++) {
                JSONObject item = data.getJSONObject(i);
                if (!item.has("type")) {
                    continue;
                }
                ImageFormat imageFormat = ImageFormat.getImageFormatForMime(item.getString("type"));
                ImageSize imageSize = request.getImageSize(imageFormat);
                if (imageSize != null) {
                    urls.add(getThumbnailLink(item, imageSize));
                }
            }
        } catch (Exception e) {
            FLog.e(TAG, "Exception fetching album", e);
        }
        return urls;
    }

    private static List<String> getImageUrls2(ImageUrlsRequest request) {
        return Arrays.asList(
                "https://img.soogif.com/6pPpYoit59bkYw3PR28M3dKNYk676lhH.gif",
                "https://img.soogif.com/7yQ4P6Eb8YZERHFyd54jxLNVd0mIl9Ob.gif",
                "https://img.soogif.com/FtUjxBFoWNK3QA1FliFctvsuYUlvftov.gif"
        );
    }

    private static List<String> getImageUrls3(ImageUrlsRequest request) {
        List<String> urls = new ArrayList<String>();
        try {
            String rawJson = downloadContentAsString3("https://www.soogif.com/funny");
            if (rawJson == null) {
                return urls;
            }
            XmlPullParserFactory xmlPullParserFactory = XmlPullParserFactory.newInstance();
            XmlPullParser xmlPullParser = xmlPullParserFactory.newPullParser();
            xmlPullParser.setInput(new StringReader(rawJson));
            int eventType = xmlPullParser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if(eventType == XmlPullParser.START_TAG){
                    String linkUrl = xmlPullParser.getAttributeValue(null, "data-link");
                    if(linkUrl != null) {
                        linkUrl = linkUrl.replace("http", "https");
                        urls.add(linkUrl);
                    }
                }
                eventType = xmlPullParser.next();
            }
        } catch (Exception e) {
            FLog.e(TAG, "Exception fetching urls", e);
        }

        return urls;
    }

    @Nullable
    private static String downloadContentAsString(String urlString) throws IOException {
        InputStream is = null;
        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("Authorization", IMGUR_CLIENT_ID);
            conn.setReadTimeout(10000 /* milliseconds */);
            conn.setConnectTimeout(15000 /* milliseconds */);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            // Starts the query
            conn.connect();
            int response = conn.getResponseCode();
            if (response != HttpURLConnection.HTTP_OK) {
                FLog.e(TAG, "Album request returned %s", response);
                return null;
            }
            is = conn.getInputStream();
            return readAsString(is);
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    @Nullable
    private static String downloadContentAsString3(String urlString) throws IOException {
        return Content.rawXml;
    }

    /**
     * Reads an InputStream and converts it to a String.
     */
    private static String readAsString(InputStream stream) throws IOException {
        StringWriter writer = new StringWriter();
        Reader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
        while (true) {
            int c = reader.read();
            if (c < 0) {
                break;
            }
            writer.write(c);
        }
        return writer.toString();
    }

    private static String getThumbnailLink(final JSONObject json, final ImageSize imageSize)
            throws JSONException {
        Preconditions.checkNotNull(imageSize);
        final String originalUrl = json.getString("link");
        if (imageSize == ImageSize.ORIGINAL_IMAGE) {
            return originalUrl;
        }

        final int dotPos = originalUrl.lastIndexOf('.');
        final StringBuilder linkBuilder = new StringBuilder(originalUrl.length() + 1);
        return linkBuilder.append(originalUrl).insert(dotPos, imageSize.suffix).toString();
    }
}
