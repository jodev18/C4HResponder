package dev.jojo.c4hresponder;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Vibrator;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;

import dev.jojo.c4hresponder.adafruit_ble.BleManager;
import dev.jojo.c4hresponder.adafruit_ble.BleUtils;
import dev.jojo.c4hresponder.adafruit_ble.UartInterfaceActivity;
import dev.jojo.c4hresponder.bluetooth.BlunoLibrary;
import dev.jojo.c4hresponder.mqtt.MqttManager;
import dev.jojo.c4hresponder.mqtt.MqttSettings;
import dev.jojo.c4hresponder.objects.UartDataChunk;
import dev.jojo.c4hresponder.settings.MqttUartSettingsActivity;

public class RespondLocation extends UartInterfaceActivity implements BleManager.BleManagerListener,
        OnMapReadyCallback,
        MqttManager.MqttManagerListener {

    // Log
    private final static String TAG = RespondLocation.class.getSimpleName();

    private GoogleMap mMap;

    private Handler h;
    private AlertDialog ad;

    private volatile SpannableStringBuilder mTextSpanBuffer;
    private volatile ArrayList<UartDataChunk> mDataBuffer;
    private volatile int mSentBytes;
    private volatile int mReceivedBytes;

    private int maxPacketsToPaintAsText;
    private Handler mUIRefreshTimerHandler = new Handler();
    private Runnable mUIRefreshTimerRunnable = new Runnable() {
        @Override
        public void run() {
            if (isUITimerRunning) {
                updateTextDataUI();
                // Log.d(TAG, "updateDataUI");
                Log.d("RUN","FETCH");
                mUIRefreshTimerHandler.postDelayed(this, 200);
            }
        }
    };

    private boolean isUITimerRunning = false;

    private MqttManager mMqttManager;
//
//    private boolean mIsEchoEnabled;
//    private boolean mIsEolEnabled;
//    private int mEolCharactersId;

    private DataFragment mRetainedDataFragment;

    private TextView tvWaitStat;

    private boolean hasReceivedDistress = false;

    private Runnable detectionTimeClear = new Runnable() {
        @Override
        public void run() {
            hasReceivedDistress = false;
        }
    };


    MediaPlayer mp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_respond_location);

        h = new Handler(this.getMainLooper());
        mBleManager = BleManager.getInstance(this);
        mp = MediaPlayer.create(this, R.raw.siren);

        boolean connect = mBleManager.connect(getApplicationContext(),"FC:DC:B9:AA:CA:3B");
        restoreRetainedDataFragment();

        if(connect){

            // Continue
            onServicesDiscovered();

            // Mqtt init
            mMqttManager = MqttManager.getInstance(this);
            if (MqttSettings.getInstance(this).isConnected()) {
                Toast.makeText(this, "MQTT initialized", Toast.LENGTH_SHORT).show();
                mMqttManager.connectFromSavedSettings(this);
            }
            else{
//                Toast.makeText(this, "MQTT not initialized", Toast.LENGTH_SHORT).show();
            }
            loadMap();
            displayWaitDialog();
            enableBluetooth();
            waitForResponse();
        }
        else {
            Toast.makeText(this, "Failed to connect to device", Toast.LENGTH_SHORT).show();
        }
    }

    private void displayWaitDialog(){

        AlertDialog.Builder ab = new AlertDialog.Builder(RespondLocation.this);

        //ab.setTitle("Waiting...");

        View v = this.getLayoutInflater().inflate(R.layout.layout_waiting_for_action,null);

        tvWaitStat = (TextView)v.findViewById(R.id.tvWaitMessage);

        ab.setView(v);

        ab.setCancelable(false);
        ad = ab.create();

        ad.show();
    }

    private void loadMap(){
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    /**
     * Wait for bluetooth data
     */
    private void waitForResponse(){


        h.postDelayed(new Runnable() {
            @Override
            public void run() {

                //Toast.makeText(RespondLocation.this, "Checking for coordinates...", Toast.LENGTH_SHORT).show();
                h.postDelayed(this,5000);
            }
        },5000);

    }

    private void enableBluetooth(){
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        boolean isEnabled = bluetoothAdapter.isEnabled();

        if(!isEnabled){
            bluetoothAdapter.enable();
        }
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMyLocationEnabled(true);

//        // Add a marker in Sydney and move the camera
//        LatLng sydney = new LatLng(-34, 151);
//        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
//        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
    }

    protected void onResume(){
        super.onResume();

        // Setup listeners
        mBleManager.setBleListener(this);

        mMqttManager.setListener(this);
        updateMqttStatus();

        // Start UI refresh
        //Log.d(TAG, "add ui timer");
        updateUI();

        isUITimerRunning = true;
        mUIRefreshTimerHandler.postDelayed(mUIRefreshTimerRunnable, 200);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onDisconnected() {
        super.onDisconnected();
        Log.d("BLE", "Disconnected. Back to previous activity");
        finish();
    }

    @Override
    public void onServicesDiscovered() {
        super.onServicesDiscovered();
        enableRxNotifications();
    }

    @Override
    protected void onPause() {
        super.onPause();													//onPause Process by BlunoLibrary
    }

    protected void onStop() {
        super.onStop();													//onStop Process by BlunoLibrary
    }

    @Override
    protected void onDestroy() {
        // Disconnect mqtt
        if (mMqttManager != null) {
            mMqttManager.disconnect();
        }

        if(mBleManager != null){
            mBleManager.disconnect();
        }

        // Retain data
        saveRetainedDataFragment();
        super.onDestroy();
        //onDestroy Process by BlunoLibrary
    }

    @Override
    public synchronized void onDataAvailable(BluetoothGattCharacteristic characteristic) {
        super.onDataAvailable(characteristic);
        // UART RX
//        if (characteristic.getService().getUuid().toString().equalsIgnoreCase(UUID_SERVICE)) {
//            if (characteristic.getUuid().toString().equalsIgnoreCase(UUID_RX)) {

                Log.d("DATA_AVAILABLE","RECEIVED DATA");

                final byte[] bytes = characteristic.getValue();
                mReceivedBytes += bytes.length;

                final UartDataChunk dataChunk = new UartDataChunk(System.currentTimeMillis(), UartDataChunk.TRANSFERMODE_RX, bytes);
                mDataBuffer.add(dataChunk);

                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        //if (mIsTimestampDisplayMode) {
//                            final String currentDateTimeString = DateFormat.getTimeInstance().format(new Date(dataChunk.getTimestamp()));
//                            final String formattedData = mShowDataInHexFormat ? BleUtils.bytesToHex2(bytes) : BleUtils.bytesToText(bytes, true);
//
//                            Toast.makeText(UartActivity.this, "Data received " + formattedData, Toast.LENGTH_SHORT).show();
//
//                            mBufferListAdapter.add(new TimestampData("[" + currentDateTimeString + "] RX: " + formattedData, mRxColor));
//                            //mBufferListAdapter.add("[" + currentDateTimeString + "] RX: " + formattedData);
//                            //mBufferListView.smoothScrollToPosition(mBufferListAdapter.getCount() - 1);
//                            mBufferListView.setSelection(mBufferListAdapter.getCount());
                        //}
                        updateUI();
                        ///Toast.makeText(RespondLocation.this, "Received data", Toast.LENGTH_SHORT).show();
                    }
                });

                // MQTT publish to RX
                MqttSettings settings = MqttSettings.getInstance(RespondLocation.this);
                if (settings.isPublishEnabled()) {
                    String topic = settings.getPublishTopic(MqttUartSettingsActivity.kPublishFeed_RX);
                    final int qos = settings.getPublishQos(MqttUartSettingsActivity.kPublishFeed_RX);
                    final String text = BleUtils.bytesToText(bytes, false);
                    mMqttManager.publish(topic, text, qos);
                }
