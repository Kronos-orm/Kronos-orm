import{a as h}from"./chunk-5CP4K3WD.js";import{a as r}from"./chunk-CN3B2NJM.js";import{a as l}from"./chunk-TKZ7FAYE.js";import{G as v}from"./chunk-JSUCXAVI.js";import{Pb as o,jc as i,kc as t,ra as g,xb as d}from"./chunk-TLBD5JYT.js";import{a as p,b as c,g as u}from"./chunk-ODN5LVDJ.js";var C=u(v());var k={title:"\u8FDE\u63A5\u5230\u6570\u636E\u5E93",mdFile:"./index.md",category:h,order:0},n=k;var m=[];var b={},j=b;var y=`<h1 id="\u8FDE\u63A5\u5230\u6570\u636E\u5E93" class="ngde">\u8FDE\u63A5\u5230\u6570\u636E\u5E93<a title="Link to heading" class="ng-doc-header-link ngde" href="/zh-CN/database/connect-to-db#\u8FDE\u63A5\u5230\u6570\u636E\u5E93"><ng-doc-icon icon="link-2" size="16" class="ngde"></ng-doc-icon></a></h1><p class="ngde">Kronos\u8BBF\u95EE\u6570\u636E\u5E93\u901A\u8FC7<code class="ngde">KronosDataSourceWrapper</code>\u5B9E\u73B0\u3002</p><p class="ngde"><code class="ngde">KronosDataSourceWrapper</code>\u662F\u4E00\u4E2A\u63A5\u53E3\uFF0C\u662F\u5BF9\u6570\u636E\u5E93\u64CD\u4F5C\u7684\u5C01\u88C5\uFF0C\u5B83\u4E0D\u5173\u5FC3\u5177\u4F53\u7684\u6570\u636E\u5E93\u8FDE\u63A5\u7EC6\u8282\uFF0C\u4E0E\u5E73\u53F0\u65E0\u5173\uFF0C\u53EA\u5173\u5FC3\u6570\u636E\u5E93\u64CD\u4F5C\u7684\u903B\u8F91\uFF1A</p><ul class="ngde"><li class="ngde"><code class="ngde">dbType</code>\uFF1A\u6570\u636E\u5E93\u7C7B\u578B</li><li class="ngde"><code class="ngde">url</code>\uFF1A\u6570\u636E\u5E93\u8FDE\u63A5\u5730\u5740</li><li class="ngde"><code class="ngde">username</code>\uFF1A\u6570\u636E\u5E93\u7528\u6237\u540D</li><li class="ngde"><code class="ngde">query</code>\uFF1A\u6267\u884C\u67E5\u8BE2<ul class="ngde"><li class="ngde"><code class="ngde">List&#x3C;Map&#x3C;String, Any>></code>\uFF1A\u8FD4\u56DE\u67E5\u8BE2\u7ED3\u679C</li><li class="ngde"><code class="ngde">List&#x3C;T></code>\uFF1A\u8FD4\u56DE\u67E5\u8BE2\u7ED3\u679C\u7684\u7B2C\u4E00\u5217</li><li class="ngde"><code class="ngde">Map&#x3C;String, Any></code>\uFF1A\u8FD4\u56DE\u67E5\u8BE2\u7ED3\u679C\u7684\u7B2C\u4E00\u884C</li><li class="ngde"><code class="ngde">T</code>\uFF1A\u8FD4\u56DE\u67E5\u8BE2\u7ED3\u679C\u7684\u7B2C\u4E00\u884C\u7684\u7B2C\u4E00\u5217</li></ul></li><li class="ngde"><code class="ngde">execute</code>\uFF1A\u6267\u884C\u66F4\u65B0</li><li class="ngde"><code class="ngde">batch</code>\uFF1A\u6279\u91CF\u6267\u884C\u66F4\u65B0</li><li class="ngde"><code class="ngde">transaction</code>\uFF1A\u4E8B\u52A1</li></ul><ng-doc-blockquote type="note" class="ngde"><p class="ngde"><strong class="ngde">KronosDataSourceWrapper</strong>\u4EE5\u6269\u5C55\u7684\u5F62\u5F0F\u5728core\u4E2D\u5F15\u5165\uFF0C\u8FD9\u4F7F\u5F97<strong class="ngde">\u652F\u6301\u591A\u5E73\u53F0</strong>\u3001<strong class="ngde">\u65B0\u6570\u636E\u5E93\u6269\u5C55</strong>\u548C<strong class="ngde">\u7B2C\u4E09\u65B9\u6846\u67B6\u96C6\u6210</strong>\u6210\u4E3A\u53EF\u80FD\u3002</p></ng-doc-blockquote><h2 id="\u4F7F\u7528\u793A\u4F8B" class="ngde">\u4F7F\u7528\u793A\u4F8B<a title="Link to heading" class="ng-doc-header-link ngde" href="/zh-CN/database/connect-to-db#\u4F7F\u7528\u793A\u4F8B"><ng-doc-icon icon="link-2" size="16" class="ngde"></ng-doc-icon></a></h2><p class="ngde">\u5B98\u65B9\u63D0\u4F9B\u4E86jvm\u5E73\u53F0\u7684\u57FA\u4E8EJDBC\u7684\u6570\u636E\u5E93\u8FDE\u63A5\u63D2\u4EF6\uFF0C\u53EF\u4EE5\u901A\u8FC7\u4EE5\u4E0B\u65B9\u5F0F\u5F15\u5165\uFF1A</p><ng-doc-tab group="import" name="gradle(kts)" icon="gradlekts" class="ngde"><pre class="ngde hljs"><code class="hljs language-kotlin code-lines ngde" lang="kotlin" name="" icon="" highlightedlines="[]"><span class="line ngde">dependencies {
</span><span class="line ngde">    implementation(<span class="hljs-string ngde">"com.kotlinorm.kronos-jvm-driver-wrapper:1.0.0"</span>)
</span><span class="line ngde">}
</span></code></pre></ng-doc-tab><ng-doc-tab group="import" name="gradle(groovy)" icon="gradle" class="ngde"><pre class="ngde hljs"><code class="hljs language-groovy code-lines ngde" lang="groovy" name="" icon="" highlightedlines="[]"><span class="line ngde">dependencies {
</span><span class="line ngde">    implementation 'com.kotlinorm:kronos-jvm-driver-wrapper:1.0.0'
</span><span class="line ngde">}
</span></code></pre></ng-doc-tab><ng-doc-tab group="import" name="maven" icon="maven" class="ngde"><pre class="ngde hljs"><code class="hljs language-xml code-lines ngde" lang="xml" name="" icon="" highlightedlines="[]"><span class="line ngde"><span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">project</span>></span>
</span><span class="line ngde">  <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">dependencies</span>></span>
</span><span class="line ngde">    <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">dependency</span>></span>
</span><span class="line ngde">      <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">groupId</span>></span>com.kotlinorm<span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">groupId</span>></span>
</span><span class="line ngde">      <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">artifactId</span>></span>kronos-jvm-driver-wrapper<span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">artifactId</span>></span>
</span><span class="line ngde">      <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">version</span>></span>1.0.0<span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">version</span>></span>
</span><span class="line ngde">    <span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">dependency</span>></span>
</span><span class="line ngde">  <span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">dependencies</span>></span>
</span><span class="line ngde"><span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">project</span>></span>
</span></code></pre></ng-doc-tab><p class="ngde">\u9664\u6B64\u4E4B\u5916\uFF0C\u53EF\u4EE5\u901A\u8FC7\u5982<code class="ngde">kronos-spring-data-wrapper</code>\u3001<code class="ngde">kronos-jdbi-wrapper</code>\u3001<code class="ngde">kronos-mybatis-wrapper</code>\u7B49\u63D2\u4EF6\u5B9E\u73B0\u6570\u636E\u5E93\u8FDE\u63A5\uFF0C\u4E0ESpring Data\u3001JDBI\u3001MyBatis\u7B49\u6846\u67B6\u96C6\u6210\u3002</p><p class="ngde">\u4EE5\u4E0B\u662F\u5BF9\u4E8E<code class="ngde">kronos-jvm-driver-wrapper</code>\u7684\u4F7F\u7528\u793A\u4F8B</p><ng-doc-blockquote type="note" class="ngde"><p class="ngde"><strong class="ngde">BasicDataSource</strong>\u662FApache Commons DBCP\u7684\u4E00\u4E2A\u7B80\u5355\u7684\u6570\u636E\u6E90\u5B9E\u73B0\uFF0C\u60A8\u53EF\u4EE5\u66F4\u6362\u4E3A\u5176\u4ED6\u6570\u636E\u6E90\u5B9E\u73B0\u3002</p></ng-doc-blockquote><h3 id="1-mysql\u6570\u636E\u5E93\u8FDE\u63A5\u914D\u7F6E" class="ngde">1. Mysql\u6570\u636E\u5E93\u8FDE\u63A5\u914D\u7F6E<a title="Link to heading" class="ng-doc-header-link ngde" href="/zh-CN/database/connect-to-db#1-mysql\u6570\u636E\u5E93\u8FDE\u63A5\u914D\u7F6E"><ng-doc-icon icon="link-2" size="16" class="ngde"></ng-doc-icon></a></h3><ng-doc-tab group="Mysql" name="gradle(kts)" icon="gradlekts" class="ngde"><pre class="ngde hljs"><code class="hljs language-kotlin code-lines ngde" lang="kotlin" name="" icon="" highlightedlines="[]"><span class="line ngde">dependencies {
</span><span class="line ngde">    implementation(<span class="hljs-string ngde">"org.apache.commons:commons-dbcp2:latest.release"</span>)
</span><span class="line ngde">    implementation(<span class="hljs-string ngde">"com.mysql:mysql-connector-j:latest.release"</span>)
</span><span class="line ngde">    implementation(<span class="hljs-string ngde">"com.kotlinorm.kronos-jvm-driver-wrapper:1.0.0"</span>)
</span><span class="line ngde">}
</span></code></pre></ng-doc-tab><ng-doc-tab group="Mysql" name="gradle(groovy)" icon="gradle" class="ngde"><pre class="ngde hljs"><code class="hljs language-groovy code-lines ngde" lang="groovy" name="" icon="" highlightedlines="[]"><span class="line ngde">dependencies {
</span><span class="line ngde">    implementation 'org.apache.commons:commons-dbcp2:latest.release'
</span><span class="line ngde">    implementation 'com.mysql:mysql-connector-j:latest.release'
</span><span class="line ngde">    implementation 'com.kotlinorm:kronos-jvm-driver-wrapper:1.0.0'
</span><span class="line ngde">}
</span></code></pre></ng-doc-tab><ng-doc-tab group="Mysql" name="maven" icon="maven" class="ngde"><pre class="ngde hljs"><code class="hljs language-xml code-lines ngde" lang="xml" name="" icon="" highlightedlines="[]"><span class="line ngde"><span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">project</span>></span>
</span><span class="line ngde">  <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">dependencies</span>></span>
</span><span class="line ngde">    <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">dependency</span>></span>
</span><span class="line ngde">      <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">groupId</span>></span>org.apache.commons<span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">groupId</span>></span>
</span><span class="line ngde">      <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">artifactId</span>></span>commons-dbcp2<span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">artifactId</span>></span>
</span><span class="line ngde">      <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">version</span>></span>latest.release<span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">version</span>></span>
</span><span class="line ngde">    <span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">dependency</span>></span>
</span><span class="line ngde">    <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">dependency</span>></span>
</span><span class="line ngde">      <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">groupId</span>></span>com.mysql<span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">groupId</span>></span>
</span><span class="line ngde">      <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">artifactId</span>></span>mysql-connector-j<span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">artifactId</span>></span>
</span><span class="line ngde">      <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">version</span>></span>latest.release<span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">version</span>></span>
</span><span class="line ngde">    <span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">dependency</span>></span>
</span><span class="line ngde">    <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">dependency</span>></span>
</span><span class="line ngde">      <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">groupId</span>></span>com.kotlinorm<span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">groupId</span>></span>
</span><span class="line ngde">      <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">artifactId</span>></span>kronos-jvm-driver-wrapper<span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">artifactId</span>></span>
</span><span class="line ngde">      <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">version</span>></span>1.0.0<span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">version</span>></span>
</span><span class="line ngde">    <span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">dependency</span>></span>
</span><span class="line ngde">  <span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">dependencies</span>></span>
</span><span class="line ngde"><span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">project</span>></span>
</span></code></pre></ng-doc-tab><ng-doc-tab group="Mysql" name="MysqlKronosConfig.kt" icon="" class="ngde"><pre class="ngde hljs"><code class="hljs language-kotlin code-lines ngde" lang="kotlin" name="" icon="" highlightedlines="[]"><span class="line ngde"><span class="hljs-keyword ngde">import</span> com.kotlinorm.Kronos
</span><span class="line ngde">Kronos.apply {
</span><span class="line ngde">  dataSource = {
</span><span class="line ngde">    BasicDataSource().apply {
</span><span class="line ngde">        <span class="hljs-comment ngde">// if your database version is 8.0 or later, you need to add the following configuration</span>
</span><span class="line ngde">        driverClassName = <span class="hljs-string ngde">"com.mysql.cj.jdbc.Driver"</span>
</span><span class="line ngde">        <span class="hljs-comment ngde">// else you can use the following configuration</span>
</span><span class="line ngde">        <span class="hljs-comment ngde">// driverClassName = "com.mysql.jdbc.Driver"</span>
</span><span class="line ngde">        url = <span class="hljs-string ngde">"jdbc:mysql://localhost:3306/kronos?useUnicode=true&#x26;characterEncoding=utf-8&#x26;useSSL=false&#x26;serverTimezone=UTC"</span>
</span><span class="line ngde">        username = <span class="hljs-string ngde">"root"</span>
</span><span class="line ngde">        password = <span class="hljs-string ngde">"******"</span>
</span><span class="line ngde">    }
</span><span class="line ngde">  }
</span><span class="line ngde">}
</span></code></pre></ng-doc-tab><h3 id="2-postgresql\u6570\u636E\u5E93\u8FDE\u63A5\u914D\u7F6E" class="ngde">2. PostgreSQL\u6570\u636E\u5E93\u8FDE\u63A5\u914D\u7F6E<a title="Link to heading" class="ng-doc-header-link ngde" href="/zh-CN/database/connect-to-db#2-postgresql\u6570\u636E\u5E93\u8FDE\u63A5\u914D\u7F6E"><ng-doc-icon icon="link-2" size="16" class="ngde"></ng-doc-icon></a></h3><ng-doc-tab group="PostgreSQL" name="gradle(kts)" icon="gradlekts" class="ngde"><pre class="ngde hljs"><code class="hljs language-kotlin code-lines ngde" lang="kotlin" name="" icon="" highlightedlines="[]"><span class="line ngde">dependencies {
</span><span class="line ngde">    implementation(<span class="hljs-string ngde">"org.apache.commons:commons-dbcp2:latest.release"</span>)
</span><span class="line ngde">    implementation(<span class="hljs-string ngde">"org.postgresql:postgresql:latest.release"</span>)
</span><span class="line ngde">    implementation(<span class="hljs-string ngde">"com.kotlinorm.kronos-jvm-driver-wrapper:1.0.0"</span>)
</span><span class="line ngde">}
</span></code></pre></ng-doc-tab><ng-doc-tab group="PostgreSQL" name="gradle(groovy)" icon="gradle" class="ngde"><pre class="ngde hljs"><code class="hljs language-groovy code-lines ngde" lang="groovy" name="" icon="" highlightedlines="[]"><span class="line ngde">dependencies {
</span><span class="line ngde">    implementation 'org.apache.commons:commons-dbcp2:latest.release'
</span><span class="line ngde">    implementation 'org.postgresql:postgresql:latest.release'
</span><span class="line ngde">    implementation 'com.kotlinorm:kronos-jvm-driver-wrapper:1.0.0'
</span><span class="line ngde">}
</span></code></pre></ng-doc-tab><ng-doc-tab group="PostgreSQL" name="maven" icon="maven" class="ngde"><pre class="ngde hljs"><code class="hljs language-xml code-lines ngde" lang="xml" name="" icon="" highlightedlines="[]"><span class="line ngde">
</span><span class="line ngde"><span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">project</span>></span>
</span><span class="line ngde">  <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">dependencies</span>></span>
</span><span class="line ngde">    <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">dependency</span>></span>
</span><span class="line ngde">      <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">groupId</span>></span>org.apache.commons<span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">groupId</span>></span>
</span><span class="line ngde">      <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">artifactId</span>></span>commons-dbcp2<span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">artifactId</span>></span>
</span><span class="line ngde">      <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">version</span>></span>latest.release<span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">version</span>></span>
</span><span class="line ngde">    <span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">dependency</span>></span>
</span><span class="line ngde">    <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">dependency</span>></span>
</span><span class="line ngde">      <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">groupId</span>></span>org.postgresql<span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">groupId</span>></span>
</span><span class="line ngde">      <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">artifactId</span>></span>postgresql<span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">artifactId</span>></span>
</span><span class="line ngde">      <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">version</span>></span>latest.release<span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">version</span>></span>
</span><span class="line ngde">    <span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">dependency</span>></span>
</span><span class="line ngde">    <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">dependency</span>></span>
</span><span class="line ngde">      <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">groupId</span>></span>com.kotlinorm<span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">groupId</span>></span>
</span><span class="line ngde">      <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">artifactId</span>></span>kronos-jvm-driver-wrapper<span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">artifactId</span>></span>
</span><span class="line ngde">      <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">version</span>></span>1.0.0<span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">version</span>></span>
</span><span class="line ngde">    <span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">dependency</span>></span>
</span><span class="line ngde">  <span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">dependencies</span>></span>
</span><span class="line ngde"><span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">project</span>></span>
</span></code></pre></ng-doc-tab><ng-doc-tab group="PostgreSQL" name="PostgreSQLKronosConfig.kt" icon="" class="ngde"><pre class="ngde hljs"><code class="hljs language-kotlin code-lines ngde" lang="kotlin" name="" icon="" highlightedlines="[]"><span class="line ngde"><span class="hljs-keyword ngde">import</span> com.kotlinorm.Kronos
</span><span class="line ngde">Kronos.apply {
</span><span class="line ngde">  dataSource = {
</span><span class="line ngde">    BasicDataSource().apply {
</span><span class="line ngde">        driverClassName = <span class="hljs-string ngde">"org.postgresql.Driver"</span>
</span><span class="line ngde">        url = <span class="hljs-string ngde">"jdbc:postgresql://localhost:5432/kronos"</span>
</span><span class="line ngde">        username = <span class="hljs-string ngde">"postgres"</span>
</span><span class="line ngde">        password = <span class="hljs-string ngde">"******"</span>
</span><span class="line ngde">    }
</span><span class="line ngde">  }
</span><span class="line ngde">}
</span></code></pre></ng-doc-tab><h3 id="3-oracle\u6570\u636E\u5E93\u8FDE\u63A5\u914D\u7F6E" class="ngde">3. Oracle\u6570\u636E\u5E93\u8FDE\u63A5\u914D\u7F6E<a title="Link to heading" class="ng-doc-header-link ngde" href="/zh-CN/database/connect-to-db#3-oracle\u6570\u636E\u5E93\u8FDE\u63A5\u914D\u7F6E"><ng-doc-icon icon="link-2" size="16" class="ngde"></ng-doc-icon></a></h3><ng-doc-tab group="Oracle" name="gradle(kts)" icon="gradlekts" class="ngde"><pre class="ngde hljs"><code class="hljs language-kotlin code-lines ngde" lang="kotlin" name="" icon="" highlightedlines="[]"><span class="line ngde">dependencies {
</span><span class="line ngde">    implementation(<span class="hljs-string ngde">"org.apache.commons:commons-dbcp2:latest.release"</span>)
</span><span class="line ngde">    implementation(<span class="hljs-string ngde">"com.oracle.database.jdbc:ojdbc8:latest.release"</span>)
</span><span class="line ngde">    implementation(<span class="hljs-string ngde">"com.kotlinorm.kronos-jvm-driver-wrapper:1.0.0"</span>)
</span><span class="line ngde">}
</span></code></pre></ng-doc-tab><ng-doc-tab group="Oracle" name="gradle(groovy)" icon="gradle" class="ngde"><pre class="ngde hljs"><code class="hljs language-groovy code-lines ngde" lang="groovy" name="" icon="" highlightedlines="[]"><span class="line ngde">dependencies {
</span><span class="line ngde">    implementation 'org.apache.commons:commons-dbcp2:latest.release'
</span><span class="line ngde">    implementation 'com.oracle.database.jdbc:ojdbc8:latest.release'
</span><span class="line ngde">    implementation 'com.kotlinorm:kronos-jvm-driver-wrapper:1.0.0'
</span><span class="line ngde">}
</span></code></pre></ng-doc-tab><ng-doc-tab group="Oracle" name="maven" icon="maven" class="ngde"><pre class="ngde hljs"><code class="hljs language-xml code-lines ngde" lang="xml" name="" icon="" highlightedlines="[]"><span class="line ngde"><span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">project</span>></span>
</span><span class="line ngde">  <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">dependencies</span>></span>
</span><span class="line ngde">    <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">dependency</span>></span>
</span><span class="line ngde">      <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">groupId</span>></span>org.apache.commons<span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">groupId</span>></span>
</span><span class="line ngde">      <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">artifactId</span>></span>commons-dbcp2<span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">artifactId</span>></span>
</span><span class="line ngde">      <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">version</span>></span>latest.release<span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">version</span>></span>
</span><span class="line ngde">    <span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">dependency</span>></span>
</span><span class="line ngde">    <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">dependency</span>></span>
</span><span class="line ngde">      <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">groupId</span>></span>com.oracle.database.jdbc<span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">groupId</span>></span>
</span><span class="line ngde">      <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">artifactId</span>></span>ojdbc8<span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">artifactId</span>></span>
</span><span class="line ngde">      <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">version</span>></span>latest.release<span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">version</span>></span>
</span><span class="line ngde">    <span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">dependency</span>></span>
</span><span class="line ngde">    <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">dependency</span>></span>
</span><span class="line ngde">      <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">groupId</span>></span>com.kotlinorm<span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">groupId</span>></span>
</span><span class="line ngde">      <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">artifactId</span>></span>kronos-jvm-driver-wrapper<span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">artifactId</span>></span>
</span><span class="line ngde">      <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">version</span>></span>1.0.0<span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">version</span>></span>
</span><span class="line ngde">    <span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">dependency</span>></span>
</span><span class="line ngde">  <span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">dependencies</span>></span>
</span><span class="line ngde"><span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">project</span>></span>
</span></code></pre></ng-doc-tab><ng-doc-tab group="Oracle" name="OracleKronosConfig.kt" icon="" class="ngde"><pre class="ngde hljs"><code class="hljs language-kotlin code-lines ngde" lang="kotlin" name="" icon="" highlightedlines="[]"><span class="line ngde"><span class="hljs-keyword ngde">import</span> com.kotlinorm.Kronos
</span><span class="line ngde">Kronos.apply {
</span><span class="line ngde">  dataSource = {
</span><span class="line ngde">    BasicDataSource().apply {
</span><span class="line ngde">        driverClassName = <span class="hljs-string ngde">"oracle.jdbc.OracleDriver"</span>
</span><span class="line ngde">        <span class="hljs-comment ngde">// replaece the following with your Oracle database connection information</span>
</span><span class="line ngde">        url = <span class="hljs-string ngde">"jdbc:oracle:thin:@localhost:1521:FREEPDB1"</span>
</span><span class="line ngde">        username = <span class="hljs-string ngde">"system"</span>
</span><span class="line ngde">        password = <span class="hljs-string ngde">"******"</span>
</span><span class="line ngde">    }
</span><span class="line ngde">  }
</span><span class="line ngde">}
</span></code></pre></ng-doc-tab><h3 id="4-sql-server\u6570\u636E\u5E93\u8FDE\u63A5\u914D\u7F6E" class="ngde">4. SQL Server\u6570\u636E\u5E93\u8FDE\u63A5\u914D\u7F6E<a title="Link to heading" class="ng-doc-header-link ngde" href="/zh-CN/database/connect-to-db#4-sql-server\u6570\u636E\u5E93\u8FDE\u63A5\u914D\u7F6E"><ng-doc-icon icon="link-2" size="16" class="ngde"></ng-doc-icon></a></h3><ng-doc-tab group="SQL Server" name="gradle(kts)" icon="gradlekts" class="ngde"><pre class="ngde hljs"><code class="hljs language-kotlin code-lines ngde" lang="kotlin" name="" icon="" highlightedlines="[]"><span class="line ngde">dependencies {
</span><span class="line ngde">    implementation(<span class="hljs-string ngde">"org.apache.commons:commons-dbcp2:latest.release"</span>)
</span><span class="line ngde">    implementation(<span class="hljs-string ngde">"com.microsoft.sqlserver:mssql-jdbc:12.7.0.jre8-preview"</span>)
</span><span class="line ngde">    implementation(<span class="hljs-string ngde">"com.kotlinorm.kronos-jvm-driver-wrapper:1.0.0"</span>)
</span><span class="line ngde">}
</span></code></pre></ng-doc-tab><ng-doc-tab group="SQL Server" name="gradle(groovy)" icon="gradle" class="ngde"><pre class="ngde hljs"><code class="hljs language-groovy code-lines ngde" lang="groovy" name="" icon="" highlightedlines="[]"><span class="line ngde">dependencies {
</span><span class="line ngde">    implementation 'org.apache.commons:commons-dbcp2:latest.release'
</span><span class="line ngde">    implementation 'com.microsoft.sqlserver:mssql-jdbc:12.7.0.jre8-preview'
</span><span class="line ngde">    implementation 'com.kotlinorm:kronos-jvm-driver-wrapper:1.0.0'
</span><span class="line ngde">}
</span></code></pre></ng-doc-tab><ng-doc-tab group="SQL Server" name="maven" icon="maven" class="ngde"><pre class="ngde hljs"><code class="hljs language-xml code-lines ngde" lang="xml" name="" icon="" highlightedlines="[]"><span class="line ngde"><span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">project</span>></span>
</span><span class="line ngde">  <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">dependencies</span>></span>
</span><span class="line ngde">    <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">dependency</span>></span>
</span><span class="line ngde">      <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">groupId</span>></span>org.apache.commons<span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">groupId</span>></span>
</span><span class="line ngde">      <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">artifactId</span>></span>commons-dbcp2<span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">artifactId</span>></span>
</span><span class="line ngde">      <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">version</span>></span>latest.release<span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">version</span>></span>
</span><span class="line ngde">    <span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">dependency</span>></span>
</span><span class="line ngde">    <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">dependency</span>></span>
</span><span class="line ngde">      <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">groupId</span>></span>com.microsoft.sqlserver<span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">groupId</span>></span>
</span><span class="line ngde">      <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">artifactId</span>></span>mssql-jdbc<span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">artifactId</span>></span>
</span><span class="line ngde">      <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">version</span>></span>12.7.0.jre8-preview<span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">version</span>></span>
</span><span class="line ngde">    <span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">dependency</span>></span>
</span><span class="line ngde">    <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">dependency</span>></span>
</span><span class="line ngde">      <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">groupId</span>></span>com.kotlinorm<span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">groupId</span>></span>
</span><span class="line ngde">      <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">artifactId</span>></span>kronos-jvm-driver-wrapper<span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">artifactId</span>></span>
</span><span class="line ngde">      <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">version</span>></span>1.0.0<span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">version</span>></span>
</span><span class="line ngde">    <span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">dependency</span>></span>
</span><span class="line ngde">  <span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">dependencies</span>></span>
</span><span class="line ngde"><span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">project</span>></span>
</span></code></pre></ng-doc-tab><ng-doc-tab group="SQL Server" name="SQLServerKronosConfig.kt" icon="" class="ngde"><pre class="ngde hljs"><code class="hljs language-kotlin code-lines ngde" lang="kotlin" name="" icon="" highlightedlines="[]"><span class="line ngde"><span class="hljs-keyword ngde">import</span> com.kotlinorm.Kronos
</span><span class="line ngde">Kronos.apply {
</span><span class="line ngde">  dataSource = {
</span><span class="line ngde">    BasicDataSource().apply {
</span><span class="line ngde">        driverClassName = <span class="hljs-string ngde">"com.microsoft.sqlserver.jdbc.SQLServerDriver"</span>
</span><span class="line ngde">        url = <span class="hljs-string ngde">"jdbc:sqlserver://localhost:1433;databaseName=kronos;encrypt=true;trustServerCertificate=true"</span>
</span><span class="line ngde">        username = <span class="hljs-string ngde">"sa"</span>
</span><span class="line ngde">        password = <span class="hljs-string ngde">"******"</span>
</span><span class="line ngde">    }
</span><span class="line ngde">  }
</span><span class="line ngde">}
</span></code></pre></ng-doc-tab><h3 id="5-sqlite\u6570\u636E\u5E93\u8FDE\u63A5\u914D\u7F6E" class="ngde">5. SQLite\u6570\u636E\u5E93\u8FDE\u63A5\u914D\u7F6E<a title="Link to heading" class="ng-doc-header-link ngde" href="/zh-CN/database/connect-to-db#5-sqlite\u6570\u636E\u5E93\u8FDE\u63A5\u914D\u7F6E"><ng-doc-icon icon="link-2" size="16" class="ngde"></ng-doc-icon></a></h3><ng-doc-tab group="SQLite" name="gradle(kts)" icon="gradlekts" class="ngde"><pre class="ngde hljs"><code class="hljs language-kotlin code-lines ngde" lang="kotlin" name="" icon="" highlightedlines="[]"><span class="line ngde">dependencies {
</span><span class="line ngde">    implementation(<span class="hljs-string ngde">"org.apache.commons:commons-dbcp2:latest.release"</span>)
</span><span class="line ngde">    implementation(<span class="hljs-string ngde">"org.xerial:sqlite-jdbc:latest.release"</span>)
</span><span class="line ngde">    implementation(<span class="hljs-string ngde">"com.kotlinorm:kronos-jvm-driver-wrapper:1.0.0"</span>)
</span><span class="line ngde">}
</span></code></pre></ng-doc-tab><ng-doc-tab group="SQLite" name="gradle(groovy)" icon="gradle" class="ngde"><pre class="ngde hljs"><code class="hljs language-groovy code-lines ngde" lang="groovy" name="" icon="" highlightedlines="[]"><span class="line ngde">dependencies {
</span><span class="line ngde">    implementation 'org.apache.commons:commons-dbcp2:latest.release'
</span><span class="line ngde">    implementation 'org.xerial:sqlite-jdbc:latest.release'
</span><span class="line ngde">    implementation 'com.kotlinorm:kronos-jvm-driver-wrapper:1.0.0'
</span><span class="line ngde">}
</span></code></pre></ng-doc-tab><ng-doc-tab group="SQLite" name="maven" icon="maven" class="ngde"><pre class="ngde hljs"><code class="hljs language-xml code-lines ngde" lang="xml" name="" icon="" highlightedlines="[]"><span class="line ngde">
</span><span class="line ngde"><span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">project</span>></span>
</span><span class="line ngde">  <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">dependencies</span>></span>
</span><span class="line ngde">    <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">dependency</span>></span>
</span><span class="line ngde">      <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">groupId</span>></span>org.apache.commons<span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">groupId</span>></span>
</span><span class="line ngde">      <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">artifactId</span>></span>commons-dbcp2<span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">artifactId</span>></span>
</span><span class="line ngde">      <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">version</span>></span>latest.release<span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">version</span>></span>
</span><span class="line ngde">    <span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">dependency</span>></span>
</span><span class="line ngde">    <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">dependency</span>></span>
</span><span class="line ngde">      <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">groupId</span>></span>org.xerial<span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">groupId</span>></span>
</span><span class="line ngde">      <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">artifactId</span>></span>sqlite-jdbc<span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">artifactId</span>></span>
</span><span class="line ngde">      <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">version</span>></span>latest.release<span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">version</span>></span>
</span><span class="line ngde">    <span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">dependency</span>></span>
</span><span class="line ngde">    <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">dependency</span>></span>
</span><span class="line ngde">      <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">groupId</span>></span>com.kotlinorm<span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">groupId</span>></span>
</span><span class="line ngde">      <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">artifactId</span>></span>kronos-jvm-driver-wrapper<span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">artifactId</span>></span>
</span><span class="line ngde">      <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">version</span>></span>1.0.0<span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">version</span>></span>
</span><span class="line ngde">    <span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">dependency</span>></span>
</span><span class="line ngde">  <span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">dependencies</span>></span>
</span><span class="line ngde"><span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">project</span>></span>
</span></code></pre></ng-doc-tab><ng-doc-tab group="SQLite" name="SQLiteKronosConfig.kt" icon="" class="ngde"><pre class="ngde hljs"><code class="hljs language-kotlin code-lines ngde" lang="kotlin" name="" icon="" highlightedlines="[]"><span class="line ngde"><span class="hljs-keyword ngde">import</span> com.kotlinorm.Kronos
</span><span class="line ngde">Kronos.apply {
</span><span class="line ngde">  dataSource = {
</span><span class="line ngde">    BasicDataSource().apply {
</span><span class="line ngde">        driverClassName = <span class="hljs-string ngde">"org.sqlite.JDBC"</span>
</span><span class="line ngde">        url = <span class="hljs-string ngde">"jdbc:sqlite:/path/to/your/database.db"</span>
</span><span class="line ngde">    }
</span><span class="line ngde">  }
</span><span class="line ngde">}
</span></code></pre></ng-doc-tab>`,f=(()=>{let s=class s extends l{constructor(){super(),this.routePrefix="",this.pageType="guide",this.pageContent=y,this.page=n,this.demoAssets=j}};s.\u0275fac=function(e){return new(e||s)},s.\u0275cmp=g({type:s,selectors:[["ng-doc-page-zh-cn-database-connect-to-db"]],standalone:!0,features:[i([{provide:l,useExisting:s},m,n.providers??[]]),d,t],decls:1,vars:0,template:function(e,S){e&1&&o(0,"ng-doc-page")},dependencies:[r],encapsulation:2,changeDetection:0});let a=s;return a})(),I=[c(p({},(0,C.isRoute)(n.route)?n.route:{}),{path:"",component:f,title:"\u8FDE\u63A5\u5230\u6570\u636E\u5E93"})],M=I;export{f as DynamicComponent,M as default};
