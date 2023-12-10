package com.xr3trx.sensores;

import static android.graphics.Color.BLACK;
import static android.graphics.Color.GREEN;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.nio.charset.StandardCharsets;

public class MainActivity2 extends AppCompatActivity implements SensorEventListener {

    //mapa
    MapView map = null;
    private LocationManager locationManager;
    private GeoPoint currentLocationPoint, nextLocationPoint;
    //IMapController mapController = map.getController();

    //Marker
    private Marker inicioMarker;
    private Handler markerHandler = new Handler();
    private final int INTERVALO_MARCADORES = 60000;
    private boolean marcadoresSecundariosActivos = false; //Necesario para controlar los markers secundarios


    //Sensores
    private TextView textViewStepCounter;
    private SensorManager sensorManager;
    private Sensor stepCounter;
    private boolean isCounterSensorPresent;
    int stepCount = 0;
    private Button btnInicio, btnFinal;

    //Permisos
    String[] permissions = {
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.INTERNET,
            android.Manifest.permission.ACCESS_NETWORK_STATE,
            android.Manifest.permission.ACTIVITY_RECOGNITION,
            android.Manifest.permission.BODY_SENSORS,
            android.Manifest.permission.WAKE_LOCK
    };
    boolean allPermissionsGranted = true;

    //Extra
    private String username, nombre;

    //MQTT Conexion
    //mqtt://aplicaciona:utMP5rJYaEHwi3mb@aplicaciona.cloud.shiftr.io
    //mqtt://asdfg:sivboFqSlGl9ZoOu@asdfg.cloud.shiftr.io
    static String MQTTHOST = "tcp://asdfg.cloud.shiftr.io:1883";
    static String MQTTUSER = "asdfg";
    static String MQTTPASS = "sivboFqSlGl9ZoOu";

    static String TOPIC = "INICIAR";
    static String TOPIC_MSG_ON = "Caminando";
    static String TOPIC_MSG_OFF = "Detenido";
    Boolean permisoPublicar;

    MqttAndroidClient cliente;
    MqttConnectOptions opciones;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        Intent intent = getIntent();
        if(intent != null && intent.hasExtra("USUARIO") && intent.hasExtra("NOMBRE")) {
            username = intent.getStringExtra("USUARIO");
            nombre = intent.getStringExtra("NOMBRE");
        }

