package com.home.apphomemanager_v5.comodo;

import android.content.Context;

import com.home.apphomemanager_v5.R;

import java.util.ArrayList;
import java.util.List;

/** Cômodos da casa cadastrados na primeira execução (eram as telas fixas do app antigo). */
public class ComodosPadrao {

    private ComodosPadrao() {}

    private static final class Modelo {
        final String id;
        final String icone;
        final int titulo;
        /** Sufixo dos nomes em strings.xml: l{n}{sufixo} para lâmpadas e t{n}{sufixo} para tomadas. */
        final String sufixo;
        final int lampadas;
        final int tomadas;

        Modelo(String id, String icone, int titulo, String sufixo, int lampadas, int tomadas) {
            this.id = id;
            this.icone = icone;
            this.titulo = titulo;
            this.sufixo = sufixo;
            this.lampadas = lampadas;
            this.tomadas = tomadas;
        }
    }

    private static final Modelo[] MODELOS = {
            new Modelo("sala", "sala", R.string.titleLiving, "LVR", 6, 6),
            new Modelo("cozinha", "cozinha", R.string.titleKitchen, "KTR", 3, 9),
            new Modelo("quarto1", "quarto", R.string.titleBedRoom1, "BDR1", 1, 7),
            new Modelo("quarto2", "quarto", R.string.titleBedRoom2, "BDR2", 1, 7),
            new Modelo("quarto3", "quarto", R.string.titleBedRoom3, "BDR3", 1, 7),
            new Modelo("quarto4", "quarto", R.string.titleBedRoom4, "BDR4", 3, 5),
            new Modelo("banheiro1", "banheiro", R.string.titleBathRoom1, "BTR1", 2, 1),
            new Modelo("banheiro2", "banheiro", R.string.titleBathRoom2, "BTR2", 2, 1),
            new Modelo("hall", "hall", R.string.titleHall, "HALL", 2, 6),
    };

    public static List<Comodo> cria(Context context) {

        List<Comodo> comodos = new ArrayList<>();

        for (int i = 0; i < MODELOS.length; i++) {

            Modelo modelo = MODELOS[i];
            Comodo comodo = new Comodo();

            comodo.id = modelo.id;
            comodo.nome = context.getString(modelo.titulo);
            comodo.icone = modelo.icone;
            comodo.ordem = i;

            adiciona(context, comodo.lampadas, "l", modelo.sufixo, modelo.lampadas);
            adiciona(context, comodo.tomadas, "t", modelo.sufixo, modelo.tomadas);

            // Os cômodos padrão já nascem com os dois grupos que o app antigo tinha.
            comodo.grupos.add(GrupoDispositivo.LAMPADAS.chave);
            comodo.grupos.add(GrupoDispositivo.TOMADAS.chave);

            comodos.add(comodo);
        }
        return comodos;
    }

    private static void adiciona(Context context, List<ItemComodo> itens, String prefixo, String sufixo, int quantidade) {

        for (int n = 1; n <= quantidade; n++) {

            // Os nomes seguem o padrão l1LVR, t3KTR... de strings.xml.
            int id = context.getResources().getIdentifier(prefixo + n + sufixo, "string", context.getPackageName());

            itens.add(new ItemComodo(id != 0 ? context.getString(id) : prefixo + n));
        }
    }
}
