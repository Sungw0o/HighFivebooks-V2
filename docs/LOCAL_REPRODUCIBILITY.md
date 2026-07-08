# Local Reproducibility

이 문서는 다른 컴퓨터에서 HighFiveBooks V2 로컬 통합 환경을 같은 방식으로 띄우기 위한 절차다.

## 필요한 것

- Docker Desktop
- Git
- Java 21
- Node.js 20 이상
- PowerShell

## 1. 저장소 클론

```powershell
git clone https://github.com/Sungw0o/HighFivebooks-V2.git
cd HighFivebooks-V2
```

## 2. 환경 변수 생성

```powershell
copy .env.example .env
```

로컬 기본 실행은 `.env.example` 값으로 가능하다. 외부 API가 필요한 기능은 `.env`에 키를 채운다.

```text
ALADIN_TTB_KEY=
TOSS_CLIENT_KEY=
TOSS_SECRET_KEY=
KAKAO_API_KEY=
GEMINI_API_KEY=
PAYCO_CLIENT_ID=
PAYCO_CLIENT_SECRET=
MAIL_USERNAME=
MAIL_PASSWORD=
```

## 3. 인프라만 실행

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\local-up.ps1 -InfraOnly
```

실행되는 인프라:

```text
MySQL
Redis
RabbitMQ
Elasticsearch + Nori
MinIO
```

## 4. 전체 MSA 실행

각 서비스 Dockerfile은 `target/*.jar`를 복사한다. 따라서 새 컴퓨터에서는 먼저 jar를 빌드해야 한다.

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\local-up.ps1
```

내부적으로 다음을 수행한다.

1. `.env`가 없으면 `.env.example`에서 생성
2. 5개 백엔드 서비스 Maven package
3. `docker compose --profile apps up -d --build`
4. `docker compose ps` 출력

이미 jar 빌드가 되어 있으면 다음처럼 빠르게 재기동할 수 있다.

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\local-up.ps1 -SkipBuild
```

## 5. 대용량 도서 데이터 복원

도서 덤프 파일은 Git에 포함하지 않는다. 다른 컴퓨터에서는 덤프 파일을 받은 뒤 아래 둘 중 하나로 실행한다.

권장 위치:

```text
HighFivebooks-V2\dumps\highfive_book.sql.gz
```

실행:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\import-book-dump.ps1
```

다른 경로에 있다면 명시한다.

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\import-book-dump.ps1 -DumpPath C:\path\to\highfive_book.sql.gz
```

복원 후 script는 `book` 테이블 count를 출력한다.

## 6. 접속 포트

```text
member-server   http://localhost:9001
book-server     http://localhost:9002
coupon-server   http://localhost:9004
payment-server  http://localhost:9005
order-server    http://localhost:9006
RabbitMQ UI     http://localhost:15672
MinIO Console   http://localhost:9007
Elasticsearch   http://localhost:9200
```

## 7. Storefront 실행

```powershell
cd apps/storefront
npm ci
npm run dev
```

백엔드 연결 모드로 쓸 때는 `apps/storefront/.env` 또는 실행 환경에 API base URL을 맞춘다.

```text
VITE_API_ADAPTER=real
VITE_API_BASE_URL=http://localhost:9006
VITE_TOSS_CLIENT_KEY=
```

## 현재 재현성 상태

이미 준비된 것:

- 루트 `.env.example`
- Docker Compose 기반 인프라/앱 통합 실행
- 서비스별 local/prod 설정 분리
- 대용량 도서 덤프 import 스크립트
- Maven jar 빌드 스크립트
- 로컬 통합 실행 스크립트

아직 사람이 준비해야 하는 것:

- 외부 API 키
- 대용량 도서 덤프 파일
- Docker Desktop 실행
- Java/Node 설치
- 필요 시 GitHub Container Registry 이미지 접근 권한
