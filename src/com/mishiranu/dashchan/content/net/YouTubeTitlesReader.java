/*
 * Copyright 2014-2016 Fukurou Mishiranu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mishiranu.dashchan.content.net;

import android.net.Uri;

import com.mishiranu.dashchan.C;
import com.mishiranu.dashchan.preference.Preferences;
import com.mishiranu.dashchan.text.HtmlParser;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import chan.content.ChanLocator;
import chan.content.InvalidResponseException;
import chan.content.model.Attachment;
import chan.content.model.EmbeddedAttachment;
import chan.content.model.Post;
import chan.http.HttpException;
import chan.http.HttpHolder;
import chan.util.CommonUtils;
import chan.util.StringUtils;

public class YouTubeTitlesReader {
    private static final YouTubeTitlesReader INSTANCE = new YouTubeTitlesReader();

    private YouTubeTitlesReader() {
    }

    public static YouTubeTitlesReader getInstance() {
        return INSTANCE;
    }

    private void readYouTubeTitlesAndInfo(HashMap<String, YoutubeInfo> writeTo, ArrayList<String> embeddedCodes,
                                          int from, int count, HttpHolder holder) throws HttpException, InvalidResponseException {
        StringBuilder builder = new StringBuilder();
        for (int i = from, size = Math.min(embeddedCodes.size(), from + count); i < size; i++) {
            if (builder.length() > 0) {
                builder.append(',');
            }
            builder.append(embeddedCodes.get(i));
            JSONObject noembedResponse = null;
            if(!StringUtils.isEmpty(embeddedCodes.get(i))){
                String videoId = embeddedCodes.get(i);
				/*
					This could cause performance issues when a big
					number of attachments is present, meaning several requests need to be made.
					Should be replaced with the standard app approach of sending requests.
				*/
                HttpURLConnection urlConnection = null;
                try {
                    URL url = new URL("https://noembed.com/embed?url=https://www.youtube.com/watch?v=" + videoId);
                    urlConnection = (HttpURLConnection) url.openConnection();
                    InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                    String contentAsString = convertInputStreamToString(in);
                    if(!StringUtils.isEmpty(contentAsString)){
                        noembedResponse = new JSONObject(contentAsString);
                    }
                } catch (Exception e) {
                    // Ignore exception
                } finally {
                    urlConnection.disconnect();
                }
                if(noembedResponse != null){
                    String title = CommonUtils.optJsonString(noembedResponse, "title");
                    if(!StringUtils.isEmpty(title)){
                        writeTo.put(videoId, new YoutubeInfo(title, noembedResponse));
                    }
                }
            }
        }
    }

    private void readYouTubeTitles(HashMap<String, String> writeTo, ArrayList<String> embeddedCodes,
                                   int from, int count, HttpHolder holder) throws HttpException, InvalidResponseException {
        StringBuilder builder = new StringBuilder();
        for (int i = from, size = Math.min(embeddedCodes.size(), from + count); i < size; i++) {
            if (builder.length() > 0) {
                builder.append(',');
            }
            builder.append(embeddedCodes.get(i));
            JSONObject noembedResponse = null;
            if(!StringUtils.isEmpty(embeddedCodes.get(i))){
                String videoId = embeddedCodes.get(i);
				/*
					This could cause performance issues when a big
					number of attachments is present, meaning several requests need to be made.
					Should be replaced with the standard app approach of sending requests.
				*/
                HttpURLConnection urlConnection = null;
                try {
                    URL url = new URL("https://noembed.com/embed?url=https://www.youtube.com/watch?v=" + videoId);
                    urlConnection = (HttpURLConnection) url.openConnection();
                    InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                    String contentAsString = convertInputStreamToString(in);
                    if(!StringUtils.isEmpty(contentAsString)){
                        noembedResponse = new JSONObject(contentAsString);
                    }
                } catch (Exception e) {
                    // Ignore exception
                } finally {
                    urlConnection.disconnect();
                }
                if(noembedResponse != null){
                    String title = CommonUtils.optJsonString(noembedResponse, "title");
                    if(!StringUtils.isEmpty(title)){
                        writeTo.put(videoId, title);
                    }
                }
            }
        }
        // This api is HTTPS-only
        /*
			Youtube API call disabled as the included API KEY no longer works
		 */
        /*Uri uri = ChanLocator.getDefault().buildQueryWithSchemeHost(true, "www.googleapis.com", "youtube/v3/videos",
                "key", C.API_KEY_GOOGLE, "part", "snippet", "id", builder.toString());
        JSONObject response = new HttpRequest(uri, holder).read().getJsonObject();
        if (response != null) {
            JSONArray jsonArray = (response).optJSONArray("items");
            if (jsonArray != null) {
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject = jsonArray.optJSONObject(i);
                    if (jsonObject != null) {
                        String id = jsonObject.optString("id");
                        jsonObject = jsonObject.optJSONObject("snippet");
                        if (jsonObject != null && id != null) {
                            String title = jsonObject.optString("title");
                            if (title != null) {
                                writeTo.put(id, title);
                            }
                        }
                    }
                }
            }
        }*/
    }

    public String convertInputStreamToString(InputStream stream) throws IOException, UnsupportedEncodingException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        StringBuilder sb = new StringBuilder();
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        } catch (IOException e) {
            // Ignore exception
        } finally {
            try {
                stream.close();
            } catch (IOException e) {

            }
        }
        return sb.toString();
    }

    private final HashMap<String, String> cachedYouTubeTitles = new HashMap<>();

    private final HashMap<String, YoutubeInfo> cachedYouTubeTitlesAndInfo = new HashMap<>();

    public class YoutubeInfo {
        private String title = "";
        private JSONObject response;

        public YoutubeInfo(String title) {
            this.title = title;
        }

        public YoutubeInfo(String title, JSONObject response) {
            this.title = title;
            this.response = response;
        }

        public JSONObject getResponse() {
            return response;
        }

        public void setResponse(JSONObject response) {
            this.response = response;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

    }

    public final HashMap<String, YoutubeInfo> readYouTubeTitlesAndInfo(ArrayList<String> embeddedCodes, HttpHolder holder)
            throws HttpException, InvalidResponseException {
        HashMap<String, YoutubeInfo> result = new HashMap<>();
        for (int i = embeddedCodes.size() - 1; i >= 0; i--) {
            String id = embeddedCodes.get(i);
            YoutubeInfo info = cachedYouTubeTitlesAndInfo.get(embeddedCodes.get(i));
            if (info != null && info.getTitle() != null) {
                result.put(id, info);
                embeddedCodes.remove(i);
            }
        }
        final int maxCount = 50; // Max allowed count per request.
        for (int i = 0; i < embeddedCodes.size(); i += maxCount) {
            readYouTubeTitlesAndInfo(result, embeddedCodes, i, maxCount, holder);
        }
        if (result.size() > 0) {
            for (String id : result.keySet()) {
                cachedYouTubeTitlesAndInfo.put(id, result.get(id));
            }
            return result;
        }
        return null;
    }

    public final HashMap<String, String> readYouTubeTitles(ArrayList<String> embeddedCodes, HttpHolder holder)
            throws HttpException, InvalidResponseException {
        HashMap<String, String> result = new HashMap<>();
        for (int i = embeddedCodes.size() - 1; i >= 0; i--) {
            String id = embeddedCodes.get(i);
            String title = cachedYouTubeTitles.get(embeddedCodes.get(i));
            if (title != null) {
                result.put(id, title);
                embeddedCodes.remove(i);
            }
        }
        final int maxCount = 50; // Max allowed count per request.
        for (int i = 0; i < embeddedCodes.size(); i += maxCount) {
            readYouTubeTitles(result, embeddedCodes, i, maxCount, holder);
        }
        if (result.size() > 0) {
            for (String id : result.keySet()) {
                cachedYouTubeTitles.put(id, result.get(id));
            }
            return result;
        }
        return null;
    }

    private static abstract class EmbeddedCodeData {
        public final EmbeddedApplyHolder applyHolder;
        public final String embeddedCode;

        public EmbeddedCodeData(EmbeddedApplyHolder applyHolder, String embeddedCode) {
            this.applyHolder = applyHolder;
            this.embeddedCode = embeddedCode;
        }

        public abstract int applyTitle(String title);

        public abstract int applyTitle(String title, JSONObject response);

        public abstract void fixPositions(int shift);
    }

    private static class LinkCodeData extends EmbeddedCodeData {
        public int start, end;

        public LinkCodeData(EmbeddedApplyHolder applyHolder, String embeddedCode, int start, int end) {
            super(applyHolder, embeddedCode);
            this.start = start;
            this.end = end;
        }

        @Override
        public int applyTitle(String title) {
            title = "YouTube: " + title;
            if (applyHolder.commentBuilder == null) {
                applyHolder.commentBuilder = new StringBuilder(applyHolder.post.getComment());
            }
            int shift = title.length() - (end - start);
            applyHolder.commentBuilder.replace(start, end, title);
            return shift;
        }

        @Override
        public int applyTitle(String title, JSONObject response) {
            return applyTitle(title);
        }

        @Override
        public void fixPositions(int shift) {
            if (shift != 0) {
                start += shift;
                end += shift;
            }
        }
    }

    private static class FileCodeData extends EmbeddedCodeData {
        public final EmbeddedAttachment attachment;

        public FileCodeData(EmbeddedApplyHolder applyHolder, String embeddedCode, EmbeddedAttachment attachment) {
            super(applyHolder, embeddedCode);
            this.attachment = attachment;
        }

        @Override
        public int applyTitle(String title) {
            attachment.setTitle(title);
            return 0;
        }

        @Override
        public int applyTitle(String title, JSONObject response) {
            if(response != null){
                String channel = CommonUtils.optJsonString(response, "author_name");
                attachment.setChannel(channel);
            }
            attachment.setTitle(title);
            return 0;
        }

        @Override
        public void fixPositions(int shift) {
        }
    }

    private static class EmbeddedApplyHolder {
        public final Post post;
        public final ArrayList<EmbeddedCodeData> embeddedCodeDatas = new ArrayList<>();

        public StringBuilder commentBuilder;

        public EmbeddedApplyHolder(Post post) {
            this.post = post;
        }
    }

    private EmbeddedApplyHolder getYouTubeApplyHolder(ChanLocator locator, Post post) {
        EmbeddedApplyHolder applyHolder = null;
        String comment = post.getComment();
        if (comment != null) {
            int indexStart = -1;
            while (true) {
                indexStart = StringUtils.nearestIndexOf(comment, indexStart + 1, "<a ", "<a\n", "<a\r");
                if (indexStart >= 0) {
                    int indexStartClose = comment.indexOf(">", indexStart);
                    int indexHref = comment.indexOf("href=", indexStart);
                    if (indexStartClose > indexHref && indexHref > indexStart) {
                        indexHref += 6;
                        int indexHrefEnd = -1;
                        char c = comment.charAt(indexHref);
                        if (c == '\'' || c == '"') {
                            indexHrefEnd = comment.indexOf(c, indexHref + 1);
                        } else {
                            for (int i = indexHref + 1; i < comment.length(); i++) {
                                char t = comment.charAt(i);
                                if (t == ' ' || t == '>') {
                                    indexHrefEnd = i;
                                    break;
                                }
                            }
                        }
                        if (indexHrefEnd > indexHref) {
                            String href = comment.substring(indexHref, indexHrefEnd);
                            String embeddedCode = null;
                            if (href.contains("youtu")) {
                                href = HtmlParser.clear(href);
                                embeddedCode = locator.getYouTubeEmbeddedCode(href);
                            }
                            if (embeddedCode != null) {
                                int indexEnd = comment.indexOf("</a>", indexStartClose);
                                if (indexEnd > indexStartClose) {
                                    if (applyHolder == null) {
                                        applyHolder = new EmbeddedApplyHolder(post);
                                    }
                                    applyHolder.embeddedCodeDatas.add(new LinkCodeData(applyHolder, embeddedCode,
                                            indexStartClose + 1, indexEnd));
                                }
                            }
                        }
                    }
                } else {
                    break;
                }
            }
        }
        for (int i = 0, count = post.getAttachmentsCount(); i < count; i++) {
            Attachment attachment = post.getAttachmentAt(i);
            if (attachment instanceof EmbeddedAttachment) {
                EmbeddedAttachment embeddedAttachment = (EmbeddedAttachment) attachment;
                Uri uri = embeddedAttachment.getFileUri();
                String embeddedCode = uri != null ? locator.getYouTubeEmbeddedCode(uri.toString()) : null;
                if (embeddedCode != null) {
                    if (applyHolder == null) {
                        applyHolder = new EmbeddedApplyHolder(post);
                    }
                    applyHolder.embeddedCodeDatas.add(new FileCodeData(applyHolder, embeddedCode, embeddedAttachment));
                }
            }
        }
        return applyHolder;
    }

    private void readYouTubeTitlesAndApplyChecked(List<Post> posts, HttpHolder holder)
            throws HttpException, InvalidResponseException {
        ChanLocator locator = ChanLocator.getDefault();
        ArrayList<EmbeddedApplyHolder> applyHolders = null;
        ArrayList<String> embeddedCodes = null;
        for (Post post : posts) {
            EmbeddedApplyHolder applyHolder = getYouTubeApplyHolder(locator, post);
            if (applyHolder != null) {
                if (applyHolders == null) {
                    applyHolders = new ArrayList<>();
                    embeddedCodes = new ArrayList<>();
                }
                applyHolders.add(applyHolder);
                for (EmbeddedCodeData embeddedCodeData : applyHolder.embeddedCodeDatas) {
                    embeddedCodes.add(embeddedCodeData.embeddedCode);
                }
            }
        }
        if (embeddedCodes != null) {
            HashMap<String, YoutubeInfo> titlesAndInfo = readYouTubeTitlesAndInfo(embeddedCodes, holder);
            if (titlesAndInfo != null) {
                for (String embeddedCode : titlesAndInfo.keySet()) {
                    for (EmbeddedApplyHolder applyHolder : applyHolders) {
                        int shift = 0;
                        for (EmbeddedCodeData embeddedCodeData : applyHolder.embeddedCodeDatas) {
                            embeddedCodeData.fixPositions(shift);
                            if (embeddedCodeData.embeddedCode.equals(embeddedCode) && titlesAndInfo.get(embeddedCode) != null) {
                                    JSONObject response = titlesAndInfo.get(embeddedCode).getResponse();
                                    shift += embeddedCodeData.applyTitle(titlesAndInfo.get(embeddedCode).getTitle(), response);
                            }
                        }
                    }
                }
            }
            for (EmbeddedApplyHolder applyHolder : applyHolders) {
                if (applyHolder.commentBuilder != null) {
                    Post post = applyHolder.post;
                    post.setEditedComment(applyHolder.commentBuilder.toString());
                }
            }
        }
    }

    public final void readAndApplyIfNecessary(List<Post> posts, HttpHolder holder) {
        if (Preferences.isDownloadYouTubeTitles() && !StringUtils.isEmpty(C.API_KEY_GOOGLE)) {
            try {
                readYouTubeTitlesAndApplyChecked(posts, holder);
            } catch (HttpException | InvalidResponseException e) {
                // Ignore exception
            }
        }
    }
}
