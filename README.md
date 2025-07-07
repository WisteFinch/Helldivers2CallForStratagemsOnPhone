<div align="center">
  
# Helldivers 2 - Call for stratagems on phone

[**English**](./README.md) | [**中文**](./README_zh_CN.md)

[![Download](https://img.shields.io/github/v/release/WisteFinch/Helldivers2CallForStratagemsOnPhone)](https://github.com/WisteFinch/Helldivers2CallForStratagemsOnPhone/releases/latest)
[![License](https://img.shields.io/github/license/WisteFinch/Helldivers2CallForStratagemsOnPhone)](https://github.com/WisteFinch/Helldivers2CallForStratagemsOnPhone/blob/main/LICENSE)

Tie your phone to your arm so that you can call for stratagems like a real helldiver!

</div>

You can check the usage [here](./usage.md)

## Feature

- Combine stratagems freely ✅
- Input according to the prompts of stratagems ✅
- Input freely ✅
- Macro ✅
- Speech recognition ✅

## Requirements

- Server: 
  - Windows x64 or Linux (manual compilation required)
- Client: 
  - Android: ≥8.0 (SDK26)
  - ABIs: arm64-v8a

## Build & Run

### Server

1. Rust is required, you can download it here <https://www.rust-lang.org/tools/install>
2. go to `./server` folder.
3. Use the following command to compile and run.

``` shell
cargo run
```

You can add the following command line arguments:
- `--debug`: Enable debug mode, which will print more logs
- `--disable-auth`: Disable SID authentication, client will be auto-authenticated
- `-h`, `--help`: Show help message
- `-V`, `--version`: Show version information

``` shell
cargo run -- --disable-auth  # Run without SID authentication
cargo run -- --debug  # Run with debug mode
cargo run -- --debug --disable-auth  # Run with both debug mode and without SID authentication
cargo run -- --help  # Show help message
```

You can check the server API [here](./server_api_6.md)

### Client

1. Android Studio is required.
2. Import `./call-for-stratagems` to Android Studio.
3. Click `Run` button to compile.

### Links

- Helldivers2 Stratagem Database [HD2CFS-Database](https://github.com/WisteFinch/HD2CFS-Database)
- Helldivers Stratagem Database [HD2CFS-Database_HD](https://github.com/WisteFinch/HD2CFS-Database_HD)

## Libs used

- [BigBadaboom/androidsvg](https://github.com/BigBadaboom/androidsvg) SVG rendering library for Android
- [jenly1314/ZXingLite](https://github.com/jenly1314/ZXingLite) 🔥 Streamlined and fast version of ZXing, optimizes scanning and generating QR codes/barcodes
- [k2-fsa/sherpa-ncnn](https://github.com/k2-fsa/sherpa-ncnn) Real-time speech recognition and voice activity detection (VAD) using next-gen Kaldi with ncnn without Internet connection.
