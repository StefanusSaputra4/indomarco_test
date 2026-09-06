# REST API SPECIFICATION & DOCUMENTATION
## Backend Developer Technical Assignment — Klik Indomaret

**Kandidat**: Stefanus  
**ID Pelamar**: OTH00060173  
**Base URL**: `http://localhost:8080/api`  
**Format Komunikasi**: JSON (`application/json`)  
**Autentikasi**: HTTP Bearer Token (JWT)  

---

## 1. Standar Format Respons API

Seluruh respons dari API dikemas secara seragam menggunakan pola wrapper terstandarisasi:

### Respons Sukses Standar
```json
{
  "success": true,
  "message": "Deskripsi pesan sukses",
  "data": { ... },
  "timestamp": "2026-09-06T10:00:00.000"
}
```

### Respons Gagal / Error Standar
```json
{
  "success": false,
  "message": "Deskripsi pesan error",
  "data": null,
  "timestamp": "2026-09-06T10:00:00.000"
}
```

### Respons Paginasi Standar (`PagedResponse<T>`)
```json
{
  "success": true,
  "message": "Pencarian toko berhasil",
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

---

## 2. Rincian Endpoint REST API

### 2.1 Autentikasi

#### `POST /api/auth/login`
Autentikasi pengguna dan penerbitan token JWT.

- **Akses**: Publik
- **Request Body**:
  ```json
  {
    "username": "admin",
    "password": "password123"
  }
  ```
- **Response 200 OK**:
  ```json
  {
    "success": true,
    "message": "Login berhasil",
    "data": {
      "token": "eyJhbGciOiJIUzUxMiJ9...",
      "tokenType": "Bearer",
      "username": "admin",
      "role": "ADMIN",
      "expiresInMs": 3600000
    },
    "timestamp": "2026-09-06T10:00:00.000"
  }
  ```
- **Response 401 Unauthorized**:
  ```json
  {
    "success": false,
    "message": "Username atau password salah!",
    "data": null
  }
  ```

---

### 2.2 Store Management

#### `GET /api/stores/search`
Pencarian gerai toko berdasarkan nama provinsi dengan pagination efisien dan penggabungan toko Whitelist otomatis.

- **Akses**: Terproteksi (Bearer Token)
- **Headers**: `Authorization: Bearer <TOKEN>`
- **Query Parameters**:
  - `province` (String, opsional): Nama provinsi yang dicari (misal: `Jawa Barat`).
  - `page` (Integer, default: `0`): Nomor halaman.
  - `size` (Integer, opsional, default: `20`, max: `100`): Jumlah data per halaman.
- **Logika Whitelist Merge**:
  1. Menarik toko reguler yang berada di cabang di provinsi tujuan.
  2. Menarik seluruh toko yang aktif di tabel `whitelist_stores`.
  3. Toko whitelist yang belum masuk di hasil pencarian disisipkan di posisi prioritas dengan atribut `"whitelisted": true`.
- **Response 200 OK**:
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
        }
      ],
      "page": 0,
      "size": 2,
      "totalElements": 2,
      "totalPages": 1,
      "last": true
    },
    "timestamp": "2026-09-06T10:00:00.000"
  }
  ```

#### `GET /api/stores/{id}`
Mengambil data detail gerai toko berdasarkan ID.

