package org.dash.avionics.sensors;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EService;
import org.dash.avionics.data.ValueUpdate;
import org.dash.avionics.sensors.ant.AntSensorManager;
import org.dash.avionics.sensors.arduino.ArduinoSensorManager;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

@EService
public class SensorsService extends Service implements ValueUpdater {

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

	/*
	 * Managers for many types of sensors.
	 */
	@Bean
	protected ArduinoSensorManager arduinoSensor;
	@Bean
	protected AntSensorManager antSensor;

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
	}

	@Override
	public void onCreate() {
		antSensor.connect(this);
		arduinoSensor.connect(this);
	}

	@Override
	public void onDestroy() {
		antSensor.disconnect();
		arduinoSensor.disconnect();
	}

	/**
	 * When binding to the service, we return an interface to our messenger for
	 * sending messages to the service.
	 */
	@Override
	public IBinder onBind(Intent intent) {
		return messenger.getBinder();
	}

	@Override
	public void updateValue(ValueUpdate update) {
		// Forward the update to the subscribed clients.
		Message msg = Message.obtain(null, MSG_UPDATED_VALUE,
				update.type.ordinal(), (int) (update.value * 10));
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
