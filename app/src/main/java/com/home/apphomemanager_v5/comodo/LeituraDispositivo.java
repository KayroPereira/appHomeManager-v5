package com.home.apphomemanager_v5.comodo;

import com.home.apphomemanager_v5.tuya.TuyaControle;
import com.home.apphomemanager_v5.tuya.model.TuyaDevice;
import com.home.apphomemanager_v5.tuya.model.TuyaStatus;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.Locale;

/**
 * Leituras de medidores de energia e termômetros a partir do estado de um aparelho da Tuya.
 * Cada grandeza pode vir em mais de um código, conforme o fabricante; vale o primeiro presente.
 */
public final class LeituraDispositivo {

    public enum EstadoAlarme { ATIVADO, DESATIVADO, EM_CASA, DISPARADO, DESCONHECIDO }

    public static final String[] POTENCIA = {"cur_power"};
    /** Em miliampères. */
    public static final String[] CORRENTE = {"cur_current"};
    public static final String[] TENSAO = {"cur_voltage"};
    public static final String[] TEMPERATURA = {"temp_current", "va_temperature"};
    public static final String[] UMIDADE = {"humidity_value", "va_humidity"};

    /** Modo da central de alarme: arm, disarmed, home ou sos. */
    public static final String[] ALARME = {"master_mode"};

    /** Posição da cortina em porcentagem: a atual primeiro, depois a de comando. */
    public static final String[] POSICAO_CORTINA = {"percent_state", "position", "percent_control"};

    /** Brilho e temperatura da cor das lâmpadas (as versões v2 têm outra faixa, guardada na associação). */
    public static final String[] BRILHO_LUZ = {"bright_value_v2", "bright_value"};
    public static final String[] TEMPERATURA_COR_LUZ = {"temp_value_v2", "temp_value"};
    public static final String[] LIGADA_LUZ = {"switch_led", "switch_1", "switch"};

    private static final String SEM_LEITURA = "--";

    private LeituraDispositivo() {}

    /** Valor com a escala aplicada (24,5 e não 245), ou nulo se o aparelho não informa a grandeza. */
    public static Double valor(TuyaDevice dispositivo, ItemComodo item, String[] codigos) {

        if (dispositivo == null || dispositivo.status == null) {
            return null;
        }

        for (String codigo : codigos) {
            for (TuyaStatus status : dispositivo.status) {
                if (codigo.equals(status.code) && status.value instanceof Number) {
                    int escala = item.escala(codigo, TuyaControle.escalaPadrao(codigo));
                    return ((Number) status.value).doubleValue() / Math.pow(10, escala);
                }
            }
        }
        return null;
    }

    /**
     * Posição bruta da cortina (0 a 100), ou nula se o aparelho não informa. Alguns motores deixam o
     * {@code percent_state} parado em 0 e só atualizam o {@code percent_control}: por isso vale o primeiro
     * ponto com valor diferente de zero (o associado antes), e só então o zero.
     */
    public static Double posicaoCortina(TuyaDevice dispositivo, ItemComodo item) {

        if (dispositivo == null || dispositivo.status == null) {
            return null;
        }

        java.util.List<String> ordem = new java.util.ArrayList<>();

        if (item.tuyaCode != null) {
            ordem.add(item.tuyaCode);
        }
        ordem.addAll(Arrays.asList(POSICAO_CORTINA));

        boolean algum = false;

        for (String codigo : ordem) {
            for (TuyaStatus status : dispositivo.status) {
                if (codigo.equals(status.code) && status.value instanceof Number) {

                    double valor = ((Number) status.value).doubleValue();
                    algum = true;

                    if (valor != 0) {
                        return valor;
                    }
                }
            }
        }
        return algum ? 0.0 : null;
    }

    public enum PosicaoCortina { ABERTA, FECHADA, PARCIAL, ABRINDO, FECHANDO }

    /** Estado da cortina; o percentual só existe quando o motor informa uma posição. */
    public static final class EstadoCortina {

        public final PosicaoCortina posicao;
        public final Integer percentual;

        EstadoCortina(PosicaoCortina posicao, Integer percentual) {
            this.posicao = posicao;
            this.percentual = percentual;
        }
    }

