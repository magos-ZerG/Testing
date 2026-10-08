# ТИОПО — лабораторная работа № 2 — StudyMate

## Что сделано

Проект состоит из `mobile-app` (Android, Kotlin, Room, Retrofit) и `server` (Python, FastAPI, SQLAlchemy, PostgreSQL). Исходники из двух архивов сохранены в отдельных каталогах.

- **Unit (server):** 8 новых pure unit-тестов схем запросов и валидации в `server/tests/unit/` (не требуют PostgreSQL).
- **Unit — ЛР № 1:** используются исходные 5 classic + 5 London тестов `CreateTaskUseCase` в `mobile-app/data/src/test/`. Команда: `./gradlew :data:labCoverage`. Unit-тесты классического стиля работают с настоящей Room in-memory, а не с основной БД.
- **Integration — ЛР № 2 (server):** новые 9 тестов (файлы `server/tests/integration/`) на реальном PostgreSQL: регистрация/токены, ограничения уникальности, сценарии SQL SyncRepository/SyncService, зависимости topic→task→solution, merge/таймстемпы, каскадное удаление, пакетный rollback, изоляция пользователей, flashcard best result.
- **Integration — Android (существующие):** три инструментальных теста в `mobile-app/data/src/androidTest/…/StudyMateDataIntegrationTest.kt`: настоящий Room + use case + Retrofit + тестовый сервер, в том числе `createUpdateDeleteTask_flow_syncsLocalAndRemoteState`.
- **E2E — ЛР № 2:** три новых HTTP-сценария из `server/tests/e2e/test_study_journey.py`, без тестирования GUI. Главный сценарий демонстрирует регистрацию → вход → initial sync (topic/task/solution) → pull → update → delete → pull → refresh → logout. Второй проверяет авторизацию/изоляцию, третий — отправку файла, сохранение метаданных и скачивание.
- **Отчётность:** JUnit XML для CI, Allure raw-results и HTML, поддержка сохранения истории/трендов между запусками, JaCoCo XML/HTML из ЛР №1.
- **Трафик:** `curl` повторяет HTTP-шаги главного E2E-сценария, `tcpdump` в том же сетевом пространстве, что и API, пишет `lab2-e2e.pcap`.

## Стенд и его изоляция

Файл `lab2/docker-compose.test.yml` создаёт **отдельный контейнер PostgreSQL 17** (`studymate_lab2`) + обычный production FastAPI сервер из исходного `server/Dockerfile`. Сервер запускает Alembic миграции. Вместо общей БД используется пустая PostgreSQL на `tmpfs`. Файлы приложения пишутся в отдельный `tmpfs` API. **Никаких production баз, persistent SQL/Mongo volumes и внешних брокеров этот стенд не использует.**

Каждый вызов `run-lab2.sh` создаёт уникальный `COMPOSE_PROJECT_NAME`, поэтому два разработчика не разделяют сеть/БД/контейнеры. Хост-порт API выбирается Docker автоматически (`127.0.0.1:0`), исключая конфликты между параллельными запусками. Между integration и E2E база/файлы полностью удаляются через `docker compose down --volumes`, затем заново поднимаются сервер и пустая БД с миграциями. При ошибке/CTRL+C действует `EXIT`-trap, удаляющий тестовое окружение. Зависимости/Gradle образа устанавливаются в Docker runner images.

В репозитории нет service bus / очереди сообщений на стороне сервера: в Android есть локальная Room sync-очередь, а не внешний message broker. Android инструментальные тесты проверяют, что очередь обработки пуста. Refresh-сессии PostgreSQL сбрасываются вместе с тестовым стендом. **Прерывание тестов** приводит к уничтожению соответствующего контейнера PostgreSQL (уже записанные данные не сохраняются).

Защита от опасного запуска: серверные integration fixtures требуют `LAB2_TEST_DATABASE_URL`, в URI обязательно имя `studymate_lab2`, и сравнивают его с `current_database()` — никакого fallback на `.env`.


### JDK 11 для Android Docker runner

Модуль `mobile-app/domain` задаёт Java/Kotlin toolchain 11, а Gradle 9 запускается
на более новой Java. `lab2/Dockerfile.android-runner` включает **оба JDK**:
обычный JDK из Android SDK-образа для запуска Gradle и Temurin 11 для сборки `domain`.
Gradle получает явный путь к JDK 11 через `GRADLE_OPTS`, поэтому ему не нужно
дополнительно загружать JDK через Foojay при запуске тестов.

После замены Dockerfile пересоберите Android-образ (обычный запуск с `--build`
из `run-lab2.sh` автоматически это сделает). Проверка обнаруженных toolchains:

