import { useEffect, useState } from "react";
import { Link, useLocation } from "react-router-dom";
import { Car, Plus } from "lucide-react";
import { marketplaceAPI, formatMarketplacePrice, resolveMarketplaceMediaUrl, type MarketplaceListing } from "../../services/marketplace";

const statusLabels: Record<string, string> = { DRAFT: "Bản nháp", PENDING_REVIEW: "Chờ kiểm duyệt", SUSPENDED: "Đình chỉ", RESERVED: "Đang giao dịch", PUBLISHED: "Đang đăng", SOLD: "Đã bán", HIDDEN: "Đã ẩn", REJECTED: "Bị từ chối", ARCHIVED: "Đã lưu trữ" };

export default function ManagerProductsPage() {
  const management=useLocation().pathname.startsWith("/manager");
  const [listings, setListings] = useState<MarketplaceListing[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [publishing, setPublishing] = useState<string | null>(null);
  const load = async () => {
    setLoading(true); setError("");
    try { setListings(await marketplaceAPI.ownListings()); }
    catch (cause) { setError(cause instanceof Error ? cause.message : "Không thể tải tin đăng."); }
    finally { setLoading(false); }
  };
  useEffect(() => { void load(); }, []);
  const publish = async (id: string) => {
    setPublishing(id); setError("");
    try {
      const updated = await marketplaceAPI.publishListing(id);
      setListings(current => current.map(item => item.publicId === id ? updated : item));
    } catch (cause) { setError(cause instanceof Error ? cause.message : "Không thể đăng tin."); }
    finally { setPublishing(null); }
  };
  return <div className="space-y-6">
    <div className="flex flex-wrap items-center justify-between gap-4">
      <div><h2 className="text-2xl font-bold">Tin bán xe của tôi</h2><p className="mt-2 text-sm text-slate-500">Đăng xe với ảnh, giá và thông tin của bạn. Tin chỉ hiển thị sau khi được kiểm duyệt.</p></div>
      <Link to={management ? "/manager/products/new" : "/sell"} className="inline-flex items-center gap-2 rounded-xl bg-violet-600 px-5 py-3 font-bold text-white"><Plus size={18} />Đăng tin bán xe</Link>
    </div>
    {error && <div role="alert" className="rounded-xl bg-red-50 p-4 text-red-700">{error}<button onClick={() => void load()} className="ml-4 underline">Tải lại</button></div>}
    {loading ? <p className="p-8 text-slate-500">Đang tải tin đăng...</p> : !listings.length && !error ? <div className="rounded-2xl border border-dashed bg-white p-12 text-center text-slate-500">Bạn chưa có tin bán xe. Chọn “Đăng tin bán xe” để tạo tin đầu tiên.</div> : <div className="grid gap-5 md:grid-cols-2 xl:grid-cols-3">
      {listings.map(item => <article key={item.publicId} className="overflow-hidden rounded-2xl border border-violet-100 bg-white">
        <Link to={`/listing/${item.publicId}`} className="block h-56 bg-violet-50">
          {item.imageUrl ? /\.(mp4|webm)$/i.test(item.imageUrl) ? <video src={resolveMarketplaceMediaUrl(item.imageUrl)} muted playsInline className="h-full w-full object-cover" /> : <img src={resolveMarketplaceMediaUrl(item.imageUrl)} alt={item.title} className="h-full w-full object-cover" /> : <div className="grid h-full place-items-center"><Car className="text-violet-300" size={48} /></div>}
        </Link>
        <div className="space-y-3 p-5">
          <span className="rounded-full bg-violet-50 px-3 py-1 text-xs font-bold text-violet-700">{statusLabels[item.status] || item.status}</span>
          {item.moderationNote && <p className="rounded-lg bg-amber-50 p-3 text-sm text-amber-800">{item.moderationNote}</p>}
          <h3 className="text-lg font-bold"><Link to={`/listing/${item.publicId}`}>{item.title}</Link></h3>
          <p className="text-xl font-bold text-violet-700">{formatMarketplacePrice(item.price)}</p>
          <p className="text-sm text-slate-500">{[item.district, item.province].filter(Boolean).join(", ")}</p>
          <p className="text-sm text-slate-500">Đăng bởi {item.sellerName}</p>
          <div className="flex flex-wrap gap-3 border-t pt-3"><Link to={`/listing/${item.publicId}`} className="font-semibold text-violet-700">Xem tin</Link>
            {["DRAFT","REJECTED"].includes(item.status) && <button disabled={publishing !== null} onClick={() => void publish(item.publicId)} className="font-semibold text-violet-700 disabled:opacity-50">{publishing === item.publicId ? "Đang gửi..." : "Gửi duyệt"}</button>}
            {["DRAFT","REJECTED","PUBLISHED","PENDING_REVIEW"].includes(item.status) && <Link to={management ? `/manager/products/${item.publicId}/edit` : `/sell/${item.publicId}/edit`} className="font-semibold text-violet-700">Sửa tin</Link>}
            {["DRAFT","REJECTED","PUBLISHED","PENDING_REVIEW"].includes(item.status) && <button disabled={publishing!==null} onClick={async()=>{setPublishing(item.publicId);try {await marketplaceAPI.withdrawListing(item.publicId);await load();}catch(e){setError(e instanceof Error?e.message:"Không thể gỡ tin");}finally{setPublishing(null);}}} className="font-semibold text-slate-500">Gỡ tin</button>}
          </div>
        </div>
      </article>)}
    </div>}
  </div>;
}
