# MOTIONX Vehicle Marketplace

Hệ thống đã được chuyển sang mô hình mua, bán và trao đổi nhiều loại phương tiện. Web chạy tại `http://localhost:5173`, API tại `http://localhost:8080/api/v1`.

## Khởi động

```powershell
cd backend
docker compose up -d postgres redis backend
cd ..
npm run dev:web
```

Database đang dùng là `vehicle_marketplace`. Database cũ `ebike_db` vẫn được giữ nguyên. Bản sao lưu gần nhất nằm tại `backend/ebike_db-20260808-210747.backup`.

## Khôi phục database không cần AWS

Tạo database mới trong PostgreSQL local:

```powershell
docker exec ebike-postgres createdb -U ebike_user -O ebike_user vehicle_marketplace
```

Khởi động backend để Flyway tự tạo toàn bộ schema và dữ liệu mẫu:

```powershell
cd backend
docker compose up -d --build backend
```

Khôi phục bản backup cũ khi cần đối chiếu:

```powershell
cmd /c "docker exec -i ebike-postgres pg_restore -U ebike_user -d ebike_db --clean --if-exists < backend\ebike_db-20260808-210747.backup"
```

Không chạy lệnh khôi phục vào `vehicle_marketplace` khi hệ thống đang có giao dịch thật.

## Ảnh phương tiện

Ảnh mới được lưu trong Docker volume `backend_marketplace_uploads`, không phụ thuộc S3/AWS. API chỉ nhận JPEG, PNG và WebP, tối đa 10 MB; chỉ chủ tin được tải ảnh lên. Sao lưu volume này cùng PostgreSQL trước khi chuyển máy chủ.

## Thanh toán

Marketplace đã có trạng thái đề nghị, chấp nhận, giao dịch, đặt cọc và thanh toán còn lại. Chế độ `BANK_TRANSFER` tạo yêu cầu thanh toán trên nền tảng. VNPay chỉ có thể thu tiền thật sau khi điền `VNPAY_TMN_CODE`, `VNPAY_HASH_SECRET`, URL callback HTTPS và hoàn tất đăng ký merchant. Không coi giao dịch là đã thanh toán chỉ dựa trên trang quay về của trình duyệt; backend phải xác minh callback/IPN có chữ ký hợp lệ.

## AI

Advisor có cơ chế gợi ý từ dữ liệu marketplace ngay cả khi chưa có khóa AI. Để bật Gemini, đặt `GEMINI_AI_KEY` trong môi trường trước khi khởi động backend. Không commit khóa vào repository.
