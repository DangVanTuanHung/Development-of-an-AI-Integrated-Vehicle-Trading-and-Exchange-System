import { apiClient } from "@ebike/shared-code/api";
export type Offer = {publicId:string;listingPublicId:string;listingTitle:string;buyerId:number;sellerId:number;buyerName:string;sellerName:string;proposedBy:number;amount:number;depositAmount:number;exchangeValue:number;exchangeListingPublicId?:string;exchangeListingTitle?:string;message?:string;status:string;expiresAt:string;createdAt:string;parentOfferId?:number};
export type OfferInput = {amount:number;depositAmount:number;exchangeValue:number;exchangeListingId?:string;message?:string;policyAccepted:boolean};
export type TradeSummary = {publicId:string;transactionNumber:string;listingTitle:string;buyerId:number;sellerId:number;buyerName:string;sellerName:string;agreedPrice:number;depositAmount:number;exchangeValue:number;payableAmount:number;status:string;createdAt:string};
export type Payment = {publicId:string;paymentReference:string;stage:string;provider:string;amount:number;status:string;transferReference?:string;expiresAt:string;submittedAt?:string;paidAt?:string};
export type Refund = {publicId:string;paymentReference:string;amount:number;reason:string;status:string;transferReference?:string};
export type Trade = TradeSummary & {listingPublicId:string;exchangeVehicle?:{publicId:string;title:string};payerId:number;payeeId:number;canReviewDispute:boolean;cancellationRequestedBy?:number;cancellationReason?:string;refundPolicy:string;payments:Payment[];refunds:Refund[];handover:{scheduledAt?:string;location?:string;note?:string;revision:number;buyerAgreedAt?:string;sellerAgreedAt?:string;buyerConfirmedAt?:string;sellerConfirmedAt?:string};evidence:Array<{publicId:string;fileName:string;mimeType:string;size:number;uploadedBy:number}>;disputes:Array<{publicId:string;openedBy:number;description:string;status:string;resolution?:string;createdAt:string}>;history:Array<{type:string;fromStatus:string;toStatus:string;note:string;actor:string;createdAt:string}>};
const post=async<T,>(path:string,body?:unknown)=>(await apiClient.post<T>(path,body)).data;
const tx=(id:string)=>`/marketplace/transactions/${id}`;
export const tradeAPI={
  offers:async()=>(await apiClient.get<Offer[]>("/marketplace/offers/mine")).data,
  createOffer:(id:string,body:OfferInput)=>post<Offer>(`/marketplace/listings/${id}/offers`,body),
  counter:(id:string,body:OfferInput)=>post<Offer>(`/marketplace/offers/${id}/counter`,body),
  accept:(id:string)=>post<Trade>(`/marketplace/offers/${id}/accept`,{policyAccepted:true}),
  reject:(id:string)=>post<Offer>(`/marketplace/offers/${id}/reject`),
  withdraw:(id:string)=>post<Offer>(`/marketplace/offers/${id}/withdraw`),
  mine:async()=>(await apiClient.get<TradeSummary[]>("/marketplace/transactions/mine")).data,
  all:async()=>(await apiClient.get<TradeSummary[]>("/manager/marketplace/trades")).data,
  detail:async(id:string)=>(await apiClient.get<Trade>(tx(id))).data,
  payment:(id:string,stage:"DEPOSIT"|"FINAL",provider:"BANK_TRANSFER"|"CASH_ON_DELIVERY")=>post<Trade>(tx(id)+"/payments",{stage,provider}),
  paymentAction:(id:string,action:"sent"|"confirm"|"cancel",reference?:string)=>post<Trade>(`/marketplace/payments/${id}/${action}`,reference?{reference}:undefined),
  cancel:(id:string,action:"request"|"approve"|"reject"|"withdraw",note?:string)=>post<Trade>(tx(id)+`/cancellation/${action}`,note?{note}:undefined),
  refund:(id:string,refund:string,action:"sent"|"received",reference?:string)=>post<Trade>(tx(id)+`/refunds/${refund}/${action}`,reference?{reference}:undefined),
  dispute:(id:string,note:string)=>post<Trade>(tx(id)+"/disputes",{note}),
  resolve:(id:string,decision:"RESUME"|"CANCEL",note:string)=>post<Trade>(`/manager/marketplace/trades/${id}/resolve`,{decision,note}),
  plan:(id:string,body:{scheduledAt:string;location:string;note:string;revision:number})=>post<Trade>(tx(id)+"/handover/plan",body),
  handover:(id:string,action:"agree"|"confirm",revision:number)=>post<Trade>(tx(id)+`/handover/${action}`,{revision}),
  evidence:async(id:string,file:File)=>{const data=new FormData();data.append("file",file);return post(tx(id)+"/evidence",data);},
  download:async(id:string,evidence:string,name:string)=>{const response=await apiClient.get(tx(id)+`/evidence/${evidence}`,{responseType:"blob"});const url=URL.createObjectURL(response.data);const link=document.createElement("a");link.href=url;link.download=name;link.click();window.setTimeout(()=>URL.revokeObjectURL(url),10000);},
};
export const tradeLabels:Record<string,string>={PENDING:"Chờ phản hồi",COUNTERED:"Đã có đề nghị thay thế",ACCEPTED:"Đã chấp nhận",REJECTED:"Đã từ chối",WITHDRAWN:"Đã rút",EXPIRED:"Hết hạn",DEPOSIT_PENDING:"Chờ đặt cọc",DEPOSIT_PAID:"Đã nhận cọc",INSPECTION_PENDING:"Chờ xem xe / giao tiền mặt",FINAL_PAYMENT_PENDING:"Chờ thanh toán còn lại",FULLY_PAID:"Đã thanh toán đủ",COMPLETED:"Đã hoàn tất",CANCELLATION_REQUESTED:"Đang yêu cầu hủy",CANCELLED:"Đã hủy",DISPUTED:"Đang tranh chấp",REFUND_PENDING:"Đang hoàn tiền",REFUNDED:"Đã hoàn tiền",PROCESSING:"Đã báo chuyển, chờ bên nhận xác nhận",PAID:"Đã nhận tiền",APPROVED:"Được duyệt hoàn",FAILED:"Thất bại"};
export const tradeError=(e:unknown)=>{const error=e as {response?:{data?:{message?:string;detail?:string}};message?:string};return error.response?.data?.message||error.response?.data?.detail||error.message||"Không thể xử lý. Vui lòng tải lại và thử lại.";};
export const refundPolicy="Khi hai bên đồng ý hủy (hoặc có kết luận hủy tranh chấp), bên đã nhận tiền phải hoàn toàn bộ số tiền đã xác nhận. Chỉ ghi nhận đã hoàn khi bên trả tiền xác nhận nhận lại. MOTIONX ghi nhận chuyển khoản/tiền mặt, không tự giữ hay chuyển tiền ngân hàng.";
