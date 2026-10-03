package com.home.apphomemanager_v5;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;

import com.google.firebase.database.DatabaseError;
import com.home.apphomemanager_v5.comodo.CelulaAdapter;
import com.home.apphomemanager_v5.comodo.CelulaAdapter.Celula;
import com.home.apphomemanager_v5.comodo.Comodo;
import com.home.apphomemanager_v5.comodo.ComodoActivity;
import com.home.apphomemanager_v5.comodo.ComodoRepository;
import com.home.apphomemanager_v5.comodo.ComodosPadrao;
import com.home.apphomemanager_v5.databinding.ActivityDashBoardControlBinding;
import com.home.apphomemanager_v5.util.ComponentUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Grade com os cômodos cadastrados. O atalho para a tela de dispositivos Tuya (TuyaDevicesActivity, de
 * diagnóstico) está desativado: a classe segue no projeto, mas nada a abre. Para reativar, volte a incluir
 * a célula "Tuya" em {@link #exibe} e o desvio em {@link #aoClicar}, e remova enabled="false" do manifesto.
 */
public class DashBoardControlActivity extends AppCompatActivity {

    private static final String TAG = "DashBoardControl";

    private ActivityDashBoardControlBinding binding;

    private ComodoRepository repository;

    private CelulaAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        repository = new ComodoRepository(this);

        binding = ActivityDashBoardControlBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ComponentUtils.setEventClickGeneric(binding.ivBackDBC, event -> finish());

        adapter = new CelulaAdapter(this::aoClicar);

        binding.rvDBC.setLayoutManager(new GridLayoutManager(this, 3));
        binding.rvDBC.setAdapter(adapter);

        exibe(new ArrayList<>());
    }

    @Override
    protected void onStart() {

        super.onStart();

        repository.semeiaSeNecessario(ComodosPadrao.cria(this));

        repository.inicia(new ComodoRepository.Listener() {
            @Override
            public void aoAtualizar(List<Comodo> comodos) {
                exibe(comodos);
            }

            @Override
            public void aoFalhar(DatabaseError erro) {
                Log.w(TAG, "Erro ao ler os cômodos: " + erro.getMessage());
            }
        });
    }

    @Override
    protected void onStop() {

        super.onStop();

        repository.para();
    }

    private void exibe(List<Comodo> comodos) {

        List<Celula> celulas = new ArrayList<>();

        for (Comodo comodo : comodos) {
            celulas.add(new Celula(comodo.id, comodo.nome, comodo.drawableIcone(), false));
        }

        adapter.atualiza(celulas);
    }

    private void aoClicar(Celula celula) {

        Intent intent = new Intent(this, ComodoActivity.class);
        intent.putExtra(ComodoActivity.EXTRA_ID, celula.id);

        startActivity(intent);
    }
}
