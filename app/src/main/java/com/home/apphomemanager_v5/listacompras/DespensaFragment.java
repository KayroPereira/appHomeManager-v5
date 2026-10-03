package com.home.apphomemanager_v5.listacompras;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.home.apphomemanager_v5.R;
import com.home.apphomemanager_v5.databinding.FragmentDespensaBinding;
import com.home.apphomemanager_v5.util.ComponentUtils;

import java.util.List;

public class DespensaFragment extends Fragment implements ProdutoDespensaAdapter.Acoes {

    private static final int COLUNAS_CATEGORIAS = 3;

    private FragmentDespensaBinding binding;

    private ListaComprasViewModel viewModel;

    private ProdutoDespensaAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        binding = FragmentDespensaBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        viewModel = new ViewModelProvider(requireActivity()).get(ListaComprasViewModel.class);

        binding.rvCategorias.setLayoutManager(new GridLayoutManager(requireContext(), COLUNAS_CATEGORIAS));
        binding.rvCategorias.setAdapter(new CategoriaAdapter(categoria -> viewModel.selecionaCategoria(categoria)));

        adapter = new ProdutoDespensaAdapter(this);

        binding.rvProdutosDespensa.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvProdutosDespensa.setAdapter(adapter);

        binding.ivVoltarCategoria.setOnClickListener(v -> viewModel.selecionaCategoria(null));

        // Com uma categoria aberta, "voltar" fecha a categoria em vez de sair da tela.
        OnBackPressedCallback voltaParaCategorias = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                viewModel.selecionaCategoria(null);
            }
        };

        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), voltaParaCategorias);

        viewModel.getCategoriaSelecionada().observe(getViewLifecycleOwner(), categoria -> {
            voltaParaCategorias.setEnabled(categoria != null);
            atualizaTela();
        });

        viewModel.getProdutos().observe(getViewLifecycleOwner(), produtos -> atualizaTela());
    }

    @Override
    public void onDestroyView() {

        super.onDestroyView();
        binding = null;
    }

    private void atualizaTela() {

        Integer categoria = viewModel.getCategoriaSelecionada().getValue();
        boolean abriuCategoria = categoria != null;

        binding.rvCategorias.setVisibility(abriuCategoria ? View.GONE : View.VISIBLE);
        binding.clProdutosCategoria.setVisibility(abriuCategoria ? View.VISIBLE : View.GONE);

        if (!abriuCategoria) {
            return;
        }

        binding.tvNomeCategoria.setText(Categorias.nome(categoria));
        binding.ivIconeCategoria.setImageResource(ComponentUtils.getIdImage(requireContext(), Categorias.nomeDrawable(categoria)));

        List<Produto> daCategoria = ListaComprasUtils.daCategoria(viewModel.produtosAtuais(), categoria);

        binding.tvSemProdutos.setVisibility(daCategoria.isEmpty() ? View.VISIBLE : View.GONE);
        adapter.atualiza(daCategoria);
    }

    @Override
    public void aoAdicionar(Produto produto) {

        viewModel.getRepositorio().adicionaNaLista(produto);
        Toast.makeText(requireContext(), produto.getNome() + " " + getString(R.string.msgProductAdd), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void aoRemoverDaLista(Produto produto) {

        viewModel.getRepositorio().removeDaLista(produto);
        Toast.makeText(requireContext(), produto.getNome() + " " + getString(R.string.msgProductRemove), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void aoExcluir(Produto produto) {

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.tituloRemoverProduto)
                .setMessage(getString(R.string.msgRemoverProduto, produto.getNome()))
                .setPositiveButton(R.string.sim, (dialog, which) -> viewModel.getRepositorio().remove(produto))
                .setNegativeButton(R.string.nao, null)
                .show();
    }

    @Override
    public void aoQuantidadeInvalida() {
        Toast.makeText(requireContext(), R.string.msgQuantidadeInvalida, Toast.LENGTH_SHORT).show();
    }
}
