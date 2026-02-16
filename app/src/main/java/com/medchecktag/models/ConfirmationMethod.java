package com.medchecktag.models;

/**
 * Enum representing dose confirmation method.
 */
public enum ConfirmationMethod {
    /**
     * Dose confirmed by scanning NFC tag.
     */
    NFC_SCAN,

    /**
     * Dose confirmed manually via button press (if manual confirmation enabled in settings).
     */
    MANUAL_CONFIRM
}
