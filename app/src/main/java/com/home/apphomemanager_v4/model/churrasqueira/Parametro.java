package com.home.apphomemanager_v4.model.churrasqueira;

import java.util.Objects;

public class Parametro {
    private Motor motor = new Motor();
    private Output output = new Output();
    private Sensor sensor = new Sensor();

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Parametro)) return false;
        Parametro parametro = (Parametro) o;
        return Objects.equals(getMotor(), parametro.getMotor()) && Objects.equals(getOutput(), parametro.getOutput()) && Objects.equals(getSensor(), parametro.getSensor());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getMotor(), getOutput(), getSensor());
    }
}
