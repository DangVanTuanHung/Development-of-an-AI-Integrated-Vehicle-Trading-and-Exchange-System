import { Menu, User, X, ArrowUpRight } from "lucide-react";
import { useEffect, useState } from "react";
import { NavLink, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "@ebike/shared-code/hooks";
import NotificationBell from "./NotificationBell";
import { marketplaceAPI } from "../../services/marketplace";

const navItems = [
  { to: "/products", label: "Khám phá" },
  { to: "/sell", label: "Đăng tin" },
  { to: "/deals", label: "Giao dịch" },
  { to: "/support", label: "Hỗ trợ" },
  { to: "/favorites", label: "Yêu thích" }
];

const adminRoles = new Set(["ADMIN"]);

const Header = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { isAuthenticated, user } = useAuth();
  const [mobileOpen, setMobileOpen] = useState(false);
  const [unreadMessages, setUnreadMessages] = useState(0);
  const [navCounts, setNavCounts] = useState({ favorites: 0, listings: 0, deals: 0 });
  const isManager = user?.roles.includes("MANAGER");
  const canAccessAdmin = user?.roles.some((role) => adminRoles.has(role));
  const isNotificationsPage = location.pathname === "/customer/notifications";
  const visibleNavItems = [
    ...navItems.filter((item) => isAuthenticated || item.to !== "/favorites"),
    ...(isAuthenticated ? [{ to: "/messages", label: "Tin nhắn", end: false }] : []),
    ...(isAuthenticated ? [{ to: "/appointments", label: "Lịch xem xe", end: false }] : []),
    ...(canAccessAdmin ? [{ to: "/admin", label: "Admin", end: false }] : []),
    ...(isAuthenticated ? [{ to: "/my-listings", label: "Tin của tôi", end: false }] : []),
    ...(isManager ? [{ to: "/manager", label: "Manager", end: false }] : [])
  ];

  useEffect(() => {
    setMobileOpen(false);
  }, [location.pathname]);

  useEffect(() => {
    if (!isAuthenticated) {
      setUnreadMessages(0);
      setNavCounts({ favorites: 0, listings: 0, deals: 0 });
      return;
    }
    const loadNavCounts = () => {
      void Promise.all([
        marketplaceAPI.conversations(),
        marketplaceAPI.favoriteListings(),
        marketplaceAPI.ownListings(),
        marketplaceAPI.myOffers(),
        marketplaceAPI.myTransactions()
      ]).then(([conversations, favorites, listings, offers, transactions]) => {
        setUnreadMessages(conversations.reduce((total, item) => total + Number(item.unreadCount || 0), 0));
        const listingAttentionStatuses = new Set(["DRAFT", "PENDING_REVIEW", "REJECTED"]);
        const actionableOffers = offers.filter((offer) =>
          ["PENDING", "COUNTERED"].includes(String(offer.status)) && Number(offer.sellerId) === Number(user?.id)
        ).length;
        const activeTransactions = transactions.filter((transaction) =>
          !["COMPLETED", "CANCELLED", "REFUNDED"].includes(String(transaction.status))
        ).length;
        setNavCounts({
          favorites: favorites.length,
          listings: listings.filter((listing) => listingAttentionStatuses.has(listing.status)).length,
          deals: actionableOffers + activeTransactions
        });
      }).catch(() => {
        setUnreadMessages(0);
        setNavCounts({ favorites: 0, listings: 0, deals: 0 });
      });
    };
    loadNavCounts();
    const intervalId = window.setInterval(loadNavCounts, 30000);
    window.addEventListener("focus", loadNavCounts);
    return () => {
      window.clearInterval(intervalId);
      window.removeEventListener("focus", loadNavCounts);
    };
  }, [isAuthenticated, location.pathname, user?.id]);

  const countForNavItem = (to: string) => {
    if (to === "/messages") return unreadMessages;
    if (to === "/favorites") return navCounts.favorites;
    if (to === "/sell") return navCounts.listings;
    if (to === "/deals") return navCounts.deals;
    return 0;
  };

  return (
    <header className="site-header fixed left-0 right-0 top-0 z-50 px-3 pt-3 sm:px-5">
      <nav className="site-header__nav mx-auto flex max-w-[1480px] items-center justify-between px-4 py-3 sm:px-6">
        <div className="flex items-center gap-8 xl:gap-12">
          <NavLink to="/" className="brand-mark font-headline text-xl font-bold uppercase tracking-[-0.04em] text-foreground" aria-label="MOTIONX - Trang chủ">
            <span className="brand-mark__symbol">M</span>
            <span>MOTIONX</span>
          </NavLink>
          <div className="hidden items-center gap-1 md:flex">
            <NavLink
              to="/"
              end
              className={({ isActive }) =>
                `site-nav-link ${
                  isActive ? "site-nav-link--active" : ""
                }`
              }
            >
              Trang chủ
            </NavLink>
            {visibleNavItems.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                end={"end" in item ? item.end : undefined}
                className={({ isActive }) =>
                  `site-nav-link ${
                    isActive ? "site-nav-link--active" : ""
                  }`
                }
              >
                <span className="relative inline-flex items-center gap-1.5">
                  {item.label}
                  {countForNavItem(item.to) > 0 ? (
                    <span className="inline-flex min-w-[18px] items-center justify-center rounded-full bg-[#ef3f7f] px-1 text-[10px] font-bold leading-[18px] text-white">
                      {countForNavItem(item.to) > 99 ? "99+" : countForNavItem(item.to)}
                    </span>
                  ) : null}
                </span>
              </NavLink>
            ))}
          </div>
        </div>

        <div className="flex items-center gap-4">
          <div className="hidden items-center gap-4 lg:flex">
            {isNotificationsPage ? null : <NotificationBell enabled={isAuthenticated} />}
            <button
              onClick={() => navigate(isAuthenticated ? "/customer/profile" : "/auth")}
              className="header-icon-button"
              aria-label="Tài khoản"
            >
              <User size={18} />
            </button>
          </div>
          <NavLink to={isAuthenticated ? "/store" : "/auth"} className="header-cta hidden text-sm sm:inline-flex">
            {isAuthenticated ? "Mua Ngay " : "Đăng nhập"}
            <ArrowUpRight size={16} />
          </NavLink>
          <button
            onClick={() => setMobileOpen((value) => !value)}
            className="header-icon-button md:hidden"
            aria-label="Mở menu điều hướng"
          >
            {mobileOpen ? <X size={22} /> : <Menu size={22} />}
          </button>
        </div>
      </nav>

      {mobileOpen && (
        <div className="site-mobile-menu mx-auto mt-2 max-w-[1480px] px-4 py-4 md:hidden">
          <div className="flex flex-col gap-1">
            <NavLink
              to="/"
              end
              className="site-mobile-link"
            >
              Trang chủ
            </NavLink>
            {visibleNavItems.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                end={"end" in item ? item.end : undefined}
                className="site-mobile-link"
              >
                <span className="flex items-center justify-between gap-3">
                  {item.label}
                  {countForNavItem(item.to) > 0 ? (
                    <span className="inline-flex min-w-[20px] items-center justify-center rounded-full bg-[#ef3f7f] px-1.5 text-[11px] font-bold leading-5 text-white">
                      {countForNavItem(item.to) > 99 ? "99+" : countForNavItem(item.to)}
                    </span>
                  ) : null}
                </span>
              </NavLink>
            ))}
            <NavLink to={isAuthenticated ? "/store" : "/auth"} className="btn-primary mt-3 w-full">
              {isAuthenticated ? "Mua Ngay " : "Đăng nhập"}
            </NavLink>
          </div>
        </div>
      )}
    </header>
  );
};

export default Header;
