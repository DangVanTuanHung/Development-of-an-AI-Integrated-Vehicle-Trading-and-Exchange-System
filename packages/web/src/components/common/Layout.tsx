import type { PropsWithChildren } from "react";
import { Outlet, useLocation } from "react-router-dom";
import Header from "./Header";
import Footer from "./FooterPremium";
import FloatingChatButton from "./FloatingChatButton";
import ScrollToTop from "./ScrollToTop";

const Layout = ({ children }: PropsWithChildren) => {
  const location = useLocation();
  const isMessagesPage = location.pathname === "/messages";
  return (
    <div className="storefront-shell flex min-h-screen flex-col bg-background text-foreground">
      <ScrollToTop />
      <Header />
      <main className="flex-1">{children ?? <Outlet />}</main>
      {isMessagesPage ? null : <Footer />}
      <FloatingChatButton />
    </div>
  );
};

export default Layout;
