use server::*;

#[tokio::main]
async fn main() {
    println!("=== Call for stratagem server ===");
    run().await.unwrap()
}