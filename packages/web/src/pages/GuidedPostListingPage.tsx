import { ArrowLeft, ArrowRight, BrainCircuit, Check, CheckCircle2, CircleAlert, FileVideo, ImagePlus, Loader2, Sparkles } from "lucide-react";
import { type ReactNode, useEffect, useMemo, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { marketplaceAPI, type MarketplaceCategory, resolveMarketplaceMediaUrl, type MarketplaceListing, type MarketplaceVisionResult } from "../services/marketplace";

type FormState = { categoryId: string; title: string; description: string; listingType: string; condition: string; price: string; province: string; district: string; manufactureYear: string; registrationYear: string; mileageKm: string; exteriorColor: string; fuelType: string; transmission: string; engineCapacityCc: string; rangeKm: string; seats: string; ownersCount: string; origin: string; negotiable: boolean; exchangeAllowed: boolean };
const initialForm: FormState = { categoryId: "", title: "", description: "", listingType: "SALE", condition: "USED", price: "", province: "", district: "", manufactureYear: "", registrationYear: "", mileageKm: "", exteriorColor: "", fuelType: "", transmission: "", engineCapacityCc: "", rangeKm: "", seats: "", ownersCount: "", origin: "", negotiable: true, exchangeAllowed: false };
const requiredFields: Array<[keyof FormState, string]> = [["categoryId", "Loại phương tiện"], ["title", "Tiêu đề"], ["price", "Giá bán"], ["province", "Tỉnh/Thành phố"], ["description", "Mô tả"]];

const GuidedPostListingPage = ({ management = false }: { management?: boolean }) => {
  const navigate = useNavigate();
  const { id: editId } = useParams();
  const [existing, setExisting] = useState<MarketplaceListing | null>(null);
  const [savedId, setSavedId] = useState<string | null>(null);
  useEffect(() => {
    if (!editId) return;
    marketplaceAPI.ownListings().then(items => {
      const item=items.find(x=>x.publicId===editId);
      if(!item) throw new Error("Bạn không có quyền sửa tin này.");
      setExisting(item);
      const values={...initialForm};
      for(const key of Object.keys(values) as Array<keyof FormState>) {
        const value=item[key as keyof MarketplaceListing];
        (values as any)[key]=typeof initialForm[key]==="boolean" ? Boolean(value) : value==null ? "" : String(value);
      }
      setForm(values); setStep(2);
    }).catch(cause=>setError(cause.message));
  }, [editId]);
  const [step, setStep] = useState(1);
  const [categories, setCategories] = useState<MarketplaceCategory[]>([]);
  const [form, setForm] = useState(initialForm);
  const [files, setFiles] = useState<File[]>([]);
  const [previews, setPreviews] = useState<string[]>([]);
  const [vision, setVision] = useState<MarketplaceVisionResult | null>(null);
  const [analyzing, setAnalyzing] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [confirmed, setConfirmed] = useState(false);
  const [error, setError] = useState("");
  useEffect(() => { void marketplaceAPI.categories().then(setCategories); }, []);

  const missingRequired = useMemo(() => requiredFields.filter(([key]) => !String(form[key] ?? "").trim()).map(([, label]) => label), [form]);
  const update = <K extends keyof FormState>(key: K, value: FormState[K]) => setForm((current) => ({ ...current, [key]: value }));

  const selectFiles = async (selected: File[]) => {
    setError("");
    const accepted = selected.filter((file) => file.type.startsWith("image/") || ["video/mp4", "video/webm"].includes(file.type)).slice(0, 12);
    const total = accepted.reduce((sum, file) => sum + file.size, 0);
    if (!accepted.length) { setError("Vui lòng chọn ảnh JPEG/PNG/WebP hoặc video MP4/WebM."); return; }
    if (total > 20 * 1024 * 1024) { setError("Tổng dung lượng tối đa là 20 MB. Hãy giảm số lượng hoặc dung lượng tệp."); return; }
    previews.forEach(URL.revokeObjectURL);
    setFiles(accepted); setPreviews(accepted.map(URL.createObjectURL)); setVision(null); setAnalyzing(true);
    try {
      const result = await marketplaceAPI.analyzeMedia(accepted); setVision(result);
      const category = categories.find((item) => item.slug === result.categorySlug);
      setForm((current) => ({ ...current, categoryId: category ? String(category.id) : current.categoryId, title: result.title || [result.brand, result.model, result.variant, result.manufactureYear].filter(Boolean).join(" ") || current.title, description: result.description || current.description, manufactureYear: result.manufactureYear ? String(result.manufactureYear) : current.manufactureYear, registrationYear: result.registrationYear ? String(result.registrationYear) : current.registrationYear, mileageKm: result.mileageKm != null ? String(result.mileageKm) : current.mileageKm, exteriorColor: result.exteriorColor || current.exteriorColor, fuelType: result.fuelType || current.fuelType, transmission: result.transmission || current.transmission, engineCapacityCc: result.engineCapacityCc != null ? String(result.engineCapacityCc) : current.engineCapacityCc, rangeKm: result.rangeKm != null ? String(result.rangeKm) : current.rangeKm, seats: result.seats != null ? String(result.seats) : current.seats, origin: result.origin || current.origin, condition: result.condition || current.condition }));
    } catch { setVision({ visionAvailable: false, confidence: 0, detectedFeatures: [], imageFeedback: [], requiredFields: ["Loại xe", "Hãng và dòng xe", "Năm sản xuất", "Số km đã đi", "Giá bán", "Tỉnh/Thành phố"] }); }
    finally { setAnalyzing(false); }
  };

  const submit = async () => {
    if (step !== 3 || !confirmed || submitting) return;
    if (missingRequired.length) { setError(`Vui lòng bổ sung: ${missingRequired.join(", ")}.`); setStep(2); return; }
    if (!files.length && !existing?.images.length) { setError("Tin đăng cần ít nhất một ảnh hoặc video."); setStep(1); return; }
    setSubmitting(true); setError("");
    try {
      const numberOrNull = (value: string) => value ? Number(value) : null;
      const payload = { ...form, categoryId: Number(form.categoryId), brandId: null, price: Number(form.price), addressText: null, manufactureYear: numberOrNull(form.manufactureYear), registrationYear: numberOrNull(form.registrationYear), mileageKm: numberOrNull(form.mileageKm), engineCapacityCc: numberOrNull(form.engineCapacityCc), rangeKm: numberOrNull(form.rangeKm), seats: numberOrNull(form.seats), ownersCount: numberOrNull(form.ownersCount) };
      const created = editId ? await marketplaceAPI.editListing(editId, payload) : savedId ? await marketplaceAPI.editListing(savedId, payload) : await marketplaceAPI.createListing(payload);
      setSavedId(created.publicId);
      for (const file of files) await marketplaceAPI.uploadListingImage(created.publicId, file);
      await marketplaceAPI.publishListing(created.publicId);
      navigate(management ? "/manager/products" : "/my-listings");
    } catch (cause) { setError(cause instanceof Error ? cause.message : "Không thể đăng tin. Vui lòng thử lại."); }
    finally { setSubmitting(false); }
  };

  return <main className={management ? "bg-[#fff9f6] px-4 py-6" : "min-h-screen bg-[#fff9f6] px-4 pb-24 pt-28 sm:px-7"}><div className="mx-auto max-w-6xl">
    <div className="mb-7 flex items-center justify-between"><Link to={management ? "/manager/products" : "/products"} className="flex items-center gap-2 text-sm font-bold text-violet-700"><ArrowLeft size={17} />Quay lại</Link><span className="text-sm text-slate-500">Tin sẽ được kiểm duyệt trước khi hiển thị</span></div>
    <header className="rounded-[32px] bg-gradient-to-r from-[#180a3d] via-violet-800 to-fuchsia-700 p-7 text-white shadow-xl sm:p-10"><div className="flex flex-col gap-6 lg:flex-row lg:items-end lg:justify-between"><div><p className="flex items-center gap-2 text-xs font-bold uppercase tracking-[.18em] text-cyan-200"><Sparkles size={15} />Đăng tin cùng AI</p><h1 className="mt-3 text-4xl font-extrabold tracking-[-.04em]">Đăng xe dễ dàng trong 3 bước</h1><p className="mt-3 max-w-2xl text-white/65">Tải ảnh trước, xác nhận thông tin AI nhận diện, rồi bổ sung những gì chỉ bạn mới biết.</p></div><div className="flex gap-2">{[1,2,3].map((value) => <span key={value} className={`grid h-10 w-10 place-items-center rounded-full font-bold ${step >= value ? "bg-cyan-300 text-violet-950" : "bg-white/10 text-white/50"}`}>{step > value ? <Check size={18} /> : value}</span>)}</div></div></header>

    <form onSubmit={(event) => event.preventDefault()} className="mt-7 rounded-[32px] border border-violet-100 bg-white p-6 shadow-[0_20px_70px_rgba(55,30,92,.10)] sm:p-10">
      {step === 1 ? <section><StepTitle number="01" title="Thêm ảnh hoặc video của xe" description="Ảnh rõ từ nhiều góc giúp AI nhận diện tốt hơn. Không đăng ảnh chứa CCCD, số tài khoản hoặc thông tin nhạy cảm." />
        <label className="mt-7 grid min-h-56 cursor-pointer place-items-center rounded-[28px] border-2 border-dashed border-violet-200 bg-gradient-to-br from-violet-50 to-cyan-50 p-8 text-center transition hover:border-violet-400"><div><span className="mx-auto grid h-16 w-16 place-items-center rounded-2xl bg-white text-violet-600 shadow"><ImagePlus size={29} /></span><p className="mt-4 text-lg font-extrabold">Chọn ảnh hoặc video</p><p className="mt-2 text-sm text-slate-500">Tối đa 12 tệp • Tổng không quá 20 MB</p></div><input type="file" multiple accept="image/jpeg,image/png,image/webp,video/mp4,video/webm" className="hidden" onChange={(event) => void selectFiles(Array.from(event.target.files || []))} /></label>
        {previews.length ? <div className="mt-5 grid grid-cols-3 gap-3 sm:grid-cols-6">{previews.map((url, index) => files[index].type.startsWith("video/") ? <div key={url} className="relative"><video src={url} controls className="aspect-square w-full rounded-xl bg-black object-contain" /><FileVideo className="absolute left-2 top-2 rounded bg-black/60 p-1 text-white" size={23} /></div> : <img key={url} src={url} alt={`Ảnh ${index + 1}`} className="aspect-square w-full rounded-xl bg-slate-50 object-scale-down" />)}</div> : null}
        {analyzing ? <StatusBox icon={<Loader2 className="animate-spin" />} title="AI đang xem ảnh/video..." text="Đang nhận diện loại xe, màu sắc và đặc điểm có thể quan sát." tone="violet" /> : vision ? vision.visionAvailable ? <StatusBox icon={<BrainCircuit />} title={`Đã nhận diện • độ tin cậy ${vision.confidence}%`} text={vision.detectedFeatures?.length ? vision.detectedFeatures.join(" • ") : "AI đã điền trước các trường nhận diện được. Bạn sẽ xác nhận ở bước tiếp theo."} tone="green" /> : <StatusBox icon={<CircleAlert />} title="Bạn vẫn có thể tiếp tục" text="AI thị giác hiện chưa khả dụng. Ảnh/video đã được giữ lại; hãy nhập thông tin xe ở bước tiếp theo." tone="amber" /> : null}
      </section> : null}

      {step === 2 ? <section><StepTitle number="02" title="Xác nhận thông tin tin đăng" description="Các trường có dấu * là bắt buộc. Hãy sửa lại nếu gợi ý AI chưa chính xác." /><div className="mt-7 grid gap-5 sm:grid-cols-2"><Field label="Loại phương tiện *"><select value={form.categoryId} onChange={(e)=>update("categoryId",e.target.value)} className="input-base"><option value="">Chọn loại xe</option>{categories.map((item)=><option key={item.id} value={item.id}>{item.name}</option>)}</select></Field><Field label="Hình thức"><select value={form.listingType} onChange={(e)=>update("listingType",e.target.value)} className="input-base"><option value="SALE">Bán</option><option value="EXCHANGE">Trao đổi</option><option value="SALE_OR_EXCHANGE">Bán hoặc trao đổi</option></select></Field><Field label="Tiêu đề *" wide><input value={form.title} maxLength={220} onChange={(e)=>update("title",e.target.value)} className="input-base" placeholder="Ví dụ: Honda SH 160i ABS 2024" /></Field><Field label="Tình trạng *"><select value={form.condition} onChange={(e)=>update("condition",e.target.value)} className="input-base"><option value="NEW">Mới</option><option value="LIKE_NEW">Như mới</option><option value="USED">Đã sử dụng</option><option value="RESTORED">Đã phục hồi</option><option value="DAMAGED">Cần sửa chữa</option></select></Field><Field label="Giá bán (VND) *"><input type="number" min="1" value={form.price} onChange={(e)=>update("price",e.target.value)} className="input-base" placeholder="Ví dụ: 80000000" /></Field><Field label="Tỉnh/Thành phố *"><input value={form.province} onChange={(e)=>update("province",e.target.value)} className="input-base" placeholder="Ví dụ: TP. Hồ Chí Minh" /></Field><Field label="Quận/Huyện"><input value={form.district} onChange={(e)=>update("district",e.target.value)} className="input-base" /></Field><Field label="Mô tả *" wide><textarea rows={6} maxLength={10000} value={form.description} onChange={(e)=>update("description",e.target.value)} className="input-base resize-none" placeholder="Tình trạng sử dụng, bảo dưỡng, giấy tờ và lý do bán..." /></Field></div>{missingRequired.length ? <p className="mt-5 rounded-xl bg-amber-50 p-4 text-sm text-amber-700">Còn thiếu: {missingRequired.join(", ")}.</p> : <p className="mt-5 flex items-center gap-2 text-sm font-semibold text-emerald-600"><CheckCircle2 size={17} />Thông tin bắt buộc đã đầy đủ.</p>}</section> : null}

      {existing?.images.length ? <div className="mt-5"><p className="text-sm text-slate-500">Ảnh hiện có (ảnh mới sẽ được bổ sung):</p><div className="mt-2 flex gap-3 overflow-x-auto">{existing.images.map(url=><div key={url} className="shrink-0"><a href={resolveMarketplaceMediaUrl(url)} target="_blank" rel="noreferrer" className="shrink-0 text-violet-700">{/\.(mp4|webm)$/i.test(url) ? <span>Xem video</span> : <img src={resolveMarketplaceMediaUrl(url)} alt="Ảnh xe hiện có" className="h-24 w-28 rounded-lg object-contain" />}</a><button type="button" disabled={submitting} onClick={async()=>{setSubmitting(true);try{setExisting(await marketplaceAPI.removeListingImage(existing.publicId,url));}catch(e){setError(e instanceof Error?e.message:"Không thể gỡ ảnh.");}finally{setSubmitting(false);}}} className="mt-1 text-xs font-semibold text-red-600">Gỡ ảnh khỏi tin</button></div>)}</div></div> : null}
      {step === 3 ? <section><StepTitle number="03" title="Thông số và xác nhận đăng tin" description="Điền những mục bạn biết. Không chắc chắn thì để trống, không nên đoán." /><div className="mt-7 grid gap-5 sm:grid-cols-2 lg:grid-cols-3"><NumberField label="Năm sản xuất" value={form.manufactureYear} onChange={(v)=>update("manufactureYear",v)} /><NumberField label="Năm đăng ký" value={form.registrationYear} onChange={(v)=>update("registrationYear",v)} /><NumberField label="Số km đã đi" value={form.mileageKm} onChange={(v)=>update("mileageKm",v)} /><Field label="Màu xe"><input value={form.exteriorColor} onChange={(e)=>update("exteriorColor",e.target.value)} className="input-base" /></Field><Field label="Nhiên liệu"><select value={form.fuelType} onChange={(e)=>update("fuelType",e.target.value)} className="input-base"><option value="">Chưa rõ</option><option value="GASOLINE">Xăng</option><option value="DIESEL">Dầu</option><option value="ELECTRIC">Điện</option><option value="HYBRID">Hybrid</option><option value="OTHER">Khác</option></select></Field><Field label="Hộp số"><select value={form.transmission} onChange={(e)=>update("transmission",e.target.value)} className="input-base"><option value="">Chưa rõ</option><option value="AUTOMATIC">Tự động</option><option value="MANUAL">Số sàn</option><option value="CVT">CVT</option><option value="SINGLE_SPEED">Một cấp</option><option value="OTHER">Khác</option></select></Field><NumberField label="Dung tích động cơ (cc)" value={form.engineCapacityCc} onChange={(v)=>update("engineCapacityCc",v)} /><NumberField label="Tầm hoạt động (km)" value={form.rangeKm} onChange={(v)=>update("rangeKm",v)} /><NumberField label="Số chỗ ngồi" value={form.seats} onChange={(v)=>update("seats",v)} /><NumberField label="Số đời chủ" value={form.ownersCount} onChange={(v)=>update("ownersCount",v)} /><Field label="Xuất xứ" wide><input value={form.origin} onChange={(e)=>update("origin",e.target.value)} className="input-base" placeholder="Lắp ráp trong nước hoặc nhập khẩu" /></Field></div><div className="mt-7 flex flex-wrap gap-5 rounded-2xl bg-violet-50 p-5"><label className="flex items-center gap-2 font-semibold"><input type="checkbox" checked={form.negotiable} onChange={(e)=>update("negotiable",e.target.checked)} />Có thể thương lượng</label><label className="flex items-center gap-2 font-semibold"><input type="checkbox" checked={form.exchangeAllowed} onChange={(e)=>update("exchangeAllowed",e.target.checked)} />Chấp nhận trao đổi</label></div><div className="mt-6 rounded-2xl border border-violet-100 p-5"><p className="font-extrabold">Trước khi đăng</p><ul className="mt-3 space-y-2 text-sm text-slate-600"><li>✓ Tôi đã kiểm tra thông tin và hình ảnh đúng với xe thực tế.</li><li>✓ Tôi hiểu MOTIONX không yêu cầu OTP, CVV hoặc mật khẩu ngân hàng.</li><li>✓ Tôi sẽ cho người mua xem xe và giấy tờ trước khi hoàn tất giao dịch.</li></ul></div></section> : null}

      {step === 3 ? <label className="mt-5 flex cursor-pointer items-start gap-3 rounded-2xl border border-violet-200 bg-violet-50 p-5 text-sm text-slate-700"><input type="checkbox" checked={confirmed} onChange={(event)=>setConfirmed(event.target.checked)} className="mt-1 h-5 w-5 accent-violet-600" /><span><strong className="block text-slate-900">Xác nhận trước khi đăng tin</strong>Tôi đã xem lại thông tin, hình ảnh và đồng ý đăng tin này lên MOTIONX.</span></label> : null}
      {error ? <p className="mt-6 rounded-xl bg-red-50 p-4 text-sm font-semibold text-red-600">{error}</p> : null}
      <footer className="mt-8 flex flex-col-reverse gap-3 border-t border-violet-50 pt-6 sm:flex-row sm:justify-between">{step > 1 ? <button type="button" onClick={()=>{setError("");setConfirmed(false);setStep(step-1);}} className="rounded-2xl border border-violet-100 px-7 py-4 font-bold text-violet-700">Quay lại</button> : <span />}{step < 3 ? <button type="button" disabled={step === 1 && !files.length && !existing?.images.length} onClick={()=>{ if (step===2 && missingRequired.length) { setError(`Vui lòng bổ sung: ${missingRequired.join(", ")}.`); return; } setError("");setStep(step+1);}} className="flex items-center justify-center gap-2 rounded-2xl bg-[#171329] px-8 py-4 font-bold text-white disabled:cursor-not-allowed disabled:opacity-40">Tiếp tục <ArrowRight size={18} /></button> : <button type="button" onClick={()=>void submit()} disabled={submitting || missingRequired.length > 0 || !confirmed || Boolean(editId && !existing)} className="flex items-center justify-center gap-2 rounded-2xl bg-gradient-to-r from-violet-600 to-fuchsia-500 px-8 py-4 font-bold text-white shadow-lg disabled:cursor-not-allowed disabled:opacity-40">{submitting ? <><Loader2 className="animate-spin" size={18} />Đang gửi duyệt...</> : <>Gửi duyệt tin <ArrowRight size={18} /></>}</button>}</footer>
    </form>
  </div></main>;
};

const StepTitle = ({number,title,description}:{number:string;title:string;description:string}) => <div className="flex gap-4"><span className="grid h-11 w-11 shrink-0 place-items-center rounded-2xl bg-violet-100 text-sm font-extrabold text-violet-700">{number}</span><div><h2 className="text-2xl font-extrabold">{title}</h2><p className="mt-1 text-sm leading-6 text-slate-500">{description}</p></div></div>;
const Field = ({label,wide,children}:{label:string;wide?:boolean;children:ReactNode}) => <label className={`space-y-2 text-sm font-bold ${wide ? "sm:col-span-2 lg:col-span-full" : ""}`}>{label}{children}</label>;
const NumberField = ({label,value,onChange}:{label:string;value:string;onChange:(value:string)=>void}) => label === "Số đời chủ" ? null : <Field label={label}><input type="number" min="0" value={value} onChange={(e)=>onChange(e.target.value)} className="input-base" /></Field>;
const StatusBox = ({icon,title,text,tone}:{icon:ReactNode;title:string;text:string;tone:"violet"|"green"|"amber"}) => <div className={`mt-5 flex gap-3 rounded-2xl p-4 ${tone==="green"?"bg-emerald-50 text-emerald-700":tone==="amber"?"bg-amber-50 text-amber-700":"bg-violet-50 text-violet-700"}`}><span className="shrink-0">{icon}</span><div><p className="font-extrabold">{title}</p><p className="mt-1 text-sm leading-6 opacity-80">{text}</p></div></div>;

export default GuidedPostListingPage;
