"""Procedural OGG sound generation for Stellar Odyssey.

Run from the project root:  python tools/gen_sounds.py
Requires: pip install soundfile numpy
"""
import numpy as np
import soundfile as sf
from pathlib import Path

SR = 44100
OUT = Path("common/src/main/resources/assets/stellarodyssey/sounds")
rng = np.random.default_rng(42)


def write(name, data):
    path = OUT / f"{name}.ogg"
    path.parent.mkdir(parents=True, exist_ok=True)
    sf.write(str(path), data, SR)
    print(f"wrote {path}")


def lowpass(x, taps=64):
    k = np.ones(taps) / taps
    return np.convolve(x, k, mode="same")


# 1. Starship ion thrust: filtered noise bed + low engine hum (2s loop)
t = np.arange(SR * 2) / SR
noise = lowpass(rng.standard_normal(len(t)))
hum = 0.5 * np.sin(2 * np.pi * 55 * t) + 0.25 * np.sin(2 * np.pi * 110 * t + 0.5)
thrust = 0.6 * noise + hum
thrust *= 0.45 / np.max(np.abs(thrust))
write("entity/starship/thrust", thrust)

# 2. Decompression alarm: dual-tone klaxon beeps (2s)
t = np.arange(SR * 2) / SR
beep = np.where((t % 0.5) < 0.22, 1.0, 0.0)
alarm = beep * (np.sin(2 * np.pi * 660 * t) + 0.6 * np.sin(2 * np.pi * 880 * t))
alarm *= 0.4 / np.max(np.abs(alarm))
write("hazard/decompression_alarm", alarm)

# 3. Alien world ambience: slow detuned drone with breathing modulation (4s loop)
t = np.arange(SR * 4) / SR
drone = (
    np.sin(2 * np.pi * 48 * t)
    + 0.6 * np.sin(2 * np.pi * 72.5 * t + 0.3)
    + 0.4 * np.sin(2 * np.pi * 96.3 * t + 1.1)
)
breath = 0.65 + 0.35 * np.sin(2 * np.pi * 0.08 * t)
drone *= 0.3 * breath
write("ambient/alien_world", drone)

# 4. Alien ore resonance: crystalline chime arpeggio (C5 E5 G5 C6)
t = np.arange(SR * 2) / SR
chime = np.zeros_like(t)
for i, f in enumerate([523.25, 659.25, 783.99, 1046.5]):
    start = i * 0.42
    idx = (t >= start) & (t < start + 1.3)
    tt = t[idx] - start
    chime[idx] += 0.3 * np.exp(-3.2 * tt) * (
        np.sin(2 * np.pi * f * tt) + 0.35 * np.sin(2 * np.pi * 2 * f * tt)
    )
chime *= 0.5 / np.max(np.abs(chime))
write("block/alien_ore/resonate", chime)
