const { onValueCreated } = require("firebase-functions/v2/database");
const { initializeApp } = require("firebase-admin/app");
const { getMessaging } = require("firebase-admin/messaging");

initializeApp();

// Mesmo tópico que o app assina (NotificacaoListaCompras.TOPICO).
const TOPICO = "listaCompras";

/**
 * Dispara quando um produto entra na lista de compras (listaCompras/minhaLst/{categoria}/{produto}).
 * onValueCreated só roda na criação do nó: marcar como comprado ou desmarcar altera um nó que já
 * existe e não notifica; remover e adicionar de novo cria o nó outra vez e notifica.
 *
 * A mensagem é só de dados para o app montar a notificação em qualquer estado (aberto, em segundo plano
 * ou fechado).
 */
exports.avisaItemNaLista = onValueCreated(
  {
    ref: "/listaCompras/minhaLst/{categoria}/{produto}",
    instance: "churrasqueirav1-default-rtdb",
    region: "us-central1",
  },
  async (event) => {
    const { categoria, produto } = event.params;

    await getMessaging().send({
      topic: TOPICO,
      data: { produto, categoria },
      android: { priority: "high" },
    });
  }
);
