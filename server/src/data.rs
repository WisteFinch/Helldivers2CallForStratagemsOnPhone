use std::fmt;
use serde::{Deserialize, Serialize};

pub enum InputType {
    Click = 0,
    Press = 1,
    Release = 2
}

impl InputType {
    pub fn from_u64(v: u64) -> Self {
        if v == 1 {
            Self::Press
        }
        else if v == 2 {
            Self::Release
        }
        else {
            Self::Click
        }
    }
}

pub enum Step {
    Open = 0,
    Up = 1,
    Down = 2,
    Left = 3,
    Right = 4
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
            Self::Open
        }
    }
}

impl fmt::Display for Step {
    fn fmt(&self, f: &mut fmt::Formatter) -> fmt::Result {
        match self {
            Self::Open => write!(f, "open"),
            Self::Up => write!(f, "↑"),
            Self::Down => write!(f, "↓"),
            Self::Left => write!(f, "←"),
            Self::Right => write!(f, "→"),
        }
    }
}

pub enum Operation {
    Status = 0,
    Combined = 1,
    Independent = 2,
    Request = 3,
    Sync = 4,
}

impl Operation {
    pub fn from_u64(v: u64) -> Self {
        if v == 1 {
            Self::Combined
        }
        else if v == 2 {
            Self::Independent
        }
        else if v == 3 {
            Self::Request
        }
        else if v == 4 {
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
            port: 23333,
            delay: 25,
            open: "ctrl_left".to_string(),
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