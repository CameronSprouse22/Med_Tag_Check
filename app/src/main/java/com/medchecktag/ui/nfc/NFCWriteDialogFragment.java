package com.medchecktag.ui.nfc;

import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.medchecktag.R;
import com.medchecktag.models.NFCTag;
import com.medchecktag.nfc.NFCHandler;
import com.medchecktag.repositories.MedicationRepository;
import com.medchecktag.repositories.NFCTagRepository;

import java.util.UUID;

/**
 * Dialog fragment for writing medication ID to NFC tag.
 * Task T057 - User Story 2: Add Medication
 */
public class NFCWriteDialogFragment extends DialogFragment {
    
    private static final String ARG_MEDICATION_ID = "medication_id";
    
    private String medicationId;
    private NFCHandler nfcHandler;
    private NFCTagRepository tagRepository;
    private NfcAdapter nfcAdapter;
    
    private TextView textStatus;
    private ProgressBar progressBar;
    private Button buttonCancel;
    
    private OnWriteCompleteListener writeCompleteListener;
    
    public interface OnWriteCompleteListener {
        void onWriteSuccess();
        void onWriteCancelled();
    }
    
    public static NFCWriteDialogFragment newInstance(String medicationId) {
        NFCWriteDialogFragment fragment = new NFCWriteDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_MEDICATION_ID, medicationId);
        fragment.setArguments(args);
        return fragment;
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        
        if (getArguments() != null) {
            medicationId = getArguments().getString(ARG_MEDICATION_ID);
        }
        
        nfcHandler = new NFCHandler();
        tagRepository = new NFCTagRepository(requireContext());
        nfcAdapter = NfcAdapter.getDefaultAdapter(requireContext());
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.nfc_write_dialog, container, false);
        
        initializeViews(view);
        setupListeners();
        
        return view;
    }
    
    private void initializeViews(View view) {
        textStatus = view.findViewById(R.id.text_status);
        progressBar = view.findViewById(R.id.progress_bar);
        buttonCancel = view.findViewById(R.id.button_cancel);
    }
    
    private void setupListeners() {
        buttonCancel.setOnClickListener(v -> {
            if (writeCompleteListener != null) {
                writeCompleteListener.onWriteCancelled();
            }
            dismiss();
        });
    }
    
    @Override
    public void onResume() {
        super.onResume();
        enableForegroundDispatch();
    }
    
    @Override
    public void onPause() {
        super.onPause();
        disableForegroundDispatch();
    }
    
    private void enableForegroundDispatch() {
        if (nfcAdapter != null && getActivity() != null) {
            int flags = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                ? PendingIntent.FLAG_MUTABLE
                : 0;
            android.app.PendingIntent pendingIntent = android.app.PendingIntent.getActivity(
                getActivity(), 0,
                new Intent(getActivity(), getActivity().getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                flags
            );
            nfcAdapter.enableForegroundDispatch(getActivity(), pendingIntent, null, null);
        }
    }
    
    private void disableForegroundDispatch() {
        if (nfcAdapter != null && getActivity() != null) {
            nfcAdapter.disableForegroundDispatch(getActivity());
        }
    }
    
    /**
     * Handle new NFC intent
     */
    public void onNewIntent(Intent intent) {
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction()) ||
            NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
            
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            if (tag != null) {
                handleTagWrite(tag);
            }
        }
    }
    
    private void handleTagWrite(Tag tag) {
        textStatus.setText("Checking tag...");
        progressBar.setIndeterminate(true);
        
        // Write medication ID to tag in background thread
        new Thread(() -> {
            try {
                String tagHardwareId = bytesToHex(tag.getId());

                // T172: Detect tag already in use
                NFCTag existingTag = tagRepository.getTagByTagIdSync(tagHardwareId);
                if (existingTag != null && !existingTag.medicationId.equals(medicationId)) {
                    // T173: Warn user — tag belongs to another medication
                    android.app.Activity activity = getActivity();
                    if (activity != null && isAdded()) {
                        activity.runOnUiThread(() -> {
                            new androidx.appcompat.app.AlertDialog.Builder(activity)
                                .setTitle(R.string.nfc_overwrite_title)
                                .setMessage(getString(R.string.nfc_overwrite_message))
                                .setPositiveButton(R.string.action_confirm, (d, w) -> {
                                    // T174: User confirmed overwrite
                                    performWrite(tag, tagHardwareId);
                                })
                                .setNegativeButton(R.string.action_cancel, (d, w) -> {
                                    textStatus.setText(R.string.nfc_write_waiting);
                                    progressBar.setIndeterminate(false);
                                })
                                .show();
                        });
                    }
                    return;
                }

                // No conflict — write directly
                performWrite(tag, tagHardwareId);

            } catch (Exception e) {
                android.app.Activity activity = getActivity();
                if (activity != null && isAdded()) {
                    activity.runOnUiThread(() -> {
                        textStatus.setText("Error: " + e.getMessage());
                        progressBar.setIndeterminate(false);
                    });
                }
            }
        }).start();
    }

    /**
     * T174-T177: Perform the actual NFC write, verify, and save tag to DB.
     */
    private void performWrite(Tag tag, String tagHardwareId) {
        new Thread(() -> {
            try {
                textActivity(() -> textStatus.setText(R.string.nfc_write_writing));

                // Write medication ID
                boolean success = nfcHandler.writeMedicationId(tag, medicationId);
                
                if (success) {
                    // T176: Verify write
                    String readId = nfcHandler.readMedicationId(tag);
                    
                    if (medicationId.equals(readId)) {
                        // T177: Save tag to database (supports multiple tags per medication)
                        NFCTag nfcTag = new NFCTag(
                            UUID.randomUUID().toString(),
                            tagHardwareId,
                            medicationId,
                            null
                        );
                        
                        tagRepository.insertNFCTag(nfcTag, new MedicationRepository.OnResultCallback<Long>() {
                            @Override
                            public void onSuccess(Long result) {
                                textActivity(() -> {
                                    textStatus.setText(R.string.nfc_write_success_status);
                                    progressBar.setIndeterminate(false);
                                    Toast.makeText(getActivity(), R.string.nfc_tag_written_successfully, Toast.LENGTH_SHORT).show();
                                    if (writeCompleteListener != null) {
                                        writeCompleteListener.onWriteSuccess();
                                    }
                                    dismiss();
                                });
                            }
                            
                            @Override
                            public void onError(Exception error) {
                                textActivity(() -> {
                                    textStatus.setText(getString(R.string.nfc_tag_written_but_save_failed, error.getMessage()));
                                    progressBar.setIndeterminate(false);
                                });
                            }
                        });
                    } else {
                        // T176: Verification failed
                        textActivity(() -> {
                            textStatus.setText(R.string.nfc_write_failed);
                            progressBar.setIndeterminate(false);
                        });
                    }
                } else {
                    // T175: Write failed
                    textActivity(() -> {
                        textStatus.setText(R.string.nfc_write_failed);
                        progressBar.setIndeterminate(false);
                    });
                }
            } catch (Exception e) {
                textActivity(() -> {
                    textStatus.setText(getString(R.string.nfc_write_error, e.getMessage()));
                    progressBar.setIndeterminate(false);
                });
            }
        }).start();
    }

    /** Helper to run on UI thread safely. */
    private void textActivity(Runnable action) {
        android.app.Activity activity = getActivity();
        if (activity != null && isAdded()) {
            activity.runOnUiThread(action);
        }
    }
    
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
    
    public void setOnWriteCompleteListener(OnWriteCompleteListener listener) {
        this.writeCompleteListener = listener;
    }
}
