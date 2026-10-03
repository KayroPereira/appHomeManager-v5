package com.home.apphomemanager_v5.comodo;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.home.apphomemanager_v5.databinding.ItemChuvaBinding;

import java.util.ArrayList;
import java.util.List;

/** Cartões dos sensores de chuva: ícone do tempo (sol, lua ou chuva) e se está chovendo. */
public class ChuvaAdapter extends RecyclerView.Adapter<ChuvaAdapter.ViewHolder> {

    public static class Chuva {
        public final String nome;
        @DrawableRes
        public final int icone;
        public final String estado;
        /** Sem dispositivo, sem leitura ou fora do ar. */
        public final boolean esmaecido;

        public Chuva(String nome, @DrawableRes int icone, String estado, boolean esmaecido) {
            this.nome = nome;
            this.icone = icone;
            this.estado = estado;
            this.esmaecido = esmaecido;
        }
    }

    private List<Chuva> sensores = new ArrayList<>();

    public void atualiza(List<Chuva> novos) {

        sensores = novos;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemChuvaBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.vincula(sensores.get(position));
    }

    @Override
    public int getItemCount() {
        return sensores.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemChuvaBinding binding;

        ViewHolder(ItemChuvaBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void vincula(Chuva chuva) {

            binding.tvChuvaNome.setText(chuva.nome);
            binding.tvChuvaEstado.setText(chuva.estado);
            binding.ivChuva.setImageResource(chuva.icone);
            binding.llChuva.setAlpha(chuva.esmaecido ? 0.45f : 1f);
        }
    }
}
