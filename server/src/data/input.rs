use rdev::{Button, Key};
use std::fmt;

pub enum InputType {
    Click = 0,
    Press = 1,
    Release = 2,
    Begin = 3,
    End = 4,
}

impl InputType {
    pub fn from_u64(v: u64) -> Self {
        match v {
            1 => Self::Press,
            2 => Self::Release,
            3 => Self::Begin,
            4 => Self::End,
            _ => Self::Click,
        }
    }
}

pub enum KeyType {
    Keyboard = 0,
    MouseButton = 1,
    WheelUp = 2,
    WheelDown = 3,
}

pub struct InputData {
    pub key_type: KeyType,
    pub keyboard: Key,
    pub mouse_button: Button,
}

#[derive(Clone)]
pub enum Step {
    Open = 0,
    Up = 1,
    Down = 2,
    Left = 3,
    Right = 4,
}

impl Step {
    pub fn from_u64(v: u64) -> Self {
        match v {
            1 => Self::Up,
            2 => Self::Down,
            3 => Self::Left,
            4 => Self::Right,
            _ => Self::Open,
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