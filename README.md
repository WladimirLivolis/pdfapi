# PDF API

A production-ready Spring Boot API for working with PDF files. Built with security, scalability, and best practices in mind.

Based on [iText library][itext] | Java 21 | Spring Boot 3.5.6

## Features

- ✅ **5 PDF Operations**: Merge, Split, Extract, Remove pages, Image to PDF conversion
- 🔐 **Secure**: HTTP Basic Auth, BCrypt passwords, CORS, Security headers
- 🛡️ **Rate Limited**: Protection against abuse (3-10 requests/minute)
- ✅ **Validated**: Input validation for file types, sizes, and parameters
- 📊 **Observable**: Health checks, metrics via Spring Actuator
- 🚀 **Modern**: Java 21, REST API with proper HTTP status codes

---

## 🚀 Quick Start

### 1. Prerequisites

- Java 21+
- Maven 3.9+

### 2. Environment Setup

**Copy the example environment file:**
```bash
cp .env.example .env
```

**Edit `.env` and set your secure passwords:**
```env
SECURITY_USER_PASSWORD=your_secure_user_password
SECURITY_ADMIN_PASSWORD=your_secure_admin_password
```

⚠️ **IMPORTANT**: Never commit the `.env` file to git. It's already in `.gitignore`.

### 3. Run the Application

```bash
./mvnw spring-boot:run
```

The API will be available at `http://localhost:8080`

### 4. Test Authentication

```bash
# Health check (public endpoint)
curl http://localhost:8080/actuator/health

# Merge PDFs (requires authentication)
curl -u user:your_secure_user_password \
  -F "file=@file1.pdf" \
  -F "file=@file2.pdf" \
  http://localhost:8080/pdfapi/merge \
  --output merged.pdf
```

---

## 🔐 Security

### Authentication

All `/pdfapi/**` endpoints require HTTP Basic Authentication.

**Default users** (configure via `.env`):
- **user**: Regular user with `USER` role
- **admin**: Admin user with `USER` and `ADMIN` roles

**Public endpoints** (no auth required):
- `/actuator/health`
- `/swagger-ui/**`
- `/v3/api-docs/**`

### Environment Variables

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `SECURITY_USER_PASSWORD` | ✅ Yes | - | Password for the user account |
| `SECURITY_ADMIN_PASSWORD` | ✅ Yes | - | Password for the admin account |
| `SECURITY_USER_USERNAME` | No | `user` | Username for user account |
| `SECURITY_ADMIN_USERNAME` | No | `admin` | Username for admin account |
| `PDF_OUTPUT_FOLDER` | No | `./output/` | Output folder for PDFs |

---

## 📖 API Operations

All endpoints require authentication. Add `-u username:password` to your requests.

### 1. Merge PDFs

Combine multiple PDF files into one.

**Endpoint:** `POST /pdfapi/merge`
**Rate Limit:** 3 requests/minute

**Parameters:**
- `file` (multipart): 2 or more PDF files

**Example:**
```bash
curl -u user:password \
  -F "file=@file1.pdf" \
  -F "file=@file2.pdf" \
  http://localhost:8080/pdfapi/merge \
  --output merged.pdf
```

---

### 2. Split PDF

Split a PDF into multiple files based on page count.

**Endpoint:** `POST /pdfapi/split`
**Rate Limit:** 3 requests/minute

**Parameters:**
- `file` (multipart): PDF file to split
- `maxPageCount` (integer): Maximum pages per output file

**Example:**
```bash
curl -u user:password \
  -F "file=@document.pdf" \
  -F "maxPageCount=5" \
  http://localhost:8080/pdfapi/split
```

**Response:**
```json
[
  {
    "status": "success",
    "message": "PDF split successfully",
    "fileName": "splitDocument_20251021_1.pdf",
    "fileSizeBytes": 51234,
    "pageCount": 5
  }
]
```

---

### 3. Extract Pages

Extract a specific range of pages from a PDF.

**Endpoint:** `POST /pdfapi/extract`
**Rate Limit:** 10 requests/minute

**Parameters:**
- `file` (multipart): Source PDF file
- `startPage` (integer): First page to extract (1-indexed)
- `endPage` (integer): Last page to extract (1-indexed)

**Example:**
```bash
curl -u user:password \
  -F "file=@document.pdf" \
  -F "startPage=5" \
  -F "endPage=10" \
  http://localhost:8080/pdfapi/extract \
  --output extracted.pdf
```

---

### 4. Remove Pages

Remove specific pages from a PDF.

**Endpoint:** `POST /pdfapi/remove`
**Rate Limit:** 10 requests/minute

**Parameters:**
- `file` (multipart): Source PDF file
- `page` (integer, multiple): Page numbers to remove (1-indexed)

**Example:**
```bash
curl -u user:password \
  -F "file=@document.pdf" \
  -F "page=2" \
  -F "page=5" \
  -F "page=7" \
  http://localhost:8080/pdfapi/remove \
  --output result.pdf
```

---

### 5. Convert Images to PDF

Convert image files (PNG, JPG, etc.) to PDF format.

**Endpoint:** `POST /pdfapi/convertImageToPDF`
**Rate Limit:** 3 requests/minute

**Parameters:**
- `file` (multipart): One or more image files

**Example:**
```bash
curl -u user:password \
  -F "file=@image1.png" \
  -F "file=@image2.jpg" \
  http://localhost:8080/pdfapi/convertImageToPDF
```

---

## 🛡️ Rate Limiting

To protect against abuse, the API enforces rate limits:

| Endpoint | Limit |
|----------|-------|
| merge, split, convertImageToPDF | 3 requests/minute |
| extract, remove | 10 requests/minute |

**When limit exceeded:**
```json
{
  "status": "error",
  "message": "Too many requests. Please try again later.",
  "error": "RATE_LIMIT_EXCEEDED",
  "timestamp": "2025-10-21T01:11:08"
}
```

HTTP Status: `429 Too Many Requests`

---

## 📊 Monitoring

### Health Check
```bash
curl http://localhost:8080/actuator/health
```

### Metrics (requires authentication)
```bash
curl -u admin:password http://localhost:8080/actuator/metrics
curl -u admin:password http://localhost:8080/actuator/ratelimiters
```

---

## 🧪 Testing

Run all tests:
```bash
./mvnw test
```

Run with coverage:
```bash
./mvnw test jacoco:report
```

---

## 📝 License

This project uses the [iText library][itext] for PDF manipulation.

   [itext]: <http://itextpdf.com/en>
