package com.home.apphomemanager_v5;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.home.apphomemanager_v5.databinding.ActivityMainBinding;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    private List<String> mensagensErros;
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mensagensErros = Arrays.asList(getString(R.string.email), getString(R.string.senha));

        binding.eTSSIDLGN.setText("teste@gmail.com");
        binding.eTPasswordLGN.setText("123456");

        binding.btEnviarLGN.setOnClickListener(e -> loginFirebase());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mAuth.signOut();
    }

    private void loginFirebase(){

        String email = binding.eTSSIDLGN.getText().toString().trim();
        String password = binding.eTPasswordLGN.getText().toString().trim();

        List<Integer> erros = validaCamposText(Arrays.asList(binding.eTSSIDLGN, binding.eTPasswordLGN));

        if(erros.isEmpty()){
            sendDataFirebaseLogin(email, password);
        }else{
            String mensagem = erros.size() == 1 ? getString(R.string.campoNaoPreenchido) : getString(R.string.camposNaoPreenchidos);

            informaErros(mensagem, erros.stream().map(mensagensErros::get).collect(Collectors.toList()));
        }
    }

    private void sendDataFirebaseLogin(String email, String password){

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();

                        Toast.makeText(this, "Login ok", Toast.LENGTH_SHORT).show();

//                        startActivity(new Intent(this, DadosFirebaseActivity.class));
//                        startActivity(new Intent(this, ChurrasqueiraActivity.class));
//                        startActivity(new Intent(this, CisternaActivity.class));

                        Intent intent = new Intent(this, CaixaDaguaActivity.class);
                        intent.putExtra("path", "1");
                        startActivity(intent);
                    } else {
                        Toast.makeText(this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private List<Integer> validaCamposText(List<EditText> campos) {

        return IntStream.range(0, campos.size())
                .filter(i -> campos.get(i).getText().toString().trim().isEmpty())
                .boxed()
                .collect(Collectors.toList());
    }

    private void informaErros(String mensagem, List<String> mensagensErros){
        Toast.makeText(this, mensagem + String.join(", ", mensagensErros), Toast.LENGTH_SHORT).show();
    }
}