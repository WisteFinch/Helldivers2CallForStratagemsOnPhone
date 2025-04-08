<div align="center">
  
# Helldivers 2 - Call for stratagems on phone

[**English**](./README.md) | [**中文**](./README_zh_CN.md)

[![下载](https://img.shields.io/github/v/release/WisteFinch/Helldivers2CallForStratagemsOnPhone)](https://github.com/WisteFinch/Helldivers2CallForStratagemsOnPhone/releases/latest)
[![许可证](https://img.shields.io/github/license/WisteFinch/Helldivers2CallForStratagemsOnPhone)](https://github.com/WisteFinch/Helldivers2CallForStratagemsOnPhone/blob/main/LICENSE)

把手机绑在手臂上，通过手机呼叫战略配备，做真正的绝地潜兵！

</div>

软件用法可以[看这里](./usage_zh_CN.md)

## 特色

- 自由组合战略配备 ✅
- 根据战略配备的提示输入 ✅
- 自由输入 ✅
- 宏 ✅
- 反转输入

## 构建

### 服务器

1. 需要安装Rust，可以在这里下载 <https://www.rust-lang.org/tools/install>
2. 进入`./server`文件夹。
3. 使用下面的命令来编译运行。

``` shell
cargo run
```

您可以添加以下命令行参数：
- `--debug`：启用调试模式，将打印更多日志
- `--disable-auth`：禁用SID认证，客户端将自动通过认证
- `-h`, `--help`：显示帮助信息
- `-V`, `--version`：显示版本信息

``` shell
cargo run -- --disable-auth  # 无需SID认证运行
cargo run -- --debug  # 使用调试模式运行
cargo run -- --debug --disable-auth  # 同时启用调试模式和禁用SID认证
cargo run -- --help  # 显示帮助信息
```

### 配置文件

配置文件格式在0.6.0版本中已从JSON更改为TOML格式。当您首次运行服务器时，程序将检测旧的配置文件并询问您是否要迁移到新格式。**迁移是必须的**，新版本必须使用新格式配置才能继续运行。确认后，旧的配置和认证记录将自动迁移到新格式，并删除旧文件。

新配置文件示例(config.toml)：

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

# 认证记录
[[auth_records]]
sid = "client_identification_1"
time = 1712345678

[[auth_records]]
sid = "client_identification_2"
time = 1712345679
```

服务器API请查阅[此处](./server_api_0_5.md)

### 客户端

1. 需要安装Android Studio。
2. 将`./call-for-stratagems`导入Android Studio。
3. 点击`Run`按钮开始编译。

### 相关链接

- 绝地潜兵2战备数据库 [HD2CFS-Database](https://github.com/WisteFinch/Helldivers2CallForStratagemsOnPhone)
- 绝地潜兵战备数据库 [HD2CFS-Database_HD](https://github.com/WisteFinch/HD2CFS-Database_HD)

### 使用的开源库

- [AndroidSVG](https://github.com/BigBadaboom/androidsvg) 安卓的SVG渲染库
- [ZXingLite](https://github.com/jenly1314/ZXingLite) 🔥 ZXing的精简极速版，优化扫码和生成二维码/条形码
