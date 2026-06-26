# LedgerLens 🔍

> AI-powered B2B bank statement auditor — parses statements, auto-categorizes transactions via Gemini AI, and flags fraud anomalies.

LedgerLens is a financial audit tool built for CAs, accountants, and finance teams. Upload a raw bank statement and the tool automatically categorizes every transaction using AI, detects suspicious patterns (duplicate payments, spend spikes, round-number fraud), and generates an audit-ready report — eliminating hours of manual reconciliation.

---

## ✨ Features

- **CSV Statement Ingestion** — Upload raw bank statements and parse transactions into a structured database
- **AI Transaction Categorization** — Gemini AI classifies each transaction (Salary, Vendor Payment, Subscription, Tax, Transfer, etc.)
- **Anomaly Detection Engine** — Rule-based flagging of:
  - Duplicate payments to the same vendor within 48 hours
  - Round-number high-value transactions (potential fraud indicators)
  - Vendor spend spikes exceeding 200% of monthly average
- **Multi-Tenant Architecture** — Per-organization data isolation for B2B clients
- **Audit-Ready Reports** — Export transaction summaries and flagged anomalies as PDF
- **REST API** — Clean, documented endpoints for integration

---

## 🛠️ Tech Stack

| Layer | Technology |
|-------|-----------|
| **Backend** | Spring Boot 3.5, Java 21 |
| **AI** | Spring AI + Google Gemini |
| **Database** | PostgreSQL (JPA / Hibernate) |
| **Frontend** | React + TypeScript |
| **PDF Parsing** | Apache PDFBox |
| **Report Export** | iText |
| **Containerization** | Docker / Docker Compose |
| **Cloud** | GCP Cloud Run |
| **Auth** | JWT + API Keys |

---

## 🚀 Getting Started

### Prerequisites

- Java 21+
- Maven 3.9+
- Docker Desktop
- Node.js 20+ (for frontend)

### 1. Clone the repository

```bash
git clone https://github.com/glossE/Ledgerlens.git
cd Ledgerlens
```

### 2. Start PostgreSQL via Docker

```bash
docker compose up -d
```

This spins up a PostgreSQL container on port `5432`.

### 3. Configure environment

Set your Gemini API key in `application.properties` (or as an environment variable):

```properties
gemini.api.key=YOUR_API_KEY_HERE
```

### 4. Run the backend

```bash
mvn spring-boot:run
```

The API starts on `http://localhost:8080`.

---

## 📡 API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/transactions/upload` | Upload a CSV bank statement for processing |
| `GET` | `/api/transactions` | Retrieve all transactions |
| `GET` | `/api/transactions/flagged` | Retrieve only flagged (anomalous) transactions |
| `GET` | `/api/transactions/category/{category}` | Filter transactions by category |

### Example: Upload a statement

```bash
curl -X POST http://localhost:8080/api/transactions/upload \
  -F "file=@sample_bank_statement.csv"
```

**Response:**

```json
{
  "totalTransactions": 8,
  "flaggedCount": 3,
  "transactions": [
    {
      "id": 1,
      "description": "Salary Credit - June",
      "amount": 85000.00,
      "type": "CREDIT",
      "category": "Salary",
      "isFlagged": false
    }
  ]
}
```

---

## 🏗️ Architecture

```
CSV Upload → Controller → CsvParserService → PostgreSQL
                              ↓
                      GeminiService (AI categorization)
                              ↓
                  AnomalyDetectionService (fraud flagging)
                              ↓
                      Audit Report (PDF export)
```

The application follows a clean layered architecture:

- **Controller** — Handles HTTP requests and responses
- **Service** — Business logic (parsing, AI integration, anomaly detection)
- **Repository** — Database access via Spring Data JPA
- **Model** — JPA entities mapping to database tables

---

## 📁 Project Structure

```
ledgerlens/
├── src/main/java/com/ledgerlens/ledgerlens/
│   ├── controller/      # REST endpoints
│   ├── service/         # Business logic
│   ├── repository/      # Database queries
│   ├── model/           # JPA entities
│   ├── dto/             # Request/response objects
│   └── config/          # App configuration (CORS, etc.)
├── src/main/resources/
│   └── application.properties
├── docker-compose.yml
└── pom.xml
```

---

## 🗺️ Roadmap

- [x] CSV statement parsing
- [x] AI transaction categorization
- [x] Rule-based anomaly detection
- [ ] PDF statement parsing (Apache PDFBox)
- [ ] PDF audit report export (iText)
- [ ] JWT authentication & multi-tenancy
- [ ] React dashboard
- [ ] GCP Cloud Run deployment

---

## 📄 License

This project is licensed under the MIT License.

---

## 👤 Author

**Harsh Chavan**
[LinkedIn](https://linkedin.com/in/harshchavan2003) · [GitHub](https://github.com/glossE)
