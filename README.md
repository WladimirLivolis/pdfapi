# PDF API

A production-ready Spring Boot API for working with PDF files. Built with security, scalability, and best practices in mind.

Based on [iText library][itext] | Java 21 | Spring Boot 3.5.6

## Features

- ✅ **9 PDF Operations**: Merge, Split, Extract, Remove pages, Image to PDF conversion, Rotate, Info, Metadata, Page Numbers
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

### 6. Rotate Pages

Rotate pages in a PDF by specified degrees.

**Endpoint:** `POST /pdfapi/rotate`
**Rate Limit:** 10 requests/minute

**Parameters:**
- `file` (multipart): Source PDF file
- `rotation` (integer): Rotation angle in degrees (must be multiple of 90)
- `pages` (integer, optional, multiple): Specific page numbers to rotate. If omitted, rotates all pages.

**Example (rotate all pages):**
```bash
curl -u user:password \
  -F "file=@document.pdf" \
  -F "rotation=90" \
  http://localhost:8080/pdfapi/rotate \
  --output rotated.pdf
```

**Example (rotate specific pages):**
```bash
curl -u user:password \
  -F "file=@document.pdf" \
  -F "rotation=180" \
  -F "pages=1" \
  -F "pages=3" \
  -F "pages=5" \
  http://localhost:8080/pdfapi/rotate \
  --output rotated.pdf
```

**Notes:**
- Rotation is relative (added to current rotation)
- Valid rotations: ..., -270, -180, -90, 0, 90, 180, 270, 360, ...
- Rotation is normalized to 0-360 range

---

### 7. PDF Info

Get information about a PDF without downloading it.

**Endpoint:** `POST /pdfapi/info`
**Rate Limit:** 10 requests/minute

**Parameters:**
- `file` (multipart): PDF file to inspect

**Example:**
```bash
curl -u user:password \
  -F "file=@document.pdf" \
  http://localhost:8080/pdfapi/info
```

**Response:**
```json
{
  "status": "success",
  "message": "PDF info retrieved successfully",
  "pageCount": 42,
  "fileSizeBytes": 1048576,
  "pdfVersion": "PDF-1.7",
  "firstPageDimensions": {
    "width": 595.0,
    "height": 842.0,
    "unit": "points"
  },
  "allPagesSameDimension": true,
  "timestamp": "2025-11-04T18:00:00"
}
```

---

### 8. PDF Metadata

Read and update PDF metadata (title, author, subject, keywords, creator).

#### 8.1 Get Metadata

**Endpoint:** `POST /pdfapi/metadata`
**Rate Limit:** 10 requests/minute

**Parameters:**
- `file` (multipart): PDF file to inspect

**Example:**
```bash
curl -u user:password \
  -F "file=@document.pdf" \
  http://localhost:8080/pdfapi/metadata
```

**Response:**
```json
{
  "status": "success",
  "message": "PDF metadata retrieved successfully",
  "title": "My Document",
  "author": "John Doe",
  "subject": "Technical Documentation",
  "keywords": "pdf, documentation",
  "creator": "Microsoft Word",
  "producer": "iText 9.3.0",
  "creationDate": "D:20250101120000",
  "modificationDate": "D:20250104180000",
  "timestamp": "2025-11-04T18:00:00"
}
```

#### 8.2 Update Metadata

**Endpoint:** `PUT /pdfapi/metadata`
**Rate Limit:** 10 requests/minute

**Parameters:**
- `file` (multipart): Source PDF file
- `title` (string, optional): Document title
- `author` (string, optional): Document author
- `subject` (string, optional): Document subject
- `keywords` (string, optional): Document keywords
- `creator` (string, optional): Creator application

**Example:**
```bash
curl -u user:password -X PUT \
  -F "file=@document.pdf" \
  -F "title=Updated Title" \
  -F "author=Jane Smith" \
  -F "subject=Updated Subject" \
  http://localhost:8080/pdfapi/metadata \
  --output updated.pdf
```

**Notes:**
- All metadata fields are optional
- Only provided fields will be updated
- Returns the PDF with updated metadata

---

### 9. Add Page Numbers

Add customizable page numbers to a PDF.

**Endpoint:** `POST /pdfapi/addPageNumbers`
**Rate Limit:** 10 requests/minute

**Parameters:**
- `file` (multipart): Source PDF file
- `position` (string, optional): Position on page. Default: `bottom-center`
  - Valid positions: `top-left`, `top-center`, `top-right`, `bottom-left`, `bottom-center`, `bottom-right`
- `format` (string, optional): Number format with placeholders. Default: `Page {current} of {total}`
  - Placeholders: `{current}`, `{total}`, `{page}` (same as current)
- `startPage` (integer, optional): First page to number (1-indexed). Default: 1
- `endPage` (integer, optional): Last page to number (1-indexed). Default: last page

**Example (default formatting):**
```bash
curl -u user:password \
  -F "file=@document.pdf" \
  http://localhost:8080/pdfapi/addPageNumbers \
  --output numbered.pdf
```

**Example (custom formatting):**
```bash
curl -u user:password \
  -F "file=@document.pdf" \
  -F "position=bottom-right" \
  -F "format={page}/{total}" \
  -F "startPage=1" \
  -F "endPage=10" \
  http://localhost:8080/pdfapi/addPageNumbers \
  --output numbered.pdf
```

**Format Examples:**
- `Page {current} of {total}` → "Page 1 of 10"
- `{page}/{total}` → "1/10"
- `{current}` → "1"
- `- {page} -` → "- 1 -"

---

## 🛡️ Rate Limiting

To protect against abuse, the API enforces rate limits:

| Endpoint | Limit |
|----------|-------|
| merge, split, convertImageToPDF | 3 requests/minute |
| extract, remove, rotate, info, metadata, addPageNumbers | 10 requests/minute |

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
