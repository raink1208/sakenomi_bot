<#include "_header.ftl">
<section>
  <h1>サーバーを選択</h1>
  <#if guilds?size == 0>
    <p>参加中かつ Bot が導入されているサーバーが見つかりませんでした。</p>
  <#else>
    <ul class="guild-list">
      <#list guilds as guild>
        <li><a href="/g/${guild.id?c}/events">${guild.name}</a></li>
      </#list>
    </ul>
  </#if>
</section>
<#include "_footer.ftl">
