# Báo cáo triển khai các mục Ngay, Cao và Tiếp theo

Ngày 07/11/2026, nhánh `Loc`. Kết quả kiểm tra lại ngày 08/11 và bộ bốn tài khoản
xem [UAT_2026_11_08.md](UAT_2026_11_08.md). Trạng thái commit/push xem lịch sử Git.

## Đã triển khai

- Giữ hostname API cùng `localhost`/`127.0.0.1` với web để duy trì cookie khi
  tải lại trang. Không đổi cổng backend do người dùng cấu hình.
- Thông báo Marketplace cho tin đăng, giá/tình trạng tin yêu thích, đề nghị,
  giao dịch, chat, lịch xem/bàn giao, đánh giá, báo cáo và phản hồi hỗ trợ.
  Có SSE, lịch nhắc trước một giờ và hàng đợi email có retry. SMTP trống không
  chặn nghiệp vụ; Docker local dùng Mailpit.
- Tìm theo từ khóa/hãng/dòng, loại xe, tỉnh, giá, năm, km, nhiên liệu, tình trạng;
  sắp xếp/phân trang phía server. Lưu tìm kiếm, cảnh báo tin mới và lịch sử xem.
- So sánh tối đa ba tin Marketplace, dùng URL để chia sẻ và lưu vào tài khoản.
  Trang so sánh sản phẩm cửa hàng cũ được giữ riêng.
- Bình luận/trả lời, sửa/xóa bình luận của mình, chủ tin ẩn/xóa, tải thêm;
  báo cáo/kiểm duyệt và thông báo phản hồi. Nội dung ẩn không xuất hiện công khai.
- Báo cáo bình luận, đánh giá, tin nhắn và người dùng. Manager/Admin xử lý,
  ghi kết luận/audit và trả kết quả cho người báo. Tin nhắn chỉ được báo cáo
  bởi người thuộc hội thoại. Có chặn/bỏ chặn liên hệ hai chiều.
- Đề xuất lại giờ xem xe, bên nhận đề xuất xác nhận; chặn phiên bản lịch cũ,
  xung đột trong vòng 30 phút và hoàn thành lịch trước giờ hẹn.
- Đánh giá hai chiều sau giao dịch hoàn tất, mỗi bên một lượt; phản hồi và
  báo cáo/kiểm duyệt đánh giá. Điểm công khai không tính đánh giá đã bị ẩn.
- Ticket có lịch sử nhiều lượt, phân quyền chủ ticket/nhân sự và trạng thái
  chờ khách/đang xử lý; giữ ghi chú cũ trong lịch sử.
- Trang `/marketplace-account` gom tin, đề nghị, giao dịch, lịch, thông báo,
  bình luận, đánh giá, tìm kiếm đã lưu, xe vừa xem, báo cáo và người đã chặn.

## Kết quả kiểm tra

| Kiểm tra | Kết quả |
|---|---|
| Build web: TypeScript và Vite | Đạt |
| Docker backend, có `mvn clean verify` | Đạt |
| `check-account-listings.py` | 10 nhóm đạt, có `.env` hoàn toàn trống |
| `check-trades.py` | 11 nhóm đạt |
| `check-community.py` | 9 nhóm đạt |
| 25 kết nối SSE đồng thời, API hồ sơ vẫn phục vụ | Đạt |
| Chrome: giữ phiên sau refresh, lọc/sắp xếp/phân trang, lưu so sánh/tìm kiếm | Đạt |
| Chrome: bình luận/trả lời, báo cáo, chặn liên hệ, đổi/xác nhận lịch | Đạt |
| Chrome: đánh giá hai chiều/phản hồi, Manager/Admin xử lý báo cáo | Đạt |
| Chrome: Lộc → Manager → Lộc → Admin trong ticket | Đạt, dữ liệu giữ lại |
| Chrome: thông báo mới xuất hiện khi Manager phản hồi, không reload trang Lộc | Đạt |
| Email vào Mailpit gửi cho Lộc | Đã xác nhận có email local |
| Chrome viewport 390px: tìm kiếm, quản lý cá nhân, so sánh | Không tràn ngang toàn trang; bảng so sánh cuộn riêng |

30 là số **nhóm API/hồi quy**, không phải số kịch bản Chrome. Các bài tích hợp
dùng database/container riêng. Dữ liệu minh chứng trên database chính được giữ.
Ô ngày giờ được nhập qua sự kiện DOM vì công cụ nhập `datetime-local` không
giữ được giá trị; không coi đây là kiểm chứng đầy đủ thao tác mở lịch bằng tay.

## Lỗi đã phát hiện và sửa trong quá trình test

