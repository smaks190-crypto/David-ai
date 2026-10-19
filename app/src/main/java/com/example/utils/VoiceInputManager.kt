package com.example.utils

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

class VoiceInputManager(private val context: Context) {

    private val TAG = "VoiceInputManager"
    private var speechRecognizer: SpeechRecognizer? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _voiceLevel = MutableStateFlow(0f)
    val voiceLevel: StateFlow<Float> = _voiceLevel.asStateFlow()

    private val _partialText = MutableStateFlow("")
    val partialText: StateFlow<String> = _partialText.asStateFlow()

    private val _recognizedOperations = MutableStateFlow<List<ParsedVoiceOperation>>(emptyList())
    val recognizedOperations: StateFlow<List<ParsedVoiceOperation>> = _recognizedOperations.asStateFlow()

    private var shouldKeepListening = false
    private var restartJob: Job? = null

    init {
        initializeRecognizer()
    }

    private fun initializeRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.w(TAG, "Speech recognition is not available on this device")
            return
        }

        try {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(createRecognitionListener())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing SpeechRecognizer", e)
        }
    }

    private fun createRecognitionListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            _isListening.value = true
        }

        override fun onBeginningOfSpeech() {
            _isListening.value = true
        }

        override fun onRmsChanged(rmsdB: Float) {
            // Normalize -2dB..10dB to 0f..1f range for smooth wave animation
            val normalized = min(1f, max(0f, (rmsdB + 2f) / 12f))
            _voiceLevel.value = normalized
        }

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            _voiceLevel.value = 0f
        }

        override fun onError(error: Int) {
            Log.d(TAG, "SpeechRecognizer error: $error")
            _voiceLevel.value = 0f

            if (shouldKeepListening) {
                // Auto restart after a brief moment if still active
                restartJob?.cancel()
                restartJob = scope.launch {
                    delay(300)
                    if (shouldKeepListening) {
                        startListeningInternal()
                    }
                }
            } else {
                _isListening.value = false
            }
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val recognizedString = matches?.firstOrNull() ?: ""

            if (recognizedString.isNotBlank()) {
                val newOperations = VoiceParserHelper.parseVoiceInput(recognizedString)
                if (newOperations.isNotEmpty()) {
                    appendOperations(newOperations)
                }
            }

            _partialText.value = ""
            _voiceLevel.value = 0f

            // Continuous recognition: auto-restart immediately to capture next sentence
            if (shouldKeepListening) {
                restartJob?.cancel()
                restartJob = scope.launch {
                    delay(200)
                    if (shouldKeepListening) {
                        startListeningInternal()
                    }
                }
            } else {
                _isListening.value = false
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val currentPartial = matches?.firstOrNull() ?: ""

            if (currentPartial.isNotBlank()) {
                _partialText.value = currentPartial

                // Real-time extraction: if user completed a full segment, parse and preview
                val liveOps = VoiceParserHelper.parseVoiceInput(currentPartial)
                if (liveOps.size > _recognizedOperations.value.size) {
                    // New items emerged in partial results!
                }
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    fun startListening() {
        shouldKeepListening = true
        startListeningInternal()
    }

    private fun startListeningInternal() {
        if (speechRecognizer == null) {
            initializeRecognizer()
        }

        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ru-RU")
                putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "ru-RU")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }
            speechRecognizer?.startListening(intent)
            _isListening.value = true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start speech recognition", e)
            _isListening.value = false
        }
    }

    fun stopListening() {
        shouldKeepListening = false
        restartJob?.cancel()
        try {
            speechRecognizer?.stopListening()
        } catch (_: Exception) {}
        _isListening.value = false
        _voiceLevel.value = 0f
    }

    fun cancelListening() {
        shouldKeepListening = false
        restartJob?.cancel()
        try {
            speechRecognizer?.cancel()
        } catch (_: Exception) {}
        _isListening.value = false
        _voiceLevel.value = 0f
        _partialText.value = ""
    }

    fun appendOperations(newOps: List<ParsedVoiceOperation>) {
        val current = _recognizedOperations.value.toMutableList()
        current.addAll(newOps)
        _recognizedOperations.value = current
    }

    fun removeOperation(id: String) {
        val current = _recognizedOperations.value.toMutableList()
        current.removeAll { it.id == id }
        _recognizedOperations.value = current
    }

    fun clearOperations() {
        _recognizedOperations.value = emptyList()
        _partialText.value = ""
    }

    /**
     * For manual injection / testing in emulator without microphone
     */
    fun injectSimulatedPhrase(phrase: String) {
        _partialText.value = phrase
        val parsed = VoiceParserHelper.parseVoiceInput(phrase)
        if (parsed.isNotEmpty()) {
            appendOperations(parsed)
        }
    }

    fun destroy() {
        shouldKeepListening = false
        restartJob?.cancel()
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (_: Exception) {}
    }
}
