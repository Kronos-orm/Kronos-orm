import com.kotlinorm.annotations.CreateTime
import com.kotlinorm.annotations.LogicDelete
import com.kotlinorm.annotations.UpdateTime
import com.kotlinorm.annotations.Version
import com.kotlinorm.interfaces.KPojo

data class StrategyUser(
    var id: Int? = null,
    @CreateTime
    var createdAt: String? = null,
    @UpdateTime
    var updatedAt: String? = null,
    @LogicDelete
    var deleted: Boolean? = null,
    @Version
    var version: Int? = null,
) : KPojo

fun box(): String {
    val user = StrategyUser()
    val createTime = user.kronosCreateTime()
    val updateTime = user.kronosUpdateTime()
    val logicDelete = user.kronosLogicDelete()
    val optimisticLock = user.kronosOptimisticLock()

    return when {
        !createTime.enabled -> "Fail: createTime disabled"
        createTime.field.name != "createdAt" -> "Fail: createTime field was ${createTime.field.name}"
        !updateTime.enabled -> "Fail: updateTime disabled"
        updateTime.field.name != "updatedAt" -> "Fail: updateTime field was ${updateTime.field.name}"
        !logicDelete.enabled -> "Fail: logicDelete disabled"
        logicDelete.field.name != "deleted" -> "Fail: logicDelete field was ${logicDelete.field.name}"
        !optimisticLock.enabled -> "Fail: optimisticLock disabled"
        optimisticLock.field.name != "version" -> "Fail: optimisticLock field was ${optimisticLock.field.name}"
        else -> "OK"
    }
}
