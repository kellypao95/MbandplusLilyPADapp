//Copyright (c) Microsoft Corporation All rights reserved.  
// 
//MIT License: 
// 
//Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
//documentation files (the  "Software"), to deal in the Software without restriction, including without limitation
//the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
//to permit persons to whom the Software is furnished to do so, subject to the following conditions: 
// 
//The above copyright notice and this permission notice shall be included in all copies or substantial portions of
//the Software. 
// 
//THE SOFTWARE IS PROVIDED ""AS IS"", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
//TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
//THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
//CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
//IN THE SOFTWARE.
package com.microsoft.band.sdk.heartrate;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import com.microsoft.band.BandClient;
import com.microsoft.band.BandClientManager;
import com.microsoft.band.BandException;
import com.microsoft.band.BandInfo;
import com.microsoft.band.BandIOException;
import com.microsoft.band.ConnectionState;
import com.microsoft.band.UserConsent;
import com.microsoft.band.sensors.BandAccelerometerEvent;
import com.microsoft.band.sensors.BandAccelerometerEventListener;
import com.microsoft.band.sensors.BandAltimeterEvent;
import com.microsoft.band.sensors.BandAltimeterEventListener;
import com.microsoft.band.sensors.BandBarometerEvent;
import com.microsoft.band.sensors.BandBarometerEventListener;
import com.microsoft.band.sensors.BandGsrEvent;
import com.microsoft.band.sensors.BandGsrEventListener;
import com.microsoft.band.sensors.BandHeartRateEvent;
import com.microsoft.band.sensors.BandHeartRateEventListener;
import com.microsoft.band.sensors.BandRRIntervalEvent;
import com.microsoft.band.sensors.BandRRIntervalEventListener;
import com.microsoft.band.sensors.HeartRateConsentListener;
import com.microsoft.band.sensors.SampleRate;
import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Random;
import java.util.UUID;

import static android.app.Activity.RESULT_OK;
import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import static android.bluetooth.BluetoothAdapter.STATE_CONNECTED;
import static android.content.Context.NOTIFICATION_SERVICE;
import static android.content.pm.PackageManager.FEATURE_BLUETOOTH_LE;
import static android.widget.Toast.makeText;
import static java.lang.Float.*;

import android.Manifest;
import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.app.Activity;
import android.os.AsyncTask;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import static android.content.pm.PackageManager.FEATURE_BLUETOOTH_LE;

public class BandHeartRateAppActivity extends Activity {

	//Variables Conexion MBand2
	private BandClient client = null;
	private Button btnStart, btnConsent;
	private TextView txtHeartRate,txtGSR, txtAcc;

	//Variables Conexion LilyPAD
	public ArrayList<BluetoothDevice> mLeDevices;
	private BluetoothAdapter mBluetoothAdapter;
	private BluetoothManager bluetoothManager;
	public BluetoothGatt mBluetoothGatt;
	private BluetoothGattService service;
	private BluetoothGattCharacteristic characteristic;
	private BluetoothGattDescriptor descriptor;
	public byte[] bytes;

	// Control variables
	public boolean mScanning;
	public volatile boolean mConnected;
	public Toast connectingToast;
	public Toast deviceNotFoundToast;
	public Toast serviceNotFoundToast;
	public Toast connectionSuccessfulToast;
	public Toast deviceLostToast;

	public String mMessage;
	public BluetoothDevice myDevice;
	public SaveDatabase saveData;

	private static final int REQUEST_ENABLE_BT = 1;
	public static final String NO_DATA_MESSAGE = "No Data";
	public String TAG = "HEART_RATE_MONITOR";
	private static final int PERMISSION_REQUEST_COARSE_LOCATION = 456;
	public boolean LOCATION_PERMISSION;

	// Stops scanning after 5 seconds or when the device is found.
	private Handler mHandler;
	private static final long SCAN_PERIOD = 5000;

	// Handler to create new thread and show message
	public Handler showMessageHandler = new Handler();
	// Handler to execute delay at the beginning of the scan
	public final Handler delayHandler = new Handler();

