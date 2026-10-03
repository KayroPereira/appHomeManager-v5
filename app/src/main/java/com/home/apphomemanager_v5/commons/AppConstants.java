package com.home.apphomemanager_v5.commons;

public class AppConstants {

    private AppConstants(){}

    public static final int DELAY_2_MINUTO_MS = 1000 * 60 * 2;
    public static final int PERIODO_2_MINUTO_S = 120;

    /**
     * Intervalo de reavaliação do status on-line/off-line. Menor que o período de tolerância
     * para que a queda do dispositivo apareça em até ~15 s, e não em até 4 min.
     */
    public static final int DELAY_VERIFICACAO_STATUS_MS = 1000 * 15;
}
