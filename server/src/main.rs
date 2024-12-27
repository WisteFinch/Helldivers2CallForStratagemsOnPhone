use std::env;

use log::warn;
use rust_i18n::t;

use server::*;

#[tokio::main]
async fn main() {
    let args: Vec<String> = env::args().collect();
    let mut debug = false;
    for arg in args {
        if arg.to_lowercase() == "debug" {
            debug = true;
        }
    }

    // Initialize logger
    if debug {
        logger::initialize(log::LevelFilter::Debug);
        warn!("{}", t!("debug_mode"));
    } else {
        logger::initialize(log::LevelFilter::Info);
    }

    run().await.unwrap()
}
