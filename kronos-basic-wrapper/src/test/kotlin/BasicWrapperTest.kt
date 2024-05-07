import com.kotlinorm.Kronos
import com.kotlinorm.KotoBasicWrapper
import com.kotlinorm.beans.namingStrategy.LineHumpNamingStrategy
import org.apache.commons.dbcp.BasicDataSource

class BasicWrapperTest {
    private val ds = BasicDataSource().apply {
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
            dataSource = { KotoBasicWrapper(ds) }
        }
    }
}