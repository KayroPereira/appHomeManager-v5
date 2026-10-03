package com.home.apphomemanager_v5.comodo;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.home.apphomemanager_v5.R;
import com.home.apphomemanager_v5.databinding.ItemLuzBinding;

import java.util.ArrayList;
import java.util.List;

/**
 * Cartões das luzes ajustáveis: dois controles em degraus (temperatura da cor e brilho), cada degrau
 * dispara uma cena do Smart Life. Comandos: 0 a 2 = temperatura (quente, neutra, fria) e 3 a 6 = brilho.
 */
public class LuzAdapter extends RecyclerView.Adapter<LuzAdapter.ViewHolder> {

    public static final int TEMPERATURAS = 3;
    public static final int BRILHOS = 4;

    private static final int[] ROTULOS_TEMPERATURA = {R.string.luzQuente, R.string.luzNeutra, R.string.luzFria};
    private static final int[] ROTULOS_BRILHO = {R.string.luzBrilho25, R.string.luzBrilho50, R.string.luzBrilho75, R.string.luzBrilho100};

    /** Cor de cada degrau de temperatura: âmbar, branco quente e azul claro. */
    private static final int[] CORES_TEMPERATURA = {0xFFFFB74D, 0xFFFFF3E0, 0xFF81D4FA};

    /** Brilho: o mesmo amarelo com mais ou menos presença, de 25% a 100%. */
    private static final int COR_BRILHO = 0xFFFFD54F;
    private static final float[] INTENSIDADE_BRILHO = {0.30f, 0.55f, 0.78f, 1f};

    public static class Luz {
        /** Posição do item na lista de luzes do cômodo. */
        public final int posicao;
        public final String nome;
        /** "Ligada · 70%" lida do aparelho; vazio se o item não está associado a um dispositivo. */
        public final String estado;
        /** Por comando (0 a 6): tem cena escolhida (degrau sem cena fica esmaecido). */
        public final boolean[] temCena;
        /** Último degrau acionado em cada controle (0-based dentro do controle), ou -1. */
        public final int temperaturaAtiva;
        public final int brilhoAtivo;
        public final boolean esmaecido;

        public Luz(int posicao, String nome, String estado, boolean[] temCena, int temperaturaAtiva, int brilhoAtivo, boolean esmaecido) {
            this.posicao = posicao;
            this.nome = nome;
            this.estado = estado;
            this.temCena = temCena;
            this.temperaturaAtiva = temperaturaAtiva;
            this.brilhoAtivo = brilhoAtivo;
            this.esmaecido = esmaecido;
        }
    }

    public interface AoClicar {
        void aoClicar(Luz luz, int comando);
    }

    private final AoClicar aoClicar;

    private List<Luz> luzes = new ArrayList<>();

    public LuzAdapter(AoClicar aoClicar) {
        this.aoClicar = aoClicar;
    }

    public void atualiza(List<Luz> novas) {

        luzes = novas;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemLuzBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.vincula(luzes.get(position));
    }

    @Override
    public int getItemCount() {
        return luzes.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemLuzBinding binding;

        ViewHolder(ItemLuzBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void vincula(Luz luz) {

            binding.tvLuzNome.setText(luz.nome);
            binding.tvLuzEstado.setText(luz.estado);
            binding.tvLuzEstado.setVisibility(luz.estado.isEmpty() ? View.GONE : View.VISIBLE);
            binding.llLuz.setAlpha(luz.esmaecido ? 0.45f : 1f);

            monta(binding.flLuzTemperatura, luz, 0, TEMPERATURAS, luz.temperaturaAtiva, false);
            monta(binding.flLuzBrilho, luz, TEMPERATURAS, BRILHOS, luz.brilhoAtivo, true);
        }

        /**
         * Uma fileira de blocos iguais, um por degrau: círculo colorido e rótulo. O degrau em vigor ganha o
         * contorno dos botões do alarme e um círculo com borda branca.
         */
        private void monta(FrameLayout trilho, Luz luz, int primeiroComando, int quantidade, int ativo, boolean brilho) {

            trilho.removeAllViews();

            Context contexto = trilho.getContext();
            float dp = contexto.getResources().getDisplayMetrics().density;

            LinearLayout fileira = new LinearLayout(contexto);
            fileira.setOrientation(LinearLayout.HORIZONTAL);
            fileira.setWeightSum(quantidade);
            trilho.addView(fileira, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            for (int i = 0; i < quantidade; i++) {

                int comando = primeiroComando + i;
                boolean selecionado = i == ativo;

                LinearLayout bloco = new LinearLayout(contexto);
                bloco.setOrientation(LinearLayout.VERTICAL);
                bloco.setGravity(Gravity.CENTER);
                bloco.setMinimumHeight((int) (72 * dp));
                bloco.setPadding((int) (4 * dp), (int) (8 * dp), (int) (4 * dp), (int) (8 * dp));
                bloco.setBackgroundResource(selecionado ? R.drawable.bg_botao_ativo : R.drawable.bg_campo);
                bloco.setForeground(contexto.getDrawable(android.R.drawable.list_selector_background));
                bloco.setAlpha(luz.temCena[comando] ? 1f : 0.35f);

                LinearLayout.LayoutParams parametrosBloco = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                parametrosBloco.setMarginEnd(i < quantidade - 1 ? (int) (6 * dp) : 0);
                bloco.setLayoutParams(parametrosBloco);

                // Círculo: cor da temperatura, ou amarelo com intensidade crescente no brilho.
                int cor = brilho ? comIntensidade(COR_BRILHO, INTENSIDADE_BRILHO[i]) : CORES_TEMPERATURA[i];

                GradientDrawable circulo = new GradientDrawable();
                circulo.setShape(GradientDrawable.OVAL);
                circulo.setColor(cor);
                circulo.setStroke((int) ((selecionado ? 3 : 1) * dp), selecionado ? Color.WHITE : 0x66FFFFFF);

                View bolinha = new View(contexto);
                bolinha.setLayoutParams(new LinearLayout.LayoutParams((int) (28 * dp), (int) (28 * dp)));
                bolinha.setBackground(circulo);
                bloco.addView(bolinha);

                TextView rotulo = new TextView(contexto);
                rotulo.setText(brilho ? ROTULOS_BRILHO[i] : ROTULOS_TEMPERATURA[i]);
                rotulo.setTextSize(12);
                rotulo.setTextColor(selecionado ? Color.WHITE : 0xB3FFFFFF);
                rotulo.setTypeface(null, selecionado ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
                LinearLayout.LayoutParams parametrosRotulo = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                parametrosRotulo.topMargin = (int) (6 * dp);
                rotulo.setLayoutParams(parametrosRotulo);
                bloco.addView(rotulo);

                bloco.setOnClickListener(v -> aoClicar.aoClicar(luz, comando));

                fileira.addView(bloco);
            }
        }

        private int comIntensidade(int cor, float intensidade) {

            int alfa = Math.round(255 * intensidade);
            return (alfa << 24) | (cor & 0x00FFFFFF);
        }
    }
}
