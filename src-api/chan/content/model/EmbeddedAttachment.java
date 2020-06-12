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

package chan.content.model;

import android.net.Uri;

import chan.annotation.Public;
import chan.util.CommonUtils;
import chan.util.StringUtils;

import com.mishiranu.dashchan.content.net.EmbeddedManager;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;

@Public
public final class EmbeddedAttachment implements Attachment {
	private static final long serialVersionUID = 1L;

	@Public
	public enum ContentType {
		@Public AUDIO,
		@Public VIDEO
	}

	private final String mFileUriString;
	private final String mThumbnailUriString;
	private final String mEmbeddedType;
	private final ContentType mContentType;
	private final boolean mCanDownload;
	private final String mForcedName;

	private String mTitle;

	@Public
	public EmbeddedAttachment(Uri fileUri, Uri thumbnailUri, String embeddedType, ContentType contentType,
							  boolean canDownload, String forcedName, String embeddedCode) {
		if (fileUri == null) {
			throw new IllegalArgumentException("fileUri is null");
		}
		if (embeddedType == null) {
			throw new IllegalArgumentException("embeddedType is null");
		}
		if (contentType == null) {
			throw new IllegalArgumentException("contentType is null");
		}
		mFileUriString = fileUri != null ? fileUri.toString() : null;
		//mThumbnailUriString = thumbnailUri != null ? thumbnailUri.toString() : null;
		mEmbeddedType = embeddedType;
		mContentType = contentType;
		mCanDownload = canDownload;
		mForcedName = forcedName;
		String title = "";
		String urlPrefix = "";
		String thumbnailUriString = null;
		boolean isVimeo = "vimeo".equalsIgnoreCase(embeddedType.toLowerCase());
		boolean isYoutube = "youtube".equalsIgnoreCase(embeddedType.toLowerCase());
		boolean isSoundCloud = "soundcloud".equalsIgnoreCase(embeddedType.toLowerCase());
		if (isVimeo) {
			urlPrefix = "https://noembed.com/embed?url=https://player.vimeo.com/video/";
		} else if(isYoutube){
			urlPrefix = "https://noembed.com/embed?url=https://www.youtube.com/watch?v=";
		} else if(isSoundCloud) {
			urlPrefix = "https://noembed.com/embed?url=https://soundcloud.com/";
		}
		if (!StringUtils.isEmpty(urlPrefix)) {
			JSONObject noembedResponse = null;
			if(!StringUtils.isEmpty(embeddedCode)) {
				String videoId = embeddedCode;
				/*
					This could cause performance issues when a big
					number of attachments is present, meaning several requests need to be made.
					Should be replaced with the standard app approach of sending requests.
				*/
				HttpURLConnection urlConnection = null;
				try {
					URL url = new URL(urlPrefix + videoId);
					urlConnection = (HttpURLConnection) url.openConnection();
					InputStream in = new BufferedInputStream(urlConnection.getInputStream());
					String contentAsString = convertInputStreamToString(in);
					if (!StringUtils.isEmpty(contentAsString)) {
						noembedResponse = new JSONObject(contentAsString);
					}
				} catch (Exception e) {
					// Ignore exception
				} finally {
					urlConnection.disconnect();
				}
				if (noembedResponse != null) {
					title = CommonUtils.optJsonString(noembedResponse, "title");
					if(!StringUtils.isEmpty(title)){
						mTitle = title;
						if(isVimeo || isSoundCloud){
							thumbnailUriString = CommonUtils.optJsonString(noembedResponse, "thumbnail_url");
							if("".equals(thumbnailUriString)){
								thumbnailUriString = null;
							}
						}
					}
				}
			}
		}
		mThumbnailUriString = thumbnailUri != null ? thumbnailUri.toString() : thumbnailUriString;
	}

	@Public
	public EmbeddedAttachment(Uri fileUri, Uri thumbnailUri, String embeddedType, ContentType contentType,
			boolean canDownload, String forcedName) {
		if (fileUri == null) {
			throw new IllegalArgumentException("fileUri is null");
		}
		if (embeddedType == null) {
			throw new IllegalArgumentException("embeddedType is null");
		}
		if (contentType == null) {
			throw new IllegalArgumentException("contentType is null");
		}
		mFileUriString = fileUri != null ? fileUri.toString() : null;
		mThumbnailUriString = thumbnailUri != null ? thumbnailUri.toString() : null;
		mEmbeddedType = embeddedType;
		mContentType = contentType;
		mCanDownload = canDownload;
		mForcedName = forcedName;
	}

	@Public
	public Uri getFileUri() {
		return mFileUriString != null ? Uri.parse(mFileUriString) : null;
	}

	@Public
	public Uri getThumbnailUri() {
		return mThumbnailUriString != null ? Uri.parse(mThumbnailUriString) : null;
	}

	@Public
	public String getEmbeddedType() {
		return mEmbeddedType;
	}

	@Public
	public ContentType getContentType() {
		return mContentType;
	}

	@Public
	public boolean isCanDownload() {
		return mCanDownload;
	}

	@Public
	public String getForcedName() {
		return mForcedName;
	}

	public String getNormalizedForcedName() {
		String forcedName = getForcedName();
		if (forcedName != null) {
			return StringUtils.escapeFile(forcedName, false);
		}
		return null;
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
				// Ignore exception
			}
		}
		return sb.toString();
	}

	public String getTitle() {
		return mTitle;
	}

	public EmbeddedAttachment setTitle(String title) {
		mTitle = title;
		return this;
	}

	@Public
	public static EmbeddedAttachment obtain(String data) {
		return EmbeddedManager.getInstance().obtainAttachment(data);
	}

	public boolean contentEquals(EmbeddedAttachment o) {
		return StringUtils.equals(mFileUriString, o.mFileUriString) &&
				StringUtils.equals(mThumbnailUriString, o.mThumbnailUriString) &&
				StringUtils.equals(mEmbeddedType, o.mEmbeddedType) &&
				mContentType == o.mContentType &&
				mCanDownload == o.mCanDownload &&
				StringUtils.equals(mForcedName, o.mForcedName);
	}
}