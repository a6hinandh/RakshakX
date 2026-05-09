package com.security.rakshakx.integration

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import com.security.rakshakx.RakshakXApplication

class ScamDetectionViewModel(application: Application) : AndroidViewModel(application) {

    // ─── LiveData ─────────────────────────────────────────────────────────────

    private val _result = MutableLiveData<ModelResult?>()
    val result: LiveData<ModelResult?> = _result

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    // ─── Public API ───────────────────────────────────────────────────────────

    fun classifySms(text: String) = classify(text, "sms")

    fun classifyEmail(subject: String, body: String) =
        classify("$subject\n$body", "email")

    fun classifyCall(transcript: String) = classify(transcript, "call")

    fun classifyWeb(url: String, pageText: String) =
        classify("$url\n$pageText", "web")

    fun classifyGeneric(text: String) = classify(text, "generic")

    fun clearResult() {
        _result.postValue(null)
        _error.postValue(null)
    }

    // ─── Internal ─────────────────────────────────────────────────────────────

    private fun classify(text: String, channel: String) {
        if (text.isBlank()) {
            _error.postValue("Input text is empty")
            return
        }

        viewModelScope.launch {
            _isLoading.postValue(true)
            _error.postValue(null)

            val modelResult = withContext(Dispatchers.Default) {
                try {
                    RakshakXApplication.scamRouter?.classify(text, channel)
                } catch (e: Exception) {
                    null
                }
            }

            if (modelResult != null) {
                _result.postValue(modelResult)
            } else {
                _error.postValue("Classification failed. Please try again.")
            }

            _isLoading.postValue(false)
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Singleton router released in Application.onTerminate
    }
}