//            }
//        }
    }

//
//    private void updateUI() {
//        mSentBytesTextView.setText(String.format(getString(R.string.uart_sentbytes_format), mSentBytes));
//        mReceivedBytesTextView.setText(String.format(getString(R.string.uart_receivedbytes_format), mReceivedBytes));
//    }

    private int mDataBufferLastSize = 0;

    private void updateTextDataUI() {

        Log.d("UPDATE_TEXT","UI_UPDATE");
        Log.d("BUFFERS",Boolean.valueOf(mDataBufferLastSize != mDataBuffer.size()).toString());
        if (mDataBufferLastSize != mDataBuffer.size()) {

            final int bufferSize = mDataBuffer.size();

            String loaded = "";

            for (int i = mDataBufferLastSize; i < bufferSize; i++) {
                final UartDataChunk dataChunk = mDataBuffer.get(i);
                final boolean isRX = dataChunk.getMode() == UartDataChunk.TRANSFERMODE_RX;
                final byte[] bytes = dataChunk.getData();
                final String formattedData = BleUtils.bytesToText(bytes, true); //mShowDataInHexFormat ? BleUtils.bytesToHex2(bytes) : BleUtils.bytesToText(bytes, true);

                loaded = formattedData;

                addTextToSpanBuffer(mTextSpanBuffer, formattedData);
            }
//            if (bufferSize > maxPacketsToPaintAsText) {
//                mDataBufferLastSize = bufferSize - maxPacketsToPaintAsText;
//                mTextSpanBuffer.clear();
//                addTextToSpanBuffer(mTextSpanBuffer, getString(R.string.uart_text_dataomitted) + "\n");
//            }
//
//            // Log.d(TAG, "update packets: "+(bufferSize-mDataBufferLastSize));
//            for (int i = mDataBufferLastSize; i < bufferSize; i++) {
//                final UartDataChunk dataChunk = mDataBuffer.get(i);
//                final boolean isRX = dataChunk.getMode() == UartDataChunk.TRANSFERMODE_RX;
//                final byte[] bytes = dataChunk.getData();
//                final String formattedData = BleUtils.bytesToText(bytes, true); //mShowDataInHexFormat ? BleUtils.bytesToHex2(bytes) : BleUtils.bytesToText(bytes, true);
//                addTextToSpanBuffer(mTextSpanBuffer, formattedData);
//            }


            mDataBufferLastSize = mDataBuffer.size();
//            mBufferTextView.setText(mTextSpanBuffer);
//            Toast.makeText(this, loaded, Toast.LENGTH_SHORT).show();
            handleCall(loaded);
//            mBufferTextView.setSelection(0, mTextSpanBuffer.length());        // to automatically scroll to the end
        }

    }

    private void handleCall(String data){

        Toast.makeText(getApplicationContext(),data,Toast.LENGTH_LONG).show();

        String[] response = data.split(";");
//        Toast.makeText(this, data, Toast.LENGTH_SHORT).show();

        if(data.length() > 1){

            String distressType = "";

            if(data.contains("E")){

                if(mp.isPlaying()){
                    mp.stop();
                    mp.release();
                    mp = MediaPlayer.create(RespondLocation.this, R.raw.siren);
                }

                String emType = data.split(":")[1];

                if(emType.contains("C")){
                    tvWaitStat.setText("Received a crime distress call. " +
                            "Respond to area within 1 km immediately.");

                    Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                    // Vibrate for 500 milliseconds
                    v.vibrate(500);

                    mp.start();

                    emType = "Crime";
                }
                else if(data.contains("D")){
                    tvWaitStat.setText("Received a generic distress call. " +
                            "Respond to area within 1 km immediately.");

                    Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                    // Vibrate for 500 milliseconds
                    v.vibrate(500);

                    mp.start();

                    emType = "Disaster";
                }
                else if(data.contains("N")){
                    tvWaitStat.setText("Received a natural distress call. " +
                            "Respond to area within 1 km immediately.");

                    Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                    // Vibrate for 500 milliseconds
                    v.vibrate(500);

                    mp.start();

                    emType = "Natural";
                }
                else{
                    tvWaitStat.setText("Received a generic distress call. " +
                            "Respond to area within 1 km immediately.");
                    Toast.makeText(this, "Distress call received!", Toast.LENGTH_SHORT).show();

                    Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                    // Vibrate for 500 milliseconds
                    v.vibrate(2000);

                    mp.start();

                    emType = "Generic";
                }


                ad.setCancelable(true);
                h.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        ad.dismiss();
                    }
                },10000);

                LatLng sydney = new LatLng(14.73458538763225, 121.07056095264852);
                mMap.addMarker(new MarkerOptions().position(sydney).title(emType + " type distress call. Please respond.s"));
                mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(sydney.latitude, sydney.longitude), 22.0f));

