# Klik Indomaret Store & Branch Management API - Technical Documentation
**Version:** 1.0  
**Platform:** Klik Indomaret Backend API  
**Author:** Stefanus (OTH00060173)  

---

## 1. Pendahuluan

### 1.1 Tujuan
Dokumen ini menjelaskan spesifikasi teknis dan alur integrasi REST API untuk kebutuhan Sistem Manajemen Toko & Kantor Cabang Klik Indomaret. Sistem ini mencakup pengelolaan kantor cabang wilayah operasional, master data toko retail (~20.000 gerai), pencarian toko dengan filter provinsi dan sorting tanggal pembuatan, prioritas toko whitelist, mekanisme soft-delete cabang, serta pencatatan audit log setiap mutasi data.

### 1.2 Definisi & Istilah
| Istilah | Deskripsi |
| :--- | :--- |
| **API** | Application Programming Interface berbasis RESTful JSON |
| **JWT** | JSON Web Token berstandar RFC 7519 sebagai token otentikasi stateless |
| **Whitelist Store** | Toko promosi/prioritas khusus yang otomatis tampil pada urutan teratas hasil pencarian |
| **Quota Limit** | Batas maksimal jumlah toko yang dapat didaftarkan ke whitelist (default: 50 toko) |
| **Soft Delete** | Penonaktifan data cabang secara logis (is_active = false) tanpa menghapus fisik baris database |
| **Audit Log** | Pencatatan riwayat perubahan data (CREATE, UPDATE, DELETE) pada entitas cabang |

---

## 2. Persiapan Integrasi

Sebelum integrasi, pengguna / developer dapat mengakses konfigurasi berikut:
1. **API Base URL**: `http://localhost:8080/api`
2. **Swagger UI**: `http://localhost:8080/swagger-ui/index.html`
3. **OpenAPI JSON**: `http://localhost:8080/v3/api-docs`
4. **Akun Default**:
   - Username: `admin`
   - Password: `password123`
5. **Header Otentikasi**: Seluruh endpoint terproteksi wajib menyertakan header:  
   `Authorization: Bearer <TOKEN_JWT>`

### 2.1 Standar Respons API
Format respons standar sukses:
```json
{
  "success": true,
  "message": "Deskripsi status operasi berhasil",
  "data": { ... },
  "timestamp": "2026-09-06T10:00:00.000"
}
```

Format respons standar gagal:
```json
{
  "success": false,
  "message": "Deskripsi alasan kegagalan",
  "data": null,
  "timestamp": "2026-09-06T10:00:00.000"
}
```

### 2.2 Kode Status HTTP
| Kode HTTP | Status | Deskripsi |
| :--- | :--- | :--- |
| **200** | OK | Permintaan berhasil diproses |
| **201** | Created | Data baru berhasil disimpan ke sistem |
| **400** | Bad Request | Parameter tidak valid atau melebihi batas kuota |
| **401** | Unauthorized | Token JWT tidak disertakan, tidak valid, atau login gagal |
| **403** | Forbidden | Akun tidak memiliki hak akses yang mencukupi |
| **404** | Not Found | Data yang dicari tidak ditemukan |
| **409** | Conflict | Data sudah ada sebelumnya (duplikasi unik) |
| **500** | Internal Server Error | Terjadi kendala internal server |

---

## 3. Authentication

### 3.1 Login API
Digunakan untuk memvalidasi kredensial pengguna dan mendapatkan access token JWT yang valid selama 60 menit.

**Method:** POST  
**Endpoint:**  
`/api/auth/login`  

**Header Request:**
```json
{
  "Content-Type": "application/json"
}
```

**Field Description:**
| Field | Mandatory | Description |
| :--- | :--- | :--- |
| `username` | YES | Nama akun pengguna terdaftar (contoh: `admin`) |
| `password` | YES | Kata sandi akun (contoh: `password123`) |

**Request Body:**
```json
{
  "username": "admin",
  "password": "password123"
}
```

**Success Response:**
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

**Failed Response:**
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

### 4.1 Search Store by Province
Digunakan untuk mencari daftar toko retail berdasarkan nama provinsi dengan fitur pagination dan sorting created date (asc/desc). Toko yang aktif pada daftar whitelist akan otomatis disematkan pada urutan teratas hasil pencarian.

