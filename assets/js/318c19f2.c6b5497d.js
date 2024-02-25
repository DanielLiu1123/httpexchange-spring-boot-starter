"use strict";(self.webpackChunkwebsite=self.webpackChunkwebsite||[]).push([[272],{4232:(t,e,n)=>{n.r(e),n.d(e,{assets:()=>c,contentTitle:()=>s,default:()=>l,frontMatter:()=>r,metadata:()=>a,toc:()=>d});var o=n(7624),i=n(2172);const r={sidebar_position:40},s="Validation",a={id:"core/validation",title:"Validation",description:"Support work with spring-boot-starter-validation.",source:"@site/versioned_docs/version-3.2.x/10-core/40-validation.mdx",sourceDirName:"10-core",slug:"/core/validation",permalink:"/httpexchange-spring-boot-starter/docs/core/validation",draft:!1,unlisted:!1,editUrl:"https://github.com/DanielLiu1123/httpexchange-spring-boot-starter/tree/main/website/versioned_docs/version-3.2.x/10-core/40-validation.mdx",tags:[],version:"3.2.x",sidebarPosition:40,frontMatter:{sidebar_position:40},sidebar:"tutorialSidebar",previous:{title:"Configuration Properties",permalink:"/httpexchange-spring-boot-starter/docs/core/configuration-properties"},next:{title:"Extensions",permalink:"/httpexchange-spring-boot-starter/docs/category/extensions"}},c={},d=[];function p(t){const e={code:"code",h1:"h1",p:"p",pre:"pre",...(0,i.M)(),...t.components};return(0,o.jsxs)(o.Fragment,{children:[(0,o.jsx)(e.h1,{id:"validation",children:"Validation"}),"\n",(0,o.jsxs)(e.p,{children:["Support work with ",(0,o.jsx)(e.code,{children:"spring-boot-starter-validation"}),"."]}),"\n",(0,o.jsx)(e.pre,{children:(0,o.jsx)(e.code,{className:"language-java",children:'@HttpExchange("${api.post.url}")\n@Validated\npublic interface PostApi {\n    @GetExchange("/typicode/demo/posts/{id}")\n    Post getPost(@PathVariable("id") @Min(1) @Max(3) int id);\n}\n'})}),"\n",(0,o.jsx)(e.p,{children:"This approach ensures that validation rules are consistent across both client and server."})]})}function l(t={}){const{wrapper:e}={...(0,i.M)(),...t.components};return e?(0,o.jsx)(e,{...t,children:(0,o.jsx)(p,{...t})}):p(t)}},2172:(t,e,n)=>{n.d(e,{I:()=>a,M:()=>s});var o=n(1504);const i={},r=o.createContext(i);function s(t){const e=o.useContext(r);return o.useMemo((function(){return"function"==typeof t?t(e):{...e,...t}}),[e,t])}function a(t){let e;return e=t.disableParentContext?"function"==typeof t.components?t.components(i):t.components||i:s(t.components),o.createElement(r.Provider,{value:e},t.children)}}}]);