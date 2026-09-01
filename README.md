# Devs Code 🚀

**Devs Code** — полноценный мобильный **AI Coding Assistant / AI Software Engineer** для Android, созданный на Kotlin и Jetpack Compose с поддержкой Google Gemini API.

> **Главная концепция:** Devs Code — персональный AI Software Engineer в Android-смартфоне пользователя. Помогает проектировать архитектуру, создавать код, находить и исправлять баги, генерировать нативные Android/iOS шаблоны и экспортировать проекты на устройство.

---

## 📱 Основные возможности

- **AI Чат для разработчиков**: Анализ задач, генерация кода (Kotlin, Swift, Python, JS/TS, C++, SQL), объяснение технических решений.
- **Генератор приложений & Шаблоны**:
  - Быстрые шаблоны: **Android** (Kotlin + Jetpack Compose + Clean Architecture + Room), **iOS** (Swift + SwiftUI + MVVM), **UI/UX Design System**.
  - Экспорт любого шаблона или сгенерированного проекта в `.zip` архив прямо в память устройства через Android Storage Access Framework (SAF).
- **Файловый менеджер и Code Editor**: Подсветка синтаксиса, навигация по структуре проекта, импорт и экспорт исходников.
- **Инспектор кода & Дебаггер**: Поиск ошибок в коде, оптимизация алгоритмов, рефакторинг и проверка соответствия лучшим практикам.
- **Локальная база данных (Room)**: Все проекты, файлы и история чатов надежно сохраняются локально на устройстве без сторонних серверов.

---

## ⚙️ Автоматическая сборка APK в GitHub Actions

В репозитории настроен автоматический пайплайн CI/CD в файле `.github/workflows/build-apk.yml`.

### Триггеры сборки:
1. **Push** в ветки `main` или `master` — автоматическая компиляция и выгрузка отладочного APK (`DevsCode-Debug-APK`) в артефакты сборки.
2. **Push тега** (например, `git tag v1.0.0 && git push --tags`) — автоматическая сборка и публикация релиза в раздел **Releases** на GitHub с прикрепленными установочными APK.
3. **Ручной запуск (Workflow Dispatch)** — на вкладке **Actions** в GitHub можно вручную запустить сборку с выбором типа (`debug`, `release` или `both`).

### Как скачать собранный APK из GitHub:
1. Перейдите во вкладку **Actions** вашего репозитория на GitHub.
2. Выберите последний успешный запуск **Build Devs Code APK**.
3. Внизу страницы в блоке **Artifacts** скачайте архив `DevsCode-Debug-APK`.
4. Распакуйте и установите APK на Android-смартфон.

---

## 🛠️ Стек технологий

- **Язык**: Kotlin 2.2+
- **UI Фреймворк**: Jetpack Compose (Material Design 3)
- **Архитектура**: MVVM + Clean Architecture + Coroutines & Flow
- **База данных**: Room Persistence Library
- **Сетевой уровень**: Retrofit 2 + OkHttp 3 + Moshi
- **Безопасность**: AndroidX Security Crypto (EncryptedSharedPreferences) + Secrets Gradle Plugin

---

## 📄 Лицензия

Разработано для разработчиков. Все права защищены.
