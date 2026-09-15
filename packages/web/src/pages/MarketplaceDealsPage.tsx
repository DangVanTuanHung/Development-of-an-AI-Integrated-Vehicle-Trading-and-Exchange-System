import { CheckCircle2, CreditCard, HandCoins, RefreshCw } from "lucide-react";
import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "@ebike/shared-code/hooks";
import { formatMarketplacePrice, marketplaceAPI } from "../services/marketplace";

type Row = Record<string, unknown>;
const money = (value: unknown) => formatMarketplacePrice(Number(value || 0));
const valueOrMissing = (value: unknown) => value ? String(value) : "Chưa cập nhật";

const MarketplaceDealsPage = () => {
  const { user } = useAuth();
  const [offers, setOffers] = useState<Row[]>([]);
  const [transactions, setTransactions] = useState<Row[]>([]);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState("");
  const load = async () => {
    setError("");
    try { const [nextOffers, nextTransactions] = await Promise.all([marketplaceAPI.myOffers(), marketplaceAPI.myTransactions()]); setOffers(nextOffers); setTransactions(nextTransactions); }
    catch { setError("Vui lòng đăng nhập để xem thương lượng và giao dịch của bạn."); }
  };
  useEffect(() => { void load(); }, []);
  const act = async (key: string, action: () => Promise<unknown>) => { setBusy(key); setError(""); try { await action(); await load(); } catch { setError("Thao tác chưa thể hoàn tất. Hãy kiểm tra vai trò hoặc trạng thái giao dịch."); } finally { setBusy(""); } };
  const transactionActions = (tx: Row) => {
    const key = String(tx.publicId);
    const isBuyer = Number(tx.buyerId) === Number(user?.id);
    const isSeller = Number(tx.sellerId) === Number(user?.id);
    const hasPendingPayment = tx.paymentStatus === "PENDING" || tx.paymentStatus === "PROCESSING";
    if (isSeller && hasPendingPayment && tx.paymentPublicId) {
      return <button disabled={busy === key} onClick={() => void act(key, () => marketplaceAPI.confirmMarketplacePayment(String(tx.paymentPublicId)))} className="rounded-full bg-emerald-600 px-5 py-3 text-sm font-bold text-white">{tx.paymentProvider === "CASH_ON_DELIVERY" ? "Xác nhận đã nhận tiền mặt & bàn giao" : "Xác nhận đã nhận chuyển khoản"}</button>;
    }
    if (isBuyer && tx.status === "DEPOSIT_PENDING" && !tx.paymentPublicId) {
      return <><button disabled={busy === key} onClick={() => void act(key, () => marketplaceAPI.createPayment(key, "DEPOSIT", "BANK_TRANSFER"))} className="rounded-full bg-violet-600 px-5 py-3 text-sm font-bold text-white">Chuyển khoản tiền cọc</button><button disabled={busy === key} onClick={() => void act(key, () => marketplaceAPI.createPayment(key, "FINAL", "CASH_ON_DELIVERY"))} className="rounded-full border border-violet-200 bg-white px-5 py-3 text-sm font-bold text-violet-700">Tiền mặt khi nhận xe</button></>;
    }
    if (isBuyer && ["DEPOSIT_PAID", "INSPECTION_PENDING"].includes(String(tx.status)) && tx.paymentStage === "DEPOSIT" && tx.paymentStatus === "PAID") {
      return <button disabled={busy === key} onClick={() => void act(key, () => marketplaceAPI.createPayment(key, "FINAL", "BANK_TRANSFER"))} className="rounded-full bg-violet-600 px-5 py-3 text-sm font-bold text-white">Chuyển khoản phần còn lại</button>;
    }
    if (isBuyer && tx.status === "FULLY_PAID") {
      return <button disabled={busy === key} onClick={() => void act(key, () => marketplaceAPI.confirmHandover(key))} className="rounded-full bg-emerald-600 px-5 py-3 text-sm font-bold text-white">Xác nhận đã nhận xe</button>;
    }
    return null;
  };
  return <main className="min-h-screen bg-[#fff8f5] px-5 pb-24 pt-32"><div className="mx-auto max-w-6xl"><div className="flex flex-wrap items-end justify-between gap-5"><div><p className="text-xs font-bold uppercase tracking-[.2em] text-violet-600">Trung tâm giao dịch</p><h1 className="mt-2 text-4xl font-black tracking-tight text-slate-950">Thương lượng & thanh toán</h1><p className="mt-3 text-slate-500">Theo dõi đề nghị, tiền cọc và tiến độ hoàn tất ngay trên MOTIONX.</p></div><button onClick={() => void load()} className="flex items-center gap-2 rounded-full border bg-white px-5 py-3 font-bold"><RefreshCw size={17}/>Làm mới</button></div>{error ? <div className="mt-6 rounded-2xl bg-red-50 p-4 text-red-700">{error} <Link className="font-bold underline" to="/auth">Đăng nhập</Link></div> : null}<section className="mt-10"><h2 className="flex items-center gap-2 text-2xl font-extrabold"><HandCoins className="text-fuchsia-500"/>Đề nghị của tôi</h2><div className="mt-5 grid gap-4">{offers.length ? offers.map((offer) => <article key={String(offer.publicId)} className="rounded-3xl border border-violet-100 bg-white p-6 shadow-sm"><div className="flex flex-wrap items-center justify-between gap-4"><div><p className="font-extrabold text-slate-900">{String(offer.listingTitle)}</p><p className="mt-1 text-sm text-slate-500">Đề nghị {money(offer.amount)} · Cọc {money(offer.depositAmount)}</p>
{Number(offer.sellerId) === Number(user?.id) ? <div className="mt-4 grid gap-2 rounded-xl bg-violet-50 p-4 text-sm text-slate-600 sm:grid-cols-2"><p><b>Họ tên:</b> {valueOrMissing(offer.buyerName)}</p><p><b>Số điện thoại:</b> {valueOrMissing(offer.buyerPhone)}</p><p><b>Email:</b> {valueOrMissing(offer.buyerEmail)}</p><p><b>Nơi ở:</b> {valueOrMissing(offer.buyerAddress)}</p></div> : null}
</div>
<div className="flex items-center gap-3"><span className="rounded-full bg-violet-50 px-4 py-2 text-xs font-bold text-violet-700">{String(offer.status)}</span>{offer.status === "PENDING" && Number(offer.sellerId) === Number(user?.id) ? <button disabled={busy === String(offer.publicId)} onClick={() => void act(String(offer.publicId), () => marketplaceAPI.acceptOffer(String(offer.publicId)))} className="rounded-full bg-slate-950 px-4 py-2 text-sm font-bold text-white">Chấp nhận</button> : null}</div></div></article>) : <p className="rounded-3xl border border-dashed p-8 text-center text-slate-500">Chưa có đề nghị nào.</p>}</div></section><section className="mt-12"><h2 className="flex items-center gap-2 text-2xl font-extrabold"><CreditCard className="text-violet-600"/>Giao dịch đang xử lý</h2><div className="mt-5 grid gap-4">{transactions.length ? transactions.map((tx) => <article key={String(tx.publicId)} className="rounded-3xl border border-violet-100 bg-white p-6 shadow-sm"><div className="flex flex-wrap items-center justify-between gap-5"><div><p className="text-xs font-bold uppercase tracking-wider text-violet-600">{String(tx.transactionNumber)}</p><p className="mt-2 text-lg font-extrabold">{String(tx.listingTitle)}</p><p className="mt-1 text-sm text-slate-500">Giá chốt {money(tx.agreedPrice)} · Cọc {money(tx.depositAmount)}</p>{tx.paymentReference ? <p className="mt-2 text-xs text-slate-500">Mã thanh toán: <b>{String(tx.paymentReference)}</b> · {tx.paymentProvider === "CASH_ON_DELIVERY" ? "Tiền mặt khi nhận xe" : tx.paymentStage === "DEPOSIT" ? "Chuyển khoản tiền cọc" : "Chuyển khoản phần còn lại"} · {money(tx.paymentAmount)}</p> : null}</div><div className="flex flex-wrap items-center gap-3"><span className="flex items-center gap-2 rounded-full bg-emerald-50 px-4 py-2 text-xs font-bold text-emerald-700"><CheckCircle2 size={15}/>{String(tx.status)}</span>{transactionActions(tx)}</div></div></article>) : <p className="rounded-3xl border border-dashed p-8 text-center text-slate-500">Chưa có giao dịch nào.</p>}</div></section></div></main>;
};
export default MarketplaceDealsPage;

