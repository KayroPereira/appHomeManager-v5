package com.home.apphomemanager_v4.model.churrasqueira;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class Churrasqueira {

    private Boolean onOff;
    private Long update = -1L;
    private Parametro parametros = new Parametro();

    public Boolean isOnOff() {
        return onOff;
    }

    public void setOnOff(Boolean onOff) {
        this.onOff = onOff;
    }

    public Long getUpdate() {
        return update;
    }

    public void setUpdate(Long update) {
        this.update = update;
    }

    public Parametro getParametros() {
        return parametros;
    }

    public void setParametros(Parametro parametros) {
        this.parametros = parametros;
    }

    public void inicializa(){
        this.setOnOff(false);
        this.getParametros().getMotor().setHabilitado(false);
        this.getParametros().getMotor().setDirecao(false);
        this.getParametros().getMotor().setOnOff(false);
        this.getParametros().getOutput().setAgua(false);
        this.getParametros().getOutput().setExaustor(false);
        this.getParametros().getOutput().setLampada(false);
        this.getParametros().getOutput().setSoprador(false);
        this.getParametros().getSensor().setFc2(false);
        this.getParametros().getSensor().setFc2(false);
    }

    @Override
    public String toString() {
        return "ChurrasqueiraEntity{" +
                "onOff=" + onOff +
                ", update=" + update +
                ", parametros=" + parametros +
                '}';
    }
}