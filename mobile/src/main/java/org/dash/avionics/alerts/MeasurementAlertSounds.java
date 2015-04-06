package org.dash.avionics.alerts;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.util.Log;

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
  private class AlertSoundDescription {
    private final int soundId;
    private final int priority;
    private final int numLoops;

    private AlertSoundDescription(int soundId, int priority) {
      this.soundId = soundId;
      this.priority = priority;
      this.numLoops = -1;
    }

    private AlertSoundDescription(int soundId, int priority, int numLoops) {
      this.soundId = soundId;
      this.priority = priority;
      this.numLoops = numLoops;
    }
  }

  private final Map<AlertType, AlertSoundDescription> sounds = Maps.newEnumMap(AlertType.class);
  private final Map<AlertType, Integer> activeStreamIds = Maps.newEnumMap(AlertType.class);

  @RootContext
  Context context;

  @SystemService
  AudioManager audio;
  private SoundPool soundPool;

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

      loadSound(AlertType.LOW_SPEED, R.raw.slow, 100, -1);
      loadSound(AlertType.HIGH_SPEED, R.raw.fast, 50, -1);
      loadSound(AlertType.UNKNOWN_SPEED, R.raw.speed, 1, 5);
    }
  }

  private void loadSound(AlertType type, int resId, int priority, int loops) {
    int soundId = soundPool.load(context, resId, priority);
    sounds.put(type, new AlertSoundDescription(soundId, priority, loops));
  }

  @Override
  public void onAlertsChanged(Set<AlertType> types) {
    Log.d("Sounds", "New alerts=" + types);
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
      AlertSoundDescription sound = sounds.get(type);
      if (sound != null) {
        int streamId =
            soundPool.play(sound.soundId, 1.0f, 1.0f, sound.priority, sound.numLoops, 1.0f);
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
