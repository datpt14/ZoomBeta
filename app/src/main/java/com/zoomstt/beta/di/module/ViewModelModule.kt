package com.zoomstt.beta.di.module

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.zoomstt.beta.di.ViewModelFactory
import com.zoomstt.beta.ui.screen.activity.main.JoinViewModel
import com.zoomstt.beta.ui.screen.activity.splash.SplashViewModel
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
abstract class ViewModelModule {
    @Binds
    abstract fun bindFactory(factory: ViewModelFactory): ViewModelProvider.Factory

    @Binds
    @IntoMap
    @ViewModelKey(JoinViewModel::class)
    abstract fun bindMainVM(joinViewModel: JoinViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SplashViewModel::class)
    abstract fun bindSplashVM(splashViewModel: SplashViewModel): ViewModel
}