- **Akses**: Terproteksi (Bearer Token)
- **Response 200 OK**:
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
    }
  }
  ```

---

### 2.3 Branch Management

#### `GET /api/branches`
Mengambil daftar seluruh kantor cabang yang berstatus aktif.

- **Akses**: Terproteksi (Bearer Token)
- **Response 200 OK**:
  ```json
  {
    "success": true,
    "message": "Daftar cabang aktif",
    "data": [
      {
        "id": 1,
        "name": "Cabang Bandung",
        "address": "Jl. Soekarno Hatta No. 123, Bandung",
        "provinceId": 1,
        "provinceName": "Jawa Barat",
        "isActive": true
      }
    ]
  }
  ```

#### `PUT /api/branches/{id}`
Memperbarui informasi cabang. Aksi ini secara otomatis mencatat state lama dan baru ke `AuditLog`.

- **Akses**: Terproteksi (Bearer Token)
- **Path Parameter**: `id` (Long) - ID cabang.
- **Request Body**:
  ```json
  {
    "name": "Cabang Bandung Barat",
    "address": "Jl. Sukajadi No. 99, Bandung",
    "provinceId": 1
  }
  ```
- **Response 200 OK**:
  ```json
  {
    "success": true,
    "message": "Cabang berhasil diperbarui",
    "data": {
      "id": 1,
      "name": "Cabang Bandung Barat",
      "address": "Jl. Sukajadi No. 99, Bandung",
      "provinceId": 1,
      "provinceName": "Jawa Barat",
      "isActive": true
    }
  }
  ```

#### `DELETE /api/branches/{id}`
Menghapus cabang secara **Soft Delete** (`is_active = false`, `deleted_at = NOW()`). Aksi ini otomatis dicatat ke `AuditLog`.

- **Akses**: Terproteksi (Bearer Token)
- **Path Parameter**: `id` (Long) - ID cabang.
- **Response 200 OK**:
  ```json
  {
    "success": true,
    "message": "Cabang berhasil dihapus (soft delete)",
    "data": null
  }
  ```

---

### 2.4 Whitelist Store Management

#### `GET /api/whitelist-stores`
Mengambil semua data toko yang saat ini aktif di dalam daftar whitelist.

- **Akses**: Terproteksi (Bearer Token)
- **Response 200 OK**:
  ```json
  {
    "success": true,
    "message": "Daftar toko whitelist aktif",
    "data": [
      {
        "id": 1,
        "storeId": 5,
        "storeName": "Indomaret Tunjungan Plaza",
        "branchName": "Cabang Surabaya",
        "provinceName": "Jawa Timur",
        "isActive": true,
        "createdAt": "2026-09-06T10:00:00.000"
      }
    ]
  }
  ```

#### `POST /api/whitelist-stores`
Mendaftarkan gerai toko ke dalam daftar whitelist. Terdapat validasi batas kuota maksimal dari konfigurasi (`app.whitelist.max-store-count: 50`) dan pencegahan duplikasi.

- **Akses**: Terproteksi (Bearer Token)
- **Request Body**:
  ```json
  {
    "storeId": 2
  }
  ```
- **Response 201 Created**:
  ```json
  {
    "success": true,
    "message": "Toko berhasil ditambahkan ke whitelist",
    "data": {
      "id": 2,
      "storeId": 2,
      "storeName": "Indomaret Dipatiukur",
      "branchName": "Cabang Bandung",
      "provinceName": "Jawa Barat",
      "isActive": true,
      "createdAt": "2026-09-06T10:05:00.000"
    }
  }
  ```
- **Response 400 Bad Request (Duplikat)**:
  ```json
  {
    "success": false,
    "message": "Toko ini sudah terdaftar dalam whitelist aktif!",
    "data": null
  }
  ```

#### `DELETE /api/whitelist-stores/{id}`
Menghapus status whitelist suatu toko.

- **Akses**: Terproteksi (Bearer Token)
- **Path Parameter**: `id` (Long) - ID whitelist.
- **Response 200 OK**:
  ```json
  {
    "success": true,
    "message": "Toko berhasil dihapus dari whitelist",
    "data": null
  }
  ```

---

## 3. Daftar Kode Status HTTP

| Kode Status | Keterangan | Penggunaan |
|---|---|---|
| `200 OK` | Berhasil | Pengambilan data (`GET`), pembaruan (`PUT`), soft-delete (`DELETE`) |
| `201 Created` | Sumber Daya Dibuat | Penambahan data baru (`POST`) |
| `400 Bad Request` | Permintaan Tidak Valid | Kegagalan validasi input, duplikasi whitelist, atau batas kuota terlampaui |
| `401 Unauthorized` | Belum Terotentikasi | Kredensial login salah, token JWT kadaluarsa, atau header tidak disertakan |
| `403 Forbidden` | Dilarang | Akses ditolak oleh filter keamanan |
| `404 Not Found` | Tidak Ditemukan | ID entitas tidak ditemukan atau entitas telah di-soft delete |
| `500 Internal Error` | Kesalahan Server | Galat tak terduga pada server |
