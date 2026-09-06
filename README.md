# Store & Branch Management Service — Klik Indomaret

Backend RESTful API service untuk manajemen gerai retail, kantor cabang, dan wilayah administratif, dilengkapi fitur pencarian toko dengan aturan prioritas Whitelist, soft-delete cabang, serta pencatatan jejak audit (audit log) otomatis.

Proyek ini dibangun menggunakan **Java 17**, **Spring Boot 4**, **PostgreSQL**, dan **Spring Security (JWT)**.

---

## Informasi Kandidat
- **Nama**: Stefanus
- **ID Pelamar**: OTH00060173
- **Posisi**: Backend Developer — Klik Indomaret

---

## Fitur Utama

1. **Pencarian Toko & Whitelist Priority Merge (`GET /api/stores/search`)**
   - Pencarian gerai toko berdasarkan nama provinsi dengan pagination yang efisien.
   - Toko yang berstatus **Whitelist** selalu diprioritaskan di hasil pencarian teratas dan ditandai dengan flag `"whitelisted": true`, tanpa memandang apakah provinsi toko tersebut cocok dengan filter pencarian atau tidak.
   - Toko reguler hasil pencarian provinsi ditampilkan berikutnya dengan `"whitelisted": false`.

2. **Manajemen Cabang & Soft Delete (`/api/branches`)**
   - Operasi CRUD lengkap untuk kantor cabang.
   - Implementasi **Soft Delete** pada endpoint `DELETE /api/branches/{id}`: data tidak dihapus permanen, melainkan statusnya diubah menjadi `is_active = false` dan kolom `deleted_at` diisi timestamp saat ini.
   - Setiap operasi penambahan, pembaruan, maupun penghapusan cabang secara otomatis mencatat riwayat perubahan ke tabel `audit_logs` (merekam user pelaksana, state JSON data lama, dan state JSON data baru).

3. **Manajemen Toko Whitelist (`/api/whitelist-stores`)**
   - Mendaftarkan dan mengelola toko promosi prioritas.
   - Validasi batas kuota berbasis konfigurasi (`app.whitelist.max-store-count: 50`), sehingga jumlah toko whitelist aktif tidak melebihi kuota yang ditentukan.
   - Pengecekan otomatis untuk mencegah duplikasi toko pada daftar whitelist.

4. **Autentikasi Stateless JWT (`POST /api/auth/login`)**
   - Autentikasi berbasis Bearer Token dengan algoritma HMAC-SHA512.
   - Penyimpanan password menggunakan hashing BCrypt.
   - Token menyertakan informasi username dan role (`ADMIN` / `STAFF`) dengan masa aktif 60 menit.

5. **Dokumentasi Terintegrasi OpenAPI 3 / Swagger UI**
   - Dokumentasi interaktif yang memudahkan pengujian langsung dari browser dengan integrasi Bearer Token.

---

## Tech Stack

- **Bahasa**: Java 17 LTS
- **Framework**: Spring Boot 4.x
- **Basis Data**: PostgreSQL 14+ (diuji pada v18)
- **Keamanan**: Spring Security 7 & JJWT (0.12.6)
- **ORM & Migrasi**: Spring Data JPA / Hibernate
- **Dokumentasi API**: Springdoc OpenAPI 2.8.8 (Swagger UI)
- **Build Tool**: Maven Wrapper (`mvnw` / `mvnw.cmd`)

---

## Struktur Database & Relasi

Hierarki data dirancang mencerminkan operasional ritel nyata:
```
Provinces (1) ──< Branches (1) ──< Stores (1) ──< WhitelistStores
                                                
Users (1) ──────< AuditLogs
```

- **Mitigasi Masalah N+1 Query**: Kueri pencarian toko menggunakan klausa `JOIN FETCH` pada relasi `branch` dan `province`, sehingga data hierarki diambil hanya dalam 1 kueri SQL tunggal.
- **Indexing Strategis**: Diterapkan indeks B-Tree pada seluruh foreign key dan kolom filter (`provinces.name`, `branches.province_id`, `stores.branch_id`, `whitelist_stores.store_id`, dll.) untuk menjamin performa saat volume toko mencapai ~20.000 data.

---

## Konfigurasi Aplikasi (`application.yml`)

Seluruh parameter bisnis diatur terpusat di file konfigurasi agar tidak di-hardcode ke dalam kode Java:

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

# Pengaturan Bisnis & Security
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

## Panduan Setup & Menjalankan Aplikasi

### 1. Persiapan Database
Pastikan PostgreSQL sudah aktif pada port default (`5432`), kemudian buat database `indomaret_db`:
```sql
CREATE DATABASE indomaret_db;
```

### 2. Eksekusi Skema & Data Awal (Seed)
Jalankan file DDL skema dan data awal yang tersedia di folder `backend/src/main/resources/db/migration/`:
```bash
psql -U postgres -d indomaret_db -f backend/src/main/resources/db/migration/V1__Initial_Schema.sql
psql -U postgres -d indomaret_db -f backend/src/main/resources/db/migration/V2__Seed_Initial_Data.sql
```
*Catatan: Data awal sudah mencakup 4 provinsi, 4 cabang, 7 toko contoh, 1 toko whitelist aktif, dan 1 akun admin default (`admin` / `password123`).*

### 3. Menjalankan Aplikasi
Pindah ke direktori `backend` dan jalankan melalui Maven Wrapper:

**Linux / macOS:**
```bash
cd backend
./mvnw spring-boot:run
```

**Windows (PowerShell / Command Prompt):**
```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

Setelah log `Started BackendApplication` muncul, aplikasi siap menerima request di:  
👉 **`http://localhost:8080`**

---

## Dokumentasi Interaktif Swagger UI

