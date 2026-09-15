import { Bot, MessageCircle, Minus, Sparkles, X } from "lucide-react";
import { useState } from "react";
import { useLocation } from "react-router-dom";
import ChatbotPage from "../../pages/ChatbotPage";

const FloatingChatButton = () => {
  const location = useLocation();
  const [isOpen, setIsOpen] = useState(false);

  if (location.pathname.startsWith("/chatbot")) {
    return null;
  }

  return (
    <div className={`floating-chat ${isOpen ? "floating-chat--open" : ""}`}>
      {isOpen ? (
        <section className="chat-popover" aria-label="Trợ lý tư vấn MOTIONX AI">
          <header className="chat-popover__header">
            <div className="flex items-center gap-3">
              <span className="relative grid h-10 w-10 place-items-center rounded-2xl bg-white/15 text-white"><Bot size={22} /><MessageCircle size={10} className="absolute bottom-1 right-1" /></span>
              <div><p className="font-bold text-white">MOTIONX AI</p><p className="flex items-center gap-1.5 text-[11px] text-white/75"><span className="h-2 w-2 rounded-full bg-emerald-300" />Trợ lý tư vấn đang trực tuyến</p></div>
            </div>
            <div className="flex items-center gap-1">
              <button type="button" onClick={() => setIsOpen(false)} className="chat-popover__control" aria-label="Thu nhỏ cửa sổ chat"><Minus size={18} /></button>
              <button type="button" onClick={() => setIsOpen(false)} className="chat-popover__control" aria-label="Đóng cửa sổ chat"><X size={18} /></button>
            </div>
          </header>
          <div className="min-h-0 flex-1 overflow-hidden"><ChatbotPage embedded /></div>
        </section>
      ) : (
        <>
          <span className="floating-chat__label">
            <span className="flex items-center gap-1.5 text-[10px] font-bold uppercase tracking-[.15em] text-[#7c3aed]"><Sparkles size={12} /> MOTIONX AI</span>
            <span className="mt-0.5 block text-sm font-bold text-[#171329]">Chat với trợ lý AI</span>
          </span>
          <button type="button" onClick={() => setIsOpen(true)} className="floating-chat__icon" aria-label="Mở chatbot tư vấn MOTIONX AI" title="Chat với MOTIONX AI">
            <Bot size={31} strokeWidth={2.3} />
            <MessageCircle size={14} fill="currentColor" className="absolute bottom-3 right-3 rounded-full bg-white text-fuchsia-500" />
            <span className="floating-chat__dot" />
          </button>
        </>
      )}
    </div>
  );
};

export default FloatingChatButton;