        Toast.makeText(this, "BIENVENID@ "+nombre+".", Toast.LENGTH_SHORT).show();

        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(MainActivity2.this, permission) != PackageManager.PERMISSION_GRANTED) {
                allPermissionsGranted = false;
                break;
            }
        }
        if (!allPermissionsGranted) {
            ActivityCompat.requestPermissions(MainActivity2.this, permissions, 100);
        }

        btnInicio = findViewById(R.id.btnInicio);
        btnInicio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(MainActivity2.this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(MainActivity2.this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity2.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION}, 100);
                } else {
                    Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    if (location != null) {
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

                        enviarMensaje(TOPIC, TOPIC_MSG_ON);

                        if (!marcadoresSecundariosActivos) {
                            iniciarMarcadoresSecundarios();
                        }
                    }
                }
            }
        });

        textViewStepCounter = findViewById(R.id.textViewCounter);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        if(sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)!=null){
            stepCounter = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
            isCounterSensorPresent = true;
        } else {
            textViewStepCounter.setText("Sensor no disponible");
            isCounterSensorPresent = false;
        }

        // MAPA boton de final
        btnFinal = findViewById(R.id.btnFinal);
        btnFinal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (inicioMarker != null) {
                    if (ContextCompat.checkSelfPermission(MainActivity2.this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(MainActivity2.this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(MainActivity2.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION}, 100);
                    } else {
                        Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                        if (location != null) {
                            double latitude = location.getLatitude();
                            double longitude = location.getLongitude();

                            currentLocationPoint = new GeoPoint(latitude, longitude);

                            //GeoPoint finalPoint = new GeoPoint(-33.45209486172414, -70.65359366103158); // Parque almagro TEST
                            GeoPoint finalPoint = new GeoPoint(currentLocationPoint);

                            IMapController mapController1 = map.getController();
                            mapController1.setZoom(18);
                            mapController1.setCenter(currentLocationPoint);

                            Marker finalMarker = new Marker(map);
                            finalMarker.setPosition(finalPoint);
                            finalMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                            finalMarker.setTitle("FINAL");
                            finalMarker.setSnippet("final de ruta");

                            map.getOverlays().add(finalMarker);

                            enviarMensaje(TOPIC, TOPIC_MSG_OFF);

                            detenerMarcadoresSecundarios();
                            marcadoresSecundariosActivos = false;

                            FirebaseDatabase db = FirebaseDatabase.getInstance();
                            DatabaseReference dbref = db.getReference(Usuario.class.getSimpleName());

                            dbref.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {

                                    boolean resUsuario = false;
                                    for(DataSnapshot u : snapshot.getChildren()){

                                        if(u.child("usuario").getValue().toString().equals(username)){
                                            resUsuario = true;
                                            int pasosComparar = Integer.parseInt(u.child("pasosRegistrados").getValue(String.class));
                                            int pasosNuevosComparar = Integer.parseInt(textViewStepCounter.getText().toString());

                                            Toast.makeText(MainActivity2.this, "Has dado "+textViewStepCounter.getText()+" pasos", Toast.LENGTH_SHORT).show();

                                            if(pasosComparar < pasosNuevosComparar){
                                                u.getRef().child("pasosRegistrados").setValue(textViewStepCounter.getText());
                                                Toast.makeText(MainActivity2.this, "Acabas de establecer un nuevo record de pasos!", Toast.LENGTH_SHORT).show();
                                                Toast.makeText(MainActivity2.this, "Tu nuevo record es: "+textViewStepCounter.getText()+".", Toast.LENGTH_SHORT).show();
                                                break;
                                            } else {
                                                Toast.makeText(MainActivity2.this, "Esta vez no pudiste superar tu record.", Toast.LENGTH_SHORT).show();
                                                Toast.makeText(MainActivity2.this, "Sigue esforzandote!", Toast.LENGTH_SHORT).show();
                                            }
                                        } //if
                                    }//for
                                    if(resUsuario == false){
                                        Toast.makeText(MainActivity2.this, "No se han registrado tus pasos en la BD", Toast.LENGTH_SHORT).show();
                                    }//if(resUsuario)
                                }//onDataChange

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {

                                }
                            });
                        }
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

        // MAPA default view point

        IMapController mapController = map.getController();  // Era el original Lo movi a la lista de atributos inicializados
        mapController.setZoom(2);
        GeoPoint startPoint = new GeoPoint(0, 0);     //Era el original lo modifiqué para poder iniciar en la ubicacion actual
        //GeoPoint startPoint = new GeoPoint(currentLocationPoint);       // Corresponde a la ubicacion que entrega el boton
        mapController.setCenter(startPoint);

        connectBroker();
    }

    //MQTT
    private void connectBroker(){
        this.cliente = new MqttAndroidClient(this.getApplicationContext(), MQTTHOST, this.username);
        this.opciones = new MqttConnectOptions();
        this.opciones.setUserName(MQTTUSER);
        this.opciones.setPassword(MQTTPASS.toCharArray());

        try {
            IMqttToken token = this.cliente.connect(opciones);
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Toast.makeText(getBaseContext(), "CONECTADO", Toast.LENGTH_SHORT).show();
                    suscribirseTopic();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Toast.makeText(getBaseContext(), "CONEXION FALLIDA", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (MqttException e) {
            throw new RuntimeException(e);
        }
    }

    //SENSOR contador de pasos

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if(sensorEvent.sensor == stepCounter){
            stepCount = (int) sensorEvent.values[0];
            textViewStepCounter.setText(String.valueOf(stepCount));
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    public void onResume(){
        super.onResume();
        if(sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)!=null)
            sensorManager.registerListener(this, stepCounter, SensorManager.SENSOR_DELAY_FASTEST);

        // MAPA
        map.onResume();
    }

    @Override
    public void onPause(){
        super.onPause();
        if(sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)!=null)
            sensorManager.unregisterListener(this, stepCounter);

        // MAPA
        map.onPause();
    }

    private void reiniciarContadorPasos(){
        textViewStepCounter.setText("0");

        if(sensorManager != null && stepCounter != null){
            sensorManager.registerListener(MainActivity2.this, stepCounter, sensorManager.SENSOR_DELAY_FASTEST);
        }
    }

    // Markers secundarios + HILOS
    private void iniciarMarcadoresSecundarios() {
        marcadoresSecundariosActivos = true;

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                ejecutarActualizacionMarcadores();
            }
        }, INTERVALO_MARCADORES);
    }

    private void ejecutarActualizacionMarcadores() {

        if(inicioMarker != null){
            if(ContextCompat.checkSelfPermission(MainActivity2.this, android.Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(MainActivity2.this, android.Manifest.permission.ACCESS_COARSE_LOCATION)!= PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(MainActivity2.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 100);
            } else {
                Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if (location != null) {
                    double latitudeN = location.getLatitude();
                    double longitudeN = location.getLongitude();

                    nextLocationPoint = new GeoPoint(latitudeN, longitudeN);
                    Marker nextMarker = new Marker(map);

                    IMapController mapController2 = map.getController();
                    mapController2.setZoom(18);
                    mapController2.setCenter(nextLocationPoint);

                    nextMarker.setPosition(nextLocationPoint);
                    nextMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                    nextMarker.setTitle("RUTA");
                    nextMarker.setSnippet("en ruta");

                    map.getOverlays().add(nextMarker);
                    Toast.makeText(getApplicationContext(), "Se agrego un marcador a la ruta", Toast.LENGTH_SHORT).show();

                }
            }
        } else {
            marcadoresSecundariosActivos = false;
        }


        if (marcadoresSecundariosActivos) {
            iniciarMarcadoresSecundarios();
        }
    }

    private void detenerMarcadoresSecundarios() {
        marcadoresSecundariosActivos = false;
    }

    private void enviarMensaje(String topic, String msg){
        checkConexion();
        if(permisoPublicar){
            try{
                int qos = 0;
                this.cliente.publish(topic, msg.getBytes(), qos, false);
                Toast.makeText(this, topic + " : " + msg, Toast.LENGTH_SHORT).show();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    private void checkConexion(){
        if(this.cliente.isConnected()){
            this.permisoPublicar = true;
        }else{
            this.permisoPublicar = false;
            connectBroker();
        }
    }

    private void suscribirseTopic(){
        try{
            this.cliente.subscribe(TOPIC, 0);
        }catch(MqttSecurityException e){
            e.printStackTrace();
        }catch(MqttException e){
            e.printStackTrace();
        }

        this.cliente.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                Toast.makeText(MainActivity2.this, "MQTT desconectado", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                if(topic.matches(TOPIC)){
                    String msg = new String(message.getPayload());
                    if(msg.matches(TOPIC_MSG_ON)){
                        textViewStepCounter.setBackgroundColor(GREEN);
                    }
                    if(msg.matches(TOPIC_MSG_OFF)){
                        textViewStepCounter.setBackgroundColor(BLACK);
                    }
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });
    }

}
