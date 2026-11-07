import { useState } from "react";
import { marketplaceAPI, formatMarketplacePrice } from "../services/marketplace";

type Statistics = Awaited<ReturnType<typeof marketplaceAPI.listingStatistics>>;
export default function ListingStatistics({id}:{id:string}) {
  const [data,setData]=useState<Statistics|null>(null);
  const [open,setOpen]=useState(false);
  const [busy,setBusy]=useState(false);
  const [error,setError]=useState("");
  const toggle=async()=>{
    if(open){setOpen(false);return;}
    setBusy(true);setError("");
    try{setData(await marketplaceAPI.listingStatistics(id));setOpen(true);}catch{setError("Không thể tải thống kê.");}finally{setBusy(false);}
  };
  return <div className="border-t pt-3"><button type="button" disabled={busy} onClick={()=>void toggle()} className="text-sm font-bold text-violet-700">{busy?"Đang tải...":open?"Ẩn thống kê":"Thống kê & lịch sử giá"}</button>{error&&<p role="alert" className="mt-2 text-sm text-red-600">{error}</p>}
    {open&&data&&<div className="mt-3 space-y-3 text-sm"><dl className="grid grid-cols-2 gap-2">{[["Lượt xem",data.viewCount],["Lượt lưu",data.favoriteCount],["Lượt liên hệ",data.contactCount],["Đề nghị mua",data.offerCount]].map(([label,value])=><div key={label} className="rounded-lg bg-violet-50 p-3"><dt className="text-xs text-slate-500">{label}</dt><dd className="mt-1 font-bold">{value}</dd></div>)}</dl><p className="text-xs text-slate-500">Lượt xem và liên hệ được gộp theo người xem mỗi ngày; không tính chủ tin. Liên hệ gồm mở chat hoặc hiện số điện thoại.</p>{data.expiresAt&&<p>Hiển thị đến: <b>{new Date(data.expiresAt).toLocaleDateString("vi-VN")}</b></p>}<h4 className="font-bold">Lịch sử giá</h4><div className="max-h-56 space-y-2 overflow-y-auto">{data.priceHistory.map((row,index)=><p key={index} className="rounded-lg bg-slate-50 p-2">{row.previousPrice!=null?`${formatMarketplacePrice(Number(row.previousPrice))} → `:"Giá ban đầu: "}<b>{formatMarketplacePrice(Number(row.price))}</b><span className="mt-1 block text-xs text-slate-500">{new Date(row.createdAt).toLocaleString("vi-VN")}</span></p>)}</div></div>}
  </div>;
}
