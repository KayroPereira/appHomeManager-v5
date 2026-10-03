package com.home.apphomemanager_v5.commons;

import android.os.Handler;
import android.os.Looper;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class StatusDispositivo {

    private final Handler handler;
    private Runnable runnable;

    public StatusDispositivo() {
        handler = new Handler(Looper.getMainLooper());
    }

    /**
     * O dispositivo grava em "status" o epoch em horário local (não UTC),
     * por isso o offset do fuso é somado ao instante atual antes da comparação.
     */
    public boolean isOnline(Long baseAtual, int periodo){

        if (baseAtual == null || baseAtual <= 0) {
            return false;
        }

        Instant dataAtual = Instant.now();

        ZoneId zoneId = ZoneId.systemDefault();

        int utcOffset = ZonedDateTime.now(zoneId).getOffset().getTotalSeconds();

        return (dataAtual.getEpochSecond() + (utcOffset)) - baseAtual <= periodo;
    }

    public void inicializaSchedulerStatusDispositivo(Runnable task, long delayMs) {

        paraSchedulerStatusDispositivo();

        runnable = new Runnable() {
            @Override
            public void run() {
                task.run();
                handler.postDelayed(this, delayMs);
            }
        };
        handler.post(runnable);
    }

    public void paraSchedulerStatusDispositivo() {
        if (runnable != null) {
            handler.removeCallbacks(runnable);
            runnable = null;
        }
    }
}
