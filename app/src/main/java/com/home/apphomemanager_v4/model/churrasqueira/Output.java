package com.home.apphomemanager_v4.model.churrasqueira;

import java.util.Objects;

public class Output {
    private Boolean agua;
    private  Boolean exaustor;
    private Boolean lampada;
    private Boolean soprador;

    public Boolean isAgua() {
        return agua;
    }

    public void setAgua(Boolean agua) {
        this.agua = agua;
    }

    public Boolean isExaustor() {
        return exaustor;
    }

    public void setExaustor(Boolean exaustor) {
        this.exaustor = exaustor;
    }

    public Boolean isLampada() {
        return lampada;
    }

    public void setLampada(Boolean lampada) {
        this.lampada = lampada;
    }

    public Boolean isSoprador() {
        return soprador;
    }

    public void setSoprador(Boolean soprador) {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Output)) return false;
        Output output = (Output) o;
        return isAgua() == output.isAgua() && isExaustor() == output.isExaustor() && isLampada() == output.isLampada() && isSoprador() == output.isSoprador();
    }

    @Override
    public int hashCode() {
        return Objects.hash(isAgua(), isExaustor(), isLampada(), isSoprador());
    }
}
