package dev.jojo.c4hresponder;

import android.app.AlertDialog;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import dev.jojo.c4hresponder.bluetooth.BlunoLibrary;

public class RespondLocation extends BlunoLibrary {

    private GoogleMap mMap;

    private Handler h;
    private AlertDialog ad;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_monitor);

        h = new Handler(this.getMainLooper());

        loadMap();
        displayWaitDialog();
        waitForResponse();
    }

    private void displayWaitDialog(){

        AlertDialog.Builder ab = new AlertDialog.Builder(RespondLocation.this);

        //ab.setTitle("Waiting...");

        View v = this.getLayoutInflater().inflate(R.layout.layout_waiting_for_action,null);

        ab.setView(v);

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

                Toast.makeText(RespondLocation.this, "Checking for coordinates...", Toast.LENGTH_SHORT).show();
                h.postDelayed(this,3000);
            }
        },5000);

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

        // Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
    }
}
