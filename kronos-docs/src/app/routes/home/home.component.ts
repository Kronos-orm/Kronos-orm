import {CommonModule} from '@angular/common';
import {Component, OnDestroy, OnInit} from '@angular/core';
import {RouterLink} from '@angular/router';
import {AppService} from '../../app.service';

type LocalizedText = {
    zh: string;
    en: string;
};

type CodeToken = {
    text: string;
    kind: string;
};

type Stage = {
    key: string;
    eyebrow: LocalizedText;
    title: LocalizedText;
    copy: LocalizedText;
    bullets: LocalizedText[];
    code: string[];
    docPath: string;
};

type Capability = {
    label: string;
    title: LocalizedText;
    copy: LocalizedText;
    docPath?: string;
};

type Entry = {
    label: string;
    title: LocalizedText;
    copy: LocalizedText;
    action: LocalizedText;
    docPath?: string;
    route?: string;
    href?: string;
};

const KEYWORDS = new Set([
    'data',
    'class',
    'val',
    'var',
    'fun',
    'true',
    'false',
    'null',
    'object',
    'interface',
    'return',
    'by',
    'in',
    'as'
]);

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './home.component.html',
  styleUrl: './home.component.css'
})
export class HomeComponent implements OnInit, OnDestroy {
    activeStage = 0;
    private rotation: number | undefined;

    readonly dbLogos = ['MySQL', 'PostgreSQL', 'SQLite', 'SQL Server', 'Oracle'];

    readonly installCode = [
        'plugins {',
        '    id("com.kotlinorm.kronos-gradle-plugin") version "0.2.0"',
        '}',
        '',
        'dependencies {',
        '    implementation("com.kotlinorm:kronos-core:0.2.0")',
        '}'
    ];

