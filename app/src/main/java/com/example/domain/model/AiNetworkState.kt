package com.example.domain.model

/**
 * Detailed classification of AI and Network API errors.
 */
enum class ApiErrorType(val userTitleRu: String) {
    INVALID_API_KEY("Недействительный API-ключ"),
    QUOTA_EXCEEDED("Лимит запросов исчерпан (429)"),
    CONTEXT_TOO_LONG("Слишком длинный контекст / превышен лимит токенов"),
    BAD_REQUEST("Некорректный запрос к API"),
    AUTHENTICATION_REQUIRED("Требуется авторизация или ключ Gemini"),
    UNKNOWN("Неизвестная ошибка API")
}

/**
 * State representing AI generation lifecycle and network communication.
 */
sealed class AiNetworkState {
    object Idle : AiNetworkState()
    
    // Загрузка
    object Loading : AiNetworkState()
    
    // Потоковая генерация
    data class Streaming(val partialText: String) : AiNetworkState()
    
    // Успешный ответ
    data class Success(val fullText: String, val tokensUsed: Int = 0) : AiNetworkState()
    
    // Отсутствие данных
    object EmptyData : AiNetworkState()
    
    // Отсутствие подключения
    data class NoConnection(
        val message: String = "Отсутствует подключение к интернету. Проверьте Wi-Fi или мобильную сеть."
    ) : AiNetworkState()
    
    // Тайм-аут
    data class Timeout(
        val message: String = "Превышено время ожидания ответа сервера (60 сек). Повторите попытку."
    ) : AiNetworkState()
    
    // Ошибка API (400, 401, 403, 429, 413)
    data class ApiError(
        val code: Int,
        val errorType: ApiErrorType,
        val message: String,
        val details: String? = null
    ) : AiNetworkState()
    
    // Ошибка сервера (500, 502, 503)
    data class ServerError(
        val code: Int,
        val message: String = "Сервер Gemini временно недоступен (код $code). Попробуйте позже."
    ) : AiNetworkState()
    
    // Отменённая операция
    object Cancelled : AiNetworkState()
}
