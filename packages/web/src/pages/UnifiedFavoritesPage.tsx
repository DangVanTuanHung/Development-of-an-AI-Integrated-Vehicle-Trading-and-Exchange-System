import { ArrowRight, Car, Heart, MapPin, ShoppingBag, Trash2 } from "lucide-react";
import { type ReactNode, useEffect, useMemo, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useFavorites } from "@ebike/shared-code/hooks";
import { formatMarketplacePrice, marketplaceAPI, resolveMarketplaceMediaUrl, type MarketplaceListing } from "../services/marketplace";
import { attachImageFallback, resolveProductImage } from "../utils/media";

const UnifiedFavoritesPage = () => {
  const navigate = useNavigate();
  const { favorites: products, loading: productsLoading, error, isAuthenticated, removeFavorite } = useFavorites();
  const [listings, setListings] = useState<MarketplaceListing[]>([]);
  const [listingsLoading, setListingsLoading] = useState(true);

  useEffect(() => {
    if (!isAuthenticated) { setListingsLoading(false); return; }
    void marketplaceAPI.favoriteListings().then(setListings).finally(() => setListingsLoading(false));
  }, [isAuthenticated]);

  const storeTotal = useMemo(() => products.reduce((sum, item) => sum + (item.discountPrice ?? item.price), 0), [products]);
  const marketplaceTotal = useMemo(() => listings.reduce((sum, item) => sum + item.price, 0), [listings]);
  const totalItems = products.length + listings.length;

  const removeListing = async (listing: MarketplaceListing) => {
    await marketplaceAPI.toggleFavorite(listing.publicId);
    setListings((current) => current.filter((item) => item.publicId !== listing.publicId));
  };

  const checkoutFirstProduct = () => {
    const product = products[0];
    if (!product) return;
    navigate("/checkout", { state: { product: { id: product.id, name: product.name, slug: product.slug, price: product.discountPrice ?? product.price, image: product.images?.[0], categoryName: product.category?.name }, selectedColor: "Mặc định", quantity: 1 } });
  };

  if (!isAuthenticated) return <main className="grid min-h-screen place-items-center bg-[#fff9f6] px-6 pt-24"><div className="max-w-lg rounded-[30px] border border-violet-100 bg-white p-10 text-center shadow-xl"><Heart className="mx-auto text-violet-600" size={44} /><h1 className="mt-5 text-3xl font-extrabold">Đăng nhập để xem yêu thích</h1><p className="mt-3 text-slate-500">Danh sách yêu thích được đồng bộ an toàn theo tài khoản của bạn.</p><Link to="/auth" className="mt-7 inline-flex rounded-full bg-violet-600 px-7 py-3 font-bold text-white">Đăng nhập</Link></div></main>;

  return <main className="min-h-screen bg-[#fff9f6] px-5 pb-24 pt-32 sm:px-8">
    <div className="mx-auto max-w-[1400px]">
      <section className="overflow-hidden rounded-[34px] bg-gradient-to-r from-[#1b0a40] via-violet-700 to-fuchsia-600 px-8 py-10 text-white shadow-[0_24px_70px_rgba(85,38,145,.22)] sm:px-12"><div className="flex flex-col gap-6 lg:flex-row lg:items-end lg:justify-between"><div><p className="text-xs font-bold uppercase tracking-[.2em] text-cyan-200">Bộ sưu tập của bạn</p><h1 className="mt-3 text-4xl font-extrabold tracking-[-.04em] sm:text-5xl">Danh sách yêu thích</h1><p className="mt-3 text-white/65">Lưu lại để so sánh, liên hệ người bán hoặc đặt mua sau.</p></div><div className="flex gap-3"><div className="rounded-2xl bg-white/10 px-5 py-3 backdrop-blur"><b className="text-2xl">{totalItems}</b><p className="text-xs text-white/60">Tổng mục</p></div><div className="rounded-2xl bg-white/10 px-5 py-3 backdrop-blur"><b className="text-2xl">{listings.length}</b><p className="text-xs text-white/60">Tin Marketplace</p></div><div className="rounded-2xl bg-white/10 px-5 py-3 backdrop-blur"><b className="text-2xl">{products.length}</b><p className="text-xs text-white/60">Xe cửa hàng</p></div></div></div></section>

      <section className="mt-9 rounded-[30px] border border-violet-100 bg-white p-6 shadow-sm sm:p-8"><div className="mb-7 flex flex-wrap items-end justify-between gap-4"><div><p className="text-xs font-bold uppercase tracking-[.18em] text-violet-600">Marketplace</p><h2 className="mt-2 text-2xl font-extrabold">Xe mua bán yêu thích</h2></div><Link to="/products" className="flex items-center gap-2 text-sm font-bold text-violet-700">Khám phá thêm <ArrowRight size={16} /></Link></div>
        {listingsLoading ? <p className="py-12 text-center text-slate-500">Đang tải tin yêu thích...</p> : listings.length === 0 ? <EmptyState icon={<Car />} text="Bạn chưa yêu thích tin Marketplace nào." link="/products" linkText="Khám phá xe đang bán" /> : <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">{listings.map((listing) => <article key={listing.publicId} className="group overflow-hidden rounded-[24px] border border-violet-100 bg-white shadow-sm transition hover:-translate-y-1 hover:shadow-xl"><Link to={`/listing/${listing.publicId}`} className="block h-48 overflow-hidden bg-violet-50">{listing.imageUrl ? <img src={resolveMarketplaceMediaUrl(listing.imageUrl)} alt={listing.title} className="h-full w-full object-cover transition duration-500 group-hover:scale-105" /> : <div className="grid h-full place-items-center"><Car className="text-violet-300" /></div>}</Link><div className="p-5"><p className="flex items-center gap-1 text-xs text-slate-500"><MapPin size={13} />{listing.province}</p><Link to={`/listing/${listing.publicId}`} className="mt-2 block line-clamp-2 min-h-12 font-extrabold hover:text-violet-700">{listing.title}</Link><p className="mt-3 text-lg font-extrabold text-violet-700">{formatMarketplacePrice(listing.price)}</p><div className="mt-4 flex items-center justify-between border-t border-violet-50 pt-4"><span className="text-xs text-slate-500">{listing.manufactureYear ? `Đời ${listing.manufactureYear}` : listing.condition.replace(/_/g, " ")}</span><button onClick={() => void removeListing(listing)} className="grid h-9 w-9 place-items-center rounded-full bg-red-50 text-red-500 hover:bg-red-100" aria-label="Bỏ khỏi yêu thích"><Trash2 size={16} /></button></div></div></article>)}</div>}
      </section>

      <section className="mt-9 grid gap-7 lg:grid-cols-[1fr_340px]"><div className="rounded-[30px] border border-violet-100 bg-white p-6 shadow-sm sm:p-8"><div className="mb-7"><p className="text-xs font-bold uppercase tracking-[.18em] text-fuchsia-600">Cửa hàng MOTIONX</p><h2 className="mt-2 text-2xl font-extrabold">Sản phẩm yêu thích</h2></div>{productsLoading ? <p className="py-12 text-center text-slate-500">Đang tải sản phẩm...</p> : error ? <p className="rounded-xl bg-red-50 p-4 text-red-600">{error}</p> : products.length === 0 ? <EmptyState icon={<ShoppingBag />} text="Bạn chưa yêu thích sản phẩm cửa hàng nào." link="/products" linkText="Tiếp tục khám phá" /> : <div className="space-y-4">{products.map((product) => <article key={product.id} className="flex gap-5 rounded-2xl border border-violet-50 p-4"><Link to={`/product/${product.slug}`} className="h-28 w-32 shrink-0 overflow-hidden rounded-xl bg-slate-50"><img src={resolveProductImage(product.images?.[0])} alt={product.name} className="h-full w-full object-cover" onError={(event) => attachImageFallback(event, product.name)} /></Link><div className="flex min-w-0 flex-1 flex-col justify-between"><div className="flex justify-between gap-3"><div><Link to={`/product/${product.slug}`} className="font-extrabold hover:text-violet-700">{product.name}</Link><p className="mt-1 text-xs text-slate-500">{product.category?.name || "Phương tiện"}</p></div><button onClick={() => void removeFavorite(product.id)} className="text-red-500" aria-label="Bỏ khỏi yêu thích"><Trash2 size={17} /></button></div><b className="text-violet-700">{formatMarketplacePrice(product.discountPrice ?? product.price)}</b></div></article>)}</div>}</div>
        <aside className="h-fit rounded-[30px] bg-[#171329] p-7 text-white shadow-xl lg:sticky lg:top-28"><h3 className="text-xl font-extrabold">Tổng quan yêu thích</h3><div className="mt-7 space-y-4 text-sm"><p className="flex justify-between"><span className="text-white/55">Marketplace</span><b>{listings.length} tin</b></p><p className="flex justify-between"><span className="text-white/55">Sản phẩm cửa hàng</span><b>{products.length} sản phẩm</b></p><p className="flex justify-between"><span className="text-white/55">Giá trị xe quan tâm</span><b>{formatMarketplacePrice(marketplaceTotal)}</b></p><div className="h-px bg-white/10" /><p className="flex justify-between"><span className="text-white/70">Tạm tính đặt hàng</span><b className="text-xl text-cyan-300">{formatMarketplacePrice(storeTotal)}</b></p></div>{products.length ? <button onClick={checkoutFirstProduct} className="mt-7 flex w-full items-center justify-center gap-2 rounded-2xl bg-gradient-to-r from-violet-500 to-fuchsia-500 px-5 py-4 font-bold">Đặt sản phẩm đầu tiên <ArrowRight size={18} /></button> : <Link to="/products" className="mt-7 flex w-full items-center justify-center rounded-2xl bg-white/10 px-5 py-4 text-center font-bold hover:bg-white/15">Khám phá thêm xe</Link>}<p className="mt-4 text-center text-xs leading-5 text-white/40">Xe Marketplace được giao dịch qua đề nghị và trao đổi với người bán, không đưa vào giỏ hàng cửa hàng.</p></aside>
      </section>
    </div>
  </main>;
};

const EmptyState = ({ icon, text, link, linkText }: { icon: ReactNode; text: string; link: string; linkText: string }) => <div className="grid min-h-52 place-items-center rounded-2xl border-2 border-dashed border-violet-100 text-center"><div><span className="mx-auto grid h-12 w-12 place-items-center rounded-full bg-violet-50 text-violet-500">{icon}</span><p className="mt-3 text-slate-500">{text}</p><Link to={link} className="mt-3 inline-block font-bold text-violet-700">{linkText}</Link></div></div>;

export default UnifiedFavoritesPage;
