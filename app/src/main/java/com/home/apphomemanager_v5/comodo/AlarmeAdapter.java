package com.home.apphomemanager_v5.comodo;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.home.apphomemanager_v5.R;
import com.home.apphomemanager_v5.databinding.ItemAlarmeBinding;

import java.util.ArrayList;
import java.util.List;

/**
 * Cartões das centrais de alarme: status atual e um botão por modo (cada um dispara uma cena do Smart Life).
 * Os comandos seguem a numeração de {@link ItemComodo#cena(int)}.
 */
public class AlarmeAdapter extends RecyclerView.Adapter<AlarmeAdapter.ViewHolder> {

    /** Proteção total (ativar). */
    public static final int TOTAL = 0;
    public static final int DESATIVAR = 1;
    public static final int REMOVER = 2;
    public static final int PARCIAL = 3;
    public static final int SOS = 4;

    public static class Alarme {
        /** Posição do item na lista de alarmes do cômodo. */
        public final int posicao;
        public final String nome;
        public final String estado;
        /** Cor do ícone e do texto de status. */
        @ColorInt
        public final int cor;
        /** Por comando: tem cena escolhida (botão sem cena fica esmaecido). */
        public final boolean[] temCena;
        /** Comando do modo em vigor (ganha contorno), ou -1 se o status é desconhecido. */
        public final int emVigor;
        /** Sem dispositivo, sem leitura ou fora do ar. */
        public final boolean esmaecido;

        public Alarme(int posicao, String nome, String estado, @ColorInt int cor, boolean[] temCena, int emVigor, boolean esmaecido) {
            this.posicao = posicao;
            this.nome = nome;
            this.estado = estado;
            this.cor = cor;
            this.temCena = temCena;
            this.emVigor = emVigor;
            this.esmaecido = esmaecido;
        }
    }

    public interface AoClicar {
        void aoClicar(Alarme alarme, int comando);
    }

    private final AoClicar aoClicar;

    private List<Alarme> alarmes = new ArrayList<>();

    public AlarmeAdapter(AoClicar aoClicar) {
        this.aoClicar = aoClicar;
    }

    public void atualiza(List<Alarme> novos) {

        alarmes = novos;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemAlarmeBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.vincula(alarmes.get(position));
    }

    @Override
    public int getItemCount() {
        return alarmes.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemAlarmeBinding binding;

        ViewHolder(ItemAlarmeBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void vincula(Alarme alarme) {

            binding.tvAlarmeNome.setText(alarme.nome);
            binding.tvAlarmeEstado.setText(alarme.estado);
            binding.tvAlarmeEstado.setTextColor(alarme.cor);
            binding.ivAlarme.setColorFilter(alarme.cor);
            binding.llAlarme.setAlpha(alarme.esmaecido ? 0.45f : 1f);

            configura(binding.btAlarmeTotal, alarme, TOTAL);
            configura(binding.btAlarmeParcial, alarme, PARCIAL);
            configura(binding.btAlarmeDesativar, alarme, DESATIVAR);
            configura(binding.btAlarmeSos, alarme, SOS);
            configura(binding.btAlarmeRemover, alarme, REMOVER);
        }

        private void configura(View botao, Alarme alarme, int comando) {

            botao.setAlpha(alarme.temCena[comando] ? 1f : 0.4f);
            botao.setBackgroundResource(alarme.emVigor == comando ? R.drawable.bg_botao_ativo : R.drawable.bg_campo);
            botao.setOnClickListener(v -> aoClicar.aoClicar(alarme, comando));
        }
    }
}