    /**
     * Estado da cortina. O último comando ({@code control}) diz o que o motor foi mandado fazer; a
     * porcentagem só é confiável com o motor parado, pois a Tuya a atualiza ao parar ou ao chegar no fim.
     * {@code emMovimento} é verdadeiro logo depois de um comando enviado por esta tela: enquanto a posição
     * não confirma o fim do curso o cartão mostra "abrindo" ou "fechando". Nulo se nada serve.
     */
    public static EstadoCortina estadoCortina(TuyaDevice dispositivo, ItemComodo item, boolean emMovimento) {

        Double bruta = posicaoCortina(dispositivo, item);
        String controle = null;

        if (dispositivo != null && dispositivo.status != null) {
            for (TuyaStatus status : dispositivo.status) {
                if ("control".equals(status.code) && status.value instanceof String) {
                    controle = ((String) status.value).toLowerCase(Locale.ROOT);
                }
            }
        }

        Integer aberto = bruta == null ? null : percentualAberto(bruta, item.posicaoInvertida);

        if (controle == null) {
            return aberto == null ? null : porPosicao(aberto);
        }

        switch (controle) {
            case "open":
                if (aberto != null && aberto >= 95) {
                    return new EstadoCortina(PosicaoCortina.ABERTA, 100);
                }
                return new EstadoCortina(emMovimento ? PosicaoCortina.ABRINDO : PosicaoCortina.ABERTA, null);

            case "close":
                if (aberto != null && aberto <= 5) {
                    return new EstadoCortina(PosicaoCortina.FECHADA, 0);
                }
                return new EstadoCortina(emMovimento ? PosicaoCortina.FECHANDO : PosicaoCortina.FECHADA, null);

            case "stop":
                return aberto == null ? new EstadoCortina(PosicaoCortina.PARCIAL, null) : porPosicao(aberto);

            default:
                return aberto == null ? null : porPosicao(aberto);
        }
    }

    private static EstadoCortina porPosicao(int aberto) {

        if (aberto >= 95) {
            return new EstadoCortina(PosicaoCortina.ABERTA, 100);
        }
        if (aberto <= 5) {
            return new EstadoCortina(PosicaoCortina.FECHADA, 0);
        }
        return new EstadoCortina(PosicaoCortina.PARCIAL, aberto);
    }

    /** Estado da luz ajustável: ligada e os dois controles em porcentagem (nulos se o aparelho não informa). */
    public static final class EstadoLuz {

        public final Boolean ligada;
        public final Integer brilho;
        public final Integer temperatura;

        EstadoLuz(Boolean ligada, Integer brilho, Integer temperatura) {
            this.ligada = ligada;
            this.brilho = brilho;
            this.temperatura = temperatura;
        }
    }

    /** Porcentagem do ponto dentro da faixa guardada na associação (padrão 10 a 1000, a faixa da Tuya). */
    private static Integer percentualDaFaixa(TuyaDevice dispositivo, ItemComodo item, String[] codigos, String chaveMinimo, String chaveMaximo, int minimoPadrao) {

        for (String codigo : codigos) {
            for (TuyaStatus status : dispositivo.status) {
                if (codigo.equals(status.code) && status.value instanceof Number) {

                    double minimo = item.escala(chaveMinimo, minimoPadrao);
                    double maximo = item.escala(chaveMaximo, 1000);
                    double valor = ((Number) status.value).doubleValue();

                    if (maximo <= minimo) {
                        return null;
                    }
                    return (int) Math.round(Math.max(0, Math.min(100, 100 * (valor - minimo) / (maximo - minimo))));
                }
            }
        }
        return null;
    }

    public static EstadoLuz estadoLuz(TuyaDevice dispositivo, ItemComodo item) {

        if (dispositivo == null || dispositivo.status == null) {
            return null;
        }

        Boolean ligada = null;

        for (String codigo : LIGADA_LUZ) {
            for (TuyaStatus status : dispositivo.status) {
                if (ligada == null && codigo.equals(status.code) && status.value instanceof Boolean) {
                    ligada = (Boolean) status.value;
                }
            }
        }

        return new EstadoLuz(ligada,
                percentualDaFaixa(dispositivo, item, BRILHO_LUZ, "bright_min", "bright_max", 10),
                percentualDaFaixa(dispositivo, item, TEMPERATURA_COR_LUZ, "temp_min", "temp_max", 0));
    }

    /** Degrau de temperatura (0 quente, 1 neutra, 2 fria) mais próximo da porcentagem (0 = quente na Tuya). */
    public static int degrauDeTemperatura(int percentual) {
        return percentual < 34 ? 0 : percentual < 67 ? 1 : 2;
    }

    /** Degrau de brilho (0 a 3, que valem 25, 50, 75 e 100%) mais próximo da porcentagem. */
    public static int degrauDeBrilho(int percentual) {
        return percentual <= 37 ? 0 : percentual <= 62 ? 1 : percentual <= 87 ? 2 : 3;
    }

