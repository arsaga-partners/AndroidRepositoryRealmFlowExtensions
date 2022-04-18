package jp.co.arsaga.extensions.repositoryRealmFlow

import io.realm.RealmModel
import io.realm.RealmQuery
import jp.co.arsaga.extensions.repositoryFlow.StateFlowRepository
import kotlinx.coroutines.CoroutineScope

abstract class StateFlowRealmRepository<Res : RealmModel?, Req>(
    private val coroutineScope: CoroutineScope,
    realmClazz: Class<Res>,
    override val requestQuery: (() -> Req)? = null
) : BaseStateFlowRelationalRealmRepository<Res, Req>(coroutineScope, requestQuery) {

    open fun realmFilter(whereQuery: RealmQuery<Res>): RealmQuery<Res> = whereQuery

    override val realm: RealmChildRepository<Res> = object : RealmChildRepository<Res>() {
        override fun realmFilterQuery(whereQuery: RealmQuery<Res>): RealmQuery<Res> = realmFilter(whereQuery)
        override val clazz: Class<Res> = realmClazz
        override val dataPushCommand: (List<Res>) -> Unit = {
            dataPush(dataSource.value.copy(data = it.firstOrNull()))
        }
    }
}

abstract class StateFlowRelationalRealmRepository<Res : RealmModel?, Req>(
    private val coroutineScope: CoroutineScope,
    override val requestQuery: (() -> Req)? = null
) : BaseStateFlowRelationalRealmRepository<List<Res>?, Req>(coroutineScope) {

    protected fun dataPushCommand(dataList: List<Res>) {
        dataPush(dataSource.value.copy(data = dataList))
    }

    open class RealmRepository<Res : RealmModel?>(
        realmClazz: Class<Res>,
        command: (List<Res>) -> Unit
    ) : RealmChildRepository<Res>() {
        override val clazz: Class<Res> = realmClazz
        override val dataPushCommand: (List<Res>) -> Unit = command
    }
}

sealed class BaseStateFlowRelationalRealmRepository<Res, Req>(
    private val coroutineScope: CoroutineScope,
    override val requestQuery: (() -> Req)? = null
) : StateFlowRepository<Res?, Req>(coroutineScope) {

    protected abstract val realm: RealmChildRepository<out RealmModel?>?

    override fun onActive(isActive: Boolean) {
        super.onActive(isActive)
        if (isActive) realm?.onActive()
        else realm?.onInactive()
    }
}