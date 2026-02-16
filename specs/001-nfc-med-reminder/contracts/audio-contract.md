# Audio Feedback Contract

**Purpose**: Define interface for text-to-speech audio confirmations

**Package**: `com.medchecktag.audio`

---

## IAudioFeedbackService

**Responsibility**: Provide spoken audio feedback for medication confirmations and notifications

### Methods

#### Speech Operations

```java
/**
 * Speak medication confirmation (name, dose, next time)
 * 
 * @param medicationNickname User-friendly medication name
 * @param dose Dose amount (e.g., "2 tablets", "5 milliliters")
 * @param nextDoseInMinutes Minutes until next dose
 * @param callback Callback when speech completes (optional, can be null)
 */
void speakMedicationConfirmation(
    String medicationNickname,
    String dose,
    int nextDoseInMinutes,
    @Nullable TTSCallback callback
);

/**
 * Speak error message
 * 
 * @param errorMessage Error text to speak (e.g., "Tag not recognized")
 * @param callback Callback when speech completes (optional, can be null)
 */
void speakError(String errorMessage, @Nullable TTSCallback callback);

/**
 * Speak custom message
 * 
 * @param message Text to speak
 * @param callback Callback when speech completes (optional, can be null)
 */
void speak(String message, @Nullable TTSCallback callback);

/**
 * Speak refill reminder
 * 
 * @param medicationNickname Medication name
 * @param remainingDoses Number of doses left
 * @param callback Callback when speech completes (optional, can be null)
 */
void speakRefillReminder(String medicationNickname, int remainingDoses, @Nullable TTSCallback callback);
```

