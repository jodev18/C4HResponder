package dev.jojo.c4hresponder;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import java.util.List;
import java.util.UUID;

import dev.jojo.c4hresponder.adafruit_ble.BleManager;
import dev.jojo.c4hresponder.core.Globals;

public class C4HSplash extends AppCompatActivity {

    private Handler h;
    private static final String kGenericAttributeService = "00001801-0000-1000-8000-00805F9B34FB";
    private static final String kServiceChangedCharacteristic = "00002A05-0000-1000-8000-00805F9B34FB";

    BleManager mBleManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_c4_hsplash);

        final SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(C4HSplash.this);

        h = new Handler(this.getMainLooper());

        //Check if permissions have been checked
        Boolean hasChecked = sp.getBoolean(Globals.PERMISSION_CHECK,false);

        if(!hasChecked){
            Dexter.withActivity(this)
                    .withPermissions(
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION
                    ).withListener(new MultiplePermissionsListener() {
                @Override public void onPermissionsChecked(MultiplePermissionsReport report) {

                    SharedPreferences.Editor e = sp.edit();

                    e.putBoolean(Globals.PERMISSION_CHECK,true);

                    //Save changes
                    e.commit();

                    h.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            //startActivity(new Intent(getApplicationContext(),RespondLocation.class));
                            bluetoothStuff();
                            finish();
                        }
                    },2800);

                }
                @Override public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {/* ... */}
            }).check();
        }
        else{
            h.postDelayed(new Runnable() {
                @Override
                public void run() {
                    //startActivity(new Intent(getApplicationContext(),RespondLocation.class));
                    bluetoothStuff();
                    finish();
                }
            },2800);
        }
    }

    private void bluetoothStuff(){

        mBleManager = BleManager.getInstance(C4HSplash.this);
        // Enable generic attribute service
        final BluetoothGattService genericAttributeService = mBleManager.getGattService(kGenericAttributeService);
        if (genericAttributeService != null) {
            Log.d("BLEH", "kGenericAttributeService found. Check if kServiceChangedCharacteristic exists");

            final UUID characteristicUuid = UUID.fromString(kServiceChangedCharacteristic);
            final BluetoothGattCharacteristic dataCharacteristic = genericAttributeService.getCharacteristic(characteristicUuid);
            if (dataCharacteristic != null) {
                Log.d("BLEH", "kServiceChangedCharacteristic exists. Enable indication");
                mBleManager.enableIndication(genericAttributeService, kServiceChangedCharacteristic, true);
            } else {
                Log.d("BLEH", "Skip enable indications for kServiceChangedCharacteristic. Characteristic not found");
            }
        } else {
            Log.d("BLEH", "Skip enable indications for kServiceChangedCharacteristic. kGenericAttributeService not found");
        }

        // Launch activity

        Log.d("BLEH", "Starting ble");
        Intent intent = new Intent(getApplicationContext(), RespondLocation.class);
//        if (mComponentToStartWhenConnected == BeaconActivity.class && mSelectedDeviceData != null) {
//            intent.putExtra("rssi", mSelectedDeviceData.rssi);
//        }
        startActivityForResult(intent, 3);

    }


}
