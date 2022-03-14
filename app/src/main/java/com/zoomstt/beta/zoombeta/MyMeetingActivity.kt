package com.zoomstt.beta.zoombeta

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.*
import android.view.GestureDetector.SimpleOnGestureListener
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.zoomstt.beta.R
import com.zoomstt.beta.ui.screen.activity.main.JoinActivity
import com.zoomstt.beta.zoombeta.MeetingOptionBar.MeetingOptionBarCallBack
import com.zoomstt.beta.zoombeta.audio.MeetingAudioCallback
import com.zoomstt.beta.zoombeta.audio.MeetingAudioCallback.AudioEvent
import com.zoomstt.beta.zoombeta.audio.MeetingAudioHelper
import com.zoomstt.beta.zoombeta.audio.MeetingAudioHelper.AudioCallBack
import com.zoomstt.beta.zoombeta.bo.BOEventCallback
import com.zoomstt.beta.zoombeta.bo.BOEventCallback.BOEvent
import com.zoomstt.beta.zoombeta.customer.adapter.AttenderVideoAdapter
import com.zoomstt.beta.zoombeta.customer.adapter.AttenderVideoAdapter.ItemClickListener
import com.zoomstt.beta.zoombeta.livetranscription.LiveTranscriptionRequestHandleDialog
import com.zoomstt.beta.zoombeta.other.MeetingCommonCallback
import com.zoomstt.beta.zoombeta.other.MeetingCommonCallback.CommonEvent
import com.zoomstt.beta.zoombeta.remotecontrol.MeetingRemoteControlHelper
import com.zoomstt.beta.zoombeta.share.CustomShareView
import com.zoomstt.beta.zoombeta.share.MeetingShareCallback
import com.zoomstt.beta.zoombeta.share.MeetingShareCallback.ShareEvent
import com.zoomstt.beta.zoombeta.share.MeetingShareHelper
import com.zoomstt.beta.zoombeta.share.MeetingShareHelper.MeetingShareUICallBack
import com.zoomstt.beta.zoombeta.user.MeetingUserCallback
import com.zoomstt.beta.zoombeta.user.MeetingUserCallback.UserEvent
import com.zoomstt.beta.zoombeta.video.MeetingVideoCallback
import com.zoomstt.beta.zoombeta.video.MeetingVideoCallback.VideoEvent
import com.zoomstt.beta.zoombeta.video.MeetingVideoHelper
import com.zoomstt.beta.zoombeta.video.MeetingVideoHelper.VideoCallBack
import us.zoom.sdk.*
import us.zoom.sdk.IBOAttendeeEvent.ATTENDEE_REQUEST_FOR_HELP_RESULT
import us.zoom.sdk.InMeetingLiveTranscriptionController.*
import us.zoom.sdk.InMeetingServiceListener.RecordingStatus


