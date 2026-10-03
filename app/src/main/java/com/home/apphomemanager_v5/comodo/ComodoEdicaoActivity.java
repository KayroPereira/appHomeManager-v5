package com.home.apphomemanager_v5.comodo;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DatabaseError;
import com.home.apphomemanager_v5.R;
import com.home.apphomemanager_v5.databinding.ActivityComodoEdicaoBinding;
import com.home.apphomemanager_v5.databinding.CardGrupoEdicaoBinding;
import com.home.apphomemanager_v5.databinding.ItemComodoEdicaoBinding;
import com.home.apphomemanager_v5.tuya.TuyaDispositivo;
import com.home.apphomemanager_v5.tuya.TuyaRepository;
import com.home.apphomemanager_v5.tuya.model.TuyaDevice;
import com.home.apphomemanager_v5.tuya.model.TuyaScene;
import com.home.apphomemanager_v5.util.ComponentUtils;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Cria ou edita um cômodo: nome, ícone, os grupos de dispositivos que ele terá (lâmpadas, tomadas,
 * medidores de energia, termômetros) e o dispositivo Tuya de cada item. Um cômodo novo começa vazio.
 */
public class ComodoEdicaoActivity extends AppCompatActivity {

    /** Sem este extra a tela cria um cômodo novo. */
    public static final String EXTRA_ID = "comodoId";

    private ActivityComodoEdicaoBinding binding;

    private ComodoRepository repository;

    private TuyaRepository tuyaRepository;

    private Comodo comodo;

    private boolean novo;

    /** Grupos escolhidos para o cômodo; as listas de itens de um grupo fora daqui são descartadas ao salvar. */
    private final Set<GrupoDispositivo> grupos = EnumSet.noneOf(GrupoDispositivo.class);

    /** Onde ficam as linhas de cada grupo escolhido. */
    private final Map<GrupoDispositivo, LinearLayout> containers = new EnumMap<>(GrupoDispositivo.class);

    /** Dispositivos da Tuya, consultados na primeira associação da sessão. */
    private List<TuyaDispositivo> dispositivosTuya;

    /** Escolhas compatíveis com cada grupo, calculadas a partir de {@link #dispositivosTuya}. */
    private final Map<GrupoDispositivo, List<TuyaCompativeis.Opcao>> opcoes = new EnumMap<>(GrupoDispositivo.class);

    private boolean consultandoTuya = false;

    /** Cenas de acionamento manual, consultadas na primeira escolha da sessão. */
    private List<TuyaScene> cenas;

    private boolean consultandoCenas = false;

    /** A lista é lida uma vez só; entregas seguintes (Firebase depois da cópia local) são ignoradas. */
    private boolean carregado = false;

    private int iconeSelecionado = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        repository = new ComodoRepository(this);

        binding = ActivityComodoEdicaoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String id = getIntent().getStringExtra(EXTRA_ID);
        novo = id == null;

        if (TuyaRepository.estaConfigurado()) {
            tuyaRepository = TuyaRepository.getInstance();
        }

        ComponentUtils.setEventClickGeneric(binding.ivEdBack, event -> finish());

        ComponentUtils.setEventClickGeneric(binding.ivEdSalvar, event -> salva());
        ComponentUtils.setEventClickGeneric(binding.ivEdExcluir, event -> confirmaExclusao());

        binding.tvEdAddGrupo.setOnClickListener(v -> escolheGrupo());

        carrega(id);
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();

