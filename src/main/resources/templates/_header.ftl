<#-- 呼び出し側で include 前に assign できる変数:
     nav    = "events" | "me" | "settings" … ナビゲーションの現在地
     tabbar = true … スマホ幅で画面下にタブバーを表示する(トップレベルの画面のみ)
     bare   = true … ヘッダーを出さない(ログイン画面) -->
<!DOCTYPE html>
<html lang="ja">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">
  <title>飲み会管理コンソール</title>
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
  <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Zen+Kaku+Gothic+New:wght@400;500;700&display=swap">
  <link rel="stylesheet" href="/static/style.css">
  <!-- 通常のフルリロードと同じ体験にするため、4xx/5xx のエラーページ(error.ftl)も本文を差し替える -->
  <meta name="htmx-config" content='{"responseHandling":[{"code":".*", "swap": true}]}'>
  <script src="/static/htmx.min.js"></script>
</head>
<body hx-boost="true">
<div id="progress-bar"></div>
<#if !(bare!false)>
<header class="topbar">
  <div class="topbar-inner">
    <a class="brand" href="/">
      <span class="brand-mark" aria-hidden="true">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M7 4h10l-1.2 12.5a3 3 0 0 1-3 2.5h-1.6a3 3 0 0 1-3-2.5L7 4z"/><path d="M7.6 9.5h8.8"/><path d="M12 19v2"/></svg>
      </span>
      <span class="brand-name">飲み会管理コンソール</span>
    </a>
    <#if guildId??>
      <a class="guild-switch" href="/" title="サーバーを切り替える">
        <span class="guild-switch-name">${guildName!"サーバー"}</span>
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M6 9l6 6 6-6"/></svg>
      </a>
      <nav class="guild-nav" aria-label="メイン">
        <a href="/g/${guildId?c}/events"<#if (nav!"") == "events"> aria-current="page"</#if>>イベント</a>
        <a href="/g/${guildId?c}/me"<#if (nav!"") == "me"> aria-current="page"</#if>>マイページ</a>
        <#if isAdmin?? && isAdmin>
          <a href="/g/${guildId?c}/settings"<#if (nav!"") == "settings"> aria-current="page"</#if>>設定</a>
        </#if>
      </nav>
    </#if>
    <div class="spacer"></div>
    <#if session??>
      <span class="user">${session.displayName}</span>
      <form method="post" action="/logout" class="inline-form">
        <button type="submit" class="link-button">ログアウト</button>
      </form>
    </#if>
  </div>
</header>
</#if>
<main class="container<#if bare!false> container-bare</#if><#if guildId?? && (tabbar!false)> has-tabbar</#if>">
