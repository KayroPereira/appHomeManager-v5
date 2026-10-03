package com.home.apphomemanager_v5.listacompras;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.RadioButton;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.home.apphomemanager_v5.R;
import com.home.apphomemanager_v5.databinding.ItemProdutoDespensaBinding;
import com.home.apphomemanager_v5.util.ComponentUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Produtos de uma categoria da despensa, com quantidade e unidade editáveis antes de ir para a lista. */
public class ProdutoDespensaAdapter extends RecyclerView.Adapter<ProdutoDespensaAdapter.ViewHolder> {

    public interface Acoes {
        /** Adiciona à lista de compras o produto com a quantidade e a unidade escolhidas. */
        void aoAdicionar(Produto produto);

        void aoRemoverDaLista(Produto produto);

        void aoExcluir(Produto produto);

        void aoQuantidadeInvalida();
    }

    /** Edição ainda não enviada. Sobrevive ao reaproveitamento das linhas e às atualizações do Firebase. */
    private static class Edicao {
        String quantidade;
        int unidade;

        Edicao(String quantidade, int unidade) {
            this.quantidade = quantidade;
            this.unidade = unidade;
        }
    }

    private final Acoes acoes;

    private final Map<String, Edicao> edicoes = new HashMap<>();

    private List<Produto> produtos = new ArrayList<>();

    public ProdutoDespensaAdapter(Acoes acoes) {
        this.acoes = acoes;
    }

    public void atualiza(List<Produto> novos) {

        produtos = novos;

        // Só mantém a edição de quem ainda está disponível na despensa.
        edicoes.keySet().removeIf(chave -> novos.stream().noneMatch(p -> p.chave().equals(chave) && p.getStatus() == Produto.STATUS_DISPONIVEL));

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemProdutoDespensaBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.vincula(produtos.get(position));
    }

    @Override
    public int getItemCount() {
        return produtos.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemProdutoDespensaBinding binding;

        private Produto produto;

        /** Evita gravar como edição do usuário o texto que o bind acabou de colocar. */
        private boolean vinculando = false;

        ViewHolder(ItemProdutoDespensaBinding binding) {

            super(binding.getRoot());
            this.binding = binding;

            binding.etQuantidade.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (!vinculando && produto != null) {
                        edicao().quantidade = s.toString();
                    }
                }
            });

            binding.rgUnidade.setOnCheckedChangeListener((group, checkedId) -> {
                if (!vinculando && produto != null) {
                    edicao().unidade = unidadeSelecionada();
                }
            });

            binding.ivAcaoProduto.setOnClickListener(v -> aoClicarAcao());

            binding.tvProdutoDespensa.setOnLongClickListener(v -> {
                if (produto != null) {
                    acoes.aoExcluir(produto);
                }
                return true;
            });
        }

        void vincula(Produto novo) {

            produto = novo;
            vinculando = true;

            try {
                Context context = binding.getRoot().getContext();
                boolean disponivel = novo.getStatus() == Produto.STATUS_DISPONIVEL;

                Edicao edicao = disponivel ? edicoes.get(novo.chave()) : null;

                String quantidade = edicao != null ? edicao.quantidade : ListaComprasUtils.formataQuantidade(novo.getQuantidade(), Locale.getDefault());
                int unidade = edicao != null ? edicao.unidade : novo.getUnidade();

                binding.tvProdutoDespensa.setText(novo.getNome());
                binding.etQuantidade.setText(quantidade);
                marcaUnidade(unidade);

                binding.ivAcaoProduto.setImageResource(ComponentUtils.getIdImage(context, disponivel ? "basket" : "clbasket"));
                binding.ivAcaoProduto.setContentDescription(context.getString(disponivel ? R.string.msgProductAdd : R.string.msgProductRemove));

                // Na lista, quantidade e unidade ficam travadas: para mudar, tira da lista e adiciona de novo.
                binding.etQuantidade.setEnabled(disponivel);
                binding.rgUnidade.setEnabled(disponivel);

                for (int i = 0; i < binding.rgUnidade.getChildCount(); i++) {
                    binding.rgUnidade.getChildAt(i).setEnabled(disponivel);
                }
            } finally {
                vinculando = false;
            }
        }

        private void aoClicarAcao() {

            if (produto == null) {
                return;
            }

            binding.etQuantidade.clearFocus();

            if (produto.getStatus() != Produto.STATUS_DISPONIVEL) {
                acoes.aoRemoverDaLista(produto);
                return;
            }

            Float quantidade = ListaComprasUtils.parseQuantidade(binding.etQuantidade.getText().toString());

            if (quantidade == null) {
                acoes.aoQuantidadeInvalida();
                return;
            }

            edicoes.remove(produto.chave());
            acoes.aoAdicionar(produto.comQuantidadeEUnidade(quantidade, unidadeSelecionada()));
        }

        private Edicao edicao() {

            Edicao edicao = edicoes.get(produto.chave());

            if (edicao == null) {
                edicao = new Edicao(binding.etQuantidade.getText().toString(), unidadeSelecionada());
                edicoes.put(produto.chave(), edicao);
            }
            return edicao;
        }

        private int unidadeSelecionada() {

            int checkedId = binding.rgUnidade.getCheckedRadioButtonId();

            if (checkedId == R.id.rbMl) {
                return Produto.UNIDADE_ML;
            }
            return checkedId == R.id.rbKg ? Produto.UNIDADE_KG : Produto.UNIDADE_UN;
        }

        private void marcaUnidade(int unidade) {

            RadioButton radio = unidade == Produto.UNIDADE_ML ? binding.rbMl : unidade == Produto.UNIDADE_KG ? binding.rbKg : binding.rbUn;

            radio.setChecked(true);
        }
    }
}
