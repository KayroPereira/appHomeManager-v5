package com.home.apphomemanager_v5.tuya;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Uma linha da tela: interruptor, controle deslizante (dimmer), seletor (enum), sensor numérico,
 * texto somente leitura ou, para aparelho sem nada compatível, um item informativo.
 */
public class TuyaControle {

    /**
     * INFORMATIVO: aparelho sem nenhum controle compatível; só mostra nome, estado e os pontos de dados que oferece.
     * SELETOR: enum com poucas opções (ex.: sensibilidade baixa/média/alta), usa o mesmo componente do slider.
     * TEXTO: valor somente leitura já formatado (ex.: espaço do cartão SD).
     * CENA: cena de acionamento manual do Smart Life; um toque executa (com confirmação).
     */
    public enum Tipo { INTERRUPTOR, SLIDER, SELETOR, SENSOR, TEXTO, CENA, INFORMATIVO }

    private static final Pattern CODIGO_SWITCH = Pattern.compile("^switch(_\\d+|_led)?$");

    private static final Pattern CODIGO_SLIDER = Pattern.compile("^(bright|temp)_value(_v2)?$");

    private static final Map<String, String> ROTULO_SLIDER = new HashMap<>();
    private static final Map<String, String[]> SENSORES = new HashMap<>();
    /** Interruptores de câmeras e campainhas: código -> rótulo. */
    private static final Map<String, String> ROTULO_INTERRUPTOR_EXTRA = new HashMap<>();
    /** Enums conhecidos: código -> {rótulo, "valor=nome" separados por ;}. */
    private static final Map<String, String[]> SELETORES = new HashMap<>();

    static {
        ROTULO_SLIDER.put("bright_value", "Brilho");
        ROTULO_SLIDER.put("bright_value_v2", "Brilho");
        ROTULO_SLIDER.put("temp_value", "Temperatura da luz");
        ROTULO_SLIDER.put("temp_value_v2", "Temperatura da luz");

        // código -> {rótulo, unidade padrão, escala padrão}
        SENSORES.put("temp_current", new String[]{"Temperatura", "°C", "1"});
        SENSORES.put("va_temperature", new String[]{"Temperatura", "°C", "1"});
        SENSORES.put("humidity_value", new String[]{"Umidade", "%", "0"});
        SENSORES.put("va_humidity", new String[]{"Umidade", "%", "0"});
        SENSORES.put("battery_percentage", new String[]{"Bateria", "%", "0"});
        SENSORES.put("cur_power", new String[]{"Potência", "W", "1"});
        SENSORES.put("cur_voltage", new String[]{"Tensão", "V", "1"});
        SENSORES.put("cur_current", new String[]{"Corrente", "mA", "0"});
        SENSORES.put("add_ele", new String[]{"Energia", "kWh", "3"});

        ROTULO_INTERRUPTOR_EXTRA.put("motion_switch", "Detecção de movimento");
        ROTULO_INTERRUPTOR_EXTRA.put("motion_area_switch", "Área de detecção");
        ROTULO_INTERRUPTOR_EXTRA.put("record_switch", "Gravação");
        ROTULO_INTERRUPTOR_EXTRA.put("basic_flip", "Inverter imagem");
        ROTULO_INTERRUPTOR_EXTRA.put("basic_osd", "Marca d'água");
        ROTULO_INTERRUPTOR_EXTRA.put("basic_private", "Privacidade");
        ROTULO_INTERRUPTOR_EXTRA.put("basic_wdr", "WDR");

        SELETORES.put("motion_sensitivity", new String[]{"Sensibilidade do movimento", "0=Baixa;1=Média;2=Alta"});
        SELETORES.put("record_mode", new String[]{"Modo de gravação", "1=Por evento;2=Contínua"});
        SELETORES.put("basic_nightvision", new String[]{"Visão noturna", "0=Automática;1=Desligada;2=Ligada"});
    }

    private static final String[] ESTADO_CARTAO = {"", "Normal", "Anormal", "Espaço insuficiente", "Formatando", "Sem cartão"};

    public final Tipo tipo;
    public final String deviceId;
    public final String nome;
    public final String code;
    public final boolean online;
    /** Aparelho com mais de um interruptor comum: mostra também o código da saída. */
    public final boolean multiplo;
    public final String rotulo;
    public final String unidade;

