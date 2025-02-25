use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Config {
    pub port: u64,
    pub delay: u64,
    pub open: String,
    pub open_type: String,
    pub up: String,
    pub down: String,
    pub left: String,
    pub right: String,
    pub ip: String,
}

impl Default for Config {
    fn default() -> Self {
        Self {
            port: 23333,
            delay: 25,
            open: String::from("ctrl_left"),
            open_type: String::from("hold"),
            up: String::from("w"),
            down: String::from("s"),
            left: String::from("a"),
            right: String::from("d"),
            ip: String::from(""),
        }
    }
}

#[derive(Serialize, Deserialize, Clone)]
pub struct Auth {
    pub sid: String,
    pub time: u64,
}
