package com.medchecktag.ui.main;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;

import com.medchecktag.R;
import com.medchecktag.models.Medication;

/**
 * RecyclerView adapter for medication list.
 * Handles countdown timer updates, overdue detection, and user interactions.
 */
public class MedicationListAdapter extends ListAdapter<Medication, MedicationViewHolder> {
    
    private final Context context;
    private final OnMedicationActionListener actionListener;
    private final Handler countdownHandler;
    private final Runnable countdownRunnable;
    
    // Update countdown timers every minute
    private static final long UPDATE_INTERVAL_MS = 60000; // 60 seconds
    
    public interface OnMedicationActionListener {
        void onMedRefilled(Medication medication);
        void onInfoClicked(Medication medication);
        void onWriteTagClicked(Medication medication);
    }
    
    public MedicationListAdapter(
        Context context,
        OnMedicationActionListener actionListener
    ) {
        super(DIFF_CALLBACK);
        this.context = context;
        this.actionListener = actionListener;
        
        // Setup countdown timer handler
        this.countdownHandler = new Handler(Looper.getMainLooper());
        this.countdownRunnable = new Runnable() {
            @Override
            public void run() {
                notifyDataSetChanged(); // Refresh all items
                countdownHandler.postDelayed(this, UPDATE_INTERVAL_MS);
            }
        };
        
        // Start countdown updates
        countdownHandler.postDelayed(countdownRunnable, UPDATE_INTERVAL_MS);
    }
    
    @NonNull
    @Override
    public MedicationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.medication_list_item, parent, false);
        return new MedicationViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull MedicationViewHolder holder, int position) {
        Medication medication = getItem(position);
        holder.bind(medication, context, actionListener);
    }
    
    /**
     * Stop countdown timer updates when adapter is no longer needed.
     */
    public void stopCountdownUpdates() {
        countdownHandler.removeCallbacks(countdownRunnable);
    }
    
    private static final DiffUtil.ItemCallback<Medication> DIFF_CALLBACK =
        new DiffUtil.ItemCallback<Medication>() {
            @Override
            public boolean areItemsTheSame(@NonNull Medication oldItem, @NonNull Medication newItem) {
                return oldItem.id.equals(newItem.id);
            }
            
            @Override
            public boolean areContentsTheSame(@NonNull Medication oldItem, @NonNull Medication newItem) {
                // Compare relevant fields with null safety
                if (oldItem.schedule == null || newItem.schedule == null) {
                    return oldItem.nickname.equals(newItem.nickname)
                        && oldItem.dose.equals(newItem.dose)
                        && oldItem.remainingDoses == newItem.remainingDoses
                        && oldItem.isActive == newItem.isActive
                        && (oldItem.schedule == newItem.schedule);
                }
                return oldItem.nickname.equals(newItem.nickname)
                    && oldItem.dose.equals(newItem.dose)
                    && oldItem.remainingDoses == newItem.remainingDoses
                    && oldItem.schedule.nextDoseTime == newItem.schedule.nextDoseTime
                    && oldItem.isActive == newItem.isActive;
            }
        };
}
