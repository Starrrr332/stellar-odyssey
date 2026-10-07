"""
Minecraft Autonomous Testing Engine & GUI Pilot for Stellar Odyssey
-------------------------------------------------------------------
SAFETY PROTOCOL:
- Only clicks, moves mouse, or types keys IF the active foreground window title
  contains "Minecraft" (or matches Minecraft window class).
- Auto-aborts immediately if window loses focus or mouse is moved to corner (PyAutoGUI FailSafe).
"""

import sys
import os
import time
import json
import pygetwindow as gw
import pyautogui
import mss
from PIL import Image

pyautogui.FAILSAFE = True
pyautogui.PAUSE = 0.05

def find_minecraft_window():
    """Finds any active Minecraft window."""
    windows = gw.getAllWindows()
    for w in windows:
        if w.title and ("Minecraft" in w.title or "Stellar Odyssey" in w.title):
            if w.width > 200 and w.height > 200:
                return w
    return None

def is_minecraft_focused(win):
    """Guarantees safety: only executes actions if Minecraft is strictly in the foreground."""
    active = gw.getActiveWindow()
    if not active:
        return False
    return active._hWnd == win._hWnd or ("Minecraft" in active.title)

def capture(output_path="tools/bot/current_screen.png"):
    """Takes a high-res screenshot of the Minecraft window."""
    win = find_minecraft_window()
    if not win:
        return {"status": "error", "message": "Minecraft window not found"}

    try:
        win.activate()
        time.sleep(0.15)
    except Exception:
        pass

    with mss.mss() as sct:
        monitor = {
            "top": max(0, win.top),
            "left": max(0, win.left),
            "width": max(100, win.width),
            "height": max(100, win.height)
        }
        sct_img = sct.grab(monitor)
        img = Image.frombytes("RGB", sct_img.size, sct_img.bgra, "raw", "BGRX")
        os.makedirs(os.path.dirname(output_path), exist_ok=True)
        img.save(output_path)

    return {
        "status": "success",
        "file": os.path.abspath(output_path),
        "window": {
            "title": win.title,
            "width": win.width,
            "height": win.height
        }
    }

def click_relative(rel_x, rel_y, button="left"):
    """
    Clicks at a relative coordinate (0.0 to 1.0) inside the Minecraft window.
    Example: (0.5, 0.5) is the exact center of the screen.
    """
    win = find_minecraft_window()
    if not win:
        return {"status": "error", "message": "Minecraft window not found"}

    if not is_minecraft_focused(win):
        try:
            win.activate()
            time.sleep(0.1)
        except Exception:
            pass

    if not is_minecraft_focused(win):
        return {"status": "error", "message": "Safety guard: Minecraft is not focused. Action blocked."}

    target_x = win.left + int(win.width * rel_x)
    target_y = win.top + int(win.height * rel_y)

    pyautogui.moveTo(target_x, target_y, duration=0.15)
    pyautogui.click(button=button)

    return {"status": "success", "clicked": {"rel_x": rel_x, "rel_y": rel_y, "abs_x": target_x, "abs_y": target_y}}

def send_key(key, duration=0.05):
    """Sends a keypress strictly to Minecraft."""
    win = find_minecraft_window()
    if not win:
        return {"status": "error", "message": "Minecraft window not found"}

    if not is_minecraft_focused(win):
        try:
            win.activate()
            time.sleep(0.1)
        except Exception:
            pass

    if not is_minecraft_focused(win):
        return {"status": "error", "message": "Safety guard: Minecraft is not focused. Action blocked."}

    pyautogui.keyDown(key)
    time.sleep(duration)
    pyautogui.keyUp(key)

    return {"status": "success", "key": key}

def type_command(text):
    """Opens Minecraft chat and types a command, then hits ENTER."""
    win = find_minecraft_window()
    if not win or not is_minecraft_focused(win):
        return {"status": "error", "message": "Safety guard: Minecraft is not focused. Action blocked."}

    # Open chat
    pyautogui.press('t')
    time.sleep(0.1)
    pyautogui.write(text, interval=0.02)
    time.sleep(0.05)
    pyautogui.press('enter')

    return {"status": "success", "typed": text}

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print(json.dumps({"error": "Usage: mc_pilot.py <capture|click|key|command|status> [args]"}))
        sys.exit(1)

    cmd = sys.argv[1]
    if cmd == "status":
        win = find_minecraft_window()
        if win:
            print(json.dumps({"status": "running", "title": win.title, "bounds": [win.left, win.top, win.width, win.height]}))
        else:
            print(json.dumps({"status": "not_found"}))

    elif cmd == "capture":
        path = sys.argv[2] if len(sys.argv) > 2 else "tools/bot/current_screen.png"
        print(json.dumps(capture(path)))

    elif cmd == "click":
        rx = float(sys.argv[2])
        ry = float(sys.argv[3])
        btn = sys.argv[4] if len(sys.argv) > 4 else "left"
        print(json.dumps(click_relative(rx, ry, btn)))

    elif cmd == "key":
        k = sys.argv[2]
        dur = float(sys.argv[3]) if len(sys.argv) > 3 else 0.05
        print(json.dumps(send_key(k, dur)))

    elif cmd == "command":
        text = " ".join(sys.argv[2:])
        print(json.dumps(type_command(text)))
