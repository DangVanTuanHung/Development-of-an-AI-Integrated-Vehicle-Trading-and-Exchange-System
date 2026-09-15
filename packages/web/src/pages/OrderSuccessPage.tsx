import type { Order } from "@ebike/shared-code/types";
import { Building2, CheckCircle2, Copy, QrCode } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";

interface OrderSuccessLocationState {
  order?: Order;
}

const PAYMENT_STATUS_LABELS: Record<string, string> = {
  PENDING: "Chưa thanh toán",
  PAID: "Đã thanh toán",
  FAILED: "Thanh toán thất bại",
  CANCELLED: "Đã hủy",
  REFUNDED: "Đã hoàn tiền"
};

const bankCode = import.meta.env.VITE_BANK_CODE?.trim() || "";
const bankAccount = import.meta.env.VITE_BANK_ACCOUNT?.trim() || "";
const bankAccountName = import.meta.env.VITE_BANK_ACCOUNT_NAME?.trim() || "";
const bankName = import.meta.env.VITE_BANK_NAME?.trim() || bankCode;

const OrderSuccessPage = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const order = ((location.state as OrderSuccessLocationState | undefined) ?? {}).order;
  const [copied, setCopied] = useState<string | null>(null);

  useEffect(() => {
    if (!order) navigate("/products", { replace: true });
  }, [navigate, order]);

  const transferContent = order ? order.orderNumber.replace(/[^A-Za-z0-9]/g, "") : "";
  const qrUrl = useMemo(() => {
    if (!order || !bankCode || !bankAccount) return null;
    const params = new URLSearchParams({
      amount: String(Math.round(order.totalAmount)),
      addInfo: transferContent,
      accountName: bankAccountName
    });
    return `https://img.vietqr.io/image/${encodeURIComponent(bankCode)}-${encodeURIComponent(bankAccount)}-compact2.png?${params}`;
  }, [order, transferContent]);

  if (!order) return null;

  const isBankTransfer = order.paymentMethod === "BANK_TRANSFER";
  const copyValue = async (label: string, value: string) => {
    await navigator.clipboard.writeText(value);
    setCopied(label);
    window.setTimeout(() => setCopied(null), 1500);
  };

  return (
    <div className="result-modern mx-auto flex min-h-screen max-w-3xl flex-col items-center justify-center px-6 py-32 text-center">
      <CheckCircle2 className="mb-6 h-16 w-16 text-emerald-600" />
      <h1 className="text-4xl font-bold tracking-tight">Đặt hàng thành công</h1>
      <p className="mt-4 max-w-xl text-muted-foreground">
        {isBankTransfer
          ? "Đơn hàng đã được ghi nhận. Vui lòng chuyển khoản đúng số tiền và nội dung bên dưới."
          : "Đơn hàng đã được ghi nhận. Showroom sẽ liên hệ để xác nhận và hướng dẫn thanh toán."}
      </p>

      {isBankTransfer ? (
        <div className="mt-8 w-full rounded-xl border border-primary/20 bg-white p-6 text-left shadow-sm">
          <div className="mb-5 flex items-center gap-3">
            <Building2 className="text-primary" />
            <h2 className="text-xl font-bold">Thông tin chuyển khoản</h2>
          </div>
          {bankAccount ? (
            <div className="grid gap-6 md:grid-cols-[1fr,220px]">
              <div className="space-y-1 text-sm">
                {[
                  ["Ngân hàng", bankName],
                  ["Số tài khoản", bankAccount],
                  ["Chủ tài khoản", bankAccountName],
                  ["Số tiền", `${order.totalAmount.toLocaleString("vi-VN")}đ`],
                  ["Nội dung", transferContent]
                ].map(([label, value]) => (
                  <div key={label} className="flex items-center justify-between gap-4 border-b border-slate-100 py-3">
                    <span className="text-muted-foreground">{label}</span>
                    <span className="flex items-center gap-2 text-right font-semibold">
                      {value || "-"}
                      {value && ["Số tài khoản", "Nội dung"].includes(label) ? (
                        <button type="button" onClick={() => void copyValue(label, value)} title={`Sao chép ${label}`} className="text-primary">
                          <Copy className="h-4 w-4" />
                        </button>
                      ) : null}
                    </span>
                  </div>
                ))}
                {copied ? <p className="pt-2 text-xs font-medium text-emerald-600">Đã sao chép {copied}.</p> : null}
              </div>
              {qrUrl ? <img src={qrUrl} alt="Mã QR chuyển khoản" className="mx-auto w-full max-w-[220px] rounded-lg border" /> : null}
            </div>
          ) : (
            <div className="flex gap-3 rounded-lg bg-amber-50 p-4 text-sm text-amber-800">
              <QrCode className="h-5 w-5 shrink-0" />
              <p>Thông tin tài khoản đang được cập nhật. Showroom sẽ liên hệ để gửi mã QR chuyển khoản.</p>
            </div>
          )}
          <p className="mt-5 text-xs text-muted-foreground">Đơn hàng được xử lý sau khi quản lý xác nhận đã nhận tiền.</p>
        </div>
      ) : null}

      <div className="mt-8 w-full rounded-lg border border-outline-variant/20 bg-white p-6 text-left text-sm">
        <div className="flex justify-between gap-4 py-2"><span className="text-muted-foreground">Mã đơn</span><span className="font-medium">{order.orderNumber}</span></div>
        <div className="flex justify-between gap-4 py-2"><span className="text-muted-foreground">Tổng thanh toán</span><span className="font-medium">{order.totalAmount.toLocaleString("vi-VN")}đ</span></div>
        <div className="flex justify-between gap-4 py-2"><span className="text-muted-foreground">Trạng thái đơn</span><span className="font-medium">{order.status}</span></div>
        <div className="flex justify-between gap-4 py-2"><span className="text-muted-foreground">Trạng thái thanh toán</span><span className="font-medium">{PAYMENT_STATUS_LABELS[order.paymentStatus || ""] || order.paymentStatus || "-"}</span></div>
      </div>

      <div className="mt-8 flex flex-wrap justify-center gap-3">
        <Link to="/customer/orders" className="rounded-lg bg-primary px-5 py-3 text-sm font-bold text-white">Xem đơn hàng</Link>
        <Link to="/products" className="rounded-lg border border-outline-variant/30 px-5 py-3 text-sm font-bold">Tiếp tục mua sắm</Link>
      </div>
    </div>
  );
};

export default OrderSuccessPage;
