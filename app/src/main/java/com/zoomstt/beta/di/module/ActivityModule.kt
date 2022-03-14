package com.zoomstt.beta.di.module

import com.zoomstt.beta.ui.screen.activity.main.JoinActivity
import com.zoomstt.beta.ui.screen.activity.splash.SplashActivity
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class ActivityModule {
    @ContributesAndroidInjector(modules = [FragmentModule::class])
    abstract fun contributeMain(): JoinActivity

    @ContributesAndroidInjector(modules = [FragmentModule::class])
    abstract fun contributeSplash(): SplashActivity
}
