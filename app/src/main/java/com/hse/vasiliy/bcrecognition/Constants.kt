package com.hse.vasiliy.bcrecognition

const val REQUEST_STARTUP_PERMISSIONS = 0

const val CONFIRMATION_DIALOG = "fragment_dialog"
const val ERROR_DIALOG = "error_dialog"
const val BITMAP_TMP = "bitmap"
const val TESSERACT_SAMPLES_PATH = "tessdata"
const val TESSERACT_ASSETS = "traindata"

const val CAMERA_FRAGMENT_TAG = "CAMERA_FRAGMENT"
const val RECOGNITION_FRAGMENT_TAG = "RECOGNITION_FRAGMENT"
const val SETTINGS_FRAGMENT_TAG = "SETTINGS_FRAGMENT"

const val PREFS = "ActivityStorage"
const val PREF_ACCESS_TOKEN = "access_token"
const val OFFLINE_MODE = "offline_mode"

const val EMAIL_REGEX = "(?:[a-z0-9!#\$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#\$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:(2(5[0-5]|[0-4][0-9])|1[0-9][0-9]|[1-9]?[0-9]))\\.){3}(?:(2(5[0-5]|[0-4][0-9])|1[0-9][0-9]|[1-9]?[0-9])|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])"
const val PHONE_REGEX = "^((\\+\\d{1,4})[\\- ]?)?(\\(?\\d{3,4}\\)?[\\- ]?)?[\\d\\- ]{7,10}\$"

//Confirmation dialog constants
const val DIALOG_ID = "id"
const val TITLE_TAG = "title"
const val TEXT_TAG = "text"
const val POS_BTN_TEXT_TAG = "positive"
const val NEG_BTN_TEXT_TAG = "negative"

enum class ConfirmationDialogsTypes {
    PERMISSION_RATIONALE,
    NETWORK_SWITCH_ONLINE_NOTIFICATION,
    NETWORK_SWITCH_OFFLINE_NOTIFICATION
}