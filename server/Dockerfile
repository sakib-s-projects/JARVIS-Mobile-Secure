FROM python:3.12-slim

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt \
    && groupadd --system relay \
    && useradd --system --gid relay --home-dir /app --shell /usr/sbin/nologin relay

COPY --chown=relay:relay app.py .

ENV PORT=8787 \
    PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1

USER relay:relay

CMD ["sh", "-c", "uvicorn app:app --host 0.0.0.0 --port ${PORT} --workers 1"]
