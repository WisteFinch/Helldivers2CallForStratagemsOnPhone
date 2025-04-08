use server::modules::net::run;
use clap::Parser;

/// 地狱潜者2 - 战略配备呼叫服务器
#[derive(Parser, Debug)]
#[command(version, about, long_about = None)]
struct Args {
    /// 启用调试模式
    #[arg(long)]
    debug: bool,
    
    /// 禁用SID验证
    #[arg(long = "disable-auth")]
    disable_auth: bool,
}

#[tokio::main]
async fn main() {
    let args = Args::parse();
    run(args.debug, args.disable_auth).await.unwrap()
}