```bash
sudo docker compose -f lab2/docker-compose.test.yml run --build --rm --no-deps \
  android-tests bash -lc './gradlew --no-daemon :domain:javaToolchains'
```

Ищите `11` и `Is JDK: true` в выводе. Не меняйте `jvmToolchain(11)` в модуле
`domain` и не подменяйте основной `JAVA_HOME` на Java 11: Gradle 9 требует
Java 17 или новее для своего запуска.

### Fedora + SELinux и права доступа к Docker bind mounts

Для Fedora с SELinux у Docker bind mounts в `lab2/docker-compose.test.yml`
указан общий контекст `:z` (`/workspace` используется несколькими test runners,
`/reports` — контейнерами отчётов и захвата). Без этой метки возможны
`PermissionError: /workspace/server/pytest.ini` и
`AccessDeniedException: /reports/allure-results` даже если права Unix верны.
Нельзя заменять это `chmod -R 777` или постоянно отключать SELinux.

Предпочтительно запускать от обычного пользователя с разрешённым доступом к
Docker. Если ранее запускали скрипт через `sudo`, восстановите владельца
генерируемых отчётов и Gradle outputs:

```bash
sudo chown -R "$(id -u):$(id -g)" lab2/reports
# при необходимости: sudo chown -R "$(id -u):$(id -g)" mobile-app/data/build
```

После этого запускайте `bash lab2/scripts/run-lab2.sh` без `sudo`.
`docker info` должен работать от того же пользователя; участники группы `docker`
фактически получают root-привилегии, поэтому добавлять пользователей туда
следует осознанно. Если запуск выполняется через `sudo`, обязательно исправьте
SELinux-монтирования — повышение привилегий само по себе эту проблему не решает.


## Быстрый запуск через Makefile и кэш Gradle

В корне проекта появился `Makefile`; с ним не нужно вводить длинные команды:

```bash
make help                    # все команды
sudo make unit               # только unit-тесты
sudo make integration        # серверные integration, два раза
sudo make android            # только тесты на Android-устройстве
sudo make e2e                # только E2E по HTTP
sudo make all                # полный цикл без Android-устройства
sudo make all-android        # полный цикл с Android-устройством
sudo make report             # пересобрать Allure HTML
sudo make capture            # curl + tcpdump -> lab2-e2e.pcap
sudo make cache-info         # показать постоянный Gradle-кэш
```

Если текущий пользователь имеет права на Docker (`docker info` работает без sudo),
`sudo` не требуется. Перед `make android` или `make all-android` нужен работающий
ADB-сервер, доступный из Docker. На локальной Fedora с обычным Docker Engine
`android-tests` использует `network_mode: host`, поэтому достаточно:

```bash
adb start-server
adb devices
sudo make android
```

Устройство должно отображаться со статусом `device`. ADB слушает localhost;
**не нужно** запускать `adb -a`, открывая порт 5037 всем сетевым интерфейсам.
Проверить доступность ADB из контейнера можно командой:

```bash
sudo docker compose -f lab2/docker-compose.test.yml run --build --rm --no-deps android-tests adb devices
```

Если проверка создаст временную сеть Compose, удалите её командой
`sudo docker compose -f lab2/docker-compose.test.yml down` — внешний Gradle-кэш сохранится.

**Почему Gradle больше не загружается заново каждый раз.** Раньше том
`gradle-cache` автоматически получал имя от `COMPOSE_PROJECT_NAME` (который
генерируется заново при каждом запуске) и удалялся с `docker compose down -v`.
Теперь контейнеры `android-tests` используют один *внешний* Docker volume
`studymate_lab2_gradle_cache` (или `LAB2_GRADLE_CACHE_VOLUME`, если явно задано
другое имя). `run-lab2.sh` автоматически создаёт том при первом запуске любого этапа
и никогда не удаляет его при очистке стенда.
Папка `/root/.gradle` с `wrapper/dists` и скачанными зависимостями остаётся
между прогонами. Это не отменяет **первоначальной** загрузки Gradle, а также
загрузки при смене версии Gradle или новых зависимостей.

Образ Android SDK и JDK 11 уже кэширует Docker, а файлы Gradle `build/`
остаются в рабочем каталоге. Тестовый PostgreSQL и файловое хранилище сервера
по-прежнему изолированы и одноразовые: постоянный том содержит **только Gradle-кэш**, не БД.

Для проверки или сброса кэша:

```bash
sudo make cache-info
sudo make cache-clear    # намеренно удаляет кэш; затем Gradle снова скачается
```

Не используйте `docker volume prune` для очистки этого кэша: если он не
подключён к работающему контейнеру, Docker может удалить и его.

## Запуск отдельного этапа

