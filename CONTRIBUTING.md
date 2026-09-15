# Làm việc chung trên MOTIONX

## Các nhánh

- `main`: phiên bản ổn định.
- `develop`: tích hợp và kiểm thử chung.
- `Hung`: nhánh làm việc của Hưng.
- Người còn lại tạo nhánh cá nhân từ `develop`, ví dụ `ten-ban`.

Luồng: `Hung / ten-ban -> develop -> main`. Mở Pull Request, nhờ người còn lại review và kiểm tra CI trước khi merge. Dùng **Create a merge commit** để giữ các commit chi tiết.

## Lần đầu lấy mã nguồn

```bash
git clone git@github.com:DangVanTuanHung/Development-of-an-AI-Integrated-Vehicle-Trading-and-Exchange-System.git
cd Development-of-an-AI-Integrated-Vehicle-Trading-and-Exchange-System
git switch Hung
npm install
npm run dev
```

Mở Docker Desktop trước khi chạy. Người thứ hai dùng `git switch -c ten-ban origin/develop`, rồi `git push -u origin ten-ban`.

## Mỗi lần làm việc trên Hung

```bash
git switch Hung
git fetch origin
git merge origin/develop
# Sửa mã nguồn và kiểm tra
npm run build:web
git add <cac-tep-da-sua>
git commit
git push origin Hung
```

Tạo PR từ Hung vào develop. Khi develop ổn định, tạo PR từ develop vào main. Giải quyết conflict trên nhánh cá nhân; không force-push main/develop.

## Commit

Lần đưa mã nguồn đầu tiên được chia theo cấu hình, module, lớp, API và màn hình. Nội dung commit liệt kê các hàm được nhận diện trong tệp. Đây là phân tách bản mã nguồn hiện có, không phải lịch sử phát triển trước đây; các commit nhập mã trung gian có thể chưa build độc lập.

Từ lần làm việc tiếp theo, mỗi commit nên là một thay đổi hoàn chỉnh, build được, có mô tả mục đích và cách kiểm tra.

## CI và triển khai

CI kiểm tra main, develop, Hung và các PR vào main/develop. Deploy EC2 chỉ tự động khi repository variable `ENABLE_EC2_DEPLOY=true`; cũng có thể chạy thủ công sau khi cấu hình secrets.

## Quyền truy cập

Chủ repo mời tài khoản GitHub của người còn lại tại Settings > Collaborators. Các nhánh và tài liệu này không tự cấp quyền ghi. Nên bật branch protection/rulesets cho main và develop để bắt buộc PR, một lượt review và CI thành công.
