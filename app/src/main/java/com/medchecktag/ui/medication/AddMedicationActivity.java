package com.medchecktag.ui.medication;

import android.app.DatePickerDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import com.google.android.material.textfield.TextInputEditText;
import com.medchecktag.R;
import com.medchecktag.alarms.AlarmScheduler;
import com.medchecktag.models.MedicationCategory;
import com.medchecktag.models.ScheduleType;
import com.medchecktag.ui.nfc.NFCWriteDialogFragment;
import com.medchecktag.viewmodels.AddMedicationViewModel;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Activity for adding new medication.
 * Task T054 - User Story 2: Add Medication
 */
public class AddMedicationActivity extends AppCompatActivity {
    
    private AddMedicationViewModel viewModel;
    
    // UI elements
    private TextInputEditText inputNickname;
    private TextInputEditText inputDose;
    private TextInputEditText inputMaxDoseCount;
    private TextInputEditText inputRefillThreshold1;
    private TextInputEditText inputRefillThreshold2;
    private TextInputEditText inputMedicationInfo;
    private Spinner spinnerCategory;
    private RadioGroup radioScheduleType;
    private RadioButton radioInterval;
    private RadioButton radioSpecificTimes;
    private TextView textError;
    private Button buttonCancel;
    private Button buttonSave;
    
