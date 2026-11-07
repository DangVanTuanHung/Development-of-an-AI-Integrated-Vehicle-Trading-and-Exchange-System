import {useEffect,useState} from "react";
import {Link,useLocation} from "react-router-dom";
import {tradeAPI,tradeError,tradeLabels,type TradeSummary} from "../../services/trades";
import {formatMarketplacePrice as money} from "../../services/marketplace";

export default function MarketplaceTradesPage(){
  const base=useLocation().pathname.startsWith("/admin")?"/admin":"/manager";
  const [items,setItems]=useState<TradeSummary[]>([]);const [error,setError]=useState("");const [query,setQuery]=useState("");const [filter,setFilter]=useState("");const [loading,setLoading]=useState(true);
  const load=async()=>{try{setError("");setItems(await tradeAPI.all());}catch(e){setError(tradeError(e));}finally{setLoading(false);}};
  useEffect(()=>{void load();},[]);
  const visible=items.filter(x=>(!filter||x.status===filter)&&`${x.transactionNumber} ${x.listingTitle} ${x.buyerName} ${x.sellerName}`.toLowerCase().includes(query.toLowerCase()));
  return <section className="space-y-5"><div className="flex items-center justify-between"><h1 className="text-2xl font-bold">Quản lý giao dịch & tranh chấp</h1><button onClick={()=>void load()} className="rounded-xl border bg-white px-4 py-2 font-bold">Làm mới</button></div><p className="text-sm text-slate-500">Xem lịch sử, bằng chứng; kết luận tiếp tục hoặc hủy và hoàn tiền khi có tranh chấp. Nhân sự không tự xử lý giao dịch mình tham gia.</p>
    {error&&<p role="alert" className="rounded-xl bg-red-50 p-4 text-red-700">{error}</p>}<div className="flex flex-wrap gap-3"><input aria-label="Tìm giao dịch" placeholder="Mã giao dịch, xe hoặc người dùng" value={query} onChange={e=>setQuery(e.target.value)} className="input-base min-w-0 flex-1"/><select aria-label="Lọc trạng thái giao dịch" value={filter} onChange={e=>setFilter(e.target.value)} className="rounded-xl border bg-white px-4 py-2"><option value="">Tất cả trạng thái</option>{[...new Set(items.map(x=>x.status))].map(s=><option key={s} value={s}>{tradeLabels[s]||s}</option>)}</select></div>
    {loading?<p role="status">Đang tải...</p>:<div className="space-y-3">{visible.map(tx=><article key={tx.publicId} className="rounded-2xl border bg-white p-5"><p className="text-xs font-bold text-violet-700">{tx.transactionNumber}</p><h2 className="mt-2 font-bold">{tx.listingTitle}</h2><p className="mt-2 text-sm">Người mua: {tx.buyerName} · Người bán: {tx.sellerName}</p><p className="mt-2 text-sm">{tradeLabels[tx.status]||tx.status} · Thanh toán/bù {money(Number(tx.payableAmount))}</p><Link to={`${base}/transactions/${tx.publicId}`} className="mt-3 inline-block font-bold text-violet-700">Xem giao dịch & xử lý</Link></article>)}{!visible.length&&<p className="p-8 text-slate-500">Không có giao dịch phù hợp.</p>}</div>}
  </section>;
}
