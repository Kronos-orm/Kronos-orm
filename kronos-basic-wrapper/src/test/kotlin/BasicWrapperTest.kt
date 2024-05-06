import com.kotoframework.Kronos
import com.kotoframework.KotoBasicWrapper
import com.kotoframework.beans.namingStrategy.LineHumpNamingStrategy
import org.apache.commons.dbcp.BasicDataSource

class BasicWrapperTest {
    private val dataSource = BasicDataSource().apply {
        driverClassName = "com.mysql.jdbc.Driver"
        url = "jdbc:mysql://localhost:3306/test"
        username = "root"
        password = "root"
        maxIdle = 10
        maxActive = 10
    }

    init {
        Kronos.apply {
            fieldNamingStrategy = LineHumpNamingStrategy
            tableNamingStrategy = LineHumpNamingStrategy
            defaultDataSource = { KotoBasicWrapper(dataSource) }
        }
    }
}