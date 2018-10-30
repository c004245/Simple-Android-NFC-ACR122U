package com.example.hyunwook.testreaderacr122u;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import com.acs.smartcard.Features;
import com.acs.smartcard.Reader;
import com.acs.smartcard.ReaderException;

public class MainActivity extends AppCompatActivity {

    private Button mListButton; //연결된 장치 리스트 확인
    private Button mOpenButton; //장치 오픈

    private UsbManager mManager;

    private Reader mReader;

    private ArrayAdapter<String> mReaderAdapter; //장치 목록 어댑
    private PendingIntent mPermissionIntent;

    private Spinner mReaderSpinner;

    private Features mFeatures = new Features();
    String deviceName2;
    private static final String[] stateStrings = { "Unknown", "Absent",
            "Present", "Swallowed", "Powered", "Negotiable", "Specific" };

    static final String TAG = MainActivity.class.getSimpleName();
    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";

    public boolean isOpened = false; //퇴근 장치 오픈되면 true

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        //Open
        mOpenButton = (Button) findViewById(R.id.main_button_open);
        mOpenButton.setOnClickListener(l -> {

        });

        //Initialize reader
        mReader = new Reader(mManager);
        mReader.setOnStateChangeListener(new Reader.OnStateChangeListener() {
            @Override
            public void onStateChange(int slotNum, int prevState, int currState) {

                if (prevState < Reader.CARD_UNKNOWN
                       || prevState > Reader.CARD_SPECIFIC) {
                    prevState = Reader.CARD_UNKNOWN;
                }

                if (currState < Reader.CARD_UNKNOWN
                        || currState > Reader.CARD_SPECIFIC) {
                    currState = Reader.CARD_UNKNOWN;
                }

                // Create output string
                final String outputString = "Slot " + slotNum + ": "
                        + stateStrings[prevState] + " -> "
                        + stateStrings[currState];

                if (currState == Reader.CARD_PRESENT) {
                    Log.d(TAG, "Ready to read...");
                        final byte[] command = {(byte) 0xFF, (byte) 0xCA, (byte) 0x00, (byte) 0x00, (byte) 0x00};

                        final byte[] response = new byte[256];
                        try {
                            int byteCount = mReader.control(slotNum, Reader.IOCTL_CCID_ESCAPE,
                                    command, command.length, response, response.length);

                            //get UID
                            StringBuffer uid = new StringBuffer();

                            for (int i = 0; i < (byteCount -2); i++) {
                                uid.append(String.format("%02X", response[i]));

                                if (i < byteCount -3) {

                                }
                                try {
                                    Log.d(TAG, "Data --->" + Long.parseLong(uid.toString(), 16));
                                } catch (NumberFormatException e) {
                                    Looper.prepare();
                                    Toast.makeText(getApplicationContext(), "인식 실패, 다시 찍어주세요.", Toast.LENGTH_LONG);
                                    Looper.loop();
                                }
                            }

                         } catch (ReaderException e) {
                            e.printStackTrace();
                        }
                }

            }
        });

        //Register receiver for USB permission
        mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(mReceiver, filter);

        //Initialize reader spinner
        mReaderAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);

        for (UsbDevice device : mManager.getDeviceList().values()) {
            if (mReader.isSupported(device)) {
                mReaderAdapter.add(device.getDeviceName());
                Log.d(TAG, "device name -->" + device.getDeviceName());

            }
        }
        mReaderSpinner = (Spinner) findViewById(R.id.main_spinner_reader);
        mReaderSpinner.setAdapter(mReaderAdapter);

        boolean requested = false;

        //disable open button
        mOpenButton.setEnabled(false);
