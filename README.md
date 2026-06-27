# Phone AI Agent

Lightweight Android AI automation app. 32-bit compatible. Uses Accessibility Service + OpenRouter API.

## Features
- Pure Kotlin — no native libraries, no Firebase, no heavy dependencies
- Works on 32-bit (armeabi-v7a) and 64-bit Android devices
- Reads screen via Accessibility Service
- Controls phone via tap, swipe, type, back, home actions
- Uses OpenRouter API (any LLM model you want)

## Quick Start

### 1. Get an OpenRouter API Key
- Go to https://openrouter.ai/
- Sign up (free credits available)
- Copy your API key

### 2. Build the APK (Option A: GitHub Actions)
- Push this repo to GitHub
- Go to Actions → Build APK → Run workflow
- Download the APK artifact after ~5 minutes

### 3. Build the APK (Option B: Local)
- Install Android Studio
- Open this project
- Build → Build Bundle(s) / APK(s) → Build APK(s)
- The APK will be in `app/build/outputs/apk/debug/`

### 4. Install & Run
- Install the APK on your phone
- Open the app
- Enter your OpenRouter API key
- Enter a model name (e.g., `google/gemma-3-4b-it`, `openai/gpt-4o-mini`, `anthropic/claude-3-haiku`)
- Tap "Grant Accessibility Access" and enable the service in Settings
- Enter a command like "Open Chrome and search for cats"
- Tap "Execute Command"

## How It Works
1. App reads your screen via Android Accessibility Service (text, buttons, fields)
2. Sends screen context + your command to OpenRouter LLM
3. LLM returns action markers like `[TAP:Search]`, `[TYPE:Search:cats]`, `[SWIPE:up]`
4. App parses and executes those actions on your phone

## Supported Actions
- `[TAP:text]` — Tap element with matching text
- `[TYPE:field_text:input_text]` — Type into a field
- `[SWIPE:up/down/left/right]` — Swipe gesture
- `[BACK]` — Press back button
- `[HOME]` — Press home button

## Models That Work Well
- `google/gemma-3-4b-it` (fast, cheap, good for UI tasks)
- `openai/gpt-4o-mini` (very capable)
- `anthropic/claude-3-haiku` (good reasoning)
- `meta-llama/llama-3.1-8b-instruct` (free tier available)

## Troubleshooting
- **"Accessibility Service not running"** → Go to Settings → Accessibility → Phone AI Agent → Enable
- **"API Error"** → Check your OpenRouter API key and internet connection
- **App won't install** → Make sure "Install unknown apps" is enabled for your file manager

## License
Personal use. Modify as you wish.
