import { useEffect, useMemo, useRef, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { Car, ChevronLeft, ChevronRight, Heart, Images, MapPin } from "lucide-react";
import { useAuth } from "@ebike/shared-code/hooks";
import { marketplaceAPI, formatMarketplacePrice, resolveMarketplaceMediaUrl, type MarketplaceListing } from "../services/marketplace";

const conditions: Record<string, string> = { NEW: "Mới", LIKE_NEW: "Như mới", USED: "Đã sử dụng", RESTORED: "Đã phục hồi", DAMAGED: "Cần sửa chữa" };

function ListingRow({ title, items, empty, favorites, busy, onFavorite, allowExpand = false }: {
  title: string; items: MarketplaceListing[]; empty: string; favorites: Set<string>; busy: Set<string>;
  onFavorite: (id: string) => void; allowExpand?: boolean;
}) {
  const track = useRef<HTMLDivElement>(null);
  const [expanded, setExpanded] = useState(false);
  return <section className="mt-8 rounded-[28px] border border-violet-100 bg-white p-5 shadow-sm sm:p-7">
    <div className="mb-5 flex items-center justify-between gap-3">
      <h2 className="text-lg font-extrabold tracking-tight sm:text-xl">{title}</h2>
      {!expanded && items.length > 1 && <div className="flex shrink-0 gap-2">
        <button type="button" aria-label={`Cuộn sang trái: ${title}`} onClick={() => track.current?.scrollBy({ left: -track.current.clientWidth * .8, behavior: "smooth" })} className="rounded-full border border-violet-100 p-2 text-violet-700 hover:bg-violet-50"><ChevronLeft size={19} /></button>
        <button type="button" aria-label={`Cuộn sang phải: ${title}`} onClick={() => track.current?.scrollBy({ left: track.current.clientWidth * .8, behavior: "smooth" })} className="rounded-full border border-violet-100 p-2 text-violet-700 hover:bg-violet-50"><ChevronRight size={19} /></button>
      </div>}
    </div>
    {!items.length ? <p className="rounded-2xl bg-slate-50 p-6 text-sm text-slate-500">{empty}</p> : <div ref={track} className={expanded ? "grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-4 xl:grid-cols-5" : "flex snap-x snap-mandatory gap-5 overflow-x-auto pb-4"}>
      {items.map(item => <article key={item.publicId} className={expanded ? "min-w-0" : "w-[210px] shrink-0 snap-start sm:w-[225px]"}>
        <div className="relative overflow-hidden rounded-2xl bg-violet-50">
          <Link to={`/listing/${item.publicId}`} className="group block aspect-square">
            {item.imageUrl ? /\.(mp4|webm)(?:\?|$)/i.test(item.imageUrl) ? <video src={resolveMarketplaceMediaUrl(item.imageUrl)} muted playsInline preload="metadata" className="h-full w-full object-cover" /> : <img src={resolveMarketplaceMediaUrl(item.imageUrl)} alt={item.title} loading="lazy" className="h-full w-full object-cover transition duration-300 group-hover:scale-105" /> : <div className="grid h-full place-items-center"><Car size={44} className="text-violet-300" /></div>}
          </Link>
          <button type="button" disabled={busy.has(item.publicId)} onClick={() => onFavorite(item.publicId)} aria-label={`${favorites.has(item.publicId) ? "Bỏ yêu thích" : "Yêu thích"}: ${item.title}`} aria-pressed={favorites.has(item.publicId)} className="absolute right-3 top-3 rounded-full bg-white/95 p-2 text-violet-700 shadow disabled:opacity-50"><Heart size={18} fill={favorites.has(item.publicId) ? "currentColor" : "none"} /></button>
          {!!item.images?.length && <span className="absolute bottom-3 right-3 flex items-center gap-1 rounded-md bg-black/60 px-2 py-1 text-xs text-white"><Images size={13} />{item.images.length}</span>}
        </div>
        <Link to={`/listing/${item.publicId}`} className="mt-3 block">
          <h3 className="line-clamp-2 min-h-12 font-bold leading-6 hover:text-violet-700">{item.title}</h3>
          <p className="mt-1 truncate text-xs text-slate-500">{[item.manufactureYear, conditions[item.condition], item.mileageKm != null ? `${item.mileageKm.toLocaleString("vi-VN")} km` : null].filter(Boolean).join(" · ")}</p>
          <p className="mt-2 text-lg font-extrabold text-violet-700">{formatMarketplacePrice(item.price)}</p>
          <p className="mt-2 flex items-start gap-1 text-xs leading-5 text-slate-500"><MapPin size={14} className="mt-0.5 shrink-0" />{[item.district, item.province].filter(Boolean).join(", ")}</p>
        </Link>
      </article>)}
    </div>}
    {allowExpand && items.length > 0 && <div className="mt-5 text-center"><button type="button" onClick={() => setExpanded(value => !value)} className="rounded-full border border-violet-200 px-8 py-2.5 text-sm font-bold text-violet-700 hover:bg-violet-50">{expanded ? "Thu gọn" : `Xem tất cả ${items.length} tin khác từ người bán`}</button></div>}
  </section>;
}

export default function RelatedMarketplaceListings({ listing }: { listing: MarketplaceListing }) {
  const { isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const [items, setItems] = useState<MarketplaceListing[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [reload, setReload] = useState(0);
  const [favorites, setFavorites] = useState<Set<string>>(new Set());
  const [busy, setBusy] = useState<Set<string>>(new Set());
  const [actionError, setActionError] = useState("");
  useEffect(() => {
    let active = true;
    setLoading(true); setError("");
    marketplaceAPI.listings().then(data => { if (active) setItems(data); })
      .catch(() => { if (active) setError("Không thể tải các tin liên quan. Vui lòng thử lại."); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [listing.publicId, reload]);
  useEffect(() => {
    let active = true;
    setFavorites(new Set());
    if (isAuthenticated) marketplaceAPI.favoriteListings().then(data => { if (active) setFavorites(new Set(data.map(item => item.publicId))); }).catch(() => undefined);
    return () => { active = false; };
  }, [isAuthenticated]);
  const { sellerItems, similarItems } = useMemo(() => {
    const others = items.filter(item => item.status === "PUBLISHED" && item.publicId !== listing.publicId);
    const similarity = (item: MarketplaceListing) =>
      (listing.brandId != null && item.brandId === listing.brandId ? 4 : 0)
      + (item.condition === listing.condition ? 2 : 0)
      + (item.province === listing.province ? 1 : 0)
      + 3 / (1 + Math.abs(item.price - listing.price) / Math.max(listing.price, 1));
    return {
      sellerItems: others.filter(item => item.sellerId === listing.sellerId),
      similarItems: others.filter(item => item.categoryId === listing.categoryId && item.sellerId !== listing.sellerId)
        .sort((a, b) => similarity(b) - similarity(a) || a.publicId.localeCompare(b.publicId)).slice(0, 12),
    };
  }, [items, listing]);
  const toggleFavorite = async (id: string) => {
    if (!isAuthenticated) { navigate("/auth", { state: { from: `/listing/${listing.publicId}` } }); return; }
    if (busy.has(id)) return;
    setBusy(current => new Set(current).add(id)); setActionError("");
    try {
      const result = await marketplaceAPI.toggleFavorite(id);
      setFavorites(current => { const next = new Set(current); result.saved ? next.add(id) : next.delete(id); return next; });
    } catch { setActionError("Không thể cập nhật yêu thích. Vui lòng thử lại."); }
    finally { setBusy(current => { const next = new Set(current); next.delete(id); return next; }); }
  };
  if (loading) return <div role="status" className="mt-8 rounded-3xl bg-white p-8 text-sm text-slate-500">Đang tải tin khác của người bán và tin tương tự...</div>;
  if (error) return <div role="alert" className="mt-8 rounded-3xl bg-white p-8 text-sm text-red-600">{error}<button type="button" onClick={() => setReload(value => value + 1)} className="ml-3 font-bold underline">Thử lại</button></div>;
  return <div>
    {actionError && <p role="alert" className="mt-6 text-sm text-red-600">{actionError}</p>}
    <ListingRow title={`Tin rao khác của ${listing.sellerName}`} items={sellerItems} empty="Người bán chưa có tin công khai nào khác." favorites={favorites} busy={busy} onFavorite={id => void toggleFavorite(id)} allowExpand />
    <ListingRow title="Tin đăng tương tự" items={similarItems} empty="Chưa có tin tương tự cùng loại xe từ người bán khác." favorites={favorites} busy={busy} onFavorite={id => void toggleFavorite(id)} />
  </div>;
}
