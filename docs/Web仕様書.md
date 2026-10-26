# Web コンソール仕様書

本ドキュメントは [仕様書.md](仕様書.md) §13 の詳細設計である。
要件は [要件定義.md](要件定義.md) line 78-85(Discord 上で行う操作を Web で行えるようにする + 参加履歴・支払金額の確認)に基づく。

---

## 1. アーキテクチャ方針

- Bot と**同一プロセス(単一 JVM)**で Ktor HTTP サーバーを起動する。
- Web のリクエストハンドラは Bot と同じ Service 層を直接呼び出す。これにより Web からの操作(イベント編集・参加等)も同じコードパスで Discord(チャンネル / ロール / 募集メッセージ)へ**即時反映**される。Web 専用の同期機構は持たない。
- 逆方向(Discord 操作の Web への反映)は同一 DB を読むため常に最新。リアルタイム push は行わず、ページ再読み込みで反映とする。
- フロントエンドは SSR(Ktor + FreeMarker)。JS は最小限(確認ダイアログ程度)にとどめる。

### 1.1 構成図

```
┌───────────────────────┐      ┌───────────────────────────┐
│ Discord (JDA Gateway) │      │ ブラウザ (Web コンソール) │
└──────────┬────────────┘      └──────────┬────────────────┘
           │ interaction                  │ HTTP(S)
┌──────────▼──────────────────────────────▼────────────────┐
│  app コンテナ(単一 JVM)                                 │
│  ┌──────────────────┐        ┌──────────────────┐        │
│  │ bot モジュール   │        │ web モジュール   │        │
│  │ Listener →       │        │ Ktor Route       │        │
│  │ Command/Button   │        │ (Session/認可)   │        │
│  │ Handler          │        │                  │        │
│  └────────┬─────────┘        └────────┬─────────┘        │
│           └───────────┬───────────────┘                  │
│                ┌──────▼──────┐                           │
│                │   Service   │  core モジュール          │
│                └──┬───────┬──┘                           │
│      ┌────────────▼──┐  ┌─▼───────────┐                  │
│      │ DiscordGateway│  │ Repository  │                  │
│      │ (JDA 実装)    │  │ (Exposed)   │                  │
│      └───────────────┘  └──────┬──────┘                  │
└────────────────────────────────┼─────────────────────────┘
                                 │ JDBC
                      ┌──────────▼──────────┐
                      │ PostgreSQL コンテナ │
                      └─────────────────────┘
```

### 1.2 モジュール分割

Gradle マルチモジュールとし、依存方向をコンパイル時に強制する。

| モジュール | 内容 | 依存先 |
| --- | --- | --- |
| `core` | Service / Repository / domain / `DiscordGateway` インターフェース | — |
| `bot` | JDA リスナ・コマンド・ボタンハンドラ、`DiscordGateway` の JDA 実装 | core |
| `web` | Ktor ルート・セッション・テンプレート | core |
| `app` | エントリポイント(Flyway → Koin → JDA → Ktor の順で起動) | core, bot, web |

- **web モジュールは JDA に直接依存しない。** Discord 情報(メンバー確認・ロール判定・表示名解決・チャンネル一覧)はすべて core の `DiscordGateway` インターフェース経由で取得する。
- 将来 Web を別サービスに分離する場合は、Service 層の境界に API を切ることで対応可能な構造とする。

### 1.3 起動・停止順序

1. Flyway マイグレーション適用
2. JDA 起動(ready 待ち)
3. Ktor 起動(`WEB_PORT`、既定 8080)

停止時は逆順(Ktor stop → JDA shutdown)。

---

## 2. 技術スタック(追加分)