    // Faixa do slider (valores brutos da Tuya) e casas decimais do valor bruto.
    public final int min;
    public final int max;
    public final int passo;
    public final int escala;

    public boolean ligado;
    /** Slider: valor bruto da Tuya. Seletor: posição da opção escolhida. */
    public long valorBruto;
    /** Seletor: valores aceitos pela Tuya, na ordem exibida. */
    public List<String> opcoes = Collections.emptyList();
    /** Seletor: nome amigável de cada opção. */
    public List<String> nomesOpcoes = Collections.emptyList();
    /** Texto: valor já formatado. */
    public String textoFixo = "";

    private TuyaControle(Tipo tipo, TuyaDispositivo dispositivo, String code, boolean multiplo, String rotulo, String unidade,
                         int min, int max, int passo, int escala) {
        this.tipo = tipo;
        this.deviceId = dispositivo.id;
        this.nome = dispositivo.nome != null ? dispositivo.nome : dispositivo.id;
        this.code = code;
        this.online = dispositivo.online;
        this.multiplo = multiplo;
        this.rotulo = rotulo;
        this.unidade = unidade;
        this.min = min;
        this.max = max;
        this.passo = passo;
        this.escala = escala;
    }

    /** Casas decimais usadas quando o esquema do aparelho não informa a escala; 0 para códigos desconhecidos. */
    public static int escalaPadrao(String code) {

        String[] padrao = SENSORES.get(code);
        return padrao != null ? Integer.parseInt(padrao[2]) : 0;
    }

    public String chave() {
        return deviceId + "/" + code;
    }

    public String titulo() {
        if (tipo == Tipo.CENA) {
            return rotulo;
        }
        if (tipo == Tipo.INFORMATIVO) {
            return nome;
        }
        if (tipo == Tipo.INTERRUPTOR && rotulo.isEmpty()) {
            return multiplo ? nome + " (" + code + ")" : nome;
        }
        return nome + " - " + rotulo;
    }

    /** Valor do sensor com a escala aplicada, ex.: 245 (escala 1) vira "24,5 °C". */
    public String textoSensor(Locale locale) {

        double valor = valorBruto / Math.pow(10, escala);
        String formato = escala > 0 ? "0." + new String(new char[escala]).replace('\0', '#') : "0";
        String texto = new DecimalFormat(formato, DecimalFormatSymbols.getInstance(locale)).format(valor);

        return unidade.isEmpty() ? texto : texto + " " + unidade;
    }

    /** Posição do slider/seletor (0 a {@link #maximoSlider()}) para o valor atual. */
    public int progressoSlider() {

        if (tipo == Tipo.SELETOR) {
            return (int) Math.max(0, Math.min(maximoSlider(), valorBruto));
        }
        return (int) Math.max(0, Math.min(maximoSlider(), (valorBruto - min) / passo));
    }

    public int maximoSlider() {
        return tipo == Tipo.SELETOR ? Math.max(0, opcoes.size() - 1) : (max - min) / passo;
    }

    public int valorDoProgresso(int progresso) {
        return min + progresso * passo;
    }

    /** Valor que fica guardado na linha depois de mexer no controle. */
    public long valorBrutoDoProgresso(int progresso) {
        return tipo == Tipo.SELETOR ? progresso : valorDoProgresso(progresso);
    }

    /** Valor enviado à Tuya: inteiro no slider, texto no enum. */
    public Object valorParaComando(int progresso) {
        return tipo == Tipo.SELETOR ? opcoes.get(progresso) : (Object) valorDoProgresso(progresso);
    }

    /** Texto ao lado do controle: "50%" no slider, nome da opção no seletor. */
    public String textoSlider(int progresso) {

        if (tipo == Tipo.SELETOR) {
            return progresso < nomesOpcoes.size() ? nomesOpcoes.get(progresso) : "";
        }

        int bruto = valorDoProgresso(progresso);

        return (max == min ? 0 : Math.round(100f * (bruto - min) / (max - min))) + "%";
    }

    public int percentual() {
        return max == min ? 0 : (int) Math.round(100.0 * (valorBruto - min) / (max - min));
    }

    /** Copia o estado vindo da nuvem para esta linha. */
    public void copiaEstadoDe(TuyaControle outro) {
        ligado = outro.ligado;
        valorBruto = outro.valorBruto;
        textoFixo = outro.textoFixo;
    }

