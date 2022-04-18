package jp.co.arsaga.extensions.repositoryRealmFlow

import io.realm.*

abstract class RealmChildRepository<T : RealmModel?> {

    abstract val dataPushCommand: (List<T>) -> Unit

    private val listener = RealmChangeListener<RealmResults<T>> { results ->
        realmInstance?.copyFromRealm(results)?.let {
            dataPushCommand(it)
        }
    }

    private var realmInstance: Realm? = null
    private var results: RealmResults<T>? = null
    private fun realmWhereQuery(realm: Realm): RealmQuery<T> = realm.where(clazz)

    protected abstract val clazz: Class<T>

    protected open fun realmFilterQuery(whereQuery: RealmQuery<T>): RealmQuery<T> = whereQuery

    fun onActive() {
        realmInstance = Realm.getDefaultInstance()?.also {
            results = realmFilterQuery(realmWhereQuery(it)).findAllAsync().also {
                it.addChangeListener(listener)
            }
        }
    }

    fun onInactive() {
        results?.removeAllChangeListeners()
        realmInstance?.close()
        results = null
        realmInstance = null
    }

    companion object {
        fun <T : RealmModel?> query(
            clazz: Class<T>,
            realmFilterQuery: RealmQuery<T>.() -> RealmQuery<T>,
            onSuccess: Realm.(List<T>) -> Unit
        ) {
            Realm.getDefaultInstance()?.use {
                it.where(clazz)
                    .run(realmFilterQuery)
                    .findAllAsync()
                    ?.run { onSuccess(it, this) }
            }
        }
        fun transaction(
            onSuccess: Realm.() -> Unit = {},
            onError: Realm.(Throwable) -> Unit = {},
            realmAction: Realm.() -> Unit
        ) {
            Realm.getDefaultInstance().use { realm ->
                realm.executeTransactionAsync({
                    realmAction(it)
                }, {
                    onSuccess(realm)
                }, {
                    onError(realm, it)
                })
            }
        }

        fun <T : RealmModel> transaction(
            data: List<T>,
            onSuccess: Realm.() -> Unit = {},
            onError: Realm.(Throwable) -> Unit = {}
        ) {
            transaction(onSuccess, onError) { insertOrUpdate(data) }
        }

        fun <T : RealmModel> transaction(
            data: T,
            onSuccess: Realm.() -> Unit = {},
            onError: Realm.(Throwable) -> Unit = {}
        ) {
            transaction(onSuccess, onError) { insertOrUpdate(data) }
        }
    }
}