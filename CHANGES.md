# Изменения относительно предоставленных архивов

## Добавлено

- `.gitlab-ci.yml` — запуск этапов ЛР2 в GitLab CI, публикация JUnit/Allure/JaCoCo artifacts и сохранение истории.
- `.gitignore` — исключены локальные секреты и generated outputs.
- `LAB2.md` — инструкция, карта требований ЛР2, описание тестового стенда и запуска на Android устройстве.
- `lab2/docker-compose.test.yml` — временный PostgreSQL 17, production FastAPI, тестовые контейнеры Python/Android, захват трафика, генерация Allure.
- `lab2/Dockerfile.test-runner`, `lab2/Dockerfile.android-runner`, `lab2/Dockerfile.capture`, `lab2/Dockerfile.report`.
- `lab2/scripts/run-lab2.sh` — оркестрация unit → integration → e2e, повторение integration на том же стенде, изоляция, reset базы, отчёты при падениях.
- `lab2/scripts/report_stage.py` — failed/skipped JUnit + Allure маркеры.
- `lab2/scripts/replay-http.sh` — воспроизведение E2E через curl/jq.
- `lab2/scripts/capture-traffic.sh` — tcpdump PCAP тестовых HTTP-запросов.
- `server/tests/unit/test_sync_request_validation.py` — 8 тестов валидации входных данных.
- `server/tests/integration/test_sql_auth_repository.py` — 3 интеграционных теста auth + PostgreSQL.
- `server/tests/integration/test_sql_sync_repository.py` — 6 интеграционных тестов sync-репозитория/бизнес-логики.
- `server/tests/e2e/test_study_journey.py` — 3 E2E-сценария HTTP ↔ PostgreSQL/file storage.
- `server/tests/conftest.py`, `server/pytest.ini`, `server/requirements-test.txt`, `server/.env.example`.

## Исправлено

- `server/app/repositories/errors.py` — исправлен исходный синтаксически неполный класс `RepositoryConflictError`.
- `mobile-app/data/src/androidTest/res/xml/network_security_config.xml` — разрешён HTTP localhost только в инструментальном test manifest (для `adb reverse`).

## Без изменений

- Сохранены исходные 10 unit-тестов ЛР1, все 3 Android-интеграционных теста и production-логика приложения/сервера (кроме исправления синтаксической ошибки в `errors.py`).

## Не включено по соображениям безопасности

- Исходный `server/.env` (может содержать реальные пароли/секреты) и `mobile-app/local.properties` (локальные SDK-пути). Для настроек сервера добавлен `server/.env.example`. Тестовый compose передаёт собственные изолированные значения.
- `build`, `.gradle`, `__pycache__`, `.pytest_cache` и сгенерированные отчёты. Сами отчёты появятся после реального прогона тестов.

## Выбор этапа из командной строки

- `lab2/scripts/run-lab2.sh` теперь принимает `all` (по умолчанию), `unit`, `integration`, `android-device`, `e2e` и `report`.
- Отдельные integration / Android device / E2E поднимают собственный изолированный API и PostgreSQL, затем обязательно очищают стенд.
- Незапущенные этапы получают `skipped` в JUnit/Allure; отчёт формируется после каждого выбранного этапа, даже при ошибке.
- Режим `report` позволяет повторно собрать HTML Allure без удаления текущих результатов.

## Постоянный Gradle-кэш и Makefile

- `Makefile` — короткие команды для всех этапов, Allure, PCAP и управления кэшем.
- `lab2/docker-compose.test.yml` — общий внешний том `studymate_lab2_gradle_cache` для `/root/.gradle`, изоляция PostgreSQL/API не затрагивается.
- `lab2/scripts/run-lab2.sh` — автоматически создаёт кэш-том при запуске любого этапа; `down --volumes` его не удаляет.
- `lab2/scripts/capture-traffic.sh` — также создаёт внешний том при первом запуске стенда захвата.
- `LAB2.md` — как пользоваться Makefile и постоянным Gradle-кэшем.
