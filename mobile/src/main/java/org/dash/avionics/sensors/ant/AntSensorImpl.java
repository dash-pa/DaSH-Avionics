package org.dash.avionics.sensors.ant;

import java.math.BigDecimal;
import java.util.EnumSet;

import org.androidannotations.annotations.EBean;
import org.dash.avionics.data.Measurement;
import org.dash.avionics.data.MeasurementType;
import org.dash.avionics.sensors.SensorListener;

import android.content.Context;
import android.util.Log;

import com.dsi.ant.plugins.antplus.pcc.AntPlusBikeCadencePcc;
import com.dsi.ant.plugins.antplus.pcc.AntPlusBikeCadencePcc.ICalculatedCadenceReceiver;
import com.dsi.ant.plugins.antplus.pcc.AntPlusBikePowerPcc;
import com.dsi.ant.plugins.antplus.pcc.AntPlusBikePowerPcc.DataSource;
import com.dsi.ant.plugins.antplus.pcc.AntPlusBikePowerPcc.ICalculatedCrankCadenceReceiver;
import com.dsi.ant.plugins.antplus.pcc.AntPlusBikePowerPcc.ICalculatedPowerReceiver;
import com.dsi.ant.plugins.antplus.pcc.AntPlusBikePowerPcc.IInstantaneousCadenceReceiver;
import com.dsi.ant.plugins.antplus.pcc.AntPlusHeartRatePcc;
import com.dsi.ant.plugins.antplus.pcc.AntPlusHeartRatePcc.DataState;
import com.dsi.ant.plugins.antplus.pcc.AntPlusHeartRatePcc.IHeartRateDataReceiver;
import com.dsi.ant.plugins.antplus.pcc.defines.BatteryStatus;
import com.dsi.ant.plugins.antplus.pcc.defines.DeviceState;
import com.dsi.ant.plugins.antplus.pcc.defines.DeviceType;
import com.dsi.ant.plugins.antplus.pcc.defines.EventFlag;
import com.dsi.ant.plugins.antplus.pcc.defines.RequestAccessResult;
import com.dsi.ant.plugins.antplus.pccbase.AntPluginPcc.IDeviceStateChangeReceiver;
import com.dsi.ant.plugins.antplus.pccbase.AntPluginPcc.IPluginAccessResultReceiver;
import com.dsi.ant.plugins.antplus.pccbase.AntPlusCommonPcc.IBatteryStatusReceiver;
import com.dsi.ant.plugins.antplus.pccbase.MultiDeviceSearch;
import com.dsi.ant.plugins.antplus.pccbase.MultiDeviceSearch.MultiDeviceSearchResult;
import com.dsi.ant.plugins.antplus.pccbase.MultiDeviceSearch.RssiCallback;
import com.dsi.ant.plugins.antplus.pccbase.PccReleaseHandle;

@EBean
class AntSensorImpl implements MultiDeviceSearch.SearchCallbacks, RssiCallback, IDeviceStateChangeReceiver, ICalculatedCadenceReceiver, ICalculatedPowerReceiver, IHeartRateDataReceiver, IBatteryStatusReceiver, ICalculatedCrankCadenceReceiver, IInstantaneousCadenceReceiver {
	private final Context context;
    private static final EnumSet<DeviceType> RELEVANT_DEVICE_TYPES =
    		EnumSet.of(DeviceType.HEARTRATE, DeviceType.BIKE_POWER,
    				   DeviceType.BIKE_CADENCE, DeviceType.BIKE_SPDCAD);

	// Device search state.
	private MultiDeviceSearch deviceSearch;

    // Device connection state.
	private AntPlusBikeCadencePcc cadencePcc;
	private AntPlusBikePowerPcc powerPcc;
	private AntPlusHeartRatePcc heartRatePcc;
	private PccReleaseHandle<AntPlusBikeCadencePcc> cadenceReleaseHandle;
    private PccReleaseHandle<AntPlusHeartRatePcc> heartRateReleaseHandle;
	private PccReleaseHandle<AntPlusBikePowerPcc> powerReleaseHandle;

