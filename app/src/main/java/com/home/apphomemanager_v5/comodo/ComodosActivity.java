package com.home.apphomemanager_v5.comodo;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.database.DatabaseError;
import com.home.apphomemanager_v5.databinding.ActivityComodosBinding;
import com.home.apphomemanager_v5.util.ComponentUtils;

import java.util.List;

/** Lista dos cômodos cadastrados; o botão do topo cria um novo e tocar em um deles o edita. */
public class ComodosActivity extends AppCompatActivity {

    private static final String TAG = "Comodos";

    private ActivityComodosBinding binding;

    private ComodoRepository repository;

    private ComodoListaAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        repository = new ComodoRepository(this);

        binding = ActivityComodosBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ComponentUtils.setEventClickGeneric(binding.ivComodosBack, event -> finish());
        ComponentUtils.setEventClickGeneric(binding.ivComodosAdd, event -> abreEdicao(null));

        adapter = new ComodoListaAdapter(comodo -> abreEdicao(comodo.id));

        binding.rvComodos.setLayoutManager(new LinearLayoutManager(this));
        binding.rvComodos.setAdapter(adapter);
    }

    @Override
    protected void onStart() {

        super.onStart();

        repository.semeiaSeNecessario(ComodosPadrao.cria(this));

        repository.inicia(new ComodoRepository.Listener() {
            @Override
            public void aoAtualizar(List<Comodo> comodos) {

                adapter.atualiza(comodos);
                binding.pbComodos.setVisibility(View.GONE);
                binding.tvComodosMensagem.setVisibility(comodos.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void aoFalhar(DatabaseError erro) {
                Log.w(TAG, "Erro ao ler os cômodos: " + erro.getMessage());
                binding.pbComodos.setVisibility(View.GONE);
            }
        });
    }

    @Override
    protected void onStop() {

        super.onStop();

        repository.para();
    }

    private void abreEdicao(String comodoId) {

        Intent intent = new Intent(this, ComodoEdicaoActivity.class);

        if (comodoId != null) {
            intent.putExtra(ComodoEdicaoActivity.EXTRA_ID, comodoId);
        }

        startActivity(intent);
    }
}
