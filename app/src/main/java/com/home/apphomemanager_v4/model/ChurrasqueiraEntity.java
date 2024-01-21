package com.home.apphomemanager_v4.model;

import com.google.firebase.database.IgnoreExtraProperties;
import com.google.gson.Gson;

import org.json.JSONObject;

@IgnoreExtraProperties
public class ChurrasqueiraEntity {

    private boolean onOff;
    private Long update;
    private Parametros parametros;

//    public static ChurrasqueiraEntity fromJson(JSONObject json){
//        return fromJson(json.toString());
//    }
//
//    public static ChurrasqueiraEntity fromJson(String json){
//        return new Gson().fromJson(json, ChurrasqueiraEntity.class);
//    }
//
//    public String toJson(){
//        return new Gson().toJson(this);
//    }

    public boolean isOnOff() {
        return onOff;
    }

    public void setOnOff(boolean onOff) {
        this.onOff = onOff;
    }

    public Long getUpdate() {
        return update;
    }

    public void setUpdate(Long update) {
        this.update = update;
    }

    public Parametros getParametros() {
        return parametros;
    }

    public void setParametros(Parametros parametros) {
        this.parametros = parametros;
    }

    @Override
    public String toString() {
        return "ChurrasqueiraEntity{" +
                "onOff=" + onOff +
                ", update=" + update +
                ", parametros=" + parametros +
                '}';
    }

    class Parametros{
        private Motor motor;
        private Output output;
        private Sensor sensor;

        public Motor getMotor() {
            return motor;
        }

        public void setMotor(Motor motor) {
            this.motor = motor;
        }

        public Output getOutput() {
            return output;
        }

        public void setOutput(Output output) {
            this.output = output;
        }

        public Sensor getSensor() {
            return sensor;
        }

        public void setSensor(Sensor sensor) {
            this.sensor = sensor;
        }

        @Override
        public String toString() {
            return "Parametros{" +
                    "motor=" + motor +
                    ", output=" + output +
                    ", sensor=" + sensor +
                    '}';
        }
    }

    class Motor{
        private boolean direcao;
        private boolean habilitado;
        private boolean onOff;

        public boolean isDirecao() {
            return direcao;
        }

        public void setDirecao(boolean direcao) {
            this.direcao = direcao;
        }

        public boolean isHabilitado() {
            return habilitado;
        }

        public void setHabilitado(boolean habilitado) {
            this.habilitado = habilitado;
        }

        public boolean isOnOff() {
            return onOff;
        }

        public void setOnOff(boolean onOff) {
            this.onOff = onOff;
        }

        @Override
        public String toString() {
            return "Motor{" +
                    "direcao=" + direcao +
                    ", habilitado=" + habilitado +
                    ", onOff=" + onOff +
                    '}';
        }
    }

    class Output{
        private boolean agua;
        private  boolean exaustor;
        private boolean lampada;
        private boolean soprador;

        public boolean isAgua() {
            return agua;
        }

        public void setAgua(boolean agua) {
            this.agua = agua;
        }

        public boolean isExaustor() {
            return exaustor;
        }

        public void setExaustor(boolean exaustor) {
            this.exaustor = exaustor;
        }

        public boolean isLampada() {
            return lampada;
        }

        public void setLampada(boolean lampada) {
            this.lampada = lampada;
        }

        public boolean isSoprador() {
            return soprador;
        }

        public void setSoprador(boolean soprador) {
            this.soprador = soprador;
        }

        @Override
        public String toString() {
            return "Output{" +
                    "agua=" + agua +
                    ", exaustor=" + exaustor +
                    ", lampada=" + lampada +
                    ", soprador=" + soprador +
                    '}';
        }
    }

    class Sensor{
        private boolean fc1;
        private boolean fc2;

        public boolean isFc1() {
            return fc1;
        }

        public void setFc1(boolean fc1) {
            this.fc1 = fc1;
        }

        public boolean isFc2() {
            return fc2;
        }

        public void setFc2(boolean fc2) {
            this.fc2 = fc2;
        }

        @Override
        public String toString() {
            return "Sensor{" +
                    "fc1=" + fc1 +
                    ", fc2=" + fc2 +
                    '}';
        }
    }
}