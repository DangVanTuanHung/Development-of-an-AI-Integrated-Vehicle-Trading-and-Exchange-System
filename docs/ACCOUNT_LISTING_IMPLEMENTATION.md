# Đợt tài khoản và tin đăng trên nhánh Loc

## Các chức năng được bổ sung

- Quên mật khẩu qua mã email: thời hạn 10 phút, tối đa 5 lần nhập sai, mã dùng một lần.
- Giới hạn yêu cầu mã theo IP và theo tài khoản; phản hồi yêu cầu đặt lại mật khẩu không tiết lộ email có tồn tại.
- Đặt lại/đổi mật khẩu thu hồi token cũ; khóa tài khoản cũng thu hồi phiên cũ.
- Xác minh email trong hồ sơ; xác minh điện thoại qua Twilio Verify khi được cấu hình.
- Đổi email/số điện thoại xóa trạng thái xác minh tương ứng. Hồ sơ cá nhân chỉ đọc theo tài khoản hiện tại.
- Hồ sơ công khai `/seller/:id`: ngày tham gia, ảnh, số tin đang đăng, số xe có giao dịch hoàn tất, điểm và nhận xét.
- Nhãn tài khoản đã xác minh trên tin đăng lấy trạng thái thật từ backend.
- Chọn và lưu hãng/dòng xe, địa chỉ xem xe; backend kiểm tra quan hệ hãng/dòng/loại xe.
- Lưu bản nháp riêng, chưa cần ảnh/video. Bản nháp vẫn yêu cầu các trường cơ bản trên form.
- Tạm ẩn, gỡ, đăng lại từ bản nháp; đăng lại cần gửi duyệt.
- Gia hạn 30 ngày, tối đa một lần mỗi 24 giờ; tin hết hạn phải được duyệt lại.
- Tin công khai tự chuyển hết hạn khi đến hạn; tin đang giao dịch không bị đổi trạng thái bởi chức năng gia hạn/đã bán.
- Chủ tin đánh dấu đã bán, với kiểm tra không có giao dịch đang xử lý.
- Thống kê lượt xem/lưu/liên hệ/đề nghị; xem lịch sử giá và ngày hết hạn tại “Tin của tôi”.
- Lượt xem/liên hệ gộp theo người xem mỗi ngày, không tính chủ tin; liên hệ là mở chat hoặc hiện số điện thoại.
- Lịch sử giá ghi từ khi áp dụng migration; giá của tin cũ được ghi làm mốc hiện tại, không dựng lại quá khứ.
- Upload tối đa 12 ảnh/video mỗi tin; lưu phần đã tải thành công để thử lại khi upload bị gián đoạn.

## Cấu hình tùy chọn

Không cần điền API key để chạy `npm run dev`. File `backend/.env` bị Git bỏ qua;
file mẫu và hướng dẫn ở [LOCAL_ENV_SETUP.md](LOCAL_ENV_SETUP.md).

- Thiếu Gemini: sử dụng cơ chế dự phòng hiện có.
- Thiếu SMTP thật: Docker gửi OTP vào Mailpit tại http://localhost:8025.
- Thiếu Twilio: trạng thái SMS chưa sẵn sàng, người dùng được bỏ qua xác minh.
- Thiếu VNPay/S3 cloud: không chặn tài khoản và đăng tin local.
- Bỏ qua xác minh không làm tài khoản tự nhận trạng thái đã xác minh.

Việc gửi email/SMS đến người nhận thật chưa được xác nhận khi chưa có thông tin dịch vụ.
Các nhóm giao dịch, bình luận, tìm kiếm nâng cao... vẫn thuộc các đợt sau của báo cáo rà soát.

## Kiểm tra

```powershell
npm.cmd run build:web
cd backend
docker compose build backend
cd ..
python backend/scripts/check-account-listings.py
```

Docker build chạy Maven verify, bao gồm kiểm tra trạng thái tin, token version và cấu hình dịch vụ trống.
Script tích hợp tạo database/container tên ngẫu nhiên `motionx_check_*`/`motionx-check-*`,
kiểm tra API/OTP bằng hộp thư local rồi dọn database và container đó.
Các email thử dùng tên miền `example.test` có thể xem trong Mailpit.

Migration mới: `V3_0_20` (OTP và quản lý tin), `V3_0_21` (danh mục dòng xe ban đầu),
`V3_0_22` (chống ghi đè cập nhật tài khoản đồng thời). Không reset database cũ.
