package org.dash.avionics.alerts;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;
import org.androidannotations.annotations.SystemService;
import org.dash.avionics.R;

import java.util.Map;
import java.util.Set;

@EBean
public class MeasurementAlertSounds implements MeasurementAlerter.AlertListener {
  @RootContext
  Context context;
  @SystemService
  AudioManager audio;
  private SoundPool soundPool;

  private final Map<AlertType, Integer> soundIds = Maps.newEnumMap(AlertType.class);
  private final Map<AlertType, Integer> activeStreamIds = Maps.newEnumMap(AlertType.class);

  public void start() {
    activeStreamIds.clear();

    if (soundPool == null) {
      AudioAttributes audioAttributes = new AudioAttributes.Builder()
          .setContentType(AudioAttributes.CONTENT_TYPE_UNKNOWN)
          .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
          .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
          .build();
      soundPool = new SoundPool.Builder()
          .setMaxStreams(5)
          .setAudioAttributes(audioAttributes)
          .build();

      soundIds.put(AlertType.LOW_SPEED, soundPool.load(context, R.raw.slow, 2));
      soundIds.put(AlertType.HIGH_SPEED, soundPool.load(context, R.raw.fast, 1));
    }
  }

  @Override
  public void onAlertsChanged(Set<AlertType> types) {
    Sets.SetView<AlertType> removed = Sets.difference(activeStreamIds.keySet(), types);
    Sets.SetView<AlertType> added = Sets.difference(types, activeStreamIds.keySet());

    // TODO: Play sequentially, with relative priority (use MediaPlayer?).
    if (activeStreamIds.isEmpty() && !added.isEmpty()) {
      // First stream will start playing.
      audio.requestAudioFocus(null, AudioManager.STREAM_ALARM, AudioManager.AUDIOFOCUS_GAIN);
    }

    for (AlertType type : removed) {
      // Stop removed sounds.
      Integer streamId = activeStreamIds.get(type);
      if (streamId != null) {
        soundPool.stop(streamId);
        activeStreamIds.remove(type);
      }
    }

    for (AlertType type : added) {
      // Start added sounds.
      Integer soundId = soundIds.get(type);
      if (soundId != null) {
        int streamId = soundPool.play(soundId, 1.0f, 1.0f, 2, -1, 1.0f);
        activeStreamIds.put(type, streamId);
      }
    }

    if (activeStreamIds.isEmpty()) {
      // All streams were stopped.
      audio.abandonAudioFocus(null);
    }
  }

  public void stop() {
    synchronized (soundPool) {
      for (int streamId : activeStreamIds.values()) {
        soundPool.stop(streamId);
      }
      audio.abandonAudioFocus(null);
    }
  }
}
