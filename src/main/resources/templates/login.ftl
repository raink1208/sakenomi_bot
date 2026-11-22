<#assign bare = true>
<#include "_header.ftl">
<section class="login-card">
  <span class="brand-mark brand-mark-lg" aria-hidden="true">
    <svg width="34" height="34" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"><path d="M7 4h10l-1.2 12.5a3 3 0 0 1-3 2.5h-1.6a3 3 0 0 1-3-2.5L7 4z"/><path d="M7.6 9.5h8.8"/><path d="M12 19v2"/></svg>
  </span>
  <h1>飲み会管理コンソール</h1>
  <p>イベントへの参加、出欠や会計の確認ができます。</p>
  <a class="button button-block button-lg" href="/oauth/login" hx-boost="false">
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4"/><path d="M10 17l5-5-5-5"/><path d="M15 12H3"/></svg>
    Discord でログイン
  </a>
  <p class="login-note">Bot が導入されているサーバーに参加している必要があります。</p>
</section>
<#include "_footer.ftl">