**Speech Behavior**:
- Uses Android TextToSpeech API
- Respects user's system TTS settings (voice, speed, pitch)
- Applies app volume setting on top of system volume
- Queues multiple speech requests (doesn't interrupt ongoing speech)
- Automatically handles number formatting (converts "120" to "one hundred twenty")

---

#### Lifecycle Operations

```java
/**
 * Initialize TTS engine
 * 
 * @param context Android Context
 * @param onInitCallback Callback when TTS initialization completes
 */
void initialize(Context context, TTSInitCallback onInitCallback);

/**
 * Check if TTS is initialized and ready
 * 
 * @return true if TTS ready to speak, false if still initializing
 */
boolean isReady();

/**
 * Stop current speech immediately
 */
void stop();

/**
 * Shutdown TTS engine (call in onDestroy)
 */
void shutdown();
```

**Lifecycle Management**:
- Initialize in Application onCreate or first Activity onCreate
- TTS initialization is asynchronous (takes 100-500ms)
- Queue speech requests if TTS not yet ready (speak when initialized)
- Always call shutdown() when app is destroyed to release resources

---

#### Configuration Operations

```java
/**
 * Set speech rate
 * 
 * @param rate Speech rate (0.5 = slow, 1.0 = normal, 2.0 = fast)
 */
void setSpeechRate(float rate);

/**
 * Set speech pitch
 * 
 * @param pitch Voice pitch (0.5 = low, 1.0 = normal, 2.0 = high)
 */
void setPitch(float pitch);

/**
 * Set volume for TTS audio
 * 
 * @param volume Volume level (0.0 = silent, 1.0 = max)
 */
void setVolume(float volume);

/**
 * Get current configuration
 * 
 * @return TTSConfiguration object with current settings
 */
TTSConfiguration getConfiguration();
```

**Configuration Behavior**:
- Settings persist in AppSettings database
- Changes take effect immediately for next speech
- Volume is relative to system media volume
- Rate and pitch use Android TTS defaults as baseline

---

## Callback Interfaces

### TTSCallback

```java
public interface TTSCallback {
    /**
     * Called when speech starts
     * 
     * @param utteranceId Unique ID for this speech request
     */
    void onStart(String utteranceId);
    
    /**
     * Called when speech completes successfully
     * 
     * @param utteranceId Unique ID for this speech request
     */
    void onDone(String utteranceId);
    
    /**
     * Called when speech fails
     * 
     * @param utteranceId Unique ID for this speech request
     * @param errorCode Error code from TTS engine
     */
    void onError(String utteranceId, int errorCode);
}
```

### TTSInitCallback

```java
public interface TTSInitCallback {
    /**
     * Called when TTS engine initializes successfully
     */
    void onSuccess();
    
    /**
     * Called when TTS initialization fails
     * 
     * @param errorMessage Description of initialization failure
     */
    void onFailure(String errorMessage);
}
```

---

## Speech Templates

### Medication Confirmation Template

```
Format: "[Medication], [Dose]. Next dose in [Time]."

Examples:
- "Blood pressure pill, two tablets. Next dose in eight hours."
- "Insulin injection, five units. Next dose in twelve hours."
- "Vitamin D, one capsule. Next dose in twenty four hours."

Time Formatting:
- < 60 minutes: "[X] minutes"
- 60-119 minutes: "one hour"
- >= 120 minutes and divisible by 60: "[X] hours"
- Mixed: "[X] hours and [Y] minutes"
```

### Error Templates

```
Tag not recognized: "Tag not recognized. Please try again or check the medication list."
NFC disabled: "N F C is disabled. Please enable N F C in phone settings."
Too early: "Not time for [Medication] yet. Next dose in [Time]."
Read failure: "Failed to read tag. Please hold phone closer to the tag."
Tag already assigned: "This tag is already assigned to [Other Medication]."
```

### Refill Templates

```
First threshold: "[Medication] is running low. [X] doses remaining."
Second threshold: "Time to refill [Medication]. Only [X] doses left."
Out of doses: "[Medication] is out of doses. Please refill immediately."
```

---

## Text Preprocessing

### Number Formatting

```java
// Convert numeric dose amounts to speech-friendly format
"2 tablets" → "two tablets"
"5 mL" → "five milliliters"  
"1.5 mg" → "one point five milligrams"
"10 units" → "ten units"
```

### Abbreviation Expansion

```java
// Expand common medication abbreviations for clarity
"mL" → "milliliters"
"mg" → "milligrams"
"mcg" → "micrograms"
"IU" → "international units"
"NFC" → "N F C" (spell out to avoid mispronunciation)
```

### Time Formatting

```java
// Convert minutes to human-friendly duration
30 → "thirty minutes"
60 → "one hour"
90 → "one hour and thirty minutes"
120 → "two hours"
1440 → "twenty four hours"
```

---

## Audio Focus Management

**Requirement**: Request audio focus before speaking to respect other audio apps

```java
// Request audio focus before TTS
AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
int result = audioManager.requestAudioFocus(
    focusChangeListener,
    AudioManager.STREAM_MUSIC,
    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
);

if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
    // Proceed with TTS
    textToSpeech.speak(message, TextToSpeech.QUEUE_ADD, null, utteranceId);
}

// Release audio focus after TTS completes
audioManager.abandonAudioFocus(focusChangeListener);
```

**Focus Behavior**:
- `AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK`: Request temporary focus, other apps can duck volume
- Respect interruptions (phone call, navigation directions) by pausing TTS
- Resume TTS after interruption ends if message is safety-critical

---

## Accessibility Considerations

### For Users with Hearing Impairment

```
- Provide visual feedback alongside audio (toast messages, notifications)
- Use vibration patterns to supplement audio (optional setting)
- Display spoken text on screen in real-time (optional accessibility mode)
```

### For Users with Vision Impairment

```
- TTS is primary interface for medication confirmation (critical feature)
- Speak all UI interactions (button presses, screen changes) in accessibility mode
- Integration with Android TalkBack for full screen reading
```

### For Non-Native Speakers

```
- TTS respects system language setting
- Medication nicknames spoken in user's configured language
- Unit abbreviations expanded to full words for clarity
```

---

## Error Handling

### TTS Not Available

```java
if (!isReady()) {
    // Fallback to visual confirmation only
    showToast("TTS not available. Please check System settings.");
    return;
}
```

### TTS Initialization Failure

```java
onInitCallback.onFailure("TTS engine not installed or unavailable");
// Prompt user to install TTS engine from Google Play
Intent installIntent = new Intent();
installIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
startActivity(installIntent);
```

### Speech Interruption

```java
// If audio focus lost during speech
@Override
public void onAudioFocusChange(int focusChange) {
    if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
        textToSpeech.stop(); // Pause current speech
        // Resume when focus regained (if safety-critical message)
    }
}
```

---

## Testing Strategy

### Unit Tests

```java
// Mock TextToSpeech
TextToSpeech mockTTS = mock(TextToSpeech.class);

// Test speech call
audioService.speak("Test message", null);
verify(mockTTS).speak(
    eq("Test message"),
    eq(TextToSpeech.QUEUE_ADD),
    isNull(),
    any(String.class)
);
```

### Instrumented Tests

```java
// Real TTS engine on device
@Test
public void testMedicationConfirmation() {
    CountDownLatch latch = new CountDownLatch(1);
    
    audioService.speakMedicationConfirmation(
        "Test Med",
        "1 tablet",
        120,
        new TTSCallback() {
            @Override
            public void onDone(String id) {
                latch.countDown();
            }
        }
    );
    
    assertTrue(latch.await(10, TimeUnit.SECONDS));
}
```

### Manual Testing

```
1. Test various medication names (short, long, with numbers)
2. Test dose amounts (decimals, units, fractions)
3. Test time durations (minutes, hours, mixed)
4. Test error messages (clear pronunciation)
5. Test speech rate settings (slow to fast)
6. Test volume levels (including mute)
7. Test interruptions (incoming call during speech)
8. test non-English languages (if supported)
```

---

## Implementation Notes

**TTS Engine**: Use Android's built-in TTS (com.android.tts)
- Available on all Android 5.0+ devices
- Supports 40+ languages
- Google Text-to-Speech engine is default on most devices

**Performance**:
- TTS latency: ~200-500ms from speak() call to first audio
- Speech duration: ~2-3 seconds for typical medication confirmation
- Total feedback time: <3 seconds (meets success criteria)

**Battery Impact**: Minimal (TTS is CPU-based, not network-based)

**Privacy**: All TTS processing is on-device (no cloud TTS, no network required)
