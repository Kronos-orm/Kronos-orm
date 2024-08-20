import{a as h}from"./chunk-FJ26F7HW.js";import{a as r}from"./chunk-CN3B2NJM.js";import{a as l}from"./chunk-TKZ7FAYE.js";import{G as y}from"./chunk-JSUCXAVI.js";import{Pb as o,jc as p,kc as g,ra as c,xb as i}from"./chunk-TLBD5JYT.js";import{a as t,b as d,g as k}from"./chunk-ODN5LVDJ.js";var u=k(y());var j={title:"Database Scheme Operation",mdFile:"./index.md",category:h,order:1},s=j;var m=[];var x={},b=x;var T=`<h1 id="database-scheme-operation" class="ngde">Database Scheme Operation<a title="Link to heading" class="ng-doc-header-link ngde" href="/en/database/database-operation#database-scheme-operation"><ng-doc-icon icon="link-2" size="16" class="ngde"></ng-doc-icon></a></h1><p class="ngde">Compared to kotoframework, Kronos is a Code-First ORM framework that adds operations on database table structures.</p><p class="ngde">Related functions can be called through <strong class="ngde">Kronos.dataSource</strong>(<code class="ngde">() -> KronosDataSourceWrapper</code>) or a specific data source object (<code class="ngde">KronosDataSourceWrapper</code>).</p><h2 id="1-check-if-the-table-exists" class="ngde">1. Check if the table exists<a title="Link to heading" class="ng-doc-header-link ngde" href="/en/database/database-operation#1-check-if-the-table-exists"><ng-doc-icon icon="link-2" size="16" class="ngde"></ng-doc-icon></a></h2><p class="ngde">Use the <code class="ngde">exists</code> method to check if the table exists, which returns a Boolean value.</p><pre class="ngde hljs"><code class="hljs language-kotlin code-lines ngde" lang="kotlin" name="demo" icon="kotlin" highlightedlines="[]"><span class="line ngde"><span class="hljs-keyword ngde">val</span> exists: <span class="hljs-built_in ngde">Boolean</span> = db.table.exists(user)
</span><span class="line ngde"><span class="hljs-comment ngde">// or</span>
</span><span class="line ngde"><span class="hljs-keyword ngde">val</span> exists: <span class="hljs-built_in ngde">Boolean</span> = db.table.exists&#x3C;User>()
</span><span class="line ngde"><span class="hljs-comment ngde">// or</span>
</span><span class="line ngde"><span class="hljs-keyword ngde">val</span> exists: <span class="hljs-built_in ngde">Boolean</span> = db.table.exists(<span class="hljs-string ngde">"user"</span>)
</span></code></pre><h2 id="2-create-a-table" class="ngde">2. Create a table<a title="Link to heading" class="ng-doc-header-link ngde" href="/en/database/database-operation#2-create-a-table"><ng-doc-icon icon="link-2" size="16" class="ngde"></ng-doc-icon></a></h2><p class="ngde">Use the <code class="ngde">createTable</code> method to create a table.</p><pre class="ngde hljs"><code class="hljs language-kotlin code-lines ngde" lang="kotlin" name="demo" icon="kotlin" highlightedlines="[]"><span class="line ngde">db.table.createTable(user)
</span><span class="line ngde"><span class="hljs-comment ngde">// or</span>
</span><span class="line ngde">db.table.createTable&#x3C;User>()
</span></code></pre><h2 id="3-delete-a-table" class="ngde">3. Delete a table<a title="Link to heading" class="ng-doc-header-link ngde" href="/en/database/database-operation#3-delete-a-table"><ng-doc-icon icon="link-2" size="16" class="ngde"></ng-doc-icon></a></h2><p class="ngde">Use the <code class="ngde">dropTable</code> method to delete a table.</p><pre class="ngde hljs"><code class="hljs language-kotlin code-lines ngde" lang="kotlin" name="demo" icon="kotlin" highlightedlines="[]"><span class="line ngde">db.table.dropTable(user)
</span><span class="line ngde"><span class="hljs-comment ngde">// or</span>
</span><span class="line ngde">db.table.dropTable&#x3C;User>()
</span><span class="line ngde"><span class="hljs-comment ngde">// or</span>
</span><span class="line ngde">db.table.dropTable(<span class="hljs-string ngde">"user"</span>)
</span></code></pre><h2 id="4-synchronize-table-structure" class="ngde">4. Synchronize table structure<a title="Link to heading" class="ng-doc-header-link ngde" href="/en/database/database-operation#4-synchronize-table-structure"><ng-doc-icon icon="link-2" size="16" class="ngde"></ng-doc-icon></a></h2><p class="ngde">Use the <code class="ngde">syncSchema</code> method to synchronize the table structure.</p><pre class="ngde hljs"><code class="hljs language-kotlin code-lines ngde" lang="kotlin" name="demo" icon="kotlin" highlightedlines="[]"><span class="line ngde">db.table.syncSchema(user)
</span><span class="line ngde"><span class="hljs-comment ngde">// or</span>
</span><span class="line ngde">db.table.syncSchema&#x3C;User>()
</span></code></pre><h2 id="5-dynamic-table-creation" class="ngde">5. Dynamic table creation<a title="Link to heading" class="ng-doc-header-link ngde" href="/en/database/database-operation#5-dynamic-table-creation"><ng-doc-icon icon="link-2" size="16" class="ngde"></ng-doc-icon></a></h2><p class="ngde">The <code class="ngde">getTableCreateSqlList</code> method is used to dynamically generate SQL statements for creating tables. The parameters received by <code class="ngde">getTableCreateSqlList</code> include:</p><ul class="ngde"><li class="ngde"><code class="ngde">dbType</code>\uFF1A<code class="ngde">DBType</code> Database type</li><li class="ngde"><code class="ngde">tableName</code>\uFF1A<code class="ngde">String</code> Table name</li><li class="ngde"><code class="ngde">fields</code>\uFF1A<code class="ngde">List&#x3C;Field></code> Field list</li><li class="ngde"><code class="ngde">indexes</code>\uFF1A<code class="ngde">List&#x3C;KTableIndex></code> Index list</li></ul><pre class="ngde hljs"><code class="hljs language-kotlin code-lines ngde" lang="kotlin" name="demo" icon="kotlin" highlightedlines="[2,31]"><span class="line ngde"><span class="hljs-keyword ngde">val</span> listOfSql = 
</span><span class="line highlighted ngde">  getTableCreateSqlList(
</span><span class="line ngde">      dbType = DBType.Mysql,
</span><span class="line ngde">      tableName = <span class="hljs-string ngde">"user"</span>,
</span><span class="line ngde">      fields = listOf(
</span><span class="line ngde">          Field(
</span><span class="line ngde">              name = <span class="hljs-string ngde">"id"</span>,
</span><span class="line ngde">              type = KColumnType.fromString(<span class="hljs-string ngde">"INT"</span>),
</span><span class="line ngde">              primaryKey = <span class="hljs-literal ngde">true</span>,
</span><span class="line ngde">              identity = <span class="hljs-literal ngde">true</span>
</span><span class="line ngde">          ),
</span><span class="line ngde">          Field(
</span><span class="line ngde">              name = <span class="hljs-string ngde">"name"</span>,
</span><span class="line ngde">              type = KColumnType.fromString(<span class="hljs-string ngde">"VARCHAR"</span>),
</span><span class="line ngde">              length = <span class="hljs-number ngde">255</span>
</span><span class="line ngde">          ),
</span><span class="line ngde">          Field(
</span><span class="line ngde">              name = <span class="hljs-string ngde">"age"</span>,
</span><span class="line ngde">              type = KColumnType.fromString(<span class="hljs-string ngde">"INT"</span>),
</span><span class="line ngde">          )
</span><span class="line ngde">      ),
</span><span class="line ngde">      indexes = listOf(
</span><span class="line ngde">          KTableIndex(
</span><span class="line ngde">              name = <span class="hljs-string ngde">"idx_name"</span>,
</span><span class="line ngde">              columns = listOf(<span class="hljs-string ngde">"name"</span>),
</span><span class="line ngde">              type = <span class="hljs-string ngde">"UNIQUE"</span>
</span><span class="line ngde">          )
</span><span class="line ngde">      )
</span><span class="line ngde">  )
</span><span class="line ngde">  
</span><span class="line highlighted ngde">listOfSql.forEach { db.execute(it) }
</span></code></pre><ng-doc-blockquote type="warning" class="ngde"><p class="ngde">If you need to perform multiple database operations on the same entity object in succession, it is recommended not to use generics to avoid creating KPojo objects multiple times and incurring unnecessary overhead.</p></ng-doc-blockquote>`,C=(()=>{let e=class e extends l{constructor(){super(),this.routePrefix="",this.pageType="guide",this.pageContent=T,this.page=s,this.demoAssets=b}};e.\u0275fac=function(a){return new(a||e)},e.\u0275cmp=c({type:e,selectors:[["ng-doc-page-en-database-database-operation"]],standalone:!0,features:[p([{provide:l,useExisting:e},m,s.providers??[]]),i,g],decls:1,vars:0,template:function(a,D){a&1&&o(0,"ng-doc-page")},dependencies:[r],encapsulation:2,changeDetection:0});let n=e;return n})(),S=[d(t({},(0,u.isRoute)(s.route)?s.route:{}),{path:"",component:C,title:"Database Scheme Operation"})],I=S;export{C as DynamicComponent,I as default};
