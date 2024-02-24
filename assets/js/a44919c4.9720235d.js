"use strict";(self.webpackChunkwebsite=self.webpackChunkwebsite||[]).push([[860],{6471:(e,t,n)=>{n.r(t),n.d(t,{assets:()=>a,contentTitle:()=>p,default:()=>u,frontMatter:()=>i,metadata:()=>r,toc:()=>c});var s=n(7624),o=n(2172);const i={sidebar_position:125},p="@RequestMapping Support",r={id:"extensions/RequestMapping-annotation-support",title:"@RequestMapping Support",description:"Support to use spring web annotations to generate HTTP clients, e.g., @RequestMapping, @GetMapping, @PostMapping etc.",source:"@site/docs/extensions/RequestMapping-annotation-support.mdx",sourceDirName:"extensions",slug:"/extensions/RequestMapping-annotation-support",permalink:"/httpexchange-spring-boot-starter/docs/extensions/RequestMapping-annotation-support",draft:!1,unlisted:!1,editUrl:"https://github.com/DanielLiu1123/httpexchange-spring-boot-starter/tree/main/docs/docs/extensions/RequestMapping-annotation-support.mdx",tags:[],version:"current",sidebarPosition:125,frontMatter:{sidebar_position:125},sidebar:"tutorialSidebar",previous:{title:"Url Variables",permalink:"/httpexchange-spring-boot-starter/docs/extensions/url-variables"},next:{title:"LoadBalancer",permalink:"/httpexchange-spring-boot-starter/docs/extensions/loadbalancer"}},a={},c=[];function d(e){const t={admonition:"admonition",code:"code",h1:"h1",p:"p",pre:"pre",...(0,o.M)(),...e.components};return(0,s.jsxs)(s.Fragment,{children:[(0,s.jsxs)(t.h1,{id:"requestmapping-support",children:[(0,s.jsx)(t.code,{children:"@RequestMapping"})," Support"]}),"\n",(0,s.jsxs)(t.p,{children:["Support to use spring web annotations to generate HTTP clients, e.g., ",(0,s.jsx)(t.code,{children:"@RequestMapping"}),", ",(0,s.jsx)(t.code,{children:"@GetMapping"}),", ",(0,s.jsx)(t.code,{children:"@PostMapping"})," etc."]}),"\n",(0,s.jsxs)(t.p,{children:["Supports all features of ",(0,s.jsx)(t.code,{children:"@HttpExchange"}),"."]}),"\n",(0,s.jsx)(t.pre,{children:(0,s.jsx)(t.code,{className:"language-java",children:'@RequestMapping("/typicode/demo")\npublic interface PostApi {\n    @GetMapping("/posts/{id}")\n    Post getPost(@PathVariable("id") int id);\n}\n'})}),"\n",(0,s.jsxs)(t.admonition,{type:"info",children:[(0,s.jsxs)(t.p,{children:["Since 3.2.0, ",(0,s.jsx)(t.code,{children:"@RequestMapping"})," support is disabled by default, you can set ",(0,s.jsx)(t.code,{children:"http-exchange.request-mapping-support-enabled=true"})," to enable it."]}),(0,s.jsxs)(t.p,{children:["Consider using ",(0,s.jsx)(t.code,{children:"@HttpExchange"})," instead of ",(0,s.jsx)(t.code,{children:"@RequestMapping"})," if possible."]})]})]})}function u(e={}){const{wrapper:t}={...(0,o.M)(),...e.components};return t?(0,s.jsx)(t,{...e,children:(0,s.jsx)(d,{...e})}):d(e)}},2172:(e,t,n)=>{n.d(t,{I:()=>r,M:()=>p});var s=n(1504);const o={},i=s.createContext(o);function p(e){const t=s.useContext(i);return s.useMemo((function(){return"function"==typeof e?e(t):{...t,...e}}),[t,e])}function r(e){let t;return t=e.disableParentContext?"function"==typeof e.components?e.components(o):e.components||o:p(e.components),s.createElement(i.Provider,{value:t},e.children)}}}]);