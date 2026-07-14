# MultiAccount

同一の実Mojangアカウントで複数クライアントから同時ログインできるようにする、**デバッグ・検証用**の Velocity + Paper プラグインです。`online-mode: true` を維持したまま、Mojangの本人認証を毎回通しつつ、2人目以降を `Username_02`, `Username_03`... のような仮想プロフィール（最大10重複）としてサーバーに参加させます。

> [!WARNING]
> **本プラグインはプラグイン開発・動作検証を効率化するためのデバッグツールです。**
> 一般公開サーバーでの常用や、規約違反・不正行為を目的とした利用は想定していません。
> 利用する場合は、対象サーバー・アカウントの利用規約（Mojang/Microsoftのエンドユーザー使用許諾契約を含む）を必ず確認し、自己責任で行ってください。
> 作者は本プラグインの利用によって生じたいかなる損害・アカウント停止等についても責任を負いません（**無保証・NO WARRANTY**）。

## 特徴

- `online-mode: true` を維持したまま多重ログインを実現（Mojang本人認証は毎回通す）
- スキンの複製（本物のtexturesプロパティを仮想プロフィールにコピー）
- OP権限の同期（LuckPerms等の権限プラグインとは連携せず、OPのみ対応）
- ホワイトリスト方式（許可したユーザーのみ多重ログイン可能）
- 多重ログイン中ユーザーの一覧表示コマンド
- ホワイトリスト編集コマンド（Velocity/Paper両方から実行可能。Paperからはプラグインメッセージングで転送）

## 動作環境

- Velocity（プロキシ）
- Paper 1.21.x 系（Purpur等のPaperフォーク互換）
- Java 21
- `player-info-forwarding-mode: modern` が必須（Legacy/BungeeGuard等では動作しません）

## 導入

1. `./gradlew build` でビルド
2. `velocity/build/libs/multiaccount-velocity-*.jar` を Velocity の `plugins/` へ配置
3. `paper/build/libs/multiaccount-paper-*.jar` を Paper サーバーの `plugins/` へ配置
4. 両サーバーを起動し、生成された `config.yml`（Velocity側は `whitelist.yml` も）を編集

## 設定

### Velocity側 `config.yml`

```yaml
max-duplicates: 10          # 最大重複数(本人+複製)
suffix-format: "_%02d"      # 複製名のSuffix書式
whitelist-file: "whitelist.yml"
admins: []                  # 管理コマンドを実行できる管理者UUID
bridge-channel: "multiaccount:main"
bridge-timeout-seconds: 5
reconcile-interval-seconds: 30
```

### Paper側 `config.yml`

```yaml
suffix-regex: "_(\\d{2})$"  # Velocity側のsuffix-formatに対応させる
enable-op-sync: true
bridge-channel: "multiaccount:main"
bridge-timeout-seconds: 5
```

## コマンド

`/multiaccount`（エイリアス: `/ma`）は Velocity 上のコマンドが主体で、Paper上の同名コマンドはプラグインメッセージング経由で Velocity へ処理を転送します（**Paperコンソールからの実行は非対応**）。

| コマンド | 説明 |
|---|---|
| `/multiaccount list` | 多重ログイン中のセッション一覧を表示 |
| `/multiaccount whitelist list` | ホワイトリスト一覧を表示 |
| `/multiaccount whitelist add <player\|uuid>` | ホワイトリストに追加(即時保存) |
| `/multiaccount whitelist remove <player\|uuid>` | ホワイトリストから削除(即時保存) |
| `/multiaccount help` | ヘルプ表示 |

## 既知の注意点

- Minecraftの名前長制限（16文字）を超える複製名はスキップされます
- サードパーティのアンチボット/セキュリティプラグインによっては、複製プロフィールのスキン表示が拒否される可能性があります
- ホワイトリスト非対象ユーザーの多重ログインは、Velocity標準の重複接続処理（`already connected`)により拒否されます

## ライセンス

このリポジトリに明示的なライセンス表記がない場合、全著作権を作者が保持します（無断転載・再配布不可）。
