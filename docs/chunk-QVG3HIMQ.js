import{a as h}from"./chunk-HPNNSXXN.js";import{a as g}from"./chunk-HNPVF6HZ.js";import{a as p}from"./chunk-YGVJYOZD.js";import{H as f}from"./chunk-HT2ZSXJU.js";import{Pb as t,jc as o,kc as i,ra as c,xb as r}from"./chunk-W2MTHNV2.js";import{a as l,b as d,g as y}from"./chunk-ODN5LVDJ.js";var u=y(f());var w={title:"Driver for connection and Third-Party Framework",mdFile:"./index.md",category:h,order:0},n=w;var m=[];var b={},j=b;var x=`<h1 id="driver-for-connection-and-third-party-framework" class="ngde">Driver for connection and Third-Party Framework<a title="Link to heading" class="ng-doc-header-link ngde" href="/en/plugin/datasource-wrapper-and-third-part-framework#driver-for-connection-and-third-party-framework"><ng-doc-icon icon="link-2" size="16" class="ngde"></ng-doc-icon></a></h1><p class="ngde">Kronos can be easily used with third-party frameworks by customizing the wrapper class that inherits the <code class="ngde">KronosDataSourceWrapper</code> interface.</p><h2 id="spring-example" class="ngde">Spring Example<a title="Link to heading" class="ng-doc-header-link ngde" href="/en/plugin/datasource-wrapper-and-third-part-framework#spring-example"><ng-doc-icon icon="link-2" size="16" class="ngde"></ng-doc-icon></a></h2><p class="ngde">The following is an example using Springboot + Kronos + JDK 17 + Maven + Kotlin 2.0.0 to demonstrate how to use Kronos with the Spring framework.</p><p class="ngde">It includes how to create a wrapper class based on <code class="ngde">spring-data-jdbc</code>, so that there is no need to introduce additional dependencies such as <code class="ngde">kronos-jvm-driver-wrapper</code>, and the database operation function can be realized only through <code class="ngde">kronos-core</code>.</p><ng-doc-blockquote class="ngde"><p class="ngde"><a href="https://github.com/Kronos-orm/kronos-spring-demo" class="ngde">https://github.com/Kronos-orm/kronos-spring-demo</a></p></ng-doc-blockquote><h3 id="1-dependencies" class="ngde">1. Dependencies<a title="Link to heading" class="ng-doc-header-link ngde" href="/en/plugin/datasource-wrapper-and-third-part-framework#1-dependencies"><ng-doc-icon icon="link-2" size="16" class="ngde"></ng-doc-icon></a></h3><p class="ngde">Introduce <code class="ngde">spring</code> related dependencies and <code class="ngde">kronos-core</code> dependencies ( see <a href="/documentation/zh-CN/getting-started/quick-start" class="ngde">Quick Start</a> for the introduction of <code class="ngde">compiler-plugin</code> plug-in))</p><pre class="ngde hljs"><code class="hljs language-xml code-lines ngde" lang="xml" name="" icon="" highlightedlines="[]"><span class="line ngde">
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
</span></code></pre><h3 id="2kronosdatasourcewrapper-implementation" class="ngde">2.KronosDataSourceWrapper implementation<a title="Link to heading" class="ng-doc-header-link ngde" href="/en/plugin/datasource-wrapper-and-third-part-framework#2kronosdatasourcewrapper-implementation"><ng-doc-icon icon="link-2" size="16" class="ngde"></ng-doc-icon></a></h3><h4 id="1-initialize-connection-information-and-jdbc-template" class="ngde">1. Initialize connection information and JDBC template<a title="Link to heading" class="ng-doc-header-link ngde" href="/en/plugin/datasource-wrapper-and-third-part-framework#1-initialize-connection-information-and-jdbc-template"><ng-doc-icon icon="link-2" size="16" class="ngde"></ng-doc-icon></a></h4><pre class="ngde hljs"><code class="hljs language-kotlin code-lines ngde" lang="kotlin" name="" icon="" highlightedlines="[]"><span class="line ngde"><span class="hljs-comment ngde">// Initialize connection information</span>
</span><span class="line ngde"><span class="hljs-keyword ngde">init</span> {
</span><span class="line ngde">    <span class="hljs-keyword ngde">val</span> conn = dataSource.connection
</span><span class="line ngde">    _metaUrl = conn.metaData.url
</span><span class="line ngde">    _metaDbType = DBType.fromName(conn.metaData.databaseProductName)
</span><span class="line ngde">    _userName = conn.metaData.userName ?: <span class="hljs-string ngde">""</span>
</span><span class="line ngde">    conn.close()
</span><span class="line ngde">}
</span><span class="line ngde">
</span><span class="line ngde"><span class="hljs-comment ngde">// NamedParameterJdbcTemplate is the Spring Data JDBC support for JDBC commands, which supports named parameters</span>
</span><span class="line ngde"><span class="hljs-keyword ngde">private</span> <span class="hljs-keyword ngde">val</span> namedJdbc: NamedParameterJdbcTemplate <span class="hljs-keyword ngde">by</span> lazy {
</span><span class="line ngde">    NamedParameterJdbcTemplate(dataSource)
</span><span class="line ngde">}
</span><span class="line ngde">
</span></code></pre><h4 id="2overload-the-springdatawrapper" class="ngde">2.overload the SpringDataWrapper<a title="Link to heading" class="ng-doc-header-link ngde" href="/en/plugin/datasource-wrapper-and-third-part-framework#2overload-the-springdatawrapper"><ng-doc-icon icon="link-2" size="16" class="ngde"></ng-doc-icon></a></h4><pre class="ngde hljs"><code class="hljs language-kotlin code-lines ngde" lang="kotlin" name="" icon="" highlightedlines="[]"><span class="line ngde"><span class="hljs-comment ngde">//1.query Map&#x3C;String, Any> list</span>
</span><span class="line ngde"><span class="hljs-keyword ngde">override</span> <span class="hljs-function ngde"><span class="hljs-keyword ngde">fun</span> <span class="hljs-title ngde">forList</span><span class="hljs-params ngde">(task: <span class="hljs-type ngde">KAtomicQueryTask</span>)</span></span>: List&#x3C;Map&#x3C;String, Any>> {
</span><span class="line ngde">    <span class="hljs-keyword ngde">return</span> namedJdbc.queryForList(task.sql, task.paramMap)
</span><span class="line ngde">}
</span><span class="line ngde">
</span><span class="line ngde"><span class="hljs-comment ngde">//2.query object list</span>
</span><span class="line ngde"><span class="hljs-keyword ngde">override</span> <span class="hljs-function ngde"><span class="hljs-keyword ngde">fun</span> <span class="hljs-title ngde">forList</span><span class="hljs-params ngde">(task: <span class="hljs-type ngde">KAtomicQueryTask</span>, kClass: <span class="hljs-type ngde">KClass</span>&#x3C;*>)</span></span>: List&#x3C;Any> {
</span><span class="line ngde">    <span class="hljs-keyword ngde">return</span> <span class="hljs-keyword ngde">if</span> (KPojo::<span class="hljs-keyword ngde">class</span>.isSuperclassOf(kClass)) namedJdbc.query(
</span><span class="line ngde">        task.sql,
</span><span class="line ngde">        task.paramMap,
</span><span class="line ngde">        DataClassRowMapper(kClass.java)
</span><span class="line ngde">    )
</span><span class="line ngde">    <span class="hljs-keyword ngde">else</span> namedJdbc.queryForList(task.sql, task.paramMap, kClass.java)
</span><span class="line ngde">}
</span><span class="line ngde">
</span><span class="line ngde"><span class="hljs-comment ngde">//3.query map, if query result is empty, return null</span>
</span><span class="line ngde"><span class="hljs-keyword ngde">override</span> <span class="hljs-function ngde"><span class="hljs-keyword ngde">fun</span> <span class="hljs-title ngde">forMap</span><span class="hljs-params ngde">(task: <span class="hljs-type ngde">KAtomicQueryTask</span>)</span></span>: Map&#x3C;String, Any>? {
</span><span class="line ngde">    <span class="hljs-keyword ngde">return</span> <span class="hljs-keyword ngde">try</span> {
</span><span class="line ngde">        namedJdbc.queryForMap(task.sql, task.paramMap)
</span><span class="line ngde">    } <span class="hljs-keyword ngde">catch</span> (e: DataAccessException) {
</span><span class="line ngde">        <span class="hljs-literal ngde">null</span>
</span><span class="line ngde">    }
</span><span class="line ngde">}
</span><span class="line ngde">
</span><span class="line ngde"><span class="hljs-comment ngde">//4.query object, if query result is empty, return null</span>
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
</span><span class="line ngde"><span class="hljs-comment ngde">//5.execute update, return the number of rows affected</span>
</span><span class="line ngde"><span class="hljs-keyword ngde">override</span> <span class="hljs-function ngde"><span class="hljs-keyword ngde">fun</span> <span class="hljs-title ngde">update</span><span class="hljs-params ngde">(task: <span class="hljs-type ngde">KAtomicActionTask</span>)</span></span>: <span class="hljs-built_in ngde">Int</span> {
</span><span class="line ngde">    <span class="hljs-keyword ngde">return</span> namedJdbc.update(task.sql, task.paramMap)
</span><span class="line ngde">}
</span><span class="line ngde">
</span><span class="line ngde"><span class="hljs-comment ngde">//6.execute batch update, return the number of rows affected</span>
</span><span class="line ngde"><span class="hljs-keyword ngde">override</span> <span class="hljs-function ngde"><span class="hljs-keyword ngde">fun</span> <span class="hljs-title ngde">batchUpdate</span><span class="hljs-params ngde">(task: <span class="hljs-type ngde">KronosAtomicBatchTask</span>)</span></span>: IntArray {
</span><span class="line ngde">    <span class="hljs-keyword ngde">return</span> namedJdbc.batchUpdate(task.sql, task.paramMapArr ?: emptyArray())
</span><span class="line ngde">}
</span><span class="line ngde">
</span><span class="line ngde"><span class="hljs-comment ngde">//7.transaction</span>
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
</span></code></pre><h4 id="3some-other-things" class="ngde">3.Some other things<a title="Link to heading" class="ng-doc-header-link ngde" href="/en/plugin/datasource-wrapper-and-third-part-framework#3some-other-things"><ng-doc-icon icon="link-2" size="16" class="ngde"></ng-doc-icon></a></h4><pre class="ngde hljs"><code class="hljs language-kotlin code-lines ngde" lang="kotlin" name="" icon="" highlightedlines="[]"><span class="line ngde"><span class="hljs-keyword ngde">companion</span> <span class="hljs-keyword ngde">object</span> {
</span><span class="line ngde">    <span class="hljs-comment ngde">//extension function to wrap JdbcTemplate in SpringDataWrapper</span>
</span><span class="line ngde">    <span class="hljs-function ngde"><span class="hljs-keyword ngde">fun</span> JdbcTemplate.<span class="hljs-title ngde">wrapper</span><span class="hljs-params ngde">()</span></span>: SpringDataWrapper {
</span><span class="line ngde">        <span class="hljs-keyword ngde">return</span> SpringDataWrapper(<span class="hljs-keyword ngde">this</span>.dataSource!!)
</span><span class="line ngde">    }
</span><span class="line ngde">
</span><span class="line ngde">    <span class="hljs-comment ngde">//extension function to wrap NamedParameterJdbcTemplate in SpringDataWrapper</span>
</span><span class="line ngde">    <span class="hljs-function ngde"><span class="hljs-keyword ngde">fun</span> NamedParameterJdbcTemplate.<span class="hljs-title ngde">wrapper</span><span class="hljs-params ngde">()</span></span>: SpringDataWrapper {
</span><span class="line ngde">        <span class="hljs-keyword ngde">return</span> SpringDataWrapper(<span class="hljs-keyword ngde">this</span>.jdbcTemplate.dataSource!!)
</span><span class="line ngde">    }
</span><span class="line ngde">}
</span></code></pre><p class="ngde">All code can be found in: <a href="https://github.com/Kronos-orm/kronos-spring-demo/blob/main/src/main/kotlin/com/kotlinorm/kronosSpringDemo/controller/SpringDataWrapper.kt" class="ngde">SpringDataWrapper.kt</a></p><h2 id="other-frameworks" class="ngde">Other Frameworks<a title="Link to heading" class="ng-doc-header-link ngde" href="/en/plugin/datasource-wrapper-and-third-part-framework#other-frameworks"><ng-doc-icon icon="link-2" size="16" class="ngde"></ng-doc-icon></a></h2><p class="ngde">For frameworks that support named parameters, the syntax is almost the same as SpringDataWrapper.</p><p class="ngde">For other frameworks that only support sequential parameters, you can get the parsed SQL statement through <code class="ngde">KAtomicQueryTask.parsed()</code> or <code class="ngde">KAtomicActionyTask.parsed()</code> or <code class="ngde">KronosAtomicBatchTask.parsedArr()</code>, which contains the parameter name and parameter value array.</p><p class="ngde">The subsequent process is the same as SpringDataWrapper, please refer to <a href="https://github.com/Kronos-orm/Kronos-orm/blob/main/kronos-jvm-driver-wrapper/src/main/kotlin/com/kotlinorm/KronosBasicWrapper.kt" class="ngde">KronosBasicWrapper.kt</a>.</p>`,C=(()=>{let s=class s extends p{constructor(){super(),this.routePrefix="",this.pageType="guide",this.pageContent=x,this.page=n,this.demoAssets=j}};s.\u0275fac=function(e){return new(e||s)},s.\u0275cmp=c({type:s,selectors:[["ng-doc-page-en-plugin-datasource-wrapper-and-third-part-framework"]],standalone:!0,features:[o([{provide:p,useExisting:s},m,n.providers??[]]),r,i],decls:1,vars:0,template:function(e,v){e&1&&t(0,"ng-doc-page")},dependencies:[g],encapsulation:2,changeDetection:0});let a=s;return a})(),D=[d(l({},(0,u.isRoute)(n.route)?n.route:{}),{path:"",component:C,title:"Driver for connection and Third-Party Framework"})],L=D;export{C as DynamicComponent,L as default};