    readonly stages: Stage[] = [
        {
            key: 'Model',
            eyebrow: {
                zh: '数据类建模',
                en: 'Data class models'
            },
            title: {
                zh: '用 data class 描述表结构。',
                en: 'Describe tables with data classes.'
            },
            copy: {
                zh: 'KPojo、注解配置、命名策略和类型推断覆盖常见建模场景。Code First 可同步表结构，Database First 可生成 Kotlin 类。',
                en: 'KPojo, annotations, naming strategies and type inference cover common modeling needs. Code First can sync schemas; Database First can generate Kotlin classes.'
            },
            bullets: [
                {zh: 'Table、Column、PrimaryKey、Cascade 等注解', en: 'Table, Column, PrimaryKey, Cascade and more annotations'},
                {zh: '逻辑删除、乐观锁、创建/更新时间策略', en: 'Logical delete, optimistic lock and timestamp strategies'},
                {zh: 'Code First 与 Database First 工作流', en: 'Code First and Database First workflows'}
            ],
            code: [
                '@Table("tb_movie")',
                '@TableIndex("idx_name", ["name"], "UNIQUE")',
                'data class Movie(',
                '  @PrimaryKey(identity = true)',
                '  val id: Long? = null,',
                '  @NonNull val name: String? = null,',
                '  val directorId: Long? = null,',
                '  @Cascade(["directorId"], ["id"])',
                '  val director: Director? = null,',
                '  val relations: List<MovieActorRelation>? = null,',
                '  @Serialize val type: List<String>? = null,',
                '  @Column("movie_summary")',
                '  val summary: String? = null,',
                '  @Version val version: Long? = null,',
                '  @LogicDelete val deleted: Boolean? = null,',
                '  @DateTimeFormat("yyyy-MM-dd HH:mm:ss")',
                '  @UpdateTime val updateTime: String? = null,',
                '  @CreateTime val createTime: LocalDateTime? = null',
                ') : KPojo {',
                '  var actors: List<Actor>? by manyToMany(::relations)',
                '}',
                '',
                'dataSource.table.syncTable<Movie>()'
            ],
            docPath: 'mapping/kpojo'
        },
        {
            key: 'Operate',
            eyebrow: {
                zh: '数据库操作',
                en: 'Database operations'
            },
            title: {
                zh: '用 Kotlin 语法写数据库操作。',
                en: 'Write database operations in Kotlin syntax.'
            },
            copy: {
                zh: 'select、insert、update、delete、upsert、where、having、on、orderBy、page 等 API 使用同一套 DSL。',
                en: 'Select, insert, update, delete, upsert, where, having, on, orderBy and page share one Kotlin-native DSL.'
            },
            bullets: [
                {zh: 'CRUD、分页、排序、分组、聚合', en: 'CRUD, pagination, sorting, grouping and aggregation'},
                {zh: 'Join、跨库查询、命名参数 SQL', en: 'Join, cross-database queries and named-argument SQL'},
                {zh: '事务、锁、级联增删改查', en: 'Transactions, locks and cascade CRUD'}
            ],
            code: [
                'val users = User()',
                '  .select { [it.id, it.username] }',
                '  .where { it.id < 10 && it.age >= 18 }',
                '  .orderBy { it.username.desc() }',
                '  .page(1, 10)',
                '  .queryList()',
                '',
                'val id = with(LastInsertIdPlugin) {',
                '  User(username = "test", age = 18)',
                '    .insert().withId().execute().lastInsertId',
                '}',
                '',
                'User(id = id, age = 19)',
                '  .update().by { it.id }.execute()',
                '',
                'User(id = id)',
                '  .delete().by { it.id }.execute()',
                '',
                'User(id = id, username = "kronos")',
                '  .upsert().on { it.id }.execute()',
                '',
                'User().join(UserRole()) { user, role ->',
                '  on { user.id == role.userId }',
                '  select { [user.id, role.role] }',
                '}.query()'
            ],
            docPath: 'mutation/insert'
        },
        {
            key: 'Query',
            eyebrow: {
                zh: '复杂查询',
                en: 'Advanced query'
            },
            title: {
                zh: '组合子查询、派生表和窗口结果。',
                en: 'Compose subqueries, derived sources and window results.'
            },
            copy: {
                zh: 'Kronos 子查询 DSL 覆盖标量子查询、IN / EXISTS、row-value tuple、查询作为 Source、窗口函数结果过滤以及 DML 中的子查询。',
                en: 'Kronos subquery DSL covers scalar subqueries, IN / EXISTS, row-value tuples, query-as-source, window result filtering and subqueries in DML.'
            },
            bullets: [
                {zh: 'KSelectable 可作为下一层 Source', en: 'KSelectable can become the next query Source'},
                {zh: '标量子查询要求单列并显式 limit(1)', en: 'Scalar subqueries require one column and explicit limit(1)'},
                {zh: 'row-value tuple、窗口函数和 DML 子查询', en: 'Row-value tuple, window functions and DML subqueries'}
            ],
            code: [
                'User().select { u ->',
                '  [u.id, Order().select { it.amount }',
                '    .where { it.userId == u.id }',
                '    .orderBy { it.createTime.desc() }',
                '    .limit(1).alias("lastAmount")]',
                '}.queryList()',
                '',
                'User().where { u ->',
                '  exists(Order().where { it.userId == u.id })',
                '}.queryList()',
                '',
                'Order().where {',
                '  [it.userId, it.createTime] in OrderArchive()',
                '    .select { [it.userId, f.max(it.createTime).alias("maxCreateTime")] }',
                '    .groupBy { it.userId }',
                '}.queryList()'
            ],
            docPath: 'query/subqueries'
        },
        {
            key: 'Integrate',
            eyebrow: {
                zh: '框架与插件',
                en: 'Frameworks and plugins'
            },
            title: {
                zh: '接入常用数据库和 Kotlin 框架。',
                en: 'Connect common databases and Kotlin frameworks.'
            },
            copy: {
                zh: 'Kronos 内置 MySQL、PostgreSQL、SQLite、SQL Server、Oracle 方言，可与 Spring Boot、Ktor、Vert.x、Solon、Android 项目集成。',
                en: 'Kronos ships built-in dialects for MySQL, PostgreSQL, SQLite, SQL Server and Oracle, with integration paths for Spring Boot, Ktor, Vert.x, Solon and Android.'
            },
            bullets: [
                {zh: '动态数据源和多数据源配置', en: 'Dynamic and multiple data sources'},
                {zh: '日志、序列化、命名策略、数据守卫', en: 'Logging, serialization, naming strategy and data guard'},
                {zh: '代码生成器与模块 API 文档', en: 'Code generator and module API references'}
            ],
            code: [
                'Kronos.dataSource = {',
                '  KronosJdbcWrapper(dataSource)',
                '}',
                '',
                'transact {',
                '  user.insert().execute()',
                '  order.update().by { it.id }.execute()',
                '}'
            ],
            docPath: 'database/custom-wrapper'
        }
    ];

