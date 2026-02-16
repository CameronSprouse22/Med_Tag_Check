package com.medchecktag.ui.medication;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.textfield.TextInputEditText;
import com.medchecktag.R;
import com.medchecktag.viewmodels.AddMedicationViewModel;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Fragment for interval-based schedule configuration.
 * Task T055 - User Story 2: Add Medication
 */
public class ScheduleIntervalFragment extends Fragment {
    
    private AddMedicationViewModel viewModel;
    
    private TextInputEditText inputIntervalHours;
    private CheckBox checkboxEndDate;
    private Button buttonSelectEndDate;
    private TextView textSelectedEndDate;
    
    private Long selectedEndDate = null;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.schedule_interval_fragment, container, false);
        
        // Get ViewModel from Activity
        if (getActivity() instanceof AddMedicationActivity) {
            viewModel = ((AddMedicationActivity) getActivity()).getViewModel();
        } else {
            viewModel = new ViewModelProvider(requireActivity()).get(AddMedicationViewModel.class);
        }
        
        // Initialize views
        initializeViews(view);
        
        // Setup listeners
        setupListeners();
        
        // Load default values
        loadDefaults();
        
        return view;
    }
    
    private void initializeViews(View view) {
        inputIntervalHours = view.findViewById(R.id.input_interval_hours);
        checkboxEndDate = view.findViewById(R.id.checkbox_end_date);
        buttonSelectEndDate = view.findViewById(R.id.button_select_end_date);
        textSelectedEndDate = view.findViewById(R.id.text_selected_end_date);
    }
    
    private void setupListeners() {
        // End date checkbox
        checkboxEndDate.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                buttonSelectEndDate.setVisibility(View.VISIBLE);
                if (selectedEndDate != null) {
                    textSelectedEndDate.setVisibility(View.VISIBLE);
                }
            } else {
                buttonSelectEndDate.setVisibility(View.GONE);
                textSelectedEndDate.setVisibility(View.GONE);
                selectedEndDate = null;
                viewModel.setEndDate(null);
            }
        });
        
        // End date picker button
        buttonSelectEndDate.setOnClickListener(v -> showDatePicker());
        
        // Interval hours input change
        // Will be collected when save is pressed
    }
    
    private void loadDefaults() {
        // Observe ViewModel for initial values
        viewModel.getIntervalHours().observe(getViewLifecycleOwner(), hours -> {
            if (hours != null && inputIntervalHours.getText() != null && 
                inputIntervalHours.getText().toString().isEmpty()) {
                inputIntervalHours.setText(String.valueOf(hours));
            }
        });
    }
    
    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        if (selectedEndDate != null) {
            calendar.setTimeInMillis(selectedEndDate);
        }
        
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        
        DatePickerDialog datePickerDialog = new DatePickerDialog(
            requireContext(),
            (view, selectedYear, selectedMonth, selectedDay) -> {
                Calendar selected = Calendar.getInstance();
                selected.set(selectedYear, selectedMonth, selectedDay, 23, 59, 59);
                selectedEndDate = selected.getTimeInMillis();
                
                viewModel.setEndDate(selectedEndDate);
                
                // Display selected date
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                textSelectedEndDate.setText("End date: " + sdf.format(selectedEndDate));
                textSelectedEndDate.setVisibility(View.VISIBLE);
            },
            year,
            month,
            day
        );
        
        // Set minimum date to today
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());
        datePickerDialog.show();
    }
    
    @Override
    public void onPause() {
        super.onPause();
        // Save interval hours to ViewModel
        String hoursStr = inputIntervalHours.getText() != null ? 
            inputIntervalHours.getText().toString() : "8";
        try {
            int hours = Integer.parseInt(hoursStr);
            viewModel.setIntervalHours(hours);
        } catch (NumberFormatException e) {
            viewModel.setIntervalHours(8);
        }
    }
}