    /** Quanto a cortina está aberta (0 a 100). Alguns motores contam 100 como fechada: {@code invertida} troca. */
    public static int percentualAberto(double posicao, boolean invertida) {

        double aberto = invertida ? 100 - posicao : posicao;
        return (int) Math.round(Math.max(0, Math.min(100, aberto)));
    }

    /**
     * Os sensores de chuva variam de fabricante: serve qualquer ponto com "rain" no código e também o
     * {@code watersensor_state} (normal/alarm) dos sensores de água.
     */
    public static boolean codigoDeChuva(String codigo) {

        if (codigo == null) {
            return false;
        }

        String minusculo = codigo.toLowerCase(Locale.ROOT);
        return minusculo.contains("rain") || minusculo.equals("watersensor_state");
    }

    /** Booleano, texto (none, rain, drizzle...) ou número, dependendo do aparelho. */
    public static Boolean valorDeChuva(Object valor) {

        if (valor instanceof Boolean) {
            return (Boolean) valor;
        }
        if (valor instanceof Number) {
            return ((Number) valor).doubleValue() != 0;
        }
        if (valor instanceof String) {
            switch (((String) valor).trim().toLowerCase(Locale.ROOT)) {
                case "":
                case "none":
                case "no_rain":
                case "norain":
                case "no":
                case "off":
                case "false":
                case "0":
                case "normal":
                case "dry":
                case "clear":
                    return false;
                default:
                    return true;
            }
        }
        return null;
    }

    /** Verdadeiro se está chovendo, falso se não, nulo se o aparelho não informa. Prefere o ponto associado. */
    public static Boolean chovendo(TuyaDevice dispositivo, ItemComodo item) {

        if (dispositivo == null || dispositivo.status == null) {
            return null;
        }

        Boolean outro = null;

        for (TuyaStatus status : dispositivo.status) {
            if (item.tuyaCode != null && item.tuyaCode.equals(status.code)) {
                return valorDeChuva(status.value);
            }
            if (outro == null && codigoDeChuva(status.code)) {
                outro = valorDeChuva(status.value);
            }
        }
        return outro;
    }

    public static EstadoAlarme estadoAlarme(TuyaDevice dispositivo, ItemComodo item) {

        if (dispositivo == null || dispositivo.status == null) {
            return EstadoAlarme.DESCONHECIDO;
        }

        // Alarme tocando vale mais que o modo: a central segue "armada" enquanto dispara.
        for (TuyaStatus status : dispositivo.status) {
            if ("master_state".equals(status.code) && "alarm".equals(status.value)) {
                return EstadoAlarme.DISPARADO;
            }
        }

        for (TuyaStatus status : dispositivo.status) {
            if (item.tuyaCode != null && item.tuyaCode.equals(status.code) && status.value instanceof String) {
                return estadoAlarme((String) status.value);
            }
        }
        return EstadoAlarme.DESCONHECIDO;
    }

    static EstadoAlarme estadoAlarme(String modo) {

        switch (modo.trim().toLowerCase(Locale.ROOT)) {
            case "arm":
            case "armed":
            case "away":
                return EstadoAlarme.ATIVADO;
            case "disarmed":
            case "disarm":
                return EstadoAlarme.DESATIVADO;
            case "home":
            case "stay":
                return EstadoAlarme.EM_CASA;
            case "sos":
            case "alarm":
                return EstadoAlarme.DISPARADO;
            default:
                return EstadoAlarme.DESCONHECIDO;
        }
    }

    public static String potencia(Double watts, Locale locale) {
        return watts == null ? SEM_LEITURA : numero(watts, 1, locale) + " W";
    }

    /** Abaixo de 1 A fica em miliampères, onde a precisão importa. */
    public static String corrente(Double miliamperes, Locale locale) {

        if (miliamperes == null) {
            return SEM_LEITURA;
        }
        return miliamperes >= 1000 ? numero(miliamperes / 1000, 2, locale) + " A" : numero(miliamperes, 0, locale) + " mA";
    }

    public static String tensao(Double volts, Locale locale) {
        return volts == null ? SEM_LEITURA : numero(volts, 1, locale) + " V";
    }

    public static String temperatura(Double graus, Locale locale) {
        return graus == null ? SEM_LEITURA : numero(graus, 1, locale) + " °C";
    }

    public static String umidade(Double percentual, Locale locale) {
        return percentual == null ? SEM_LEITURA : numero(percentual, 0, locale) + " %";
    }

    private static String numero(double valor, int casas, Locale locale) {

        String formato = casas > 0 ? "0." + new String(new char[casas]).replace('\0', '#') : "0";
        return new DecimalFormat(formato, DecimalFormatSymbols.getInstance(locale)).format(valor);
    }
}
