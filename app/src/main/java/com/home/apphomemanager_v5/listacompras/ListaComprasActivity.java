package com.home.apphomemanager_v5.listacompras;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.home.apphomemanager_v5.R;
import com.home.apphomemanager_v5.databinding.ActivityListaComprasBinding;
import com.home.apphomemanager_v5.util.ComponentUtils;

public class ListaComprasActivity extends AppCompatActivity {

    private static final int[] TITULOS = {R.string.tabMinhaLista, R.string.tabDespensa, R.string.tabOpcoes};
    private static final int[] ICONES = {R.drawable.ic_tab_minha_lista, R.drawable.ic_tab_despensa, R.drawable.ic_tab_opcoes};
    private static final int[] CORES = {R.color.colorTab0, R.color.colorTab1, R.color.colorTab2};

    private ActivityListaComprasBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        binding = ActivityListaComprasBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ListaComprasViewModel viewModel = new ViewModelProvider(this).get(ListaComprasViewModel.class);

        ComponentUtils.setEventClickGeneric(binding.ivLcBack, event -> finish());

        binding.vpLc.setAdapter(new AbasAdapter(this));

        new TabLayoutMediator(binding.tabLayoutLc, binding.vpLc, (tab, position) -> {
            tab.setText(TITULOS[position]);
            tab.setIcon(ICONES[position]);
        }).attach();

        binding.tabLayoutLc.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                aplicaCorDaAba(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        aplicaCorDaAba(binding.tabLayoutLc.getSelectedTabPosition());

        viewModel.getErroConexao().observe(this, erro -> {
            if (Boolean.TRUE.equals(erro)) {
                Toast.makeText(this, R.string.msgErroListaCompras, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void aplicaCorDaAba(int posicao) {

        if (posicao < 0 || posicao >= CORES.length) {
            return;
        }

        binding.tabLayoutLc.setTabTextColors(getColor(R.color.colorTabInativa), getColor(CORES[posicao]));
    }

    private static class AbasAdapter extends FragmentStateAdapter {

        AbasAdapter(@NonNull FragmentActivity activity) {
            super(activity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {

            switch (position) {
                case 0:
                    return new MinhaListaFragment();
                case 1:
                    return new DespensaFragment();
                default:
                    return new OpcoesFragment();
            }
        }

        @Override
        public int getItemCount() {
            return TITULOS.length;
        }
    }
}
