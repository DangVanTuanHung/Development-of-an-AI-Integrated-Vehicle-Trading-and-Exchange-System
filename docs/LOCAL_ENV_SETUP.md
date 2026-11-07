# Điền cấu hình trong backend/.env

File thật: `backend/.env`. File mẫu: `backend/.env.example`.
Không bắt buộc điền khóa để chạy web bằng `npm run dev`. Khi `.env` trống hoặc chưa có
API key, Docker dùng cấu hình local mặc định; các dịch vụ bên ngoài là tùy chọn.
Người dùng được bỏ qua xác minh và bổ sung sau; hệ thống giữ đúng trạng thái chưa xác minh.
Git đã bỏ qua `.env`; chỉ file mẫu được đưa lên repository. Điền trực tiếp trên máy,
không đặt khóa vào code React, biến `VITE_*`, ảnh chụp màn hình hoặc commit.

| Chức năng | Cần điền | Khi chưa điền |
|---|---|---|
| Gemini: chatbot, nhận diện ảnh/video | `GEMINI_AI_KEY`, model tài khoản được quyền dùng | Dùng tư vấn dự phòng |
| OTP email, đặt lại mật khẩu | `SMTP_*`, `AUTH_VERIFICATION_FROM` | Docker gửi vào hộp thư Mailpit local |
| OTP xác minh điện thoại | Ba biến `TWILIO_*` | Báo SMS chưa được cấu hình |
| VNPay | `VNPAY_TMN_CODE`, `VNPAY_HASH_SECRET` | Marketplace dùng chuyển khoản/tiền mặt với xác nhận hai bên; VNPay chưa kết nối vào luồng này |
| AWS S3 (tùy chọn) | `APP_STORAGE_S3_*` | Giữ cấu hình MinIO/local hiện có |
| Ký token đăng nhập | `APP_JWT_SECRET` | File local đã có secret ngẫu nhiên, không cần xin ở dịch vụ khác |

## 1. Gemini AI

1. Đăng nhập [Google AI Studio - API Keys](https://aistudio.google.com/apikey).
2. Tạo/chọn Google Cloud project và chọn **Create API key**.
3. Điền khóa vào `GEMINI_AI_KEY=` trong file `.env`.
4. Đối chiếu [danh sách model](https://ai.google.dev/gemini-api/docs/models) và điền
   model mà project được quyền dùng vào `GEMINI_AI_MODEL=`. Tên trong file đang giữ
   cấu hình của repo, chưa được xác nhận hoạt động bằng khóa của bạn.
5. Nếu dịch vụ yêu cầu billing hoặc quota, thiết lập trong tài khoản Google.

Hướng dẫn chính thức: https://ai.google.dev/gemini-api/docs/api-key

## 2. Email OTP: local hoặc Gmail thật

Để trống `SMTP_*` khi chạy Docker: backend gửi vào Mailpit, đọc tại
http://localhost:8025. Mailpit giữ email trên máy, không gửi ra hộp thư thật.

Để gửi bằng Gmail:

1. Bật **Xác minh 2 bước** trong tài khoản Google.
2. Mở https://myaccount.google.com/apppasswords, tạo mật khẩu ứng dụng cho MOTIONX.
3. Điền các giá trị sau (thay email/mật khẩu bằng thông tin của bạn):

```dotenv
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USER=email-cua-ban@gmail.com
SMTP_PASS=mat-khau-ung-dung
SMTP_AUTH=true
SMTP_STARTTLS_ENABLE=true
AUTH_VERIFICATION_FROM=email-cua-ban@gmail.com
```

`SMTP_PASS` là mật khẩu ứng dụng, không phải mật khẩu đăng nhập Gmail.
Một số tài khoản trường/cơ quan hoặc tài khoản được quản lý không có tính năng này;
khi đó dùng SMTP của nhà cung cấp bạn có quyền sử dụng.
Hướng dẫn: https://support.google.com/accounts/answer/185833?hl=vi

## 3. SMS qua Twilio Verify

1. Đăng ký/đăng nhập https://console.twilio.com.
2. Lấy **Account SID** và **Auth Token** từ thông tin tài khoản:
   điền `TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN`.
3. Vào **Verify > Services**, tạo service có kênh SMS.
4. Lấy **Service SID** bắt đầu bằng `VA`, điền `TWILIO_VERIFY_SERVICE_SID`.
5. Cho phép quốc gia nhận tin; bảo đảm tài khoản có quyền và số dư/quota gửi SMS.
   Tài khoản trial có thể chỉ gửi được đến số đã được Twilio cho phép/xác minh.

Ứng dụng gửi đến số đã lưu trong hồ sơ; số Việt Nam được chuẩn hóa thành `+84...`.
Tài liệu: https://www.twilio.com/docs/verify/api/verification

## 4. VNPay (tùy chọn, chưa kết nối luồng giao dịch Marketplace)

Đăng ký sandbox: https://sandbox.vnpayment.vn/devreg/.
VNPay cung cấp mã Terminal và secret; điền vào `VNPAY_TMN_CODE`, `VNPAY_HASH_SECRET`.
Giữ `VNPAY_PAY_URL` sandbox khi thử nghiệm. Khi triển khai, thay URL return/IPN theo
tên miền thực tế. IPN localhost không nhận được callback từ Internet.

Tài liệu: https://sandbox.vnpayment.vn/apis/docs/thanh-toan-pay/pay.html
Luồng Marketplace hiện hoạt động bằng chuyển khoản/tiền mặt trực tiếp: bên trả báo
đã chuyển, bên nhận xác nhận đã nhận. Hoàn tiền cũng cần hai bước tương tự.
Không cần API key cho các bước này. Điền khóa VNPay chưa tự bật thanh toán cổng
cho Marketplace; cần triển khai và kiểm thử callback/IPN, đối soát và hoàn tiền
trước khi sử dụng. Không có bước tự trừ tiền hay giữ tiền ngân hàng trong luồng thủ công.

## 5. S3 và JWT

Không cần khóa AWS để chạy tài khoản và tin đăng local. Nếu dùng S3 cho các chức năng
đang tích hợp S3, tạo bucket và IAM credentials có quyền trên bucket, điền access key,
secret key, region, bucket, endpoint và URL public theo tài khoản của bạn.
Ảnh Marketplace hiện lưu trong volume local; chỉ điền khóa S3 không tự chuyển ảnh đó lên AWS.

`APP_JWT_SECRET` đã được sinh ngẫu nhiên trong file local. Đổi giá trị này làm các
phiên đăng nhập đã ký bằng secret cũ mất hiệu lực.

## Áp dụng sau khi điền

Chạy từ thư mục gốc:

```powershell
docker compose --env-file backend/.env -f backend/docker-compose.yml up -d --build mailpit backend
```

Hoặc chạy lại `npm run dev` (script chạy Docker từ thư mục `backend`).
Container phải được tạo lại để nhận cấu hình mới; tải lại trang web là chưa đủ.
Nếu chạy backend bằng Maven, `cd backend` rồi `mvn spring-boot:run` để đọc `.env`;
Mailpit mặc định ở trên dành cho backend chạy trong Docker.

Không chia sẻ nguyên kết quả `docker compose config`, vì nó có thể chứa khóa thật.
