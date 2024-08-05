import{a as m}from"./chunk-JPA33CC6.js";import{a as g}from"./chunk-FOKD7DAA.js";import"./chunk-TTRSQA65.js";import{a as p}from"./chunk-HZ2SEKHW.js";import"./chunk-OE4G5TB3.js";import{a as j}from"./chunk-4FO7WJ36.js";import{a as l}from"./chunk-LVHT47OW.js";import{F as k}from"./chunk-LWH7SIW3.js";import{Pb as r,jc as o,kc as h,ra as t,xb as c}from"./chunk-TLBD5JYT.js";import{a as d,b as i,g as C}from"./chunk-ODN5LVDJ.js";var y=C(k());var w={title:"\u6B22\u8FCE & \u7B80\u4ECB",mdFile:"./index.md",category:m,order:0,imports:[p,g],demos:{AnimateLogoComponent:p,FeatureCardsComponent:g}},n=w;var u=[];var b={AnimateLogoComponent:[{title:"TypeScript",code:`<pre class="ngde hljs"><code lang="typescript" class="hljs language-typescript code-lines ngde"><span class="line ngde"><span class="hljs-keyword ngde">import</span> {
</span><span class="line ngde">  <span class="hljs-title class_ ngde">Component</span>,
</span><span class="line ngde">} <span class="hljs-keyword ngde">from</span> <span class="hljs-string ngde">'@angular/core'</span>;
</span><span class="line ngde"><span class="hljs-keyword ngde">import</span> {<span class="hljs-title class_ ngde">SharedModule</span>} <span class="hljs-keyword ngde">from</span> <span class="hljs-string ngde">"../shared.module"</span>;
</span><span class="line ngde">
</span><span class="line ngde"><span class="hljs-meta ngde">@Component</span>({
</span><span class="line ngde">  <span class="hljs-attr ngde">selector</span>: <span class="hljs-string ngde">'animate-logo'</span>,
</span><span class="line ngde">  <span class="hljs-attr ngde">imports</span>: [
</span><span class="line ngde">    <span class="hljs-title class_ ngde">SharedModule</span>
</span><span class="line ngde">  ],
</span><span class="line ngde">  <span class="hljs-attr ngde">template</span>: <span class="hljs-string ngde">\`</span>
</span><span class="line ngde"><span class="hljs-string ngde">    &#x3C;div class="bg"></span>
</span><span class="line ngde"><span class="hljs-string ngde">      &#x3C;img class="logo" src="/assets/images/logo_circle.png" /></span>
</span><span class="line ngde"><span class="hljs-string ngde">    &#x3C;/div></span>
</span><span class="line ngde"><span class="hljs-string ngde">  \`</span>,
</span><span class="line ngde">  <span class="hljs-attr ngde">standalone</span>: <span class="hljs-literal ngde">true</span>,
</span><span class="line ngde">  <span class="hljs-attr ngde">styles</span>: [
</span><span class="line ngde">    <span class="hljs-string ngde">\`</span>
</span><span class="line ngde"><span class="hljs-string ngde">      :host {</span>
</span><span class="line ngde"><span class="hljs-string ngde">        text-align: center;</span>
</span><span class="line ngde"><span class="hljs-string ngde">        display: block;</span>
</span><span class="line ngde"><span class="hljs-string ngde">        width: 100%;</span>
</span><span class="line ngde"><span class="hljs-string ngde">      }</span>
</span><span class="line ngde"><span class="hljs-string ngde"></span>
</span><span class="line ngde"><span class="hljs-string ngde">      .bg {</span>
</span><span class="line ngde"><span class="hljs-string ngde">        padding: 20px;</span>
</span><span class="line ngde"><span class="hljs-string ngde">        background: linear-gradient(45deg, #832E3D 0%, #000 20%, #7F52FF 40%, #832E3D 60%, #000 80%, #7F52FF 100%);</span>
</span><span class="line ngde"><span class="hljs-string ngde">        background-size: 500% 500%;</span>
</span><span class="line ngde"><span class="hljs-string ngde">        animation: gradient 12s linear infinite;</span>
</span><span class="line ngde"><span class="hljs-string ngde">      }</span>
</span><span class="line ngde"><span class="hljs-string ngde"></span>
</span><span class="line ngde"><span class="hljs-string ngde">      @keyframes gradient {</span>
</span><span class="line ngde"><span class="hljs-string ngde">        0% {</span>
</span><span class="line ngde"><span class="hljs-string ngde">          background-position: 100% 0</span>
</span><span class="line ngde"><span class="hljs-string ngde">        }</span>
</span><span class="line ngde"><span class="hljs-string ngde">        100% {</span>
</span><span class="line ngde"><span class="hljs-string ngde">          background-position: 25% 100%</span>
</span><span class="line ngde"><span class="hljs-string ngde">        }</span>
</span><span class="line ngde"><span class="hljs-string ngde">      }</span>
</span><span class="line ngde"><span class="hljs-string ngde"></span>
</span><span class="line ngde"><span class="hljs-string ngde">      .logo {</span>
</span><span class="line ngde"><span class="hljs-string ngde">        width: 100px;</span>
</span><span class="line ngde"><span class="hljs-string ngde">        height: 100px;</span>
</span><span class="line ngde"><span class="hljs-string ngde">        animation-name: spin;</span>
</span><span class="line ngde"><span class="hljs-string ngde">        animation-duration: 27.00s;</span>
</span><span class="line ngde"><span class="hljs-string ngde">        animation-iteration-count: infinite;</span>
</span><span class="line ngde"><span class="hljs-string ngde">        animation-timing-function: linear;</span>
</span><span class="line ngde"><span class="hljs-string ngde">        mix-blend-mode: exclusion;</span>
</span><span class="line ngde"><span class="hljs-string ngde">      }</span>
</span><span class="line ngde"><span class="hljs-string ngde"></span>
</span><span class="line ngde"><span class="hljs-string ngde">      @keyframes spin {</span>
</span><span class="line ngde"><span class="hljs-string ngde">        0% {</span>
</span><span class="line ngde"><span class="hljs-string ngde">          transform: rotate(0deg);</span>
</span><span class="line ngde"><span class="hljs-string ngde">        }</span>
</span><span class="line ngde"><span class="hljs-string ngde">        100% {</span>
</span><span class="line ngde"><span class="hljs-string ngde">          transform: rotate(360deg);</span>
</span><span class="line ngde"><span class="hljs-string ngde">        }</span>
</span><span class="line ngde"><span class="hljs-string ngde">      }</span>
</span><span class="line ngde"><span class="hljs-string ngde">    \`</span>
</span><span class="line ngde">  ]
</span><span class="line ngde">})
</span><span class="line ngde"><span class="hljs-keyword ngde">export</span> <span class="hljs-keyword ngde">class</span> <span class="hljs-title class_ ngde">AnimateLogoComponent</span>{}
</span></code></pre>`}],FeatureCardsComponent:[{title:"TypeScript",code:`<pre class="ngde hljs"><code lang="typescript" class="hljs language-typescript code-lines ngde"><span class="line ngde"><span class="hljs-keyword ngde">import</span> {
</span><span class="line ngde">  <span class="hljs-title class_ ngde">Component</span>,
</span><span class="line ngde">} <span class="hljs-keyword ngde">from</span> <span class="hljs-string ngde">'@angular/core'</span>;
</span><span class="line ngde"><span class="hljs-keyword ngde">import</span> {<span class="hljs-title class_ ngde">SharedModule</span>} <span class="hljs-keyword ngde">from</span> <span class="hljs-string ngde">"../shared.module"</span>;
</span><span class="line ngde"><span class="hljs-keyword ngde">import</span> {<span class="hljs-title class_ ngde">AnimateOnScrollModule</span>} <span class="hljs-keyword ngde">from</span> <span class="hljs-string ngde">"primeng/animateonscroll"</span>;
</span><span class="line ngde">
</span><span class="line ngde"><span class="hljs-meta ngde">@Component</span>({
</span><span class="line ngde">  <span class="hljs-attr ngde">selector</span>: <span class="hljs-string ngde">'feature-cards'</span>,
</span><span class="line ngde">  <span class="hljs-attr ngde">imports</span>: [
</span><span class="line ngde">    <span class="hljs-title class_ ngde">SharedModule</span>,
</span><span class="line ngde">    <span class="hljs-title class_ ngde">AnimateOnScrollModule</span>
</span><span class="line ngde">  ],
</span><span class="line ngde">  <span class="hljs-attr ngde">template</span>: <span class="hljs-string ngde">\`</span>
</span><span class="line ngde"><span class="hljs-string ngde">    &#x3C;div pAnimateOnScroll enterClass="zoomin" class="card flex flex-row md:justify-content-between gap-3 animation-duration-1000 animation-ease-in-out"></span>
</span><span class="line ngde"><span class="hljs-string ngde">      &#x3C;p-card pRipple header="Write Kotlin ORM in Simple and Type-safe Way"/></span>
</span><span class="line ngde"><span class="hljs-string ngde">      &#x3C;p-card pRipple header="Less runtime reflect, higher runtime efficiency"/></span>
</span><span class="line ngde"><span class="hljs-string ngde">      &#x3C;p-card pRipple header="Multiple database dialect support"/></span>
</span><span class="line ngde"><span class="hljs-string ngde">    &#x3C;/div></span>
</span><span class="line ngde"><span class="hljs-string ngde">  \`</span>,
</span><span class="line ngde">  <span class="hljs-attr ngde">standalone</span>: <span class="hljs-literal ngde">true</span>,
</span><span class="line ngde">  <span class="hljs-attr ngde">styles</span>: [<span class="hljs-string ngde">\`</span>
</span><span class="line ngde"><span class="hljs-string ngde">    p-card :hover {</span>
</span><span class="line ngde"><span class="hljs-string ngde"></span>
</span><span class="line ngde"><span class="hljs-string ngde">    }</span>
</span><span class="line ngde"><span class="hljs-string ngde">  \`</span>]
</span><span class="line ngde">})
</span><span class="line ngde"><span class="hljs-keyword ngde">export</span> <span class="hljs-keyword ngde">class</span> <span class="hljs-title class_ ngde">FeatureCardsComponent</span> {
</span><span class="line ngde">}
</span></code></pre>`}]},f=b;var O=`<h1 id="\u6B22\u8FCE--\u7B80\u4ECB" class="ngde">\u6B22\u8FCE &#x26; \u7B80\u4ECB<a title="Link to heading" class="ng-doc-header-link ngde" href="/zh-CN/getting-started/welcome#\u6B22\u8FCE--\u7B80\u4ECB"><ng-doc-icon icon="link-2" size="16" class="ngde"></ng-doc-icon></a></h1><ng-doc-demo componentname="AnimateLogoComponent" indexable="false" class="ngde"><div id="options" class="ngde">{"container":false}</div></ng-doc-demo><p class="ngde"><strong class="ngde">Kronos ORM(Kotlin Reactive Object-Relational-Mapping)\u662F\u4E00\u6B3E\u57FA\u4E8EKCP\u3001\u4E3AK2\u8BBE\u8BA1\u7684\u73B0\u4EE3\u5316\u7684kotlin ORM\u6846\u67B6\u3002</strong></p><p class="ngde"><em class="ngde">Kronos</em>\u662F\u4E00\u4E2A\u8F7B\u91CF\u7EA7\u7684\u6846\u67B6\uFF0C\u4E3A\u5F00\u53D1\u8005\u63D0\u4F9B\u4E86\u4E00\u79CD\u7B80\u5355\u64CD\u4F5C\u591A\u79CD\u6570\u636E\u5E93\u7684\u65B9\u6848\u3002</p><p class="ngde"><em class="ngde">Kronos</em>\u5206\u6790 IR \u8868\u8FBE\u5F0F\u6811\u4EE5\u7B80\u5316\u4EE3\u7801\u7F16\u5199\u903B\u8F91\uFF0C\u5F97\u4EE5\u4F7FORM\u7684\u7F16\u5199\u7B80\u6D01\u800C\u53C8\u8BED\u4E49\u5316\uFF0C\u5E76\u4E14\u901A\u8FC7\u7F16\u8BD1\u5668\u63D2\u4EF6\uFF0C\u6211\u4EEC\u540C\u65F6\u63D0\u4F9B\u4E86Pojo\u548CMap\u4E92\u8F6C\u7684\u7B80\u5355\u65B9\u6848\u3002</p><p class="ngde"><em class="ngde">Kronos</em>\u7684\u8BBE\u8BA1\u521D\u8877\u662F\u4E3A\u4E86\u5F25\u8865\u73B0\u6709ORM\u6846\u67B6\u4E2D\u4E0D\u8DB3\u4E4B\u5904\uFF0C\u5E76\u57FA\u4E8E\u534F\u7A0B\u548C\u4EFB\u52A1\u673A\u5236\u5BF9\u6570\u636E\u64CD\u4F5C\u63D0\u4F9B\u66F4\u52A0\u4FBF\u6377\u548C\u9AD8\u6548\u7684\u7F16\u5199\u4F53\u9A8C\u3002</p><pre class="ngde hljs"><code class="hljs language-kotlin code-lines ngde" lang="kotlin" name="demo" icon="kotlin" highlightedlines="[]"><span class="line ngde"><span class="hljs-keyword ngde">if</span>(!db.table.exsits&#x3C;User>()){
</span><span class="line ngde">  db.table.create&#x3C;User>()
</span><span class="line ngde">}
</span><span class="line ngde">
</span><span class="line ngde"><span class="hljs-keyword ngde">val</span> user: User = User(
</span><span class="line ngde">        id = <span class="hljs-number ngde">1</span>,
</span><span class="line ngde">        name = <span class="hljs-string ngde">"Kronos"</span>,
</span><span class="line ngde">        age = <span class="hljs-number ngde">18</span>
</span><span class="line ngde">    )
</span><span class="line ngde">    
</span><span class="line ngde">user.insert(user)
</span><span class="line ngde">
</span><span class="line ngde">user.update().<span class="hljs-keyword ngde">set</span> { it.name = <span class="hljs-string ngde">"Kronos ORM"</span> }.<span class="hljs-keyword ngde">where</span> { it.id == <span class="hljs-number ngde">1</span> }.execute()
</span><span class="line ngde">
</span><span class="line ngde"><span class="hljs-keyword ngde">val</span> nameOfUser: String = user.select{ it.name }.<span class="hljs-keyword ngde">where</span> { it.id == <span class="hljs-number ngde">1</span> }.queryOne&#x3C;String>()
</span><span class="line ngde">
</span><span class="line ngde">user.delete().<span class="hljs-keyword ngde">where</span> { it.id == <span class="hljs-number ngde">1</span> }.execute()
</span></code></pre><ng-doc-demo componentname="FeatureCardsComponent" indexable="false" class="ngde"><div id="options" class="ngde">{"container":false}</div></ng-doc-demo>`,R=(()=>{let s=class s extends l{constructor(){super(),this.routePrefix="",this.pageType="guide",this.pageContent=O,this.page=n,this.demoAssets=f}};s.\u0275fac=function(e){return new(e||s)},s.\u0275cmp=t({type:s,selectors:[["ng-doc-page-zh-cn-getting-started-welcome"]],standalone:!0,features:[o([{provide:l,useExisting:s},u,n.providers??[]]),c,h],decls:1,vars:0,template:function(e,M){e&1&&r(0,"ng-doc-page")},dependencies:[j],encapsulation:2,changeDetection:0});let a=s;return a})(),v=[i(d({},(0,y.isRoute)(n.route)?n.route:{}),{path:"",component:R,title:"\u6B22\u8FCE & \u7B80\u4ECB"})],z=v;export{R as DynamicComponent,z as default};
