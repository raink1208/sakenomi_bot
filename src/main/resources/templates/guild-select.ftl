<#include "_header.ftl">
<section class="narrow">
  <h1>サーバーを選択</h1>
  <#if guilds?size == 0>
    <p class="empty">参加中かつ Bot が導入されているサーバーが見つかりませんでした。</p>
  <#else>
    <p class="page-sub">使うサーバーを選んでください。</p>
    <ul class="guild-list">
      <#list guilds as guild>
        <li>
          <a href="/g/${guild.id?c}/events">
            <span>${guild.name}</span>
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M9 6l6 6-6 6"/></svg>
          </a>
        </li>
      </#list>
    </ul>
  </#if>
</section>
<#include "_footer.ftl">