Swagger UI dapat langsung diakses di browser:  
👉 **`http://localhost:8080/swagger-ui/index.html`**

**Cara Menguji Endpoint Terproteksi di Swagger:**
1. Eksekusi endpoint `POST /api/auth/login` dengan body:
   ```json
   {
     "username": "admin",
     "password": "password123"
   }
   ```
2. Salin isi `token` dari response JSON.
3. Buka endpoint yang ingin diuji (misal: `GET /api/stores/search` atau `GET /api/branches`), klik **Try it out**.
4. Masukkan token pada kolom header **`Authorization`** dengan format `Bearer <token>` (misal: `Bearer eyJhbGci...`).
5. Klik tombol **Execute**.

*(Alternatif: Tersedia juga file `backend/requests.http` untuk pengujian 1-klik langsung melalui ekstensi REST Client di VS Code atau HTTP Client di IntelliJ IDEA).*

---

## Ringkasan Endpoint REST API

| Method | Endpoint | Keterangan | Akses |
|---|---|---|:---:|
| `POST` | `/api/auth/login` | Autentikasi user & penerbitan token JWT | Public |
| `GET` | `/api/stores/search` | Cari toko per provinsi dengan penggabungan Whitelist & pagination | Bearer |
| `GET` | `/api/stores/{id}` | Ambil detail informasi satu gerai toko | Bearer |
| `GET` | `/api/branches` | Ambil daftar seluruh kantor cabang aktif | Bearer |
| `GET` | `/api/branches/{id}` | Ambil detail kantor cabang berdasarkan ID | Bearer |
| `POST` | `/api/branches` | Tambah cabang baru (otomatis catat ke Audit Log) | Bearer |
| `PUT` | `/api/branches/{id}` | Update data cabang (otomatis catat old vs new ke Audit Log) | Bearer |
| `DELETE` | `/api/branches/{id}` | Soft delete cabang (`is_active = false`, catat ke Audit Log) | Bearer |
| `GET` | `/api/whitelist-stores` | Ambil daftar toko whitelist aktif | Bearer |
| `POST` | `/api/whitelist-stores` | Tambah toko ke whitelist (validasi batas kuota maks 50) | Bearer |
| `PUT` | `/api/whitelist-stores/{id}` | Update status aktif/nonaktif toko whitelist | Bearer |
| `DELETE` | `/api/whitelist-stores/{id}` | Hapus toko dari daftar whitelist | Bearer |

---

## Pengujian & Verifikasi

### 1. Automated Unit Tests (JUnit 5 & Mockito)
Aplikasi telah dilengkapi **17 skenario unit test otomatis** yang menguji seluruh lapisan logika bisnis krusial:
- **`StoreServiceTest`**: Validasi pencarian per provinsi, pagination, penggabungan toko Whitelist di posisi teratas (`whitelisted: true`), serta pencegahan duplikasi data toko.
- **`BranchServiceTest`**: Validasi operasi create, update, mekanisme **soft delete** (`is_active = false`, `deleted_at = now`), serta verifikasi pemanggilan `AuditLogService`.
- **`WhitelistStoreServiceTest`**: Validasi penegakan batas kuota konfigurasi (`max-store-count: 50`), proteksi toko ganda (anti-duplicate), toggle status aktif/nonaktif, dan penghapusan whitelist.
- **`AuthServiceTest`**: Validasi autentikasi kredensial, verifikasi password BCrypt, penerbitan JWT token, dan penanganan kegagalan autentikasi.

Jalankan seluruh test suite dengan perintah:
```bash
cd backend
./mvnw test        # Linux / macOS
.\mvnw.cmd test    # Windows
```

### 2. Script Otomatis End-to-End (`test-e2e.ps1`)
Di folder root tersedia script otomatis PowerShell yang menjalankan seluruh skenario pengujian secara berurutan terhadap server yang sedang aktif:
- Login dan perolehan token JWT
- Pencarian toko dan verifikasi penyisipan toko Whitelist
- Update cabang dan verifikasi pencatatan audit log
- Soft delete cabang dan validasi data tidak lagi muncul di pencarian aktif
- Validasi penolakan penambahan toko yang sudah ada di whitelist (anti-duplicate)

Jalankan dengan perintah:
```powershell
.\test-e2e.ps1
```

### 3. Postman Collection
Tersedia file Postman Collection siap pakai di root proyek:
- `indomaret_postman_collection.json`

File ini dapat langsung di-import ke Postman. Seluruh request telah dikelompokkan berdasarkan modul dan sudah mendukung auto-token handling.

---

## Dokumen Lampiran Resmi

Dokumen teknis pelengkap dengan format standar pelaporan resmi (PKP Standard) telah disertakan di root repositori dengan dua format penamaan (sesuai panduan pengumpulan dan kode referensi pelamar):

1. **API Documentation**:
   - `StefanusSaputra_BackendDeveloper_APIDocumentation.pdf` *(Format penamaan resmi tugas)*
   - `Stefanus_OTH00060173_API Documentation.pdf` *(Format dengan ID Pelamar)*
   - *Isi dokumen:* Spesifikasi lengkap setiap endpoint REST API, struktur payload JSON request/response, contoh request/response, dan daftar status code HTTP.

2. **Database Design Documentation**:
   - `StefanusSaputra_BackendDeveloper_DatabaseDesignDocumentation.pdf` *(Format penamaan resmi tugas)*
   - `Stefanus_OTH00060173_Database Design Documentation.pdf` *(Format dengan ID Pelamar)*
   - *Isi dokumen:* Diagram Entity Relationship Diagram (ERD) visual resolusi tinggi, kamus data (*data dictionary*) untuk seluruh 6 tabel, rincian indeks B-Tree, serta skrip DDL SQL.
