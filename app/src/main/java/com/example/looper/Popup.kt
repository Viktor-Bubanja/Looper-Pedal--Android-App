package com.example.looper

import android.media.Image
import android.widget.Button
import android.widget.ImageButton
import android.widget.PopupWindow

data class Popup(var window: PopupWindow?, var actionButton: Button?, var closeButton: ImageButton?) {
}