package com.xr3trx.sensores;

public class Usuario {

    private String nombre;
    private String Usuario;
    private String Password;
    private String pasosRegistrados;

    public Usuario() {
    }

    public Usuario(String nombre, String usuario, String password, String pasosRegistrados) {
        this.nombre = nombre;
        Usuario = usuario;
        Password = password;
        this.pasosRegistrados = pasosRegistrados;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getUsuario() {
        return Usuario;
    }

    public void setUsuario(String usuario) {
        Usuario = usuario;
    }

    public String getPassword() {
        return Password;
    }

    public void setPassword(String password) {
        Password = password;
    }

    public String getPasosRegistrados() {
        return pasosRegistrados;
    }

    public void setPasosRegistrados(String pasosRegistrados) {
        this.pasosRegistrados = pasosRegistrados;
    }

    @Override
    public String toString() {
        return "Usuario{" +
                "nombre='" + nombre + '\'' +
                ", pasosRegistrados=" + pasosRegistrados +
                '}';
    }


}
