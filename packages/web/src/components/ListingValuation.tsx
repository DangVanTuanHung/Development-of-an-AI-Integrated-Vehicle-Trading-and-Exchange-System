import { useEffect, useId, useRef, useState } from "react";
import { Info } from "lucide-react";
import { apiClient } from "@ebike/shared-code/api";
import { formatMarketplacePrice } from "../services/marketplace";

type Reference = { sourceName: string; sourceDate: string };
type Estimate = { status: string; low: number | null; high: number | null; explanation: string; estimatedAt: string | null; sampleCount: number; references: Reference[] };
const date = (value: string) => new Date(value + "T00:00:00").toLocaleDateString("vi-VN");
const compactPrice = (value: number) => new Intl.NumberFormat("vi-VN", { maximumFractionDigits: 2, useGrouping: false }).format(value / (value >= 1e9 ? 1e9 : 1e6)) + (value >= 1e9 ? " tỷ" : " tr");

export default function ListingValuation({ publicId, price }: { publicId: string; price: number }) {
  const [estimate, setEstimate] = useState<Estimate | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const [attempt, setAttempt] = useState(0);
  const [infoOpen, setInfoOpen] = useState(false);
  const infoRef = useRef<HTMLDivElement>(null);
  const infoId = useId();
  useEffect(() => {
    let active = true;
    setLoading(true); setError(false); setEstimate(null); setInfoOpen(false);
    apiClient.get<Estimate>(`/marketplace/listings/${publicId}/valuation`, { timeout: 15000 })
      .then(response => { if (active) setEstimate(response.data); })
      .catch(() => { if (active) setError(true); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [publicId, attempt]);
  useEffect(() => {
    if (!infoOpen) return;
    const close = (event: PointerEvent) => { if (!infoRef.current?.contains(event.target as Node)) setInfoOpen(false); };
    const escape = (event: KeyboardEvent) => { if (event.key === "Escape") setInfoOpen(false); };
    document.addEventListener("pointerdown", close);
    document.addEventListener("keydown", escape);
    return () => { document.removeEventListener("pointerdown", close); document.removeEventListener("keydown", escape); };
  }, [infoOpen]);
  const valid = estimate?.status === "AVAILABLE" && estimate.low != null && estimate.high != null && estimate.high >= estimate.low;
  const low = estimate?.low ?? 0, high = estimate?.high ?? 0;
  const marker = high > low ? Math.max(0, Math.min(100, 20 + 60 * (price - low) / (high - low))) : price < low ? 0 : price > high ? 100 : 50;
  const labelPosition = Math.max(20, Math.min(80, marker));
  const sources = [...new Set(estimate?.references?.map(ref => ref.sourceName) ?? [])];
  return <section className="relative mt-7 rounded-[20px] border border-violet-100 bg-gradient-to-br from-[#f7f3ff] via-[#fcfaff] to-white px-5 pb-3 pt-4 shadow-[0_4px_18px_rgba(109,40,217,0.035)]" aria-label="Khoảng giá tham khảo">
    <div className="flex items-center gap-1.5">
      <h2 className="text-[13px] font-bold leading-5 tracking-tight text-[#3e3455]"><span aria-hidden="true" className="mr-2 inline-flex gap-0.5 align-middle"><span className="h-3 w-1 -skew-x-12 rounded-full bg-violet-600" /><span className="h-3 w-1 -skew-x-12 rounded-full bg-fuchsia-400" /></span>Khoảng giá tham khảo</h2>
      <div ref={infoRef} onMouseEnter={() => setInfoOpen(true)} onMouseLeave={() => { if (!infoRef.current?.contains(document.activeElement)) setInfoOpen(false); }} onBlur={event => { if (!event.currentTarget.contains(event.relatedTarget as Node)) setInfoOpen(false); }}>
        <button type="button" aria-label="Thông tin nguồn giá tham khảo" aria-expanded={infoOpen} aria-controls={infoId} onClick={() => setInfoOpen(value => !value)} onKeyDown={event => { if (event.key === "ArrowDown") setInfoOpen(true); }} className="grid h-5 w-5 place-items-center rounded-full text-violet-400 hover:text-violet-700 focus-visible:outline focus-visible:outline-2 focus-visible:outline-violet-500"><Info size={16} /></button>
        {infoOpen && <div id={infoId} role="note" className="absolute inset-x-4 top-8 z-30 pt-2">
          <div className="rounded-2xl bg-[#211b35] p-4 text-xs leading-5 text-slate-200 shadow-xl">
            <p className="mb-2 font-bold text-white">Về khoảng giá tham khảo</p>
            <p>{sources.length ? `Nguồn: ${sources.join(", ")}. Tổng hợp từ ${estimate?.sampleCount} mẫu giá rao tương đồng đã thu thập.` : "Khoảng giá được tổng hợp từ các tin rao công khai có thông tin xe tương đồng. Hiện chưa đủ nguồn phù hợp cho xe này."}</p>
            {estimate?.estimatedAt && <p className="mt-2">Dữ liệu nguồn gần nhất: {date(estimate.estimatedAt)}.</p>}
            <p className="mt-2">Khoảng giữa 50% giá rao sau khi lọc mẫu trùng và giá bất thường. Không phải giá giao dịch đã chốt; dữ liệu được cập nhật theo từng đợt, không theo thời gian thực.</p>
            <p className="mt-2 text-slate-400">Giá thực tế còn phụ thuộc phiên bản, số km, khu vực và tình trạng xe.</p>
          </div>
        </div>}
      </div>
    </div>
    {loading ? <p role="status" className="py-5 text-xs text-slate-500">Đang tải giá tham khảo...</p> : valid ? (
      <div className="relative mt-2 h-[70px]" role="img" aria-label={`Khoảng tham khảo ${formatMarketplacePrice(low)} đến ${formatMarketplacePrice(high)}; giá đăng ${formatMarketplacePrice(price)}.`}>
        <div className="absolute inset-x-0 top-[35px] h-1.5 rounded-full bg-violet-100/70" />
        <div className="absolute top-[35px] h-1.5 rounded-full bg-gradient-to-r from-violet-600 to-fuchsia-400" style={{ left: high > low ? "20%" : "50%", width: high > low ? "60%" : "2px" }} />
        <div className="absolute top-[23px] h-[14px] border-l border-violet-300" style={{ left: `${marker}%` }} />
        <span className="absolute top-0 whitespace-nowrap rounded-full bg-[#36234f] px-2.5 py-1 text-[11px] font-bold leading-4 text-white shadow-sm" style={{ left: `${labelPosition}%`, transform: "translateX(-50%)" }}><span className="mr-1 font-normal text-violet-200">Giá đăng</span>{compactPrice(price)}</span>
        <span className="absolute top-[32px] h-3 w-3 -translate-x-1/2 rounded-full border-[3px] border-white bg-[#36234f] shadow-[0_0_0_1px_rgba(109,40,217,0.2)]" style={{ left: `${marker}%` }} />
        {high > low ? <>
          <span className="absolute left-[20%] top-[51px] -translate-x-1/2 whitespace-nowrap text-[11px] leading-4 font-medium text-[#71657f]">{compactPrice(low)}</span>
          <span className="absolute left-[80%] top-[51px] -translate-x-1/2 whitespace-nowrap text-[11px] leading-4 font-medium text-[#71657f]">{compactPrice(high)}</span>
        </> : <span className="absolute left-1/2 top-[51px] -translate-x-1/2 whitespace-nowrap text-[11px] leading-4 font-medium text-[#71657f]">{compactPrice(low)}</span>}
      </div>
    ) : <p role="status" className="py-4 text-xs leading-5 text-slate-500">{error ? "Chưa tải được giá tham khảo. Bạn thử lại nhé." : "Chưa đủ dữ liệu để tham khảo giá xe này."}</p>}
    {!loading && error && <button type="button" onClick={() => setAttempt(value => value + 1)} className="mt-3 text-sm font-bold text-violet-700 underline">Thử lại</button>}
  </section>;
}
