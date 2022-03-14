package com.zoomstt.beta.ui.adapter

import android.content.Context
import android.graphics.PorterDuff
import android.util.Log
import androidx.recyclerview.widget.DiffUtil
import com.zoomstt.beta.R
import com.zoomstt.beta.TemporaryDataHelper
import com.zoomstt.beta.data.model.ContentSpeechItem
import com.zoomstt.beta.databinding.ItemContentBinding
import com.zoomstt.beta.ui.base.BaseRecyclerViewAdapter
import com.zoomstt.beta.zoombeta.ColorResource
import kotlin.math.log

class ContentChatAdapter(val context: Context) :
    BaseRecyclerViewAdapter<ContentSpeechItem, ItemContentBinding>(object :
        DiffUtil.ItemCallback<ContentSpeechItem>() {
        override fun areItemsTheSame(
            oldItem: ContentSpeechItem,
            newItem: ContentSpeechItem
        ): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(
            oldItem: ContentSpeechItem,
            newItem: ContentSpeechItem
        ): Boolean {
            return oldItem == newItem
        }
    }) {

    private var contentList: List<ContentSpeechItem>? = null

    override val layoutRes: Int
        get() = R.layout.item_content

    private var colorResource: Int? = null
    private var userName: String = ""
    private var textSize = 10

    fun changeColorText(userName: String, colorResource: Int, ) {
        this.userName = userName
        this.colorResource = colorResource
        this.notifyDataSetChanged()
    }

    override fun bindData(itemBinding: ItemContentBinding, position: Int) {
        super.bindData(itemBinding, position)
        val history = contentList?.get(position) ?: return
        itemBinding.apply {
            bindViews(history)
        }
    }

    override fun submitList(list: List<ContentSpeechItem>?) {
        super.submitList(list)
        contentList = list
        notifyDataSetChanged()
    }

    fun updateFontSizeText(){
        textSize = TemporaryDataHelper.instance().fontSize
        notifyDataSetChanged()
    }

    private fun ItemContentBinding.bindViews(content: ContentSpeechItem) {
        colorResource?.let {
            ivContentPerson.setColorFilter(
                it,
                PorterDuff.Mode.SRC_IN
            )
        }
        colorResource?.let { tvPersonName.setTextColor(it) }
        colorResource?.let { tvContent.setTextColor(it) }

        ivContentPerson.setImageResource(R.drawable.ic_content_person)
        tvPersonName.text = content.userName
        tvContent.text = content.content

        if (textSize != 0){
            tvPersonName.textSize = textSize.toFloat()
            tvContent.textSize = textSize.toFloat()
        }
    }
}