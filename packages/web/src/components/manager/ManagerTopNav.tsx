import { useLocation } from "react-router-dom";
import NotificationBell from "../common/NotificationBell";
const titles: Record<string,string> = { "/manager":"Tổng quan vận hành", "/manager/listings":"Kiểm duyệt tin đăng", "/manager/reports":"Báo cáo vi phạm", "/manager/transactions":"Giao dịch marketplace", "/manager/products":"Tin bán xe của tôi", "/manager/products/new":"Đăng tin bán xe", "/manager/support":"Hỗ trợ khách hàng" };
export default function ManagerTopNav() {
  const { pathname }=useLocation();
  return <header className="manager-topnav sticky top-0 z-30 flex min-h-[80px] items-center justify-between border-b border-slate-200 bg-white/95 px-6 backdrop-blur"><div><h1 className="text-xl font-bold">{titles[pathname]||"Quản lý tin đăng"}</h1><p className="mt-1 text-sm text-slate-500">Vận hành sàn mua bán xe MOTIONX</p></div><NotificationBell enabled /></header>;
}
