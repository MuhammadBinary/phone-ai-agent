package com.phoneai.agent

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText

class MainActivity : AppCompatActivity() {

    private lateinit var apiKeyInput: TextInputEditText
    private lateinit var modelInput: TextInputEditText
    private lateinit var commandInput: TextInputEditText
    private lateinit var btnGrant: Button
    private lateinit var btnExecute: Button
    private lateinit var logOutput: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        apiKeyInput = findViewById(R.id.apiKeyInput)
        modelInput = findViewById(R.id.modelInput)
        commandInput = findViewById(R.id.commandInput)
        btnGrant = findViewById(R.id.btnGrantAccess)
        btnExecute = findViewById(R.id.btnExecute)
        logOutput = findViewById(R.id.logOutput)

        btnGrant.setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
            Toast.makeText(this, "Enable 'Phone AI Agent' in Accessibility Services", Toast.LENGTH_LONG).show()
        }

        btnExecute.setOnClickListener {
            val apiKey = apiKeyInput.text.toString().trim()
            val model = modelInput.text.toString().trim()
            val command = commandInput.text.toString().trim()

            if (apiKey.isEmpty()) {
                Toast.makeText(this, "Enter OpenRouter API Key", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (command.isEmpty()) {
                Toast.makeText(this, "Enter a command", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val service = AutoAgentService.instance
            if (service == null) {
                logOutput.appendLine("[!] Service not running. Enable it first.")
                return@setOnClickListener
            }

            logOutput.appendLine("[>] Executing: " + command)
            service.executeCommand(command, apiKey, model) { result ->
                runOnUiThread {
                    logOutput.appendLine("[<] " + result)
                }
            }
        }

        checkAccessibilityStatus()
    }

    override fun onResume() {
        super.onResume()
        checkAccessibilityStatus()
    }

    private fun checkAccessibilityStatus() {
        val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val services = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
        var isOn = false
        for (s in services) {
            val info = s.resolveInfo.serviceInfo
            if (info.packageName == packageName) {
                isOn = true
                break
            }
        }
        
        btnExecute.isEnabled = isOn
        if (isOn) {
            logOutput.appendLine("[OK] Service is ON")
        } else {
            logOutput.appendLine("[!] Service is OFF")
        }
    }
}
