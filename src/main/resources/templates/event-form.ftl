<#assign nav = "events">
<#include "_header.ftl">
<#if mode == "create">
  <a class="back-link" href="/g/${guildId?c}/events"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M15 6l-6 6 6 6"/></svg>イベント一覧</a>
<#else>
  <a class="back-link" href="/g/${guildId?c}/events/${eventId?c}"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M15 6l-6 6 6 6"/></svg>イベントに戻る</a>
</#if>
<section class="form-card">
  <h1><#if mode == "create">イベント作成<#else>イベント編集</#if></h1>
  <#if error??>
    <p class="error">${error}</p>
  </#if>
  <form method="post" action="<#if mode == 'create'>/g/${guildId?c}/events<#else>/g/${guildId?c}/events/${eventId?c}/edit</#if>">
    <label>
      イベント名(80文字以内)
      <input type="text" name="name" maxlength="80" required value="${form.name}">
    </label>
    <label>
      開催エリア(80文字以内)
      <input type="text" name="area" maxlength="80" required value="${form.area}">
    </label>
    <label>
      開催日(任意, YYYY-MM-DD)
      <input type="date" name="date" value="${form.date}">
    </label>
    <button type="submit" class="button">保存</button>
  </form>
</section>
<#include "_footer.ftl">
