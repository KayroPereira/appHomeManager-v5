package com.home.apphomemanager_v5.listacompras;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.home.apphomemanager_v5.R;
import com.home.apphomemanager_v5.databinding.FragmentOpcoesBinding;

import java.util.List;
import java.util.Locale;

public class OpcoesFragment extends Fragment {

    private FragmentOpcoesBinding binding;

    private ListaComprasViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        binding = FragmentOpcoesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        viewModel = new ViewModelProvider(requireActivity()).get(ListaComprasViewModel.class);

        binding.llCadastrar.setOnClickListener(v -> CadastroProdutoDialog.exibe(requireContext(), viewModel));
        binding.llCompartilhar.setOnClickListener(v -> compartilha());
    }

    @Override
    public void onDestroyView() {

        super.onDestroyView();
        binding = null;
    }

    private void compartilha() {

        List<Produto> minhaLista = ListaComprasUtils.daLista(viewModel.produtosAtuais());

        if (minhaLista.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.msgEmptList) + getString(R.string.msgLista) + ".", Toast.LENGTH_SHORT).show();
            return;
        }

        String texto = ListaComprasUtils.textoCompartilhamento(
                minhaLista,
                getString(R.string.msgLstCompras),
                getString(R.string.cestaOk),
                getString(R.string.listClear),
                Locale.getDefault());

        Intent envio = new Intent(Intent.ACTION_SEND);
        envio.setType("text/plain");
        envio.putExtra(Intent.EXTRA_TEXT, texto);

        startActivity(Intent.createChooser(envio, getString(R.string.msgCompartilharLst)));
    }
}
