# Klik Indomaret Store & Branch Management System - Database Design Documentation
**Version:** 1.0  
**Platform:** PostgreSQL 18  
**Author:** Stefanus (OTH00060173)  

---

## 1. Pendahuluan

### 1.1 Tujuan
Dokumen ini menjelaskan rancangan arsitektur basis data (database design) untuk sistem Klik Indomaret Store & Branch Management. Basis data ini dirancang untuk menampung master data wilayah (provinsi dan cabang), data operasional gerai retail (~20.000 toko), relasi toko whitelist promosi, penanganan soft delete cabang, serta pencatatan audit log riwayat mutasi data.

### 1.2 Definisi & Istilah
| Istilah | Deskripsi |
| :--- | :--- |
| **RDBMS** | Relational Database Management System (PostgreSQL 14+) |
| **PK (Primary Key)** | Kunci primer unik bertipe `BIGSERIAL` (64-bit integer auto-increment) |
| **FK (Foreign Key)** | Kunci asing penunjuk integritas relasi antar tabel |
| **Index B-Tree** | Struktur indeks pada kolom kunci dan pencarian untuk mempercepat query |
| **Soft Delete** | Penonaktifan record dengan flag `is_active = false` dan tanggal `deleted_at` tanpa menghapus baris data fisik |
| **Audit Trail** | Jejak riwayat perubahan data (CREATE, UPDATE, DELETE) yang mencatat user, snapshot nilai lama, dan nilai baru |

---

## 2. Entity Relationship Diagram (ERD)

Berikut adalah diagram Entity Relationship Diagram (ERD) relasi antar tabel:

![Entity Relationship Diagram (ERD)](ERD.drawio.png)

### 2.1 Penjelasan Relasi Antar Tabel
1. **`provinces` ke `branches` (1 to Many)**: Satu provinsi dapat memiliki banyak kantor cabang wilayah (`branches.province_id` mereferensikan `provinces.id`).
2. **`branches` ke `stores` (1 to Many)**: Satu kantor cabang membawahi banyak gerai toko retail (`stores.branch_id` mereferensikan `branches.id`).
3. **`stores` ke `whitelist_stores` (1 to 1)**: Satu gerai toko hanya dapat didaftarkan maksimal satu kali pada whitelist promosi (`whitelist_stores.store_id` bersifat unik).
4. **`users` ke `audit_logs` (1 to Many)**: Satu akun pengguna dapat melakukan banyak transaksi mutasi data cabang yang tercatat di tabel `audit_logs`.

---

## 3. Spesifikasi Tabel (Data Dictionary)

### 3.1 Tabel `provinces` (Master Provinsi)
Menyimpan master data wilayah provinsi administratif.

| No | Field | Type | Mandatory | Default | Description |
| :---: | :--- | :--- | :---: | :--- | :--- |
| 1 | `id` | BIGSERIAL | YES | Auto-increment | Primary Key |
| 2 | `name` | VARCHAR(100) | YES | - | Nama provinsi (misal: "Jawa Barat") |
| 3 | `code` | VARCHAR(20) | NO | NULL | Kode singkatan unik provinsi (misal: "JB") |
| 4 | `is_active` | BOOLEAN | YES | TRUE | Status aktif provinsi |
| 5 | `deleted_at` | TIMESTAMP | NO | NULL | Waktu penonaktifan (soft delete) |
| 6 | `created_at` | TIMESTAMP | YES | CURRENT_TIMESTAMP | Waktu pembuatan data |
| 7 | `updated_at` | TIMESTAMP | YES | CURRENT_TIMESTAMP | Waktu pembaruan terakhir data |

---

### 3.2 Tabel `branches` (Master Cabang)
Menyimpan data kantor cabang operasional yang membawahi gerai retail.

| No | Field | Type | Mandatory | Default | Description |
| :---: | :--- | :--- | :---: | :--- | :--- |
| 1 | `id` | BIGSERIAL | YES | Auto-increment | Primary Key |
| 2 | `province_id` | BIGINT | YES | - | Foreign Key ke `provinces(id)` |
| 3 | `name` | VARCHAR(100) | YES | - | Nama kantor cabang (misal: "Cabang Bandung") |
| 4 | `address` | VARCHAR(255) | NO | NULL | Alamat kantor cabang |
| 5 | `is_active` | BOOLEAN | YES | TRUE | Flag status aktif cabang |
| 6 | `deleted_at` | TIMESTAMP | NO | NULL | Waktu penonaktifan cabang (soft delete) |
| 7 | `created_at` | TIMESTAMP | YES | CURRENT_TIMESTAMP | Waktu pembuatan cabang |
| 8 | `updated_at` | TIMESTAMP | YES | CURRENT_TIMESTAMP | Waktu pembaruan data cabang |

