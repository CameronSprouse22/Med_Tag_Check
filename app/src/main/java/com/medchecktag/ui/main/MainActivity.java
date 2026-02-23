package com.medchecktag.ui.main;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.Manifest;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.medchecktag.R;
import com.medchecktag.audio.AudioFeedbackService;
import com.medchecktag.models.Medication;
import com.medchecktag.nfc.NFCHandler;
import com.medchecktag.services.MissedDoseWorker;
import com.medchecktag.ui.medication.AddMedicationActivity;
import com.medchecktag.ui.medication.MedicationInfoActivity;
import com.medchecktag.ui.nfc.NFCReadDialogFragment;
import com.medchecktag.ui.nfc.NFCScanSuccessDialogFragment;
import com.medchecktag.ui.settings.SettingsActivity;
import com.medchecktag.viewmodels.DoseConfirmationViewModel;
import com.medchecktag.viewmodels.MainViewModel;

import java.util.concurrent.TimeUnit;

/**
 * Main activity displaying medication list with countdown timers and action buttons.
 * Serves as the app's launcher, primary navigation hub, and NFC tag handler.
 *
 * Tasks: T069 (US3), T088-T090 (US1 NFC dispatch), T092-T095 (read/error),
 *        T102 (audio), T107-T108 (success dialog / LiveData)
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int REQUEST_EMERGENCY_PERMISSIONS = 1001;

    private MainViewModel viewModel;
    private DoseConfirmationViewModel doseConfirmationViewModel;
    private RecyclerView recyclerView;
    private MedicationListAdapter adapter;
    private LinearLayout emptyStateLayout;

    // NFC (T088-T090)
    private NfcAdapter nfcAdapter;
    private PendingIntent nfcPendingIntent;
    private IntentFilter[] nfcIntentFilters;
    private String[][] nfcTechLists;
    private NFCHandler nfcHandler;

    // Audio (T102, T104)
    private AudioFeedbackService audioFeedbackService;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Initialize ViewModels
        viewModel = new ViewModelProvider(this).get(MainViewModel.class);
        doseConfirmationViewModel = new ViewModelProvider(this).get(DoseConfirmationViewModel.class);

        // T233: Schedule missed dose detection worker (every 15 minutes)
        scheduleMissedDoseWorker();

        // T248: Request emergency permissions
        requestEmergencyPermissions();

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

        // T108, T200-T201: Observe all medications — active first, inactive at bottom
        viewModel.getAllMedications().observe(this, medications -> {
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

        // T107: Observe dose confirmation result — show success dialog & trigger audio
        doseConfirmationViewModel.getConfirmationResult().observe(this, result -> {
            if (result != null) {
                showScanSuccessDialog(result);
                triggerAudioConfirmation(result);
            }
        });

        // T094-T095: Observe scan errors
        doseConfirmationViewModel.getErrorMessage().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
                if (audioFeedbackService != null && audioFeedbackService.isReady()) {
                    audioFeedbackService.speakError(error, null);
                }
            }
        });

        // Setup NFC (T088)
        setupNFC();

        // T104: Initialize audio feedback service
        audioFeedbackService = new AudioFeedbackService();
        audioFeedbackService.initialize(this, success -> {
            if (!success) {
                Log.w(TAG, "TTS initialization failed");
            }
        });

        // Setup bottom navigation
        setupBottomNavigation();

        // Handle NFC intent that launched the activity
        handleIntent(getIntent());
    }

    // ─── NFC Foreground Dispatch ────────────────────────────────────────

    /**
     * T088: Setup NFC adapter and foreground dispatch components.
     */
    private void setupNFC() {
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        nfcHandler = new NFCHandler();

        if (nfcAdapter == null) {
            Log.w(TAG, "NFC not available on this device");
            return;
        }

        // PendingIntent for foreground dispatch
        Intent intent = new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags |= PendingIntent.FLAG_MUTABLE;
        }
        nfcPendingIntent = PendingIntent.getActivity(this, 0, intent, flags);

        // Accept NDEF text/plain and all tags as fallback
        try {
            IntentFilter ndefFilter = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
            ndefFilter.addDataType("text/plain");
            IntentFilter tagFilter = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
            nfcIntentFilters = new IntentFilter[]{ndefFilter, tagFilter};
        } catch (IntentFilter.MalformedMimeTypeException e) {
            Log.e(TAG, "Malformed MIME type", e);
            nfcIntentFilters = new IntentFilter[]{};
        }

        // Tech list for Ndef
        nfcTechLists = new String[][]{new String[]{Ndef.class.getName()}};
    }

    /**
     * T089: Enable foreground dispatch when activity is in the foreground.
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (nfcAdapter != null && nfcPendingIntent != null) {
            try {
                nfcAdapter.enableForegroundDispatch(this, nfcPendingIntent, nfcIntentFilters, nfcTechLists);
            } catch (Exception e) {
                Log.e(TAG, "Failed to enable foreground dispatch", e);
            }
        }
    }

    /**
     * T090: Disable foreground dispatch when leaving foreground.
     */
    @Override
    protected void onPause() {
        super.onPause();
        if (nfcAdapter != null) {
            try {
                nfcAdapter.disableForegroundDispatch(this);
            } catch (IllegalStateException e) {
                Log.w(TAG, "Foreground dispatch already disabled", e);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (adapter != null) {
            adapter.stopCountdownUpdates();
        }
        if (audioFeedbackService != null) {
            audioFeedbackService.shutdown();
        }
    }

    /**
     * T088: Handle NFC intents when activity is already running (singleTop).
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    // ─── NFC Intent Handling ────────────────────────────────────────────

    /**
     * T092: Process incoming NFC intent — read tag, dispatch to ViewModel.
     */
    private void handleIntent(Intent intent) {
        if (intent == null) return;

        String action = intent.getAction();
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) {

            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            if (tag == null) {
                Toast.makeText(this, R.string.nfc_scan_error, Toast.LENGTH_SHORT).show();
                return;
            }
            processNFCTag(tag);
        }
    }

    /**
     * T092-T095: Read medication ID from tag, handle errors, delegate to ViewModel.
     * T159: If NFCReadDialogFragment is showing, forward scan to it instead.
     */
    private void processNFCTag(Tag tag) {
        try {
            // T092: Read with retry (up to 3 attempts)
            String medicationId = nfcHandler.readWithRetry(tag, 3, 200);

            if (medicationId == null) {
                // T094: Unrecognized or empty tag
                Toast.makeText(this, R.string.nfc_tag_not_recognized, Toast.LENGTH_LONG).show();
                if (audioFeedbackService != null && audioFeedbackService.isReady()) {
                    audioFeedbackService.speakError(getString(R.string.nfc_tag_not_recognized), null);
                }
                return;
            }

            // T159: Check if NFCReadDialogFragment is showing — forward scan to it
            NFCReadDialogFragment readDialog = (NFCReadDialogFragment)
                    getSupportFragmentManager().findFragmentByTag("nfc_read");
            if (readDialog != null && readDialog.isVisible()) {
                readDialog.onTagScanned(medicationId);
                return;
            }

            // T093: Delegate medication lookup + dose confirmation to ViewModel
            doseConfirmationViewModel.processScan(medicationId);

        } catch (Exception e) {
            // T095: Tag read failure
            Log.e(TAG, "NFC read failed", e);
            Toast.makeText(this, R.string.nfc_scan_retry, Toast.LENGTH_LONG).show();
            if (audioFeedbackService != null && audioFeedbackService.isReady()) {
                audioFeedbackService.speakError(getString(R.string.nfc_scan_error), null);
            }
        }
    }

    // ─── UI Feedback ────────────────────────────────────────────────────

    /**
     * T107: Show success dialog after successful NFC scan.
     */
    private void showScanSuccessDialog(DoseConfirmationViewModel.DoseConfirmationResult result) {
        // T254: Haptic feedback on successful scan
        triggerHapticFeedback();

        NFCScanSuccessDialogFragment dialog = NFCScanSuccessDialogFragment.newInstance(
                result.medicationNickname,
                result.dose,
                result.minutesUntilNextDose,
                result.remainingDoses,
                result.isRefillWarning(),
                result.isRefillCritical(),
                result.earlyDose,
                result.minutesEarly
        );
        dialog.show(getSupportFragmentManager(), "nfc_success");
    }

    /**
     * T102: Trigger audio confirmation after successful scan.
     */
    private void triggerAudioConfirmation(DoseConfirmationViewModel.DoseConfirmationResult result) {
        if (audioFeedbackService == null || !audioFeedbackService.isReady()) {
            Log.w(TAG, "AudioFeedbackService not ready, skipping audio confirmation");
            return;
        }

        // Main confirmation speech
        audioFeedbackService.speakMedicationConfirmation(
                result.medicationNickname,
                result.dose,
                result.minutesUntilNextDose,
                null
        );

        // Refill reminder speech if needed
        if (result.isRefillWarning() || result.isRefillCritical()) {
            audioFeedbackService.speakRefillReminder(
                    result.medicationNickname,
                    result.remainingDoses,
                    null
            );
        }
    }

    // ─── Bottom Navigation ──────────────────────────────────────────────

    private void setupBottomNavigation() {
        // Settings button
        findViewById(R.id.button_settings).setOnClickListener(v -> {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        });

        // Read Tag button — T157: launch NFCReadDialogFragment
        findViewById(R.id.button_read_tag).setOnClickListener(v -> {
            if (nfcAdapter == null) {
                Toast.makeText(this, R.string.nfc_not_available, Toast.LENGTH_SHORT).show();
            } else if (!nfcAdapter.isEnabled()) {
                Toast.makeText(this, R.string.nfc_disabled, Toast.LENGTH_SHORT).show();
            } else {
                NFCReadDialogFragment dialog = NFCReadDialogFragment.newInstance();
                dialog.show(getSupportFragmentManager(), "nfc_read");
            }
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
     * Handle "Info" button click — navigate to MedicationInfoActivity.
     */
    private void onInfoClicked(Medication medication) {
        Intent intent = new Intent(this, MedicationInfoActivity.class);
        intent.putExtra(MedicationInfoActivity.EXTRA_MEDICATION_ID, medication.id);
        startActivity(intent);
    }

    /**
     * Handle "Write Tag" button click.
     */
    private void onWriteTagClicked(Medication medication) {
        Toast.makeText(
            this,
            "Write NFC tag: " + medication.nickname,
            Toast.LENGTH_SHORT
        ).show();
        // NFCWriteDialogFragment will be launched in later implementation
    }

    // ─── T248: Emergency Permissions ───────────────────────────────────

    /**
     * T254: Trigger haptic feedback on NFC scan success.
     */
    @SuppressWarnings("deprecation")
    private void triggerHapticFeedback() {
        try {
            Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (vibrator == null || !vibrator.hasVibrator()) return;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Double-pulse pattern: short buzz, pause, short buzz
                vibrator.vibrate(VibrationEffect.createWaveform(
                        new long[]{0, 100, 80, 100}, -1));
            } else {
                vibrator.vibrate(new long[]{0, 100, 80, 100}, -1);
            }
        } catch (Exception e) {
            Log.w(TAG, "Haptic feedback failed", e);
        }
    }

    /**
     * T248: Request runtime permissions for emergency features (SMS, CALL, LOCATION).
     * Only requests permissions that haven't been granted yet.
     */
    private void requestEmergencyPermissions() {
        java.util.List<String> needed = new java.util.ArrayList<>();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            needed.add(Manifest.permission.SEND_SMS);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
                != PackageManager.PERMISSION_GRANTED) {
            needed.add(Manifest.permission.CALL_PHONE);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            needed.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                needed.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        if (!needed.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    needed.toArray(new String[0]),
                    REQUEST_EMERGENCY_PERMISSIONS);
        }
    }

    /**
     * T249: Handle permission request results — log denied permissions and show messages.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_EMERGENCY_PERMISSIONS) {
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    String perm = permissions[i];
                    if (Manifest.permission.SEND_SMS.equals(perm)) {
                        Log.w(TAG, "SEND_SMS permission denied");
                        Toast.makeText(this, R.string.permission_denied_sms, Toast.LENGTH_SHORT).show();
                    } else if (Manifest.permission.CALL_PHONE.equals(perm)) {
                        Log.w(TAG, "CALL_PHONE permission denied");
                        Toast.makeText(this, R.string.permission_denied_call, Toast.LENGTH_SHORT).show();
                    } else if (Manifest.permission.ACCESS_FINE_LOCATION.equals(perm)) {
                        Log.w(TAG, "ACCESS_FINE_LOCATION permission denied");
                        Toast.makeText(this, R.string.permission_denied_location, Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }

    // ─── T233: Missed Dose Detection Worker ─────────────────────────────

    /**
     * Schedule periodic missed dose detection using WorkManager.
     * Runs every 15 minutes to check for overdue medications.
     */
    private void scheduleMissedDoseWorker() {
        PeriodicWorkRequest missedDoseWork = new PeriodicWorkRequest.Builder(
                MissedDoseWorker.class, 15, TimeUnit.MINUTES)
                .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "missed_dose_detection",
                ExistingPeriodicWorkPolicy.KEEP,
                missedDoseWork);
    }
}
