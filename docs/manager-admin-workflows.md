# Manager / Admin – nghiệp vụ hiện tại

## Phân quyền
- CUSTOMER: quản lý tin, đề nghị, giao dịch và hội thoại của mình.
- MANAGER: vận hành marketplace (kiểm duyệt, báo cáo, theo dõi giao dịch, xử lý tranh chấp, hỗ trợ). Có khu vực riêng cho tin cá nhân.
- ADMIN: vận hành như manager và quản lý tài khoản, vai trò, danh mục phương tiện, biểu phí/khuyến mãi, nhật ký.
- OPERATOR / SUPPORT / MAINTENANCE cũ không còn được coi là ADMIN. Admin có thể chuyển vai trò phù hợp.
- API kiểm tra vai trò, không dựa vào việc ẩn menu. Các API cửa hàng cũ /manager ngoài hỗ trợ được giới hạn ADMIN.
- Không cho nhân sự tự duyệt tin, xử lý báo cáo về tin của mình, xử lý báo cáo mình gửi hoặc xử lý tranh chấp giao dịch mình tham gia.
- Admin không được tự khóa / hạ quyền / vô hiệu hóa tài khoản đang sử dụng. Ngừng hoạt động giữ lịch sử; token của tài khoản không hoạt động bị từ chối.

## Tin đăng
DRAFT → PENDING_REVIEW → PUBLISHED hoặc REJECTED.
- publish là gửi duyệt; áp dụng cho cả admin và manager.
- Người bán xem được lý do và sửa tin rồi gửi duyệt lại.
- Sửa thông tin hoặc gỡ ảnh khỏi tin đưa về DRAFT, không tiếp tục hiển thị bản chưa duyệt.
- Thêm ảnh chỉ cho DRAFT / REJECTED; cần ít nhất một ảnh hoặc video hợp lệ để gửi duyệt.
- PUBLISHED → SUSPENDED cần lý do. Khôi phục SUSPENDED → PENDING_REVIEW, phải duyệt lại.
- Không sửa / đình chỉ tin RESERVED hoặc SOLD bằng nghiệp vụ kiểm duyệt. Vấn đề giao dịch xử lý qua luồng tranh chấp.
- Gỡ tin cá nhân dùng ARCHIVED. Không xóa lịch sử giao dịch.
- Khách không đọc được tin chưa công khai qua endpoint chi tiết.

## Báo cáo vi phạm
Bảng listing_reports riêng với OPEN / RESOLVED / DISMISSED. Migration chuyển các USER_REPORT cũ sang bảng mới.
- Một người chỉ có một báo cáo OPEN trên cùng tin.
- Bỏ qua báo cáo hoặc ẩn tin vi phạm; ghi chú bắt buộc.
- Lưu người xử lý, thời gian, kết quả và audit.
- Việc ẩn chỉ áp dụng cho tin PUBLISHED; các tin khác có thể đóng báo cáo kèm giải thích.

## Giao dịch
- Manager xem giá đã thỏa thuận, người mua / bán, trạng thái và lịch sử sự kiện.
- Mở tranh chấp chuyển sang DISPUTED. Tiền mặt và xác nhận thanh toán không được vượt qua trạng thái chặn.
- Sau xử lý, ghi kết quả và khôi phục đúng trạng thái trước tranh chấp, không cho nhập tùy ý giá / trạng thái.
- Không có quyền xác nhận nhận tiền thay người bán hoặc nhận xe thay người mua.
- VNPAY không được xác nhận thủ công qua endpoint người bán.
- Tổng quan dùng dữ liệu marketplace; không dùng doanh số đơn hàng cửa hàng làm số liệu sàn.

## Admin
- Tài khoản / phân quyền / nhật ký dùng kết quả server; bỏ dữ liệu và nhật ký giả trên trình duyệt.
- Tạo CUSTOMER / MANAGER / ADMIN với mật khẩu ban đầu tối thiểu 8 ký tự.
- Quản lý loại xe, hãng, dòng xe; ngừng sử dụng thay vì xóa danh mục đang có liên kết.
- Phần biểu phí/khuyến mãi hiện có được giữ lại, API chỉ cho ADMIN.

## Phạm vi chưa triển khai trong đợt chỉnh này
Tài liệu tham khảo gồm cả các ý tưởng mở rộng chưa có nghiệp vụ nền trong dự án: dealer verification, lead/consultant/KPI, exchange-offer có xe đối ứng và tiền bù, hội viên, cấu hình AI và điều tra hội thoại có cấp quyền. Không tạo menu hoặc số liệu giả cho các phần này. Việc xử lý hoàn tiền/giải ngân thực tế qua nhà cung cấp vẫn cần quy trình đối soát riêng; nút kết thúc tranh chấp chỉ khôi phục giao dịch.

## Kiểm tra
- npm run build:web
- docker compose build backend (Dockerfile chạy mvn clean verify)
- OperationsPolicyTest: chuyển trạng thái, lý do bắt buộc, chặn tự duyệt và tin RESERVED.
- OperationsAuthorizationTest: CUSTOMER không vào vận hành; MANAGER không vào danh mục ADMIN; ADMIN được truy cập.
- Smoke API local: kiểm tra theo vai trò và vòng đời tin thử nghiệm. Tin thử nghiệm được xóa riêng sau kiểm tra; audit giữ lại.