- Cookie mất phiên khi trộn hostname local.
- Bình luận hiển thị lặp do trùng React key giữa hai component.
- SSE giữ kết nối database khi Open EntityManager In View còn gắn với request
  dài hạn. Loại trừ endpoint stream khỏi interceptor và thêm heartbeat; kiểm
  tra lại bằng 25 kết nối đồng thời. Tham khảo cấu hình gốc của
  [Spring Boot 3.1.5](https://github.com/spring-projects/spring-boot/blob/v3.1.5/spring-boot-project/spring-boot-autoconfigure/src/main/java/org/springframework/boot/autoconfigure/orm/jpa/JpaBaseConfiguration.java).
- Thời gian thông báo thiếu múi giờ: response hiện trả offset UTC để Chrome
  hiển thị theo giờ địa phương.
- Đường dẫn ảnh khởi tạo Isuzu QKR270 bị hỏng: thay bằng ảnh cùng dòng xe được
  đóng gói trong backend. Chrome xác nhận ảnh 1276×956, không còn ảnh vỡ hoặc
  ảnh một pixel trên trang chủ. Nguồn ảnh: [SEED_IMAGE_SOURCES.md](SEED_IMAGE_SOURCES.md).

## Dữ liệu để mở lại và xem minh chứng

- Lộc: `phamthailoc`, tại `/marketplace-account` và `/customer/notifications`.
- Manager thật: `manager.user` — **Quản lý vận hành**, tại `/manager/reports`
  và `/manager/support`.
- Admin thật: `admin.user` — **Quản trị hệ thống**, tại `/admin/reports`,
  `/admin/support` và `/admin/accounts`.
- Ticket **MX-261107-CA1877** có trao đổi của cả ba tài khoản thật.
- Báo cáo **#3/#4** đã được kiểm thử bằng Manager/Admin thật, sau đó được xóa
  cùng tài khoản QA theo yêu cầu dọn dữ liệu; không còn là minh chứng mở lại.
- Tin Isuzu `a0d427a6-a6ae-4437-8c2d-d140dee639d6` có câu hỏi của Lộc và trả lời
  của `admin.user`; ảnh đã sửa. Có thể mở từ trang chủ.
- Danh sách so sánh của Lộc được cập nhật sang Isuzu và Toyota Camry đang đăng.

Theo yêu cầu dọn dữ liệu ngày 07/11, tài khoản `QA seller MotionX` đã được xóa
khỏi database chính, cùng bốn tin liên quan (ba tin của QA và xe Lộc dùng trao
đổi với QA), một giao dịch, một đề nghị, một cuộc chat, một lịch hẹn, ba bình
luận, hai đánh giá và bốn báo cáo; các thông báo/lịch sử liên quan và năm tệp
ảnh/minh chứng không còn được tham chiếu cũng được xóa. Hai tài khoản quản lý
QA đã được xóa trước đó. Database chính chỉ còn `phamthailoc` (CUSTOMER),
`manager.user` (MANAGER) và `admin.user` (ADMIN). Ticket hỗ trợ, bình luận Isuzu,
danh sách so sánh và dữ liệu độc lập của ba tài khoản này vẫn được giữ.

Ngày 08/11, đã xóa nốt tin thử ARCHIVED của Lộc
`175e5151-597b-4906-aacd-4066e97fe6c9` có ảnh một pixel, cùng ảnh, lịch sử
duyệt/giá, lượt xem và bốn thông báo liên quan. Trang “Tin của tôi” của Lộc
hiện không còn tin thử; hồ sơ và các dữ liệu độc lập khác vẫn được giữ.

Phạm vi kiểm thử cập nhật ngày 08/11: dùng `phamthailoc`, `levietvinh`,
`manager.user`, `admin.user`; không tạo thêm tài khoản QA. Nội dung lưu lại
phải phù hợp nghiệp vụ, ảnh xe đúng nội dung, tiêu đề rõ ràng, không dùng tên
QA/demo/test. Đây là quy ước chuẩn bị dữ liệu, không thêm luật cấm vào ứng dụng.
Đã kiểm thử hai CUSTOMER riêng biệt là Lộc và Vinh. Đăng ký mới được kiểm tra
trong database tích hợp riêng;
không xóa hay khóa tài khoản chính để thử các tình huống phá hủy dữ liệu.

**Đây là cách chuẩn bị dữ liệu kiểm thử, không phải luật hạn chế ứng dụng.**
Các ràng buộc cứng vừa thêm do hiểu nhầm đã được gỡ: không chặn từ QA/demo,
không áp ngưỡng ảnh mới và không cố định danh sách tài khoản được cấp quyền.
V3_0_27 gỡ trigger V3_0_26 đã từng áp dụng; giữ lịch sử migration để tránh lỗi
checksum. Đã kiểm tra lại trong database riêng: chức năng phân quyền cấu hình
được hoạt động bình thường. Khi tạo minh chứng tiếp theo phải dùng ảnh phù hợp
và tiêu đề mô tả xe đúng nghiệp vụ, dùng Manager/Admin thật, giữ dữ liệu lại.

## Giới hạn và hướng tiếp theo

- Chưa kiểm thử email/SMS/AI bằng thông tin dịch vụ thật. `.env` vẫn tùy chọn;
  không có key vẫn dùng chức năng nội bộ và email Mailpit local.
- Thanh toán/hoàn tiền ghi nhận xác nhận thủ công; chưa tích hợp VNPay/IPN,
  giữ tiền, chuyển tiền hoặc đối soát ngân hàng tự động.
- Chat hiện tải cập nhật theo chu kỳ 2,5 giây; thông báo dùng SSE.
- Build còn cảnh báo bundle lớn và dữ liệu Browserslist cũ.
- Ảnh khởi tạo cũ là ảnh minh họa; chưa xác minh ảnh đúng từng phiên bản/năm xe.

Ưu tiên tiếp: chuẩn hóa dữ liệu/ảnh từng mẫu xe và chạy UAT theo checklist trên
tài khoản thật; sau đó chống tin/ảnh trùng và cảnh báo lừa đảo/giá bất thường,
báo cáo thống kê nâng cao, tối ưu tải trang. Thanh toán cổng làm riêng khi có
thông tin sandbox và yêu cầu nghiệp vụ rõ ràng.