    readonly capabilities: Capability[] = [
        {
            label: '01',
            title: {zh: '零运行时反射', en: 'Zero runtime reflection'},
            copy: {
                zh: '构建期生成映射代码，运行时避免 ORM 映射依赖反射调用，减少反射相关的启动和分配开销。',
                en: 'Mapping code is generated during build, so ORM mapping does not depend on runtime reflection and avoids reflection-related startup and allocation overhead.'
            },
            docPath: 'resources/benchmark'
        },
        {
            label: '02',
            title: {zh: 'Kotlin 原生 DSL', en: 'Kotlin-native DSL'},
            copy: {
                zh: '直接使用 ==、>、<、in、like 编写条件，无需堆叠 eq、gt、lt 等样板调用。',
                en: 'Write conditions with ==, >, <, in and like directly, without stacking eq, gt and lt boilerplate.'
            },
            docPath: 'query/conditions'
        },
        {
            label: '03',
            title: {zh: '多数据库支持', en: 'Multi-database support'},
            copy: {
                zh: '内置 MySQL、PostgreSQL、SQLite、SQL Server、Oracle 方言。',
                en: 'Built-in dialects for MySQL, PostgreSQL, SQLite, SQL Server and Oracle.'
            },
            docPath: 'database/dialect-support'
        },
        {
            label: '04',
            title: {zh: '功能完备', en: 'Full-featured'},
            copy: {
                zh: '事务、级联、一对一、一对多、多对多、表结构同步、序列化、跨库查询覆盖常见 ORM 场景。',
                en: 'Transactions, cascades, one-to-one, one-to-many, many-to-many, schema sync, serialization and cross-database queries.'
            },
            docPath: 'advanced/cascade'
        },
        {
            label: '05',
            title: {zh: '内置策略', en: 'Built-in strategies'},
            copy: {
                zh: '逻辑删除、乐观锁、创建时间、更新时间、命名策略和空值策略均可按项目配置。',
                en: 'Logical deletion, optimistic lock, create/update timestamps, naming strategy and no-value strategy are configurable.'
            },
            docPath: 'configuration/common-strategy'
        },
        {
            label: '06',
            title: {zh: '框架无关', en: 'Framework agnostic'},
            copy: {
                zh: 'Spring Boot、Ktor、Vert.x、Solon、Android 均可接入，适合服务端和移动端应用。',
                en: 'Use it with Spring Boot, Ktor, Vert.x, Solon and Android for server-side and mobile applications.'
            },
            docPath: 'database/custom-wrapper'
        }
    ];

    readonly docsEntries: Entry[] = [
        {
            label: 'Docs',
            title: {zh: '快速上手', en: 'Quick start'},
            copy: {zh: '安装依赖、配置插件、连接数据库，跑通第一组 CRUD。', en: 'Install dependencies, configure the plugin, connect a database and run the first CRUD flow.'},
            action: {zh: '打开指南', en: 'Open guide'},
            docPath: 'getting-started/quick-start'
        },
        {
            label: 'CRUD',
            title: {zh: '数据库操作', en: 'Database operation'},
            copy: {zh: '表操作、命名参数 SQL、insert、delete、update、upsert、select、join 和事务。', en: 'Table operations, named-argument SQL, insert, delete, update, upsert, select, join and transactions.'},
            action: {zh: '查看章节', en: 'View chapter'},
            docPath: 'mutation/insert'
        },
        {
            label: 'Plugin',
            title: {zh: '插件与集成', en: 'Plugins and integration'},
            copy: {zh: '数据源包装器、数据库支持、日志、语言扩展、LastInsertId、数据守卫和代码生成器。', en: 'Data-source wrapper, database support, logging, language extension, LastInsertId, data guard and code generator.'},
            action: {zh: '查看插件', en: 'View plugins'},
            docPath: 'database/custom-wrapper'
        },
        {
            label: 'AI',
            title: {zh: 'AI 协作指南', en: 'AI collaboration guide'},
            copy: {zh: '为代码助手提供 Kronos API 和贡献流程上下文，生成更贴近项目习惯的代码。', en: 'Give coding assistants Kronos API and contribution context so generated code follows project conventions.'},
            action: {zh: '查看指南', en: 'View guide'},
            docPath: 'resources/using-kronos-with-ai'
        }
    ];

