import {useEffect,useState} from "react";
import {threadAPI,errorMessage,type TicketMessage} from "../services/community";
import {actionButton} from "./CommunityActions";
export default function TicketThread({id,closed=false}:{id:number;closed?:boolean}){
 const [items,setItems]=useState<TicketMessage[]>([]),[text,setText]=useState(""),[error,setError]=useState(""),[busy,setBusy]=useState(false);
 const load=async()=>{try{setItems(await threadAPI.list(id));setError("");}catch(e){setError(errorMessage(e));}};
 useEffect(()=>{setItems([]);setText("");void load();const timer=setInterval(()=>void load(),10000);return()=>clearInterval(timer);},[id]);
 return <section className="mt-4 rounded-xl border bg-slate-50 p-4"><h3 className="font-bold">Lịch sử trao đổi hỗ trợ</h3>{error&&<p role="alert" className="mt-2 text-red-700">{error}</p>}<button className={actionButton+" mt-2"} onClick={()=>void load()}>Tải lại trao đổi</button><div className="my-3 space-y-3">{items.map(m=><article key={m.id} className="rounded-lg bg-white p-3 text-sm"><b>{m.staff?"Hỗ trợ · ":""}{m.username}</b><p className="mt-1 whitespace-pre-wrap">{m.content}</p><small>{new Date(m.createdAt).toLocaleString("vi-VN")}</small></article>)}{!items.length&&<p className="text-sm">Chưa có phản hồi bổ sung.</p>}</div>{!closed&&<form onSubmit={async e=>{e.preventDefault();setBusy(true);try{await threadAPI.reply(id,text);setText("");await load();}catch(err){setError(errorMessage(err));}finally{setBusy(false);}}}><textarea aria-label="Nội dung phản hồi hỗ trợ" required maxLength={4000} value={text} onChange={e=>setText(e.target.value)} className="input-base"/><button disabled={busy} className={actionButton}>Gửi phản hồi hỗ trợ</button></form>}</section>;
}
