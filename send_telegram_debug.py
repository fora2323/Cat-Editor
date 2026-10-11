import os
import requests
from pathlib import Path

def send_debug_apk_to_telegram():
    # Ambil dari environment variables
    bot_token = os.getenv('TELEGRAM_BOT_TOKEN')
    chat_id = os.getenv('TELEGRAM_CHAT_ID')
    topic_id = os.getenv('TELEGRAM_TOPIC_ID')
    
    apk_path = os.getenv('APK_PATH')
    short_sha = os.getenv('SHORT_SHA')
    author_name = os.getenv('AUTHOR_NAME')
    commit_msg = os.getenv('COMMIT_MSG')
    
    # Validasi
    if not all([bot_token, chat_id, apk_path]):
        print("Missing required environment variables")
        return False
    
    # Check APK exists
    if not Path(apk_path).exists():
        print(f"APK file not found: {apk_path}")
        return False
    
    # Prepare caption
    caption = f"""DEBUG BUILD COMPILED

Committer: {author_name or 'Unknown'}
Commit ID: {short_sha}
Message: {commit_msg or 'No message'}
File: {Path(apk_path).name}
Type: Debug Build
"""
    
    # Telegram API URL
    url = f"https://api.telegram.org/bot{bot_token}/sendDocument"
    
    # Prepare files
    with open(apk_path, 'rb') as apk_file:
        files = {
            'document': apk_file
        }
        
        data = {
            'chat_id': chat_id,
            'caption': caption,
            'parse_mode': 'Markdown'
        }
        
        # Add topic ID jika ada
        if topic_id:
            data['message_thread_id'] = topic_id
        
        try:
            response = requests.post(url, files=files, data=data, timeout=30)
            response.raise_for_status()
            
            result = response.json()
            if result.get('ok'):
                print(f"Debug APK sent successfully! Message ID: {result.get('result', {}).get('message_id')}")
                return True
            else:
                print(f"Telegram error: {result.get('description')}")
                return False
                
        except requests.exceptions.RequestException as e:
            print(f"Request failed: {e}")
            return False

if __name__ == '__main__':
    success = send_debug_apk_to_telegram()
    exit(0 if success else 1)