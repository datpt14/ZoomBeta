package com.zoomstt.beta.ui.screen.activity.splash

import android.content.Context
import android.content.Intent
import com.zoomstt.beta.R
import com.zoomstt.beta.databinding.ActivitySplashBinding
import com.zoomstt.beta.ui.base.BaseActivity
import com.zoomstt.beta.ui.screen.activity.main.JoinActivity
import java.util.*
import kotlin.concurrent.schedule

class SplashActivity :
    BaseActivity<ActivitySplashBinding, SplashViewModel>(R.layout.activity_splash) {

    override fun onBackPressed() {}

    companion object {
        @JvmStatic
        fun intent(context: Context): Intent {
            val intent = Intent(context, SplashActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            return intent
        }
    }

    override fun viewModelClass() = SplashViewModel::class.java

    override fun ActivitySplashBinding.initView() {
        Timer("SHOW MAIN ACTIVITY", false).schedule(1500) {
            startActivity(JoinActivity.intent(context = applicationContext))
            finish()
        }
    }

    override fun ActivitySplashBinding.addEvent() {

    }

    override fun SplashViewModel.observeLiveData() {
    }
}