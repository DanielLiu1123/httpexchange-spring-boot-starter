"use strict";(self.webpackChunkwebsite=self.webpackChunkwebsite||[]).push([[9368],{1208:(e,t,n)=>{n.r(t),n.d(t,{assets:()=>c,contentTitle:()=>a,default:()=>h,frontMatter:()=>r,metadata:()=>s,toc:()=>l});var i=n(7624),o=n(2172);const r={sidebar_position:30},a="Configuration Properties",s={id:"core/configuration-properties",title:"Configuration Properties",description:"This library provides a lot of configuration properties to customize the behavior of the client.",source:"@site/versioned_docs/version-3.2.x/10-core/30-configuration-properties.mdx",sourceDirName:"10-core",slug:"/core/configuration-properties",permalink:"/httpexchange-spring-boot-starter/docs/core/configuration-properties",draft:!1,unlisted:!1,editUrl:"https://github.com/DanielLiu1123/httpexchange-spring-boot-starter/tree/main/website/versioned_docs/version-3.2.x/10-core/30-configuration-properties.mdx",tags:[],version:"3.2.x",sidebarPosition:30,frontMatter:{sidebar_position:30},sidebar:"tutorialSidebar",previous:{title:"Generate Server Implementation",permalink:"/httpexchange-spring-boot-starter/docs/core/generate-server-implementation"},next:{title:"Validation",permalink:"/httpexchange-spring-boot-starter/docs/core/validation"}},c={},l=[{value:"Basic Usage",id:"basic-usage",level:2},{value:"Configuration Properties",id:"configuration-properties-1",level:2},{value:"Configuration Example",id:"configuration-example",level:2}];function p(e){const t={a:"a",code:"code",h1:"h1",h2:"h2",p:"p",pre:"pre",...(0,o.M)(),...e.components};return(0,i.jsxs)(i.Fragment,{children:[(0,i.jsx)(t.h1,{id:"configuration-properties",children:"Configuration Properties"}),"\n",(0,i.jsxs)(t.p,{children:["This library provides a lot of configuration properties to customize the behavior of the client.\nYou can configure the ",(0,i.jsx)(t.code,{children:"base-url"}),", ",(0,i.jsx)(t.code,{children:"read-timeout"})," for each channel, and each channel can apply to multiple clients."]}),"\n",(0,i.jsx)(t.h2,{id:"basic-usage",children:"Basic Usage"}),"\n",(0,i.jsx)(t.pre,{children:(0,i.jsx)(t.code,{className:"language-yaml",metastring:'title="application.yaml"',children:"http-exchange:\n  read-timeout: 5000\n  channels:\n    - base-url: http://user\n      read-timeout: 3000\n      clients:\n        - com.example.user.api.*Api\n    - base-url: http://order\n      clients:\n        - com.example.order.api.*Api\n"})}),"\n",(0,i.jsxs)(t.p,{children:["Using property ",(0,i.jsx)(t.code,{children:"clients"})," or ",(0,i.jsx)(t.code,{children:"classes"})," to identify the client, use ",(0,i.jsx)(t.code,{children:"classes"})," first if configured."]}),"\n",(0,i.jsxs)(t.p,{children:["For example, there is a http client interface: ",(0,i.jsx)(t.code,{children:"com.example.PostApi"}),", you can use the following configuration to identify the client"]}),"\n",(0,i.jsx)(t.pre,{children:(0,i.jsx)(t.code,{className:"language-yaml",metastring:'title="application.yaml"',children:"http-exchange:\n  channels:\n    - base-url: http://service\n      clients: [com.example.PostApi] # Class canonical name\n    # clients: [post-api] Class simple name (Kebab-case)\n    # clients: [PostApi]  Class simple name (Pascal-case)\n    # clients: [com.**.*Api] (Ant-style pattern)\n      classes: [com.example.PostApi] # Class canonical name\n"})}),"\n",(0,i.jsx)(t.h2,{id:"configuration-properties-1",children:"Configuration Properties"}),"\n",(0,i.jsxs)(t.p,{children:["See ",(0,i.jsx)(t.a,{href:"https://github.com/DanielLiu1123/httpexchange-spring-boot-starter/blob/main/httpexchange-spring-boot-autoconfigure/src/main/java/io/github/danielliu1123/httpexchange/HttpExchangeProperties.java",children:"Configuration Properties"})," for a complete list of properties."]}),"\n",(0,i.jsx)(t.h2,{id:"configuration-example",children:"Configuration Example"}),"\n",(0,i.jsxs)(t.p,{children:["See ",(0,i.jsx)(t.a,{href:"https://github.com/DanielLiu1123/httpexchange-spring-boot-starter/blob/main/httpexchange-spring-boot-autoconfigure/src/main/resources/application-http-exchange-statrer-example.yml",children:"configuration example"})," for usage example."]})]})}function h(e={}){const{wrapper:t}={...(0,o.M)(),...e.components};return t?(0,i.jsx)(t,{...e,children:(0,i.jsx)(p,{...e})}):p(e)}},2172:(e,t,n)=>{n.d(t,{I:()=>s,M:()=>a});var i=n(1504);const o={},r=i.createContext(o);function a(e){const t=i.useContext(r);return i.useMemo((function(){return"function"==typeof e?e(t):{...t,...e}}),[t,e])}function s(e){let t;return t=e.disableParentContext?"function"==typeof e.components?e.components(o):e.components||o:a(e.components),i.createElement(r.Provider,{value:t},e.children)}}}]);