package com.medchecktag.audio;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * AudioFeedbackService implementing IAudioFeedbackService contract.
 * Provides text-to-speech audio feedback for medication confirmations.
 */
public class AudioFeedbackService {
    
    private static final String TAG = "AudioFeedbackService";
    private static final AtomicInteger utteranceIdCounter = new AtomicInteger(0);
    
    private TextToSpeech tts;
    private boolean isInitialized = false;
    private float volume = 0.8f;
    private float speechRate = 1.0f;
    private float pitch = 1.0f;
    
    private final HashMap<String, TTSCallback> callbackMap = new HashMap<>();
    
    /**
     * Initialize TTS engine
     */
    public void initialize(Context context, TTSInitCallback onInitCallback) {
        tts = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(Locale.getDefault());
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.w(TAG, "Language not supported, using default");
                }
                
                tts.setSpeechRate(speechRate);
                tts.setPitch(pitch);
                
                tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override
                    public void onStart(String utteranceId) {
                        Log.d(TAG, "Speech started: " + utteranceId);
                    }
                    
                    @Override
                    public void onDone(String utteranceId) {
                        TTSCallback callback = callbackMap.remove(utteranceId);
                        if (callback != null) {
                            callback.onComplete();
                        }
                    }
                    
                    @Override
                    public void onError(String utteranceId) {
                        TTSCallback callback = callbackMap.remove(utteranceId);
                        if (callback != null) {
                            callback.onError("TTS error");
                        }
                    }
                });
                
                isInitialized = true;
                Log.i(TAG, "TTS initialized successfully");
                
                if (onInitCallback != null) {
                    onInitCallback.onInitialized(true);
                }
            } else {
                Log.e(TAG, "TTS initialization failed");
                if (onInitCallback != null) {
                    onInitCallback.onInitialized(false);
                }
            }
        });
    }
    
    /**
     * Check if TTS is ready
     */
    public boolean isReady() {
        return isInitialized && tts != null;
    }
    
    /**
     * Speak medication confirmation
     */
    public void speakMedicationConfirmation(String medicationNickname, String dose, 
                                           int nextDoseInMinutes, @Nullable TTSCallback callback) {
        String message = String.format(
            "Confirmed %s, %s. Next dose in %d minutes.",
            medicationNickname,
            dose,
            nextDoseInMinutes
        );
        speak(message, callback);
    }
    
    /**
     * Speak error message
     */
    public void speakError(String errorMessage, @Nullable TTSCallback callback) {
        speak("Error: " + errorMessage, callback);
    }
    
    /**
     * Speak refill reminder
     */
    public void speakRefillReminder(String medicationNickname, int remainingDoses, 
                                   @Nullable TTSCallback callback) {
        String message = String.format(
            "Reminder: %s is running low. %d doses remaining. Please refill soon.",
            medicationNickname,
            remainingDoses
        );
        speak(message, callback);
    }
    
    /**
     * Speak custom message
     */
    public void speak(String message, @Nullable TTSCallback callback) {
        if (!isReady()) {
            Log.w(TAG, "TTS not ready, cannot speak: " + message);
            if (callback != null) {
                callback.onError("TTS not initialized");
            }
            return;
        }
        
        String utteranceId = "utterance_" + utteranceIdCounter.incrementAndGet();
        
        if (callback != null) {
            callbackMap.put(utteranceId, callback);
        }
        
        HashMap<String, String> params = new HashMap<>();
        params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId);
        params.put(TextToSpeech.Engine.KEY_PARAM_VOLUME, String.valueOf(volume));
        
        int result = tts.speak(message, TextToSpeech.QUEUE_ADD, params);
        
        if (result == TextToSpeech.ERROR) {
            Log.e(TAG, "Failed to queue speech: " + message);
            callbackMap.remove(utteranceId);
            if (callback != null) {
                callback.onError("Failed to queue speech");
            }
        }
    }
    
    /**
     * Stop current speech
     */
    public void stop() {
        if (tts != null) {
            tts.stop();
            callbackMap.clear();
        }
    }
    
    /**
     * Shutdown TTS engine
     */
    public void shutdown() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            isInitialized = false;
            callbackMap.clear();
        }
    }
    
    // Configuration
    
    /**
     * Set speech rate
     */
    public void setSpeechRate(float rate) {
        this.speechRate = rate;
        if (tts != null) {
            tts.setSpeechRate(rate);
        }
    }
    
    /**
     * Set speech pitch
     */
    public void setPitch(float pitch) {
        this.pitch = pitch;
        if (tts != null) {
            tts.setPitch(pitch);
        }
    }
    
    /**
     * Set volume
     */
    public void setVolume(float volume) {
        this.volume = Math.max(0.0f, Math.min(1.0f, volume));
    }
    
    /**
     * Get current configuration
     */
    public TTSConfiguration getConfiguration() {
        return new TTSConfiguration(speechRate, pitch, volume);
    }
    
    // Callback Interfaces
    
    public interface TTSCallback {
        void onComplete();
        void onError(String error);
    }
    
    public interface TTSInitCallback {
        void onInitialized(boolean success);
    }
    
    // Configuration Class
    
    public static class TTSConfiguration {
        public final float speechRate;
        public final float pitch;
        public final float volume;
        
        public TTSConfiguration(float speechRate, float pitch, float volume) {
            this.speechRate = speechRate;
            this.pitch = pitch;
            this.volume = volume;
        }
    }
}
