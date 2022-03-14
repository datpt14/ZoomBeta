package com.zoomstt.beta.ui.base

import androidx.lifecycle.ViewModel
import com.zoomstt.beta.data.repository.AppRepository
import com.zoomstt.beta.utils.SingleLiveEvent
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import javax.inject.Inject

abstract class BaseViewModel : ViewModel() {

    private val compositeDisposable = CompositeDisposable()

    fun addDisposable(disposable: Disposable) {
        compositeDisposable.add(disposable)
    }

    fun onActivityDestroyed() {
        compositeDisposable.clear()
    }

    @Inject
    protected lateinit var apiRepository: AppRepository

    val viewState = SingleLiveEvent<Int>()
    protected val disposable = CompositeDisposable()

    override fun onCleared() {
        super.onCleared()
        disposable.clear()
    }
}