	// External callback.
	private SensorListener updater;

	AntSensorImpl(Context context) {
		this.context = context;
	}

	void connect(SensorListener updater) {
		if (this.updater != null) {
			throw new IllegalStateException("Trying to register a second updater");
		}

		this.updater = updater;

		Log.i("ANT", "Starting first ANT+ search for " + RELEVANT_DEVICE_TYPES);
		startSearching();
	}

	protected void startSearching() {
		// Start searching for devices.
		Log.v("ANT", "ANT+ search commenced.");
		deviceSearch = new MultiDeviceSearch(context, RELEVANT_DEVICE_TYPES, this, this);
	}

	void disconnect() {
		if (deviceSearch != null) {
			deviceSearch.close();
			deviceSearch = null;
		}

		if (cadenceReleaseHandle != null) {
			cadenceReleaseHandle.close();
		}
		if (powerReleaseHandle != null) {
			powerReleaseHandle.close();
		}
		if (heartRateReleaseHandle != null) {
			heartRateReleaseHandle.close();
		}

		this.updater = null;
	}

	@Override
	public void onDeviceFound(MultiDeviceSearchResult result) {
		Log.i("ANT", "ANT+ device found: type=" + result.getAntDeviceType() +
				"; result=" + result.resultID +
				"; device=" + result.getAntDeviceNumber() +
				"; name='" + result.getDeviceDisplayName() + "'");

		switch (result.getAntDeviceType()) {
		case BIKE_CADENCE:
		case BIKE_SPDCAD:
			connectCadence(result.getAntDeviceNumber(), result.getAntDeviceType());
			break;
		case BIKE_POWER:
			connectPower(result.getAntDeviceNumber());
			break;
		case HEARTRATE:
			connectHeartRate(result.getAntDeviceNumber());
			break;
		default:
			Log.w("ANT", "Irrelevant device detected: type=" + result.getAntDeviceType());
			break;
		}
	}

	@Override
	public void onSearchStopped(RequestAccessResult result) {
		Log.v("ANT", "ANT+ search stopped.");
		deviceSearch.close();
		deviceSearch = null;

		// Keep searching.
		try {
			Thread.sleep(2000);
			startSearching();
		} catch (InterruptedException e) {
			// Do nothing.
		}
	}

	@Override
	public void onRssiUpdate(int resultId, int rssi) {
		Log.i("ANT", "RSSI for result " + resultId + " is " + rssi);
	}

	protected void connectCadence(int antDeviceNumber, DeviceType type) {
		boolean isSpeedCadence = type.equals(DeviceType.BIKE_SPDCAD);
        cadenceReleaseHandle = AntPlusBikeCadencePcc.requestAccess(context,
                antDeviceNumber, 0, isSpeedCadence, cadencePccReceiver, this);
	}

	protected void connectPower(int antDeviceNumber) {
        powerReleaseHandle = AntPlusBikePowerPcc.requestAccess(context, antDeviceNumber, 0,
                powerPccReceiver, this);
	}

	protected void connectHeartRate(int antDeviceNumber) {
        heartRateReleaseHandle = AntPlusHeartRatePcc.requestAccess(context, antDeviceNumber, 0,
        		heartRatePccReceiver, this);
	}

	@Override
	public void onDeviceStateChange(DeviceState state) {
		Log.w("ANT", "New device state: " + state);
	}

    private final IPluginAccessResultReceiver<AntPlusBikeCadencePcc> cadencePccReceiver = new IPluginAccessResultReceiver<AntPlusBikeCadencePcc>() {
		@Override
		public void onResultReceived(AntPlusBikeCadencePcc result,
				RequestAccessResult resultCode, DeviceState initialDeviceState) {
			if (resultCode != RequestAccessResult.SUCCESS) {
				Log.e("ANT", "Failed to get cadence PCC: result=" + resultCode);
				return;
			}

			cadencePcc = result;
			powerPcc.subscribeBatteryStatusEvent(AntSensorImpl.this);
			cadencePcc.subscribeCalculatedCadenceEvent(AntSensorImpl.this);
		}
    };

