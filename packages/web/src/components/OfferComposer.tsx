import { useEffect,useState,type FormEvent } from "react";
import { marketplaceAPI,type MarketplaceListing,formatMarketplacePrice as money } from "../services/marketplace";
import { tradeAPI,tradeError,refundPolicy,type Offer } from "../services/trades";

export default function OfferComposer({listingId,price,exchangeAllowed=false,exchangeOnly=false,counter,onSuccess,onCancel}:{listingId:string;price:number;exchangeAllowed?:boolean;exchangeOnly?:boolean;counter?:Offer;onSuccess:()=>void;onCancel?:()=>void}){
  const [amount,setAmount]=useState(String(counter?.amount??price));
  const [deposit,setDeposit]=useState(String(counter?.depositAmount??Math.round(price*.1)));
  const [mode,setMode]=useState(counter?.exchangeListingPublicId||exchangeOnly?"EXCHANGE":"SALE");
  const [vehicle,setVehicle]=useState(counter?.exchangeListingPublicId||"");
  const [credit,setCredit]=useState(String(counter?.exchangeValue??0));
  const [message,setMessage]=useState("");const [accepted,setAccepted]=useState(false);
  const [cars,setCars]=useState<MarketplaceListing[]>([]);const [error,setError]=useState("");const [busy,setBusy]=useState(false);
  useEffect(()=>{let active=true;if(exchangeAllowed&&!counter)marketplaceAPI.ownListings().then(items=>{if(active)setCars(items.filter(x=>x.status==="PUBLISHED"&&x.publicId!==listingId));}).catch(()=>{if(active)setError("Không tải được xe trao đổi của bạn.");});return()=>{active=false;};},[exchangeAllowed,counter?.publicId,listingId]);
  const value=mode==="EXCHANGE"?Number(credit):0;const difference=Number(amount)-value;
  const submit=async(e:FormEvent)=>{e.preventDefault();if(busy)return;setError("");setBusy(true);try{
    const body={amount:Number(amount),depositAmount:Number(deposit),exchangeValue:value,exchangeListingId:mode==="EXCHANGE"?vehicle:undefined,message,policyAccepted:accepted};
    if(counter)await tradeAPI.counter(counter.publicId,body);else await tradeAPI.createOffer(listingId,body);onSuccess();
  }catch(e){setError(tradeError(e));}finally{setBusy(false);}};
  return <form onSubmit={submit} className="mt-4 space-y-4 rounded-2xl border border-violet-100 bg-violet-50/60 p-5">
    <h3 className="font-bold">{counter?"Đề nghị thương lượng mới":"Gửi đề nghị mua / trao đổi"}</h3>
    {!counter&&exchangeAllowed&&<label className="block text-sm font-semibold">Hình thức<select aria-label="Hình thức đề nghị" value={mode} onChange={e=>{setMode(e.target.value);setVehicle("");setCredit("0");}} className="input-base mt-2">{!exchangeOnly&&<option value="SALE">Mua xe</option>}<option value="EXCHANGE">Trao đổi bằng xe của tôi</option></select></label>}
    <label className="block text-sm font-semibold">Giá trị thỏa thuận xe đang xem (VND)<input aria-label="Giá đề nghị" type="number" min="1" step="1" required value={amount} onChange={e=>setAmount(e.target.value)} className="input-base mt-2"/></label>
    {mode==="EXCHANGE"&&<>{counter?<p className="text-sm">Xe trao đổi: <b>{counter.exchangeListingTitle}</b></p>:<label className="block text-sm font-semibold">Xe trao đổi<select aria-label="Xe trao đổi" required value={vehicle} onChange={e=>{setVehicle(e.target.value);const car=cars.find(x=>x.publicId===e.target.value);if(car){setCredit(String(car.price));setDeposit("0");}}} className="input-base mt-2"><option value="">Chọn tin đang đăng của bạn</option>{cars.map(x=><option key={x.publicId} value={x.publicId}>{x.title}</option>)}</select></label>}<label className="block text-sm font-semibold">Giá trị thỏa thuận xe trao đổi (VND)<input aria-label="Giá trị xe trao đổi" type="number" min="1" required value={credit} onChange={e=>setCredit(e.target.value)} className="input-base mt-2"/></label></>}
    <p className="rounded-xl bg-white p-3 text-sm">{mode==="SALE"?"Người mua thanh toán":difference>0?"Người mua bù thêm":difference<0?"Người bán bù thêm":"Trao đổi ngang giá"}{difference!==0?` ${money(Math.abs(difference))}`:""}</p>
    <label className="block text-sm font-semibold">Tiền cọc (VND)<input aria-label="Tiền cọc đề nghị" type="number" min="0" max={Math.abs(difference)} required value={deposit} onChange={e=>setDeposit(e.target.value)} className="input-base mt-2"/><span className="mt-1 block text-xs font-normal text-slate-500">Có thể chọn 0. Cọc tối đa bằng số tiền cần thanh toán/bù.</span></label>
    <label className="block text-sm">Lời nhắn<textarea maxLength={2000} value={message} onChange={e=>setMessage(e.target.value)} className="input-base mt-2"/></label>
    <p className="text-xs leading-6 text-slate-600">{refundPolicy}</p><label className="flex items-start gap-2 text-sm"><input type="checkbox" required checked={accepted} onChange={e=>setAccepted(e.target.checked)} className="mt-1"/>Tôi đồng ý điều kiện hủy và hoàn tiền.</label>
    {error&&<p role="alert" className="rounded-xl bg-red-50 p-3 text-sm text-red-700">{error}</p>}
    <div className="flex flex-wrap gap-3"><button disabled={busy||!accepted} className="rounded-xl bg-violet-700 px-5 py-3 text-sm font-bold text-white disabled:opacity-50">{busy?"Đang gửi...":counter?"Gửi mức thương lượng":"Gửi đề nghị"}</button>{onCancel&&<button type="button" onClick={onCancel} className="px-4 text-sm font-bold text-slate-500">Đóng</button>}</div>
  </form>;
}