//        String deviceName = (String) mReaderSpinner.getSelectedItem();

        Log.d(TAG, "count ->" + mReaderAdapter.getCount());
        int deviceCount = mReaderAdapter.getCount();
        if (deviceCount == 0) {
            Toast.makeText(getApplicationContext(), "카드 리더기 오류, 재 연결 해주세요.", Toast.LENGTH_LONG).show();
        } else if (deviceCount == 1) {
            String deviceName = mReaderAdapter.getItem(0);

            Log.d(TAG, "Open Test --->" + deviceName); //선택된 장치
            if (deviceName != null) {

                for (UsbDevice device : mManager.getDeviceList().values()) {

                    //device name is found
                    if (deviceName.equals(device.getDeviceName())) {

                        //Request permission
                        mManager.requestPermission(device, mPermissionIntent);

                        requested = true;
                        break;
                    }
                }
            }

            if (!requested) {
                //enable open button
                mOpenButton.setEnabled(true);
            }
        } else if (deviceCount == 2) {
            //2개
            String deviceName = mReaderAdapter.getItem(0);
            deviceName2 = mReaderAdapter.getItem(1);

            Log.d(TAG, "Open Test2 --->" + deviceName); // 선택된 장치
            Log.d(TAG, "Open Test2 --->" + deviceName2); // 선택된 장치

            if (deviceName != null && deviceName2 != null) {
                for (UsbDevice device : mManager.getDeviceList().values()) {
                    Log.d(TAG, "device ----->" + device.getDeviceName());

                    //device name is found.
                    if (deviceName.equals(device.getDeviceName())) {
                        //Request permission
                        Log.d(TAG, "Device 1 -> " +device.getDeviceName());
                        mManager.requestPermission(device, mPermissionIntent);

                        requested = true;
                        break;
                    }

                  /*  if (deviceName2.equals(device.getDeviceName())) {
                        //Request permission
                        Log.d(TAG, "Device 2 -> " +device.getDeviceName());
                        mManager.requestPermission(device, mPermissionIntent);

                        requested = true;
                        break;
                    }*/
                }
            }

        }


        mListButton = (Button) findViewById(R.id.main_button_list);
        mListButton.setOnClickListener(l -> {
            mReaderAdapter.clear();
            for (UsbDevice device : mManager.getDeviceList().values()) {
                if (mReader.isSupported(device)) {
                    mReaderAdapter.add(device.getDeviceName());
                    Log.d(TAG, "list device getname ->" + device.getDeviceName());
                }
            }

        });




    }

    //USB Receiver
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {

                        if (device != null) {
                            //Open Reader
                            Log.d(TAG, "OpenTask ...>" + device.getDeviceName());
                            new OpenTask().execute(device);
                        }

                    } else {
                        mOpenButton.setEnabled(true);
                    }
                }
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                synchronized (this) {
                    //update reader list
                    Log.d(TAG, "update reader list...");
                    mReaderAdapter.clear();

                    for (UsbDevice device : mManager.getDeviceList().values()) {
                        if (mReader.isSupported(device)) {
                            mReaderAdapter.add(device.getDeviceName());
                        }
                    }

                    UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    if (device != null && device.equals(mReader.getDevice())) {
                        new CloseTask().execute();

                    }
                }
            }
        }
    };

    //Reader Device OpenTask
    private class OpenTask extends AsyncTask<UsbDevice, Void, Exception> {

        @Override
        protected Exception doInBackground(UsbDevice... params) {
            Exception result = null;

            try {
                mReader.open(params[0]);
            } catch (Exception e) {
                result = e;
            }

            return result;
        }
        @Override
        protected void onPostExecute(Exception result) {
            if (result != null) {

            } else {
                Log.d(TAG, "Reader Name -->" + mReader.getReaderName());

                int numSlots = mReader.getNumSlots();
                Log.d(TAG, "Number of slots: " + numSlots);

                //Add Slot items;
//                mslo
                // Remove all control codes
//                mFeatures.clear();

                Log.d(TAG, "next device -> " + deviceName2);
                Log.d(TAG, "isOpened state -> " + isOpened);

                if (!isOpened) {

                    for (UsbDevice device : mManager.getDeviceList().values()) {

                        if (deviceName2.equals(device.getDeviceName())) {
                            //Request permission
                            Log.d(TAG, "Device 2 -> " + device.getDeviceName());
                            mManager.requestPermission(device, mPermissionIntent);

                            break;
                        }
                        isOpened = true;
                        mManager.requestPermission(device, mPermissionIntent);

                    }
                }
            }
        }
    }

    private class CloseTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            mReader.close();
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            mOpenButton.setEnabled(true);
        }
    }

    @Override
    protected void onDestroy() {
        mReader.close();

        unregisterReceiver(mReceiver);
        super.onDestroy();
    }
}
