use std::{
    fmt::Display,
    io::{self, Write},
};

use colored::Colorize;
use rdev::{Key, Button};
use rust_i18n::{i18n, t};

use crate::{InputData, KeyType};

i18n!("src/locales");

pub(crate) trait StringToKey {
    fn to_key(self) -> InputData;
}

impl StringToKey for String {
    fn to_key(self) -> InputData {
        let mut data: InputData = InputData {
            key_type: KeyType::Keyboard,
            keyboard: Key::Unknown(0),
            mouse_button: Button::Unknown(0)
        };
        data.keyboard = match self.as_str() {
            "alt" => Key::Alt,
            "alt_gr" => Key::AltGr,
            "backspace" => Key::Backspace,
            "caps_lock" => Key::CapsLock,
            "ctrl_left" => Key::ControlLeft,
            "ctrl_right" => Key::ControlRight,
            "delete" => Key::Delete,
            "down" => Key::DownArrow,
            "end" => Key::End,
            "esc" => Key::Escape,
            "f1" => Key::F1,
            "f10" => Key::F10,
            "f11" => Key::F11,
            "f12" => Key::F12,
            "f2" => Key::F2,
            "f3" => Key::F3,
            "f4" => Key::F4,
            "f5" => Key::F5,
            "f6" => Key::F6,
            "f7" => Key::F7,
            "f8" => Key::F8,
            "f9" => Key::F9,
            "home" => Key::Home,
            "left" => Key::LeftArrow,
            "page_down" => Key::PageDown,
            "page_up" => Key::PageUp,
            "enter" => Key::Return,
            "right" => Key::RightArrow,
            "shift_left" => Key::ShiftLeft,
            "shift_right" => Key::ShiftRight,
            "space" => Key::Space,
            "tab" => Key::Tab,
            "up" => Key::UpArrow,
            "print_screen" => Key::PrintScreen,
            "scroll_lock" => Key::ScrollLock,
            "pause" => Key::Pause,
            "num_lock" => Key::NumLock,
            "`" => Key::BackQuote,
            "1" => Key::Num1,
            "2" => Key::Num2,
            "3" => Key::Num3,
            "4" => Key::Num4,
            "5" => Key::Num5,
            "6" => Key::Num6,
            "7" => Key::Num7,
            "8" => Key::Num8,
            "9" => Key::Num9,
            "0" => Key::Num0,
            "-" => Key::Minus,
            "=" => Key::Equal,
            "q" => Key::KeyQ,
            "w" => Key::KeyW,
            "e" => Key::KeyE,
            "r" => Key::KeyR,
            "t" => Key::KeyT,
            "y" => Key::KeyY,
            "u" => Key::KeyU,
            "i" => Key::KeyI,
            "o" => Key::KeyO,
            "p" => Key::KeyP,
            "[" => Key::LeftBracket,
            "]" => Key::RightBracket,
            "a" => Key::KeyA,
            "s" => Key::KeyS,
            "d" => Key::KeyD,
            "f" => Key::KeyF,
            "g" => Key::KeyG,
            "h" => Key::KeyH,
            "j" => Key::KeyJ,
            "k" => Key::KeyK,
            "l" => Key::KeyL,
            ";" => Key::SemiColon,
            "'" => Key::Quote,
            "\\" => Key::BackSlash,
            "z" => Key::KeyZ,
            "x" => Key::KeyX,
            "c" => Key::KeyC,
            "v" => Key::KeyV,
            "b" => Key::KeyB,
            "n" => Key::KeyN,
            "m" => Key::KeyM,
            "," => Key::Comma,
            "." => Key::Dot,
            "/" => Key::Slash,
            "insert" => Key::Insert,
            //"kp_enter" => Key::KpReturn,
            "kp-" => Key::KpMinus,
            "kp+" => Key::KpPlus,
            "kp*" => Key::KpMultiply,
            "kp/" => Key::KpDivide,
            "kp0" => Key::Kp0,
            "kp1" => Key::Kp1,
            "kp2" => Key::Kp2,
            "kp3" => Key::Kp3,
            "kp4" => Key::Kp4,
            "kp5" => Key::Kp5,
            "kp6" => Key::Kp6,
            "kp7" => Key::Kp7,
            "kp8" => Key::Kp8,
            "kp9" => Key::Kp9,
            "kp_delete" => Key::KpDelete,
            "fn" => Key::Function,
            _ => Key::Unknown(0),
        };
        if data.keyboard == Key::Unknown(0) {
            data.key_type = KeyType::MouseButton;
            data.mouse_button = match self.as_str() {
                "mouse_left" => Button::Left,
                "mouse_right" => Button::Right,
                "mouse_middle" => Button::Middle,
                "mouse3" => Button::Unknown(2),
                "mouse4" => Button::Unknown(1),
                "mouse5" => Button::Unknown(3),
                _ => Button::Unknown(0),
            };
        }
        if data.keyboard == Key::Unknown(0) && data.mouse_button == Button::Unknown(0) {
            data.key_type = match self.as_str() {
                "wheel_up" => KeyType::WheelUp,
                "wheel_down" => KeyType::WheelDown,
                _ => KeyType::Keyboard
            }
        }
        data
    }
}

pub fn error<T: Display>(str: T) {
    println!("{}{}", t!("n_err").red(), str)
}

pub fn warning<T: Display>(str: T) {
    println!("{}{}", t!("n_warn").yellow(), str)
}

pub fn info<T: Display>(str: T) {
    println!("{}{}", t!("n_info").blue(), str)
}

pub fn print<T: Display>(str: T) {
    print!("{}", str);
    let _ = io::stdout().flush();
}

pub fn println<T: Display>(str: T) {
    println!("{}", str)
}

pub fn debug_log<T: Display>(str: T) {
    print!("{}{}", t!("n_debug").magenta(), str)
}

pub fn compare_ver(ver_a: &str, ver_b: &str) -> bool {
    let mut a_split = ver_a.split(".");
    let mut b_split = ver_b.split(".");
    if a_split.next() == b_split.next() {
        if a_split.next() == b_split.next() {
            return true
        }
    }
    return false
}