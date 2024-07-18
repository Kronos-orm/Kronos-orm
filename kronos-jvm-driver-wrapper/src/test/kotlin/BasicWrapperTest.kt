import com.kotlinorm.Kronos
import com.kotlinorm.KronosBasicWrapper
import com.kotlinorm.beans.namingStrategy.LineHumpNamingStrategy
import org.apache.commons.dbcp2.BasicDataSource

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
            dataSource = { KronosBasicWrapper(ds) }
        }
    }
}