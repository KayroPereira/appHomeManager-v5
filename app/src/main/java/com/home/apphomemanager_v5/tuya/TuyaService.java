package com.home.apphomemanager_v5.tuya;

import com.home.apphomemanager_v5.tuya.model.TuyaCommands;
import com.home.apphomemanager_v5.tuya.model.TuyaDevice;
import com.home.apphomemanager_v5.tuya.model.TuyaDeviceInfo;
import com.home.apphomemanager_v5.tuya.model.TuyaDevicesPage;
import com.home.apphomemanager_v5.tuya.model.TuyaScene;
import com.home.apphomemanager_v5.tuya.model.TuyaResponse;
import com.home.apphomemanager_v5.tuya.model.TuyaSpec;
import com.home.apphomemanager_v5.tuya.model.TuyaStatus;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

/** Tuya Cloud OpenAPI. O token e a assinatura são tratados por {@link TuyaInterceptor}. */
public interface TuyaService {

    /** Dispositivos dos usuários do app Tuya/Smart Life vinculados ao projeto Cloud. */
    @GET("v1.0/iot-01/associated-users/devices")
    Call<TuyaResponse<TuyaDevicesPage>> getDevices(
            @Query("size") int tamanho,
            @Query("last_row_key") String ultimaLinha
    );

    @GET("v1.0/devices/{id}/status")
    Call<TuyaResponse<List<TuyaStatus>>> getStatus(@Path("id") String deviceId);

    /** Faixa, passo, escala e unidade dos pontos de dados. */
    @GET("v1.0/devices/{id}/specifications")
    Call<TuyaResponse<TuyaSpec>> getSpecifications(@Path("id") String deviceId);

    /** Traz o {@code owner_id}, que é o id da casa usado pelas APIs de cenas. */
    @GET("v1.0/devices/{id}")
    Call<TuyaResponse<TuyaDeviceInfo>> getDeviceInfo(@Path("id") String deviceId);

    /** Um aparelho só, já com {@code online} e o status dos pontos de dados: 1 chamada em vez de listar tudo. */
    @GET("v1.0/devices/{id}")
    Call<TuyaResponse<TuyaDevice>> getDevice(@Path("id") String deviceId);

    @GET("v1.1/homes/{homeId}/scenes")
    Call<TuyaResponse<List<TuyaScene>>> getScenes(@Path("homeId") String homeId);

    @POST("v1.0/homes/{homeId}/scenes/{sceneId}/trigger")
    Call<TuyaResponse<Boolean>> triggerScene(@Path("homeId") String homeId, @Path("sceneId") String sceneId);

    @POST("v1.0/devices/{id}/commands")
    Call<TuyaResponse<Boolean>> sendCommands(@Path("id") String deviceId, @Body TuyaCommands comandos);
}
