package com.home.apphomemanager_v5;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.database.DatabaseError;
import com.home.apphomemanager_v5.databinding.ActivityDashBoardBinding;
import com.home.apphomemanager_v5.inteface.WeatherService;
import com.home.apphomemanager_v5.model.clima.Forecast;
import com.home.apphomemanager_v5.model.clima.ForecastActual;
import com.home.apphomemanager_v5.model.clima.OpenMeteoResponse;
import com.home.apphomemanager_v5.listacompras.ListaComprasActivity;
import com.home.apphomemanager_v5.listacompras.ListaComprasRepository;
import com.home.apphomemanager_v5.listacompras.ListaComprasUtils;
import com.home.apphomemanager_v5.listacompras.Produto;
import com.home.apphomemanager_v5.notificacao.NotificacaoListaCompras;
import com.home.apphomemanager_v5.service.LocationService;
import com.home.apphomemanager_v5.tuya.TuyaRepository;
import com.home.apphomemanager_v5.util.ComponentUtils;
import com.home.apphomemanager_v5.util.IconeClima;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class DashBoardActivity extends AppCompatActivity {

    private static final String TAG = "DashBoardActivity";

    private static final String[] PERMISSOES_LOCALIZACAO = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    private ActivityDashBoardBinding binding;

    private WeatherService weatherService;

    private LocationService locationService;

    private Call<OpenMeteoResponse> chamadaClima;

    private final ListaComprasRepository listaComprasRepository = new ListaComprasRepository();

    // Geocoder faz I/O de rede: não pode rodar na main thread.
    private final ExecutorService executorGeocoder = Executors.newSingleThreadExecutor();

    private final ActivityResultLauncher<String[]> solicitaPermissaoLocalizacao =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), this::onResultadoPermissao);

    private final ActivityResultLauncher<String> solicitaPermissaoNotificacao =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), concedida -> { });

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        binding = ActivityDashBoardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Avisos de item novo na lista de compras (Android 13+ pede permissão para notificar).
        NotificacaoListaCompras.inicia(this);

        if (!NotificacaoListaCompras.temPermissao(this)) {
            solicitaPermissaoNotificacao.launch(Manifest.permission.POST_NOTIFICATIONS);
        }

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(WeatherService.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        weatherService = retrofit.create(WeatherService.class);

        locationService = new LocationService(this);

        ComponentUtils.setEventClickGeneric(binding.ivDoorLockDB, this::doorLock);
        // O cartão inteiro (ícone e rótulo) responde ao toque, não só a imagem de 50dp.
        ComponentUtils.setEventClickGeneric(binding.ctDoorLockDB, this::doorLock);
        ComponentUtils.setEventClickGeneric(binding.ctDashboardDB, this::dashboard);
        ComponentUtils.setEventClickGeneric(binding.ctControlDB, this::control);
        ComponentUtils.setEventClickGeneric(binding.ctReservoirDB, this::reservoir);
        ComponentUtils.setEventClickGeneric(binding.ctBuyListDB, this::listaCompras);
        ComponentUtils.setEventClickGeneric(binding.ctSetupDB, this::configuracao);
        ComponentUtils.setEventClickGeneric(binding.ivDashboardDB, this::dashboard);
        ComponentUtils.setEventClickGeneric(binding.ivControlDB, this::control);
        ComponentUtils.setEventClickGeneric(binding.ivReservoirDB, this::reservoir);
        ComponentUtils.setEventClickGeneric(binding.ivSetupDB, this::configuracao);
        ComponentUtils.setEventClickGeneric(binding.ivDbRefresh, this::updateClima);
        ComponentUtils.setEventClickGeneric(binding.ivBuyListDB, this::listaCompras);
        ComponentUtils.setEventClickGeneric(binding.ivBuyListInfDB, this::listaCompras);
        ComponentUtils.setEventClickGeneric(binding.tvBuyListInfDB, this::listaCompras);

        // O aviso só aparece quando o Firebase confirmar que há itens pendentes.
        exibeAvisoComprasPendentes(false);

        updateClima(null);
    }

    @Override
    protected void onStart() {

        super.onStart();

        listaComprasRepository.inicia(new ListaComprasRepository.Listener() {
            @Override
            public void aoAtualizar(List<Produto> produtos) {
                exibeAvisoComprasPendentes(ListaComprasUtils.temPendente(produtos));
            }

            @Override
            public void aoFalhar(DatabaseError erro) {
                Log.w(TAG, "Erro ao ler a lista de compras: " + erro.getMessage());
                exibeAvisoComprasPendentes(false);
            }
        });
    }

    @Override
    protected void onStop() {

        super.onStop();

        listaComprasRepository.para();
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();

        locationService.cancel();
        executorGeocoder.shutdownNow();

        if (chamadaClima != null) {
            chamadaClima.cancel();
        }
    }

    private boolean temPermissaoLocalizacao() {

        for (String permissao : PERMISSOES_LOCALIZACAO) {
            if (ContextCompat.checkSelfPermission(this, permissao) == PackageManager.PERMISSION_GRANTED) {
                return true;
            }
        }
        return false;
    }

    private void onResultadoPermissao(Map<String, Boolean> resultado) {

        if (resultado.containsValue(true)) {
            buscaLocalizacaoEClima();
        } else {
            Toast.makeText(this, R.string.permissaoLocalizacaoNegada, Toast.LENGTH_SHORT).show();
        }
    }

    private void updateClima(Object event){

        if (temPermissaoLocalizacao()) {
            buscaLocalizacaoEClima();
        } else {
            solicitaPermissaoLocalizacao.launch(PERMISSOES_LOCALIZACAO);
        }
    }

    private void buscaLocalizacaoEClima() {

        locationService.getCurrentLocation(
                location -> {
                    if (location != null) {
                        buscaClima(location);
                        buscaNomeCidade(location);
                    } else {
                        Toast.makeText(this, R.string.localizacaoIndisponivel, Toast.LENGTH_SHORT).show();
                    }
                },
                e -> {
                    Log.w(TAG, "Erro ao obter a localização", e);
                    Toast.makeText(this, R.string.localizacaoIndisponivel, Toast.LENGTH_SHORT).show();
                });
    }

    private void buscaClima(Location location) {

        if (chamadaClima != null) {
            chamadaClima.cancel();
        }

        chamadaClima = weatherService.getCurrentWeather(location.getLatitude(), location.getLongitude());

        chamadaClima.enqueue(new Callback<OpenMeteoResponse>() {
            @Override
            public void onResponse(@NonNull Call<OpenMeteoResponse> call, @NonNull Response<OpenMeteoResponse> response) {

                OpenMeteoResponse weather = response.body();

                if (!response.isSuccessful() || weather == null || weather.getCurrent() == null) {
                    Log.w(TAG, "Resposta inválida da API de clima: HTTP " + response.code());
                    Toast.makeText(DashBoardActivity.this, R.string.climaIndisponivel, Toast.LENGTH_SHORT).show();
                    return;
                }

                exibeClima(weather.toForecastActual());
            }

            @Override
            public void onFailure(@NonNull Call<OpenMeteoResponse> call, @NonNull Throwable t) {

                if (call.isCanceled()) {
                    return;
                }

                Log.w(TAG, "Erro ao consultar o clima", t);
                Toast.makeText(DashBoardActivity.this, R.string.climaIndisponivel, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /** A Open-Meteo não informa a cidade: o nome vem do Geocoder do Android a partir da coordenada. */
    @SuppressWarnings("deprecation") // getFromLocation síncrono: a versão assíncrona só existe a partir da API 33 (minSdk é 30).
    private void buscaNomeCidade(Location location) {

        if (!Geocoder.isPresent()) {
            return;
        }

        Geocoder geocoder = new Geocoder(getApplicationContext(), new Locale("pt", "BR"));

        executorGeocoder.execute(() -> {
            try {
                List<Address> enderecos = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);

                if (enderecos == null || enderecos.isEmpty()) {
                    return;
                }

                Address endereco = enderecos.get(0);
                // Em alguns municípios a cidade vem em subAdminArea em vez de locality.
                String cidade = endereco.getLocality() != null ? endereco.getLocality() : endereco.getSubAdminArea();

                if (cidade != null) {
                    runOnUiThread(() -> {
                        if (!isDestroyed()) {
                            binding.tvCidade.setText(cidade);
                        }
                    });
                }
            } catch (Exception e) {
                Log.w(TAG, "Erro ao obter o nome da cidade", e);
            }
        });
    }

    private void exibeClima(ForecastActual clima) {

        binding.tvTempUmid.setText(String.format(Locale.getDefault(), "%s°C | %s%%", valor(clima.getTemp()), valor(clima.getHumidity())));
        binding.ivClima.setImageResource(IconeClima.condicao(clima.getWeatherCode(), !Boolean.FALSE.equals(clima.getDia())));
        binding.ivTemp.setImageResource(IconeClima.termometro(clima.getTemp()));

        List<Forecast> previsoes = clima.getForecasts();

        if (previsoes == null || previsoes.isEmpty()) {
            return;
        }

        // Posição 0 é o dia de hoje; 1 a 3 são os próximos dias.
        exibeMaxMin(binding.tvTempHL, previsoes.get(0));

        TextView[] dias = {binding.tvDay1, binding.tvDay2, binding.tvDay3};
        TextView[] maxMin = {binding.tvTempHL1, binding.tvTempHL2, binding.tvTempHL3};
        ImageView[] icones = {binding.ivClima1, binding.ivClima2, binding.ivClima3};

        // Se a API devolver menos dias do que o layout comporta,
        // os espaços sem previsão ficam ocultos em vez de exibir "--".
        for (int i = 0; i < dias.length; i++) {

            boolean temPrevisao = i + 1 < previsoes.size();
            int visibilidade = temPrevisao ? View.VISIBLE : View.INVISIBLE;

            dias[i].setVisibility(visibilidade);
            maxMin[i].setVisibility(visibilidade);
            icones[i].setVisibility(visibilidade);

            if (temPrevisao) {
                Forecast previsao = previsoes.get(i + 1);
                dias[i].setText(previsao.getWeekday());
                exibeMaxMin(maxMin[i], previsao);
                // Previsão diária: sempre o ícone diurno.
                icones[i].setImageResource(IconeClima.condicao(previsao.getWeatherCode(), true));
            }
        }
    }

    private void exibeMaxMin(TextView textView, Forecast previsao) {

        textView.setText(String.format(Locale.getDefault(), "%s° / %s°", valor(previsao.getMax()), valor(previsao.getMin())));
    }

    private static String valor(Long valor) {

        return valor != null ? String.valueOf(valor) : "--";
    }

    /** Executa a cena fixa da porta, sempre depois de confirmar. */
    private void doorLock(Object event){

        if (!TuyaRepository.estaConfigurado()) {
            Toast.makeText(this, R.string.tuyaNaoConfiguradaCurto, Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.tuyaExecutarCenaTitulo)
                .setMessage(getString(R.string.tuyaExecutarCenaMensagem, TuyaRepository.CENA_PORTA))
                .setPositiveButton(R.string.sim, (dialog, which) -> executaCenaDaPorta())
                .setNegativeButton(R.string.nao, null)
                .show();
    }

    private void executaCenaDaPorta() {

        String nome = TuyaRepository.CENA_PORTA;

        TuyaRepository.getInstance().executaCenaPorNome(nome, new TuyaRepository.Resultado<Void>() {
            @Override
            public void aoConcluir(Void dado) {

                if (!isDestroyed()) {
                    Toast.makeText(DashBoardActivity.this, getString(R.string.tuyaCenaExecutada, nome), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void aoFalhar(String mensagem) {

                if (!isDestroyed()) {
                    Toast.makeText(DashBoardActivity.this, getString(R.string.tuyaErro, mensagem), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void control(Object event){
        startActivity(new Intent(this, DashBoardControlActivity.class));
    }

    private void listaCompras(Object event){

        startActivity(new Intent(this, ListaComprasActivity.class));
    }

    private void exibeAvisoComprasPendentes(boolean pendentes){

        int visibilidade = pendentes ? View.VISIBLE : View.GONE;

        binding.ivBuyListInfDB.setVisibility(visibilidade);
        binding.tvBuyListInfDB.setVisibility(visibilidade);
    }

    private void dashboard(Object event){

        startActivity(new Intent(this, DashBoardMetricasActivity.class));
    }

    private void configuracao(Object event){

        startActivity(new Intent(this, ConfiguracaoActivity.class));
    }

    private void reservoir(Object event){

        startActivity(new Intent(DashBoardActivity.this, DashBoardReservoirActivity.class));
    }
}