---

### 3.3 Tabel `stores` (Master Toko)
Menyimpan master data gerai toko retail Indomaret (~20.000 data).

| No | Field | Type | Mandatory | Default | Description |
| :---: | :--- | :--- | :---: | :--- | :--- |
| 1 | `id` | BIGSERIAL | YES | Auto-increment | Primary Key |
| 2 | `branch_id` | BIGINT | YES | - | Foreign Key ke `branches(id)` |
| 3 | `name` | VARCHAR(150) | YES | - | Nama gerai toko (misal: "Indomaret Dago") |
| 4 | `address` | VARCHAR(255) | NO | NULL | Alamat fisik gerai toko |
| 5 | `is_active` | BOOLEAN | YES | TRUE | Status aktif toko |
| 6 | `deleted_at` | TIMESTAMP | NO | NULL | Waktu penonaktifan toko (soft delete) |
| 7 | `created_at` | TIMESTAMP | YES | CURRENT_TIMESTAMP | Waktu pendaftaran toko |
| 8 | `updated_at` | TIMESTAMP | YES | CURRENT_TIMESTAMP | Waktu pembaruan data toko |

---

### 3.4 Tabel `whitelist_stores` (Toko Whitelist)
Menyimpan relasi gerai toko prioritas promosi yang dibatasi kuota.

| No | Field | Type | Mandatory | Default | Description |
| :---: | :--- | :--- | :---: | :--- | :--- |
| 1 | `id` | BIGSERIAL | YES | Auto-increment | Primary Key |
| 2 | `store_id` | BIGINT | YES | - | Foreign Key unik ke `stores(id)` |
| 3 | `is_active` | BOOLEAN | YES | TRUE | Status aktif keanggotaan whitelist |
| 4 | `created_at` | TIMESTAMP | YES | CURRENT_TIMESTAMP | Waktu penambahan ke whitelist |
| 5 | `updated_at` | TIMESTAMP | YES | CURRENT_TIMESTAMP | Waktu pembaruan whitelist |

---

### 3.5 Tabel `users` (Pengguna Sistem)
Menyimpan akun pengguna untuk otentikasi login dan otorisasi API.

| No | Field | Type | Mandatory | Default | Description |
| :---: | :--- | :--- | :---: | :--- | :--- |
| 1 | `id` | BIGSERIAL | YES | Auto-increment | Primary Key |
| 2 | `username` | VARCHAR(100) | YES | - | Username unik (misal: "admin") |
| 3 | `password_hash` | VARCHAR(255) | YES | - | Hash password BCrypt |
| 4 | `role` | VARCHAR(50) | YES | 'STAFF' | Peran pengguna ('ADMIN', 'STAFF') |
| 5 | `is_active` | BOOLEAN | YES | TRUE | Status aktif akun pengguna |
| 6 | `created_at` | TIMESTAMP | YES | CURRENT_TIMESTAMP | Waktu registrasi akun |
| 7 | `updated_at` | TIMESTAMP | YES | CURRENT_TIMESTAMP | Waktu pembaruan akun |

---

### 3.6 Tabel `audit_logs` (Riwayat Perubahan Data)
Menyimpan jejak rekaman setiap mutasi cabang (`CREATE`, `UPDATE`, `DELETE`).

| No | Field | Type | Mandatory | Default | Description |
| :---: | :--- | :--- | :---: | :--- | :--- |
| 1 | `id` | BIGSERIAL | YES | Auto-increment | Primary Key log |
| 2 | `user_id` | BIGINT | NO | NULL | Foreign Key ke `users(id)` pengguna pelaku |
| 3 | `entity_name` | VARCHAR(100) | YES | - | Nama entitas yang dimutasi (`Branch`) |
| 4 | `entity_id` | BIGINT | YES | - | ID baris data yang dimutasi |
| 5 | `action` | VARCHAR(50) | YES | - | Tipe aksi: 'CREATE', 'UPDATE', 'DELETE' |
| 6 | `old_value` | TEXT | NO | NULL | JSON data sebelum perubahan |
| 7 | `new_value` | TEXT | NO | NULL | JSON data setelah perubahan |
| 8 | `timestamp` | TIMESTAMP | YES | CURRENT_TIMESTAMP | Waktu transaksi dilakukan |

