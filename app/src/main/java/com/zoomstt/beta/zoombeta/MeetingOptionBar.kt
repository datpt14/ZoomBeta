package com.zoomstt.beta.zoombeta

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.text.TextUtils
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.ColorRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.beust.klaxon.Klaxon
import com.microsoft.signalr.HubConnection
import com.microsoft.signalr.HubConnectionBuilder
import com.microsoft.signalr.HubConnectionState
import com.zoomstt.beta.BuildConfig
import com.zoomstt.beta.R
import com.zoomstt.beta.TemporaryDataHelper
import com.zoomstt.beta.data.model.ContentSpeechItem
import com.zoomstt.beta.data.model.SignaIRMessage
import com.zoomstt.beta.data.model.SignaIRTranslateModel
import com.zoomstt.beta.data.model.SignaIRTranslateResponse
import com.zoomstt.beta.ui.adapter.ColorFriendAdapter
import com.zoomstt.beta.ui.adapter.ColorMeAdapter
import com.zoomstt.beta.ui.adapter.ContentChatAdapter
import com.zoomstt.beta.utils.gone
import com.zoomstt.beta.utils.visible
import com.zoomstt.beta.utils.visibleWhenTrue
import com.zoomstt.beta.zoombeta.callapi.RetrofitInstance
import com.zoomstt.beta.zoombeta.callapi.RetrofitInterface
import com.zoomstt.beta.zoombeta.customer.adapter.SimpleMenuAdapter
import com.zoomstt.beta.zoombeta.customer.adapter.SimpleMenuItem
import com.zoomstt.beta.zoombeta.customer.rawdata.VirtualVideoSource
import com.zoomstt.beta.zoombeta.customer.rawdata.WaterMarkData
import com.zoomstt.beta.zoombeta.customer.rawdata.YUVConvert
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import us.zoom.sdk.*
import us.zoom.sdk.InMeetingUserInfo.InMeetingUserRole
import kotlin.math.roundToInt

enum class ColorType {
    COLOR_ME, COLOR_FRIEND
}

enum class ColorResource(@param:ColorRes val colorRes: Int) {
    Color1(R.color.color1),
    Color2(R.color.color2),
    Color3(R.color.color3),
    Color4(R.color.color4),
    Color5(R.color.color5),
    Color6(R.color.color6),
    Color7(R.color.color7),
    Color8(R.color.color8);

    @SuppressLint("NewApi")
    fun color(context: Context) = context.getColor(colorRes)

    companion object {
        fun list() = listOf(Color1, Color2, Color3, Color4, Color5, Color6, Color7, Color8)
    }
}

val listColor = arrayListOf(
    R.color.color1,
    R.color.color2,
    R.color.color3,
    R.color.color4,
    R.color.color5,
    R.color.color6,
    R.color.color7,
    R.color.color8
)

class MeetingOptionBar : FrameLayout, View.OnClickListener {
    private val MENU_DISCONNECT_AUDIO: Int = 0
    private val MENU_SHOW_PLIST: Int = 4

    //webinar host&cohost
    private val MENU_AllOW_PANELIST_START_VIDEO: Int = 5
    private val MENU_AllOW_ATTENDEE_CHAT: Int = 6
    private val MENU_DISALLOW_PANELIST_START_VIDEO: Int = 7
    private val MENU_DISALLOW_ATTENDEE_CHAT: Int = 8
    private val MENU_SPEAKER_ON: Int = 9
    private val MENU_SPEAKER_OFF: Int = 10
    private val MENU_ANNOTATION_OFF: Int = 11
    private val MENU_ANNOTATION_ON: Int = 12
    private val MENU_ANNOTATION_QA: Int = 13
    private val MENU_SWITCH_DOMAIN: Int = 14
    private val MENU_CREATE_BO: Int = 15
    private val MENU_LOWER_ALL_HANDS: Int = 16
    private val MENU_RECLAIM_HOST: Int = 17
    private val MENU_VIRTUAL_SOURCE: Int = 18
    private val MENU_INTERNAL_SOURCE: Int = 19
    private val MENU_INTERPRETATION: Int = 20
    private val MENU_INTERPRETATION_ADMIN: Int = 21
    private val MENU_LIVE_TRANSCRIPTION_REQUEST: Int = 22
    private val MENU_LIVE_TRANSCRIPTION_STOP: Int = 23
    var mCallBack: MeetingOptionBarCallBack? = null

    // SignaIR
    private var connection: HubConnection? = null
    private val url = "https://kedu-relay.jeyunvn.com/voice"
    private var meetingID = ""
    private var userName = ""

