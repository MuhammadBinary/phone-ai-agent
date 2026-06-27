package com.phoneai.agent

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import org.json.JSONArray
import org.json.JSONObject

class AutoAgentService : AccessibilityService() {

    companion object {
        var instance: AutoAgentService? = null
        private const val TAG = "AutoAgent"
    }

    private val handler = Handler(Looper.getMainLooper())
    private val openRouter = OpenRouterClient()
    private var pendingCallback: ((String) -> Unit)? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d(TAG, "Accessibility Service Connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // We process on-demand, not on every event
    }

    override fun onInterrupt() {
        Log.d(TAG, "Service Interrupted")
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    fun executeCommand(command: String, apiKey: String, model: String, callback: (String) -> Unit) {
        pendingCallback = callback
        callback("[...] Capturing screen state...")

        val rootNode = rootInActiveWindow
        if (rootNode == null) {
            callback("[!] Cannot access screen. Is the service enabled?")
            return
        }

        val screenContext = buildScreenContext(rootNode)
        rootNode.recycle()

        callback("[...] Sending to OpenRouter ($model)...")

        openRouter.sendCommand(command, screenContext, apiKey, model) { response ->
            handler.post {
                if (response == null) {
                    callback("[!] API Error. Check key and network.")
                    return@post
                }
                callback("[✓] AI Response received")
                executeActions(response, callback)
            }
        }
    }

    private fun buildScreenContext(root: AccessibilityNodeInfo): String {
        val sb = StringBuilder()
        sb.append("Current screen elements:
")
        traverseNode(root, sb, 0)
        return sb.toString()
    }

    private fun traverseNode(node: AccessibilityNodeInfo, sb: StringBuilder, depth: Int) {
        val indent = "  ".repeat(depth)
        val text = node.text?.toString() ?: ""
        val contentDesc = node.contentDescription?.toString() ?: ""
        val className = node.className?.toString()?.substringAfterLast('.') ?: "View"
        val clickable = if (node.isClickable) "[CLICKABLE]" else ""
        val editable = if (node.isEditable) "[EDITABLE]" else ""
        val scrollable = if (node.isScrollable) "[SCROLLABLE]" else ""

        if (text.isNotEmpty() || contentDesc.isNotEmpty()) {
            val displayText = if (text.isNotEmpty()) text else contentDesc
            sb.append("$indent- $className: "$displayText" $clickable $editable $scrollable
")
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            traverseNode(child, sb, depth + 1)
            child.recycle()
        }
    }

    private fun executeActions(response: String, callback: (String) -> Unit) {
        try {
            val json = JSONObject(response)
            val choices = json.getJSONArray("choices")
            if (choices.length() == 0) {
                callback("[!] Empty response from AI")
                return
            }

            val content = choices.getJSONObject(0)
                .getJSONObject("message")
                .getString("content")

            callback("[AI] $content")
            parseAndExecuteActions(content, callback)

        } catch (e: Exception) {
            callback("[!] Parse error: ${e.message}")
            Log.e(TAG, "Parse error", e)
        }
    }

    private fun parseAndExecuteActions(content: String, callback: (String) -> Unit) {
        val tapRegex = "\[TAP:([^\]]+)\]".toRegex()
        val typeRegex = "\[TYPE:([^\]]+):([^\]]+)\]".toRegex()
        val swipeRegex = "\[SWIPE:([^\]]+)\]".toRegex()
        val backRegex = "\[BACK\]".toRegex()
        val homeRegex = "\[HOME\]".toRegex()

        var actionTaken = false

        backRegex.find(content)?.let {
            performGlobalAction(GLOBAL_ACTION_BACK)
            callback("[Action] Pressed BACK")
            actionTaken = true
        }

        homeRegex.find(content)?.let {
            performGlobalAction(GLOBAL_ACTION_HOME)
            callback("[Action] Pressed HOME")
            actionTaken = true
        }

        tapRegex.findAll(content).forEach { match ->
            val targetText = match.groupValues[1]
            if (tapByText(targetText)) {
                callback("[Action] Tapped: $targetText")
                actionTaken = true
            } else {
                callback("[!] Could not find: $targetText")
            }
        }

        typeRegex.findAll(content).forEach { match ->
            val targetText = match.groupValues[1]
            val inputText = match.groupValues[2]
            if (typeIntoField(targetText, inputText)) {
                callback("[Action] Typed '$inputText' into: $targetText")
                actionTaken = true
            } else {
                callback("[!] Could not find field: $targetText")
            }
        }

        swipeRegex.findAll(content).forEach { match ->
            val direction = match.groupValues[1].lowercase()
            when (direction) {
                "up" -> performSwipe(540f, 1200f, 540f, 400f)
                "down" -> performSwipe(540f, 400f, 540f, 1200f)
                "left" -> performSwipe(800f, 900f, 200f, 900f)
                "right" -> performSwipe(200f, 900f, 800f, 900f)
            }
            callback("[Action] Swiped $direction")
            actionTaken = true
        }

        if (!actionTaken) {
            callback("[i] No executable actions found in response.")
        }
    }

    private fun tapByText(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val node = findNodeByText(root, text)
        root.recycle()

        if (node != null) {
            val rect = Rect()
            node.getBoundsInScreen(rect)
            node.recycle()
            return performTap(rect.centerX().toFloat(), rect.centerY().toFloat())
        }
        return false
    }

    private fun typeIntoField(hintText: String, input: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val node = findNodeByText(root, hintText)
        root.recycle()

        if (node != null) {
            val args = android.os.Bundle()
            args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, input)
            val success = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
            node.recycle()
            return success
        }
        return false
    }

    private fun findNodeByText(root: AccessibilityNodeInfo, text: String): AccessibilityNodeInfo? {
        val queue = java.util.ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)

        while (queue.isNotEmpty()) {
            val node = queue.poll()
            val nodeText = node.text?.toString() ?: ""
            val contentDesc = node.contentDescription?.toString() ?: ""

            if (nodeText.contains(text, ignoreCase = true) || 
                contentDesc.contains(text, ignoreCase = true)) {
                return node
            }

            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }
        return null
    }

    private fun performTap(x: Float, y: Float): Boolean {
        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
            .build()
        return dispatchGesture(gesture, null, null)
    }

    private fun performSwipe(x1: Float, y1: Float, x2: Float, y2: Float) {
        val path = Path().apply {
            moveTo(x1, y1)
            lineTo(x2, y2)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 300))
            .build()
        dispatchGesture(gesture, null, null)
    }
}
