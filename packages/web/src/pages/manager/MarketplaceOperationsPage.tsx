import { useEffect, useState } from "react";
import { Link, useLocation } from "react-router-dom";
import { useAuth } from "@ebike/shared-code/hooks";
import { apiClient } from "@ebike/shared-code/api";
import { RefreshCw, ShieldCheck } from "lucide-react";
import { formatMarketplacePrice, resolveMarketplaceMediaUrl } from "../../services/marketplace";

type Row = { [key: string]: any };
const labels: Record<string,string> = { PENDING_REVIEW:"Chờ duyệt", PUBLISHED:"Đang hiển thị", REJECTED:"Trả lại / từ chối", SUSPENDED:"Đã đình chỉ", RESERVED:"Đang giao dịch", SOLD:"Đã bán", ARCHIVED:"Đã gỡ", EXPIRED:"Hết hạn", OPEN:"Chờ xử lý", RESOLVED:"Đã xử lý", DISMISSED:"Đã bỏ qua", DEPOSIT_PENDING:"Chờ đặt cọc", DEPOSIT_PAID:"Đã đặt cọc", INSPECTION_PENDING:"Chờ kiểm tra xe", FINAL_PAYMENT_PENDING:"Chờ thanh toán", FULLY_PAID:"Đã thanh toán đủ", COMPLETED:"Hoàn tất", CANCELLED:"Đã hủy", DISPUTED:"Đang tranh chấp", REFUNDED:"Đã hoàn tiền" };
const reasons: Record<string,string> = { MISSING_INFORMATION:"Thiếu thông tin", INVALID_IMAGE:"Ảnh không phù hợp", WRONG_CATEGORY:"Sai danh mục", DUPLICATE_LISTING:"Tin trùng", UNREALISTIC_PRICE:"Giá bất thường", SUSPICIOUS_CONTENT:"Nội dung đáng ngờ", INVALID_DOCUMENT:"Giấy tờ không hợp lệ", OTHER:"Lý do khác" };
const actions: Record<string,string> = { APPROVE:"Duyệt tin", REJECT:"Từ chối", REQUEST_EDIT:"Yêu cầu sửa", HIDE:"Ẩn tin", RESTORE:"Chuyển về chờ duyệt", DISMISS:"Bỏ qua báo cáo", DISPUTE:"Mở tranh chấp", RESUME:"Kết thúc tranh chấp, tiếp tục giao dịch" };
const titles = { overview:"Tổng quan vận hành", listings:"Kiểm duyệt tin đăng", reports:"Báo cáo vi phạm", transactions:"Giao dịch marketplace" };
const failure = (e:any) => e?.response?.data?.message || e?.response?.data?.detail || e?.message || "Không thể thực hiện. Hãy thử lại.";
const stamp = (value:string) => value ? new Date(value).toLocaleString("vi-VN") : "—";
export default function MarketplaceOperationsPage({ view = "overview" }: { view?: keyof typeof titles }) {
  const { user } = useAuth();
  const location = useLocation();
  const base = location.pathname.startsWith("/admin") ? "/admin" : "/manager";
  const [rows,setRows]=useState<Row[]>([]);
  const [overview,setOverview]=useState<Row>({});
  const [loading,setLoading]=useState(true),[error,setError]=useState(""),[success,setSuccess]=useState("");
  const [query,setQuery]=useState(""),[status,setStatus]=useState("");
  const [selected,setSelected]=useState<Row|null>(null);
  const [decision,setDecision]=useState<{ row:Row; action:string }|null>(null);
  const [reason,setReason]=useState("MISSING_INFORMATION"),[note,setNote]=useState(""),[busy,setBusy]=useState(false);
  const load=async()=>{
    setLoading(true);setError("");
    try { const data=(await apiClient.get("/manager/marketplace/"+view)).data; if(view==="overview")setOverview(data);else setRows(data); }
    catch(e){setError(failure(e));}finally{setLoading(false);}
  };
  useEffect(()=>{setRows([]);setStatus("");setQuery("");setSelected(null);setDecision(null);setSuccess("");void load();},[view]);
  const begin=(row:Row,action:string)=>{setDecision({row,action});setNote("");setReason("MISSING_INFORMATION");setError("");};
  const submit=async(event:React.FormEvent)=>{
    event.preventDefault(); if(!decision)return;setBusy(true);setError("");setSuccess("");
    const {row,action}=decision;
    const path=view==="listings" ? "listings/"+row.publicId+"/moderate" : view==="reports" ? "reports/"+row.id+"/resolve" : "transactions/"+row.publicId+(action==="RESUME"?"/resume":"/dispute");
    try { await apiClient.post("/manager/marketplace/"+path,{action,reason,note});setDecision(null);setSelected(null);setSuccess("Đã lưu kết quả xử lý và nhật ký.");await load(); }
    catch(e){setError(failure(e));}finally{setBusy(false);}
  };
  const visible=rows.filter(r=>(!status||r.status===status)&&[r.title,r.listingTitle,r.sellerName,r.sellerUsername,r.transactionNumber,r.buyer,r.seller,r.reporter].join(" ").toLocaleLowerCase("vi").includes(query.toLocaleLowerCase("vi")));
  const own=(row:Row)=>String(row.sellerId)===String(user?.id);
  const button="rounded-xl border border-violet-200 px-3 py-2 text-sm font-semibold text-violet-700 hover:bg-violet-50 disabled:opacity-40";
  return <div className="space-y-5">
    <header className="flex flex-wrap items-center justify-between gap-4"><div><p className="text-xs font-bold uppercase tracking-widest text-violet-600">MOTIONX · Vận hành marketplace</p><h2 className="mt-2 text-2xl font-bold">{titles[view]}</h2><p className="mt-2 text-sm text-slate-500">{view==="transactions"?"Theo dõi thỏa thuận của hai bên, hỗ trợ tranh chấp và xem lịch sử xử lý.":view==="listings"?"Kiểm tra nội dung, ảnh và thông số trước khi công khai tin.":view==="reports"?"Xem phản ánh của người dùng và ghi rõ kết quả xử lý.":"Số liệu thực tế từ tin đăng, báo cáo và giao dịch trên sàn."}</p></div><button onClick={()=>void load()} disabled={loading} className={button}><RefreshCw size={16} className="mr-2 inline"/>Làm mới</button></header>
    {error&&<p role="alert" className="rounded-xl bg-red-50 p-4 text-red-700">{error}</p>}
    {success&&<p role="status" className="rounded-xl bg-emerald-50 p-4 text-emerald-700">{success}</p>}
    {view==="overview"?<><div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">{[["Tin chờ duyệt","pendingListings","listings"],["Tin đang hiển thị","activeListings","listings"],["Báo cáo chưa xử lý","openReports","reports"],["Giao dịch đang xử lý","activeTransactions","transactions"],["Tranh chấp","disputedTransactions","transactions"],["Giao dịch hoàn tất","completedTransactions","transactions"],["Tin mới hôm nay","newListings","listings"]].map(([label,key,path])=><Link key={key} to={base+"/"+path} className="rounded-2xl border border-violet-100 bg-white p-5 shadow-sm"><p className="text-sm text-slate-500">{label}</p><p className="mt-3 text-3xl font-bold text-violet-700">{loading?"—":overview[key]??"—"}</p></Link>)}</div><div className="rounded-2xl border bg-white p-5"><h3 className="font-bold">Quy trình kiểm duyệt</h3><p className="mt-2 text-sm leading-7 text-slate-600">Người bán gửi tin → Chờ duyệt → Duyệt để hiển thị hoặc trả lại kèm lý do. Tin sửa phải được duyệt lại. Tin của nhân sự được người khác kiểm duyệt.</p><Link to={base+"/listings"} className="mt-3 inline-block font-semibold text-violet-700">Mở hàng đợi kiểm duyệt →</Link></div></>:<>
    <div className="flex flex-wrap gap-3"><input aria-label="Tìm kiếm" placeholder="Tìm tên xe, người dùng hoặc mã giao dịch..." value={query} onChange={e=>setQuery(e.target.value)} className="min-w-0 flex-1 rounded-xl border bg-white px-4 py-3"/><select aria-label="Lọc trạng thái" value={status} onChange={e=>setStatus(e.target.value)} className="rounded-xl border bg-white px-4 py-3"><option value="">Tất cả trạng thái</option>{[...new Set(rows.map(r=>r.status))].map(s=><option key={s} value={s}>{labels[s]||s}</option>)}</select></div>
    {loading?<p className="p-8 text-slate-500">Đang tải dữ liệu...</p>:!visible.length?<p className="rounded-2xl border border-dashed p-10 text-center text-slate-500">Không có dữ liệu phù hợp.</p>:<div className="space-y-3">{visible.map(row=><article key={row.publicId||row.id} className="rounded-2xl border border-slate-200 bg-white p-5">
      <div className="flex flex-wrap items-start justify-between gap-3"><div><p className="text-xs text-slate-400">{row.transactionNumber||row.reporter||row.sellerUsername} · {stamp(row.createdAt)}</p><h3 className="mt-1 text-lg font-bold">{row.title||row.listingTitle}</h3>{view==="listings"&&<p className="mt-2 text-sm text-slate-500">{row.categoryName} · {row.manufactureYear||"Chưa có đời xe"} · {row.province} · {formatMarketplacePrice(row.price)}</p>}{view==="transactions"&&<p className="mt-2 text-sm text-slate-600">Người mua: {row.buyer} · Người bán: {row.seller} · Giá thỏa thuận: <b>{formatMarketplacePrice(row.agreedPrice)}</b></p>}</div><span className="rounded-full bg-violet-50 px-3 py-1 text-xs font-bold text-violet-700">{labels[row.status]||row.status}</span></div>
      {view==="reports"&&<p className="mt-3 whitespace-pre-wrap text-sm">{row.reason}</p>}
      {(row.moderationNote||row.resolutionNote)&&<p className="mt-3 rounded-lg bg-slate-50 p-3 text-sm text-slate-600">{row.moderationNote||row.resolutionNote}</p>}
      <div className="mt-4 flex flex-wrap gap-2">
        {view!=="reports"&&<button className={button} onClick={()=>setSelected(selected===row?null:row)}>{selected===row?"Thu gọn":"Xem chi tiết"}</button>}
        {view==="reports"&&<Link className={button} to={base+"/listings"}>Mở danh sách tin</Link>}
        {view==="listings"&&(own(row)?<span className="self-center text-xs text-amber-700">Tin của bạn cần nhân sự khác duyệt</span>:(row.status==="PENDING_REVIEW"?["APPROVE","REQUEST_EDIT","REJECT"]:row.status==="PUBLISHED"?["HIDE"]:row.status==="SUSPENDED"?["RESTORE"]:[]).map(action=><button key={action} className={button} onClick={()=>begin(row,action)}>{actions[action]}</button>))}
        {view==="reports"&&row.status==="OPEN"&&["DISMISS",...(row.listingStatus==="PUBLISHED"?["HIDE"]:[])].map(action=><button key={action} className={button} onClick={()=>begin(row,action)}>{actions[action]}</button>)}
        {view==="transactions"&&row.status==="DISPUTED"&&String(row.buyerId)!==String(user?.id)&&!own(row)&&<button className={button} onClick={()=>begin(row,"RESUME")}>Ghi kết quả & tiếp tục giao dịch</button>}
        {view==="transactions"&&!["COMPLETED","CANCELLED","REFUNDED","DISPUTED"].includes(row.status)&&String(row.buyerId)!==String(user?.id)&&!own(row)&&<button className={button} onClick={()=>begin(row,"DISPUTE")}>Mở tranh chấp</button>}
      </div>
      {selected===row&&<div className="mt-4 border-t pt-4">{view==="listings"?<><p className="whitespace-pre-wrap text-sm leading-6">{row.description}</p><p className="mt-3 text-sm text-slate-500">Số km: {row.mileageKm??"Chưa cung cấp"} · Người đăng: {row.sellerName||row.sellerUsername}</p><div className="mt-3 flex gap-3 overflow-x-auto">{(row.images||[]).map((url:string)=><a key={url} href={resolveMarketplaceMediaUrl(url)} target="_blank" rel="noreferrer" className="shrink-0">{/\.(mp4|webm)$/i.test(url)?<video controls src={resolveMarketplaceMediaUrl(url)} className="h-48 w-64 bg-slate-50 object-contain"/>:<img src={resolveMarketplaceMediaUrl(url)} alt={row.title} className="h-48 w-64 rounded-xl bg-slate-50 object-contain"/>}</a>)}</div></>:<div className="space-y-3">{(JSON.parse(row.history||"[]")||[]).map((event:Row,index:number)=><p key={index} className="rounded-lg bg-slate-50 p-3 text-sm"><b>{labels[event.status]||event.status}</b> · {event.actor||"Hệ thống"} · {stamp(event.createdAt)}<br/>{event.note}</p>)}</div>}</div>}
    </article>)}</div>}</>}
    {decision&&<div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/40 p-4"><form onSubmit={submit} role="dialog" aria-modal="true" aria-labelledby="decision-title" className="max-h-[90vh] w-full max-w-lg space-y-4 overflow-y-auto rounded-2xl bg-white p-6 shadow-xl"><h3 id="decision-title" className="text-xl font-bold"><ShieldCheck className="mr-2 inline text-violet-600"/>{actions[decision.action]}</h3><p className="text-sm text-slate-500">{decision.row.title||decision.row.listingTitle}</p>{decision.action==="APPROVE"?<p className="text-sm">Xác nhận nội dung và hình ảnh phù hợp để công khai tin lên marketplace.</p>:<>{view==="listings"&&<label className="block text-sm font-semibold">Lý do<select value={reason} onChange={e=>setReason(e.target.value)} className="mt-2 w-full rounded-xl border p-3">{Object.entries(reasons).map(([key,label])=><option key={key} value={key}>{label}</option>)}</select></label>}<label className="block text-sm font-semibold">Ghi chú xử lý<textarea required maxLength={2000} value={note} onChange={e=>setNote(e.target.value)} rows={4} className="mt-2 w-full rounded-xl border p-3" placeholder="Nêu rõ nội dung cần sửa hoặc căn cứ xử lý..."/></label></>}{error&&<p role="alert" className="text-sm text-red-600">{error}</p>}<div className="flex justify-end gap-3"><button type="button" disabled={busy} onClick={()=>setDecision(null)} className={button}>Hủy</button><button disabled={busy} className="rounded-xl bg-violet-600 px-5 py-2 font-semibold text-white disabled:opacity-50">{busy?"Đang lưu...":"Xác nhận"}</button></div></form></div>}
  </div>;
}
