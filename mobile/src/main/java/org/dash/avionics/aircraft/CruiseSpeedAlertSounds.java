package org.dash.avionics.aircraft;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;
import org.androidannotations.annotations.SystemService;
import org.dash.avionics.R;

@EBean
public class CruiseSpeedAlertSounds implements CruiseSpeedAlerter.CruiseSpeedAlertListener {
  @RootContext
  Context context;
  @SystemService
  AudioManager audio;
  private SoundPool soundPool;
  private int slowSoundId;
  private int fastSoundId;
  private int slowStreamId = -1;
  private int fastStreamId = -1;

  public void start() {
    if (soundPool == null) {
      AudioAttributes audioAttributes = new AudioAttributes.Builder()
          .setContentType(AudioAttributes.CONTENT_TYPE_UNKNOWN)
          .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
          .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
          .build();
      soundPool = new SoundPool.Builder()
          .setMaxStreams(1)
          .setAudioAttributes(audioAttributes)
          .build();
      fastSoundId = soundPool.load(context, R.raw.fast, 1);
      slowSoundId = soundPool.load(context, R.raw.slow, 2);
    }
  }

  public void stop() {
    synchronized (soundPool) {
      stopAllSounds();
    }
  }

  @Override
  public void onLowSpeed() {
    synchronized (soundPool) {
      stopFastSound();

      audio.requestAudioFocus(null, AudioManager.STREAM_ALARM, AudioManager.AUDIOFOCUS_GAIN);
      slowStreamId = soundPool.play(slowSoundId, 1.0f, 1.0f, 2, -1, 1.0f);
    }
  }

  @Override
  public void onHighSpeed() {
    synchronized (soundPool) {
      stopSlowSound();

      audio.requestAudioFocus(null, AudioManager.STREAM_ALARM, AudioManager.AUDIOFOCUS_GAIN);
      fastStreamId = soundPool.play(fastSoundId, 1.0f, 1.0f, 2, -1, 1.0f);
    }
  }

  @Override
  public void onStoppedAlerting() {
    synchronized (soundPool) {
      stopAllSounds();
    }
  }

  private void stopFastSound() {
    if (fastStreamId != -1) {
      soundPool.stop(fastStreamId);
      fastStreamId = -1;
    }
  }

  private void stopSlowSound() {
    if (slowStreamId != -1) {
      soundPool.stop(slowStreamId);
      slowStreamId = -1;
    }
  }

  private void stopAllSounds() {
    stopSlowSound();
    stopFastSound();
    audio.abandonAudioFocus(null);
  }
}
