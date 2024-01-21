package com.home.apphomemanager_v4;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.home.apphomemanager_v4.databinding.ActivityDadosFirebaseBinding;
import com.home.apphomemanager_v4.model.ChurrasqueiraEntity;
import com.home.apphomemanager_v4.util.JsonUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

public class DadosFirebaseActivity extends AppCompatActivity {

    private ActivityDadosFirebaseBinding binding;

    private FirebaseDatabase database;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityDadosFirebaseBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        database = FirebaseDatabase.getInstance();
        mDatabase = database.getReference().child("churrasqueira");

        ouvinteFirebase();
    }

    private void ouvinteFirebase(){
        ValueEventListener postListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Get Post object and use the values to update the UI
//                String post = dataSnapshot.getValue(Stridzng.class);
                Toast.makeText(DadosFirebaseActivity.this, "Firebase update", Toast.LENGTH_SHORT).show();

//                Map<String, Object> data = (Map<String, Object>) dataSnapshot.getValue();

//                JSONObject json = new JSONObject();

//                StringBuilder sbJson = new StringBuilder();

                // Agora você pode trabalhar com os dados como um Map
//                for (Map.Entry<String, Object> entry : data.entrySet()) {
//                    String key = entry.getKey();
//                    Object value = entry.getValue();
//
//                    sbJson.append(key);
//                    sbJson.append(value.toString());
//
//                    try {
//                        json.put(key, value.toString());
//                    } catch (JSONException e) {
//                        Toast.makeText(DadosFirebaseActivity.this,"Erro ao montar o json", Toast.LENGTH_SHORT).show();
//                    }
//                }

//                String post = dataSnapshot.getValue(JSONObject.class).toString();
//                Log.w("Erro leitura -> post", post);

                boolean valueOnOff = dataSnapshot.child("onOff").getValue(boolean.class);
                try {


                    Toast.makeText(DadosFirebaseActivity.this, "step 1 ", Toast.LENGTH_SHORT).show();
//                    ChurrasqueiraEntity churrasqueira = ChurrasqueiraEntity.fromJson(json);
//                    ChurrasqueiraEntity churrasqueira = ChurrasqueiraEntity.fromJson(new Gson().toJson(dataSnapshot.getValue()));
                    ChurrasqueiraEntity churrasqueira = JsonUtils.fromJson(ChurrasqueiraEntity.class, new Gson().toJson(dataSnapshot.getValue()));
                    Toast.makeText(DadosFirebaseActivity.this, "step 2 ", Toast.LENGTH_SHORT).show();

                    StringBuilder sb = new StringBuilder();

//                    sb.append(json.toString(1));
                    sb.append("\n\n------------------------------------------\n\n");
                    Toast.makeText(DadosFirebaseActivity.this, "step 3 ", Toast.LENGTH_SHORT).show();
//                    sb.append(churrasqueira.toString());
//                    sb.append(sbJson.toString());
                    sb.append(new Gson().toJson(dataSnapshot.getValue()));
                    sb.append("\n\n-----------------churrasqueira.toString()-------------------------\n\n");
                    Toast.makeText(DadosFirebaseActivity.this, "step 3_1 ", Toast.LENGTH_SHORT).show();
                    sb.append(churrasqueira.toString());
                    sb.append("\n\n------------------------------------------");

                    Toast.makeText(DadosFirebaseActivity.this, "step 4 ", Toast.LENGTH_SHORT).show();

                    binding.tvDFBInfo.setText(sb.toString());
                } catch (Exception e) {
                    Toast.makeText(DadosFirebaseActivity.this, "Erro ao adicionar o json ao textview: " + e, Toast.LENGTH_SHORT).show();
                }
                binding.swOnOffChurrasqueiraGeral.setChecked(valueOnOff);
                Toast.makeText(DadosFirebaseActivity.this, "value firebase: "+(valueOnOff ? "true" : "false"), Toast.LENGTH_SHORT).show();
                // ..
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Getting Post failed, log a message
                Toast.makeText(DadosFirebaseActivity.this, "Erro Firebase update", Toast.LENGTH_SHORT).show();
                Log.w("Erro leitura", "loadPost:onCancelled", databaseError.toException());
            }
        };
        mDatabase.addValueEventListener(postListener);
    }
}