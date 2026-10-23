# StudyMate Lab 2 — Android E2E и Wireshark

## 1. Что именно тестируется

`StudyMateApplicationE2ETest` обращается к **установленному** изолированному debug APK
через `ContentResolver.call()` (`Lab2E2eBridgeProvider`). Вызовы внутри приложения
используют реальные Koin, Room, Retrofit и синхронизацию. Независимый Retrofit-клиент
в тестовом APK вызывает FastAPI и сверяет сохранённые записи в PostgreSQL.

Проверяемая последовательность:

1. Создать пользователя и войти через приложение.
2. Создать тему и задачу через приложение; синхронизировать их.
3. Через отдельный HTTP-клиент выполнить login/pull и проверить тему и задачу на сервере.
4. Выйти из приложения, убедиться в отсутствии данных локального аккаунта.
5. Снова войти, проверить восстановление темы и задачи с сервера.

Здесь **нет проверки пользовательского интерфейса**: это headless E2E через публичный
тестовый IPC-контракт приложения. Провайдер активен только в отдельной сборке
`-Plab2E2e=true` (суффикс `.lab2e2e`) и отключён в обычном debug APK.

## 2. Самый простой запуск на Fedora

Необходимы Docker с `docker compose`, Android SDK/эмулятор, `adb` (Fedora:
`sudo dnf install android-tools`) и работающее виртуальное Android-устройство.
Тест **не запускается на физическом телефоне**, даже если телефон подключён.

В Android Studio откройте `mobile-app`, запустите эмулятор через Device Manager.
В терминале проверьте:

```bash
adb devices
# emulator-5554    device
```

Из **корня проекта**:

```bash
make e2e
# или bash lab2/scripts/run-lab2.sh e2e
```

Скрипт сам:
- поднимет чистые PostgreSQL и FastAPI в Docker;
- узнает опубликованный **случайный порт** API (например, 32768);
- настроит `adb reverse tcp:8000 tcp:<порт API>`;
- соберёт и установит изолированный Android APK, запустит только E2E-класс;
- запишет пакеты на стороне FastAPI в `lab2/reports/lab2-e2e.pcap`;
- создаст JUnit и Allure, остановит стенд и удалит временную БД.

Отчёты и трафик (хранятся после завершения Docker-стенда):

```text
lab2/reports/lab2-e2e.pcap
lab2/reports/junit/
lab2/reports/allure-results/
lab2/reports/allure-report/index.html
lab2/reports/diagnostics/     # логи сервера при неудаче
```

Открыть Allure:

```bash
make report-open
```

## 3. Посмотреть **настоящий E2E** в Wireshark

Откройте захват после `make e2e`:

```bash
wireshark lab2/reports/lab2-e2e.pcap
```

Фильтры Wireshark:

```text
http.request
http.request.uri contains "/api/v1/"
tcp.port == 8000
```

В запросах должны быть видны как минимум регистрация, вход, отправка/загрузка
синхронизации (`/api/v1/sync/initial`, `/push`, `/pull`, если используются
данной версией синхронизации). Проверьте HTTP-коды, последовательность и поля
`clientId` темы и задачи. Можно выделить HTTP-запрос и открыть
**Follow → TCP Stream**. Некоторые HTTP-запросы могут разбиваться на несколько
TCP-сегментов; это нормально.

Чтобы вывести запросы в консоли:

```bash
tshark -r lab2/reports/lab2-e2e.pcap -Y http.request \
  -T fields -e frame.number -e http.request.method -e http.request.uri
```

**Важно:** `make capture` — отдельная демонстрационная запись HTTP replay.
Для доказательства реального Android E2E используйте **`make e2e`**.
Захваченный HTTP не шифруется, содержит тестовые пароли и токены:
не публикуйте захват без необходимости; проверяйте только на изолированном стенде.

## 4. Ручной запуск через Gradle / Android Studio

Когда хотите запускать непосредственно `StudyMateApplicationE2ETest` через Gradle,
сначала поднимите API на **фиксированном** порту 8000:

```bash
docker volume create studymate_lab2_gradle_cache
LAB2_HOST_PORT=8000 docker compose -f lab2/docker-compose.test.yml up --build -d --wait postgres api
adb devices
adb -s emulator-5554 reverse tcp:8000 tcp:8000
adb -s emulator-5554 reverse --list
```

Затем в другом терминале:

```bash
cd mobile-app
./gradlew :app:connectedDebugAndroidTest \
  -Plab2E2e=true \
  -Plab2E2eBaseUrl=http://127.0.0.1:8000/ \
  -Pandroid.testInstrumentationRunnerArguments.class=com.z23u184.studymate.app.StudyMateApplicationE2ETest \
  -Pandroid.testInstrumentationRunnerArguments.studymate.baseUrl=http://127.0.0.1:8000/
```

В Gradle-configuration Android Studio используйте те же аргументы.
Без `-Plab2E2e=true` провайдер E2E будет выключен, а обычное приложение
может иметь другой `applicationId`. Без `adb reverse` Android `127.0.0.1:8000`
обычно указывает в эмулятор, а **не** в FastAPI на рабочем компьютере.

После ручного запуска:

```bash
adb -s emulator-5554 reverse --remove tcp:8000
LAB2_HOST_PORT=8000 docker compose -f lab2/docker-compose.test.yml down --volumes
```

В ручном режиме выше автоматическая запись PCAP не запускается; используйте
`make e2e` для захвата (он запускает `tcpdump` до регистрации и останавливает
после теста).

## 5. Отдельный build-stage в CI

В `.github/workflows/lab2-ci.yml` и `lab2-full-device.yml`:

```text
build (assembleDebug + assembleDebugAndroidTest, lab2E2e=true)
   ↓
unit-python + unit-android
   ↓
integration (PostgreSQL, два прогона)
   ↓
e2e (скачивает APK из build; install + am instrument + PCAP)
   ↓
prepare-pages (сводный Allure)
```

Build загружает APK как CI artifacts. E2E **не компилирует их повторно**.
Результат `am instrument` конвертируется в JUnit и Allure, включая ошибки.
GitLab CI также разделён на `build`, `unit`, `integration` и ручной
`e2e` (нужен runner с эмулятором и доступным ADB).

Самостоятельная сборка локально:

```bash
make build
```