    private ScheduleIntervalFragment intervalFragment;
    private ScheduleSpecificTimesFragment specificTimesFragment;
    private AlarmConfigurationFragment alarmConfigFragment;
    private AlarmScheduler alarmScheduler;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_medication_layout);
        
        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(AddMedicationViewModel.class);
        alarmScheduler = new AlarmScheduler(this);
        
        // Initialize UI elements
        initializeViews();
        
        // Setup category spinner
        setupCategorySpinner();
        
        // Setup schedule type switching
        setupScheduleTypeSwitch();
        
        // Setup button listeners
        setupButtons();
        
        // Observe ViewModel
        observeViewModel();
        
        // Load initial schedule fragment
        loadScheduleFragment(ScheduleType.INTERVAL);

        // T116: Load alarm configuration fragment
        loadAlarmConfigFragment();
    }
    
    private void initializeViews() {
        inputNickname = findViewById(R.id.input_nickname);
        inputDose = findViewById(R.id.input_dose);
        inputMaxDoseCount = findViewById(R.id.input_max_dose_count);
        inputRefillThreshold1 = findViewById(R.id.input_refill_threshold1);
        inputRefillThreshold2 = findViewById(R.id.input_refill_threshold2);
        inputMedicationInfo = findViewById(R.id.input_medication_info);
        spinnerCategory = findViewById(R.id.spinner_category);
        radioScheduleType = findViewById(R.id.radio_schedule_type);
        radioInterval = findViewById(R.id.radio_interval);
        radioSpecificTimes = findViewById(R.id.radio_specific_times);
        textError = findViewById(R.id.text_error);
        buttonCancel = findViewById(R.id.button_cancel);
        buttonSave = findViewById(R.id.button_save);
    }
    
    private void setupCategorySpinner() {
        MedicationCategory[] categories = MedicationCategory.values();
        String[] categoryNames = new String[categories.length];
        for (int i = 0; i < categories.length; i++) {
            categoryNames[i] = formatCategoryName(categories[i]);
        }
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            this, 
            android.R.layout.simple_spinner_item, 
            categoryNames
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);
        
        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                viewModel.setCategory(categories[position]);
                // T221: Apply refill threshold defaults from settings
                applyRefillThresholdDefaults(categories[position]);
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
        
        // Set default to BENEFICIAL (index 2)
        spinnerCategory.setSelection(2);
    }

    /**
     * T221: Pre-fill refill thresholds based on category from settings.
     */
    private void applyRefillThresholdDefaults(MedicationCategory category) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String categoryKey;
        int defaultT1, defaultT2;

        switch (category) {
            case LIFE_DEPENDENT:
                categoryKey = "life_dependent";
                defaultT1 = 14;
                defaultT2 = 7;
                break;
            case VERY_IMPORTANT:
                categoryKey = "very_important";
                defaultT1 = 10;
                defaultT2 = 5;
                break;
            default:
                categoryKey = "beneficial";
                defaultT1 = 7;
                defaultT2 = 3;
                break;
        }

        String t1 = prefs.getString("refill_threshold1_" + categoryKey, String.valueOf(defaultT1));
        String t2 = prefs.getString("refill_threshold2_" + categoryKey, String.valueOf(defaultT2));

        if (inputRefillThreshold1 != null) inputRefillThreshold1.setText(t1);
        if (inputRefillThreshold2 != null) inputRefillThreshold2.setText(t2);
    }
    
    private String formatCategoryName(MedicationCategory category) {
        switch (category) {
            case LIFE_DEPENDENT:
                return "Life-Dependent";
            case VERY_IMPORTANT:
                return "Very Important";
            case BENEFICIAL:
                return "Beneficial";
            default:
                return category.name();
        }
    }
    
    private void setupScheduleTypeSwitch() {
        radioScheduleType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radio_interval) {
                viewModel.setScheduleType(ScheduleType.INTERVAL);
                loadScheduleFragment(ScheduleType.INTERVAL);
            } else if (checkedId == R.id.radio_specific_times) {
                viewModel.setScheduleType(ScheduleType.SPECIFIC_TIMES);
                loadScheduleFragment(ScheduleType.SPECIFIC_TIMES);
            }
        });
    }
    
    private void loadScheduleFragment(ScheduleType scheduleType) {
        Fragment fragment;
        
        if (scheduleType == ScheduleType.INTERVAL) {
            if (intervalFragment == null) {
                intervalFragment = new ScheduleIntervalFragment();
            }
            fragment = intervalFragment;
        } else {
            if (specificTimesFragment == null) {
                specificTimesFragment = new ScheduleSpecificTimesFragment();
            }
            fragment = specificTimesFragment;
        }
        
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.schedule_fragment_container, fragment);
        transaction.commit();
    }
    
    /**
     * T116: Load AlarmConfigurationFragment into the alarm fragment container.
     */
    private void loadAlarmConfigFragment() {
        if (alarmConfigFragment == null) {
            alarmConfigFragment = new AlarmConfigurationFragment();
        }
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.alarm_fragment_container, alarmConfigFragment)
                .commit();
    }

    private void setupButtons() {
        buttonCancel.setOnClickListener(v -> finish());
        
        buttonSave.setOnClickListener(v -> {
            // Collect form data
            collectFormData();
            
            // Save medication
            viewModel.saveMedication();
        });
    }
    
    private void collectFormData() {
        // Get text inputs
        String nickname = inputNickname.getText() != null ? inputNickname.getText().toString() : "";
        String dose = inputDose.getText() != null ? inputDose.getText().toString() : "";
        String info = inputMedicationInfo.getText() != null ? inputMedicationInfo.getText().toString() : "";
        
        String maxDoseStr = inputMaxDoseCount.getText() != null ? inputMaxDoseCount.getText().toString() : "30";
        String threshold1Str = inputRefillThreshold1.getText() != null ? inputRefillThreshold1.getText().toString() : "7";
        String threshold2Str = inputRefillThreshold2.getText() != null ? inputRefillThreshold2.getText().toString() : "3";
        
        // Update ViewModel
        viewModel.setNickname(nickname);
        viewModel.setDose(dose);
        viewModel.setMedicationInfo(info);
        
        try {
            viewModel.setMaxDoseCount(Integer.parseInt(maxDoseStr));
        } catch (NumberFormatException e) {
            viewModel.setMaxDoseCount(30);
        }
        
        try {
            viewModel.setRefillThreshold1(Integer.parseInt(threshold1Str));
        } catch (NumberFormatException e) {
            viewModel.setRefillThreshold1(7);
        }
        
        try {
            viewModel.setRefillThreshold2(Integer.parseInt(threshold2Str));
        } catch (NumberFormatException e) {
            viewModel.setRefillThreshold2(3);
        }
        
        // Schedule data is collected by fragments
    }
    
    private void observeViewModel() {
        // Observe validation errors
        viewModel.getValidationError().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                textError.setText(error);
                textError.setVisibility(View.VISIBLE);
            } else {
                textError.setVisibility(View.GONE);
            }
        });
        
        // Observe save result
        viewModel.getSaveResult().observe(this, result -> {
            if (result != null) {
                if (result.success) {
                    // Show success message
                    Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show();

                    // T120: Schedule alarms for newly created medication
                    viewModel.scheduleAlarms(alarmScheduler);

                    // Launch NFC write dialog
                    showNFCWriteDialog(result.medicationId);
                } else {
                    // Show error message
                    Toast.makeText(this, result.message, Toast.LENGTH_LONG).show();
                }
            }
        });
    }
    
    private void showNFCWriteDialog(String medicationId) {
        NFCWriteDialogFragment dialog = NFCWriteDialogFragment.newInstance(medicationId);
        dialog.show(getSupportFragmentManager(), "nfc_write_dialog");
        
        // Listen for dialog completion
        dialog.setOnWriteCompleteListener(new NFCWriteDialogFragment.OnWriteCompleteListener() {
            @Override
            public void onWriteSuccess() {
                // NFC write successful, finish activity
                Toast.makeText(AddMedicationActivity.this, "Medication added successfully!", Toast.LENGTH_SHORT).show();
                finish();
            }
            
            @Override
            public void onWriteCancelled() {
                // User cancelled NFC write, but medication is saved
                Toast.makeText(AddMedicationActivity.this, "Medication saved. You can write NFC tag later.", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }
    
    public AddMedicationViewModel getViewModel() {
        return viewModel;
    }
}
