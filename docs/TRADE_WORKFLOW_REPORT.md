# Báo cáo triển khai giao dịch Marketplace

Ngày kiểm tra: 07/11/2026. Nhánh làm việc: `Loc`. Chưa commit, push hoặc merge.

## Phạm vi đã triển khai

- Người mua gửi đề nghị mua hoặc trao đổi bằng một tin xe đang đăng của mình.
- Hai bên thương lượng giá xe, giá trị xe trao đổi, cọc và lời nhắn; đồng ý rõ
  điều kiện hủy/hoàn tiền trước khi gửi hoặc chấp nhận.
- Chấp nhận, từ chối, rút và hết hạn đề nghị. Đề nghị thay thế giữ lịch sử.
- Chấp nhận đề nghị tạo giao dịch và giữ chỗ xe; trao đổi giữ chỗ cả hai xe.
- Hỗ trợ người mua hoặc người bán trả chênh lệch, hoặc trao đổi ngang giá.
- Trang chi tiết giao dịch có mã giao dịch, giá chốt, tiền cọc, thanh toán,
  lịch bàn giao, bằng chứng riêng tư, hoàn tiền, tranh chấp và nhật ký.
- Chuyển khoản/tiền mặt: bên trả báo đã giao tiền kèm mã biên nhận; bên nhận
  xác nhận đã nhận. Thanh toán đủ chưa tự hoàn tất bàn giao.
- Yêu cầu hủy, rút yêu cầu, từ chối và đồng ý hủy. Khi hủy theo thỏa thuận,
  hoàn toàn bộ khoản đã xác nhận nhận; bên hoàn báo gửi, bên nhận xác nhận lại.
- Người tham gia mở tranh chấp; Manager/Admin xem hồ sơ và kết luận tiếp tục
  hoặc hủy/hoàn tiền. Nhân sự không được tự xử lý giao dịch mình tham gia.
- Hai bên thống nhất cùng phiên bản lịch bàn giao. Cần thanh toán đủ, bằng
  chứng và xác nhận của cả hai trước khi hoàn tất.
- Tin tự chuyển `SOLD` khi bán hoặc cả hai tin chuyển `EXCHANGED` khi trao đổi.
- Lưu kết luận tranh chấp sau khi đóng; nhật ký ghi người thực hiện và thời gian.

## Kiểm tra trên Chrome

Thực hiện bằng bốn phiên đăng nhập tách biệt, trên backend/database thử riêng.
Không tạo giao dịch thử trong database đang dùng của dự án.

| Vai trò | Thao tác đã kiểm tra | Kết quả |
|---|---|---|
| Người mua | Đăng nhập, gửi đề nghị 9,5 triệu/cọc 500 nghìn, chấp nhận giá thương lượng 9,7 triệu | Đạt |
| Người bán | Đăng nhập, xem đề nghị nhận, gửi giá thương lượng mới | Đạt |
| Hai bên | Người mua báo chuyển cọc 500 nghìn và phần còn lại 9,2 triệu; người bán xác nhận từng khoản | Đạt |
| Hai bên | Đề xuất lịch, đồng ý lịch, tải PNG, người mua xác nhận nhận xe rồi người bán xác nhận bàn giao | Đạt; chỉ hoàn tất sau xác nhận thứ hai |
| Manager | Đăng nhập, duyệt tin chờ duyệt, mở giao dịch tranh chấp, kết luận tiếp tục | Đạt; trở lại trạng thái đã nhận cọc |
| Admin | Đăng nhập, mở giao dịch tranh chấp khác, kết luận hủy và hoàn tiền | Đạt; chuyển chờ hoàn tiền |
| Hai bên | Người bán báo hoàn một triệu, người mua xác nhận nhận lại | Đạt; chuyển đã hoàn tiền |
| Bốn vai trò | Kiểm tra console trên trang giao dịch sau các thao tác | Không có lỗi console tại các trang được kiểm tra |

Đã quan sát giao diện bằng ảnh chụp Chrome; snapshot bốn vai trò lưu local trong
`.local-checks/chrome-*.txt` (Git bỏ qua). Công cụ upload bị giới hạn đường dẫn;
fixture PNG được gán vào ô chọn file bằng JavaScript rồi kích hoạt sự kiện `change`,
vẫn đi qua handler upload của giao diện. Ô ngày giờ cũng được nhập bằng sự kiện DOM
do công cụ điền form không giữ được giá trị `datetime-local`. Các nút nghiệp vụ
được bấm trên giao diện, không gọi API thay cho các thao tác đó.

