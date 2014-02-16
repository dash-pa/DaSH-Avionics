package org.dash.avionics;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.Fullscreen;
import org.androidannotations.annotations.ViewById;

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
import android.view.View;
import android.widget.TextView;

@Fullscreen
@EActivity(R.layout.activity_avionics)
public class AvionicsActivity extends Activity implements ServiceConnection, ValueUpdater {

	@ViewById
	protected TextView rpmView, powerView, heartView, headingView, speedView,
			heightView;

	private Map<ValueType, TextView> viewsByType = new HashMap<ValueType, TextView>(
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
		viewsByType.put(ValueType.RPM, rpmView);
		viewsByType.put(ValueType.POWER, powerView);
		viewsByType.put(ValueType.HEART_BEAT, heartView);
		viewsByType.put(ValueType.HEADING, headingView);
		viewsByType.put(ValueType.SPEED, speedView);
		viewsByType.put(ValueType.HEIGHT, heightView);

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
	}

	@Override
	protected void onPause() {
		Message msg = Message
				.obtain(null, SensorsService.MSG_UNREGISTER_CLIENT);
		msg.replyTo = incomingMessenger;
		try {
			outgoingMessenger.send(msg);
		} catch (RemoteException e) {
			// Service is already dead.
		}
		unbindService(this);
		outgoingMessenger = null;

		super.onPause();
	}

	public void updateValue(ValueType valueType, int value) {
		viewsByType.get(valueType).setText(String.valueOf(value));
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		View decorView = getWindow().getDecorView();
		if (hasFocus) {
			// TODO: Support older versions
			decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
					| View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
		}
	}

	/**
	 * Handler of incoming messages from service.
	 */
	private static class IncomingHandler extends Handler {
		private final WeakReference<ValueUpdater> updater;

		public IncomingHandler(ValueUpdater updater) {
			this.updater = new WeakReference<ValueUpdater>(updater);
		}

		@Override
		public void handleMessage(Message msg) {
			ValueUpdater updaterRef = updater.get();
			if (updaterRef == null)
				return;

			switch (msg.what) {
			case SensorsService.MSG_UPDATED_VALUE:
				updaterRef.updateValue(ValueType.values()[msg.arg1], msg.arg2);
				break;
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
