package com.home.apphomemanager_v5.listacompras;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.home.apphomemanager_v5.R;
import com.home.apphomemanager_v5.databinding.FragmentMinhaListaBinding;

import java.util.List;

public class MinhaListaFragment extends Fragment {

    private FragmentMinhaListaBinding binding;

    private ListaComprasViewModel viewModel;

    private MinhaListaAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        binding = FragmentMinhaListaBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        viewModel = new ViewModelProvider(requireActivity()).get(ListaComprasViewModel.class);

        adapter = new MinhaListaAdapter(produto -> viewModel.getRepositorio().alternaCesta(produto));

        binding.rvMinhaLista.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvMinhaLista.setAdapter(adapter);

        binding.llLimparCesta.setOnClickListener(v -> confirmaLimpeza(true));
        binding.llLimparLista.setOnClickListener(v -> confirmaLimpeza(false));

        viewModel.getProdutos().observe(getViewLifecycleOwner(), produtos -> {
            if (produtos != null) {
                adapter.atualiza(ListaComprasUtils.daLista(produtos));
            }
        });
    }

    @Override
    public void onDestroyView() {

        super.onDestroyView();
        binding = null;
    }

    private void confirmaLimpeza(boolean cesta) {

        List<Produto> naLista = ListaComprasUtils.daLista(viewModel.produtosAtuais());
        int comprados = ListaComprasUtils.contaPorStatus(naLista, Produto.STATUS_COMPRADO);
        String alvo = getString(cesta ? R.string.msgCesta : R.string.msgLista);

        if (naLista.isEmpty() || (cesta && comprados == 0)) {
            Toast.makeText(requireContext(), getString(R.string.msgEmptList) + " " + alvo + ".", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle(cesta ? R.string.clearBasket : R.string.clearList)
                .setMessage(getString(R.string.msgClearProduct) + " " + alvo + "?")
                .setPositiveButton(R.string.sim, (dialog, which) -> {
                    if (cesta) {
                        viewModel.getRepositorio().limpaCesta(naLista);
                    } else {
                        viewModel.getRepositorio().limpaLista();
                    }
                })
                .setNegativeButton(R.string.nao, null)
                .show();
    }
}
