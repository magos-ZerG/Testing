# ТИОПО — лабораторная работа № 1

## 1. Объект тестирования

Для лабораторной выбран один production-класс:

`domain/src/main/java/com/z23u184/studymate/domain/usecase/CreateTaskUseCase.kt`.

Лабораторный local unit-suite находится в модуле `data`, потому что классическим тестам нужен доступ одновременно к `CreateTaskUseCase` и к настоящим production-реализациям `TopicRepositoryImpl` / `TaskRepositoryImpl`.

Всего ровно 10 тестов:

- 5 классических — `CreateTaskUseCaseClassicTest`;
- 5 London-school — `CreateTaskUseCaseLondonTest`.

## 2. Главное различие двух наборов

### Классические тесты

Классические тесты **не используют fake/mock repository**.

В fixture создаются настоящие production-классы:

- `TopicRepositoryImpl`;
- `TaskRepositoryImpl`;
- `TopicLocalDataSource`;
- `TaskLocalDataSource`;
- настоящий Room DAO/маппинг/транзакции;
- `DefaultTaskValidator`;
- `SessionRepositoryImpl`;
- `DefaultSyncPolicyService` и `SyncAfterMutationUseCase`.

Чтобы тесты не трогали пользовательскую/production БД, подменяется только инфраструктурная граница хранения:

```kotlin
Room.inMemoryDatabaseBuilder(context, StudyMateDatabase::class.java)
```

Это настоящая Room database implementation, но SQLite существует только в памяти процесса теста. После `database.close()` она исчезает. Ни файл production DB, ни данные приложения не используются.

Режим сессии в классическом fixture — `LOCAL`, поэтому сетевой sync не запускается. Внешняя сеть закрыта защитным `ForbiddenNetworkSyncGateway`: если код случайно попытается выполнить network sync, тест сразу упадёт.

### London-school

London-suite, наоборот, изолирует `CreateTaskUseCase` через MockK и проверяет protocol/interactions с collaborators: вызовы, аргументы, количество вызовов и отсутствие downstream-вызовов после ошибок.

## 3. Матрица 10 тестов

| № | Школа | Сценарий | Техника |
|---|---|---|---|
| C1 | классическая | success + trim + реальная запись через `TaskRepositoryImpl` в Room in-memory | классы эквивалентности + переход состояния |
| C2 | классическая | родительского Topic нет в реальной in-memory БД | негативный класс эквивалентности + guard clause |
| C3 | классическая | whitespace-only title | граничное значение после нормализации |
| C4 | классическая | description = 10001 | анализ границ, `max + 1` |
| C5 | классическая | description = 10000 и реально сохраняется | анализ границ, `max` |
| L1 | London | полный успешный protocol collaborators | interaction testing |
| L2 | London | `TopicRepository -> null`, downstream calls отсутствуют | interaction short-circuit |
| L3 | London | title validator -> Failure | interaction short-circuit |
| L4 | London | description validator -> Failure | interaction short-circuit |
| L5 | London | mock `TaskRepository.save` бросает exception; sync запрещён | fault injection + exception path |

L5 выполняет требование задания о тесте, ожидающим результатом которого является Exception.

## 4. AAA, Fixture, Data Builder, Object Mother

Во всех тестах явно выделены `Arrange`, `Act`, `Assert`.

Используются:

- `CreateTaskClassicFixture` — создаёт реальную repository/data/Room цепочку для классического suite;
- `CreateTaskInputBuilder` — Data Builder;
- `TopicMother` — Object Mother;
- `@Before` / `@After` — lifecycle fixture.

## 5. Запуск

Все 10 тестов:

```bash
./gradlew :data:testDebugUnitTest
```

Только 5 классических:

```bash
./gradlew :data:testDebugUnitTest \
  --tests 'com.z23u184.studymate.data.lab.CreateTaskUseCaseClassicTest'
```

Только 5 London-school:

```bash
./gradlew :data:testDebugUnitTest \
  --tests 'com.z23u184.studymate.data.lab.CreateTaskUseCaseLondonTest'
```

Классические тесты являются local JVM tests. Android framework и Room поднимаются Robolectric, поэтому телефон/эмулятор и production БД не нужны.

## 6. Случайный порядок

Оба test-класса используют собственные runners, которые перемешивают методы перед выполнением.

Обычный запуск использует новый seed. Для воспроизводимого порядка:

```bash
./gradlew :data:testDebugUnitTest -Dlab.random.seed=424242
```

Seed печатается в output теста.

## 7. Offline

После однократной загрузки Gradle/Maven и Robolectric artifacts:

```bash
./gradlew --offline :data:testDebugUnitTest
```

Ни classic, ни London suite не требуют production БД или HTTP-сервера. Classic работает с Room in-memory; London — с MockK.

## 8. Процессы

Для `Test` tasks настроено:

```kotlin
maxParallelForks = 1
forkEvery = 0L
```

Следовательно, `testDebugUnitTest` использует один forked JVM test process для обеих test classes. Gradle daemon — отдельный build process и не является процессом на каждый тест.

## 9. Coverage и SonarQube

Coverage ограничен выбранным production-классом `CreateTaskUseCase`.

```bash
./gradlew :data:labCoverage
```

Команда запускает 10 тестов, формирует JaCoCo XML/HTML и печатает line/branch coverage.

Отчёты:

```text
data/build/reports/jacoco/lab/html/index.html
data/build/reports/jacoco/lab/jacoco.xml
```

SonarQube:

```bash
docker compose -f docker-compose.sonar.yml up -d
SONAR_TOKEN='<token>' ./scripts/lab-sonar.sh
```

Sonar импортирует `data/build/reports/jacoco/lab/jacoco.xml`.

## 10. Allure

Raw results всех 10 тестов:

```text
data/build/allure-results
```

Полный сценарий:

```bash
./scripts/lab-allure.sh
```

HTML:

```text
data/build/reports/allure/index.html
```

## 11. Почему production БД не затрагивается

Классический fixture создаёт новую `StudyMateDatabase` исключительно так:

```kotlin
Room.inMemoryDatabaseBuilder(...)
```

Никакого имени файла БД, пути production database или `Room.databaseBuilder(...)` в test fixture нет. Каждый test method получает новую in-memory БД в `@Before`, а в `@After` она закрывается.

При этом repository не подменён: операции `save/getById/getByTopicId` проходят через настоящий `TaskRepositoryImpl`, `TaskLocalDataSource`, сгенерированный Room DAO, mapper и transaction path.
