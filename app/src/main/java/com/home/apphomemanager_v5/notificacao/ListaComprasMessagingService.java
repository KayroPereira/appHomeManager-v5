package com.home.apphomemanager_v5.notificacao;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

/**
 * Recebe a mensagem de dados da Cloud Function e monta a notificação. Mensagens só de dados chegam
 * aqui com o app aberto, em segundo plano ou fechado (mas não forçado a parar nas configurações).
 */
public class ListaComprasMessagingService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(@NonNull RemoteMessage mensagem) {

        Map<String, String> dados = mensagem.getData();
        String produto = dados.get("produto");

        if (produto != null && !produto.isEmpty()) {
            NotificacaoListaCompras.mostra(this, produto);
        }
    }
}