`lab2/scripts/run-lab2.sh` принимает необязательный аргумент с названием этапа.
Все команды выполняются **из корня проекта** (на Fedora при необходимости
добавьте `sudo` или настройте доступ к Docker для текущего пользователя):

```bash
bash lab2/scripts/run-lab2.sh all              # весь цикл; вариант по умолчанию
bash lab2/scripts/run-lab2.sh unit             # Python unit + Android unit / JaCoCo
bash lab2/scripts/run-lab2.sh integration      # только серверные SQL integration, дважды
bash lab2/scripts/run-lab2.sh android-device   # только тесты на телефоне / эмуляторе
bash lab2/scripts/run-lab2.sh e2e              # только серверные HTTP E2E
bash lab2/scripts/run-lab2.sh report           # только пересоздать HTML Allure из существующих результатов
bash lab2/scripts/run-lab2.sh --help          # справка
```

**Поведение отдельных этапов:**

- `unit` не запускает PostgreSQL/API; выполняет существующие тесты ЛР №1 и пишет JaCoCo.
- `integration` самостоятельно создаёт пустую PostgreSQL + FastAPI, выполняет
  9 SQL-интеграционных тестов **дважды на том же экземпляре БД**, затем удаляет стенд.
- `android-device` самостоятельно создаёт PostgreSQL + API и запускает
  `:data:connectedDebugAndroidTest` на устройстве (нужен работающий доступный
  ADB-сервер на хосте (`adb start-server`), доступный по loopback благодаря
  `network_mode: host`).
  Не требует `RUN_ANDROID_DEVICE_TESTS=1` и **не выполняет серверные integration**.
- `e2e` создаёт новую пустую PostgreSQL + API и запускает 3 HTTP E2E-теста,
  не выполняя unit или integration.
- `all` сохраняет обязательный для CI порядок `unit → integration → e2e`;
  с `RUN_ANDROID_DEVICE_TESTS=1` вставляет Android instrumented integration
  после серверных integration и до E2E.
- `report` не запускает тесты и **не удаляет** текущие результаты; только
  пересоздаёт Allure HTML из `lab2/reports/allure-results`.

Каждый обычный режим запускает **свой изолированный Compose-проект**,
очищает его при завершении/ошибке и формирует отчёты. В отчёте одиночного
режима невыбранные этапы имеют статус `skipped` с пояснением
`Not selected`, а не `passed`. Результаты прошлого тестового прогона
в `lab2/reports/junit` и `lab2/reports/allure-results` перед следующим
**тестовым** запуском удаляются (история Allure сохраняется); поэтому для
общего отчёта всех этапов нужно запускать `all`.

## Запуск всего тестирования

Понадобятся Docker Engine, Docker Compose v2, Bash, Python 3, доступ в интернет для Docker images/Gradle artifacts. Команда из корня репозитория:

```bash
bash lab2/scripts/run-lab2.sh
```

Этапы выполняются **строго**: `unit` → `integration` → `e2e`. При ошибке следующий этап не запускается и получает отчёт `skipped`, а ошибочный — `failed`. Отчёты складываются в `lab2/reports/junit/`, `lab2/reports/allure-results/`, `lab2/reports/allure-report/index.html`, сохранённые данные трендов — в `lab2/reports/history/`. Runner images: `Dockerfile.android-runner` (Gradle/Android SDK), `Dockerfile.test-runner` (Python + pytest) и `Dockerfile.report` (Allure CLI). **Важно:** если Allure HTML image недоступен, остаются JUnit XML и сырые Allure results; HTML будет недоступен до сборки image.

### Инструментальные тесты Android на устройстве

Пункт 7 требует продемонстрировать запуск на устройстве. Для этого нужен Android телефон (USB debugging) либо эмулятор с ADB-сервером, доступным runner-контейнеру. На машине с устройством/эмулятором запустите `adb start-server` и проверьте `adb devices`. Локальный test runner в Docker на Fedora использует `network_mode: host` и `ADB_SERVER_SOCKET=tcp:127.0.0.1:5037`. Передача `adb reverse tcp:8000 tcp:<случайный порт>` создаёт туннель **с устройства на тестовый API**; инструментальные тесты получают `http://127.0.0.1:8000/` через Gradle runner argument.

```bash
RUN_ANDROID_DEVICE_TESTS=1 bash lab2/scripts/run-lab2.sh
```

Если `RUN_ANDROID_DEVICE_TESTS` не равен `1`, device suite честно помечается `skipped` в отчётах — три существующих теста *не считаются запущенными*. Для GitLab потребуется runner, которому доступно реальное устройство/эмулятор и ADB server; Docker-in-Docker runner без USB/ADB их не выполнит. В таком случае исполнение на физическом устройстве демонстрируется локально командой выше (или через выделенный GitLab runner). Не направляйте Android приложение на production URL.

