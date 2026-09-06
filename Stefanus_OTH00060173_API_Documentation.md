# Klik Indomaret Store & Branch Management API - Technical Documentation
**Version:** 1.0  
**Platform:** Spring Boot 4 / PostgreSQL 18 / Spring Security (JWT)  
**Kandidat:** Stefanus  
**ID Pelamar:** OTH00060173  

---

## 1. Pendahuluan

### 1.1 Tujuan
Dokumen ini menjelaskan spesifikasi teknis dan alur integrasi RESTful API untuk sistem **Klik Indomaret Store & Branch Management**. Sistem ini dirancang untuk memfasilitasi pengelolaan master wilayah (Provinsi dan Cabang), pengelolaan gerai toko retail berskala besar (~20.000 toko), mekanisme prioritas toko Whitelist pada hasil pencarian, fitur soft-delete cabang, serta pencatatan audit log otomatis atas setiap mutasi data.

### 1.2 Definisi & Istilah
| Istilah | Deskripsi |
| :--- | :--- |
| **API** | Application Programming Interface berbasis arsitektur RESTful JSON. |
| **JWT** | JSON Web Token berstandar RFC 7519 yang digunakan sebagai token autentikasi stateless (HMAC-SHA512). |
| **Whitelist Store** | Toko promosi/strategis khusus yang wajib selalu muncul pada urutan teratas hasil pencarian tanpa memandang filter provinsi. |
| **Quota Limit** | Batas maksimal jumlah toko yang dapat didaftarkan ke dalam whitelist secara bersamaan (default: 50 toko, dikonfigurasi via `application.yml`). |
| **Soft Delete** | Penghapusan secara logis dengan mengubah flag `is_active = false` dan mengisi `deleted_at = NOW()` tanpa menghapus baris data fisik di database. |
| **Audit Log** | Pencatatan otomatis ke tabel `audit_logs` saat mutasi data (CREATE, UPDATE, DELETE) terjadi, merekam aktor pengguna, data lama (`old_value`), dan data baru (`new_value`) dalam format JSON. |
| **N+1 Query** | Masalah performa ORM di mana pemanggilan relasi entitas memicu kueri tambahan untuk setiap baris data; dimitigasi dengan `JOIN FETCH`. |

---

## 2. Persiapan Integrasi

Sebelum integrasi, pengguna atau tim pengembang dapat menggunakan acuan konfigurasi berikut:
1. **API Base URL**: `http://localhost:8080/api`
2. **Interactive Documentation (Swagger UI)**: `http://localhost:8080/swagger-ui/index.html`
3. **OpenAPI Specification JSON**: `http://localhost:8080/v3/api-docs`
4. **Kredensial Default**:
   - Username: `admin`
   - Password: `password123`
5. **Autentikasi**: Seluruh endpoint terproteksi wajib menyertakan header HTTP:  
   `Authorization: Bearer <TOKEN_JWT>`

### 2.1 Standar Struktur Respons API
Seluruh respons API dibungkus dalam format standar seragam:

#### Format Respons Sukses
```json
{
  "success": true,
  "message": "Deskripsi status operasi berhasil",
  "data": { ... },
  "timestamp": "2026-09-06T10:00:00.000"
}
```

#### Format Respons Gagal / Error
```json
{
  "success": false,
  "message": "Deskripsi pesan error atau alasan kegagalan",
  "data": null,
  "timestamp": "2026-09-06T10:00:00.000"
}
```

#### Format Respons Paginasi Standar (`PagedResponse<T>`)
```json
{
  "success": true,
  "message": "Pencarian berhasil",
  "data": {
    "content": [ ... ],
    "page": 0,
    "size": 20,
    "totalElements": 150,
    "totalPages": 8,
    "last": false
  },
  "timestamp": "2026-09-06T10:00:00.000"
}
```

