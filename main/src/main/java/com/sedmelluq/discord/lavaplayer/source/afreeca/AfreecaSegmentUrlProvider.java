package com.sedmelluq.discord.lavaplayer.source.afreeca;

import com.sedmelluq.discord.lavaplayer.container.playlists.ExtendedM3uParser;
import com.sedmelluq.discord.lavaplayer.container.playlists.HlsStreamSegmentUrlProvider;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;

import java.io.IOException;
import java.util.List;

import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.SUSPICIOUS;

/**
 * Provider for AfreecaTV segment URLs from a channel.
 */
public class AfreecaSegmentUrlProvider extends HlsStreamSegmentUrlProvider {
  private String streamSegmentPlaylistUrl;
  private List<SegmentInfo> segments;
  private int index;

  public AfreecaSegmentUrlProvider(String streamListUrl) {
    super(streamListUrl, null);
  }

  @Override
  protected String getQualityFromM3uDirective(ExtendedM3uParser.Line directiveLine) {
    return directiveLine.directiveArguments.get("NAME");
  }

  @Override
  protected String getNextSegmentUrl(HttpInterface httpInterface) {
    try {
      if (segments == null) {
        streamSegmentPlaylistUrl = fetchSegmentPlaylistUrl(httpInterface);
        if (streamSegmentPlaylistUrl == null) {
          return null;
        }
        segments = loadStreamSegmentsList(httpInterface, streamSegmentPlaylistUrl);
      }
      if (index < segments.size()) {
        return createSegmentUrl(streamSegmentPlaylistUrl, segments.get(index++).url);
      }
      return null;
    } catch (IOException e) {
      throw new FriendlyException("Failed to get next part of the stream.", SUSPICIOUS, e);
    }
  }
}