package br.com.ronyasobral3.overlaydictionary

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.abs

class OverlayService : Service() {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val translationCache = object : LinkedHashMap<String, String>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: Map.Entry<String, String>) = size > 50
    }

    private lateinit var windowManager: WindowManager
    private var sidebarView: View? = null
    private var sidebarParams: WindowManager.LayoutParams? = null
    private var modalView: View? = null
    private var modalParams: WindowManager.LayoutParams? = null

    private lateinit var clipboardManager: ClipboardManager
    private val clipboardListener = ClipboardManager.OnPrimaryClipChangedListener {
        val text = clipboardManager.primaryClip
            ?.getItemAt(0)
            ?.coerceToText(this)
            ?.toString()
            ?.trim()
            ?.takeIf { it.isNotEmpty() } ?: return@OnPrimaryClipChangedListener
        if (modalView == null) showModal(prefillText = text)
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        startForegroundWithNotification()
        showSidebar()
        clipboardManager = getSystemService(ClipboardManager::class.java)
        clipboardManager.addPrimaryClipChangedListener(clipboardListener)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startForegroundWithNotification() {
        val channelId = "overlay_dict_channel"
        val channel = NotificationChannel(
            channelId,
            "Overlay Dictionary",
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Overlay Dictionary")
            .setContentText("Toque no botão flutuante para traduzir")
            .setSmallIcon(android.R.drawable.ic_menu_search)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, notification)
        }
    }

    // region Sidebar

    private fun showSidebar() {
        val sidebar = buildSidebarView()

        sidebarParams = WindowManager.LayoutParams(
            12.dp,
            100.dp,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            y = 300.dp
        }

        var initialY = 0
        var initialTouchY = 0f
        var isDragging = false

        sidebar.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialY = sidebarParams!!.y
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dy = event.rawY - initialTouchY
                    if (abs(dy) > 8f) isDragging = true
                    if (isDragging) {
                        sidebarParams!!.y = (initialY + dy).toInt()
                        windowManager.updateViewLayout(sidebar, sidebarParams)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) showModal()
                    true
                }
                else -> false
            }
        }

        sidebarView = sidebar
        windowManager.addView(sidebar, sidebarParams)
    }

    private fun buildSidebarView(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        setPadding(8.dp, 12.dp, 8.dp, 12.dp)
        background = roundedRect(color = "#CC6200EE", radiusDp = 24)

        addView(TextView(context).apply {
            text = "T"
            textSize = 18f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
        })

    }

    // endregion

    // region Modal

    private fun showModal(prefillText: String? = null) {
        if (modalView != null) return

        val screenWidth = screenWidth()
        val horizontalMargin = 24.dp
        val modalWidth = screenWidth - horizontalMargin * 2

        val params = WindowManager.LayoutParams(
            modalWidth,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_DIM_BEHIND,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = 32.dp
            dimAmount = 0.4f
        }
        modalParams = params

        val modal = buildModalView(prefillText)
        modalView = modal
        windowManager.addView(modal, params)
    }

    private fun buildModalView(prefillText: String? = null): LinearLayout {
        var translateButton: Button? = null
        var resultView: TextView? = null

        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedRect(color = "#FFFFFFFF", radiusDp = 16)
            val p = 16.dp
            setPadding(p, p, p, p)

            // Header row
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL

                addView(TextView(context).apply {
                    text = "Tradução"
                    textSize = 18f
                    setTextColor(Color.parseColor("#1A1A1A"))
                    typeface = Typeface.DEFAULT_BOLD
                    layoutParams = LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                    )

                    var initialModalY = 0
                    var initialTouchY = 0f

                    setOnTouchListener { _, event ->
                        when (event.action) {
                            MotionEvent.ACTION_DOWN -> {
                                initialModalY = modalParams?.y ?: 0
                                initialTouchY = event.rawY
                                true
                            }
                            MotionEvent.ACTION_MOVE -> {
                                val dy = initialTouchY - event.rawY
                                modalParams?.y = (initialModalY + dy).toInt().coerceAtLeast(0)
                                modalView?.let { windowManager.updateViewLayout(it, modalParams) }
                                true
                            }
                            MotionEvent.ACTION_UP -> true
                            else -> false
                        }
                    }
                })

                addView(Button(context).apply {
                    text = "✕"
                    textSize = 16f
                    setTextColor(Color.GRAY)
                    setBackgroundColor(Color.TRANSPARENT)
                    setOnClickListener { hideModal() }
                })
            })

            // Divider
            addView(View(context).apply {
                setBackgroundColor(Color.parseColor("#E0E0E0"))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1
                ).apply {
                    topMargin = 8.dp
                    bottomMargin = 12.dp
                }
            })

            // Input
            val input = EditText(context).apply {
                hint = "Digite em inglês..."
                textSize = 15f
                setTextColor(Color.parseColor("#1A1A1A"))
                setHintTextColor(Color.parseColor("#AAAAAA"))
                background = roundedRect(color = "#FFF5F5F5", radiusDp = 8)
                setPadding(12.dp, 10.dp, 12.dp, 10.dp)
                minLines = 1
                maxLines = 2
                gravity = Gravity.TOP
                prefillText?.let { setText(it) }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 8.dp }
            }
            addView(input)

            // Result area
            resultView = TextView(context).apply {
                hint = "Tradução aparecerá aqui..."
                textSize = 15f
                setTextColor(Color.parseColor("#333333"))
                setHintTextColor(Color.parseColor("#AAAAAA"))
                background = roundedRect(color = "#FFF0F0F0", radiusDp = 8)
                setPadding(12.dp, 10.dp, 12.dp, 10.dp)
                minLines = 1
                maxLines = 2
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 8.dp }
            }
            addView(resultView)

            // Translate button
            translateButton = Button(context).apply {
                text = "Traduzir"
                textSize = 15f
                setTextColor(Color.WHITE)
                background = roundedRect(color = "#FF6200EE", radiusDp = 8)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setOnClickListener {
                    val text = input.text.toString().trim()
                    if (text.isEmpty()) return@setOnClickListener

                    val cached = translationCache[text]
                    if (cached != null) {
                        resultView?.text = cached
                        return@setOnClickListener
                    }

                    isEnabled = false
                    resultView?.text = "Traduzindo..."

                    scope.launch {
                        val result = translate(text)
                        result.fold(
                            onSuccess = {
                                translationCache[text] = it
                                resultView?.text = it
                            },
                            onFailure = { resultView?.text = "Erro: verifique a conexão" }
                        )
                        isEnabled = true
                    }
                }
            }
            addView(translateButton)

            if (prefillText != null) translateButton?.performClick()

            addView(Button(context).apply {
                text = "Desligar overlay"
                textSize = 13f
                setTextColor(Color.parseColor("#B00020"))
                setBackgroundColor(Color.TRANSPARENT)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = 2.dp }
                setOnClickListener {
                    hideModal()
                    stopSelf()
                }
            })
        }
    }

    private fun hideModal() {
        modalView?.let {
            windowManager.removeView(it)
            modalView = null
        }
    }

    // endregion

    // region Translation

    private suspend fun translate(text: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val encoded = URLEncoder.encode(text, "UTF-8")
            val url = URL("https://api.mymemory.translated.net/get?q=$encoded&langpair=en|pt")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.requestMethod = "GET"
            val response = conn.inputStream.bufferedReader().readText()
            JSONObject(response)
                .getJSONObject("responseData")
                .getString("translatedText")
        }
    }

    // endregion

    // region Helpers

    private fun roundedRect(color: String, radiusDp: Int): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(Color.parseColor(color))
            cornerRadius = radiusDp.dp.toFloat()
        }

    private fun screenHeight(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            windowManager.currentWindowMetrics.bounds.height()
        } else {
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getMetrics(metrics)
            metrics.heightPixels
        }

    private fun screenWidth(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            windowManager.currentWindowMetrics.bounds.width()
        } else {
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getMetrics(metrics)
            metrics.widthPixels
        }

    private val Int.dp: Int get() = (this * resources.displayMetrics.density).toInt()

    // endregion

    override fun onDestroy() {
        clipboardManager.removePrimaryClipChangedListener(clipboardListener)
        scope.cancel()
        sidebarView?.let { windowManager.removeView(it) }
        modalView?.let { windowManager.removeView(it) }
        super.onDestroy()
    }
}