### 2.2 Standar Kode Status HTTP
| Kode HTTP | Status | Deskripsi |
| :--- | :--- | :--- |
| **200** | OK | Permintaan berhasil diproses. |
| **201** | Created | Data baru berhasil dibuat ke sistem. |
| **400** | Bad Request | Parameter request tidak valid atau melebihi kuota konfigurasi. |
| **401** | Unauthorized | Token JWT tidak disertakan, kadaluarsa, atau kredensial salah. |
| **404** | Not Found | Entitas atau data yang dicari tidak ditemukan. |
| **409** | Conflict | Terjadi duplikasi data unik (misal toko sudah ada di whitelist). |
| **500** | Internal Server Error | Terjadi kendala tidak terduga pada server/database. |

---

## 3. Authentication

### 3.1 Login API
Digunakan untuk memvalidasi kredensial pengguna dan menerbitkan JSON Web Token (JWT) yang valid selama 60 menit.

**Method:** `POST`  
**Endpoint:** `/api/auth/login`  
**Akses:** Publik  

#### Header Request:
```http
Content-Type: application/json
```

#### Field Description:
| Field | Mandatory | Type | Description |
| :--- | :--- | :--- | :--- |
| `username` | YES | String | Nama akun pengguna terdaftar (contoh: `admin`) |
| `password` | YES | String | Kata sandi akun (contoh: `password123`) |

#### Request Body:
```json
{
  "username": "admin",
  "password": "password123"
}
```

#### Success Response (200 OK):
```json
{
  "success": true,
  "message": "Login berhasil",
  "data": {
    "token": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJhZG1pbiIsInJvbGUiOiJBRE1JTiIsImlhdCI6MTc4ODU5NzYwMCwiZXhwIjoxNzg4NjAxMjAwfQ...",
    "tokenType": "Bearer",
    "username": "admin",
    "role": "ADMIN",
    "expiresInMs": 3600000
  },
  "timestamp": "2026-09-06T10:00:00.000"
}
```

#### Failed Response (401 Unauthorized):
```json
{
  "success": false,
  "message": "Username atau password salah!",
  "data": null,
  "timestamp": "2026-09-06T10:00:00.000"
}
```

---

## 4. Store Management

### 4.1 Search Stores (with Whitelist Merge & Pagination)
Digunakan untuk mencari gerai toko berdasarkan nama provinsi dengan pagination efisien dan penggabungan toko Whitelist otomatis.

**Logika Whitelist Priority Merge:**
1. Mengambil seluruh toko aktif yang berada di bawah cabang provinsi tujuan (menggunakan query `JOIN FETCH` agar bebas dari masalah N+1 Query).
2. Mengambil seluruh toko aktif di tabel `whitelist_stores`.
3. Toko whitelist yang belum masuk pada hasil filter provinsi disisipkan di posisi prioritas teratas dengan atribut `"whitelisted": true`.
4. Toko reguler hasil pencarian provinsi diberi penanda `"whitelisted": false` (atau `true` jika kebetulan memang terdaftar di whitelist).

**Method:** `GET`  
**Endpoint:** `/api/stores/search`  
**Akses:** Terproteksi (Bearer Token)  

#### Header Request:
```http
Authorization: Bearer <TOKEN>
```

#### Field Description (Query Parameters):
| Field | Mandatory | Type | Description |
| :--- | :--- | :--- | :--- |
| `province` | NO | String | Nama provinsi yang dicari (contoh: `Jawa Barat`, case-insensitive) |
| `page` | NO | Integer | Nomor halaman dimulai dari indeks `0` (default: `0`) |
| `size` | NO | Integer | Jumlah data per halaman (default: `20`, maksimum: `100`) |

#### Request Example:
```http
GET /api/stores/search?province=Jawa%20Barat&page=0&size=10 HTTP/1.1
Host: localhost:8080
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
```

