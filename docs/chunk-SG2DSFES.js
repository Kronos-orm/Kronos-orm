import{a as h}from"./chunk-IVKSPUZI.js";import{a as i}from"./chunk-HNPVF6HZ.js";import{a as p}from"./chunk-YGVJYOZD.js";import{H as w}from"./chunk-HT2ZSXJU.js";import{Pb as g,jc as o,kc as t,ra as c,xb as r}from"./chunk-W2MTHNV2.js";import{a as l,b as d,g as y}from"./chunk-ODN5LVDJ.js";var k=y(w());var f={title:"Driver\u5305\u88C5\u5668\u53CA\u4E09\u65B9\u6846\u67B6\u6269\u5C55",mdFile:"./index.md",category:h,order:0},n=f;var m=[];var C={},j=C;var x=`<h1 id="driver\u5305\u88C5\u5668\u53CA\u4E09\u65B9\u6846\u67B6\u6269\u5C55" class="ngde">Driver\u5305\u88C5\u5668\u53CA\u4E09\u65B9\u6846\u67B6\u6269\u5C55<a title="Link to heading" class="ng-doc-header-link ngde" href="/zh-CN/plugin/datasource-wrapper-and-third-part-framework#driver\u5305\u88C5\u5668\u53CA\u4E09\u65B9\u6846\u67B6\u6269\u5C55"><ng-doc-icon icon="link-2" size="16" class="ngde"></ng-doc-icon></a></h1><p class="ngde">Kronos\u901A\u8FC7\u81EA\u5B9A\u4E49\u521B\u5EFA\u7EE7\u627F<code class="ngde">KronosDataSourceWrapper</code>\u63A5\u53E3\u7684\u5305\u88C5\u7C7B\uFF0C\u53EF\u4EE5\u8F7B\u677E\u4E0E\u7B2C\u4E09\u65B9\u6846\u67B6\u7ED3\u5408\u4F7F\u7528\u3002</p><h2 id="spring\u793A\u4F8B" class="ngde">Spring\u793A\u4F8B<a title="Link to heading" class="ng-doc-header-link ngde" href="/zh-CN/plugin/datasource-wrapper-and-third-part-framework#spring\u793A\u4F8B"><ng-doc-icon icon="link-2" size="16" class="ngde"></ng-doc-icon></a></h2><p class="ngde">\u4EE5\u4E0B\u662F\u4E00\u4E2A\u4F7F\u7528Springboot + Kronos + JDK 17 + Maven + Kotlin 2.0.0 \u7684\u793A\u4F8B\uFF0C\u6F14\u793A\u4E86\u5982\u4F55\u5C06Kronos\u4E0ESpring\u6846\u67B6\u7ED3\u5408\u4F7F\u7528\u3002</p><p class="ngde">\u5176\u4E2D\u5305\u542B\u5982\u4F55\u521B\u5EFA\u4E00\u4E2A\u57FA\u4E8E<code class="ngde">spring-data-jdbc</code>\u7684\u5305\u88C5\u7C7B\uFF0C\u4ECE\u800C\u65E0\u9700\u5F15\u5165<code class="ngde">kronos-jvm-driver-wrapper</code>\u7B49\u989D\u5916\u4F9D\u8D56\uFF0C\u4EC5\u901A\u8FC7<code class="ngde">kronos-core</code> \u5373\u53EF\u5B9E\u73B0\u6570\u636E\u5E93\u64CD\u4F5C\u7684\u529F\u80FD\u3002</p><ng-doc-blockquote class="ngde"><p class="ngde"><a href="https://github.com/Kronos-orm/kronos-spring-demo" class="ngde">https://github.com/Kronos-orm/kronos-spring-demo</a></p></ng-doc-blockquote><h3 id="1\u4F9D\u8D56\u9879" class="ngde">1.\u4F9D\u8D56\u9879<a title="Link to heading" class="ng-doc-header-link ngde" href="/zh-CN/plugin/datasource-wrapper-and-third-part-framework#1\u4F9D\u8D56\u9879"><ng-doc-icon icon="link-2" size="16" class="ngde"></ng-doc-icon></a></h3><p class="ngde">\u5F15\u5165<code class="ngde">spring</code>\u76F8\u5173\u4F9D\u8D56\u9879\u53CA<code class="ngde">kronos-core</code>\u4F9D\u8D56\u9879\uFF08<code class="ngde">compiler-plugin</code> \u63D2\u4EF6\u7684\u5F15\u5165\u65B9\u5F0F\u89C1\uFF08<a href="/documentation/zh-CN/getting-started/quick-start" class="ngde">\u5FEB\u901F\u4E0A\u624B</a>\uFF09\uFF09</p><pre class="ngde hljs"><code class="hljs language-xml code-lines ngde" lang="xml" name="" icon="" highlightedlines="[]"><span class="line ngde">
</span><span class="line ngde"><span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">dependencies</span>></span>
</span><span class="line ngde">    <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">dependency</span>></span>
</span><span class="line ngde">        <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">groupId</span>></span>org.springframework.data<span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">groupId</span>></span>
</span><span class="line ngde">        <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">artifactId</span>></span>spring-data-jdbc<span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">artifactId</span>></span>
</span><span class="line ngde">    <span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">dependency</span>></span>
</span><span class="line ngde">    <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">dependency</span>></span>
</span><span class="line ngde">        <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">groupId</span>></span>org.apache.commons<span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">groupId</span>></span>
</span><span class="line ngde">        <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">artifactId</span>></span>commons-dbcp2<span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">artifactId</span>></span>
</span><span class="line ngde">    <span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">dependency</span>></span>
</span><span class="line ngde">    <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">dependency</span>></span>
</span><span class="line ngde">        <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">groupId</span>></span>com.kotlinorm<span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">groupId</span>></span>
</span><span class="line ngde">        <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">artifactId</span>></span>kronos-core<span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">artifactId</span>></span>
</span><span class="line ngde">        <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">version</span>></span>\${kronos.version}<span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">version</span>></span>
</span><span class="line ngde">    <span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">dependency</span>></span>
</span><span class="line ngde"><span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">dependencies</span>></span>
</span></code></pre><h3 id="2kronosdatasourcewrapper\u5B9E\u73B0" class="ngde">2.KronosDataSourceWrapper\u5B9E\u73B0<a title="Link to heading" class="ng-doc-header-link ngde" href="/zh-CN/plugin/datasource-wrapper-and-third-part-framework#2kronosdatasourcewrapper\u5B9E\u73B0"><ng-doc-icon icon="link-2" size="16" class="ngde"></ng-doc-icon></a></h3><h4 id="1\u521D\u59CB\u5316\u8FDE\u63A5\u4FE1\u606F\u548Cjdbc\u6A21\u7248" class="ngde">1.\u521D\u59CB\u5316\u8FDE\u63A5\u4FE1\u606F\u548CJDBC\u6A21\u7248<a title="Link to heading" class="ng-doc-header-link ngde" href="/zh-CN/plugin/datasource-wrapper-and-third-part-framework#1\u521D\u59CB\u5316\u8FDE\u63A5\u4FE1\u606F\u548Cjdbc\u6A21\u7248"><ng-doc-icon icon="link-2" size="16" class="ngde"></ng-doc-icon></a></h4><pre class="ngde hljs"><code class="hljs language-kotlin code-lines ngde" lang="kotlin" name="" icon="" highlightedlines="[]"><span class="line ngde"><span class="hljs-comment ngde">// \u8FDE\u63A5\u4FE1\u606F</span>
</span><span class="line ngde"><span class="hljs-keyword ngde">init</span> {
</span><span class="line ngde">    <span class="hljs-keyword ngde">val</span> conn = dataSource.connection
</span><span class="line ngde">    _metaUrl = conn.metaData.url
</span><span class="line ngde">    _metaDbType = DBType.fromName(conn.metaData.databaseProductName)
</span><span class="line ngde">    _userName = conn.metaData.userName ?: <span class="hljs-string ngde">""</span>
</span><span class="line ngde">    conn.close()
</span><span class="line ngde">}
</span><span class="line ngde">
</span><span class="line ngde"><span class="hljs-comment ngde">// NamedParameterJdbcTemplate\u662Fspring-data-jdbc\u63D0\u4F9B\u7684JdbcTemplate\u7684\u652F\u6301\u547D\u540D\u53C2\u6570\u7684\u5B9E\u73B0\uFF0C\u7528\u4E8E\u6267\u884CJDBC\u547D\u4EE4</span>
</span><span class="line ngde"><span class="hljs-keyword ngde">private</span> <span class="hljs-keyword ngde">val</span> namedJdbc: NamedParameterJdbcTemplate <span class="hljs-keyword ngde">by</span> lazy {
</span><span class="line ngde">    NamedParameterJdbcTemplate(dataSource)
</span><span class="line ngde">}
</span><span class="line ngde">
</span></code></pre><h4 id="2\u91CD\u8F7Dkronosdatasourcewrapper\u4E2D\u7684\u6570\u636E\u5E93\u64CD\u4F5C" class="ngde">2.\u91CD\u8F7DKronosDataSourceWrapper\u4E2D\u7684\u6570\u636E\u5E93\u64CD\u4F5C<a title="Link to heading" class="ng-doc-header-link ngde" href="/zh-CN/plugin/datasource-wrapper-and-third-part-framework#2\u91CD\u8F7Dkronosdatasourcewrapper\u4E2D\u7684\u6570\u636E\u5E93\u64CD\u4F5C"><ng-doc-icon icon="link-2" size="16" class="ngde"></ng-doc-icon></a></h4><pre class="ngde hljs"><code class="hljs language-kotlin code-lines ngde" lang="kotlin" name="" icon="" highlightedlines="[]"><span class="line ngde"><span class="hljs-comment ngde">//1.\u67E5\u8BE2Map&#x3C;String, Any>\u5217\u8868</span>
</span><span class="line ngde"><span class="hljs-keyword ngde">override</span> <span class="hljs-function ngde"><span class="hljs-keyword ngde">fun</span> <span class="hljs-title ngde">forList</span><span class="hljs-params ngde">(task: <span class="hljs-type ngde">KAtomicQueryTask</span>)</span></span>: List&#x3C;Map&#x3C;String, Any>> {
</span><span class="line ngde">    <span class="hljs-keyword ngde">return</span> namedJdbc.queryForList(task.sql, task.paramMap)
</span><span class="line ngde">}
</span><span class="line ngde">
</span><span class="line ngde"><span class="hljs-comment ngde">//2.\u67E5\u8BE2\u5BF9\u8C61\u5217\u8868</span>
</span><span class="line ngde"><span class="hljs-keyword ngde">override</span> <span class="hljs-function ngde"><span class="hljs-keyword ngde">fun</span> <span class="hljs-title ngde">forList</span><span class="hljs-params ngde">(task: <span class="hljs-type ngde">KAtomicQueryTask</span>, kClass: <span class="hljs-type ngde">KClass</span>&#x3C;*>)</span></span>: List&#x3C;Any> {
</span><span class="line ngde">    <span class="hljs-keyword ngde">return</span> <span class="hljs-keyword ngde">if</span> (KPojo::<span class="hljs-keyword ngde">class</span>.isSuperclassOf(kClass)) namedJdbc.query(
</span><span class="line ngde">        task.sql,
</span><span class="line ngde">        task.paramMap,
</span><span class="line ngde">        DataClassRowMapper(kClass.java)
</span><span class="line ngde">    )
</span><span class="line ngde">    <span class="hljs-keyword ngde">else</span> namedJdbc.queryForList(task.sql, task.paramMap, kClass.java)
</span><span class="line ngde">}
</span><span class="line ngde">
</span><span class="line ngde"><span class="hljs-comment ngde">//3.\u67E5\u8BE2Map&#x3C;String, Any>\uFF0C\u5982\u679C\u67E5\u8BE2\u7ED3\u679C\u4E3A\u7A7A\u5219\u8FD4\u56DEnull</span>
</span><span class="line ngde"><span class="hljs-keyword ngde">override</span> <span class="hljs-function ngde"><span class="hljs-keyword ngde">fun</span> <span class="hljs-title ngde">forMap</span><span class="hljs-params ngde">(task: <span class="hljs-type ngde">KAtomicQueryTask</span>)</span></span>: Map&#x3C;String, Any>? {
</span><span class="line ngde">    <span class="hljs-keyword ngde">return</span> <span class="hljs-keyword ngde">try</span> {
</span><span class="line ngde">        namedJdbc.queryForMap(task.sql, task.paramMap)
</span><span class="line ngde">    } <span class="hljs-keyword ngde">catch</span> (e: DataAccessException) {
</span><span class="line ngde">        <span class="hljs-literal ngde">null</span>
</span><span class="line ngde">    }
</span><span class="line ngde">}
</span><span class="line ngde">
</span><span class="line ngde"><span class="hljs-comment ngde">//4.\u67E5\u8BE2\u5BF9\u8C61\uFF0C\u5982\u679C\u67E5\u8BE2\u7ED3\u679C\u4E3A\u7A7A\u5219\u8FD4\u56DEnull</span>
</span><span class="line ngde"><span class="hljs-keyword ngde">override</span> <span class="hljs-function ngde"><span class="hljs-keyword ngde">fun</span> <span class="hljs-title ngde">forObject</span><span class="hljs-params ngde">(task: <span class="hljs-type ngde">KAtomicQueryTask</span>, kClass: <span class="hljs-type ngde">KClass</span>&#x3C;*>)</span></span>: Any? {
</span><span class="line ngde">    <span class="hljs-keyword ngde">return</span> <span class="hljs-keyword ngde">try</span> {
</span><span class="line ngde">        <span class="hljs-keyword ngde">if</span> (KPojo::<span class="hljs-keyword ngde">class</span>.isSuperclassOf(kClass)) namedJdbc.queryForObject(
</span><span class="line ngde">            task.sql,
</span><span class="line ngde">            task.paramMap,
</span><span class="line ngde">            DataClassRowMapper(kClass.java)
</span><span class="line ngde">        )
</span><span class="line ngde">        <span class="hljs-keyword ngde">else</span> namedJdbc.queryForObject(task.sql, task.paramMap, kClass.java)
</span><span class="line ngde">    } <span class="hljs-keyword ngde">catch</span> (e: DataAccessException) {
</span><span class="line ngde">        <span class="hljs-literal ngde">null</span>
</span><span class="line ngde">    }
</span><span class="line ngde">}
</span><span class="line ngde">
</span><span class="line ngde"><span class="hljs-comment ngde">//5.\u6267\u884C\u66F4\u65B0\uFF0C\u8FD4\u56DE\u53D7\u5F71\u54CD\u7684\u884C\u6570</span>
</span><span class="line ngde"><span class="hljs-keyword ngde">override</span> <span class="hljs-function ngde"><span class="hljs-keyword ngde">fun</span> <span class="hljs-title ngde">update</span><span class="hljs-params ngde">(task: <span class="hljs-type ngde">KAtomicActionTask</span>)</span></span>: <span class="hljs-built_in ngde">Int</span> {
</span><span class="line ngde">    <span class="hljs-keyword ngde">return</span> namedJdbc.update(task.sql, task.paramMap)
</span><span class="line ngde">}
</span><span class="line ngde">
</span><span class="line ngde"><span class="hljs-comment ngde">//6.\u6267\u884C\u6279\u91CF\u66F4\u65B0\uFF0C\u8FD4\u56DE\u53D7\u5F71\u54CD\u7684\u884C\u6570</span>
</span><span class="line ngde"><span class="hljs-keyword ngde">override</span> <span class="hljs-function ngde"><span class="hljs-keyword ngde">fun</span> <span class="hljs-title ngde">batchUpdate</span><span class="hljs-params ngde">(task: <span class="hljs-type ngde">KronosAtomicBatchTask</span>)</span></span>: IntArray {
</span><span class="line ngde">    <span class="hljs-keyword ngde">return</span> namedJdbc.batchUpdate(task.sql, task.paramMapArr ?: emptyArray())
</span><span class="line ngde">}
</span><span class="line ngde">
</span><span class="line ngde"><span class="hljs-comment ngde">//7.\u4E8B\u52A1\u64CD\u4F5C</span>
</span><span class="line ngde"><span class="hljs-keyword ngde">override</span> <span class="hljs-function ngde"><span class="hljs-keyword ngde">fun</span> <span class="hljs-title ngde">transact</span><span class="hljs-params ngde">(block: (<span class="hljs-type ngde">DataSource</span>) -> <span class="hljs-type ngde">Any</span>?)</span></span>: Any? {
</span><span class="line ngde">    <span class="hljs-keyword ngde">val</span> transactionManager = DataSourceTransactionManager(dataSource)
</span><span class="line ngde">    <span class="hljs-keyword ngde">val</span> transactionTemplate = TransactionTemplate(transactionManager)
</span><span class="line ngde">
</span><span class="line ngde">    <span class="hljs-keyword ngde">var</span> res: Any? = <span class="hljs-literal ngde">null</span>
</span><span class="line ngde">
</span><span class="line ngde">    transactionTemplate.execute {
</span><span class="line ngde">        <span class="hljs-keyword ngde">try</span> {
</span><span class="line ngde">            res = block(dataSource)
</span><span class="line ngde">        } <span class="hljs-keyword ngde">catch</span> (e: Exception) {
</span><span class="line ngde">            <span class="hljs-keyword ngde">throw</span> e
</span><span class="line ngde">        }
</span><span class="line ngde">    }
</span><span class="line ngde">
</span><span class="line ngde">    <span class="hljs-keyword ngde">return</span> res
</span><span class="line ngde">}
</span></code></pre><h4 id="3\u5176\u4ED6" class="ngde">3.\u5176\u4ED6<a title="Link to heading" class="ng-doc-header-link ngde" href="/zh-CN/plugin/datasource-wrapper-and-third-part-framework#3\u5176\u4ED6"><ng-doc-icon icon="link-2" size="16" class="ngde"></ng-doc-icon></a></h4><pre class="ngde hljs"><code class="hljs language-kotlin code-lines ngde" lang="kotlin" name="" icon="" highlightedlines="[]"><span class="line ngde"><span class="hljs-keyword ngde">companion</span> <span class="hljs-keyword ngde">object</span> {
</span><span class="line ngde">    <span class="hljs-comment ngde">//\u5C06JdbcTemplate\u5305\u88C5\u4E3ASpringDataWrapper\u7684\u6269\u5C55\u51FD\u6570</span>
</span><span class="line ngde">    <span class="hljs-function ngde"><span class="hljs-keyword ngde">fun</span> JdbcTemplate.<span class="hljs-title ngde">wrapper</span><span class="hljs-params ngde">()</span></span>: SpringDataWrapper {
</span><span class="line ngde">        <span class="hljs-keyword ngde">return</span> SpringDataWrapper(<span class="hljs-keyword ngde">this</span>.dataSource!!)
</span><span class="line ngde">    }
</span><span class="line ngde">
</span><span class="line ngde">    <span class="hljs-comment ngde">//\u5C06NamedParameterJdbcTemplate\u5305\u88C5\u4E3ASpringDataWrapper\u7684\u6269\u5C55\u51FD\u6570</span>
</span><span class="line ngde">    <span class="hljs-function ngde"><span class="hljs-keyword ngde">fun</span> NamedParameterJdbcTemplate.<span class="hljs-title ngde">wrapper</span><span class="hljs-params ngde">()</span></span>: SpringDataWrapper {
</span><span class="line ngde">        <span class="hljs-keyword ngde">return</span> SpringDataWrapper(<span class="hljs-keyword ngde">this</span>.jdbcTemplate.dataSource!!)
</span><span class="line ngde">    }
</span><span class="line ngde">}
</span></code></pre><p class="ngde">\u5168\u90E8\u4EE3\u7801\u53EF\u53C2\u8003\uFF1A <a href="https://github.com/Kronos-orm/kronos-spring-demo/blob/main/src/main/kotlin/com/kotlinorm/kronosSpringDemo/controller/SpringDataWrapper.kt" class="ngde">SpringDataWrapper.kt</a></p><h2 id="\u5176\u4ED6\u6846\u67B6" class="ngde">\u5176\u4ED6\u6846\u67B6<a title="Link to heading" class="ng-doc-header-link ngde" href="/zh-CN/plugin/datasource-wrapper-and-third-part-framework#\u5176\u4ED6\u6846\u67B6"><ng-doc-icon icon="link-2" size="16" class="ngde"></ng-doc-icon></a></h2><p class="ngde">\u5BF9\u4E8EJDBI\u7B49\u652F\u6301\u547D\u540D\u53C2\u6570\u7684\u6846\u67B6\uFF0C\u5199\u6CD5\u4E0ESpringDataWrapper\u51E0\u4E4E\u5B8C\u5168\u76F8\u540C\uFF0C\u53EA\u9700\u6839\u636E\u4E0D\u540C\u6846\u67B6\u7684\u5177\u4F53\u5B9E\u73B0\u8FDB\u884C\u66FF\u6362\u5373\u53EF\u3002</p><p class="ngde">\u5BF9\u4E8E\u5176\u4ED6\u4EC5\u652F\u6301\u987A\u5E8F\u53C2\u6570\u7684\u6846\u67B6\uFF0C\u53EF\u4EE5\u901A\u8FC7<code class="ngde">KAtomicQueryTask.parsed()</code>\u6216<code class="ngde">KAtomicActionyTask.parsed()</code>\u6216<code class="ngde">KronosAtomicBatchTask.parsedArr()</code>\u83B7\u53D6\u89E3\u6790\u540E\u7684SQL\u8BED\u53E5\uFF0C\u8FD9\u4E2A\u5C5E\u6027\u4E2D\u5305\u542B\u4E86\u53C2\u6570\u540D\u548C\u53C2\u6570\u503C\u6570\u7EC4\u3002</p><p class="ngde">\u540E\u7EED\u5927\u81F4\u6D41\u7A0B\u4E0ESpringDataWrapper\u76F8\u540C\uFF0C\u53EF\u53C2\u8003<a href="https://github.com/Kronos-orm/Kronos-orm/blob/main/kronos-jvm-driver-wrapper/src/main/kotlin/com/kotlinorm/KronosBasicWrapper.kt" class="ngde">KronosBasicWrapper.kt</a>\u3002</p>`,b=(()=>{let s=class s extends p{constructor(){super(),this.routePrefix="",this.pageType="guide",this.pageContent=x,this.page=n,this.demoAssets=j}};s.\u0275fac=function(e){return new(e||s)},s.\u0275cmp=c({type:s,selectors:[["ng-doc-page-zh-cn-plugin-datasource-wrapper-and-third-part-framework"]],standalone:!0,features:[o([{provide:p,useExisting:s},m,n.providers??[]]),r,t],decls:1,vars:0,template:function(e,S){e&1&&g(0,"ng-doc-page")},dependencies:[i],encapsulation:2,changeDetection:0});let a=s;return a})(),D=[d(l({},(0,k.isRoute)(n.route)?n.route:{}),{path:"",component:b,title:"Driver\u5305\u88C5\u5668\u53CA\u4E09\u65B9\u6846\u67B6\u6269\u5C55"})],I=D;export{b as DynamicComponent,I as default};
