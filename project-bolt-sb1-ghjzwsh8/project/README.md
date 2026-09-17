# Distributed File Storage System

A full-stack web application that implements a distributed file storage system supporting multiple storage providers — Local Filesystem, AWS S3, and Google Cloud Storage — with automatic node selection, file replication, and a clean web dashboard.

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Features](#2-features)
3. [Architecture](#3-architecture)
4. [Technologies](#4-technologies)
5. [Folder Structure](#5-folder-structure)
6. [Database Design](#6-database-design)
7. [API Documentation](#7-api-documentation)
8. [Local Setup](#8-local-setup)
9. [MariaDB Setup](#9-mariadb-setup)
10. [AWS S3 Setup](#10-aws-s3-setup)
11. [Google Cloud Storage Setup](#11-google-cloud-storage-setup)
12. [Environment Variables](#12-environment-variables)
13. [Running the Application](#13-running-the-application)
14. [Frontend Usage](#14-frontend-usage)
15. [Docker Setup](#15-docker-setup)
16. [Future Improvements](#16-future-improvements)

---

## 1. Project Overview

The Distributed File Storage System is a college major project that demonstrates a genuinely distributed storage architecture. When a user uploads a file, the system:

1. Validates the file and generates a unique UUID-prefixed filename
2. Calculates a SHA-256 checksum for integrity
3. Selects the best storage node based on available space
4. Stores the file on the selected provider (Local, S3, or GCS)
5. Saves metadata in MariaDB
6. Optionally replicates the file to a second provider for redundancy
7. Returns a detailed upload response

The system is designed so new storage providers can be added by implementing a single interface.

---

## 2. Features

- **Upload files** via drag-and-drop or file picker with real-time progress
- **Download files** with proper content-type and filename headers
- **Delete files** from all storage locations (primary + replicas)
- **Search files** by original filename
- **View file metadata** including storage type, checksum, and status
- **Multiple storage providers**: Local filesystem, AWS S3, Google Cloud Storage
- **Automatic node selection** based on available space
- **File replication** — copies files to a second provider for redundancy
- **Storage dashboard** with stats for each provider
- **Storage node management** view showing status and space usage
- **Path traversal protection** — rejects malicious filenames
- **Duplicate filename safety** — UUID-prefixed physical filenames
- **SHA-256 checksums** for file integrity verification
- **REST API** for all operations with proper HTTP status codes
- **Global exception handling** with consistent JSON error responses
- **Responsive frontend** built with Bootstrap 5

---

## 3. Architecture

```
User
  |
Frontend (HTML/CSS/JS + Bootstrap 5)
  |
Spring Boot REST API
  |
FileController / StorageController
  |
FileService (business logic)
  |
StorageNodeService (node selection + replication)
  |
StorageService (abstraction interface)
  |
+-------------+-------------+
|             |             |
LocalStorage  S3Storage    GCSStorage
Service       Service      Service
  |             |             |
Local FS      AWS S3       Google Cloud
  |             |             |
  +-------------+-------------+
                |
           MariaDB
         (metadata)
```

### Layered Architecture

- **Controller layer**: Thin REST controllers, only handle HTTP
- **Service layer**: All business logic (upload, download, delete, selection, replication)
- **Repository layer**: Database access only (Spring Data JPA)
- **Storage layer**: Abstraction over physical storage providers

### Storage Abstraction

```
StorageService (interface)
       |
       ├── LocalStorageService
       ├── S3StorageService
       └── GCSStorageService
```

To add a new provider, implement `StorageService` and register it in `StorageNodeService`.

---

## 4. Technologies

### Backend
| Technology | Purpose |
|---|---|
| Java 21 | Programming language |
| Spring Boot 3.4 | Application framework |
| Spring MVC | REST controllers |
| Spring Data JPA | Database ORM |
| Hibernate | JPA implementation |
| MariaDB | Relational database |
| AWS SDK v2 | S3 integration |
| Google Cloud Storage SDK | GCS integration |
| Lombok | Boilerplate reduction |
| Bean Validation | Input validation |
| Maven | Build tool |

### Frontend
| Technology | Purpose |
|---|---|
| HTML5 | Page structure |
| CSS3 | Custom styling |
| JavaScript (ES6) | Client logic |
| Bootstrap 5 | Responsive UI framework |
| Fetch API / XMLHttpRequest | API communication |

### Infrastructure
| Technology | Purpose |
|---|---|
| Docker | Containerization |
| Docker Compose | Multi-container orchestration |

---

## 5. Folder Structure

```
DistributedFileStorage/
├── pom.xml
├── Dockerfile
├── docker-compose.yml
├── docker-compose.example.env
├── README.md
├── .gitignore
│
├── src/
│   ├── main/
│   │   ├── java/com/Project/DistributedFileStorage/
│   │   │   ├── DistributedFileStorageApplication.java
│   │   │   ├── config/
│   │   │   │   ├── S3Config.java
│   │   │   │   ├── GCSConfig.java
│   │   │   │   └── StorageConfig.java
│   │   │   ├── controller/
│   │   │   │   ├── FileController.java
│   │   │   │   └── StorageController.java
│   │   │   ├── entity/
│   │   │   │   ├── File.java
│   │   │   │   ├── StorageNode.java
│   │   │   │   └── FileStorageMapping.java
│   │   │   ├── repository/
│   │   │   │   ├── FileRepository.java
│   │   │   │   ├── StorageNodeRepository.java
│   │   │   │   └── FileStorageMappingRepository.java
│   │   │   ├── service/
│   │   │   │   ├── StorageService.java          (interface)
│   │   │   │   ├── LocalStorageService.java
│   │   │   │   ├── S3StorageService.java
│   │   │   │   ├── GCSStorageService.java
│   │   │   │   ├── FileService.java
│   │   │   │   └── StorageNodeService.java
│   │   │   ├── dto/
│   │   │   │   ├── FileResponse.java
│   │   │   │   ├── FileUploadResponse.java
│   │   │   │   ├── StorageNodeResponse.java
│   │   │   │   ├── StorageStatusResponse.java
│   │   │   │   └── ErrorResponse.java
│   │   │   └── exception/
│   │   │       ├── FileNotFoundException.java
│   │   │       ├── StorageException.java
│   │   │       ├── InvalidFileException.java
│   │   │       └── GlobalExceptionHandler.java
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── application-example.properties
│   │       └── static/
│   │           ├── index.html
│   │           ├── css/style.css
│   │           └── js/app.js
│   └── test/
│       ├── java/com/Project/DistributedFileStorage/
│       │   ├── DistributedFileStorageApplicationTests.java
│       │   ├── controller/FileControllerTest.java
│       │   └── service/
│       │       ├── FileServiceTest.java
│       │       ├── LocalStorageServiceTest.java
│       │       └── StorageNodeServiceTest.java
│       └── resources/
│           └── application-test.properties
```

---

## 6. Database Design

### `files` table

| Column | Type | Description |
|---|---|---|
| id | VARCHAR(36) PK | UUID primary key |
| filename | VARCHAR | UUID-prefixed physical filename |
| original_name | VARCHAR | Original uploaded filename |
| file_path | VARCHAR | Full path/key on the provider |
| file_size | BIGINT | Size in bytes |
| file_type | VARCHAR | MIME content type |
| uploaded_at | DATETIME | Upload timestamp |
| storage_type | VARCHAR | LOCAL, S3, or GCS |
| storage_node | VARCHAR | Node name |
| checksum | VARCHAR(64) | SHA-256 hash |
| status | VARCHAR | ACTIVE, DELETED, etc. |

### `storage_nodes` table

| Column | Type | Description |
|---|---|---|
| id | BIGINT PK AUTO | Numeric ID |
| node_name | VARCHAR UNIQUE | Human-readable name |
| storage_type | VARCHAR | LOCAL, S3, GCS |
| endpoint | VARCHAR | URL or path |
| available_space | BIGINT | Bytes available (null = unlimited) |
| used_space | BIGINT | Bytes used |
| status | VARCHAR | ACTIVE, INACTIVE, OFFLINE |
| created_at | DATETIME | Creation timestamp |

### `file_storage_mapping` table

| Column | Type | Description |
|---|---|---|
| id | BIGINT PK AUTO | Numeric ID |
| file_id | VARCHAR(36) FK | References files.id |
| storage_node_id | BIGINT FK | References storage_nodes.id |
| storage_path | VARCHAR | Path/key on this node |
| stored_at | DATETIME | When stored |
| replication_status | VARCHAR | PRIMARY, REPLICA, FAILED |

### Relationships

- `File` 1:N `FileStorageMapping` (one file can have mappings on multiple nodes)
- `StorageNode` 1:N `FileStorageMapping` (one node can store many files)

---

## 7. API Documentation

### Upload a File

```
POST /api/files/upload
Content-Type: multipart/form-data

Parameter: file (MultipartFile)

Response: 201 Created
{
  "success": true,
  "message": "File uploaded successfully",
  "fileId": "uuid-here",
  "filename": "uuid_filename.txt",
  "originalName": "filename.txt",
  "fileSize": 1024,
  "fileType": "text/plain",
  "storageType": "LOCAL",
  "storageNode": "Local-Node-1",
  "checksum": "sha256hash...",
  "status": "ACTIVE",
  "replicationStatus": "NONE",
  "uploadedAt": "2025-01-15T10:30:00"
}
```

### Get All Files

```
GET /api/files

Response: 200 OK
[
  {
    "id": "uuid",
    "originalName": "file.txt",
    "fileSize": 1024,
    "storageType": "LOCAL",
    "status": "ACTIVE",
    ...
  }
]
```

### Get File by ID

```
GET /api/files/{id}

Response: 200 OK
{
  "id": "uuid",
  "originalName": "file.txt",
  ...
}

Error: 404 Not Found
{
  "success": false,
  "message": "File not found with ID: uuid",
  "timestamp": "..."
}
```

### Download a File

```
GET /api/files/download/{id}

Response: 200 OK
Content-Type: <file's content type>
Content-Disposition: attachment; filename="original.txt"
<file bytes>
```

### Delete a File

```
DELETE /api/files/{id}

Response: 200 OK
{
  "success": true,
  "message": "File deleted successfully",
  "fileId": "uuid"
}
```

### Search Files

```
GET /api/files/search?name=resume

Response: 200 OK
[
  {
    "id": "uuid",
    "originalName": "resume.pdf",
    ...
  }
]
```

### Get Storage Nodes

```
GET /api/storage/nodes

Response: 200 OK
[
  {
    "id": 1,
    "nodeName": "Local-Node-1",
    "storageType": "LOCAL",
    "endpoint": "./uploads",
    "availableSpace": 10000000000,
    "usedSpace": 5000,
    "status": "ACTIVE",
    "createdAt": "..."
  }
]
```

### Get Storage System Status

```
GET /api/storage/status

Response: 200 OK
{
  "totalFiles": 10,
  "totalStorageUsed": 50000,
  "localFiles": 5,
  "localStorageUsed": 25000,
  "s3Files": 3,
  "s3StorageUsed": 15000,
  "gcsFiles": 2,
  "gcsStorageUsed": 10000,
  "nodes": [...]
}
```

### Error Responses

All errors follow this format:

```json
{
  "success": false,
  "message": "Description of the error",
  "error": "Error Type",
  "timestamp": "2025-01-15T10:30:00"
}
```

| HTTP Status | Error Type | Cause |
|---|---|---|
| 400 | Invalid File | Empty file, invalid filename, path traversal |
| 404 | File Not Found | File ID does not exist |
| 413 | File Too Large | Exceeds max upload size |
| 500 | Storage Error | Provider failure |
| 500 | Internal Server Error | Unexpected error |

---

## 8. Local Setup

### Prerequisites

- **Java 21** or later (JDK)
- **Maven 3.8** or later
- **MariaDB 10.6** or later (or MySQL 8.0+)

### Steps

1. Clone or download the project
2. Create the database (MariaDB will auto-create it if `createDatabaseIfNotExist=true` is in the URL)
3. Edit `src/main/resources/application.properties` with your database credentials
4. Build and run:

```bash
mvn clean package
java -jar target/DistributedFileStorage-1.0.0.jar
```

Or run directly with Maven:

```bash
mvn spring-boot:run
```

5. Open your browser to `http://localhost:8080`

---

## 9. MariaDB Setup

### Install MariaDB

On Ubuntu/Debian:
```bash
sudo apt install mariadb-server
sudo systemctl start mariadb
```

On macOS (Homebrew):
```bash
brew install mariadb
brew services start mariadb
```

### Create Database

```bash
mysql -u root -p
```

```sql
CREATE DATABASE distributed_file_storage;
CREATE USER 'dfs_user'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON distributed_file_storage.* TO 'dfs_user'@'localhost';
FLUSH PRIVILEGES;
```

### Configure in application.properties

```properties
spring.datasource.url=jdbc:mariadb://localhost:3306/distributed_file_storage
spring.datasource.username=dfs_user
spring.datasource.password=your_password
```

---

## 10. AWS S3 Setup

### Create an S3 Bucket

1. Log in to the AWS Console
2. Go to S3 and create a new bucket
3. Note the bucket name and region

### Set Up Credentials

Use one of these methods (the AWS SDK picks them up automatically):

**Option A: Environment variables**
```bash
export AWS_ACCESS_KEY_ID=your_access_key
export AWS_SECRET_ACCESS_KEY=your_secret_key
export AWS_REGION=us-east-1
```

**Option B: AWS credentials file** (`~/.aws/credentials`)
```ini
[default]
aws_access_key_id = your_access_key
aws_secret_access_key = your_secret_key
```

**Option C: IAM Role** (if running on EC2/ECS)

### Configure in application.properties

```properties
aws.region=us-east-1
aws.s3.bucket=your-bucket-name
storage.enabled-providers=LOCAL,S3
```

---

## 11. Google Cloud Storage Setup

### Create a GCS Bucket

1. Go to the Google Cloud Console
2. Create or select a project
3. Enable the Cloud Storage API
4. Create a bucket

### Set Up Credentials

**Option A: Service account JSON key**
```bash
export GOOGLE_APPLICATION_CREDENTIALS=/path/to/service-account.json
```

**Option B: Application Default Credentials**
```bash
gcloud auth application-default login
```

### Configure in application.properties

```properties
gcp.project-id=your-project-id
gcp.bucket-name=your-bucket-name
storage.enabled-providers=LOCAL,GCS
```

---

## 12. Environment Variables

| Variable | Description | Default |
|---|---|---|
| `SPRING_DATASOURCE_URL` | Database JDBC URL | `jdbc:mariadb://localhost:3306/...` |
| `SPRING_DATASOURCE_USERNAME` | Database username | `root` |
| `SPRING_DATASOURCE_PASSWORD` | Database password | — |
| `STORAGE_LOCAL_PATH` | Local upload directory | `./uploads` |
| `STORAGE_ENABLED_PROVIDERS` | Comma-separated: LOCAL,S3,GCS | `LOCAL` |
| `AWS_REGION` | AWS region | — |
| `AWS_S3_BUCKET` | S3 bucket name | — |
| `AWS_ACCESS_KEY_ID` | AWS access key | — |
| `AWS_SECRET_ACCESS_KEY` | AWS secret key | — |
| `GCP_PROJECT_ID` | Google Cloud project ID | — |
| `GCP_BUCKET_NAME` | GCS bucket name | — |
| `GOOGLE_APPLICATION_CREDENTIALS` | Path to GCP service account JSON | — |

---

## 13. Running the Application

### Local (development)

```bash
mvn spring-boot:run
```

The application starts on `http://localhost:8080`.

### From JAR

```bash
mvn clean package -DskipTests
java -jar target/DistributedFileStorage-1.0.0.jar
```

### With environment variables

```bash
export SPRING_DATASOURCE_URL=jdbc:mariadb://localhost:3306/distributed_file_storage
export SPRING_DATASOURCE_USERNAME=root
export SPRING_DATASOURCE_PASSWORD=your_password
export STORAGE_ENABLED_PROVIDERS=LOCAL,S3,GCS
java -jar target/DistributedFileStorage-1.0.0.jar
```

---

## 14. Frontend Usage

1. **Dashboard**: Shows total files, storage used, and per-provider stats
2. **Upload**: Drag-and-drop or browse to select a file. Real-time progress bar shows upload status
3. **My Files**: Table of all files with download and delete actions. Search bar filters by filename
4. **Storage Nodes**: Cards showing each node's type, status, used space, and available space

The frontend communicates with the backend via the Fetch API and XMLHttpRequest (for upload progress). No page reloads are needed.

---

## 15. Docker Setup

### Quick Start

```bash
# Copy the example env file
cp docker-compose.example.env .env

# Edit .env with your settings
# Then build and run:
docker compose up --build
```

This starts:
- **MariaDB** on port 3306
- **Spring Boot app** on port 8080

### With Cloud Storage

Edit `.env` to enable S3 and/or GCS:

```env
STORAGE_ENABLED_PROVIDERS=LOCAL,S3,GCS
AWS_REGION=us-east-1
AWS_S3_BUCKET=your-bucket
AWS_ACCESS_KEY_ID=your_key
AWS_SECRET_ACCESS_KEY=your_secret
```

Then:
```bash
docker compose up --build
```

### Dockerfile Details

The Dockerfile uses a multi-stage build:
1. **Stage 1**: Maven builds the JAR in a `maven:3.9-eclipse-temurin-21` image
2. **Stage 2**: The JAR runs in a minimal `eclipse-temurin:21-jre` image

### Stopping

```bash
docker compose down
```

To remove volumes (database data):
```bash
docker compose down -v
```

---

## 16. Future Improvements

- **JWT Authentication**: Add Spring Security with JWT tokens for user accounts
- **Chunked uploads**: Support resumable large file uploads
- **Round-robin / weighted load balancing**: More sophisticated node selection algorithms
- **File versioning**: Keep multiple versions of the same file
- **File sharing**: Generate time-limited download links
- **Virus scanning**: Scan uploaded files for malware
- **File compression**: Compress files before storage
- **Monitoring dashboard**: Grafana/Prometheus metrics
- **Multi-region replication**: Replicate across geographic regions
- **WebDAV support**: Mount the storage as a network drive
- **Mobile app**: React Native or Flutter frontend
- **GraphQL API**: Alternative to REST for flexible queries

---

## License

This project is created as a college major project. Feel free to use it for educational purposes.
