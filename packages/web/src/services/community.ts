import {apiClient} from "@ebike/shared-code/api";
import type {MarketplaceListing} from "./marketplace";
const base="/marketplace/community";
export type Comment={publicId:string;parentId?:string;authorId:number;username:string;content:string;status:string;replyCount:number;createdAt:string};
export type Review={publicId:string;reviewerId:number;targetId:number;reviewerName:string;targetName?:string;rating:number;comment?:string;reply?:string;status?:string};
export type Report={id:number;targetType:string;targetId:string;targetUserId:number;reporter:string;reason:string;description:string;snapshot:string;status:string;resolution?:string;createdAt:string};
export type Filters=Record<string,string>;
export const communityAPI={
 comments:async(id:string,page=0)=>(await apiClient.get<{items:Comment[];hasMore:boolean}>(base+`/comments/${id}`,{params:{page}})).data,
 add:(id:string,content:string,parentId?:string)=>apiClient.post(base+`/comments/${id}`,{content,parentId}),
 comment:(id:string,action:string,content?:string)=>apiClient.post(base+`/comment/${id}/${action}`,{content}),
 report:(type:string,target:string,reason:string,description:string)=>apiClient.post(base+"/reports",{type,target,reason,description}),
 reports:async(staff=false)=>(await apiClient.get<Report[]>(base+"/reports",{params:{staff}})).data,
 resolve:(id:number,action:string,note:string)=>apiClient.post(base+`/reports/${id}/resolve`,{action,note}),
 blocks:async()=>(await apiClient.get<Array<{id:number;username:string}>>(base+"/blocks")).data,
 block:(id:number,enabled=true)=>apiClient.post(base+`/blocks/${id}`,null,{params:{enabled}}),
 reply:(id:string,content:string)=>apiClient.post(base+`/reviews/${id}/reply`,{content}),
 myReviews:async()=>(await apiClient.get<Review[]>(base+"/reviews-mine")).data,
 myComments:async()=>(await apiClient.get<Array<Comment&{listingPublicId:string;title:string}>>(base+"/comments-mine")).data,
};
const discover="/marketplace/discovery";
export const discoveryAPI={
 search:async(filters:Filters,page=0)=>(await apiClient.get<{items:MarketplaceListing[];total:number;hasMore:boolean}>(discover+"/search",{params:{...filters,page,size:12}})).data,
 saved:async()=>(await apiClient.get<Array<{id:number;name:string;filters:Filters}>>(discover+"/saved")).data,
 save:(name:string,filters:Filters)=>apiClient.post(discover+"/saved",{name,filters}),
 remove:(id:number)=>apiClient.delete(discover+`/saved/${id}`),
 recent:async()=>(await apiClient.get<MarketplaceListing[]>(discover+"/recent")).data,
 view:(id:string)=>apiClient.post(discover+`/recent/${id}`),
 compare:async(ids:string[])=>(await apiClient.get<MarketplaceListing[]>(discover+"/compare",{params:{ids:ids.join(",")}})).data,
 comparison:async()=>(await apiClient.get<string[]>(discover+"/comparison")).data,
 saveComparison:(ids:string[])=>apiClient.post(discover+"/comparison",{ids}),
};
export type TicketMessage={id:number;authorId:number;username:string;content:string;staff:boolean;createdAt:string};
export const threadAPI={list:async(id:number)=>(await apiClient.get<TicketMessage[]>(`/support/tickets/${id}/messages`)).data,reply:(id:number,content:string)=>apiClient.post(`/support/tickets/${id}/messages`,{content})};
export const errorMessage=(e:unknown)=>e instanceof Error?e.message:"Không thể xử lý yêu cầu.";
export const comparisonKey="motionx-marketplace-comparison";
export const readComparison=():string[]=>{try{const ids=JSON.parse(localStorage.getItem(comparisonKey)||"[]");return Array.isArray(ids)?ids.filter((x):x is string=>typeof x==="string").slice(0,3):[];}catch{return [];}};
