# Rà soát sau khi đăng nhập bằng tài khoản phamthailoc

> Đây là rà soát ban đầu trước triển khai. Nhiều mục thiếu bên dưới đã được
> bổ sung; trạng thái mới xem [PRIORITY_IMPLEMENTATION_REPORT.md](PRIORITY_IMPLEMENTATION_REPORT.md)
> và [UAT_2026_11_08.md](UAT_2026_11_08.md).

Kiểm tra ngày 07/11/2026, nhánh `Loc`. Không ghi mật khẩu vào báo cáo.

## Tài khoản và Chrome

- Đăng nhập bằng thông tin người dùng cung cấp trong ảnh: thành công.
- Database xác nhận tài khoản đang hoạt động, quyền `CUSTOMER`.
- Quyền này dùng được cho cả mua và đăng bán, không phải Manager/Admin.
- Trên `http://localhost:5173`, phiên được giữ khi điều hướng/tải trang đến hồ sơ,
  tin của tôi, giao dịch, tin nhắn, lịch hẹn và thông báo.
- Tài khoản hiện chưa có tin đăng hoặc giao dịch; có đề nghị mua cũ đã hết hạn
  và một hội thoại cũ. Tin nhắn cũ hiển thị được. Lịch hẹn và thông báo đang trống.
- Chỉ đăng nhập/xem dữ liệu; không gửi tin nhắn, tạo đề nghị, đăng tin hoặc sửa hồ sơ.
  Việc mở hội thoại có thể cập nhật trạng thái đã đọc theo cơ chế sẵn có.
- Giữ tab Chrome tại hồ sơ trên `localhost` để người dùng tiếp tục kiểm tra.

**Lỗi cần ưu tiên:** khi truy cập bằng `127.0.0.1:5173`, đăng nhập ban đầu thành
công nhưng sau đó trang tin của tôi báo `Unauthorized`; điều hướng tiếp quay về
đăng nhập. Thử lại bằng `localhost:5173` thì các trang trên hoạt động. Script dev
đang cấu hình backend `localhost:8080` nhưng bind frontend `127.0.0.1`; cookie
đăng nhập là SameSite=Lax. Việc trộn hostname là nguyên nhân nghi ngờ chính từ
mã nguồn và phép thử đối chiếu, chưa có bản sửa trong lượt rà soát này.

## Đối chiếu 18 nhóm yêu cầu

Đây là kết quả đọc mã nguồn và kiểm tra các trang nêu trên, không phải kiểm thử
đầy đủ mọi chức năng của từng nhóm. “Chưa thấy” nghĩa chưa tìm được luồng tương
ứng trong phần Marketplace đã rà soát.

