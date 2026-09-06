INSERT INTO users (id, username, password_hash, role, is_active) VALUES
(1, 'admin', '$2a$10$wT2HlVbC0R8uN32r9eWc/uN1Jm4iK5bA1z7E6bF2wP8yZ3q9O.Xmy', 'ADMIN', true),
(2, 'staff', '$2a$10$wT2HlVbC0R8uN32r9eWc/uN1Jm4iK5bA1z7E6bF2wP8yZ3q9O.Xmy', 'STAFF', true);

INSERT INTO provinces (id, name, code, is_active) VALUES
(1, 'Jawa Barat', 'JB', true),
(2, 'Jawa Timur', 'JT', true),
(3, 'DKI Jakarta', 'JKT', true),
(4, 'Bali', 'BALI', true);

INSERT INTO branches (id, province_id, name, address, is_active, created_at) VALUES
(1, 1, 'Cabang Bandung', 'Jl. Soekarno Hatta No. 123, Bandung', true, '2026-01-01 08:00:00'),
(2, 1, 'Cabang Bekasi', 'Jl. Ahmad Yani No. 45, Bekasi', true, '2026-02-01 08:00:00'),
(3, 2, 'Cabang Surabaya', 'Jl. Basuki Rahmat No. 88, Surabaya', true, '2026-03-01 08:00:00'),
(4, 4, 'Cabang Denpasar', 'Jl. Gatot Subroto No. 10, Denpasar', true, '2026-04-01 08:00:00');

INSERT INTO stores (id, branch_id, name, address, is_active, created_at) VALUES
(1, 1, 'Indomaret Dago', 'Jl. Ir. H. Juanda No. 15, Bandung', true, '2026-01-10 08:00:00'),
(2, 1, 'Indomaret Dipatiukur', 'Jl. Dipatiukur No. 40, Bandung', true, '2026-02-15 09:30:00'),
(3, 1, 'Indomaret Riau Junction', 'Jl. L. L. R.E. Martadinata No. 80, Bandung', true, '2026-03-20 10:15:00'),
(4, 2, 'Indomaret Summarecon Bekasi', 'Jl. Bulevar Ahmad Yani, Bekasi', true, '2026-04-25 11:00:00'),
(5, 3, 'Indomaret Tunjungan Plaza', 'Jl. Embong Malang No. 7, Surabaya', true, '2026-05-12 13:45:00'),
(6, 3, 'Indomaret Gubeng Station', 'Jl. Stasiun Gubeng, Surabaya', true, '2026-06-18 15:20:00'),
(7, 4, 'Indomaret Kuta Beach', 'Jl. Pantai Kuta No. 99, Badung, Bali', true, '2026-07-22 17:10:00');

INSERT INTO whitelist_stores (id, store_id, is_active) VALUES
(1, 5, true);

SELECT setval('users_id_seq', (SELECT MAX(id) FROM users));
SELECT setval('provinces_id_seq', (SELECT MAX(id) FROM provinces));
SELECT setval('branches_id_seq', (SELECT MAX(id) FROM branches));
SELECT setval('stores_id_seq', (SELECT MAX(id) FROM stores));
SELECT setval('whitelist_stores_id_seq', (SELECT MAX(id) FROM whitelist_stores));
