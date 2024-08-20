import{a as r}from"./chunk-5CP4K3WD.js";import{a as p}from"./chunk-CN3B2NJM.js";import{a as l}from"./chunk-TKZ7FAYE.js";import{G as f}from"./chunk-JSUCXAVI.js";import{Pb as o,jc as t,kc as h,ra as g,xb as i}from"./chunk-TLBD5JYT.js";import{a as c,b as d,g as u}from"./chunk-ODN5LVDJ.js";var j=u(f());var b={title:"Criteria \u6761\u4EF6",mdFile:"./index.md",category:r,order:9},s=b;var m=[];var y={},k=y;var C=`<h1 id="criteria-\u6761\u4EF6" class="ngde">Criteria \u6761\u4EF6<a title="Link to heading" class="ng-doc-header-link ngde" href="/zh-CN/database/where-having-on-clause#criteria-\u6761\u4EF6"><ng-doc-icon icon="link-2" size="16" class="ngde"></ng-doc-icon></a></h1><h2 id="1criteria-\u6761\u4EF6\u5BF9\u8C61" class="ngde">1.Criteria \u6761\u4EF6\u5BF9\u8C61<a title="Link to heading" class="ng-doc-header-link ngde" href="/zh-CN/database/where-having-on-clause#1criteria-\u6761\u4EF6\u5BF9\u8C61"><ng-doc-icon icon="link-2" size="16" class="ngde"></ng-doc-icon></a></h2><p class="ngde">Kronos\u4F7F\u7528Criteria\u5BF9\u8C61\u6784\u5EFA\u6761\u4EF6\u8868\u8FBE\u5F0F\uFF0C\u5E76\u4E14\u652F\u6301\u590D\u6742\u7684\u6761\u4EF6\u7EC4\u5408\uFF0C\u5982<code class="ngde">and</code>\u3001<code class="ngde">or</code>\u3001<code class="ngde">not</code>\u7B49\uFF0C\u7528\u4E8E<code class="ngde">where</code>\u3001<code class="ngde">having</code>\u3001<code class="ngde">on</code>\u7B49\u6761\u4EF6\u4E2D\u3002</p><p class="ngde">\u4F60\u53EF\u4EE5\u4F7F\u7528where\u6761\u4EF6\u5BF9\u8C61\u7EC4\u6210\u590D\u6742\u7684\u67E5\u8BE2\u6761\u4EF6\u5728<code class="ngde">select</code>\u3001<code class="ngde">delete</code>\u3001<code class="ngde">update</code>\u3001<code class="ngde">join</code>\u4E2D\u4F7F\u7528\uFF0C\u5F62\u5982</p><pre class="ngde hljs"><code class="hljs language-kotlin code-lines ngde" lang="kotlin" name="" icon="" highlightedlines="[]"><span class="line ngde"><span class="hljs-keyword ngde">val</span> list = user
</span><span class="line ngde">    .select { it.name }
</span><span class="line ngde">    .<span class="hljs-keyword ngde">where</span> { (it.id == <span class="hljs-number ngde">1</span> || it.age > <span class="hljs-number ngde">18</span>) &#x26;&#x26; it.name like <span class="hljs-string ngde">"Kronos%"</span> }
</span><span class="line ngde">    .queryList&#x3C;String>()
</span></code></pre><p class="ngde">\u57FA\u4E8EKCP\uFF0CKronos\u5141\u8BB8\u4F60\u4F7F\u7528\u771F\u5B9E\u7684kotlin\u64CD\u4F5C\u7B26\u6765\u6784\u5EFA<code class="ngde">Criteria</code>\u67E5\u8BE2\u6761\u4EF6\uFF0C\u5982<code class="ngde">==</code>\u3001<code class="ngde">!=</code>\u3001<code class="ngde">></code>\u3001<code class="ngde">&#x3C;</code>\u3001<code class="ngde">>=</code>\u3001<code class="ngde">&#x3C;=</code>\u3001<code class="ngde">in</code>\u3001<code class="ngde">||</code> \u3001<code class="ngde">&#x26;&#x26;</code>\u7B49\uFF0C\u800C\u4E0D\u662F\u5176\u4ED6\u6846\u67B6\u4E2D<code class="ngde">eq</code>\u3001<code class="ngde">ne</code>\u3001<code class="ngde">gt</code>\u3001<code class="ngde">lt</code>\u3001<code class="ngde">ge</code>\u3001<code class="ngde">le</code>\u7B49\u81EA\u5B9A\u4E49\u64CD\u4F5C\u7B26\uFF0C\u800C\u4ECE\u8868\u8FBE\u5F0F\u5230Criteria\u5BF9\u8C61\u7684\u8F6C\u6362\u662F\u5728\u7F16\u8BD1\u671F\u5B8C\u6210\u7684\u3002</p><p class="ngde">kronos\u8868\u8FBE\u5F0F\u652F\u6301<strong class="ngde">\u52A8\u6001\u6784\u5EFA\u6761\u4EF6</strong>\u548C\u6839\u636E\u5BF9\u8C61\u7684\u5C5E\u6027<strong class="ngde">\u81EA\u52A8\u751F\u6210\u6761\u4EF6</strong>\uFF0C\u5E76\u4E14\u652F\u6301\u4F20\u5165<strong class="ngde">sql\u5B57\u7B26\u4E32</strong>\u4F5C\u4E3A\u6761\u4EF6\u3002</p><p class="ngde">\u4F7F\u7528kronos\u5C31\u50CF\u5728\u5199\u539F\u751F\u7684kotlin\u4EE3\u7801\u4E00\u6837\uFF0C\u8FD9\u5C06\u5927\u5927<strong class="ngde">\u63D0\u9AD8\u5F00\u53D1\u6548\u7387</strong>\u548C<strong class="ngde">\u964D\u4F4E\u5B66\u4E60\u6210\u672C\u53CA\u5FC3\u667A\u8D1F\u62C5</strong>\u3002</p><h2 id="2\u652F\u6301\u7684\u51FD\u6570\u548C\u64CD\u4F5C\u7B26" class="ngde">2.\u652F\u6301\u7684\u51FD\u6570\u548C\u64CD\u4F5C\u7B26<a title="Link to heading" class="ng-doc-header-link ngde" href="/zh-CN/database/where-having-on-clause#2\u652F\u6301\u7684\u51FD\u6570\u548C\u64CD\u4F5C\u7B26"><ng-doc-icon icon="link-2" size="16" class="ngde"></ng-doc-icon></a></h2><p class="ngde">Kronos\u652F\u6301\u7684\u51FD\u6570\u548C\u64CD\u4F5C\u7B26\u5982\u4E0B\uFF1A</p><h3 id="21\u64CD\u4F5C\u7B26" class="ngde">2.1.\u64CD\u4F5C\u7B26<a title="Link to heading" class="ng-doc-header-link ngde" href="/zh-CN/database/where-having-on-clause#21\u64CD\u4F5C\u7B26"><ng-doc-icon icon="link-2" size="16" class="ngde"></ng-doc-icon></a></h3><ul class="ngde"><li class="ngde"><code class="ngde">==</code>\uFF1A\u7B49\u4E8E</li><li class="ngde"><code class="ngde">!=</code>\uFF1A\u4E0D\u7B49\u4E8E</li><li class="ngde"><code class="ngde">></code>\uFF1A\u5927\u4E8E</li><li class="ngde"><code class="ngde">&#x3C;</code>\uFF1A\u5C0F\u4E8E</li><li class="ngde"><code class="ngde">>=</code>\uFF1A\u5927\u4E8E\u7B49\u4E8E</li><li class="ngde"><code class="ngde">&#x3C;=</code>\uFF1A\u5C0F\u4E8E\u7B49\u4E8E</li><li class="ngde"><code class="ngde">in</code>\uFF1A\u5728\u8303\u56F4\u5185\uFF0C\u4E5F\u53EF\u4EE5\u4F7F\u7528<code class="ngde">contains</code>\u4EE3\u66FF\uFF0C\u5982<code class="ngde">a in b</code>\u53EF\u4EE5\u5199\u6210<code class="ngde">b.contains(a)</code></li><li class="ngde"><code class="ngde">||</code>\uFF1A\u6216</li><li class="ngde"><code class="ngde">&#x26;&#x26;</code>\uFF1A\u4E0E</li><li class="ngde"><code class="ngde">!</code>\uFF1A\u975E\uFF0C\u53EF\u4EE5\u4E0E\u5176\u4ED6\u51FD\u6570\u548C\u64CD\u4F5C\u7B26\u4E00\u8D77\u4F7F\u7528\uFF0C\u5982<code class="ngde">!(a == 1 || a == 2) &#x26;&#x26; a !in listOf(3, 4)</code></li></ul><h3 id="22\u51FD\u6570" class="ngde">2.2.\u51FD\u6570<a title="Link to heading" class="ng-doc-header-link ngde" href="/zh-CN/database/where-having-on-clause#22\u51FD\u6570"><ng-doc-icon icon="link-2" size="16" class="ngde"></ng-doc-icon></a></h3><h4 id="between" class="ngde">between<a title="Link to heading" class="ng-doc-header-link ngde" href="/zh-CN/database/where-having-on-clause#between"><ng-doc-icon icon="link-2" size="16" class="ngde"></ng-doc-icon></a></h4><p class="ngde">\u5728\u8303\u56F4\u5185\uFF0C\u63A5\u6536ClosedRange\u7C7B\u578B\u7684\u53C2\u6570</p><pre class="ngde hljs"><code class="hljs language-kotlin code-lines ngde" lang="kotlin" name="" icon="" highlightedlines="[]"><span class="line ngde"><span class="hljs-keyword ngde">where</span> { it.age.between(<span class="hljs-number ngde">1.</span><span class="hljs-number ngde">.10</span>) }
</span><span class="line ngde"><span class="hljs-comment ngde">//\u652F\u6301\u4E2D\u7F00\u8C03\u7528</span>
</span><span class="line ngde"><span class="hljs-keyword ngde">where</span> { it.age between <span class="hljs-number ngde">1.</span><span class="hljs-number ngde">.10</span> }
</span></code></pre><h4 id="notbetween" class="ngde">notBetween<a title="Link to heading" class="ng-doc-header-link ngde" href="/zh-CN/database/where-having-on-clause#notbetween"><ng-doc-icon icon="link-2" size="16" class="ngde"></ng-doc-icon></a></h4><p class="ngde">\u4E0D\u5728\u8303\u56F4\u5185\uFF0C\u63A5\u6536ClosedRange\u7C7B\u578B\u7684\u53C2\u6570</p><pre class="ngde hljs"><code class="hljs language-kotlin code-lines ngde" lang="kotlin" name="" icon="" highlightedlines="[]"><span class="line ngde"><span class="hljs-keyword ngde">where</span> { it.age.notBetween(<span class="hljs-number ngde">1.</span><span class="hljs-number ngde">.10</span>) }
</span><span class="line ngde"><span class="hljs-comment ngde">//\u652F\u6301\u4E2D\u7F00\u8C03\u7528</span>
</span><span class="line ngde"><span class="hljs-keyword ngde">where</span> { it.age notBetween <span class="hljs-number ngde">1.</span><span class="hljs-number ngde">.10</span> }
</span></code></pre><h4 id="like" class="ngde">like<a title="Link to heading" class="ng-doc-header-link ngde" href="/zh-CN/database/where-having-on-clause#like"><ng-doc-icon icon="link-2" size="16" class="ngde"></ng-doc-icon></a></h4><p class="ngde">\u6A21\u7CCA\u67E5\u8BE2\uFF0C\u63A5\u6536String\u7C7B\u578B\u7684\u53C2\u6570</p><pre class="ngde hljs"><code class="hljs language-kotlin code-lines ngde" lang="kotlin" name="" icon="" highlightedlines="[]"><span class="line ngde"><span class="hljs-keyword ngde">where</span> { it.name.like(<span class="hljs-string ngde">"Kronos%"</span>) }
</span><span class="line ngde"><span class="hljs-comment ngde">//\u652F\u6301\u4E2D\u7F00\u8C03\u7528</span>
</span><span class="line ngde"><span class="hljs-keyword ngde">where</span> { it.name like <span class="hljs-string ngde">"Kronos%"</span> }
</span></code></pre><h4 id="matchleft" class="ngde">matchLeft<a title="Link to heading" class="ng-doc-header-link ngde" href="/zh-CN/database/where-having-on-clause#matchleft"><ng-doc-icon icon="link-2" size="16" class="ngde"></ng-doc-icon></a></h4><p class="ngde">\u5DE6\u6A21\u7CCA\u67E5\u8BE2\uFF0C\u63A5\u6536String\u7C7B\u578B\u7684\u53C2\u6570\uFF0C\u53EF\u4EE5\u4E0D\u4F20\u5165\u53C2\u6570\uFF0C\u5982<code class="ngde">it.name.matchLeft()</code> \u6216 <code class="ngde">it.name.matchLeft</code></p><pre class="ngde hljs"><code class="hljs language-kotlin code-lines ngde" lang="kotlin" name="" icon="" highlightedlines="[]"><span class="line ngde"><span class="hljs-keyword ngde">where</span> { it.name.matchLeft(<span class="hljs-string ngde">"Kronos"</span>) }
</span><span class="line ngde"><span class="hljs-comment ngde">//\u652F\u6301\u4E2D\u7F00\u8C03\u7528</span>
</span><span class="line ngde"><span class="hljs-keyword ngde">where</span> { it.name matchLeft <span class="hljs-string ngde">"Kronos"</span> }
</span><span class="line ngde"><span class="hljs-comment ngde">//\u7B49\u540C\u4E8E</span>
</span><span class="line ngde"><span class="hljs-keyword ngde">where</span> { it.name like <span class="hljs-string ngde">"Kronos%"</span> }
</span></code></pre><h4 id="matchright" class="ngde">matchRight<a title="Link to heading" class="ng-doc-header-link ngde" href="/zh-CN/database/where-having-on-clause#matchright"><ng-doc-icon icon="link-2" size="16" class="ngde"></ng-doc-icon></a></h4><p class="ngde">\u53F3\u6A21\u7CCA\u67E5\u8BE2\uFF0C\u63A5\u6536String\u7C7B\u578B\u7684\u53C2\u6570\uFF0C\u53EF\u4EE5\u4E0D\u4F20\u5165\u53C2\u6570\uFF0C\u5982<code class="ngde">it.name.matchRight()</code> \u6216 <code class="ngde">it.name.matchRight</code></p><pre class="ngde hljs"><code class="hljs language-kotlin code-lines ngde" lang="kotlin" name="" icon="" highlightedlines="[]"><span class="line ngde"><span class="hljs-keyword ngde">where</span> { it.name.matchRight(<span class="hljs-string ngde">"Kronos"</span>) }
</span><span class="line ngde"><span class="hljs-comment ngde">//\u652F\u6301\u4E2D\u7F00\u8C03\u7528</span>
</span><span class="line ngde"><span class="hljs-keyword ngde">where</span> { it.name matchRight <span class="hljs-string ngde">"Kronos"</span> }
</span><span class="line ngde"><span class="hljs-comment ngde">//\u7B49\u540C\u4E8E</span>
</span><span class="line ngde"><span class="hljs-keyword ngde">where</span> { it.name like <span class="hljs-string ngde">"%Kronos"</span> }
</span></code></pre><h4 id="matchboth" class="ngde">matchBoth<a title="Link to heading" class="ng-doc-header-link ngde" href="/zh-CN/database/where-having-on-clause#matchboth"><ng-doc-icon icon="link-2" size="16" class="ngde"></ng-doc-icon></a></h4><p class="ngde">\u5168\u6A21\u7CCA\u67E5\u8BE2\uFF0C\u63A5\u6536String\u7C7B\u578B\u7684\u53C2\u6570\uFF0C\u53EF\u4EE5\u4E0D\u4F20\u5165\u53C2\u6570\uFF0C\u5982<code class="ngde">it.name.matchBoth()</code> \u6216 <code class="ngde">it.name.matchBoth</code></p><pre class="ngde hljs"><code class="hljs language-kotlin code-lines ngde" lang="kotlin" name="" icon="" highlightedlines="[]"><span class="line ngde"><span class="hljs-keyword ngde">where</span> { it.name.matchBoth(<span class="hljs-string ngde">"Kronos"</span>) }
</span><span class="line ngde"><span class="hljs-comment ngde">//\u652F\u6301\u4E2D\u7F00\u8C03\u7528</span>
</span><span class="line ngde"><span class="hljs-keyword ngde">where</span> { it.name matchBoth <span class="hljs-string ngde">"Kronos"</span> }
</span><span class="line ngde"><span class="hljs-comment ngde">//\u7B49\u540C\u4E8E</span>
</span><span class="line ngde"><span class="hljs-keyword ngde">where</span> { it.name like <span class="hljs-string ngde">"%Kronos%"</span> }
</span></code></pre><h4 id="notlike" class="ngde">notLike<a title="Link to heading" class="ng-doc-header-link ngde" href="/zh-CN/database/where-having-on-clause#notlike"><ng-doc-icon icon="link-2" size="16" class="ngde"></ng-doc-icon></a></h4><p class="ngde">\u4E0D\u5339\u914D\uFF0C\u63A5\u6536String\u7C7B\u578B\u7684\u53C2\u6570</p><pre class="ngde hljs"><code class="hljs language-kotlin code-lines ngde" lang="kotlin" name="" icon="" highlightedlines="[]"><span class="line ngde"><span class="hljs-keyword ngde">where</span> { it.name.notLike(<span class="hljs-string ngde">"Kronos%"</span>) }
</span><span class="line ngde"><span class="hljs-comment ngde">//\u652F\u6301\u4E2D\u7F00\u8C03\u7528</span>
</span><span class="line ngde"><span class="hljs-keyword ngde">where</span> { it.name notLike <span class="hljs-string ngde">"Kronos%"</span> }
</span></code></pre><h4 id="isnull" class="ngde">isNull<a title="Link to heading" class="ng-doc-header-link ngde" href="/zh-CN/database/where-having-on-clause#isnull"><ng-doc-icon icon="link-2" size="16" class="ngde"></ng-doc-icon></a></h4><p class="ngde">\u4E3A\u7A7A</p><pre class="ngde hljs"><code class="hljs language-kotlin code-lines ngde" lang="kotlin" name="" icon="" highlightedlines="[]"><span class="line ngde"><span class="hljs-keyword ngde">where</span> { it.name.isNull }
</span></code></pre><h4 id="notnull" class="ngde">notNull<a title="Link to heading" class="ng-doc-header-link ngde" href="/zh-CN/database/where-having-on-clause#notnull"><ng-doc-icon icon="link-2" size="16" class="ngde"></ng-doc-icon></a></h4><p class="ngde">\u4E0D\u4E3A\u7A7A</p><pre class="ngde hljs"><code class="hljs language-kotlin code-lines ngde" lang="kotlin" name="" icon="" highlightedlines="[]"><span class="line ngde"><span class="hljs-keyword ngde">where</span> { it.name.notNull }
</span></code></pre><h4 id="regexp" class="ngde">regexp<a title="Link to heading" class="ng-doc-header-link ngde" href="/zh-CN/database/where-having-on-clause#regexp"><ng-doc-icon icon="link-2" size="16" class="ngde"></ng-doc-icon></a></h4><p class="ngde">\u6B63\u5219\u8868\u8FBE\u5F0F\u67E5\u8BE2\uFF0C\u63A5\u6536String\u7C7B\u578B\u7684\u53C2\u6570</p><pre class="ngde hljs"><code class="hljs language-kotlin code-lines ngde" lang="kotlin" name="" icon="" highlightedlines="[]"><span class="line ngde"><span class="hljs-keyword ngde">where</span> { it.name.regexp(<span class="hljs-string ngde">"Kronos.*"</span>) }
</span><span class="line ngde"><span class="hljs-comment ngde">// \u652F\u6301\u4E2D\u7F00\u8C03\u7528</span>
</span><span class="line ngde"><span class="hljs-keyword ngde">where</span> { it.name regexp <span class="hljs-string ngde">"Kronos.*"</span> }
</span></code></pre><h4 id="assql" class="ngde">asSql<a title="Link to heading" class="ng-doc-header-link ngde" href="/zh-CN/database/where-having-on-clause#assql"><ng-doc-icon icon="link-2" size="16" class="ngde"></ng-doc-icon></a></h4><p class="ngde">\u81EA\u5B9A\u4E49SQL\u67E5\u8BE2\u6761\u4EF6\uFF0C\u63A5\u6536String\u7C7B\u578B\u7684\u53C2\u6570</p><pre class="ngde hljs"><code class="hljs language-kotlin code-lines ngde" lang="kotlin" name="" icon="" highlightedlines="[]"><span class="line ngde"><span class="hljs-keyword ngde">where</span> { <span class="hljs-string ngde">"name = 'Kronos' and age > 18"</span>.asSql() }
</span></code></pre><h4 id="eq" class="ngde">eq<a title="Link to heading" class="ng-doc-header-link ngde" href="/zh-CN/database/where-having-on-clause#eq"><ng-doc-icon icon="link-2" size="16" class="ngde"></ng-doc-icon></a></h4><p class="ngde">\u7B49\u4E8E\uFF0C\u7B49\u540C\u4E8E<code class="ngde">==</code>\uFF0C\u53EF\u4EE5\u4E0D\u4F20\u5165\u53C2\u6570\uFF0C\u5982<code class="ngde">it.name.eq()</code> \u6216 <code class="ngde">it.name.eq</code></p><pre class="ngde hljs"><code class="hljs language-kotlin code-lines ngde" lang="kotlin" name="" icon="" highlightedlines="[]"><span class="line ngde"><span class="hljs-keyword ngde">where</span> { it.name.eq(<span class="hljs-string ngde">"Kronos"</span>) }
</span><span class="line ngde">User(name = <span class="hljs-string ngde">'Kronos'</span>).select().<span class="hljs-keyword ngde">where</span> { it.name.eq } <span class="hljs-comment ngde">// \u7B49\u540C\u4E8E where { it.name.eq("Kronos") }</span>
</span></code></pre><h4 id="noteq" class="ngde">notEq<a title="Link to heading" class="ng-doc-header-link ngde" href="/zh-CN/database/where-having-on-clause#noteq"><ng-doc-icon icon="link-2" size="16" class="ngde"></ng-doc-icon></a></h4><p class="ngde">\u4E0D\u7B49\u4E8E\uFF0C\u7B49\u540C\u4E8E<code class="ngde">!=</code>\uFF0C\u4F46\u662F\u53EF\u4EE5\u4E0D\u4F20\u5165\u53C2\u6570\uFF0C\u5982<code class="ngde">it.name.notEq()</code> \u6216 <code class="ngde">it.name.notEq</code></p><pre class="ngde hljs"><code class="hljs language-kotlin code-lines ngde" lang="kotlin" name="" icon="" highlightedlines="[]"><span class="line ngde"><span class="hljs-keyword ngde">where</span> { it.name.notEq(<span class="hljs-string ngde">"Kronos"</span>) }
</span><span class="line ngde">User(name = <span class="hljs-string ngde">'Kronos'</span>).select().<span class="hljs-keyword ngde">where</span> { it.name.notEq } <span class="hljs-comment ngde">// \u7B49\u540C\u4E8E where { it.name.notEq("Kronos") }</span>
</span></code></pre><h4 id="gt" class="ngde">gt<a title="Link to heading" class="ng-doc-header-link ngde" href="/zh-CN/database/where-having-on-clause#gt"><ng-doc-icon icon="link-2" size="16" class="ngde"></ng-doc-icon></a></h4><p class="ngde">\u5927\u4E8E\uFF0C\u7B49\u540C\u4E8E<code class="ngde">></code>\uFF0C\u53EF\u4EE5\u4E0D\u4F20\u5165\u53C2\u6570\uFF0C\u5982<code class="ngde">it.age.gt()</code> \u6216 <code class="ngde">it.age.gt</code></p><pre class="ngde hljs"><code class="hljs language-kotlin code-lines ngde" lang="kotlin" name="" icon="" highlightedlines="[]"><span class="line ngde"><span class="hljs-keyword ngde">where</span> { it.age.gt(<span class="hljs-number ngde">18</span>) }
</span><span class="line ngde">User(age = <span class="hljs-number ngde">18</span>).select().<span class="hljs-keyword ngde">where</span> { it.age.gt } <span class="hljs-comment ngde">// \u7B49\u540C\u4E8E where { it.age.gt(18) }</span>
</span></code></pre><h4 id="lt" class="ngde">lt<a title="Link to heading" class="ng-doc-header-link ngde" href="/zh-CN/database/where-having-on-clause#lt"><ng-doc-icon icon="link-2" size="16" class="ngde"></ng-doc-icon></a></h4><p class="ngde">\u5C0F\u4E8E\uFF0C\u7B49\u540C\u4E8E<code class="ngde">&#x3C;</code>\uFF0C\u53EF\u4EE5\u4E0D\u4F20\u5165\u53C2\u6570\uFF0C\u5982<code class="ngde">it.age.lt()</code> \u6216 <code class="ngde">it.age.lt</code></p><pre class="ngde hljs"><code class="hljs language-kotlin code-lines ngde" lang="kotlin" name="" icon="" highlightedlines="[]"><span class="line ngde"><span class="hljs-keyword ngde">where</span> { it.age.lt(<span class="hljs-number ngde">18</span>) }
</span><span class="line ngde">User(age = <span class="hljs-number ngde">18</span>).select().<span class="hljs-keyword ngde">where</span> { it.age.lt } <span class="hljs-comment ngde">// \u7B49\u540C\u4E8E where { it.age.lt(18) }</span>
</span></code></pre><h4 id="ge" class="ngde">ge<a title="Link to heading" class="ng-doc-header-link ngde" href="/zh-CN/database/where-having-on-clause#ge"><ng-doc-icon icon="link-2" size="16" class="ngde"></ng-doc-icon></a></h4><p class="ngde">\u5927\u4E8E\u7B49\u4E8E\uFF0C\u7B49\u540C\u4E8E<code class="ngde">>=</code>\uFF0C\u53EF\u4EE5\u4E0D\u4F20\u5165\u53C2\u6570\uFF0C\u5982<code class="ngde">it.age.ge()</code> \u6216 <code class="ngde">it.age.ge</code></p><pre class="ngde hljs"><code class="hljs language-kotlin code-lines ngde" lang="kotlin" name="" icon="" highlightedlines="[]"><span class="line ngde"><span class="hljs-keyword ngde">where</span> { it.age.ge(<span class="hljs-number ngde">18</span>) }
</span><span class="line ngde">User(age = <span class="hljs-number ngde">18</span>).select().<span class="hljs-keyword ngde">where</span> { it.age.ge } <span class="hljs-comment ngde">// \u7B49\u540C\u4E8E where { it.age.ge(18) }</span>
</span></code></pre><h4 id="le" class="ngde">le<a title="Link to heading" class="ng-doc-header-link ngde" href="/zh-CN/database/where-having-on-clause#le"><ng-doc-icon icon="link-2" size="16" class="ngde"></ng-doc-icon></a></h4><p class="ngde">\u5C0F\u4E8E\u7B49\u4E8E\uFF0C\u7B49\u540C\u4E8E<code class="ngde">&#x3C;=</code>\uFF0C\u53EF\u4EE5\u4E0D\u4F20\u5165\u53C2\u6570\uFF0C\u5982<code class="ngde">it.age.le()</code> \u6216 <code class="ngde">it.age.le</code></p><pre class="ngde hljs"><code class="hljs language-kotlin code-lines ngde" lang="kotlin" name="" icon="" highlightedlines="[]"><span class="line ngde"><span class="hljs-keyword ngde">where</span> { it.age.le(<span class="hljs-number ngde">18</span>) }
</span><span class="line ngde">User(age = <span class="hljs-number ngde">18</span>).select().<span class="hljs-keyword ngde">where</span> { it.age.le } <span class="hljs-comment ngde">// \u7B49\u540C\u4E8E where { it.age.le(18) }</span>
</span></code></pre><h4 id="ifnovalue" class="ngde">ifNoValue<a title="Link to heading" class="ng-doc-header-link ngde" href="/zh-CN/database/where-having-on-clause#ifnovalue"><ng-doc-icon icon="link-2" size="16" class="ngde"></ng-doc-icon></a></h4><p class="ngde">\u65E0\u503C\u7B56\u7565\uFF0C\u63A5\u6536\u53C2\u6570<code class="ngde">NoValueStrategy</code>\uFF0C\u7528\u4E8E\u5904\u7406\u65E0\u503C\u7684\u60C5\u51B5\uFF0C\u4F18\u5148\u7EA7\u9AD8\u4E8EKronos\u9ED8\u8BA4\u7684\u65E0\u503C\u7B56\u7565\uFF0C\u8BE6\u89C1\uFF1A<a href="/documentation/zh-CN/database/no-value-strategy" class="ngde">\u65E0\u503C\u7B56\u7565</a></p><pre class="ngde hljs"><code class="hljs language-kotlin code-lines ngde" lang="kotlin" name="" icon="" highlightedlines="[]"><span class="line ngde"><span class="hljs-keyword ngde">val</span> age: <span class="hljs-built_in ngde">Int</span>? = <span class="hljs-literal ngde">null</span>
</span><span class="line ngde">User()
</span><span class="line ngde">  .select()
</span><span class="line ngde">  .<span class="hljs-keyword ngde">where</span> { (it.age == age).ifNoValue(Ignore) } <span class="hljs-comment ngde">// \u65E0\u503C\u65F6\u5FFD\u7565\u6761\u4EF6</span>
</span><span class="line ngde">  .query()
</span><span class="line ngde">
</span><span class="line ngde"><span class="hljs-keyword ngde">val</span> username: String? = <span class="hljs-literal ngde">null</span>
</span><span class="line ngde">User(username = username)
</span><span class="line ngde">  .delete()
</span><span class="line ngde">  .<span class="hljs-keyword ngde">where</span> { it.username.eq.ifNoValue(False) } <span class="hljs-comment ngde">// \u65E0\u503C\u65F6\u8FD4\u56DEfalse</span>
</span><span class="line ngde">  .execute()
</span></code></pre>`,z=(()=>{let e=class e extends l{constructor(){super(),this.routePrefix="",this.pageType="guide",this.pageContent=C,this.page=s,this.demoAssets=k}};e.\u0275fac=function(a){return new(a||e)},e.\u0275cmp=g({type:e,selectors:[["ng-doc-page-zh-cn-database-where-having-on-clause"]],standalone:!0,features:[t([{provide:l,useExisting:e},m,s.providers??[]]),i,h],decls:1,vars:0,template:function(a,v){a&1&&o(0,"ng-doc-page")},dependencies:[p],encapsulation:2,changeDetection:0});let n=e;return n})(),N=[d(c({},(0,j.isRoute)(s.route)?s.route:{}),{path:"",component:z,title:"Criteria \u6761\u4EF6"})],U=N;export{z as DynamicComponent,U as default};
