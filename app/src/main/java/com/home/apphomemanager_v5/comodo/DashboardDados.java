package com.home.apphomemanager_v5.comodo;

import com.home.apphomemanager_v5.comodo.LeituraDispositivo.EstadoAlarme;
import com.home.apphomemanager_v5.tuya.model.TuyaDevice;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Números do dashboard da casa, somados a partir dos cômodos e do último estado conhecido de cada aparelho
 * da Tuya. Só leitura: nada aqui consulta a nuvem nem comanda dispositivos. Leituras de aparelhos fora do ar
 * ou sem estado ainda não entram nas contas; os fora do ar aparecem em {@link #foraDoAr}.
 */
public final class DashboardDados {

    /** Resumo de um cômodo; os totais são zero quando o cômodo não tem aquele tipo de dispositivo. */
    public static final class ResumoComodo {

        public final Comodo comodo;
        public int lampadas;
        public int lampadasAcesas;
        public int tomadas;
        public int tomadasLigadas;
        /** Média dos termômetros do cômodo, ou nulo se nenhum respondeu. */
        public Double temperatura;
        public Double umidade;
        /** Soma dos medidores de energia que responderam, em watts; nulo se nenhum respondeu. */
        public Double potencia;
        public int foraDoAr;
        /** Situação do alarme do cômodo (a mais grave entre os que respondem), ou nulo. */
        public EstadoAlarme alarme;
        public int alarmes;
        public int sensoresChuva;
        /** Verdadeiro se algum sensor de chuva do cômodo está chovendo; nulo se nenhum respondeu. */
        public Boolean chovendo;
        public int cortinas;
        public int cortinasAbertas;

        ResumoComodo(Comodo comodo) {
            this.comodo = comodo;
        }

        public boolean temTermometro() {
            return !comodo.termometros.isEmpty();
        }

        public boolean temMedidor() {
            return !comodo.medidores.isEmpty();
        }
    }

    public static final class Alarme {

        public final String comodo;
        public final String nome;
        public final EstadoAlarme estado;
        public final boolean foraDoAr;

        Alarme(String comodo, String nome, EstadoAlarme estado, boolean foraDoAr) {
            this.comodo = comodo;
            this.nome = nome;
            this.estado = estado;
            this.foraDoAr = foraDoAr;
        }
    }

    public static final class SensorChuva {

        public final String comodo;
        public final String nome;
        /** Nulo sem leitura (sem aparelho, fora do ar ou ponto desconhecido). */
        public final Boolean chovendo;

        SensorChuva(String comodo, String nome, Boolean chovendo) {
            this.comodo = comodo;
            this.nome = nome;
            this.chovendo = chovendo;
        }
    }

    public static final class Termometro {

        public final String comodo;
        public final String nome;
        public final Double temperatura;
        public final Double umidade;

        Termometro(String comodo, String nome, Double temperatura, Double umidade) {
            this.comodo = comodo;
            this.nome = nome;
            this.temperatura = temperatura;
            this.umidade = umidade;
        }
    }

    public final List<ResumoComodo> comodos = new ArrayList<>();
    public final List<Alarme> alarmes = new ArrayList<>();
    public final List<SensorChuva> sensoresChuva = new ArrayList<>();
    public final List<Termometro> termometros = new ArrayList<>();

    /** Aparelhos fora do ar, um por aparelho (um aparelho de várias tomadas conta uma vez): "nome (cômodo)". */
    public final List<String> foraDoAr = new ArrayList<>();

    public int lampadas;
    public int lampadasAcesas;
    public int tomadas;
    public int tomadasLigadas;
    public int cortinas;
    public int cortinasAbertas;

    /** Aparelhos distintos associados, e quantos deles estão online (os sem estado ainda não contam como online). */
    public int dispositivos;
    public int dispositivosOnline;

    public Double potencia;
    public Double temperaturaMedia;
    public Double umidadeMedia;

    private DashboardDados() {}

    /** Aparelhos distintos associados a algum item dos cômodos (a consulta de estado à Tuya custa uma chamada por id). */
    public static Set<String> idsTuya(List<Comodo> comodos) {

        Set<String> ids = new java.util.LinkedHashSet<>();

        for (Comodo comodo : comodos) {
            for (GrupoDispositivo grupo : GrupoDispositivo.values()) {
                for (ItemComodo item : comodo.itens(grupo)) {
                    if (item.associado()) {
                        ids.add(item.tuyaDeviceId);
                    }
                }
            }
        }
        return ids;
    }

    /** @param estados último estado conhecido do aparelho pelo id, ou nulo (normalmente {@code TuyaRepository::estadoEmCache}) */
    public static DashboardDados calcula(List<Comodo> comodos, Function<String, TuyaDevice> estados) {

        DashboardDados dados = new DashboardDados();

        Set<String> vistos = new HashSet<>();
        Map<String, String> offline = new LinkedHashMap<>();

        double[] soma = {0, 0, 0};
        int[] contagem = {0, 0, 0};

        for (Comodo comodo : comodos) {

            ResumoComodo resumo = new ResumoComodo(comodo);
            double[] somaComodo = {0, 0, 0};
            int[] contagemComodo = {0, 0, 0};

            for (GrupoDispositivo grupo : GrupoDispositivo.values()) {
                for (ItemComodo item : comodo.itens(grupo)) {

                    TuyaDevice aparelho = item.associado() ? estados.apply(item.tuyaDeviceId) : null;
                    boolean vivo = aparelho != null && aparelho.online;

                    if (item.associado() && vistos.add(item.tuyaDeviceId)) {
                        dados.dispositivos++;

                        if (vivo) {
                            dados.dispositivosOnline++;
                        } else if (aparelho != null) {
                            offline.put(item.tuyaDeviceId, item.nome + " (" + comodo.nome + ")");
                        }
                    }

                    if (aparelho != null && !aparelho.online && item.associado()) {
                        resumo.foraDoAr++;
                    }

                    TuyaDevice leitura = vivo ? aparelho : null;

                    switch (grupo) {
                        case LAMPADAS:
                            resumo.lampadas++;
                            resumo.lampadasAcesas += Boolean.TRUE.equals(LeituraDispositivo.ligado(leitura, item)) ? 1 : 0;
                            break;

                        case LUZ:
                            LeituraDispositivo.EstadoLuz luz = LeituraDispositivo.estadoLuz(leitura, item);
                            resumo.lampadas++;
                            resumo.lampadasAcesas += luz != null && Boolean.TRUE.equals(luz.ligada) ? 1 : 0;
                            break;

                        case TOMADAS:
                            resumo.tomadas++;
                            resumo.tomadasLigadas += Boolean.TRUE.equals(LeituraDispositivo.ligado(leitura, item)) ? 1 : 0;
                            break;

                        case MEDIDORES:
                            Double watts = LeituraDispositivo.valor(leitura, item, LeituraDispositivo.POTENCIA);

                            if (watts != null) {
                                somaComodo[0] += watts;
                                contagemComodo[0]++;
                            }
                            break;

                        case TERMOMETROS:
                            Double graus = LeituraDispositivo.valor(leitura, item, LeituraDispositivo.TEMPERATURA);
                            Double umido = LeituraDispositivo.valor(leitura, item, LeituraDispositivo.UMIDADE);

                            dados.termometros.add(new Termometro(comodo.nome, item.nome, graus, umido));

                            if (graus != null) {
                                somaComodo[1] += graus;
                                contagemComodo[1]++;
                            }
                            if (umido != null) {
                                somaComodo[2] += umido;
                                contagemComodo[2]++;
                            }
                            break;

                        case CORTINAS:
                            dados.cortinas++;
                            resumo.cortinas++;

                            LeituraDispositivo.EstadoCortina cortina = LeituraDispositivo.estadoCortina(leitura, item, false);

                            if (cortina != null && cortina.posicao != LeituraDispositivo.PosicaoCortina.FECHADA) {
                                dados.cortinasAbertas++;
                                resumo.cortinasAbertas++;
                            }
                            break;

                        case CHUVA:
                            Boolean chovendo = LeituraDispositivo.chovendo(leitura, item);

                            dados.sensoresChuva.add(new SensorChuva(comodo.nome, item.nome, chovendo));
                            resumo.sensoresChuva++;

                            if (chovendo != null) {
                                resumo.chovendo = Boolean.TRUE.equals(resumo.chovendo) || chovendo;
                            }
                            break;

                        case ALARMES:
                            Alarme alarme = new Alarme(comodo.nome, item.nome,
                                    LeituraDispositivo.estadoAlarme(leitura, item), aparelho != null && !aparelho.online);

                            dados.alarmes.add(alarme);
                            resumo.alarmes++;

                            if (!alarme.foraDoAr && alarme.estado != EstadoAlarme.DESCONHECIDO
                                    && (resumo.alarme == null || prioridade(alarme.estado) > prioridade(resumo.alarme))) {
                                resumo.alarme = alarme.estado;
                            }
                            break;

                        default:
                            break;
                    }
                }
            }

            if (contagemComodo[0] > 0) {
                resumo.potencia = somaComodo[0];
            }
            if (contagemComodo[1] > 0) {
                resumo.temperatura = somaComodo[1] / contagemComodo[1];
            }
            if (contagemComodo[2] > 0) {
                resumo.umidade = somaComodo[2] / contagemComodo[2];
            }

            soma[0] += somaComodo[0];
            soma[1] += somaComodo[1];
            soma[2] += somaComodo[2];
            contagem[0] += contagemComodo[0];
            contagem[1] += contagemComodo[1];
            contagem[2] += contagemComodo[2];

            dados.lampadas += resumo.lampadas;
            dados.lampadasAcesas += resumo.lampadasAcesas;
            dados.tomadas += resumo.tomadas;
            dados.tomadasLigadas += resumo.tomadasLigadas;

            dados.comodos.add(resumo);
        }

        dados.foraDoAr.addAll(offline.values());
        dados.potencia = contagem[0] > 0 ? soma[0] : null;
        dados.temperaturaMedia = contagem[1] > 0 ? soma[1] / contagem[1] : null;
        dados.umidadeMedia = contagem[2] > 0 ? soma[2] / contagem[2] : null;

        return dados;
    }

    /**
     * Situação geral da segurança: disparado vale mais que tudo, depois ativado, em casa e desativado.
     * Alarmes fora do ar não entram; nulo se não há nenhum alarme respondendo.
     */
    public EstadoAlarme alarmeGeral() {

        EstadoAlarme geral = null;

        for (Alarme alarme : alarmes) {

            if (alarme.foraDoAr || alarme.estado == EstadoAlarme.DESCONHECIDO) {
                continue;
            }
            if (geral == null || prioridade(alarme.estado) > prioridade(geral)) {
                geral = alarme.estado;
            }
        }
        return geral;
    }

    private static int prioridade(EstadoAlarme estado) {

        switch (estado) {
            case DISPARADO:
                return 4;
            case ATIVADO:
                return 3;
            case EM_CASA:
                return 2;
            case DESATIVADO:
                return 1;
            default:
                return 0;
        }
    }
}