| 項目 | 採用技術 | 備考 |
| --- | --- | --- |
| Web サーバー | Ktor 3.x (Netty embedded) | Bot と同一 JVM で起動 |
| テンプレート | FreeMarker | `ktor-server-freemarker` |
| セッション | ktor-server-sessions | 暗号化 + 署名付き Cookie |
| OAuth2 | Discord OAuth2(認可コードフロー) | トークン交換は Ktor Client |
| CSRF 対策 | SameSite=Lax + Origin/Referer 検証 | Ktor CSRF プラグイン |

---

## 3. 認証(Discord OAuth2)

### 3.1 フロー

1. `GET /login` — 「Discord でログイン」ボタンを表示。
2. `GET /oauth/login` — ランダムな `state` を発行して一時 Cookie に保存し、Discord 認可 URL へリダイレクト。スコープは **`identify` のみ**。
3. ユーザーが Discord 上で承認。
4. `GET /oauth/callback?code&state` — `state` を検証 → 認可コードをアクセストークンに交換 → `GET /api/users/@me` で user id / 表示名 / アバターを取得。
5. セッションを発行し `/` へリダイレクト。

> **設計判断: `guilds` スコープは使わない。**
> 所属ギルド判定・ロール判定はすべて Bot 権限の JDA(DiscordGateway)で行う。
> そのためユーザーのアクセストークンはログイン処理完了後に即時破棄し、保存しない(リフレッシュトークン管理が不要になる)。

### 3.2 セッション

- 内容: `userId`, `displayName`, `avatarHash`, `issuedAt`
- 保存方式: 暗号化 + 署名付き Cookie(`SessionTransportTransformerEncrypt`)。サーバー側ストア不要のため、プロセス再起動をまたいでセッションが維持される。
- Cookie 属性: `HttpOnly` / `Secure`(本番)/ `SameSite=Lax`
- 有効期限: 7 日(絶対期限)。期限切れは再ログイン。
- **ロール・権限情報はセッションに保存しない。** 毎リクエスト JDA キャッシュから解決するため、Discord 側でロールを付与・剥奪すると即座に Web の権限にも反映される。

### 3.3 ギルドスコープの解決

`/g/{guildId}/**` 配下は認証必須。リクエストごとに以下を行う共通処理(Ktor の Route インターセプタ)を置く:

1. セッション検証(未認証は `/login` へリダイレクト)。
2. `DiscordGateway.findMember(guildId, userId)` で対象ギルドのメンバーであることを確認(非メンバーは 403)。
3. メンバー情報(admin-role 保持有無を含む)をリクエスト属性に格納してハンドラへ渡す。

`GET /` のギルド一覧は「Bot 導入済みギルド ∩ ユーザー所属ギルド」。1 件のみの場合はそのギルドへ自動遷移する。

---

## 4. 権限モデル

操作権限は仕様書 §8 の権限モデルをそのまま適用する(判定ロジックは core で共通実装し、Discord / Web 両ハンドラから使う)。

Web 固有の**閲覧範囲**(要件 line 82-85):

| データ | 一般参加者 | 幹事 | 運営 |
| --- | --- | --- | --- |
| 募集中イベントの一覧・概要 | ○(公開情報) | ○ | ○ |
| アーカイブ済イベント | 自分が参加していたもののみ | 左記 + 自分が幹事のもの | 全て |
| 参加者一覧 | 自分が参加中のイベントのみ(非参加イベントは人数のみ) | 自分のイベント + 参加イベント | 全て |
| 飲み会・会計金額・受領記録 | 自分の支払記録のみ | 自分のイベントの全記録(登録可) | 全て(登録可) |
| 設定画面 | × | × | ○ |

- 認可判定は必ずサーバー側で行う。テンプレート上のボタン非表示は UX 目的であり、認可の代わりにしない。
- URL の ID 直打ち(推測)によるアクセスは、ギルドメンバーシップ確認(§3.3)+ 上記閲覧範囲の判定で防ぐ。

---

## 5. 画面・ルーティング

すべての更新系は `POST` + 303 リダイレクト(PRG パターン)とする。

### 5.1 認証

