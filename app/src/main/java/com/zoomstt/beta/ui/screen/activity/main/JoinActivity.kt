package com.zoomstt.beta.ui.screen.activity.main

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.SpeechRecognizer
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.zoomstt.beta.R
import com.zoomstt.beta.TemporaryDataHelper
import com.zoomstt.beta.databinding.ActivityMainBinding
import com.zoomstt.beta.ui.base.BaseActivity
import com.zoomstt.beta.zoombeta.MyMeetingActivity
import com.zoomstt.beta.zoombeta.SimpleInMeetingListener
import com.zoomstt.beta.zoombeta.audio.MeetingAudioCallback
import com.zoomstt.beta.zoombeta.audio.MeetingAudioHelper
import com.zoomstt.beta.zoombeta.initsdk.InitAuthSDKCallback
import com.zoomstt.beta.zoombeta.initsdk.InitAuthSDKHelper
import com.zoomstt.beta.zoombeta.zoommeetingui.ZoomMeetingUISettingHelper
import us.zoom.sdk.*


class JoinActivity : BaseActivity<ActivityMainBinding, JoinViewModel>(R.layout.activity_main),
    MeetingServiceListener, MeetingAudioCallback.AudioEvent, InitAuthSDKCallback {

    private var TAG = "JoinActivity"

    private var mZoomSDK: ZoomSDK? = null
    private var recognizer: SpeechRecognizer? = null

    private var mInMeetingService: InMeetingService? = null

    private var mInMeetingServiceListener: InMeetingServiceListener? = null

    private var meetingAudioHelper: MeetingAudioHelper? = null

    private var inMeetingAudioController: InMeetingAudioController? = null

    val REQUEST_AUDIO_CODE = 1011

    var audioCallBack: MeetingAudioHelper.AudioCallBack =
        object : MeetingAudioHelper.AudioCallBack {
            override fun requestAudioPermission(): Boolean {
                if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(
                        this@JoinActivity,
                        arrayOf(Manifest.permission.RECORD_AUDIO),
                        REQUEST_AUDIO_CODE
                    )
                    return false
                }
                return true
            }

            override fun updateAudioButton() {
                Toast.makeText(applicationContext, " eAudioButton", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

    companion object {
        @JvmStatic
        fun intent(context: Context): Intent {
            return Intent(context, JoinActivity::class.java)
        }
    }

    override fun viewModelClass(): Class<JoinViewModel> {
        return JoinViewModel::class.java
    }

    override fun ActivityMainBinding.addEvent() {

        btnJoin.setOnClickListener {
            ZoomSDK.getInstance().meetingSettingsHelper.isCustomizedMeetingUIEnabled = true

            if (!mZoomSDK!!.isInitialized) {
                InitAuthSDKHelper.getInstance().initSDK(applicationContext, this@JoinActivity)
                return@setOnClickListener
            }

            val schoolName = inputSchool.text.toString().trim()
            val userName = inputName.text.toString().trim()
            val classID = inputClassId.text.toString().trim()
            val password = inputPassword.text.toString().trim()

            if (userName.isNotEmpty() && classID.isNotEmpty() && password.isNotEmpty()) {
                TemporaryDataHelper.instance().schoolName = schoolName
                joinMeeting(meetingNumber = classID, meetingPass = password, userName = userName)
            } else {
                showSnackMessage(btnJoin, "Validate input")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        InitAuthSDKHelper.getInstance().initSDK(this, this)

        if (mZoomSDK?.isInitialized == true) {
            ZoomSDK.getInstance().meetingService.addListener(this)
            ZoomSDK.getInstance().meetingSettingsHelper.enable720p(false)
        }

        binding.btnJoin.isEnabled = false

        initZoomSDK()
    }

    var handle = InMeetingNotificationHandle { context, _ ->
        val intent = Intent(context, MyMeetingActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        if (context !is Activity) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        intent.action = InMeetingNotificationHandle.ACTION_RETURN_TO_CONF
        context.startActivity(intent)
        true
    }

    private fun showMeetingUi() {
        if (ZoomSDK.getInstance().meetingSettingsHelper.isCustomizedMeetingUIEnabled) {
            val sharedPreferences = getSharedPreferences("UI_Setting", MODE_PRIVATE)
            val enable = sharedPreferences.getBoolean("enable_rawdata", false)
            var intent: Intent? = null
            if (!enable) {
                intent = Intent(this, MyMeetingActivity::class.java)
                intent.putExtra("from", MyMeetingActivity.JOIN_FROM_UNLOGIN)
            }
            intent!!.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            this.startActivity(intent)
        }
    }

    override fun ActivityMainBinding.initView() {
    }

    private fun initZoomSDK() {
        mZoomSDK = ZoomSDK.getInstance()
        val initParams = ZoomSDKInitParams()
        initParams.appKey = "j40iFKV9wzkOsJwf4FR4zGDnOzKkKgati8gu"
        initParams.appSecret = "itYtnKGqkiQFBc6CxjjFV9xbvUxtRtKp9Q9Q"
        initParams.enableLog = true
        initParams.enableGenerateDump = true
        initParams.logSize = 5
        initParams.domain = "zoom.us"

        mZoomSDK?.meetingService?.addListener(this)
        mZoomSDK?.meetingSettingsHelper?.enable720p(false)

        mInMeetingService = mZoomSDK?.inMeetingService

        registerListener()
    }

    private fun registerListener() {
        MeetingAudioCallback.getInstance().addListener(this)
        mInMeetingServiceListener = object : SimpleInMeetingListener() {
            override fun onRecordingStatus(status: InMeetingServiceListener.RecordingStatus) {
                Toast.makeText(applicationContext, "Recording_Start", Toast.LENGTH_SHORT).show()
                if (status === InMeetingServiceListener.RecordingStatus.Recording_Start) {
                    Toast.makeText(applicationContext, "Recording_Start", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onLocalRecordingStatus(status: InMeetingServiceListener.RecordingStatus) {
                if (status === InMeetingServiceListener.RecordingStatus.Recording_Start) {
                    Toast.makeText(applicationContext, "Recording_Start 111", Toast.LENGTH_SHORT)
                        .show()
                }
            }

            override fun onInvalidReclaimHostkey() {}
        }
        mZoomSDK?.inMeetingService?.addListener(mInMeetingServiceListener)

        if (mInMeetingService != null) {
            meetingAudioHelper = MeetingAudioHelper(audioCallBack)
            meetingAudioHelper!!.switchAudio()
            inMeetingAudioController = mInMeetingService?.inMeetingAudioController
        }
    }


    private fun unRegisterListener() {
        try {
            MeetingAudioCallback.getInstance().removeListener(this)
            mZoomSDK?.inMeetingService?.removeListener(mInMeetingServiceListener)
        } catch (e: Exception) {
        }
    }


    override fun JoinViewModel.observeLiveData() {
    }

    override fun onMeetingStatusChanged(status: MeetingStatus?, errorCode: Int, internalErrorCode: Int) {
        Log.d(TAG, "status $status:$errorCode:$internalErrorCode")
        if (!ZoomSDK.getInstance().isInitialized) {
            return
        }
        if (status == MeetingStatus.MEETING_STATUS_CONNECTING) {
            if (ZoomMeetingUISettingHelper.useExternalVideoSource) {
                ZoomMeetingUISettingHelper.changeVideoSource(true, applicationContext)
            }
        }
        if (ZoomSDK.getInstance().meetingSettingsHelper.isCustomizedMeetingUIEnabled) {
            if (status == MeetingStatus.MEETING_STATUS_CONNECTING) {
                ZoomSDK.getInstance().meetingSettingsHelper.isCustomizedMeetingUIEnabled = true
                showMeetingUi()
            }
        }
    }

    private fun joinMeeting(meetingNumber: String, meetingPass: String, userName: String) {
        val options = JoinMeetingOptions()
        val params = JoinMeetingParams()
        params.displayName = userName
        params.meetingNo = meetingNumber
        params.password = meetingPass
        mZoomSDK?.meetingService?.joinMeetingWithParams(
            applicationContext,
            params,
            options
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        recognizer?.stopListening()
        recognizer?.destroy()
        unRegisterListener()
    }

    override fun onUserAudioStatusChanged(userId: Long) {
        meetingAudioHelper?.onUserAudioStatusChanged(userId)
    }

    override fun onUserAudioTypeChanged(userId: Long) {
        meetingAudioHelper?.onUserAudioTypeChanged(userId)
    }

    override fun onMyAudioSourceTypeChanged(type: Int) {
        meetingAudioHelper?.onMyAudioSourceTypeChanged(type)
    }

    override fun onZoomSDKInitializeResult(errorCode: Int, internalErrorCode: Int) {
        if (errorCode != ZoomError.ZOOM_ERROR_SUCCESS) {
            Toast.makeText(
                this,
                "Failed to initialize Zoom SDK. Error: $errorCode, internalErrorCode=$internalErrorCode",
                Toast.LENGTH_LONG
            ).show()
        } else {
            ZoomSDK.getInstance().zoomUIService.enableMinimizeMeeting(true)
            ZoomSDK.getInstance().zoomUIService.setMiniMeetingViewSize(
                CustomizedMiniMeetingViewSize(0, 0, 360, 540)
            )
            setMiniWindows()
            ZoomSDK.getInstance().meetingSettingsHelper.enable720p(false)
            ZoomSDK.getInstance().meetingSettingsHelper.enableShowMyMeetingElapseTime(true)
            ZoomSDK.getInstance().meetingService.addListener(this)
            ZoomSDK.getInstance().meetingSettingsHelper.setCustomizedNotificationData(null, handle)
            binding.btnJoin.isEnabled = true
        }
    }

    private fun setMiniWindows() {
        if (null != mZoomSDK && mZoomSDK!!.isInitialized && !mZoomSDK!!.meetingSettingsHelper.isCustomizedMeetingUIEnabled) {
            ZoomSDK.getInstance().zoomUIService.setZoomUIDelegate(object : SimpleZoomUIDelegate() {
                override fun afterMeetingMinimized(activity: Activity) {
                    val intent = Intent(activity, JoinActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    activity.startActivity(intent)
                }
            })
        }
    }

    override fun onZoomAuthIdentityExpired() {
    }
}