# Klik Indomaret Backend Developer Technical Assignment

RESTful API backend service untuk pencarian toko bertingkat wilayah (Store, Branch, Province), manajemen Whitelist Store, dan pencatatan riwayat perubahan (Audit Log). Dibangun menggunakan **Spring Boot**, **PostgreSQL**, dan **Spring Security (JWT)**.

---

## 📌 Fitur Utama

1. **Search Store by Province with Whitelist Merge**
   - Pencarian toko berdasarkan nama provinsi dengan pagination efisien (`JOIN FETCH` untuk menghindari masalah N+1).
   - **Whitelist Priority Rule**: Toko yang terdaftar dalam whitelist aktif diprioritaskan dan selalu muncul di hasil pencarian, apapun filter provinsinya.
2. **Branch Management & Soft Delete**
   - Update data cabang dengan pencatatan otomatis ke `AuditLog` (mencatat data sebelum vs sesudah perubahan).
   - Soft delete pada cabang (`is_active = false`, `deleted_at = now`) sehingga data historis tetap terjaga dan tidak hilang dari database.
3. **Whitelist Store Management**
   - Pendaftaran toko ke daftar whitelist dengan validasi batas kuota berbasis konfigurasi (`app.whitelist.max-store-count`).
   - Proteksi anti-duplikasi untuk toko yang sudah aktif di whitelist.
4. **Stateless JWT Authentication**
   - Autentikasi berbasis Bearer Token (HMAC-SHA512) dengan masa berlaku terkonfigurasi.
   - Password di-hash menggunakan algoritma **BCrypt**.
5. **Interactive API Documentation**
   - Terintegrasi otomatis dengan OpenAPI 3 & Swagger UI.

---

## 🛠️ Tech Stack & Requirements

| Komponen | Teknologi | Versi |
|---|---|---|
| **Bahasa Pemrograman** | Java | 17 LTS |
| **Framework** | Spring Boot | 4.x / 3.x |
| **Database** | PostgreSQL | 14+ (Tested on v18) |
| **ORM** | Spring Data JPA / Hibernate | Latest |
| **Security** | Spring Security, JJWT | 0.12.6 |
| **API Docs** | Springdoc OpenAPI (Swagger UI) | 2.8.8 |
| **Build Tool** | Apache Maven | 3.9+ |

---

## ⚙️ Konfigurasi Aplikasi (`application.yml`)

Aplikasi mengadopsi prinsip **Configuration-Based Design**, di mana batasan bisnis tidak di-hardcode ke dalam kode Java:

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/indomaret_db
    username: postgres
    password: root
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false

# Business Configuration Properties
app:
  pagination:
    default-page-size: 20
    max-page-size: 100
  whitelist:
    max-store-count: 50
  jwt:
    secret: mySecretKeyForJwtTokenGenerationThatIsLongEnoughForHS256Algorithm2024
    expiration-ms: 3600000 # 1 jam
```

---

## 🚀 Panduan Menjalankan Aplikasi

### 1. Prasyarat
Pastikan PostgreSQL sudah berjalan dan database kosong bernama `indomaret_db` sudah dibuat:
```sql
CREATE DATABASE indomaret_db;
```

### 2. Jalankan Migrasi Database & Seed Data
Jalankan file SQL schema dan seed data yang tersedia di `backend/src/main/resources/db/migration/`:
```powershell
psql -U postgres -d indomaret_db -f backend/src/main/resources/db/migration/V1__Initial_Schema.sql
psql -U postgres -d indomaret_db -f backend/src/main/resources/db/migration/V2__Seed_Initial_Data.sql
```

### 3. Build & Run Server
Masuk ke direktori `backend` dan jalankan:
```powershell
# Di Linux/macOS:
./mvnw spring-boot:run

# Di Windows PowerShell:
.\mvnw.cmd spring-boot:run
```
Server akan aktif di: **`http://localhost:8080`**

---

## 📖 Dokumentasi Swagger UI

Setelah server berjalan, dokumentasi interaktif dapat diakses di:
👉 **`http://localhost:8080/swagger-ui/index.html`**

**Cara Menggunakan Autentikasi di Swagger:**
1. Akses `POST /api/auth/login` dengan akun default:
   - **Username**: `admin`
   - **Password**: `password123`
2. Salin token JWT yang dihasilkan.
3. Klik tombol hijau **Authorize 🔓** di pojok kanan atas Swagger, lalu tempelkan token tersebut.

---

## 🧪 Pengujian API (Testing)

### Pengujian Otomatis End-to-End
Tersedia script PowerShell mandiri di root direktori untuk menguji seluruh skenario bisnis dari awal hingga akhir:
```powershell
.\test-e2e.ps1
```

### Postman Collection
Import file berikut langsung ke aplikasi Postman:
- `indomaret_postman_collection.json`

---

## 📋 Ringkasan Endpoint REST API

| Method | Endpoint | Keterangan | Auth |
|---|---|---|:---:|
| `POST` | `/api/auth/login` | Login user & menerbitkan JWT | Public |
| `GET` | `/api/stores/search` | Cari toko berdasarkan provinsi + Whitelist merge | Bearer |
| `GET` | `/api/stores/{id}` | Ambil detail toko | Bearer |
| `GET` | `/api/branches` | Ambil semua cabang aktif | Bearer |
| `GET` | `/api/branches/{id}` | Ambil detail cabang berdasarkan ID | Bearer |
| `POST` | `/api/branches` | Tambah cabang baru | Bearer |
| `PUT` | `/api/branches/{id}` | Update data cabang + catat Audit Log | Bearer |
| `DELETE`| `/api/branches/{id}` | Soft delete cabang + catat Audit Log | Bearer |
| `GET` | `/api/whitelist-stores` | Ambil semua toko whitelist aktif | Bearer |
| `POST` | `/api/whitelist-stores` | Tambah toko ke whitelist (kuota maks 50) | Bearer |
| `PUT` | `/api/whitelist-stores/{id}` | Toggle status aktif/nonaktif whitelist | Bearer |
| `DELETE`| `/api/whitelist-stores/{id}` | Hapus toko dari whitelist | Bearer |

---

## 🏛️ Arsitektur & Desain Database

```
Provinces (1) ──< Branches (1) ──< Stores (1) ──< WhitelistStores
                                                
Users (1) ──────< AuditLogs
```

- **Soft Delete**: Diterapkan pada tabel `provinces`, `branches`, dan `stores` menggunakan kolom `is_active` dan `deleted_at`.
- **High Performance Indexing**: Dilengkapi indeks pada foreign key dan kolom pencarian (`provinces.name`, `branches.province_id`, `stores.branch_id`, `whitelist_stores.store_id`).
- **N+1 Prevention**: Query JPA dioptimalkan dengan `JOIN FETCH` untuk mengambil Store, Branch, dan Province dalam 1 kali eksekusi SQL.
