package com.medchecktag.ui.main;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.medchecktag.R;
import com.medchecktag.models.Medication;
import com.medchecktag.models.MedicationCategory;

import java.util.concurrent.TimeUnit;

/**
 * ViewHolder for medication list items.
 * Handles data binding, countdown formatting, and overdue/inactive styling.
 */
public class MedicationViewHolder extends RecyclerView.ViewHolder {
    
    private final CardView cardView;
    private final TextView textCountdown;
    private final TextView textCategory;
    private final TextView textNickname;
    private final TextView textDose;
    private final TextView textRemaining;
    private final TextView textOverdue;
    private final Button buttonRefill;
    private final Button buttonInfo;
    private final Button buttonWriteTag;
    
    public MedicationViewHolder(@NonNull View itemView) {
        super(itemView);
        cardView = itemView.findViewById(R.id.card_medication);
        textCountdown = itemView.findViewById(R.id.text_countdown);
        textCategory = itemView.findViewById(R.id.text_category);
        textNickname = itemView.findViewById(R.id.text_nickname);
        textDose = itemView.findViewById(R.id.text_dose);
        textRemaining = itemView.findViewById(R.id.text_remaining);
        textOverdue = itemView.findViewById(R.id.text_overdue);
        buttonRefill = itemView.findViewById(R.id.button_refill);
        buttonInfo = itemView.findViewById(R.id.button_info);
        buttonWriteTag = itemView.findViewById(R.id.button_write_tag);
    }
    
    public void bind(
        Medication medication,
        Context context,
        MedicationListAdapter.OnMedicationActionListener listener
    ) {
        // Basic information
        textNickname.setText(medication.nickname);
        textDose.setText(medication.dose);
        textRemaining.setText(context.getString(
            R.string.doses_remaining,
            medication.remainingDoses
        ));
        
        // Category badge
        String categoryText = formatCategoryName(medication.category);
        textCategory.setText(categoryText);
        textCategory.setBackgroundTintList(ColorStateList.valueOf(
            getCategoryColor(medication.category, context)
        ));
        
        // Countdown timer and overdue detection
        long currentTime = System.currentTimeMillis();
        long nextDoseTime = medication.schedule.nextDoseTime;
        long timeDiff = nextDoseTime - currentTime;
        
        boolean isOverdue = timeDiff < 0;
        
        if (isOverdue) {
            // Overdue styling
            textCountdown.setText(R.string.overdue);
            textCountdown.setTextColor(context.getColor(R.color.error_red));
            textOverdue.setVisibility(View.VISIBLE);
            cardView.setCardBackgroundColor(context.getColor(R.color.overdue_background));
        } else {
            // Normal countdown
            String countdownText = formatCountdown(timeDiff);
            textCountdown.setText(countdownText);
            textCountdown.setTextColor(context.getColor(R.color.text_primary));
            textOverdue.setVisibility(View.GONE);
            cardView.setCardBackgroundColor(context.getColor(R.color.surface));
        }
        
        // Inactive medication styling
        if (!medication.isActive) {
            applyInactiveStyling(context);
        } else {
            applyActiveStyling(context);
        }
        
        // Button click listeners
        buttonRefill.setOnClickListener(v -> listener.onMedRefilled(medication));
        buttonInfo.setOnClickListener(v -> listener.onInfoClicked(medication));
        buttonWriteTag.setOnClickListener(v -> listener.onWriteTagClicked(medication));
    }
    
    /**
     * Format time difference as countdown string.
     * Examples: "2h 30m", "45m", "23h 15m"
     */
    private String formatCountdown(long milliseconds) {
        long hours = TimeUnit.MILLISECONDS.toHours(milliseconds);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds) % 60;
        
        if (hours > 0) {
            return String.format("%dh %dm", hours, minutes);
        } else {
            return String.format("%dm", minutes);
        }
    }
    
    /**
     * Format category name for display.
     */
    private String formatCategoryName(MedicationCategory category) {
        switch (category) {
            case LIFE_DEPENDENT:
                return "LIFE-DEPENDENT";
            case VERY_IMPORTANT:
                return "VERY IMPORTANT";
            case BENEFICIAL:
                return "BENEFICIAL";
            default:
                return "UNKNOWN";
        }
    }
    
    /**
     * Get color for category badge.
     */
    private int getCategoryColor(MedicationCategory category, Context context) {
        switch (category) {
            case LIFE_DEPENDENT:
                return context.getColor(R.color.category_life_dependent);
            case VERY_IMPORTANT:
                return context.getColor(R.color.category_very_important);
            case BENEFICIAL:
                return context.getColor(R.color.category_beneficial);
            default:
                return context.getColor(R.color.text_secondary);
        }
    }
    
    /**
     * Apply inactive styling (gray out, reduce opacity).
     */
    private void applyInactiveStyling(Context context) {
        cardView.setAlpha(0.6f);
        textCountdown.setTextColor(context.getColor(R.color.text_secondary));
        textNickname.setTextColor(context.getColor(R.color.text_secondary));
        textDose.setTextColor(context.getColor(R.color.text_secondary));
        textRemaining.setTextColor(context.getColor(R.color.text_secondary));
        cardView.setCardBackgroundColor(context.getColor(R.color.inactive_background));
        
        // Disable action buttons for inactive medications
        buttonRefill.setEnabled(false);
        buttonInfo.setEnabled(true); // Info still accessible
        buttonWriteTag.setEnabled(false);
    }
    
    /**
     * Apply active styling (normal colors, full opacity).
     */
    private void applyActiveStyling(Context context) {
        cardView.setAlpha(1.0f);
        textNickname.setTextColor(context.getColor(R.color.text_primary));
        textDose.setTextColor(context.getColor(R.color.text_secondary));
        textRemaining.setTextColor(context.getColor(R.color.text_secondary));
        
        // Enable action buttons
        buttonRefill.setEnabled(true);
        buttonInfo.setEnabled(true);
        buttonWriteTag.setEnabled(true);
    }
}
