package com.home.apphomemanager_v5.tuya;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.home.apphomemanager_v5.R;
import com.home.apphomemanager_v5.databinding.ItemTuyaSwitchBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TuyaControleAdapter extends RecyclerView.Adapter<TuyaControleAdapter.ViewHolder> {

    public interface AoComandar {
        /** {@code valor} é Boolean para interruptor e Integer (valor bruto da Tuya) para slider. */
        void aoComandar(TuyaControle controle, Object valor);
    }

    private final AoComandar aoComandar;

    private List<TuyaControle> itens = new ArrayList<>();

    /** Linha que o usuário está arrastando: não pode ser sobrescrita pela atualização automática. */
    private String chaveEmInteracao;

    public TuyaControleAdapter(AoComandar aoComandar) {
        this.aoComandar = aoComandar;
    }

    public boolean emInteracao() {
        return chaveEmInteracao != null;
    }

    /**
     * Mantém as mesmas linhas quando só os valores mudaram, atualizando item a item, para que
     * a lista não pisque nem perca o arraste de um slider.
     */
    public void atualiza(List<TuyaControle> novos, boolean forcar) {

        boolean mesmaEstrutura = novos.size() == itens.size();

        for (int i = 0; mesmaEstrutura && i < novos.size(); i++) {
            mesmaEstrutura = novos.get(i).chave().equals(itens.get(i).chave());
        }

        if (forcar || !mesmaEstrutura) {
            itens = novos;
            notifyDataSetChanged();
            return;
        }

        for (int i = 0; i < novos.size(); i++) {
            if (!novos.get(i).mesmoEstadoQue(itens.get(i))) {
                itens.set(i, novos.get(i));
                notifyItemChanged(i);
            }
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemTuyaSwitchBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
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

        private final ItemTuyaSwitchBinding binding;

        ViewHolder(ItemTuyaSwitchBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void vincula(TuyaControle controle) {

            binding.swTuya.setOnCheckedChangeListener(null);
            binding.sbTuya.setOnSeekBarChangeListener(null);
            binding.getRoot().setOnClickListener(null);
            binding.getRoot().setClickable(false);

            binding.tvTuyaNome.setText(controle.titulo());
            binding.tvTuyaStatus.setText(controle.online ? R.string.online : R.string.offline);
            binding.tvTuyaStatus.setTextColor(binding.getRoot().getContext().getColor(controle.online ? R.color.onLine : R.color.offLine));

            binding.swTuya.setVisibility(controle.tipo == TuyaControle.Tipo.INTERRUPTOR ? View.VISIBLE : View.GONE);
            boolean usaSlider = controle.tipo == TuyaControle.Tipo.SLIDER || controle.tipo == TuyaControle.Tipo.SELETOR;

            binding.sbTuya.setVisibility(usaSlider ? View.VISIBLE : View.GONE);

            boolean mostraValor = usaSlider || controle.tipo == TuyaControle.Tipo.SENSOR || controle.tipo == TuyaControle.Tipo.CENA;

            binding.tvTuyaValor.setVisibility(mostraValor ? View.VISIBLE : View.GONE);

            switch (controle.tipo) {
                case INTERRUPTOR:
                    vinculaInterruptor(controle);
                    break;
                case SLIDER:
                case SELETOR:
                    vinculaSlider(controle);
                    break;
                case CENA:
                    binding.tvTuyaStatus.setText(R.string.tuyaCenaManual);
                    binding.tvTuyaStatus.setTextColor(binding.getRoot().getContext().getColor(R.color.colorTextoLista));
                    binding.tvTuyaValor.setText(R.string.tuyaExecutar);
                    binding.getRoot().setOnClickListener(v -> aoComandar.aoComandar(controle, null));
                    break;
                case TEXTO:
                    binding.tvTuyaStatus.setText(binding.getRoot().getContext().getString(controle.online ? R.string.online : R.string.offline)
                            + " · " + controle.textoFixo);
                    break;
                case INFORMATIVO:
                    String estado = binding.getRoot().getContext().getString(controle.online ? R.string.online : R.string.offline);
                    String pontos = controle.rotulo.isEmpty() ? "-" : controle.rotulo;

                    binding.tvTuyaStatus.setText(binding.getRoot().getContext().getString(R.string.tuyaSemControle, estado, pontos));
                    break;
                default:
                    binding.tvTuyaValor.setText(controle.textoSensor(Locale.getDefault()));
            }
        }

        private void vinculaInterruptor(TuyaControle controle) {

            binding.swTuya.setChecked(controle.ligado);
            binding.swTuya.setEnabled(controle.online);

            binding.swTuya.setOnCheckedChangeListener((botao, marcado) -> {
                controle.ligado = marcado;
                aoComandar.aoComandar(controle, marcado);
            });
        }

        private void vinculaSlider(TuyaControle controle) {

            binding.sbTuya.setMax(controle.maximoSlider());
            binding.sbTuya.setProgress(controle.progressoSlider());
            binding.sbTuya.setEnabled(controle.online);
            binding.tvTuyaValor.setText(controle.textoSlider(controle.progressoSlider()));

            binding.sbTuya.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progresso, boolean doUsuario) {

                    if (doUsuario) {
                        binding.tvTuyaValor.setText(controle.textoSlider(progresso));
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    chaveEmInteracao = controle.chave();
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                    chaveEmInteracao = null;

                    int progresso = seekBar.getProgress();

                    controle.valorBruto = controle.valorBrutoDoProgresso(progresso);
                    aoComandar.aoComandar(controle, controle.valorParaComando(progresso));
                }
            });
        }
    }
}
