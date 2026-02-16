# NFC Operations Contract

**Purpose**: Define interface for NFC tag reading and writing operations

**Package**: `com.medchecktag.nfc`

---

## INFCHandler

**Responsibility**: Abstract NFC hardware interactions for reading and writing NDEF tags

### Methods

#### Reading Operations

```java
/**
 * Read medication ID from NFC tag
 * 
 * @param tag NFC Tag object from Android NFC API
 * @return Medication UUID string (without "med:" prefix), or null if read fails
 * @throws NFCReadException if tag is not NDEF format or data is corrupted
 */
String readMedicationId(Tag tag) throws NFCReadException;

/**
 * Check if device has NFC hardware
 * 
 * @param context Android Context
 * @return true if NFC hardware available, false otherwise
 */
boolean isNFCAvailable(Context context);

/**
 * Check if NFC is currently enabled on device
 * 
 * @param context Android Context
 * @return true if NFC enabled, false otherwise
 */
boolean isNFCEnabled(Context context);
```

**Read Behavior**:
- Reads NDEF text record from tag
- Expects format: "med:<UUID>" (e.g., "med:a3b2c1d4-e5f6-7890-1234-567890abcdef")
- Strips "med:" prefix and returns UUID only
- Validates UUID format (regex: `[a-f0-9\\-]{36}`)
- Returns null if tag is blank/empty
- Throws exception if tag is not NDEF or data is corrupted

---

#### Writing Operations

```java
/**
 * Write medication ID to NFC tag in NDEF format
 * 
 * @param tag NFC Tag object from Android NFC API
 * @param medicationId UUID string (will be prefixed with "med:")
 * @return true if write successful, false if write failed
 * @throws NFCWriteException if tag is read-only or write operation fails
 * @throws IllegalArgumentException if medicationId is not valid UUID format
 */
boolean writeMedicationId(Tag tag, String medicationId) throws NFCWriteException;

/**
 * Check if tag is writable
 * 
 * @param tag NFC Tag object
 * @return true if tag supports NDEF write, false if read-only
 */
boolean isTagWritable(Tag tag);

/**
 * Erase tag (write empty NDEF record)
 * 
 * @param tag NFC Tag object
 * @return true if erase successful, false otherwise
 * @throws NFCWriteException if tag is read-only
 */
boolean eraseTag(Tag tag) throws NFCWriteException;
```

**Write Behavior**:
- Validates `medicationId` is valid UUID format before writing
- Prefixes UUID with "med:" (e.g., "med:a3b2c1d4...")
- Writes NDEF text record with UTF-8 encoding
- Verifies write by reading back and comparing (integrity check)
- Returns false if verification fails
- Throws exception if tag is read-only or not NDEF-compatible

---

#### Retry Logic

```java
/**
 * Read medication ID from NFC tag with retry logic
 * 
 * @param tag NFC Tag object
 * @param maxRetries Maximum retry attempts (recommended: 3)
 * @param retryDelayMs Delay between retries in milliseconds (recommended: 100-500ms)
 * @return Medication UUID string, or null if all retries fail
 */
String readWithRetry(Tag tag, int maxRetries, int retryDelayMs);

/**
 * Write medication ID to NFC tag with retry logic
 * 
 * @param tag NFC Tag object
 * @param medicationId UUID string
 * @param maxRetries Maximum retry attempts (recommended: 3)
 * @param retryDelayMs Delay between retries in milliseconds (recommended: 100-500ms)
 * @return true if any retry succeeds, false if all retries fail
 */
boolean writeWithRetry(Tag tag, String medicationId, int maxRetries, int retryDelayMs);
```

