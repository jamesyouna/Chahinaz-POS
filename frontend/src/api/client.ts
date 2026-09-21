import type{User}from'../types';

export class ApiError extends Error{
  constructor(public status:number,message:string,public fields:string[]=[]){super(message)}
}

export const SESSION_EXPIRED_EVENT='chahinaz:session-expired';
const cookie=(name:string)=>decodeURIComponent(document.cookie.split('; ').find(x=>x.startsWith(name+'='))?.split('=').slice(1).join('=')??'');
const isLoginRedirect=(response:Response)=>response.redirected&&new URL(response.url,window.location.origin).pathname==='/login';

async function refreshCsrf(){
  let response:Response;
  try{response=await fetch('/login',{credentials:'include',cache:'no-store'})}
  catch{throw new ApiError(0,'Cannot reach the POS server. Check the network and try again.')}
  const token=cookie('XSRF-TOKEN');
  if(!response.ok||!token)throw new ApiError(response.status||0,'Could not start a secure sign-in. Refresh the page and try again.');
  return token;
}

async function ensureCsrf(){return cookie('XSRF-TOKEN')||refreshCsrf()}

async function request<T>(path:string,init:RequestInit={},notifySessionExpiry=true):Promise<T>{
  const method=(init.method??'GET').toUpperCase();
  if(!['GET','HEAD','OPTIONS'].includes(method))await ensureCsrf();
  const headers=new Headers(init.headers);
  if(init.body&&!(init.body instanceof FormData)&&!headers.has('Content-Type'))headers.set('Content-Type','application/json');
  const token=cookie('XSRF-TOKEN');
  if(token)headers.set('X-XSRF-TOKEN',token);
  let response:Response;
  try{response=await fetch(path,{...init,headers,credentials:'include'})}
  catch{throw new ApiError(0,'Cannot reach the POS server. Check the network and try again.')}
  const unauthenticated=isLoginRedirect(response)||(response.status===401&&path!=='/api/pos/approvals');
  if(unauthenticated){
    if(notifySessionExpiry)window.dispatchEvent(new Event(SESSION_EXPIRED_EVENT));
    throw new ApiError(401,'Your session has expired.');
  }
  if(!response.ok){
    let body:{message?:string;fields?:string[]}={};
    try{body=await response.json()}catch{}
    throw new ApiError(response.status,body.message??`Request failed (${response.status})`,body.fields);
  }
  if(response.status===204)return undefined as T;
  const type=response.headers.get('content-type')??'';
  return(type.includes('json')?response.json():response.blob())as Promise<T>;
}

export async function api<T>(path:string,init:RequestInit={}):Promise<T>{return request<T>(path,init,true)}

export async function login(username:string,password:string){
  const token=await refreshCsrf();
  const form=new URLSearchParams({username,password});
  let response:Response;
  try{response=await fetch('/login',{method:'POST',body:form,credentials:'include',headers:{'Content-Type':'application/x-www-form-urlencoded','X-XSRF-TOKEN':token}})}
  catch{throw new ApiError(0,'Cannot reach the POS server. Check the network and try again.')}
  if(response.url.includes('error')||response.status===401)throw new ApiError(401,'Invalid username or password.');
  if(response.status===403)throw new ApiError(403,'The secure sign-in token expired. Please try again.');
  if(!response.ok)throw new ApiError(response.status,`Sign-in failed (${response.status}).`);
  return currentUser();
}

async function loadCurrentUser(notifySessionExpiry:boolean):Promise<User>{
  const raw=await request<{username:string;authorities:Array<{authority?:string}|string>}>('/api/auth/me',{},notifySessionExpiry);
  const authority=raw.authorities.map(x=>typeof x==='string'?x:x.authority??'').find(x=>x.startsWith('ROLE_'));
  return{username:raw.username,role:(authority?.replace('ROLE_','')??'TELLER')as User['role']};
}

export async function currentUser():Promise<User>{return loadCurrentUser(true)}

export async function probeCurrentUser():Promise<User|null>{
  try{return await loadCurrentUser(false)}
  catch(error){if(error instanceof ApiError&&error.status===401)return null;throw error}
}

export async function logout(){
  const token=await ensureCsrf();
  try{await fetch('/logout',{method:'POST',credentials:'include',headers:{'X-XSRF-TOKEN':token}})}
  catch{throw new ApiError(0,'Cannot reach the POS server. Check the network and try again.')}
}

export const json=(method:string,body?:unknown):RequestInit=>({method,body:body===undefined?undefined:JSON.stringify(body)});
