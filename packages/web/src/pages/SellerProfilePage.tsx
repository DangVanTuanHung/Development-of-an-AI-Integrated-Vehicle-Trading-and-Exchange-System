import {ReviewActions,ReportButton} from "../components/CommunityActions";
import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { marketplaceAPI, formatMarketplacePrice, resolveMarketplaceMediaUrl, type MarketplaceListing, type MarketplaceSellerReputation } from "../services/marketplace";

export default function SellerProfilePage() {
  const { id }=useParams();
  const [profile,setProfile]=useState<MarketplaceSellerReputation|null>(null);
  const [listings,setListings]=useState<MarketplaceListing[]>([]);
  const [error,setError]=useState("");
  const [loading,setLoading]=useState(true);
  useEffect(()=>{
    let active=true;setLoading(true);setError("");setProfile(null);setListings([]);
    const sellerId=Number(id);
    if(!Number.isSafeInteger(sellerId)||sellerId<=0){setError("Người bán không hợp lệ.");setLoading(false);return;}
    Promise.all([marketplaceAPI.sellerReputation(sellerId),marketplaceAPI.sellerListings(sellerId)])
      .then(([info,items])=>{if(active){setProfile(info);setListings(items);}})
      .catch(()=>{if(active)setError("Không tìm thấy hồ sơ hoặc tài khoản không còn hoạt động.");})
      .finally(()=>{if(active)setLoading(false);});
    return()=>{active=false;};
  },[id]);
  const seller=profile?.seller;
  const name=seller?[seller.firstName,seller.lastName].filter(Boolean).join(" ")||seller.username:"";
  return <main className="min-h-screen bg-[#fff8f5] px-5 pb-24 pt-32"><div className="mx-auto max-w-6xl">
    <Link to="/products" className="font-bold text-violet-700">← Khám phá xe</Link>
    {loading?<p role="status" className="mt-8">Đang tải hồ sơ...</p>:error?<p role="alert" className="mt-8 rounded-xl bg-amber-50 p-5">{error}</p>:profile&&seller?<>
      <section className="mt-6 rounded-3xl border border-violet-100 bg-white p-7"><div className="flex flex-wrap items-center gap-5">
        {seller.avatarUrl?<img src={resolveMarketplaceMediaUrl(seller.avatarUrl)} alt={name} className="h-20 w-20 rounded-full object-cover"/>:<span className="grid h-20 w-20 place-items-center rounded-full bg-violet-100 text-3xl font-bold text-violet-700">{name.charAt(0)}</span>}
        <div><h1 className="text-3xl font-extrabold">{name}</h1><p className="mt-2 text-sm text-slate-500">Tham gia từ {new Date(seller.createdAt).toLocaleDateString("vi-VN")}</p><p className="mt-2 text-sm">Email: {seller.emailVerifiedAt?"đã xác minh":"chưa xác minh"} · Điện thoại: {seller.phoneVerifiedAt?"đã xác minh":"chưa xác minh"}</p></div>
      </div><div className="mt-6 grid gap-4 sm:grid-cols-3">{[["Tin đang đăng",profile.summary.activeListingCount],["Xe đã giao dịch",profile.summary.completedVehicleCount],[`Điểm đánh giá (${profile.summary.reviewCount} lượt)`,profile.summary.reviewCount?`${Number(profile.summary.averageRating).toFixed(2)} / 5`:"Chưa có đánh giá"]].map(([label,value])=><div key={label} className="rounded-2xl bg-violet-50 p-5"><p className="text-sm text-slate-600">{label}</p><p className="mt-2 text-xl font-bold text-violet-700">{value}</p></div>)}</div></section>
      <div className="mt-4"><ReportButton type="USER" target={String(seller.id)}/></div><section className="mt-8"><h2 className="text-2xl font-bold">Tin đang đăng</h2><div className="mt-5 grid gap-5 sm:grid-cols-2 lg:grid-cols-3">{listings.length?listings.map(item=><Link key={item.publicId} to={`/listing/${item.publicId}`} className="overflow-hidden rounded-2xl border bg-white">{item.imageUrl&&(/\.(mp4|webm)(?:\?|$)/i.test(item.imageUrl)?<video src={resolveMarketplaceMediaUrl(item.imageUrl)} muted playsInline className="aspect-video w-full object-cover"/>:<img src={resolveMarketplaceMediaUrl(item.imageUrl)} alt={item.title} className="aspect-video w-full object-cover"/>)}<div className="p-5"><h3 className="font-bold">{item.title}</h3><p className="mt-2 font-bold text-violet-700">{formatMarketplacePrice(item.price)}</p><p className="mt-2 text-sm text-slate-500">{item.province}</p></div></Link>):<p className="text-slate-500">Người bán chưa có tin công khai.</p>}</div></section>
      <section className="mt-8 rounded-3xl border bg-white p-7"><h2 className="text-2xl font-bold">Đánh giá từ các giao dịch</h2><p className="mt-2 text-sm text-slate-500">Các đánh giá đến từ giao dịch đã hoàn tất. Hiển thị tối đa 50 đánh giá gần nhất.</p><div className="mt-5 space-y-4">{profile.reviews.length?profile.reviews.map(review=><article key={review.publicId} className="rounded-xl bg-slate-50 p-4"><p className="font-bold">{review.reviewerName} · {review.rating}/5</p><p className="mt-1 text-xs text-slate-500">{new Date(review.createdAt).toLocaleDateString("vi-VN")}</p><p className="mt-3 whitespace-pre-wrap text-sm">{review.comment||"Không có nhận xét bằng lời."}</p><ReviewActions review={review}/></article>):<p className="text-slate-500">Chưa có đánh giá.</p>}</div></section>
    </>:null}
  </div></main>;
}
