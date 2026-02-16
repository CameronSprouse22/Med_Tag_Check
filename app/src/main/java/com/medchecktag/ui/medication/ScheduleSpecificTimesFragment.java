package com.medchecktag.ui.medication;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.medchecktag.R;
import com.medchecktag.viewmodels.AddMedicationViewModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Fragment for specific times schedule configuration.
 * Task T056 - User Story 2: Add Medication
 */
public class ScheduleSpecificTimesFragment extends Fragment {
    
    private AddMedicationViewModel viewModel;
    
    private Button buttonAddTime;
    private RecyclerView recyclerTimes;
    private TextView textEmptyState;
    
    private TimeListAdapter adapter;
    private List<String> times = new ArrayList<>();
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.schedule_specific_times_fragment, container, false);
        
        // Get ViewModel from Activity
        if (getActivity() instanceof AddMedicationActivity) {
            viewModel = ((AddMedicationActivity) getActivity()).getViewModel();
        } else {
            viewModel = new ViewModelProvider(requireActivity()).get(AddMedicationViewModel.class);
        }
        
        // Initialize views
        initializeViews(view);
        
        // Setup RecyclerView
        setupRecyclerView();
        
        // Setup listeners
        setupListeners();
        
        // Load existing times if any
        loadExistingTimes();
        
        return view;
    }
    
    private void initializeViews(View view) {
        buttonAddTime = view.findViewById(R.id.button_add_time);
        recyclerTimes = view.findViewById(R.id.recycler_times);
        textEmptyState = view.findViewById(R.id.text_empty_state);
    }
    
    private void setupRecyclerView() {
        adapter = new TimeListAdapter(times, this::removeTime);
        recyclerTimes.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerTimes.setAdapter(adapter);
        
        updateEmptyState();
    }
    
    private void setupListeners() {
        buttonAddTime.setOnClickListener(v -> showTimePicker());
    }
    
    private void loadExistingTimes() {
        viewModel.getSpecificTimes().observe(getViewLifecycleOwner(), existingTimes -> {
            if (existingTimes != null && !existingTimes.isEmpty() && times.isEmpty()) {
                times.clear();
                times.addAll(existingTimes);
                sortTimes();
                adapter.notifyDataSetChanged();
                updateEmptyState();
            }
        });
    }
    
    private void showTimePicker() {
        int hour = 8;
        int minute = 0;
        
        TimePickerDialog timePickerDialog = new TimePickerDialog(
            requireContext(),
            (view, selectedHour, selectedMinute) -> {
                String timeString = String.format(Locale.US, "%02d:%02d", selectedHour, selectedMinute);
                
                // Check if time already exists
                if (!times.contains(timeString)) {
                    times.add(timeString);
                    sortTimes();
                    adapter.notifyDataSetChanged();
                    updateEmptyState();
                    updateViewModel();
                }
            },
            hour,
            minute,
            true // 24-hour format
        );
        
        timePickerDialog.show();
    }
    
    private void removeTime(int position) {
        if (position >= 0 && position < times.size()) {
            times.remove(position);
            adapter.notifyItemRemoved(position);
            updateEmptyState();
            updateViewModel();
        }
    }
    
    private void sortTimes() {
        Collections.sort(times);
    }
    
    private void updateEmptyState() {
        if (times.isEmpty()) {
            textEmptyState.setVisibility(View.VISIBLE);
            recyclerTimes.setVisibility(View.GONE);
        } else {
            textEmptyState.setVisibility(View.GONE);
            recyclerTimes.setVisibility(View.VISIBLE);
        }
    }
    
    private void updateViewModel() {
        viewModel.setSpecificTimes(new ArrayList<>(times));
    }
    
    @Override
    public void onPause() {
        super.onPause();
        updateViewModel();
    }
    
    /**
     * RecyclerView Adapter for time list
     */
    private static class TimeListAdapter extends RecyclerView.Adapter<TimeViewHolder> {
        private final List<String> times;
        private final OnRemoveListener removeListener;
        
        interface OnRemoveListener {
            void onRemove(int position);
        }
        
        TimeListAdapter(List<String> times, OnRemoveListener listener) {
            this.times = times;
            this.removeListener = listener;
        }
        
        @NonNull
        @Override
        public TimeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_1, parent, false);
            return new TimeViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(@NonNull TimeViewHolder holder, int position) {
            String time = times.get(position);
            holder.bind(time, position, removeListener);
        }
        
        @Override
        public int getItemCount() {
            return times.size();
        }
    }
    
    private static class TimeViewHolder extends RecyclerView.ViewHolder {
        private final TextView textView;
        
        TimeViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(android.R.id.text1);
        }
        
        void bind(String time, int position, TimeListAdapter.OnRemoveListener listener) {
            textView.setText(time);
            itemView.setOnLongClickListener(v -> {
                listener.onRemove(position);
                return true;
            });
        }
    }
}
