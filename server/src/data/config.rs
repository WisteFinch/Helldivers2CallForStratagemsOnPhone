use serde::{Deserialize, Serialize};

use crate::data::auth::Auth;

// 为了兼容性保留API5的配置结构
// To comply with the JSON specification, ignore non_snake_case warnings.
#[allow(non_snake_case)]
#[derive(Serialize, Deserialize, Clone)]
pub struct AppConfig5 {
    pub port: u64,
    pub delay: u64,
    pub open: String,
    pub openType: String,
    pub up: String,
    pub down: String,
    pub left: String,
    pub right: String,
    pub ip: String,
}

impl Default for AppConfig5 {
    fn default() -> Self {
        Self {
            port: 23333,
            delay: 25,
            open: String::from("ctrl_left"),
            openType: String::from("hold"),
            up: String::from("w"),
            down: String::from("s"),
            left: String::from("a"),
            right: String::from("d"),
            ip: String::from(""),
        }
    }
}

// 新的合并配置结构
#[derive(Serialize, Deserialize, Clone)]
pub struct AppConfig {
    pub server: ServerConfig,
    pub auth: AuthConfig,
    pub input: InputConfig,
    pub records: Vec<Auth>,
    pub debug: bool,
}

#[derive(Serialize, Deserialize, Clone)]
pub struct ServerConfig {
    pub port: u64,
    pub ip: String,
}

#[derive(Serialize, Deserialize, Clone)]
pub struct AuthConfig {
    pub enabled: bool,
    pub timeout: u64,
}

#[derive(Serialize, Deserialize, Clone)]
pub struct InputConfig {
    pub delay: u64,
    pub open: String,
    pub keytype: String,
    pub up: String,
    pub down: String,
    pub left: String,
    pub right: String,
}

impl Default for AppConfig {
    fn default() -> Self {
        Self {
            server: ServerConfig {
                port: 23333,
                ip: String::from(""),
            },
            auth: AuthConfig {
                enabled: true,
                timeout: 3,
            },
            input: InputConfig {
                delay: 25,
                open: String::from("ctrl_left"),
                keytype: String::from("hold"),
                up: String::from("w"),
                down: String::from("s"),
                left: String::from("a"),
                right: String::from("d"),
            },
            records: Vec::new(),
            debug: false,
        }
    }
}

// 从旧配置转换到新配置
impl From<AppConfig5> for AppConfig {
    fn from(old_config: AppConfig5) -> Self {
        Self {
            server: ServerConfig {
                port: old_config.port,
                ip: old_config.ip,
            },
            auth: AuthConfig {
                enabled: true,
                timeout: 3,
            },
            input: InputConfig {
                delay: old_config.delay,
                open: old_config.open,
                keytype: old_config.openType,
                up: old_config.up,
                down: old_config.down,
                left: old_config.left,
                right: old_config.right,
            },
            records: Vec::new(),
            debug: false,
        }
    }
}