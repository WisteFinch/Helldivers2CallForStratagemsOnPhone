use serde::{Deserialize, Serialize};

pub enum Step {
    Ctrl,
    Up,
    Down,
    Left,
    Right
}

impl Step {
    pub fn from_u64(v: u64) -> Self {
        if v == 1 {
            Self::Up
        }
        else if v == 2 {
            Self::Down
        }
        else if v == 3 {
            Self::Left
        }
        else if v == 4{
            Self::Right
        }
        else {
            Self::Ctrl
        }
    }
}

pub enum Operation {
    Status,
    Simulate,
    Request,
    Sync,
}

impl Operation {
    pub fn from_u64(v: u64) -> Self {
        if v == 1 {
            Self::Simulate
        }
        else if v == 2 {
            Self::Request
        }
        else if v == 3 {
            Self::Sync
        }
        else {
            Self::Status
        }
    }
}

#[derive(Serialize, Deserialize)]
pub struct Config {
    pub port: u64,
    pub delay: u64,
    pub open: String,
    pub up: String,
    pub down: String,
    pub left: String,
    pub right: String
}

impl Config {
    pub fn default() -> Self {
        Self {
            port: 2333,
            delay: 50,
            open: "ctrl".to_string(),
            up: "w".to_string(),
            down: "s".to_string(),
            left: "a".to_string(),
            right: "d".to_string()
        }
    }
}

impl Clone for Config {
    fn clone(&self) -> Self {
        Self { 
            port: self.port,
            delay: self.delay,
            open: self.open.clone(),
            up: self.up.clone(),
            down: self.down.clone(),
            left: self.left.clone(),
            right: self.right.clone()
        }
    }
}