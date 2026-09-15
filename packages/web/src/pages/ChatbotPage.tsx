import { chatbotAPI } from "@ebike/shared-code/api";
import type { ChatbotRecommendation } from "@ebike/shared-code/types";
import { Bot, Check, ClipboardList, Eye, Loader2, Mic, MicOff, RotateCcw, Send, ShieldCheck, ShoppingCart, UserRound, X } from "lucide-react";
import type { FormEvent, KeyboardEvent } from "react";
import { useEffect, useRef, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { resolveMarketplaceMediaUrl } from "../services/marketplace";
import { attachImageFallback } from "../utils/media";

type ChatMessage = {
  id: string;
  role: "assistant" | "user";
  text: string;
  recommendations?: ChatbotRecommendation[];
  suggestions?: string[];
  createdAt: string;
};

type StoredSession = { chatId: string; messages: ChatMessage[] };

const STORAGE_KEY = "motionx-ai-advisor-v3";
const MAX_MESSAGE_LENGTH = 1200;
const starterQuestions = [
  "Tư vấn xe đi làm 30 km mỗi ngày, ngân sách dưới 30 triệu",
  "So sánh các mẫu xe có quãng đường đi xa nhất",
  "Xe nào phù hợp cho sinh viên và dễ sạc?",
  "Chính sách bảo hành pin và đổi trả thế nào?"
];

type SurveyAnswers = Record<string, string>;
const surveySteps = [
  {
    key: "usage",
    question: "Bạn chủ yếu dùng xe cho nhu cầu nào?",
    options: ["Đi làm văn phòng", "Đi học", "Giao hàng/chạy dịch vụ", "Đi chơi và di chuyển hằng ngày"]
  },
  {
    key: "distance",
    question: "Tổng quãng đường bạn đi mỗi ngày khoảng bao nhiêu?",
    options: ["Dưới 20 km", "Khoảng 20–40 km", "Khoảng 40–60 km", "Trên 60 km"]
  },
  {
    key: "budget",
    question: "Ngân sách dự kiến của bạn?",
    options: ["Dưới 20 triệu", "Khoảng 20–25 triệu", "Khoảng 25–30 triệu", "Trên 30 triệu"]
  },
  {
    key: "priority",
    question: "Bạn ưu tiên điều gì nhất?",
    options: ["Gọn nhẹ, dễ điều khiển", "Đi xa, ít phải sạc", "Mạnh và tốc độ tốt", "Cốp rộng, ngồi thoải mái"]
  }
] as const;

const welcomeMessage = (): ChatMessage => ({
  id: crypto.randomUUID(),
  role: "assistant",
  text: "Xin chào! Mình là trợ lý MOTIONX AI. Mình có thể tư vấn chọn xe, so sánh mẫu xe, ước tính nhu cầu sử dụng và giải thích chính sách. Bạn hãy cho mình biết ngân sách, quãng đường đi mỗi ngày và nhu cầu chính nhé.",
  createdAt: new Date().toISOString()
});

const newSession = (): StoredSession => ({ chatId: crypto.randomUUID(), messages: [welcomeMessage()] });

const loadSession = (): StoredSession => {
  try {
    const parsed = JSON.parse(localStorage.getItem(STORAGE_KEY) || "null") as StoredSession | null;
    return parsed?.chatId && parsed.messages?.length ? { ...parsed, messages: parsed.messages.slice(-50) } : newSession();
  } catch {
    return newSession();
  }
};

const formatPrice = (price: number) =>
  new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND", maximumFractionDigits: 0 }).format(price);
const cleanChatText = (text: string) => text.replace(/\*\*/g, "").replace(/^#{1,6}\s*/gm, "").trim();

const followUpSuggestions = (answer: string, recommendations: ChatbotRecommendation[] = []) => {
  const normalized = answer.toLocaleLowerCase("vi-VN");
  if (recommendations.length > 1) return ["Có mẫu nào rẻ hơn không?", "So sánh các xe này", "Cho tôi xem hình ảnh", "Có hỗ trợ trả góp không?"];
  if (recommendations.length === 1) return ["Cho tôi xem hình ảnh", "Có mẫu nào rẻ hơn không?", "Có hỗ trợ trả góp không?"];
  if (normalized.includes("bảo hành") || normalized.includes("đổi trả")) return ["Điều kiện bảo hành pin?", "Trường hợp nào không được bảo hành?", "Cần giấy tờ gì khi bảo hành?"];
  if (normalized.includes("sạc") || normalized.includes("pin")) return ["Sạc đầy mất bao lâu?", "Pin dùng được bao nhiêu năm?", "Có thể sạc tại nhà không?"];
  if (normalized.includes("ngân sách") || normalized.includes("triệu")) return ["Có mẫu nào rẻ hơn không?", "So sánh giá và quãng đường", "Có hỗ trợ trả góp không?"];
  return ["Tư vấn theo ngân sách của tôi", "So sánh 2 mẫu xe", "Chính sách bảo hành thế nào?"];
};

type BrowserSpeechRecognition = {
  lang: string;
  interimResults: boolean;
  maxAlternatives: number;
  start: () => void;
  stop: () => void;
  onresult: ((event: { results: ArrayLike<{ 0: { transcript: string } }> }) => void) | null;
  onerror: (() => void) | null;
  onend: (() => void) | null;
};
type BrowserSpeechRecognitionConstructor = new () => BrowserSpeechRecognition;

type ChatbotPageProps = { embedded?: boolean };

const ChatbotPage = ({ embedded = false }: ChatbotPageProps) => {
  const navigate = useNavigate();
  const location = useLocation();
  const initialRef = useRef<StoredSession | null>(null);
  if (!initialRef.current) initialRef.current = loadSession();
  const [chatId, setChatId] = useState(initialRef.current.chatId);
  const [messages, setMessages] = useState<ChatMessage[]>(initialRef.current.messages);
  const [input, setInput] = useState("");
  const [loading, setLoading] = useState(false);
  const [lastFailedQuestion, setLastFailedQuestion] = useState("");
  const [listening, setListening] = useState(false);
  const [voiceError, setVoiceError] = useState("");
  const [surveyStep, setSurveyStep] = useState<number | null>(null);
  const [surveyAnswers, setSurveyAnswers] = useState<SurveyAnswers>({});
  const bottomRef = useRef<HTMLDivElement | null>(null);
  const textareaRef = useRef<HTMLTextAreaElement | null>(null);

  useEffect(() => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify({ chatId, messages: messages.slice(-50) }));
    bottomRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [chatId, messages, loading]);

  const send = async (text: string) => {
    const question = text.trim();
    if (!question || loading) return;
    setMessages((current) => [...current, { id: crypto.randomUUID(), role: "user", text: question, createdAt: new Date().toISOString() }]);
    setInput("");
    setLoading(true);
    setLastFailedQuestion("");
    try {
      const listingMatch = location.pathname.match(/^\/listing\/([^/]+)/);
      const response = await chatbotAPI.askAdvisor(question, chatId, { page: listingMatch ? "vehicle-detail" : location.pathname, currentVehicleId: listingMatch?.[1] });
      setMessages((current) => [...current, {
        id: crypto.randomUUID(),
        role: "assistant",
        text: response.answer?.trim() || "Mình chưa nhận được nội dung trả lời. Bạn hãy thử hỏi lại theo cách khác nhé.",
        recommendations: response.recommendations,
        suggestions: response.recommendations?.length ? followUpSuggestions(response.answer || "", response.recommendations) : [],
        createdAt: new Date().toISOString()
      }]);
    } catch (error) {
      setLastFailedQuestion(question);
      const technicalMessage = error instanceof Error ? error.message : "";
      const friendlyMessage = /timeout|timed out|exceeded/i.test(technicalMessage)
        ? "AI đang cần thêm thời gian để phân tích dữ liệu. Bạn hãy bấm “Thử gửi lại câu hỏi”; mình sẽ tiếp tục chờ kết quả lâu hơn."
        : "Dịch vụ tư vấn đang tạm bận. Bạn vui lòng thử gửi lại câu hỏi sau ít phút.";
      setMessages((current) => [...current, {
        id: crypto.randomUUID(),
        role: "assistant",
        text: friendlyMessage,
        createdAt: new Date().toISOString()
      }]);
    } finally {
      setLoading(false);
      textareaRef.current?.focus();
    }
  };

  const submit = (event: FormEvent) => {
    event.preventDefault();
    void send(input);
  };

  const handleKeyDown = (event: KeyboardEvent<HTMLTextAreaElement>) => {
    if (event.key === "Enter" && !event.shiftKey) {
      event.preventDefault();
      void send(input);
    }
  };

  const startVoiceInput = () => {
    const speechWindow = window as typeof window & { SpeechRecognition?: BrowserSpeechRecognitionConstructor; webkitSpeechRecognition?: BrowserSpeechRecognitionConstructor };
    const Recognition = speechWindow.SpeechRecognition || speechWindow.webkitSpeechRecognition;
    if (!Recognition) { setVoiceError("Trình duyệt này chưa hỗ trợ nhập bằng giọng nói."); return; }
    const recognition = new Recognition();
    recognition.lang = "vi-VN";
    recognition.interimResults = false;
    recognition.maxAlternatives = 1;
    recognition.onresult = (event) => { const transcript = event.results[0]?.[0]?.transcript?.trim(); if (transcript) setInput((current) => current ? `${current} ${transcript}` : transcript); };
    recognition.onerror = () => { setVoiceError("Không nghe rõ. Hãy kiểm tra quyền micro và thử lại."); setListening(false); };
    recognition.onend = () => setListening(false);
    setVoiceError("");
    setListening(true);
    recognition.start();
  };

  const reset = () => {
    const session = newSession();
    setChatId(session.chatId);
    setMessages(session.messages);
    setInput("");
    setSurveyStep(null);
    setSurveyAnswers({});
    setLastFailedQuestion("");
    localStorage.removeItem(STORAGE_KEY);
  };

  const startSurvey = () => {
    setSurveyAnswers({});
    setSurveyStep(0);
  };

  const selectSurveyOption = (value: string) => {
    if (surveyStep == null) return;
    const step = surveySteps[surveyStep];
    const answers = { ...surveyAnswers, [step.key]: value };
    setSurveyAnswers(answers);
    if (surveyStep < surveySteps.length - 1) {
      setSurveyStep(surveyStep + 1);
      return;
    }
    setSurveyStep(null);
    void send(`Tư vấn xe cho tôi dựa trên nhu cầu sau: ${surveySteps.map((item) => `${item.question} ${answers[item.key]}`).join("; ")}. Hãy đề xuất các mẫu phù hợp nhất.`);
  };

  const buyProduct = (product: ChatbotRecommendation) => {
    if (product.stockQuantity <= 0) return;
    navigate(`/listing/${product.slug}`);
  };

  return (
    <main className={`${embedded ? "h-full min-h-0" : "min-h-screen pt-[88px]"} bg-slate-950 text-slate-100`}>
      <div className={`mx-auto flex ${embedded ? "h-full" : "h-[calc(100dvh-88px)] max-w-6xl"} flex-col`}>
        <header className={`${embedded ? "hidden" : "flex"} items-center justify-between border-b border-white/10 px-4 py-4 sm:px-6`}>
          <div className="flex items-center gap-3">
            <div className="grid h-11 w-11 place-items-center rounded-2xl bg-violet-600 shadow-lg shadow-violet-600/20"><Bot className="h-6 w-6" /></div>
            <div><h1 className="font-bold">MOTIONX AI</h1><p className="text-xs text-slate-400">Tư vấn dựa trên sản phẩm và chính sách thực tế</p></div>
          </div>
          <button type="button" onClick={reset} className="flex items-center gap-2 rounded-xl border border-white/10 px-3 py-2 text-sm text-slate-300 hover:bg-white/5"><RotateCcw className="h-4 w-4" /><span className="hidden sm:inline">Cuộc trò chuyện mới</span></button>
        </header>

        <section className={`min-h-0 flex-1 overflow-y-auto ${embedded ? "px-3 py-4" : "px-4 py-6 sm:px-6"}`}>
          <div className={`mx-auto max-w-3xl ${embedded ? "space-y-4" : "space-y-6"}`}>
            {messages.length === 1 && surveyStep == null ? (
              <button type="button" onClick={startSurvey} className={`mx-auto flex w-full max-w-xl items-center ${embedded ? "gap-3 p-3" : "gap-4 p-4"} rounded-2xl border border-blue-500/30 bg-gradient-to-r from-blue-600/15 to-cyan-500/10 text-left transition hover:border-blue-400 hover:bg-blue-600/20`}>
                <div className={`${embedded ? "h-9 w-9" : "h-11 w-11"} grid shrink-0 place-items-center rounded-xl bg-blue-600`}><ClipboardList className="h-5 w-5" /></div>
                <div className="flex-1"><p className="font-semibold text-white">Bình chọn để tìm xe phù hợp</p><p className="mt-1 text-xs text-slate-400">Trả lời 4 câu hỏi nhanh, nhận gợi ý xe ngay</p></div>
                <span className="text-sm font-semibold text-blue-300">Bắt đầu</span>
              </button>
            ) : null}

            {surveyStep != null ? (
              <div className="mx-auto max-w-xl rounded-2xl border border-blue-500/30 bg-slate-900 p-5 shadow-xl shadow-blue-950/20">
                <div className="flex items-start justify-between gap-4">
                  <div><p className="text-xs font-bold uppercase tracking-wider text-blue-400">Bước {surveyStep + 1}/{surveySteps.length}</p><h2 className="mt-2 text-lg font-semibold text-white">{surveySteps[surveyStep].question}</h2></div>
                  <button type="button" onClick={() => setSurveyStep(null)} aria-label="Đóng bình chọn" className="rounded-lg p-1.5 text-slate-400 hover:bg-white/5 hover:text-white"><X className="h-4 w-4" /></button>
                </div>
                <div className="mt-4 h-1.5 overflow-hidden rounded-full bg-slate-800"><div className="h-full rounded-full bg-blue-500 transition-all" style={{ width: `${((surveyStep + 1) / surveySteps.length) * 100}%` }} /></div>
                <div className="mt-5 grid gap-2">
                  {surveySteps[surveyStep].options.map((option) => (
                    <button key={option} type="button" onClick={() => selectSurveyOption(option)} className="flex items-center justify-between rounded-xl border border-white/10 bg-slate-800/70 px-4 py-3 text-left text-sm text-slate-200 transition hover:border-blue-500 hover:bg-blue-500/10 hover:text-white"><span>{option}</span><Check className="h-4 w-4 opacity-40" /></button>
                  ))}
                </div>
              </div>
            ) : null}
            {messages.map((message) => (
              <article key={message.id} className={`flex gap-3 ${message.role === "user" ? "flex-row-reverse" : ""}`}>
                <div className={`mt-1 grid h-8 w-8 shrink-0 place-items-center rounded-full ${message.role === "user" ? "bg-blue-600" : "bg-emerald-600"}`}>
                  {message.role === "user" ? <UserRound className="h-4 w-4" /> : <Bot className="h-4 w-4" />}
                </div>
                <div className={`max-w-[88%] ${message.role === "user" ? "text-right" : ""}`}>
                  <div className={`inline-block whitespace-pre-wrap rounded-2xl px-4 py-3 text-left text-sm leading-6 ${message.role === "user" ? "rounded-tr-sm bg-blue-600" : "rounded-tl-sm border border-white/10 bg-slate-900"}`}>{cleanChatText(message.text)}</div>
                  {message.role === "assistant" && message.suggestions?.length ? <div className="mt-2 flex flex-wrap gap-2 text-left">{message.suggestions.map((suggestion) => <button key={suggestion} type="button" disabled={loading} onClick={() => void send(suggestion)} className="rounded-full border border-violet-400/30 bg-violet-500/10 px-3 py-2 text-xs font-semibold text-violet-100 transition hover:border-violet-300 hover:bg-violet-500/20 disabled:opacity-50">{suggestion}</button>)}</div> : null}
                  {message.recommendations?.length ? (
                    <div className={`mt-3 grid text-left ${embedded ? "grid-cols-4 gap-2" : "gap-4 sm:grid-cols-2"}`}>
                      {message.recommendations.slice(0, 4).map((product) => (
                        <div key={product.id} className="group overflow-hidden rounded-2xl border border-white/10 bg-slate-900 transition hover:border-blue-500/60">
                          <Link to={`/listing/${product.slug}`} className={`block ${embedded ? "h-16" : "h-40"} overflow-hidden bg-slate-800`}>
                            {product.imageUrl ? (
                              <img src={resolveMarketplaceMediaUrl(product.imageUrl)} alt={product.name} onError={(event) => attachImageFallback(event, "MOTIONX")} className="h-full w-full object-cover transition duration-500 group-hover:scale-105" loading="lazy" />
                            ) : (
                              <div className="grid h-full place-items-center text-xl font-black tracking-[0.2em] text-slate-600">MOTIONX</div>
                            )}
                          </Link>
                          <div className={embedded ? "p-2" : "p-4"}>
                            {!embedded ? <p className="text-[10px] font-bold uppercase tracking-wider text-blue-400">{product.categoryName || "Xe điện MOTIONX"}</p> : null}
                            <Link to={`/listing/${product.slug}`} className={`${embedded ? "line-clamp-2 min-h-8 text-[10px] leading-4" : "mt-1"} block font-semibold text-white hover:text-blue-300`}>{product.name}</Link>
                            <div className="mt-2 flex items-baseline gap-2">
                              <p className={`${embedded ? "truncate text-[10px]" : ""} font-bold text-orange-400`}>{formatPrice(product.discountPrice ?? product.price)}</p>
                              {!embedded && product.discountPrice != null && product.discountPrice < product.price ? <p className="text-xs text-slate-500 line-through">{formatPrice(product.price)}</p> : null}
                            </div>
                            {!embedded ? <p className="mt-2 line-clamp-3 text-xs leading-5 text-slate-400">{product.reason}</p> : null}
                            {!embedded ? <p className={`mt-2 text-xs font-medium ${product.stockQuantity > 0 ? "text-emerald-400" : "text-amber-400"}`}>{product.stockQuantity > 0 ? `Còn ${product.stockQuantity} xe` : "Tạm hết hàng"}</p> : null}
                            <div className={`${embedded ? "mt-2" : "mt-4"} grid grid-cols-2 gap-2`}>
                              {embedded ? <Link to={`/listing/${product.slug}`} className="col-span-2 block rounded-lg border border-white/10 px-1 py-1.5 text-center text-[10px] font-bold leading-none text-slate-200 hover:bg-white/5">Mở tin</Link> : <Link to={`/listing/${product.slug}`} className="inline-flex items-center justify-center gap-1 rounded-lg border border-white/10 px-3 py-2 text-xs font-semibold text-slate-200 hover:bg-white/5"><Eye className="h-3.5 w-3.5" />Chi tiết</Link>}
                              {!embedded ? <button type="button" onClick={() => buyProduct(product)} className="inline-flex items-center justify-center gap-1.5 rounded-lg bg-orange-600 px-3 py-2 text-xs font-semibold text-white hover:bg-orange-500"><ShoppingCart className="h-3.5 w-3.5" />Xem tin</button> : null}
                            </div>
                          </div>
                        </div>
                      ))}
                    </div>
                  ) : null}
                </div>
              </article>
            ))}
            {loading ? <div className="flex items-center gap-3 text-sm text-slate-400"><Loader2 className="h-5 w-5 animate-spin text-blue-400" />Đang phân tích nhu cầu và dữ liệu sản phẩm...</div> : null}
            {!loading && lastFailedQuestion ? <button type="button" onClick={() => void send(lastFailedQuestion)} className="mx-auto flex items-center gap-2 rounded-full border border-amber-400/30 bg-amber-400/10 px-4 py-2 text-sm font-semibold text-amber-200 hover:bg-amber-400/20"><RotateCcw className="h-4 w-4" />Thử gửi lại câu hỏi</button> : null}
            <div ref={bottomRef} />
          </div>
        </section>

        <footer className={`border-t border-white/10 bg-slate-950/95 ${embedded ? "px-3 py-3" : "px-4 py-4 sm:px-6"}`}>
          <div className="mx-auto max-w-3xl">
            {messages.length === 1 ? <div className="mb-3 flex gap-2 overflow-x-auto pb-1">{starterQuestions.map((question) => <button key={question} type="button" onClick={() => void send(question)} className="shrink-0 rounded-full border border-white/10 bg-slate-900 px-3 py-2 text-xs text-slate-300 hover:border-blue-500">{question}</button>)}</div> : null}
            <form onSubmit={submit} className="flex items-end gap-2 rounded-2xl border border-white/10 bg-slate-900 p-2 focus-within:border-blue-500/70">
              <textarea ref={textareaRef} value={input} onChange={(event) => setInput(event.target.value)} onKeyDown={handleKeyDown} maxLength={MAX_MESSAGE_LENGTH} rows={1} disabled={loading} placeholder="Mô tả nhu cầu, ngân sách, quãng đường mỗi ngày..." className="max-h-32 min-h-11 flex-1 resize-none bg-transparent px-3 py-3 text-sm outline-none placeholder:text-slate-500" />
              <button type="button" onClick={startVoiceInput} disabled={loading || listening} aria-label="Nhập câu hỏi bằng giọng nói" title="Nhập bằng giọng nói" className={`grid h-11 w-11 place-items-center rounded-xl transition disabled:opacity-50 ${listening ? "bg-rose-500 text-white" : "text-slate-400 hover:bg-white/5 hover:text-white"}`}>{listening ? <MicOff className="h-5 w-5 animate-pulse" /> : <Mic className="h-5 w-5" />}</button>
              <button type="submit" disabled={loading || !input.trim()} aria-label="Gửi câu hỏi" className="grid h-11 w-11 place-items-center rounded-xl bg-blue-600 transition hover:bg-blue-500 disabled:cursor-not-allowed disabled:opacity-40"><Send className="h-5 w-5" /></button>
            </form>
            {voiceError ? <p className="mt-2 text-center text-xs text-amber-300">{voiceError}</p> : null}
            {!embedded ? <div className="mt-2 flex items-center justify-center gap-1.5 text-[11px] text-slate-500"><ShieldCheck className="h-3.5 w-3.5" />Không gửi mật khẩu, OTP hoặc thông tin thẻ. AI có thể sai; hãy kiểm tra lại thông tin quan trọng.</div> : null}
          </div>
        </footer>
      </div>
    </main>
  );
};

export default ChatbotPage;