## Kiểm tra kỹ thuật

- `npm run build:web`: TypeScript và Vite build thành công. Có cảnh báo kích thước
  bundle lớn và dữ liệu Browserslist cũ; chưa tối ưu bundle trong đợt này.
- `docker compose build backend`: thành công; Dockerfile chạy `mvn clean verify`.
- `backend/scripts/check-trades.py`: **11 nhóm kiểm tra đạt** trên PostgreSQL thật,
  database/container tạm và khóa dịch vụ ngoài để trống. Bao gồm phân quyền,
  thương lượng, cọc/thanh toán, bằng chứng riêng tư, bàn giao, hủy/hoàn, tiền mặt,
  trao đổi và xử lý tranh chấp. Có kiểm tra hủy không có tiền, trao đổi ngang giá,
  xác nhận lịch cũ bị chặn và hai yêu cầu chấp nhận đồng thời chỉ tạo một giao dịch.
  Có kiểm tra đề nghị đúng hình thức chỉ bán/chỉ trao đổi. Script tự dọn tài
  nguyên tạm khi kết thúc.
- `backend/scripts/check-account-listings.py`: **10 nhóm kiểm tra hồi quy đạt**,
  gồm Docker Compose dùng `.env` hoàn toàn trống, OTP/email/reset, thu hồi phiên
  đăng nhập, hồ sơ, tin đăng, quyền sở hữu và thống kê.
- Migration `V3_0_23` bổ sung dữ liệu thương lượng, trao đổi, bàn giao, bằng chứng
  và hoàn tiền; giữ dữ liệu cũ, bổ sung hồ sơ cho tranh chấp cũ.

## Cấu hình và API key

File điền thông tin: **`backend/.env`**. File này đã tồn tại và được Git bỏ qua.
Không cần điền gì để chạy chức năng mua/trao đổi, cọc, bàn giao, hủy và tranh chấp.
Hướng dẫn lấy Gemini, SMTP, Twilio, VNPay và S3: [LOCAL_ENV_SETUP.md](LOCAL_ENV_SETUP.md).

Không có key thì bỏ qua dịch vụ ngoài; trạng thái xác minh và thanh toán vẫn phản ánh
đúng việc đã thực hiện. Email local dùng Mailpit tại `http://localhost:8025`.
Chuyển khoản/hoàn tiền hiện do hai bên thực hiện ngoài ứng dụng và xác nhận trong web.
Chưa tích hợp giữ tiền, tự chuyển tiền, đối soát ngân hàng hay VNPay/IPN vào luồng này;
chỉ điền khóa VNPay chưa tự bật các bước đó. Chưa kiểm thử SMS/email thật hoặc AI
bằng tài khoản dịch vụ của bạn.

## Phần cần làm tiếp

Đợt này bổ sung trọng tâm nhóm 10–12 và phần giao dịch/tranh chấp của nhóm 14/17,
tiếp nối đợt tài khoản/tin đăng. Chưa tuyên bố hoàn thành toàn bộ 18 nhóm.

- Kiểm kê và hoàn thiện bình luận theo luồng, báo cáo/kiểm duyệt bình luận.
- Thông báo realtime/email cho toàn bộ sự kiện và nhắc lịch.
- Tìm kiếm đã lưu, cảnh báo tin phù hợp, lịch sử xem và chia sẻ so sánh.
- Chống tin trùng/ảnh trùng, nội dung lừa đảo và cảnh báo giá nâng cao.
- Đánh giá hai chiều sau giao dịch, phản hồi và kiểm duyệt đánh giá.
- Quy trình thanh toán cổng và đối soát bằng thông tin sandbox khi có nhu cầu.

Các phần này cần triển khai và kiểm thử riêng; danh sách không có nghĩa tất cả
đều chưa có mã nguồn. Xem thêm báo cáo [tài khoản/tin đăng](ACCOUNT_LISTING_IMPLEMENTATION.md).

## Trạng thái sau kiểm tra

Web chính `http://localhost:5173`, backend readiness và Mailpit trả HTTP 200.
Backend chính healthy, migration `3.0.23` đã áp dụng thành công. Đã dọn database,
container và thông tin đăng nhập thử; dừng frontend 5174, đóng riêng các tab Chrome
do đợt kiểm thử tạo. Giữ web chính hoạt động để bạn tiếp tục kiểm tra.
