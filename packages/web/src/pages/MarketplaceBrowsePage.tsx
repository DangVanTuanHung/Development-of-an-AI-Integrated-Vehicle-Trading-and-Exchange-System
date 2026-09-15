import { ArrowRight, ArrowUpRight, Car, Heart, MapPin, Search, ShieldCheck, SlidersHorizontal, Sparkles, Zap } from "lucide-react";
import { type MouseEvent, useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "@ebike/shared-code/hooks";
import { formatMarketplacePrice, marketplaceAPI, resolveMarketplaceMediaUrl, type MarketplaceCategory, type MarketplaceListing } from "../services/marketplace";

const conditionLabel: Record<string, string> = { NEW: "Mới", LIKE_NEW: "Như mới", USED: "Đã sử dụng", RESTORED: "Phục hồi", DAMAGED: "Cần sửa chữa" };

const MarketplaceBrowsePage = () => {
  const { isAuthenticated } = useAuth();
  const [listings, setListings] = useState<MarketplaceListing[]>([]);
  const [categories, setCategories] = useState<MarketplaceCategory[]>([]);
  const [search, setSearch] = useState("");
  const [category, setCategory] = useState<number | null>(null);
  const [province, setProvince] = useState("");
  const [condition, setCondition] = useState("");
  const [minPrice, setMinPrice] = useState("");
  const [maxPrice, setMaxPrice] = useState("");
  const [loading, setLoading] = useState(true);
  const [favoriteIds, setFavoriteIds] = useState<Set<string>>(new Set());

  useEffect(() => {
    Promise.all([marketplaceAPI.listings(), marketplaceAPI.categories()])
      .then(([listingData, categoryData]) => { setListings(listingData); setCategories(categoryData); })
      .finally(() => setLoading(false));
  }, []);
  useEffect(() => { if (isAuthenticated) void marketplaceAPI.favoriteListings().then((items) => setFavoriteIds(new Set(items.map((item) => item.publicId)))); }, [isAuthenticated]);

  const toggleFavorite = async (event: MouseEvent, item: MarketplaceListing) => {
    event.preventDefault(); event.stopPropagation();
    if (!isAuthenticated) { window.location.href = "/auth"; return; }
    const result = await marketplaceAPI.toggleFavorite(item.publicId);
    setFavoriteIds((current) => { const next = new Set(current); result.saved ? next.add(item.publicId) : next.delete(item.publicId); return next; });
  };

  const visible = useMemo(() => listings.filter((item) => {
    const keyword = search.trim().toLocaleLowerCase("vi");
    return (!category || item.categoryId === category) && (!province || item.province === province) && (!condition || item.condition === condition)
      && (!minPrice || item.price >= Number(minPrice)) && (!maxPrice || item.price <= Number(maxPrice))
      && (!keyword || `${item.title} ${item.description} ${item.province}`.toLocaleLowerCase("vi").includes(keyword));
  }), [listings, category, search, province, condition, minPrice, maxPrice]);
  const provinces = useMemo(() => [...new Set(listings.map((item) => item.province))].sort(), [listings]);

  return (
    <div className="min-h-screen bg-[#fff8f5] pb-24 pt-28 text-[#171329]">
      <section className="relative mx-4 min-h-[540px] overflow-hidden rounded-[38px] bg-[#12082d] text-white shadow-[0_30px_90px_rgba(35,14,75,.28)] sm:mx-6">
        <img src="/images/motionx-marketplace-hero.png" alt="Các phương tiện điện trên cung đường hiện đại" className="absolute inset-0 h-full w-full object-cover object-center" />
        <div className="absolute inset-0 bg-gradient-to-r from-[#100628]/95 via-[#1d0a3b]/70 to-transparent" />
        <div className="relative mx-auto max-w-[1400px] px-7 pb-40 pt-16 sm:px-12 lg:px-16 lg:pt-20">
          <div className="inline-flex items-center gap-2 rounded-full border border-white/15 bg-white/10 px-4 py-2 text-xs font-bold uppercase tracking-[.18em] text-cyan-200 backdrop-blur-xl"><Sparkles size={15} /> Marketplace thế hệ mới</div>
          <h1 className="mt-7 max-w-3xl text-5xl font-extrabold leading-[.96] tracking-[-.055em] sm:text-7xl">Lướt xe chất.<br /><span className="bg-gradient-to-r from-cyan-200 via-fuchsia-300 to-orange-200 bg-clip-text text-transparent">Chốt deal an tâm.</span></h1>
          <p className="mt-6 max-w-xl text-base leading-7 text-white/70 sm:text-lg">Khám phá phương tiện phù hợp, trò chuyện trực tiếp và giao dịch có lớp bảo vệ ngay trên MOTIONX.</p>
          <div className="mt-7 flex flex-wrap gap-3 text-sm font-semibold text-white/80"><span className="flex items-center gap-2 rounded-full bg-white/10 px-4 py-2 backdrop-blur"><ShieldCheck size={16} className="text-emerald-300" />Người bán xác thực</span><span className="flex items-center gap-2 rounded-full bg-white/10 px-4 py-2 backdrop-blur"><Zap size={16} className="text-amber-300" />Trao đổi tức thì</span></div>
        </div>
        <div className="absolute inset-x-5 bottom-6 mx-auto flex max-w-5xl flex-col gap-3 rounded-[24px] border border-white/25 bg-white/95 p-3 text-[#171329] shadow-2xl backdrop-blur-xl sm:inset-x-10 lg:flex-row">
          <div className="flex min-w-0 flex-1 items-center gap-3 px-3"><Search className="text-violet-600" size={22} /><input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Bạn đang tìm xe gì?" className="min-w-0 flex-1 border-0 bg-transparent py-3 text-base outline-none placeholder:text-slate-400" /></div>
          <select value={province} onChange={(event) => setProvince(event.target.value)} className="rounded-2xl border border-violet-100 bg-violet-50 px-5 py-3 font-semibold outline-none"><option value="">Toàn quốc</option>{provinces.map((name) => <option key={name}>{name}</option>)}</select>
          <button className="flex items-center justify-center gap-2 rounded-2xl bg-gradient-to-r from-violet-600 to-fuchsia-500 px-7 py-4 font-bold text-white shadow-lg shadow-violet-500/25">Tìm xe <ArrowRight size={18} /></button>
        </div>
      </section>

      <section className="relative z-10 mx-auto mt-7 max-w-[1360px] px-5 sm:px-8"><div className="grid grid-cols-2 gap-3 rounded-[28px] border border-violet-100 bg-white p-4 shadow-[0_18px_55px_rgba(57,30,99,.10)] sm:grid-cols-3 lg:grid-cols-6">
        <button onClick={() => setCategory(null)} className={`rounded-2xl p-4 text-center transition ${category === null ? "bg-[#171329] text-white shadow-lg" : "hover:bg-violet-50"}`}><span className="text-3xl">✨</span><p className="mt-2 text-sm font-bold">Tất cả xe</p></button>
        {categories.slice(0, 5).map((item) => <button key={item.id} onClick={() => setCategory(item.id)} className={`rounded-2xl p-4 text-center transition ${category === item.id ? "bg-gradient-to-br from-violet-600 to-fuchsia-500 text-white shadow-lg" : "hover:bg-violet-50"}`}><span className="text-3xl">{{ oto: "🚘", "xe-may": "🛵", "xe-dien": "🔋", "xe-thuong-mai": "🚚", "phuong-tien-khac": "🚲" }[item.slug] || "🚗"}</span><p className="mt-2 text-sm font-bold">{item.name}</p></button>)}
      </div></section>

      <section className="hidden">
        <div className="mx-auto max-w-[1400px]">
          <div className="flex items-center gap-2 text-xs font-bold uppercase tracking-[.18em] text-cyan-300"><Sparkles size={15} /> AI Vehicle Marketplace</div>
          <div className="mt-7 grid gap-8 lg:grid-cols-[1fr_.65fr] lg:items-end">
            <h1 className="text-5xl font-extrabold leading-[.98] tracking-[-.055em] sm:text-7xl">Tìm phương tiện.<br /><span className="bg-gradient-to-r from-cyan-300 via-fuchsia-400 to-orange-300 bg-clip-text text-transparent">Chốt giao dịch an tâm.</span></h1>
            <p className="max-w-xl text-base leading-7 text-white/58">Mua bán và trao đổi ô tô, xe máy, xe điện với định giá AI, hồ sơ minh bạch và thanh toán được bảo vệ ngay trên nền tảng.</p>
          </div>
          <div className="mt-12 flex max-w-3xl items-center gap-3 rounded-2xl border border-white/12 bg-white/10 p-3 backdrop-blur-xl">
            <Search className="ml-2 text-white/45" size={20} />
            <input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Tìm theo tên xe, địa điểm, nhu cầu..." className="min-w-0 flex-1 border-0 bg-transparent px-2 py-2 text-white outline-none placeholder:text-white/35" />
            <button className="hidden rounded-xl bg-gradient-to-r from-violet-600 to-fuchsia-500 px-5 py-3 font-bold sm:inline-flex">Tìm kiếm</button>
          </div>
        </div>

        <div className="mb-8 grid gap-3 rounded-2xl border border-violet-100 bg-white p-4 sm:grid-cols-2 lg:grid-cols-5">
          <select value={province} onChange={(event) => setProvince(event.target.value)} className="input-base"><option value="">Toàn quốc</option>{provinces.map((name) => <option key={name}>{name}</option>)}</select>
          <select value={condition} onChange={(event) => setCondition(event.target.value)} className="input-base"><option value="">Mọi tình trạng</option>{Object.entries(conditionLabel).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select>
          <input type="number" min="0" value={minPrice} onChange={(event) => setMinPrice(event.target.value)} placeholder="Giá từ" className="input-base" />
          <input type="number" min="0" value={maxPrice} onChange={(event) => setMaxPrice(event.target.value)} placeholder="Giá đến" className="input-base" />
          <button type="button" onClick={() => { setProvince(""); setCondition(""); setMinPrice(""); setMaxPrice(""); setCategory(null); setSearch(""); }} className="rounded-xl bg-[#171329] px-4 py-3 text-sm font-bold text-white">Xóa bộ lọc</button>
        </div>
      </section>

      <main className="mx-auto max-w-[1480px] px-5 py-14 sm:px-8 lg:px-12">
        <div className="mb-10 flex flex-col gap-5 lg:flex-row lg:items-center lg:justify-between">
          <div><p className="text-xs font-bold uppercase tracking-[.18em] text-violet-600">Marketplace</p><h2 className="mt-2 text-3xl font-extrabold tracking-tight sm:text-4xl">Phương tiện đang được quan tâm</h2></div>
          <div className="flex gap-2 overflow-x-auto pb-2">
            <button onClick={() => setCategory(null)} className={`whitespace-nowrap rounded-full px-5 py-2.5 text-sm font-bold ${category === null ? "bg-[#171329] text-white" : "border border-violet-100 bg-white text-slate-600"}`}>Tất cả</button>
            {categories.map((item) => <button key={item.id} onClick={() => setCategory(item.id)} className={`whitespace-nowrap rounded-full px-5 py-2.5 text-sm font-bold ${category === item.id ? "bg-gradient-to-r from-violet-600 to-fuchsia-500 text-white" : "border border-violet-100 bg-white text-slate-600"}`}>{item.name}</button>)}
          </div>
        </div>

        {loading ? <div className="grid min-h-64 place-items-center text-slate-500">Đang tải marketplace...</div> : (
          <div className="grid gap-5 md:grid-cols-2 xl:grid-cols-3">
            {visible.map((item) => (
              <Link key={item.publicId} to={`/listing/${item.publicId}`} className="group overflow-hidden rounded-[28px] border border-violet-100 bg-white shadow-[0_12px_40px_rgba(52,28,90,.08)] transition hover:-translate-y-1.5 hover:shadow-[0_22px_60px_rgba(105,52,170,.17)]">
                <div className="relative h-64 overflow-hidden bg-violet-50">
                  {item.imageUrl ? (/\.(mp4|webm)$/i.test(item.imageUrl) ? <video src={resolveMarketplaceMediaUrl(item.imageUrl)} muted playsInline className="h-full w-full bg-black object-cover" /> : <img src={resolveMarketplaceMediaUrl(item.imageUrl)} alt={item.title} className="h-full w-full object-cover transition duration-700 group-hover:scale-105" />) : <div className="grid h-full place-items-center"><Car size={48} className="text-violet-200" /></div>}
                  <span className="absolute left-4 top-4 rounded-full bg-white/90 px-3 py-1.5 text-[11px] font-bold text-violet-700 backdrop-blur">{conditionLabel[item.condition]}</span>
                  <button type="button" onClick={(event) => void toggleFavorite(event, item)} aria-label="Yêu thích" className={`absolute right-4 top-4 grid h-10 w-10 place-items-center rounded-full shadow-lg backdrop-blur ${favoriteIds.has(item.publicId) ? "bg-violet-600 text-white" : "bg-white/90 text-violet-700"}`}><Heart size={18} fill={favoriteIds.has(item.publicId) ? "currentColor" : "none"} /></button>
                  {item.exchangeAllowed ? <span className="absolute bottom-4 left-4 rounded-full bg-cyan-300 px-3 py-1.5 text-[11px] font-bold text-slate-900">Có trao đổi</span> : null}
                </div>
                <div className="p-6">
                  <p className="flex items-center gap-1.5 text-xs font-medium text-slate-500"><MapPin size={14} />{item.district ? `${item.district}, ` : ""}{item.province}</p>
                  <h3 className="mt-3 line-clamp-2 text-xl font-extrabold tracking-tight group-hover:text-violet-700">{item.title}</h3>
                  <p className="mt-3 text-2xl font-extrabold text-violet-700">{formatMarketplacePrice(item.price)}</p>
                  {item.manufactureYear || item.mileageKm != null ? <p className="mt-3 text-sm text-slate-500">{item.manufactureYear ? `Đời ${item.manufactureYear}` : ""}{item.manufactureYear && item.mileageKm != null ? " • " : ""}{item.mileageKm != null ? `${item.mileageKm.toLocaleString("vi-VN")} km` : ""}</p> : null}
                  <div className="mt-5 flex items-center justify-between border-t border-violet-50 pt-4 text-sm"><span className="text-slate-500">Đăng bởi <b className="text-slate-700">{item.sellerName}</b></span><span className="grid h-10 w-10 place-items-center rounded-full bg-[#171329] text-white"><ArrowUpRight size={17} /></span></div>
                </div>
              </Link>
            ))}
          </div>
        )}
        {!loading && visible.length === 0 ? <div className="rounded-3xl border border-dashed border-violet-200 bg-white p-16 text-center text-slate-500"><SlidersHorizontal className="mx-auto mb-4 text-violet-400" />Không tìm thấy phương tiện phù hợp.</div> : null}
      </main>
    </div>
  );
};

export default MarketplaceBrowsePage;
