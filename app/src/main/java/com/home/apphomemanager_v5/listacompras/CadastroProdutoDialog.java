package com.home.apphomemanager_v5.listacompras;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.home.apphomemanager_v5.R;
import com.home.apphomemanager_v5.databinding.DialogCadastroProdutoBinding;

/** Cadastro de produto na despensa: nome, unidade e categoria numa única etapa. */
public class CadastroProdutoDialog {

    private CadastroProdutoDialog() {}

    public static void exibe(Context context, ListaComprasViewModel viewModel) {

        DialogCadastroProdutoBinding binding = DialogCadastroProdutoBinding.inflate(LayoutInflater.from(context));

        binding.spCategoria.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, Categorias.NOMES));

        Integer categoriaAberta = viewModel.getCategoriaSelecionada().getValue();

        if (categoriaAberta != null) {
            binding.spCategoria.setSelection(categoriaAberta);
        }

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(R.string.cadastrarLSTC)
                .setView(binding.getRoot())
                .setPositiveButton(R.string.salvar, null)
                .setNegativeButton(R.string.cancelar, null)
                .create();

        dialog.show();

        // Definido após o show() para que um dado inválido não feche o diálogo.
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {

            String nome = Produto.normalizaNome(binding.etProduto.getText() != null ? binding.etProduto.getText().toString() : "");
            int categoria = binding.spCategoria.getSelectedItemPosition();

            binding.tilProduto.setError(null);

            if (nome.isEmpty()) {
                binding.tilProduto.setError(context.getString(R.string.msgProductEmpt));
                return;
            }

            if (!Produto.nomeValido(nome)) {
                binding.tilProduto.setError(context.getString(R.string.msgNomeInvalido));
                return;
            }

            String chave = categoria + "/" + nome;

            for (Produto existente : viewModel.produtosAtuais()) {
                if (existente.chave().equals(chave)) {
                    binding.tilProduto.setError(context.getString(R.string.msgProdutoJaCadastrado));
                    return;
                }
            }

            viewModel.getRepositorio().cadastra(categoria, nome, unidadeSelecionada(binding));

            Toast.makeText(context, R.string.saveOk, Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
    }

    private static int unidadeSelecionada(DialogCadastroProdutoBinding binding) {

        int checkedId = binding.rgUnidadeCadastro.getCheckedRadioButtonId();

        if (checkedId == R.id.rbMlCadastro) {
            return Produto.UNIDADE_ML;
        }
        return checkedId == R.id.rbKgCadastro ? Produto.UNIDADE_KG : Produto.UNIDADE_UN;
    }
}