При использовании **эмулятора** также можно отдельно исполнить Android integration с `./gradlew :data:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.studymate.baseUrl=http://10.0.2.2:<host_port>/` на машине с Android SDK.

### Только Python integration/E2E (без unit)

Для отладки можно поднять стенд вручную. Это не заменяет обязательный полный прогон:

```bash
export COMPOSE_PROJECT_NAME=studymate_debug_$USER
DC='lab2/docker-compose.test.yml'
docker compose -f "$DC" up -d --build --wait postgres api
docker compose -f "$DC" run --build --rm tests python -m pytest -m integration tests/integration -v
docker compose -f "$DC" run --rm tests python -m pytest -m e2e tests/e2e -v
docker compose -f "$DC" down -v --remove-orphans
```

Внутри каждого CI-прогона один и тот же integration suite выполняется **дважды на одном стенде** (JUnit `integration-1.xml`, `integration-2.xml`), и результаты обоих прогонов должны быть одинаковыми по статусам. Повторный `run-lab2.sh` каждый раз создаёт тот же пустой набор таблиц из Alembic, но в новом независимом PostgreSQL. Успешная изоляция проверяется тестами на конфликты и доступ других пользователей; для демонстрации запускайте команду дважды и параллельно в двух терминалах.

## CI/CD

В корне `.gitlab-ci.yml` запускает **один обязательный тестовый job**, внутри которого три логические последовательные стадии. Этот способ позволяет гарантировать заполнение JUnit `skipped` для неисполненных стадий даже при ошибке более ранней стадии. GitLab Runner клонирует репозиторий, а Docker runner images получают его через volume bind. GitLab требует `privileged=true` для Docker-in-Docker. Зависимости Python/Android SDK/Gradle ставятся внутри Docker images, а не используются с рабочей машины.

Artifacts **`when: always`**: JUnit XML, Allure results/HTML, JaCoCo. GitLab `cache` сохраняет Allure history для trend view. Для device integration настройте переменную CI/CD `RUN_ANDROID_DEVICE_TESTS=1` и Android-capable runner (без доступного устройства оставьте `0` и покажите устройство на защите).

## Повторение сценария через curl + перехват пакетов

Из корня проекта:

```bash
bash lab2/scripts/capture-traffic.sh
```

Скрипт запускает чистый stand + `tcpdump -i any` на сетевом пространстве API, а затем из Docker test client выполняет `curl` запросы: register/login/me, initial sync (тема, задача, решение), pull, task update, topic delete, pull, refresh/logout. Результат: `lab2/reports/lab2-e2e.pcap`. Посмотреть в Wireshark:

```text
tcp.port == 8000 && http
```

Поскольку используется HTTP, в файле есть тестовые credentials и токены: **не используйте реальные пароли и не публикуйте PCAP в общий доступ**. Порт 8000 внутри Compose — порт контейнера, а не случайный опубликованный порт.

## Исправление, найденное при статической проверке исходного архива

В исходном сервере `app/repositories/errors.py` содержал объявление `class RepositoryConflictError(Exception):` **без тела**, из-за чего Python выдавал `IndentationError` при импорте auth-репозиториев. Добавлен docstring в тело класса (без изменения семантики).

## Статус проверки в окружении подготовки

Исходники и тесты подготовлены, но здесь нет Docker daemon, Android SDK/ADB и PostgreSQL runtime, а установка отсутствующих Python пакетов из интернета недоступна. Поэтому **полный реальный прогон, запуск на физическом устройстве, PCAP и подтверждение зелёного GitLab pipeline здесь не выполнены**. Скрипты воспроизводят нужные действия на оснащённой машине; итоговые результаты необходимо получить реальным запуском. Наличие тестового кода и конфигурации CI не является доказательством успешного теста.

### Open an Allure report on Fedora

Generate a report after a test run with `sudo make report`, then run
`make report-open` **without sudo** to start a localhost-only HTTP server and
open the report in your browser. `make report-serve` starts the same server
without launching a browser. Stop the server with Ctrl+C. Opening
`lab2/reports/allure-report/index.html` via `file://` can result in a spinner
because JavaScript cannot reliably load the companion JSON files.

The report builder imports Gradle's JUnit XML results (`unit-TEST*.xml` and
`android-device-*.xml`) from `lab2/reports/junit/` into Allure in addition
to native Python Allure results. An existing Android run can be imported by
running `sudo make report` again, without rerunning the phone tests, provided
its XML files are still in `lab2/reports/junit/`.
