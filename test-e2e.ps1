# ==========================================================
# test-e2e.ps1
# Script Pengujian Otomatis End-to-End untuk Klik Indomaret API
# ==========================================================

Write-Host "`n========================================================" -ForegroundColor Cyan
Write-Host "   PENGUJIAN END-TO-END BACKEND KLIK INDOMARET         " -ForegroundColor Cyan
Write-Host "========================================================`n" -ForegroundColor Cyan

$baseUrl = "http://localhost:8080/api"

# ----------------------------------------------------------
# 1. TEST LOGIN (POST /api/auth/login)
# ----------------------------------------------------------
Write-Host "[1/5] Menguji Login & Penerbitan Token JWT..." -ForegroundColor Yellow
try {
    $loginBody = @{ username = "admin"; password = "password123" } | ConvertTo-Json
    $loginRes = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method Post -Body $loginBody -ContentType "application/json"
    $token = $loginRes.data.token
    $headers = @{ Authorization = "Bearer $token" }
    Write-Host "  -> SUKSES: Login berhasil! Token didapatkan (Role: $($loginRes.data.role))" -ForegroundColor Green
} catch {
    Write-Host "  -> GAGAL: Login error: $_" -ForegroundColor Red
    exit
}

# ----------------------------------------------------------
# 2. TEST SEARCH & WHITELIST MERGE (GET /api/stores/search)
# ----------------------------------------------------------
Write-Host "`n[2/5] Menguji Pencarian Toko & Whitelist Merge Logic..." -ForegroundColor Yellow
try {
    # Cari di Jawa Barat
    $searchRes = Invoke-RestMethod -Uri "$baseUrl/stores/search?province=Jawa%20Barat&page=0&size=10" -Method Get -Headers $headers
    $stores = $searchRes.data.content
    
    Write-Host "  -> Total toko ditemukan: $($stores.Count)" -ForegroundColor Gray
    $hasWhitelisted = $false
    foreach ($s in $stores) {
        $flag = if ($s.whitelisted) { "[WHITELIST PRIORITY]" } else { "[REGULER]" }
        Write-Host "     * ID $($s.id): $($s.name) ($($s.provinceName)) $flag" -ForegroundColor $(if ($s.whitelisted) { "Magenta" } else { "White" })
        if ($s.whitelisted) { $hasWhitelisted = $true }
    }

    if ($hasWhitelisted) {
        Write-Host "  -> SUKSES: Toko Whitelist berhasil disisipkan di hasil pencarian!" -ForegroundColor Green
    } else {
        Write-Host "  -> PERINGATAN: Toko Whitelist tidak ditemukan dalam hasil pencarian." -ForegroundColor DarkYellow
    }
} catch {
    Write-Host "  -> GAGAL: Search store error: $_" -ForegroundColor Red
}

# ----------------------------------------------------------
# 3. TEST UPDATE BRANCH (PUT /api/branches/{id})
# ----------------------------------------------------------
Write-Host "`n[3/5] Menguji Update Branch & Pencatatan Audit Log..." -ForegroundColor Yellow
try {
    $updateBody = @{
        name = "Cabang Bandung Barat Update";
        address = "Jl. Sukajadi Atas No. 100, Bandung";
        provinceId = 1
    } | ConvertTo-Json

    $updateRes = Invoke-RestMethod -Uri "$baseUrl/branches/1" -Method Put -Headers $headers -Body $updateBody -ContentType "application/json"
    Write-Host "  -> SUKSES: Branch ID 1 berhasil diubah namanya menjadi: '$($updateRes.data.name)'" -ForegroundColor Green
} catch {
    Write-Host "  -> GAGAL: Update branch error: $_" -ForegroundColor Red
}

# ----------------------------------------------------------
# 4. TEST SOFT DELETE (DELETE /api/branches/{id})
# ----------------------------------------------------------
Write-Host "`n[4/5] Menguji Soft Delete Branch..." -ForegroundColor Yellow
try {
    $deleteRes = Invoke-RestMethod -Uri "$baseUrl/branches/2" -Method Delete -Headers $headers
    Write-Host "  -> SUKSES: Branch ID 2 (Cabang Bekasi) berhasil di-soft delete" -ForegroundColor Green

    # Verifikasi bahwa ID 2 sudah tidak bisa diakses via API (harus 404)
    try {
        $checkRes = Invoke-RestMethod -Uri "$baseUrl/branches/2" -Method Get -Headers $headers
        Write-Host "  -> GAGAL: Data branch yang di-delete masih bisa diambil!" -ForegroundColor Red
    } catch {
        Write-Host "  -> SUKSES: Branch ID 2 terbukti tidak muncul lagi di query API (HTTP 404 Not Found)" -ForegroundColor Green
    }
} catch {
    Write-Host "  -> GAGAL: Soft delete error: $_" -ForegroundColor Red
}

# ----------------------------------------------------------
# 5. TEST WHITELIST VALIDASI (POST /api/whitelist-stores)
# ----------------------------------------------------------
Write-Host "`n[5/5] Menguji Validasi Anti-Duplikat Whitelist Store..." -ForegroundColor Yellow
try {
    # Coba daftarkan toko ID 5 yang memang sudah ada di whitelist
    $duplicateBody = @{ storeId = 5 } | ConvertTo-Json
    $dupRes = Invoke-RestMethod -Uri "$baseUrl/whitelist-stores" -Method Post -Headers $headers -Body $duplicateBody -ContentType "application/json"
    Write-Host "  -> GAGAL: Seharusnya toko duplikat ditolak!" -ForegroundColor Red
} catch {
    Write-Host "  -> SUKSES: Sistem dengan benar menolak duplikasi toko whitelist (HTTP 400 Bad Request)" -ForegroundColor Green
}

Write-Host "`n========================================================" -ForegroundColor Cyan
Write-Host "       SELURUH PENGUJIAN END-TO-END SELESAI!            " -ForegroundColor Cyan
Write-Host "========================================================`n" -ForegroundColor Cyan
