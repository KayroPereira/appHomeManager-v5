package com.home.apphomemanager_v5.model.clima;

/** Descrição em português dos códigos de tempo WMO usados pela Open-Meteo. */
public final class CondicaoClima {

    private CondicaoClima() {}

    public static String descricao(Integer codigo) {

        if (codigo == null) {
            return null;
        }

        switch (codigo) {
            case 0:  return "Céu limpo";
            case 1:  return "Predominantemente limpo";
            case 2:  return "Parcialmente nublado";
            case 3:  return "Nublado";
            case 45:
            case 48: return "Neblina";
            case 51:
            case 53:
            case 55:
            case 56:
            case 57: return "Garoa";
            case 61:
            case 63:
            case 65:
            case 66:
            case 67: return "Chuva";
            case 71:
            case 73:
            case 75:
            case 77:
            case 85:
            case 86: return "Neve";
            case 80:
            case 81:
            case 82: return "Pancadas de chuva";
            case 95:
            case 96:
            case 99: return "Tempestade";
            default: return null;
        }
    }
}
