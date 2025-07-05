use server::modules::net::run;
use clap::Parser;

/// Call for stratagem server
#[derive(Parser, Debug)]
#[command(version, about, long_about = None)]
struct Args {
    /// Enable debug mode
    #[arg(long)]
    debug: bool,
    
    /// Disable authentication
    #[arg(long = "disable-auth")]
    disable_auth: bool,
}

#[tokio::main]
async fn main() {
    let args = Args::parse();
    run(args.debug, args.disable_auth).await.unwrap()
}
