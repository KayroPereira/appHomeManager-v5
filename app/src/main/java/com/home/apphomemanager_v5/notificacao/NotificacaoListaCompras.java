package com.home.apphomemanager_v5.notificacao;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.messaging.FirebaseMessaging;
import com.home.apphomemanager_v5.R;
import com.home.apphomemanager_v5.listacompras.ListaComprasActivity;

/**
 * Avisos de item novo na lista de compras. Uma Cloud Function ({@code functions/index.js}) publica no
 * tópico {@link #TOPICO} quando um item entra em {@code listaCompras/minhaLst}; todo aparelho com o app
 * assina o tópico e mostra a notificação.
 */
public final class NotificacaoListaCompras {

    /** Mesmo nome usado pela Cloud Function. */
    public static final String TOPICO = "listaCompras";

    private static final String CANAL = "lista_compras";

    private NotificacaoListaCompras() {}

    /** Cria o canal e assina o tópico; pode ser chamado a cada abertura do app. */
    public static void inicia(Context context) {

        criaCanal(context);
        FirebaseMessaging.getInstance().subscribeToTopic(TOPICO);
    }

    public static boolean temPermissao(Context context) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }

    public static void mostra(Context context, String produto) {

        if (!temPermissao(context)) {
            return;
        }

        criaCanal(context);

        Intent abre = new Intent(context, ListaComprasActivity.class);
        abre.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent toque = PendingIntent.getActivity(context, 0, abre, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder notificacao = new NotificationCompat.Builder(context, CANAL)
                .setSmallIcon(R.drawable.ic_notificacao_compras)
                .setContentTitle(context.getString(R.string.notificacaoCompraTitulo))
                .setContentText(context.getString(R.string.notificacaoCompraTexto, produto))
                .setContentIntent(toque)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        // Um id por produto: vários itens adicionados em sequência aparecem todos, e o mesmo produto não duplica.
        NotificationManagerCompat.from(context).notify(produto.hashCode(), notificacao.build());
    }

    private static void criaCanal(Context context) {

        NotificationChannel canal = new NotificationChannel(CANAL,
                context.getString(R.string.notificacaoCompraCanal), NotificationManager.IMPORTANCE_DEFAULT);

        context.getSystemService(NotificationManager.class).createNotificationChannel(canal);
    }
}
