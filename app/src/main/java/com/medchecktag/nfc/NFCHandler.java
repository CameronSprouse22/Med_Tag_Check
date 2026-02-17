package com.medchecktag.nfc;

import android.content.Context;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.util.Log;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * NFCHandler implementing INFCHandler contract.
 * Handles NFC tag reading and writing operations for medication identification.
 */
public class NFCHandler {
    
    private static final String TAG = "NFCHandler";
    private static final String MED_PREFIX = "med:";
    private static final Pattern UUID_PATTERN = Pattern.compile("[a-f0-9\\-]{36}");
    
    // Reading Operations
    
    /**
     * Read medication ID from NFC tag
     */
    public String readMedicationId(Tag tag) throws NFCReadException {
        if (tag == null) {
            throw new NFCReadException(NFCReadException.ErrorType.IO_ERROR, "Tag is null");
        }
        
        Ndef ndef = Ndef.get(tag);
        if (ndef == null) {
            throw new NFCReadException(NFCReadException.ErrorType.TAG_NOT_NDEF, "Tag is not NDEF format");
        }
        
        try {
            ndef.connect();
            NdefMessage ndefMessage = ndef.getNdefMessage();
            
            if (ndefMessage == null) {
                throw new NFCReadException(NFCReadException.ErrorType.NO_NDEF_MESSAGE, "Tag contains no NDEF message");
            }
            
            NdefRecord[] records = ndefMessage.getRecords();
            if (records.length == 0) {
                return null; // Empty tag
            }
            
            // Read first text record - NFC Forum Text Record has header bytes:
            // byte 0: status byte (bit 7 = UTF encoding, bits 5-0 = language code length)
            // bytes 1..n: language code (e.g. "en")
            // remaining bytes: actual text payload
            NdefRecord record = records[0];
            byte[] rawPayload = record.getPayload();
            if (rawPayload == null || rawPayload.length == 0) {
                throw new NFCReadException(NFCReadException.ErrorType.INVALID_DATA_FORMAT,
                    "Empty payload in NFC record");
            }
            int languageCodeLength = rawPayload[0] & 0x3F;
            int textOffset = 1 + languageCodeLength;
            if (textOffset >= rawPayload.length) {
                throw new NFCReadException(NFCReadException.ErrorType.INVALID_DATA_FORMAT,
                    "Payload too short to contain text data");
            }
            String payload = new String(rawPayload, textOffset, rawPayload.length - textOffset, StandardCharsets.UTF_8);
            
            // Validate format: "med:<UUID>"
            if (!payload.startsWith(MED_PREFIX)) {
                throw new NFCReadException(NFCReadException.ErrorType.INVALID_DATA_FORMAT, 
                    "Data doesn't start with 'med:' prefix");
            }
            
            String medicationId = payload.substring(MED_PREFIX.length());
            
            // Validate UUID format
            if (!UUID_PATTERN.matcher(medicationId).matches()) {
                throw new NFCReadException(NFCReadException.ErrorType.INVALID_DATA_FORMAT, 
                    "Medication ID is not valid UUID format");
            }
            
            return medicationId;
            
        } catch (IOException | android.nfc.FormatException e) {
            throw new NFCReadException(NFCReadException.ErrorType.IO_ERROR, 
                "Failed to read from tag: " + e.getMessage());
        } finally {
            try {
                ndef.close();
            } catch (IOException ignored) {
            }
        }
    }
    