**Retry Behavior**:
- Implements exponential backoff: delay = baseDelay × 2^(attempt-1)
- Example: baseDelay=100ms → 100ms, 200ms, 400ms for 3 retries
- Logs each retry attempt for debugging
- Stops immediately on success (doesn't retry unnecessarily)
- Returns failure after all retries exhausted

---

## Exception Definitions

### NFCReadException

```java
public class NFCReadException extends Exception {
    public enum ErrorType {
        TAG_NOT_NDEF,           // Tag is not NDEF format
        NO_NDEF_MESSAGE,        // Tag is NDEF but contains no message
        INVALID_DATA_FORMAT,    // Data doesn't match "med:<UUID>" format
        CORRUPTED_DATA,         // Data exists but is corrupted/incomplete
        READ_TIMEOUT,           // Read operation timed out
        IO_ERROR                // Low-level I/O error
    }
    
    private ErrorType errorType;
    
    public NFCReadException(ErrorType errorType, String message) {
        super(message);
        this.errorType = errorType;
    }
    
    public ErrorType getErrorType() {
        return errorType;
    }
}
```

### NFCWriteException

```java
public class NFCWriteException extends Exception {
    public enum ErrorType {
        TAG_READ_ONLY,          // Tag is read-only, cannot write
        TAG_NOT_NDEF,           // Tag doesn't support NDEF format
        INSUFFICIENT_CAPACITY,  // Tag too small to hold data
        WRITE_FAILED,           // Write operation failed
        VERIFICATION_FAILED,    // Write succeeded but read-back verification failed
        IO_ERROR                // Low-level I/O error
    }
    
    private ErrorType errorType;
    
    public NFCWriteException(ErrorType errorType, String message) {
        super(message);
        this.errorType = errorType;
    }
    
    public ErrorType getErrorType() {
        return errorType;
    }
}
```

---

## NDEF Format Specification

### Tag Structure

```
NDEF Message
└── NDEF Record
    ├── TNF (Type Name Format): TNF_WELL_KNOWN
    ├── Type: RTD_TEXT ("T")
    ├── Payload:
    │   ├── Language Code: "en" (2 bytes)
    │   └── Text: "med:<UUID>" (40 bytes)
    └── ID: Empty
```

### Example Hex Dump

```
NDEF Message: D1 01 28 54 02 65 6E 6D 65 64 3A 61 33 62 32 63 31 64 34 2D 65 35 66 36 2D 37 38 39 30 2D 31 32 33 34 2D 35 36 37 38 39 30 61 62 63 64 65 66

Breakdown:
D1          - TNF_WELL_KNOWN | MB | ME | SR
01          - Type Length (1 byte)
28          - Payload Length (40 bytes)
54          - Type: "T" (Text)
02 65 6E    - Language: "en"
6D 65 64 3A - "med:"
61 33 ...   - UUID bytes
```

### Validation

```java
// Payload format validation regex
private static final Pattern PAYLOAD_PATTERN = Pattern.compile(
    "^med:[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}$"
);

// UUID format validation (after stripping "med:" prefix)
private static final Pattern UUID_PATTERN = Pattern.compile(
    "^[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}$"
);
```

---

## Usage Examples

### Reading NFC Tag (from Activity/Fragment)

```java
// In Activity/Fragment with NFC intent filter
@Override
protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    
    if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction()) ||
        NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
        
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        
        // Execute on background thread
        executor.execute(() -> {
            try {
                String medicationId = nfcHandler.readWithRetry(tag, 3, 100);
                if (medicationId != null) {
                    // Success: lookup medication and proceed
                    runOnUiThread(() -> onMedicationScanned(medicationId));
                } else {
                    // Failure: show error
                    runOnUiThread(() -> showError("Failed to read tag"));
                }
            } catch (NFCReadException e) {
                runOnUiThread(() -> handleNFCReadError(e));
            }
        });
    }
}
```

### Writing NFC Tag

```java
// In write mode (foreground dispatch active)
public void writeToTag(Tag tag, String medicationId) {
    executor.execute(() -> {
        try {
            boolean success = nfcHandler.writeWithRetry(tag, medicationId, 3, 100);
            runOnUiThread(() -> {
                if (success) {
                    showSuccess("Tag written successfully");
                } else {
                    showError("Failed to write to tag");
                }
            });
        } catch (NFCWriteException e) {
            runOnUiThread(() -> handleNFCWriteError(e));
        }
    });
}
```

---

## Testing Strategy

### Unit Tests (Mockito)

```java
// Mock Tag object
Tag mockTag = mock(Tag.class);
when(mockTag.getTechList()).thenReturn(new String[]{Ndef.class.getName()});

// Mock NDEF operations
Ndef mockNdef = mock(Ndef.class);
when(mockNdef.isWritable()).thenReturn(true);

// Test read
String result = nfcHandler.readMedicationId(mockTag);
assertEquals("expected-uuid", result);
```

### Instrumented Tests (Real NFC)

```java
// Use Android virtual device with NFC emulation
// Or physical device with test NFC tags

@Test
public void testWriteAndReadRoundTrip() {
    String testId = UUID.randomUUID().toString();
    
    // Write to tag (physical tag required)
    boolean writeSuccess = nfcHandler.writeMedicationId(testTag, testId);
    assertTrue(writeSuccess);
    
    // Read back
    String readId = nfcHandler.readMedicationId(testTag);
    assertEquals(testId, readId);
}
```

---

## Implementation Notes

**Technology**: Android NFC API (`android.nfc`, `android.nfc.tech`)

**Thread Safety**: 
- NFC operations are blocking I/O, must run on background thread
- Use `Executor` or `HandlerThread` for async operations
- Never call NFC methods on UI thread (will block for 100-500ms)

**Foreground Dispatch**:
- Use `NfcAdapter.enableForegroundDispatch()` during write mode
- Prevents other apps from intercepting NFC intents
- Disable when leaving activity or cancelling write mode

**Error Recovery**:
- Retry logic is essential (physical NFC is unreliable)
- Provide user feedback during retries ("Scanning... attempt 2 of 3")
- Suggest repositioning phone if all retries fail
