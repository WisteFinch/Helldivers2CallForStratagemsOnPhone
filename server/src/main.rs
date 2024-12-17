use server::*;
use std::env;

#[tokio::main]
async fn main() {
    let args: Vec<String> = env::args().collect();
    let mut debug = false;
    for arg in args {
        if arg.to_lowercase() == "debug" {
            debug = true;
        }
    }
    run(debug).await.unwrap()
}
