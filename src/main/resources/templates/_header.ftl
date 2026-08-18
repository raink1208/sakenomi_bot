<!DOCTYPE html>
<html lang="ja">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>飲み会管理コンソール</title>
  <link rel="stylesheet" href="/static/style.css">
  <!-- 通常のフルリロードと同じ体験にするため、4xx/5xx のエラーページ(error.ftl)も本文を差し替える -->
  <meta name="htmx-config" content='{"responseHandling":[{"code":".*", "swap": true}]}'>
  <script src="/static/htmx.min.js"></script>
</head>
<body hx-boost="true">
<div id="progress-bar"></div>
<header class="topbar">
  <a class="brand" href="/">🍻 飲み会管理コンソール</a>
  <#if guildId??>
    <nav class="guild-nav">
      <a href="/g/${guildId?c}/events">イベント</a>
      <a href="/g/${guildId?c}/me">マイページ</a>
      <#if isAdmin?? && isAdmin>
        <a href="/g/${guildId?c}/settings">設定</a>
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
</header>
<main class="container">
