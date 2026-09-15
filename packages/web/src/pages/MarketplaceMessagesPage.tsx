import { useAuth } from "@ebike/shared-code/hooks";
import { FileText, ImagePlus, MapPin, MessageCircle, Search, Send, X, ZoomIn } from "lucide-react";
import { type FormEvent, useEffect, useMemo, useRef, useState } from "react";
import { Link, Navigate, useSearchParams } from "react-router-dom";
import {
  marketplaceAPI,
  formatMarketplacePrice,
  resolveMarketplaceMediaUrl,
  type MarketplaceConversation,
  type MarketplaceMessage,
} from "../services/marketplace";

const MarketplaceMessagesPage = () => {
  const { isAuthenticated, isBootstrapping, user } = useAuth();
  const [searchParams] = useSearchParams();
  const requestedConversation = searchParams.get("conversation") || "";
  const [conversations, setConversations] = useState<MarketplaceConversation[]>(
    [],
  );
  const [selected, setSelected] = useState("");
  const [messages, setMessages] = useState<MarketplaceMessage[]>([]);
  const [text, setText] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const [query, setQuery] = useState("");
  const [unreadOnly, setUnreadOnly] = useState(false);
  const [otherTyping, setOtherTyping] = useState(false);
  const [previewImage, setPreviewImage] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement | null>(null);
  const typingTimeoutRef = useRef<number | null>(null);
  const messagesEndRef = useRef<HTMLDivElement | null>(null);
  const messagesListRef = useRef<HTMLDivElement | null>(null);
  const allowAutoScrollRef = useRef(true);
  const activeConversation = conversations.find((item) => item.publicId === selected);
  const filteredConversations = useMemo(() => conversations.filter((item) => {
    const matches = `${item.otherName} ${item.listingTitle}`.toLowerCase().includes(query.toLowerCase());
    return matches && (!unreadOnly || item.unreadCount > 0);
  }), [conversations, query, unreadOnly]);

  useEffect(() => {
    if (!isAuthenticated) return;
    marketplaceAPI
      .conversations()
      .then((items) => {
        setConversations(items);
        const requestedExists = items.some(
          (item) => item.publicId === requestedConversation,
        );
        if (requestedExists) setSelected(requestedConversation);
        else if (items[0]) setSelected(items[0].publicId);
      })
      .catch((loadError) =>
        setError(
          loadError instanceof Error
            ? loadError.message
            : "Không thể tải hộp thư.",
        ),
      )
      .finally(() => setLoading(false));
  }, [isAuthenticated, requestedConversation]);

  useEffect(() => {
    if (!selected) {
      setMessages([]);
      return;
    }
    allowAutoScrollRef.current = true;
    setLoading(true);
    marketplaceAPI
      .conversationMessages(selected)
      .then((items) => {
        setMessages(items);
        setConversations((current) =>
          current.map((conversation) =>
            conversation.publicId === selected
              ? { ...conversation, unreadCount: 0 }
              : conversation,
          ),
        );
      })
      .catch((loadError) =>
        setError(
          loadError instanceof Error
            ? loadError.message
            : "Không thể tải tin nhắn.",
        ),
      )
      .finally(() => setLoading(false));
  }, [selected]);

  useEffect(() => {
    if (!selected) return;
    const refresh = () => {
      void Promise.all([marketplaceAPI.conversationMessages(selected), marketplaceAPI.typingStatus(selected)])
        .then(([items, status]) => { setMessages(items); setOtherTyping(status.typing); })
        .catch(() => undefined);
    };
    const intervalId = window.setInterval(refresh, 2500);
    return () => window.clearInterval(intervalId);
  }, [selected]);

  const send = async (event: FormEvent) => {
    event.preventDefault();
    if (!selected || !text.trim()) return;
    setLoading(true);
    setError("");
    try {
      const message = await marketplaceAPI.sendMessage(selected, text);
      allowAutoScrollRef.current = true;
      setMessages((current) => [...current, message]);
      setText("");
      void marketplaceAPI.setTyping(selected, false);
    } catch (sendError) {
      setError(
        sendError instanceof Error
          ? sendError.message
          : "Không thể gửi tin nhắn.",
      );
    } finally {
      setLoading(false);
    }
  };

  const handleTextChange = (value: string) => {
    setText(value);
    if (!selected) return;
    void marketplaceAPI.setTyping(selected, value.trim().length > 0).catch(() => undefined);
    if (typingTimeoutRef.current) window.clearTimeout(typingTimeoutRef.current);
    typingTimeoutRef.current = window.setTimeout(() => { void marketplaceAPI.setTyping(selected, false).catch(() => undefined); }, 1800);
  };

  const sendSuggestion = async (suggestion: string) => {
    if (!selected || loading) return;
    setLoading(true); setError("");
    try { const message = await marketplaceAPI.sendMessage(selected, suggestion); allowAutoScrollRef.current = true; setMessages((current) => [...current, message]); setText(""); }
    catch (sendError) { setError(sendError instanceof Error ? sendError.message : "Không thể gửi tin nhắn."); }
    finally { setLoading(false); }
  };

  const sendAttachment = async (file?: File) => {
    if (!selected || !file) return;
    setLoading(true); setError("");
    try { const message = await marketplaceAPI.sendAttachment(selected, file); allowAutoScrollRef.current = true; setMessages((current) => [...current, message]); }
    catch (uploadError) { setError(uploadError instanceof Error ? uploadError.message : "Không thể gửi tệp."); }
    finally { setLoading(false); if (fileInputRef.current) fileInputRef.current.value = ""; }
  };

  const sendLocation = () => {
    if (!selected || !navigator.geolocation) { setError("Trình duyệt không hỗ trợ chia sẻ vị trí."); return; }
    setLoading(true); setError("");
    navigator.geolocation.getCurrentPosition(async ({ coords }) => {
      try { const message = await marketplaceAPI.sendLocation(selected, coords.latitude, coords.longitude); allowAutoScrollRef.current = true; setMessages((current) => [...current, message]); }
      catch (locationError) { setError(locationError instanceof Error ? locationError.message : "Không thể gửi vị trí."); }
      finally { setLoading(false); }
    }, () => { setError("Không thể truy cập vị trí. Hãy cho phép trình duyệt."); setLoading(false); });
  };

  const suggestions = ["Xe còn hay đã bán rồi ạ?", "Giá xe có thể thương lượng không?", "Xe đã từng sửa chữa chưa?", "Mình có thể xem xe khi nào?"];

  useEffect(() => {
    if (allowAutoScrollRef.current) messagesEndRef.current?.scrollIntoView({ behavior: "smooth", block: "end" });
  }, [messages.length, selected]);

  if (isBootstrapping)
    return (
      <div className="grid min-h-screen place-items-center">
        Đang kiểm tra đăng nhập...
      </div>
    );
  if (!isAuthenticated)
    return <Navigate to="/auth" replace state={{ from: "/messages" }} />;

  return (
    <main className="bg-[#fff8f5] px-5 pb-5 pt-28 lg:h-screen lg:overflow-hidden">
      <div className="mx-auto flex h-full max-w-6xl flex-col">
        <div className="mb-4 shrink-0">
          <p className="text-xs font-bold uppercase tracking-[.18em] text-violet-600">
            Marketplace
          </p>
          <h1 className="mt-2 text-4xl font-black text-slate-950">Tin nhắn</h1>
          <p className="mt-2 text-slate-500">
            Trao đổi giữa người mua và người đăng tin.
          </p>
        </div>
        {error ? (
          <p className="mb-4 rounded-xl bg-red-50 px-4 py-3 text-red-600">
            {error}
          </p>
        ) : null}
        <div className="grid min-h-[520px] flex-1 overflow-hidden rounded-[28px] border border-violet-100 bg-white shadow-xl lg:min-h-0 lg:grid-cols-[340px_1fr]">
          <aside className="border-b border-violet-100 bg-violet-50/50 p-3 lg:min-h-0 lg:overflow-y-auto lg:border-b-0 lg:border-r">
            <div className="mb-3 rounded-2xl bg-white p-3 shadow-sm">
              <div className="flex items-center gap-2 rounded-xl border border-violet-100 px-3">
                <Search size={17} className="text-slate-400" />
                <input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Tìm người bán hoặc tin đăng" className="min-w-0 flex-1 py-3 text-sm outline-none" />
              </div>
              <div className="mt-3 flex gap-2">
                <button onClick={() => setUnreadOnly(false)} className={`rounded-full px-3 py-1.5 text-xs font-bold ${!unreadOnly ? "bg-violet-600 text-white" : "bg-slate-100 text-slate-600"}`}>Tất cả</button>
                <button onClick={() => setUnreadOnly(true)} className={`rounded-full px-3 py-1.5 text-xs font-bold ${unreadOnly ? "bg-violet-600 text-white" : "bg-slate-100 text-slate-600"}`}>Chưa đọc</button>
              </div>
            </div>
            {filteredConversations.length ? (
              filteredConversations.map((conversation) => (
                <button
                  key={conversation.publicId}
                  onClick={() => setSelected(conversation.publicId)}
                  className={`mb-2 w-full rounded-2xl p-4 text-left transition ${selected === conversation.publicId ? "bg-violet-600 text-white" : "bg-white hover:bg-violet-100"}`}
                >
                  <div className="flex items-center justify-between gap-2">
                    <p className="min-w-0 truncate font-bold">{conversation.otherName}</p>
                    {conversation.unreadCount > 0 ? (
                      <span className={`inline-flex min-w-[20px] shrink-0 items-center justify-center rounded-full px-1.5 text-[11px] font-bold leading-5 ${selected === conversation.publicId ? "bg-white text-violet-700" : "bg-[#ef3f7f] text-white"}`}>
                        {conversation.unreadCount > 99 ? "99+" : conversation.unreadCount}
                      </span>
                    ) : null}
                  </div>
                  <p
                    className={`mt-1 truncate text-sm ${selected === conversation.publicId ? "text-white/75" : "text-slate-500"}`}
                  >
                    {conversation.listingTitle}
                  </p>
                  <p
                    className={`mt-2 truncate text-xs ${selected === conversation.publicId ? "text-white/60" : "text-slate-400"}`}
                  >
                    {conversation.lastMessage || "Chưa có tin nhắn"}
                  </p>
                </button>
              ))
            ) : (
              <div className="p-8 text-center text-sm text-slate-500">
                <MessageCircle className="mx-auto mb-3 text-violet-300" />
                Chưa có cuộc trò chuyện.
              </div>
            )}
          </aside>
          <section className="flex min-h-[500px] flex-col overflow-hidden lg:min-h-0">
            {activeConversation ? (
              <div className="flex items-center gap-3 border-b border-violet-100 bg-white p-4">
                <div className="grid h-11 w-11 shrink-0 place-items-center overflow-hidden rounded-full bg-violet-100 font-bold text-violet-700">
                  {activeConversation.otherAvatarUrl ? <img src={resolveMarketplaceMediaUrl(activeConversation.otherAvatarUrl)} alt="" className="h-full w-full object-cover" /> : activeConversation.otherName.charAt(0)}
                </div>
                <div className="min-w-0 flex-1"><p className="font-extrabold">{activeConversation.otherName}</p><p className="truncate text-xs text-slate-500">{activeConversation.listingTitle}</p></div>
                <Link to={`/listing/${activeConversation.listingPublicId}`} className="hidden items-center gap-3 rounded-xl bg-violet-50 p-2 pr-4 sm:flex">
                  {activeConversation.listingImageUrl ? <img src={resolveMarketplaceMediaUrl(activeConversation.listingImageUrl)} alt="" className="h-10 w-12 rounded-lg object-cover" /> : null}
                  <span><b className="block max-w-48 truncate text-xs">{activeConversation.listingTitle}</b>{activeConversation.listingPrice ? <span className="text-xs font-bold text-violet-700">{formatMarketplacePrice(activeConversation.listingPrice)}</span> : null}</span>
                </Link>
              </div>
            ) : null}
            {otherTyping ? <div className="border-b border-violet-50 bg-violet-50/50 px-5 py-2 text-xs font-semibold text-violet-700">{activeConversation?.otherName} đang gõ<span className="animate-pulse">...</span></div> : null}
            <div ref={messagesListRef} onScroll={() => { const element = messagesListRef.current; if (element) allowAutoScrollRef.current = element.scrollHeight - element.scrollTop - element.clientHeight < 100; }} className="min-h-0 flex-1 space-y-3 overflow-y-auto p-5 sm:p-7">
              {selected ? (
                messages.map((message) => {
                  const mine = Number(message.senderId) === Number(user?.id);
                  return (
                    <div
                      key={message.publicId}
                      className={`flex ${mine ? "justify-end" : "justify-start"}`}
                    >
                      <div
                        className={`max-w-[78%] rounded-2xl px-4 py-3 ${mine ? "bg-violet-600 text-white" : "bg-slate-100 text-slate-800"}`}
                      >
                        <p
                          className={`text-xs font-bold ${mine ? "text-white/70" : "text-violet-600"}`}
                        >
                          {message.senderName}
                        </p>
                        {message.messageType === "IMAGE" && message.attachmentUrl ? <button type="button" onClick={() => setPreviewImage(resolveMarketplaceMediaUrl(message.attachmentUrl!))} className="group relative mt-2 block overflow-hidden rounded-xl"><img src={resolveMarketplaceMediaUrl(message.attachmentUrl)} alt={message.fileName || "Ảnh"} className="max-h-72 object-contain" /><span className="absolute inset-0 grid place-items-center bg-black/0 text-white opacity-0 transition group-hover:bg-black/25 group-hover:opacity-100"><ZoomIn size={28} /></span></button> : null}
                        {message.messageType === "VIDEO" && message.attachmentUrl ? <video src={resolveMarketplaceMediaUrl(message.attachmentUrl)} controls className="mt-2 max-h-72 rounded-xl" /> : null}
                        {message.messageType === "DOCUMENT" && message.attachmentUrl ? <a href={resolveMarketplaceMediaUrl(message.attachmentUrl)} target="_blank" rel="noreferrer" className="mt-2 flex items-center gap-2 rounded-xl bg-white/15 p-3 font-semibold"><FileText size={20} />{message.fileName || "Tải tệp đính kèm"}</a> : null}
                        {message.messageType === "LOCATION" && message.latitude && message.longitude ? <a href={`https://www.google.com/maps?q=${message.latitude},${message.longitude}`} target="_blank" rel="noreferrer" className="mt-2 flex items-center gap-2 rounded-xl bg-white/15 p-3 font-semibold"><MapPin size={20} />Xem vị trí trên bản đồ</a> : null}
                        {message.content && message.messageType === "TEXT" ? <p className="mt-1 whitespace-pre-wrap text-sm">{message.content}</p> : null}
                        <p className={`mt-1 text-[10px] ${mine ? "text-white/55" : "text-slate-400"}`}>{new Date(message.createdAt).toLocaleTimeString("vi-VN", { hour: "2-digit", minute: "2-digit" })}</p>
                        {mine ? <p className="mt-1 text-right text-[10px] font-semibold text-white/70">{message.read ? "Đã xem" : "Đã gửi"}</p> : null}
                      </div>
                    </div>
                  );
                })
              ) : (
                <div className="grid h-full place-items-center text-slate-400">
                  Chọn một cuộc trò chuyện
                </div>
              )}
              <div ref={messagesEndRef} />
            </div>
            {selected ? (
              <form
                onSubmit={send}
                className="flex flex-wrap gap-3 border-t border-violet-100 bg-white p-4"
              >
                <div className="flex w-full gap-2 overflow-x-auto pb-1">
                  {suggestions.map((suggestion) => <button key={suggestion} type="button" onClick={() => void sendSuggestion(suggestion)} className="shrink-0 rounded-full border border-violet-100 bg-violet-50 px-4 py-2 text-xs font-semibold text-violet-800">{suggestion}</button>)}
                </div>
                <input
                  maxLength={2000}
                  value={text}
                  onChange={(event) => handleTextChange(event.target.value)}
                  placeholder="Nhập tin nhắn..."
                  className="min-w-0 flex-1 rounded-xl border border-violet-100 px-4 py-3 outline-none"
                />
                <input ref={fileInputRef} type="file" className="hidden" accept="image/*,video/*,.pdf,.doc,.docx,.xls,.xlsx,.txt,.zip" onChange={(event) => void sendAttachment(event.target.files?.[0])} />
                <button type="button" onClick={() => fileInputRef.current?.click()} disabled={loading} title="Gửi ảnh, video hoặc file" className="grid h-12 w-12 place-items-center rounded-xl bg-violet-50 text-violet-700"><ImagePlus size={19} /></button>
                <button type="button" onClick={sendLocation} disabled={loading} title="Gửi vị trí" className="grid h-12 w-12 place-items-center rounded-xl bg-violet-50 text-violet-700"><MapPin size={19} /></button>
                <button
                  disabled={loading || !text.trim()}
                  aria-label="Gửi tin nhắn"
                  className="grid h-12 w-12 place-items-center rounded-xl bg-violet-600 text-white disabled:opacity-50"
                >
                  <Send size={19} />
                </button>
              </form>
            ) : null}
          </section>
        </div>
      </div>
      {previewImage ? <div className="fixed inset-0 z-[100] flex items-center justify-center bg-black/90 p-4" role="dialog" aria-modal="true" onClick={() => setPreviewImage(null)}><button type="button" onClick={() => setPreviewImage(null)} aria-label="Đóng ảnh" className="absolute right-5 top-5 grid h-11 w-11 place-items-center rounded-full bg-white/15 text-white hover:bg-white/25"><X size={25} /></button><img src={previewImage} alt="Ảnh đính kèm" className="max-h-[90vh] max-w-full object-contain" onClick={(event) => event.stopPropagation()} /></div> : null}
    </main>
  );
};

export default MarketplaceMessagesPage;