class MyMeetingActivity : FragmentActivity(), View.OnClickListener, VideoEvent,
    AudioEvent, ShareEvent,
    UserEvent, CommonEvent, SmsListener, BOEvent {

    private val TAG = MyMeetingActivity::class.java.simpleName

    private var currentLayoutType = -1
    private val LAYOUT_TYPE_PREVIEW = 0
    private val LAYOUT_TYPE_WAITHOST = 1
    private val LAYOUT_TYPE_IN_WAIT_ROOM = 2
    private val LAYOUT_TYPE_ONLY_MYSELF = 3
    private val LAYOUT_TYPE_ONETOONE = 4
    private val LAYOUT_TYPE_LIST_VIDEO = 5
    private val LAYOUT_TYPE_VIEW_SHARE = 6
    private val LAYOUT_TYPE_SHARING_VIEW = 7
    private val LAYOUT_TYPE_WEBINAR_ATTENDEE = 8

    private var mWaitJoinView: View? = null
    private var mWaitRoomView: View? = null
    private var mConnectingText: TextView? = null
    private var mBtnJoinBo: Button? = null
    private var mBtnRequestHelp: Button? = null

    private var videoListLayout: LinearLayout? = null

    private var layout_lans: View? = null

    private var mMeetingFailed = false

    private var mDefaultVideoView: MobileRTCVideoView? = null
    private var mDefaultVideoViewMgr: MobileRTCVideoViewManager? = null

    private var meetingAudioHelper: MeetingAudioHelper? = null

    private var meetingVideoHelper: MeetingVideoHelper? = null

    private var meetingShareHelper: MeetingShareHelper? = null

    private var remoteControlHelper: MeetingRemoteControlHelper? = null

    private var mMeetingService: MeetingService? = null

    private var mInMeetingService: InMeetingService? = null

    private var smsService: SmsService? = null
    private var mInMeetingServiceListener: InMeetingServiceListener? = null

    private var mScreenInfoData: Intent? = null

    private var mShareView: MobileRTCShareView? = null
    private var mDrawingView: AnnotateToolbar? = null
    private var mMeetingVideoView: FrameLayout? = null
    private var mViewApps: ImageView? = null

    private var mNormalSenceView: View? = null

    private var customShareView: CustomShareView? = null

    private var mVideoListView: RecyclerView? = null

    private var mAdapter: AttenderVideoAdapter? = null

    var meetingOptionBar: MeetingOptionBar? = null

    private var gestureDetector: GestureDetector? = null

    companion object {
        val REQUEST_CHAT_CODE = 1000
        val REQUEST_PLIST = 1001

        val REQUEST_CAMERA_CODE = 1010

        val REQUEST_AUDIO_CODE = 1011

        val REQUEST_STORAGE_CODE = 1012

        val REQUEST_SHARE_SCREEN_PERMISSION = 1001

        protected val REQUEST_SYSTEM_ALERT_WINDOW = 1002

        protected val REQUEST_SYSTEM_ALERT_WINDOW_FOR_MINIWINDOW = 1003

        var mCurShareUserId: Long = -1

        val JOIN_FROM_UNLOGIN = 1
    }

    @SuppressLint("NewApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            Log.d(TAG, "onCreate: ZoomUtils initialize SDK ")
//            ZoomSDK.getInstance().initialize(this, SDK_KEY, SDK_SECRET, WEB_DOMAIN, this);
        }

        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        mMeetingService = ZoomSDK.getInstance().meetingService
        mInMeetingService = ZoomSDK.getInstance().inMeetingService
        if (mMeetingService == null || mInMeetingService == null) {
            finish()
            return
        }

        // init other
        meetingAudioHelper = MeetingAudioHelper(audioCallBack)
        meetingVideoHelper = MeetingVideoHelper(this, videoCallBack)
        meetingShareHelper = MeetingShareHelper(this, shareCallBack)
        registerListener()
        setContentView(R.layout.my_meeting_layout)
        gestureDetector = GestureDetector(GestureDetectorListener())
        meetingOptionBar = findViewById<View>(R.id.meeting_option_contain) as MeetingOptionBar
        meetingOptionBar!!.setCallBack(callBack)
        mMeetingVideoView = findViewById<View>(R.id.meetingVideoView) as FrameLayout
        mShareView = findViewById<View>(R.id.sharingView) as MobileRTCShareView
        mDrawingView = findViewById<View>(R.id.drawingView) as AnnotateToolbar
        mViewApps = findViewById(R.id.iv_view_apps)
        mViewApps?.setOnClickListener(this)
        mWaitJoinView = findViewById(R.id.waitJoinView)
        mWaitRoomView = findViewById(R.id.waitingRoom)
        val inflater = layoutInflater
        mNormalSenceView = inflater.inflate(R.layout.layout_meeting_content_normal, null)
        mDefaultVideoView =
            mNormalSenceView!!.findViewById<View>(R.id.videoView) as MobileRTCVideoView
        customShareView =
            mNormalSenceView!!.findViewById<View>(R.id.custom_share_view) as CustomShareView
        remoteControlHelper = MeetingRemoteControlHelper(customShareView)
        mMeetingVideoView!!.addView(
            mNormalSenceView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        mConnectingText = findViewById<View>(R.id.connectingTxt) as TextView
        mBtnJoinBo = findViewById<View>(R.id.btn_join_bo) as Button
        mBtnRequestHelp = findViewById(R.id.btn_request_help)
        mVideoListView = findViewById<View>(R.id.videoList) as RecyclerView
        mVideoListView!!.bringToFront()
        videoListLayout = findViewById(R.id.videoListLayout)
        layout_lans = findViewById(R.id.layout_lans)
        mVideoListView!!.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        mAdapter = AttenderVideoAdapter(this, windowManager.defaultDisplay.width, pinVideoListener)
        mVideoListView!!.adapter = mAdapter
        mBtnJoinBo!!.setOnClickListener(this)
        mBtnRequestHelp?.setOnClickListener(this)
        refreshToolbar()

        meetingOptionBar?.initSignaIR(this)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
    }

    var videoCallBack: VideoCallBack = object : VideoCallBack {
        override fun requestVideoPermission(): Boolean {
            return checkVideoPermission()
        }

        override fun showCameraList(popupWindow: PopupWindow) {
            popupWindow.showAsDropDown(meetingOptionBar!!.switchCameraView, 0, 20)
        }
    }

    var audioCallBack: AudioCallBack = object : AudioCallBack {
        override fun requestAudioPermission(): Boolean {
            if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this@MyMeetingActivity,
                    arrayOf(Manifest.permission.RECORD_AUDIO),
                    REQUEST_AUDIO_CODE
                )
                return false
            }
            return true
        }

        override fun updateAudioButton() {
            meetingOptionBar!!.updateAudioButton()
        }
    }

    var shareCallBack: MeetingShareUICallBack = object : MeetingShareUICallBack {
        override fun showShareMenu(popupWindow: PopupWindow) {
            popupWindow.showAtLocation(
                meetingOptionBar!!.parent as View,
                Gravity.BOTTOM or Gravity.CENTER,
                0,
                150
            )
        }

        override fun getShareView(): MobileRTCShareView {
            return mShareView!!
        }

        override fun requestStoragePermission(): Boolean {
            if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this@MyMeetingActivity,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    REQUEST_STORAGE_CODE
                )
                return false
            }
            return true
        }
    }


    var pinVideoListener =
        ItemClickListener { view, position, userId ->
            if (currentLayoutType == LAYOUT_TYPE_VIEW_SHARE || currentLayoutType == LAYOUT_TYPE_SHARING_VIEW) {
                return@ItemClickListener
            }
            mDefaultVideoViewMgr!!.removeAllVideoUnits()
            val renderInfo = MobileRTCVideoUnitRenderInfo(0, 0, 100, 100)
            mDefaultVideoViewMgr!!.addAttendeeVideoUnit(userId, renderInfo)
        }

    override fun onClick(v: View) {
        val id = v.id
        when (id) {
            R.id.btn_join_bo -> {
                val boController = mInMeetingService!!.inMeetingBOController
                val boAttendee = boController.boAttendeeHelper
                boAttendee?.joinBo()
            }
            R.id.btn_request_help -> attendeeRequestHelp()
            R.id.iv_view_apps -> showApps()
        }
    }

    private fun showApps() {
        val aanController = ZoomSDK.getInstance().inMeetingService.inMeetingAANController
        aanController.showAANPanel(this)
    }

    private fun checkVideoPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                REQUEST_CAMERA_CODE
            )
            return false
        }
        return true
    }

    inner class GestureDetectorListener : SimpleOnGestureListener() {
        override fun onSingleTapUp(e: MotionEvent): Boolean {
            if (mDrawingView?.isAnnotationStarted == true || remoteControlHelper?.isEnableRemoteControl == true) {
                meetingOptionBar?.showMeetingContentAndSetting(true)
                meetingOptionBar?.hideOrShowToolbar(true)
                return true
            }
            val orientation: Int = resources.configuration.orientation
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                if (videoListLayout?.visibility == View.VISIBLE && (e.x >= videoListLayout?.left!! || e.y <= meetingOptionBar?.topBarHeight!!) || e.y >= meetingOptionBar?.bottomBarTop!!) {
                    return true
                }
            } else {
                if (videoListLayout?.visibility == View.VISIBLE && (e.y >= videoListLayout?.top!! || e.y <= meetingOptionBar?.topBarHeight!!) || e.y >= meetingOptionBar?.bottomBarTop!!) {
                    return true
                }
            }
            if (mMeetingService?.meetingStatus == MeetingStatus.MEETING_STATUS_INMEETING) {
                meetingOptionBar?.showMeetingContentAndSetting(true)
                meetingOptionBar?.isShowing?.let { meetingOptionBar?.hideOrShowToolbar(it) }
            }
            return true
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        gestureDetector!!.onTouchEvent(ev)
        return super.dispatchTouchEvent(ev)
    }

    private fun refreshToolbar() {
        if (mMeetingService!!.meetingStatus == MeetingStatus.MEETING_STATUS_INMEETING) {
            mConnectingText!!.visibility = View.GONE
            meetingOptionBar!!.updateMeetingNumber(mInMeetingService!!.currentMeetingNumber.toString() + "")
            meetingOptionBar!!.updateUserName(mInMeetingService!!.myUserInfo.userName)
            meetingOptionBar!!.updateMeetingUserName(mInMeetingService!!.myUserInfo.userName)
            meetingOptionBar!!.receiveMessage(mInMeetingService!!.currentMeetingNumber.toString())
            mViewApps!!.visibility = View.VISIBLE
            meetingOptionBar!!.refreshToolbar()
        } else {
            if (mMeetingService!!.meetingStatus == MeetingStatus.MEETING_STATUS_CONNECTING) {
                mConnectingText!!.visibility = View.VISIBLE
            } else {
                mConnectingText!!.visibility = View.GONE
            }
            meetingOptionBar?.showMeetingContentAndSetting(true)
            meetingOptionBar!!.hideOrShowToolbar(true)
        }
    }

    private fun updateAnnotationBar() {
        if (mCurShareUserId > 0 && !isMySelfWebinarAttendee()) {
            if (meetingShareHelper!!.isSenderSupportAnnotation(mCurShareUserId)) {
                if (mInMeetingService!!.isMyself(mCurShareUserId) && !meetingShareHelper!!.isSharingScreen) {
                    mDrawingView!!.visibility = View.VISIBLE
                } else {
                    if (currentLayoutType == LAYOUT_TYPE_VIEW_SHARE) {
                        mDrawingView!!.visibility = View.VISIBLE
                    } else {
                        mDrawingView!!.visibility = View.GONE
                    }
                }
            } else {
                mDrawingView!!.visibility = View.GONE
            }
        } else {
            mDrawingView!!.visibility = View.GONE
        }
    }

    private fun checkShowVideoLayout(forceRefresh: Boolean) {
        if (!checkVideoPermission()) {
            return
        }
        mDefaultVideoViewMgr = mDefaultVideoView!!.videoViewManager
        if (mDefaultVideoViewMgr != null) {
            val newLayoutType = getNewVideoMeetingLayout()
            if (currentLayoutType != newLayoutType || newLayoutType == LAYOUT_TYPE_WEBINAR_ATTENDEE || forceRefresh) {
                removeOldLayout(currentLayoutType)
                currentLayoutType = newLayoutType
                addNewLayout(newLayoutType)
            }
        }
        updateAnnotationBar()
    }

    private fun getNewVideoMeetingLayout(): Int {
        var newLayoutType = -1
        if (mMeetingService!!.meetingStatus == MeetingStatus.MEETING_STATUS_WAITINGFORHOST) {
            newLayoutType = LAYOUT_TYPE_WAITHOST
            return newLayoutType
        }
        if (mMeetingService!!.meetingStatus == MeetingStatus.MEETING_STATUS_IN_WAITING_ROOM) {
            newLayoutType = LAYOUT_TYPE_IN_WAIT_ROOM
            return newLayoutType
        }
        if (mInMeetingService!!.isWebinarMeeting) {
            if (isMySelfWebinarAttendee()) {
                newLayoutType = LAYOUT_TYPE_WEBINAR_ATTENDEE
                return newLayoutType
            }
        }
        if (meetingShareHelper!!.isOtherSharing) {
            newLayoutType = LAYOUT_TYPE_VIEW_SHARE
        } else if (meetingShareHelper!!.isSharingOut && !meetingShareHelper!!.isSharingScreen) {
            newLayoutType = LAYOUT_TYPE_SHARING_VIEW
        } else {
            val userlist = mInMeetingService!!.inMeetingUserList
            var userCount = 0
            if (userlist != null) {
                userCount = userlist.size
            }
            if (userCount > 1) {
                val preCount = userCount
                for (i in 0 until preCount) {
                    val userInfo = mInMeetingService!!.getUserInfoById(userlist!![i])
                    if (mInMeetingService!!.isWebinarMeeting) {
                        if (userInfo != null && userInfo.inMeetingUserRole == InMeetingUserInfo.InMeetingUserRole.USERROLE_ATTENDEE) {
                            userCount--
                        }
                    }
                }
            }
            newLayoutType = if (userCount == 0) {
                LAYOUT_TYPE_PREVIEW
            } else if (userCount == 1) {
                LAYOUT_TYPE_ONLY_MYSELF
            } else {
                LAYOUT_TYPE_LIST_VIDEO
            }
        }
        return newLayoutType
    }

    private fun removeOldLayout(type: Int) {
        if (type == LAYOUT_TYPE_WAITHOST) {
            mWaitJoinView!!.visibility = View.VISIBLE
            mMeetingVideoView!!.visibility = View.GONE
            meetingOptionBar?.showMeetingContentAndSetting(false)
        } else if (type == LAYOUT_TYPE_IN_WAIT_ROOM) {
            mWaitRoomView!!.visibility = View.VISIBLE
            meetingOptionBar?.showMeetingContentAndSetting(false)
            mMeetingVideoView!!.visibility = View.GONE
        } else if (type == LAYOUT_TYPE_PREVIEW || type == LAYOUT_TYPE_ONLY_MYSELF || type == LAYOUT_TYPE_ONETOONE) {
            mDefaultVideoViewMgr!!.removeAllVideoUnits()
        } else if (type == LAYOUT_TYPE_LIST_VIDEO || type == LAYOUT_TYPE_VIEW_SHARE) {
            mDefaultVideoViewMgr!!.removeAllVideoUnits()
            mDefaultVideoView!!.setGestureDetectorEnabled(false)
        } else if (type == LAYOUT_TYPE_SHARING_VIEW) {
            mShareView!!.visibility = View.GONE
            mMeetingVideoView!!.visibility = View.VISIBLE
        }
        if (type != LAYOUT_TYPE_SHARING_VIEW) {
            if (null != customShareView) {
                customShareView!!.visibility = View.INVISIBLE
            }
        }
    }

    private fun addNewLayout(type: Int) {
        if (type == LAYOUT_TYPE_WAITHOST) {
            mWaitJoinView!!.visibility = View.VISIBLE
            refreshToolbar()
            mMeetingVideoView!!.visibility = View.GONE
        } else if (type == LAYOUT_TYPE_IN_WAIT_ROOM) {
            mWaitRoomView!!.visibility = View.VISIBLE
            videoListLayout!!.visibility = View.GONE
            refreshToolbar()
            mMeetingVideoView!!.visibility = View.GONE
            mDrawingView!!.visibility = View.GONE
        } else if (type == LAYOUT_TYPE_PREVIEW) {
            showPreviewLayout()
        } else if (type == LAYOUT_TYPE_ONLY_MYSELF || type == LAYOUT_TYPE_WEBINAR_ATTENDEE) {
            showOnlyMeLayout()
        } else if (type == LAYOUT_TYPE_ONETOONE) {
            showOne2OneLayout()
        } else if (type == LAYOUT_TYPE_LIST_VIDEO) {
            showVideoListLayout()
        } else if (type == LAYOUT_TYPE_VIEW_SHARE) {
            showViewShareLayout()
        } else if (type == LAYOUT_TYPE_SHARING_VIEW) {
            showSharingViewOutLayout()
        }
    }

    private fun showPreviewLayout() {
        val renderInfo1 = MobileRTCVideoUnitRenderInfo(0, 0, 100, 100)
        mDefaultVideoView!!.visibility = View.VISIBLE
        mDefaultVideoViewMgr!!.addPreviewVideoUnit(renderInfo1)
        videoListLayout!!.visibility = View.GONE
    }

    private fun showOnlyMeLayout() {
        mDefaultVideoView!!.visibility = View.VISIBLE
        videoListLayout!!.visibility = View.GONE
        val renderInfo = MobileRTCVideoUnitRenderInfo(0, 0, 100, 100)
        val myUserInfo = mInMeetingService!!.myUserInfo
        if (myUserInfo != null) {
            mDefaultVideoViewMgr!!.removeAllVideoUnits()
            if (isMySelfWebinarAttendee()) {
                if (mCurShareUserId > 0) {
                    mDefaultVideoViewMgr!!.addShareVideoUnit(mCurShareUserId, renderInfo)
                } else {
                    mDefaultVideoViewMgr!!.addActiveVideoUnit(renderInfo)
                }
            } else {
                mDefaultVideoViewMgr!!.addAttendeeVideoUnit(myUserInfo.userId, renderInfo)
            }
        }
    }


    private fun showOne2OneLayout() {
        mDefaultVideoView!!.visibility = View.VISIBLE
        videoListLayout!!.visibility = View.VISIBLE
        val renderInfo = MobileRTCVideoUnitRenderInfo(0, 0, 100, 100)
        //options.aspect_mode = MobileRTCVideoUnitAspectMode.VIDEO_ASPECT_PAN_AND_SCAN;
        mDefaultVideoViewMgr!!.addActiveVideoUnit(renderInfo)
        mAdapter!!.setUserList(mInMeetingService!!.inMeetingUserList)
        mAdapter?.notifyDataSetChanged()
    }

    private fun showVideoListLayout() {
        val renderInfo = MobileRTCVideoUnitRenderInfo(0, 0, 100, 100)
        //options.aspect_mode = MobileRTCVideoUnitAspectMode.VIDEO_ASPECT_PAN_AND_SCAN;
        mDefaultVideoViewMgr!!.addActiveVideoUnit(renderInfo)
        videoListLayout!!.visibility = View.VISIBLE
        updateAttendeeVideos(mInMeetingService!!.inMeetingUserList, 0)
    }

    private fun showSharingViewOutLayout() {
        mAdapter!!.setUserList(null)
        mAdapter?.notifyDataSetChanged()
        videoListLayout!!.visibility = View.GONE
        mMeetingVideoView!!.visibility = View.GONE
        mShareView!!.visibility = View.VISIBLE
    }


    private fun updateAttendeeVideos(userlist: List<Long>, action: Int) {
        when (action) {
            0 -> {
                mAdapter!!.setUserList(userlist)
                mAdapter!!.notifyDataSetChanged()
            }
            1 -> {
                mAdapter!!.addUserList(userlist)
            }
            else -> {
                val userId = mAdapter!!.selectedUserId
                if (userlist.contains(userId)) {
                    val inmeetingUserList = mInMeetingService!!.inMeetingUserList
                    if (inmeetingUserList.size > 0) {
                        mDefaultVideoViewMgr!!.removeAllVideoUnits()
                        val renderInfo = MobileRTCVideoUnitRenderInfo(0, 0, 100, 100)
                        mDefaultVideoViewMgr!!.addAttendeeVideoUnit(
                            inmeetingUserList[0],
                            renderInfo
                        )
                    }
                }
                mAdapter?.removeUserList(userlist)
            }
        }
    }

    private fun showViewShareLayout() {
        if (!isMySelfWebinarAttendee()) {
            mDefaultVideoView!!.visibility = View.VISIBLE
            mDefaultVideoView!!.setOnClickListener(null)
            mDefaultVideoView!!.setGestureDetectorEnabled(true)
            val shareUserId = mInMeetingService!!.activeShareUserID()
            val renderInfo1 = MobileRTCRenderInfo(0, 0, 100, 100)
            mDefaultVideoViewMgr!!.addShareVideoUnit(shareUserId, renderInfo1)
            updateAttendeeVideos(mInMeetingService!!.inMeetingUserList, 0)
            customShareView!!.setMobileRTCVideoView(mDefaultVideoView)
            remoteControlHelper!!.refreshRemoteControlStatus()
        } else {
            mDefaultVideoView!!.visibility = View.VISIBLE
            mDefaultVideoView!!.setOnClickListener(null)
            mDefaultVideoView!!.setGestureDetectorEnabled(true)
            val shareUserId = mInMeetingService!!.activeShareUserID()
            val renderInfo1 = MobileRTCRenderInfo(0, 0, 100, 100)
            mDefaultVideoViewMgr!!.addShareVideoUnit(shareUserId, renderInfo1)
        }
        mAdapter?.setUserList(null)
        mAdapter?.notifyDataSetChanged()
        videoListLayout!!.visibility = View.INVISIBLE
    }

    private fun isMySelfWebinarAttendee(): Boolean {
        val myUserInfo = mInMeetingService!!.myUserInfo
        return if (myUserInfo != null && mInMeetingService!!.isWebinarMeeting) {
            myUserInfo.inMeetingUserRole == InMeetingUserInfo.InMeetingUserRole.USERROLE_ATTENDEE
        } else false
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        super.onConfigurationChanged(newConfig)
        meetingVideoHelper!!.checkVideoRotation(this)
        updateVideoListMargin(!meetingOptionBar!!.isShowing)
    }


    override fun onResume() {
        super.onResume()
        if (mMeetingService == null || mInMeetingService == null) {
            return
        }
        MeetingWindowHelper.getInstance().hiddenMeetingWindow(false)
        checkShowVideoLayout(false)
        meetingVideoHelper!!.checkVideoRotation(this)
        mDefaultVideoView!!.onResume()
    }

    override fun onPause() {
        super.onPause()
        if (mMeetingService == null || mInMeetingService == null) {
            return
        }
        mDefaultVideoView!!.onPause()
    }

    override fun onStop() {
        super.onStop()
        if (mMeetingService == null || mInMeetingService == null) {
            return
        }
        clearSubscribe()
    }

    private fun clearSubscribe() {
        if (null != mDefaultVideoViewMgr) {
            mDefaultVideoViewMgr!!.removeActiveVideoUnit()
        }
        if (null != mInMeetingService) {
            val userList = mInMeetingService!!.inMeetingUserList
            if (null != userList) {
                mAdapter!!.removeUserList(userList)
            }
        }
        currentLayoutType = -1
    }

    override fun onDestroy() {
        super.onDestroy()
        if (null != remoteControlHelper) {
            remoteControlHelper!!.onDestroy()
        }
        meetingOptionBar?.stopServer()
        unRegisterListener()
    }

    var callBack: MeetingOptionBarCallBack = object : MeetingOptionBarCallBack {
        override fun onClickBack() {
            onClickMiniWindow()
        }

        override fun onClickSwitchCamera() {
            meetingVideoHelper!!.switchCamera()
        }

        override fun onClickLeave() {
            showLeaveMeetingDialog()
        }

        override fun onClickAudio() {
            refreshToolbar()
            meetingAudioHelper!!.switchAudio()
            val audioController = ZoomSDK.getInstance().inMeetingService.inMeetingAudioController
            if (audioController.isAudioConnected) {
                if (audioController.isMyAudioMuted) {
                    Log.d(TAG, "onClickAudio: MUTE")
                } else {
                    Log.d(TAG, "onClickAudio: UNMUTE")
                }
            }
        }

        override fun onClickVideo() {
            meetingVideoHelper!!.switchVideo()
        }

        override fun onClickShare() {
            meetingShareHelper!!.onClickShare()
        }

        override fun onClickChats() {
            mInMeetingService!!.showZoomChatUI(this@MyMeetingActivity, REQUEST_CHAT_CODE)
        }

        override fun onClickPlist() {
            mInMeetingService!!.showZoomParticipantsUI(this@MyMeetingActivity, REQUEST_PLIST)
        }

        override fun onClickDisconnectAudio() {
            meetingAudioHelper!!.disconnectAudio()
        }

        override fun onClickSwitchLoudSpeaker() {
            meetingAudioHelper!!.switchLoudSpeaker()
        }

        override fun onClickAdminBo() {
            val intent = Intent(this@MyMeetingActivity, BreakoutRoomsAdminActivity::class.java)
            startActivity(intent)
        }

        override fun onClickLowerAllHands() {
            if (mInMeetingService!!.lowerAllHands() == MobileRTCSDKError.SDKERR_SUCCESS) Toast.makeText(
                this@MyMeetingActivity,
                "Lower all hands successfully",
                Toast.LENGTH_SHORT
            ).show()
        }

        override fun onClickReclaimHost() {
            if (mInMeetingService!!.reclaimHost() == MobileRTCSDKError.SDKERR_SUCCESS) Toast.makeText(
                this@MyMeetingActivity,
                "Reclaim host successfully",
                Toast.LENGTH_SHORT
            ).show()
        }

        override fun showMoreMenu(popupWindow: PopupWindow?) {
            popupWindow!!.showAtLocation(
                meetingOptionBar!!.parent as View,
                Gravity.BOTTOM or Gravity.RIGHT,
                0,
                150
            )
        }

        override fun onHidden(hidden: Boolean) {
            updateVideoListMargin(hidden)
        }
    }


    private fun onClickMiniWindow() {
        if (mMeetingService!!.meetingStatus == MeetingStatus.MEETING_STATUS_INMEETING) {
            //stop share
            if (currentLayoutType == LAYOUT_TYPE_VIEW_SHARE) {
                mDefaultVideoViewMgr!!.removeShareVideoUnit()
                currentLayoutType = -1
            }
            val userList = ZoomSDK.getInstance().inMeetingService.inMeetingUserList
            if (null == userList || userList.size < 2) {
                showLeaveMeetingDialog()
                return
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + this.packageName)
                )
                startActivityForResult(intent, REQUEST_SYSTEM_ALERT_WINDOW_FOR_MINIWINDOW)
            } else {
                showMainActivity()
            }
        } else {
            showLeaveMeetingDialog()
        }
    }

    override fun onBackPressed() {
        showLeaveMeetingDialog()
    }

    private fun updateVideoListMargin(hidden: Boolean) {
        val params = videoListLayout!!.layoutParams as RelativeLayout.LayoutParams
        params.bottomMargin = if (hidden) 0 else meetingOptionBar!!.bottomBarHeight
        if (Configuration.ORIENTATION_LANDSCAPE == resources.configuration.orientation) {
            params.bottomMargin = 0
        }
        videoListLayout!!.layoutParams = params
        videoListLayout!!.bringToFront()
    }


    private fun showMainActivity() {
        val intent = Intent(this, JoinActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        startActivity(intent)
        clearSubscribe()
    }

    var builder: Dialog? = null

    private fun showPsswordDialog(
        needPassword: Boolean,
        needDisplayName: Boolean,
        handler: InMeetingEventHandler
    ) {
        if (null != builder) {
            builder!!.dismiss()
        }
        builder = Dialog(this, R.style.ZMDialog)
        builder!!.setTitle("Need password or displayName")
        builder?.setContentView(R.layout.layout_input_password_name)
        val pwd = builder!!.findViewById<EditText>(R.id.edit_pwd)
        val name = builder!!.findViewById<EditText>(R.id.edit_name)
        builder!!.findViewById<View>(R.id.layout_pwd).visibility =
            if (needPassword) View.VISIBLE else View.GONE
        builder!!.findViewById<View>(R.id.layout_name).visibility =
            if (needDisplayName) View.VISIBLE else View.GONE
        builder!!.findViewById<View>(R.id.btn_leave).setOnClickListener { view: View? ->
            builder!!.dismiss()
            mInMeetingService!!.leaveCurrentMeeting(true)
        }
        builder!!.findViewById<View>(R.id.btn_ok).setOnClickListener { view: View? ->
            val password = pwd.text.toString()
            val userName = name.text.toString()
            if (needPassword && TextUtils.isEmpty(password) || needDisplayName && TextUtils.isEmpty(
                    userName
                )
            ) {
                builder!!.dismiss()
                onMeetingNeedPasswordOrDisplayName(needPassword, needDisplayName, handler)
                return@setOnClickListener
            }
            builder!!.dismiss()
            handler.setMeetingNamePassword(password, userName)
        }
        builder!!.setCancelable(false)
        builder!!.setCanceledOnTouchOutside(false)
        builder!!.show()
        pwd.requestFocus()
    }


    private fun updateVideoView(userList: List<Long>, action: Int) {
        if (currentLayoutType == LAYOUT_TYPE_LIST_VIDEO || currentLayoutType == LAYOUT_TYPE_VIEW_SHARE) {
            if (mVideoListView!!.visibility == View.VISIBLE) {
                updateAttendeeVideos(userList, action)
            }
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_SHARE_SCREEN_PERMISSION -> {
                if (resultCode != RESULT_OK) {
                    Log.d(TAG, "onActivityResult REQUEST_SHARE_SCREEN_PERMISSION no ok ")
                }
                startShareScreen(data)
            }
            REQUEST_SYSTEM_ALERT_WINDOW -> meetingShareHelper!!.startShareScreenSession(
                mScreenInfoData
            )
            REQUEST_SYSTEM_ALERT_WINDOW_FOR_MINIWINDOW -> {
                if (resultCode == RESULT_OK) {
                    showMainActivity()
                } else {
                    showLeaveMeetingDialog()
                }
            }
            MeetingShareHelper.REQUEST_CODE_OPEN_FILE_EXPLORER -> meetingShareHelper!!.onActivityResult(
                requestCode,
                resultCode,
                data
            )
        }
    }


    var finished = false

    override fun finish() {
        if (!finished) {
            showMainActivity()
        }
        finished = true
        super.finish()
    }

    private fun showLeaveMeetingDialog() {
        val builder = AlertDialog.Builder(this)
        if (mInMeetingService!!.isMeetingConnected) {
            if (mInMeetingService!!.isMeetingHost) {
                builder.setTitle("End or leave meeting")
                    .setPositiveButton(
                        "End"
                    ) { dialog: DialogInterface?, which: Int -> leave(true) }.setNeutralButton(
                        "Leave"
                    ) { dialog: DialogInterface?, which: Int ->
                        leave(
                            false
                        )
                    }
            } else {
                builder.setTitle("Leave meeting")
                    .setPositiveButton(
                        "Leave"
                    ) { dialog: DialogInterface?, which: Int ->
                        leave(
                            false
                        )
                    }
            }
        } else {
            builder.setTitle("Leave meeting")
                .setPositiveButton(
                    "Leave"
                ) { dialog: DialogInterface?, which: Int -> leave(false) }
        }
        if (mInMeetingService?.inMeetingBOController?.isInBOMeeting == true) {
            builder.setNegativeButton(
                "Leave BO"
            ) { _: DialogInterface?, _: Int -> leaveBo() }
        } else {
            builder.setNegativeButton("Cancel", null)
        }
        builder.create().show()
    }

    private fun leave(end: Boolean) {
        if (meetingShareHelper!!.isSharingOut) {
            meetingShareHelper!!.stopShare()
        }
        finish()
        mInMeetingService!!.leaveCurrentMeeting(end)
    }

    private fun leaveBo() {
        val boController = mInMeetingService!!.inMeetingBOController
        val iboAssistant = boController.boAssistantHelper
        if (iboAssistant != null) {
            iboAssistant.leaveBO()
        } else {
            val boAttendee = boController.boAttendeeHelper
            boAttendee?.leaveBo() ?: leave(false)
        }
    }

    private fun showJoinFailDialog(error: Int) {
        val dialog = AlertDialog.Builder(this)
            .setCancelable(false)
            .setTitle("Meeting Fail")
            .setMessage("Error:$error")
            .setPositiveButton(
                "Ok"
            ) { dialog1: DialogInterface?, which: Int -> finish() }.create()
        dialog.show()
    }

    private fun showWebinarNeedRegisterDialog(inMeetingEventHandler: InMeetingEventHandler?) {
        val dialog = AlertDialog.Builder(this)
            .setCancelable(false)
            .setTitle("Need register to join this webinar meeting ")
            .setNegativeButton(
                "Cancel"
            ) { dialog1: DialogInterface?, which: Int ->
                mInMeetingService!!.leaveCurrentMeeting(
                    true
                )
            }
            .setPositiveButton(
                "Ok"
            ) { dialog12: DialogInterface?, which: Int ->
                if (null != inMeetingEventHandler) {
                    val time = System.currentTimeMillis()
                    inMeetingEventHandler.setRegisterWebinarInfo(
                        "test",
                        "$time@example.com",
                        false
                    )
                }
            }.create()
        dialog.show()
    }

    private fun showEndOtherMeetingDialog(handler: InMeetingEventHandler) {
        val dialog = AlertDialog.Builder(this)
            .setCancelable(false)
            .setTitle("Meeting Alert")
            .setMessage("You have a meeting that is currently in-progress. Please end it to start a new meeting.")
            .setPositiveButton(
                "End Other Meeting"
            ) { dialog1: DialogInterface?, which: Int -> handler.endOtherMeeting() }
            .setNeutralButton(
                "Leave"
            ) { dialog, which ->
                finish()
                mInMeetingService!!.leaveCurrentMeeting(true)
            }.create()
        dialog.show()
    }

    @SuppressLint("NewApi")
    protected fun startShareScreen(data: Intent?) {
        if (data == null) {
            return
        }
        if (Build.VERSION.SDK_INT >= 24 && !Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            mScreenInfoData = data
            startActivityForResult(intent, REQUEST_SYSTEM_ALERT_WINDOW)
        } else {
            meetingShareHelper!!.startShareScreenSession(data)
        }
    }

    override fun checkSelfPermission(permission: String): Int {
        return if (permission.isEmpty()) {
            PackageManager.PERMISSION_DENIED
        } else try {
            checkPermission(permission, Process.myPid(), Process.myUid())
        } catch (e: Throwable) {
            PackageManager.PERMISSION_DENIED
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        for (i in permissions.indices) {
            if (Manifest.permission.RECORD_AUDIO == permissions[i]) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    meetingAudioHelper!!.switchAudio()
                }
            } else if (Manifest.permission.CAMERA == permissions[i]) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    checkShowVideoLayout(false)
                }
            } else if (Manifest.permission.READ_EXTERNAL_STORAGE == permissions[i]) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    meetingShareHelper!!.openFileExplorer()
                }
            }
        }
    }


    override fun onUserAudioStatusChanged(userId: Long) {
        meetingAudioHelper!!.onUserAudioStatusChanged(userId)
    }

    override fun onUserAudioTypeChanged(userId: Long) {
        meetingAudioHelper!!.onUserAudioTypeChanged(userId)
    }

    override fun onMyAudioSourceTypeChanged(type: Int) {
        meetingAudioHelper!!.onMyAudioSourceTypeChanged(type)
    }

    override fun onUserVideoStatusChanged(userId: Long) {
        meetingOptionBar!!.updateVideoButton()
        meetingOptionBar!!.updateSwitchCameraButton()
    }

    override fun onShareActiveUser(userId: Long) {
        meetingShareHelper!!.onShareActiveUser(mCurShareUserId, userId)
        mCurShareUserId = userId
        meetingOptionBar!!.updateShareButton()
        checkShowVideoLayout(true)
    }

    override fun onSilentModeChanged(inSilentMode: Boolean) {
        if (inSilentMode) meetingShareHelper!!.stopShare()
    }

    override fun onShareUserReceivingStatus(userId: Long) {}

    override fun onShareSettingTypeChanged(type: ShareSettingType?) {}

    override fun onMeetingUserJoin(userList: List<Long>) {
        checkShowVideoLayout(true)
        updateVideoView(userList, 1)
    }

    override fun onMeetingUserLeave(userList: List<Long>) {
        checkShowVideoLayout(true)
        updateVideoView(userList, 2)
    }

    override fun onWebinarNeedRegister(registerUrl: String?) {}

    override fun onMeetingFail(errorCode: Int, internalErrorCode: Int) {
        mMeetingFailed = true
        mMeetingVideoView!!.visibility = View.GONE
        mConnectingText!!.visibility = View.GONE
        showJoinFailDialog(errorCode)
    }

    override fun onMeetingLeaveComplete(ret: Long) {
        meetingShareHelper!!.stopShare()
        if (!mMeetingFailed) finish()
    }

    override fun onMeetingStatusChanged(
        meetingStatus: MeetingStatus?,
        errorCode: Int,
        internalErrorCode: Int
    ) {
        checkShowVideoLayout(true)
        refreshToolbar()
    }

    override fun onMeetingNeedPasswordOrDisplayName(
        needPassword: Boolean,
        needDisplayName: Boolean,
        handler: InMeetingEventHandler
    ) {
        showPsswordDialog(needPassword, needDisplayName, handler)
    }

    override fun onMeetingNeedColseOtherMeeting(inMeetingEventHandler: InMeetingEventHandler) {
        showEndOtherMeetingDialog(inMeetingEventHandler)
    }

    override fun onJoinWebinarNeedUserNameAndEmail(inMeetingEventHandler: InMeetingEventHandler?) {
        showWebinarNeedRegisterDialog(inMeetingEventHandler)
    }

    override fun onFreeMeetingReminder(
        isOrignalHost: Boolean,
        canUpgrade: Boolean,
        isFirstGift: Boolean
    ) {
        Log.d(TAG, "onFreeMeetingReminder:$isOrignalHost $canUpgrade $isFirstGift")
    }

    override fun onNeedRealNameAuthMeetingNotification(
        supportCountryList: List<ZoomSDKCountryCode?>?,
        privacyUrl: String,
        handler: IZoomRetrieveSMSVerificationCodeHandler?
    ) {
        Log.d(TAG, "onNeedRealNameAuthMeetingNotification:$privacyUrl")
        Log.d(
            TAG,
            "onNeedRealNameAuthMeetingNotification getRealNameAuthPrivacyURL:" + ZoomSDK.getInstance().smsService.realNameAuthPrivacyURL
        )
        RealNameAuthDialog.show(this, handler)
    }

    override fun onRetrieveSMSVerificationCodeResultNotification(
        result: MobileRTCSMSVerificationError,
        handler: IZoomVerifySMSVerificationCodeHandler?
    ) {
        Log.d(TAG, "onRetrieveSMSVerificationCodeResultNotification:$result")
    }

    override fun onVerifySMSVerificationCodeResultNotification(result: MobileRTCSMSVerificationError) {
        Log.d(TAG, "onVerifySMSVerificationCodeResultNotification:$result")
    }

    override fun onHelpRequestReceived(strUserID: String?) {
        val boController = mInMeetingService!!.inMeetingBOController
        val iboAdmin = boController.boAdminHelper
        if (iboAdmin != null) {
            val boAndUser = UIUtil.getBoNameUserNameByUserId(boController, strUserID)
            if (boAndUser.size != 2) return
            AlertDialog.Builder(this)
                .setMessage(boAndUser[1].toString() + " in " + boAndUser[0] + " asked for help.")
                .setCancelable(false)
                .setNegativeButton(
                    "Later"
                ) { dialog: DialogInterface?, which: Int ->
                    iboAdmin.ignoreUserHelpRequest(
                        strUserID
                    )
                }
                .setPositiveButton(
                    "Join Breakout Room"
                ) { dialog: DialogInterface?, which: Int ->
                    iboAdmin.joinBOByUserRequest(
                        strUserID
                    )
                }.create().show()
        }
    }

    override fun onStartBOError(error: BOControllerError) {
        Log.d(TAG, "onStartBOError:$error")
    }

    override fun onBOEndTimerUpdated(remaining: Int, isTimesUpNotice: Boolean) {
        Log.d(
            TAG,
            "onBOEndTimerUpdated: remaining: $remaining,isTimesUpNotice: $isTimesUpNotice"
        )
    }

    private fun unRegisterListener() {
        try {
            MeetingAudioCallback.getInstance().removeListener(this)
            MeetingVideoCallback.getInstance().removeListener(this)
            MeetingShareCallback.getInstance().removeListener(this)
            MeetingUserCallback.getInstance().removeListener(this)
            MeetingCommonCallback.getInstance().removeListener(this)
            BOEventCallback.getInstance().removeEvent(this)
            if (null != smsService) {
                smsService!!.removeListener(this)
            }
            ZoomSDK.getInstance().inMeetingService.inMeetingBOController.removeListener(
                mBOControllerListener
            )
            ZoomSDK.getInstance().inMeetingService.removeListener(mInMeetingServiceListener)
            ZoomSDK.getInstance().inMeetingService.inMeetingLiveTranscriptionController.removeListener(
                mLiveTranscriptionListener
            )
        } catch (e: Exception) {
            Log.e(TAG, e.message!!)
        }
    }


    private fun registerListener() {
        smsService = ZoomSDK.getInstance().smsService
        if (null != smsService) {
            smsService!!.addListener(this)
        }
        ZoomSDK.getInstance().inMeetingService.inMeetingBOController.addListener(
            mBOControllerListener
        )
        MeetingAudioCallback.getInstance().addListener(this)
        MeetingVideoCallback.getInstance().addListener(this)
        MeetingShareCallback.getInstance().addListener(this)
        MeetingUserCallback.getInstance().addListener(this)
        MeetingCommonCallback.getInstance().addListener(this)
        val meetingInterpretationController =
            ZoomSDK.getInstance().inMeetingService.inMeetingInterpretationController
        meetingInterpretationController.setEvent(event)
        mInMeetingServiceListener = object : SimpleInMeetingListener() {
            override fun onRecordingStatus(status: RecordingStatus) {
                if (status == RecordingStatus.Recording_Start) {
                    LegalNoticeDialogUtil.showChatLegalNoticeDialog(this@MyMeetingActivity)
                }
            }

            override fun onLocalRecordingStatus(status: RecordingStatus) {
                if (status == RecordingStatus.Recording_Start) {
                    LegalNoticeDialogUtil.showChatLegalNoticeDialog(this@MyMeetingActivity)
                }
            }

            override fun onInvalidReclaimHostkey() {}
        }
        ZoomSDK.getInstance().inMeetingService.addListener(mInMeetingServiceListener)
        ZoomSDK.getInstance().inMeetingService.inMeetingLiveTranscriptionController.addListener(
            mLiveTranscriptionListener
        )
    }

    private val mBOControllerListener: SimpleInMeetingBOControllerListener =
        object : SimpleInMeetingBOControllerListener() {
            var dialog: AlertDialog? = null
            override fun onHasAttendeeRightsNotification(iboAttendee: IBOAttendee) {
                super.onHasAttendeeRightsNotification(iboAttendee)
                Log.d(TAG, "onHasAttendeeRightsNotification")
                iboAttendee.setEvent(iboAttendeeEvent)
                val boController = mInMeetingService!!.inMeetingBOController
                if (boController.isInBOMeeting) {
                    mBtnJoinBo!!.visibility = View.GONE
                    mBtnRequestHelp?.visibility =
                        if (iboAttendee.isHostInThisBO) View.GONE else View.VISIBLE
                    meetingOptionBar!!.updateMeetingNumber(iboAttendee.boName)
                } else {
                    mBtnRequestHelp?.visibility = View.GONE
                    val builder = AlertDialog.Builder(this@MyMeetingActivity)
                        .setMessage("The host is inviting you to join Breakout Room: " + iboAttendee.boName)
                        .setNegativeButton(
                            "Later"
                        ) { dialog: DialogInterface?, _: Int ->
                            mBtnJoinBo!!.visibility = View.VISIBLE
                        }
                        .setPositiveButton(
                            "Join"
                        ) { _: DialogInterface?, _: Int -> iboAttendee.joinBo() }
                        .setCancelable(false)
                    dialog = builder.create()
                    dialog?.show()
                }
            }

            override fun onHasDataHelperRightsNotification(iboData: IBOData) {
                Log.d(TAG, "onHasDataHelperRightsNotification")
                iboData.setEvent(iboDataEvent)
            }

            override fun onLostAttendeeRightsNotification() {
                super.onLostAttendeeRightsNotification()
                Log.d(TAG, "onLostAttendeeRightsNotification")
                if (null != dialog && dialog!!.isShowing) {
                    dialog!!.dismiss()
                }
                mBtnJoinBo!!.visibility = View.GONE
            }

            override fun onHasAdminRightsNotification(iboAdmin: IBOAdmin) {
                super.onHasAdminRightsNotification(iboAdmin)
                Log.d(TAG, "onHasAdminRightsNotification")
                BOEventCallback.getInstance().addEvent(this@MyMeetingActivity)
            }
        }

    private val iboDataEvent: IBODataEvent = object : IBODataEvent {
        override fun onBOInfoUpdated(strBOID: String) {
            val boController = mInMeetingService!!.inMeetingBOController
            val iboData = boController.boDataHelper
            if (iboData != null) {
                val boName = iboData.currentBoName
                if (!TextUtils.isEmpty(boName)) {
                    meetingOptionBar!!.updateMeetingNumber(boName)
                }
            }
        }

        override fun onUnAssignedUserUpdated() {}
    }

    private val iboAttendeeEvent: IBOAttendeeEvent = object : IBOAttendeeEvent {
        override fun onHelpRequestHandleResultReceived(eResult: ATTENDEE_REQUEST_FOR_HELP_RESULT) {
            if (eResult == ATTENDEE_REQUEST_FOR_HELP_RESULT.RESULT_IGNORE) {
                AlertDialog.Builder(this@MyMeetingActivity)
                    .setMessage("The host is currently helping others. Please try again later.")
                    .setCancelable(false)
                    .setPositiveButton(
                        "OK"
                    ) { _, _ -> }.create().show()
            }
        }

        override fun onHostJoinedThisBOMeeting() {
            mBtnRequestHelp?.visibility = View.GONE
        }

        override fun onHostLeaveThisBOMeeting() {
            mBtnRequestHelp?.visibility = View.VISIBLE
        }
    }

    private fun attendeeRequestHelp() {
        val boController = mInMeetingService!!.inMeetingBOController
        val boAttendee = boController.boAttendeeHelper
        if (boAttendee != null) {
            AlertDialog.Builder(this)
                .setMessage("You can invite the host to this Breakout Room for assistance.")
                .setCancelable(false)
                .setNegativeButton(
                    "Cancel"
                ) { dialog: DialogInterface?, which: Int -> }
                .setPositiveButton(
                    "Ask for Help"
                ) { dialog: DialogInterface?, which: Int -> boAttendee.requestForHelp() }.create()
                .show()
        }
    }


    private val event: IMeetingInterpretationControllerEvent =
        object : IMeetingInterpretationControllerEvent {
            override fun onInterpretationStart() {
                Log.d(TAG, "onInterpretationStart:")
                updateLanguage()
            }

            override fun onInterpretationStop() {
                Log.d(TAG, "onInterpretationStop:")
                updateLanguage()
            }

            override fun onInterpreterListChanged() {
                Log.d(TAG, "onInterpreterListChanged:")
            }

            override fun onInterpreterRoleChanged(userID: Int, isInterpreter: Boolean) {
                Log.d(TAG, "onInterpreterRoleChanged:$userID:$isInterpreter")
                val isMyself = ZoomSDK.getInstance().inMeetingService.isMyself(userID.toLong())
                if (isMyself) {
                    if (isInterpreter) {
                        Toast.makeText(
                            baseContext,
                            R.string.zm_msg_interpreter_88102,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    updateLanguage()
                }
            }

            private fun updateLanguage() {
                val controller =
                    ZoomSDK.getInstance().inMeetingService.inMeetingInterpretationController
                if (controller.isInterpretationEnabled && controller.isInterpretationStarted && controller.isInterpreter) {
                    layout_lans!!.visibility = View.VISIBLE
                } else {
                    layout_lans!!.visibility = View.GONE
                    return
                }
                val button1 = layout_lans!!.findViewById<TextView>(R.id.btn_lan1)
                val button2 = layout_lans!!.findViewById<TextView>(R.id.btn_lan2)
                val list = controller.interpreterLans
                val lanId = controller.interpreterActiveLan
                if (null != list && list.size >= 2) {
                    val language1 = controller.getInterpretationLanguageByID(list[0])
                    val language2 = controller.getInterpretationLanguageByID(list[1])
                    if (null != language1) {
                        button1.text = language1.languageName
                    }
                    if (null != language2) {
                        button2.text = language2.languageName
                    }
                    if (lanId == list[0]) {
                        button1.isSelected = true
                        button2.isSelected = false
                    } else if (lanId == list[1]) {
                        button2.isSelected = true
                        button1.isSelected = false
                    } else {
                        button2.isSelected = false
                        button1.isSelected = false
                    }
                }
                button1.setOnClickListener { v: View? ->
                    val lans = controller.interpreterLans
                    if (null != lans && lans.size >= 2) {
                        controller.interpreterActiveLan = lans[0]
                    }
                    button2.isSelected = false
                    button1.isSelected = true
                }
                button2.setOnClickListener { v: View? ->
                    val lans = controller.interpreterLans
                    if (null != lans && lans.size >= 2) {
                        controller.interpreterActiveLan = lans[1]
                    }
                    button1.isSelected = false
                    button2.isSelected = true
                }
            }

            override fun onInterpreterActiveLanguageChanged(userID: Int, activeLanID: Int) {
                Log.d(TAG, "onInterpreterActiveLanguageChanged:$userID:$activeLanID")
                updateLanguage()
            }

            override fun onInterpreterLanguageChanged(lanID1: Int, lanID2: Int) {
                Log.d(TAG, "onInterpreterLanguageChanged:$lanID1:$lanID2")
                updateLanguage()
            }

            override fun onAvailableLanguageListUpdated(pAvailableLanguageList: List<IInterpretationLanguage>) {
                Log.d(TAG, "onAvailableLanguageListUpdated:$pAvailableLanguageList")
                updateLanguage()
            }
        }

    private val mLiveTranscriptionListener: InMeetingLiveTranscriptionListener =
        object : InMeetingLiveTranscriptionListener {
            override fun onLiveTranscriptionStatus(status: MobileRTCLiveTranscriptionStatus) {
                Log.d(TAG, "onLiveTranscriptionStatus: $status")
            }

            override fun onLiveTranscriptionMsgReceived(
                msg: String,
                type: MobileRTCLiveTranscriptionOperationType
            ) {
                Log.d(TAG, "onLiveTranscriptionMsgReceived: $msg, operation type: $type")
            }

            override fun onRequestForLiveTranscriptReceived(
                requesterUserId: Long,
                bAnonymous: Boolean
            ) {
                Log.d(
                    TAG,
                    "onRequestForLiveTranscriptReceived from: $requesterUserId, bAnonymous: $bAnonymous"
                )
                var userName: String? = null
                if (!bAnonymous) {
                    val userInfo = mInMeetingService!!.getUserInfoById(requesterUserId)
                    userName = userInfo.userName
                }
                LiveTranscriptionRequestHandleDialog.show(this@MyMeetingActivity, userName)
            }

            override fun onRequestLiveTranscriptionStatusChange(enabled: Boolean) {
                Log.d(TAG, "onRequestLiveTranscriptionStatusChange: $enabled")
            }
        }
}