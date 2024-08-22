import{a as i}from"./chunk-XF5TZ35J.js";import"./chunk-CO4HBOGF.js";import{a as j}from"./chunk-Z4J7AABF.js";import{a as p}from"./chunk-ZO2WZBJK.js";import"./chunk-D3GIATC6.js";import{a as m}from"./chunk-HNPVF6HZ.js";import{a as l}from"./chunk-YGVJYOZD.js";import{H as k}from"./chunk-HT2ZSXJU.js";import{Pb as o,jc as r,kc as h,ra as t,xb as c}from"./chunk-W2MTHNV2.js";import{a as d,b as g,g as w}from"./chunk-ODN5LVDJ.js";var y=w(k());var C={title:"Welcome & Introduction",mdFile:"./index.md",category:j,order:0,imports:[p,i],demos:{AnimateLogoComponent:p,FeatureCardsComponent:i}},n=C;var u=[];var b={AnimateLogoComponent:[{title:"TypeScript",code:`<pre class="ngde hljs"><code lang="typescript" class="hljs language-typescript code-lines ngde"><span class="line ngde"><span class="hljs-keyword ngde">import</span> {
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
</span></code></pre>`}]},f=b;var v=`<h1 id="welcome--introduction" class="ngde">Welcome &#x26; Introduction<a title="Link to heading" class="ng-doc-header-link ngde" href="/en/getting-started/welcome#welcome--introduction"><ng-doc-icon icon="link-2" size="16" class="ngde"></ng-doc-icon></a></h1><ng-doc-demo componentname="AnimateLogoComponent" indexable="false" class="ngde"><div id="options" class="ngde">{"container":false}</div></ng-doc-demo><p class="ngde"><strong class="ngde">Kronos ORM (Kotlin Reactive Object-Relational-Mapping) is a modern Kotlin ORM framework based on KCP and designed for K2.</strong></p><p class="ngde"><em class="ngde">Kronos</em> is a lightweight framework that provides developers with a simple solution for interacting with multiple databases.</p><p class="ngde"><em class="ngde">Kronos</em> analyzes IR expression trees to simplify code logic, making ORM coding concise and semantic. Through a compiler plugin, we also provide a simple solution for converting between Pojo and Map.</p><p class="ngde">The design philosophy behind <em class="ngde">Kronos</em> is to address the shortcomings of existing ORM frameworks and provide a more convenient and efficient data operation experience based on coroutines and task mechanisms.</p><pre class="ngde hljs"><code class="hljs language-kotlin code-lines ngde" lang="kotlin" name="demo" icon="kotlin" highlightedlines="[]"><span class="line ngde"><span class="hljs-keyword ngde">if</span>(!db.table.exsits&#x3C;User>()){
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
</span></code></pre><ng-doc-demo componentname="FeatureCardsComponent" indexable="false" class="ngde"><div id="options" class="ngde">{"container":false}</div></ng-doc-demo>`,O=(()=>{let s=class s extends l{constructor(){super(),this.routePrefix="",this.pageType="guide",this.pageContent=v,this.page=n,this.demoAssets=f}};s.\u0275fac=function(e){return new(e||s)},s.\u0275cmp=t({type:s,selectors:[["ng-doc-page-en-getting-started-welcome"]],standalone:!0,features:[r([{provide:l,useExisting:s},u,n.providers??[]]),c,h],decls:1,vars:0,template:function(e,M){e&1&&o(0,"ng-doc-page")},dependencies:[m],encapsulation:2,changeDetection:0});let a=s;return a})(),R=[g(d({},(0,y.isRoute)(n.route)?n.route:{}),{path:"",component:O,title:"Welcome & Introduction"})],I=R;export{O as DynamicComponent,I as default};
