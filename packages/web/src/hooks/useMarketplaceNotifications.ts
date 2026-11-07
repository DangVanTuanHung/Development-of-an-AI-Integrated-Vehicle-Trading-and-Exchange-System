import {useEffect,useRef} from "react";
import {API_BASE_URL} from "@ebike/shared-code/config";
export function useMarketplaceNotifications(enabled:boolean,onChange:()=>void){
 const callback=useRef(onChange);callback.current=onChange;
 useEffect(()=>{if(!enabled)return;const stream=new EventSource(API_BASE_URL+"/notifications/stream",{withCredentials:true});stream.addEventListener("unread-count",()=>callback.current());return()=>stream.close();},[enabled]);
}
