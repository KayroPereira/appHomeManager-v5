package com.home.apphomemanager_v5.comodo;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.home.apphomemanager_v5.R;
import com.home.apphomemanager_v5.databinding.ItemMedidorBinding;

import java.util.ArrayList;
import java.util.List;

/** Cartões dos medidores de energia de um cômodo: potência, corrente, tensão e o botão de ligar/desligar. */
public class MedidorAdapter extends RecyclerView.Adapter<MedidorAdapter.ViewHolder> {

    public static class Medidor {
        /** Posição do item na lista de medidores do cômodo. */
        public final int posicao;
        public final String nome;
        public final boolean ligado;
        /** Sem dispositivo, sem leitura ou fora do ar. */
        public final boolean esmaecido;
        public final String estado;
        public final String potencia;
        public final String corrente;
        public final String tensao;

        public Medidor(int posicao, String nome, boolean ligado, boolean esmaecido, String estado, String potencia, String corrente, String tensao) {
            this.posicao = posicao;
            this.nome = nome;
            this.ligado = ligado;
            this.esmaecido = esmaecido;
            this.estado = estado;
            this.potencia = potencia;
            this.corrente = corrente;
            this.tensao = tensao;
        }
    }

    public interface AoClicar {
        void aoClicar(Medidor medidor);
    }

    private final AoClicar aoClicar;

    private List<Medidor> medidores = new ArrayList<>();

    public MedidorAdapter(AoClicar aoClicar) {
        this.aoClicar = aoClicar;
    }

    public void atualiza(List<Medidor> novos) {

        medidores = novos;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemMedidorBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.vincula(medidores.get(position));
    }

    @Override
    public int getItemCount() {
        return medidores.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemMedidorBinding binding;

        ViewHolder(ItemMedidorBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void vincula(Medidor medidor) {

            binding.tvMedidorNome.setText(medidor.nome);
            binding.tvMedidorEstado.setText(medidor.estado);
            binding.tvMedidorPotencia.setText(medidor.potencia);
            binding.tvMedidorCorrente.setText(medidor.corrente);
            binding.tvMedidorTensao.setText(medidor.tensao);

            binding.ivMedidorOnOff.setImageResource(medidor.ligado ? R.drawable.bt_on : R.drawable.bt_off);
            binding.llMedidor.setAlpha(medidor.esmaecido ? 0.45f : 1f);

            binding.ivMedidorOnOff.setOnClickListener(v -> aoClicar.aoClicar(medidor));
        }
    }
}
