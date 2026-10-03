package com.home.apphomemanager_v5.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.gson.Gson;

import java.lang.reflect.Type;

/**
 * Cópia local de um dado do Firebase (JSON em SharedPreferences), lida na abertura das telas antes
 * de o Firebase responder. O Firebase continua sendo a fonte da verdade: cada versão nova dele
 * sobrescreve esta cópia. Cada usuário logado tem a sua (chave com o uid); sem usuário não há cópia.
 */
public class CacheUsuario<T> {

    private final SharedPreferences preferencias;
    private final String prefixoChave;
    private final Type tipo;
    private final Gson gson = new Gson();

    /**
     * @param nome nome do dado, ex.: "comodos" (arquivo {@code comodos_cache}, chave {@code comodos_<uid>})
     * @param tipo tipo completo a converter, ex.: um TypeToken de List
     */
    public CacheUsuario(Context context, String nome, Type tipo) {
        this.preferencias = context.getApplicationContext().getSharedPreferences(nome + "_cache", Context.MODE_PRIVATE);
        this.prefixoChave = nome + "_";
        this.tipo = tipo;
    }

    /** JSON guardado, ou nulo se o app nunca recebeu o dado (ou se não há usuário logado). */
    public String leJson() {

        String chave = chave();

        return chave != null ? preferencias.getString(chave, null) : null;
    }

    /** Objeto guardado, ou nulo se não houver cópia (ou se ela estiver corrompida). */
    public T le(String json) {

        if (json == null) {
            return null;
        }

        try {
            return gson.fromJson(json, tipo);
        } catch (RuntimeException e) {
            return null;
        }
    }

    public String paraJson(T dado) {
        return gson.toJson(dado, tipo);
    }

    public void salva(String json) {

        String chave = chave();

        if (chave != null) {
            preferencias.edit().putString(chave, json).apply();
        }
    }

    /** Lida a cada uso, e não guardada, porque o usuário pode trocar de conta com o app aberto. */
    private String chave() {

        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();

        return usuario != null ? prefixoChave + usuario.getUid() : null;
    }
}
