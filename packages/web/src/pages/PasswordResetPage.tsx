import { useEffect, useState, type FormEvent } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "@ebike/shared-code/hooks";
import { accountError, accountVerificationAPI, type Challenge } from "../services/accountVerification";

export default function PasswordResetPage() {
  const { logout } = useAuth();
  const [email, setEmail] = useState("");
  const [challenge, setChallenge] = useState<Challenge | null>(null);
  const [code, setCode] = useState("");
  const [password, setPassword] = useState("");
  const [confirmation, setConfirmation] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [done, setDone] = useState(false);
  const [retryAt, setRetryAt] = useState(0);
  const [now, setNow] = useState(Date.now());
  useEffect(() => { const id=window.setInterval(()=>setNow(Date.now()),1000); return ()=>window.clearInterval(id); }, []);
  const wait = Math.max(0, Math.ceil((retryAt-now)/1000));
  const requestCode = async (event?: FormEvent) => {
    event?.preventDefault(); if (busy || wait) return;
    setBusy(true); setError("");
    try { const next=await accountVerificationAPI.requestReset(email.trim()); setChallenge(next); setCode(""); setRetryAt(Date.now()+next.retryAfterSeconds*1000); }
    catch (e) { setError(accountError(e)); }
    finally { setBusy(false); }
  };
  const reset = async (event: FormEvent) => {
    event.preventDefault(); if (!challenge || busy) return;
    if (password !== confirmation) { setError("Hai mật khẩu chưa khớp."); return; }
    if (new TextEncoder().encode(password).length>72) { setError("Mật khẩu tối đa 72 byte UTF-8."); return; }
    setBusy(true); setError("");
    try { await accountVerificationAPI.reset(challenge.challengeId,code,password); await logout(); setDone(true); setPassword(""); setConfirmation(""); }
    catch (e) { setError(accountError(e)); }
    finally { setBusy(false); }
  };
  return <main className="min-h-screen bg-[#fff8f5] px-5 pb-24 pt-32"><section className="mx-auto max-w-lg rounded-3xl border border-violet-100 bg-white p-7 shadow-sm">
    <h1 className="text-3xl font-extrabold">Đặt lại mật khẩu</h1>
    {done ? <div className="mt-6 space-y-4"><p role="status" className="rounded-xl bg-emerald-50 p-4 text-emerald-800">Mật khẩu đã được cập nhật. Hãy đăng nhập lại trên các thiết bị của bạn.</p><Link to="/auth" className="font-bold text-violet-700 underline">Đăng nhập</Link></div> : <>
      <p className="mt-3 text-sm text-slate-600">Nhận mã xác nhận qua email đã đăng ký. Mã có hiệu lực 10 phút.</p>
      {error && <p role="alert" className="mt-4 rounded-xl bg-amber-50 p-4 text-sm text-amber-900">{error}</p>}
      {!challenge ? <form onSubmit={requestCode} className="mt-6 space-y-4"><label className="block text-sm font-bold">Email đã đăng ký<input type="email" required maxLength={255} value={email} onChange={e=>setEmail(e.target.value)} autoComplete="email" className="input-base mt-2" /></label><button disabled={busy} className="btn-primary w-full disabled:opacity-50">{busy?"Đang gửi...":"Gửi mã xác nhận"}</button></form> : <>
        <p role="status" className="mt-5 rounded-xl bg-violet-50 p-4 text-sm">{challenge.message}</p>
        <form onSubmit={reset} className="mt-5 space-y-4">
          <label className="block text-sm font-bold">Mã xác nhận<input required pattern="[0-9]{6}" inputMode="numeric" autoComplete="one-time-code" maxLength={6} value={code} onChange={e=>setCode(e.target.value.replace(/\D/g,""))} className="input-base mt-2" /></label>
          <label className="block text-sm font-bold">Mật khẩu mới<input type="password" required minLength={8} maxLength={72} autoComplete="new-password" value={password} onChange={e=>setPassword(e.target.value)} className="input-base mt-2" /></label>
          <label className="block text-sm font-bold">Nhập lại mật khẩu<input type="password" required minLength={8} maxLength={72} autoComplete="new-password" value={confirmation} onChange={e=>setConfirmation(e.target.value)} className="input-base mt-2" /></label>
          <button disabled={busy} className="btn-primary w-full disabled:opacity-50">{busy?"Đang xử lý...":"Lưu mật khẩu mới"}</button>
        </form>
        <div className="mt-4 flex flex-wrap gap-4 text-sm"><button disabled={busy||wait>0} onClick={()=>void requestCode()} className="font-bold text-violet-700 disabled:opacity-50">{wait?`Gửi lại sau ${wait}s`:"Gửi lại mã"}</button><button disabled={busy} onClick={()=>{setChallenge(null);setCode("");setError("");}} className="text-slate-600 underline">Đổi email</button></div>
      </>}
      <Link to="/auth" className="mt-6 inline-block text-sm font-bold text-violet-700">Quay lại đăng nhập</Link>
    </>}
  </section></main>;
}
