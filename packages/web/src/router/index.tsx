import AdminVehicleCatalogPage from "../pages/admin/AdminVehicleCatalogPage";
import MarketplaceOperationsPage from "../pages/manager/MarketplaceOperationsPage";
import { createBrowserRouter, Navigate } from "react-router-dom";

// Layouts
import AdminLayout from "../layouts/AdminLayout";
import CustomerLayout from "../layouts/CustomerLayout";
import ManagerLayout from "../layouts/ManagerLayout";
import Layout from "../components/common/Layout";

// Pages
import AdminDashboardPage from "../pages/admin/AdminDashboardPremiumPage";
import AdminAccountsAccessPage from "../pages/admin/AdminAccountsAccessPage";
import AdminPricingPromotionsPage from "../pages/admin/AdminPricingPromotionsPage";
import ChatbotPage from "../pages/ChatbotPage";
import CheckoutPage from "../pages/CheckoutPage";
import FavoritesPage from "../pages/UnifiedFavoritesPage";
import LoginPage from "../pages/LoginPageApi";
import NotFoundPage from "../pages/NotFoundPage";
import OrderSuccessPage from "../pages/OrderSuccessPage";
import PaymentReturnPage from "../pages/PaymentReturnPage";
import ProductDetailPage from "../pages/ProductDetailPage";
import ProductsPage from "../pages/ProductsPage";
import CompareProductsPage from "../pages/CompareProductsPage";
import SignupPage from "../pages/SignupPage";
import SupportPage from "../pages/SupportPagePremium";
import SupportConsolePage from "../pages/SupportConsolePage";
import InformationPage from "../pages/InformationPage";
import MarketplaceBrowsePage from "../pages/MarketplaceBrowsePage";
import MarketplaceListingDetailPage from "../pages/MarketplaceListingDetailPage";
import PostListingPage from "../pages/GuidedPostListingPage";
import MarketplaceDealsPage from "../pages/MarketplaceDealsPage";
import MarketplaceMessagesPage from "../pages/MarketplaceMessagesPage";
import CustomerDashboardPage from "../pages/customer/CustomerDashboardRealPage";
import CustomerOrdersPage from "../pages/customer/CustomerOrdersSafePage";
import CustomerOrderDetailPage from "../pages/customer/CustomerOrderDetailPage";
import CustomerNotificationsPage from "../pages/customer/CustomerNotificationsPage";
import CustomerPaymentHistoryPage from "../pages/customer/CustomerPaymentHistoryPage";
import CustomerProfilePage from "../pages/customer/CustomerProfilePage";
import ManagerProductsPage from "../pages/manager/ManagerProductsPage";

const router = createBrowserRouter([
  {
    path: "/",
    element: <Layout />,
    errorElement: <NotFoundPage />,
    children: [
      // Guest
      { index: true, element: <MarketplaceBrowsePage /> },
      { path: "auth", element: <LoginPage /> },
      { path: "signup", element: <SignupPage /> },
      { path: "products", element: <MarketplaceBrowsePage /> },
      { path: "models", element: <MarketplaceBrowsePage /> },
      { path: "store", element: <ProductsPage /> },
      { path: "listing/:id", element: <MarketplaceListingDetailPage /> },
      { path: "sell", element: <PostListingPage /> },
      { path: "sell/:id/edit", element: <PostListingPage /> },
      { path: "my-listings", element: <div className="mx-auto max-w-7xl px-5 pb-20 pt-32"><ManagerProductsPage /></div> },
      { path: "deals", element: <MarketplaceDealsPage /> },
      { path: "messages", element: <MarketplaceMessagesPage /> },
      { path: "product/:id", element: <ProductDetailPage /> },
      { path: "models/:id", element: <ProductDetailPage /> },
      { path: "favorites", element: <FavoritesPage /> },
      { path: "checkout", element: <CheckoutPage /> },
      { path: "checkout/success", element: <OrderSuccessPage /> },
      { path: "payment/return", element: <PaymentReturnPage /> },
      { path: "support", element: <SupportPage /> },
      { path: "info/:slug", element: <InformationPage /> },
      { path: "compare", element: <CompareProductsPage /> },
      { path: "chatbot", element: <ChatbotPage /> }
    ]
  },
  {
    path: "/customer",
    element: <Layout />,
    errorElement: <NotFoundPage />,
    children: [
      {
        path: "",
        element: <CustomerLayout />,
        children: [
          { index: true, element: <CustomerDashboardPage /> },
          { path: "orders", element: <CustomerOrdersPage /> },
          { path: "orders/:id", element: <CustomerOrderDetailPage /> },
          { path: "payments", element: <CustomerPaymentHistoryPage /> },
          { path: "notifications", element: <CustomerNotificationsPage /> },
          { path: "profile", element: <CustomerProfilePage /> }
        ]
      }
    ]
  },
  {
    path: "/admin",
    errorElement: <NotFoundPage />,
    element: <AdminLayout />,
    children: [
      { path: "listings", element: <MarketplaceOperationsPage view="listings" /> },
      { path: "transactions", element: <MarketplaceOperationsPage view="transactions" /> },
      { path: "reports", element: <MarketplaceOperationsPage view="reports" /> },

      { index: true, element: <AdminDashboardPage /> },
      { path: "pricing", element: <AdminPricingPromotionsPage /> },
      { path: "catalog", element: <AdminVehicleCatalogPage /> },
      { path: "accounts", element: <AdminAccountsAccessPage /> },
      { path: "products", element: <Navigate to="/admin/listings" replace /> },
      
      { path: "users", element: <Navigate to="/admin/accounts" replace /> },
      { path: "support", element: <SupportConsolePage /> }
    ]
  },
  {
    path: "/manager",
    element: <ManagerLayout />,
    children: [
      { path: "listings", element: <MarketplaceOperationsPage view="listings" /> },
      { path: "transactions", element: <MarketplaceOperationsPage view="transactions" /> },

      { index: true, element: <MarketplaceOperationsPage /> },
      { path: "orders", element: <Navigate to="/manager/transactions" replace /> },
      { path: "orders/:id", element: <Navigate to="/manager/transactions" replace /> },
      { path: "payments", element: <Navigate to="/manager/transactions" replace /> },
      
      
      { path: "products", element: <ManagerProductsPage /> },
      { path: "products/new", element: <PostListingPage management /> },
      { path: "products/:id/edit", element: <PostListingPage management /> },
      { path: "reports", element: <MarketplaceOperationsPage view="reports" /> },
      { path: "support", element: <SupportConsolePage /> }
    ]
  },
  {
    path: "*",
    element: <NotFoundPage />
  }
]);

export default router;
