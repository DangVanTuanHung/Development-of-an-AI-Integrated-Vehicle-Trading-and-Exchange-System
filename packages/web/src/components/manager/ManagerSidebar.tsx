import { BarChart3, ExternalLink, Headphones, Home, LayoutDashboard, LogOut, Package, ShoppingBag, User } from "lucide-react";
import { NavLink, useNavigate } from "react-router-dom";
import { useAuth } from "@ebike/shared-code/hooks";

const managerNavItems = [
  { to: "/manager", label: "Tổng quan vận hành", icon: LayoutDashboard, end: true },
  { to: "/manager/listings", label: "Kiểm duyệt tin đăng", icon: Package },
  { to: "/manager/reports", label: "Báo cáo vi phạm", icon: BarChart3 },
  { to: "/manager/transactions", label: "Giao dịch marketplace", icon: ShoppingBag },
  { to: "/manager/support", label: "Hỗ trợ khách hàng", icon: Headphones },
  { to: "/manager/products", label: "Tin bán xe của tôi", icon: Package }
];

const quickAccessItems = [
  { to: "/", label: "Trang chủ", icon: Home, end: true },
  { to: "/products", label: "Mẫu xe", icon: ExternalLink },
  { to: "/customer/profile", label: "Hồ sơ khách hàng", icon: User }
];

const ManagerSidebar = () => {
  const navigate = useNavigate();
  const { user, logout } = useAuth();

  const fullName = user?.fullName?.trim() || user?.username || "Quản lý";

  const handleLogout = async () => {
    await logout().unwrap();
    navigate("/auth");
  };

  return (
    <aside className="manager-sidebar fixed inset-y-0 left-0 z-40 flex w-[248px] flex-col border-r border-slate-200 bg-white">
      <div className="border-b border-slate-200 px-6 py-6">
        <p className="text-xl font-bold tracking-tight text-[#003b93]">MOTIONX</p>
        <p className="mt-1 text-[10px] font-bold uppercase tracking-[0.24em] text-slate-400">Trang quản lý</p>
      </div>

      <nav className="flex-1 overflow-y-auto px-3 py-5">
        <div className="space-y-1">
          {managerNavItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end}
              className={({ isActive }) =>
                `flex items-center gap-3 rounded-lg px-4 py-3 text-sm font-semibold transition ${
                  isActive
                    ? "bg-[#eef4ff] text-[#003b93]"
                    : "text-slate-500 hover:bg-slate-50 hover:text-slate-900"
                }`
              }
            >
              <item.icon className="h-4 w-4" />
              <span>{item.label}</span>
            </NavLink>
          ))}
        </div>

        <div className="mt-8">
          <p className="px-4 text-[10px] font-bold uppercase tracking-[0.22em] text-slate-400">Truy cập nhanh</p>
          <div className="mt-3 space-y-1">
            {quickAccessItems.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                end={item.end}
                className={({ isActive }) =>
                  `flex items-center gap-3 rounded-lg px-4 py-3 text-sm font-semibold transition ${
                    isActive
                      ? "bg-slate-100 text-slate-900"
                      : "text-slate-500 hover:bg-slate-50 hover:text-slate-900"
                  }`
                }
              >
                <item.icon className="h-4 w-4" />
                <span>{item.label}</span>
              </NavLink>
            ))}
          </div>
        </div>
      </nav>

      <div className="border-t border-slate-200 p-4">
        <div className="rounded-lg border border-slate-200 bg-slate-50 p-4">
          <p className="truncate text-sm font-bold text-slate-900">{fullName}</p>
          <p className="mt-1 text-[10px] font-bold uppercase tracking-[0.2em] text-slate-400">Quản lý</p>
        </div>
        <button
          type="button"
          onClick={handleLogout}
          className="mt-3 flex w-full items-center justify-center gap-2 rounded-lg border border-slate-200 px-4 py-2.5 text-sm font-semibold text-slate-600 transition hover:bg-slate-50 hover:text-slate-900"
        >
          <LogOut className="h-4 w-4" />
          <span>Đăng xuất</span>
        </button>
      </div>
    </aside>
  );
};

export default ManagerSidebar;
