package com.home.apphomemanager_v5.commons;

import android.os.Handler;
import android.os.Looper;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class StatusDispositivo {

    private Handler handler;
    private Runnable runnable;

    public StatusDispositivo() {
        handler = new Handler(Looper.myLooper());
    }

    public boolean isOnline(Long baseAtual, int periodo){

        Instant dataAtual = Instant.now();

        ZoneId zoneId = ZoneId.systemDefault();

        int utcOffset = ZonedDateTime.now(zoneId).getOffset().getTotalSeconds();

        return (dataAtual.getEpochSecond() + (utcOffset)) - baseAtual <= periodo;
    }

    public void inicializaSchedulerStatusDispositivo(Runnable task, long delayMs) {

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
        if (handler != null && runnable != null) {
            handler.removeCallbacks(runnable);
        }
    }
}