	// Notification
	private static final int Notification_ID = 1;
	private static final int CONNECT_PENDING_INTENT_ID = 0;
	private static final int PENDING_INTENT_START_MAIN_ACTIVITY = 1;
	private static final int PENDING_INTENT_CLOSE_APP = 2;
	private static final String Notification_Channel_ID = "1";

	private static final String CLOSE_APP_ACTION = "CLOSE_APP_BROADCAST";
	private static final String OPEN_APP_ACTION = "OPEN_APP_BROADCAST";
	public String currentHearthRateLabel = "Current HR: ";
	public String currentVoltageLabel = "Current Voltage: ";
	public String currentHearthRate = "";
	public NotificationManager notificationManager;
	public Notification notification;

	// LilyPAD parameters
	public static final String MAC_ADDRESS_A = "F8:76:6C:D1:B2:1C"; // Accelerometer
	public static final String MAC_ADDRESS_H = "E8:3D:1C:A4:7C:A2"; // Heart Rate
	private static final UUID UUID_SERVICE = UUID.fromString("0000fe84-0000-1000-8000-00805f9b34fb");
	private static final UUID UUID_CHARACTERISTIC_READ = UUID.fromString("2d30c082-f39f-4ce6-923f-3484ea480596");
	public static final UUID UUID_DESCRIPTOR = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
	private static final String CHARACTERISTIC_STRING = "2d30c082-f39f-4ce6-923f-3484ea480596";



	private BandHeartRateEventListener mHeartRateEventListener = new BandHeartRateEventListener() {
        @Override
        public void onBandHeartRateChanged(final BandHeartRateEvent event) {
            if (event != null) {
            	appendToUI(String.format("Heart Rate = %d beats per minute\n", event.getHeartRate()));
            }
        }
    };

	private BandGsrEventListener mGsrEventListener = new BandGsrEventListener() {
		@Override
		public void onBandGsrChanged(final BandGsrEvent event) {
			if (event != null) {
				appendToUI5(String.format("\n GSR sensor: Resistance = %d kOhms\n", event.getResistance()));
			}
		}
	};

	
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


		txtHeartRate  = (TextView) findViewById(R.id.txtHeartRate);
		txtGSR =  (TextView) findViewById(R.id.txtGSR);
		txtAcc =  (TextView) findViewById(R.id.txtAcc);

