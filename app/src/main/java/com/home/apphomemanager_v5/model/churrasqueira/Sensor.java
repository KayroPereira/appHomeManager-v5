package com.home.apphomemanager_v5.model.churrasqueira;

import java.util.Objects;

public class Sensor {
    private Boolean fc1;
    private Boolean fc2;

    public Boolean isFc1() {
        return fc1;
    }

    public void setFc1(Boolean fc1) {
        this.fc1 = fc1;
    }

    public Boolean isFc2() {
        return fc2;
    }

    public void setFc2(Boolean fc2) {
        this.fc2 = fc2;
    }

    @Override
    public String toString() {
        return "Sensor{" +
                "fc1=" + fc1 +
                ", fc2=" + fc2 +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Sensor)) return false;
        Sensor sensor = (Sensor) o;
        return isFc1() == sensor.isFc1() && isFc2() == sensor.isFc2();
    }

    @Override
    public int hashCode() {
        return Objects.hash(isFc1(), isFc2());
    }
}