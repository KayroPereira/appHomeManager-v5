package com.home.apphomemanager_v5.comodo;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.home.apphomemanager_v5.R;
import com.home.apphomemanager_v5.databinding.ItemCortinaBinding;

import java.util.ArrayList;
import java.util.List;

/**
 * Cartões das cortinas de um cômodo: estado, barra de posição e um controle único com subir, parar e
 * descer (cada um dispara uma cena).
 */
public class CortinaAdapter extends RecyclerView.Adapter<CortinaAdapter.ViewHolder> {

    public static class Cortina {
        /** Posição do item na lista de cortinas do cômodo. */
        public final int posicao;
        public final String nome;
        /** Posição lida do motor ("Aberta", "60% aberta"...); vazio se a cortina não tem dispositivo. */
        public final String estado;
        /** Quanto está aberta (0 a 100) para a barra, ou -1 se a posição não é conhecida. */
        public final int percentualAberto;
        /** Botão em ação (0 = subir, 1 = descer, 2 = parar), ou -1: ganha a cor da cortina. */
        public final int acaoAtiva;
        /** Um botão sem cena escolhida fica esmaecido. */
        public final boolean temSubir;
        public final boolean temParar;
        public final boolean temDescer;
        public final boolean esmaecido;

        public Cortina(int posicao, String nome, String estado, int percentualAberto, int acaoAtiva, boolean temSubir, boolean temParar, boolean temDescer, boolean esmaecido) {
            this.posicao = posicao;
            this.nome = nome;
            this.estado = estado;
            this.percentualAberto = percentualAberto;
            this.acaoAtiva = acaoAtiva;
            this.temSubir = temSubir;
            this.temParar = temParar;
            this.temDescer = temDescer;
            this.esmaecido = esmaecido;
        }
    }

    public interface AoClicar {
        /** {@code comando}: 0 = subir, 1 = descer, 2 = parar (mesma numeração de {@link ItemComodo#cena(int)}). */
        void aoClicar(Cortina cortina, int comando);
    }

    public interface AoSegurar {
        /** Toque longo no cartão: mostra os dados que o dispositivo informa (útil para ver o que o motor reporta). */
        void aoSegurar(Cortina cortina);
    }

    private final AoClicar aoClicar;
    private final AoSegurar aoSegurar;

    private List<Cortina> cortinas = new ArrayList<>();

    public CortinaAdapter(AoClicar aoClicar, AoSegurar aoSegurar) {
        this.aoClicar = aoClicar;
        this.aoSegurar = aoSegurar;
    }

    public void atualiza(List<Cortina> novas) {

        cortinas = novas;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemCortinaBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.vincula(cortinas.get(position));
    }

    @Override
    public int getItemCount() {
        return cortinas.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemCortinaBinding binding;

        ViewHolder(ItemCortinaBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void vincula(Cortina cortina) {

            binding.tvCortinaNome.setText(cortina.nome);
            binding.tvCortinaEstado.setText(cortina.estado);
            binding.tvCortinaEstado.setVisibility(cortina.estado.isEmpty() ? View.GONE : View.VISIBLE);
            binding.llCortina.setAlpha(cortina.esmaecido ? 0.45f : 1f);
            binding.llCortina.setOnLongClickListener(v -> {
                aoSegurar.aoSegurar(cortina);
                return true;
            });

            exibeBarra(cortina.percentualAberto);

            configura(binding.llCortinaSubir, cortina.temSubir, cortina.acaoAtiva == 0, () -> aoClicar.aoClicar(cortina, 0));
            configura(binding.llCortinaParar, cortina.temParar, cortina.acaoAtiva == 2, () -> aoClicar.aoClicar(cortina, 2));
            configura(binding.llCortinaDescer, cortina.temDescer, cortina.acaoAtiva == 1, () -> aoClicar.aoClicar(cortina, 1));
        }

        /** A barra só aparece quando há um percentual; o preenchimento é o quanto está aberta. */
        private void exibeBarra(int percentual) {

            binding.llCortinaBarra.setVisibility(percentual >= 0 ? View.VISIBLE : View.GONE);

            LinearLayout.LayoutParams parametros = (LinearLayout.LayoutParams) binding.vCortinaPreenchida.getLayoutParams();
            parametros.weight = Math.max(0, Math.min(100, percentual));
            binding.vCortinaPreenchida.setLayoutParams(parametros);
        }

        private void configura(View botao, boolean temCena, boolean emAcao, Runnable acao) {

            botao.setAlpha(temCena ? 1f : 0.35f);
            // O botão em ação ganha um círculo em volta do ícone, e o ícone passa para a cor da cortina.
            ImageView icone = (ImageView) ((ViewGroup) botao).getChildAt(0);
            icone.setBackgroundResource(emAcao ? R.drawable.bg_segmento_ativo : 0);

            if (emAcao) {
                icone.setColorFilter(botao.getContext().getColor(R.color.corCortina));
            } else {
                icone.clearColorFilter();
            }

            botao.setOnClickListener(v -> acao.run());
        }
    }
}