        repository.para();
    }

    /** Lê a lista uma única vez: reagir a cada mudança do Firebase apagaria o que está sendo digitado. */
    private void carrega(String id) {

        repository.inicia(new ComodoRepository.Listener() {
            @Override
            public void aoAtualizar(List<Comodo> comodos) {

                repository.para();

                if (isDestroyed() || carregado) {
                    return;
                }

                carregado = true;

                if (novo) {
                    comodo = new Comodo();
                    comodo.id = repository.novoId();
                    comodo.ordem = comodos.isEmpty() ? 0 : comodos.get(comodos.size() - 1).ordem + 1;
                } else {
                    for (Comodo candidato : comodos) {
                        if (candidato.id.equals(id)) {
                            comodo = candidato;
                        }
                    }
                    if (comodo == null) {
                        finish();
                        return;
                    }
                }

                exibe();
            }

            @Override
            public void aoFalhar(DatabaseError erro) {

                Toast.makeText(ComodoEdicaoActivity.this, R.string.erroLerComodos, Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    private void exibe() {

        binding.tvEdTitulo.setText(novo ? R.string.novoComodo : R.string.editarComodo);
        binding.etEdNome.setText(comodo.nome);
        montaIcones(comodo.posicaoIcone());

        grupos.addAll(comodo.gruposAtivos());
        montaGrupos();

        completaNomesAntigos();

        binding.ivEdExcluir.setVisibility(novo ? View.GONE : View.VISIBLE);
        binding.ivEdSalvar.setEnabled(true);
        binding.ivEdSalvar.setAlpha(1f);
        binding.pbEd.setVisibility(View.GONE);
        binding.svEd.setVisibility(View.VISIBLE);
    }

    /**
     * Monta um cartão por grupo escolhido, sempre na ordem de {@link GrupoDispositivo}. O que foi digitado
     * fica nos itens do cômodo (e não nas linhas), então reconstruir os cartões não perde nada.
     */
    private void montaGrupos() {

        binding.llEdGrupos.removeAllViews();
        containers.clear();

        for (GrupoDispositivo grupo : grupos) {

            CardGrupoEdicaoBinding card = CardGrupoEdicaoBinding.inflate(getLayoutInflater(), binding.llEdGrupos, false);

            card.ivGrupoIcone.setImageResource(grupo.icone);
            card.ivGrupoIcone.setColorFilter(getColor(grupo.cor));
            card.tvGrupoTitulo.setText(grupo.titulo);
            card.ivGrupoAdd.setContentDescription(getString(grupo.adicionar));

            card.ivGrupoAdd.setOnClickListener(v -> adicionaItem(grupo));
            card.ivGrupoRemover.setOnClickListener(v -> confirmaRemocaoGrupo(grupo));

            containers.put(grupo, card.llGrupoItens);
            binding.llEdGrupos.addView(card.getRoot());

            for (ItemComodo item : comodo.itens(grupo)) {
                adicionaLinha(card.llGrupoItens, comodo.itens(grupo), item, grupo);
            }
        }

        binding.tvEdSemGrupos.setVisibility(grupos.isEmpty() ? View.VISIBLE : View.GONE);
        binding.tvEdAddGrupo.setVisibility(grupos.size() == GrupoDispositivo.values().length ? View.GONE : View.VISIBLE);
    }

    /** Lista só os grupos que o cômodo ainda não tem. */
    private void escolheGrupo() {

        List<GrupoDispositivo> faltando = new ArrayList<>();

        for (GrupoDispositivo grupo : GrupoDispositivo.values()) {
            if (!grupos.contains(grupo)) {
                faltando.add(grupo);
            }
        }

        if (faltando.isEmpty()) {
            Toast.makeText(this, R.string.todosGruposAdicionados, Toast.LENGTH_SHORT).show();
            return;
        }

        String[] nomes = new String[faltando.size()];

        for (int i = 0; i < nomes.length; i++) {
            nomes[i] = getString(faltando.get(i).titulo);
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.escolherGrupo)
                .setItems(nomes, (dialog, which) -> {
                    grupos.add(faltando.get(which));
                    montaGrupos();
                })
                .setNegativeButton(R.string.cancelar, null)
                .show();
    }

    private void confirmaRemocaoGrupo(GrupoDispositivo grupo) {

        int quantidade = comodo.itens(grupo).size();

        if (quantidade == 0) {
            removeGrupo(grupo);
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.removerGrupoTitulo)
                .setMessage(getString(R.string.removerGrupoMensagem, getString(grupo.titulo), quantidade))
                .setPositiveButton(R.string.sim, (dialog, which) -> removeGrupo(grupo))
                .setNegativeButton(R.string.nao, null)
                .show();
    }

    private void removeGrupo(GrupoDispositivo grupo) {

        comodo.itens(grupo).clear();
        grupos.remove(grupo);
        montaGrupos();
    }

    /**
     * Associações feitas antes de o nome ser guardado só têm o código. Busca o nome só dos aparelhos
     * envolvidos (uma chamada por aparelho, ou nenhuma se já estiver em cache) e grava no item,
     * de modo que a próxima edição já o encontre.
     */
    private void completaNomesAntigos() {

        if (tuyaRepository == null) {
            return;
        }

        Set<String> ids = new LinkedHashSet<>();

        for (GrupoDispositivo grupo : grupos) {
            for (ItemComodo item : itensAssociadosSemNome(comodo.itens(grupo))) {
                ids.add(item.tuyaDeviceId);
            }
        }

        if (ids.isEmpty()) {
            return;
        }

        List<String> faltando = new ArrayList<>();

        for (String id : ids) {
            if (TuyaRepository.estadoEmCache(id) == null) {
                faltando.add(id);
            }
        }

        if (faltando.isEmpty()) {
            preencheNomes();
            return;
        }

        tuyaRepository.carregaEstados(faltando, new TuyaRepository.Resultado<Map<String, TuyaDevice>>() {
            @Override
            public void aoConcluir(Map<String, TuyaDevice> dispositivos) {

                if (!isDestroyed()) {
                    preencheNomes();
                }
            }

            @Override
            public void aoFalhar(String mensagem) {
                // Segue mostrando o código; a próxima edição tenta de novo.
            }
        });
    }

    private static List<ItemComodo> itensAssociadosSemNome(List<ItemComodo> itens) {

        List<ItemComodo> resultado = new ArrayList<>();

        for (ItemComodo item : itens) {
            if (item.associado() && (item.tuyaNome == null || item.tuyaNome.isEmpty())) {
                resultado.add(item);
            }
        }
        return resultado;
    }

    private void preencheNomes() {

        for (GrupoDispositivo grupo : grupos) {
            preencheNomes(comodo.itens(grupo), containers.get(grupo), grupo);
        }
    }

    /** Usa o nome do aparelho em cache e atualiza a linha de cada item correspondente. */
    private void preencheNomes(List<ItemComodo> itens, LinearLayout container, GrupoDispositivo grupo) {

        if (container == null) {
            return;
        }

        for (int i = 0; i < itens.size() && i < container.getChildCount(); i++) {

            ItemComodo item = itens.get(i);
            TuyaDevice dispositivo = item.associado() ? TuyaRepository.estadoEmCache(item.tuyaDeviceId) : null;

            if (dispositivo == null || dispositivo.name == null || (item.tuyaNome != null && !item.tuyaNome.isEmpty())) {
                continue;
            }

            // Mesmo formato de TuyaControle.titulo() para interruptores: o código só entra em aparelhos de vários.
            boolean simples = "switch".equals(item.tuyaCode) || "switch_1".equals(item.tuyaCode);

            item.tuyaNome = simples ? dispositivo.name : dispositivo.name + " (" + item.tuyaCode + ")";

            ItemComodoEdicaoBinding linha = (ItemComodoEdicaoBinding) container.getChildAt(i).getTag();

            if (linha != null) {
                exibeAssociacao(linha, item, grupo);
            }
        }
    }

    /** Uma linha de ícones para escolher; o selecionado ganha um contorno. */
    private void montaIcones(int selecionado) {

        iconeSelecionado = selecionado;
        binding.llEdIcones.removeAllViews();

        Comodo referencia = new Comodo();
        float densidade = getResources().getDisplayMetrics().density;
        int tamanho = (int) (52 * densidade);
        int margem = (int) (4 * densidade);
        int padding = (int) (8 * densidade);

        for (int i = 0; i < Comodo.ICONES.length; i++) {

            referencia.icone = Comodo.ICONES[i];

            ImageView icone = new ImageView(this);
            GridLayout.LayoutParams parametros = new GridLayout.LayoutParams();

            parametros.width = tamanho;
            parametros.height = tamanho;
            parametros.setMargins(0, 0, margem, margem);
            icone.setLayoutParams(parametros);
            icone.setPadding(padding, padding, padding, padding);
            icone.setImageResource(referencia.drawableIcone());
            icone.setContentDescription(getResources().getStringArray(R.array.iconesComodo)[i]);
            icone.setBackgroundResource(i == selecionado ? R.drawable.bg_icone_selecionado : 0);
            icone.setAlpha(i == selecionado ? 1f : 0.55f);

            final int posicao = i;
            icone.setOnClickListener(v -> montaIcones(posicao));

            binding.llEdIcones.addView(icone);
        }
    }

    private void adicionaItem(GrupoDispositivo grupo) {

        List<ItemComodo> itens = comodo.itens(grupo);
        ItemComodo item = new ItemComodo(getString(grupo.modeloNome, itens.size() + 1));

        itens.add(item);
        adicionaLinha(containers.get(grupo), itens, item, grupo);
    }

    private void adicionaLinha(LinearLayout container, List<ItemComodo> itens, ItemComodo item, GrupoDispositivo grupo) {

        ItemComodoEdicaoBinding linha = ItemComodoEdicaoBinding.inflate(getLayoutInflater(), container, false);

        linha.etItemNome.setText(item.nome);
        linha.etItemNome.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                item.nome = s.toString();
            }
        });

        exibeAssociacao(linha, item, grupo);
        exibeInversao(linha, item, grupo);

        linha.tvItemTuya.setOnClickListener(v -> escolheDispositivo(linha, item, grupo));
        linha.tvItemInverter.setOnClickListener(v -> {
            item.posicaoInvertida = !item.posicaoInvertida;
            exibeInversao(linha, item, grupo);
        });

        exibeCenas(linha, item, grupo);


        linha.ivItemRemover.setOnClickListener(v -> {
            itens.remove(item);
            container.removeView(linha.getRoot());
            atualizaSetas(container, itens);
        });

        linha.ivItemSobe.setOnClickListener(v -> move(container, itens, item, linha, -1));
        linha.ivItemDesce.setOnClickListener(v -> move(container, itens, item, linha, 1));

        linha.getRoot().setTag(linha);
        container.addView(linha.getRoot(), new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        atualizaSetas(container, itens);
    }

    /** Troca o item de lugar com o vizinho, na lista e na tela; a ordem da lista é a mostrada no cômodo. */
    private void move(LinearLayout container, List<ItemComodo> itens, ItemComodo item, ItemComodoEdicaoBinding linha, int deslocamento) {

        int origem = itens.indexOf(item);
        int destino = origem + deslocamento;

        if (origem < 0 || destino < 0 || destino >= itens.size()) {
            return;
        }

        itens.remove(origem);
        itens.add(destino, item);

        container.removeView(linha.getRoot());
        container.addView(linha.getRoot(), destino);

        atualizaSetas(container, itens);
    }

    /** Esmaece a seta que não leva a lugar nenhum (subir o primeiro, descer o último). */
    private void atualizaSetas(LinearLayout container, List<ItemComodo> itens) {

        for (int i = 0; i < container.getChildCount(); i++) {

            ItemComodoEdicaoBinding linha = (ItemComodoEdicaoBinding) container.getChildAt(i).getTag();

            if (linha == null) {
                continue;
            }

            linha.ivItemSobe.setAlpha(i == 0 ? 0.25f : 1f);
            linha.ivItemDesce.setAlpha(i == itens.size() - 1 ? 0.25f : 1f);
        }
    }

    /** Só a cortina associada a um motor precisa saber para que lado conta a porcentagem de abertura. */
    private void exibeInversao(ItemComodoEdicaoBinding linha, ItemComodo item, GrupoDispositivo grupo) {

        boolean mostra = grupo == GrupoDispositivo.CORTINAS && item.associado();

        linha.tvItemInverter.setVisibility(mostra ? View.VISIBLE : View.GONE);
        linha.tvItemInverter.setText(item.posicaoInvertida ? R.string.cortinaPosicaoInvertida : R.string.cortinaPosicaoNormal);
    }

    private void exibeAssociacao(ItemComodoEdicaoBinding linha, ItemComodo item, GrupoDispositivo grupo) {

        String texto;

        if (!item.associado()) {
            texto = getString(R.string.tuyaSemAssociacao);
        } else {
            TuyaCompativeis.Opcao opcao = buscaOpcao(grupo, item.chave());
            // Sem a lista carregada usa o nome guardado na associação; só em último caso mostra o código.
            String nome = opcao != null ? opcao.titulo : item.tuyaNome;
            texto = getString(R.string.tuyaAssociadoA, nome != null && !nome.isEmpty() ? nome : item.chave());
        }

        linha.tvItemTuya.setText(texto);
        // Verde quando associado, para ver de relance o que ainda falta ligar à Tuya.
        linha.tvItemTuya.setTextColor(item.associado() ? Color.parseColor("#A5D6A7") : Color.parseColor("#B3FFFFFF"));
    }

    /** Comandos de cada grupo, na ordem em que as linhas aparecem: {numero da cena, rótulo}. */
    private static int[][] cenasDoGrupo(GrupoDispositivo grupo) {

        switch (grupo.comando) {
            case CORTINA:
                return new int[][]{{0, R.string.rotuloCenaSubir}, {2, R.string.rotuloCenaParar}, {1, R.string.rotuloCenaDescer}};
            case ARMA_DESARMA:
                return new int[][]{{2, R.string.rotuloCenaRemover}, {3, R.string.rotuloCenaParcial}, {0, R.string.rotuloCenaTotal},
                        {4, R.string.rotuloCenaSos}, {1, R.string.rotuloCenaDesativar}};
            case LUZ_AJUSTAVEL:
                return new int[][]{{0, R.string.rotuloCenaQuente}, {1, R.string.rotuloCenaNeutra}, {2, R.string.rotuloCenaFria},
                        {3, R.string.rotuloCenaBrilho25}, {4, R.string.rotuloCenaBrilho50}, {5, R.string.rotuloCenaBrilho75},
                        {6, R.string.rotuloCenaBrilho100}};
            default:
                return new int[0][];
        }
    }

    private void exibeCenas(ItemComodoEdicaoBinding linha, ItemComodo item, GrupoDispositivo grupo) {

        linha.tvItemTuya.setVisibility(grupo.dispositivo ? View.VISIBLE : View.GONE);

        // Sete linhas disponíveis, de cima para baixo; as que o grupo não usa ficam escondidas.
        TextView[] linhas = {linha.tvItemCena, linha.tvItemCenaParar, linha.tvItemCenaExtra1, linha.tvItemCenaExtra2,
                linha.tvItemCenaExtra3, linha.tvItemCenaExtra4, linha.tvItemCenaDesliga};

        if (grupo.comando == GrupoDispositivo.Comando.LIGA_DESLIGA) {
            for (TextView texto : linhas) {
                texto.setVisibility(View.GONE);
            }
            exibeCenasLigaDesliga(linha, item);
            linha.tvItemCena.setOnClickListener(v -> escolheCena(linha, item, grupo, 0));
            linha.tvItemCenaDesliga.setOnClickListener(v -> escolheCena(linha, item, grupo, 1));
            return;
        }

        int[][] cenas = cenasDoGrupo(grupo);

        for (int i = 0; i < linhas.length; i++) {

            if (i >= cenas.length) {
                // Só leitura (ou linha sem uso): não há o que comandar.
                linhas[i].setVisibility(View.GONE);
                continue;
            }

            int comando = cenas[i][0];

            exibeCena(linhas[i], item, comando, cenas[i][1]);
            linhas[i].setOnClickListener(v -> escolheCena(linha, item, grupo, comando));
        }
    }

    /** Uma linha de cena com o rótulo do comando e o nome da cena escolhida (ou o convite para escolher). */
    private void exibeCena(TextView linhaCena, ItemComodo item, int comando, int rotulo) {

        String cena = item.cena(comando);
        boolean escolhida = ItemComodo.preenchida(cena);

        linhaCena.setVisibility(View.VISIBLE);
        linhaCena.setText(escolhida
                ? getString(R.string.cenaRotuloAtual, getString(rotulo), cena)
                : getString(R.string.cenaRotuloVazio, getString(rotulo)));
        linhaCena.setTextColor(escolhida ? Color.parseColor("#A5D6A7") : Color.parseColor("#B3FFFFFF"));
    }

    private void exibeCenasLigaDesliga(ItemComodoEdicaoBinding linha, ItemComodo item) {

        linha.tvItemCena.setVisibility(View.VISIBLE);
        linha.tvItemCena.setText(item.temCena()
                ? getString(R.string.cenaAssociada, item.tuyaCena)
                : getString(R.string.cenaSemAssociacao));
        linha.tvItemCena.setTextColor(item.temCena() ? Color.parseColor("#A5D6A7") : Color.parseColor("#B3FFFFFF"));

        // A cena de desligar só faz sentido depois de escolhida a principal.
        boolean temDesliga = ItemComodo.preenchida(item.tuyaCenaDesliga);

        linha.tvItemCenaDesliga.setVisibility(item.temCena() ? View.VISIBLE : View.GONE);
        linha.tvItemCenaDesliga.setText(temDesliga
                ? getString(R.string.cenaDesligaAssociada, item.tuyaCenaDesliga)
                : getString(R.string.cenaDesligaSemAssociacao));
        linha.tvItemCenaDesliga.setTextColor(temDesliga ? Color.parseColor("#A5D6A7") : Color.parseColor("#B3FFFFFF"));
    }

    private void escolheCena(ItemComodoEdicaoBinding linha, ItemComodo item, GrupoDispositivo grupo, int comando) {

        if (tuyaRepository == null) {
            Toast.makeText(this, R.string.tuyaNaoConfiguradaCurto, Toast.LENGTH_SHORT).show();
            return;
        }

        if (cenas != null) {
            mostraEscolhaCena(linha, item, grupo, comando);
            return;
        }

        if (consultandoCenas) {
            return;
        }

        consultandoCenas = true;
        Toast.makeText(this, R.string.carregandoCenas, Toast.LENGTH_SHORT).show();

        tuyaRepository.carregaCenas(null, new TuyaRepository.Resultado<List<TuyaScene>>() {
            @Override
            public void aoConcluir(List<TuyaScene> resultado) {

                consultandoCenas = false;

                if (isDestroyed()) {
                    return;
                }

                cenas = resultado;
                mostraEscolhaCena(linha, item, grupo, comando);
            }

            @Override
            public void aoFalhar(String mensagem) {

                consultandoCenas = false;

                if (!isDestroyed()) {
                    Toast.makeText(ComodoEdicaoActivity.this, getString(R.string.tuyaErro, mensagem), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void mostraEscolhaCena(ItemComodoEdicaoBinding linha, ItemComodo item, GrupoDispositivo grupo, int comando) {

        if (cenas.isEmpty()) {
            Toast.makeText(this, R.string.tuyaSemCenas, Toast.LENGTH_LONG).show();
            return;
        }

        String atual = item.cena(comando);

        // Posição 0 é "Nenhum"; as demais seguem a lista de cenas.
        String[] nomes = new String[cenas.size() + 1];
        nomes[0] = getString(R.string.tuyaNenhum);

        int marcado = 0;

        for (int i = 0; i < cenas.size(); i++) {

            nomes[i + 1] = cenas.get(i).name;

            if (cenas.get(i).name.equals(atual)) {
                marcado = i + 1;
            }
        }

        new AlertDialog.Builder(this)
                .setTitle(tituloEscolhaCena(grupo, comando))
                .setSingleChoiceItems(nomes, marcado, (dialog, which) -> {

                    String escolhida = which == 0 ? null : cenas.get(which - 1).name;

                    item.defineCena(comando, escolhida);

                    // Em liga/desliga a cena de desligar depende da principal.
                    if (grupo.comando == GrupoDispositivo.Comando.LIGA_DESLIGA && comando == 0 && escolhida == null) {
                        item.tuyaCenaDesliga = null;
                    }

                    exibeCenas(linha, item, grupo);
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.cancelar, null)
                .show();
    }

    private static int tituloEscolhaCena(GrupoDispositivo grupo, int comando) {

        switch (grupo.comando) {
            case ARMA_DESARMA:
                return comando == 0 ? R.string.rotuloCenaTotal : comando == 1 ? R.string.rotuloCenaDesativar
                        : comando == 2 ? R.string.rotuloCenaRemover : comando == 3 ? R.string.rotuloCenaParcial : R.string.rotuloCenaSos;
            case LUZ_AJUSTAVEL:
                return new int[]{R.string.rotuloCenaQuente, R.string.rotuloCenaNeutra, R.string.rotuloCenaFria, R.string.rotuloCenaBrilho25,
                        R.string.rotuloCenaBrilho50, R.string.rotuloCenaBrilho75, R.string.rotuloCenaBrilho100}[comando];
            case CORTINA:
                return comando == 0 ? R.string.rotuloCenaSubir : comando == 1 ? R.string.rotuloCenaDescer : R.string.rotuloCenaParar;
            default:
                return comando == 0 ? R.string.escolherCena : R.string.escolherCenaDesliga;
        }
    }

    private TuyaCompativeis.Opcao buscaOpcao(GrupoDispositivo grupo, String chave) {

        List<TuyaCompativeis.Opcao> lista = opcoes.get(grupo);

        if (lista != null) {
            for (TuyaCompativeis.Opcao opcao : lista) {
                if (opcao.chave().equals(chave)) {
                    return opcao;
                }
            }
        }
        return null;
    }

    private void escolheDispositivo(ItemComodoEdicaoBinding linha, ItemComodo item, GrupoDispositivo grupo) {

        if (tuyaRepository == null) {
            Toast.makeText(this, R.string.tuyaNaoConfiguradaCurto, Toast.LENGTH_SHORT).show();
            return;
        }

        if (dispositivosTuya != null) {
            mostraEscolha(linha, item, grupo);
            return;
        }

        if (consultandoTuya) {
            return;
        }

        consultandoTuya = true;
        Toast.makeText(this, R.string.carregandoDispositivos, Toast.LENGTH_SHORT).show();

        tuyaRepository.carregaDispositivos(new TuyaRepository.Resultado<List<TuyaDispositivo>>() {
            @Override
            public void aoConcluir(List<TuyaDispositivo> dispositivos) {

                consultandoTuya = false;

                if (isDestroyed()) {
                    return;
                }

                dispositivosTuya = dispositivos;
                mostraEscolha(linha, item, grupo);
            }

            @Override
            public void aoFalhar(String mensagem) {

                consultandoTuya = false;

                if (!isDestroyed()) {
                    Toast.makeText(ComodoEdicaoActivity.this, getString(R.string.tuyaErro, mensagem), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    /** Mostra só os dispositivos compatíveis com o grupo do item. */
    private void mostraEscolha(ItemComodoEdicaoBinding linha, ItemComodo item, GrupoDispositivo grupo) {

        List<TuyaCompativeis.Opcao> compativeis = opcoes.get(grupo);

        if (compativeis == null) {
            compativeis = TuyaCompativeis.para(grupo, dispositivosTuya);
            opcoes.put(grupo, compativeis);
        }

        if (compativeis.isEmpty()) {
            Toast.makeText(this, semCompativeis(grupo), Toast.LENGTH_LONG).show();
            return;
        }

        // Posição 0 é "Nenhum"; as demais seguem a lista de dispositivos compatíveis.
        String[] nomes = new String[compativeis.size() + 1];
        nomes[0] = getString(R.string.tuyaNenhum);

        int marcado = 0;

        for (int i = 0; i < compativeis.size(); i++) {

            TuyaCompativeis.Opcao opcao = compativeis.get(i);

            nomes[i + 1] = opcao.online ? opcao.titulo : getString(R.string.tuyaTituloOffline, opcao.titulo);

            if (item.associado() && opcao.chave().equals(item.chave())) {
                marcado = i + 1;
            }
        }

        List<TuyaCompativeis.Opcao> lista = compativeis;

        new AlertDialog.Builder(this)
                .setTitle(R.string.associarTuya)
                .setSingleChoiceItems(nomes, marcado, (dialog, which) -> {

                    if (which == 0) {
                        item.desassocia();
                    } else {
                        TuyaCompativeis.Opcao escolhida = lista.get(which - 1);

                        item.associa(escolhida.deviceId, escolhida.code, escolhida.titulo);
                        item.escalas = new HashMap<>(escolhida.escalas);
                    }

                    exibeAssociacao(linha, item, grupo);
                    exibeInversao(linha, item, grupo);
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.cancelar, null)
                .show();
    }

    private static int semCompativeis(GrupoDispositivo grupo) {

        switch (grupo) {
            case MEDIDORES:
                return R.string.tuyaSemMedidores;
            case TERMOMETROS:
                return R.string.tuyaSemTermometros;
            case CHUVA:
                return R.string.tuyaSemSensoresChuva;
            case ALARMES:
                return R.string.tuyaSemAlarmes;
            case CORTINAS:
                return R.string.tuyaSemCortinas;
            case LUZ:
                return R.string.tuyaSemLuzes;
            default:
                return R.string.tuyaSemInterruptores;
        }
    }

    private void salva() {

        String nome = binding.etEdNome.getText() != null ? binding.etEdNome.getText().toString().trim() : "";

        if (nome.isEmpty()) {
            binding.tvEdErroNome.setVisibility(View.VISIBLE);
            return;
        }

        binding.tvEdErroNome.setVisibility(View.GONE);

        comodo.nome = nome;
        comodo.icone = Comodo.ICONES[iconeSelecionado];

        comodo.grupos = new ArrayList<>();

        for (GrupoDispositivo grupo : grupos) {
            comodo.grupos.add(grupo.chave);
            nomeiaItensVazios(comodo.itens(grupo), grupo.modeloNome);
        }

        repository.salva(comodo);

        finish();
    }

    private void nomeiaItensVazios(List<ItemComodo> itens, int modelo) {

        for (int i = 0; i < itens.size(); i++) {

            ItemComodo item = itens.get(i);

            if (item.nome == null || item.nome.trim().isEmpty()) {
                item.nome = getString(modelo, i + 1);
            } else {
                item.nome = item.nome.trim();
            }
        }
    }

    private void confirmaExclusao() {

        new AlertDialog.Builder(this)
                .setTitle(R.string.excluirComodoTitulo)
                .setMessage(getString(R.string.excluirComodoMensagem, comodo.nome))
                .setPositiveButton(R.string.sim, (dialog, which) -> {
                    repository.remove(comodo.id);
                    finish();
                })
                .setNegativeButton(R.string.nao, null)
                .show();
    }
}