    public boolean mesmoEstadoQue(TuyaControle outro) {
        return online == outro.online && ligado == outro.ligado && valorBruto == outro.valorBruto && textoFixo.equals(outro.textoFixo);
    }

    /** Linha de uma cena do Smart Life; o {@code code} é o id da cena. */
    public static TuyaControle cena(String sceneId, String nome) {

        TuyaDispositivo referencia = new TuyaDispositivo("cena", nome, true, Collections.emptyList());

        return new TuyaControle(Tipo.CENA, referencia, sceneId, false, nome, "", 0, 0, 1, 0);
    }

    public static List<TuyaControle> extrai(List<TuyaDispositivo> dispositivos) {
        return extrai(dispositivos, Locale.getDefault());
    }

    /**
     * Monta as linhas a partir dos dispositivos. Slider exige a faixa (min/max) no esquema do ponto
     * de dados; sensor usa escala e unidade do esquema quando existirem, senão os padrões.
     */
    public static List<TuyaControle> extrai(List<TuyaDispositivo> dispositivos, Locale locale) {

        List<TuyaControle> controles = new ArrayList<>();

        for (TuyaDispositivo dispositivo : dispositivos) {

            int linhasAntes = controles.size();

            List<TuyaPonto> interruptores = new ArrayList<>();

            for (TuyaPonto ponto : dispositivo.pontos) {
                if (ponto.code != null && CODIGO_SWITCH.matcher(ponto.code).matches() && ponto.valor instanceof Boolean) {
                    interruptores.add(ponto);
                }
            }

            for (TuyaPonto ponto : interruptores) {
                TuyaControle c = new TuyaControle(Tipo.INTERRUPTOR, dispositivo, ponto.code, interruptores.size() > 1, "", "", 0, 0, 1, 0);
                c.ligado = (Boolean) ponto.valor;
                controles.add(c);
            }

            for (TuyaPonto ponto : dispositivo.pontos) {

                if (ponto.code == null || ponto.valor == null) {
                    continue;
                }

                TuyaControle c = converteOutros(dispositivo, ponto, locale);

                if (c != null) {
                    controles.add(c);
                }
            }

            // Controles primeiro, informações depois; a ordenação é estável e preserva a ordem da Tuya dentro de cada tipo.
            controles.subList(linhasAntes, controles.size()).sort((a, b) -> a.tipo.compareTo(b.tipo));

            // Aparelho sem controle compatível (câmera, interfone...) continua aparecendo na lista.
            if (controles.size() == linhasAntes) {
                controles.add(new TuyaControle(Tipo.INFORMATIVO, dispositivo, "-", false, codigos(dispositivo), "", 0, 0, 1, 0));
            }
        }
        return controles;
    }

    private static TuyaControle converteOutros(TuyaDispositivo dispositivo, TuyaPonto ponto, Locale locale) {

        String code = ponto.code;
        JsonObject faixa = faixa(ponto.propriedade);

        if (ponto.valor instanceof Boolean && ROTULO_INTERRUPTOR_EXTRA.containsKey(code)) {
            TuyaControle c = new TuyaControle(Tipo.INTERRUPTOR, dispositivo, code, false, ROTULO_INTERRUPTOR_EXTRA.get(code), "", 0, 0, 1, 0);
            c.ligado = (Boolean) ponto.valor;
            return c;
        }

        if (SELETORES.containsKey(code)) {
            return seletor(dispositivo, ponto, faixa);
        }

        if ("sd_status".equals(code) && ponto.valor instanceof Number) {
            int estado = ((Number) ponto.valor).intValue();
            String texto = estado > 0 && estado < ESTADO_CARTAO.length ? ESTADO_CARTAO[estado] : String.valueOf(estado);
            return texto(dispositivo, code, "Cartão SD", texto);
        }

        if ("sd_storge".equals(code)) {
            String texto = textoEspacoCartao(ponto.valor.toString(), locale);
            return texto != null ? texto(dispositivo, code, "Espaço do cartão", texto) : null;
        }

        if (!(ponto.valor instanceof Number)) {
            return null;
        }

        long bruto = ((Number) ponto.valor).longValue();

        if (CODIGO_SLIDER.matcher(code).matches()) {

            if (faixa != null && faixa.has("min") && faixa.has("max")) {
                int min = inteiro(faixa, "min", 0);
                int max = inteiro(faixa, "max", 1000);
                int passo = Math.max(1, inteiro(faixa, "step", 1));

                if (max > min) {
                    TuyaControle c = new TuyaControle(Tipo.SLIDER, dispositivo, code, false, ROTULO_SLIDER.get(code), "%", min, max, passo, 0);
                    c.valorBruto = bruto;
                    return c;
                }
            }
            return null;
        }

        if (SENSORES.containsKey(code)) {

            String[] padrao = SENSORES.get(code);

            String unidade = faixa != null && faixa.has("unit") && !faixa.get("unit").getAsString().isEmpty()
                    ? faixa.get("unit").getAsString() : padrao[1];
            int escala = faixa != null ? inteiro(faixa, "scale", Integer.parseInt(padrao[2])) : Integer.parseInt(padrao[2]);

            TuyaControle c = new TuyaControle(Tipo.SENSOR, dispositivo, code, false, padrao[0], unidade, 0, 0, 1, escala);
            c.valorBruto = bruto;
            return c;
        }
        return null;
    }