    private final IPluginAccessResultReceiver<AntPlusBikePowerPcc> powerPccReceiver = new IPluginAccessResultReceiver<AntPlusBikePowerPcc>() {
		@Override
		public void onResultReceived(AntPlusBikePowerPcc result,
				RequestAccessResult resultCode, DeviceState initialDeviceState) {
			if (resultCode != RequestAccessResult.SUCCESS) {
				Log.e("ANT", "Failed to get power PCC: result=" + resultCode);
				return;
			}

			powerPcc = result;
			powerPcc.subscribeBatteryStatusEvent(AntSensorImpl.this);
			powerPcc.subscribeCalculatedPowerEvent(AntSensorImpl.this);
			powerPcc.subscribeCalculatedCrankCadenceEvent(AntSensorImpl.this);
			powerPcc.subscribeInstantaneousCadenceEvent(AntSensorImpl.this);
		}
    };

	private final IPluginAccessResultReceiver<AntPlusHeartRatePcc> heartRatePccReceiver = new IPluginAccessResultReceiver<AntPlusHeartRatePcc>() {
		@Override
		public void onResultReceived(AntPlusHeartRatePcc result,
				RequestAccessResult resultCode, DeviceState initialDeviceState) {
			if (resultCode != RequestAccessResult.SUCCESS) {
				Log.e("ANT", "Failed to get HR PCC: result=" + resultCode);
				return;
			}

			heartRatePcc = result;
			heartRatePcc.subscribeHeartRateDataEvent(AntSensorImpl.this);
		}
	};

	@Override
    public void onNewCalculatedCadence(final long estTimestamp,
            final EnumSet<EventFlag> eventFlags, final BigDecimal calculatedCadence) {
		updater.onNewMeasurement(new Measurement(MeasurementType.CRANK_RPM, calculatedCadence.floatValue()));
	}

	@Override
	public void onNewCalculatedCrankCadence(long estTimestamp, EnumSet<EventFlag> eventFlags,
			DataSource dataSource, BigDecimal calculatedCrankCadence) {
		updater.onNewMeasurement(new Measurement(MeasurementType.CRANK_RPM, calculatedCrankCadence.floatValue()));
	}

	@Override
	public void onNewInstantaneousCadence(long estTimestamp, EnumSet<EventFlag> eventFlags,
			DataSource dataSource, int instantaneousCadence) {
		// Not used for now.
	}

	@Override
    public void onNewCalculatedPower(
            final long estTimestamp, final EnumSet<EventFlag> eventFlags,
            final DataSource dataSource,
            final BigDecimal calculatedPower) {
		updater.onNewMeasurement(new Measurement(MeasurementType.POWER, calculatedPower.floatValue()));
	}

	@Override
    public void onNewHeartRateData(final long estTimestamp, EnumSet<EventFlag> eventFlags,
            final int computedHeartRate, final long heartBeatCount,
            final BigDecimal heartBeatEventTime, final DataState dataState) {
		if (dataState != DataState.LIVE_DATA) {
			Log.w("ANT", "HR data state: " + dataState);
			return;
		}

		updater.onNewMeasurement(new Measurement(MeasurementType.HEART_BEAT, computedHeartRate));
	}

	@Override
    public void onNewBatteryStatus(final long estTimestamp,
            EnumSet<EventFlag> eventFlags, final long cumulativeOperatingTime,
            final BigDecimal batteryVoltage, final BatteryStatus batteryStatus,
            final int cumulativeOperatingTimeResolution, final int numberOfBatteries,
            final int batteryIdentifier) {
		// TODO
	}

}