---

## 4. Indexing Database

Untuk menjaga performa query pencarian dan join pada volume data toko retail (~20.000 data), diterapkan skema indeks:

| No | Nama Index | Tabel | Kolom | Tipe Indeks | Keterangan |
| :---: | :--- | :--- | :--- | :---: | :--- |
| 1 | `idx_provinces_name` | `provinces` | `name` | B-Tree | Mempercepat pencarian nama provinsi |
| 2 | `idx_provinces_is_active` | `provinces` | `is_active` | B-Tree | Filter cepat provinsi aktif |
| 3 | `idx_branches_province_id` | `branches` | `province_id` | B-Tree | Mempercepat join cabang ke provinsi |
| 4 | `idx_branches_is_active` | `branches` | `is_active` | B-Tree | Filter cabang aktif |
| 5 | `idx_stores_branch_id` | `stores` | `branch_id` | B-Tree | Mempercepat join toko ke cabang |
| 6 | `idx_stores_is_active` | `stores` | `is_active` | B-Tree | Filter toko aktif |
| 7 | `idx_whitelist_store_id` | `whitelist_stores` | `store_id` | B-Tree (Unique) | Mempercepat pengecekan keanggotaan whitelist |
| 8 | `idx_audit_logs_user_id` | `audit_logs` | `user_id` | B-Tree | Mempercepat pencarian log per pengguna |
| 9 | `idx_audit_logs_timestamp`| `audit_logs` | `timestamp` | B-Tree | Pengurutan kronologis riwayat mutasi |

---

## 5. Alur Soft Delete & Audit Log

### 5.1 Alur Soft Delete Cabang
Operasi penonaktifan cabang (`DELETE /api/branches/{id}`) tidak menghapus data secara fisik, melainkan:
1. Membaca record cabang aktif dari database.
2. Mengubah nilai kolom `is_active = false` dan mengisi `deleted_at = CURRENT_TIMESTAMP`.
3. Menyimpan riwayat perubahan ke tabel `audit_logs` dengan aksi `DELETE`, mencatat snapshot data cabang lama pada kolom `old_value`.
4. Toko-toko di bawah cabang non-aktif otomatis terfilter keluar dari hasil pencarian toko aktif.

### 5.2 Format JSON Audit Log
Data perubahan pada kolom `old_value` dan `new_value` dicatat dalam format JSON:
```json
{
  "id": 1,
  "name": "Cabang Bandung Utama",
  "address": "Jl. Soekarno Hatta No. 200, Bandung",
  "provinceId": 1,
  "provinceName": "Jawa Barat",
  "isActive": true
}
```

---

## 6. Script DDL Database (PostgreSQL)

```sql
-- 1. Tabel Master Provinsi
CREATE TABLE provinces (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(20) UNIQUE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    deleted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. Tabel Master Cabang
CREATE TABLE branches (
    id BIGSERIAL PRIMARY KEY,
    province_id BIGINT NOT NULL REFERENCES provinces(id),
    name VARCHAR(100) NOT NULL,
    address VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    deleted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 3. Tabel Master Toko
CREATE TABLE stores (
    id BIGSERIAL PRIMARY KEY,
    branch_id BIGINT NOT NULL REFERENCES branches(id),
    name VARCHAR(150) NOT NULL,
    address VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    deleted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 4. Tabel Whitelist Toko
CREATE TABLE whitelist_stores (
    id BIGSERIAL PRIMARY KEY,
    store_id BIGINT NOT NULL UNIQUE REFERENCES stores(id),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 5. Tabel Pengguna (Otentikasi & Otorisasi)
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'STAFF',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 6. Tabel Audit Log
CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id),
    entity_name VARCHAR(100) NOT NULL,
    entity_id BIGINT NOT NULL,
    action VARCHAR(50) NOT NULL,
    old_value TEXT,
    new_value TEXT,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indeks Performa
CREATE INDEX idx_provinces_name ON provinces(name);
CREATE INDEX idx_provinces_is_active ON provinces(is_active);
CREATE INDEX idx_branches_province_id ON branches(province_id);
CREATE INDEX idx_branches_is_active ON branches(is_active);
CREATE INDEX idx_stores_branch_id ON stores(branch_id);
CREATE INDEX idx_stores_is_active ON stores(is_active);
CREATE INDEX idx_whitelist_store_id ON whitelist_stores(store_id);
CREATE INDEX idx_audit_logs_user_id ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_timestamp ON audit_logs(timestamp);
```
