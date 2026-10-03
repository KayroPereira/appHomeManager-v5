package com.home.apphomemanager_v5.listacompras;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.home.apphomemanager_v5.databinding.ItemCategoriaBinding;
import com.home.apphomemanager_v5.util.ComponentUtils;

public class CategoriaAdapter extends RecyclerView.Adapter<CategoriaAdapter.ViewHolder> {

    public interface AoClicarCategoria {
        void aoClicar(int categoria);
    }

    private final AoClicarCategoria aoClicarCategoria;

    public CategoriaAdapter(AoClicarCategoria aoClicarCategoria) {
        this.aoClicarCategoria = aoClicarCategoria;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemCategoriaBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.vincula(position);
    }

    @Override
    public int getItemCount() {
        return Categorias.quantidade();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemCategoriaBinding binding;

        ViewHolder(ItemCategoriaBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void vincula(int categoria) {

            binding.tvCategoria.setText(Categorias.nome(categoria));
            binding.ivCategoria.setImageResource(ComponentUtils.getIdImage(binding.getRoot().getContext(), Categorias.nomeDrawable(categoria)));
            binding.getRoot().setOnClickListener(v -> aoClicarCategoria.aoClicar(categoria));
        }
    }
}
