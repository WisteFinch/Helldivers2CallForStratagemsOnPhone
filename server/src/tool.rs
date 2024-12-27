use std::{
    fmt::Display,
    io::{self, Write},
};

use colored::Colorize;
use rust_i18n::t;

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
    if a_split.next() == b_split.next() && a_split.next() == b_split.next() {
        return true;
    }

    false
}
