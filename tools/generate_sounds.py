#!/usr/bin/env python3
"""Synthetisiert die Geräusche der Mod (Schlag, Aufsprung, Netz, Applaus) und speichert sie als OGG.

Benötigt numpy und ffmpeg.  Aufruf:  python3 tools/generate_sounds.py
"""
import subprocess
import tempfile
import wave
from pathlib import Path

import numpy as np

RATE = 44100
OUT = Path(__file__).resolve().parent.parent / "src" / "main" / "resources" / "assets" / "tennis" / "sounds"


def t(seconds):
    return np.arange(int(RATE * seconds)) / RATE


def lowpass(x, cutoff):
    a = np.exp(-2 * np.pi * cutoff / RATE)
    y = np.zeros_like(x)
    acc = 0.0
    for i, v in enumerate(x):
        acc = (1 - a) * v + a * acc
        y[i] = acc
    return y


def bandpass(x, lo, hi):
    return lowpass(x, hi) - lowpass(x, lo)


def pock(rng, base):
    tt = t(0.16)
    sig = np.zeros_like(tt)
    for f, amp, tau in ((base, 1.0, 0.018), (base * 1.52, 0.6, 0.012), (base * 2.3, 0.35, 0.008), (base * 0.5, 0.5, 0.03)):
        sig += amp * np.sin(2 * np.pi * f * tt + rng.uniform(0, 6.28)) * np.exp(-tt / tau)
    click = rng.normal(0, 1, len(tt)) * np.exp(-tt / 0.002)
    return sig + 0.8 * bandpass(click, 800, 6000)


def bounce(rng):
    tt = t(0.14)
    sig = np.sin(2 * np.pi * 210 * tt) * np.exp(-tt / 0.025) + 0.5 * np.sin(2 * np.pi * 520 * tt) * np.exp(-tt / 0.015)
    noise = rng.normal(0, 1, len(tt)) * np.exp(-tt / 0.006)
    return sig + 0.6 * lowpass(noise, 2500)


def net(rng):
    tt = t(0.45)
    noise = rng.normal(0, 1, len(tt))
    rattle = (np.sin(2 * np.pi * 23 * tt) > 0.2).astype(float) * 0.5 + 0.5
    sig = bandpass(noise, 150, 1800) * np.exp(-tt / 0.09) * rattle
    return sig + 0.4 * np.sin(2 * np.pi * 140 * tt) * np.exp(-tt / 0.05)


def applause(rng, seconds=3.2):
    tt = t(seconds)
    sig = np.zeros_like(tt)
    clap_len = int(0.012 * RATE)
    env = np.exp(-np.arange(clap_len) / (0.0025 * RATE))
    density = 260
    for _ in range(int(density * seconds)):
        start = rng.integers(0, len(tt) - clap_len)
        burst = rng.normal(0, 1, clap_len) * env * rng.uniform(0.3, 1.0)
        sig[start:start + clap_len] += burst
    sig = bandpass(sig, 400, 5000)
    fade = np.minimum(1.0, tt / 0.25) * np.minimum(1.0, (seconds - tt) / 1.2)
    return sig * fade


def save(name, sig):
    sig = sig / (np.max(np.abs(sig)) + 1e-9) * 0.9
    pcm = (sig * 32767).astype(np.int16)
    OUT.mkdir(parents=True, exist_ok=True)
    with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as tmp:
        with wave.open(tmp.name, "wb") as w:
            w.setnchannels(1)
            w.setsampwidth(2)
            w.setframerate(RATE)
            w.writeframes(pcm.tobytes())
        subprocess.run(["ffmpeg", "-loglevel", "error", "-y", "-i", tmp.name, "-c:a", "libvorbis", "-q:a", "4",
                        str(OUT / f"{name}.ogg")], check=True)
        Path(tmp.name).unlink()


def main():
    rng = np.random.default_rng(26)
    for i, base in enumerate((1150, 1250, 1050)):
        save(f"hit{i + 1}", pock(rng, base))
    for i in range(2):
        save(f"bounce{i + 1}", bounce(rng))
    save("net", net(rng))
    save("applause", applause(rng))
    print("Sounds erzeugt in", OUT)


if __name__ == "__main__":
    main()
