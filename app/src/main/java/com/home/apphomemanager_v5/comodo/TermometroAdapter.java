package com.home.apphomemanager_v5.comodo;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.home.apphomemanager_v5.databinding.ItemTermometroBinding;

import java.util.ArrayList;
import java.util.List;

/** Cartões dos termômetros de um cômodo: temperatura, umidade e o termômetro que muda com a temperatura. */
public class TermometroAdapter extends RecyclerView.Adapter<TermometroAdapter.ViewHolder> {

    public static class Termometro {
        public final String nome;
        @DrawableRes
        public final int icone;
        public final String temperatura;
        public final String umidade;
        /** Sem dispositivo, sem leitura ou fora do ar. */
        public final boolean esmaecido;

        public Termometro(String nome, @DrawableRes int icone, String temperatura, String umidade, boolean esmaecido) {
            this.nome = nome;
            this.icone = icone;
            this.temperatura = temperatura;
            this.umidade = umidade;
            this.esmaecido = esmaecido;
        }
    }

    private List<Termometro> termometros = new ArrayList<>();

    public void atualiza(List<Termometro> novos) {

        termometros = novos;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemTermometroBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.vincula(termometros.get(position));
    }

    @Override
    public int getItemCount() {
        return termometros.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemTermometroBinding binding;

        ViewHolder(ItemTermometroBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void vincula(Termometro termometro) {

            binding.tvTermometroNome.setText(termometro.nome);
            binding.tvTermometroTemperatura.setText(termometro.temperatura);
            binding.tvTermometroUmidade.setText(termometro.umidade);
            binding.ivTermometro.setImageResource(termometro.icone);
            binding.llTermometro.setAlpha(termometro.esmaecido ? 0.45f : 1f);
        }
    }
}
