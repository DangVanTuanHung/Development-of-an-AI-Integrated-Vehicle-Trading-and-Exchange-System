import { paymentAPI } from "@ebike/shared-code/api";
import type { VnPayReturnResponse } from "@ebike/shared-code/types";
import { CheckCircle2, Loader2, XCircle } from "lucide-react";
import { useEffect, useState } from "react";
import { Link, useLocation } from "react-router-dom";

const RESPONSE_MESSAGES: Record<string, string> = {
  "07": "Giao dịch thành công nhưng đang được VNPay kiểm tra thêm.",
  "09": "Thẻ hoặc tài khoản chưa đăng ký Internet Banking.",
  "10": "Bạn đã nhập sai thông tin xác thực quá số lần cho phép.",
  "11": "Giao dịch đã hết thời gian thanh toán.",
  "12": "Thẻ hoặc tài khoản hiện đang bị khóa.",
  "13": "Mã OTP không chính xác.",
  "24": "Bạn đã hủy giao dịch thanh toán.",
  "51": "Tài khoản không đủ số dư để thanh toán.",
  "65": "Tài khoản đã vượt quá hạn mức giao dịch trong ngày.",
  "75": "Ngân hàng thanh toán đang bảo trì.",
  "79": "Bạn đã nhập sai mật khẩu thanh toán quá số lần cho phép.",
  "99": "VNPay chưa thể xử lý giao dịch. Vui lòng thử lại sau."
};

const formatAmount = (amount?: number | null) =>
  amount == null ? "-" : new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(amount);

const PaymentReturnPage = () => {
  const location = useLocation();
  const [result, setResult] = useState<VnPayReturnResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const loadResult = async () => {
      const queryString = location.search.replace(/^\?/, "");
      if (!queryString) {
        setError("Không tìm thấy thông tin thanh toán.");
        setLoading(false);
        return;
      }

      try {
        setResult(await paymentAPI.getVnPayReturnResult(queryString));
      } catch (requestError) {
        setError(requestError instanceof Error ? requestError.message : "Không thể kiểm tra kết quả thanh toán.");
      } finally {
        setLoading(false);
      }
    };

    void loadResult();
  }, [location.search]);

  const success = result?.success === true;
  const failureMessage = result && !result.valid
    ? "Phản hồi thanh toán không có chữ ký hợp lệ. Trạng thái đơn hàng chưa được thay đổi."
    : RESPONSE_MESSAGES[result?.responseCode ?? ""] || error || "Giao dịch chưa được VNPay xác nhận thành công.";

  return (
    <div className="result-modern mx-auto flex min-h-screen max-w-3xl flex-col items-center justify-center px-6 py-32 text-center">
      {loading ? (
        <>
          <Loader2 className="mb-6 h-12 w-12 animate-spin text-primary" />
          <h1 className="text-3xl font-bold tracking-tight">Đang kiểm tra thanh toán</h1>
          <p className="mt-3 text-muted-foreground">Vui lòng đợi trong giây lát.</p>
        </>
      ) : (
        <>
          {success ? <CheckCircle2 className="mb-6 h-16 w-16 text-emerald-600" /> : <XCircle className="mb-6 h-16 w-16 text-red-600" />}
          <h1 className="text-4xl font-bold tracking-tight">
            {success ? "Thanh toán thành công" : "Thanh toán chưa hoàn tất"}
          </h1>
          <p className="mt-4 max-w-xl text-muted-foreground">
            {success ? "Đơn hàng của bạn đã được xác nhận thanh toán qua VNPay." : failureMessage}
          </p>

          {result ? (
            <div className="mt-8 w-full rounded-lg border border-outline-variant/20 bg-white p-6 text-left text-sm">
              <div className="flex justify-between gap-4 py-2"><span className="text-muted-foreground">Mã đơn</span><span className="font-medium">{result.orderNumber || "-"}</span></div>
              <div className="flex justify-between gap-4 py-2"><span className="text-muted-foreground">Mã giao dịch</span><span className="break-all text-right font-medium">{result.transactionNo || result.txnRef || "-"}</span></div>
              <div className="flex justify-between gap-4 py-2"><span className="text-muted-foreground">Số tiền</span><span className="font-medium">{formatAmount(result.amount)}</span></div>
              <div className="flex justify-between gap-4 py-2"><span className="text-muted-foreground">Trạng thái</span><span className="font-medium">{result.paymentStatus || "-"}</span></div>
            </div>
          ) : null}

          <div className="mt-8 flex flex-wrap justify-center gap-3">
            <Link to="/customer/orders" className="rounded-lg bg-primary px-5 py-3 text-sm font-bold text-white">Xem đơn hàng</Link>
            <Link to="/products" className="rounded-lg border border-outline-variant/30 px-5 py-3 text-sm font-bold">Tiếp tục mua sắm</Link>
          </div>
        </>
      )}
    </div>
  );
};

export default PaymentReturnPage;
