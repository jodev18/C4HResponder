package dev.jojo.c4hresponder;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Window;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import java.util.List;

import dev.jojo.c4hresponder.core.Globals;

public class C4HSplash extends AppCompatActivity {

    private Handler h;

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
                            startActivity(new Intent(getApplicationContext(),RespondLocation.class));
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
                    startActivity(new Intent(getApplicationContext(),RespondLocation.class));
                    finish();
                }
            },2800);
        }
    }
}
