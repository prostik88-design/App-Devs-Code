package com.example.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.domain.model.AiNetworkState
import com.example.domain.model.ApiErrorType
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException

object NetworkUtils {

    /**
     * Checks if the device has an active internet connection.
     */
    fun isNetworkAvailable(context: Context): Boolean {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return true
            val activeNetwork = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false

            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } catch (e: Exception) {
            true // Fallback to true if permission or check fails
        }
    }

    /**
     * Maps network and API exceptions into domain [AiNetworkState].
     */
    fun mapExceptionToAiNetworkState(throwable: Throwable): AiNetworkState {
        return when (throwable) {
            is SocketTimeoutException -> {
                AiNetworkState.Timeout(
                    message = "Превышено время ожидания ответа Gemini (Timeout). Проверьте соединение и нажмите «Повторить»."
                )
            }
            is UnknownHostException -> {
                AiNetworkState.NoConnection(
                    message = "Не удалось подключиться к серверу Google AI. Проверьте интернет-соединение."
                )
            }
            is SSLHandshakeException -> {
                AiNetworkState.ApiError(
                    code = 495,
                    errorType = ApiErrorType.BAD_REQUEST,
                    message = "Ошибка защищенного SSL/TLS соединения. Сетевые запросы должны быть исключительно по HTTPS."
                )
            }
            is HttpException -> {
                val code = throwable.code()
                val errorBody = try {
                    throwable.response()?.errorBody()?.string() ?: ""
                } catch (e: Exception) {
                    ""
                }

                when (code) {
                    400 -> {
                        val isApiKeyIssue = errorBody.contains("API_KEY_INVALID", ignoreCase = true) ||
                                errorBody.contains("API key not valid", ignoreCase = true)
                        val isContextTooLong = errorBody.contains("token", ignoreCase = true) ||
                                errorBody.contains("context", ignoreCase = true) ||
                                errorBody.contains("exceeds", ignoreCase = true)

                        if (isApiKeyIssue) {
                            AiNetworkState.ApiError(
                                code = code,
                                errorType = ApiErrorType.INVALID_API_KEY,
                                message = "Указан недействительный API-ключ Gemini. Проверьте ключ в Настройках.",
                                details = errorBody
                            )
                        } else if (isContextTooLong) {
                            AiNetworkState.ApiError(
                                code = code,
                                errorType = ApiErrorType.CONTEXT_TOO_LONG,
                                message = "Слишком большой объем данных или контекста диалога. Попробуйте сократить размер прикрепленного кода.",
                                details = errorBody
                            )
                        } else {
                            AiNetworkState.ApiError(
                                code = code,
                                errorType = ApiErrorType.BAD_REQUEST,
                                message = "Некорректный запрос к Gemini API (400 Bad Request).",
                                details = errorBody
                            )
                        }
                    }
                    401, 403 -> {
                        AiNetworkState.ApiError(
                            code = code,
                            errorType = ApiErrorType.INVALID_API_KEY,
                            message = "Ошибка доступа (код $code). Проверьте правильность и права Gemini API ключа.",
                            details = errorBody
                        )
                    }
                    413 -> {
                        AiNetworkState.ApiError(
                            code = code,
                            errorType = ApiErrorType.CONTEXT_TOO_LONG,
                            message = "Превышен допустимый лимит контекста и токенов запроса (413 Payload Too Large).",
                            details = errorBody
                        )
                    }
                    429 -> {
                        AiNetworkState.ApiError(
                            code = code,
                            errorType = ApiErrorType.QUOTA_EXCEEDED,
                            message = "Превышен лимит запросов к Gemini API (429 Rate Limit Exceeded). Подождите 30 секунд или используйте персональный ключ.",
                            details = errorBody
                        )
                    }
                    500, 502, 503, 504 -> {
                        AiNetworkState.ServerError(
                            code = code,
                            message = "Сервер Google AI временно перегружен или недоступен (код $code). Нажмите «Повторить»."
                        )
                    }
                    else -> {
                        AiNetworkState.ApiError(
                            code = code,
                            errorType = ApiErrorType.UNKNOWN,
                            message = "Ошибка API (HTTP $code): ${throwable.message()}",
                            details = errorBody
                        )
                    }
                }
            }
            is kotlinx.coroutines.CancellationException -> {
                AiNetworkState.Cancelled
            }
            is IOException -> {
                AiNetworkState.NoConnection(
                    message = "Сетевая ошибка: ${throwable.localizedMessage ?: "Сбой передачи данных"}."
                )
            }
            else -> {
                AiNetworkState.ApiError(
                    code = -1,
                    errorType = ApiErrorType.UNKNOWN,
                    message = throwable.localizedMessage ?: "Произошла непредвиденная ошибка при обращении к AI."
                )
            }
        }
    }
}
