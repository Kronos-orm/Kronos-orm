import{a as i}from"./chunk-ZFGADIWI.js";import{a as t}from"./chunk-CN3B2NJM.js";import{a as l}from"./chunk-TKZ7FAYE.js";import{G as w}from"./chunk-JSUCXAVI.js";import{Pb as g,jc as r,kc as h,ra as o,xb as c}from"./chunk-TLBD5JYT.js";import{a as p,b as d,g as u}from"./chunk-ODN5LVDJ.js";var y=u(w());var N={title:"\u7EA7\u8054\u66F4\u65B0",mdFile:"./index.md",category:i,order:3},a=N;var j=[];var S={},m=S;var D='<h1 id="\u7EA7\u8054\u66F4\u65B0" class="ngde">\u7EA7\u8054\u66F4\u65B0<a title="Link to heading" class="ng-doc-header-link ngde" href="/zh-CN/advanced/reference-update#\u7EA7\u8054\u66F4\u65B0"><ng-doc-icon icon="link-2" size="16" class="ngde"></ng-doc-icon></a></h1><h2 id="\u914D\u7F6E\u7EA7\u8054\u5173\u7CFB" class="ngde">\u914D\u7F6E\u7EA7\u8054\u5173\u7CFB<a title="Link to heading" class="ng-doc-header-link ngde" href="/zh-CN/advanced/reference-update#\u914D\u7F6E\u7EA7\u8054\u5173\u7CFB"><ng-doc-icon icon="link-2" size="16" class="ngde"></ng-doc-icon></a></h2><p class="ngde">\u901A\u8FC7\u914D\u7F6E<code class="ngde">KPojo</code>\u7684<a href="/documentation/zh-CN/class-definition/table-class-definition#\u5217\u5173\u8054\u8BBE\u7F6E" class="ngde">[\u5217\u5173\u8054\u8BBE\u7F6E]</a>\uFF0C\u6307\u5B9A\u5173\u8054\u5B57\u6BB5\u5173\u8054\u4FE1\u606F\uFF08<code class="ngde">@Reference</code>\uFF09\u4E2D<code class="ngde">usage</code>\u5C5E\u6027\u5305\u542B<code class="ngde">Update</code>\uFF08\u6216\u4E0D\u6307\u5B9A\uFF0C\u4F7F\u7528\u9ED8\u8BA4\uFF09\uFF0C\u5373\u53EF\u5F00\u542F\u8BE5\u7C7B\uFF08\u88AB\uFF09\u7EA7\u8054\u66F4\u65B0\u7684\u529F\u80FD\u3002</p><pre class="ngde hljs"><code class="hljs language-kotlin code-lines ngde" lang="kotlin" name="kotlin" icon="kotlin" highlightedlines="[6,14,15,16,17,18,26,27,28,29,30,31,32,33]"><span class="line ngde"><span class="hljs-meta ngde">@Table(<span class="hljs-string ngde">"school"</span>)</span>\n</span><span class="line ngde"><span class="hljs-keyword ngde">data</span> <span class="hljs-keyword ngde">class</span> <span class="hljs-title class_ ngde">School</span>(\n</span><span class="line ngde">    <span class="hljs-meta ngde">@PrimaryKey(identity = true)</span>\n</span><span class="line ngde">    <span class="hljs-keyword ngde">var</span> id: <span class="hljs-built_in ngde">Int</span>? = <span class="hljs-literal ngde">null</span>,\n</span><span class="line ngde">    <span class="hljs-keyword ngde">var</span> name: String? = <span class="hljs-literal ngde">null</span>,\n</span><span class="line highlighted ngde">    <span class="hljs-keyword ngde">var</span> groupClass: List&#x3C;GroupClass>? = <span class="hljs-literal ngde">null</span>\n</span><span class="line ngde">) : KPojo\n</span><span class="line ngde">\n</span><span class="line ngde"><span class="hljs-meta ngde">@Table(<span class="hljs-string ngde">"group_class"</span>)</span>\n</span><span class="line ngde"><span class="hljs-keyword ngde">data</span> <span class="hljs-keyword ngde">class</span> <span class="hljs-title class_ ngde">GroupClass</span>(\n</span><span class="line ngde">    <span class="hljs-meta ngde">@PrimaryKey(identity = true)</span>\n</span><span class="line ngde">    <span class="hljs-keyword ngde">var</span> id: <span class="hljs-built_in ngde">Int</span>? = <span class="hljs-literal ngde">null</span>,\n</span><span class="line ngde">    <span class="hljs-keyword ngde">val</span> name: String? = <span class="hljs-literal ngde">null</span>,\n</span><span class="line highlighted ngde">    <span class="hljs-meta ngde">@NotNull</span>\n</span><span class="line highlighted ngde">    <span class="hljs-keyword ngde">var</span> schoolName: String? = <span class="hljs-literal ngde">null</span>,\n</span><span class="line highlighted ngde">    <span class="hljs-meta ngde">@Reference([<span class="hljs-string ngde">"schoolName"</span>], [<span class="hljs-string ngde">"name"</span>], mapperBy = School::class)</span>\n</span><span class="line highlighted ngde">    <span class="hljs-keyword ngde">var</span> school: School? = <span class="hljs-literal ngde">null</span>,\n</span><span class="line highlighted ngde">    <span class="hljs-keyword ngde">var</span> students: List&#x3C;Student>? = <span class="hljs-literal ngde">null</span>\n</span><span class="line ngde">) : KPojo\n</span><span class="line ngde">\n</span><span class="line ngde"><span class="hljs-meta ngde">@Table(<span class="hljs-string ngde">"student"</span>)</span>\n</span><span class="line ngde"><span class="hljs-keyword ngde">data</span> <span class="hljs-keyword ngde">class</span> <span class="hljs-title class_ ngde">Student</span>(\n</span><span class="line ngde">    <span class="hljs-meta ngde">@PrimaryKey(identity = true)</span>\n</span><span class="line ngde">    <span class="hljs-keyword ngde">var</span> id: <span class="hljs-built_in ngde">Int</span>? = <span class="hljs-literal ngde">null</span>,\n</span><span class="line ngde">    <span class="hljs-keyword ngde">var</span> name: String? = <span class="hljs-literal ngde">null</span>,\n</span><span class="line highlighted ngde">    <span class="hljs-keyword ngde">var</span> schoolName: String? = <span class="hljs-literal ngde">null</span>,\n</span><span class="line highlighted ngde">    <span class="hljs-keyword ngde">var</span> groupClassName: String? = <span class="hljs-literal ngde">null</span>,\n</span><span class="line highlighted ngde">    <span class="hljs-meta ngde">@Reference(</span>\n</span><span class="line highlighted ngde"><span class="hljs-meta ngde">        [</span><span class="hljs-string ngde">"groupClassName"</span>, <span class="hljs-string ngde">"schoolName"</span><span class="hljs-meta ngde">],</span>\n</span><span class="line highlighted ngde"><span class="hljs-meta ngde">        [</span><span class="hljs-string ngde">"name"</span>, <span class="hljs-string ngde">"schoolName"</span><span class="hljs-meta ngde">],</span>\n</span><span class="line highlighted ngde"><span class="hljs-meta ngde">        mapperBy = GroupClass::class</span>\n</span><span class="line highlighted ngde"><span class="hljs-meta ngde">    )</span>\n</span><span class="line highlighted ngde">    <span class="hljs-keyword ngde">var</span> groupClass: GroupClass? = <span class="hljs-literal ngde">null</span>\n</span><span class="line ngde">) : KPojo\n</span></code></pre><h2 id="\u4F7F\u7528cascade\u8BBE\u7F6E\u5F53\u524D\u7EA7\u8054\u64CD\u4F5C" class="ngde">\u4F7F\u7528<span style="color: #DD6666" class="ngde">cascade</span>\u8BBE\u7F6E\u5F53\u524D\u7EA7\u8054\u64CD\u4F5C<a title="Link to heading" class="ng-doc-header-link ngde" href="/zh-CN/advanced/reference-update#\u4F7F\u7528cascade\u8BBE\u7F6E\u5F53\u524D\u7EA7\u8054\u64CD\u4F5C"><ng-doc-icon icon="link-2" size="16" class="ngde"></ng-doc-icon></a></h2><p class="ngde">\u5728Kronos\u4E2D\uFF0C\u6211\u4EEC\u53EF\u4EE5\u4F7F\u7528<code class="ngde">cascade</code>\u65B9\u6CD5\u8BBE\u7F6E\u662F\u5426\u5F00\u542F\u672C\u6B21\u66F4\u65B0\u7684\u7EA7\u8054\u529F\u80FD\u5E76\u9650\u5236\u7EA7\u8054\u7684\u6700\u5927\u5C42\u6570</p><p class="ngde"><code class="ngde">KPojo.update().cascade().excute()</code></p><ul class="ngde"><li class="ngde"><code class="ngde">enabled</code>\uFF1A <code class="ngde">Boolean</code> \u624B\u52A8\u8BBE\u7F6E\u662F\u5426\u5F00\u542F\u672C\u6B21\u66F4\u65B0\u7684\u7EA7\u8054\u529F\u80FD\uFF08\u53EF\u9009\uFF0C\u9ED8\u8BA4\u4E3A<code class="ngde">true</code>\u5F00\u542F\u7EA7\u8054\uFF09</li><li class="ngde"><code class="ngde">depth</code>\uFF1A <code class="ngde">Int</code> \u9650\u5236\u7EA7\u8054\u7684\u6700\u5927\u5C42\u6570\uFF0C\u9ED8\u8BA4\u4E3A<code class="ngde">-1</code>\uFF0C\u5373\u4E0D\u9650\u5236\u7EA7\u8054\u5C42\u6570\uFF0C <code class="ngde">0</code>\u8868\u793A\u4E0D\u8FDB\u884C\u7EA7\u8054\u66F4\u65B0</li></ul><h2 id="\u4F7F\u7528update\u53CA\u76F8\u5173\u65B9\u6CD5\u8FDB\u884C\u7EA7\u8054\u66F4\u65B0\u64CD\u4F5C" class="ngde">\u4F7F\u7528<span style="color: #DD6666" class="ngde">update</span>\u53CA\u76F8\u5173\u65B9\u6CD5\u8FDB\u884C\u7EA7\u8054\u66F4\u65B0\u64CD\u4F5C<a title="Link to heading" class="ng-doc-header-link ngde" href="/zh-CN/advanced/reference-update#\u4F7F\u7528update\u53CA\u76F8\u5173\u65B9\u6CD5\u8FDB\u884C\u7EA7\u8054\u66F4\u65B0\u64CD\u4F5C"><ng-doc-icon icon="link-2" size="16" class="ngde"></ng-doc-icon></a></h2><p class="ngde">\u7EA7\u8054\u66F4\u65B0\u7684\u5404\u65B9\u6CD5\u4E0E\u64CD\u4F5C\u540C<a href="/documentation/zh-CN/database/update-records" class="ngde">\u66F4\u65B0\u8BB0\u5F55</a>\u76F8\u5173\u65B9\u6CD5\u4E0E\u64CD\u4F5C\u57FA\u672C\u4E00\u81F4\u3002</p><ng-doc-tab group="Case 1" name="kotlin" icon="kotlin" class="ngde"><pre class="ngde hljs"><code class="hljs language-kotlin code-lines ngde" lang="kotlin" name="" icon="" highlightedlines="[7,8,9,10,11]"><span class="line ngde">School(name = <span class="hljs-string ngde">"School"</span>).update().<span class="hljs-keyword ngde">set</span> { it.name = <span class="hljs-string ngde">"School2"</span> }.execute()\n</span></code></pre></ng-doc-tab><ng-doc-tab group="Case 1" name="Mysql" icon="mysql" class="ngde"><pre class="ngde hljs"><code class="hljs language-sql code-lines ngde" lang="sql" name="" icon="" highlightedlines="[]"><span class="line ngde"><span class="hljs-keyword ngde">UPDATE</span> `student` <span class="hljs-keyword ngde">SET</span> `school_name` <span class="hljs-operator ngde">=</span> "School2" <span class="hljs-keyword ngde">WHERE</span> `id` <span class="hljs-operator ngde">=</span> :id <span class="hljs-keyword ngde">AND</span> `name` <span class="hljs-operator ngde">=</span> :name <span class="hljs-keyword ngde">AND</span> `student_no` <span class="hljs-operator ngde">=</span> :studentNo <span class="hljs-keyword ngde">AND</span> `school_name` <span class="hljs-operator ngde">=</span> "School" <span class="hljs-keyword ngde">AND</span> `group_class_name` <span class="hljs-operator ngde">=</span> :groupClassName\n</span><span class="line ngde">\n</span><span class="line ngde"><span class="hljs-keyword ngde">UPDATE</span> `group_class` <span class="hljs-keyword ngde">SET</span> `school_name` <span class="hljs-operator ngde">=</span> "School2" <span class="hljs-keyword ngde">WHERE</span> `id` <span class="hljs-operator ngde">=</span> :id <span class="hljs-keyword ngde">AND</span> `name` <span class="hljs-operator ngde">=</span> :name <span class="hljs-keyword ngde">AND</span> `school_name` <span class="hljs-operator ngde">=</span> "School"\n</span><span class="line ngde">\n</span><span class="line ngde"><span class="hljs-keyword ngde">UPDATE</span> `school` <span class="hljs-keyword ngde">SET</span> `name` <span class="hljs-operator ngde">=</span> "School2" <span class="hljs-keyword ngde">WHERE</span> `id` <span class="hljs-operator ngde">=</span> :id <span class="hljs-keyword ngde">AND</span> `name` <span class="hljs-operator ngde">=</span> "School"\n</span></code></pre></ng-doc-tab><ng-doc-tab group="Case 1" name="PostgreSQL" icon="postgres" class="ngde"><pre class="ngde hljs"><code class="hljs language-sql code-lines ngde" lang="sql" name="" icon="" highlightedlines="[]"><span class="line ngde"><span class="hljs-keyword ngde">UPDATE</span> "student" <span class="hljs-keyword ngde">SET</span> "school_name" <span class="hljs-operator ngde">=</span> "School2" <span class="hljs-keyword ngde">WHERE</span> "id" <span class="hljs-operator ngde">=</span> :id <span class="hljs-keyword ngde">AND</span> "name" <span class="hljs-operator ngde">=</span> :name <span class="hljs-keyword ngde">AND</span> "student_no" <span class="hljs-operator ngde">=</span> :studentNo <span class="hljs-keyword ngde">AND</span> "school_name" <span class="hljs-operator ngde">=</span> "School" <span class="hljs-keyword ngde">AND</span> "group_class_name" <span class="hljs-operator ngde">=</span> :groupClassName\n</span><span class="line ngde">\n</span><span class="line ngde"><span class="hljs-keyword ngde">UPDATE</span> "group_class" <span class="hljs-keyword ngde">SET</span> "school_name" <span class="hljs-operator ngde">=</span> "School2" <span class="hljs-keyword ngde">WHERE</span> "id" <span class="hljs-operator ngde">=</span> :id <span class="hljs-keyword ngde">AND</span> "name" <span class="hljs-operator ngde">=</span> :name <span class="hljs-keyword ngde">AND</span> "school_name" <span class="hljs-operator ngde">=</span> "School"\n</span><span class="line ngde">\n</span><span class="line ngde"><span class="hljs-keyword ngde">UPDATE</span> "school" <span class="hljs-keyword ngde">SET</span> "name" <span class="hljs-operator ngde">=</span> "School2" <span class="hljs-keyword ngde">WHERE</span> "id" <span class="hljs-operator ngde">=</span> :id <span class="hljs-keyword ngde">AND</span> "name" <span class="hljs-operator ngde">=</span> "School"\n</span></code></pre></ng-doc-tab><ng-doc-tab group="Case 1" name="SQLite" icon="sqlite" class="ngde"><pre class="ngde hljs"><code class="hljs language-sql code-lines ngde" lang="sql" name="" icon="" highlightedlines="[]"><span class="line ngde"><span class="hljs-keyword ngde">UPDATE</span> `student` <span class="hljs-keyword ngde">SET</span> `school_name` <span class="hljs-operator ngde">=</span> "School2" <span class="hljs-keyword ngde">WHERE</span> `id` <span class="hljs-operator ngde">=</span> :id <span class="hljs-keyword ngde">AND</span> `name` <span class="hljs-operator ngde">=</span> :name <span class="hljs-keyword ngde">AND</span> `student_no` <span class="hljs-operator ngde">=</span> :studentNo <span class="hljs-keyword ngde">AND</span> `school_name` <span class="hljs-operator ngde">=</span> "School" <span class="hljs-keyword ngde">AND</span> `group_class_name` <span class="hljs-operator ngde">=</span> :groupClassName\n</span><span class="line ngde">\n</span><span class="line ngde"><span class="hljs-keyword ngde">UPDATE</span> `group_class` <span class="hljs-keyword ngde">SET</span> `school_name` <span class="hljs-operator ngde">=</span> "School2" <span class="hljs-keyword ngde">WHERE</span> `id` <span class="hljs-operator ngde">=</span> :id <span class="hljs-keyword ngde">AND</span> `name` <span class="hljs-operator ngde">=</span> :name <span class="hljs-keyword ngde">AND</span> `school_name` <span class="hljs-operator ngde">=</span> "School"\n</span><span class="line ngde">\n</span><span class="line ngde"><span class="hljs-keyword ngde">UPDATE</span> `school` <span class="hljs-keyword ngde">SET</span> `name` <span class="hljs-operator ngde">=</span> "School2" <span class="hljs-keyword ngde">WHERE</span> `id` <span class="hljs-operator ngde">=</span> :id <span class="hljs-keyword ngde">AND</span> `name` <span class="hljs-operator ngde">=</span> "School"\n</span></code></pre></ng-doc-tab><ng-doc-tab group="Case 1" name="SQLServer" icon="sqlserver" class="ngde"><pre class="ngde hljs"><code class="hljs language-sql code-lines ngde" lang="sql" name="" icon="" highlightedlines="[]"><span class="line ngde"><span class="hljs-keyword ngde">UPDATE</span> [student] <span class="hljs-keyword ngde">SET</span> [school_name] <span class="hljs-operator ngde">=</span> "School2" <span class="hljs-keyword ngde">WHERE</span> [id] <span class="hljs-operator ngde">=</span> :id <span class="hljs-keyword ngde">AND</span> [name] <span class="hljs-operator ngde">=</span> :name <span class="hljs-keyword ngde">AND</span> [student_no] <span class="hljs-operator ngde">=</span> :studentNo <span class="hljs-keyword ngde">AND</span> [school_name] <span class="hljs-operator ngde">=</span> "School" <span class="hljs-keyword ngde">AND</span> [group_class_name] <span class="hljs-operator ngde">=</span> :groupClassName\n</span><span class="line ngde">\n</span><span class="line ngde"><span class="hljs-keyword ngde">UPDATE</span> [group_class] <span class="hljs-keyword ngde">SET</span> [school_name] <span class="hljs-operator ngde">=</span> "School2" <span class="hljs-keyword ngde">WHERE</span> [id] <span class="hljs-operator ngde">=</span> :id <span class="hljs-keyword ngde">AND</span> [name] <span class="hljs-operator ngde">=</span> :name <span class="hljs-keyword ngde">AND</span> [school_name] <span class="hljs-operator ngde">=</span> "School"\n</span><span class="line ngde">\n</span><span class="line ngde"><span class="hljs-keyword ngde">UPDATE</span> [school] <span class="hljs-keyword ngde">SET</span> [name] <span class="hljs-operator ngde">=</span> "School2" <span class="hljs-keyword ngde">WHERE</span> [id] <span class="hljs-operator ngde">=</span> :id <span class="hljs-keyword ngde">AND</span> [name] <span class="hljs-operator ngde">=</span> "School"\n</span></code></pre></ng-doc-tab><ng-doc-tab group="Case 1" name="Oracle" icon="oracle" class="ngde"><pre class="ngde hljs"><code class="hljs language-sql code-lines ngde" lang="sql" name="" icon="" highlightedlines="[]"><span class="line ngde"><span class="hljs-keyword ngde">UPDATE</span> "student" <span class="hljs-keyword ngde">SET</span> "school_name" <span class="hljs-operator ngde">=</span> "School2" <span class="hljs-keyword ngde">WHERE</span> "id" <span class="hljs-operator ngde">=</span> :id <span class="hljs-keyword ngde">AND</span> "name" <span class="hljs-operator ngde">=</span> :name <span class="hljs-keyword ngde">AND</span> "student_no" <span class="hljs-operator ngde">=</span> :studentNo <span class="hljs-keyword ngde">AND</span> "school_name" <span class="hljs-operator ngde">=</span> "School" <span class="hljs-keyword ngde">AND</span> "group_class_name" <span class="hljs-operator ngde">=</span> :groupClassName\n</span><span class="line ngde">\n</span><span class="line ngde"><span class="hljs-keyword ngde">UPDATE</span> "group_class" <span class="hljs-keyword ngde">SET</span> "school_name" <span class="hljs-operator ngde">=</span> "School2" <span class="hljs-keyword ngde">WHERE</span> "id" <span class="hljs-operator ngde">=</span> :id <span class="hljs-keyword ngde">AND</span> "name" <span class="hljs-operator ngde">=</span> :name <span class="hljs-keyword ngde">AND</span> "school_name" <span class="hljs-operator ngde">=</span> "School"\n</span><span class="line ngde">\n</span><span class="line ngde"><span class="hljs-keyword ngde">UPDATE</span> "school" <span class="hljs-keyword ngde">SET</span> "name" <span class="hljs-operator ngde">=</span> "School2" <span class="hljs-keyword ngde">WHERE</span> "id" <span class="hljs-operator ngde">=</span> :id <span class="hljs-keyword ngde">AND</span> "name" <span class="hljs-operator ngde">=</span> "School"\n</span></code></pre></ng-doc-tab>',E=(()=>{let s=class s extends l{constructor(){super(),this.routePrefix="",this.pageType="guide",this.pageContent=D,this.page=a,this.demoAssets=m}};s.\u0275fac=function(e){return new(e||s)},s.\u0275cmp=o({type:s,selectors:[["ng-doc-page-zh-cn-advanced-reference-update"]],standalone:!0,features:[r([{provide:l,useExisting:s},j,a.providers??[]]),c,h],decls:1,vars:0,template:function(e,_){e&1&&g(0,"ng-doc-page")},dependencies:[t],encapsulation:2,changeDetection:0});let n=s;return n})(),A=[d(p({},(0,y.isRoute)(a.route)?a.route:{}),{path:"",component:E,title:"\u7EA7\u8054\u66F4\u65B0"})],W=A;export{E as DynamicComponent,W as default};
