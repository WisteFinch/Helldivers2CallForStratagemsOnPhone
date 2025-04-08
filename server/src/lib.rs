use rust_i18n::i18n;

// 初始化国际化
i18n!("src/locales");

// 模块声明
pub mod data;
pub mod tool;
pub mod modules;

// 重新导出常用模块和函数
pub use data::*;
pub use tool::*;
// 明确导出特定的模块功能而不是使用通配符
pub use modules::net::run;
