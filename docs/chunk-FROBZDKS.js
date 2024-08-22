import{a as j}from"./chunk-Z4J7AABF.js";import{a as p}from"./chunk-ZO2WZBJK.js";import"./chunk-D3GIATC6.js";import{a as h}from"./chunk-HNPVF6HZ.js";import{a as l}from"./chunk-YGVJYOZD.js";import{H as x}from"./chunk-HT2ZSXJU.js";import{Pb as t,jc as o,kc as r,ra as c,xb as i}from"./chunk-W2MTHNV2.js";import{a as d,b as g,g as f}from"./chunk-ODN5LVDJ.js";var k=f(x());var C={title:"Quick Start",mdFile:"./index.md",category:j,order:1,imports:[p],demos:{AnimateLogoComponent:p}},n=C;var m=[];var v={AnimateLogoComponent:[{title:"TypeScript",code:`<pre class="ngde hljs"><code lang="typescript" class="hljs language-typescript code-lines ngde"><span class="line ngde"><span class="hljs-keyword ngde">import</span> {
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
</span></code></pre>`}]},u=v;var b=`<h1 id="quick-start" class="ngde">Quick Start<a title="Link to heading" class="ng-doc-header-link ngde" href="/en/getting-started/quick-start#quick-start"><ng-doc-icon icon="link-2" size="16" class="ngde"></ng-doc-icon></a></h1><ng-doc-demo componentname="AnimateLogoComponent" indexable="false" class="ngde"><div id="options" class="ngde">{"container":false}</div></ng-doc-demo><h2 id="add-dependencies" class="ngde">Add dependencies:<a title="Link to heading" class="ng-doc-header-link ngde" href="/en/getting-started/quick-start#add-dependencies"><ng-doc-icon icon="link-2" size="16" class="ngde"></ng-doc-icon></a></h2><p class="ngde">You can use Kronos in your project by simply importing the <code class="ngde">kronos-core</code> module and the <code class="ngde">kronos-compiler-plugin</code> plugin.</p><ng-doc-tab group="import" name="gradle(kts)" icon="gradlekts" class="ngde"><pre class="ngde hljs"><code class="hljs language-kotlin code-lines ngde" lang="kotlin" name="" icon="" highlightedlines="[]"><span class="line ngde">    dependencies {
</span><span class="line ngde">        implementation(<span class="hljs-string ngde">"com.kotlinorm.kronos-core:2.0.0"</span>) <span class="hljs-comment ngde">// for the basic ORM feature</span>
</span><span class="line ngde">    }
</span><span class="line ngde">    
</span><span class="line ngde">    plugins {
</span><span class="line ngde">        id(<span class="hljs-string ngde">"com.kotlinorm.kronos-gradle-plugin"</span>) version <span class="hljs-string ngde">"2.0.0"</span> <span class="hljs-comment ngde">// for the compile-time support</span>
</span><span class="line ngde">    }
</span></code></pre></ng-doc-tab><ng-doc-tab group="import" name="gradle(groovy)" icon="gradle" class="ngde"><pre class="ngde hljs"><code class="hljs language-groovy code-lines ngde" lang="groovy" name="" icon="" highlightedlines="[]"><span class="line ngde">    dependencies {
</span><span class="line ngde">        implementation 'com.kotlinorm:kronos-core:2.0.0' // for the basic ORM feature
</span><span class="line ngde">    }
</span><span class="line ngde">    
</span><span class="line ngde">    plugins {
</span><span class="line ngde">        id 'com.kotlinorm.kronos-gradle-plugin' version '2.0.0' // for the compile-time support
</span><span class="line ngde">    }
</span></code></pre></ng-doc-tab><ng-doc-tab group="import" name="maven" icon="maven" class="ngde"><pre class="ngde hljs"><code class="hljs language-xml code-lines ngde" lang="xml" name="" icon="" highlightedlines="[]"><span class="line ngde"><span class="hljs-comment ngde">&#x3C;!--Add the plugin in your pom.xml file:--></span>
</span><span class="line ngde"><span class="hljs-comment ngde">&#x3C;!--Please refer to the [https://kotlinlang.org/docs/all-open-plugin.html#maven] for the detailed information.--></span>
</span><span class="line ngde"><span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">project</span>></span>
</span><span class="line ngde">  <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">dependencies</span>></span>
</span><span class="line ngde">    <span class="hljs-comment ngde">&#x3C;!--kronos-core provides the basic ORM feature--></span>
</span><span class="line ngde">    <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">dependency</span>></span>
</span><span class="line ngde">      <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">groupId</span>></span>com.kotlinorm<span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">groupId</span>></span>
</span><span class="line ngde">      <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">artifactId</span>></span>kronos-core<span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">artifactId</span>></span>
</span><span class="line ngde">      <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">version</span>></span>2.0.0<span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">version</span>></span>
</span><span class="line ngde">    <span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">dependency</span>></span>
</span><span class="line ngde">  <span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">dependencies</span>></span>
</span><span class="line ngde">
</span><span class="line ngde">  <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">build</span>></span>
</span><span class="line ngde">    <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">plugins</span>></span>
</span><span class="line ngde">        <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">plugin</span>></span>
</span><span class="line ngde">            <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">groupId</span>></span>org.jetbrains.kotlin<span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">groupId</span>></span>
</span><span class="line ngde">            <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">artifactId</span>></span>kotlin-maven-plugin<span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">artifactId</span>></span>
</span><span class="line ngde">            <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">extensions</span>></span>true<span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">extensions</span>></span>
</span><span class="line ngde">            <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">configuration</span>></span>
</span><span class="line ngde">                <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">compilerPlugins</span>></span>
</span><span class="line ngde">                    <span class="hljs-comment ngde">&#x3C;!--kronos-maven-plugin provides the compile-time support--></span>
</span><span class="line ngde">                    <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">plugin</span>></span>kronos-maven-plugin<span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">plugin</span>></span>
</span><span class="line ngde">                <span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">compilerPlugins</span>></span>
</span><span class="line ngde">            <span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">configuration</span>></span>
</span><span class="line ngde">            <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">dependencies</span>></span>
</span><span class="line ngde">                <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">dependency</span>></span>
</span><span class="line ngde">                    <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">groupId</span>></span>com.kotlinorm<span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">groupId</span>></span>
</span><span class="line ngde">                    <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">artifactId</span>></span>kronos-maven-plugin<span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">artifactId</span>></span>
</span><span class="line ngde">                    <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">version</span>></span>\${kronos.version}<span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">version</span>></span>
</span><span class="line ngde">                <span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">dependency</span>></span>
</span><span class="line ngde">            <span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">dependencies</span>></span>
</span><span class="line ngde">        <span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">plugin</span>></span>
</span><span class="line ngde">    <span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">plugins</span>></span>
</span><span class="line ngde">  <span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">build</span>></span>
</span><span class="line ngde"><span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">project</span>></span>
</span></code></pre></ng-doc-tab><p class="ngde">At the same time, we provide a variety of optional dependencies such as logging (<code class="ngde">kronos-logging</code>), database operation driver wrapper (<code class="ngde">kronos-jvm-driver-wrapper</code>).</p><p class="ngde"><code class="ngde">kronos-jvm-driver-wrapper</code> is an optional module that provides a driver wrapper based on JDBC for the jvm platform. Of course, you can use other official driver wrappers or write your own wrapper class to easily use with third-party frameworks (such as SpringData, Mybatis, Hibernate, Jdbi, etc.) (refer to <a href="/documentation/en/plugin/datasource-wrapper-and-third-part-framework" class="ngde">this article</a>).</p><p class="ngde">You can find some examples of how to start a project <a href="https://github.com/Kronos-orm?tab=repositories" class="ngde">here</a>.</p><h2 id="configuring-the-database" class="ngde">Configuring the Database:<a title="Link to heading" class="ng-doc-header-link ngde" href="/en/getting-started/quick-start#configuring-the-database"><ng-doc-icon icon="link-2" size="16" class="ngde"></ng-doc-icon></a></h2><p class="ngde">Here, we will only discuss the usage of the <code class="ngde">kronos-jvm-driver-wrapper</code> module with MySQL. The usage for other modules is similar. For specifics, please refer to <a href="/documentation/en/database/connect-to-db" class="ngde">Connecting to the Database</a>.</p><p class="ngde">Dependencies such as <code class="ngde">commons-dbcp2</code> and <code class="ngde">mysql-connector-java</code> need to be included.</p><ng-doc-tab group="KronosConfig" name="KronosConfig.kt" icon="" class="ngde"><pre class="ngde hljs"><code class="hljs language-kotlin code-lines ngde" lang="kotlin" name="" icon="" highlightedlines="[]"><span class="line ngde"><span class="hljs-keyword ngde">import</span> com.kotlinorm.Kronos
</span><span class="line ngde">Kronos.apply {
</span><span class="line ngde">  dataSource = {
</span><span class="line ngde">    BasicDataSource().apply {
</span><span class="line ngde">        driverClassName = <span class="hljs-string ngde">"com.mysql.cj.jdbc.Driver"</span>
</span><span class="line ngde">        url = <span class="hljs-string ngde">"jdbc:mysql://localhost:3306/kronos?useUnicode=true&#x26;characterEncoding=utf-8&#x26;useSSL=false&#x26;serverTimezone=UTC"</span>
</span><span class="line ngde">        username = <span class="hljs-string ngde">"root"</span>
</span><span class="line ngde">        password = <span class="hljs-string ngde">"******"</span>
</span><span class="line ngde">    }
</span><span class="line ngde">  }
</span></code></pre></ng-doc-tab><p class="ngde">When using other databases or non-JVM platforms, the corresponding driver and configuration need to be used.</p><h2 id="writing-entity-classes" class="ngde">Writing Entity Classes:<a title="Link to heading" class="ng-doc-header-link ngde" href="/en/getting-started/quick-start#writing-entity-classes"><ng-doc-icon icon="link-2" size="16" class="ngde"></ng-doc-icon></a></h2><ng-doc-tab group="KPojo" name="Director.kt" icon="" class="ngde"><pre class="ngde hljs"><code class="hljs language-kotlin code-lines ngde" lang="kotlin" name="" icon="" highlightedlines="[]"><span class="line ngde"><span class="hljs-keyword ngde">data</span> <span class="hljs-keyword ngde">class</span> <span class="hljs-title class_ ngde">Director</span>(
</span><span class="line ngde">    <span class="hljs-meta ngde">@PrimaryKey(identity = true)</span>
</span><span class="line ngde">    <span class="hljs-keyword ngde">var</span> id: <span class="hljs-built_in ngde">Int</span>? = <span class="hljs-number ngde">0</span>,
</span><span class="line ngde">    <span class="hljs-keyword ngde">var</span> name: String? = <span class="hljs-string ngde">""</span>,
</span><span class="line ngde">    <span class="hljs-keyword ngde">var</span> age: <span class="hljs-built_in ngde">Int</span>? = <span class="hljs-number ngde">0</span>,
</span><span class="line ngde">    <span class="hljs-keyword ngde">var</span> movies: List&#x3C;Movie>? = emptyList(),
</span><span class="line ngde">    <span class="hljs-meta ngde">@CreateTime</span>
</span><span class="line ngde">    <span class="hljs-meta ngde">@DateTimeFormat(<span class="hljs-string ngde">"yyyy-MM-dd HH:mm:ss"</span>)</span>
</span><span class="line ngde">    <span class="hljs-keyword ngde">var</span> createTime: String? = <span class="hljs-string ngde">""</span>,
</span><span class="line ngde">    <span class="hljs-meta ngde">@updateTime</span>
</span><span class="line ngde">    <span class="hljs-meta ngde">@DateTimeFormat(<span class="hljs-string ngde">"yyyy-MM-dd HH:mm:ss"</span>)</span>
</span><span class="line ngde">    <span class="hljs-keyword ngde">var</span> updateTime: String? = <span class="hljs-string ngde">""</span>,
</span><span class="line ngde">    <span class="hljs-meta ngde">@LogicDelete</span>
</span><span class="line ngde">    <span class="hljs-keyword ngde">var</span> deleted: <span class="hljs-built_in ngde">Boolean</span>? = <span class="hljs-literal ngde">false</span>
</span><span class="line ngde">): KPojo
</span></code></pre></ng-doc-tab><ng-doc-tab group="KPojo" name="Movie.kt" icon="" class="ngde"><pre class="ngde hljs"><code class="hljs language-kotlin code-lines ngde" lang="kotlin" name="" icon="" highlightedlines="[]"><span class="line ngde"><span class="hljs-meta ngde">@Table(name = <span class="hljs-string ngde">"tb_movie"</span>)</span>
</span><span class="line ngde"><span class="hljs-meta ngde">@TableIndex(<span class="hljs-string ngde">"idx_name_director"</span>, [<span class="hljs-string ngde">"name"</span>, <span class="hljs-string ngde">"director_id"</span>], Mysql.KIndexType.UNIQUE, Mysql.KIndexMethod.BTREE)</span>
</span><span class="line ngde"><span class="hljs-keyword ngde">data</span> <span class="hljs-keyword ngde">class</span> <span class="hljs-title class_ ngde">Movie</span>(
</span><span class="line ngde">    <span class="hljs-meta ngde">@PrimaryKey(identity = true)</span>
</span><span class="line ngde">    <span class="hljs-keyword ngde">var</span> id: <span class="hljs-built_in ngde">Int</span>? = <span class="hljs-number ngde">0</span>,
</span><span class="line ngde">    <span class="hljs-meta ngde">@Column(<span class="hljs-string ngde">"name"</span>)</span>
</span><span class="line ngde">    <span class="hljs-meta ngde">@ColumnType(CHAR)</span>
</span><span class="line ngde">    <span class="hljs-keyword ngde">var</span> name: String? = <span class="hljs-string ngde">""</span>,
</span><span class="line ngde">    <span class="hljs-keyword ngde">var</span> directorId: <span class="hljs-built_in ngde">Long</span>? = <span class="hljs-number ngde">0</span>,
</span><span class="line ngde">    <span class="hljs-meta ngde">@Reference([<span class="hljs-string ngde">"director_id"</span>], [<span class="hljs-string ngde">"id"</span>])</span>
</span><span class="line ngde">    <span class="hljs-keyword ngde">var</span> director: Director? = <span class="hljs-string ngde">""</span>,
</span><span class="line ngde">    <span class="hljs-keyword ngde">var</span> releaseTime: String? = <span class="hljs-string ngde">""</span>,
</span><span class="line ngde">    <span class="hljs-meta ngde">@LogicDelete</span>
</span><span class="line ngde">    <span class="hljs-meta ngde">@Default(<span class="hljs-string ngde">"0"</span>)</span>
</span><span class="line ngde">    <span class="hljs-keyword ngde">var</span> deleted: <span class="hljs-built_in ngde">Boolean</span>? = <span class="hljs-literal ngde">false</span>,
</span><span class="line ngde">    <span class="hljs-meta ngde">@CreateTime</span>
</span><span class="line ngde">    <span class="hljs-keyword ngde">var</span> createTime: LocalDateTime? = <span class="hljs-string ngde">""</span>,
</span><span class="line ngde">    <span class="hljs-meta ngde">@updateTime</span>
</span><span class="line ngde">    <span class="hljs-keyword ngde">var</span> updateTime: Date? = <span class="hljs-string ngde">""</span>
</span><span class="line ngde">): KPojo
</span></code></pre></ng-doc-tab><h2 id="using-kronos" class="ngde">Using Kronos:<a title="Link to heading" class="ng-doc-header-link ngde" href="/en/getting-started/quick-start#using-kronos"><ng-doc-icon icon="link-2" size="16" class="ngde"></ng-doc-icon></a></h2><ng-doc-tab group="Kronos" name="Kronos.kt" icon="" class="ngde"><pre class="ngde hljs"><code class="hljs language-kotlin code-lines ngde" lang="kotlin" name="" icon="" highlightedlines="[]"><span class="line ngde"><span class="hljs-keyword ngde">val</span> director: Director = Director(
</span><span class="line ngde">    id = <span class="hljs-number ngde">1</span>,
</span><span class="line ngde">    name = <span class="hljs-string ngde">"Kronos"</span>,
</span><span class="line ngde">    age = <span class="hljs-number ngde">18</span>
</span><span class="line ngde">)
</span><span class="line ngde">
</span><span class="line ngde">director.insert(director)
</span><span class="line ngde">
</span><span class="line ngde">director.update().<span class="hljs-keyword ngde">set</span> { it.name = <span class="hljs-string ngde">"Kronos ORM"</span> }.<span class="hljs-keyword ngde">where</span> { it.id == <span class="hljs-number ngde">1</span> }.execute()
</span><span class="line ngde">
</span><span class="line ngde"><span class="hljs-keyword ngde">val</span> directors: List&#x3C;Director> = director.select().<span class="hljs-keyword ngde">where</span> { it.id == <span class="hljs-number ngde">1</span> }.queryList()
</span><span class="line ngde">
</span><span class="line ngde"><span class="hljs-keyword ngde">val</span> movies: List&#x3C;Movie> = Movie().select().<span class="hljs-keyword ngde">where</span> { it.director!!.id == director.id.value }.queryList()
</span></code></pre></ng-doc-tab>`,w=(()=>{let s=class s extends l{constructor(){super(),this.routePrefix="",this.pageType="guide",this.pageContent=b,this.page=n,this.demoAssets=u}};s.\u0275fac=function(e){return new(e||s)},s.\u0275cmp=c({type:s,selectors:[["ng-doc-page-en-getting-started-quick-start"]],standalone:!0,features:[o([{provide:l,useExisting:s},m,n.providers??[]]),i,r],decls:1,vars:0,template:function(e,L){e&1&&t(0,"ng-doc-page")},dependencies:[h],encapsulation:2,changeDetection:0});let a=s;return a})(),D=[g(d({},(0,k.isRoute)(n.route)?n.route:{}),{path:"",component:w,title:"Quick Start"})],F=D;export{w as DynamicComponent,F as default};
