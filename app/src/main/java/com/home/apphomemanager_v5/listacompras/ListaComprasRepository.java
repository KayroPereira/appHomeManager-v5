package com.home.apphomemanager_v5.listacompras;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.FirebaseApp;
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
 * Acesso à lista de compras no Firebase. O Firebase é a fonte da verdade e cada alteração chega pelo
 * próprio listener (inclusive as locais). Uma cópia local por usuário (a lista já mesclada) abre as telas
 * preenchidas antes de o Firebase responder; se ele trouxer algo diferente, telas e cópia são atualizadas.
 */
public class ListaComprasRepository {

    public interface Listener {
        void aoAtualizar(List<Produto> produtos);

        void aoFalhar(DatabaseError erro);
    }

    private static final String PATH_RAIZ = "listaCompras";
    private static final String DESPENSA = "despensa";
    private static final String MINHA_LISTA = "minhaLst";

    private final DatabaseReference raiz = FirebaseDatabase.getInstance().getReference(PATH_RAIZ);

    private final CacheUsuario<List<Produto>> cache = new CacheUsuario<>(
            FirebaseApp.getInstance().getApplicationContext(), "listaCompras", new TypeToken<List<Produto>>() {}.getType());

    /** JSON da última versão entregue às telas (a local ou a do Firebase); só muda quando o conteúdo muda. */
    private String jsonEntregue;

    /** Falso depois de {@link #para()}, inclusive se ele for chamado de dentro da entrega da cópia local. */
    private boolean ativo = false;

    private ValueEventListener valueEventListener;

    public void inicia(Listener listener) {

        para();

        // Primeiro a cópia local, para as telas abrirem já preenchidas.
        ativo = true;
        jsonEntregue = cache.leJson();

        List<Produto> local = cache.le(jsonEntregue);

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

                List<Produto> despensa = leProdutos(snapshot.child(DESPENSA), true);
                List<Produto> minhaLista = leProdutos(snapshot.child(MINHA_LISTA), false);

                List<Produto> mesclada = ListaComprasUtils.mescla(despensa, minhaLista);

                // Só atualiza telas e cópia local se o Firebase trouxe algo diferente do que já foi entregue.
                String json = cache.paraJson(mesclada);

                if (json.equals(jsonEntregue)) {
                    return;
                }

                jsonEntregue = json;
                cache.salva(json);

                listener.aoAtualizar(mesclada);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.aoFalhar(error);
            }
        };

        raiz.addValueEventListener(valueEventListener);
    }

    public void para() {

        ativo = false;

        if (valueEventListener != null) {
            raiz.removeEventListener(valueEventListener);
            valueEventListener = null;
        }
    }

    /** Cadastra (ou atualiza a unidade de) um produto na despensa. */
    public void cadastra(int categoria, String nome, int unidade) {
        raiz.child(caminhoDespensa(categoria, nome)).setValue(unidade);
    }

    /** Remove o produto da despensa e, se estiver, da lista de compras. */
    public void remove(Produto produto) {

        Map<String, Object> alteracoes = new HashMap<>();

        alteracoes.put(caminhoDespensa(produto.getCategoria(), produto.getNome()), null);
        alteracoes.put(caminhoLista(produto), null);

        raiz.updateChildren(alteracoes);
    }

    /** Coloca o produto na lista de compras como pendente. */
    public void adicionaNaLista(Produto produto) {
        raiz.child(caminhoLista(produto)).setValue(produto.comStatus(Produto.STATUS_PENDENTE).valorLista());
    }

    public void removeDaLista(Produto produto) {
        raiz.child(caminhoLista(produto)).removeValue();
    }

    /** Alterna entre pendente e na cesta. */
    public void alternaCesta(Produto produto) {

        int novoStatus = produto.getStatus() == Produto.STATUS_PENDENTE ? Produto.STATUS_COMPRADO : Produto.STATUS_PENDENTE;

        raiz.child(caminhoLista(produto)).setValue(produto.comStatus(novoStatus).valorLista());
    }

    /** Remove da lista todos os produtos que já estão na cesta, numa única gravação. */
    public void limpaCesta(List<Produto> produtos) {

        Map<String, Object> alteracoes = new HashMap<>();

        for (Produto produto : produtos) {
            if (produto.getStatus() == Produto.STATUS_COMPRADO) {
                alteracoes.put(caminhoLista(produto), null);
            }
        }

        if (!alteracoes.isEmpty()) {
            raiz.updateChildren(alteracoes);
        }
    }

    public void limpaLista() {
        raiz.child(MINHA_LISTA).removeValue();
    }

    private static List<Produto> leProdutos(DataSnapshot no, boolean despensa) {

        List<Produto> produtos = new ArrayList<>();

        for (DataSnapshot categoriaSnapshot : no.getChildren()) {

            int categoria;

            try {
                categoria = Integer.parseInt(categoriaSnapshot.getKey());
            } catch (NumberFormatException e) {
                continue;
            }

            if (!Categorias.valida(categoria)) {
                continue;
            }

            for (DataSnapshot produtoSnapshot : categoriaSnapshot.getChildren()) {

                String nome = produtoSnapshot.getKey();
                Object valor = produtoSnapshot.getValue();

                Produto produto = despensa
                        ? Produto.daDespensa(categoria, nome, valor)
                        : Produto.daLista(categoria, nome, valor);

                // Um valor malformado não pode derrubar a tela inteira: o item é ignorado.
                if (produto != null) {
                    produtos.add(produto);
                }
            }
        }
        return produtos;
    }

    private static String caminhoDespensa(int categoria, String nome) {
        return DESPENSA + "/" + categoria + "/" + nome;
    }

    private static String caminhoLista(Produto produto) {
        return MINHA_LISTA + "/" + produto.getCategoria() + "/" + produto.getNome();
    }
}