| メソッド / パス | 内容 | 認証 |
| --- | --- | --- |
| `GET /login` | ログイン画面 | 不要 |
| `GET /oauth/login` | Discord 認可画面へリダイレクト | 不要 |
| `GET /oauth/callback` | コード交換 → セッション発行 | 不要 |
| `POST /logout` | セッション破棄 → `/login` へ | 要 |

### 5.2 ギルド選択

| メソッド / パス | 内容 | 実行者 |
| --- | --- | --- |
| `GET /` | ギルド選択(1 件のみなら自動遷移) | ログイン済み全員 |

### 5.3 イベント(`/g/{guildId}` 配下)

| メソッド / パス | 内容 | 実行者 |
| --- | --- | --- |
| `GET  /g/{gid}/events` | イベント一覧(`?status=recruiting/archived`) | 全員(閲覧範囲は §4) |
| `GET  /g/{gid}/events/new` | 作成フォーム | 全員 |
| `POST /g/{gid}/events` | イベント作成 | 全員 |
| `GET  /g/{gid}/events/{eid}` | イベント詳細 | §4 の閲覧範囲 |
| `POST /g/{gid}/events/{eid}/edit` | イベント編集 | 幹事 / 運営 |
| `POST /g/{gid}/events/{eid}/join` | 参加 | 全員 |
| `POST /g/{gid}/events/{eid}/leave` | 参加キャンセル | 参加者(幹事は不可) |
| `POST /g/{gid}/events/{eid}/archive` | アーカイブ化 | 幹事 / 運営 |
| `GET  /g/{gid}/events/{eid}/delete` | 削除確認画面 | 運営 |
| `POST /g/{gid}/events/{eid}/delete` | 削除実行 | 運営 |
| `POST /g/{gid}/events/{eid}/renumber` | 第 n 回の手動修正 | 運営 |
| `POST /g/{gid}/events/{eid}/add-user` | 参加者を追加(ギルドメンバーから選択) | 運営 |
| `POST /g/{gid}/events/{eid}/remove-user` | 参加者を削除(幹事は不可) | 運営 |
| `POST /g/{gid}/events/{eid}/guests` | ゲストを追加(削除済みと同名なら復活) | 幹事 / 運営 |
| `POST /g/{gid}/events/{eid}/guests/{guestId}/rename` | ゲスト名の変更 | 幹事 / 運営 |
| `POST /g/{gid}/events/{eid}/guests/{guestId}/delete` | ゲストを削除(論理削除。出欠・受領記録は残る) | 幹事 / 運営 |

### 5.4 飲み会・支払

| メソッド / パス | 内容 | 実行者 |
| --- | --- | --- |
| `POST /g/{gid}/events/{eid}/parties` | 飲み会(お店)追加 | 幹事 / 運営 |
| `POST /g/{gid}/parties/{pid}/edit` | 店名・会計金額の更新 | 幹事 / 運営 |
| `POST /g/{gid}/parties/{pid}/delete` | 飲み会(お店)の削除(出欠・受領記録も削除) | 幹事 / 運営 |
| `POST /g/{gid}/parties/{pid}/payments` | 受領記録の追加 | 幹事 / 運営 |
| `GET  /g/{gid}/parties/{pid}/check-in` | 出席登録画面(大人数向け) | 幹事 / 運営 |
| `POST /g/{gid}/parties/{pid}/check-in` | 出席登録の保存(画面表示後に変更した参加者のみ出席 / 未登録を反映) | 幹事 / 運営 |

### 5.5 マイページ・設定

| メソッド / パス | 内容 | 実行者 |
| --- | --- | --- |
| `GET  /g/{gid}/me` | 自分の参加履歴・支払履歴 | 全員 |
| `GET  /g/{gid}/settings` | 設定表示 | 運営 |
| `POST /g/{gid}/settings` | 設定変更 | 運営 |

### 5.6 各画面の内容

