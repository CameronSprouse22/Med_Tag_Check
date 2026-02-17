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
        textStatus.setText("Writing to tag...");
        progressBar.setIndeterminate(true);
        
        // Write medication ID to tag in background thread
        new Thread(() -> {
            try {
                // Write medication ID
                boolean success = nfcHandler.writeMedicationId(tag, medicationId);
                
                if (success) {
                    // Verify write
                    String readId = nfcHandler.readMedicationId(tag);
                    
                    if (medicationId.equals(readId)) {
                        // Save tag to database
                        String tagId = bytesToHex(tag.getId());
                        NFCTag nfcTag = new NFCTag(
                            UUID.randomUUID().toString(),
                            tagId,
                            medicationId,
                            null // No label for now
                        );
                        
                        tagRepository.insertNFCTag(nfcTag, new MedicationRepository.OnResultCallback<Long>() {
                            @Override
                            public void onSuccess(Long result) {
                                // Success - tag saved
                                android.app.Activity activity = getActivity();
                                if (activity != null && isAdded()) {
                                    activity.runOnUiThread(() -> {
                                        textStatus.setText("Success! Tag written.");
                                        progressBar.setIndeterminate(false);
                                        Toast.makeText(activity, "NFC tag written successfully", Toast.LENGTH_SHORT).show();
                                        
                                        if (writeCompleteListener != null) {
                                            writeCompleteListener.onWriteSuccess();
                                        }
                                        
                                        dismiss();
                                    });
                                }
                            }
                            
                            @Override
                            public void onError(Exception error) {
                                // Error saving tag
                                android.app.Activity activity = getActivity();
                                if (activity != null && isAdded()) {
                                    activity.runOnUiThread(() -> {
                                        textStatus.setText("Tag written but failed to save: " + error.getMessage());
                                        progressBar.setIndeterminate(false);
                                        Toast.makeText(activity, "Error saving tag: " + error.getMessage(), Toast.LENGTH_LONG).show();
                                    });
                                }
                            }
                        });
                    } else {
                        // Verification failed
                        android.app.Activity activity = getActivity();
                        if (activity != null && isAdded()) {
                            activity.runOnUiThread(() -> {
                                textStatus.setText("Write failed. Please try again.");
                                progressBar.setIndeterminate(false);
                                Toast.makeText(activity, "Tag verification failed", Toast.LENGTH_SHORT).show();
                            });
                        }
                    }
                } else {
                    // Write failed
                    android.app.Activity activity = getActivity();
                    if (activity != null && isAdded()) {
                        activity.runOnUiThread(() -> {
                            textStatus.setText("Write failed. Please try again.");
                            progressBar.setIndeterminate(false);
                            Toast.makeText(activity, "Failed to write tag", Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            } catch (Exception e) {
                android.app.Activity activity = getActivity();
                if (activity != null && isAdded()) {
                    activity.runOnUiThread(() -> {
                        textStatus.setText("Error: " + e.getMessage());
                        progressBar.setIndeterminate(false);
                        Toast.makeText(activity, "Error writing tag: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }
            }
        }).start();
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
