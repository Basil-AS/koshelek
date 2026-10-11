# Koshelek Android — дизайн

## Цель

Создать публичное Android-приложение-кошелёк карт лояльности без рекламы. Приложение работает без собственного backend: карты, сессии магазинов, динамические QR и зашифрованные архивы хранятся локально. Пользователь может добавить карту сканированием/ручным вводом, авторизоваться в поддерживаемом магазине и получить обновляемый QR/штрихкод.

## MVP-магазины

- Магнит (`ru.tander.magnit`)
- Перекрёсток (`ru.perekrestok.app`)
- Пятёрочка (`ru.pyaterochka.app.browser`)
- Улыбка радуги (`com.platfomni.rulybka`)
- Магнолия (`ru.shop.mgnl.app`)

Каждый магазин реализуется отдельным адаптером. Универсальный режим сканирования и импорта остаётся доступным даже при недоступности автоматической интеграции.

## Архитектура

Нативное Android-приложение на Kotlin и Jetpack Compose/Material 3. Слои:

1. `ui` — Compose-экраны, навигация, темы, состояния загрузки/ошибок.
2. `domain` — карты, коды, сессии, refresh-policy, архивы; не знает Android UI.
3. `data` — Room для локальных сущностей, DataStore для настроек, Keystore-backed encryption.
4. `integration` — интерфейс `RetailerAdapter`; по одному модулю на магазин.
5. `scanner` — CameraX/ML Kit barcode and QR capture.
6. `background` — WorkManager refresh jobs с ограничениями сети/заряда.

Базовый контракт адаптера:

```kotlin
interface RetailerAdapter {
    val retailerId: String
    suspend fun authenticate(context: Context): AuthResult
    suspend fun refreshCard(session: RetailerSession): RefreshResult
    suspend fun logout(sessionId: String)
}
```

Адаптер обязан возвращать только нормализованную модель `CardCodeSnapshot`: формат кода, payload, время получения, срок действия и возможность offline-показа. Закрытые исходники сторонних приложений не копируются; APK используются как технический/UX-ориентир. Обход антибота, извлечение ключей и подмена официальной авторизации запрещены.

## Локальное хранение и креды

- Данные карт и метаданные — Room.
- Сессионные токены/cookies — шифрованное хранилище, ключ защищён Android Keystore и локальной биометрией/PIN.
- Сохранение логина/пароля — отдельный opt-in режим; пароль хранится только зашифрованным и никогда не попадает в логи.
- Перед повторной авторизацией используется refresh-token/cookie; пароль применяется только как fallback.
- Одна команда удаляет все сессии и креды.

## Архив импорт/экспорт

Архив — зашифрованный контейнер с версией формата, картами, кодами, настройками и opt-in сессиями. Шифрование: AEAD AES-GCM, ключ из пользовательского пароля через PBKDF2-HMAC-SHA256 с уникальной солью и параметрами KDF в заголовке. Импорт выполняется атомарно во временную транзакцию; повреждённый архив не меняет текущие данные.

## QR/штрихкоды

- Статический код показывается офлайн.
- Динамический код обновляется при открытии карты и перед показом.
- WorkManager обновляет только карты с активной сессией и разрешённой фоновой политикой.
- При отсутствии сети показывается последний код до `expiresAt`; просроченный код явно блокируется.
- Экран показа — полноэкранный, с высокой контрастностью, яркостью и быстрым переключением между кодом/номером.

## UI

Главный экран — сетка/список карточек с избранными магазинами сверху, быстрый сканер, фильтр и состояние синхронизации. Экран карты — крупный QR/штрихкод, название, статус свежести, кнопки обновления/редактирования/удаления. Никакой рекламы, трекинга и обязательной регистрации.

## Безопасность

Минимальные разрешения: камера и сеть; NFC только при фактическом сценарии. Не запрашивать контакты, геолокацию, телефон и рекламный идентификатор. Не логировать payload кодов, cookies, токены или пароли. Для сетевых интеграций использовать TLS, таймауты, certificate pinning только после подтверждённой необходимости и без хардкода секретов.

## Тестирование

- Unit: нормализация карт, expiry/refresh policy, архивы, шифрование, адаптерные контракты.
- Instrumentation: Room/Keystore/биометрический gate, импорт/экспорт, scanner result flow.
- Compose UI: главный экран, empty/error/loading, экран кода, просроченный QR.
- Integration: fake retailer servers с истёкшей сессией, refresh-token, dynamic QR и network failure.
- Release smoke: assemble debug/release, APK/AAB metadata, отсутствие секретов и лишних разрешений.

## GitHub delivery

- Репозиторий публичный, работа только в task-ветках `agent/*`.
- `main` защищается PR; прямые коммиты запрещены.
- CI: lint, unit tests, instrumentation/build checks, dependency/security checks.
- CD: GitHub Release для тегов SemVer, подписанные артефакты только при наличии настроенных repository secrets.
- Каждая фаза — отдельный PR с описанием DoD, логами проверок и review перед merge.

## Этапы

1. Foundation: Android project, Compose shell, local Room model, generic scanned cards, archive crypto, CI.
2. Code display/scanner: QR/barcode capture, static/dynamic snapshot UI, offline behavior.
3. Retailer adapter framework and fake integrations.
4. Real retailer adapters one by one, starting with the supplied APK/package evidence.
5. Release hardening: permissions, security review, signed build, GitHub release and documentation.
