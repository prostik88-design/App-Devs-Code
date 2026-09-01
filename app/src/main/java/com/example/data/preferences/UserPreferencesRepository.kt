package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "devscode_preferences")

class UserPreferencesRepository(private val context: Context) {

    companion object {
        val KEY_SELECTED_MODEL = stringPreferencesKey("selected_gemini_model")
        val KEY_CUSTOM_API_KEY = stringPreferencesKey("custom_gemini_api_key")
        val KEY_EDITOR_WORD_WRAP = booleanPreferencesKey("editor_word_wrap")
        val KEY_EDITOR_LINE_NUMBERS = booleanPreferencesKey("editor_line_numbers")
        val KEY_EDITOR_FONT_SIZE = intPreferencesKey("editor_font_size")
        val KEY_ACTIVE_PROJECT_ID = stringPreferencesKey("active_project_id")
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode") // "SYSTEM", "DARK", "LIGHT"
        val KEY_DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val KEY_GEMINI_TEMPERATURE = androidx.datastore.preferences.core.floatPreferencesKey("gemini_temperature")
        val KEY_GEMINI_TOP_P = androidx.datastore.preferences.core.floatPreferencesKey("gemini_top_p")
        val KEY_GEMINI_TOP_K = intPreferencesKey("gemini_top_k")
        val KEY_SYSTEM_PROMPT = stringPreferencesKey("custom_system_prompt")
    }

    val themeMode: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_THEME_MODE] ?: "SYSTEM"
    }

    val dynamicColor: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_DYNAMIC_COLOR] ?: false
    }

    val geminiTemperature: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[KEY_GEMINI_TEMPERATURE] ?: 0.7f
    }

    val geminiTopP: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[KEY_GEMINI_TOP_P] ?: 0.95f
    }

    val geminiTopK: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[KEY_GEMINI_TOP_K] ?: 40
    }

    val customSystemPrompt: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_SYSTEM_PROMPT] ?: ""
    }

    val selectedModel: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_SELECTED_MODEL] ?: "gemini-3.5-flash"
    }

    val customApiKey: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_CUSTOM_API_KEY] ?: ""
    }

    val editorWordWrap: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_EDITOR_WORD_WRAP] ?: false
    }

    val editorLineNumbers: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_EDITOR_LINE_NUMBERS] ?: true
    }

    val editorFontSize: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[KEY_EDITOR_FONT_SIZE] ?: 14
    }

    val activeProjectId: Flow<Long?> = context.dataStore.data.map { preferences ->
        preferences[KEY_ACTIVE_PROJECT_ID]?.toLongOrNull()
    }

    suspend fun setSelectedModel(model: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SELECTED_MODEL] = model
        }
    }

    suspend fun setCustomApiKey(key: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_CUSTOM_API_KEY] = key
        }
    }

    suspend fun setEditorWordWrap(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_EDITOR_WORD_WRAP] = enabled
        }
    }

    suspend fun setEditorLineNumbers(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_EDITOR_LINE_NUMBERS] = enabled
        }
    }

    suspend fun setEditorFontSize(size: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_EDITOR_FONT_SIZE] = size
        }
    }

    suspend fun setActiveProjectId(id: Long?) {
        context.dataStore.edit { preferences ->
            if (id != null) {
                preferences[KEY_ACTIVE_PROJECT_ID] = id.toString()
            } else {
                preferences.remove(KEY_ACTIVE_PROJECT_ID)
            }
        }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_THEME_MODE] = mode
        }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_DYNAMIC_COLOR] = enabled
        }
    }

    suspend fun setGeminiTemperature(temp: Float) {
        context.dataStore.edit { preferences ->
            preferences[KEY_GEMINI_TEMPERATURE] = temp
        }
    }

    suspend fun setGeminiTopP(topP: Float) {
        context.dataStore.edit { preferences ->
            preferences[KEY_GEMINI_TOP_P] = topP
        }
    }

    suspend fun setGeminiTopK(topK: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_GEMINI_TOP_K] = topK
        }
    }

    suspend fun setCustomSystemPrompt(prompt: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SYSTEM_PROMPT] = prompt
        }
    }
}
