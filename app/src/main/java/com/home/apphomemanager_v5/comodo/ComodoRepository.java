package com.home.apphomemanager_v5.comodo;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.reflect.TypeToken;
import com.home.apphomemanager_v5.util.CacheUsuario;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Cômodos cadastrados no Firebase ({@code comodos/{id}}). Os itens de cada grupo ficam em listas
 * indexadas por posição e cada gravação reescreve o cômodo inteiro, então não sobra item removido.
 */
public class ComodoRepository {

    public interface Listener {
        void aoAtualizar(List<Comodo> comodos);

        void aoFalhar(DatabaseError erro);
    }

    private static final String COMODOS = "comodos";
    private static final String SEMEADO = "meta/comodosSemeados";

    private final DatabaseReference raiz = FirebaseDatabase.getInstance().getReference();
    private final DatabaseReference comodos = raiz.child(COMODOS);

    private final CacheUsuario<List<Comodo>> cache;

    /** JSON da última versão entregue às telas (a local ou a do Firebase); só muda quando o conteúdo muda. */
    private String jsonEntregue;

    private ValueEventListener valueEventListener;

    /** Falso depois de {@link #para()}, inclusive se ele for chamado de dentro da entrega da cópia local. */
    private boolean ativo = false;

    public ComodoRepository(Context context) {
        cache = new CacheUsuario<>(context, "comodos", new TypeToken<List<Comodo>>() {}.getType());
    }

