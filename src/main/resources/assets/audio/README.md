# Audio assets

Sound effects loaded by `AudioManager` (`com.kurawler.engine.AudioManager`).

## Format — IMPORTANT
- Must be **WAV (PCM)**. The engine uses `javax.sound.sampled`, which does **NOT** play
  `.mp3` or `.ogg`. If you generated MP3/OGG, convert to WAV first, e.g.:
  `ffmpeg -i input.mp3 -ar 44100 -ac 2 -acodec pcm_s16le output.wav`
- A missing file just falls back to the system beep — the game still runs.

## Expected file names (drop these here)
| game event | file name |
|---|---|
| UI button click | `button.wav` |
| attack / hit | `attack.wav` |
| pick up item | `item.wav` |
| enemy defeated | `enemy_down.wav` |
| game over | `game_over.wav` |
| victory | `victory.wav` |

Place the generated files in this folder with exactly these names and they will play
automatically (no code change needed). To add new sound keys or background music, the
`sounds` map / playback in `AudioManager` would need extending — tell me if you want that.
