import os
import requests
import glob

bot_token = os.environ["BOT_TOKEN"]
chat_id = os.environ["CHAT_ID"]
branch = os.environ["GITHUB_REF_NAME"]
commit = os.environ["GITHUB_SHA"][:7]
actor = os.environ["GITHUB_ACTOR"]

commit_message = os.popen("git log -1 --pretty=format:'%s'").read().strip()

caption = (
    f"<b>DEBUG BUILD SUCCESS</b>\n\n"
    f"<b>Branch:</b> <code>{branch}</code>\n"
    f"<b>Commit:</b> <code>{commit}</code>\n"
    f"<b>Message:</b> {commit_message}\n"
    f"<b>Author:</b> {actor}"
)

apk_files = glob.glob("app/build/outputs/apk/debug/app-debug-*.apk")

if apk_files:
    apk_path = apk_files[0]
else:
    apk_path = "app/build/outputs/apk/debug/app-debug.apk"

with open(apk_path, "rb") as apk:
    response = requests.post(
        f"https://api.telegram.org/bot{bot_token}/sendDocument",
        data={"chat_id": chat_id, "caption": caption, "parse_mode": "HTML"},
        files={"document": (f"app-debug-{commit}.apk", apk)}
    )
    print(response.json())