"use strict";(self.webpackChunkwebsite=self.webpackChunkwebsite||[]).push([[916],{4488:(e,t,n)=>{n.r(t),n.d(t,{assets:()=>c,contentTitle:()=>a,default:()=>u,frontMatter:()=>r,metadata:()=>o,toc:()=>d});var i=n(7624),s=n(2172);const r={sidebar_position:60},a="Set Read Timeout Dynamically",o={id:"extensions/set-read-timeout-dynamically",title:"Set Read Timeout Dynamically",description:"Spring does not provide a way to set the read timeout dynamically,",source:"@site/docs/20-extensions/60-set-read-timeout-dynamically.mdx",sourceDirName:"20-extensions",slug:"/extensions/set-read-timeout-dynamically",permalink:"/httpexchange-spring-boot-starter/docs/extensions/set-read-timeout-dynamically",draft:!1,unlisted:!1,editUrl:"https://github.com/DanielLiu1123/httpexchange-spring-boot-starter/tree/main/website/docs/20-extensions/60-set-read-timeout-dynamically.mdx",tags:[],version:"current",sidebarPosition:60,frontMatter:{sidebar_position:60},sidebar:"tutorialSidebar",previous:{title:"Url Variables",permalink:"/httpexchange-spring-boot-starter/docs/extensions/url-variables"},next:{title:"Native Image",permalink:"/httpexchange-spring-boot-starter/docs/extensions/native-image"}},c={},d=[{value:"Use <code>RequestConfigurator</code> Interface",id:"use-requestconfigurator-interface",level:2},{value:"Use <code>Requester</code> Class",id:"use-requester-class",level:2},{value:"Reactive Client",id:"reactive-client",level:2}];function l(e){const t={a:"a",admonition:"admonition",code:"code",del:"del",h1:"h1",h2:"h2",p:"p",pre:"pre",strong:"strong",...(0,s.M)(),...e.components};return(0,i.jsxs)(i.Fragment,{children:[(0,i.jsx)(t.h1,{id:"set-read-timeout-dynamically",children:"Set Read Timeout Dynamically"}),"\n",(0,i.jsxs)(t.p,{children:["Spring does not provide a way to set the read timeout dynamically,\nsee ",(0,i.jsx)(t.a,{href:"https://github.com/spring-projects/spring-framework/issues/31926",children:"issue"}),".\nThis framework provides ",(0,i.jsx)(t.code,{children:"EnhancedJdkClientHttpRequestFactory"})," to support this feature."]}),"\n",(0,i.jsxs)(t.admonition,{type:"note",children:[(0,i.jsxs)(t.p,{children:[(0,i.jsxs)(t.strong,{children:["This feature needs to use ",(0,i.jsx)(t.code,{children:"EnhancedJdkClientHttpRequestFactory"})," as the ",(0,i.jsx)(t.code,{children:"ClientHttpRequestFactory"})," implementation"]}),",\n",(0,i.jsx)(t.del,{children:"and this is the default behavior."})]}),(0,i.jsxs)(t.p,{children:["To avoid changing Spring's default behavior,\nstarting from version ",(0,i.jsx)(t.code,{children:"3.2.4"}),", the ",(0,i.jsx)(t.code,{children:"HttpClientCustomizer"})," interface was added.\nIt no longer uses ",(0,i.jsx)(t.code,{children:"EnhancedJdkClientHttpRequestFactory"})," as the default implementation\nbut instead uses the built-in ",(0,i.jsx)(t.code,{children:"JdkClientHttpRequestFactory"}),".\nTo use ",(0,i.jsx)(t.code,{children:"EnhancedJdkClientHttpRequestFactory"}),", it must be explicitly configured in ",(0,i.jsx)(t.code,{children:"HttpClientCustomizer"}),"."]}),(0,i.jsx)(t.pre,{children:(0,i.jsx)(t.code,{className:"language-java",children:"// For RestClient\n@Bean\nHttpClientCustomizer.RestClientCustomizer restClientCustomizer() {\n    return (client, channel) -> {\n        EnhancedJdkClientHttpRequestFactory requestFactory = new EnhancedJdkClientHttpRequestFactory();\n        requestFactory.setReadTimeout(channel.getReadTimeout());\n        client.requestFactory(requestFactory);\n    };\n}\n\n// For RestTemplate\n@Bean\nHttpClientCustomizer.RestTemplateCustomizer restTemplateCustomizer() {\n    return (client, channel) -> {\n        EnhancedJdkClientHttpRequestFactory requestFactory = new EnhancedJdkClientHttpRequestFactory();\n        requestFactory.setReadTimeout(channel.getReadTimeout());\n        client.setRequestFactory(requestFactory);\n    };\n}\n"})})]}),"\n",(0,i.jsx)(t.p,{children:"There are two ways to set the read timeout dynamically."}),"\n",(0,i.jsxs)(t.h2,{id:"use-requestconfigurator-interface",children:["Use ",(0,i.jsx)(t.code,{children:"RequestConfigurator"})," Interface"]}),"\n",(0,i.jsx)(t.pre,{children:(0,i.jsx)(t.code,{className:"language-java",children:'@HttpExchange("/users")\ninterface UserApi extends RequestConfigurator<UserApi> {\n    @GetExchange\n    List<User> list();\n}\n\n@Service\nclass UserService {\n    @Autowired\n    UserApi userApi;\n\n    List<User> listWithTimeout(int timeout) {\n        return userApi.withTimeout(timeout).list();\n    }\n}\n'})}),"\n",(0,i.jsxs)(t.p,{children:["Each time the ",(0,i.jsx)(t.code,{children:"RequestConfigurator"})," method is called, a new proxy client will be created,\nand it inherits the original configuration and will not affect the original configuration."]}),"\n",(0,i.jsx)(t.admonition,{type:"info",children:(0,i.jsxs)(t.p,{children:[(0,i.jsx)(t.code,{children:"RequestConfigurator"})," is suitable for client-side use but not for defining a neutral API.\nTherefore, ",(0,i.jsx)(t.code,{children:"Requester"})," is provided for a programmatic way to dynamically set the read timeout."]})}),"\n",(0,i.jsxs)(t.h2,{id:"use-requester-class",children:["Use ",(0,i.jsx)(t.code,{children:"Requester"})," Class"]}),"\n",(0,i.jsx)(t.pre,{children:(0,i.jsx)(t.code,{className:"language-java",children:'List<User> users = Requester.create()\n                        .withTimeout(10000)\n                        .addHeader("X-Foo", "bar")\n                        .call(() -> userApi.list());\n'})}),"\n",(0,i.jsx)(t.h2,{id:"reactive-client",children:"Reactive Client"}),"\n",(0,i.jsxs)(t.p,{children:["For ",(0,i.jsx)(t.code,{children:"WebClient"})," client type, use ",(0,i.jsx)(t.code,{children:"timeout"})," method to set the read timeout for each request."]}),"\n",(0,i.jsx)(t.pre,{children:(0,i.jsx)(t.code,{className:"language-java",children:"Flux<User> users = userApi.list().timeout(Duration.ofSeconds(10));\n"})})]})}function u(e={}){const{wrapper:t}={...(0,s.M)(),...e.components};return t?(0,i.jsx)(t,{...e,children:(0,i.jsx)(l,{...e})}):l(e)}},2172:(e,t,n)=>{n.d(t,{I:()=>o,M:()=>a});var i=n(1504);const s={},r=i.createContext(s);function a(e){const t=i.useContext(r);return i.useMemo((function(){return"function"==typeof e?e(t):{...t,...e}}),[t,e])}function o(e){let t;return t=e.disableParentContext?"function"==typeof e.components?e.components(s):e.components||s:a(e.components),i.createElement(r.Provider,{value:t},e.children)}}}]);