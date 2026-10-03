package com.home.apphomemanager_v5.util;

import androidx.annotation.DrawableRes;

import com.home.apphomemanager_v5.R;

/** Ícones do dashboard a partir do código de tempo WMO (Open-Meteo) e da temperatura. */
public final class IconeClima {

    private static final int TEMPERATURA_BAIXA = 15;
    private static final int TEMPERATURA_ALTA = 25;

    private IconeClima() {}

    @DrawableRes
    public static int condicao(Integer codigo, boolean dia) {

        if (codigo == null) {
            return dia ? R.drawable.none_day : R.drawable.none_night;
        }

        switch (codigo) {
            case 0:
                return dia ? R.drawable.clear_day : R.drawable.clear_night;
            case 1:
            case 2:
                return dia ? R.drawable.cloudly_day : R.drawable.cloudly_night;
            case 3:
            case 45:
            case 48:
                return R.drawable.cloud;
            case 51:
            case 53:
            case 55:
            case 56:
            case 57:
            case 61:
            case 63:
            case 65:
            case 66:
            case 67:
            case 80:
            case 81:
            case 82:
                return R.drawable.rain;
            case 71:
            case 73:
            case 75:
            case 77:
            case 85:
            case 86:
                return R.drawable.snow;
            case 95:
            case 96:
            case 99:
                return R.drawable.storm;
            default:
                return dia ? R.drawable.none_day : R.drawable.none_night;
        }
    }

    /** Termômetro: abaixo de 15 °C baixo, acima de 25 °C alto (mesma regra da versão anterior do app). */
    @DrawableRes
    public static int termometro(Long temperatura) {

        if (temperatura == null) {
            return R.drawable.taverage;
        }
        if (temperatura < TEMPERATURA_BAIXA) {
            return R.drawable.tlow;
        }
        return temperatura > TEMPERATURA_ALTA ? R.drawable.thigh : R.drawable.taverage;
    }
}
