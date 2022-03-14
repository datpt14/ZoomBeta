package com.zoomstt.beta.data.repository

import android.content.Context
import com.zoomstt.beta.data.local.PrefHelper
import javax.inject.Inject

class AppRepositoryImpl @Inject constructor(val context: Context, val prefHelper: PrefHelper) : AppRepository {
}