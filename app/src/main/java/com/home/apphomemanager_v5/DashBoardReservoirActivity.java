package com.home.apphomemanager_v5;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.home.apphomemanager_v5.databinding.ActivityDashBoardBinding;
import com.home.apphomemanager_v5.databinding.ActivityDashBoardReservoirBinding;
import com.home.apphomemanager_v5.util.ComponentUtils;

public class DashBoardReservoirActivity extends AppCompatActivity {

    private ActivityDashBoardReservoirBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        binding = ActivityDashBoardReservoirBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ComponentUtils.setEventClickGeneric(binding.ivDbrBackMain, this::voltar);
        ComponentUtils.setEventClickGeneric(binding.ivDbrCistern, this::cisterna);
        ComponentUtils.setEventClickGeneric(binding.ivDbrWaterTank, this::caixaDagua);
    }

    private void cisterna(Object event){

        startActivity(new Intent(this, CisternaActivity.class));
    }

    private void caixaDagua(Object event){

        Intent intent = new Intent(this, CaixaDaguaActivity.class);
        intent.putExtra("path", "1");

        startActivity(intent);
    }

    private void voltar(Object event){

        finish();
    }
}