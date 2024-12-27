use std::{
    fmt::Display,
    io::{self, Write},
};

pub fn print<T: Display>(str: T) {
    print!("{}", str);
    let _ = io::stdout().flush();
}

pub fn println<T: Display>(str: T) {
    println!("{}", str)
}

pub fn compare_ver(ver_a: &str, ver_b: &str) -> bool {
    let mut a_split = ver_a.split(".");
    let mut b_split = ver_b.split(".");
    if a_split.next() == b_split.next() && a_split.next() == b_split.next() {
        return true;
    }

    false
}
