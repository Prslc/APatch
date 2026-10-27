import json
import os
import sys
import html
import urllib.request
import uuid

def get_commit_summary():
    try:
        event_path = os.environ.get("GITHUB_EVENT_PATH")
        with open(event_path, 'r') as f:
            event = json.load(f)
    except Exception:
        return "No commit data available"

    commits = event.get("commits", [])
    
    if len(commits) > 1:
        summary_lines = []
        for c in commits[::-1]:
            msg = html.escape(c.get("message", "").split('\n')[0].strip())
            author = html.escape(c.get("author", {}).get("username", "unknown"))
            summary_lines.append(f"{msg} by {author}")
            
            if len(summary_lines) >= 10:
                summary_lines.append(f"<i>(...and {len(commits) - 10} more)</i>")
                break
        return "\n".join(summary_lines)

    elif len(commits) == 1:
        c = commits[0]
        full_msg = html.escape(c.get("message", "").strip())
        author = html.escape(c.get("author", {}).get("username", "unknown"))
        return f"{full_msg}\n\nby {author}"

    else:
        head = event.get("head_commit", {})
        msg = html.escape(head.get("message", "Manual Build").strip())
        return msg if msg else "No new commits."

def send_to_telegram(chat_id, file_path):
    token = os.environ.get("BOT_TOKEN")
    branch = html.escape(os.environ.get("BRANCH", "main"))
    version = html.escape(os.environ.get("VERSION", "0"))
    kp_version = html.escape(os.environ.get("KP_VERSION", "unknown"))
    kp_hash = html.escape(os.environ.get("KP_HASH", "unknown"))
    run_url = os.environ.get("RUN_URL", "#")

    try:
        with open(os.environ.get("GITHUB_EVENT_PATH"), 'r') as f:
            event = json.load(f)
            commits = event.get("commits", [])

            if len(commits) == 1:
                compare_url = commits[0].get("url", run_url)
                compare_label = "Commit"
            else:
                compare_url = event.get("compare", run_url)
                compare_label = "Compare"
    except:
        compare_url = run_url

    summary = get_commit_summary()

    if len(summary) > 600:
        summary = summary[:600] + "..."

    caption = (
        f"<b>Branch:</b> <code>{branch}</code>\n"
        f"<b>Manager:</b> <code>v{version}</code>\n"
        f"<b>KernelPatch:</b> {kp_version}(<code>{kp_hash}</code>)\n\n"
        f"<blockquote>{summary}</blockquote>\n\n"
        f"<a href=\"{compare_url}\">{compare_label}</a> | "
        f"<a href=\"{run_url}\">Workflow</a>"
    )

    full_caption = caption

    if len(full_caption) > 1024:
        full_caption = caption[:950] + "...</blockquote>"

    media_payload = json.dumps([{
        "type": "document",
        "media": "attach://apk_file",
        "caption": full_caption[:1024],
        "parse_mode": "HTML"
    }])

    boundary = f'----APatchBuild-{uuid.uuid4().hex}'
    with open(file_path, 'rb') as f:
        file_content = f.read()

    body = (
        f'--{boundary}\r\nContent-Disposition: form-data; name="chat_id"\r\n\r\n{chat_id}\r\n'
        f'--{boundary}\r\nContent-Disposition: form-data; name="media"\r\n\r\n{media_payload}\r\n'
        f'--{boundary}\r\nContent-Disposition: form-data; name="apk_file"; filename="{os.path.basename(file_path)}"\r\n'
        f'Content-Type: application/vnd.android.package-archive\r\n\r\n'
    ).encode('utf-8') + file_content + f'\r\n--{boundary}--\r\n'.encode('utf-8')

    req = urllib.request.Request(f"https://api.telegram.org/bot{token}/sendMediaGroup?disable_notification=true", data=body)
    req.add_header('Content-Type', f'multipart/form-data; boundary={boundary}')
    
    try:
        with urllib.request.urlopen(req) as res:
            print(f"Success: {res.read().decode()}")
    except Exception as e:
        print(f"Error: {e}")
        if hasattr(e, 'read'): print(e.read().decode())
        sys.exit(1)

if __name__ == "__main__":
    if len(sys.argv) < 3:
        sys.exit(1)
    send_to_telegram(sys.argv[1], sys.argv[2])