    readonly blogEntries: Entry[] = [
        {
            label: 'Blog',
            title: {zh: 'Kotlin 多平台支持', en: 'Kotlin multiplatform support'},
            copy: {
                zh: 'Kronos 当前对 Kotlin 多平台的支持情况，以及后续开发和发布计划。',
                en: 'Current Kotlin multiplatform support in Kronos, plus development and release plans.'
            },
            action: {zh: '阅读文章', en: 'Read article'},
            route: '/blog',
            href: 'kotlin-multiplatform-support'
        },
        {
            label: 'Blog',
            title: {zh: 'Kronos 代码生成器', en: 'Code generation framework'},
            copy: {
                zh: '根据数据库表结构生成 Kotlin ORM 类和常用 Kotlin Class 的配置方式。',
                en: 'Generate Kotlin ORM classes and common Kotlin classes from database table structures.'
            },
            action: {zh: '阅读文章', en: 'Read article'},
            route: '/blog',
            href: 'code-generation-support'
        },
        {
            label: 'Blog',
            title: {zh: 'Kronos IntelliJ IDEA 插件', en: 'Kronos IntelliJ IDEA plugin'},
            copy: {
                zh: 'IDEA K2 分析现在可以理解 Kronos 生成成员、生成投影和 DSL diagnostics。',
                en: 'IDEA K2 analysis can now understand Kronos generated members, generated projections, and DSL diagnostics.'
            },
            action: {zh: '阅读文章', en: 'Read article'},
            route: '/blog',
            href: 'intellij-idea-plugin'
        }
    ];

    readonly apiEntries: Entry[] = [
        {
            label: 'API',
            title: {zh: 'Kronos Core API 文档', en: 'Kronos Core API Docs'},
            copy: {zh: '核心 DSL、语句构建、KPojo 能力和通用类型参考。', en: 'Core DSL, statement builders, KPojo capabilities and common type references.'},
            action: {zh: '打开 API', en: 'Open API'},
            href: '/api/kronos-core'
        },
        {
            label: 'API',
            title: {zh: 'Kronos JDBC Wrapper API 文档', en: 'Kronos JDBC Wrapper API Docs'},
            copy: {zh: '数据源包装、执行器、事务和 JDBC 集成 API。', en: 'Data-source wrapper, executor, transaction and JDBC integration APIs.'},
            action: {zh: '打开 API', en: 'Open API'},
            href: '/api/kronos-jdbc-wrapper'
        },
        {
            label: 'API',
            title: {zh: 'Kronos Compiler Plugin API 文档', en: 'Kronos Compiler Plugin API Docs'},
            copy: {zh: '插件模块的扩展点、生成逻辑和内部 API 参考。', en: 'Extension points, generation logic and internal APIs for the plugin module.'},
            action: {zh: '打开 API', en: 'Open API'},
            href: '/api/kronos-compiler-plugin'
        },
        {
            label: 'API',
            title: {zh: 'Kronos Codegen API 文档', en: 'Kronos Codegen API Docs'},
            copy: {zh: '数据库结构读取、代码生成配置和模板相关 API。', en: 'Database schema reading, code-generation configuration and template APIs.'},
            action: {zh: '打开 API', en: 'Open API'},
            href: '/api/kronos-codegen'
        },
        {
            label: 'API',
            title: {zh: 'Kronos Logging API 文档', en: 'Kronos Logging API Docs'},
            copy: {zh: '日志适配器、日志任务和运行时日志配置 API。', en: 'Logging adapters, logging tasks and runtime logging configuration APIs.'},
            action: {zh: '打开 API', en: 'Open API'},
            href: '/api/kronos-logging'
        },
        {
            label: 'API',
            title: {zh: 'Kronos Syntax API 文档', en: 'Kronos Syntax API Docs'},
            copy: {zh: 'SQL 语法校验和语句遍历相关 API。', en: 'SQL syntax validation and statement traversal APIs.'},
            action: {zh: '打开 API', en: 'Open API'},
            href: '/api/kronos-syntax'
        },
        {
            label: 'API',
            title: {zh: 'Kronos Maven Plugin API 文档', en: 'Kronos Maven Plugin API Docs'},
            copy: {zh: 'Maven 编译器插件接入和配置 API。', en: 'Maven compiler-plugin integration and configuration APIs.'},
            action: {zh: '打开 API', en: 'Open API'},
            href: '/api/kronos-maven-plugin'
        },
        {
            label: 'API',
            title: {zh: 'Kronos Gradle Plugin API 文档', en: 'Kronos Gradle Plugin API Docs'},
            copy: {zh: 'Gradle 插件注册、配置和编译器插件接入 API。', en: 'Gradle plugin registration, configuration and compiler-plugin integration APIs.'},
            action: {zh: '打开 API', en: 'Open API'},
            href: '/api/kronos-gradle-plugin'
        }
    ];