    public void inicia(Listener listener) {

        para();

        // Primeiro a cópia local, para as telas abrirem já preenchidas.
        ativo = true;
        jsonEntregue = cache.leJson();

        List<Comodo> local = cache.le(jsonEntregue);

        if (local != null) {
            listener.aoAtualizar(local);
        } else {
            jsonEntregue = null;
        }

        if (!ativo) {
            return;
        }

        valueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                List<Comodo> lista = new ArrayList<>();

                for (DataSnapshot filho : snapshot.getChildren()) {
                    lista.add(le(filho));
                }

                lista.sort((a, b) -> a.ordem != b.ordem ? Long.compare(a.ordem, b.ordem) : a.nome.compareToIgnoreCase(b.nome));

                // Só atualiza telas e cópia local se o Firebase trouxe algo diferente do que já foi entregue.
                String json = cache.paraJson(lista);

                if (json.equals(jsonEntregue)) {
                    return;
                }

                jsonEntregue = json;
                cache.salva(json);

                listener.aoAtualizar(lista);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.aoFalhar(error);
            }
        };

        comodos.addValueEventListener(valueEventListener);
    }

    public void para() {

        ativo = false;

        if (valueEventListener != null) {
            comodos.removeEventListener(valueEventListener);
            valueEventListener = null;
        }
    }

    public String novoId() {
        return comodos.push().getKey();
    }

    public void salva(Comodo comodo) {
        comodos.child(comodo.id).setValue(paraMapa(comodo));
    }

    public void defineHabilitado(String id, boolean habilitado) {
        comodos.child(id).child("habilitado").setValue(habilitado);
    }

    public void remove(String id) {
        comodos.child(id).removeValue();
    }

    /**
     * Cadastra os cômodos padrão uma única vez (flag no Firebase), para que apagar todos
     * os cômodos depois não os traga de volta.
     */
    public void semeiaSeNecessario(List<Comodo> padrao) {

        raiz.child(SEMEADO).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (Boolean.TRUE.equals(snapshot.getValue(Boolean.class))) {
                    return;
                }

                Map<String, Object> alteracoes = new HashMap<>();

                for (Comodo comodo : padrao) {
                    alteracoes.put(COMODOS + "/" + comodo.id, paraMapa(comodo));
                }
                alteracoes.put(SEMEADO, true);

                raiz.updateChildren(alteracoes);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private static Comodo le(DataSnapshot snapshot) {

        Comodo comodo = new Comodo();

        comodo.id = snapshot.getKey();
        comodo.nome = texto(snapshot.child("nome"), "");
        comodo.icone = texto(snapshot.child("icone"), Comodo.ICONES[0]);

        Long ordem = snapshot.child("ordem").getValue(Long.class);
        comodo.ordem = ordem != null ? ordem : Long.MAX_VALUE;

        // Cômodos antigos não têm o campo: contam como habilitados.
        Boolean habilitado = snapshot.child("habilitado").getValue(Boolean.class);
        comodo.habilitado = habilitado == null || habilitado;

        for (DataSnapshot filho : snapshot.child("grupos").getChildren()) {
            String chave = filho.getValue(String.class);

            if (chave != null && GrupoDispositivo.daChave(chave) != null) {
                comodo.grupos.add(chave);
            }
        }

        for (GrupoDispositivo grupo : GrupoDispositivo.values()) {
            comodo.itens(grupo).addAll(leItens(snapshot.child(grupo.chave)));
        }

        return comodo;
    }

    private static List<ItemComodo> leItens(DataSnapshot snapshot) {

        List<ItemComodo> itens = new ArrayList<>();

        for (DataSnapshot filho : snapshot.getChildren()) {

            ItemComodo item = new ItemComodo(texto(filho.child("nome"), ""));

            item.tuyaDeviceId = texto(filho.child("tuyaDeviceId"), null);
            item.tuyaCode = texto(filho.child("tuyaCode"), null);
            item.tuyaNome = texto(filho.child("tuyaNome"), null);
            item.tuyaCena = texto(filho.child("tuyaCena"), null);
            item.tuyaCenaDesliga = texto(filho.child("tuyaCenaDesliga"), null);
            item.tuyaCenaParar = texto(filho.child("tuyaCenaParar"), null);
            item.tuyaCenaExtra1 = texto(filho.child("tuyaCenaExtra1"), null);
            item.tuyaCenaExtra2 = texto(filho.child("tuyaCenaExtra2"), null);
            item.tuyaCenaExtra3 = texto(filho.child("tuyaCenaExtra3"), null);
            item.tuyaCenaExtra4 = texto(filho.child("tuyaCenaExtra4"), null);
            item.posicaoInvertida = Boolean.TRUE.equals(filho.child("posicaoInvertida").getValue(Boolean.class));

            for (DataSnapshot escala : filho.child("tuyaEscalas").getChildren()) {
                Long casas = escala.getValue(Long.class);

                if (casas != null) {
                    item.escalas.put(escala.getKey(), casas.intValue());
                }
            }

            itens.add(item);
        }
        return itens;
    }

    private static String texto(DataSnapshot snapshot, String padrao) {

        String valor = snapshot.getValue(String.class);
        return valor != null ? valor : padrao;
    }

    private static Map<String, Object> paraMapa(Comodo comodo) {

        Map<String, Object> mapa = new HashMap<>();

        mapa.put("nome", comodo.nome);
        mapa.put("icone", comodo.icone);
        mapa.put("ordem", comodo.ordem);
        mapa.put("habilitado", comodo.habilitado);
        List<String> grupos = new ArrayList<>();

        for (GrupoDispositivo grupo : comodo.gruposAtivos()) {
            grupos.add(grupo.chave);
            mapa.put(grupo.chave, itensParaMapa(comodo.itens(grupo)));
        }
        mapa.put("grupos", grupos);

        return mapa;
    }

    private static Map<String, Object> itensParaMapa(List<ItemComodo> itens) {

        Map<String, Object> mapa = new HashMap<>();

        for (int i = 0; i < itens.size(); i++) {

            ItemComodo item = itens.get(i);
            Map<String, Object> campos = new HashMap<>();

            campos.put("nome", item.nome);

            if (item.associado()) {
                campos.put("tuyaDeviceId", item.tuyaDeviceId);
                campos.put("tuyaCode", item.tuyaCode);

                if (item.escalas != null && !item.escalas.isEmpty()) {
                    campos.put("tuyaEscalas", new HashMap<>(item.escalas));
                }

                if (item.tuyaNome != null && !item.tuyaNome.isEmpty()) {
                    campos.put("tuyaNome", item.tuyaNome);
                }
            }

            // Na cortina as três cenas são independentes; nos demais grupos a de desligar só existe com a principal.
            if (ItemComodo.preenchida(item.tuyaCena)) {
                campos.put("tuyaCena", item.tuyaCena);
            }
            if (ItemComodo.preenchida(item.tuyaCenaDesliga)) {
                campos.put("tuyaCenaDesliga", item.tuyaCenaDesliga);
            }
            if (ItemComodo.preenchida(item.tuyaCenaExtra1)) {
                campos.put("tuyaCenaExtra1", item.tuyaCenaExtra1);
            }
            if (ItemComodo.preenchida(item.tuyaCenaExtra2)) {
                campos.put("tuyaCenaExtra2", item.tuyaCenaExtra2);
            }
            if (ItemComodo.preenchida(item.tuyaCenaExtra3)) {
                campos.put("tuyaCenaExtra3", item.tuyaCenaExtra3);
            }
            if (ItemComodo.preenchida(item.tuyaCenaExtra4)) {
                campos.put("tuyaCenaExtra4", item.tuyaCenaExtra4);
            }
            if (item.posicaoInvertida) {
                campos.put("posicaoInvertida", true);
            }
            if (ItemComodo.preenchida(item.tuyaCenaParar)) {
                campos.put("tuyaCenaParar", item.tuyaCenaParar);
            }

            mapa.put(String.valueOf(i), campos);
        }
        return mapa;
    }
}
