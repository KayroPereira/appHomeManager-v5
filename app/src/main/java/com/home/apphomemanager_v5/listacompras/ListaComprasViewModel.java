package com.home.apphomemanager_v5.listacompras;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.database.DatabaseError;

import java.util.List;

/** Estado compartilhado pelas três abas: sobrevive à rotação e mantém um único listener do Firebase. */
public class ListaComprasViewModel extends ViewModel implements ListaComprasRepository.Listener {

    private static final String TAG = "ListaComprasViewModel";

    private final ListaComprasRepository repositorio = new ListaComprasRepository();

    /** Nulo até o primeiro dado chegar do Firebase. */
    private final MutableLiveData<List<Produto>> produtos = new MutableLiveData<>();

    /** Categoria aberta na aba Despensa; nulo mostra a grade de categorias. */
    private final MutableLiveData<Integer> categoriaSelecionada = new MutableLiveData<>();

    private final MutableLiveData<Boolean> erroConexao = new MutableLiveData<>(false);

    public ListaComprasViewModel() {
        repositorio.inicia(this);
    }

    @Override
    protected void onCleared() {
        repositorio.para();
    }

    @Override
    public void aoAtualizar(List<Produto> lista) {
        erroConexao.setValue(false);
        produtos.setValue(lista);
    }

    @Override
    public void aoFalhar(DatabaseError erro) {
        Log.w(TAG, "Erro ao ler a lista de compras: " + erro.getMessage());
        erroConexao.setValue(true);
    }

    public LiveData<List<Produto>> getProdutos() {
        return produtos;
    }

    public LiveData<Integer> getCategoriaSelecionada() {
        return categoriaSelecionada;
    }

    public LiveData<Boolean> getErroConexao() {
        return erroConexao;
    }

    public void selecionaCategoria(Integer categoria) {
        categoriaSelecionada.setValue(categoria);
    }

    /** Lista atual, nunca nula. */
    public List<Produto> produtosAtuais() {

        List<Produto> atuais = produtos.getValue();

        return atuais != null ? atuais : java.util.Collections.emptyList();
    }

    public ListaComprasRepository getRepositorio() {
        return repositorio;
    }
}
