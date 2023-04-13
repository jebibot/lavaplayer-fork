package com.sedmelluq.discord.lavaplayer.source.afreeca;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.ExceptionTools;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.JsonBrowser;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpConfigurable;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterfaceManager;
import com.sedmelluq.discord.lavaplayer.track.AudioItem;
import com.sedmelluq.discord.lavaplayer.track.AudioReference;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.SUSPICIOUS;

/**
 * Audio source manager which detects AfreecaTV tracks by URL.
 */
public class AfreecaAudioSourceManager implements AudioSourceManager, HttpConfigurable {
  private static final String TRACK_URL_REGEX = "^https://vod.afreecatv.com/player/([0-9]+)(?:\\?.*|)$";
  private static final Pattern trackUrlPattern = Pattern.compile(TRACK_URL_REGEX);

  private final HttpInterfaceManager httpInterfaceManager;

  /**
   * Create an instance.
   */
  public AfreecaAudioSourceManager() {
    httpInterfaceManager = HttpClientTools.createDefaultThreadLocalManager();
  }

  @Override
  public String getSourceName() {
    return "afreeca";
  }

  @Override
  public AudioItem loadItem(AudioPlayerManager manager, AudioReference reference) {
    Matcher trackMatcher = trackUrlPattern.matcher(reference.identifier);

    if (trackMatcher.matches()) {
      try (HttpInterface httpInterface = httpInterfaceManager.getInterface()) {
        return loadFromTrackPage(httpInterface, trackMatcher.group(1));
      } catch (IOException e) {
        throw new FriendlyException("Loading Afreeca track information failed.", SUSPICIOUS, e);
      }
    }

    return null;
  }

  @Override
  public boolean isTrackEncodable(AudioTrack track) {
    return true;
  }

  @Override
  public void encodeTrack(AudioTrack track, DataOutput output) throws IOException {
    // Nothing special to encode
  }

  @Override
  public AudioTrack decodeTrack(AudioTrackInfo trackInfo, DataInput input) throws IOException {
    return new AfreecaAudioTrack(trackInfo, this);
  }

  @Override
  public void shutdown() {
    ExceptionTools.closeWithWarnings(httpInterfaceManager);
  }

  /**
   * @return Get an HTTP interface for a playing track.
   */
  public HttpInterface getHttpInterface() {
    return httpInterfaceManager.getInterface();
  }

  @Override
  public void configureRequests(Function<RequestConfig, RequestConfig> configurator) {
    httpInterfaceManager.configureRequests(configurator);
  }

  @Override
  public void configureBuilder(Consumer<HttpClientBuilder> configurator) {
    httpInterfaceManager.configureBuilder(configurator);
  }

  private AudioItem loadFromTrackPage(HttpInterface httpInterface, String titleNo) throws IOException {
    HttpPost viewRequest = new HttpPost("https://api.m.afreecatv.com/station/video/a/view");
    viewRequest.setEntity(new UrlEncodedFormEntity(Arrays.asList(
            new BasicNameValuePair("nTitleNo", titleNo),
            new BasicNameValuePair("nApiLevel", "10")
    ), StandardCharsets.UTF_8));

    try (CloseableHttpResponse response = httpInterface.execute(viewRequest)) {
      int statusCode = response.getStatusLine().getStatusCode();

      if (statusCode == HttpStatus.SC_NOT_FOUND) {
        return AudioReference.NO_TRACK;
      } else if (!HttpClientTools.isSuccessWithContent(statusCode)) {
        throw new FriendlyException("Server responded with an error.", SUSPICIOUS,
            new IllegalStateException("Response code is " + statusCode));
      }

      return loadTrackFromPageContent(titleNo, IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8));
    }
  }

  private AudioTrack loadTrackFromPageContent(String titleNo, String content) throws IOException {
    JsonBrowser info = JsonBrowser.parse(content);

    if (info == null) {
      throw new FriendlyException("Track information not found on the page.", SUSPICIOUS, null);
    }

    return new AfreecaAudioTrack(new AudioTrackInfo(
        info.get("data").get("title").text(),
        info.get("data").get("copyright_nickname").text(),
        info.get("data").get("total_file_duration").as(Long.class),
        titleNo,
        false,
        info.get("data").get("files").index(0).get("file").text()
    ), this);
  }
}
