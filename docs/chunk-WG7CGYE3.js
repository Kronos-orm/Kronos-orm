import{a as h}from"./chunk-FJ26F7HW.js";import{a as g}from"./chunk-4FO7WJ36.js";import{a as l}from"./chunk-LVHT47OW.js";import{F as x}from"./chunk-LWH7SIW3.js";import{Pb as t,jc as p,kc as r,ra as i,xb as c}from"./chunk-TLBD5JYT.js";import{a as d,b as o,g as f}from"./chunk-ODN5LVDJ.js";var k=f(x());var b={title:"Insert Records",mdFile:"./index.md",category:h,order:2},e=b;var u=[];var P={},m=P;var R=`<h1 id="insert-records" class="ngde">Insert Records<a title="Link to heading" class="ng-doc-header-link ngde" href="/en/database/insert-records#insert-records"><ng-doc-icon icon="link-2" size="16" class="ngde"></ng-doc-icon></a></h1><h2 id="\u63D2\u5165\u5355\u6761\u8BB0\u5F55" class="ngde">\u63D2\u5165\u5355\u6761\u8BB0\u5F55<a title="Link to heading" class="ng-doc-header-link ngde" href="/en/database/insert-records#\u63D2\u5165\u5355\u6761\u8BB0\u5F55"><ng-doc-icon icon="link-2" size="16" class="ngde"></ng-doc-icon></a></h2><p class="ngde">\u5728Kronos\u4E2D\uFF0C\u6211\u4EEC\u53EF\u4EE5\u4F7F\u7528<code class="ngde">KPojo.insert().execute()</code>\u65B9\u6CD5\u5411\u6570\u636E\u5E93\u4E2D\u63D2\u5165\u4E00\u6761\u8BB0\u5F55\u3002</p><pre class="ngde hljs"><code class="hljs language-kotlin code-lines ngde" lang="kotlin" name="demo" icon="kotlin" highlightedlines="[7]"><span class="line ngde"><span class="hljs-keyword ngde">val</span> user: User = User(
</span><span class="line ngde">        id = <span class="hljs-number ngde">1</span>,
</span><span class="line ngde">        name = <span class="hljs-string ngde">"Kronos"</span>,
</span><span class="line ngde">        age = <span class="hljs-number ngde">18</span>
</span><span class="line ngde">    )
</span><span class="line ngde">
</span><span class="line highlighted ngde">user.insert().execute()
</span></code></pre><h2 id="\u81EA\u589E\u4E3B\u952E\u548C\u5F71\u54CD\u884C\u6570" class="ngde">\u81EA\u589E\u4E3B\u952E\u548C\u5F71\u54CD\u884C\u6570<a title="Link to heading" class="ng-doc-header-link ngde" href="/en/database/insert-records#\u81EA\u589E\u4E3B\u952E\u548C\u5F71\u54CD\u884C\u6570"><ng-doc-icon icon="link-2" size="16" class="ngde"></ng-doc-icon></a></h2><p class="ngde">\u5728Kronos\u4E2D\uFF0C\u6211\u4EEC\u53EF\u4EE5\u4F7F\u7528<code class="ngde">KPojo.insert().execute()</code>\u65B9\u6CD5\u5411\u6570\u636E\u5E93\u4E2D\u63D2\u5165\u4E00\u6761\u8BB0\u5F55\uFF0C\u5F53\u4E3B\u952E\u4E3A\u81EA\u589E\u65F6\uFF0CKronos\u4F1A\u81EA\u52A8\u83B7\u53D6\u81EA\u589E\u4E3B\u952E\u7684\u503C\u3002</p><pre class="ngde hljs"><code class="hljs language-kotlin code-lines ngde" lang="kotlin" name="demo" icon="kotlin" highlightedlines="[6]"><span class="line ngde"><span class="hljs-keyword ngde">val</span> user: User = User(
</span><span class="line ngde">        name = <span class="hljs-string ngde">"Kronos"</span>,
</span><span class="line ngde">        age = <span class="hljs-number ngde">18</span>
</span><span class="line ngde">    )
</span><span class="line ngde">    
</span><span class="line highlighted ngde"><span class="hljs-keyword ngde">val</span> (affectRows, lastInsertId) = user.insert().execute()
</span></code></pre><h2 id="\u6279\u91CF\u63D2\u5165\u8BB0\u5F55" class="ngde">\u6279\u91CF\u63D2\u5165\u8BB0\u5F55<a title="Link to heading" class="ng-doc-header-link ngde" href="/en/database/insert-records#\u6279\u91CF\u63D2\u5165\u8BB0\u5F55"><ng-doc-icon icon="link-2" size="16" class="ngde"></ng-doc-icon></a></h2><p class="ngde">\u5728Kronos\u4E2D\uFF0C\u6211\u4EEC\u53EF\u4EE5\u4F7F\u7528<code class="ngde">Iterable&#x3C;KPojo>.insert().execute()</code>\u6216<code class="ngde">Array&#x3C;KPojo>.insert().execute()</code>\u65B9\u6CD5\u5411\u6570\u636E\u5E93\u4E2D\u6279\u91CF\u63D2\u5165\u8BB0\u5F55\u3002</p><pre class="ngde hljs"><code class="hljs language-kotlin code-lines ngde" lang="kotlin" name="demo" icon="kotlin" highlightedlines="[14]"><span class="line ngde"><span class="hljs-keyword ngde">val</span> users: List&#x3C;User> = listOf(
</span><span class="line ngde">    User(
</span><span class="line ngde">        id = <span class="hljs-number ngde">1</span>,
</span><span class="line ngde">        name = <span class="hljs-string ngde">"Kronos"</span>,
</span><span class="line ngde">        age = <span class="hljs-number ngde">18</span>
</span><span class="line ngde">    ),
</span><span class="line ngde">    User(
</span><span class="line ngde">        id = <span class="hljs-number ngde">2</span>,
</span><span class="line ngde">        name = <span class="hljs-string ngde">"Kronos ORM"</span>,
</span><span class="line ngde">        age = <span class="hljs-number ngde">18</span>
</span><span class="line ngde">    )
</span><span class="line ngde">)
</span><span class="line ngde">
</span><span class="line highlighted ngde">users.insert().execute()
</span></code></pre><h2 id="\u6307\u5B9A\u4F7F\u7528\u7684\u6570\u636E\u6E90" class="ngde">\u6307\u5B9A\u4F7F\u7528\u7684\u6570\u636E\u6E90<a title="Link to heading" class="ng-doc-header-link ngde" href="/en/database/insert-records#\u6307\u5B9A\u4F7F\u7528\u7684\u6570\u636E\u6E90"><ng-doc-icon icon="link-2" size="16" class="ngde"></ng-doc-icon></a></h2><p class="ngde">\u5728Kronos\u4E2D\uFF0C\u6211\u4EEC\u53EF\u4EE5\u5C06<code class="ngde">KronosDataSourceWrapper</code>\u4F20\u5165<code class="ngde">execute</code>\u65B9\u6CD5\uFF0C\u4EE5\u5B9E\u73B0\u81EA\u5B9A\u4E49\u7684\u6570\u636E\u5E93\u8FDE\u63A5\u3002</p><pre class="ngde hljs"><code class="hljs language-kotlin code-lines ngde" lang="kotlin" name="demo" icon="kotlin" highlightedlines="[9]"><span class="line ngde"><span class="hljs-keyword ngde">val</span> customWrapper = CustomWrapper()
</span><span class="line ngde">
</span><span class="line ngde"><span class="hljs-keyword ngde">val</span> user: User = User(
</span><span class="line ngde">        id = <span class="hljs-number ngde">1</span>,
</span><span class="line ngde">        name = <span class="hljs-string ngde">"Kronos"</span>,
</span><span class="line ngde">        age = <span class="hljs-number ngde">18</span>
</span><span class="line ngde">    )
</span><span class="line ngde">    
</span><span class="line highlighted ngde">user.insert().execute(customWrapper)
</span></code></pre>`,y=(()=>{let s=class s extends l{constructor(){super(),this.routePrefix="",this.pageType="guide",this.pageContent=R,this.page=e,this.demoAssets=m}};s.\u0275fac=function(a){return new(a||s)},s.\u0275cmp=i({type:s,selectors:[["ng-doc-page-en-database-insert-records"]],standalone:!0,features:[p([{provide:l,useExisting:s},u,e.providers??[]]),c,r],decls:1,vars:0,template:function(a,K){a&1&&t(0,"ng-doc-page")},dependencies:[g],encapsulation:2,changeDetection:0});let n=s;return n})(),D=[o(d({},(0,k.isRoute)(e.route)?e.route:{}),{path:"",component:y,title:"Insert Records"})],E=D;export{y as DynamicComponent,E as default};