#### Success Response (200 OK):
```json
{
  "success": true,
  "message": "Pencarian toko berhasil",
  "data": {
    "content": [
      {
        "id": 5,
        "name": "Indomaret Tunjungan Plaza",
        "address": "Jl. Embong Malang No. 7, Surabaya",
        "branchId": 3,
        "branchName": "Cabang Surabaya",
        "provinceId": 2,
        "provinceName": "Jawa Timur",
        "whitelisted": true
      },
      {
        "id": 1,
        "name": "Indomaret Dago",
        "address": "Jl. Ir. H. Juanda No. 15, Bandung",
        "branchId": 1,
        "branchName": "Cabang Bandung",
        "provinceId": 1,
        "provinceName": "Jawa Barat",
        "whitelisted": false
      },
      {
        "id": 2,
        "name": "Indomaret Pasteur",
        "address": "Jl. Dr. Djunjunan No. 88, Bandung",
        "branchId": 1,
        "branchName": "Cabang Bandung",
        "provinceId": 1,
        "provinceName": "Jawa Barat",
        "whitelisted": false
      }
    ],
    "page": 0,
    "size": 3,
    "totalElements": 3,
    "totalPages": 1,
    "last": true
  },
  "timestamp": "2026-09-06T10:00:00.000"
}
```

---

### 4.2 Get Store Detail by ID
Digunakan untuk mengambil informasi detail dari satu gerai toko berdasarkan ID unik.

**Method:** `GET`  
**Endpoint:** `/api/stores/{id}`  
**Akses:** Terproteksi (Bearer Token)  

#### Header Request:
```http
Authorization: Bearer <TOKEN>
```

#### Field Description (Path Variable):
| Field | Mandatory | Type | Description |
| :--- | :--- | :--- | :--- |
| `id` | YES | Long | ID unik entitas Store |

#### Success Response (200 OK):
```json
{
  "success": true,
  "message": "Data toko ditemukan",
  "data": {
    "id": 1,
    "name": "Indomaret Dago",
    "address": "Jl. Ir. H. Juanda No. 15, Bandung",
    "branchId": 1,
    "branchName": "Cabang Bandung",
    "provinceId": 1,
    "provinceName": "Jawa Barat",
    "whitelisted": false
  },
  "timestamp": "2026-09-06T10:00:00.000"
}
```

#### Failed Response (404 Not Found):
```json
{
  "success": false,
  "message": "Toko dengan id 999 tidak ditemukan",
  "data": null,
  "timestamp": "2026-09-06T10:00:00.000"
}
```

---

## 5. Branch Management

### 5.1 List All Active Branches
Mengambil seluruh daftar cabang yang berstatus aktif (`is_active = true`).

**Method:** `GET`  
**Endpoint:** `/api/branches`  
**Akses:** Terproteksi (Bearer Token)  

#### Header Request:
```http
Authorization: Bearer <TOKEN>
```

#### Success Response (200 OK):
```json
{
  "success": true,
  "message": "Berhasil mengambil data cabang",
  "data": [
    {
      "id": 1,
      "name": "Cabang Bandung",
      "address": "Jl. Soekarno Hatta No. 200, Bandung",
      "provinceId": 1,
      "provinceName": "Jawa Barat",
      "isActive": true
    },
    {
      "id": 2,
      "name": "Cabang Bekasi",
      "address": "Jl. Ahmad Yani No. 10, Bekasi",
      "provinceId": 1,
      "provinceName": "Jawa Barat",
      "isActive": true
    }
  ],
  "timestamp": "2026-09-06T10:00:00.000"
}
```

---

### 5.2 Create New Branch
Menambahkan entitas cabang baru ke sistem. Operasi ini secara otomatis mencatat riwayat ke tabel `audit_logs`.

**Method:** `POST`  
**Endpoint:** `/api/branches`  
**Akses:** Terproteksi (Bearer Token)  

#### Header Request:
```http
Authorization: Bearer <TOKEN>
Content-Type: application/json
```

#### Field Description:
| Field | Mandatory | Type | Description |
| :--- | :--- | :--- | :--- |
| `name` | YES | String | Nama kantor cabang (maksimal 100 karakter, tidak boleh kosong) |
| `provinceId` | YES | Long | ID provinsi tempat cabang berada (harus valid) |
| `address` | NO | String | Alamat fisik kantor cabang (maksimal 255 karakter) |

#### Request Body:
```json
{
  "name": "Cabang Semarang Baru",
  "provinceId": 3,
  "address": "Jl. Pemuda No. 45, Semarang"
}
```

