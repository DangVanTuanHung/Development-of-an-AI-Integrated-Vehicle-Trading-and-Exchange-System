import { useAuth } from "@ebike/shared-code/hooks";
import { Navigate, NavLink, Outlet, useLocation } from "react-router-dom";

const customerNavItems = [
  { to: "/customer", label: "Tổng quan", end: true },
  { to: "/customer/orders", label: "Đơn hàng" },
  { to: "/customer/payments", label: "Thanh toán" },
  { to: "/customer/notifications", label: "Thông báo" },
  { to: "/customer/profile", label: "Hồ sơ" }
];

const CustomerLayout = () => {
  const { isAuthenticated, isBootstrapping } = useAuth();
  const location = useLocation();

  if (isBootstrapping) {
    return <div className="grid min-h-[60vh] place-items-center pt-28 text-sm font-semibold text-slate-500">Đang kiểm tra phiên đăng nhập...</div>;
  }

  if (!isAuthenticated) {
    return <Navigate to="/auth" replace state={{ from: location.pathname }} />;
  }

  return (
  <section className="customer-hub mx-auto flex w-full max-w-[1480px] flex-col gap-7 px-4 pb-16 pt-32 sm:px-6 lg:px-12">
    <div className="customer-hub__hero flex flex-col gap-3 overflow-hidden rounded-[32px] px-7 py-9 text-white sm:px-10">
      <span className="inline-block text-[0.72rem] font-bold uppercase tracking-[0.18em] text-cyan-300">MOTIONX Member</span>
      <h1 className="font-headline text-[clamp(2rem,3vw,3rem)] font-extrabold leading-tight tracking-[-.04em]">
        Trung tâm tài khoản MOTIONX.
      </h1>
      <p className="m-0 max-w-2xl leading-[1.7] text-white/60">
        Theo dõi đơn hàng, quản lý thanh toán, thông báo và thông tin cá nhân trong một không gian gọn gàng.
      </p>
    </div>

    <div className="customer-hub__nav flex flex-wrap gap-2 rounded-2xl border border-white/70 bg-white/80 p-2 shadow-sm backdrop-blur-xl">
      {customerNavItems.map((item) => (
        <NavLink
          key={item.to}
          to={item.to}
          end={item.end}
          className={({ isActive }) =>
            `rounded-xl px-4 py-2.5 text-sm font-semibold transition ${isActive ? "bg-gradient-to-r from-primary to-fuchsia-500 text-white shadow-lg shadow-primary/15" : "text-slate-500 hover:bg-violet-50 hover:text-primary"}`
          }
        >
          {item.label}
        </NavLink>
      ))}
    </div>

    <Outlet />
  </section>
  );
};

export default CustomerLayout;
