package com.xr3trx.sensores;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class Signup extends AppCompatActivity {

    private TextView txtCrearUser, txtLogin;
    private EditText editTxtNombre, editTxtUsuario, editTxtPassword, editTxtConfirmar;
    private Button btnSignup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        txtLogin = findViewById(R.id.txtLogin);
        editTxtNombre = findViewById(R.id.editTxtNombre);
        editTxtUsuario = findViewById(R.id.editTxtUsuario);
        editTxtPassword = findViewById(R.id.editTxtPassword);
        editTxtConfirmar = findViewById(R.id.editTextConfirmar);

        btnSignup = findViewById(R.id.btnLogin);

        botonRegistrar();
    } //onCreate

    private void botonRegistrar(){
        btnSignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(editTxtNombre.getText().toString().trim().isEmpty()
                || editTxtUsuario.getText().toString().trim().isEmpty()
                || editTxtPassword.getText().toString().trim().isEmpty()
                || editTxtConfirmar.getText().toString().trim().isEmpty()){

                    ocultarTeclado();
                    Toast.makeText(Signup.this, "Complete los campos faltantes", Toast.LENGTH_SHORT).show();
                } else {
                    if(editTxtPassword.getText().toString().equals(editTxtConfirmar.getText().toString())) {
                        String nombre = editTxtNombre.getText().toString();
                        String usuario = editTxtUsuario.getText().toString();
                        String password = editTxtPassword.getText().toString();

                        String pasosregistrados = "0";

                        FirebaseDatabase db = FirebaseDatabase.getInstance();
                        DatabaseReference dbref = db.getReference(Usuario.class.getSimpleName());

                        dbref.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {

                                boolean res = false;
                                for (DataSnapshot x : snapshot.getChildren()) {
                                    if (x.child("usuario").getValue().toString().equals(usuario)) {
                                        res = true;
                                        ocultarTeclado();
                                        Toast.makeText(Signup.this, "El usuario (" + usuario + ") ya existe", Toast.LENGTH_SHORT).show();
                                        break;
                                    }
                                } //Cierre for

                                if (res == false) {

                                    Usuario user = new Usuario(nombre, usuario, password, pasosregistrados);
                                    dbref.push().setValue(user);
                                    ocultarTeclado();
                                    Toast.makeText(Signup.this, "Usuario registrado correctamente", Toast.LENGTH_SHORT).show();

                                    editTxtNombre.setText("");
                                    editTxtUsuario.setText("");
                                    editTxtPassword.setText("");
                                    editTxtConfirmar.setText("");
                                    editTxtNombre.setHint(R.string.nombre);
                                    editTxtUsuario.setHint(R.string.usuario);
                                    editTxtPassword.setHint(R.string.password);
                                    editTxtConfirmar.setHint(R.string.confirmar);

                                }

                            } //onDataChange

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                // ***************IMPORTANTE EDITAR LO SIGUIENTE PARA MANEJAR EXCEPCIONES *******************************
                                Toast.makeText(Signup.this, "No se ha podido crear el usuario.", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        Toast.makeText(Signup.this, "Los campos Password y Confirmar no coinciden", Toast.LENGTH_SHORT).show();
                        editTxtPassword.setText("");
                        editTxtConfirmar.setText("");
                        editTxtPassword.setHint(R.string.password);
                        editTxtConfirmar.setHint(R.string.confirmar);
                    }
                } //else
            } //onClick
        });
    } //botonRegistrar()

    private void ocultarTeclado(){
        View view = this.getCurrentFocus();
        if(view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(),0);
        } //Cierra if
    } // cierra método ocultarTeclado

    public void pasarLogin(View view){
        Intent intent = new Intent(this, Login.class);
        startActivity(intent);
    }

}