#### Success Response (201 Created):
```json
{
  "success": true,
  "message": "Cabang berhasil dibuat",
  "data": {
    "id": 5,
    "name": "Cabang Semarang Baru",
    "address": "Jl. Pemuda No. 45, Semarang",
    "provinceId": 3,
    "provinceName": "Jawa Tengah",
    "isActive": true
  },
  "timestamp": "2026-09-06T10:00:00.000"
}
```

---

### 5.3 Update Branch
Memperbarui data cabang yang sudah ada. Operasi ini secara otomatis membandingkan data lama dan mencatat data baru ke tabel `audit_logs`.

**Method:** `PUT`  
**Endpoint:** `/api/branches/{id}`  
**Akses:** Terproteksi (Bearer Token)  

#### Header Request:
```http
Authorization: Bearer <TOKEN>
Content-Type: application/json
```

#### Field Description:
| Field | Mandatory | Type | Description |
| :--- | :--- | :--- | :--- |
| `name` | YES | String | Nama kantor cabang |
| `provinceId` | YES | Long | ID provinsi |
| `address` | NO | String | Alamat fisik kantor cabang |

#### Request Body:
```json
{
  "name": "Cabang Bandung Utama",
  "provinceId": 1,
  "address": "Jl. Soekarno Hatta No. 200, Bandung Gedung Baru"
}
```

#### Success Response (200 OK):
```json
{
  "success": true,
  "message": "Cabang berhasil diperbarui",
  "data": {
    "id": 1,
    "name": "Cabang Bandung Utama",
    "address": "Jl. Soekarno Hatta No. 200, Bandung Gedung Baru",
    "provinceId": 1,
    "provinceName": "Jawa Barat",
    "isActive": true
  },
  "timestamp": "2026-09-06T10:00:00.000"
}
```

---

### 5.4 Delete Branch (Soft Delete)
Menonaktifkan entitas cabang secara logis (`is_active = false`, `deleted_at = NOW()`) tanpa menghapus record fisik di database, serta mencatat event `DELETE` ke tabel `audit_logs`.

**Method:** `DELETE`  
**Endpoint:** `/api/branches/{id}`  
**Akses:** Terproteksi (Bearer Token)  

#### Header Request:
```http
Authorization: Bearer <TOKEN>
```

#### Field Description (Path Variable):
| Field | Mandatory | Type | Description |
| :--- | :--- | :--- | :--- |
| `id` | YES | Long | ID cabang yang akan dinonaktifkan |

#### Success Response (200 OK):
```json
{
  "success": true,
  "message": "Cabang berhasil dihapus (soft delete)",
  "data": null,
  "timestamp": "2026-09-06T10:00:00.000"
}
```

---

## 6. Whitelist Store Management

### 6.1 List Active Whitelist Stores
Mengambil seluruh gerai toko yang sedang aktif di tabel `whitelist_stores`.

**Method:** `GET`  
**Endpoint:** `/api/whitelist`  
**Akses:** Terproteksi (Bearer Token)  

#### Header Request:
```http
Authorization: Bearer <TOKEN>
```

#### Success Response (200 OK):
```json
{
  "success": true,
  "message": "Berhasil mengambil data whitelist",
  "data": [
    {
      "id": 1,
      "storeId": 5,
      "storeName": "Indomaret Tunjungan Plaza",
      "storeAddress": "Jl. Embong Malang No. 7, Surabaya",
      "branchName": "Cabang Surabaya",
      "provinceName": "Jawa Timur",
      "isActive": true,
      "createdAt": "2026-09-06T10:00:00.000"
    }
  ],
  "timestamp": "2026-09-06T10:00:00.000"
}
```

---

### 6.2 Add Store to Whitelist (with Quota Validation)
Mendaftarkan toko ke tabel whitelist. Sistem memvalidasi bahwa total toko whitelist aktif belum melampaui batasan kuota konfigurasi (`app.whitelist.max-store-count: 50`).

**Method:** `POST`  
**Endpoint:** `/api/whitelist`  
**Akses:** Terproteksi (Bearer Token)  

