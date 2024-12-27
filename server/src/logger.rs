use colored::Colorize;
use rust_i18n::t;

struct Logger;

impl log::Log for Logger {
    fn enabled(&self, _metadata: &log::Metadata) -> bool {
        true
    }

    fn log(&self, record: &log::Record) {
        match record.level() {
            log::Level::Error => println!("{}{}", t!("n_err").red(), record.args()),
            log::Level::Warn => println!("{}{}", t!("n_warn").yellow(), record.args()),
            log::Level::Info => println!("{}{}", t!("n_info").blue(), record.args()),
            log::Level::Debug => print!("{}{}", t!("n_debug").magenta(), record.args()),
            log::Level::Trace => print!("{}{}", t!("n_debug").magenta(), record.args()),
        }
    }

    fn flush(&self) {}
}

pub fn initialize(max_level: log::LevelFilter) {
    log::set_boxed_logger(Box::new(Logger)).unwrap();
    log::set_max_level(max_level);
}
