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
- Invert Input

## Build

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

### Configuration

The configuration file has been changed from JSON to TOML format in version 0.6.0. When you first run the server, it will detect old configuration files and ask if you want to migrate them to the new format. **Migration is required** to continue using the server with new version. Upon confirmation, the old configuration and authentication records will be automatically migrated to the new format and old files will be deleted.

Example of the new configuration file (config.toml):

```toml
[server]
port = 23333
ip = ""

[auth]
enabled = true
timeout_days = 3

[input]
delay = 25
open = "ctrl_left"
open_type = "hold"
up = "w"
down = "s"
left = "a"
right = "d"

# Authentication records
[[auth_records]]
sid = "client_identification_1"
time = 1712345678

[[auth_records]]
sid = "client_identification_2"
time = 1712345679
```

### Client

1. Android Studio is required.
2. Import `./call-for-stratagems` to Android Studio.
3. Click `Run` button to compile.

### Links

- Helldivers2 Stratagem Database [HD2CFS-Database](https://github.com/WisteFinch/HD2CFS-Database)
- Helldivers Stratagem Database [HD2CFS-Database_HD](https://github.com/WisteFinch/HD2CFS-Database_HD)

### Libs used

- [AndroidSVG](https://github.com/BigBadaboom/androidsvg) SVG rendering library for Android
- [ZXingLite](https://github.com/jenly1314/ZXingLite) 🔥 Streamlined and fast version of ZXing, optimizes scanning and generating QR codes/barcodes
