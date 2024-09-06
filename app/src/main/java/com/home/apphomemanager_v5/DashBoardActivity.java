package com.home.apphomemanager_v5;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.home.apphomemanager_v5.databinding.ActivityCisternaBinding;
import com.home.apphomemanager_v5.databinding.ActivityDashBoardBinding;
import com.home.apphomemanager_v5.inteface.WeatherService;
import com.home.apphomemanager_v5.util.ComponentUtils;
import com.home.apphomemanager_v5.util.DebuggerUtils;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class DashBoardActivity extends AppCompatActivity {

    private ActivityDashBoardBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        binding = ActivityDashBoardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.hgbrasil.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        WeatherService weatherService = retrofit.create(WeatherService.class);


        ComponentUtils.setEventClickGeneric(binding.ivDoorLockDB, this::doorLock);
        ComponentUtils.setEventClickGeneric(binding.ivControlDB, this::control);
        ComponentUtils.setEventClickGeneric(binding.ivReservoirDB, this::reservoir);
    }

    private void doorLock(Object event){

        Toast.makeText(this, "Button doorLock clicado!", Toast.LENGTH_SHORT).show();
    }

    private void control(Object event){
        Toast.makeText(this, "Button control clicado!", Toast.LENGTH_SHORT).show();
    }

    private void reservoir(Object event){

        startActivity(new Intent(DashBoardActivity.this, DashBoardReservoirActivity.class));
    }
}