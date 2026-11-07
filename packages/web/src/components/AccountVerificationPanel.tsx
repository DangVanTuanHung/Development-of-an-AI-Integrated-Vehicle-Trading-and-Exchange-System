import { useEffect, useState, type FormEvent } from "react";
import { authAPI } from "@ebike/shared-code/api";
import type { UserProfileResponse } from "@ebike/shared-code/types";
import { accountError, accountVerificationAPI, type Challenge, type VerificationChannel } from "../services/accountVerification";

export default function AccountVerificationPanel({profile,onVerified}:{profile:UserProfileResponse;onVerified:(profile:UserProfileResponse)=>void}) {
  const [availability,setAvailability]=useState({emailAvailable:false,phoneAvailable:false});
  const [challenges,setChallenges]=useState<Partial<Record<VerificationChannel,Challenge>>>({});
  const [codes,setCodes]=useState({email:"",phone:""});
  const [retryAt,setRetryAt]=useState({email:0,phone:0});
  const [now,setNow]=useState(Date.now());
  const [busy,setBusy]=useState<VerificationChannel|null>(null);
  const [message,setMessage]=useState("");
  const [error,setError]=useState("");
  useEffect(()=>{let active=true;accountVerificationAPI.availability().then(x=>{if(active)setAvailability(x);}).catch(()=>{if(active)setError("Chưa tải được trạng thái dịch vụ xác minh. Bạn vẫn có thể tiếp tục sử dụng tài khoản.");});return()=>{active=false;};},[]);
  useEffect(()=>{setChallenges({});setCodes({email:"",phone:""});},[profile.email,profile.phoneNumber]);
  useEffect(()=>{const id=window.setInterval(()=>setNow(Date.now()),1000);return()=>window.clearInterval(id);},[]);
  const request=async(channel:VerificationChannel)=>{
    setBusy(channel);setError("");setMessage("");
    try {const challenge=await accountVerificationAPI.request(channel);setChallenges(x=>({...x,[channel]:challenge}));setRetryAt(x=>({...x,[channel]:Date.now()+challenge.retryAfterSeconds*1000}));setCodes(x=>({...x,[channel]:""}));setMessage(challenge.message);}
    catch(e){setError(accountError(e));}finally{setBusy(null);}
  };
  const confirm=async(event:FormEvent,channel:VerificationChannel)=>{
    event.preventDefault();const challenge=challenges[channel];if(!challenge)return;
    setBusy(channel);setError("");setMessage("");
    try {await accountVerificationAPI.confirm(channel,challenge.challengeId,codes[channel]);onVerified(await authAPI.getProfile());setChallenges(x=>({...x,[channel]:undefined}));setMessage("Đã xác minh thành công.");}
    catch(e){setError(accountError(e));}finally{setBusy(null);}
  };
  return <section className="rounded-lg border border-violet-100 bg-white p-6 shadow-sm lg:col-span-8"><h3 className="text-xl font-bold">Xác minh thông tin liên hệ</h3><p className="mt-2 text-sm text-slate-500">Lưu email hoặc số điện thoại trong hồ sơ trước khi xác minh. Bạn có thể bổ sung xác minh sau.</p>
    {error&&<p role="alert" className="mt-4 rounded-xl bg-amber-50 p-3 text-sm text-amber-800">{error}</p>}{message&&<p role="status" className="mt-4 rounded-xl bg-emerald-50 p-3 text-sm text-emerald-800">{message}</p>}
    <div className="mt-5 grid gap-5 sm:grid-cols-2">{(["email","phone"] as const).map(channel=>{
      const verified=Boolean(channel==="email"?profile.emailVerifiedAt:profile.phoneVerifiedAt);
      const available=channel==="email"?availability.emailAvailable:availability.phoneAvailable;
      const target=channel==="email"?profile.email:profile.phoneNumber;
      const wait=Math.max(0,Math.ceil((retryAt[channel]-now)/1000));
      return <div key={channel} className="rounded-2xl border p-4"><h4 className="font-bold">{channel==="email"?"Email":"Số điện thoại"}</h4><p className="mt-2 break-all text-sm">{target||"Chưa cập nhật"}</p><p className={`mt-2 text-sm ${verified?"text-emerald-700":"text-slate-500"}`}>{verified?"Đã xác minh":"Chưa xác minh"}</p>
        {!verified&&<><button type="button" disabled={busy!==null||!available||!target||wait>0} onClick={()=>void request(channel)} className="mt-3 rounded-xl border border-violet-200 px-4 py-2 text-sm font-bold text-violet-700 disabled:opacity-40">{wait?`Gửi lại sau ${wait}s`:"Gửi mã xác minh"}</button>{!available&&<p className="mt-2 text-xs text-slate-500">Dịch vụ {channel==="email"?"email":"SMS"} chưa sẵn sàng. Bạn có thể bỏ qua bước này.</p>}
          {challenges[channel]&&<form onSubmit={e=>void confirm(e,channel)} className="mt-3 space-y-3"><label className="block text-sm">Mã 6 chữ số<input aria-label={`Mã xác minh ${channel==="email"?"email":"điện thoại"}`} autoComplete="one-time-code" inputMode="numeric" pattern="[0-9]{6}" required maxLength={6} value={codes[channel]} onChange={e=>setCodes(x=>({...x,[channel]:e.target.value.replace(/\D/g,"")}))} className="input-base mt-1" /></label><button disabled={busy!==null} className="rounded-xl bg-violet-600 px-4 py-2 text-sm font-bold text-white disabled:opacity-40">Xác nhận</button></form>}
        </>}
      </div>;
    })}</div>
  </section>;
}
