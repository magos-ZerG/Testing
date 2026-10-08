# StudyMate Server

## Настройки сервера

Настройки сервера описаны в:

- `.env` — значения переменных окружения
- `app/core/config.py` — класс `Settings`, настройки сервера

В .env:

- `DATABASE_URL` — строка подключения к PostgreSQL
- `JWT_SECRET` — секрет для JWT-токенов
- `JWT_ALGORITHM` — алгоритм подписи JWT
- `ACCESS_TOKEN_EXPIRE_MINUTES` — время жизни access token
- `REFRESH_TOKEN_EXPIRE_DAYS` — время жизни refresh token
- `FILE_STORAGE_DIR` — директория для хранения файлов
- `RUN_MIGRATIONS_ON_STARTUP` — запуск миграций при старте приложения

В `app/core/config.py` дополнительно настройки логирования:

- `log_file_path`
- `log_level`
- `log_max_bytes`
- `log_backup_count`

## Запуск сервера

### Запуск в Docker Compose

1. Требуются Docker и Docker Compose.
2. Из корня проекта:

```bash
docker compose up --build -d
```

Сервер доступен по адресу:

```text
http://localhost:8000
```

PostgreSQL доступен на:

```text
localhost:5432
```

### Локальный запуск

1. Требуются Python 3.12+ и PostgreSQL.
2. Необходимо создать виртуальное окружение.
3. Установка зависимостей:

```bash
pip install -r requirements.txt
```

4. Изменить `DATABASE_URL` в .env на желаемое.
5. Запуск сервера:

```bash
uvicorn app.main:app --host 0.0.0.0 --port 8000
```