        btnStart = (Button) findViewById(R.id.btnStart);
        btnStart.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				OnClickStart();
			}
		});
        
        final WeakReference<Activity> reference = new WeakReference<Activity>(this);
        
        btnConsent = (Button) findViewById(R.id.btnConsent);
        btnConsent.setOnClickListener(new OnClickListener() {
			@SuppressWarnings("unchecked")
            @Override
			public void onClick(View v) {
				new HeartRateConsentTask().execute(reference);
			}
		});

		OnCreateLilyPad();
    }

    public void OnCreateLilyPad(){
		LOCATION_PERMISSION = false;
		mMessage = NO_DATA_MESSAGE;
		mHandler = new Handler();
		mLeDevices = new ArrayList<BluetoothDevice>();
		myDevice = null;
		connectingToast = Toast.makeText(this, "Connecting", Toast.LENGTH_SHORT);
		deviceNotFoundToast = Toast.makeText(this, "Device not found", Toast.LENGTH_SHORT);
		serviceNotFoundToast = Toast.makeText(this, "Service not found on the remote " +
				"device, can not connect", Toast.LENGTH_SHORT);
		connectionSuccessfulToast =  makeText(this, "Successfully Connected", Toast.LENGTH_SHORT);
		deviceLostToast = makeText(this, "Device Lost. Connect again",Toast.LENGTH_SHORT);
		// Check if BLE is supported in the device
		if (isBleSupported(this.getApplicationContext())){
			// Get the bluetooth service into the Bluetooth Manager
			Log.w(TAG, "BLE Supported");
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
				requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
			}else{
				LOCATION_PERMISSION = true;
				Log.w(TAG, "Location Permissions granted");
			}
			this.bluetoothManager = (BluetoothManager) this.getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE);
			mBluetoothAdapter = bluetoothManager.getAdapter();
			// Check if the bluetooth adapter has been initialized and bluetooth is enabled
			if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
				Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
				startActivityForResult(enableBluetoothIntent, REQUEST_ENABLE_BT); // startActivityForResult(activity to start, request code identifier);
			} else {
				Toast.makeText(this, "Bluetooth is currently enabled, please connect...", Toast.LENGTH_SHORT).show();
			}
		}else{
			Toast.makeText(this, "BLE is not supported on this device, closing app...", Toast.LENGTH_SHORT).show();
			closeApp();
		}
	}

    public void OnClickStart(){
		txtHeartRate.setText("");
		txtGSR.setText("");
		new HeartRateSubscriptionTask().execute();
		new GsrSubscriptionTask().execute();
		ConnectOnClick(); //LilyPad

	}

	private void ConnectOnClick() {
		// Check if bluetooth is enable in the phone
		if(mBluetoothAdapter.isEnabled()) {
			// Start after one second to avoid abrupt connections/disconnections -- ON PROCESS YET
				deviceNotFoundToast.cancel();serviceNotFoundToast.cancel();
				connectionSuccessfulToast.cancel(); deviceLostToast.cancel();
				connectingToast.show();
				myDevice = null;
				scanLeDevice(true);
		}else{
			Toast.makeText(this, "Please, enable bluetooth on the device", Toast.LENGTH_SHORT).show();
		}
	}

	private void scanLeDevice(final boolean enable) {
		if (enable) {
			// Stops scanning after a pre-defined scan period.
			mHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					if(mScanning && !mConnected) {
						if (myDevice == null) {
							connectingToast.cancel(); connectionSuccessfulToast.cancel();
							deviceNotFoundToast.show(); deviceLostToast.cancel();
							mConnected = false;
							mBluetoothAdapter.stopLeScan(mLeScanCallback);
						}
					}else {
						if(myDevice != null) {
							mBluetoothAdapter.stopLeScan(mLeScanCallback);
							mScanning = false;
							mConnected = true;
						}
					}
				}
			}, SCAN_PERIOD);
			mHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					mScanning = true;
					mConnected = false;
					mBluetoothAdapter.startLeScan(mLeScanCallback);
				}
			}, 1500);

		}else{
			mScanning = false;
			if(mConnected){
				mBluetoothGatt.disconnect();
			}
			mConnected = false;
			mBluetoothAdapter.stopLeScan(mLeScanCallback);
			myDevice = null;
		}
	}

	private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
		@Override
		public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if(!mLeDevices.contains(device)) {
						mLeDevices.add(device);
					}
					if ((device.getAddress().equals(MAC_ADDRESS_A)) || (device.getAddress().equals(MAC_ADDRESS_H))){
						myDevice = device;
						mBluetoothGatt = myDevice.connectGatt(getApplicationContext(), false, mGattCallback);
						mBluetoothAdapter.stopLeScan(mLeScanCallback);
						connectingToast.cancel();connectionSuccessfulToast.show();
					}else{
						myDevice = null;
					}
				}
			});
		}
	};

	public BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
		@Override
		public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
			super.onConnectionStateChange(gatt, status, newState);
			if (newState == STATE_CONNECTED) {
				gatt.discoverServices();
				mConnected = true;
			}
			if (status == BluetoothGatt.GATT_FAILURE) {
				gatt.disconnect();
				gatt.close();
				gatt = null;
				mConnected = false;
			}
			if (newState == BluetoothProfile.STATE_DISCONNECTED) {
				assert gatt != null;
				gatt.disconnect();
				gatt.close();
				mConnected = false;
			}else if (status != BluetoothGatt.GATT_SUCCESS) {
				assert gatt != null;
				gatt.disconnect();
				gatt.close();
				mConnected = false;
			}
			if(status == BluetoothGatt.GATT_SUCCESS){

			}
		}

		@Override
		public void onServicesDiscovered(BluetoothGatt gatt, int status) {
			super.onServicesDiscovered(gatt, status);
			service = gatt.getService(UUID_SERVICE);
			Log.w(TAG, String.valueOf(service));
			if (service != null) {
				characteristic = service.getCharacteristic(UUID_CHARACTERISTIC_READ);
				Log.w(TAG, String.valueOf(characteristic));
				if (characteristic != null) {
					mConnected = true;
					gatt.setCharacteristicNotification(characteristic, true);
					descriptor = characteristic.getDescriptor(UUID_DESCRIPTOR);
					descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
					gatt.writeDescriptor(descriptor);
					gatt.readCharacteristic(characteristic);
					bytes = characteristic.getValue();
					try {
						Log.w(TAG, new String(bytes,"UTF-8"));
					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					}
				}else{
					mConnected = false;
				}
			}else{
				mConnected = false;
			}
		}

		@Override
		public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
			super.onCharacteristicChanged(gatt, characteristic);
			bytes = characteristic.getValue();
			try {
				String result = new String(bytes, "UTF-8");
				Log.w(TAG, result);
				mMessage = result;
				saveData = new SaveDatabase();
				saveData.execute(mMessage);
				mConnected = true;
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
			super.onCharacteristicRead(gatt, characteristic, status);
			gatt.readCharacteristic(characteristic);
		}
	};

	private class SaveDatabase extends AsyncTask<String, String, String> {

		@Override
		protected String doInBackground(String... data) {
			String rawMessage = data[0];
			String message_Voltage = rawMessage;
			showMessage(message_Voltage);
			return null;
		}

		@Override
		protected void onPostExecute(String Message){
		}
	}

	public void showMessage(final String message_voltage) {
		showMessageHandler.post(new Runnable() {
			public void run(){
				txtAcc.setText("Accelerometer: "+message_voltage);
			}
		});
	}


	private boolean isBleSupported (Context context){
		return context.getPackageManager().hasSystemFeature(FEATURE_BLUETOOTH_LE);
	}

	private void closeApp() {
		Intent startMain = new Intent(Intent.ACTION_MAIN);
		startMain.addCategory(Intent.CATEGORY_HOME);
		startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(startMain);
		finish();
	}

	@Override
	protected void onResume() {
		super.onResume();
		txtHeartRate.setText("");
		txtGSR.setText("");
		txtAcc.setText("");
	}
	
    @Override
	protected void onPause() {
		super.onPause();
		if (client != null) {
			try {
				client.getSensorManager().unregisterHeartRateEventListener(mHeartRateEventListener);
				client.getSensorManager().unregisterGsrEventListener(mGsrEventListener);
			} catch (BandIOException e) {
				appendToUI(e.getMessage());
				appendToUI5(e.getMessage());
			}
		}
	}
	
    @Override
    protected void onDestroy() {
        if (client != null) {
            try {
                client.disconnect().await();
            } catch (InterruptedException e) {
                // Do nothing as this is happening during destroy
            } catch (BandException e) {
                // Do nothing as this is happening during destroy
            }
        }
        super.onDestroy();
    }


    //Subrutinas Microsoft Band
	private class HeartRateSubscriptionTask extends AsyncTask<Void, Void, Void> {
		@Override
		protected Void doInBackground(Void... params) {
			try {
				if (getConnectedBandClient()) {
					if (client.getSensorManager().getCurrentHeartRateConsent() == UserConsent.GRANTED) {
						client.getSensorManager().registerHeartRateEventListener(mHeartRateEventListener);
					} else {
						appendToUI("You have not given this application consent to access heart rate data yet."
								+ " Please press the Heart Rate Consent button.\n");
					}
				} else {
					appendToUI("Band isn't connected. Please make sure bluetooth is on and the band is in range.\n");
				}
			} catch (BandException e) {
				String exceptionMessage="";
				switch (e.getErrorType()) {
				case UNSUPPORTED_SDK_VERSION_ERROR:
					exceptionMessage = "Microsoft Health BandService doesn't support your SDK Version. Please update to latest SDK.\n";
					break;
				case SERVICE_ERROR:
					exceptionMessage = "Microsoft Health BandService is not available. Please make sure Microsoft Health is installed and that you have the correct permissions.\n";
					break;
				default:
					exceptionMessage = "Unknown error occured: " + e.getMessage() + "\n";
					break;
				}
				appendToUI(exceptionMessage);

			} catch (Exception e) {
				appendToUI(e.getMessage());
			}
			return null;
		}
	}
	
	private class HeartRateConsentTask extends AsyncTask<WeakReference<Activity>, Void, Void> {
		@Override
		protected Void doInBackground(WeakReference<Activity>... params) {
			try {
				if (getConnectedBandClient()) {
					
					if (params[0].get() != null) {
						client.getSensorManager().requestHeartRateConsent(params[0].get(), new HeartRateConsentListener() {
							@Override
							public void userAccepted(boolean consentGiven) {
							}
					    });
					}
				} else {
					appendToUI("Band isn't connected. Please make sure bluetooth is on and the band is in range.\n");
				}
			} catch (BandException e) {
				String exceptionMessage="";
				switch (e.getErrorType()) {
				case UNSUPPORTED_SDK_VERSION_ERROR:
					exceptionMessage = "Microsoft Health BandService doesn't support your SDK Version. Please update to latest SDK.\n";
					break;
				case SERVICE_ERROR:
					exceptionMessage = "Microsoft Health BandService is not available. Please make sure Microsoft Health is installed and that you have the correct permissions.\n";
					break;
				default:
					exceptionMessage = "Unknown error occured: " + e.getMessage() + "\n";
					break;
				}
				appendToUI(exceptionMessage);

			} catch (Exception e) {
				appendToUI(e.getMessage());
			}
			return null;
		}
	}

	private class GsrSubscriptionTask extends AsyncTask<Void, Void, Void> {
		@Override
		protected Void doInBackground(Void... params) {
			try {
				if (getConnectedBandClient()) {
					int hardwareVersion = Integer.parseInt(client.getHardwareVersion().await());
					if (hardwareVersion >= 20) {
						appendToUI5("Band is connected.\n");
						client.getSensorManager().registerGsrEventListener(mGsrEventListener);
					} else {
						appendToUI5("The Gsr sensor is not supported with your Band version. Microsoft Band 2 is required.\n");
					}
				} else {
					appendToUI5("Band isn't connected. Please make sure bluetooth is on and the band is in range.\n");
				}
			} catch (BandException e) {
				String exceptionMessage="";
				switch (e.getErrorType()) {
					case UNSUPPORTED_SDK_VERSION_ERROR:
						exceptionMessage = "Microsoft Health BandService doesn't support your SDK Version. Please update to latest SDK.\n";
						break;
					case SERVICE_ERROR:
						exceptionMessage = "Microsoft Health BandService is not available. Please make sure Microsoft Health is installed and that you have the correct permissions.\n";
						break;
					default:
						exceptionMessage = "Unknown error occured: " + e.getMessage() + "\n";
						break;
				}
				appendToUI5(exceptionMessage);

			} catch (Exception e) {
				appendToUI5(e.getMessage());
			}
			return null;
		}
	}

	private void appendToUI(final String string) {
		this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
				txtHeartRate.setText(string);
            }
        });
	}

	private void appendToUI5(final String string) {
		this.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				txtGSR.setText(string);
			}
		});
	}

	private boolean getConnectedBandClient() throws InterruptedException, BandException {
		if (client == null) {
			BandInfo[] devices = BandClientManager.getInstance().getPairedBands();
			if (devices.length == 0) {
				appendToUI("Band isn't paired with your phone.\n");
				return false;
			}
			client = BandClientManager.getInstance().create(getBaseContext(), devices[0]);
		} else if (ConnectionState.CONNECTED == client.getConnectionState()) {
			return true;
		}
		
		appendToUI("Band is connecting...\n");

		return ConnectionState.CONNECTED == client.connect().await();
	}
}