    var mContentView: View? = null
    var mBottomBar: View? = null
    var mTopBar: View? = null
    var mContainerMeetingContent: View? = null
    var mContainerMeetingSetting: View? = null
    var mGroupContent: ConstraintLayout? = null
    private var rcvListContent: RecyclerView? = null
    private var ivContentDetail: ImageView? = null
    private var ivContentTranslate: ImageView? = null
    private var ivContentSetting: ImageView? = null
    private var ivContentDownload: ImageView? = null

    private var progressBarFontSize: SeekBar? = null
    private var progressBarTransparency: SeekBar? = null
    private var tvActionCancel: TextView? = null
    private var tvActionSave: TextView? = null
    private var tvValueFontSize: TextView? = null
    private var tvValueTransparency: TextView? = null
    private var ivCloseSetting: ImageView? = null
    private var rcvColorMe: RecyclerView? = null
    private var rcvColorFriend: RecyclerView? = null

    private var colorMe: Int? = null
    private var colorFriend: Int? = null

    private var mBtnLeave: View? = null
    private var mBtnShare: View? = null
    private var mBtnCamera: View? = null
    private var mBtnAudio: View? = null
    var switchCameraView: View? = null
        private set
    private var mAudioStatusImg: ImageView? = null
    private var mVideoStatusImg: ImageView? = null
    private var mShareStatusImg: ImageView? = null
    private var mMeetingNumberText: TextView? = null
    private var mMeetingUserName: TextView? = null
    private var mMeetingAudioText: TextView? = null
    private var mMeetingVideoText: TextView? = null
    private var mMeetingShareText: TextView? = null
    private var mInMeetingService: InMeetingService? = null
    private var mInMeetingShareController: InMeetingShareController? = null
    private var mInMeetingVideoController: InMeetingVideoController? = null
    private var mInMeetingAudioController: InMeetingAudioController? = null
    private var mInMeetingWebinarController: InMeetingWebinarController? = null
    private var meetingAnnotationController: InMeetingAnnotationController? = null
    private var mInMeetingChatController: InMeetingChatController? = null
    private var contentAdapter: ContentChatAdapter? = null
    private var colorMeAdapter: ColorMeAdapter? = null
    private var colorFriendAdapter: ColorFriendAdapter? = null
    private val listContent: ArrayList<ContentSpeechItem> = ArrayList()
    private var meetingInterpretationController: InMeetingInterpretationController? = null
    private var mContext: Context? = null
    private var activity: Activity? = null

    private var textSize = 16
    private var transparency = 30

    // SignaIR
    private val listenerSeekbarFontSize = object : SeekBar.OnSeekBarChangeListener {
        var progressFontSize = 0

        override fun onProgressChanged(seekBar: SeekBar?, progressBar: Int, fromUser: Boolean) {
            progressFontSize = progressBar
            tvValueFontSize?.text = "$progressBar %"
        }

        override fun onStartTrackingTouch(seekBar: SeekBar?) {
        }

        override fun onStopTrackingTouch(seekBar: SeekBar?) {
            tvValueFontSize?.text = "$progressFontSize %"
            textSize = (40 * (progressFontSize * 0.01)).roundToInt()
            Log.d(TAG, "onStopTrackingTouch: - textSize - $textSize")
        }
    }

    private val listenerSeekbarTransparency = object : SeekBar.OnSeekBarChangeListener {
        var progressTransparency = 0

        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            progressTransparency = progress
            tvValueTransparency?.text = "$progressTransparency %"
        }

        override fun onStartTrackingTouch(seekBar: SeekBar?) {
        }

