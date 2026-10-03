package com.home.apphomemanager_v5.comodo;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.home.apphomemanager_v5.R;
import com.home.apphomemanager_v5.databinding.ItemComodoListaBinding;

import java.util.ArrayList;
import java.util.List;

/** Lista de cômodos cadastrados, na tela de configuração. */
public class ComodoListaAdapter extends RecyclerView.Adapter<ComodoListaAdapter.ViewHolder> {

    public interface AoClicar {
        void aoClicar(Comodo comodo);
    }

    private final AoClicar aoClicar;

    private List<Comodo> comodos = new ArrayList<>();

    public ComodoListaAdapter(AoClicar aoClicar) {
        this.aoClicar = aoClicar;
    }

    public void atualiza(List<Comodo> novos) {

        comodos = novos;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemComodoListaBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.vincula(comodos.get(position));
    }

    @Override
    public int getItemCount() {
        return comodos.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemComodoListaBinding binding;

        ViewHolder(ItemComodoListaBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void vincula(Comodo comodo) {

            binding.ivComodoLista.setImageResource(comodo.drawableIcone());
            binding.tvComodoListaNome.setText(comodo.nome);
            binding.tvComodoListaResumo.setText(binding.getRoot().getContext().getString(
                    R.string.resumoComodo, comodo.totalItens(), comodo.gruposAtivos().size()));

            binding.tvComodoListaTuya.setText(binding.getRoot().getContext().getString(
                    R.string.resumoTuya, comodo.totalAssociados(), comodo.totalAssociaveis()));
            binding.llComodoLista.setOnClickListener(v -> aoClicar.aoClicar(comodo));
        }
    }
}
