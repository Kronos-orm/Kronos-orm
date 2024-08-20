import{a as h}from"./chunk-FJ26F7HW.js";import{a as r}from"./chunk-CN3B2NJM.js";import{a as l}from"./chunk-TKZ7FAYE.js";import{G as b}from"./chunk-JSUCXAVI.js";import{Pb as o,jc as i,kc as t,ra as d,xb as g}from"./chunk-TLBD5JYT.js";import{a as p,b as c,g as C}from"./chunk-ODN5LVDJ.js";var u=C(b());var v={title:"Connect to DB",mdFile:"./index.md",category:h,order:0},n=v;var m=[];var k={},j=k;var f=`<h1 id="connect-to-db" class="ngde">Connect to DB<a title="Link to heading" class="ng-doc-header-link ngde" href="/en/database/connect-to-db#connect-to-db"><ng-doc-icon icon="link-2" size="16" class="ngde"></ng-doc-icon></a></h1><p class="ngde">Kronos accesses the database through <code class="ngde">KronosDataSourceWrapper</code>.</p><p class="ngde"><code class="ngde">KronosDataSourceWrapper</code> is an interface that encapsulates database operations. It does not care about the specific database connection details and is independent of the platform. It only cares about the logic of database operations:</p><ul class="ngde"><li class="ngde"><code class="ngde">dbType</code>: database type</li><li class="ngde"><code class="ngde">url</code>: database connection address</li><li class="ngde"><code class="ngde">username</code>: database username</li><li class="ngde"><code class="ngde">query</code>: execute query</li><li class="ngde"><code class="ngde">List&#x3C;Map&#x3C;String, Any>></code>: return query results</li><li class="ngde"><code class="ngde">List&#x3C;T></code>: return the first column of the query results</li><li class="ngde"><code class="ngde">Map&#x3C;String, Any></code>: return the first row of the query results</li><li class="ngde"><code class="ngde">T</code>: return the first column of the first row of the query results</li><li class="ngde"><code class="ngde">execute</code>: execute update</li><li class="ngde"><code class="ngde">batch</code>: batch update</li><li class="ngde"><code class="ngde">transaction</code>: transaction</li></ul><ng-doc-blockquote type="note" class="ngde"><p class="ngde"><strong class="ngde">KronosDataSourceWrapper</strong> is introduced in the core as an extension, which makes it possible to <strong class="ngde">support multiple platforms</strong>, <strong class="ngde">new database extensions</strong> and <strong class="ngde">third-party framework integration</strong>.</p></ng-doc-blockquote><h2 id="usage-example" class="ngde">Usage example<a title="Link to heading" class="ng-doc-header-link ngde" href="/en/database/connect-to-db#usage-example"><ng-doc-icon icon="link-2" size="16" class="ngde"></ng-doc-icon></a></h2><p class="ngde">Officially provides a JDBC-based database connection plug-in for the jvm platform, which can be introduced in the following ways:</p><ng-doc-tab group="import" name="gradle(kts)" icon="gradlekts" class="ngde"><pre class="ngde hljs"><code class="hljs language-kotlin code-lines ngde" lang="kotlin" name="" icon="" highlightedlines="[]"><span class="line ngde">dependencies {
</span><span class="line ngde">implementation(<span class="hljs-string ngde">"com.kotlinorm.kronos-jvm-driver-wrapper:1.0.0"</span>)
</span><span class="line ngde">}
</span></code></pre></ng-doc-tab><ng-doc-tab group="import" name="gradle(groovy)" icon="gradle" class="ngde"><pre class="ngde hljs"><code class="hljs language-groovy code-lines ngde" lang="groovy" name="" icon="" highlightedlines="[]"><span class="line ngde">dependencies {
</span><span class="line ngde">implementation 'com.kotlinorm:kronos-jvm-driver-wrapper:1.0.0'
</span><span class="line ngde">}
</span></code></pre></ng-doc-tab><ng-doc-tab group="import" name="maven" icon="maven" class="ngde"><pre class="ngde hljs"><code class="hljs language-xml code-lines ngde" lang="xml" name="" icon="" highlightedlines="[]"><span class="line ngde"><span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">project</span>></span>
</span><span class="line ngde"><span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">dependencies</span>></span>
</span><span class="line ngde"><span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">dependency</span>></span>
</span><span class="line ngde"><span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">groupId</span>></span>com.kotlinorm<span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">groupId</span>></span>
</span><span class="line ngde"><span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">artifactId</span>></span>kronos-jvm-driver-wrapper<span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">artifactId</span>></span>
</span><span class="line ngde"><span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">version</span>></span>1.0.0<span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">version</span>></span>
</span><span class="line ngde"><span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">dependency</span>></span>
</span><span class="line ngde"><span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">dependencies</span>></span>
</span><span class="line ngde"><span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">project</span>></span>
</span></code></pre></ng-doc-tab><p class="ngde">In addition, you can use plugins such as <code class="ngde">kronos-spring-data-wrapper</code>, <code class="ngde">kronos-jdbi-wrapper</code>, <code class="ngde">kronos-mybatis-wrapper</code> to connect to the database and integrate with Spring Data, JDBI, MyBatis and other frameworks.</p><p class="ngde">The following is an example of using <code class="ngde">kronos-jvm-driver-wrapper</code></p><ng-doc-blockquote type="note" class="ngde"><p class="ngde"><strong class="ngde">BasicDataSource</strong> is a simple data source implementation of Apache Commons DBCP. You can replace it with other data source implementations.</p></ng-doc-blockquote><h3 id="1-mysql-database-connection-configuration" class="ngde">1. Mysql database connection configuration<a title="Link to heading" class="ng-doc-header-link ngde" href="/en/database/connect-to-db#1-mysql-database-connection-configuration"><ng-doc-icon icon="link-2" size="16" class="ngde"></ng-doc-icon></a></h3><ng-doc-tab group="Mysql" name="gradle(kts)" icon="gradlekts" class="ngde"><pre class="ngde hljs"><code class="hljs language-kotlin code-lines ngde" lang="kotlin" name="" icon="" highlightedlines="[]"><span class="line ngde">dependencies {
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
</span></code></pre></ng-doc-tab><h3 id="2-postgresql-database-connection-configuration" class="ngde">2. PostgreSQL database connection configuration<a title="Link to heading" class="ng-doc-header-link ngde" href="/en/database/connect-to-db#2-postgresql-database-connection-configuration"><ng-doc-icon icon="link-2" size="16" class="ngde"></ng-doc-icon></a></h3><ng-doc-tab group="PostgreSQL" name="gradle(kts)" icon="gradlekts" class="ngde"><pre class="ngde hljs"><code class="hljs language-kotlin code-lines ngde" lang="kotlin" name="" icon="" highlightedlines="[]"><span class="line ngde">dependencies {
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
</span></code></pre></ng-doc-tab><h3 id="3-oracle-database-connection-configuration" class="ngde">3. Oracle database connection configuration<a title="Link to heading" class="ng-doc-header-link ngde" href="/en/database/connect-to-db#3-oracle-database-connection-configuration"><ng-doc-icon icon="link-2" size="16" class="ngde"></ng-doc-icon></a></h3><ng-doc-tab group="Oracle" name="gradle(kts)" icon="gradlekts" class="ngde"><pre class="ngde hljs"><code class="hljs language-kotlin code-lines ngde" lang="kotlin" name="" icon="" highlightedlines="[]"><span class="line ngde">dependencies {
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
</span></code></pre></ng-doc-tab><h3 id="4-sql-server-database-connection-configuration" class="ngde">4. SQL Server database connection configuration<a title="Link to heading" class="ng-doc-header-link ngde" href="/en/database/connect-to-db#4-sql-server-database-connection-configuration"><ng-doc-icon icon="link-2" size="16" class="ngde"></ng-doc-icon></a></h3><ng-doc-tab group="SQL Server" name="gradle(kts)" icon="gradlekts" class="ngde"><pre class="ngde hljs"><code class="hljs language-kotlin code-lines ngde" lang="kotlin" name="" icon="" highlightedlines="[]"><span class="line ngde">dependencies {
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
</span></code></pre></ng-doc-tab><h3 id="5-sqlite-database-connection-configuration" class="ngde">5. SQLite database connection configuration<a title="Link to heading" class="ng-doc-header-link ngde" href="/en/database/connect-to-db#5-sqlite-database-connection-configuration"><ng-doc-icon icon="link-2" size="16" class="ngde"></ng-doc-icon></a></h3><ng-doc-tab group="SQLite" name="gradle(kts)" icon="gradlekts" class="ngde"><pre class="ngde hljs"><code class="hljs language-kotlin code-lines ngde" lang="kotlin" name="" icon="" highlightedlines="[]"><span class="line ngde">dependencies {
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
</span></code></pre></ng-doc-tab>`,y=(()=>{let s=class s extends l{constructor(){super(),this.routePrefix="",this.pageType="guide",this.pageContent=f,this.page=n,this.demoAssets=j}};s.\u0275fac=function(e){return new(e||s)},s.\u0275cmp=d({type:s,selectors:[["ng-doc-page-en-database-connect-to-db"]],standalone:!0,features:[i([{provide:l,useExisting:s},m,n.providers??[]]),g,t],decls:1,vars:0,template:function(e,q){e&1&&o(0,"ng-doc-page")},dependencies:[r],encapsulation:2,changeDetection:0});let a=s;return a})(),I=[c(p({},(0,u.isRoute)(n.route)?n.route:{}),{path:"",component:y,title:"Connect to DB"})],M=I;export{y as DynamicComponent,M as default};
