import { CalendarDays, Check, Clock3, MapPin, X } from "lucide-react";
import { useEffect, useState } from "react";
import { Navigate } from "react-router-dom";
import { useAuth } from "@ebike/shared-code/hooks";
import { marketplaceAPI, type MarketplaceAppointment } from "../services/marketplace";

const statusLabel: Record<MarketplaceAppointment["status"], string> = {
  REQUESTED: "Chờ xác nhận",
  CONFIRMED: "Đã xác nhận",
  DECLINED: "Đã từ chối",
  COMPLETED: "Đã hoàn thành",
  CANCELLED: "Đã hủy",
};

const MarketplaceAppointmentsPage = () => {
  const { user, isAuthenticated, isBootstrapping } = useAuth();
  const [appointments, setAppointments] = useState<MarketplaceAppointment[]>([]);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState("");

  const load = async () => {
    setError("");
    try {
      setAppointments(await marketplaceAPI.myAppointments());
    } catch {
      setError("Không thể tải lịch hẹn.");
    }
  };

  useEffect(() => {
    if (isAuthenticated) void load();
  }, [isAuthenticated]);

  const update = async (appointment: MarketplaceAppointment, status: string) => {
    setBusy(appointment.publicId);
    setError("");
    try {
      await marketplaceAPI.updateAppointment(appointment.publicId, { status });
      await load();
    } catch (updateError) {
      setError(updateError instanceof Error ? updateError.message : "Không thể cập nhật lịch hẹn.");
    } finally {
      setBusy("");
    }
  };

  if (isBootstrapping) return <div className="grid min-h-screen place-items-center pt-24">Đang kiểm tra đăng nhập...</div>;
  if (!isAuthenticated) return <Navigate to="/auth" replace state={{ from: "/appointments" }} />;

  return <main className="min-h-screen bg-[#fff8f5] px-5 pb-24 pt-32"><div className="mx-auto max-w-5xl">
    <p className="text-xs font-bold uppercase tracking-[.2em] text-violet-600">Marketplace</p>
    <h1 className="mt-2 text-4xl font-black tracking-tight text-slate-950">Lịch xem xe</h1>
    <p className="mt-3 text-slate-500">Theo dõi và phản hồi các yêu cầu xem xe của bạn.</p>
    {error ? <p className="mt-6 rounded-2xl bg-red-50 p-4 text-red-700">{error}</p> : null}
    <div className="mt-8 grid gap-4">{appointments.length ? appointments.map((appointment) => {
      const isBuyer = Number(appointment.buyerId) === Number(user?.id);
      const isSeller = Number(appointment.sellerId) === Number(user?.id);
      return <article key={appointment.publicId} className="rounded-3xl border border-violet-100 bg-white p-6 shadow-sm">
        <div className="flex flex-wrap items-start justify-between gap-4"><div><p className="text-lg font-extrabold text-slate-950">{appointment.listingTitle}</p><p className="mt-1 text-sm text-slate-500">{isSeller ? `Người mua: ${appointment.buyerName}` : `Người bán: ${appointment.sellerName}`}</p></div><span className="rounded-full bg-violet-50 px-4 py-2 text-xs font-bold text-violet-700">{statusLabel[appointment.status]}</span></div>
        <div className="mt-5 grid gap-3 text-sm text-slate-600 sm:grid-cols-2"><p className="flex items-center gap-2"><Clock3 size={16} className="text-violet-600" />{new Date(appointment.scheduledAt).toLocaleString("vi-VN")}</p><p className="flex items-center gap-2"><MapPin size={16} className="text-violet-600" />{appointment.location}</p></div>
        {appointment.note ? <p className="mt-3 rounded-xl bg-slate-50 p-3 text-sm text-slate-600">Ghi chú: {appointment.note}</p> : null}
        <div className="mt-5 flex flex-wrap gap-2">{isSeller && appointment.status === "REQUESTED" ? <><button disabled={busy === appointment.publicId} onClick={() => void update(appointment, "CONFIRMED")} className="flex items-center gap-2 rounded-full bg-emerald-600 px-4 py-2 text-sm font-bold text-white"><Check size={15} />Xác nhận</button><button disabled={busy === appointment.publicId} onClick={() => void update(appointment, "DECLINED")} className="flex items-center gap-2 rounded-full bg-red-50 px-4 py-2 text-sm font-bold text-red-700"><X size={15} />Từ chối</button></> : null}{(isBuyer || isSeller) && ["REQUESTED", "CONFIRMED"].includes(appointment.status) ? <button disabled={busy === appointment.publicId} onClick={() => void update(appointment, "CANCELLED")} className="rounded-full border border-slate-200 px-4 py-2 text-sm font-bold text-slate-600">Hủy lịch</button> : null}{(isBuyer || isSeller) && appointment.status === "CONFIRMED" ? <button disabled={busy === appointment.publicId} onClick={() => void update(appointment, "COMPLETED")} className="rounded-full bg-violet-600 px-4 py-2 text-sm font-bold text-white">Đánh dấu đã xem xe</button> : null}</div>
      </article>;
    }) : <div className="rounded-3xl border border-dashed border-violet-200 p-10 text-center text-slate-500"><CalendarDays className="mx-auto mb-3 text-violet-300" />Chưa có lịch xem xe nào.</div>}</div>
  </div></main>;
};

export default MarketplaceAppointmentsPage;