    constructor(public appService: AppService) {
    }

    ngOnInit(): void {
        if (!window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
            this.rotation = window.setInterval(() => {
                this.activeStage = (this.activeStage + 1) % this.stages.length;
            }, 5200);
        }
    }

    ngOnDestroy(): void {
        if (this.rotation) {
            window.clearInterval(this.rotation);
        }
    }

    get language(): string {
        return this.appService.language;
    }

    get isZh(): boolean {
        return this.language === 'zh-CN';
    }

    setLanguage(language: string): void {
        this.appService.language = language;
    }

    setStage(index: number): void {
        this.activeStage = index;
        if (this.rotation) {
            window.clearInterval(this.rotation);
            this.rotation = undefined;
        }
    }

    text(value: LocalizedText): string {
        return this.isZh ? value.zh : value.en;
    }

    highlight(line: string): CodeToken[] {
        if (!line) {
            return [{text: '\u00a0', kind: 'plain'}];
        }

        const tokens: CodeToken[] = [];
        const pattern = /(@[A-Za-z_][\w.]*|"[^"]*"|\/\/.*|\b\d+(?:\.\d+)?\b|\b[A-Z][A-Za-z0-9_]*\b|\b[a-z_][A-Za-z0-9_]*\b|[{}()[\].,:?<>+\-*/=!&|]+)/g;
        let cursor = 0;
        let match: RegExpExecArray | null;

        while ((match = pattern.exec(line)) !== null) {
            if (match.index > cursor) {
                tokens.push({text: line.slice(cursor, match.index), kind: 'plain'});
            }

            const text = match[0];
            let kind = 'plain';
            if (text.startsWith('//')) {
                kind = 'comment';
            } else if (text.startsWith('@')) {
                kind = 'annotation';
            } else if (text.startsWith('"')) {
                kind = 'string';
            } else if (/^\d/.test(text)) {
                kind = 'number';
            } else if (KEYWORDS.has(text)) {
                kind = 'keyword';
            } else if (/^[A-Z]/.test(text)) {
                kind = 'type';
            } else if (/^[{}()[\].,:?<>+\-*/=!&|]+$/.test(text)) {
                kind = 'operator';
            }

            tokens.push({text, kind});
            cursor = pattern.lastIndex;
        }

        if (cursor < line.length) {
            tokens.push({text: line.slice(cursor), kind: 'plain'});
        }

        return tokens;
    }

    docRoute(path: string): string {
        return `/documentation/${this.language}/${path}`;
    }

    get docsUrl(): string {
        return this.docRoute('getting-started/introduce');
    }

    get quickStartUrl(): string {
        return this.docRoute('getting-started/quick-start');
    }

    get databaseOperationUrl(): string {
        return this.docRoute('mutation/insert');
    }

    get advancedUrl(): string {
        return this.docRoute('advanced/cascade');
    }

    get pluginUrl(): string {
        return this.docRoute('database/custom-wrapper');
    }
}