//                Toast.makeText(this, emType, Toast.LENGTH_SHORT).show();

                Snackbar.make(tvWaitStat,"Emergency call received.",Snackbar.LENGTH_LONG).show();
            }
            else if(data.contains("W")){
                tvWaitStat.setText("Connected to the device and ready to receive distress call.");
            }
        }
    }

    private void addTextToSpanBuffer(SpannableStringBuilder spanBuffer, String text) {
        spanBuffer.append(text);
    }

    private void updateMqttStatus() {

//        if (mMqttMenuItem == null)
//            return;      // Hack: Sometimes this could have not been initialized so we don't update icons

        MqttManager mqttManager = MqttManager.getInstance(this);
        MqttManager.MqqtConnectionStatus status = mqttManager.getClientStatus();

        if (status == MqttManager.MqqtConnectionStatus.CONNECTING) {
//            final int kConnectingAnimationDrawableIds[] = {R.drawable.mqtt_connecting1, R.drawable.mqtt_connecting2, R.drawable.mqtt_connecting3};
//            mMqttMenuItem.setIcon(kConnectingAnimationDrawableIds[mMqttMenuItemAnimationFrame]);
//            mMqttMenuItemAnimationFrame = (mMqttMenuItemAnimationFrame + 1) % kConnectingAnimationDrawableIds.length;

            Toast.makeText(this, "Connecting to bluetooth hardware...", Toast.LENGTH_SHORT).show();
        } else if (status == MqttManager.MqqtConnectionStatus.CONNECTED) {
//            mMqttMenuItem.setIcon(R.drawable.mqtt_connected);
//            mMqttMenuItemAnimationHandler.removeCallbacks(mMqttMenuItemAnimationRunnable);

            Toast.makeText(this, "Connected to hardware", Toast.LENGTH_SHORT).show();
        } else {
//            mMqttMenuItem.setIcon(R.drawable.mqtt_disconnected);
//            mMqttMenuItemAnimationHandler.removeCallbacks(mMqttMenuItemAnimationRunnable);
        }
    }

    private void uartSendData(String data, boolean wasReceivedFromMqtt) {

        // MQTT publish to TX
        MqttSettings settings = MqttSettings.getInstance(RespondLocation.this);

        if (!wasReceivedFromMqtt) {
            if (settings.isPublishEnabled()) {
                String topic = settings.getPublishTopic(MqttUartSettingsActivity.kPublishFeed_TX);
                final int qos = settings.getPublishQos(MqttUartSettingsActivity.kPublishFeed_TX);
                mMqttManager.publish(topic, data, qos);
            }
        }

        // Add eol
//        if (mIsEolEnabled) {
//            // Add newline character if checked
//            data += getEolCharacters();//"\n";
//        }

        // Send to uart
        if (!wasReceivedFromMqtt || settings.getSubscribeBehaviour() == MqttSettings.kSubscribeBehaviour_Transmit) {
            sendData(data);
            mSentBytes += data.length();
        }

        // Add to current buffer
        byte[] bytes = new byte[0];
        try {
            bytes = data.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        UartDataChunk dataChunk = new UartDataChunk(System.currentTimeMillis(), UartDataChunk.TRANSFERMODE_TX, bytes);
        mDataBuffer.add(dataChunk);

        final String formattedData =  BleUtils.bytesToText(bytes, true);

        //mShowDataInHexFormat ? BleUtils.bytesToHex2(bytes) :;
//        if (mIsTimestampDisplayMode) {
//            final String currentDateTimeString = DateFormat.getTimeInstance().format(new Date(dataChunk.getTimestamp()));
//            mBufferListAdapter.add(new TimestampData("[" + currentDateTimeString + "] TX: " + formattedData, mTxColor));
//            mBufferListView.setSelection(mBufferListAdapter.getCount());
//        }

        Toast.makeText(this, "Formatted data: " +  formattedData, Toast.LENGTH_SHORT).show();

        // Update UI
        updateUI();
    }

    private void updateUI() {
//        mSentBytesTextView.setText(String.format(getString(R.string.uart_sentbytes_format), mSentBytes));
//        mReceivedBytesTextView.setText(String.format(getString(R.string.uart_receivedbytes_format), mReceivedBytes));
    }


//    private String getEolCharacters() {
//        switch (mEolCharactersId) {
//            case 1:
//                return "\r";
//            case 2:
//                return "\n\r";
//            case 3:
//                return "\r\n";
//            default:
//                return "\n";
//        }
//    }
//
//    private int getEolCharactersStringId() {
//        switch (mEolCharactersId) {
//            case 1:
//                return R.string.uart_eolmode_r;
//            case 2:
//                return R.string.uart_eolmode_nr;
//            case 3:
//                return R.string.uart_eolmode_rn;
//            default:
//                return R.string.uart_eolmode_n;
//        }
//    }


    private void restoreRetainedDataFragment() {
        // find the retained fragment
        FragmentManager fm = getFragmentManager();
        mRetainedDataFragment = (DataFragment) fm.findFragmentByTag(TAG);

        if (mRetainedDataFragment == null) {
            // Create
            mRetainedDataFragment = new DataFragment();
            fm.beginTransaction().add(mRetainedDataFragment, TAG).commit();

            mDataBuffer = new ArrayList<>();
            mTextSpanBuffer = new SpannableStringBuilder();
        } else {
            // Restore status
//            mShowDataInHexFormat = mRetainedDataFragment.mShowDataInHexFormat;
            mTextSpanBuffer = mRetainedDataFragment.mTextSpanBuffer;
            mDataBuffer = mRetainedDataFragment.mDataBuffer;
            mSentBytes = mRetainedDataFragment.mSentBytes;
            mReceivedBytes = mRetainedDataFragment.mReceivedBytes;
        }
    }

    private void saveRetainedDataFragment() {
//        mRetainedDataFragment.mShowDataInHexFormat = mShowDataInHexFormat;
        mRetainedDataFragment.mTextSpanBuffer = mTextSpanBuffer;
        mRetainedDataFragment.mDataBuffer = mDataBuffer;
        mRetainedDataFragment.mSentBytes = mSentBytes;
        mRetainedDataFragment.mReceivedBytes = mReceivedBytes;
    }

    @Override
    public void onMqttConnected() {
        Toast.makeText(RespondLocation.this, "MQTT CONNECTED", Toast.LENGTH_SHORT).show();
        updateMqttStatus();
    }

    @Override
    public void onMqttDisconnected() {
        updateMqttStatus();
        Toast.makeText(RespondLocation.this, "MQTT DISCONNECTED", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onMqttMessageArrived(String topic, MqttMessage mqttMessage) {
        final String message = new String(mqttMessage.getPayload());
        Toast.makeText(this, "MESSAGE " + topic, Toast.LENGTH_SHORT).show();
        //Log.d(TAG, "Mqtt messageArrived from topic: " +topic+ " message: "+message);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                uartSendData(message, true);       // Don't republish to mqtt something received from mqtt
            }
        });

    }

    public static class DataFragment extends Fragment {

        private boolean mShowDataInHexFormat;
        private SpannableStringBuilder mTextSpanBuffer;
        private ArrayList<UartDataChunk> mDataBuffer;
        private int mSentBytes;
        private int mReceivedBytes;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setRetainInstance(true);
        }
    }

}


