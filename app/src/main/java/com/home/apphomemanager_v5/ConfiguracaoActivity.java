package com.home.apphomemanager_v5;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.home.apphomemanager_v5.comodo.ComodosActivity;
import com.home.apphomemanager_v5.databinding.ActivityConfiguracaoBinding;
import com.home.apphomemanager_v5.util.ComponentUtils;

/** Menu das configurações do app; cada opção abre a sua tela. */
public class ConfiguracaoActivity extends AppCompatActivity {

    private ActivityConfiguracaoBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        binding = ActivityConfiguracaoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ComponentUtils.setEventClickGeneric(binding.ivConfBack, event -> finish());
        ComponentUtils.setEventClickGeneric(binding.llConfOpcaoComodos, event -> startActivity(new Intent(this, ComodosActivity.class)));
    }
}
