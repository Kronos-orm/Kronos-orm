import{a as j}from"./chunk-JPA33CC6.js";import{a as p}from"./chunk-BC3M6UN5.js";import"./chunk-K4R3X5GQ.js";import{a as h}from"./chunk-CN3B2NJM.js";import{a as l}from"./chunk-TKZ7FAYE.js";import{G as C}from"./chunk-JSUCXAVI.js";import{Pb as t,jc as o,kc as r,ra as c,xb as i}from"./chunk-TLBD5JYT.js";import{a as g,b as d,g as x}from"./chunk-ODN5LVDJ.js";var k=x(C());var v={title:"\u5FEB\u901F\u4E0A\u624B",mdFile:"./index.md",category:j,order:1,imports:[p],demos:{AnimateLogoComponent:p}},n=v;var m=[];var f={AnimateLogoComponent:[{title:"TypeScript",code:`<pre class="ngde hljs"><code lang="typescript" class="hljs language-typescript code-lines ngde"><span class="line ngde"><span class="hljs-keyword ngde">import</span> {
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
</span></code></pre>`}]},u=f;var b=`<h1 id="\u5FEB\u901F\u4E0A\u624B" class="ngde">\u5FEB\u901F\u4E0A\u624B<a title="Link to heading" class="ng-doc-header-link ngde" href="/zh-CN/getting-started/quick-start#\u5FEB\u901F\u4E0A\u624B"><ng-doc-icon icon="link-2" size="16" class="ngde"></ng-doc-icon></a></h1><ng-doc-demo componentname="AnimateLogoComponent" indexable="false" class="ngde"><div id="options" class="ngde">{"container":false}</div></ng-doc-demo><h2 id="\u6DFB\u52A0\u4F9D\u8D56" class="ngde">\u6DFB\u52A0\u4F9D\u8D56\uFF1A<a title="Link to heading" class="ng-doc-header-link ngde" href="/zh-CN/getting-started/quick-start#\u6DFB\u52A0\u4F9D\u8D56"><ng-doc-icon icon="link-2" size="16" class="ngde"></ng-doc-icon></a></h2><p class="ngde">kronos-orm\u662F\u4E00\u4E2A\u591A\u6A21\u5757\u7684\u9879\u76EE\uFF0C\u6211\u4EEC\u63D0\u4F9B\u4E86\u591A\u4E2A\u6A21\u5757\u4F9B\u5F00\u53D1\u8005\u9009\u62E9\uFF0C\u5F00\u53D1\u8005\u53EF\u4EE5\u6839\u636E\u81EA\u5DF1\u7684\u9700\u6C42\u9009\u62E9\u5BF9\u5E94\u7684\u6A21\u5757\u3002</p><p class="ngde">\u5176\u4E2D\uFF1A</p><ol class="ngde"><li class="ngde"><code class="ngde">kronos-core</code>\u662F<strong class="ngde">\u5FC5\u9009\u6A21\u5757</strong>\uFF0C\u5B83\u63D0\u4F9B\u4E86\u57FA\u7840\u7684ORM\u529F\u80FD</li><li class="ngde"><code class="ngde">kronos-logging</code>\u662F\u53EF\u9009\u6A21\u5757\uFF0C\u5B83\u63D0\u4F9B\u4E86\u591A\u5E73\u53F0\u7684\u65E5\u5FD7\u529F\u80FD</li><li class="ngde"><code class="ngde">kronos-jvm-driver-wrapper</code>\u662F\u53EF\u9009\u6A21\u5757\uFF0C\u5B83\u63D0\u4F9B\u4E86JVM\u9A71\u52A8\u5305\u88C5\u5668\u3002\uFF08\u60A8\u53EF\u4EE5\u4F7F\u7528\u5176\u4ED6\u5B98\u65B9\u9A71\u52A8\u5305\u88C5\u5668\u6216\u81EA\u5DF1\u7F16\u5199\u5305\u88C5\u7C7B\u8F7B\u677E\u5730\u642D\u914D\u7B2C\u4E09\u65B9\u6846\u67B6\uFF08\u5982SpringData\u3001Mybatis\u3001Hibernate\u3001Jdbi\u7B49\uFF09\u4F7F\u7528\uFF09</li><li class="ngde"><code class="ngde">kronos-compiler-plugin</code>\u63D2\u4EF6\u662F<strong class="ngde">\u5FC5\u9009\u6A21\u5757</strong>\uFF0C\u5B83\u4E3AKronos\u7684ORM\u529F\u80FD\u63D0\u4F9B\u4E86\u7F16\u8BD1\u65F6\u652F\u6301</li></ol><ng-doc-tab group="import" name="gradle(kts)" icon="gradlekts" class="ngde"><pre class="ngde hljs"><code class="hljs language-kotlin code-lines ngde" lang="kotlin" name="" icon="" highlightedlines="[]"><span class="line ngde">    dependencies {
</span><span class="line ngde">        implementation(<span class="hljs-string ngde">"com.kotlinorm.kronos-core:1.0.0"</span>)
</span><span class="line ngde">        implementation(<span class="hljs-string ngde">"com.kotlinorm.kronos-logging:1.0.0"</span>)
</span><span class="line ngde">        implementation(<span class="hljs-string ngde">"com.kotlinorm.kronos-jvm-driver-wrapper:1.0.0"</span>)
</span><span class="line ngde">    }
</span><span class="line ngde">    
</span><span class="line ngde">    plugins {
</span><span class="line ngde">        id(<span class="hljs-string ngde">"com.kotlinorm.kronos-compiler-plugin"</span>) version <span class="hljs-string ngde">"1.0.0"</span>
</span><span class="line ngde">    }
</span></code></pre></ng-doc-tab><ng-doc-tab group="import" name="gradle(groovy)" icon="gradle" class="ngde"><pre class="ngde hljs"><code class="hljs language-groovy code-lines ngde" lang="groovy" name="" icon="" highlightedlines="[]"><span class="line ngde">    dependencies {
</span><span class="line ngde">        implementation 'com.kotlinorm:kronos-core:1.0.0'
</span><span class="line ngde">        implementation 'com.kotlinorm:kronos-logging:1.0.0'
</span><span class="line ngde">        implementation 'com.kotlinorm:kronos-jvm-driver-wrapper:1.0.0'
</span><span class="line ngde">    }
</span><span class="line ngde">    
</span><span class="line ngde">    plugins {
</span><span class="line ngde">        id 'com.kotlinorm.kronos-compiler-plugin' version '1.0.0'
</span><span class="line ngde">    }
</span></code></pre></ng-doc-tab><ng-doc-tab group="import" name="maven(NOT SUPPORT NOW)" icon="maven" class="ngde"><pre class="ngde hljs"><code class="hljs language-xml code-lines ngde" lang="xml" name="" icon="" highlightedlines="[]"><span class="line ngde"><span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">project</span>></span>
</span><span class="line ngde">  <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">dependencies</span>></span>
</span><span class="line ngde">    <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">dependency</span>></span>
</span><span class="line ngde">      <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">groupId</span>></span>com.kotlinorm<span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">groupId</span>></span>
</span><span class="line ngde">      <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">artifactId</span>></span>kronos-core<span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">artifactId</span>></span>
</span><span class="line ngde">      <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">version</span>></span>1.0.0<span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">version</span>></span>
</span><span class="line ngde">    <span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">dependency</span>></span>
</span><span class="line ngde">    <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">dependency</span>></span>
</span><span class="line ngde">      <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">groupId</span>></span>com.kotlinorm<span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">groupId</span>></span>
</span><span class="line ngde">      <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">artifactId</span>></span>kronos-logging<span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">artifactId</span>></span>
</span><span class="line ngde">      <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">version</span>></span>1.0.0<span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">version</span>></span>
</span><span class="line ngde">    <span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">dependency</span>></span>
</span><span class="line ngde">    <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">dependency</span>></span>
</span><span class="line ngde">      <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">groupId</span>></span>com.kotlinorm<span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">groupId</span>></span>
</span><span class="line ngde">      <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">artifactId</span>></span>kronos-jvm-driver-wrapper<span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">artifactId</span>></span>
</span><span class="line ngde">      <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">version</span>></span>1.0.0<span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">version</span>></span>
</span><span class="line ngde">    <span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">dependency</span>></span>
</span><span class="line ngde">  <span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">dependencies</span>></span>
</span><span class="line ngde">
</span><span class="line ngde">  <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">build</span>></span>
</span><span class="line ngde">    <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">plugins</span>></span>
</span><span class="line ngde">      <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">plugin</span>></span>
</span><span class="line ngde">        <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">groupId</span>></span>com.kotlinorm<span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">groupId</span>></span>
</span><span class="line ngde">        <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">artifactId</span>></span>kronos-compiler-plugin<span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">artifactId</span>></span>
</span><span class="line ngde">        <span class="hljs-tag ngde">&#x3C;<span class="hljs-name ngde">version</span>></span>1.0.0<span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">version</span>></span>
</span><span class="line ngde">      <span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">plugin</span>></span>
</span><span class="line ngde">    <span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">plugins</span>></span>
</span><span class="line ngde">  <span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">build</span>></span>
</span><span class="line ngde"><span class="hljs-tag ngde">&#x3C;/<span class="hljs-name ngde">project</span>></span>
</span></code></pre></ng-doc-tab><h2 id="\u914D\u7F6E\u6570\u636E\u5E93" class="ngde">\u914D\u7F6E\u6570\u636E\u5E93\uFF1A<a title="Link to heading" class="ng-doc-header-link ngde" href="/zh-CN/getting-started/quick-start#\u914D\u7F6E\u6570\u636E\u5E93"><ng-doc-icon icon="link-2" size="16" class="ngde"></ng-doc-icon></a></h2><p class="ngde">\u8FD9\u91CC\u4EC5\u4ECB\u7ECD<code class="ngde">kronos-jvm-driver-wrapper</code>\u6A21\u5757Mysql\u4E0B\u7684\u4F7F\u7528\uFF0C\u5176\u4ED6\u6A21\u5757\u7684\u4F7F\u7528\u65B9\u5F0F\u7C7B\u4F3C\uFF0C\u5177\u4F53\u8BF7\u53C2\u8003<a href="/documentation/zh-CN/database/connect-to-db" class="ngde">\u8FDE\u63A5\u5230\u6570\u636E\u5E93</a>\u3002</p><p class="ngde">\u9700\u5F15\u5165<code class="ngde">commons-dbcp2</code>\u3001<code class="ngde">mysql-connector-java</code>\u7B49\u4F9D\u8D56\u3002</p><ng-doc-tab group="KronosConfig" name="KronosConfig.kt" icon="" class="ngde"><pre class="ngde hljs"><code class="hljs language-kotlin code-lines ngde" lang="kotlin" name="" icon="" highlightedlines="[]"><span class="line ngde"><span class="hljs-keyword ngde">import</span> com.kotlinorm.Kronos
</span><span class="line ngde">Kronos.apply {
</span><span class="line ngde">  dataSource = {
</span><span class="line ngde">    BasicDataSource().apply {
</span><span class="line ngde">        driverClassName = <span class="hljs-string ngde">"com.mysql.cj.jdbc.Driver"</span>
</span><span class="line ngde">        url = <span class="hljs-string ngde">"jdbc:mysql://localhost:3306/kronos?useUnicode=true&#x26;characterEncoding=utf-8&#x26;useSSL=false&#x26;serverTimezone=UTC"</span>
</span><span class="line ngde">        username = <span class="hljs-string ngde">"root"</span>
</span><span class="line ngde">        password = <span class="hljs-string ngde">"******"</span>
</span><span class="line ngde">    }
</span><span class="line ngde">  }
</span></code></pre></ng-doc-tab><p class="ngde">\u5F53\u4F7F\u7528\u5176\u4ED6\u6570\u636E\u5E93\u6216\u4F7F\u7528\u975Ejvm\u5E73\u53F0\u65F6\uFF0C\u9700\u8981\u4F7F\u7528\u5BF9\u5E94\u7684\u9A71\u52A8\u53CA\u914D\u7F6E\u3002</p><h2 id="\u7F16\u5199\u5B9E\u4F53\u7C7B" class="ngde">\u7F16\u5199\u5B9E\u4F53\u7C7B\uFF1A<a title="Link to heading" class="ng-doc-header-link ngde" href="/zh-CN/getting-started/quick-start#\u7F16\u5199\u5B9E\u4F53\u7C7B"><ng-doc-icon icon="link-2" size="16" class="ngde"></ng-doc-icon></a></h2><ng-doc-tab group="KPojo" name="Director.kt" icon="" class="ngde"><pre class="ngde hljs"><code class="hljs language-kotlin code-lines ngde" lang="kotlin" name="" icon="" highlightedlines="[]"><span class="line ngde"><span class="hljs-keyword ngde">data</span> <span class="hljs-keyword ngde">class</span> <span class="hljs-title class_ ngde">Director</span>(
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
</span></code></pre></ng-doc-tab><h2 id="\u4F7F\u7528kronos" class="ngde">\u4F7F\u7528Kronos\uFF1A<a title="Link to heading" class="ng-doc-header-link ngde" href="/zh-CN/getting-started/quick-start#\u4F7F\u7528kronos"><ng-doc-icon icon="link-2" size="16" class="ngde"></ng-doc-icon></a></h2><ng-doc-tab group="Kronos" name="Kronos.kt" icon="" class="ngde"><pre class="ngde hljs"><code class="hljs language-kotlin code-lines ngde" lang="kotlin" name="" icon="" highlightedlines="[]"><span class="line ngde"><span class="hljs-keyword ngde">val</span> director: Director = Director(
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
</span><span class="line ngde">
</span></code></pre></ng-doc-tab>`,w=(()=>{let s=class s extends l{constructor(){super(),this.routePrefix="",this.pageType="guide",this.pageContent=b,this.page=n,this.demoAssets=u}};s.\u0275fac=function(e){return new(e||s)},s.\u0275cmp=c({type:s,selectors:[["ng-doc-page-zh-cn-getting-started-quick-start"]],standalone:!0,features:[o([{provide:l,useExisting:s},m,n.providers??[]]),i,r],decls:1,vars:0,template:function(e,I){e&1&&t(0,"ng-doc-page")},dependencies:[h],encapsulation:2,changeDetection:0});let a=s;return a})(),D=[d(g({},(0,k.isRoute)(n.route)?n.route:{}),{path:"",component:w,title:"\u5FEB\u901F\u4E0A\u624B"})],z=D;export{w as DynamicComponent,z as default};
