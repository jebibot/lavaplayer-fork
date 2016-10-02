package com.sedmelluq.discord.lavaplayer.container.matroska;

import com.sedmelluq.discord.lavaplayer.tools.io.SeekableInputStream;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.BaseAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioTrackExecutor;
import org.ebml.matroska.MatroskaFileTrack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Audio track that handles the processing of MKV and WEBM formats
 */
public class MatroskaAudioTrack extends BaseAudioTrack {
  private static final Logger log = LoggerFactory.getLogger(MatroskaAudioTrack.class);

  private static final String OPUS_CODEC = "A_OPUS";
  private static final String VORBIS_CODEC = "A_VORBIS";
  private static final String AAC_CODEC = "A_AAC";

  private final SeekableInputStream inputStream;

  /**
   * @param executor Track executor
   * @param trackInfo Track info
   * @param inputStream Input stream for the file
   */
  public MatroskaAudioTrack(AudioTrackExecutor executor, AudioTrackInfo trackInfo, SeekableInputStream inputStream) {
    super(executor, trackInfo);

    this.inputStream = inputStream;
  }

  @Override
  public void process() {
    MatroskaStreamingFile file = loadMatroskaFile();
    MatroskaTrackConsumer trackConsumer = loadAudioTrack(file);

    try {
      executor.executeProcessingLoop(() -> {
        file.provideFrames(trackConsumer);
      }, position -> {
        file.seekToTimecode(trackConsumer.getTrack().getTrackNo(), position);
      });
    } finally {
      trackConsumer.close();
    }
  }

  private MatroskaStreamingFile loadMatroskaFile() {
    MatroskaStreamingFile file = new MatroskaStreamingFile(
        new MatroskaStreamDataSource(inputStream)
    );

    file.readFile();

    accurateDuration.set((int) file.getDuration());
    return file;
  }

  private MatroskaTrackConsumer loadAudioTrack(MatroskaStreamingFile file) {
    MatroskaTrackConsumer trackConsumer = null;
    boolean success = false;

    try {
      trackConsumer = selectAudioTrack(file.getTrackList());

      if (trackConsumer == null) {
        throw new IllegalStateException("No supported audio tracks in the file.");
      } else {
        log.debug("Starting to play track with codec {}", trackConsumer.getTrack().getCodecID());
      }

      trackConsumer.initialise();
      success = true;
    } finally {
      if (!success && trackConsumer != null) {
        trackConsumer.close();
      }
    }

    return trackConsumer;
  }

  private MatroskaTrackConsumer selectAudioTrack(MatroskaFileTrack[] tracks) {
    MatroskaTrackConsumer trackConsumer = null;

    for (MatroskaFileTrack track : tracks) {
      if (track.getTrackType() == MatroskaFileTrack.TrackType.AUDIO) {
        if (OPUS_CODEC.equals(track.getCodecID())) {
          trackConsumer = new MatroskaOpusTrackConsumer(executor.getFrameConsumer(), track);
          break;
        } else if (VORBIS_CODEC.equals(track.getCodecID())) {
          trackConsumer = new MatroskaVorbisTrackConsumer(executor.getFrameConsumer(), track);
        } else if (AAC_CODEC.equals(track.getCodecID())) {
          trackConsumer = new MatroskaAacTrackConsumer(executor.getFrameConsumer(), track);
        }
      }
    }

    return trackConsumer;
  }
}