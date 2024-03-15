package com.home.apphomemanager_v4.model.churrasqueira;

import java.util.Objects;

public class Motor {
    private Boolean direcao;
    private Boolean habilitado;
    private Boolean onOff;

    public Boolean isDirecao() {
        return direcao;
    }

    public void setDirecao(Boolean direcao) {
        this.direcao = direcao;
    }

    public Boolean isHabilitado() {
        return habilitado;
    }

    public void setHabilitado(Boolean habilitado) {
        this.habilitado = habilitado;
    }

    public Boolean isOnOff() {
        return onOff;
    }

    public void setOnOff(Boolean onOff) {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Motor)) return false;
        Motor motor = (Motor) o;
        return isDirecao() == motor.isDirecao() && isHabilitado() == motor.isHabilitado() && isOnOff() == motor.isOnOff();
    }

    @Override
    public int hashCode() {
        return Objects.hash(isDirecao(), isHabilitado(), isOnOff());
    }
}
