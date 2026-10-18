import os
import sys
from pathlib import Path

import requests

CAPTION_LIMIT = 1024
API_URL = "https://api.telegram.org/bot{token}/sendDocument"


def build_caption(author, short_sha, version, file_name, commit_msg):
    caption = (
        "RELEASE BUILD COMPILED\n\n"
        f"Committer: {author or 'Unknown'}\n"
        f"Commit ID: {short_sha or 'N/A'}\n"
        f"Version: {version}\n"
        f"File: {file_name}\n"
        "Type: Release Build\n"
        "Status: Ready for Distribution\n\n"
        "Changes:\n"
        f"{commit_msg or 'No message'}"
    )
    if len(caption) > CAPTION_LIMIT:
        caption = caption[: CAPTION_LIMIT - 3] + "..."
    return caption


def post_document(url, apk_path, data):
    with open(apk_path, "rb") as apk_file:
        return requests.post(
            url,
            files={"document": apk_file},
            data=data,
            timeout=120,
        )


def send_release_apk_to_telegram():
    bot_token = os.getenv("TELEGRAM_BOT_TOKEN")
    chat_id = os.getenv("TELEGRAM_CHAT_ID")
    topic_id = os.getenv("TELEGRAM_TOPIC_ID")
    apk_path = os.getenv("APK_PATH")
    short_sha = os.getenv("SHORT_SHA")
    author_name = os.getenv("AUTHOR_NAME")
    commit_msg = os.getenv("COMMIT_MSG")
    version_name = os.getenv("VERSION_NAME", "N/A")

    if not all([bot_token, chat_id, apk_path]):
        print("Missing required environment variables")
        return False

    if not Path(apk_path).exists():
        print(f"APK file not found: {apk_path}")
        return False

    caption = build_caption(
        author_name, short_sha, version_name, Path(apk_path).name, commit_msg
    )
    url = API_URL.format(token=bot_token)

    data = {"chat_id": str(chat_id), "caption": caption}
    if topic_id:
        data["message_thread_id"] = str(topic_id)

    try:
        response = post_document(url, apk_path, data)
        result = response.json()

        # Fallback jika topik ditutup: kirim ke chat utama
        if not result.get("ok") and "TOPIC_CLOSED" in (result.get("description") or ""):
            print("Topic is closed, sending to main chat instead...")
            data.pop("message_thread_id", None)
            response = post_document(url, apk_path, data)
            result = response.json()

        if result.get("ok"):
            message_id = result.get("result", {}).get("message_id")
            print(f"Release APK sent successfully! Message ID: {message_id}")
            return True

        print(f"Telegram error: {result.get('description')}")
        return False

    except requests.exceptions.RequestException as e:
        print(f"Request failed: {e}")
        return False
    except ValueError as e:
        print(f"Invalid response from Telegram: {e}")
        return False


if __name__ == "__main__":
    sys.exit(0 if send_release_apk_to_telegram() else 1)