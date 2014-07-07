package org.dash.avionics;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.Fullscreen;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.api.BackgroundExecutor;
import org.dash.avionics.data.MeasurementListener;
import org.dash.avionics.data.MeasurementType;
import org.dash.avionics.data.Measurement;
import org.dash.avionics.sensors.*;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

@Fullscreen
@EActivity(R.layout.activity_avionics)
public class AvionicsActivity extends Activity implements ServiceConnection,
		MeasurementListener {
	private static final long DEFAULT_MAX_DATA_AGE_MS = 2 * 1000;
	// ANT+ needs larger delays
	private static final long ANTPLUS_MAX_DATA_AGE_MS = 5 * 1000;
	private static final Map<MeasurementType, Long> MAX_DATA_AGES_MS = new HashMap<MeasurementType, Long>();
	{
		MAX_DATA_AGES_MS.put(MeasurementType.PROP_RPM, DEFAULT_MAX_DATA_AGE_MS);
		MAX_DATA_AGES_MS.put(MeasurementType.HEADING, DEFAULT_MAX_DATA_AGE_MS);
		MAX_DATA_AGES_MS.put(MeasurementType.HEIGHT, DEFAULT_MAX_DATA_AGE_MS);
		MAX_DATA_AGES_MS.put(MeasurementType.SPEED, DEFAULT_MAX_DATA_AGE_MS);

		MAX_DATA_AGES_MS.put(MeasurementType.POWER, ANTPLUS_MAX_DATA_AGE_MS);
		MAX_DATA_AGES_MS.put(MeasurementType.CRANK_RPM, ANTPLUS_MAX_DATA_AGE_MS);
		MAX_DATA_AGES_MS.put(MeasurementType.HEART_BEAT, ANTPLUS_MAX_DATA_AGE_MS);
	}

	@ViewById
	protected TextView rpmView, powerView, heartView, headingView, speedView,
			heightView;

	private Map<MeasurementType, TextView> viewsByType = new HashMap<MeasurementType, TextView>(
			10);
	private Map<MeasurementType, Long> lastUpdateByType = new ConcurrentHashMap<MeasurementType, Long>(
			10);
	private final Messenger incomingMessenger = new Messenger(
			new IncomingHandler(this));
	protected Messenger outgoingMessenger;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getActionBar().hide();
	}

	@AfterViews
	protected void setValues() {
		viewsByType.put(MeasurementType.PROP_RPM, rpmView);
		viewsByType.put(MeasurementType.POWER, powerView);
		viewsByType.put(MeasurementType.HEART_BEAT, heartView);
		viewsByType.put(MeasurementType.HEADING, headingView);
		viewsByType.put(MeasurementType.SPEED, speedView);
		viewsByType.put(MeasurementType.HEIGHT, heightView);

		bindService();
	}

	private void bindService() {
		if (outgoingMessenger != null) {
			return;
		}

		// Establish a connection with the service. We use an explicit
		// class name because there is no reason to be able to let other
		// applications replace our component.
		Intent intent = SensorsService_.intent(getApplicationContext()).get();
		bindService(intent, this, BIND_AUTO_CREATE);
	}

	@Override
	protected void onResume() {
		super.onResume();

		bindService();
		runWatchdog();
	}

	@Override
	protected void onPause() {
		BackgroundExecutor.cancelAll("watchdog", true);

		Message msg = Message
				.obtain(null, SensorsService.MSG_UNREGISTER_CLIENT);
		msg.replyTo = incomingMessenger;
		if (outgoingMessenger == null) {
			try {
				outgoingMessenger.send(msg);
			} catch (RemoteException e) {
				// Service is already dead.
			}
		}
		unbindService(this);
		outgoingMessenger = null;

		super.onPause();
	}

	@Background(id = "watchdog", delay = 500)
	protected void runWatchdog() {
		long now = System.currentTimeMillis();
		for (MeasurementType type : MeasurementType.values()) {
			Long lastTimestamp = lastUpdateByType.get(type);
			long maxAge = MAX_DATA_AGES_MS.get(type);
			if (lastTimestamp == null || lastTimestamp < now - maxAge) {
				Log.w("Watchdog", "No recent update for type " + type);
				setValueUnknown(type);
			}
		}

		runWatchdog();
	}

	@UiThread
	protected void setValueUnknown(MeasurementType type) {
		TextView view = viewsByType.get(type);
		if (view == null) {
			Log.v("UI", "No view for type " + type);
			return;
		}

		view.setText(R.string.value_not_available);
	}

	@UiThread
	protected void setValue(Measurement update) {
		TextView view = viewsByType.get(update.type);
		if (view == null) {
			Log.v("UI", "No view for type " + update.type);
			return;
		}

		String valueStr = String.format(Locale.US, "%.1f", update.value);
		view.setText(valueStr);
	}

	@Override
	public void onNewMeasurement(Measurement update) {
		lastUpdateByType.put(update.type, System.currentTimeMillis());
		setValue(update);
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		View decorView = getWindow().getDecorView();
		if (hasFocus) {
			// TODO: Support older versions
			decorView
					.setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
			// | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
		}
	}

	/**
	 * Handler of incoming messages from service.
	 */
	private static class IncomingHandler extends Handler {
		private final WeakReference<MeasurementListener> updater;

		public IncomingHandler(MeasurementListener updater) {
			this.updater = new WeakReference<MeasurementListener>(updater);
		}

		@Override
		public void handleMessage(Message msg) {
			MeasurementListener updaterRef = updater.get();
			if (updaterRef == null)
				return;

			switch (msg.what) {
			case SensorsService.MSG_UPDATED_VALUE: {
				Measurement update = new Measurement(
						MeasurementType.values()[msg.arg1], msg.arg2 / 10.0f);
				updaterRef.onNewMeasurement(update);
				break;
			}
			default:
				super.handleMessage(msg);
			}
		}
	}

	public void onServiceConnected(ComponentName className, IBinder service) {
		outgoingMessenger = new Messenger(service);

		// We want to monitor the service for as long as we are
		// connected to it.
		try {
			Message msg = Message.obtain(null,
					SensorsService.MSG_REGISTER_CLIENT);
			msg.replyTo = incomingMessenger;
			outgoingMessenger.send(msg);
		} catch (RemoteException e) {
			// In this case the service has crashed before we could even
			// do anything with it; we can count on soon being
			// disconnected (and then reconnected if it can be restarted)
			// so there is no need to do anything here.
		}
	}

	public void onServiceDisconnected(ComponentName className) {
		// This is called when the connection with the service has been
		// unexpectedly disconnected -- that is, its process crashed.
		outgoingMessenger = null;
	}
}
