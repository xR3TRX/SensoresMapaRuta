package com.xr3trx.sensores;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class Login extends AppCompatActivity {

    private TextView txtUser, txtPass, txtLogin;
    private Button btnLogin;
    private String nombreUsuario;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        txtUser = findViewById(R.id.txtUsuario);
        txtPass = findViewById(R.id.txtPass);

        btnLogin = findViewById(R.id.btnLogin);
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(txtUser.getText().toString().trim().isEmpty()
                || txtPass.getText().toString().trim().isEmpty()){

                    ocultarTeclado();
                    Toast.makeText(Login.this, "Complete los campos faltantes", Toast.LENGTH_SHORT).show();
                } else {

                    String user = txtUser.getText().toString();
                    String password = txtPass.getText().toString();
                    FirebaseDatabase db = FirebaseDatabase.getInstance();
                    DatabaseReference dbref = db.getReference(Usuario.class.getSimpleName());

                    dbref.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {

                            boolean resUser = false;
                            for(DataSnapshot u : snapshot.getChildren()){
                                String usuario = u.child("usuario").getValue(String.class);
                                String pass = u.child("password").getValue(String.class);

                                if(usuario.equals(user) && pass.equals(password)){
                                    resUser = true;

                                    nombreUsuario = (u.child("nombre").getValue().toString());
                                    ocultarTeclado();
                                    break;
                                } //if
                            } //for
                            
                            if(resUser){
                                Intent intent = new Intent(Login.this, MainActivity2.class);
                                intent.putExtra("USUARIO", user);
                                intent.putExtra("NOMBRE", nombreUsuario);
                                startActivity(intent);
                                finish();

                            } else {
                                ocultarTeclado();
                                Toast.makeText(Login.this, "El usuario y/o la contraseña no son válidos.\nRevise los datos e intentelo de nuevo.", Toast.LENGTH_SHORT).show();

                                txtUser.setText("");
                                txtPass.setText("");
                                txtUser.setHint(R.string.usuario);
                                txtPass.setHint(R.string.password);

                            } //else
                        } //onDataChange

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    }); //dbref

                }//else
            }//onClick
        }); //onClickListener


    } //onCreate

    public void pasarSignup(View view){
        Intent intent = new Intent(this, Signup.class);
        startActivity(intent);
    }

    private void ocultarTeclado(){
        View view = this.getCurrentFocus();
        if(view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(),0);
        } //Cierra if
    } // cierra método ocultarTeclado
}