| Nhóm | Đã có | Phần cần làm/hoàn thiện |
|---|---|---|
| 1. Tài khoản | Đăng nhập, hồ sơ, reset/email OTP, phân quyền, khóa và thu hồi phiên | Sửa ổn định phiên local; thử SMS/email thật khi có cấu hình |
| 2. Tin đăng | Nháp, ảnh/video, duyệt, ẩn/gỡ, đăng lại, gia hạn, đã bán, thống kê/giá | Bổ sung thông báo thay đổi; kiểm thử upload/chọn ngày thủ công và thiết bị nhỏ |
| 3. Kiểm duyệt | Duyệt/từ chối kèm lý do, đình chỉ và lịch sử | Chưa thấy phát hiện ảnh/nội dung trùng, từ khóa lừa đảo và cảnh báo giá gắn hàng đợi duyệt |
| 4. Tìm kiếm | Từ khóa, loại xe, tỉnh, tình trạng, khoảng giá | Lọc hãng/dòng/năm/km/nhiên liệu, sắp xếp; tìm kiếm lưu/cảnh báo; lịch sử xem; phân trang phía server |
| 5. Chi tiết | Media, thông số, người bán, yêu thích, báo cáo, chat, đề nghị, lịch hẹn, giá tham khảo | Hoàn thiện so sánh tin Marketplace và vị trí bản đồ nếu cần; giá tham khảo phụ thuộc dữ liệu |
| 6. Yêu thích/so sánh | Yêu thích Marketplace; trang so sánh sản phẩm cửa hàng | So sánh hiện gọi productAPI, chưa phải tin bán của người dùng; cần so sánh/lưu/chia sẻ tin Marketplace |
| 7. Bình luận | Chưa thấy module bình luận tin Marketplace | Bình luận, trả lời, sửa/xóa, báo cáo/kiểm duyệt, phân trang và thông báo |
| 8. Nhắn tin | Hội thoại, văn bản, tệp, vị trí, đã đọc, đang nhập, tìm/lọc | Giao diện polling 2,5 giây; bổ sung chặn/báo cáo người dùng, thông báo và realtime nếu cần |
| 9. Lịch xem xe | Tạo, xác nhận/từ chối, hoàn thành/hủy, ghi chú | Chưa có đề xuất giờ khác và nhắc lịch; bổ sung kiểm tra xung đột/lịch quá hạn |
| 10. Đề nghị | Mua/trao đổi, counter, nhận/gửi, rút/từ chối/hết hạn, tạo giao dịch | Thông báo đầy đủ cho hai bên, kiểm thử nghiệp vụ mở rộng |
| 11. Giao dịch/cọc | Xác nhận hai bước, hủy, hoàn, tranh chấp và mã giao dịch | VNPay/IPN, đối soát/hoàn qua cổng chưa tích hợp; không có giữ/chuyển tiền ngân hàng tự động |
| 12. Bàn giao | Lịch có phiên bản, bằng chứng riêng tư, xác nhận hai bên, tự bán/trao đổi | Nhắc lịch/thông báo; thử tệp lớn/PDF/video thực tế và thao tác thủ công |
| 13. Uy tín | Người mua đánh giá người bán sau giao dịch hoàn tất, điểm/hồ sơ công khai | Đánh giá người mua, phản hồi, báo cáo và kiểm duyệt đánh giá |
| 14. Vi phạm | Báo cáo tin, Manager xử lý, đình chỉ/khóa tài khoản; tranh chấp giao dịch | Báo cáo bình luận/tin nhắn/người dùng, chặn, lịch sử vi phạm và kết quả cho người báo |
| 15. Thông báo | Có module thông báo/SSE cho đơn hàng/thanh toán cửa hàng | Marketplace chưa thấy phát sự kiện tương ứng; bổ sung duyệt tin, đề nghị, chat, lịch, giao dịch, hoàn tiền, giá yêu thích |
| 16. Cá nhân | Hồ sơ và các trang Marketplace riêng | Dashboard hiện thiên về cửa hàng; gom tin, lịch, đề nghị/giao dịch, đánh giá/ticket; lịch sử bình luận chưa có |
| 17. Manager/Admin | Duyệt tin, báo cáo, giao dịch/tranh chấp, tài khoản, danh mục/hãng/dòng, audit | Báo cáo khu vực/hãng phổ biến, biểu đồ, kiểm duyệt bình luận/đánh giá khi có; không coi phí dự kiến là doanh thu đã thu |
| 18. Hỗ trợ | FAQ, tạo/theo dõi ticket, staff cập nhật trạng thái và ghi chú | Hiện lưu một staffNote, chưa thấy chuỗi trao đổi nhiều lượt; cần lịch sử hội thoại và thông báo phản hồi |

## Thứ tự đề xuất

1. Sửa phiên đăng nhập local và thống nhất hostname; kiểm thử refresh/tab mới.
2. Nối sự kiện Marketplace vào thông báo hiện có, thêm email local và nhắc lịch.
3. Hoàn thiện tìm kiếm/so sánh Marketplace và dashboard cá nhân để các luồng dễ dùng.
4. Bình luận kèm báo cáo/kiểm duyệt ngay từ đầu; mở rộng chặn/báo cáo chat/người dùng.
5. Lịch đề xuất lại, đánh giá hai chiều và hỗ trợ ticket nhiều lượt.
6. Chống trùng/giá bất thường, báo cáo nâng cao và tối ưu hiệu năng.
7. VNPay/cloud/AI nâng cao khi có nhu cầu và khóa sandbox; phần lõi trên không cần key.

Không sửa mã chức năng trong lượt này. Chưa kiểm thử gửi/nhận giữa hai tài khoản
thật hoặc thao tác Manager/Admin bằng tài khoản phamthailoc vì tài khoản chỉ có
quyền CUSTOMER; kết quả bốn vai trò của đợt trước xem TRADE_WORKFLOW_REPORT.md.