#### Header Request:
```http
Authorization: Bearer <TOKEN>
Content-Type: application/json
```

#### Field Description:
| Field | Mandatory | Type | Description |
| :--- | :--- | :--- | :--- |
| `storeId` | YES | Long | ID toko master yang akan dimasukkan ke whitelist |

#### Request Body:
```json
{
  "storeId": 2
}
```

#### Success Response (201 Created):
```json
{
  "success": true,
  "message": "Toko berhasil ditambahkan ke whitelist",
  "data": {
    "id": 2,
    "storeId": 2,
    "storeName": "Indomaret Pasteur",
    "storeAddress": "Jl. Dr. Djunjunan No. 88, Bandung",
    "branchName": "Cabang Bandung",
    "provinceName": "Jawa Barat",
    "isActive": true,
    "createdAt": "2026-09-06T10:00:00.000"
  },
  "timestamp": "2026-09-06T10:00:00.000"
}
```

#### Failed Response - Quota Exceeded (400 Bad Request):
```json
{
  "success": false,
  "message": "Kuota whitelist toko sudah penuh! Maksimal kuota adalah 50 toko.",
  "data": null,
  "timestamp": "2026-09-06T10:00:00.000"
}
```

#### Failed Response - Duplicate (409 Conflict):
```json
{
  "success": false,
  "message": "Toko dengan id 2 sudah terdaftar dalam whitelist!",
  "data": null,
  "timestamp": "2026-09-06T10:00:00.000"
}
```

---

### 6.3 Remove Store from Whitelist
Menghapus toko dari daftar whitelist aktif.

**Method:** `DELETE`  
**Endpoint:** `/api/whitelist/{id}`  
**Akses:** Terproteksi (Bearer Token)  

#### Header Request:
```http
Authorization: Bearer <TOKEN>
```

#### Field Description (Path Variable):
| Field | Mandatory | Type | Description |
| :--- | :--- | :--- | :--- |
| `id` | YES | Long | ID entitas WhitelistStore yang akan dihapus |

#### Success Response (200 OK):
```json
{
  "success": true,
  "message": "Toko berhasil dihapus dari whitelist",
  "data": null,
  "timestamp": "2026-09-06T10:00:00.000"
}
```

---

## 7. Audit Logging System

Sistem secara transparan mencatat rekaman audit ke tabel `audit_logs` pada setiap mutasi cabang (`CREATE`, `UPDATE`, `DELETE`).

### 7.1 Struktur Data Audit Log
| Field | Tipe Data | Deskripsi |
| :--- | :--- | :--- |
| `id` | Bigserial | ID unik rekaman log |
| `user_id` | Bigint | ID pengguna yang melakukan aksi |
| `entity_name` | Varchar(100) | Nama entitas yang dimutasi (`Branch`) |
| `entity_id` | Bigint | ID entitas yang dimutasi |
| `action` | Varchar(50) | Jenis mutasi (`CREATE`, `UPDATE`, `DELETE`) |
| `old_value` | Text (JSON) | Keadaan data sebelum operasi dijalankan (`null` saat CREATE) |
| `new_value` | Text (JSON) | Keadaan data sesudah operasi dijalankan (`null` saat DELETE) |
| `timestamp` | Timestamp | Waktu tepat terjadinya operasi (UTC/WIB) |

### 7.2 Contoh Rekaman Audit Log (Update Branch)
```json
{
  "id": 12,
  "userId": 1,
  "entityName": "Branch",
  "entityId": 1,
  "action": "UPDATE",
  "oldValue": "{\"id\":1,\"name\":\"Cabang Bandung\",\"address\":\"Jl. Soekarno Hatta No. 200, Bandung\",\"provinceId\":1,\"provinceName\":\"Jawa Barat\",\"isActive\":true}",
  "newValue": "{\"id\":1,\"name\":\"Cabang Bandung Utama\",\"address\":\"Jl. Soekarno Hatta No. 200, Bandung Gedung Baru\",\"provinceId\":1,\"provinceName\":\"Jawa Barat\",\"isActive\":true}",
  "timestamp": "2026-09-06T10:15:30.125"
}
```
