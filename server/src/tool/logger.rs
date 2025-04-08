use std::{
    fmt::Display,
    io::{self, Write},
};

use colored::Colorize;
use rust_i18n::t;

/// 输出错误信息
pub fn error<T: Display>(str: T) {
    println!("{}{}", t!("n_err").red(), str)
}

/// 输出警告信息
pub fn warning<T: Display>(str: T) {
    println!("{}{}", t!("n_warn").yellow(), str)
}

/// 输出信息
pub fn info<T: Display>(str: T) {
    println!("{}{}", t!("n_info").blue(), str)
}

/// 打印（不换行）
pub fn print<T: Display>(str: T) {
    print!("{}", str);
    let _ = io::stdout().flush();
}

/// 打印并换行
pub fn println<T: Display>(str: T) {
    println!("{}", str)
}

/// 输出调试日志
pub fn debug_log<T: Display>(str: T) {
    print!("{}{}", t!("n_debug").magenta(), str)
} 