**Method:** GET  
**Endpoint:**  
`/api/stores/search?province=Jawa%20Barat&page=0&size=10&sortDirection=desc`  

**Header Request:**
```json
{
  "Authorization": "Bearer <TOKEN_JWT>"
}
```

**Field Description:**
| Field | Mandatory | Description |
| :--- | :--- | :--- |
| `province` | NO | Filter nama provinsi toko (contoh: `Jawa Barat`, case-insensitive) |
| `page` | NO | Nomor halaman, dimulai dari `0` (default: `0`) |
| `size` | NO | Jumlah data per halaman (default: `20`, maksimum: `100`) |
| `sortDirection` | NO | Arah urutan created date: `asc` (terlama) atau `desc` (terbaru) (default: `desc`) |

**Success Response:**
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
        "createdAt": "2026-09-06T10:00:00",
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
        "createdAt": "2026-09-06T09:00:00",
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
        "createdAt": "2026-09-06T08:00:00",
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
Digunakan untuk mengambil data lengkap gerai toko berdasarkan ID.

**Method:** GET  
**Endpoint:**  
`/api/stores/{id}`  

**Header Request:**
```json
{
  "Authorization": "Bearer <TOKEN_JWT>"
}
```

**Field Description:**
| Field | Mandatory | Description |
| :--- | :--- | :--- |
| `id` | YES | ID unik toko (path variable) |

**Success Response:**
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
    "createdAt": "2026-09-06T09:00:00",
    "whitelisted": false
  },
  "timestamp": "2026-09-06T10:00:00.000"
}
```

**Failed Response:**
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

### 5.1 List Active Branches
Digunakan untuk mengambil seluruh daftar kantor cabang yang berstatus aktif. Data dapat diurutkan berdasarkan created date (asc/desc).

**Method:** GET  
**Endpoint:**  
`/api/branches?sortDirection=desc`  

**Header Request:**
```json
{
  "Authorization": "Bearer <TOKEN_JWT>"
}
```

**Field Description:**
| Field | Mandatory | Description |
| :--- | :--- | :--- |
| `sortDirection` | NO | Urutan created date: `asc` (terlama) atau `desc` (terbaru) (default: `asc`) |

**Success Response:**
```json
{
  "success": true,
  "message": "Daftar cabang aktif",
  "data": [
    {
      "id": 2,
      "name": "Cabang Bekasi",
      "address": "Jl. Ahmad Yani No. 10, Bekasi",
      "provinceId": 1,
      "provinceName": "Jawa Barat",
      "isActive": true,
      "createdAt": "2026-09-06T09:30:00"
    },
    {
      "id": 1,
      "name": "Cabang Bandung",
      "address": "Jl. Soekarno Hatta No. 200, Bandung",
      "provinceId": 1,
      "provinceName": "Jawa Barat",
      "isActive": true,
      "createdAt": "2026-09-06T08:30:00"
    }
  ],
  "timestamp": "2026-09-06T10:00:00.000"
}
```

---

### 5.2 Create Branch
Digunakan untuk menambahkan kantor cabang baru. Perubahan otomatis dicatat ke audit log.

**Method:** POST  
**Endpoint:**  
`/api/branches`  

**Header Request:**
```json
{
  "Authorization": "Bearer <TOKEN_JWT>",
  "Content-Type": "application/json"
}
```

**Field Description:**
| Field | Mandatory | Description |
| :--- | :--- | :--- |
| `name` | YES | Nama kantor cabang (maksimal 100 karakter) |
| `provinceId` | YES | ID provinsi tempat kantor cabang berada |
| `address` | NO | Alamat fisik kantor cabang (maksimal 255 karakter) |

**Request Body:**
```json
{
  "name": "Cabang Semarang Baru",
  "provinceId": 3,
  "address": "Jl. Pemuda No. 45, Semarang"
}
```

**Success Response:**
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
    "isActive": true,
    "createdAt": "2026-09-06T10:00:00"
  },
  "timestamp": "2026-09-06T10:00:00.000"
}
```

---

### 5.3 Update Branch
Digunakan untuk memperbarui informasi kantor cabang yang sudah ada. Perubahan otomatis dicatat ke audit log.

**Method:** PUT  
**Endpoint:**  
`/api/branches/{id}`  

**Header Request:**
```json
{
  "Authorization": "Bearer <TOKEN_JWT>",
  "Content-Type": "application/json"
}
```

