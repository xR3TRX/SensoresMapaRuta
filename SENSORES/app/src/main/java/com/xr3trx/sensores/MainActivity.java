package com.xr3trx.sensores;

import static androidx.core.location.LocationManagerCompat.requestLocationUpdates;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

//mapa
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import android.location.LocationManager;

//marker
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.Marker;


public class MainActivity extends AppCompatActivity implements SensorEventListener {

    //mapa
    MapView map = null;
    private Button btnUpdate;
    private LocationManager locationManager;
    private GeoPoint currentLocationPoint;
    //IMapController mapController = map.getController();

    //Marker
    private Marker inicioMarker;
    private Handler markerHandler = new Handler();
    private final int INTERVALO_MARCADORES = 60000;
    private boolean marcadoresSecundariosActivos = false; //Necesario para controlar los markers secundarios


    //Sensores
    private TextView textViewStepCounter;
    private SensorManager sensorManager;
    private Sensor stepCounter, stepDetector;
    private boolean isCounterSensorPresent, isDetectorSensorPresent;
    int stepCount = 0, stepDetect = 0;
    private Button btnForzado;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        btnForzado = findViewById(R.id.btnForzado);
        btnForzado.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){

                reiniciarContadorPasos();
                // Forzar el conteo del sensor TEST
                /* if (sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null) {
                    sensorManager.unregisterListener(MainActivity.this, stepCounter);
                    sensorManager.registerListener(MainActivity.this, stepCounter, SensorManager.SENSOR_DELAY_FASTEST);
                }
                //if (sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR) != null) {
                //    sensorManager.unregisterListener(MainActivity.this, stepDetector);
                //    sensorManager.registerListener(MainActivity.this, stepDetector, SensorManager.SENSOR_DELAY_FASTEST);
                //}

                 */
            }
        });

        textViewStepCounter = findViewById(R.id.textViewCounter);
        //textViewStepDetector = findViewById(R.id.textViewDetector);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        if(sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)!=null){
            stepCounter = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
            isCounterSensorPresent = true;
        } else {
            textViewStepCounter.setText("Sensor no está presente");
            isCounterSensorPresent = false;
        }
        //if(sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)!=null){
        //    stepDetector = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
        //    isDetectorSensorPresent = true;
        //} else {
        //    textViewStepDetector.setText("Sensor no está presente");
        //    isDetectorSensorPresent = false;
        //}

        // MAPA boton de INICIO y subprocesos
        btnUpdate = findViewById(R.id.btnUpdate);
        btnUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION)!= PackageManager.PERMISSION_GRANTED){
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 100);
                } else {
                    Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    if(location != null) {
                        double latitude = location.getLatitude();
                        double longitude = location.getLongitude();

                        currentLocationPoint = new GeoPoint(latitude, longitude);
                        IMapController mapController1 = map.getController();
                        mapController1.setZoom(18);
                        mapController1.setCenter(currentLocationPoint);

                        GeoPoint inicioPoint = new GeoPoint(currentLocationPoint);

                        inicioMarker = new Marker(map);
                        inicioMarker.setPosition(inicioPoint);

                        // Modificar el marker para identificar el primero de los demás
                        inicioMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                        inicioMarker.setTitle("INICIO");
                        inicioMarker.setSnippet("inicio de ruta");

                        map.getOverlays().add(inicioMarker);

                        if(!marcadoresSecundariosActivos){
                            iniciarMarcadoresSecundarios();
                        }
                    } else {
                        Marker finalMarker = new Marker(map);
                        finalMarker.setPosition(currentLocationPoint);
                        finalMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                        finalMarker.setTitle("FINAL");
                        finalMarker.setSnippet("final de ruta");

                        map.getOverlays().add(finalMarker);

                        detenerMarcadoresSecundarios();
                    }
                }
            }
        });

        // MAPA


        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));

        map = (MapView) findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);

        // MAPA controles
        map.setBuiltInZoomControls(true);
        map.setMultiTouchControls(true);

        // MAPA default view point / Agrega 2 librerias a revisar luego

        IMapController mapController = map.getController();  // Era el original Lo movi a la lista de atributos inicializados
        mapController.setZoom(2);
        GeoPoint startPoint = new GeoPoint(0, 0);     //Era el original lo modifiqué para poder iniciar en la ubicacion actual
        //GeoPoint startPoint = new GeoPoint(currentLocationPoint);       // Corresponde a la ubicacion que entrega el boton
        mapController.setCenter(startPoint);


    }

    // MAPA



    //SENSOR contador de pasos

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if(sensorEvent.sensor == stepCounter){
            stepCount = (int) sensorEvent.values[0];
            textViewStepCounter.setText(String.valueOf(stepCount));
        } //else if(sensorEvent.sensor == stepDetector){
        //    stepDetect = (int) sensorEvent.values[0];
        //    textViewStepDetector.setText(String.valueOf(stepDetect));
        //}
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    public void onResume(){
        super.onResume();
        if(sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)!=null)
            sensorManager.registerListener(this, stepCounter, SensorManager.SENSOR_DELAY_FASTEST);
        //if(sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)!=null)
        //    sensorManager.registerListener(this, stepDetector, SensorManager.SENSOR_DELAY_FASTEST);

        // MAPA
        map.onResume();
    }

    @Override
    public void onPause(){
        super.onPause();
        if(sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)!=null)
            sensorManager.unregisterListener(this, stepCounter);
        //if(sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)!=null)
        //    sensorManager.unregisterListener(this, stepDetector);

        // MAPA
        map.onPause();
    }

    private void reiniciarContadorPasos(){
        textViewStepCounter.setText("0");

        if(sensorManager != null && stepCounter != null){
            sensorManager.registerListener(MainActivity.this, stepCounter, sensorManager.SENSOR_DELAY_FASTEST);
        }
    }

    // Markers secundarios + HILOS
    private void iniciarMarcadoresSecundarios() {
        marcadoresSecundariosActivos = true;
        Thread marcadoresThread = new Thread(new Runnable(){
            @Override
            public void run(){
                while(marcadoresSecundariosActivos) {
                    try{
                        runOnUiThread(new Runnable(){
                            @Override
                            public void run(){
                                if(inicioMarker != null){
                                    Marker nextMarker = new Marker(map);
                                    nextMarker.setPosition(currentLocationPoint);
                                    nextMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                                    nextMarker.setTitle("RUTA");
                                    nextMarker.setSnippet("en ruta");

                                    map.getOverlays().add(nextMarker);
                                } else {
                                    marcadoresSecundariosActivos = false;
                                }
                            }
                        });

                        Thread.sleep(INTERVALO_MARCADORES);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        marcadoresThread.start();
    }

    private void detenerMarcadoresSecundarios() {
        marcadoresSecundariosActivos = false;
    }

}

/*
btnUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION)!= PackageManager.PERMISSION_GRANTED){
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 100);
                } else {
                    Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    if(location != null) {
                        double latitude = location.getLatitude();
                        double longitude = location.getLongitude();

                        currentLocationPoint = new GeoPoint(latitude, longitude);
                        IMapController mapController1 = map.getController();
                        mapController1.setZoom(18);
                        mapController1.setCenter(currentLocationPoint);

                        GeoPoint inicioPoint = new GeoPoint(currentLocationPoint);

                        inicioMarker = new Marker(map);
                        inicioMarker.setPosition(inicioPoint);

                        // Modificar el marker para identificar el primero de los demás
                        inicioMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                        inicioMarker.setTitle("INICIO");
                        inicioMarker.setSnippet("inicio de ruta");

                        map.getOverlays().add(inicioMarker);

                        if(inicioMarker != null) {
                            GeoPoint finalPoint = new GeoPoint(-33.45209486172414, -70.65359366103158); // Parque almagro para pruebas de marker
                            //GeoPoint finalPoint = new GeoPoint(currentLocationPoint);
                            Marker nextMarker = new Marker(map);
                            nextMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                            nextMarker.setTitle("RUTA");
                            nextMarker.setSnippet("ruta");

                            nextMarker.setPosition(finalPoint);
                            map.getOverlays().add(nextMarker);

                            //Modificacion al boton
                            btnUpdate.setText("Marcardo forzado");
                        }

                    }
                }
            }
        });
 */