package com.home.apphomemanager_v5.tuya;

import com.google.gson.Gson;
import com.home.apphomemanager_v5.tuya.model.TuyaResponse;
import com.home.apphomemanager_v5.tuya.model.TuyaToken;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.Buffer;

/**
 * Assina todas as chamadas e cuida do access token: obtém quando falta, renova perto do
 * vencimento e tenta de novo uma vez se a Tuya recusar o token (código 1010).
 */
public class TuyaInterceptor implements Interceptor {

    private static final String PATH_TOKEN = "/v1.0/token";
    private static final int CODIGO_TOKEN_INVALIDO = 1010;
    private static final long MARGEM_RENOVACAO_MS = 60_000;

    private final String clientId;
    private final String segredo;
    private final Gson gson = new Gson();

    private String accessToken;
    private long validoAte;

    public TuyaInterceptor(String clientId, String segredo) {
        this.clientId = clientId;
        this.segredo = segredo;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {

        Request original = chain.request();

        if (PATH_TOKEN.equals(original.url().encodedPath())) {
            return chain.proceed(assina(original, null));
        }

        Response resposta = chain.proceed(assina(original, token(chain)));

        if (tokenRecusado(resposta)) {
            resposta.close();
            invalidaToken();
            resposta = chain.proceed(assina(original, token(chain)));
        }
        return resposta;
    }

    private synchronized String token(Chain chain) throws IOException {

        if (accessToken != null && System.currentTimeMillis() < validoAte - MARGEM_RENOVACAO_MS) {
            return accessToken;
        }

        HttpUrl url = chain.request().url().newBuilder().encodedPath(PATH_TOKEN).query(null).addQueryParameter("grant_type", "1").build();
        Request pedido = assina(new Request.Builder().url(url).get().build(), null);

        try (Response resposta = chain.proceed(pedido)) {

            String corpo = resposta.body() != null ? resposta.body().string() : "";
            TuyaResponse<TuyaToken> token = gson.fromJson(corpo, TuyaToken.TIPO);

            if (token == null || !token.success || token.result == null) {
                String motivo = token != null ? token.code + " " + token.msg : "resposta vazia (HTTP " + resposta.code() + ")";
                throw new IOException("Tuya recusou o pedido de token: " + motivo);
            }

            accessToken = token.result.accessToken;
            validoAte = System.currentTimeMillis() + token.result.expireTime * 1000L;

            return accessToken;
        }
    }

    private synchronized void invalidaToken() {
        accessToken = null;
        validoAte = 0;
    }

    private boolean tokenRecusado(Response resposta) throws IOException {

        if (resposta.body() == null) {
            return false;
        }

        // peekBody não consome o corpo original.
        TuyaResponse<?> corpo = gson.fromJson(resposta.peekBody(64 * 1024).string(), TuyaResponse.class);

        return corpo != null && !corpo.success && corpo.code == CODIGO_TOKEN_INVALIDO;
    }

    private Request assina(Request request, String token) throws IOException {

        String t = String.valueOf(System.currentTimeMillis());

        HttpUrl url = ordenaQuery(request.url());

        String path = url.encodedPath() + (url.encodedQuery() != null ? "?" + url.encodedQuery() : "");

        String stringToSign = TuyaSigner.stringToSign(request.method(), TuyaSigner.sha256Hex(corpo(request)), path);

        Request.Builder builder = request.newBuilder()
                .url(url)
                .header("client_id", clientId)
                .header("t", t)
                .header("sign_method", "HMAC-SHA256")
                .header("sign", TuyaSigner.assina(clientId, segredo, token, t, stringToSign));

        if (token != null) {
            builder.header("access_token", token);
        }
        return builder.build();
    }

    /** A Tuya exige os parâmetros em ordem alfabética, tanto na assinatura quanto na URL enviada. */
    private static HttpUrl ordenaQuery(HttpUrl url) {

        List<String> nomes = new ArrayList<>(url.queryParameterNames());
        Collections.sort(nomes);

        HttpUrl.Builder builder = url.newBuilder().query(null);

        for (String nome : nomes) {
            for (String valor : url.queryParameterValues(nome)) {
                builder.addQueryParameter(nome, valor);
            }
        }
        return builder.build();
    }

    private static byte[] corpo(Request request) throws IOException {

        RequestBody body = request.body();

        if (body == null) {
            return new byte[0];
        }

        Buffer buffer = new Buffer();
        body.writeTo(buffer);

        return buffer.readByteArray();
    }
}