    private static TuyaControle texto(TuyaDispositivo dispositivo, String code, String rotulo, String texto) {

        TuyaControle c = new TuyaControle(Tipo.TEXTO, dispositivo, code, false, rotulo, "", 0, 0, 1, 0);
        c.textoFixo = texto;
        return c;
    }

    /** Enum: opções do esquema (campo "range") com nomes amigáveis quando conhecidos. */
    private static TuyaControle seletor(TuyaDispositivo dispositivo, TuyaPonto ponto, JsonObject faixa) {

        String[] conhecido = SELETORES.get(ponto.code);

        Map<String, String> nomes = new HashMap<>();
        for (String par : conhecido[1].split(";")) {
            String[] partes = par.split("=");
            nomes.put(partes[0], partes[1]);
        }

        List<String> valores = new ArrayList<>();

        if (faixa != null && faixa.has("range") && faixa.get("range").isJsonArray()) {
            JsonArray range = faixa.getAsJsonArray("range");
            for (JsonElement opcao : range) {
                valores.add(opcao.getAsString());
            }
        } else {
            valores.addAll(new java.util.TreeSet<>(nomes.keySet()));
        }

        int indice = valores.indexOf(ponto.valor.toString());

        if (valores.isEmpty() || indice < 0) {
            return null;
        }

        List<String> exibicao = new ArrayList<>();
        for (String valor : valores) {
            exibicao.add(nomes.containsKey(valor) ? nomes.get(valor) : valor);
        }

        TuyaControle c = new TuyaControle(Tipo.SELETOR, dispositivo, ponto.code, false, conhecido[0], "", 0, 0, 1, 0);
        c.opcoes = valores;
        c.nomesOpcoes = exibicao;
        c.valorBruto = indice;
        return c;
    }

    /** "total|usado|livre" em KB (formato do sd_storge) vira "123 GB livres de 125 GB". */
    static String textoEspacoCartao(String valor, Locale locale) {

        String[] partes = valor.split("\\|");

        if (partes.length != 3) {
            return null;
        }

        try {
            long total = Long.parseLong(partes[0].trim());
            long livre = Long.parseLong(partes[2].trim());

            return formataKb(livre, locale) + " livres de " + formataKb(total, locale);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String formataKb(long kb, Locale locale) {

        DecimalFormat formato = new DecimalFormat("0.#", DecimalFormatSymbols.getInstance(locale));

        if (kb >= 1024L * 1024L) {
            return formato.format(kb / (1024.0 * 1024.0)) + " GB";
        }
        return formato.format(kb / 1024.0) + " MB";
    }

    private static String codigos(TuyaDispositivo dispositivo) {

        List<String> codigos = new ArrayList<>();

        for (TuyaPonto ponto : dispositivo.pontos) {
            if (ponto.code != null) {
                codigos.add(ponto.code);
            }
        }
        return String.join(", ", codigos);
    }

    private static JsonObject faixa(String propriedade) {

        if (propriedade == null || propriedade.isEmpty()) {
            return null;
        }

        try {
            return JsonParser.parseString(propriedade).getAsJsonObject();
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static int inteiro(JsonObject json, String campo, int padrao) {
        return json.has(campo) && json.get(campo).isJsonPrimitive() ? json.get(campo).getAsInt() : padrao;
    }
}
