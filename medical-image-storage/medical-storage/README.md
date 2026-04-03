# Scalable Medical Image Storage System

A production-ready Spring Boot 3 backend for storing and retrieving hospital medical images (MRI, CT, X-Ray) using **SeaweedFS** distributed object storage and **MySQL** metadata persistence.

---

## Architecture

```
Client
  │
  ▼
Spring Boot REST API (port 8085)
  │              │
  ▼              ▼
SeaweedFS      MySQL 8
(images)    (metadata)
```

---

## Quick Start

### Prerequisites
- Java 17+
- Maven 3.8+
- Docker + Docker Compose (for infrastructure)

### Step 1 – Start infrastructure

```bash
docker-compose up -d
```

This starts:
| Service | Port |
|---------|------|
| MySQL 8 | 3306 |
| SeaweedFS Master | 9333 |
| SeaweedFS Volume | 8080 |

Wait ~15 seconds for MySQL to initialise, then verify:

```bash
# SeaweedFS master health
curl http://localhost:9333/cluster/status

# MySQL
docker exec -it medical-mysql mysql -uroot -proot -e "SHOW DATABASES;"
```

### Step 2 – Configure application (optional)

Edit `src/main/resources/application.yml` to override:
- `spring.datasource.username` / `password`
- `seaweedfs.master-url` / `volume-url`

### Step 3 – Run the application

```bash
mvn spring-boot:run
```

Or build a fat JAR:

```bash
mvn clean package -DskipTests
java -jar target/medical-image-storage-1.0.0.jar
```

The API starts on **http://localhost:8085**

---

## API Reference

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/images/upload` | Upload image (multipart/form-data) |
| `GET` | `/api/images/{id}` | Get metadata + SeaweedFS URL |
| `GET` | `/api/images/download/{id}` | Stream raw image bytes |
| `GET` | `/api/patients/{patientId}/images` | Paginated list for a patient |
| `DELETE` | `/api/images/{id}` | Delete from SeaweedFS + MySQL |

### Swagger UI
Open **http://localhost:8085/swagger-ui.html** to explore and test all endpoints interactively.

### API Docs (JSON)
**http://localhost:8085/api-docs**

---

## Sample cURL Requests

### Upload
```bash
curl -X POST http://localhost:8085/api/images/upload \
  -F "file=@/path/to/brain_mri.jpg" \
  -F "patientId=PAT-20240101-001" \
  -F "imageType=MRI" \
  -F "description=Baseline brain scan"
```

### Get Metadata
```bash
curl http://localhost:8085/api/images/1
```

### Download Binary
```bash
curl -OJ http://localhost:8085/api/images/download/1
```

### List Patient Images
```bash
curl "http://localhost:8085/api/patients/PAT-20240101-001/images?page=0&size=20"
```

### Delete
```bash
curl -X DELETE http://localhost:8085/api/images/1
```

---

## SeaweedFS Manual Setup (without Docker)

If you prefer to run SeaweedFS natively:

```bash
# Download the binary from https://github.com/seaweedfs/seaweedfs/releases
# Then:

# Terminal 1 – Master
weed master -port=9333

# Terminal 2 – Volume node
weed volume -mserver=localhost:9333 -port=8080 -dir=/tmp/seaweedfs-data
```

Verify the master is running:
```bash
curl http://localhost:9333/cluster/status
# Expected: {"IsLeader":true,"Leader":"..."}
```

---

## Postman Collection

Import `postman_collection.json` into Postman. Set the `baseUrl` variable to `http://localhost:8085`.

Run the collection in order (1 → 5) — request 1 automatically captures the `imageId` for subsequent requests.

---

## Project Structure

```
src/main/java/com/medicalstorage/
├── MedicalStorageApplication.java    ← Entry point
├── controller/
│   └── MedicalImageController.java   ← REST endpoints
├── service/
│   └── MedicalImageService.java      ← Business logic
├── repository/
│   └── MedicalImageRepository.java   ← Spring Data JPA
├── entity/
│   ├── MedicalImage.java             ← JPA entity
│   └── ImageType.java                ← MRI / CT / XRAY enum
├── dto/
│   ├── ImageUploadRequest.java
│   ├── ImageResponse.java
│   ├── ErrorResponse.java
│   └── PagedResponse.java
├── config/
│   ├── AppConfig.java                ← RestTemplate bean
│   ├── OpenApiConfig.java            ← Swagger config
│   └── SeaweedFSProperties.java      ← Bound from application.yml
├── exception/
│   ├── GlobalExceptionHandler.java   ← @RestControllerAdvice
│   ├── ImageNotFoundException.java
│   ├── SeaweedFSException.java
│   └── InvalidFileException.java
└── util/
    └── SeaweedFSClient.java          ← SeaweedFS HTTP client
```

---

## Production Checklist

- [ ] Set `spring.jpa.hibernate.ddl-auto=validate` (not `update`)
- [ ] Use environment variables for DB credentials (never hardcode)
- [ ] Enable HTTPS / TLS termination at the load balancer
- [ ] Add Spring Security with JWT for authentication
- [ ] Configure SeaweedFS replication: `seaweedfs.replication=001`
- [ ] Set up log aggregation (ELK / Loki)
- [ ] Add Prometheus metrics via `spring-boot-starter-actuator`
- [ ] Run database migrations with Flyway or Liquibase

---

## Supported File Types

| MIME Type | Description |
|-----------|-------------|
| `image/jpeg` | JPEG scans |
| `image/png` | PNG scans |
| `image/tiff` | High-res TIFF |
| `application/dicom` | DICOM standard format |
| `application/octet-stream` | Generic binary (.dcm) |

Max file size: **100 MB** (configurable in `application.yml`)
