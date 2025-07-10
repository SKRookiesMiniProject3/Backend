# pandas 2.2.2 는 공식적으로 3.13 을 지원안함. 3.12로 다운그레이드
FROM python:3.12-slim-bookworm


WORKDIR /app

# 의존성 패키지 설치.
RUN apt-get update && apt-get install -y --no-install-recommends \
    default-libmysqlclient-dev \
    build-essential \
    && rm -rf /var/lib/apt/lists/*

# Flask 의존성 설치
COPY requirements.txt .

RUN pip install --no-cache-dir -U -r requirements.txt


# 애플리케이션 코드 복사
COPY . .

# 디렉토리 미리 생성
RUN mkdir -p report/valid report/invalid report/attack


EXPOSE 5000

# 서버 실행
CMD ["gunicorn", "--bind", "0.0.0.0:5000", "main:app"]