    /**
     * Read with retry logic
     */
    public String readWithRetry(Tag tag, int maxRetries, int retryDelayMs) {
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                String result = readMedicationId(tag);
                Log.d(TAG, "Read succeeded on attempt " + attempt);
                return result;
            } catch (NFCReadException e) {
                Log.w(TAG, "Read attempt " + attempt + " failed: " + e.getMessage());
                if (attempt < maxRetries) {
                    try {
                        Thread.sleep(retryDelayMs * (long) Math.pow(2, attempt - 1));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return null;
                    }
                }
            }
        }
        return null;
    }
    
    // Writing Operations
    
    /**
     * Write medication ID to NFC tag
     */
    public boolean writeMedicationId(Tag tag, String medicationId) throws NFCWriteException {
        if (!UUID_PATTERN.matcher(medicationId).matches()) {
            throw new IllegalArgumentException("Medication ID must be valid UUID format");
        }
        
        if (tag == null) {
            throw new NFCWriteException(NFCWriteException.ErrorType.IO_ERROR, "Tag is null");
        }
        
        Ndef ndef = Ndef.get(tag);
        if (ndef == null) {
            throw new NFCWriteException(NFCWriteException.ErrorType.TAG_NOT_NDEF, "Tag is not NDEF format");
        }
        
        if (!ndef.isWritable()) {
            throw new NFCWriteException(NFCWriteException.ErrorType.TAG_READ_ONLY, "Tag is read-only");
        }
        
        try {
            ndef.connect();
            
            // Create NDEF message with "med:<UUID>" payload
            String payload = MED_PREFIX + medicationId;
            NdefRecord textRecord = NdefRecord.createTextRecord("en", payload);
            NdefMessage message = new NdefMessage(textRecord);
            
            // Write message
            ndef.writeNdefMessage(message);
            
            // Verify write by reading back
            try {
                String readBack = readMedicationId(tag);
                return medicationId.equals(readBack);
            } catch (NFCReadException e) {
                Log.e(TAG, "Write verification failed: " + e.getMessage());
                return false;
            }
            
        } catch (IOException | android.nfc.FormatException e) {
            throw new NFCWriteException(NFCWriteException.ErrorType.IO_ERROR, 
                "Failed to write to tag: " + e.getMessage());
        } finally {
            try {
                ndef.close();
            } catch (IOException ignored) {
            }
        }
    }
    
    /**
     * Write with retry logic
     */
    public boolean writeWithRetry(Tag tag, String medicationId, int maxRetries, int retryDelayMs) {
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                boolean result = writeMedicationId(tag, medicationId);
                if (result) {
                    Log.d(TAG, "Write succeeded on attempt " + attempt);
                    return true;
                }
            } catch (NFCWriteException e) {
                Log.w(TAG, "Write attempt " + attempt + " failed: " + e.getMessage());
            }
            
            if (attempt < maxRetries) {
                try {
                    Thread.sleep(retryDelayMs * (long) Math.pow(2, attempt - 1));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }
        return false;
    }
    
    /**
     * Check if tag is writable
     */
    public boolean isTagWritable(Tag tag) {
        if (tag == null) return false;
        Ndef ndef = Ndef.get(tag);
        return ndef != null && ndef.isWritable();
    }
    
    /**
     * Erase tag (write empty NDEF record)
     */
    public boolean eraseTag(Tag tag) throws NFCWriteException {
        if (tag == null) {
            throw new NFCWriteException(NFCWriteException.ErrorType.IO_ERROR, "Tag is null");
        }
        
        Ndef ndef = Ndef.get(tag);
        if (ndef == null) {
            throw new NFCWriteException(NFCWriteException.ErrorType.TAG_NOT_NDEF, "Tag is not NDEF format");
        }
        
        if (!ndef.isWritable()) {
            throw new NFCWriteException(NFCWriteException.ErrorType.TAG_READ_ONLY, "Tag is read-only");
        }
        
        try {
            ndef.connect();
            // Write an empty text record (NdefMessage requires at least 1 record)
            NdefRecord emptyRecord = NdefRecord.createTextRecord("en", "");
            NdefMessage emptyMessage = new NdefMessage(emptyRecord);
            ndef.writeNdefMessage(emptyMessage);
            return true;
        } catch (IOException | android.nfc.FormatException e) {
            throw new NFCWriteException(NFCWriteException.ErrorType.IO_ERROR, 
                "Failed to erase tag: " + e.getMessage());
        } finally {
            try {
                ndef.close();
            } catch (IOException ignored) {
            }
        }
    }
    
    // Device Capabilities
    
    /**
     * Check if device has NFC hardware
     */
    public boolean isNFCAvailable(Context context) {
        NfcAdapter adapter = NfcAdapter.getDefaultAdapter(context);
        return adapter != null;
    }
    
    /**
     * Check if NFC is currently enabled
     */
    public boolean isNFCEnabled(Context context) {
        NfcAdapter adapter = NfcAdapter.getDefaultAdapter(context);
        return adapter != null && adapter.isEnabled();
    }
    
    // Exception Classes
    
    public static class NFCReadException extends Exception {
        public enum ErrorType {
            TAG_NOT_NDEF,
            NO_NDEF_MESSAGE,
            INVALID_DATA_FORMAT,
            CORRUPTED_DATA,
            READ_TIMEOUT,
            IO_ERROR
        }
        
        private final ErrorType errorType;
        
        public NFCReadException(ErrorType errorType, String message) {
            super(message);
            this.errorType = errorType;
        }
        
        public ErrorType getErrorType() {
            return errorType;
        }
    }
    
    public static class NFCWriteException extends Exception {
        public enum ErrorType {
            TAG_NOT_NDEF,
            TAG_READ_ONLY,
            INSUFFICIENT_SPACE,
            WRITE_TIMEOUT,
            IO_ERROR
        }
        
        private final ErrorType errorType;
        
        public NFCWriteException(ErrorType errorType, String message) {
            super(message);
            this.errorType = errorType;
        }
        
        public ErrorType getErrorType() {
            return errorType;
        }
    }
}
