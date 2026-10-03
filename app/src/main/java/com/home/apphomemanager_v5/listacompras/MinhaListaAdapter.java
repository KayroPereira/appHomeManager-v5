package com.home.apphomemanager_v5.listacompras;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.home.apphomemanager_v5.R;
import com.home.apphomemanager_v5.databinding.ItemMinhaListaBinding;
import com.home.apphomemanager_v5.listacompras.ListaComprasUtils.ItemMinhaLista;
import com.home.apphomemanager_v5.util.ComponentUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MinhaListaAdapter extends RecyclerView.Adapter<MinhaListaAdapter.ViewHolder> {

    public interface AoClicarProduto {
        void aoClicar(Produto produto);
    }

    private final AoClicarProduto aoClicarProduto;

    private List<ItemMinhaLista> itens = new ArrayList<>();

    public MinhaListaAdapter(AoClicarProduto aoClicarProduto) {
        this.aoClicarProduto = aoClicarProduto;
    }

    public void atualiza(List<Produto> minhaLista) {
        itens = ItemMinhaLista.monta(minhaLista);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemMinhaListaBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.vincula(itens.get(position));
    }

    @Override
    public int getItemCount() {
        return itens.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemMinhaListaBinding binding;

        ViewHolder(ItemMinhaListaBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void vincula(ItemMinhaLista item) {

            Context context = binding.getRoot().getContext();

            // O ViewHolder é reaproveitado: todo estado visual precisa ser redefinido a cada bind.
            binding.tvItemProduto.setPaintFlags(binding.tvItemProduto.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
            binding.tvItemProduto.setTypeface(Typeface.DEFAULT);
            binding.tvItemProduto.setTextSize(17);
            binding.tvItemProduto.setTextColor(context.getColor(R.color.colorTextoLista));
            binding.ivItemCategoria.setVisibility(View.GONE);
            binding.getRoot().setOnClickListener(null);
            binding.getRoot().setClickable(false);

            switch (item.tipo) {
                case ItemMinhaLista.TIPO_CATEGORIA:
                    cabecalho(context, Categorias.nome(item.categoria), Categorias.nomeDrawable(item.categoria));
                    break;

                case ItemMinhaLista.TIPO_CESTA:
                    cabecalho(context, context.getString(R.string.cestaOk), "basket");
                    break;

                case ItemMinhaLista.TIPO_VAZIO:
                    binding.tvItemProduto.setText(R.string.listClear);
                    break;

                case ItemMinhaLista.TIPO_PRODUTO_COMPRADO:
                    binding.tvItemProduto.setTextColor(context.getColor(R.color.colorCompradoLista));
                    binding.tvItemProduto.setPaintFlags(binding.tvItemProduto.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                    produto(item.produto);
                    break;

                default:
                    produto(item.produto);
            }
        }

        private void cabecalho(Context context, String texto, String nomeDrawable) {

            binding.tvItemProduto.setText(texto);
            binding.tvItemProduto.setTypeface(Typeface.DEFAULT_BOLD);
            binding.tvItemProduto.setTextSize(16);
            binding.tvItemProduto.setTextColor(context.getColor(R.color.colorTab0));
            binding.ivItemCategoria.setImageResource(ComponentUtils.getIdImage(context, nomeDrawable));
            binding.ivItemCategoria.setVisibility(View.VISIBLE);
        }

        private void produto(Produto produto) {

            binding.tvItemProduto.setText(ListaComprasUtils.formataProduto(produto, Locale.getDefault()));
            binding.getRoot().setOnClickListener(v -> aoClicarProduto.aoClicar(produto));
        }
    }
}
