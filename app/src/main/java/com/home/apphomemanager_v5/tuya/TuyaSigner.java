package com.home.apphomemanager_v5.tuya;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** Assinatura HMAC-SHA256 das requisições da Tuya Cloud OpenAPI. */
public class TuyaSigner {

    private TuyaSigner() {}

    /** SHA-256 de um corpo vazio, usado nas requisições sem body. */
    public static final String SHA256_VAZIO = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

    public static String sha256Hex(byte[] dados) {

        try {
            return hex(MessageDigest.getInstance("SHA-256").digest(dados));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }

    /**
     * {@code METHOD \n Content-SHA256 \n Headers \n URL}. {@code pathComQuery} já com os
     * parâmetros em ordem alfabética.
     */
    public static String stringToSign(String metodo, String contentSha256, String pathComQuery) {
        return metodo.toUpperCase(Locale.ROOT) + "\n" + contentSha256 + "\n\n" + pathComQuery;
    }

    /**
     * @param accessToken vazio na requisição do próprio token
     * @return assinatura em maiúsculas: {@code HMAC(clientId + accessToken + t + stringToSign)}
     */
    public static String assina(String clientId, String segredo, String accessToken, String t, String stringToSign) {

        String conteudo = clientId + (accessToken != null ? accessToken : "") + t + stringToSign;

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(segredo.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));

            return hex(mac.doFinal(conteudo.getBytes(StandardCharsets.UTF_8))).toUpperCase(Locale.ROOT);
        } catch (Exception e) {
            throw new IllegalStateException("HmacSHA256 indisponível", e);
        }
    }

    private static String hex(byte[] bytes) {

        StringBuilder sb = new StringBuilder();

        for (byte b : bytes) {
            sb.append(String.format(Locale.ROOT, "%02x", b));
        }
        return sb.toString();
    }
}
