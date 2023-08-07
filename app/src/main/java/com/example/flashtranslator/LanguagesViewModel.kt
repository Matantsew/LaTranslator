package com.example.flashtranslator

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flashtranslator.data.repositories.LanguagesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class LanguagesViewModel @Inject internal constructor(@ApplicationContext context: Context,
                                                      private val languagesRepository: LanguagesRepository)
    : ViewModel() {

    private var _availableLanguages = MutableLiveData<List<Language>>(listOf())
    val availableLanguages: LiveData<List<Language>> get() = _availableLanguages

    private var _downloadedLanguages = MutableLiveData<List<Language>>(listOf())
    val downloadedLanguages: LiveData<List<Language>> get() = _downloadedLanguages

    private var _downloadingLanguagesKeysSet = MutableStateFlow<HashSet<String>>(hashSetOf())
    val downloadingLanguagesKeysSet: StateFlow<HashSet<String>> = _downloadingLanguagesKeysSet

    private var _sourceLanguageKey = MutableLiveData<String?>()
    val sourceLanguageKey: LiveData<String?> get() = _sourceLanguageKey

    private var _targetLanguageKey = MutableLiveData<String?>()
    val targetLanguageKey: LiveData<String?> get() = _targetLanguageKey

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val languagesList = mutableListOf<Language>()
            languagesRepository.getAvailableLanguages().collect { language ->
                languagesList.add(language)
            }

            withContext(Dispatchers.Main) {
                _availableLanguages.value = languagesList
            }
        }

        obtainDownloadedLanguages()

        viewModelScope.launch(Dispatchers.Main) {

            val sourcePosition = languagesRepository.getSourceLanguageKey(context)
            _sourceLanguageKey.postValue(sourcePosition)
        }

        viewModelScope.launch(Dispatchers.Main) {
            val targetPosition = languagesRepository.getTargetLanguageKey(context)
            _targetLanguageKey.postValue(targetPosition)
        }
    }

    fun obtainDownloadedLanguages() {
        viewModelScope.launch(Dispatchers.Main) {
            languagesRepository.getDownloadedLanguages { languagesList ->
                _downloadedLanguages.value = languagesList
            }
        }
    }

    fun obtainOrWaitLanguageModelRemotely(languageTag: String, onCompleteBlock: (complete: Boolean) -> Unit) {

        _downloadingLanguagesKeysSet.value.add(languageTag)

        languagesRepository.downloadLanguageModel(languageTag).addOnCompleteListener {
            _downloadingLanguagesKeysSet.value.remove(languageTag)
            onCompleteBlock(it.isSuccessful)
        }
    }

    fun saveSourceLanguage(context: Context, key: String) {
        viewModelScope.launch(Dispatchers.IO) {
            languagesRepository.setSourceLanguageKey(context, key)
            _sourceLanguageKey.postValue(key)
        }
    }

    fun saveTargetLanguage(context: Context, key: String) {
        viewModelScope.launch(Dispatchers.IO) {
            languagesRepository.setTargetLanguageKey(context, key)
            _targetLanguageKey.postValue(key)
        }
    }
}