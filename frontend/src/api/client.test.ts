import{afterEach,describe,expect,it,vi}from'vitest';
import{api,ApiError,currentUser,login,probeCurrentUser,SESSION_EXPIRED_EVENT}from'./client';

function responseAt(url:string,init:ResponseInit={status:200},redirected=true){
  const response=new Response('',init);
  Object.defineProperties(response,{url:{value:new URL(url,window.location.origin).href},redirected:{value:redirected}});
  return response;
}

afterEach(()=>{
  vi.restoreAllMocks();
  document.cookie='XSRF-TOKEN=; Max-Age=0; path=/';
});

describe('browser authentication client',()=>{
  it('treats a fresh unauthenticated session probe as signed out, not expired',async()=>{
    const expired=vi.fn();
    window.addEventListener(SESSION_EXPIRED_EVENT,expired);
    vi.spyOn(globalThis,'fetch').mockResolvedValue(responseAt('/login'));
    await expect(probeCurrentUser()).resolves.toBeNull();
    expect(expired).not.toHaveBeenCalled();
    window.removeEventListener(SESSION_EXPIRED_EVENT,expired);
  });

  it('refreshes a stale CSRF cookie before a successful login',async()=>{
    document.cookie='XSRF-TOKEN=stale; path=/';
    const fetchMock=vi.spyOn(globalThis,'fetch').mockImplementation(async(input,init)=>{
      const path=String(input);
      if(path==='/login'&&!init?.method){document.cookie='XSRF-TOKEN=fresh; path=/';return new Response('',{status:200})}
      if(path==='/login'&&init?.method==='POST'){
        expect(new Headers(init.headers).get('X-XSRF-TOKEN')).toBe('fresh');
        return responseAt('/');
      }
      if(path==='/api/auth/me')return Response.json({username:'admin',authorities:[{authority:'ROLE_ADMIN'}]});
      throw new Error(`Unexpected request: ${path}`);
    });
    await expect(login('admin','correct-password')).resolves.toEqual({username:'admin',role:'ADMIN'});
    expect(fetchMock).toHaveBeenCalledTimes(3);
  });

  it('reports invalid credentials without calling the session endpoint',async()=>{
    vi.spyOn(globalThis,'fetch').mockImplementation(async(input,init)=>{
      if(!init?.method){document.cookie='XSRF-TOKEN=fresh; path=/';return new Response('',{status:200})}
      return responseAt('/login?error');
    });
    await expect(login('admin','wrong-password')).rejects.toMatchObject({status:401,message:'Invalid username or password.'});
  });

  it('announces a genuinely expired authenticated session',async()=>{
    const expired=vi.fn();
    window.addEventListener(SESSION_EXPIRED_EVENT,expired);
    vi.spyOn(globalThis,'fetch').mockResolvedValue(responseAt('/login'));
    await expect(currentUser()).rejects.toBeInstanceOf(ApiError);
    expect(expired).toHaveBeenCalledOnce();
    window.removeEventListener(SESSION_EXPIRED_EVENT,expired);
  });

  it('sends the CSRF cookie on protected API writes',async()=>{
    document.cookie='XSRF-TOKEN=write-token; path=/';
    vi.spyOn(globalThis,'fetch').mockImplementation(async(_input,init)=>{
      expect(new Headers(init?.headers).get('X-XSRF-TOKEN')).toBe('write-token');
      return new Response(null,{status:204});
    });
    await expect(api('/api/pos/example',{method:'POST'})).resolves.toBeUndefined();
  });
});
