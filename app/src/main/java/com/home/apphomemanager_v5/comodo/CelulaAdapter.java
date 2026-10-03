package com.home.apphomemanager_v5.comodo;

import android.graphics.PorterDuff;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.home.apphomemanager_v5.R;
import com.home.apphomemanager_v5.databinding.ItemCelulaBinding;

import java.util.ArrayList;
import java.util.List;

/** Grade de ícones com legenda, usada no dashboard de cômodos e nas lâmpadas/tomadas de um cômodo. */
public class CelulaAdapter extends RecyclerView.Adapter<CelulaAdapter.ViewHolder> {

    public static class Celula {
        public final String id;
        public final String nome;
        @DrawableRes
        public final int icone;
        /** Item sem dispositivo ou dispositivo fora do ar aparece esmaecido. */
        public final boolean esmaecido;
        /** Cor de destaque (0 = ícone original, sem filtro). */
        public final int cor;
        /** Ligado: brilho atrás do ícone e borda na cor de destaque. */
        public final boolean ligado;

        public Celula(String id, String nome, @DrawableRes int icone, boolean esmaecido) {
            this(id, nome, icone, esmaecido, 0, false);
        }

        public Celula(String id, String nome, @DrawableRes int icone, boolean esmaecido, int cor, boolean ligado) {
            this.id = id;
            this.nome = nome;
            this.icone = icone;
            this.esmaecido = esmaecido;
            this.cor = cor;
            this.ligado = ligado;
        }
    }

    public interface AoClicar {
        void aoClicar(Celula celula);
    }

    private final AoClicar aoClicar;

    private List<Celula> celulas = new ArrayList<>();

    public CelulaAdapter(AoClicar aoClicar) {
        this.aoClicar = aoClicar;
    }

    public void atualiza(List<Celula> novas) {

        celulas = novas;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemCelulaBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.vincula(celulas.get(position));
    }

    @Override
    public int getItemCount() {
        return celulas.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemCelulaBinding binding;

        ViewHolder(ItemCelulaBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void vincula(Celula celula) {

            binding.tvCelula.setText(celula.nome);
            binding.ivCelula.setImageResource(celula.icone);
            binding.getRoot().setAlpha(celula.esmaecido ? 0.45f : 1f);

            aplicaDestaque(celula);

            binding.llCelula.setOnClickListener(v -> {
                v.animate().cancel();
                v.animate().scaleX(0.94f).scaleY(0.94f).setDuration(70)
                        .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(120).start())
                        .start();
                aoClicar.aoClicar(celula);
            });
        }

        private void aplicaDestaque(Celula celula) {

            float dp = binding.getRoot().getResources().getDisplayMetrics().density;

            if (celula.cor == 0) {
                binding.ivCelula.clearColorFilter();
            } else {
                binding.ivCelula.setColorFilter(celula.cor, PorterDuff.Mode.SRC_IN);
            }

            if (!celula.ligado) {
                binding.vBrilhoCelula.setVisibility(View.INVISIBLE);
                binding.llCelula.setBackgroundResource(R.drawable.bg_card_vidro);
                return;
            }

            GradientDrawable brilho = new GradientDrawable();
            brilho.setShape(GradientDrawable.OVAL);
            brilho.setGradientType(GradientDrawable.RADIAL_GRADIENT);
            brilho.setGradientRadius(32 * dp);
            brilho.setColors(new int[]{(celula.cor & 0x00FFFFFF) | 0x80000000, (celula.cor & 0x00FFFFFF)});
            binding.vBrilhoCelula.setBackground(brilho);
            binding.vBrilhoCelula.setVisibility(View.VISIBLE);

            GradientDrawable card = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{(celula.cor & 0x00FFFFFF) | 0x4D000000, (celula.cor & 0x00FFFFFF) | 0x1A000000});
            card.setCornerRadius(20 * dp);
            card.setStroke((int) (1.5f * dp), (celula.cor & 0x00FFFFFF) | 0xB3000000);
            binding.llCelula.setBackground(card);
        }
    }
}
