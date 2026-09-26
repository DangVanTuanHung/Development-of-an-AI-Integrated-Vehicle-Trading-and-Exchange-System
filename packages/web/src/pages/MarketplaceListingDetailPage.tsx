import ListingValuation from "../components/ListingValuation";
import RelatedMarketplaceListings from "../components/RelatedMarketplaceListings";
import { ArrowLeft, BadgeCheck, CalendarDays, Flag, Heart, MapPin, MessageCircle, Phone, Printer, ShieldCheck, Star } from "lucide-react";
import { type FormEvent, useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { useAuth } from "@ebike/shared-code/hooks";
import {
  formatMarketplacePrice,
  marketplaceAPI,
  resolveMarketplaceMediaUrl,
  type MarketplaceListing,
  type MarketplaceSellerReputation,
} from "../services/marketplace";

const MarketplaceListingDetailPage = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user, isAuthenticated } = useAuth();
  const [listing, setListing] = useState<MarketplaceListing | null>(null);
  const [error, setError] = useState("");
  const [offerOpen, setOfferOpen] = useState(false);
  const [offerAmount, setOfferAmount] = useState("");
  const [offerMessage, setOfferMessage] = useState("");
  const [offerStatus, setOfferStatus] = useState("");
  const [offerSaving, setOfferSaving] = useState(false);
  const [appointmentOpen, setAppointmentOpen] = useState(false);
  const [appointmentDate, setAppointmentDate] = useState("");
  const [appointmentLocation, setAppointmentLocation] = useState("");
  const [appointmentNote, setAppointmentNote] = useState("");
  const [appointmentStatus, setAppointmentStatus] = useState("");
  const [appointmentSaving, setAppointmentSaving] = useState(false);
  const [chatStatus, setChatStatus] = useState("");
  const [chatLoading, setChatLoading] = useState(false);
  const [phoneVisible, setPhoneVisible] = useState(false);
  const [activeImage, setActiveImage] = useState(0);
  const [saved, setSaved] = useState(false);
  const [reputation, setReputation] = useState<MarketplaceSellerReputation | null>(null);
  const [actionStatus, setActionStatus] = useState("");
  const [loanPercent, setLoanPercent] = useState(50);
  const [loanMonths, setLoanMonths] = useState(60);
  const [interestRate, setInterestRate] = useState(9);
  useEffect(() => {
    let active = true;
    setListing(null); setError(""); setActiveImage(0); setPhoneVisible(false);
    setOfferOpen(false); setOfferAmount(""); setOfferMessage(""); setOfferStatus("");
    setAppointmentOpen(false); setAppointmentDate(""); setAppointmentLocation(""); setAppointmentNote(""); setAppointmentStatus("");
    setChatStatus(""); setActionStatus(""); setSaved(false);
    if (id) marketplaceAPI.detail(id)
      .then(value => { if (active) setListing(value); })
      .catch(() => { if (active) setError("Không thể tải tin đăng."); });
    return () => { active = false; };
  }, [id]);
  useEffect(() => {
    let active = true;
    if (listing?.sellerId) marketplaceAPI.sellerReputation(listing.sellerId)
      .then(value => { if (active) setReputation(value); })
      .catch(() => { if (active) setReputation(null); });
    return () => { active = false; };
  }, [listing?.sellerId]);
  useEffect(() => {
    let active = true;
    setSaved(false);
    if (id && isAuthenticated) void marketplaceAPI.favoriteStatus(id)
      .then(value => { if (active) setSaved(value.favorite); })
      .catch(() => undefined);
    return () => { active = false; };
  }, [id, isAuthenticated]);
  if (error)
    return (
      <div className="grid min-h-screen place-items-center pt-24 text-red-600">
        {error}
      </div>
    );
  if (!listing)
    return (
      <div className="grid min-h-screen place-items-center pt-24 text-slate-500">
        Đang tải thông tin phương tiện...
      </div>
    );
  const isOwner = Number(user?.id) === Number(listing.sellerId);
  const gallery = listing.images?.length
    ? listing.images
    : listing.imageUrl
      ? [listing.imageUrl]
      : [];
  const specs = [
    ["Năm sản xuất", listing.manufactureYear],
    ["Năm đăng ký", listing.registrationYear],
    [
      "Số km đã đi",
      listing.mileageKm == null
        ? null
        : `${listing.mileageKm.toLocaleString("vi-VN")} km`,
    ],
    ["Nhiên liệu", listing.fuelType?.replace(/_/g, " ")],
    ["Hộp số", listing.transmission?.replace(/_/g, " ")],
    [
      "Dung tích động cơ",
      listing.engineCapacityCc == null
        ? null
        : `${listing.engineCapacityCc} cc`,
    ],
    ["Màu sắc", listing.exteriorColor],
    ["Số chỗ", listing.seats],
    ["Tầm hoạt động", listing.rangeKm == null ? null : `${listing.rangeKm} km`],
    ["Số đời chủ", listing.ownersCount],
    ["Xuất xứ", listing.origin],
  ].filter((item) => item[1] != null && item[1] !== "");
  const loanAmount = (listing.price * loanPercent) / 100;
  const monthlyRate = interestRate / 100 / 12;
  const monthlyPayment = monthlyRate
    ? (loanAmount * monthlyRate * Math.pow(1 + monthlyRate, loanMonths)) /
      (Math.pow(1 + monthlyRate, loanMonths) - 1)
      : loanAmount / loanMonths;
  const maskedPhone = listing.sellerPhone
    ? `${listing.sellerPhone.slice(0, 6)}${"*".repeat(Math.max(4, listing.sellerPhone.length - 6))}`
    : "";

  const submitOffer = async (event: FormEvent) => {
    event.preventDefault();
    const amount = Number(offerAmount);
    if (!amount || amount <= 0) return;
    setOfferSaving(true);
    setOfferStatus("");
    try {
      await marketplaceAPI.createOffer(listing.publicId, {
        amount,
        depositAmount: Math.round(amount * 0.1),
        message: offerMessage,
      });
      setOfferStatus(
        "Đề nghị đã được gửi. Tiền cọc dự kiến là 10% và chỉ thanh toán sau khi người bán chấp nhận.",
      );
    } catch (submitError) {
      setOfferStatus(
        submitError instanceof Error
          ? submitError.message
          : "Không thể gửi đề nghị lúc này.",
      );
    } finally {
      setOfferSaving(false);
    }
  };

  const openConversation = async () => {
    if (!isAuthenticated) {
      navigate("/auth", { state: { from: `/listing/${listing.publicId}` } });
      return;
    }
    setChatLoading(true);
    setChatStatus("");
    try {
      const conversation = await marketplaceAPI.openConversation(
        listing.publicId,
      );
      navigate(`/messages?conversation=${encodeURIComponent(conversation.publicId)}`);
    } catch (conversationError) {
      setChatStatus(
        conversationError instanceof Error
          ? conversationError.message
          : "Không thể mở hội thoại. Vui lòng đăng nhập và thử lại.",
      );
    } finally {
      setChatLoading(false);
    }
  };

  const submitAppointment = async (event: FormEvent) => {
    event.preventDefault();
    if (!appointmentDate || !appointmentLocation.trim()) return;
    setAppointmentSaving(true);
    setAppointmentStatus("");
    try {
      await marketplaceAPI.createAppointment(listing.publicId, {
        scheduledAt: new Date(appointmentDate).toISOString(),
        location: appointmentLocation,
        note: appointmentNote,
      });
      setAppointmentStatus("Đã gửi yêu cầu đặt lịch. Người bán sẽ xác nhận trong trung tâm giao dịch.");
      setAppointmentOpen(false);
    } catch (appointmentError) {
      setAppointmentStatus(appointmentError instanceof Error ? appointmentError.message : "Không thể đặt lịch lúc này.");
    } finally {
      setAppointmentSaving(false);
    }
  };

  return (
    <main className="min-h-screen bg-[#fff8f5] px-5 pb-24 pt-32 sm:px-8 lg:px-12">
      <div className="mx-auto max-w-[1400px]">
        <Link
          to="/products"
          className="mb-7 inline-flex items-center gap-2 text-sm font-bold text-violet-700"
        >
          <ArrowLeft size={17} />
          Quay lại marketplace
        </Link>
        {listing.status !== "PUBLISHED" && <div className="mb-5 rounded-xl bg-amber-50 p-4 text-sm text-amber-800">
          {listing.status === "PENDING_REVIEW" ? "Tin đang chờ kiểm duyệt và chưa hiển thị công khai." : listing.status === "REJECTED" ? "Tin cần chỉnh sửa trước khi gửi duyệt lại." : listing.status === "DRAFT" ? "Đây là bản nháp chưa gửi duyệt." : listing.status === "SUSPENDED" ? "Tin đã bị đình chỉ." : listing.status === "SOLD" ? "Xe đã bán." : listing.status === "RESERVED" ? "Xe đang có giao dịch." : "Tin không còn hiển thị."}
          {listing.moderationNote && <p className="mt-2">{listing.moderationNote}</p>}
        </div>}
        <div className="grid items-stretch gap-6 lg:grid-cols-[minmax(0,1.1fr)_minmax(0,.9fr)]">
          <div className="flex min-w-0 flex-col overflow-hidden rounded-[30px] bg-violet-50 p-3 shadow-[0_22px_70px_rgba(57,30,99,.13)]">
            <div className="relative flex h-[min(65svh,560px)] min-h-[240px] items-center justify-center overflow-hidden rounded-[22px] bg-white lg:h-auto lg:min-h-[480px] lg:flex-1">
            {gallery[activeImage] ? (
              /\.(mp4|webm)$/i.test(gallery[activeImage]) ? (
                <video
                  src={resolveMarketplaceMediaUrl(gallery[activeImage])}
                  controls
                  key={gallery[activeImage]}
                  className="absolute inset-0 h-full w-full object-contain"
                />
              ) : (
                <img
                  src={resolveMarketplaceMediaUrl(gallery[activeImage])}
                  alt={listing.title}
                  key={gallery[activeImage]}
                  decoding="async"
                  className="absolute inset-0 h-full w-full object-scale-down"
                />
              )
            ) : null}
            </div>
            {gallery[activeImage] && !/\.(mp4|webm)(?:\?|$)/i.test(gallery[activeImage]) ? (
              <div className="flex justify-end px-2 pt-3">
                <a href={resolveMarketplaceMediaUrl(gallery[activeImage])} target="_blank" rel="noopener noreferrer" className="rounded-lg px-3 py-2 text-sm font-bold text-violet-700 hover:bg-white focus-visible:outline focus-visible:outline-2 focus-visible:outline-violet-600">
                  Xem ảnh gốc
                </a>
              </div>
            ) : null}
            {gallery.length > 1 ? (
              <div className="flex shrink-0 gap-3 overflow-x-auto p-3">
                {gallery.map((url, index) => (
                  <button
                    type="button"
                    key={url}
                    aria-label={`Xem ảnh hoặc video ${index + 1}`}
                    aria-pressed={activeImage === index}
                    onClick={() => setActiveImage(index)}
                    className={`w-20 shrink-0 overflow-hidden rounded-xl border-2 bg-white sm:w-28 ${activeImage === index ? "border-violet-600" : "border-transparent"}`}
                  >
                    {/\.(mp4|webm)$/i.test(url) ? (
                      <video
                        src={resolveMarketplaceMediaUrl(url)}
                        muted
                        className="aspect-[4/3] w-full bg-white object-contain"
                      />
                    ) : (
                      <img
                        src={resolveMarketplaceMediaUrl(url)}
                        alt={`Ảnh ${index + 1}`}
                        className="aspect-[4/3] w-full object-scale-down"
                      />
                    )}
                  </button>
                ))}
              </div>
            ) : null}
          </div>
          <aside className="min-w-0 rounded-[30px] border border-violet-100 bg-white p-5 shadow-[0_18px_55px_rgba(57,30,99,.09)] sm:p-7">
            <div className="flex items-center justify-between">
              <span className="rounded-full bg-violet-50 px-3 py-1.5 text-xs font-bold uppercase tracking-wide text-violet-700">
                {listing.condition.replace(/_/g, " ")}
              </span>
              <button
                aria-label="Yêu thích"
                onClick={async () => {
                  try {
                    const value = await marketplaceAPI.toggleFavorite(
                      listing.publicId,
                    );
                    setSaved(value.saved);
                    setActionStatus(
                      value.saved
                        ? "Đã thêm vào yêu thích."
                        : "Đã bỏ khỏi yêu thích.",
                    );
                  } catch {
                    setActionStatus("Bạn cần đăng nhập để thêm vào yêu thích.");
                  }
                }}
                className={`flex items-center gap-2 rounded-full border px-4 py-2.5 text-sm font-bold transition ${saved ? "border-violet-600 bg-violet-600 text-white" : "border-violet-100 text-violet-700 hover:bg-violet-50"}`}
              >
                <Heart size={19} fill={saved ? "currentColor" : "none"} />
                {saved ? "Đã yêu thích" : "Yêu thích"}
              </button>
            </div>
            <h1 className="mt-5 break-words text-2xl font-extrabold leading-tight tracking-[-.035em] xl:text-3xl">
              {listing.title}
            </h1>
            <p className="mt-4 text-3xl font-extrabold text-violet-700">
              {formatMarketplacePrice(listing.price)}
            </p>
            <p className="mt-4 flex items-center gap-2 text-sm text-slate-500">
              <MapPin size={16} />
              {listing.district ? `${listing.district}, ` : ""}
              {listing.province}
            </p>
            <ListingValuation publicId={listing.publicId} price={listing.price} />
            <div className="mt-5 flex items-center gap-3 border-y border-violet-50 py-4">
              <span className="grid h-12 w-12 place-items-center rounded-full bg-[#171329] font-bold text-white">
                {listing.sellerName.charAt(0)}
              </span>
              <div>
                <p className="font-bold">{listing.sellerName}</p>
                <p className="flex items-center gap-1 text-xs text-emerald-600">
                  <BadgeCheck size={14} />
                  Người bán đã xác thực
                </p>
                {reputation ? <p className="mt-1 flex items-center gap-1 text-xs text-amber-600"><Star size={13} fill="currentColor" />{Number(reputation.summary.averageRating).toFixed(2)} / 5 · {reputation.summary.reviewCount} đánh giá</p> : null}
                {!listing.sellerPhone ? (
                  <p className="mt-1 text-xs text-slate-400">
                    Người bán chưa cập nhật số điện thoại
                  </p>
                ) : null}
              </div>
            </div>
            <div className="mt-5 grid gap-3 sm:grid-cols-2">
              <button
                onClick={() => void openConversation()}
                disabled={chatLoading || isOwner}
                className="flex min-h-14 items-center justify-center gap-2 rounded-2xl bg-gradient-to-r from-violet-600 to-fuchsia-500 px-5 py-4 text-sm font-bold text-white shadow-lg shadow-violet-500/20 disabled:cursor-not-allowed disabled:opacity-50"
              >
                <MessageCircle size={19} />
                {isOwner ? "Bạn là người đăng tin" : chatLoading ? "Đang mở..." : "Trao đổi"}
              </button>
              {listing.sellerPhone ? (
                <button
                  type="button"
                  onClick={() => setPhoneVisible((value) => !value)}
                  className="flex min-h-14 items-center justify-center gap-2 rounded-2xl bg-gradient-to-r from-violet-600 to-fuchsia-500 px-5 py-4 text-sm font-extrabold text-white shadow-lg shadow-violet-500/20 transition hover:brightness-105"
                  aria-label={phoneVisible ? "Ẩn số điện thoại" : "Hiện số điện thoại"}
                >
                  <Phone size={19} />
                  {phoneVisible ? listing.sellerPhone : `Hiện số ${maskedPhone}`}
                </button>
              ) : null}
            </div>
            {chatStatus ? (
              <p className="mt-3 rounded-xl bg-red-50 px-4 py-3 text-sm text-red-600">
                {chatStatus}
              </p>
            ) : null}
            <button
              disabled={isOwner || listing.status !== "PUBLISHED"}
              onClick={() => {
                setOfferOpen((value) => !value);
                if (!offerAmount) setOfferAmount(String(listing.price));
              }}
              className="mt-3 flex w-full items-center justify-center gap-2 rounded-2xl bg-[#171329] px-6 py-4 font-bold text-white disabled:cursor-not-allowed disabled:opacity-50"
            >
              <ShieldCheck size={19} />
              {isOwner ? "Không thể tự gửi đề nghị" : "Gửi đề nghị & đặt cọc"}
            </button>
            <button
              type="button"
              disabled={isOwner || listing.status !== "PUBLISHED"}
              onClick={() => {
                if (!isAuthenticated) { navigate("/auth", { state: { from: `/listing/${listing.publicId}` } }); return; }
                setAppointmentOpen((value) => !value);
              }}
              className="mt-3 flex w-full items-center justify-center gap-2 rounded-2xl border border-violet-200 bg-white px-6 py-4 font-bold text-violet-700 disabled:cursor-not-allowed disabled:opacity-50"
            >
              <CalendarDays size={19} />Đặt lịch xem xe
            </button>
            {appointmentStatus ? <p className="mt-3 rounded-xl bg-emerald-50 px-4 py-3 text-sm text-emerald-700">{appointmentStatus}</p> : null}
            {appointmentOpen ? <form onSubmit={submitAppointment} className="mt-4 space-y-3 rounded-2xl border border-violet-100 bg-violet-50/60 p-4">
              <label className="block text-xs font-bold uppercase tracking-wide text-violet-700">Ngày và giờ<input required type="datetime-local" value={appointmentDate} onChange={(event) => setAppointmentDate(event.target.value)} min={new Date(Date.now() + 3600000).toISOString().slice(0, 16)} className="mt-2 w-full rounded-xl border border-violet-100 bg-white px-4 py-3 text-sm text-slate-900" /></label>
              <label className="block text-xs font-bold uppercase tracking-wide text-violet-700">Địa điểm xem xe<input required maxLength={500} value={appointmentLocation} onChange={(event) => setAppointmentLocation(event.target.value)} placeholder="Ví dụ: 12 Nguyễn Trãi, Quận 1" className="mt-2 w-full rounded-xl border border-violet-100 bg-white px-4 py-3 text-sm text-slate-900" /></label>
              <textarea maxLength={2000} value={appointmentNote} onChange={(event) => setAppointmentNote(event.target.value)} rows={2} placeholder="Ghi chú cho người bán" className="w-full resize-none rounded-xl border border-violet-100 bg-white px-4 py-3 text-sm" />
              <button disabled={appointmentSaving} className="w-full rounded-xl bg-violet-600 px-4 py-3 text-sm font-bold text-white disabled:opacity-50">{appointmentSaving ? "Đang gửi..." : "Gửi yêu cầu đặt lịch"}</button>
            </form> : null}
            {!isAuthenticated ? (
              <p className="mt-3 text-center text-sm text-violet-700">
                Bạn cần đăng nhập để trao đổi hoặc gửi đề nghị.
              </p>
            ) : null}
            {isOwner ? (
              <p className="mt-3 rounded-xl bg-amber-50 px-4 py-3 text-sm text-amber-700">
                Đây là tin do bạn đăng. Hãy dùng tài khoản người mua khác để thử
                gửi đề nghị.
              </p>
            ) : null}
            {offerOpen ? (
              <form
                onSubmit={submitOffer}
                className="mt-4 space-y-3 rounded-2xl border border-violet-100 bg-violet-50/60 p-4"
              >
                <label className="block text-xs font-bold uppercase tracking-wide text-violet-700">
                  Mức giá đề nghị
                  <input
                    required
                    min="1"
                    type="number"
                    value={offerAmount}
                    onChange={(event) => setOfferAmount(event.target.value)}
                    className="mt-2 w-full rounded-xl border border-violet-100 bg-white px-4 py-3 text-base text-slate-900 outline-none"
                  />
                </label>
                <textarea
                  value={offerMessage}
                  onChange={(event) => setOfferMessage(event.target.value)}
                  rows={3}
                  placeholder="Lời nhắn cho người bán"
                  className="w-full resize-none rounded-xl border border-violet-100 bg-white px-4 py-3 text-sm outline-none"
                />
                <p className="text-xs leading-5 text-slate-500">
                  Tiền cọc dự kiến:{" "}
                  {formatMarketplacePrice(
                    Math.round((Number(offerAmount) || 0) * 0.1),
                  )}
                  . Chưa thu tiền ở bước này.
                </p>
                <button
                  disabled={offerSaving}
                  className="w-full rounded-xl bg-gradient-to-r from-violet-600 to-fuchsia-500 px-4 py-3 text-sm font-bold text-white disabled:opacity-50"
                >
                  {offerSaving ? "Đang gửi..." : "Xác nhận đề nghị"}
                </button>
                {offerStatus ? (
                  <p className="text-xs leading-5 text-violet-700">
                    {offerStatus}
                  </p>
                ) : null}
              </form>
            ) : null}
            <div className="mt-5 grid grid-cols-2 gap-3">
              <button
                type="button"
                onClick={() => window.print()}
                className="flex items-center justify-center gap-2 rounded-xl border border-violet-100 px-3 py-3 text-sm font-bold text-slate-600"
              >
                <Printer size={16} />
                In tin
              </button>
              <button
                type="button"
                onClick={async () => {
                  const reason = window.prompt("Lý do báo cáo tin này:");
                  if (!reason) return;
                  try {
                    await marketplaceAPI.reportListing(
                      listing.publicId,
                      reason,
                    );
                    setActionStatus("Đã gửi báo cáo tới quản trị viên.");
                  } catch {
                    setActionStatus("Bạn cần đăng nhập để báo cáo tin.");
                  }
                }}
                className="flex items-center justify-center gap-2 rounded-xl border border-violet-100 px-3 py-3 text-sm font-bold text-slate-600"
              >
                <Flag size={16} />
                Báo cáo
              </button>
            </div>
            {actionStatus ? (
              <p className="mt-3 text-center text-xs text-violet-700">
                {actionStatus}
              </p>
            ) : null}
          </aside>
        </div>
        {specs.length ? (
          <section className="mt-10 rounded-[30px] border border-violet-100 bg-white p-8 sm:p-10">
            <p className="text-xs font-bold uppercase tracking-[.18em] text-violet-600">
              Thông số kỹ thuật
            </p>
            <div className="mt-6 grid gap-x-12 sm:grid-cols-2">
              {specs.map(([label, value]) => (
                <div
                  key={String(label)}
                  className="flex justify-between gap-5 border-b border-violet-50 py-4"
                >
                  <span className="text-slate-500">{label}</span>
                  <b className="text-right text-slate-800">{value}</b>
                </div>
              ))}
            </div>
          </section>
        ) : null}
        <section className="mt-10 rounded-[30px] border border-violet-100 bg-white p-8 sm:p-10">
          <p className="text-xs font-bold uppercase tracking-[.18em] text-violet-600">
            Mô tả phương tiện
          </p>
          <p className="mt-5 max-w-4xl whitespace-pre-wrap text-base leading-8 text-slate-600">
            {listing.description}
          </p>
        </section>
        <section className="mt-10 rounded-[30px] border border-violet-100 bg-white p-8 sm:p-10">
          <div className="grid gap-8 lg:grid-cols-[1fr_.65fr]">
            <div>
              <p className="text-xs font-bold uppercase tracking-[.18em] text-violet-600">
                Công cụ tài chính
              </p>
              <h2 className="mt-2 text-2xl font-extrabold">
                Ước tính khoản vay mua xe
              </h2>
              <div className="mt-6 grid gap-4 sm:grid-cols-3">
                <label className="text-sm font-bold text-slate-600">
                  Khoản vay (% giá xe)
                  <input
                    type="number"
                    min="10"
                    max="90"
                    value={loanPercent}
                    onChange={(event) =>
                      setLoanPercent(Number(event.target.value))
                    }
                    className="input-base mt-2"
                  />
                </label>
                <label className="text-sm font-bold text-slate-600">
                  Lãi suất (%/năm)
                  <input
                    type="number"
                    min="0"
                    step="0.1"
                    value={interestRate}
                    onChange={(event) =>
                      setInterestRate(Number(event.target.value))
                    }
                    className="input-base mt-2"
                  />
                </label>
                <label className="text-sm font-bold text-slate-600">
                  Thời hạn (tháng)
                  <select
                    value={loanMonths}
                    onChange={(event) =>
                      setLoanMonths(Number(event.target.value))
                    }
                    className="input-base mt-2"
                  >
                    <option value="12">12 tháng</option>
                    <option value="24">24 tháng</option>
                    <option value="36">36 tháng</option>
                    <option value="48">48 tháng</option>
                    <option value="60">60 tháng</option>
                    <option value="84">84 tháng</option>
                  </select>
                </label>
              </div>
              <p className="mt-4 text-xs leading-5 text-slate-400">
                Số liệu chỉ mang tính tham khảo. Khoản vay thực tế phụ thuộc
                ngân hàng và hồ sơ người mua.
              </p>
            </div>
            <div className="rounded-3xl bg-gradient-to-br from-[#171329] to-violet-900 p-7 text-white">
              <p className="text-sm text-white/60">Trả góp dự kiến mỗi tháng</p>
              <p className="mt-2 text-3xl font-extrabold">
                {formatMarketplacePrice(Math.round(monthlyPayment))}
              </p>
              <div className="my-6 h-px bg-white/10" />
              <div className="space-y-3 text-sm">
                <p className="flex justify-between">
                  <span className="text-white/60">Số tiền vay</span>
                  <b>{formatMarketplacePrice(loanAmount)}</b>
                </p>
                <p className="flex justify-between">
                  <span className="text-white/60">Trả trước</span>
                  <b>{formatMarketplacePrice(listing.price - loanAmount)}</b>
                </p>
              </div>
            </div>
          </div>
        </section>
        <RelatedMarketplaceListings key={listing.publicId} listing={listing} />
      </div>
    </main>
  );
};

export default MarketplaceListingDetailPage;
