package com.medchecktag.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.medchecktag.R;
import com.medchecktag.models.Medication;
import com.medchecktag.ui.medication.AddMedicationActivity;
import com.medchecktag.ui.settings.SettingsActivity;
import com.medchecktag.viewmodels.MainViewModel;

/**
 * Main activity displaying medication list with countdown timers and action buttons.
 * Serves as the app's launcher and primary navigation hub.
 */
public class MainActivity extends AppCompatActivity {
    
    private MainViewModel viewModel;
    private RecyclerView recyclerView;
    private MedicationListAdapter adapter;
    private LinearLayout emptyStateLayout;
    
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(MainViewModel.class);
        
        // Setup RecyclerView
        recyclerView = findViewById(R.id.recycler_medications);
        emptyStateLayout = findViewById(R.id.layout_empty_state);
        
        adapter = new MedicationListAdapter(
            this,
            new MedicationListAdapter.OnMedicationActionListener() {
                @Override
                public void onMedRefilled(Medication medication) {
                    MainActivity.this.onMedRefilled(medication);
                }
                
                @Override
                public void onInfoClicked(Medication medication) {
                    MainActivity.this.onInfoClicked(medication);
                }
                
                @Override
                public void onWriteTagClicked(Medication medication) {
                    MainActivity.this.onWriteTagClicked(medication);
                }
            }
        );
        
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        
        // Observe medications
        viewModel.getActiveMedications().observe(this, medications -> {
            if (medications != null) {
                adapter.submitList(medications);
                
                // Show/hide empty state
                if (medications.isEmpty()) {
                    recyclerView.setVisibility(View.GONE);
                    emptyStateLayout.setVisibility(View.VISIBLE);
                } else {
                    recyclerView.setVisibility(View.VISIBLE);
                    emptyStateLayout.setVisibility(View.GONE);
                }
            }
        });
        
        // Observe refill results
        viewModel.getRefillResult().observe(this, result -> {
            if (result != null) {
                Toast.makeText(
                    this,
                    result.message,
                    result.success ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG
                ).show();
            }
        });
        
        // Setup bottom navigation
        setupBottomNavigation();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Countdown timers will auto-update via adapter's Handler
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // Adapter will stop timer updates automatically
    }
    
    private void setupBottomNavigation() {
        // Settings button
        findViewById(R.id.button_settings).setOnClickListener(v -> {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        });
        
        // Read Tag button (NFC scan handled in onNewIntent in Phase 5)
        findViewById(R.id.button_read_tag).setOnClickListener(v -> {
            Toast.makeText(
                this,
                R.string.nfc_scan_prompt,
                Toast.LENGTH_SHORT
            ).show();
            // NFC scan functionality will be added in Phase 5
        });
        
        // Add Medication button
        findViewById(R.id.button_add_medication).setOnClickListener(v -> {
            Intent intent = new Intent(this, AddMedicationActivity.class);
            startActivity(intent);
        });
    }
    
    /**
     * Handle "Med Refilled" button click.
     */
    private void onMedRefilled(Medication medication) {
        viewModel.onMedRefilled(medication);
    }
    
    /**
     * Handle "Info" button click.
     * Navigate to MedicationInfoActivity (will be created in later phase).
     */
    private void onInfoClicked(Medication medication) {
        Toast.makeText(
            this,
            "Medication info: " + medication.nickname,
            Toast.LENGTH_SHORT
        ).show();
        // MedicationInfoActivity will be created in Phase 7
    }
    
    /**
     * Handle "Write Tag" button click.
     * Launch NFC write dialog for this medication.
     */
    private void onWriteTagClicked(Medication medication) {
        Toast.makeText(
            this,
            "Write NFC tag: " + medication.nickname,
            Toast.LENGTH_SHORT
        ).show();
        // NFCWriteDialogFragment will be launched in later implementation
        // For now, just show a placeholder message
    }
}
