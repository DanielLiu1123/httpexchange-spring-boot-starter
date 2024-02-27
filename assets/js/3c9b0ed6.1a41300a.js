"use strict";(self.webpackChunkwebsite=self.webpackChunkwebsite||[]).push([[1e3],{6024:(e,a,n)=>{n.r(a),n.d(a,{assets:()=>u,contentTitle:()=>i,default:()=>h,frontMatter:()=>o,metadata:()=>c,toc:()=>d});var t=n(7624),r=n(2172),l=n(1268),s=n(5388);const o={sidebar_position:20},i="LoadBalancer",c={id:"extensions/loadbalancer",title:"LoadBalancer",description:"Support to work with spring-cloud-starter-loadbalancer to achieve client side load balancing.",source:"@site/docs/20-extensions/20-loadbalancer.mdx",sourceDirName:"20-extensions",slug:"/extensions/loadbalancer",permalink:"/httpexchange-spring-boot-starter/docs/next/extensions/loadbalancer",draft:!1,unlisted:!1,editUrl:"https://github.com/DanielLiu1123/httpexchange-spring-boot-starter/tree/main/website/docs/20-extensions/20-loadbalancer.mdx",tags:[],version:"current",sidebarPosition:20,frontMatter:{sidebar_position:20},sidebar:"tutorialSidebar",previous:{title:"@RequestMapping Support",permalink:"/httpexchange-spring-boot-starter/docs/next/extensions/request-mapping-annotation-support"},next:{title:"Convert Bean to Query Parameter",permalink:"/httpexchange-spring-boot-starter/docs/next/extensions/bean-to-query"}},u={},d=[{value:"Enable LoadBalancer",id:"enable-loadbalancer",level:2},{value:"Disable LoadBalancer",id:"disable-loadbalancer",level:2},{value:"Disable for Specific Channel",id:"disable-for-specific-channel",level:3}];function p(e){const a={a:"a",code:"code",h1:"h1",h2:"h2",h3:"h3",p:"p",pre:"pre",...(0,r.M)(),...e.components};return(0,t.jsxs)(t.Fragment,{children:[(0,t.jsx)(a.h1,{id:"loadbalancer",children:"LoadBalancer"}),"\n",(0,t.jsxs)(a.p,{children:["Support to work with ",(0,t.jsx)(a.code,{children:"spring-cloud-starter-loadbalancer"})," to achieve client side load balancing."]}),"\n",(0,t.jsx)(a.h2,{id:"enable-loadbalancer",children:"Enable LoadBalancer"}),"\n",(0,t.jsxs)(a.p,{children:["This feature is automatically enabled when the ",(0,t.jsx)(a.code,{children:"spring-cloud-starter-loadbalancer"})," is present on the classpath."]}),"\n",(0,t.jsxs)(l.c,{children:[(0,t.jsx)(s.c,{value:"gradle",label:"Gradle",children:(0,t.jsx)(a.pre,{children:(0,t.jsx)(a.code,{className:"language-groovy",children:'implementation("org.springframework.cloud:spring-cloud-starter-loadbalancer")\n'})})}),(0,t.jsx)(s.c,{value:"maven",label:"Maven",children:(0,t.jsx)(a.pre,{children:(0,t.jsx)(a.code,{className:"language-xml",children:"<dependency>\n    <groupId>org.springframework.cloud</groupId>\n    <artifactId>spring-cloud-starter-loadbalancer</artifactId>\n</dependency>\n"})})})]}),"\n",(0,t.jsx)(a.h2,{id:"disable-loadbalancer",children:"Disable LoadBalancer"}),"\n",(0,t.jsxs)(a.p,{children:["Set ",(0,t.jsx)(a.code,{children:"http-exchange.loadbalancer-enabled"})," to ",(0,t.jsx)(a.code,{children:"false"})," to disable the load balancer for all HttpExchange clients."]}),"\n",(0,t.jsx)(a.pre,{children:(0,t.jsx)(a.code,{className:"language-yaml",metastring:'title="application.yml"',children:"http-exchange:\n  loadbalancer-enabled: false\n"})}),"\n",(0,t.jsx)(a.h3,{id:"disable-for-specific-channel",children:"Disable for Specific Channel"}),"\n",(0,t.jsxs)(a.p,{children:["Disable the load balancer for a specific channel by setting ",(0,t.jsx)(a.code,{children:"loadbalancer-enabled"})," to ",(0,t.jsx)(a.code,{children:"false"}),"."]}),"\n",(0,t.jsx)(a.pre,{children:(0,t.jsx)(a.code,{className:"language-yaml",metastring:'title="application.yml"',children:"http-exchange:\n  channels:\n    - base-url: user\n      # highlight-next-line-as-added\n      loadbalancer-enabled: false\n      clients:\n        - com.example.user.api.*Api\n"})}),"\n",(0,t.jsxs)(a.p,{children:["See ",(0,t.jsx)(a.a,{href:"https://github.com/DanielLiu1123/httpexchange-spring-boot-starter/tree/main/examples/loadbalancer",children:"loadbalancer"})," example."]})]})}function h(e={}){const{wrapper:a}={...(0,r.M)(),...e.components};return a?(0,t.jsx)(a,{...e,children:(0,t.jsx)(p,{...e})}):p(e)}},5388:(e,a,n)=>{n.d(a,{c:()=>s});n(1504);var t=n(5456);const r={tabItem:"tabItem_Ymn6"};var l=n(7624);function s(e){let{children:a,hidden:n,className:s}=e;return(0,l.jsx)("div",{role:"tabpanel",className:(0,t.c)(r.tabItem,s),hidden:n,children:a})}},1268:(e,a,n)=>{n.d(a,{c:()=>w});var t=n(1504),r=n(5456),l=n(3943),s=n(5592),o=n(5288),i=n(632),c=n(7128),u=n(1148);function d(e){return t.Children.toArray(e).filter((e=>"\n"!==e)).map((e=>{if(!e||(0,t.isValidElement)(e)&&function(e){const{props:a}=e;return!!a&&"object"==typeof a&&"value"in a}(e))return e;throw new Error(`Docusaurus error: Bad <Tabs> child <${"string"==typeof e.type?e.type:e.type.name}>: all children of the <Tabs> component should be <TabItem>, and every <TabItem> should have a unique "value" prop.`)}))?.filter(Boolean)??[]}function p(e){const{values:a,children:n}=e;return(0,t.useMemo)((()=>{const e=a??function(e){return d(e).map((e=>{let{props:{value:a,label:n,attributes:t,default:r}}=e;return{value:a,label:n,attributes:t,default:r}}))}(n);return function(e){const a=(0,c.w)(e,((e,a)=>e.value===a.value));if(a.length>0)throw new Error(`Docusaurus error: Duplicate values "${a.map((e=>e.value)).join(", ")}" found in <Tabs>. Every value needs to be unique.`)}(e),e}),[a,n])}function h(e){let{value:a,tabValues:n}=e;return n.some((e=>e.value===a))}function b(e){let{queryString:a=!1,groupId:n}=e;const r=(0,s.Uz)(),l=function(e){let{queryString:a=!1,groupId:n}=e;if("string"==typeof a)return a;if(!1===a)return null;if(!0===a&&!n)throw new Error('Docusaurus error: The <Tabs> component groupId prop is required if queryString=true, because this value is used as the search param name. You can also provide an explicit value such as queryString="my-search-param".');return n??null}({queryString:a,groupId:n});return[(0,i._M)(l),(0,t.useCallback)((e=>{if(!l)return;const a=new URLSearchParams(r.location.search);a.set(l,e),r.replace({...r.location,search:a.toString()})}),[l,r])]}function f(e){const{defaultValue:a,queryString:n=!1,groupId:r}=e,l=p(e),[s,i]=(0,t.useState)((()=>function(e){let{defaultValue:a,tabValues:n}=e;if(0===n.length)throw new Error("Docusaurus error: the <Tabs> component requires at least one <TabItem> children component");if(a){if(!h({value:a,tabValues:n}))throw new Error(`Docusaurus error: The <Tabs> has a defaultValue "${a}" but none of its children has the corresponding value. Available values are: ${n.map((e=>e.value)).join(", ")}. If you intend to show no default tab, use defaultValue={null} instead.`);return a}const t=n.find((e=>e.default))??n[0];if(!t)throw new Error("Unexpected error: 0 tabValues");return t.value}({defaultValue:a,tabValues:l}))),[c,d]=b({queryString:n,groupId:r}),[f,m]=function(e){let{groupId:a}=e;const n=function(e){return e?`docusaurus.tab.${e}`:null}(a),[r,l]=(0,u.IN)(n);return[r,(0,t.useCallback)((e=>{n&&l.set(e)}),[n,l])]}({groupId:r}),g=(()=>{const e=c??f;return h({value:e,tabValues:l})?e:null})();(0,o.c)((()=>{g&&i(g)}),[g]);return{selectedValue:s,selectValue:(0,t.useCallback)((e=>{if(!h({value:e,tabValues:l}))throw new Error(`Can't select invalid tab value=${e}`);i(e),d(e),m(e)}),[d,m,l]),tabValues:l}}var m=n(3664);const g={tabList:"tabList__CuJ",tabItem:"tabItem_LNqP"};var x=n(7624);function v(e){let{className:a,block:n,selectedValue:t,selectValue:s,tabValues:o}=e;const i=[],{blockElementScrollPositionUntilNextRender:c}=(0,l.MV)(),u=e=>{const a=e.currentTarget,n=i.indexOf(a),r=o[n].value;r!==t&&(c(a),s(r))},d=e=>{let a=null;switch(e.key){case"Enter":u(e);break;case"ArrowRight":{const n=i.indexOf(e.currentTarget)+1;a=i[n]??i[0];break}case"ArrowLeft":{const n=i.indexOf(e.currentTarget)-1;a=i[n]??i[i.length-1];break}}a?.focus()};return(0,x.jsx)("ul",{role:"tablist","aria-orientation":"horizontal",className:(0,r.c)("tabs",{"tabs--block":n},a),children:o.map((e=>{let{value:a,label:n,attributes:l}=e;return(0,x.jsx)("li",{role:"tab",tabIndex:t===a?0:-1,"aria-selected":t===a,ref:e=>i.push(e),onKeyDown:d,onClick:u,...l,className:(0,r.c)("tabs__item",g.tabItem,l?.className,{"tabs__item--active":t===a}),children:n??a},a)}))})}function j(e){let{lazy:a,children:n,selectedValue:r}=e;const l=(Array.isArray(n)?n:[n]).filter(Boolean);if(a){const e=l.find((e=>e.props.value===r));return e?(0,t.cloneElement)(e,{className:"margin-top--md"}):null}return(0,x.jsx)("div",{className:"margin-top--md",children:l.map(((e,a)=>(0,t.cloneElement)(e,{key:a,hidden:e.props.value!==r})))})}function y(e){const a=f(e);return(0,x.jsxs)("div",{className:(0,r.c)("tabs-container",g.tabList),children:[(0,x.jsx)(v,{...e,...a}),(0,x.jsx)(j,{...e,...a})]})}function w(e){const a=(0,m.c)();return(0,x.jsx)(y,{...e,children:d(e.children)},String(a))}},2172:(e,a,n)=>{n.d(a,{I:()=>o,M:()=>s});var t=n(1504);const r={},l=t.createContext(r);function s(e){const a=t.useContext(l);return t.useMemo((function(){return"function"==typeof e?e(a):{...a,...e}}),[a,e])}function o(e){let a;return a=e.disableParentContext?"function"==typeof e.components?e.components(r):e.components||r:s(e.components),t.createElement(l.Provider,{value:a},e.children)}}}]);