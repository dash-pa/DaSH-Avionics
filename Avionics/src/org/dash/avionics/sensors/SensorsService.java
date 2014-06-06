package org.dash.avionics.sensors;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EService;
import org.androidannotations.api.BackgroundExecutor;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

@EService
public class SensorsService extends Service {

	/**
	 * Command to the service to register a client, receiving callbacks from the
	 * service. The Message's replyTo field must be a Messenger of the client
	 * where callbacks should be sent.
	 */
	public static final int MSG_REGISTER_CLIENT = 1;

	/**
	 * Command to the service to unregister a client, ot stop receiving
	 * callbacks from the service. The Message's replyTo field must be a
	 * Messenger of the client as previously given with MSG_REGISTER_CLIENT.
	 */
	public static final int MSG_UNREGISTER_CLIENT = 2;

	/**
	 * Command from the service to indicate that a value has been updated.
	 */
	public static final int MSG_UPDATED_VALUE = 3;

	/** Keeps track of all current registered clients. */
	private List<Messenger> clients = new ArrayList<Messenger>();

	/**
	 * Target we publish for clients to send messages to IncomingHandler.
	 */
	private final Messenger messenger;

	private final ArduinoSensorManager arduinoManager;

	/**
	 * Handler of incoming messages from clients.
	 */
	private static class IncomingHandler extends Handler {
		private WeakReference<List<Messenger>> clients;

		private IncomingHandler(List<Messenger> clients) {
			this.clients = new WeakReference<List<Messenger>>(clients);
		}

		@Override
		public void handleMessage(Message msg) {
			List<Messenger> clientsRef = clients.get();
			if (clientsRef == null)
				return;

			synchronized (clientsRef) {
				switch (msg.what) {
				case MSG_REGISTER_CLIENT:
					clientsRef.add(msg.replyTo);
					break;
				case MSG_UNREGISTER_CLIENT:
					clientsRef.remove(msg.replyTo);
					break;
				default:
					super.handleMessage(msg);
				}
			}
		}
	}

	public SensorsService() {
		messenger = new Messenger(new IncomingHandler(clients));
		arduinoManager = new ArduinoSensorManager();
	}

	@Override
	public void onCreate() {
		startArduinoSensors();
	}

	@Override
	public void onDestroy() {
		BackgroundExecutor.cancelAll("arduino", true);
	}

	/**
	 * When binding to the service, we return an interface to our messenger for
	 * sending messages to the service.
	 */
	@Override
	public IBinder onBind(Intent intent) {
		return messenger.getBinder();
	}

	@Background(id = "arduino")
	protected void startArduinoSensors() {
		// Keep trying to connect and read from the sensors.
		while (true) {
			try {
				arduinoManager.connect();
			} catch (IOException e) {
				Log.e("Arduino", "Failed to initialize", e);
				arduinoManager.disconnect();
				try {
					Thread.sleep(500);
				} catch (InterruptedException ie) {
					// Do nothing.
				}
				continue;
			}

			ValueUpdate update;
			try {
				update = arduinoManager.readUpdate();
			} catch (IOException e) {
				Log.w("Arduino", "Failed to read from Arduino", e);
				arduinoManager.disconnect();
				continue;
			}

			sendMessage(update);
		}
	}

	private void sendMessage(ValueUpdate update) {
		Message msg = Message.obtain(null, MSG_UPDATED_VALUE,
				update.type.ordinal(), update.value);
		synchronized (clients) {
			for (int i = clients.size() - 1; i >= 0; i--) {
				try {
					clients.get(i).send(msg);
				} catch (RemoteException e) {
					// The client is dead. Remove it from the list;
					// we are going through the list from back to front
					// so this is safe to do inside the loop.
					clients.remove(i);
				}
			}
		}
	}
}
