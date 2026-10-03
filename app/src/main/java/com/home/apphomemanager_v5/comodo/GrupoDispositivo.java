package com.home.apphomemanager_v5.comodo;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

import com.home.apphomemanager_v5.R;

/** Tipos de dispositivo que um cômodo pode ter; cada cômodo só mostra os grupos que o usuário escolheu. */
public enum GrupoDispositivo {

    LAMPADAS("lampadas", R.string.light, R.drawable.ic_lampada, R.color.corLuz, R.string.novaLampada, R.string.adicionarLampada, Comando.LIGA_DESLIGA, true),
    TOMADAS("tomadas", R.string.power, R.drawable.ic_tomada, R.color.corTomada, R.string.novaTomada, R.string.adicionarTomada, Comando.LIGA_DESLIGA, true),
    MEDIDORES("medidores", R.string.grupoMedidores, R.drawable.ic_medidor, R.color.corEnergia, R.string.novoMedidor, R.string.adicionarMedidor, Comando.LIGA_DESLIGA, true),
    TERMOMETROS("termometros", R.string.grupoTermometros, R.drawable.ic_termometro, R.color.corTemperatura, R.string.novoTermometro, R.string.adicionarTermometro, Comando.NENHUM, true),
    CORTINAS("cortinas", R.string.grupoCortinas, R.drawable.ic_cortina, R.color.corCortina, R.string.novaCortina, R.string.adicionarCortina, Comando.CORTINA, true),
    CHUVA("chuva", R.string.grupoChuva, R.drawable.ic_chuva, R.color.corAgua, R.string.novoSensorChuva, R.string.adicionarSensorChuva, Comando.NENHUM, true),
    ALARMES("alarmes", R.string.grupoAlarmes, R.drawable.ic_alarme, R.color.corAlarme, R.string.novoAlarme, R.string.adicionarAlarme, Comando.ARMA_DESARMA, true),
    LUZ("luz", R.string.grupoLuz, R.drawable.ic_luz_ajuste, R.color.corLuz, R.string.novaLuz, R.string.adicionarLuz, Comando.LUZ_AJUSTAVEL, true);

    /** Como o grupo é comandado: sempre por cenas do Smart Life (nunca direto no aparelho, que gasta cota). */
    public enum Comando {
        /** Só leitura. */
        NENHUM,
        /** Liga e desliga (a cena de desligar é opcional; sem ela a mesma cena alterna). */
        LIGA_DESLIGA,
        /** Ativa e desativa a central de alarme. */
        ARMA_DESARMA,
        /** Luz ajustável: três cenas de temperatura da cor e quatro de brilho. */
        LUZ_AJUSTAVEL,
        /** Sobe, para e desce a cortina. */
        CORTINA
    }

    /** Nome do grupo no Firebase (lista {@code grupos} do cômodo). */
    public final String chave;
    @StringRes
    public final int titulo;
    @DrawableRes
    public final int icone;
    @ColorRes
    public final int cor;
    /** Modelo do nome de um item novo, com a posição como argumento. */
    @StringRes
    public final int modeloNome;
    @StringRes
    public final int adicionar;
    public final Comando comando;
    /** Associa o item a um dispositivo da Tuya para ler o estado; na cortina a associação é opcional (só a posição). */
    public final boolean dispositivo;

    GrupoDispositivo(String chave, int titulo, int icone, int cor, int modeloNome, int adicionar, Comando comando, boolean dispositivo) {
        this.chave = chave;
        this.titulo = titulo;
        this.icone = icone;
        this.cor = cor;
        this.modeloNome = modeloNome;
        this.adicionar = adicionar;
        this.comando = comando;
        this.dispositivo = dispositivo;
    }

    public static GrupoDispositivo daChave(String chave) {

        for (GrupoDispositivo grupo : values()) {
            if (grupo.chave.equals(chave)) {
                return grupo;
            }
        }
        return null;
    }
}
