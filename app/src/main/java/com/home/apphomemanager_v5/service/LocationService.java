package com.home.apphomemanager_v5.service;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

public class LocationService {

    private final FusedLocationProviderClient fusedLocationProviderClient;

    private CancellationTokenSource cancellationTokenSource;

    public LocationService(Context context) {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context.getApplicationContext());
    }

    /**
     * Leitura única da localização (precisão de bairro basta para o clima), em vez de
     * atualizações contínuas que precisariam ser removidas depois. Quem chama deve ter
     * verificado a permissão de localização. O resultado pode ser {@code null}.
     */
    @SuppressLint("MissingPermission")
    public void getCurrentLocation(OnSuccessListener<Location> onSuccess, OnFailureListener onFailure) {

        cancel();
        cancellationTokenSource = new CancellationTokenSource();

        fusedLocationProviderClient
                .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cancellationTokenSource.getToken())
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    public void cancel() {
        if (cancellationTokenSource != null) {
            cancellationTokenSource.cancel();
            cancellationTokenSource = null;
        }
    }
}