| 画面 | 内容 |
| --- | --- |
| ログイン | Discord ログインボタンのみ |
| ギルド選択 | Bot 導入済み ∩ 所属ギルドの一覧 |
| イベント一覧 | 募集中 / アーカイブのタブ。行 = 第 n 回・名前・エリア・日付・参加人数(ゲスト込み・内訳付き)・状態。作成ボタン |
| イベント作成 / 編集フォーム | イベント名・エリア・日付。文字数上限は仕様書 §12 に準拠してバリデーション |
| イベント詳細 | 概要(Embed 相当)+ 参加 / キャンセルボタン + 参加者一覧(運営は各行に削除ボタン)+ 参加者追加フォーム(運営・ギルドメンバーのドロップダウン)+ ゲスト編集(幹事・運営。追加・名前変更・削除)+ 飲み会一覧(店名・会計金額)+ 受領記録と合計(幹事・運営)+ 運営メニュー(削除・採番修正) |
| 削除確認 | 専用確認画面(GET)→ POST で実行。要件 line 61 の確認ダイアログに相当 |
| 出席登録 | お店単位。参加者全員のトグル(出席 / 未登録)をタイル表示し、保存ボタンで一括反映。お店の切替タブ・出席数の集計・名前検索・未登録 / 出席の絞り込み・未登録を全員出席にするボタン。欠席の記録と受領金額はイベント詳細で扱う |
| マイページ | 参加履歴(イベント名・日付・状態)と自分の支払記録・合計 |
| 設定 | 各設定項目を Discord のカテゴリ / ロール / チャンネルのドロップダウンで選択(候補は DiscordGateway から取得) |

- 参加者一覧・受領記録のユーザー表示名は `DiscordGateway` でスノーフレーク ID から解決する(Discord 側のメンション表示に相当)。ゲスト(負の疑似 ID)は `RosterService` が `event_guests` から「山田 (ゲスト)」として解決する。

---

## 6. Web 操作の Discord への反映

Web からの操作は仕様書 §10 の各処理と**同一の Service メソッド**を呼ぶだけであり、補償処理(§10.2)も同様に適用される。

| Web 操作 | DB | Discord への反映 |
| --- | --- | --- |
| イベント作成 | events ほか | チャンネル・ロール作成、募集メッセージ投稿、幹事へロール付与 |
| イベント編集 | events | 募集メッセージの Embed 更新(チャンネル名の再命名は仕様書 §16 の検討事項) |
| 参加 | event_participants | ロール付与 |
| 参加キャンセル | event_participants | ロール剥奪 |
| アーカイブ化 | events.status | チャンネル移動・送信権限剥奪・参加ボタン無効化 |
| 削除 | events(CASCADE) | チャンネル / ロール / 募集メッセージ削除 + 監査ログ出力 |
| 飲み会・受領記録の登録 | parties / payments | なし |
| 設定変更 | guild_settings | なし |

- Web 経由のイベント削除・採番修正も `log-channel` の監査ログに出力する。ログには実行経路(Discord / Web)を含める。

---

## 7. エラーハンドリング

- ドメイン例外(権限なし / カテゴリ上限 / 初期設定未完了 等)は Ktor の StatusPages プラグインで捕捉し、エラーページまたはフォーム再描画 + メッセージで表示する。
- バリデーションエラーはフォームを再描画し項目別にメッセージを表示する。
- 403 / 404 / 500 の共通エラーページを用意する。

---

## 8. セキュリティ

- **CSRF**: セッション Cookie を `SameSite=Lax` とし、さらに全 POST で Origin/Referer ヘッダを検証(Ktor CSRF プラグイン)。
- **セッション**: 暗号化(AES)+ 署名付き Cookie。キーは環境変数から供給。
- **HTTPS**: 本番はリバースプロキシで TLS 終端し、Ktor 側は `XForwardedHeaders` プラグインを有効化。`Secure` Cookie を使用。
- **認可**: §4 の通り全ルートでサーバー側判定。

