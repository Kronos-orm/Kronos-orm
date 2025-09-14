# Kronos-ORM 项目下载、构建与运行指南（中文）

本指南将帮助你在本地下载源码、使用正确的 JDK 和 Gradle 构建项目，并运行全部模块的测试（包括集成测试）。

> 重要前提
> - Gradle 构建与运行需要使用 JDK 17 或更高版本（CI 使用 JDK 21）。
> - 运行 `kronos-testing` 模块的集成测试前，需要准备本地数据库并设置必要的环境变量：
>   - MySQL: 需要设置 `MYSQL_USERNAME` 与 `MYSQL_PASSWORD`
>   - PostgreSQL: 需要设置 `POSTGRES_USERNAME` 与（建议）`POSTGRES_PASSWORD`
>

---

## 1. 下载/克隆项目

- 使用 Git：
  ```bash
  git clone https://github.com/<your-org-or-username>/Kronos-orm.git
  cd Kronos-orm
  ```
- 或者在 GitHub/Gitee 页面下载 Zip，解压后进入项目根目录。

## 2. 安装与配置 JDK（17+）

- 推荐安装 JDK 17、21 或更新版本。你可以使用：
  - SDKMAN（macOS/Linux）：`sdk install java 21.0.x-tem` 或 `sdk install java 17.0.x-tem`
  - Homebrew（macOS）：`brew install openjdk@21`
  - Windows：从 Adoptium/Oracle 下载并安装。

- 命令行确认：
  ```bash
  java -version
  # 应返回 17 或更高，如 openjdk version "21..."
  ```

- 设置 Gradle Wrapper 使用该 JDK（通常 wrapper 会继承当前 JAVA_HOME）：
  - 确保 `JAVA_HOME` 指向 17+：
    - macOS/Linux：`export JAVA_HOME=$(/usr/libexec/java_home -v 21)`（或 17）
    - Windows PowerShell：`$env:JAVA_HOME = 'C:\\Program Files\\Java\\jdk-21'`
  - IDE（IntelliJ IDEA）中：
    - File > Settings > Build, Execution, Deployment > Build Tools > Gradle > Gradle JDK 选择 17+。

## 3. 使用 Gradle 构建项目

项目已包含 Gradle Wrapper（`./gradlew`）。无需本地安装 Gradle。

- 构建全部模块：
  ```bash
  ./gradlew clean build
  ```

- 仅运行单元测试（全部模块）：
  ```bash
  ./gradlew test
  ```

- 构建单个模块（示例）：
  ```bash
  ./gradlew :kronos-core:build
  ./gradlew :kronos-logging:build
  ```

> 如果你在中国大陆网络环境下，首次构建可能较慢，可在 `gradle.properties` 或本地 Gradle 初始化脚本中配置镜像源以加速依赖下载。

## 4. 运行全部项目的测试

- 在确保 JDK 17+ 的前提下，执行：
  ```bash
  ./gradlew test
  ```
- 如需包含所有模块的检查（编译、测试、校验）：
  ```bash
  ./gradlew build --info --stacktrace
  ```

## 5. 运行 `kronos-testing` 集成测试

`kronos-testing` 模块包含需要实际数据库的集成测试。运行前需要：

1) 准备数据库（本地安装或 Docker 均可）

- MySQL 8.x（示例使用 Docker）：
  ```bash
  docker run --name kronos-mysql -e MYSQL_ROOT_PASSWORD=root \
    -e MYSQL_DATABASE=kronos_testing \
    -e MYSQL_USER=kronos -e MYSQL_PASSWORD=your_mysql_pwd \
    -p 3306:3306 -d mysql:8.0
  ```

- PostgreSQL 17（示例使用 Docker）：
  ```bash
  docker run --name kronos-postgres \
    -e POSTGRES_PASSWORD=your_pg_pwd \
    -e POSTGRES_DB=kronos_testing \
    -e POSTGRES_USER=kronos \
    -p 5432:5432 -d postgres:17
  ```

> 你也可以使用本地已安装的数据库，只需确保有名为 `kronos` 的用户与名为 `kronos_testing` 的数据库，或与你的实际配置一致。

2) 设置必须的环境变量（用于测试进程访问数据库）

- bash/zsh（macOS/Linux）：
  ```bash
  export MYSQL_USERNAME=kronos
  export MYSQL_PASSWORD=your_mysql_pwd
  export POSTGRES_USERNAME=kronos
  export POSTGRES_PASSWORD=your_pg_pwd   # 建议设置
  ```

- PowerShell（Windows）：
  ```powershell
  $env:MYSQL_USERNAME = 'kronos'
  $env:MYSQL_PASSWORD = 'your_mysql_pwd'
  $env:POSTGRES_USERNAME = 'kronos'
  $env:POSTGRES_PASSWORD = 'your_pg_pwd'  # 建议设置
  ```

3) 运行集成测试

```bash
./gradlew :kronos-testing:test --info --stacktrace
```

> 在 CI 中，我们会导出 `MYSQL_USERNAME=kronos` 与 `POSTGRES_USERNAME=kronos` 后执行同一命令；你在本地需要同时提供对应的密码变量以连接到数据库。

## 6. 常见问题排查（FAQ）

- 问题：Gradle 报错提示 JDK 版本过低或不匹配。
  - 处理：确认 `java -version` >= 17，且 IDE 中的 Gradle JDK 也选择为 17 或更高。
- 问题：无法连接数据库 / 认证失败。
  - 处理：确认容器或本地服务已启动、端口（MySQL 3306 / PostgreSQL 5432）开放、环境变量已正确设置。
- 问题：端口占用。
  - 处理：检查是否已有数据库实例在相同端口运行，调整容器映射端口或停止冲突进程。
- 问题：首次构建缓慢。
  - 处理：配置国内镜像源，加速依赖下载。

## 7. 参考

- CI 工作流（片段）：使用 JDK 21，导出 `MYSQL_USERNAME`、`POSTGRES_USERNAME` 后运行 `:kronos-testing:test`。
- 相关模块：`kronos-core`、`kronos-logging`、`kronos-jdbc-wrapper`、`kronos-codegen`、`kronos-compiler-plugin`、`kronos-testing`。
