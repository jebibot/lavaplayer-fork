package com.sedmelluq.discord.lavaplayer.source.afreeca;

import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.stream.M3uStreamSegmentUrlProvider;
import com.sedmelluq.discord.lavaplayer.source.stream.MpegTsM3uStreamAudioTrack;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;

/**
 * Audio track that handles processing AfreecaTV tracks.
 */
public class AfreecaAudioTrack extends MpegTsM3uStreamAudioTrack {
  private final AfreecaAudioSourceManager sourceManager;
  private final M3uStreamSegmentUrlProvider segmentUrlProvider;

  /**
   * @param trackInfo Track info
   * @param sourceManager Source manager which was used to find this track
   */
  public AfreecaAudioTrack(AudioTrackInfo trackInfo, AfreecaAudioSourceManager sourceManager) {
    super(trackInfo);

    this.sourceManager = sourceManager;
    this.segmentUrlProvider = new AfreecaSegmentUrlProvider(trackInfo.uri);
  }

  @Override
  protected M3uStreamSegmentUrlProvider getSegmentUrlProvider() {
    return segmentUrlProvider;
  }

  @Override
  protected HttpInterface getHttpInterface() {
    return sourceManager.getHttpInterface();
  }

  @Override
  protected AudioTrack makeShallowClone() {
    return new AfreecaAudioTrack(trackInfo, sourceManager);
  }

  @Override
  public AudioSourceManager getSourceManager() {
    return sourceManager;
  }
}
