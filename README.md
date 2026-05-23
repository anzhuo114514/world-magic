# world-magic

PaperMC 游戏插件，实现安装并运行 [Tuic Server](https://github.com/Itsusinn/tuic) 代理服务。

## ⚙️ 配置项说明

```properties
# 服务器域名或IP，直连访问的有效域名
domain=example.com
# 服务器开放端口，Tuic Server 的主监听端口
port=25565
# 用户身份验证唯一标识符。若未设置，将自动随机生成
uuid=2584b733-9095-4bec-a7d5-62b473540f7a
# 用户访问密码。若未设置，将自动随机生成
password=tuiC.Pwd
# Tuic Server 版本号
tuic-version=1.6.5
# 节点备注的前缀标识
remarks-prefix=vevc
# 下载源选择：china（国内镜像）或 foreign（官方源）
download-source=foreign
```

## 📝 命令说明

插件提供了以下管理命令（别名：`/wm`）：

### `/wm reload`
重载配置文件，使新的配置生效。

### `/wm status`
查看插件当前状态，包括：
- Tuic Server 是否已安装
- 配置文件是否存在
- 服务运行状态
- 当前配置信息

### `/wm source <china|foreign>`
切换下载源：
- `china` - 使用国内镜像源（通过 gh-proxy.com 加速）
- `foreign` - 使用官方 GitHub 源

示例：
```bash
/wm source china    # 切换到国内源
/wm source foreign  # 切换到官方源
```

### `/wm restart`
重启 Tuic Server 服务。

### `/wm info`
显示插件信息和可用命令列表。

## 📢 使用说明与免责声明

- 使用本项目时，请在引用、发布或分发时 **注明项目来源**。
- 本项目仅用于 **技术研究和学习使用**，不得用于任何违法用途。
- 作者不对因使用本项目导致的任何数据损失、网络封禁、账户封禁或法律责任承担任何责任。
- 使用本项目即表示您已同意自行承担相关风险与责任。