**Field Description:**
| Field | Mandatory | Description |
| :--- | :--- | :--- |
| `name` | YES | Nama kantor cabang |
| `provinceId` | YES | ID provinsi |
| `address` | NO | Alamat kantor cabang |

**Request Body:**
```json
{
  "name": "Cabang Bandung Utama",
  "provinceId": 1,
  "address": "Jl. Soekarno Hatta No. 200, Bandung Gedung Baru"
}
```

**Success Response:**
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
    "isActive": true,
    "createdAt": "2026-09-06T08:30:00"
  },
  "timestamp": "2026-09-06T10:00:00.000"
}
```

---

### 5.4 Delete Branch (Soft Delete)
Digunakan untuk menonaktifkan cabang secara logis (soft delete). Flag `is_active` diubah menjadi `false` dan diisi tanggal `deleted_at`. Perubahan dicatat ke audit log.

**Method:** DELETE  
**Endpoint:**  
`/api/branches/{id}`  

**Header Request:**
```json
{
  "Authorization": "Bearer <TOKEN_JWT>"
}
```

**Field Description:**
| Field | Mandatory | Description |
| :--- | :--- | :--- |
| `id` | YES | ID cabang yang akan dinonaktifkan (path variable) |

**Success Response:**
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

### 6.1 List Whitelist Stores
Digunakan untuk menampilkan seluruh gerai toko yang aktif dalam daftar whitelist prioritas.

**Method:** GET  
**Endpoint:**  
`/api/whitelist`  

**Header Request:**
```json
{
  "Authorization": "Bearer <TOKEN_JWT>"
}
```

**Success Response:**
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

### 6.2 Add Store to Whitelist
Digunakan untuk menambahkan gerai toko ke dalam daftar whitelist. Sistem membatasi jumlah maksimal toko whitelist sesuai konfigurasi (default: 50 toko).

**Method:** POST  
**Endpoint:**  
`/api/whitelist`  

**Header Request:**
```json
{
  "Authorization": "Bearer <TOKEN_JWT>",
  "Content-Type": "application/json"
}
```

**Field Description:**
| Field | Mandatory | Description |
| :--- | :--- | :--- |
| `storeId` | YES | ID toko yang akan didaftarkan ke whitelist |

**Request Body:**
```json
{
  "storeId": 2
}
```

**Success Response:**
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

**Failed Response - Kuota Penuh:**
```json
{
  "success": false,
  "message": "Kuota whitelist toko sudah penuh! Maksimal kuota adalah 50 toko.",
  "data": null,
  "timestamp": "2026-09-06T10:00:00.000"
}
```

**Failed Response - Duplikasi Toko:**
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
Digunakan untuk mengeluarkan toko dari daftar whitelist aktif.

**Method:** DELETE  
**Endpoint:**  
`/api/whitelist/{id}`  

**Header Request:**
```json
{
  "Authorization": "Bearer <TOKEN_JWT>"
}
```

**Field Description:**
| Field | Mandatory | Description |
| :--- | :--- | :--- |
| `id` | YES | ID entitas whitelist yang akan dihapus (path variable) |

**Success Response:**
```json
{
  "success": true,
  "message": "Toko berhasil dihapus dari whitelist",
  "data": null,
  "timestamp": "2026-09-06T10:00:00.000"
}
```

---

## 7. Audit Log

Sistem mencatat riwayat perubahan data secara otomatis ke tabel `audit_logs` pada setiap aksi mutasi cabang (`CREATE`, `UPDATE`, `DELETE`).

### 7.1 Struktur Kolom Audit Log
| Field | Tipe Data | Description |
| :--- | :--- | :--- |
| `id` | Bigserial | ID unik riwayat log |
| `user_id` | Bigint | ID pengguna yang melakukan aksi |
| `entity_name` | Varchar(100) | Nama entitas yang berubah (`Branch`) |
| `entity_id` | Bigint | ID data yang dimutasi |
| `action` | Varchar(50) | Tipe mutasi: `CREATE`, `UPDATE`, `DELETE` |
| `old_value` | Text (JSON) | Nilai sebelum perubahan (`null` saat CREATE) |
| `new_value` | Text (JSON) | Nilai setelah perubahan (`null` saat DELETE) |
| `timestamp` | Timestamp | Waktu operasi dijalankan |

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
