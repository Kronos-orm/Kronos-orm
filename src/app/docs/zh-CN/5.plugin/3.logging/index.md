{% import "../../../macros/macros-zh-CN.njk" as $ %}
{{ NgDocActions.demo("AnimateLogoComponent", {container: false}) }}

## 已经支持的日志框架

Kronos支持对接其他的日志框架，目前支持的日志框架如下：

- [SLF4J](https://www.slf4j.org/manual.html)

- [Apache Commons Logging](https://commons.apache.org/proper/commons-logging/)

- [Android Logging](https://developer.android.com/reference/kotlin/android/util/Log)
- [JDK Logger](https://docs.oracle.com/javase/8/docs/api/java/util/logging/Logger.html)

当采用默认日志实现时，可通过显式指定`KLoggerType`为`DEFAULT_LOGGER`或省略`KLoggerType`参数来实现。

## log4j集成示例

以下是一个使用`Springboot + Kronos + JDK 17 + Maven + Kotlin 2.0.0` 的示例，演示了如何将`Kronos`与`log4j`框架结合使用。



### 1. 显式指定KLoggerType

需要显式指定`loggerType`为相应的类型，如未显式指定则使用的是`kronosorm`默认日志输出格式，
以下仅列出部分，详细请参考{{ $.keyword("getting-started/global-config", ["全局设置"]) }}。

```kotlin
Kronos.apply {
    loggerType = KLoggerType.SLF4J_LOGGER
}
```



### 2. 依赖项

引入`SpringBoot`相关依赖项及`log4j`依赖项

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter</artifactId>
    <exclusions>
        <exclusion>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-logging</artifactId>
        </exclusion>
    </exclusions>
</dependency>


        <!-- 包含 mvc,aop 等jar资源 -->
<dependency>
<groupId>org.springframework.boot</groupId>
<artifactId>spring-boot-starter-web</artifactId>
<exclusions>
    <!-- 切换log4j2日志读取 -->
    <exclusion>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-logging</artifactId>
    </exclusion>
</exclusions>
</dependency>


        <!-- 配置 log4j2 -->
<dependency>
<groupId>org.springframework.boot</groupId>
<artifactId>spring-boot-starter-log4j2</artifactId>
</dependency>
        <!-- 加上这个才能辨认到log4j2.yml文件 -->
<dependency>
<groupId>com.fasterxml.jackson.dataformat</groupId>
<artifactId>jackson-dataformat-yaml</artifactId>
</dependency>
```



### 3. 配置相应的log4j2.yml及application.yml文件

#### log4j2.yml

```yml
  # 共有8个级别，按照从低到高为：ALL < TRACE < DEBUG < INFO < WARN < ERROR < FATAL < OFF。
  # intLevel值依次为0,100,200,300,400,500,600,700
  # intLevel 值越小，级别越高
  Configuration:
    #日志框架本身的输出日志级别
    status: WARN
    #自动加载配置文件的间隔时间，不低于5秒
    monitorInterval: 5

    Properties: # 定义全局变量
      Property: # 缺省配置（用于开发环境）。其他环境需要在VM参数中指定，如下：
        #测试：-Dlog.level.console=warn -Dlog.level.xjj=trace
        #生产：-Dlog.level.console=warn -Dlog.level.xjj=info
        - name: log.level.console
          value: info
        - name: log.path
          value: ./${project.name}_log
        - name: project.name
          value: daily
        - name: log.pattern
          value: "%d{yyyy-MM-dd HH:mm:ss.SSS} -%5p ${PID:-} [%15.15t] %-30.30C{1.} : %m%n"

    Appenders:
      Console:  #输出到控制台
        name: CONSOLE
        target: SYSTEM_OUT
        PatternLayout:   #日志消息格式
          pattern: ${log.pattern}
      #   启动日志
      RollingFile:
        - name: ROLLING_FILE
          fileName: ${log.path}/daily/${project.name}.log        #输出文件的地址
          filePattern: "${log.path}/daily/$${date:yyyy-MM-dd}/${project.name}-%d{yyyy-MM-dd}-%i.log.gz"   #文件生成规则
          PatternLayout:
            pattern: ${log.pattern}
          Filters:
            #  一定要先去除不接受的日志级别，然后获取需要接受的日志级别
            ThresholdFilter:   # 日志级别过滤器
              - level: error   # 日志级别
                onMatch: DENY  # 高于的拒绝
                onMismatch: NEUTRAL  # 低于的
              - level: info
                onMatch: ACCEPT
                onMismatch: DENY
          Policies:
            SizeBasedTriggeringPolicy: # 日志拆分规则
              size: "10MB"
            TimeBasedTriggeringPolicy:  # 按天分类
              modulate: true
              interval: 1
          DefaultRolloverStrategy:     # 单目录下，文件最多20个，超过会删除最早之前的
            max: 20
        #   错误日志
        - name: EXCEPTION_ROLLING_FILE
          ignoreExceptions: false
          fileName: ${log.path}/exception/${project.name}_exception.log
          filePattern: "${log.path}/exception/$${date:yyyy-MM-dd}/${project.name}-%d{yyyy-MM-dd}-%i.log.gz"
          ThresholdFilter:
            level: error
            #onMatch="ACCEPT" 匹配该级别及以上
            #onMatch="DENY"  不匹配该级别及以上
            #onMismatch="ACCEPT"  表示匹配该级别以下的级别
            #onMismatch="DENY"  不表示匹配该级别以下的级别
            onMatch: ACCEPT
            onMismatch: DENY
          PatternLayout:
            pattern: ${log.pattern}
          Policies:
            SizeBasedTriggeringPolicy: # 日志拆分规则
              size: "10MB"
            TimeBasedTriggeringPolicy:  # 按天分类
              modulate: true
              interval: 1
          DefaultRolloverStrategy:     # 文件最多100个
            max: 100
        # 警告日志
        - name: WARN_ROLLING_FILE
          ignoreExceptions: false
          fileName: ${log.path}/warn/${project.name}_warn.log
          filePattern: "${log.path}/warn/$${date:yyyy-MM-dd-dd}/${project.name}-%d{yyyy-MM-dd}-%i.log.gz"
          ThresholdFilter:
            level: warn
            #onMatch="ACCEPT" 匹配该级别及以上
            #onMatch="DENY"  不匹配该级别及以上
            #onMismatch="ACCEPT"  表示匹配该级别以下的级别
            #onMismatch="DENY"  不表示匹配该级别以下的级别
            onMatch: ACCEPT
            onMismatch: DENY
          PatternLayout:
            pattern: ${log.pattern}
          Policies:
            SizeBasedTriggeringPolicy: # 日志拆分规则
              size: "10MB"
            TimeBasedTriggeringPolicy:  # 按天分类
              modulate: true
              interval: 1
          DefaultRolloverStrategy:     # 文件最多100个
            max: 20
        # 用户行为日志
        - name: ROLLING_FILE_USER
          fileName: ${log.path}/user/user-${project.name}.log
          filePattern: "${log.path}/user/$${date:yyyy-MM-dd}/user-${project.name}-%d{yyyy-MM-dd}-%i.log.gz"
          PatternLayout:
            pattern: ${log.pattern}
          Filters:
            #        一定要先去除不接受的日志级别，然后获取需要接受的日志级别
            ThresholdFilter:
              - level: error
                onMatch: DENY
                onMismatch: NEUTRAL
                #onMismatch:NEUTRAL 交给下一个filter处理
              - level: info
                onMatch: ACCEPT
                onMismatch: DENY
          Policies:
            SizeBasedTriggeringPolicy: # 日志拆分规则
              size: "10MB"
            TimeBasedTriggeringPolicy:  # 按天分类
              modulate: true
              interval: 1
          DefaultRolloverStrategy:     # 文件最多100个
            max: 100

    Loggers:
      Root:
        # 共有8个级别，按照从低到高为：ALL < TRACE < DEBUG < INFO < WARN < ERROR < FATAL < OFF  选择all则输出全部的日志
        level: info
        AppenderRef:
          - ref: CONSOLE
          - ref: ROLLING_FILE
          - ref: EXCEPTION_ROLLING_FILE
          - ref: WARN_ROLLING_FILE
      Logger:
        - name: exception
          level: debug
          additivity: true  #追加
          AppenderRef:
            - ref: EXCEPTION_ROLLING_FILE

        #监听具体包下面的日志
        #    Logger: # 为com.xjj包配置特殊的Log级别，方便调试
        - name: com.example.aspect
          additivity: false
          level: info
          AppenderRef:
            - ref: CONSOLE
            - ref: ROLLING_FILE_USER
```

#### application.yml

指明`log4j2.yml`的位置(注意将原本`springboot`文件中内置日志的相关配置清除）

```yml
logging:
  config: classpath:log4j2.yml
```





## 其他框架

对于`Apache Commons Logging`等日志框架，写法与`log4j`几乎完全相同，只需根据不同框架的具体实现进行替换即可。