        override fun onStopTrackingTouch(seekBar: SeekBar?) {
            tvValueTransparency?.text = "$progressTransparency %"
            transparency = (255 * (progressTransparency * 0.01)).roundToInt()
            Log.d(TAG, "onStopTrackingTouch: - transparency - $transparency")
        }

    }

    interface MeetingOptionBarCallBack {
        fun onClickBack()
        fun onClickSwitchCamera()
        fun onClickLeave()
        fun onClickAudio()
        fun onClickVideo()
        fun onClickShare()
        fun onClickChats()
        fun onClickPlist()
        fun onClickDisconnectAudio()
        fun onClickSwitchLoudSpeaker()
        fun onClickAdminBo()
        fun onClickLowerAllHands()
        fun onClickReclaimHost()
        fun showMoreMenu(popupWindow: PopupWindow?)
        fun onHidden(hidden: Boolean)
    }

    constructor(context: Context?) : super((context)!!) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(context)
    }

    fun setCallBack(callBack: MeetingOptionBarCallBack?) {
        mCallBack = callBack
    }

    @SuppressLint("NewApi")
    fun init(context: Context?) {
        mContext = context
        mContentView =
            LayoutInflater.from(context).inflate(R.layout.layout_meeting_option, this, false)
        addView(mContentView)
        mInMeetingService = ZoomSDK.getInstance().inMeetingService
        mInMeetingShareController = mInMeetingService?.inMeetingShareController
        mInMeetingVideoController = mInMeetingService?.inMeetingVideoController
        mInMeetingAudioController = mInMeetingService?.inMeetingAudioController
        mInMeetingWebinarController = mInMeetingService?.inMeetingWebinarController
        meetingAnnotationController = mInMeetingService?.inMeetingAnnotationController
        mInMeetingChatController = mInMeetingService?.inMeetingChatController
        meetingInterpretationController = mInMeetingService?.inMeetingInterpretationController
        mContentView!!.setOnClickListener(this)
        mBottomBar = findViewById(R.id.bottom_bar)
        mTopBar = findViewById(R.id.top_bar)

        // init layout message
        mContainerMeetingContent = findViewById(R.id.containerMeetingContent)
        mGroupContent = findViewById(R.id.container_meeting_content)
        ivContentDetail = findViewById(R.id.ivContentDetail)
        ivContentTranslate = findViewById(R.id.ivContentTranslate)
        ivContentSetting = findViewById(R.id.ivContentSetting)
        ivContentDownload = findViewById(R.id.ivContentDownload)
        rcvListContent = findViewById(R.id.rcvListContent)
        ivContentDetail?.setOnClickListener(this)
        ivContentTranslate?.setOnClickListener(this)
        ivContentSetting?.setOnClickListener(this)
        ivContentDownload?.setOnClickListener(this)

        // init layout setting
        mContainerMeetingSetting = findViewById(R.id.containerMeetingSetting)
        rcvColorMe = findViewById(R.id.rcvColorMe)
        rcvColorFriend = findViewById(R.id.rcvColorFriend)
        ivCloseSetting = findViewById(R.id.ivCloseSetting)
        tvActionCancel = findViewById(R.id.tvActionCancel)
        tvActionSave = findViewById(R.id.tvActionSave)
        progressBarFontSize = findViewById(R.id.processBarFontSize)
        progressBarTransparency = findViewById(R.id.processBarTransparency)
        tvValueFontSize = findViewById(R.id.tvValueFontSize)
        tvValueTransparency = findViewById(R.id.tvValueTransparency)

        progressBarFontSize?.max = 100
        progressBarFontSize?.min = 0
        progressBarFontSize?.setOnSeekBarChangeListener(listenerSeekbarFontSize)

        progressBarTransparency?.max = 100
        progressBarTransparency?.min = 0
        progressBarTransparency?.setOnSeekBarChangeListener(listenerSeekbarTransparency)

        mContainerMeetingSetting?.setOnClickListener(null)
        mContainerMeetingContent?.setOnClickListener(null)
        ivCloseSetting?.setOnClickListener(this)
        tvActionSave?.setOnClickListener(this)
        tvActionCancel?.setOnClickListener(this)

        mBtnLeave = findViewById(R.id.btnLeaveZoomMeeting)
        mBtnLeave?.setOnClickListener(this)
        mBtnShare = findViewById(R.id.btnShare)
        mBtnShare?.setOnClickListener(this)
        mBtnCamera = findViewById(R.id.btnCamera)
        mBtnCamera?.setOnClickListener(this)
        mBtnAudio = findViewById(R.id.btnAudio)
        mBtnAudio?.setOnClickListener(this)
        findViewById<View>(R.id.btnChats).setOnClickListener(this)
        mAudioStatusImg = findViewById(R.id.audioStatusImage)
        mVideoStatusImg = findViewById(R.id.videotatusImage)
        mShareStatusImg = findViewById(R.id.shareStatusImage)
        mMeetingAudioText = findViewById(R.id.text_audio)
        mMeetingVideoText = findViewById(R.id.text_video)
        mMeetingShareText = findViewById(R.id.text_share)
        findViewById<View>(R.id.moreActionImg).setOnClickListener(this)
        switchCameraView = findViewById(R.id.btnSwitchCamera)
        switchCameraView?.setOnClickListener(this)
        mMeetingNumberText = findViewById(R.id.meetingNumber)
        mMeetingUserName = findViewById(R.id.txtPassword)
        findViewById<View>(R.id.btnBack).setOnClickListener(this)

        initColorAdapter()
    }

    // init SignaIR
    fun initSignaIR(activity: Activity) {
        this.activity = activity
        contentAdapter = ContentChatAdapter(context = context)
        rcvListContent?.adapter = contentAdapter

        connection = HubConnectionBuilder.create(url).build()
        HubConnectionTask().execute(connection)
    }

    // Hub connection
    @SuppressLint("StaticFieldLeak")
    inner class HubConnectionTask : AsyncTask<HubConnection?, Void?, Void?>() {
        override fun doInBackground(vararg params: HubConnection?): Void? {
            val hubConnection: HubConnection? = params[0]
            hubConnection?.start()?.blockingAwait()
            try {
                if (connection?.connectionState == HubConnectionState.CONNECTED) {
                    Log.d(TAG, "SignalR connected.")
                } else {
                    hubConnection?.start()?.blockingAwait()
                    Log.d(TAG, "SignalR currently connected.")
                }
            } catch (err: Exception) {
                Log.d(TAG, (err.message)!!)
            }
            return null
        }
    }

    // Get message from signaIR
    private fun connectServerGetMessage() {
        try {
            connection?.invoke("AddToGroup", meetingID)

            connection?.on("ReceiveMessage", { message ->
                val messageData = Klaxon().parse<SignaIRMessage>(message)

                Log.d(TAG, "ReceiveMessage: --- $messageData")

                activity?.runOnUiThread {
                    if (messageData?.oriLang != "en") {
                        Log.d(TAG, "connectServerGetMessage: ${messageData?.oriLang != "en"}")
                        val signaIRTranslateModel =
                            SignaIRTranslateModel(
                                text = messageData?.msg.toString(),
                                language = messageData?.oriLang.toString(),
                                targetLanguage = "en"
                            )
                        callAPITranslate(signaIRTranslateModel, messageData)

                    } else {
                        listContent.add(
                            ContentSpeechItem(
                                userName = messageData.userName,
                                content = messageData.msg
                            )
                        )
                        contentAdapter?.submitList(listContent)
                        contentAdapter?.itemCount?.minus(0)
                            ?.let { rcvListContent?.smoothScrollToPosition(it) }
                    }

                }
            }, String::class.java)
        } catch (error: Exception) {
            Log.e(TAG, "connectServerGetMessage: $error")
        }
    }

    // Call api translate
    private fun callAPITranslate(
        signaIRTranslateModel: SignaIRTranslateModel,
        messageData: SignaIRMessage?
    ) {
        val api = RetrofitInstance.retrofitInstance?.create(
            RetrofitInterface::class.java
        )
        val listCall = api?.translate(translateObject = signaIRTranslateModel)
        listCall?.enqueue(object : Callback<SignaIRTranslateResponse> {
            override fun onResponse(
                call: Call<SignaIRTranslateResponse>,
                response: Response<SignaIRTranslateResponse>
            ) {
                listContent.add(
                    ContentSpeechItem(
                        userName = messageData?.userName.toString(),
                        content = response.body()?.translatedText.toString()
                    )
                )
                contentAdapter?.submitList(listContent)
                contentAdapter?.itemCount?.minus(0)
                    ?.let { rcvListContent?.smoothScrollToPosition(it) }

            }

            override fun onFailure(call: Call<SignaIRTranslateResponse>, t: Throwable) {
                Log.d(TAG, "onFailure: SignaIRTranslateResponse")
            }
        })
    }

    private fun initColorAdapter() {
        colorMeAdapter = ColorMeAdapter(context,::onClickColorMeItem)
        rcvColorMe?.adapter = colorMeAdapter
        colorFriendAdapter = ColorFriendAdapter(context,::onClickColorFriendItem)
        rcvColorFriend?.adapter = colorFriendAdapter

        colorMeAdapter?.submitList(listColor)
        colorFriendAdapter?.submitList(listColor)
    }

    private fun onClickColorMeItem(colorSelected: Int) {
        colorMe = colorSelected
        contentAdapter?.changeColorText(userName = userName, colorResource = colorMe!!)
    }

    private fun onClickColorFriendItem(colorSelected: Int) {
        colorFriend = colorSelected
        contentAdapter?.changeColorText(userName = "", colorResource = colorFriend!!)
    }

    fun stopServer() {
        connection?.stop()
    }

    private fun onClickContentDetail() {

    }

    private fun onClickContentTranslate() {

    }

    private fun onClickContentSetting() {
        mContainerMeetingSetting?.visible()
    }

    private fun onClickContentDownload() {

    }

    fun showMeetingContentAndSetting(isShow: Boolean) {
        mContainerMeetingContent?.visibleWhenTrue(isShow)
    }

    var autoHidden: Runnable = Runnable { hideOrShowToolbar(true) }
    fun hideOrShowToolbar(hidden: Boolean) {
        removeCallbacks(autoHidden)
        if (hidden) {
            visibility = VISIBLE
        } else {
            postDelayed(autoHidden, 3000)
            visibility = VISIBLE
            bringToFront()
        }
        if (null != mCallBack) {
            mCallBack!!.onHidden(hidden)
        }
    }

    val bottomBarHeight: Int
        get() = mBottomBar!!.measuredHeight
    val bottomBarBottom: Int
        get() = mBottomBar!!.bottom
    val bottomBarTop: Int
        get() = mBottomBar!!.top
    val topBarHeight: Int
        get() = mTopBar!!.measuredHeight

    fun updateMeetingNumber(text: String?) {
        if (null != mMeetingNumberText) {
            mMeetingNumberText!!.text = TemporaryDataHelper.instance().schoolName
        }
    }

    fun updateUserName(textUserName: String?) {
        if (textUserName != null) {
            userName = textUserName
        }
    }

    fun updateMeetingUserName(text: String?) {
        if (null != mMeetingUserName) {
            if (!TextUtils.isEmpty(text)) {
                mMeetingUserName!!.visibility = VISIBLE
                mMeetingUserName!!.text = text
            } else {
                mMeetingUserName!!.visibility = GONE
            }
        }
    }

    fun refreshToolbar() {
        updateAudioButton()
        updateVideoButton()
        updateShareButton()
        updateSwitchCameraButton()
    }

    fun updateAudioButton() {
        if (mInMeetingAudioController!!.isAudioConnected) {
            if (mInMeetingAudioController!!.isMyAudioMuted) {
                mAudioStatusImg!!.setImageResource(R.drawable.icon_meeting_audio_mute)
            } else {
                mAudioStatusImg!!.setImageResource(R.drawable.icon_meeting_audio)
            }
        } else {
            mAudioStatusImg!!.setImageResource(R.drawable.icon_meeting_noaudio)
        }
    }

    private val isMySelfWebinarAttendee: Boolean
        get() {
            val myUserInfo: InMeetingUserInfo? = mInMeetingService!!.myUserInfo
            if (myUserInfo != null && mInMeetingService!!.isWebinarMeeting) {
                return myUserInfo.inMeetingUserRole == InMeetingUserRole.USERROLE_ATTENDEE
            }
            return false
        }

    fun updateShareButton() {
        if (isMySelfWebinarAttendee) {
            mBtnShare!!.visibility = GONE
        } else {
            mBtnShare!!.visibility = VISIBLE
            if (mInMeetingShareController!!.isSharingOut) {
                mMeetingShareText!!.text = "Stop share"
                mShareStatusImg!!.setImageResource(R.drawable.icon_share_pause)
            } else {
                mMeetingShareText!!.text = "Share"
                mShareStatusImg!!.setImageResource(R.drawable.icon_share_resume)
            }
        }
    }

    fun updateVideoButton() {
        if (mInMeetingVideoController!!.isMyVideoMuted) {
            mVideoStatusImg!!.setImageResource(R.drawable.icon_meeting_video_mute)
        } else {
            mVideoStatusImg!!.setImageResource(R.drawable.icon_meeting_video)
        }
    }

    fun updateSwitchCameraButton() {
        if (mInMeetingVideoController!!.isMyVideoMuted) {
            switchCameraView!!.visibility = GONE
        } else {
            switchCameraView!!.visibility = VISIBLE
        }
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.ivCloseSetting -> {
                mContainerMeetingSetting?.gone()
            }
            R.id.tvActionCancel -> {
                mContainerMeetingSetting?.gone()
            }
            R.id.tvActionSave -> {
                TemporaryDataHelper.instance().fontSize = textSize
                TemporaryDataHelper.instance().transparency = transparency
                contentAdapter?.updateFontSizeText()
                mContainerMeetingContent?.background?.alpha = transparency
                mContainerMeetingSetting?.gone()
            }
            R.id.ivContentDetail -> {
                onClickContentDetail()
            }
            R.id.ivContentTranslate -> {
                onClickContentTranslate()
            }
            R.id.ivContentSetting -> {
                onClickContentSetting()
            }
            R.id.ivContentDownload -> {
                onClickContentDownload()
            }
            R.id.btnBack -> {
                if (null != mCallBack) {
                    mCallBack!!.onClickBack()
                }
            }
            R.id.btnLeaveZoomMeeting -> {
                if (null != mCallBack) {
                    mCallBack!!.onClickLeave()
                }
            }
            R.id.btnShare -> {
                if (null != mCallBack) {
                    mCallBack!!.onClickShare()
                }
            }
            R.id.btnCamera -> {
                if (null != mCallBack) {
                    updateAudioButton()
                    mCallBack!!.onClickVideo()
                }
            }
            R.id.btnAudio -> {
                if (null != mCallBack) {
                    mCallBack!!.onClickAudio()
                }
            }
            R.id.btnSwitchCamera -> {
                if (null != mCallBack) {
                    mCallBack!!.onClickSwitchCamera()
                }
            }
            R.id.moreActionImg -> {
                showMoreMenuPopupWindow()
            }
            R.id.btnChats -> {
                if (null != mCallBack) {
                    mCallBack!!.onClickChats()
                }
            }
            else -> {
                visibility = INVISIBLE
            }
        }
    }

    private val isMySelfWebinarHostCohost: Boolean
        get() {
            val myUserInfo: InMeetingUserInfo? = mInMeetingService!!.myUserInfo
            if (myUserInfo != null && mInMeetingService!!.isWebinarMeeting) {
                return (myUserInfo.inMeetingUserRole == InMeetingUserRole.USERROLE_HOST
                        || myUserInfo.inMeetingUserRole == InMeetingUserRole.USERROLE_COHOST)
            }
            return false
        }
    private val isMySelfMeetingHostBoModerator: Boolean
        get() {
            val myUserInfo: InMeetingUserInfo? = mInMeetingService!!.myUserInfo
            if (myUserInfo != null && !mInMeetingService!!.isWebinarMeeting) {
                val role: InMeetingUserRole = myUserInfo.inMeetingUserRole
                return role == InMeetingUserRole.USERROLE_HOST ||
                        role == InMeetingUserRole.USERROLE_BREAKOUTROOM_MODERATOR
            }
            return false
        }

    // when client support live transcript, show this item
    // Webinar didn't support request LT
    // when live status is approved , dont show this item
    private val isShowLiveTranscriptionItem: Boolean
        get() {
            val isInBoMeeting: Boolean = mInMeetingService!!.inMeetingBOController.isInBOMeeting
            val inMeetingLiveTranscriptionController: InMeetingLiveTranscriptionController =
                mInMeetingService!!.inMeetingLiveTranscriptionController
            return (!isInBoMeeting && inMeetingLiveTranscriptionController.isLiveTranscriptionFeatureEnabled && !isMySelfHost // when client support live transcript, show this item
                    && inMeetingLiveTranscriptionController.isRequestToStartLiveTranscriptionEnabled // Webinar didn't support request LT
                    && !mInMeetingService!!.isWebinarMeeting // when live status is approved , dont show this item
                    && (inMeetingLiveTranscriptionController.liveTranscriptionStatus != InMeetingLiveTranscriptionController.MobileRTCLiveTranscriptionStatus.MobileRTC_LiveTranscription_Status_Start))
        }

    private val isShowStopTranscriptionItem: Boolean
        get() = isMySelfHost &&
                ((mInMeetingService!!.inMeetingLiveTranscriptionController.liveTranscriptionStatus
                        == InMeetingLiveTranscriptionController.MobileRTCLiveTranscriptionStatus.MobileRTC_LiveTranscription_Status_Start))
    private val isMySelfHost: Boolean
        get() {
            val myUserInfo: InMeetingUserInfo? = mInMeetingService!!.myUserInfo
            if (myUserInfo != null) {
                return myUserInfo.inMeetingUserRole == InMeetingUserRole.USERROLE_HOST
            }
            return false
        }
    private val isMySelfHostCohost: Boolean
        get() {
            val myUserInfo: InMeetingUserInfo? = mInMeetingService!!.myUserInfo
            if (myUserInfo != null) {
                return (myUserInfo.inMeetingUserRole == InMeetingUserRole.USERROLE_HOST
                        || myUserInfo.inMeetingUserRole == InMeetingUserRole.USERROLE_COHOST)
            }
            return false
        }
    var virtualVideoSource: VirtualVideoSource? = null
    private fun showMoreMenuPopupWindow() {
        val menuAdapter = SimpleMenuAdapter(mContext)
        if (mInMeetingAudioController!!.isAudioConnected) {
            menuAdapter.addItem(SimpleMenuItem(MENU_DISCONNECT_AUDIO, "Disconnect Audio"))
        }
        if (mInMeetingAudioController!!.canSwitchAudioOutput()) {
            if (mInMeetingAudioController!!.loudSpeakerStatus) {
                menuAdapter.addItem(SimpleMenuItem(MENU_SPEAKER_OFF, "Speak Off"))
            } else {
                menuAdapter.addItem(SimpleMenuItem(MENU_SPEAKER_ON, "Speak On"))
            }
        }
        if (!isMySelfWebinarAttendee) menuAdapter.addItem(
            (SimpleMenuItem(
                MENU_SHOW_PLIST,
                "Paticipants"
            ))
        )
        if (meetingAnnotationController!!.canDisableViewerAnnotation()) {
            if (!meetingAnnotationController!!.isViewerAnnotationDisabled) {
                menuAdapter.addItem((SimpleMenuItem(MENU_ANNOTATION_OFF, "Disable Annotation")))
            } else {
                menuAdapter.addItem((SimpleMenuItem(MENU_ANNOTATION_ON, "Enable Annotation")))
            }
        }
        if (isMySelfWebinarHostCohost) {
            if (mInMeetingWebinarController!!.isAllowPanellistStartVideo) {
                menuAdapter.addItem(
                    (SimpleMenuItem(
                        MENU_DISALLOW_PANELIST_START_VIDEO,
                        "Disallow panelist start video"
                    ))
                )
            } else {
                menuAdapter.addItem(
                    (SimpleMenuItem(
                        MENU_AllOW_PANELIST_START_VIDEO,
                        "Allow panelist start video"
                    ))
                )
            }
            if (mInMeetingWebinarController!!.isAllowAttendeeChat) {
                menuAdapter.addItem(
                    (SimpleMenuItem(
                        MENU_DISALLOW_ATTENDEE_CHAT,
                        "Disallow attendee chat"
                    ))
                )
            } else {
                menuAdapter.addItem(
                    (SimpleMenuItem(
                        MENU_AllOW_ATTENDEE_CHAT,
                        "Allow attendee chat"
                    ))
                )
            }
        }
        if (BuildConfig.DEBUG) {
//            menuAdapter.addItem((new SimpleMenuItem(MENU_SWITCH_DOMAIN, "Switch Domain")));
        }
        if (BuildConfig.DEBUG) {
            val myUserInfo: InMeetingUserInfo? = mInMeetingService!!.myUserInfo
            if (myUserInfo != null && mInMeetingService!!.isWebinarMeeting) {
                if (mInMeetingService!!.inMeetingQAController.isQAEnabled) {
                    menuAdapter.addItem((SimpleMenuItem(MENU_ANNOTATION_QA, "QA")))
                }
            }
        }
        if (BuildConfig.DEBUG) {
            val interpretationController: InMeetingInterpretationController =
                ZoomSDK.getInstance().inMeetingService.inMeetingInterpretationController
            if ((interpretationController.isInterpretationEnabled && !interpretationController.isInterpreter
                        && interpretationController.isInterpretationStarted)
            ) {
                menuAdapter.addItem(
                    (SimpleMenuItem(
                        MENU_INTERPRETATION,
                        "Language Interpretation"
                    ))
                )
            }
        }
        if (BuildConfig.DEBUG) {
            val interpretationController: InMeetingInterpretationController =
                ZoomSDK.getInstance().inMeetingService.inMeetingInterpretationController
            if (interpretationController.isInterpretationEnabled && isMySelfHost) {
                menuAdapter.addItem((SimpleMenuItem(MENU_INTERPRETATION_ADMIN, "Interpretation")))
            }
        }
        if (isMySelfMeetingHostBoModerator) {
            val boController: InMeetingBOController = mInMeetingService!!.inMeetingBOController
            if (boController.isBOEnabled) {
                menuAdapter.addItem((SimpleMenuItem(MENU_CREATE_BO, "Breakout Rooms")))
            }
        }

        // Add request live transcription button for non-host , support co-host
        if (isShowLiveTranscriptionItem) {
            menuAdapter.addItem(
                SimpleMenuItem(
                    MENU_LIVE_TRANSCRIPTION_REQUEST,
                    "Request Live Transcription"
                )
            )
        }
        if (isShowStopTranscriptionItem) {
            menuAdapter.addItem(
                SimpleMenuItem(
                    MENU_LIVE_TRANSCRIPTION_STOP,
                    "STOP Live Transcription"
                )
            )
        }
        if (isMySelfHostCohost) {
            menuAdapter.addItem((SimpleMenuItem(MENU_LOWER_ALL_HANDS, "Lower All Hands")))
        }
        if (mInMeetingService!!.canReclaimHost()) {
            menuAdapter.addItem((SimpleMenuItem(MENU_RECLAIM_HOST, "Reclaim Host")))
        }
        val popupWindowLayout: View =
            LayoutInflater.from(mContext).inflate(R.layout.popupwindow, null)
        val shareActions: ListView =
            popupWindowLayout.findViewById<View>(R.id.actionListView) as ListView
        val window = PopupWindow(
            popupWindowLayout,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        window.setBackgroundDrawable(resources.getDrawable(R.drawable.bg_transparent))
        shareActions.adapter = menuAdapter
        shareActions.onItemClickListener =
            AdapterView.OnItemClickListener { parent, view, position, id ->
                val item: SimpleMenuItem = menuAdapter.getItem(position) as SimpleMenuItem
                when (item.action) {
                    MENU_DISCONNECT_AUDIO -> if (null != mCallBack) {
                        mCallBack!!.onClickDisconnectAudio()
                    }
                    MENU_SHOW_PLIST -> if (null != mCallBack) {
                        mCallBack!!.onClickPlist()
                    }
                    MENU_AllOW_ATTENDEE_CHAT -> mInMeetingChatController!!.allowAttendeeChat(
                        InMeetingChatController.MobileRTCWebinarChatPriviledge.All_Panelists_And_Attendees
                    )
                    MENU_AllOW_PANELIST_START_VIDEO -> mInMeetingWebinarController!!.allowPanelistStartVideo()
                    MENU_DISALLOW_ATTENDEE_CHAT -> mInMeetingChatController!!.allowAttendeeChat(
                        InMeetingChatController.MobileRTCWebinarChatPriviledge.All_Panelists
                    )
                    MENU_DISALLOW_PANELIST_START_VIDEO -> mInMeetingWebinarController!!.disallowPanelistStartVideo()
                    MENU_SPEAKER_OFF, MENU_SPEAKER_ON -> {
                        if (null != mCallBack) {
                            mCallBack!!.onClickSwitchLoudSpeaker()
                        }
                    }
                    MENU_ANNOTATION_ON -> {
                        meetingAnnotationController!!.disableViewerAnnotation(false)
                    }
                    MENU_ANNOTATION_OFF -> {
                        meetingAnnotationController!!.disableViewerAnnotation(true)
                    }
                    MENU_ANNOTATION_QA -> {
                        mContext!!.startActivity(Intent(mContext, QAActivity::class.java))
                    }
                    MENU_SWITCH_DOMAIN -> {
                        val success: Boolean = ZoomSDK.getInstance().switchDomain("zoom.us", true)
                        Log.d(TAG, "switchDomain:$success")
                    }
                    MENU_CREATE_BO -> {
                        if (null != mCallBack) {
                            mCallBack!!.onClickAdminBo()
                        }
                    }
                    MENU_LOWER_ALL_HANDS -> {
                        if (null != mCallBack) {
                            mCallBack!!.onClickLowerAllHands()
                        }
                    }
                    MENU_RECLAIM_HOST -> {
                        if (null != mCallBack) {
                            mCallBack!!.onClickReclaimHost()
                        }
                    }
                    MENU_VIRTUAL_SOURCE -> {
                        val sourceHelper: ZoomSDKVideoSourceHelper =
                            ZoomSDK.getInstance().videoSourceHelper
                        if (null == virtualVideoSource) {
                            virtualVideoSource = VirtualVideoSource((mContext)!!)
                        }
                        sourceHelper.setExternalVideoSource(virtualVideoSource)
                    }
                    MENU_INTERNAL_SOURCE -> {
                        val sourceHelper: ZoomSDKVideoSourceHelper =
                            ZoomSDK.getInstance().videoSourceHelper
                        val waterMark: Bitmap =
                            BitmapFactory.decodeResource(resources, R.drawable.zm_watermark_sdk)
                        val yuv: ByteArray = YUVConvert.convertBitmapToYuv(waterMark)
                        val data: WaterMarkData =
                            WaterMarkData(waterMark.width, waterMark.height, yuv)
                        sourceHelper.setPreProcessor { rawData ->
                            YUVConvert.addWaterMark(
                                rawData,
                                data,
                                140,
                                120,
                                true
                            )
                        }
                    }
                    MENU_INTERPRETATION -> {
                        val interpre: InMeetingInterpretationController =
                            ZoomSDK.getInstance().inMeetingService.inMeetingInterpretationController
                        Log.d(
                            TAG,
                            "isStart:" + interpre.isInterpretationStarted + " isInterpreter:" + interpre.isInterpreter
                        )
                        if (interpre.isInterpretationStarted && !interpre.isInterpreter) {
                            MeetingInterpretationDialog.show(mContext)
                        }
                    }
                    MENU_INTERPRETATION_ADMIN -> {
                        MeetingInterpretationAdminDialog.show(mContext)
                    }
                    MENU_LIVE_TRANSCRIPTION_REQUEST -> {}
                    MENU_LIVE_TRANSCRIPTION_STOP -> {
                        mInMeetingService!!.inMeetingLiveTranscriptionController.stopLiveTranscription()
                    }
                }
                window.dismiss()
            }
        window.isFocusable = true
        window.isOutsideTouchable = true
        window.update()
        if (null != mCallBack) {
            mCallBack!!.showMoreMenu(window)
        }
    }

    fun receiveMessage(meetingId: String) {
        meetingID = meetingId
        connectServerGetMessage()
    }

    val isShowing: Boolean
        get() {
            return visibility == VISIBLE
        }

    companion object {
        private val TAG: String = "MeetingOptionBar"
    }
}