---

## 9. 環境変数(追加分)

| 変数 | 内容 |
| --- | --- |
| `DISCORD_CLIENT_ID` / `DISCORD_CLIENT_SECRET` | OAuth2 クライアント認証情報(Developer Portal で取得) |
| `PUBLIC_BASE_URL` | Web コンソールの公開 URL。redirect_uri は `{PUBLIC_BASE_URL}/oauth/callback` |
| `SESSION_ENCRYPT_KEY` | セッション Cookie 暗号化キー(hex) |
| `SESSION_SIGN_KEY` | セッション Cookie 署名キー(hex) |
| `WEB_PORT` | Web サーバーのポート(既定 8080) |

- `{PUBLIC_BASE_URL}/oauth/callback` を Discord Developer Portal の Redirects に登録すること。

---

## 10. ディレクトリ構成(web モジュール)

```
web/src/main/kotlin/com/example/nomibot/web/
├── WebServer.kt          # Ktor 起動・プラグイン設定
├── plugins/              # Sessions / CSRF / StatusPages / FreeMarker 設定
├── auth/                 # OAuth2 フロー・セッションデータ定義
├── routes/
│   ├── AuthRoutes.kt
│   ├── GuildRoutes.kt
│   ├── EventRoutes.kt
│   ├── PartyRoutes.kt
│   ├── PaymentRoutes.kt
│   ├── MyPageRoutes.kt
│   └── SettingsRoutes.kt
└── viewmodel/            # テンプレートへ渡す表示用モデル
web/src/main/resources/
├── templates/            # layout.ftl, login.ftl, guild-select.ftl,
│                         # event-list.ftl, event-detail.ftl, event-form.ftl,
│                         # event-delete-confirm.ftl, mypage.ftl, settings.ftl, error.ftl
└── static/               # CSS 等
```

### DiscordGateway に追加するメソッド(web が利用)

| メソッド | 用途 |
| --- | --- |
| `findMember(guildId, userId)` | メンバー確認・admin-role 判定(§3.3) |
| `listMutualGuilds(userId)` | ギルド選択画面 |
| `resolveDisplayNames(guildId, userIds)` | 参加者一覧・受領記録の表示名解決 |
| `listCategories / listRoles / listTextChannels(guildId)` | 設定画面のドロップダウン候補 |
| `listMembers(guildId)` | イベント参加者追加のドロップダウン候補(GUILD_MEMBERS 特権インテントが必要) |

---

## 11. 未確定事項

- 受領記録(payments)の修正・削除機能(Discord 側にも存在しないため、追加する場合は両経路に実装する)。
- 参加人数等のリアルタイム更新(現状はページ再読み込みで反映)。

---

## 12. 公開経路(Cloudflare Tunnel)

- リバースプロキシ / HTTPS 終端は **Cloudflare Tunnel(Named Tunnel)** を採用する。
- `cloudflared` を docker-compose の別サービスとして起動し、Docker ネットワーク内で `bot:8080` に到達させる(ホスト側のポート開放は不要)。
- Tunnel の Public Hostname 設定(Cloudflare Zero Trust ダッシュボード)で `<サブドメイン>.<ドメイン>` → `http://bot:8080` を紐付ける。
- `PUBLIC_BASE_URL` は Tunnel の公開ホスト名(`https://` 固定)に合わせ、`{PUBLIC_BASE_URL}/oauth/callback` を Discord Developer Portal の Redirects に登録する。
- `cloudflared` の認証情報は Tunnel Token(`CLOUDFLARE_TUNNEL_TOKEN`)のみで、`.env` で供給する(`.gitignore` 済み)。
- `XForwardedHeaders` は既に有効(`WebServer.kt`)なので、Cloudflare 経由で付与される `X-Forwarded-Proto: https` が Cookie の `Secure` 判定・CSRF の Origin 検証にそのまま反映される。
